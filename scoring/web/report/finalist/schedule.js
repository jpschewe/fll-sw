/*
 * Copyright (c) 2012INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

/**
 * Used for packaging up and sending to the server to put in the database. Needs
 * to match fll.web.report.finalist.FinalistDBRow.
 */
function FinalistDBRow(categoryName, hour, minute, teamNumber) {
	this.categoryName = categoryName;
	this.hour = hour;
	this.minute = minute;
	this.teamNumber = teamNumber;
}

/**
 * Used for packaging up and sending to the server to put in the database. Needs
 * to match fll.web.report.finalist.FinalistCategoryRow.
 */
function FinalistCategoryRow(categoryName, isPublic, room) {
	this.categoryName = categoryName;
	this.isPublic = isPublic;
	this.room = room;
}

$(document).ready(
		function() {
			var currentDivision = $.finalist.getCurrentDivision();
			$("#division").text(currentDivision);

			// output header
			var headerRow = $("<tr></tr>");
			$("#schedule").append(headerRow);

			headerRow.append($("<th>Time Slot</th>"));
			$.each($.finalist.getAllCategories(), function(i, category) {
				var room = $.finalist.getRoom(category, currentDivision);
				var header;
				if (room == undefined || "" == room) {
					header = $("<th>" + category.name + "</th>");
				} else {
					header = $("<th>" + category.name + "<br/>Room: " + room
							+ "</th>");
				}
				headerRow.append(header);
			});

			var schedule = $.finalist.scheduleFinalists();

			var schedRows = [];
			$.each(schedule, function(i, slot) {
				var row = $("<tr></tr>");
				$("#schedule").append(row);

				row.append($("<td>" + slot.time.getHours().toString().padL(2, "0")
						+ ":" + slot.time.getMinutes().toString().padL(2, "0")
						+ "</td>"));

				$.each($.finalist.getAllCategories(), function(i, category) {
					var teamNum = slot.categories[category.catId];
					if (teamNum == null) {
						row.append($("<td>&nbsp;</td>"));
					} else {
						var team = $.finalist.lookupTeam(teamNum);
						var group = team.judgingStation;
						row.append($("<td>" + teamNum + " - " + team.name
								+ " (" + group + ")</td>"));

						var dbrow = new FinalistDBRow(category.name, slot.time
								.getHours(), slot.time.getMinutes(), teamNum);
						schedRows.push(dbrow);
					}
				}); // foreach category

			}); // foreach timeslot

			$('#sched_data').val($.toJSON(schedRows));

			var categoryRows = [];
			$.each($.finalist.getAllCategories(), function(i, category) {
				var cat = new FinalistCategoryRow(category.name,
						category.isPublic, $.finalist.getRoom(category, currentDivision));
				categoryRows.push(cat);
			}); // foreach category
			$('#category_data').val($.toJSON(categoryRows));
			$('#division_data').val(currentDivision);

			$.finalist.displayNavbar();
		});