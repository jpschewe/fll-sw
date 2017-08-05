/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

(function($) {
  if (!$) {
    throw new Error("jQuery needs to be loaded!");
  }
  if (!$.jStorage) {
    throw new Error("jStorage needs to be loaded!");
  }

  var STORAGE_PREFIX = "fll.finalists";

  // //////////////////////// PRIVATE METHODS ////////////////////////
  var _teams;
  var _categories;
  var _tournament;
  var _divisions;
  var _currentDivision;
  var _numTeamsAutoSelected;
  var _startHour;
  var _startMinute;
  var _duration; // minutes
  var _categoriesVisited;
  var _currentCategoryId; // category to display with numeric.html
  var _playoffDivisions;
  var _playoffStartHour;
  var _playoffStartMinute;
  var _playoffEndHour;
  var _playoffEndMinute;
  var _schedules;

  function _init_variables() {
    _teams = {};
    _categories = {};
    _tournament = null;
    _divisions = [];
    _currentDivision = null;
    _numTeamsAutoSelected = 1;
    _startHour = 2;
    _startMinute = 0;
    _duration = 20;
    _categoriesVisited = {};
    _currentCategory = null;
    _playoffDivisions = [];
    _playoffStartHour = {};
    _playoffStartMinute = {};
    _playoffEndHour = {};
    _playoffEndMinute = {};
    _schedules = {};
  }

  /**
   * Save the current state to local storage.
   */
  function _save() {
    $.jStorage.set(STORAGE_PREFIX + "_teams", _teams);
    $.jStorage.set(STORAGE_PREFIX + "_categories", _categories);
    $.jStorage.set(STORAGE_PREFIX + "_tournament", _tournament);
    $.jStorage.set(STORAGE_PREFIX + "_divisions", _divisions);
    $.jStorage.set(STORAGE_PREFIX + "_currentDivision", _currentDivision);
    $.jStorage.set(STORAGE_PREFIX + "_numTeamsAutoSelected",
        _numTeamsAutoSelected);
    $.jStorage.set(STORAGE_PREFIX + "_startHour", _startHour);
    $.jStorage.set(STORAGE_PREFIX + "_startMinute", _startMinute);
    $.jStorage.set(STORAGE_PREFIX + "_duration", _duration);
    $.jStorage.set(STORAGE_PREFIX + "_categoriesVisited", _categoriesVisited);
    $.jStorage.set(STORAGE_PREFIX + "_currentCategoryId", _currentCategoryId);
    $.jStorage.set(STORAGE_PREFIX + "_playoffDivisions", _playoffDivisions);
    $.jStorage.set(STORAGE_PREFIX + "_playoffStartHour", _playoffStartHour);
    $.jStorage.set(STORAGE_PREFIX + "_playoffStartMinute", _playoffStartMinute);
    $.jStorage.set(STORAGE_PREFIX + "_playoffEndHour", _playoffEndHour);
    $.jStorage.set(STORAGE_PREFIX + "_playoffEndMinute", _playoffEndMinute);

    $.jStorage.set(STORAGE_PREFIX + "_schedules", _schedules);
  }

  /**
   * Load the current state from local storage.
   */
  function _load() {
    _init_variables();

    var value = $.jStorage.get(STORAGE_PREFIX + "_teams");
    if (null != value) {
      _teams = value;
    }
    value = $.jStorage.get(STORAGE_PREFIX + "_categories");
    if (null != value) {
      _categories = value;
    }
    value = $.jStorage.get(STORAGE_PREFIX + "_tournament");
    if (null != value) {
      _tournament = value;
    }
    value = $.jStorage.get(STORAGE_PREFIX + "_divisions");
    if (null != value) {
      _divisions = value;
    }
    value = $.jStorage.get(STORAGE_PREFIX + "_currentDivision");
    if (null != value) {
      _currentDivision = value;
    }
    value = $.jStorage.get(STORAGE_PREFIX + "_numTeamsAutoSelected");
    if (null != value) {
      _numTeamsAutoSelected = value;
    }
    value = $.jStorage.get(STORAGE_PREFIX + "_startHour");
    if (null != value) {
      _startHour = value;
    }
    value = $.jStorage.get(STORAGE_PREFIX + "_startMinute");
    if (null != value) {
      _startMinute = value;
    }
    value = $.jStorage.get(STORAGE_PREFIX + "_duration");
    if (null != value) {
      _duration = value;
    }
    value = $.jStorage.get(STORAGE_PREFIX + "_categoriesVisited");
    if (null != value) {
      _categoriesVisited = value;
    }
    value = $.jStorage.get(STORAGE_PREFIX + "_currentCategoryId");
    if (null != value) {
      _currentCategoryId = value;
    }
    value = $.jStorage.get(STORAGE_PREFIX + "_playoffDivisions");
    if (null != value) {
      _playoffDivisions = value;
    }

    value = $.jStorage.get(STORAGE_PREFIX + "_playoffStartHour");
    if (null != value) {
      _playoffStartHour = value;
    }
    value = $.jStorage.get(STORAGE_PREFIX + "_playoffStartMinute");
    if (null != value) {
      _playoffStartMinute = value;
    }
    value = $.jStorage.get(STORAGE_PREFIX + "_playoffEndHour");
    if (null != value) {
      _playoffEndHour = value;
    }
    value = $.jStorage.get(STORAGE_PREFIX + "_playoffEndMinute");
    if (null != value) {
      _playoffEndMinute = value;
    }

    value = $.jStorage.get(STORAGE_PREFIX + "_schedules");
    if (null != value) {
      _schedules = value;
    }
  }

  /**
   * Clear anything from local storage with a prefix of STORAGE_PREFIX.
   */
  function _clear_local_storage() {
    $.each($.jStorage.index(), function(index, value) {
      if (value.substring(0, STORAGE_PREFIX.length) == STORAGE_PREFIX) {
        $.jStorage.deleteKey(value);
      }
    });
  }

  /**
   * @return true if a category with the specified name exists
   */
  function _check_duplicate_category(name) {
    var duplicate = false;
    $.each(_categories, function(i, val) {
      if (val.name == name) {
        duplicate = true;
      }
    });
    return duplicate;
  }

  /**
   * Constructor for a Team.
   */
  function Team(num, name, org, judgingGroup) {
    if (typeof (_teams[num]) != 'undefined') {
      throw new Error("Team already exists with number: " + num);
    }

    this.num = num;
    this.divisions = [];
    this.name = name;
    this.org = org;
    this.judgingGroup = judgingGroup;
    this.categoryScores = {};
    this.playoffDivisions = [];
    _teams[num] = this;
    _save();
  }

  /**
   * Constructor for a category. Finds the first free ID and assigns it to this
   * new category.
   * 
   * @param name
   *          the name of the category
   * @param numeric
   *          boolean stating if this is a numeric or non-numeric category
   */
  function Category(name, numeric) {
    var category_id;
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
    this.teams = []; // all teams reguardless of division
    this.isPublic = true;
    this.room = {}; // division -> room

    _categories[this.catId] = this;
    _save();
  }

  /**
   * A time object.
   */
  function Time(hour, minute) {
    this.hour = hour;
    this.minute = minute;
  }

  /**
   * Convert a time to a Date object
   */
  function timeToDate(time) {
    var d = new Date();
    d.setHours(time.hour);
    d.setMinutes(time.minute);
    return d;
  }

  /**
   * Schedule timeslot.
   * 
   * @param time
   *          Time object for start of slot
   * @param duration
   *          integer number of minutes for time slot
   */
  function Timeslot(time, duration) {
    this.categories = {}; // categoryId -> teamNumber
    this.time = time;
    this.endTime = $.finalist.addMinutesToTime(this.time, duration);
  }

  function _addMinutesToTime(time, minutes) {
    var d = timeToDate(time);
    d.setTime(d.getTime() + (minutes * 60 * 1000));
    var t = new Time(d.getHours(), d.getMinutes());
    return t;
  }

  /**
   * Add the specified number of minutes to all timeslots in existing schedules.
   * 
   * Does NOT save.
   */
  function _addMinutesToSchedules(minutes) {

    $.each(_schedules, function(i, schedule) {
      if (null != schedule) {
        $.each(schedule, function(k, slot) {

          slot.time = $.finalist.addMinutesToTime(slot.time, minutes);
          slot.endTime = $.finalist.addMinutesToTime(slot.endTime, minutes);

        }); // foreach timeslot
      }
    }); // foreach schedule

  }

  /**
   * Add the specified number of minutes to the duration of all timeslots in
   * existing schedules. This slides everything forward to prevent
   * 
   * Does NOT save.
   */
  function _addMinutesToDurationOfSchedules(minutes) {

    $.each(_schedules, function(i, schedule) {
      if (null != schedule) {
        var addToStart = 0;
        var addToEnd = minutes;

        $.each(schedule, function(k, slot) {

          slot.time = $.finalist.addMinutesToTime(slot.time, addToStart);
          slot.endTime = $.finalist.addMinutesToTime(slot.endTime, addToEnd);

          addToStart = addToEnd;
          addToEnd = addToEnd + minutes;
        }); // foreach timeslot
      }
    }); // foreach schedule

  }

  // //////////////////////// PUBLIC INTERFACE /////////////////////////
  $.finalist = {
    CHAMPIONSHIP_NAME : "Championship",

    clearAllData : function() {
      _clear_local_storage();
      _init_variables();
    },

    /**
     * Compare 2 times.
     * 
     * @return -1 if a is less than b, 0 if they are equal, 1 if a is greater
     *         than b
     */
    compareTimes : function(a, b) {
      if (a.hour == b.hour) {
        if (a.minute == b.minute) {
          return 0;
        } else if (a.minute < b.minute) {
          return -1;
        } else {
          return 1;
        }
      } else if (a.hour < b.hour) {
        return -1
      } else {
        return 1;
      }
    },

    /**
     * Add a number of minutes to a time.
     * 
     * @param time
     *          Time object
     * @param minutes
     *          integer minutes
     * @return a new Time object
     */
    addMinutesToTime : function(time, minutes) {
      return _addMinutesToTime(time, minutes);
    },

    /**
     * Convert a time object to a string to be displayed.
     */
    timeToDisplayString : function(time) {
      return time.hour.toString().padL(2, "0") + ":"
          + time.minute.toString().padL(2, "0");
    },

    setCategoryVisited : function(category, division) {
      var visited = _categoriesVisited[division];
      if (null == visited) {
        visited = [];
        _categoriesVisited[division] = visited;
      }
      if (-1 == visited.indexOf(category.catId)) {
        _categoriesVisited[division].push(category.catId);
      }
    },

    isCategoryVisited : function(category, division) {
      var visited = _categoriesVisited[division];
      return null != visited && -1 != visited.indexOf(category.catId);
    },

    getNumTeamsAutoSelected : function() {
      return _numTeamsAutoSelected;
    },

    getTournament : function() {
      return _tournament;
    },

    setTournament : function(tournament) {
      _tournament = tournament;
      _save();
    },

    /**
     * Add a division to the list of known divisions. If the division already
     * exists it is not added.
     */
    addDivision : function(division) {
      if (-1 == $.inArray(division, _divisions)) {
        _divisions.push(division);
      }
      _save();
    },

    getDivisions : function() {
      return _divisions;
    },

    setCurrentDivision : function(division) {
      _currentDivision = division;
      _save();
    },

    getCurrentDivision : function() {
      return _currentDivision;
    },

    getDivisionByIndex : function(divIndex) {
      return _divisions[divIndex];
    },

    /**
     * Add a playoff division to the list of known divisions. If the division
     * already exists it is not added.
     */
    addPlayoffDivision : function(division) {
      if (-1 == $.inArray(division, _playoffDivisions)) {
        _playoffDivisions.push(division);

        $.finalist.setPlayoffStartHour(division, -1);
        $.finalist.setPlayoffStartMinute(division, -1);
        $.finalist.setPlayoffEndHour(division, -1);
        $.finalist.setPlayoffEndMinute(division, -1);
      }
      _save();
    },

    getPlayoffDivisions : function() {
      return _playoffDivisions;
    },

    getPlayoffDivisionByIndex : function(divIndex) {
      return _playoffDivisions[divIndex];
    },

    /**
     * Integer or undefined or -1 (unset).
     */
    getPlayoffStartHour : function(division) {
      return _playoffStartHour[division];
    },
    setPlayoffStartHour : function(division, hour) {
      _playoffStartHour[division] = hour;
      _save();
    },

    /**
     * Integer or undefined or -1 (unset).
     */
    getPlayoffStartMinute : function(division) {
      return _playoffStartMinute[division];
    },
    setPlayoffStartMinute : function(division, minute) {
      _playoffStartMinute[division] = minute;
      _save();
    },

    /**
     * Time or undefined.
     */
    getPlayoffStartTime : function(division) {
      var hour = $.finalist.getPlayoffStartHour(division);
      var minute = $.finalist.getPlayoffStartMinute(division);
      if (hour != undefined && hour >= 0 && minute != undefined && minute >= 0) {
        var time = new Time(hour, minute);
        return time;
      } else {
        return undefined;
      }
    },

    /**
     * Integer or undefined or -1 (unset).
     */
    getPlayoffEndHour : function(division) {
      return _playoffEndHour[division];
    },
    setPlayoffEndHour : function(division, hour) {
      _playoffEndHour[division] = hour;
      _save();
    },

    /**
     * Integer or undefined or -1 (unset).
     */
    getPlayoffEndMinute : function(division) {
      return _playoffEndMinute[division];
    },
    setPlayoffEndMinute : function(division, minute) {
      _playoffEndMinute[division] = minute;
      _save();
    },

    /**
     * Time or undefined.
     */
    getPlayoffEndTime : function(division) {
      var hour = $.finalist.getPlayoffEndHour(division);
      var minute = $.finalist.getPlayoffEndMinute(division);
      if (hour != undefined && hour >= 0 && minute != undefined && minute >= 0) {
        var time = new Time(hour, minute);
        return time;
      } else {
        return undefined;
      }
    },

    /**
     * Get the room for the specified category and division.
     */
    getRoom : function(category, division) {
      return category.room[division];
    },

    setRoom : function(category, division, room) {
      category.room[division] = room;
      _save();
    },

    /**
     * Create a new team.
     */
    addTeam : function(num, name, org, judgingStation) {
      return new Team(num, name, org, judgingStation);
    },

    /**
     * Add a team to a division. A team may be in multiple divisions.
     * 
     * @param team
     *          a team object
     * @param division
     *          a string
     */
    addTeamToDivision : function(team, division) {
      if (-1 == $.inArray(division, team.divisions)) {
        team.divisions.push(division);
      }
    },

    /**
     * Add a team to a playoff division. A team may be in multiple divisions.
     * 
     * @param team
     *          a team object
     * @param division
     *          a string
     */
    addTeamToPlayoffDivision : function(team, division) {
      if (-1 == $.inArray(division, team.playoffDivisions)) {
        team.playoffDivisions.push(division);
      }
    },

    /**
     * Check if a team is in a division.
     * 
     * @param team
     *          a team object
     * @param division
     *          a string
     * @return true/false
     */
    isTeamInDivision : function(team, division) {
      if (-1 == $.inArray(division, team.divisions)) {
        return false;
      } else {
        return true;
      }
    },

    /**
     * Find a team by number.
     */
    lookupTeam : function(teamNum) {
      return _teams[teamNum];
    },

    getCategoryScore : function(team, category) {
      return team.categoryScores[category.catId];
    },

    setCategoryScore : function(team, category, score) {
      team.categoryScores[category.catId] = score;
      _save;
    },

    /**
     * Get all teams.
     */
    getAllTeams : function() {
      var teams = [];
      $.each(_teams, function(i, val) {
        teams.push(val);
      });
      return teams;
    },

    /**
     * compute the set of all score groups in the specified division.
     * 
     * @return map of score group to number of teams to auto select
     */
    getScoreGroups : function(teams, currentDivision) {
      var scoreGroups = {};
      $.each(teams, function(i, team) {
        if ($.finalist.isTeamInDivision(team, currentDivision)) {
          var group = team.judgingGroup;
          scoreGroups[group] = $.finalist.getNumTeamsAutoSelected();
        }
      });
      return scoreGroups;
    },

    /**
     * Sort teams first by score group (unless this is the championship
     * category), then sort by score in the specified category.
     */
    sortTeamsByCategory : function(teams, currentCategory) {
      teams.sort(function(a, b) {
        if (currentCategory.name != $.finalist.CHAMPIONSHIP_NAME) {
          // sort by score group first
          var aGroup = a.judgingGroup;
          var bGroup = b.judgingGroup;
          if (aGroup < bGroup) {
            return -1;
          } else if (aGroup > bGroup) {
            return 1;
          }
          // fall through to score check
        }
        var aScore = $.finalist.getCategoryScore(a, currentCategory);
        var bScore = $.finalist.getCategoryScore(b, currentCategory);
        if (aScore == bScore) {
          return 0;
        } else if (aScore < bScore) {
          return 1;
        } else {
          return -1;
        }
      });
    },

    /**
     * Initialize the teams in the specified numeric category if it has not been
     * visited yet.
     */
    initializeTeamsInNumericCategory : function(currentDivision,
        currentCategory, teams, scoreGroups) {
      var previouslyVisited = $.finalist.isCategoryVisited(currentCategory,
          currentDivision);
      if (previouslyVisited) {
        // don't mess with existing values
        return;
      }

      $.finalist.clearTeamsInCategory(currentCategory, currentDivision);
      var checkedEnoughTeams = false;
      var prevScores = {};
      $.finalist.sortTeamsByCategory(teams, currentCategory);
      $.each(teams, function(i, team) {
        if ($.finalist.isTeamInDivision(team, currentDivision)) {
          if (!checkedEnoughTeams) {
            var group = team.judgingGroup;
            prevScore = prevScores[group];
            curScore = $.finalist.getCategoryScore(team, currentCategory);
            if (prevScore == undefined) {
              $.finalist.addTeamToCategory(currentCategory, team.num);
            } else if (scoreGroups[group] > 0) {
              if (Math.abs(prevScore - curScore) < 1) {
                $.finalist.addTeamToCategory(currentCategory, team.num);
              } else {
                scoreGroups[group] = scoreGroups[group] - 1;

                checkedEnoughTeams = true;
                $.each(scoreGroups, function(key, value) {
                  if (value > 0) {
                    checkedEnoughTeams = false;
                  }
                });
              }
            }
            prevScores[group] = curScore;
          } // checked enough teams
        } // if current division
      }); // foreach team
    },

    /**
     * Create a new category.
     * 
     * @param category_name
     *          the name of the category
     * @param numeric
     *          boolean if this is a numeric category or not
     * @returns the new category or Null if there is a duplicate
     */
    addCategory : function(categoryName, numeric) {
      if (_check_duplicate_category(categoryName)) {
        alert("There already exists a category with the name '" + categoryName
            + "'");
        return null;
      } else {
        var newCategory = new Category(categoryName, numeric);
        return newCategory;
      }
    },

    /**
     * Remove the speciifed category.
     * 
     * @param category
     *          a category object
     */
    removeCategory : function(category) {
      delete _categories[category.catId];
      _save();
    },

    /**
     * Get the non-numeric categories known to the system.
     * 
     * @returns {Array} sorted by name
     */
    getNonNumericCategories : function() {
      var categories = [];
      $.each(_categories, function(i, val) {
        if (!val.numeric) {
          categories.push(val);
        }
      });
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
    },

    /**
     * Get the numeric categories known to the system.
     * 
     * @returns {Array} sorted by name
     */
    getNumericCategories : function() {
      var categories = [];
      $.each(_categories, function(i, val) {
        if (val.numeric) {
          categories.push(val);
        }
      });
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
    },

    /**
     * Get the all categories known to the system.
     * 
     * @returns {Array} sorted by name
     */
    getAllCategories : function() {
      var categories = [];
      $.each(_categories, function(i, val) {
        categories.push(val);
      });
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
    },

    /**
     * Get a category by id
     * 
     * @param toFind
     *          the id to find
     * @returns the category or null
     */
    getCategoryById : function(toFind) {
      var category = null;
      $.each(_categories, function(i, val) {
        if (val.catId == toFind) {
          category = val;
        }
      });
      return category;
    },

    getCategoryByName : function(toFind) {
      var category = null;
      $.each(_categories, function(i, val) {
        if (val.name == toFind) {
          category = val;
        }
      });
      return category;
    },

    setCategoryName : function(category, newName) {
      if (category.name == newName) {
        return true;
      }

      if (_check_duplicate_category(newName)) {
        return false;
      } else {
        category.name = newName;
        _save();
        return true;
      }
    },

    addTeamToCategory : function(category, teamNum) {
      teamNum = parseInt(teamNum, 10);
      var index = category.teams.indexOf(teamNum);
      if (-1 == index) {
        // clear the schedule for the current division
        _schedules[$.finalist.getCurrentDivision()] = null;

        category.teams.push(teamNum);
        _save();
      }
    },

    /**
     * Removes a team and saves the data.
     */
    removeTeamFromCategory : function(category, teamNum) {
      $.finalist._removeTeamFromCategory(category, teamNum, true);
    },

    /**
     * Removes a team, but only calls save if told to.
     */
    _removeTeamFromCategory : function(category, teamNum, save) {
      teamNum = parseInt(teamNum, 10);
      var index = category.teams.indexOf(teamNum);
      if (index != -1) {
        // clear the schedule for the current division
        _schedules[$.finalist.getCurrentDivision()] = null;

        category.teams.splice(index, 1);
        if (save) {
          _save();
        }
      }
    },

    isTeamInCategory : function(category, teamNum) {
      teamNum = parseInt(teamNum, 10);
      return -1 != category.teams.indexOf(teamNum);
    },

    clearTeamsInCategory : function(category, division) {
      var toRemove = [];
      $.each(category.teams, function(index, teamNum) {
        var team = $.finalist.lookupTeam(teamNum);
        if ($.finalist.isTeamInDivision(team, division)) {
          toRemove.push(teamNum);
        }
      });

      $.each(toRemove, function(index, teamNum) {
        $.finalist._removeTeamFromCategory(category, teamNum, false);
      });
      _save();
    },

    addTeamToTimeslot : function(timeslot, categoryId, teamNum) {
      timeslot.categories[categoryId] = teamNum;
    },

    clearTimeslot : function(timeslot) {
      timeslot.categories = {};
    },

    /**
     * Check if this timeslot is busy for the specified category
     */
    isTimeslotBusy : function(timeslot, categoryId) {
      return timeslot.categories[categoryId] != null;
    },

    isTeamInTimeslot : function(timeslot, teamNum) {
      var found = false;
      $.each(timeslot.categories, function(catId, slotTeamNum) {
        if (teamNum == slotTeamNum) {
          found = true;
        }
      });
      return found;
    },

    setCategoryPublic : function(category, value) {
      category.isPublic = value;
      _save();
    },

    isCategoryPublic : function(category) {
      return category.isPublic;
    },

    /**
     * Get the schedule for the specified division. If no schedule exists, one
     * is created.
     * 
     * @return the output of scheduleFinalists
     * @see scheduleFinalists
     */
    getSchedule : function(currentDivision) {
      var schedule = _schedules[currentDivision];
      if (null == schedule) {
        schedule = $.finalist.scheduleFinalists(currentDivision);
        _schedules[currentDivision] = schedule;
        _save();
      }
      return schedule;
    },

    /**
     * Set the schedule for the specified division.
     * 
     * @see scheduleFinalists
     */
    setSchedule : function(currentDivision, schedule) {
      _schedules[currentDivision] = schedule;
      _save();
    },

    /**
     * @param division the division to work with
     * @return map with key of team number and value is an array of categories
     *         that the team is being judged in
     */
    getTeamToCategoryMap : function(division) {
      var finalistsCount = {};
      $.each($.finalist.getAllCategories(), function(i, category) {
        $.each(category.teams, function(j, teamNum) {
          var team = $.finalist.lookupTeam(teamNum);
          if ($.finalist.isTeamInDivision(team, division)) {
            if (null == finalistsCount[teamNum]) {
              finalistsCount[teamNum] = [];
            }
            finalistsCount[teamNum].push(category);
          }
        });
      });

      return finalistsCount;
    },

    /**
     * Create the finalist schedule for the specified division.
     * 
     * @param currentDivision
     *          the division to create the schedule for
     * @return array of timeslots in order from earliest to latest
     */
    scheduleFinalists : function(currentDivision) {
      var finalistsCount = $.finalist.getTeamToCategoryMap(currentDivision);

      // sort the map so that the team in the most categories is first,
      // this
      // should ensure the minimum amount of time to do the finalist
      // judging
      var sortedTeams = [];
      $.each(finalistsCount, function(teamNum, categories) {
        sortedTeams.push(teamNum);
      });
      sortedTeams.sort(function(a, b) {
        var aCategories = finalistsCount[a];
        var bCategories = finalistsCount[b];

        var aCount = aCategories.length;
        var bCount = bCategories.length;
        if (aCount == bCount) {
          return 0;
        } else if (aCount > bCount) {
          return -1;
        } else {
          return 1;
        }
      });

      // list of Timeslots in time order
      var schedule = [];
      var nextTime = $.finalist.getStartTime();
      var slotDuration = $.finalist.getDuration();
      $.finalist.log("Next timeslot starts at " + nextTime.hour + ":"
          + nextTime.minute + " duration is " + slotDuration);
      $.each(sortedTeams, function(i, teamNum) {
        var team = $.finalist.lookupTeam(teamNum);
        var teamCategories = finalistsCount[teamNum];
        $.each(teamCategories, function(j, category) {
          var scheduled = false;
          $.each(schedule, function(k, slot) {
            if (!scheduled && !$.finalist.isTimeslotBusy(slot, category.catId)
                && !$.finalist.isTeamInTimeslot(slot, teamNum)
                && !$.finalist.hasPlayoffConflict(team, slot)) {
              $.finalist.addTeamToTimeslot(slot, category.catId, teamNum);
              scheduled = true;
            }
          }); // foreach timeslot
          while (!scheduled) {
            var newSlot = new Timeslot(nextTime, slotDuration);
            schedule.push(newSlot);

            nextTime = $.finalist.addMinutesToTime(nextTime, slotDuration);

            if (!$.finalist.hasPlayoffConflict(team, newSlot)) {
              scheduled = true;
              $.finalist.addTeamToTimeslot(newSlot, category.catId, teamNum);
            }
          }
        }); // foreach category
      }); // foreach sorted team

      return schedule;
    },

    /**
     * Sort the specified schedule by time. This is useful after adding slots
     * that may be out of order.
     */
    sortSchedule : function(schedule) {
      schedule.sort(function(slotA, slotB) {
        return $.finalist.compareTimes(slotA.time, slotB.time);
      });
    },

    /**
     * Add a timeslot to the specified schedule. Note that the schedule is not
     * saved by this function.
     * 
     * @param schedule
     *          the schedule to add a slot to, it is modified
     * @return the slot that was added.
     */
    addSlotToSchedule : function(schedule) {
      var lastSlot = schedule[schedule.length - 1];
      var newSlot = new Timeslot(lastSlot.endTime, $.finalist.getDuration());
      schedule.push(newSlot);

      return newSlot;
    },

    /**
     * Check if a team has a playoff conflict with the specified timeslot
     * 
     * @param team
     *          Team object
     * @param slot
     *          Timeslot object
     * @returns true or false
     */
    hasPlayoffConflict : function(team, slot) {
      var conflict = false;
      $.each(team.playoffDivisions, function(i, playoffDivision) {
        var start = $.finalist.getPlayoffStartTime(playoffDivision);
        var end = $.finalist.getPlayoffEndTime(playoffDivision);
        if ($.finalist.slotHasPlayoffConflict(playoffDivision, slot)) {
          conflict = true;
        }
      });
      return conflict;
    },

    /**
     * Check if there is a conflict between the specified time slot and the
     * playoff times for the specified playoff division.
     * 
     * @param playoffDivision
     *          one of the playoff divisions
     * @param slot
     *          Timeslot object
     * @returns true or false
     */
    slotHasPlayoffConflict : function(playoffDivision, slot) {
      var start = $.finalist.getPlayoffStartTime(playoffDivision);
      var end = $.finalist.getPlayoffEndTime(playoffDivision);
      if (start != undefined && end != undefined) {
        if ($.finalist.compareTimes(start, slot.endTime) < 0
            && $.finalist.compareTimes(slot.time, end) < 0) {
          return true;
        }
      }
      return false;
    },

    setStartHour : function(hour) {
      var diffHours = hour - _startHour;
      var diffMinutes = diffHours * 60;
      _addMinutesToSchedules(diffMinutes);

      _startHour = hour;
      _save();
    },

    getStartHour : function() {
      return _startHour;
    },

    setStartMinute : function(minute) {
      var diffMinutes = minute - _startMinute;
      _addMinutesToSchedules(diffMinutes);

      _startMinute = minute;
      _save();
    },

    getStartMinute : function() {
      return _startMinute;
    },

    getStartTime : function() {
      var time = new Time(_startHour, _startMinute);
      return time;
    },

    setDuration : function(v) {
      var diffMinutes = v - _duration;
      _addMinutesToDurationOfSchedules(diffMinutes);

      _duration = v;
      _save();
    },

    getDuration : function() {
      return _duration;
    },

    setCurrentCategoryId : function(catId) {
      _currentCategoryId = catId;
      _save();
    },

    getCurrentCategoryId : function() {
      return _currentCategoryId;
    },

    displayNavbar : function() {
      var element;
      if (window.location.pathname.match(/\/params.html$/)) {
        element = $("<span></span>");
      } else {
        element = $("<a href='params.html'></a>");
      }
      element.text("Parameters");
      $("#navbar").append(element);

      $("#navbar").append($("<span> - </span>"));

      if (window.location.pathname.match(/\/non-numeric.html$/)) {
        element = $("<span></span>");
      } else {
        element = $("<a href='non-numeric.html'></a>");
      }
      element.text("Non-numeric");
      $("#navbar").append(element);

      $("#navbar").append($("<span> - </span>"));

      $.each($.finalist.getNumericCategories(), function(i, category) {
        if (category.name != $.finalist.CHAMPIONSHIP_NAME) {
          if (window.location.pathname.match(/\/numeric.html$/)
              && window.location.search == "?" + category.catId) {
            element = $("<span></span>");
          } else {
            element = $("<a href='numeric.html'></a>");
            element.click(function() {
              $.finalist.setCurrentCategoryId(category.catId);
            });
          }
          element.text(category.name);
          $("#navbar").append(element);

          $("#navbar").append($("<span> - </span>"));
        }
      });

      // make sure that championship is last
      var championshipCategory = $.finalist
          .getCategoryByName($.finalist.CHAMPIONSHIP_NAME);
      if (window.location.pathname.match(/\/numeric.html$/)
          && window.location.search == "?" + championshipCategory.catId) {
        element = $("<span></span>");
      } else {
        element = $("<a href='numeric.html'></a>");
        element.click(function() {
          $.finalist.setCurrentCategoryId(championshipCategory.catId);
        });
      }
      element.text($.finalist.CHAMPIONSHIP_NAME);
      $("#navbar").append(element);

      $("#navbar").append($("<span> - </span>"));

      if (window.location.pathname.match(/\/choose-public-categories.html$/)) {
        element = $("<span></span>");
      } else {
        element = $("<a href='choose-public-categories.html'></a>");
      }
      element.text("Public Categories");
      $("#navbar").append(element);

      $("#navbar").append($("<span> - </span>"));

      if (window.location.pathname.match(/\/schedule.html$/)) {
        element = $("<span></span>");
      } else {
        element = $("<a href='schedule.html'></a>");
      }
      element.text("Schedule");
      $("#navbar").append(element);

    },

    handleCacheEvent : function(e) {
      $.finalist.log("cache event: " + e.type);
      var appCache = window.applicationCache;
      switch (appCache.status) {
      case appCache.UNCACHED: // UNCACHED == 0
        $.finalist.log('cache state:UNCACHED');
        break;
      case appCache.IDLE: // IDLE == 1
        $.finalist.log('cache state:IDLE');
        $("#cache-ready").show();
        break;
      case appCache.CHECKING: // CHECKING == 2
        $.finalist.log('cache state:CHECKING');
        break;
      case appCache.DOWNLOADING: // DOWNLOADING == 3
        $.finalist.log('cache state:DOWNLOADING');
        break;
      case appCache.UPDATEREADY: // UPDATEREADY == 4
        $.finalist.log('cache state:UPDATEREADY');
        $("#cache-ready").show();
        break;
      case appCache.OBSOLETE: // OBSOLETE == 5
        $.finalist.log('cache state:OBSOLETE');
        break;
      default:
        return 'cache state:UKNOWN CACHE STATUS';
        break;
      }
      ;
    },

    setupAppCache : function() {
      $("#cache-ready").hide();

      var appCache = window.applicationCache;
      if (!appCache) {
        alert("Your browser doesn't support application caching. This app cannot be run offline");
        return;
      }

      // Fired after the first cache of the manifest.
      appCache.addEventListener('cached', $.finalist.handleCacheEvent, false);

      // Checking for an update. Always the first event fired in the
      // sequence.
      appCache.addEventListener('checking', $.finalist.handleCacheEvent, false);

      // An update was found. The browser is fetching resources.
      appCache.addEventListener('downloading', $.finalist.handleCacheEvent,
          false);

      // The manifest returns 404 or 410, the download failed,
      // or the manifest changed while the download was in progress.
      appCache.addEventListener('error', $.finalist.handleCacheEvent, false);

      // Fired after the first download of the manifest.
      appCache.addEventListener('noupdate', $.finalist.handleCacheEvent, false);

      // Fired if the manifest file returns a 404 or 410.
      // This results in the application cache being deleted.
      appCache.addEventListener('obsolete', $.finalist.handleCacheEvent, false);

      // Fired for each resource listed in the manifest as it is being
      // fetched.
      appCache.addEventListener('progress', $.finalist.handleCacheEvent, false);

      // Fired when the manifest resources have been newly redownloaded.
      appCache.addEventListener('updateready', $.finalist.handleCacheEvent,
          false);

      appCache.addEventListener('error', function(e) {
        $.finalist.log("Error loading the appcache manifest");
      }, false);

      if (appCache.status == appCache.UPDATEREADY
          || appCache.status == appCache.IDLE) {
        $.finalist.log("poll: cache ready");
        $("#cache-ready").show();
      }
    },

    log : function(str) {
      if (typeof (console) != 'undefined') {
        console.log(str);
      }
    },

  };

  _load();
})(window.jQuery || window.$);
