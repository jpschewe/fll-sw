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

	/**
	 * Save the current state to local storage.
	 */
	function _save() {
		$.jStorage.set(STORAGE_PREFIX + "_teams", _teams);
		$.jStorage.set(STORAGE_PREFIX + "_categories", _teams);
	}

	/**
	 * Load the current state from local storage.
	 */
	function _load() {
		var value = $.jStorage.get(STORAGE_PREFIX + "_teams");
		if (null != value) {
			_teams = value;
		} else {
			// FIXME load some test data
			alert("Loading test data");
			new Team(1, "Team 1", "Org 1");
		}
		value = $.jStorage.get(STORAGE_PREFIX + "_categoires");
		if (null != value) {
			_categories = value;
		}
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
	function Team(num, name, org) {
		if (typeof (_teams[num]) != 'undefined') {
			throw "Team already exists with number: " + num;
		}

		this.num = num;
		this.name = name;
		this.org = org;
		_teams[num] = this;
		_save();
	}

	/**
	 * Constructor for a category. Finds the first free ID and assigns it to
	 * this new category.
	 */
	function Category(name) {
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
		this.cat_id = category_id;
		this.teams = []
		_categories[this.cat_id] = this;
		_save();
	}

	// //////////////////////// PUBLIC INTERFACE /////////////////////////
	$.finalists = {
		/**
		 * Find a team by number.
		 */
		lookupTeam : function(teamNum) {
			return _teams[teamNum];
		},

		/**
		 * Get the categories known to the system.
		 * 
		 * @returns {Array} sorted by name
		 */
		getCategories : function() {
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
				if (val.cat_id == toFind) {
					category = val;
				}
			});
			return category;
		},

		addTeamToCategory : function(category, teamNum) {
			category.teams.push(teamNum);
		},

		removeTeamFromCategory : function(category, teamNum) {
			var index = category.teams.indexOf(teamNum);
			if (index != -1) {
				category.teams.splice(index, 1);
			}
		},

		clearTeamsInCategory : function(category) {
			category.teams = [];
		}

	};

	_load();
})(window.jQuery || window.$);
