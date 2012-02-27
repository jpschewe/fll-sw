/*
 * Copyright (c) 2012INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

$(document)
		.ready(
				function() {
					$("#division").text($.finalist.getCurrentDivision());
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

					var championshipCategory = $.finalist
							.getCategoryByName($.finalist.CHAMPIONSHIP_NAME);
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
											championshipCategory, team.num);
								} else {
									$.finalist.addTeamToCategory(
											championshipCategory, team.num);
								}
							});

							// FIXME auto check boxes until have enough

							var sgCol = $("<td></td>");
							row.append(sgCol);
							sgCol.text($.finalist.getOverallGroup(team));

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
