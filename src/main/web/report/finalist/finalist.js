/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

// Developer note: ES6 Map objects are not used due to them not serializing to JSON in 
// a way that is compatible with the Java Jackson library. Instead Objects are used
// as maps.

// Note that the modification functions do not save to local storage. It is up to the calling
// code to save to local storage. This is to support use cases where local storage is not used.
// See non-numeric_ui.js for this use case.


const finalist_module = {}

{
    if (typeof fllStorage != 'object') {
        throw new Error("fllStorage needs to be loaded");
    }

    const STORAGE_PREFIX = "fll.finalists";

    // //////////////////////// PRIVATE METHODS ////////////////////////
    let _teams;
    let _categories;
    let _tournament;
    let _divisions;
    let _currentDivision;
    let _numTeamsAutoSelected;
    let _scheduleParameters;
    let _categoriesVisited;
    let _currentCategoryName; // category to display with numeric.html
    let _playoffSchedules;
    let _schedules; // awardGroup -> list of FinalistDBRow

    function _init_variables() {
        _teams = {};
        _categories = {};
        _tournament = null;
        _divisions = [];
        _currentDivision = null;
        _numTeamsAutoSelected = 1;
        _scheduleParameters = {};
        _categoriesVisited = {};
        _currentCategoryName = null;
        _playoffSchedules = {};
        _schedules = {};
    }

    /**
     * Save the current state to local storage.
     */
    function _save() {
        fllStorage.set(STORAGE_PREFIX, "_teams", _teams);
        fllStorage.set(STORAGE_PREFIX, "_categories", _categories);
        fllStorage.set(STORAGE_PREFIX, "_tournament", _tournament);
        fllStorage.set(STORAGE_PREFIX, "_divisions", _divisions);
        fllStorage.set(STORAGE_PREFIX, "_currentDivision", _currentDivision);
        fllStorage.set(STORAGE_PREFIX, "_numTeamsAutoSelected",
            _numTeamsAutoSelected);
        fllStorage.set(STORAGE_PREFIX, "_scheduleParameters", _scheduleParameters);
        fllStorage.set(STORAGE_PREFIX, "_categoriesVisited", _categoriesVisited);
        fllStorage.set(STORAGE_PREFIX, "_currentCategoryName", _currentCategoryName);
        fllStorage.set(STORAGE_PREFIX, "_playoffSchedules", _playoffSchedules);

        fllStorage.set(STORAGE_PREFIX, "_schedules", _schedules);
    }

    /**
     * Load the current state from local storage.
     */
    function _load() {
        _init_variables();

        let value = fllStorage.get(STORAGE_PREFIX, "_teams");
        if (null != value) {
            _teams = value;
        }
        value = fllStorage.get(STORAGE_PREFIX, "_categories");
        if (null != value) {
            _categories = value;
        }
        value = fllStorage.get(STORAGE_PREFIX, "_tournament");
        if (null != value) {
            _tournament = value;
        }
        value = fllStorage.get(STORAGE_PREFIX, "_divisions");
        if (null != value) {
            _divisions = value;
        }
        value = fllStorage.get(STORAGE_PREFIX, "_currentDivision");
        if (null != value) {
            _currentDivision = value;
        }
        value = fllStorage.get(STORAGE_PREFIX, "_numTeamsAutoSelected");
        if (null != value) {
            _numTeamsAutoSelected = value;
        }
        value = fllStorage.get(STORAGE_PREFIX, "_scheduleParameters");
        if (null != value) {
            _scheduleParameters = value;
        }
        value = fllStorage.get(STORAGE_PREFIX, "_categoriesVisited");
        if (null != value) {
            _categoriesVisited = value;
        }
        value = fllStorage.get(STORAGE_PREFIX, "_currentCategoryName");
        if (null != value) {
            _currentCategoryName = value;
        }
        value = fllStorage.get(STORAGE_PREFIX, "_playoffSchedules");
        if (null != value) {
            _playoffSchedules = value;
        }

        value = fllStorage.get(STORAGE_PREFIX, "_schedules");
        if (null != value) {
            _schedules = value;
        }

    }

    /**
     * Clear anything from local storage with a prefix of STORAGE_PREFIX.
     */
    function _clear_local_storage() {
        fllStorage.clearNamespace(STORAGE_PREFIX);
    }

    /**
     * @return true if a category with the specified name exists
     */
    function _check_duplicate_category(name) {
        let duplicate = false;
        for (const [_, val] of Object.entries(_categories)) {
            if (val.name == name) {
                duplicate = true;
            }
        }
        return duplicate;
    }

    /**
     * Constructor for a Team.
     */
    function Team(num, name, org, judgingGroup, awardGroup) {
        if (typeof (_teams[num]) != 'undefined') {
            throw new Error("Team already exists with number: " + num);
        }

        this.num = num;
        this.awardGroup = awardGroup;
        this.name = name;
        this.org = org;
        this.judgingGroup = judgingGroup;
        this.categoryScores = {};
        // names of playoff brackets this team is competing in
        this.playoffDivisions = [];
        _teams[num] = this;
    }

    /**
     * Constructor for a category. Finds the first free ID and assigns it to this
     * new category.
     * 
     * @param name
     *          the name of the category
     * @param numeric
     *          boolean stating if this is a numeric or non-numeric category
     * @param overall
     *          true if a category that is awarded for the tournament rather than
     *          an award group
     */
    function Category(name, numeric, overall) {
        let category_id;
        // find the next available id
        for (category_id = 0; category_id < Number.MAX_VALUE
            && _categories[category_id]; category_id = category_id + 1)
            ;

        if (category_id == Number.MAX_VALUE || category_id + 1 == Number.MAX_VALUE) {
            throw new Error("No free category IDs");
        }

        this.name = name;
        this.numeric = numeric;
        this.catId = category_id;
        this.teams = []; // all teams regardless of division
        this.room = {}; // division -> room
        this.scheduled = false;
        this.overall = overall;
        this.judges = {}; // teamNumber -> [judges]

        _categories[this.catId] = this;
    }

    /**
     * Schedule timeslot.
     * 
     * @param time
     *          JSJoda.LocalTime object for start of slot
     * @param minutes
     *          minutes for time slot duration
     */
    function Timeslot(time, minutes) {
        this.categories = {}; // categoryId -> teamNumber
        this.time = time;
        const duration = JSJoda.Duration.ofMinutes(minutes)
        this.endTime = time.plus(duration);
    }

    /**
     * Add the specified offset to all timeslots in the specified schedule.
     * 
     * Does NOT save.
     *
     * @param currentDivision the schedule to change (if it exists)
     * @param offset the temporal offset to add to the schedule
     */
    function _addTimeToSchedule(currentDivision, offset) {
        const currentSchedule = _schedules[currentDivision];
        if (null != currentSchedule) {
            for (const slot of currentSchedule) {
                slot.time = slot.time.plus(offset);
                slot.endTime = slot.endTime.plus(offset);
            } // foreach timeslot
        }
    }

    /**
     * Add the specified offset to the duration of all timeslots in
     * existing schedules. This slides everything forward to prevent
     * 
     * Does NOT save.
     *
     * @param offset JSJoda.Duration
     */
    function _addToScheduleSlotDurations(offset) {

        for (const [_, schedule] of Object.entries(_schedules)) {
            if (null != schedule) {
                let addToStart = JSJoda.Duration.ofMinutes(0);
                let addToEnd = offset;

                for (const slot of schedule) {
                    slot.time = slot.time.plus(addToStart);
                    slot.endTime = slot.endTime.plus(addToEnd);

                    addToStart = addToEnd;
                    addToEnd = addToEnd.plus(offset);
                } // foreach timeslot
            }
        } // foreach schedule

    }

    /**
     * Make sure that the start and end times are JSJoda.LocalTime objects.
     */
    function _fixPlayoffSchedule(playoffSchedule) {
        if (null != playoffSchedule.startTime && !(playoffSchedule.startTime instanceof JSJoda.LocalTime)) {
            playoffSchedule.startTime = JSJoda.LocalTime.parse(playoffSchedule.startTime);
        }
        if (null != playoffSchedule.endTime && !(playoffSchedule.endTime instanceof JSJoda.LocalTime)) {
            playoffSchedule.endTime = JSJoda.LocalTime.parse(playoffSchedule.endTime);
        }
    }

    /**
     * Make sure that the start is a JSJoda.LocalTime object.
     */
    function _fixScheduleParameters(params) {
        if (null != params.startTime && !(params.startTime instanceof JSJoda.LocalTime)) {
            params.startTime = JSJoda.LocalTime.parse(params.startTime);
        }
    }

    /**
     * Make sure that the time is a JSJoda.LocalTime object.
     */
    function _fixFinalistDBRow(row) {
        if (null != row.time && !(row.time instanceof JSJoda.LocalTime)) {
            row.time = JSJoda.LocalTime.parse(row.time);
        }
        if (null != row.endTime && !(row.endTime instanceof JSJoda.LocalTime)) {
            row.endTime = JSJoda.LocalTime.parse(row.endTime);
        }
    }


    function _removeTeamFromCategory(category, teamNum) {
        teamNum = parseInt(teamNum, 10);
        var index = category.teams.indexOf(teamNum);
        if (index != -1) {
            if (category.overall) {
                // clear all schedules
                for (const division of finalist_module.getDivisions()) {
                    _schedules[division] = null;
                }
            } else {
                // clear the schedule for the current division
                _schedules[finalist_module.getCurrentDivision()] = null;
            }

            category.teams.splice(index, 1);
            delete category.judges[teamNum];
        }
    }

    // //////////////////////// PUBLIC INTERFACE /////////////////////////
    finalist_module.CHAMPIONSHIP_NAME = "Champion's";

    finalist_module.clearAllData = function() {
        _log("Clearing finalist data from local storage");
        _clear_local_storage();
        _init_variables();
    };

    /**
     * Convert a JSJoda.LocalTime object to a string to be displayed.
     */
    finalist_module.timeToDisplayString = function(time) {
        const timeFormatter = JSJoda.DateTimeFormatter.ofPattern("HH:mm");
        return time.format(timeFormatter);
    };

    /**
     * Mark a category as not visited so that the list of selected teams can be
     * recomputed.
     */
    finalist_module.unsetCategoryVisited = function(category, division) {
        const visited = _categoriesVisited[division];
        if (null != visited) {
            const index = visited.indexOf(category.catId);
            if (index >= 0) {
                visited.splice(index, 1);
                _categoriesVisited[division] = visited;
            }
        }
    };

    finalist_module.setCategoryVisited = function(category, division) {
        let visited = _categoriesVisited[division];
        if (null == visited) {
            visited = [];
            _categoriesVisited[division] = visited;
        }
        if (-1 == visited.indexOf(category.catId)) {
            _categoriesVisited[division].push(category.catId);
        }
    };

    finalist_module.isCategoryVisited = function(category, division) {
        var visited = _categoriesVisited[division];
        return null != visited && -1 != visited.indexOf(category.catId);
    };

    finalist_module.getNumTeamsAutoSelected = function() {
        return _numTeamsAutoSelected;
    };

    finalist_module.getTournament = function() {
        return _tournament;
    };

    finalist_module.setTournament = function(tournament) {
        _tournament = tournament;
    };

    /**
     * Add a division to the list of known divisions. If the division already
     * exists it is not added.
     */
    finalist_module.addDivision = function(division) {
        if (-1 == _divisions.indexOf(division)) {
            _divisions.push(division);
        }
    };

    finalist_module.getDivisions = function() {
        return _divisions;
    };

    finalist_module.setCurrentDivision = function(division) {
        _currentDivision = division;
    };

    finalist_module.getCurrentDivision = function() {
        return _currentDivision;
    };

    finalist_module.getDivisionByIndex = function(divIndex) {
        return _divisions[divIndex];
    };

    /**
     * Add a playoff division to the list of known playoff schedules. If the division
     * already exists it is not added.
     * 
     * @param division the name of the playoff bracket
     */
    finalist_module.addPlayoffDivision = function(division) {
        const existing = _playoffSchedules[division];
        if (null == existing) {
            const playoffSchedule = new PlayoffSchedule();
            _playoffSchedules[division] = playoffSchedule;
        }
    };

    /**
     * The playoff schedules. Note that the times may be null. 
     *
     * @return key=bracket name, value=PlayoffSchedule
     */
    finalist_module.getPlayoffSchedules = function() {
        return _playoffSchedules;
    };

    /**
     * @param division the playoff bracket name
     * @return JSJoda.LocalTime or null.
     */
    finalist_module.getPlayoffStartTime = function(division) {
        const existing = _playoffSchedules[division];
        if (null == existing) {
            return null;
        } else {
            return existing.startTime;
        }
    };

    /**
     * @param division playoff bracket name
     * @Param time JSJoda.LocalTime
     */
    finalist_module.setPlayoffStartTime = function(division, time) {
        let existing = _playoffSchedules[division];
        if (null == existing) {
            existing = new PlayoffSchdule();
        }
        existing.startTime = time;
        _playoffSchedules[division] = existing;
    };

    /**
     * @return JSJoda.LocalTime or null
     * @param division the playoff bracket name
     */
    finalist_module.getPlayoffEndTime = function(division) {
        const existing = _playoffSchedules[division];
        if (null == existing) {
            return null;
        } else {
            return existing.endTime;
        }
    };

    /**
     * @param division playoff bracket name
     * @Param time JSJoda.LocalTime
     */
    finalist_module.setPlayoffEndTime = function(division, time) {
        let existing = _playoffSchedules[division];
        if (null == existing) {
            existing = new PlayoffSchdule();
        }
        existing.endTime = time;
        _playoffSchedules[division] = existing;
    };

    /**
     * Get the room for the specified category and division.
     */
    finalist_module.getRoom = function(category, division) {
        return category.room[division];
    };

    finalist_module.setRoom = function(category, division, room) {
        category.room[division] = room;
    };

    /**
     * Create a new team.
     */
    finalist_module.addTeam = function(num, name, org, judgingStation, awardGroup) {
        return new Team(num, name, org, judgingStation, awardGroup);
    };

    /**
     * Add a team to a playoff division. A team may be in multiple divisions.
     * 
     * @param team
     *          a team object
     * @param division
     *          a string
     */
    finalist_module.addTeamToPlayoffDivision = function(team, division) {
        if (-1 == team.playoffDivisions.indexOf(division)) {
            team.playoffDivisions.push(division);
        }
    };

    /**
     * Find a team by number.
     * 
     * @param {int} teamNum the team number
     * @return the team or null if not found
     */
    finalist_module.lookupTeam = function(teamNum) {
        return _teams[teamNum];
    };

    finalist_module.getCategoryScore = function(team, category) {
        return team.categoryScores[category.catId];
    };

    finalist_module.setCategoryScore = function(team, category, score) {
        team.categoryScores[category.catId] = score;
    };

    /**
     * Get all teams.
     *
     * @return Array of teams
     */
    finalist_module.getAllTeams = function() {
        const teams = [];
        for (const [_, team] of Object.entries(_teams)) {
            teams.push(team);
        }
        return teams;
    };

    /**
     * compute the set of all score groups in the specified division.
     * 
     * @return map of score group to number of teams to auto select
     */
    finalist_module.getScoreGroups = function(teams, currentDivision) {
        const scoreGroups = {};
        for (const team of teams) {
            if (team.awardGroup == currentDivision) {
                const group = team.judgingGroup;
                scoreGroups[group] = finalist_module.getNumTeamsAutoSelected();
            }
        }
        return scoreGroups;
    };

    /**
     * Sort teams first by score group (unless this is the championship
     * category), then sort by score in the specified category.
     */
    finalist_module.sortTeamsByCategory = function(teams, currentCategory) {
        teams.sort(function(a, b) {
            if (currentCategory.name != finalist_module.CHAMPIONSHIP_NAME) {
                // sort by score group first
                const aGroup = a.judgingGroup;
                const bGroup = b.judgingGroup;
                if (aGroup < bGroup) {
                    return -1;
                } else if (aGroup > bGroup) {
                    return 1;
                }
                // fall through to score check
            }
            const aScore = finalist_module.getCategoryScore(a, currentCategory);
            const bScore = finalist_module.getCategoryScore(b, currentCategory);
            if (aScore == bScore) {
                return 0;
            } else if (null == aScore) {
                // no score is lowest
                return 1;
            } else if (null == bScore) {
                // no score is lowest
                return -1;
            } else if (aScore < bScore) {
                return 1;
            } else {
                return -1;
            }
        });
    };

    /**
     * Initialize the teams in the specified numeric category if it has not been
     * visited yet.
     * 
     * @param currentDivision the award group to work with
     * @param currentCategory the category object to initialize teams for
     * @param teams all teams, expected to already be sorted via sortTeamsByCategory
     * @param scoreGroups see getScoreGroups
     */
    finalist_module.initializeTeamsInNumericCategory = function(currentDivision,
        currentCategory, teams, scoreGroups) {
        const previouslyVisited = finalist_module.isCategoryVisited(currentCategory,
            currentDivision);
        if (previouslyVisited) {
            // don't mess with existing values
            return;
        }

        finalist_module.clearTeamsInCategory(currentCategory, currentDivision);
        const prevScores = {};
        finalist_module.sortTeamsByCategory(teams, currentCategory);
        for (const team of teams) {
            if (currentCategory.overall
                || currentDivision == team.awardGroup) {
                const group = team.judgingGroup;
                const prevScore = prevScores[group];
                const curScore = finalist_module.getCategoryScore(team, currentCategory);
                if (curScore != undefined && curScore > 0) {
                    if (prevScore == undefined) {
                        finalist_module.addTeamToCategory(currentCategory, team.num);
                        scoreGroups[group] = scoreGroups[group] - 1;
                        prevScores[group] = curScore;
                    } else if (Math.abs(prevScore - curScore) < 1) {
                        // tie
                        finalist_module.addTeamToCategory(currentCategory, team.num);
                        prevScores[group] = curScore;
                    } else if (scoreGroups[group] > 0) {
                        finalist_module.addTeamToCategory(currentCategory, team.num);
                        scoreGroups[group] = scoreGroups[group] - 1;
                        prevScores[group] = curScore;
                    } else {
                        // check if we can short circuit the loop over teams
                        let checkedEnoughTeams = true;
                        for (const [_, value] of Object.entries(scoreGroups)) {
                            if (value > 0) {
                                checkedEnoughTeams = false;
                            }
                        }
                        if (checkedEnoughTeams) {
                            break;
                        }
                    }
                }// valid curScore
            } // if current division
        } // foreach team
    };

    /**
     * Create a new category.
     * 
     * @param category_name
     *          the name of the category
     * @param numeric
     *          boolean if this is a numeric category or not
     * @param overall
     *          if true, this is a category that is awarded globally rather than
     *          by award group
     * @returns the new category or Null if there is a duplicate
     */
    finalist_module.addCategory = function(categoryName, numeric, overall) {
        if (_check_duplicate_category(categoryName)) {
            alert("There already exists a category with the name '" + categoryName
                + "'");
            return null;
        } else {
            const newCategory = new Category(categoryName, numeric, overall);
            return newCategory;
        }
    };

    /**
     * Remove the specified category.
     * 
     * @param category
     *          a category object
     */
    finalist_module.removeCategory = function(category) {
        delete _categories[category.catId];
    };

    /**
     * Get the non-numeric categories known to the system.
     * 
     * @returns {Array} sorted by name
     */
    finalist_module.getNonNumericCategories = function() {
        const categories = [];
        for (const [_, val] of Object.entries(_categories)) {
            if (!val.numeric) {
                categories.push(val);
            }
        }
        categories.sort(function(a, b) {
            if (a.name == b.name) {
                return 0;
            } else if (a.name < b.name) {
                return -1;
            } else {
                return 1;
            }
        });
        return categories;
    };

    /**
     * Get the numeric categories known to the system.
     * 
     * @returns {Array} sorted by name
     */
    finalist_module.getNumericCategories = function() {
        const categories = [];
        for (const [_, val] of Object.entries(_categories)) {
            if (val.numeric) {
                categories.push(val);
            }
        }
        categories.sort(function(a, b) {
            if (a.name == b.name) {
                return 0;
            } else if (a.name < b.name) {
                return -1;
            } else {
                return 1;
            }
        });
        return categories;
    };

    /**
     * Get the all categories known to the system.
     * 
     * @returns {Array} sorted by name
     */
    finalist_module.getAllCategories = function() {
        const categories = [];
        for (const [_, val] of Object.entries(_categories)) {
            categories.push(val);
        }
        categories.sort(function(a, b) {
            if (a.name == b.name) {
                return 0;
            } else if (a.name < b.name) {
                return -1;
            } else {
                return 1;
            }
        });
        return categories;
    };

    /**
     * Get the all categories to be scheduled.
     * 
     * @returns {Array} sorted by name
     */
    finalist_module.getAllScheduledCategories = function() {
        const categories = [];
        for (const [_, category] of Object.entries(_categories)) {
            if (finalist_module.isCategoryScheduled(category)) {
                categories.push(category);
            }
        }

        categories.sort(function(a, b) {
            if (a.name == b.name) {
                return 0;
            } else if (a.name < b.name) {
                return -1;
            } else {
                return 1;
            }
        });
        return categories;
    };

    /**
     * Get a category by name
     * 
     * @param toFind
     *          the name to find
     * @returns the category or null
     */
    finalist_module.getCategoryByName = function(toFind) {
        let category = null;
        for (const [_, val] of Object.entries(_categories)) {
            if (val.name == toFind) {
                category = val;
            }
        }
        return category;
    };

    finalist_module.addTeamToCategory = function(category, teamNum) {
        teamNum = parseInt(teamNum, 10);
        const index = category.teams.indexOf(teamNum);
        if (-1 == index) {
            if (finalist_module.isCategoryScheduled(category)) {
                if (category.overall) {
                    // clear schedule for all categories
                    for (const division of finalist_module.getDivisions()) {
                        _schedules[division] = null;
                    }
                } else {
                    // clear the schedule for the current division
                    _schedules[finalist_module.getCurrentDivision()] = null;
                }
            }

            category.teams.push(teamNum);
        }
    };

    finalist_module.removeTeamFromCategory = function(category, teamNum) {
        _removeTeamFromCategory(category, teamNum);
    };

    finalist_module.isTeamInCategory = function(category, teamNum) {
        teamNum = parseInt(teamNum, 10);
        return -1 != category.teams.indexOf(teamNum);
    };

    finalist_module.clearTeamsInCategory = function(category, division) {
        const toRemove = [];
        for (const teamNum of category.teams) {
            const team = finalist_module.lookupTeam(teamNum);
            if (category.overall || division == team.awardGroup) {
                toRemove.push(teamNum);
            }
        }

        for (const teamNum of toRemove) {
            _removeTeamFromCategory(category, teamNum);
        }
    };

    finalist_module.addTeamToTimeslot = function(timeslot, categoryName, teamNum) {
        timeslot.categories[categoryName] = teamNum;
    };

    finalist_module.clearTimeslot = function(timeslot) {
        timeslot.categories = {};
    };

    /**
     * Check if this timeslot is busy for the specified category
     */
    finalist_module.isTimeslotBusy = function(timeslot, categoryName) {
        return timeslot.categories[categoryName] != null;
    };

    finalist_module.isTeamInTimeslot = function(timeslot, teamNum) {
        let found = false;
        for (const [_, slotTeamNum] of Object.entries(timeslot.categories)) {
            if (teamNum == slotTeamNum) {
                found = true;
            }
        }
        return found;
    };

    /**
     * Get the schedule for the specified division.
     */
    finalist_module.getSchedule = function(currentDivision) {
        return _schedules[currentDivision];
    };

    /**
     * Set the schedule for the specified division.
     */
    finalist_module.setSchedule = function(currentDivision, schedule) {
        _schedules[currentDivision] = schedule;
    };

    /**
     * @param division
     *          the division to work with
     * @return Map with key of team number and value is an array of scheduled
     *         categories that the team is being judged in
     */
    finalist_module.getTeamToCategoryMap = function(division) {
        const finalistsCount = new Map();
        for (const category of finalist_module.getAllScheduledCategories()) {
            for (const teamNum of category.teams) {
                const team = finalist_module.lookupTeam(teamNum);
                if (category.overall || division == team.awardGroup) {
                    if (!finalistsCount.has(teamNum)) {
                        finalistsCount.set(teamNum, []);
                    }
                    finalistsCount.get(teamNum).push(category);
                }
            }
        }

        return finalistsCount;
    };

    /**
     * @param finalistsCount the output of getTeamToCategoryMap
     * @return array of teams sorted so that the team in the most categories is first
     */
    finalist_module.sortTeamsByCategoryCount = function(finalistsCount) {
        const sortedTeams = [];

        for (const [teamNum, _] of finalistsCount) {
            sortedTeams.push(teamNum);
        }
        sortedTeams.sort(function(a, b) {
            const aCategories = finalistsCount.get(a);
            const bCategories = finalistsCount.get(b);

            const aCount = aCategories.length;
            const bCount = bCategories.length;
            if (aCount == bCount) {
                return 0;
            } else if (aCount > bCount) {
                return -1;
            } else {
                return 1;
            }
        });

        return sortedTeams;
    };

    /**
     * Create the finalist schedule for the specified division.
     * 
     * @param currentDivision
     *          the division to create the schedule for
     * @return array of timeslots in order from earliest to latest
     */
    finalist_module.scheduleFinalists = function(currentDivision) {
        _log("Creating schedule for " + currentDivision);

        const finalistsCount = finalist_module.getTeamToCategoryMap(currentDivision);

        // sort the map so that the team in the most categories is first,
        // this
        // should ensure the minimum amount of time to do the finalist
        // judging
        const sortedTeams = finalist_module.sortTeamsByCategoryCount(finalistsCount);

        // list of Timeslots in time order
        const schedule = [];
        let nextTime = finalist_module.getStartTime(currentDivision);
        const slotMinutes = finalist_module.getDuration(currentDivision);
        const slotDuration = JSJoda.Duration.ofMinutes(slotMinutes);
        finalist_module.log("Next timeslot starts at " + nextTime + " duration is " + slotDuration);
        for (const teamNum of sortedTeams) {
            const team = finalist_module.lookupTeam(teamNum);
            const teamCategories = finalistsCount.get(teamNum);
            for (const category of teamCategories) {
                if (finalist_module.isCategoryScheduled(category)) {

                    let scheduled = false;
                    for (const slot of schedule) {
                        if (!scheduled
                            && !finalist_module.isTimeslotBusy(slot, category.name)
                            && !finalist_module.isTeamInTimeslot(slot, teamNum)
                            && !finalist_module.hasPlayoffConflict(team, slot)) {
                            finalist_module.addTeamToTimeslot(slot, category.name, teamNum);
                            scheduled = true;
                        }
                    } // foreach timeslot

                    while (!scheduled) {
                        const newRow = new FinalistDBRow(nextTime, nextTime.plus(slotDuration));
                        schedule.push(newRow);

                        nextTime = newRow.endTime;

                        if (!finalist_module.hasPlayoffConflict(team, newRow)) {
                            scheduled = true;
                            finalist_module.addTeamToTimeslot(newRow, category.name, teamNum);
                        }
                    }

                } // category is scheduled

            } // foreach category
        } // foreach sorted team

        return schedule;
    };

    /**
     * Sort the specified schedule by time. This is useful after adding slots
     * that may be out of order.
     */
    finalist_module.sortSchedule = function(schedule) {
        schedule.sort(function(slotA, slotB) {
            return slotA.compareTo(slotB);
        });
    };

    /**
     * Add a timeslot to the specified schedule. Note that the schedule is not
     * saved by this function.
     * 
     * @param schedule
     *          the schedule to add a slot to, it is modified
     * @return the slot that was added.
     */
    finalist_module.addSlotToSchedule = function(schedule) {
        const lastRow = schedule[schedule.length - 1];
        const duration = JSJoda.Duration.ofMinutes(finalist_module.getDuration(finalist_module.getCurrentDivision()));
        const newRow = new FinalistDBRow(lastRow.endTime, lastRow.endTime.plus(duration));
        schedule.push(newRow);

        return newRow;
    };

    /**
     * Check if a team has a playoff conflict with the specified timeslot
     * 
     * @param team
     *          Team object
     * @param slot
     *          Timeslot object
     * @returns true or false
     */
    finalist_module.hasPlayoffConflict = function(team, slot) {
        let conflict = false;
        for (const bracketName of team.playoffDivisions) {
            const playoffSchedule = _playoffSchedules[bracketName];
            if (null != playoffSchedule && finalist_module.slotHasPlayoffConflict(playoffSchedule, slot)) {
                conflict = true;
            }
        }
        return conflict;
    };

    /**
     * Check if there is a conflict between the specified time slot and the
     * playoff times for the specified playoff division.
     * 
     * @param playoffSchedule
     *          The playoff schedule
     * @param slot
     *          Timeslot object
     * @returns true or false
     */
    finalist_module.slotHasPlayoffConflict = function(playoffSchedule, slot) {
        const start = playoffSchedule.startTime;
        const end = playoffSchedule.endTime;
        if (null != start && null != end) {
            if (start.isBefore(slot.endTime)
                && slot.time.isBefore(end)) {
                return true;
            }
        }
        return false;
    };

    /**
     * The schedule parameters.
     *
     * @param currentDivision the award group to get the schedule start time for
     */
    finalist_module.getScheduleParameters = function(currentDivision) {
        let prevValue = _scheduleParameters[currentDivision];
        if (undefined == prevValue) {
            prevValue = new FinalistScheduleParameters();
            _scheduleParameters[currentDivision] = prevValue;
        }
        return prevValue;
    };

    /**
     * The start time for the schedule.
     *
     * @param currentDivision the award group to get the schedule start time for
     */
    finalist_module.getStartTime = function(currentDivision) {
        return finalist_module.getScheduleParameters(currentDivision).startTime;
    };

    /**
     * Set a new start time for the schedule. This adjusts the schedule by the difference between the old and new times.
     *
     * @param currentDivision the award group to set the schedule start time for
     * @param newStartTime the new start time for the schedule
     */
    finalist_module.setStartTime = function(currentDivision, newStartTime) {
        const prevValue = finalist_module.getStartTime(currentDivision);
        const diff = prevValue.until(newStartTime, JSJoda.ChronoUnit.MINUTES);
        const durationDiff = JSJoda.Duration.ofMinutes(diff);
        _addTimeToSchedule(currentDivision, durationDiff);

        finalist_module.getScheduleParameters(currentDivision).startTime = newStartTime;
    };


    /**
     * Set a slot duration for the schedule. This adjusts the schedule by the difference between the old and new slot durations.
     *
     * @param currentDivision the award group to set the schedule slot duration for
     * @param v the new slot duration for the schedule
     */
    finalist_module.setDuration = function(currentDivision, v) {
        const prevValue = finalist_module.getDuration(currentDivision);
        const diffMinutes = v - prevValue;
        const diffDuration = JSJoda.Duration.ofMinutes(diffMinutes);
        _addToScheduleSlotDurations(diffDuration);

        finalist_module.getScheduleParameters(currentDivision).intervalMinutes = v;
    };

    /**
     * @return the value or 20 minutes if not yet set
     */
    finalist_module.getDuration = function(currentDivision) {
        return finalist_module.getScheduleParameters(currentDivision).intervalMinutes;
    };

    finalist_module.setCurrentCategoryName = function(name) {
        _currentCategoryName = name;
    };

    finalist_module.getCurrentCategoryName = function() {
        return _currentCategoryName;
    };

    finalist_module.displayNavbar = function() {
        const navbar = document.getElementById("navbar");

        let element;
        if (window.location.pathname.match(/\/params.html$/)) {
            element = document.createElement("span");
        } else {
            element = document.createElement("a");
            element.setAttribute("href", "params.html");
        }
        element.innerText = "Parameters";
        navbar.appendChild(element);

        const spacer = document.createElement("span");
        spacer.innerText = " - ";
        navbar.appendChild(spacer.cloneNode(true));

        if (window.location.pathname.match(/\/non-numeric.html$/)) {
            element = document.createElement("span");
        } else {
            element = document.createElement("a");
            element.setAttribute("href", "non-numeric.html");
        }
        element.innerText = "Non-numeric";
        navbar.appendChild(element);

        navbar.appendChild(spacer.cloneNode(true));

        for (const category of finalist_module.getNumericCategories()) {
            if (category.name != finalist_module.CHAMPIONSHIP_NAME) {
                if (window.location.pathname.match(/\/numeric.html$/)
                    && finalist_module.getCurrentCategoryName() == category.name) {
                    element = document.createElement("span");
                } else {
                    element = document.createElement("a");
                    element.setAttribute("href", "numeric.html");
                    element.addEventListener('click', function() {
                        finalist_module.setCurrentCategoryName(category.name);
                        finalist_module.saveToLocalStorage();
                    });
                }
                element.innerText = category.name;
                navbar.appendChild(element);

                navbar.appendChild(spacer.cloneNode(true));
            }
        }

        // make sure that championship is last
        const championshipCategory = finalist_module
            .getCategoryByName(finalist_module.CHAMPIONSHIP_NAME);
        if (window.location.pathname.match(/\/numeric.html$/)
            && finalist_module.getCurrentCategoryName() == championshipCategory.name) {
            element = document.createElement("span");
        } else {
            element = document.createElement("a");
            element.setAttribute("href", "numeric.html");
            element.addEventListener('click', function() {
                finalist_module.setCurrentCategoryName(championshipCategory.name);
                finalist_module.saveToLocalStorage();
            });
        }
        element.innerText = finalist_module.CHAMPIONSHIP_NAME;
        navbar.appendChild(element);

        navbar.appendChild(spacer.cloneNode(true));

        if (window.location.pathname.match(/\/schedule.html$/)) {
            element = document.createElement("span");
        } else {
            element = document.createElement("a");
            element.setAttribute("href", "schedule.html");
        }
        element.innerText = "Schedule";
        navbar.appendChild(element);

    };

    finalist_module.log = function(str) {
        if (typeof (console) != 'undefined') {
            console.log(str);
        }
    };

    /**
     * @param category
     *          the category
     * @param isScheduled
     *          true if this category should be considered in the schedule
     */
    finalist_module.setCategoryScheduled = function(category, isScheduled) {
        category.scheduled = isScheduled;

        // invalidate the schedule
        finalist_module.setSchedule(finalist_module.getCurrentDivision(), null);
    };

    /**
     * @param category
     *          the category to check
     * @return is this category to be put in the schedule
     */
    finalist_module.isCategoryScheduled = function(category) {
        return category.scheduled;
    };

    /**
     * Get the judges that nominated a team for a category.
     */
    finalist_module.getNominatingJudges = function(category, teamNumber) {
        return category.judges[teamNumber];
    };

    finalist_module.setNominatingJudges = function(category, teamNumber, judges) {
        category.judges[teamNumber] = judges;
    };

    /**
      * Load the non-numeric nominees from the server.
      * 
      * @return promise to execute
      */
    finalist_module.loadNonNumericNominees = function() {
        return fetch("../../api/NonNumericNominees").then(checkJsonResponse).then(function(data) {
            for (const nonNumericNominee of data) {
                let category = finalist_module.getCategoryByName(nonNumericNominee.categoryName);
                if (null == category) {
                    // 8/22/2020 JPS - this isn't needed other than support for old databases where
                    // the non-numeric categories are not in the challenge description. All of these
                    // categories are per award group.
                    category = finalist_module.addCategory(nonNumericNominee.categoryName, false, false);

                    // eventually throw new Error("Cannot find non-numeric category '" + nonNumericNominee.categoryName + "'");
                }

                for (const nominee of nonNumericNominee.nominees) {
                    finalist_module.addTeamToCategory(category, nominee.teamNumber);
                    finalist_module.setNominatingJudges(category, nominee.teamNumber, nominee.judges);
                } // category scores
            } // categories
        });
    };

    /**
     * Convert the non-numeric nominee information into a list of
     * NonNumericNominees objects and upload to the server.
     * @param successCallback called with the server result on success
     * @param failCallback called with the server result on failure
     * @return promise to execute
     */
    finalist_module.uploadNonNumericNominees = function(successCallback, failCallback) {
        const allNonNumericNominees = [];
        for (const category of finalist_module.getNonNumericCategories()) {
            var categoryNominees = [];
            for (const teamNumber of category.teams) {
                const judges = finalist_module.getNominatingJudges(category, teamNumber);
                const nominee = new Nominee(teamNumber, judges);
                categoryNominees.push(nominee);
            } // foreach team
            const nominees = new NonNumericNominees(category.name, categoryNominees);
            allNonNumericNominees.push(nominees);
        } // foreach category

        const dataToUpload = JSON.stringify(allNonNumericNominees);
        return fetch("../../api/NonNumericNominees", {
            method: "POST",
            headers: { 'Content-Type': 'application/json' },
            body: dataToUpload
        }).then(checkJsonResponse).then(function(result) {
            if (result.success) {
                successCallback(result);
            } else {
                failCallback(result);
            }
        }).catch(function(result) {
            failCallback(result);
        });
    };

    /**
     * Load the playoff schedules from the server.
     * 
     * @return promise to execute
     */
    finalist_module.loadPlayoffSchedules = function() {
        return fetch("../../api/PlayoffSchedules").then(checkJsonResponse).then(function(data) {
            for (const [bracketName, playoffSchedule] of Object.entries(data)) {
                _fixPlayoffSchedule(playoffSchedule);
                _playoffSchedules[bracketName] = playoffSchedule;
            }
        });
    };

    /**
     * Upload the playoff schedules to the server.
     * 
     * @param successCallback called with the server result on success
     * @param failCallback called with the server result on failure
     * @return promise to execute
     */
    finalist_module.uploadPlayoffSchedules = function(successCallback, failCallback) {
        const schedulesToUpload = {};
        for (const [bracketName, schedule] of Object.entries(_playoffSchedules)) {
            if (null != schedule.startTime && null != schedule.endTime) {
                schedulesToUpload[bracketName] = schedule;
            }
        }

        const dataToUpload = JSON.stringify(schedulesToUpload);
        return fetch("../../api/PlayoffSchedules", {
            method: "POST",
            headers: { 'Content-Type': 'application/json' },
            body: dataToUpload
        }).then(checkJsonResponse).then(function(result) {
            if (result.success) {
                successCallback(result);
            } else {
                failCallback(result);
            }
        }).catch(function(result) {
            failCallback(result);
        });
    };

    /**
      * Load the finalist schedule parameters from the server.
      * 
      * @return promise to execute
      */
    finalist_module.loadFinalistScheduleParameters = function() {
        return fetch("../../api/FinalistScheduleParameters").then(checkJsonResponse).then(function(data) {
            for (const [awardGroup, parameters] of Object.entries(data)) {
                _fixScheduleParameters(parameters);
                _scheduleParameters[awardGroup] = parameters;
            }
        });
    };

    /**
     * Upload the schedule parameters to the server.
     * 
     * @param successCallback called with the server result on success
     * @param failCallback called with the server result on failure
     * @return promise to execute
     */
    finalist_module.uploadScheduleParameters = function(successCallback, failCallback) {
        const paramsToUpload = {};
        for (const [awardGroup, params] of Object.entries(_scheduleParameters)) {
            if (null != params.startTime) {
                paramsToUpload[awardGroup] = params;
            }
        }

        const dataToUpload = JSON.stringify(paramsToUpload);
        return fetch("../../api/FinalistScheduleParameters", {
            method: "POST",
            headers: { 'Content-Type': 'application/json' },
            body: dataToUpload
        }).then(checkJsonResponse).then(function(result) {
            if (result.success) {
                successCallback(result);
            } else {
                failCallback(result);
            }
        }).catch(function(result) {
            failCallback(result);
        });
    };

    /**
     * Load the finalist schedules from the server.
     * 
     * @return promise to execute
     */
    finalist_module.loadFinalistSchedules = function() {
        return fetch("../../api/FinalistSchedule").then(checkJsonResponse).then(function(data) {
            for (const [awardGroup, schedule] of Object.entries(data)) {
                for (const finalistCategory of schedule.categories) {
                    const category = finalist_module.getCategoryByName(finalistCategory.categoryName);
                    if (null == category) {
                        alert("Unable to find category with name '" + finalistCategory.categoryName + "' referenced in the schedule categories'");
                    } else {
                        finalist_module.setRoom(category, awardGroup, finalistCategory.room);

                        // if the category is in a schedule, then it's scheduled
                        finalist_module.setCategoryScheduled(category, true);
                    }
                }

                for (const scheduleRow of schedule.schedule) {
                    _fixFinalistDBRow(scheduleRow);

                    for (const [categoryName, teamNumber] of Object.entries(scheduleRow.categories)) {
                        const category = finalist_module.getCategoryByName(categoryName);
                        if (null == category) {
                            alert("Unable to find category with name '" + categoryName + "' referenced in the schedule rows'");
                        } else {
                            finalist_module.addTeamToCategory(category, teamNumber);
                            // mark category visited so that the numeric nominees don't get reset'
                            finalist_module.setCategoryVisited(category, awardGroup);
                        }
                    }
                }

                // _schedules is the array of FinalistDBRow objects
                _schedules[awardGroup] = schedule.schedule;
            }
        });
    };

    /**
     * Upload the schedules to the server.
     * 
     * @param successCallback called with the server result on success
     * @param failCallback called with the server result on failure
     * @return promise to execute
     */
    finalist_module.uploadSchedules = function(successCallback, failCallback) {
        const scheduleMap = {}
        for (const [awardGroup, scheduleRows] of Object.entries(_schedules)) {
            if (null != awardGroup && null != scheduleRows) {
                const categoryRows = [];
                for (const category of finalist_module.getAllScheduledCategories()) {
                    const cat = new FinalistCategory(category.name, finalist_module.getRoom(category,
                        awardGroup));
                    categoryRows.push(cat);
                } // foreach category

                const finalistSchedule = new FinalistSchedule(categoryRows, scheduleRows);
                scheduleMap[awardGroup] = finalistSchedule;
            }
        }

        const dataToUpload = JSON.stringify(scheduleMap);
        return fetch("../../api/FinalistSchedule", {
            method: "POST",
            headers: { 'Content-Type': 'application/json' },
            body: dataToUpload
        }).then(checkJsonResponse).then(function(result) {
            if (result.success) {
                successCallback(result);
            } else {
                failCallback(result);
            }
        }).catch(function(result) {
            failCallback(result);
        });
    };

    /**
      * Load the award groups from the server.
      * 
      * @return promise to execute
      */
    finalist_module.loadAwardGroups = function() {
        return fetch("../../api/AwardGroups").then(checkJsonResponse).then(function(data) {
            for (const awardGroup of data) {
                finalist_module.addDivision(awardGroup);
            }
        });
    };

    /**
      * Load the playoff brackets from the server.
      * 
      * @return promise to execute
      */
    finalist_module.loadPlayoffBrackets = function() {
        return fetch("../../api/PlayoffBrackets").then(checkJsonResponse).then(function(data) {
            for (const bracket of data) {
                finalist_module.addPlayoffDivision(bracket);
            }
        });
    };


    /**
      * Load the numeric categories in the challenge description from the server.
      * 
      * @return promise to execute
      */
    finalist_module.loadNumericCategories = function() {
        return fetch("../../api/ChallengeDescription/SubjectiveCategories").then(checkJsonResponse).then(function(subjectiveCategories) {
            for (const categoryDescription of subjectiveCategories) {
                const category = finalist_module.getCategoryByName(categoryDescription.title);
                if (null == category) {
                    const newCategory = finalist_module.addCategory(categoryDescription.title, true, false);
                    // all subjective categories are scheduled
                    finalist_module.setCategoryScheduled(newCategory, true);
                }
            }

            let championship = finalist_module
                .getCategoryByName(finalist_module.CHAMPIONSHIP_NAME);
            if (null == championship) {
                championship = finalist_module.addCategory(finalist_module.CHAMPIONSHIP_NAME, true,
                    false);
            }
            finalist_module.setCategoryScheduled(championship, true);

        });
    };

    /**
      * Load the non-numeric categories in the challenge description from the server.
      * 
      * @return promise to execute
      */
    finalist_module.loadNonNumericCategories = function() {
        return fetch("../../api/ChallengeDescription/NonNumericCategories").then(checkJsonResponse).then(function(nonNumericCategories) {
            for (const categoryDescription of nonNumericCategories) {
                const category = finalist_module.getCategoryByName(categoryDescription.title);
                if (null == category) {
                    finalist_module.addCategory(categoryDescription.title, false, !categoryDescription.perAwardGroup);
                }
            }
        });
    };

    /**
    * Load the teams from the server.
    * 
    * @return promise to execute
    */
    finalist_module.loadTournamentTeams = function() {
        return fetch("../../api/TournamentTeams").then(checkJsonResponse).then(function(data) {
            for (const tournamentTeam of data) {
                let team = finalist_module.lookupTeam(tournamentTeam.teamNumber);
                if (null == team) {
                    team = finalist_module.addTeam(tournamentTeam.teamNumber, tournamentTeam.teamName, tournamentTeam.organization, tournamentTeam.judgingGroup, tournamentTeam.awardGroup);
                }
            } // teams
        });
    };

    /**
    * Load the teams in each playoff bracket from the server.
    * 
    * @return promise to execute
    */
    finalist_module.loadPlayoffBracketTeams = function() {
        return fetch("../../api/PlayoffBracketTeams").then(checkJsonResponse).then(function(data) {
            for (const [playoffBracket, teams] of Object.entries(data)) {
                for (const teamNumber of teams) {
                    const team = finalist_module.lookupTeam(teamNumber);
                    if (null == team) {
                        alert("Cannot find team with number " + teamNumber + " that is specified in the playoff bracket " + playoffBracket);
                    }
                    finalist_module.addTeamToPlayoffDivision(team, playoffBracket);
                } // teams
            } // brackets
        });
    };

    /**
     * Load the overall scores from the server.
     * 
     * @return promise to execute
     */
    finalist_module.loadOverallScores = function() {
        return fetch("../../api/OverallScores").then(checkJsonResponse).then(function(data) {
            const championship = finalist_module
                .getCategoryByName(finalist_module.CHAMPIONSHIP_NAME);
            if (null == championship) {
                throw new Error("Missing championship category");
            }

            for (const [teamNumber, score] of Object.entries(data)) {
                const team = finalist_module.lookupTeam(teamNumber);
                if (null == team) {
                    throw new Error("Cannot find team with " + teamNumber + " found in overall scores");
                }

                finalist_module.setCategoryScore(team, championship, score);
            } // scores
        });
    };

    /**
     * Load the scores for the numeric categories from the server.
     * 
     * @return promise to execute
     */
    finalist_module.loadCategoryScores = function() {
        return fetch("../../api/NumericCategoryScores").then(checkJsonResponse).then(function(data) {
            for (const [categoryName, categoryScores] of Object.entries(data)) {
                const category = finalist_module.getCategoryByName(categoryName);
                if (null == category) {
                    throw new Error("Cannot find category '" + categoryName + "'");
                }

                for (const [teamNumber, score] of Object.entries(categoryScores)) {
                    const team = finalist_module.lookupTeam(teamNumber);
                    if (null == team) {
                        throw new Error("Cannot find team with " + teamNumber + " found in scores for category '" + categoryName + "'");
                    }

                    finalist_module.setCategoryScore(team, category, score);
                } // category scores
            } // categories
        });
    };

    /**
     * Load the current tournament name.
     */
    finalist_module.loadCurrentTournament = function() {
        return fetch("../../api/Tournaments/current").then(checkJsonResponse).then(function(tournament) {
            finalist_module.setTournament(tournament.name);
        });
    };

    /**
     * Load the current tournament.
     */
    finalist_module.loadTournament = function(doneCallback, failCallback) {
        const waitList = [];

        const loadCurrentTournamentPromise = finalist_module.loadCurrentTournament();
        loadCurrentTournamentPromise.catch(function() {
            failCallback("Current tournament");
        })
        waitList.push(loadCurrentTournamentPromise);

        Promise.all(waitList).then(function(_) {
            doneCallback();
        });
    };

    finalist_module.loadCategoriesAndScores = function(doneCallback, failCallback) {

        // Some things need to be loaded first
        const waitList1 = [];

        const teamsPromise = finalist_module.loadTournamentTeams();
        teamsPromise.catch(function() {
            failCallback("Teams");
        });
        waitList1.push(teamsPromise);

        const numericCategoriesPromise = finalist_module.loadNumericCategories();
        numericCategoriesPromise.catch(function() {
            failCallback("Numeric categories");
        });
        waitList1.push(numericCategoriesPromise);

        const nonNumericCategoriesPromise = finalist_module.loadNonNumericCategories();
        nonNumericCategoriesPromise.catch(function() {
            failCallback("Non-Numeric categories");
        });
        waitList1.push(nonNumericCategoriesPromise);

        const awardGroupsPromise = finalist_module.loadAwardGroups();
        awardGroupsPromise.catch(function() {
            failCallback("Award Groups");
        })
        waitList1.push(awardGroupsPromise);

        const playoffBracketsPromise = finalist_module.loadPlayoffBrackets();
        playoffBracketsPromise.catch(function() {
            failCallback("Playoff Brackets");
        })
        waitList1.push(playoffBracketsPromise);


        Promise.all(waitList1).then(function(_) {
            // everything else can be loaded in parallel
            const waitList = [];

            const overallScoresPromise = finalist_module.loadOverallScores();
            overallScoresPromise.catch(function() {
                failCallback("Overall scores");
            })
            waitList.push(overallScoresPromise);

            const numericCategoryScoresPromise = finalist_module.loadCategoryScores();
            numericCategoryScoresPromise.catch(function() {
                failCallback("Numeric category scores");
            })
            waitList.push(numericCategoryScoresPromise);

            const playoffBracketTeamsPromise = finalist_module.loadPlayoffBracketTeams();
            playoffBracketTeamsPromise.catch(function() {
                failCallback("Playoff bracket teams");
            })
            waitList.push(playoffBracketTeamsPromise);

            Promise.all(waitList1).then(function(_) {
                doneCallback();
            });

        });
    };

    /**
     * @param doneCallback
     *          called with no arguments on success
     * @param failCallback
     *          called with message on failure
     */
    finalist_module.loadNominieesAndSchedules = function(doneCallback, failCallback) {
        const waitList = [];

        const playoffSchedulesPromise = finalist_module.loadPlayoffSchedules();
        playoffSchedulesPromise.catch(function() {
            failCallback("Playoff Schedules");
        })
        waitList.push(playoffSchedulesPromise);

        const finalistParamsPromise = finalist_module.loadFinalistScheduleParameters();
        finalistParamsPromise.catch(function() {
            failCallback("Finalist Schedule Parameters");
        })
        waitList.push(finalistParamsPromise);

        const finalistSchedulesPromise = finalist_module.loadFinalistSchedules();
        finalistSchedulesPromise.catch(function() {
            failCallback("Finalist Schedules");
        })
        waitList.push(finalistSchedulesPromise);

        const nonNumericNomineesPromise = finalist_module.loadNonNumericNominees();
        nonNumericNomineesPromise.catch(function() {
            failCallback("Non-numeric nominees");
        })
        waitList.push(nonNumericNomineesPromise);

        Promise.all(waitList).then(function(_) {
            doneCallback();
        });
    };

    finalist_module.saveToLocalStorage = function() {
        _save();
    };

    finalist_module.loadFromLocalStorage = function() {
        _load();
    };

    // always need to initialize variables
    _init_variables();

}
