/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

function createNewScore() {
	score = new Object();
	score.modified = false;
	score.deleted = false;
	score.noShow = false;
	score.standardSubScores = {};
	score.enumSubScores = {};
	score.judge = $.subjective.getCurrentJudge().id;
	score.teamNumber = $.subjective.getCurrentTeam().teamNumber;

	return score;
}

/**
 * Save the state of the current page to the specified score object. If null, do
 * nothing.
 */
function saveToScoreObject(score) {
	if (null == score) {
		return;
	}

	$.each($.subjective.getCurrentCategory().goals, function(index, goal) {
		if (goal.enumerated) {
			alert("Enumerated goals not supported: " + goal.name);
		} else {
			var subscore = Number($("#" + goal.name).val());
			score.standardSubScores[goal.name] = subscore;
		}
	});
}

function recomputeTotal() {
	var currentTeam = $.subjective.getCurrentTeam();
	var score = $.subjective.getScore(currentTeam.teamNumber);

	var total = 0;
	$.each($.subjective.getCurrentCategory().goals, function(index, goal) {
		if (goal.enumerated) {
			alert("Enumerated goals not supported: " + goal.name);
		} else {
			var subscore = Number($("#" + goal.name).val());
			var multiplier = Number(goal.multiplier);
			total = total + subscore * multiplier;
		}
	});
	$("#total-score").text(total);
}

function createScoreRow(goal, subscore) {
	var row = $("<div class=\"ui-grid-b ui-responsive\"></div>");

	var categoryBlock = $("<div class=\"ui-block-a\"></div>");
	var categoryLabel = $("<p>" + goal.category + "</p>");
	categoryBlock.append(categoryLabel);
	row.append(categoryBlock);

	var titleBlock = $("<div class=\"ui-block-b\"></div>");
	var titleLabel = $("<p>" + goal.title + "</p>");
	titleBlock.append(titleLabel);
	row.append(titleBlock);

	var rightBlock = $("<div class=\"ui-block-c\"></div>");
	var rightContainer = $("<div class=\"ui-grid-a ui-responsive\"></div>");
	rightBlock.append(rightContainer);

	var scoreBlock = $("<div class=\"ui-block-a\"></div>");
	var scoreSelect = $("<select id=\"" + goal.name + "\"></select>");
	scoreSelect.change(function() {
		recomputeTotal();
	});
	if (goal.scoreType == "INTEGER") {
		for (var v = Number(goal.min); v <= Number(goal.max); ++v) {
			var selected = "";
			if (null != subscore && subscore == v) {
				selected = "selected";
			}
			var option = $("<option value=\"" + v + "\" " + selected + " >" + v
					+ "</option>");
			scoreSelect.append(option);
		}
	} else {
		alert("Non-integer goals are not supported: " + goal.name);
	}

	scoreBlock.append(scoreSelect);
	rightContainer.append(scoreBlock);

	var rubricBlock = $("<div class=\"ui-block-b\"></div>");
	rightContainer.append(rubricBlock);
	var rubricButton = $("<button class=\"ui-btn ui-corner-all ui-icon-arrow-r ui-btn-icon-notext ui-btn-inline\"></button>");
	rubricBlock.append(rubricButton);
	rubricButton.click(function() {
		var currentTeam = $.subjective.getCurrentTeam();
		var score = $.subjective.getScore(currentTeam.teamNumber);
		if(null == score) {
			score = createNewScore();
		}
		
		var scoreCopy = $.extend(true, {}, score);
		saveToScoreObject(scoreCopy);
		$.subjective.setTempScore(scoreCopy);
		$.subjective.setCurrentGoal(goal);
		location.href = "rubric.html";
	});

	row.append(rightContainer);

	$("#score-content").append(row);
}

$(document).on("pagebeforecreate", "#enter-score-page", function(event) {
	var currentTeam = $.subjective.getCurrentTeam();
	$("#team-number").text(currentTeam.teamNumber);
	$("#team-name").text(currentTeam.teamName);

	// load the saved score if needed
	var score = $.subjective.getTempScore();
	if (null == score) {
		score = $.subjective.getScore(currentTeam.teamNumber);
	}
	$.each($.subjective.getCurrentCategory().goals, function(index, goal) {
		if (goal.enumerated) {
			alert("Enumerated goals not supported: " + goal.name);
		} else {
			var subscore = null;
			if ($.subjective.isScoreCompleted(score)) {
				subscore = score.standardSubScores[goal.name];
			}
			createScoreRow(goal, subscore);
		}
	});

	recomputeTotal();

	// clear out temp state so that we don't get it again
	$.subjective.setTempScore(null);
	$.subjective.setCurrentGoal(null);
});

$(document).on("pageinit", "#enter-score-page", function(event) {
	$("#save-score").click(function() {

		var currentTeam = $.subjective.getCurrentTeam();
		var score = $.subjective.getScore(currentTeam.teamNumber);
		if (null == score) {
			score = createNewScore();
		}
		score.modified = true;
		score.deleted = false;
		score.noShow = false;

		saveToScoreObject(score);

		$.subjective.saveScore(score);

		location.href = "teams-list.html";
	});

	$("#cancel-score").click(function() {
		location.href = "teams-list.html";
	});

	$("#delete-score").click(function() {
		var currentTeam = $.subjective.getCurrentTeam();
		var score = $.subjective.getScore(currentTeam.teamNumber);
		if (null == score) {
			score = createNewScore();
		}
		score.modified = true;
		score.noShow = false;
		score.deleted = true;
		$.subjective.saveScore(score);

		location.href = "teams-list.html";
	});

	$("#noshow-score").click(function() {
		var currentTeam = $.subjective.getCurrentTeam();
		var score = $.subjective.getScore(currentTeam.teamNumber);
		if (null == score) {
			score = createNewScore();
		}
		score.modified = true;
		score.noShow = true;
		score.deleted = false;
		$.subjective.saveScore(score);

		location.href = "teams-list.html";
	});

	$("#add-note").click(function() {
		// FIXME setup click handler for add note
		alert("Adding notes not yet supported");
	});

});