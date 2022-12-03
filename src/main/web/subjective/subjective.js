/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

const subjective_module = {}

{
    if (typeof fllStorage != 'object') {
        throw new Error("fllStorage needs to be loaded!");
    }

    const STORAGE_PREFIX = "fll.subjective.";

    // //////////////////////// PRIVATE INTERFACE ////////////////////////

    let _subjectiveCategories; // category.name -> category
    let _nonNumericCategories; // category.title -> category
    let _tournament;
    let _teams;
    let _schedule;
    let _currentJudgingGroup;
    let _currentCategory;
    let _judges;
    let _currentJudge;
    let _allScores;
    let _teamTimeCache;
    let _currentTeam;
    let _scoreEntryBackPage;
    let _categoryColumnMapping;

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

        let value = fllStorage.get(STORAGE_PREFIX, "_subjectiveCategories");
        if (null != value) {
            _subjectiveCategories = value;
        }

        value = fllStorage.get(STORAGE_PREFIX, "_nonNumericCategories");
        if (null != value) {
            _nonNumericCategories = value;
        }

        value = fllStorage.get(STORAGE_PREFIX, "_tournament");
        if (null != value) {
            _tournament = value;
        }

        value = fllStorage.get(STORAGE_PREFIX, "_teams");
        if (null != value) {
            _teams = value;
        }

        value = fllStorage.get(STORAGE_PREFIX, "_schedule");
        if (null != value) {
            _schedule = value;
        }

        value = fllStorage.get(STORAGE_PREFIX, "_currentJudgingGroup");
        if (null != value) {
            _currentJudgingGroup = value;
        }

        value = fllStorage.get(STORAGE_PREFIX, "_currentCategory");
        if (null != value) {
            _currentCategory = value;
        }

        value = fllStorage.get(STORAGE_PREFIX, "_judges");
        if (null != value) {
            _judges = value;
        }

        value = fllStorage.get(STORAGE_PREFIX, "_currentJudge");
        if (null != value) {
            _currentJudge = value;
        }

        value = fllStorage.get(STORAGE_PREFIX, "_allScores");
        if (null != value) {
            _allScores = value;
        }

        value = fllStorage.get(STORAGE_PREFIX, "_teamTimeCache");
        if (null != value) {
            _teamTimeCache = value;
        }

        value = fllStorage.get(STORAGE_PREFIX, "_currentTeam");
        if (null != value) {
            _currentTeam = value;
        }

        value = fllStorage.get(STORAGE_PREFIX, "_scoreEntryBackPage");
        if (null != value) {
            _scoreEntryBackPage = value;
        }

        value = fllStorage.get(STORAGE_PREFIX, "_categoryColumnMapping");
        if (null != value) {
            _categoryColumnMapping = value;
        }
    }

    function _save() {
        fllStorage.set(STORAGE_PREFIX, "_subjectiveCategories",
            _subjectiveCategories);
        fllStorage.set(STORAGE_PREFIX, "_nonNumericCategories",
            _nonNumericCategories);
        fllStorage.set(STORAGE_PREFIX, "_tournament", _tournament);
        fllStorage.set(STORAGE_PREFIX, "_teams", _teams);
        fllStorage.set(STORAGE_PREFIX, "_schedule", _schedule);
        fllStorage.set(STORAGE_PREFIX, "_currentJudgingGroup",
            _currentJudgingGroup);
        fllStorage.set(STORAGE_PREFIX, "_currentCategory", _currentCategory);
        fllStorage.set(STORAGE_PREFIX, "_judges", _judges);
        fllStorage.set(STORAGE_PREFIX, "_currentJudge", _currentJudge);
        fllStorage.set(STORAGE_PREFIX, "_allScores", _allScores);
        fllStorage.set(STORAGE_PREFIX, "_teamTimeCache", _teamTimeCache);
        fllStorage.set(STORAGE_PREFIX, "_currentTeam", _currentTeam);
        fllStorage.set(STORAGE_PREFIX, "_scoreEntryBackPage", _scoreEntryBackPage);
        fllStorage.set(STORAGE_PREFIX, "_categoryColumnMapping",
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
        fllStorage.clearNamespace(STORAGE_PREFIX);
    }

    function _loadTournament() {
        _tournament = null;

        return fetch("../api/Tournaments/current").then(checkJsonResponse).then(function(tournament) {
            _tournament = tournament;
        });
    }

    function _loadSchedule() {
        _schedule = null;

        return fetch("../api/Schedule").then(checkJsonResponse).then(function(data) {
            if (null != data && data != "") {
                _schedule = data;
            }
        });
    }

    function _loadJudges() {
        _judges = [];

        return fetch("../api/Judges").then(checkJsonResponse).then(function(data) {
            for (const judge of data) {
                _judges.push(judge);
            }
        });
    }

    function _loadTeams() {
        _teams = {};

        return fetch("../api/TournamentTeams").then(checkJsonResponse).then(function(teams) {
            for (const team of teams) {
                _teams[team.teamNumber] = team;
            }
        });
    }

    function _loadAllScores() {
        _allScores = {};

        return fetch("../api/SubjectiveScores").then(checkJsonResponse).then(function(data) {
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

        return fetch("../api/ChallengeDescription/SubjectiveCategories").then(checkJsonResponse).then(function(subjectiveCategories) {
            for (const scoreCategory of subjectiveCategories) {
                _subjectiveCategories[scoreCategory.name] = scoreCategory;
            }
        });
    }

    function loadNonNumericCategories() {
        _nonNumericCategories = {};

        return fetch("/api/ChallengeDescription/NonNumericCategories").then(checkJsonResponse).then(function(
            categories) {
            for (const category of categories) {
                _nonNumericCategories[category.title] = category;
            }
        });
    }


    function _loadCategoryColumnMapping() {
        _categoryColumnMapping = {};
        return fetch("../api/CategoryScheduleMapping").then(checkJsonResponse).then(function(data) {
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
    /**
     * Clear all data from local storage.
     */
    subjective_module.clearAllData = function() {
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
        subjective_module.loadFromServer = function(doneCallback, failCallback, clearVariables) {
            if (clearVariables) {
                _init_variables();
            }

            _log("Loading from server");

            let waitList = []

            const subjectiveCategoriesPromise = _loadSubjectiveCategories();
            subjectiveCategoriesPromise.catch(function() {
                failCallback("Subjective Categories");
            });
            waitList.push(subjectiveCategoriesPromise);

            const nonNumericCategoriesPromise = loadNonNumericCategories();
            nonNumericCategoriesPromise.catch(function() {
                failCallback("NonNumeric Categories");
            });
            waitList.push(nonNumericCategoriesPromise);

            const tournamentPromise = _loadTournament();
            tournamentPromise.catch(function() {
                failCallback("Tournament");
            });
            waitList.push(tournamentPromise);

            const teamsPromise = _loadTeams();
            teamsPromise.catch(function() {
                failCallback("Teams");
            });
            waitList.push(teamsPromise);

            const schedulePromise = _loadSchedule();
            schedulePromise.catch(function() {
                failCallback("Schedule");
            });
            waitList.push(schedulePromise);

            const judgesPromise = _loadJudges();
            judgesPromise.catch(function() {
                failCallback("Judges Categories");
            });
            waitList.push(judgesPromise);

            const allScoresPromise = _loadAllScores();
            allScoresPromise.catch(function() {
                failCallback("All Scores");
            });
            waitList.push(allScoresPromise);

            const categoryMappingPromise = _loadCategoryColumnMapping();
            categoryMappingPromise.catch(function() {
                failCallback("Category Mapping");
            });
            waitList.push(categoryMappingPromise);

            Promise.all(waitList).then(function() {
                _save();
                doneCallback();
            });
        },

        /**
         * @return list of subjective categories
         */
        subjective_module.getSubjectiveCategories = function() {
            const retval = [];
            for (const [_, category] of Object.entries(_subjectiveCategories)) {
                retval.push(category);
            }
            return retval;
        },

        /**
         * @param title the title of the category to find
         * @return category with the specified title
         */
        subjective_module.getNonNumericCategory = function(title) {
            return _nonNumericCategories[title];
        },

        /**
         * @return list of teams
         */
        subjective_module.getTeams = function() {
            let retval = [];
            for (const [_, val] of Object.entries(_teams)) {
                retval.push(val);
            }
            return retval;
        },

        /**
         * Find a team by number.
         */
        subjective_module.lookupTeam = function(teamNum) {
            return _teams[teamNum];
        },

        subjective_module.log = function(str) {
            _log(str);
        },

        /**
         * @return current stored tournament
         */
        subjective_module.getTournament = function() {
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
        subjective_module.getServerTournament = function(doneCallback, failCallback) {
            return fetch("../api/Tournaments/current").then(checkJsonResponse).then(function(tournament) {
                doneCallback(tournament);
            }).catch(function() {
                failCallback();
            });
        },

        /**
         * @return true if stored data exists
         */
        subjective_module.storedDataExists = function() {
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
        subjective_module.getJudgingGroups = function() {
            let retval = [];
            for (const [_, team] of Object.entries(_teams)) {
                if (-1 == retval.indexOf(team.judgingGroup)) {
                    retval.push(team.judgingGroup);
                }
            }
            return retval;
        },

        /**
         * @return current judging group, may be null
         */
        subjective_module.getCurrentJudgingGroup = function() {
            return _currentJudgingGroup;
        },

        subjective_module.setCurrentJudgingGroup = function(v) {
            _currentJudgingGroup = v;
            _save();
        },

        /**
         * @return current category, may be null
         */
        subjective_module.getCurrentCategory = function() {
            return _currentCategory;
        },

        subjective_module.setCurrentCategory = function(v) {
            _currentCategory = v;

            _teamTimeCache = {};

            _save();
        },

        /**
         * The judges that are judging at current station and current category.
         */
        subjective_module.getPossibleJudges = function() {
            const retval = [];
            if (null == _judges) {
                return retval;
            } else {
                for (const judge of _judges) {
                    if (judge.group == _currentJudgingGroup
                        && judge.category == _currentCategory.name) {
                        retval.push(judge);
                    }
                }
                return retval;
            }
        },

        subjective_module.setCurrentJudge = function(judgeId) {
            var foundJudge = null;
            if (null != _judges) {
                for (const judge of _judges) {
                    if (judge.group == _currentJudgingGroup
                        && judge.category == _currentCategory.name
                        && judge.id == judgeId) {
                        foundJudge = judge;
                    }
                }
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

        subjective_module.getCurrentJudge = function() {
            return _currentJudge;
        },

        subjective_module.addJudge = function(judgeID) {
            var foundJudge = null;
            if (null != _judges) {
                for (const judge of _judges) {
                    if (judge.group == _currentJudgingGroup
                        && judge.category == _currentCategory.name
                        && judge.id == judgeID) {
                        foundJudge = judge;
                    }
                }
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
        subjective_module.isScoreCompleted = function(score) {
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
        subjective_module.getCurrentTeams = function() {
            let retval = [];
            for (const [_, team] of Object.entries(_teams)) {
                if (team.judgingGroup == _currentJudgingGroup) {
                    retval.push(team);
                }
            }

            retval.sort(function(a, b) {
                const scoreA = subjective_module.getScore(a.teamNumber);
                const scoreB = subjective_module.getScore(b.teamNumber);
                if (!subjective_module.isScoreCompleted(scoreA)
                    && subjective_module.isScoreCompleted(scoreB)) {
                    return -1;
                } else if (subjective_module.isScoreCompleted(scoreA)
                    && !subjective_module.isScoreCompleted(scoreB)) {
                    return 1;
                } else {
                    const timeA = subjective_module.getScheduledTime(a.teamNumber);
                    const timeB = subjective_module.getScheduledTime(b.teamNumber);
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
        subjective_module.getSchedInfoForTeam = function(teamNumber) {
            if (null == _schedule) {
                return null;
            } else {
                var schedInfo = null;
                for (const value of _schedule.schedule) {
                    if (value.teamNumber == teamNumber) {
                        schedInfo = value;
                    }
                }
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
        subjective_module.getScore = function(teamNumber) {
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

        subjective_module.saveScore = function(score) {
            if (score.deleted && !score.scoreOnServer) {
                // don't bother saving scores not on the server that are deleted
                // This should keep one judge from deleting another judge's
                // scores
                subjective_module.log("Ignoring deleted score not on the server");
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
        subjective_module.computeScore = function(score) {
            let retval = 0;

            if (!score.deleted && !score.noShow) {
                for (const goal of _currentCategory.allGoals) {
                    if (goal.enumerated) {
                        document.getElementById('alert-dialog_text').innerText = "Enumerated goals are not yet supported";
                        document.getElementById('alert-dialog').classList.remove("fll-sw-ui-inactive");
                    } else {
                        const rawScore = Number(score.standardSubScores[goal.name]);
                        const multiplier = Number(goal.multiplier);
                        const subscore = rawScore * multiplier;

                        retval = retval + subscore;
                    }
                }
            }

            return retval;
        },

        /**
         * Get the schedule column for the specified category name.
         */
        subjective_module.getScheduleColumnForCategory = function(categoryName) {
            let column = null;

            for (const mapping of _categoryColumnMapping) {
                if (mapping.categoryName == categoryName) {
                    column = mapping.scheduleColumn;
                }
            }

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
        subjective_module.getScheduledTime = function(teamNumber) {
            const cachedDate = _teamTimeCache[teamNumber];
            if (null != cachedDate) {
                return cachedDate;
            }

            let retval;
            const schedInfo = subjective_module.getSchedInfoForTeam(teamNumber);
            if (null == schedInfo) {
                _log("No schedinfo for " + teamNumber);
                retval = null;
            } else {
                let time = null;
                const column = subjective_module
                    .getScheduleColumnForCategory(_currentCategory.name);
                for (const value of schedInfo.subjectiveTimes) {
                    if (value.name == column) {
                        time = value.time;
                    }
                }
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

        subjective_module.setCurrentTeam = function(team) {
            _currentTeam = team;

            _save();
        },

        subjective_module.getCurrentTeam = function() {
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
        subjective_module.uploadScores = function(doneCallback, failCallback) {
            const dataToUpload = JSON.stringify(_allScores);
            return fetch("../api/SubjectiveScores", {
                method: "POST",
                headers: { 'Content-Type': 'application/json' },
                body: dataToUpload
            }).then(checkJsonResponse).then(function(result) {
                if (result.success) {
                    doneCallback(result);
                } else {
                    failCallback(result);
                }
            }).catch(function(result) {
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
        subjective_module.uploadJudges = function(doneCallback, failCallback) {
            const dataToUpload = JSON.stringify(_judges);
            return fetch("../api/Judges", {
                method: "POST",
                headers: { 'Content-Type': 'application/json' },
                body: dataToUpload
            }).then(checkJsonResponse).then(function(result) {
                subjective_module.log("uploading judges success");
                if (result.success) {
                    doneCallback(result);
                } else {
                    failCallback(result);
                }
            }).catch(function(result) {
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
        subjective_module.uploadData = function(scoresSuccess, scoresFail, judgesSuccess, judgesFail,
            loadSuccess, loadFail) {

            subjective_module.checkServerStatus(function() {
                fetch("CheckAuth").then(checkJsonResponse).then(function(data) {
                    if (data.authenticated) {
                        subjective_module
                            .getServerTournament(function(serverTournament) {
                                const storedTournament = subjective_module.getTournament();
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
                                    const waitList = []
                                    waitList.push(subjective_module.uploadScores(
                                        scoresSuccess, scoresFail));
                                    waitList.push(subjective_module.uploadJudges(
                                        judgesSuccess, judgesFail));

                                    Promise.all(waitList).then(function() {
                                        subjective_module.loadFromServer(function() {
                                            loadSuccess();
                                        }, function() {
                                            loadFail("Error getting updated scores");
                                        }, false);
                                    });
                                }
                            });
                    } else {
                        alertCallback = function() {
                            // hide the spinning animation
                            loadSuccess();

                            window.open('../login.jsp', '_login');
                        }
                        document.getElementById("alert-dialog_text").innerText = "Your device has been logged out. A new window will be opened to the login page. Once you have logged in, close that window and synchronize again.";
                        document.getElementById("alert-dialog").classList.remove("fll-sw-ui-inactive");
                    }
                });
            }, // server online
                function() {
                    loadFail("Server is offline, please connect to the network before synchronizing");
                }); // server offline
        },

        /**
         * @return true if there are modified scores in the list
         */
        subjective_module.checkForModifiedScores = function() {
            let modified = false;
            for (const [_, categoryScores] of Object.entries(_allScores)) {
                for (const [_, judgeScores] of Object.entries(categoryScores)) {
                    for (const [_, score] of Object.entries(judgeScores)) {
                        if (score.modified) {
                            modified = true;
                        }
                    }
                }
            }
            return modified;
        },

        subjective_module.setScoreEntryBackPage = function(page) {
            _scoreEntryBackPage = page;
            _save();
        },
        subjective_module.getScoreEntryBackPage = function() {
            return _scoreEntryBackPage;
        },

        /**
         * Check if the server is reachable.
         *
         * @param onlineCallback function to execute if the server is reachable
         * @param offlineCallback function to execute if the server is not reachable 
         */
        subjective_module.checkServerStatus = function(onlineCallback, offlineCallback) {
            subjective_module.log("Checking server status");

            const FETCH_TIMEOUT = 35000; // milliseconds, need 35 seconds for chromebooks (not sure why)            
            const controller = new AbortController();
            const timeout = setTimeout(function() {
                controller.abort('Request timed out');
            }, FETCH_TIMEOUT);

            const request = new Request("../images/blank.gif", {
                headers: new Headers({
                    'pragma': 'no-cache',
                    'cache-control': 'no-cache'
                }),
                cache: "no-store",
                signal: controller.signal,
                method: 'HEAD'
            });

            subjective_module.log("Executing fetch: " + request);
            fetch(request)
                .then(function(response) {
                    subjective_module.log("Response received");
                    
                    // Clear the timeout as cleanup
                    clearTimeout(timeout);
                    if (!response.ok) {
                        throw new Error(`HTTP error: ${response.status}`);
                    } else {
                        subjective_module.log('fetch good! ' + response);
                        subjective_module.log("server online");
                        onlineCallback();
                    }
                    // no need to resolve the response object as we know the server is online, we don't need the data in the response
                })
                .catch(function(err) {
                    subjective_module.log('Server offline (fetch error): ' + err);

                    // Error: response error, request timeout or runtime error
                    subjective_module.log("server offline: " + err);
                    offlineCallback();
                });
        },

        /**
         * Create a javascript object that contains the information needed for 
         * an offline download that can be imported when the synchronize functionality
         * isn't working.
         * Note that the object returned has references to internal structures.
         */
        subjective_module.getOfflineDownloadObject = function() {
            var toDownload = {};
            toDownload['scores'] = _allScores;
            toDownload['judges'] = _judges;
            return toDownload;
        },



        // ///// Load data //////////
        _loadFromDisk();
}
