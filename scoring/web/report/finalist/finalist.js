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

    //var STORAGE_PREFIX = "time-tracker.";
    

    // //////////////////////// PUBLIC INTERFACE /////////////////////////
    $.finalists = {
	lookup_name : function(num_field, name_field) {
	    var team_data = new Array();
	    team_data[1] = 'Team 1';
	    return team_data[num];
	}
    };

    // _load();
})(window.jQuery || window.$);
