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

$(document).ready(
		function() {
			$("#division").text($.finalist.getCurrentDivisionName());

			// output header
			var headerRow = $("<tr></tr>");
			$("#schedule").append(headerRow);

			headerRow.append($("<th>Time Slot</th>"));
			$.each($.finalist.getAllCategories(), function(i, category) {
				var header = $("<th>" + category.name + "</th>");
				headerRow.append(header);
			});

			// FIXME duration, startHour, startMinute needs to be specified up
			// front
			var duration = 20;
			var startHour = 14;
			var startMinute = 0;

			var time = new Date();
			time.setHours(startHour);
			time.setMinutes(startMinute);
			var schedule = $.finalist.scheduleFinalists();
			$.each(schedule, function(i, slot) {
				var row = $("<tr></tr>");
				$("#schedule").append(row);

				row.append($("<td>" + time.getHours().toString().padL(2, "0")
						+ ":" + time.getMinutes().toString().padL(2, "0")
						+ "</td>"));

				$.each($.finalist.getCategories(), function(i, category) {
					var teamNum = slot.categories[category.catId]
					if (teamNum == null) {
						row.append($("<td>&nbsp;</td>"));
					} else {
						row.append($("<td>" + teamNum + "</td>"));
					}
				}); // foreach category

				time.setTime(time.getTime() + (duration * 60 * 1000));
			}); // foreach timeslot

			$.finalist.displayNavbar();
		});