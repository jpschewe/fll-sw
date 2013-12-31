/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

function loadData() {
	$("#wait").show();

	$.subjective.loadFromServer(function() {
		subjectiveCategories = $.subjective.getSubjectiveCategories();

		$("#wait").hide();
		$("#choose_clear").hide();
		// location.href = "params.html";

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
	}, function() {
		alert("Error getting data from server");
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
	$("#messages").append("<p>FIXME prompt user for judging group</p>");
}

function promptForReload() {
	$("#choose_clear").show();
}

function reloadData() {
	$.subjective.clearAllData();
	loadData();
}

function checkTournament() {
	$("#wait").show();
	$.subjective
			.getServerTournament(
					function(serverTournament) {
						$("#wait").hide();

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

$(document).ready(function() {
	$("#wait").hide();
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
