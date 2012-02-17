/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

(function($) {
    if (!$) {
        throw new Error("jQuery needs to be loaded!");
    }
    //if (!$.jStorage) {
    //    throw new Error("jStorage needs to be loaded!");
    //}

    var STORAGE_PREFIX = "fll.finalists";

    // //////////////////////// PRIVATE METHODS ////////////////////////
    var _teams = {};

    /**
     * Save the current state to local storage.
     */
    function _save() {
        //FIXME $.jStorage.set(STORAGE_PREFIX + "_teams", _teams);
    }

    /**
     * Load the current state from local storage.
     */
    function _load() {
        //FIXME var value = $.jStorage.get(STORAGE_PREFIX + "_teams");
        //FIXME if (null != value) {
        //FIXME     _teams = value;
        //FIXME }
	//FIXME load some test data
	new Team(1, "Team 1", "Org 1");
    }
    
    /**
     * Constructor for a Team.
     */
    function Team(num, name, org) {
	if(typeof(_teams[num]) != 'undefined') {
	    throw "Team already exists with number: " + num;
	}

	this.num = num;
        this.name = name;
        this.org = org;
	_teams[num] = this;
        _save();
    }    

    // //////////////////////// PUBLIC INTERFACE /////////////////////////
    $.finalists = {
	/**
	 * Find a team by number.
	 */
	lookupTeam : function(teamNum) {
	    return _teams[teamNum];
	}
    };

    _load();
})(window.jQuery || window.$);
