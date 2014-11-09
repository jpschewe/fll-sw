/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

function loadData() {
	$.mobile.loading("show");

	$.subjective.loadFromServer(function() {
		subjectiveCategories = $.subjective.getSubjectiveCategories();

		$.mobile.loading("hide");
		$("#choose_clear").hide();

		if (0 == subjectiveCategories.length) {
			alert("No subjective data loaded from server");
		} else {
			$("#messages").append(
					"Loaded " + subjectiveCategories.length
							+ " categories from the server<br/>");
		}
		$("#messages").append(
				"Current tournament is " + $.subjective.getTournament().name
						+ "<br/>");

		promptForJudgingGroup();
	}, function(message) {
		$.mobile.loading("hide");

		alert("Error getting data from server: " + message);
	});
}

function checkStoredData() {
	if ($.subjective.storedDataExists()) {
		checkTournament();
	} else {
		loadData();
	}
}

function promptForJudgingGroup() {
	location.href = "subjective-ui.html";
}

function promptForReload() {
	$("#choose_clear").show();
}

function reloadData() {
	if ($.subjective.checkForModifiedScores()) {
		var answer = confirm("You have modified scores, this will remove them. Are you sure?")
		if (!answer) {
			return;
		}
	}
	$.subjective.clearAllData();
	loadData();
}

function checkTournament() {
	$.mobile.loading("show");

	$.subjective
			.getServerTournament(
					function(serverTournament) {
						$.mobile.loading("hide");

						var storedTournament = $.subjective.getTournament();
						if (null == storedTournament) {
							reloadData();
						} else if (storedTournament.name != serverTournament.name
								|| storedTournament.tournamentID != serverTournament.tournamentID) {
							reloadData();
						} else {
							promptForReload();
						}
					}, function() {
						alert("Error getting data from server");
					});
}

$(document).on("pageshow", "#index-page", function(event) {

	$("#choose_clear").hide();

	$("#clear").click(function() {
		$("#choose_clear").hide();
		reloadData();
	});
	$("#keep").click(function() {
		$("#choose_clear").hide();
		promptForJudgingGroup();
	});

	checkStoredData();

});
