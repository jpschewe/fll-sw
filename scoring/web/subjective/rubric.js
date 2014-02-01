/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

function setRubricScore(value) {
	$("#rubric-score").val(value).selectmenu("refresh", true);
}

function rangeSort(a, b) {
	if (a.min < b.min) {
		return -1;
	} else if (a.min > b.min) {
		return 1;
	} else {
		return 0;
	}
}

function populateRubric(goal) {
	$("#rubric-content").empty();

	var ranges = goal.rubric;
	ranges.sort(rangeSort);

	var ndGrid = $("<div class=\"ui-grid-a\"></div>");
	var ndBlockA = $("<div class=\"ui-grid-a rubric-not-done\">Not Done</div>");
	var ndBlockB = $("<div class=\"ui-grid-b\"></div>");
	var ndButton = $("<button>Set</button>");
	ndButton.click(function() {
		setRubricScore(0);
	});
	ndBlockB.append(ndButton);
	ndGrid.append(ndBlockA);
	ndGrid.append(ndBlockB);
	$("#rubric-content").append(ndGrid);

	$.each(ranges, function(index, range) {
		var titleDiv = $("<div class=\"rubric-title\">" + range.title + " ("
				+ range.min + "-" + range.max + ")<div>");
		$("#rubric-content").append(titleDiv);

		var grid = $("<div class=\"ui-grid-a\"></div>");
		var blockA = $("<div class=\"ui-grid-a\"></div>");
		blockA.text(range.description);

		var blockB = $("<div class=\"ui-grid-b\"></div>");
		var button = $("<button>Set</button>");
		button.click(function() {
			var mid = Math.floor((range.min + range.max) / 2);
			$.subjective.log("Setting score to " + mid);
			setRubricScore(mid);
		});
		blockB.append(button);

		grid.append(blockA);
		grid.append(blockB);
		$("#rubric-content").append(grid);

	});
}

$(document).on("pagebeforeshow", "#rubric-page", function(event) {
	var score = $.subjective.getTempScore();
	var goal = $.subjective.getCurrentGoal();

	var subscore;
	$.subjective.log("goal: " + goal.name);
	if (goal.enumerated) {
		subscore = null;
		$.subjective.log("enumerated score: "
				+ score.enumSubScores[goal.name]);
	} else {
		subscore = score.standardSubScores[goal.name];
		$.subjective.log("score: " + subscore);
	}
	setRubricScore(subscore);

	if (goal.scoreType == "INTEGER") {
		for (var v = Number(goal.min); v <= Number(goal.max); ++v) {
			var option = $("<option value=\"" + v + "\">" + v + "</option>");
			$("#rubric-score").append(option);
		}
	} else {
		alert("Non-integer goals are not supported: " + goal.name);
	}

	$("#rubric-category").text(goal.category);
	$("#rubric-goal-title").text(goal.title);
	$("#rubric-description").text(goal.description);

	populateRubric(goal);
	
	$("#rubric-page").trigger("create");
});

$(document).on("pageinit", "#rubric-page", function(event) {
	$("#rubric-save-score").click(function() {
		var score = $.subjective.getTempScore();
		var goal = $.subjective.getCurrentGoal();
		if (goal.enumerated) {
			alert("Enumerated unsupported");
		} else {
			score.standardSubScores[goal.name] = $("#rubric-score").val();
			$.subjective.setTempScore(score);
			location.href = "enter-score.html";
		}

	});

	$("#rubric-cancel-score").click(function() {
		location.href = "enter-score.html";
	});

});