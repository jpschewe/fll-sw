/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

function selectTeam(team) {
	$.subjective.setCurrentTeam(team);

	location.href = "enter-score.html";
}

$("#teams-list-page").live("pagebeforecreate", function(event) {

	var teams = $.subjective.getCurrentTeams();
	$.each(teams, function(i, team) {
		var time = $.subjective.getScheduledTime(team.teamNumber);
		var timeStr = time.getHours() + ":" + time.getMinutes();

		var scoreStr;
		var score = $.subjective.getScore(team.teamNumber);
		if (!$.subjective.isScoreCompleted(score)) {
			scoreStr = "";
		} else if (score.noShow) {
			scoreStr = "No Show";
		} else {
			var computedScore = $.subjective.computeScore(score);
			scoreStr = computedScore;
		}
		var button = $("<button class='ui-btn ui-corner-all'>" + timeStr //
				+ " " + team.teamNumber //
				+ " " + team.teamName //
				+ " " + team.organization //
				+ " " + scoreStr //
				+ "</button>");
		$("#teams").append(button);
		button.click(function() {
			selectTeam(team);
		});

	});

	var currentJudgingGroup = $.subjective.getCurrentJudgingGroup();
	$("#judging-group").text(currentJudgingGroup);

	var currentCategory = $.subjective.getCurrentCategory();
	$("#category").text(currentCategory.title);

});
