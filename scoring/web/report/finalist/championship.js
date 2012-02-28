/*
 * Copyright (c) 2012INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

function initializeTeamsInCategory(currentCategory, teams, scoreGroups) {
	var checkedEnoughTeams = false;
	$.each(teams, function(i, team) {
		if (team.division == $.finalist.getCurrentDivision()) {
			if (!checkedEnoughTeams) {
				$.finalist.addTeamToCategory(currentCategory, team.num);
				
				var group = $.finalist.getOverallGroup(team);
				scoreGroups[group] = scoreGroups[group] - 1;

				checkedEnoughTeams = true;
				$.each(scoreGroups, function(key, value) {
					if (value > 0) {
						checkedEnoughTeams = false;
					}
				});
			}
		}
	});
}

$(document)
		.ready(
				function() {
					var currentCategory = $.finalist
							.getCategoryByName($.finalist.CHAMPIONSHIP_NAME);
					var currentDivision = $.finalist.getCurrentDivision();
					
					var previouslyVisited = $.finalist
							.isCategoryVisited(currentCategory, currentDivision);
					$.finalist.setCategoryVisited(currentCategory, currentDivision);

					$("#division").text(currentDivision);
					$("#data").empty();

					var headerRow = $("<tr><th>Finalist?</th><th>Score Group</th><th>Team #</th><th>Team Name</th><th>Score</th><th>Num Finalists</th></tr>");
					$("#data").append(headerRow);

					var teams = $.finalist.getAllTeams();
					// sort with highest score first
					teams.sort(function(a, b) {
						var aScore = $.finalist.getOverallScore(a);
						var bScore = $.finalist.getOverallScore(b);
						if (aScore == bScore) {
							return 0;
						} else if (aScore < bScore) {
							return 1;
						} else {
							return -1;
						}
					});

					// compute the set of all score groups
					var scoreGroups = {};
					$.each(teams, function(i, team) {
						if (team.division == currentDivision) {
							var group = $.finalist.getOverallGroup(team);
							scoreGroups[group] = $.finalist
									.getNumTeamsAutoSelected();
						}
					});

					if (!previouslyVisited) {
						// initialize teams in category
						initializeTeamsInCategory(currentCategory, teams, scoreGroups);
					}

					$.each(teams, function(i, team) {
						if (team.division == $.finalist.getCurrentDivision()) {
							var row = $("<tr></tr>");
							$("#data").append(row);

							var finalistCol = $("<td></td>");
							row.append(finalistCol);
							var finalistCheck = $("<input type='checkbox'/>");
							finalistCol.append(finalistCheck);
							finalistCheck.change(function() {
								if ($(this).attr("checked") == undefined) {
									$.finalist.removeTeamFromCategory(
											currentCategory, team.num);
								} else {
									$.finalist.addTeamToCategory(
											currentCategory, team.num);
								}
							});
							if ($.finalist.isTeamInCategory(currentCategory,
									team.num)) {
								finalistCheck.attr("checked", true);
							}

							var sgCol = $("<td></td>");
							row.append(sgCol);
							var group = $.finalist.getOverallGroup(team);
							sgCol.text(group);

							var numCol = $("<td></td>");
							row.append(numCol);
							numCol.text(team.num);

							var nameCol = $("<td></td>");
							row.append(nameCol);
							nameCol.text(team.name);

							var scoreCol = $("<td></td>");
							row.append(scoreCol);
							scoreCol.text($.finalist.getOverallScore(team));

							var numFinalistCol = $("<td></td>");
							row.append(numFinalistCol);
						} // in correct division
					});

					$.finalist.displayNavbar();
				}); // end ready function
