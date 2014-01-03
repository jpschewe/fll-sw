/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
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

	var STORAGE_PREFIX = "fll.subjective.";

	// //////////////////////// PRIVATE INTERFACE ////////////////////////

	var _subjectiveCategories;
	var _tournament;
	var _teams;
	var _schedule;
	var _currentJudgingGroup;
	var _currentCategory;
	var _judges;
	var _currentJudge;
	var _currentJudgePhone;
	var _allScores;
	var _teamTimeCache;
	var _currentTeam;

	function _init_variables() {
		_subjectiveCategories = {};
		_tournament = null;
		_teams = {};
		_schedule = null;
		_currentJudgingGroup = null;
		_currentCategory = null;
		_judges = [];
		_currentJudge = null;
		_currentJudgePhone = null;
		_allScores = {};
		_teamTimeCache = {};
		_currentTeam = null;
	}

	function _loadFromDisk() {
		_init_variables();

		_log("Loading from disk");

		var value = $.jStorage.get(STORAGE_PREFIX + "_subjectiveCategories");
		if (null != value) {
			_subjectiveCategories = value;
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

		value = $.jStorage.get(STORAGE_PREFIX + "_currentJudgePhone");
		if (null != value) {
			_currentJudgePhone = value;
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

	}

	function _save() {
		_log("Saving data to disk");

		$.jStorage.set(STORAGE_PREFIX + "_subjectiveCategories",
				_subjectiveCategories);
		$.jStorage.set(STORAGE_PREFIX + "_tournament", _tournament);
		$.jStorage.set(STORAGE_PREFIX + "_teams", _teams);
		$.jStorage.set(STORAGE_PREFIX + "_schedule", _schedule);
		$.jStorage.set(STORAGE_PREFIX + "_currentJudgingGroup",
				_currentJudgingGroup);
		$.jStorage.set(STORAGE_PREFIX + "_currentCategory", _currentCategory);
		$.jStorage.set(STORAGE_PREFIX + "_judges", _judges);
		$.jStorage.set(STORAGE_PREFIX + "_currentJudge", _currentJudge);
		$.jStorage.set(STORAGE_PREFIX + "_currentJudgePhone",
				_currentJudgePhone);
		$.jStorage.set(STORAGE_PREFIX + "_allScores", _allScores);
		$.jStorage.set(STORAGE_PREFIX + "_teamTimeCache", _teamTimeCache);
		$.jStorage.set(STORAGE_PREFIX + "_currentTeam", _currentTeam);

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
			_schedule = data;
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

		return $
				.getJSON(
						"../api/ChallengeDescription/SubjectiveCategories",
						function(subjectiveCategories) {
							$
									.each(
											subjectiveCategories,
											function(i, scoreCategory) {
												_subjectiveCategories[scoreCategory.name] = scoreCategory;
											});
						});
	}

	// //////////////////////// PUBLIC INTERFACE /////////////////////////
	$.subjective = {

		/**
		 * Clear all data from local storage.
		 */
		clearAllData : function() {
			_clear_local_storage();
			_init_variables();
		},

		/**
		 * @param doneCallback
		 *            called with no arguments on success
		 * @param failCallback
		 *            called with no arguments on failure
		 */
		loadFromServer : function(doneCallback, failCallback) {
			_init_variables();

			_log("Loading from server");

			var waitList = []
			waitList.push(_loadSubjectiveCategories());
			waitList.push(_loadTournament());
			waitList.push(_loadTeams());
			waitList.push(_loadSchedule());
			waitList.push(_loadJudges());
			waitList.push(_loadAllScores());

			$.when.apply($, waitList).done(function() {
				_save();
				doneCallback();
			}).fail(function() {
				failCallback();
			});
		},

		/**
		 * @return list of subjective categories
		 */
		getSubjectiveCategories : function() {
			var retval = [];
			$.each(_subjectiveCategories, function(i, val) {
				retval.push(val);
			});
			return retval;
		},

		/**
		 * @return list of teams
		 */
		getTeams : function() {
			var retval = [];
			$.each(_teams, function(i, val) {
				retval.push(val);
			});
			return retval;
		},

		/**
		 * Find a team by number.
		 */
		lookupTeam : function(teamNum) {
			return _teams[teamNum];
		},

		log : function(str) {
			_log(str);
		},

		/**
		 * @return current stored tournament
		 */
		getTournament : function() {
			return _tournament;
		},

		/**
		 * Get the current tournament from the server.
		 * 
		 * @param doneCallback
		 *            called with the server tournament as the argument
		 * @param failCallback
		 *            called with no arguments
		 */
		getServerTournament : function(doneCallback, failCallback) {
			return $.getJSON("../api/Tournaments/current",
					function(tournament) {
						doneCallback(tournament);
					}).fail(function() {
				failCallback();
			});
		},

		/**
		 * @return true if stored data exists
		 */
		storedDataExists : function() {
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
		getJudgingGroups : function() {
			return _schedule.judgingGroups;
		},

		/**
		 * @return current judging group, may be null
		 */
		getCurrentJudgingGroup : function() {
			return _currentJudgingGroup;
		},

		setCurrentJudgingGroup : function(v) {
			_currentJudgingGroup = v;
			_save();
		},

		/**
		 * @return current category, may be null
		 */
		getCurrentCategory : function() {
			return _currentCategory;
		},

		setCurrentCategory : function(v) {
			_currentCategory = v;

			_teamTimeCache = {};

			_save();
		},

		/**
		 * The judges that are judging at current station and current category.
		 */
		getPossibleJudges : function() {
			retval = [];
			if (null == _judges) {
				return retval;
			} else {
				$.each(_judges, function(index, judge) {
					if (judge.station == _currentJudgingGroup
							&& judge.category == _currentCategory.name) {
						retval.push(judge);
					}
				});
				return retval;
			}
		},

		setCurrentJudge : function(judgeId, judgePhone) {
			var foundJudge = null;
			$.each(_judges, function(index, judge) {
				if (judge.station == _currentJudgingGroup
						&& judge.category == _currentCategory.name
						&& judge.id == judgeId) {
					foundJudge = judge;
				}
			});
			if (null == foundJudge) {
				foundJudge = new Object();
				foundJudge.id = judgeId;
				foundJudge.category = _currentCategory.name;
				foundJudge.station = _currentJudgingGroup;
				_judges.push(foundJudge);
			}

			_currentJudge = foundJudge;
			_currentJudgePhone = judgePhone;
			_save();
		},

		getCurrentJudge : function() {
			return _currentJudge;
		},

		getCurrentJudgePhone : function() {
			return _currentJudgePhone;
		},

		/**
		 * @return false if the score is null or score.deleted is true
		 */
		isScoreCompleted : function(score) {
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
		getCurrentTeams : function() {
			var retval = [];
			$.each(_teams, function(index, team) {
				if (team.judgingStation == _currentJudgingGroup) {
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
					return timeA < timeB ? -1 : timeA > timeB ? 1 : 0;
				}
			});
			return retval;
		},

		/**
		 * Get schedule info object for the specified team.
		 * 
		 * @param teamNumber
		 *            the team number
		 * @return the object or null if not found
		 */
		getSchedInfoForTeam : function(teamNumber) {
			if (null == _schedule) {
				_log("No schedule");
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
		 *            number of the team to find
		 * @return the score object or null if not yet scored
		 */
		getScore : function(teamNumber) {
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

		saveScore : function(score) {
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
		 *            a score object from getScore
		 * @return a number
		 */
		computeScore : function(score) {
			var retval = 0;

			if (!score.deleted && !score.noShow) {
				$
						.each(
								_currentCategory.goals,
								function(index, goal) {
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
		 * Get the scheduled time for the specified team for the current
		 * category. If there is no schedule, this returns the Date(0).
		 * 
		 * @param teamNumber
		 *            number of the team to find
		 * @return Date
		 */
		getScheduledTime : function(teamNumber) {
			var cachedDate = _teamTimeCache[teamNumber];
			if (null != cachedDate) {
				return new Date(cachedDate);
			}

			var retval;
			var schedInfo = $.subjective.getSchedInfoForTeam(teamNumber);
			if (null == schedInfo) {
				_log("No schedinfo for " + teamNumber);
				retval = new Date(0);
			} else {
				var timeStr = null;
				$
						.each(
								schedInfo.subjectiveTimes,
								function(index, value) {
									// FIXME need a non-hardcoded mapping
									// between schedule
									// stations and category names
									var found = false;
									if ("Project" == value.name) {
										if ("project" == _currentCategory.name) {
											found = true;
										}
									} else if ("Design" == value.name) {
										if ("robot_design" == _currentCategory.name) {
											found = true;
										} else if ("robot_programming" == _currentCategory.name) {
											found = true;
										}
									} else if ("Core Values" == value.name) {
										if ("core_values" == _currentCategory.name) {
											found = true;
										}
									}
									if (found) {
										timeStr = value.time;
									}
								});
				if (null == timeStr) {
					_log("No time found for " + teamNumber);
					retval = new Date(0);
				} else {
					_teamTimeCache[teamNumber] = retval = new Date(
							Number(timeStr));
				}
			}

			_teamTimeCache[teamNumber] = retval;
			_save();

			return retval;
		},

		setCurrentTeam : function(team) {
			_currentTeam = team;

			_save();
		},

		getCurrentTeam : function() {
			return _currentTeam;
		},

	};

	// ///// Load data //////////
	_loadFromDisk();
})(window.jQuery || window.$);
