/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

$(document).on(
		"pagebeforecreate",
		"#rubric-page",
		function(event) {
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

			if (goal.scoreType == "INTEGER") {
				for (var v = Number(goal.min); v <= Number(goal.max); ++v) {
					var selected = "";
					if (null != subscore && subscore == v) {
						selected = "selected";
					}
					var option = $("<option value=\"" + v + "\" " + selected
							+ " >" + v + "</option>");
					$("#rubric-score").append(option);
				}
			} else {
				alert("Non-integer goals are not supported: " + goal.name);
			}

			$("#rubric-category").text(goal.category);
			$("#rubric-goal-title").text(goal.title);

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