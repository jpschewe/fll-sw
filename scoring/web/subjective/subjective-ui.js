/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

function selectJudgingGroup(group) {
	$.subjective.setCurrentJudgingGroup(group);
	$.mobile.navigate("#choose-category-page");
}

$(document).on(
		"pagebeforeshow",
		"#choose-judging-group-page",
		function(event) {

			
			$("#choose-judging-group_judging-groups").empty();
			
			var judgingGroups = $.subjective.getJudgingGroups();
			$.each(judgingGroups, function(i, group) {
				var button = $("<button class='ui-btn ui-corner-all'>" + group
						+ "</button>");
				$("#choose-judging-group_judging-groups").append(button);
				button.click(function() {
					selectJudgingGroup(group);
				});

			});

			$("#choose-judging-group-page").trigger("create");
		});

function selectCategory(category) {
	$.subjective.setCurrentCategory(category);
	$.mobile.navigate("#choose-judge-page");
}

$(document).on(
		"pagebeforeshow",
		"#choose-category-page",
		function(event) {
			$.subjective.log("refreshing choose-category page");

			$("#choose-category_categories").empty();

			var categories = $.subjective.getSubjectiveCategories();
			$.each(categories, function(i, category) {
				var button = $("<button class='ui-btn ui-corner-all'>"
						+ category.title + "</button>");
				$("#choose-category_categories").append(button);
				button.click(function() {
					selectCategory(category);
				});
				button.trigger("updateLayout");
			});

			var currentJudgingGroup = $.subjective.getCurrentJudgingGroup();
			$("#choose-category_judging-group").text(currentJudgingGroup);
			
			$("#choose-category-page").trigger("create");
			});


$(document).on(
		"pagebeforeshow",
		"#choose-judge-page",
		function(event) {
			$.subjective.log("refreshing choose judge page");
			
			$("#choose-judge_new-judge-info").hide();

			$("#choose-judge_judges").empty();
			$("#choose-judge_judges").append(
					"<input type='radio' name='judge' id='choose-judge_new-judge' value='new-judge'>");
			$("#choose-judge_judges").append(
					"<label for='choose-judge_new-judge'>New Judge</label>");
			
			var judges = $.subjective.getPossibleJudges();
			$.each(judges, function(i, judge) {
				$("#choose-judge_judges").append(
						"<input type='radio' name='judge' id='choose-judge_" + judge.id
								+ "' value='" + judge.id + "'>");
				$("#choose-judge_judges").append(
						"<label for='choose-judge_" + judge.id + "'>" + judge.id
								+ "</label>");
			});

			var currentJudgingGroup = $.subjective.getCurrentJudgingGroup();
			$("#choose-judge_judging-group").text(currentJudgingGroup);

			var currentCategory = $.subjective.getCurrentCategory();
			$("#choose-judge_category").text(currentCategory.title);

			var currentJudge = $.subjective.getCurrentJudge();
			if (null != currentJudge) {
				$("input:radio[value=\"" + currentJudge.id + "\"]").prop(
						'checked', true);
			} else {
				$("input:radio[value='new-judge']").prop('checked', true);
				$("#choose-judge_new-judge-info").show();
			}
						
			$("input[name=judge]:radio").change(function () {
				var judgeID = $("input:radio[name='judge']:checked").val();
				if ('new-judge' == judgeID) {
					$("#choose-judge_new-judge-info").show();
				} else {
					$("#choose-judge_new-judge-info").hide();
				}
			});
			
			$("#choose-judge-page").trigger("create");
		});

$(document).on("pageinit", "#choose-judge-page", function(event) {

	$("#choose-judge_judge-submit").click(function() {
		setJudge();
	});

});

function setJudge() {
	var judgeID = $("input:radio[name='judge']:checked").val();
	if ('new-judge' == judgeID) {
		judgeID = $("#choose-judge_new-judge-name").val();
		if (null == judgeID || "" == judgeID) {
			alert("You must enter a name");
			return;
		}
		judgeID = judgeID.toUpperCase();
		
		var phone = $("#choose-judge_new-judge-phone").val();
		if (null == phone || "" == phone) {
			alert("You must enter a phone nunmber");
			return;
		}
		
		$.subjective.addJudge(judgeID, phone);
	}


	$.subjective.setCurrentJudge(judgeID);

	$.mobile.navigate("#teams-list-page");
}


function selectTeam(team) {
	$.subjective.setCurrentTeam(team);

	location.href = "enter-score.html";
}

function populateTeams() {
	$("#teams-list_teams").empty();
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
		$("#teams-list_teams").append(button);
		button.click(function() {
			selectTeam(team);
		});

	});
}

$(document).on("pagebeforecreate", "#teams-list-page", function(event) {

	populateTeams();

	var currentJudgingGroup = $.subjective.getCurrentJudgingGroup();
	$("#teams-list_judging-group").text(currentJudgingGroup);

	var currentCategory = $.subjective.getCurrentCategory();
	$("#teams-list_category").text(currentCategory.title);

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
		$.mobile.navigate("#choose-judge-page");
	});
	$("#nav-choose-category").click(function() {
		$.mobile.navigate("#choose-category-page");
	});
	$("#nav-choose-judge").click(function() {
		$.mobile.navigate("#choose-judge-page");
	});

	$("#upload-scores").click(function() {
		$("#upload-scores-wait").show();

		$.subjective.uploadData(uploadScoresSuccess, uploadScoresFail,
				uploadJudgesSuccess, uploadJudgesFail,
				loadScoresSuccess, loadScoresFail);
	});
});
