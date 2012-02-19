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
		$.jStorage.set(STORAGE_PREFIX + "_categories", _categories);
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
		value = $.jStorage.get(STORAGE_PREFIX + "_categories");
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
		this.catId = category_id;
		this.teams = [];

		this.setName = function(newName) {
			if (this.name == newName) {
				return true;
			}

			if (_check_duplicate_category(newName)) {
				return false;
			} else {
				this.name = newName;
				_save();
				return true;
			}
		};

		this.addTeam = function(teamNum) {
			this.teams.push(teamNum);
			_save();
		};

		this.removeTeam = function(teamNum) {
			var index = this.teams.indexOf(teamNum);
			if (index != -1) {
				this.teams.splice(index, 1);
				_save();
			}
		};

		this.clearTeams = function() {
			this.teams = [];
			_save();
		};

		_categories[this.catId] = this;
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
		 * Create a new category.
		 * 
		 * @param category_name
		 *            the name of the category
		 * @returns the new category or Null if there is a duplicate
		 */
		addCategory : function(categoryName) {
			if (_check_duplicate_category(categoryName)) {
				alert("There already exists a category with the name '"
						+ categoryName + "'");
				return null;
			} else {
				var newCategory = new Category(categoryName);
				return newCategory;
			}
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
				if (val.catId == toFind) {
					category = val;
				}
			});
			return category;
		},

	};

	_load();
})(window.jQuery || window.$);
