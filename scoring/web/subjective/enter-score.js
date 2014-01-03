/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

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

	// FIXME update total score when this
	// score changes
	var scoreBlock = $("<div class=\"ui-block-a\"></div>");
	var scoreSelect = $("<select></select>");
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

	var detailBlock = $("<div class=\"ui-block-b\"></div>");
	rightContainer.append(detailBlock);
	var detailButton = $("<button class=\"ui-btn ui-corner-all ui-icon-arrow-r ui-btn-icon-notext ui-btn-inline\"></button>");
	detailBlock.append(detailButton);

	row.append(rightContainer);

	$("#score-content").append(row);
}

$("#enter-score-page").live("pagebeforecreate", function(event) {
	var currentTeam = $.subjective.getCurrentTeam();
	$("#team-number").text(currentTeam.teamNumber);
	$("#team-name").text(currentTeam.teamName);

	var score = $.subjective.getScore(currentTeam.teamNumber);
	$.each($.subjective.getCurrentCategory().goals, function(index, goal) {
		if (goal.enumerated) {
			alert("Enumerated goals not supported: " + goal.name);
		} else {
			var subscore = score.standardSubScores[goal.name];
			createScoreRow(goal, subscore);
		}
	});

});

$("#enter-score-page").live("pageinit", function(event) {
	$("#save-score").click(function() {
		
		var currentTeam = $.subjective.getCurrentTeam();
		var score = $.subjective.getScore(currentTeam.teamNumber);
		score.modified = true;
		
		// FIXME populate score based upon values in the dropdown boxes 
		
		$.subjective.saveScore(score);
		
		location.href = "teams-list.html";
	});

	$("#cancel-score").click(function() {
		location.href = "teams-list.html";
	});

	$("#delete-score").click(function() {
		var currentTeam = $.subjective.getCurrentTeam();
		var score = $.subjective.getScore(currentTeam.teamNumber);
		score.modified = true;
		score.deleted = true;
		$.subjective.saveScore(score);
		
		location.href = "teams-list.html";
	});

	$("#noshow-score").click(function() {
		var currentTeam = $.subjective.getCurrentTeam();
		var score = $.subjective.getScore(currentTeam.teamNumber);
		score.modified = true;
		score.noShow = true;
		$.subjective.saveScore(score);

		location.href = "teams-list.html";
	});

	$("#add-note").click(function() {
		// FIXME setup click handler for add note
		alert("Adding notes not yet supported");
	});

});