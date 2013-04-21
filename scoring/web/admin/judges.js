/*
 * Copyright (c) 2012INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

$(document).ready(function() {

	$("#total_num_rows").val(maxIndex);

	$("#add_rows").click(function() {
		var numRowsStr = $("#num_rows").val();
		var numRows = 1;
		if ($.isNumeric(numRowsStr)) {
			numRows = parseInt($("#num_rows").val());
		}

		addRows(numRows);

		// keep form from submitting
		return false;
	});

	var numCategories = 0;
	$.each(categories, function(catId, catName) {
		numCategories = numCategories+1;
	});

	var missingRows = (judging_stations.length * numCategories) - maxIndex;
	if (missingRows > 0) {
		addRows(missingRows);
	}

}); // end ready function

function addRows(numRows) {
	var rowIndex;
	for (rowIndex = 0; rowIndex < numRows; ++rowIndex) {
		var judgeIdx = maxIndex + 1;
		maxIndex = judgeIdx;
		$("#total_num_rows").val(maxIndex);

		var row = $("<tr></tr>");
		$("#data").append(row);

		var idCol = $("<td></td>");
		row.append(idCol);
		var id = $("<input type='text' name='id" + judgeIdx + "'>");
		idCol.append(id);

		var categoryCol = $("<td></td>");
		row.append(categoryCol);
		var category = $("<select name='cat" + judgeIdx + "'></select>");
		categoryCol.append(category);
		$.each(categories, function(catId, catName) {
			var option = $("<option value='" + catId + "'>" + catName
					+ "</option>");
			category.append(option);
		});

		var stationCol = $("<td></td>");
		row.append(stationCol);
		var station = $("<select name='station" + judgeIdx + "'></select>");
		stationCol.append(station);
		$.each(judging_stations, function(i, stationName) {
			var option = $("<option value='" + stationName + "'>" + stationName
					+ "</option>");
			station.append(option);
		});

	}
}