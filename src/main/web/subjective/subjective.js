/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

(function($) {
    if (!$) {
        throw new Error("jQuery needs to be loaded!");
    }
    if (!$.jStorage) {
        throw new Error("jStorage needs to be loaded!");
    }

    var STORAGE_PREFIX = "fll.subjective.";

    // //////////////////////// PRIVATE INTERFACE ////////////////////////

    var _subjectiveCategories; // category.name -> category
    var _nonNumericCategories; // category.title -> category
    var _tournament;
    var _teams;
    var _schedule;
    var _currentJudgingGroup;
    var _currentCategory;
    var _judges;
    var _currentJudge;
    var _allScores;
    var _teamTimeCache;
    var _currentTeam;
    var _scoreEntryBackPage;
    var _categoryColumnMapping;

    function _init_variables() {
        _subjectiveCategories = {};
        _nonNumericCategories = {};
        _tournament = null;
        _teams = {};
        _schedule = null;
        _currentJudgingGroup = null;
        _currentCategory = null;
        _judges = [];
        _currentJudge = null;
        _allScores = {};
        _teamTimeCache = {};
        _currentTeam = null;
        _scoreEntryBackPage = null;
        _categoryColumnMapping = null;
    }

    function _loadFromDisk() {
        _init_variables();

        var value = $.jStorage.get(STORAGE_PREFIX + "_subjectiveCategories");
        if (null != value) {
            _subjectiveCategories = value;
        }

        var value = $.jStorage.get(STORAGE_PREFIX + "_nonNumericCategories");
        if (null != value) {
            _nonNumericCategories = value;
        }

        value = $.jStorage.get(STORAGE_PREFIX + "_tournament");
        if (null != value) {
            _tournament = value;
        }

        value = $.jStorage.get(STORAGE_PREFIX + "_teams");
        if (null != value) {
            _teams = value;
        }

        value = $.jStorage.get(STORAGE_PREFIX + "_schedule");
        if (null != value) {
            _schedule = value;
        }

        value = $.jStorage.get(STORAGE_PREFIX + "_currentJudgingGroup");
        if (null != value) {
            _currentJudgingGroup = value;
        }

        value = $.jStorage.get(STORAGE_PREFIX + "_currentCategory");
        if (null != value) {
            _currentCategory = value;
        }

        value = $.jStorage.get(STORAGE_PREFIX + "_judges");
        if (null != value) {
            _judges = value;
        }

        value = $.jStorage.get(STORAGE_PREFIX + "_currentJudge");
        if (null != value) {
            _currentJudge = value;
        }

        value = $.jStorage.get(STORAGE_PREFIX + "_allScores");
        if (null != value) {
            _allScores = value;
        }

        value = $.jStorage.get(STORAGE_PREFIX + "_teamTimeCache");
        if (null != value) {
            _teamTimeCache = value;
        }

        value = $.jStorage.get(STORAGE_PREFIX + "_currentTeam");
        if (null != value) {
            _currentTeam = value;
        }

        value = $.jStorage.get(STORAGE_PREFIX + "_scoreEntryBackPage");
        if (null != value) {
            _scoreEntryBackPage = value;
        }

        value = $.jStorage.get(STORAGE_PREFIX + "_categoryColumnMapping");
        if (null != value) {
            _categoryColumnMapping = value;
        }
    }

    function _save() {
        $.jStorage.set(STORAGE_PREFIX + "_subjectiveCategories",
            _subjectiveCategories);
        $.jStorage.set(STORAGE_PREFIX + "_nonNumericCategories",
            _nonNumericCategories);
        $.jStorage.set(STORAGE_PREFIX + "_tournament", _tournament);
        $.jStorage.set(STORAGE_PREFIX + "_teams", _teams);
        $.jStorage.set(STORAGE_PREFIX + "_schedule", _schedule);
        $.jStorage.set(STORAGE_PREFIX + "_currentJudgingGroup",
            _currentJudgingGroup);
        $.jStorage.set(STORAGE_PREFIX + "_currentCategory", _currentCategory);
        $.jStorage.set(STORAGE_PREFIX + "_judges", _judges);
        $.jStorage.set(STORAGE_PREFIX + "_currentJudge", _currentJudge);
        $.jStorage.set(STORAGE_PREFIX + "_allScores", _allScores);
        $.jStorage.set(STORAGE_PREFIX + "_teamTimeCache", _teamTimeCache);
        $.jStorage.set(STORAGE_PREFIX + "_currentTeam", _currentTeam);
        $.jStorage.set(STORAGE_PREFIX + "_scoreEntryBackPage", _scoreEntryBackPage);
        $.jStorage.set(STORAGE_PREFIX + "_categoryColumnMapping",
            _categoryColumnMapping);

    }

    function _log(str) {
        if (typeof (console) != 'undefined') {
            console.log(str);
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

    function _loadTournament() {
        _tournament = null;

        return $.getJSON("../api/Tournaments/current", function(tournament) {
            _tournament = tournament;
        });
    }

    function _loadSchedule() {
        _schedule = null;

        return $.getJSON("../api/Schedule", function(data) {
            if (null != data && data != "") {
                _schedule = data;
            }
        });
    }

    function _loadJudges() {
        _judges = [];

        return $.getJSON("../api/Judges", function(data) {
            $.each(data, function(i, judge) {
                _judges.push(judge);
            });
        });
    }

    function _loadTeams() {
        _teams = {};

        return $.getJSON("../api/TournamentTeams", function(teams) {
            $.each(teams, function(i, team) {
                _teams[team.teamNumber] = team;
            });
        });
    }

    function _loadAllScores() {
        _allScores = {};

        return $.getJSON("../api/SubjectiveScores", function(data) {
            _allScores = data;
        });
    }

    /**
     * Load the subjective categories.
     * 
     * @returns the promise for the ajax query
     */
    function _loadSubjectiveCategories() {
        _subjectiveCategories = {};

        return $.getJSON("../api/ChallengeDescription/SubjectiveCategories",
            function(subjectiveCategories) {
                $.each(subjectiveCategories, function(i, scoreCategory) {
                    _subjectiveCategories[scoreCategory.name] = scoreCategory;
                });
            });
    }

    function loadNonNumericCategories() {
        _nonNumericCategories = {};

        return $.getJSON("/api/ChallengeDescription/NonNumericCategories", function(
            categories) {
            $.each(categories, function(i, category) {
                _nonNumericCategories[category.title] = category;
            });
        });
    }


    function _loadCategoryColumnMapping() {
        _categoryColumnMapping = {};
        return $.getJSON("../api/CategoryScheduleMapping", function(data) {
            _categoryColumnMapping = data;
        });
    }

    /**
     * @param javaTime java time string from Jackson converting LocalTime to string without writing as timestamps
     * @return javascript Date object
     */
    function _javaLocalTimeToJsDate(javaTime) {
        const jsLocalTime = JSJoda.LocalTime.parse(javaTime);
        return jsLocalTime;
    }

    // //////////////////////// PUBLIC INTERFACE /////////////////////////
    $.subjective = {

        /**
         * Clear all data from local storage.
         */
        clearAllData: function() {
            _clear_local_storage();
            _init_variables();
        },

        /**
         * Load all data from server.
         * 
         * @param doneCallback
         *          called with no arguments on success
         * @param failCallback
         *          called with no arguments on failure
         * @param clearVariables
         *          if true, clear all variables, false to just load data from
         *          server
         */
        loadFromServer: function(doneCallback, failCallback, clearVariables) {
            if (clearVariables) {
                _init_variables();
            }

            _log("Loading from server");

            var waitList = []

            var subjectiveCategoriesPromise = _loadSubjectiveCategories();
            subjectiveCategoriesPromise.fail(function() {
                failCallback("Subjective Categories");
            });
            waitList.push(subjectiveCategoriesPromise);

            var nonNumericCategoriesPromise = loadNonNumericCategories();
            nonNumericCategoriesPromise.fail(function() {
                failCallback("NonNumeric Categories");
            });
            waitList.push(nonNumericCategoriesPromise);

            var tournamentPromise = _loadTournament();
            tournamentPromise.fail(function() {
                failCallback("Tournament");
            });
            waitList.push(tournamentPromise);

            var teamsPromise = _loadTeams();
            teamsPromise.fail(function() {
                failCallback("Teams");
            });
            waitList.push(teamsPromise);

            var schedulePromise = _loadSchedule();
            schedulePromise.fail(function() {
                failCallback("Schedule");
            });
            waitList.push(schedulePromise);

            var judgesPromise = _loadJudges();
            judgesPromise.fail(function() {
                failCallback("Judges Categories");
            });
            waitList.push(judgesPromise);

            var allScoresPromise = _loadAllScores();
            allScoresPromise.fail(function() {
                failCallback("All Scores");
            });
            waitList.push(allScoresPromise);

            var categoryMappingPromise = _loadCategoryColumnMapping();
            categoryMappingPromise.fail(function() {
                failCallback("Category Mapping");
            });
            waitList.push(categoryMappingPromise);

            $.when.apply($, waitList).done(function() {
                _save();
                doneCallback();
            });
        },

        /**
         * @return list of subjective categories
         */
        getSubjectiveCategories: function() {
            var retval = [];
            $.each(_subjectiveCategories, function(i, val) {
                retval.push(val);
            });
            return retval;
        },

        /**
         * @param title the title of the category to find
         * @return category with the specified title
         */
        getNonNumericCategory: function(title) {
            return _nonNumericCategories[title];
        },

        /**
         * @return list of teams
         */
        getTeams: function() {
            var retval = [];
            $.each(_teams, function(i, val) {
                retval.push(val);
            });
            return retval;
        },

        /**
         * Find a team by number.
         */
        lookupTeam: function(teamNum) {
            return _teams[teamNum];
        },

        log: function(str) {
            _log(str);
        },

        /**
         * @return current stored tournament
         */
        getTournament: function() {
            return _tournament;
        },

        /**
         * Get the current tournament from the server.
         * 
         * @param doneCallback
         *          called with the server tournament as the argument
         * @param failCallback
         *          called with no arguments
         */
        getServerTournament: function(doneCallback, failCallback) {
            return $.getJSON("../api/Tournaments/current", function(tournament) {
                doneCallback(tournament);
            }).fail(function() {
                failCallback();
            });
        },

        /**
         * @return true if stored data exists
         */
        storedDataExists: function() {
            if (null == _subjectiveCategories) {
                return false;
            } else if (_subjectiveCategories.length == 0) {
                return false;
            } else {
                return true;
            }
        },

        /**
         * @return list of judging groups
         */
        getJudgingGroups: function() {
            var retval = [];
            $.each(_teams, function(index, team) {
                if (-1 == $.inArray(team.judgingGroup, retval)) {
                    retval.push(team.judgingGroup);
                }
            });
            return retval;
        },

        /**
         * @return current judging group, may be null
         */
        getCurrentJudgingGroup: function() {
            return _currentJudgingGroup;
        },

        setCurrentJudgingGroup: function(v) {
            _currentJudgingGroup = v;
            _save();
        },

        /**
         * @return current category, may be null
         */
        getCurrentCategory: function() {
            return _currentCategory;
        },

        setCurrentCategory: function(v) {
            _currentCategory = v;

            _teamTimeCache = {};

            _save();
        },

        /**
         * The judges that are judging at current station and current category.
         */
        getPossibleJudges: function() {
            const retval = [];
            if (null == _judges) {
                return retval;
            } else {
                $.each(_judges, function(index, judge) {
                    if (judge.group == _currentJudgingGroup
                        && judge.category == _currentCategory.name) {
                        retval.push(judge);
                    }
                });
                return retval;
            }
        },

        setCurrentJudge: function(judgeId) {
            var foundJudge = null;
            if (null != _judges) {
                $.each(_judges,
                    function(index, judge) {
                        if (judge.group == _currentJudgingGroup
                            && judge.category == _currentCategory.name
                            && judge.id == judgeId) {
                            foundJudge = judge;
                        }
                    });
            }
            if (null == foundJudge) {
                foundJudge = new Object();
                foundJudge.id = judgeId;
                foundJudge.category = _currentCategory.name;
                foundJudge.group = _currentJudgingGroup;
                _judges.push(foundJudge);
            }

            _currentJudge = foundJudge;
            _save();
        },

        getCurrentJudge: function() {
            return _currentJudge;
        },

        addJudge: function(judgeID) {
            var foundJudge = null;
            if (null != _judges) {
                $.each(_judges,
                    function(index, judge) {
                        if (judge.group == _currentJudgingGroup
                            && judge.category == _currentCategory.name
                            && judge.id == judgeID) {
                            foundJudge = judge;
                        }
                    });
            }
            if (null == foundJudge) {
                var judge = new Object();
                judge.id = judgeID;
                judge.category = _currentCategory.name;
                judge.group = _currentJudgingGroup;
            }
            _judges.push(judge);
            _save();
        },

        /**
         * @return false if the score is null or score.deleted is true
         */
        isScoreCompleted: function(score) {
            if (null == score) {
                return false;
            } else if (score.deleted) {
                return false;
            } else {
                return true;
            }
        },

        /**
         * Get the teams that the current judge should see.
         * 
         * @return list of teams sorted by completed, then scheduled time
         */
        getCurrentTeams: function() {
            var retval = [];
            $.each(_teams, function(index, team) {
                if (team.judgingGroup == _currentJudgingGroup) {
                    retval.push(team);
                }
            });

            retval.sort(function(a, b) {
                var scoreA = $.subjective.getScore(a.teamNumber);
                var scoreB = $.subjective.getScore(b.teamNumber);
                if (!$.subjective.isScoreCompleted(scoreA)
                    && $.subjective.isScoreCompleted(scoreB)) {
                    return -1;
                } else if ($.subjective.isScoreCompleted(scoreA)
                    && !$.subjective.isScoreCompleted(scoreB)) {
                    return 1;
                } else {
                    var timeA = $.subjective.getScheduledTime(a.teamNumber);
                    var timeB = $.subjective.getScheduledTime(b.teamNumber);
                    if (null == timeA && null == timeB) {
                        return 0;
                    } else if (null == timeA) {
                        return -1;
                    } else if (null == timeB) {
                        return 1;
                    } else {
                        return timeA.compareTo(timeB);
                    }
                }
            });
            return retval;
        },

        /**
         * Get schedule info object for the specified team.
         * 
         * @param teamNumber
         *          the team number
         * @return the object or null if not found
         */
        getSchedInfoForTeam: function(teamNumber) {
            if (null == _schedule) {
                return null;
            } else {
                var schedInfo = null;
                $.each(_schedule.schedule, function(index, value) {
                    if (value.teamNumber == teamNumber) {
                        schedInfo = value;
                    }
                });
                return schedInfo;
            }
        },

        /**
         * Get the score for the specified team for the current category.
         * 
         * @param teamNumber
         *          number of the team to find
         * @return the score object or null if not yet scored
         */
        getScore: function(teamNumber) {
            var categoryScores = _allScores[_currentCategory.name];
            if (null == categoryScores) {
                return null;
            }

            var judgeScores = categoryScores[_currentJudge.id];
            if (null == judgeScores) {
                return null;
            }

            return judgeScores[teamNumber];
        },

        saveScore: function(score) {
            if (score.deleted && !score.scoreOnServer) {
                // don't bother saving scores not on the server that are deleted
                // This should keep one judge from deleting another judge's
                // scores
                $.subjective.log("Ignoring deleted score not on the server");
                return;
            }

            var categoryScores = _allScores[_currentCategory.name];
            if (null == categoryScores) {
                categoryScores = {}
                _allScores[_currentCategory.name] = categoryScores;
            }

            var judgeScores = categoryScores[_currentJudge.id];
            if (null == judgeScores) {
                judgeScores = {}
                categoryScores[_currentJudge.id] = judgeScores;
            }

            judgeScores[score.teamNumber] = score;
            _save();
        },

        /**
         * Compute the score. Assumes the score is for the current category.
         * 
         * @param score
         *          a score object from getScore
         * @return a number
         */
        computeScore: function(score) {
            var retval = 0;

            if (!score.deleted && !score.noShow) {
                $.each(_currentCategory.allGoals, function(index, goal) {
                    if (goal.enumerated) {
                        alert("Enumerated goals are not yet supported");
                    } else {
                        var rawScore = Number(score.standardSubScores[goal.name]);
                        var multiplier = Number(goal.multiplier);
                        var subscore = rawScore * multiplier;

                        retval = retval + subscore;
                    }
                });
            }

            return retval;
        },

        /**
         * Get the schedule column for the specified category name.
         */
        getScheduleColumnForCategory: function(categoryName) {
            var column = null;

            $.each(_categoryColumnMapping, function(index, mapping) {
                if (mapping.categoryName == categoryName) {
                    column = mapping.scheduleColumn;
                }
            });

            return column;
        },

        /**
         * Get the scheduled time for the specified team for the current category.
         * If there is no schedule, this returns JSJoda.LocalTime.of().
         * 
         * @param teamNumber
         *          number of the team to find
         * @return LocalTime or null if there is no schedule information for the team
         */
        getScheduledTime: function(teamNumber) {
            const cachedDate = _teamTimeCache[teamNumber];
            if (null != cachedDate) {
                return cachedDate;
            }

            var retval;
            var schedInfo = $.subjective.getSchedInfoForTeam(teamNumber);
            if (null == schedInfo) {
                _log("No schedinfo for " + teamNumber);
                retval = null;
            } else {
                var time = null;
                var column = $.subjective
                    .getScheduleColumnForCategory(_currentCategory.name);
                $.each(schedInfo.subjectiveTimes, function(index, value) {
                    if (value.name == column) {
                        time = value.time;
                    }
                });
                if (null == time) {
                    _log("No time found for " + teamNumber);
                    retval = JSJoda.LocalTime.of();
                } else {
                    retval = _javaLocalTimeToJsDate(time);
                    _teamTimeCache[teamNumber] = retval;
                }
            }

            _teamTimeCache[teamNumber] = retval;
            _save();

            return retval;
        },

        setCurrentTeam: function(team) {
            _currentTeam = team;

            _save();
        },

        getCurrentTeam: function() {
            return _currentTeam;
        },

        /**
         * Upload scores to the server calling the appropriate callback.
         * 
         * @param doneCallback
         *          called with a SubjectiveScoresServlet.UploadResult object on
         *          success
         * @param failCallback
         *          called with a SubjectiveScoresServlet.UploadResult object on
         *          failure, may be null
         * @return the promise from the AJAX function
         */
        uploadScores: function(doneCallback, failCallback) {
            var dataToUpload = $.toJSON(_allScores);
            return $.ajax({
                type: "POST",
                dataType: "json",
                contentType: "application/json",
                url: "../api/SubjectiveScores",
                data: dataToUpload,
                success: function(result) {
                    if (result.success) {
                        doneCallback(result);
                    } else {
                        failCallback(result);
                    }
                }
            }).fail(function(result) {
                failCallback(result);
            });
        },

        /**
         * Upload judges to the server calling the appropriate callback.
         * 
         * @param doneCallback
         *          called with a JudgesServlet.UploadResult object on success
         * @param failCallback
         *          called with a JudgesServlet.UploadResult object on failure, may
         *          be null
         * @return the promise from the AJAX function
         */
        uploadJudges: function(doneCallback, failCallback) {
            var dataToUpload = $.toJSON(_judges);
            return $.ajax({
                type: "POST",
                dataType: "json",
                contentType: "application/json",
                url: "../api/Judges",
                data: dataToUpload,
                success: function(result) {
                    $.subjective.log("uploading judges success");
                    if (result.success) {
                        doneCallback(result);
                    } else {
                        failCallback(result);
                    }
                }
            }).fail(function(result) {
                failCallback(result);
            });
        },

        /**
         * Upload all data to the server
         * 
         * @param scoresSuccess
         *          called with a SubjectiveScoresServlet.UploadResult object on
         *          successful upload of scores
         * @param scoresFail
         *          called with a SubjectiveScoresServlet.UploadResult object on
         *          failed upload of scores
         * @param judgesSuccess
         *          called with a JudgesServlet.UploadResult object on successful
         *          upload of judges
         * @param judgesFail
         *          called with a JudgesServlet.UploadResult object on failed upload
         *          of judges
         * @param loadSuccess
         *          called after all uploads and successful load of data (no
         *          argument)
         * @param loadFail
         *          called after all uploads and failed load of data (string
         *          message)
         */
        uploadData: function(scoresSuccess, scoresFail, judgesSuccess, judgesFail,
            loadSuccess, loadFail) {

            $
                .getJSON(
                    "CheckAuth",
                    function(data) {
                        if (data.authenticated) {
                            $.subjective
                                .getServerTournament(function(serverTournament) {
                                    var storedTournament = $.subjective.getTournament();
                                    if (null == storedTournament) {
                                        loadFail("Internal error, no saved tournament");
                                    } else if (storedTournament.name != serverTournament.name
                                        || storedTournament.tournamentID != serverTournament.tournamentID) {
                                        loadFail("Tournament mismatch local: "
                                            + storedTournament.name + "("
                                            + storedTournament.tournamentID + ")"
                                            + " server: " + serverTournament.name + "("
                                            + serverTournament.tournamentID + ")");
                                    } else {
                                        var waitList = []
                                        waitList.push($.subjective.uploadScores(
                                            scoresSuccess, scoresFail));
                                        waitList.push($.subjective.uploadJudges(
                                            judgesSuccess, judgesFail));

                                        $.when.apply($, waitList).done(function() {
                                            $.subjective.loadFromServer(function() {
                                                loadSuccess();
                                            }, function() {
                                                loadFail("Error getting updated scores");
                                            }, false);
                                        });
                                    }
                                });
                        } else {
                            location.href = "Auth";
                        }
                    });
        },

        /**
         * @return true if there are modified scores in the list
         */
        checkForModifiedScores: function() {
            var modified = false;
            $.each(_allScores, function(category, categoryScores) {
                $.each(categoryScores, function(judge, judgeScores) {
                    $.each(judgeScores, function(teamNumber, score) {
                        if (score.modified) {
                            modified = true;
                        }
                    });
                });
            });
            return modified;
        },

        setScoreEntryBackPage: function(page) {
            _scoreEntryBackPage = page;
            _save();
        },
        getScoreEntryBackPage: function() {
            return _scoreEntryBackPage;
        },

    };

    // ///// Load data //////////
    _loadFromDisk();
})(window.jQuery || window.$);
