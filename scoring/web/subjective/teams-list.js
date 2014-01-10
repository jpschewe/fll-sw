/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

function selectTeam(team) {
	$.subjective.setCurrentTeam(team);

	location.href = "enter-score.html";
}

function populateTeams() {
	$("#teams").empty();
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
}

$(document).on("pagebeforecreate", "#teams-list-page", function(event) {

	populateTeams();

	var currentJudgingGroup = $.subjective.getCurrentJudgingGroup();
	$("#judging-group").text(currentJudgingGroup);

	var currentCategory = $.subjective.getCurrentCategory();
	$("#category").text(currentCategory.title);

});

function uploadScoresSuccess(result) {
	populateTeams();

	alert("Uploaded " + result.numModified + " scores. message: " + result.message);
}

function uploadScoresFail(result) {
	populateTeams();

	var message;
	if (null == result) {
		message = "Unknown server error";
	} else {
		message = result.message;
	}

	alert("Failed to upload scores: " + message);
}

function uploadJudgesSuccess(result) {
	$.subjective.log("Judges modified: " + result.numModifiedJudges + " new: " + result.numNewJudges);
}

function uploadJudgesFail(result) {
	var message;
	if (null == result) {
		message = "Unknown server error";
	} else {
		message = result.message;
	}

	alert("Failed to upload judges: " + message);
}

function loadScoresSuccess() {
	$("#upload-scores-wait").hide();
}

function loadScoresFail(message) {
	$("#upload-scores-wait").hide();

	alert("Failed to load scores from server: " + message);
}

$(document).on("pageinit", "#teams-list-page", function(event) {
	$("#upload-scores-wait").hide();

	$("#nav-top").click(function() {
		location.href = "index.html";
	});
	$("#nav-choose-judging-group").click(function() {
		location.href = "choose-judge.html";
	});
	$("#nav-choose-category").click(function() {
		location.href = "choose-category.html";
	});
	$("#nav-choose-judge").click(function() {
		location.href = "choose-judge.html";
	});

	$("#upload-scores").click(function() {
		$("#upload-scores-wait").show();

		$.subjective.uploadData(uploadScoresSuccess, uploadScoresFail,
				uploadJudgesSuccess, uploadJudgesFail,
				loadScoresSuccess, loadScoresFail);
	});
});

