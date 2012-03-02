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
	var _duration;
	var _categoriesVisited;
	var _currentCategoryId; // category to display with numeric.html

	function _init_variables() {
		_teams = {};
		_categories = {};
		_tournament = null;
		_divisions = [];
		_currentDivision = null;
		_numTeamsAutoSelected = 2;
		_startHour = 2;
		_startMinute = 0;
		_duration = 20;
		_categoriesVisited = {};
		_currentCategory = null;
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
		$.jStorage.set(STORAGE_PREFIX + "_categoriesVisited",
				_categoriesVisited);
		$.jStorage.set(STORAGE_PREFIX + "_currentCategoryId",
				_currentCategoryId);
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
			throw new Error("Team already exists with number: " + num);
		}

		this.num = num;
		this.division = division;
		this.name = name;
		this.org = org;
		this.categoryScores = {};
		this.categoryGroups = {};
		this.overallScore = null;
		this.overallGroup = null;
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
			throw new Error("No free category IDs");
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
		CHAMPIONSHIP_NAME : "Championship",

		clearAllData : function() {
			_clear_local_storage();
			_init_variables();
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

		setNumTeamsAutoSelected : function(num) {
			_numTeamsAutoSelected = num;
			_save();
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

		setOverallScore : function(team, score, group) {
			team.overallScore = score;
			team.overallJudgingGroup = group;
			_save();
		},

		getOverallScore : function(team) {
			return team.overallScore;
		},

		getOverallGroup : function(team) {
			return team.overallJudgingGroup;
		},

		getCategoryScore : function(team, category) {
			return team.categoryScores[category.catId];
		},

		getCategoryGroup : function(team, category) {
			return team.categoryGroups[category.catId];
		},

		setCategoryScore : function(team, category, score, group) {
			team.categoryScores[category.catId] = score;
			team.categoryGroups[category.catId] = group;
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
				category.teams.push(teamNum);
				_save();
			}
		},

		removeTeamFromCategory : function(category, teamNum) {
			teamNum = parseInt(teamNum, 10);
			var index = category.teams.indexOf(teamNum);
			if (index != -1) {
				category.teams.splice(index, 1);
				_save();
			}
		},

		isTeamInCategory : function(category, teamNum) {
			teamNum = parseInt(teamNum, 10);
			return -1 != category.teams.indexOf(teamNum);
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
			var found = false;
			$.each(timeslot.categories, function(catId, slotTeamNum) {
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
				$.each(category.teams, function(j, teamNum) {
					var team = $.finalist.lookupTeam(teamNum);
					if (team.division == $.finalist.getCurrentDivision()) {
						if (null == finalistsCount[teamNum]) {
							finalistsCount[teamNum] = [];
						}
						finalistsCount[teamNum].push(category);
					}
				});
			});

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
				} else if (aCount < bCount) {
					return -1;
				} else {
					return 1;
				}
			});

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
									$.finalist.addTeamToTimeslot(slot,
											category.catId, teamNum);
									scheduled = true;
								}
							}); // foreach timeslot
					if (!scheduled) {
						var newSlot = new Timeslot();
						schedule.push(newSlot);
						$.finalist.addTeamToTimeslot(newSlot, category.catId,
								teamNum);
					}
				}); // foreach category
			}); // foreach sorted team

			return schedule;
		},

		setStartHour : function(hour) {
			_startHour = hour;
			_save();
		},

		getStartHour : function() {
			return _startHour;
		},

		setStartMinute : function(minute) {
			_startMinute = minute;
			_save();
		},

		getStartMinute : function() {
			return _startMinute;
		},

		getStartTime : function() {
			var time = new Date();
			time.setHours(_startHour);
			time.setMinutes(_startMinute);
			return time;
		},

		setDuration : function(v) {
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
				element = $("<span></span>")
			} else {
				element = $("<a href='params.html'></a>")
			}
			element.text("Parameters");
			$("#navbar").append(element);

			$("#navbar").append($("<span> - </span>"));

			if (window.location.pathname.match(/\/non-numeric.html$/)) {
				element = $("<span></span>")
			} else {
				element = $("<a href='non-numeric.html'></a>")
			}
			element.text("Non-numeric Categories");
			$("#navbar").append(element);

			$("#navbar").append($("<span> - </span>"));

			$.each($.finalist.getNumericCategories(), function(i, category) {
				if (category.name != $.finalist.CHAMPIONSHIP_NAME) {
					if (window.location.pathname.match(/\/numeric.html$/)
							&& window.location.search == "?" + category.catId) {
						element = $("<span></span>")
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
					&& window.location.search == "?"
							+ championshipCategory.catId) {
				element = $("<span></span>")
			} else {
				element = $("<a href='numeric.html'></a>")
				element
						.click(function() {
							$.finalist
									.setCurrentCategoryId(championshipCategory.catId);
						});
			}
			element.text($.finalist.CHAMPIONSHIP_NAME);
			$("#navbar").append(element);

			$("#navbar").append($("<span> - </span>"));

			if (window.location.pathname.match(/\/schedule.html$/)) {
				element = $("<span></span>")
			} else {
				element = $("<a href='schedule.html'></a>")
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
			appCache.addEventListener('cached', $.finalist.handleCacheEvent,
					false);

			// Checking for an update. Always the first event fired in the
			// sequence.
			appCache.addEventListener('checking', $.finalist.handleCacheEvent,
					false);

			// An update was found. The browser is fetching resources.
			appCache.addEventListener('downloading',
					$.finalist.handleCacheEvent, false);

			// The manifest returns 404 or 410, the download failed,
			// or the manifest changed while the download was in progress.
			appCache.addEventListener('error', $.finalist.handleCacheEvent,
					false);

			// Fired after the first download of the manifest.
			appCache.addEventListener('noupdate', $.finalist.handleCacheEvent,
					false);

			// Fired if the manifest file returns a 404 or 410.
			// This results in the application cache being deleted.
			appCache.addEventListener('obsolete', $.finalist.handleCacheEvent,
					false);

			// Fired for each resource listed in the manifest as it is being
			// fetched.
			appCache.addEventListener('progress', $.finalist.handleCacheEvent,
					false);

			// Fired when the manifest resources have been newly redownloaded.
			appCache.addEventListener('updateready',
					$.finalist.handleCacheEvent, false);

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
