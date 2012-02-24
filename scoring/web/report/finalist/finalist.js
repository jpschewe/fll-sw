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
	var _teams = {};
	var _categories = {};
	var _tournament = null;
	var _divisions = [];
	var _currentDivisionIndex = null;

	/**
	 * Save the current state to local storage.
	 */
	function _save() {
		$.jStorage.set(STORAGE_PREFIX + "_teams", _teams);
		$.jStorage.set(STORAGE_PREFIX + "_categories", _categories);
		$.jStorage.set(STORAGE_PREFIX + "_tournament", _tournament);
		$.jStorage.set(STORAGE_PREFIX + "_divisions", _divisions);
		$.jStorage.set(STORAGE_PREFIX + "_currentDivisionIndex",
				_currentDivisionIndex);
	}

	/**
	 * Load the current state from local storage.
	 */
	function _load() {
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
		value = $.jStorage.get(STORAGE_PREFIX + "_currentDivisionIndex");
		if (null != value) {
			_currentDivisionIndex = value;
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
	function Team(num, division, name, org) {
		if (typeof (_teams[num]) != 'undefined') {
			throw "Team already exists with number: " + num;
		}

		this.num = num;
		this.division = division;
		this.name = name;
		this.org = org;
		_teams[num] = this;
		_save();
	}

	/**
	 * Constructor for a category. Finds the first free ID and assigns it to
	 * this new category.
	 * 
	 * @param name
	 *            the name of the category
	 * @param numeric
	 *            boolean stating if this is a numeric or non-numeric category
	 */
	function Category(name, numeric) {
		var category_id;
		// find the next available id
		for (category_id = 0; category_id < Number.MAX_VALUE
				&& _categories[category_id]; category_id = category_id + 1)
			;

		if (category_id == Number.MAX_VALUE
				|| category_id + 1 == Number.MAX_VALUE) {
			throw "No free category IDs";
		}

		this.name = name;
		this.numeric = numeric;
		this.catId = category_id;
		this.teams = [];

		_categories[this.catId] = this;
		_save();
	}

	/**
	 * Schedule timeslot.
	 */
	function Timeslot() {
		this.categories = {};
	}

	// //////////////////////// PUBLIC INTERFACE /////////////////////////
	$.finalist = {
		clearAllData : function() {
			_clear_local_storage();
			_teams = {};
			_categories = {};
			_divisions = [];
			_tournament = null;
		},

		getTournament : function() {
			return _tournament;
		},

		setTournament : function(tournament) {
			_tournament = tournament;
			_save();
		},

		/**
		 * Add a division to the list of known divisions. If the division
		 * already exists it is not added.
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

		setCurrentDivisionIndex : function(divisionIndex) {
			console.log("Set division index to " + divisionIndex);
			_currentDivisionIndex = divisionIndex;
			_save();
		},

		getCurrentDivisionIndex : function() {
			return _currentDivisionIndex;
		},

		getCurrentDivisionName : function() {
			console.log("_divisions: " + _divisions);
			console.log("_currentDivisionIndex: " + _currentDivisionIndex);
			if (null == _divisions || _currentDivisionIndex == null
					|| _currentDivisionIndex < -1
					|| _currentDivisionIndex >= _divisions.length) {
				return "Unknown";
			} else {
				return _divisions[_currentDivisionIndex];
			}
		},

		/**
		 * Create a new team.
		 */
		addTeam : function(num, division, name, org) {
			return new Team(num, division, name, org);
		},

		/**
		 * Find a team by number.
		 */
		lookupTeam : function(teamNum) {
			return _teams[teamNum];
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
		 * Create a new category.
		 * 
		 * @param category_name
		 *            the name of the category
		 * @param numeric
		 *            boolean if this is a numeric category or not
		 * @returns the new category or Null if there is a duplicate
		 */
		addCategory : function(categoryName, numeric) {
			if (_check_duplicate_category(categoryName)) {
				alert("There already exists a category with the name '"
						+ categoryName + "'");
				return null;
			} else {
				var newCategory = new Category(categoryName, numeric);
				return newCategory;
			}
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
		 *            the id to find
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
			category.teams.push(teamNum);
			_save();
		},

		removeTeamFromCategory : function(category, teamNum) {
			var index = category.teams.indexOf(teamNum);
			if (index != -1) {
				category.teams.splice(index, 1);
				_save();
			}
		},

		clearTeamsInCategory : function(category) {
			category.teams = [];
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
			console.log("Checking for team: " + teamNum + " in timeslot");
			var found = false;
			$.each(timeslot.categories, function(catId, slotTeamNum) {
				console.log("  Comparing against catId: " + catId
						+ " slotTeamNum: " + slotTeamNum);
				if (teamNum == slotTeamNum) {
					found = true;
				}
			});
			return found;
		},

		/**
		 * Create the finalist schedule.
		 * 
		 * @return array of timeslots in order from earliest to latest
		 */
		scheduleFinalists : function() {
			// Create map of teamNum -> [categories]
			var finalistsCount = {};
			$.each($.finalist.getAllCategories(), function(i, category) {
				console.log("Walking categories i: " + i + " category.name: "
						+ category.name);
				$.each(category.teams, function(j, teamNum) {
					console
							.log("  Walking teams j: " + j + " team: "
									+ teamNum);
					if (null == finalistsCount[teamNum]) {
						finalistsCount[teamNum] = [];
					}
					finalistsCount[teamNum].push(category);
					console.log("  Adding to finalistsCount[" + teamNum + "] "
							+ category.name);
				});
			});

			// sort the map so that the team in the most categories is first,
			// this
			// should ensure the minimum amount of time to do the finalist
			// judging
			var sortedTeams = [];
			$.each(finalistsCount, function(teamNum, categories) {
				console.log("Walking finalistsCount teamNum: " + teamNum
						+ " num categories: " + categories.length);
				sortedTeams.push(teamNum);
			});
			sortedTeams.sort(function(a, b) {
				console.log("a is: " + a);
				console.log("b is: " + b);
				var aCategories = finalistsCount[a];
				var bCategories = finalistsCount[b];

				// var aCount = finalistsCount[a].length;
				// var bCount = finalistsCount[b].length;
				if (aCount == bCount) {
					return 0;
				} else if (aCount < bCount) {
					return -1;
				} else {
					return 1;
				}
			});
			console.log("Sorted teams: " + sortedTeams);

			// list of Timeslots
			var schedule = [];
			$.each(sortedTeams, function(i, teamNum) {
				var teamCategories = finalistsCount[teamNum];
				$.each(teamCategories, function(j, category) {
					var scheduled = false;
					$.each(schedule,
							function(k, slot) {
								if (!scheduled
										&& !$.finalist.isTimeslotBusy(slot,
												category.catId)
										&& !$.finalist.isTeamInTimeslot(slot,
												teamNum)) {
									console.log("Adding team " + teamNum
											+ " to slot for category: "
											+ category.catId);
									$.finalist.addTeamToTimeslot(slot,
											category.catId, teamNum);
									scheduled = true;
								}
							}); // foreach timeslot
					if (!scheduled) {
						var newSlot = new Timeslot();
						schedule.push(newSlot);
						console.log("Adding team " + teamNum
								+ " to new slot for category: "
								+ category.catId);
						$.finalist.addTeamToTimeslot(newSlot, category.catId,
								teamNum);
					}
				}); // foreach category
			}); // foreach sorted team

			return schedule;
		},

	// FIXME handle timeslot duration on output

	};

	_load();
})(window.jQuery || window.$);
