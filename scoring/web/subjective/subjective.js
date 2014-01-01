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
		$.jStorage.set(STORAGE_PREFIX + "_currentJudgePhone", _currentJudgePhone);

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

	};

	// ///// Load data //////////
	_loadFromDisk();
})(window.jQuery || window.$);
