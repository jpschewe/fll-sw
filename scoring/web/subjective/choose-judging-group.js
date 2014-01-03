/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

function selectJudgingGroup(group) {
	$.subjective.setCurrentJudgingGroup(group);
	location.href = "choose-category.html";
}

$(document).on(
		"pagebeforecreate",
		"#choose-judging-group-page",
		function(event) {

			var judgingGroups = $.subjective.getJudgingGroups();
			$.each(judgingGroups, function(i, group) {
				var button = $("<button class='ui-btn ui-corner-all'>" + group
						+ "</button>");
				$("#judging-groups").append(button);
				button.click(function() {
					selectJudgingGroup(group);
				});

			});

		});
