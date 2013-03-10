/*
 * Copyright (c) 2012INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

String.repeat = function(chr, count) {
	var str = "";
	for ( var x = 0; x < count; x++) {
		str += chr
	}
	;
	return str;
}

String.prototype.padL = function(width, pad) {
	if (!width || width < 1)
		return this;

	if (!pad)
		pad = " ";
	var length = width - this.length
	if (length < 1)
		return this.substr(0, width);

	return (String.repeat(pad, length) + this).substr(0, width);
}

/**
 * Used for packaging up and sending to the server to put in the database.
 */
function FinalistDBRow(categoryName, hour, minute, teamNumber) {
	this.categoryName = categoryName;
	this.hour = hour;
	this.minute = minute;
	this.teamNumber = teamNumber;
}

$(document).ready(
		function() {
			$("#division").text($.finalist.getCurrentDivision());

			// output header
			var headerRow = $("<tr></tr>");
			$("#schedule").append(headerRow);

			headerRow.append($("<th>Time Slot</th>"));
			$.each($.finalist.getAllCategories(), function(i, category) {
				var header = $("<th>" + category.name + "</th>");
				headerRow.append(header);
			});

			var duration = $.finalist.getDuration();
			var time = $.finalist.getStartTime();
			var schedule = $.finalist.scheduleFinalists();
			
			var schedRows = [];			
			$.each(schedule, function(i, slot) {
				var row = $("<tr></tr>");
				$("#schedule").append(row);

				row.append($("<td>" + time.getHours().toString().padL(2, "0")
						+ ":" + time.getMinutes().toString().padL(2, "0")
						+ "</td>"));

				$.each($.finalist.getAllCategories(),
						function(i, category) {
							var teamNum = slot.categories[category.catId]
							if (teamNum == null) {
								row.append($("<td>&nbsp;</td>"));
							} else {
								var team = $.finalist.lookupTeam(teamNum);
								var group = $.finalist.getCategoryGroup(team,
										category);
								row.append($("<td>" + teamNum + " - "
										+ team.name + " (" + group + ")</td>"));
								
								var dbrow = new FinalistDBRow(category.name, time.getHours(), time.getMinutes(), teamNum);
								schedRows.push(dbrow);
							}
						}); // foreach category

				time.setTime(time.getTime() + (duration * 60 * 1000));
			}); // foreach timeslot

			$('#sched_data').val($.toJSON(schedRows));

			$.finalist.displayNavbar();
		});