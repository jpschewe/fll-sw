/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */


$(document).on("pagecreate", function() {

	var judgingGroups = $.subjective.getJudgingGroups();
	$.each(judgingGroups, function(i, group) {
		var button = "<button class='ui-btn ui-corner-all' id='" + group + "'>" + group + "</button>";
		$("#judging-groups").append(button);
		//FIXME need action
		//FIXME store judging group
		//FIXME display judging group in footer
	});
	
});
