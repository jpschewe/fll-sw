/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

$(document).ready(function() {
	$("#execute_query").click(function() {
		executeQuery();
	});
}); // end ready function

function executeQuery() {
	var queryString = $.trim($("#query").val());
	if (queryString.length < 1) {
		alert("Query is empty");
		return;
	}

	$("#query_result").empty();
	$("#query_result").append($("<tr><td>Processing Query</td></tr>"));

	$.post('QueryHandler', {
		query : queryString
	}, function(data) {
		populateQueryResult(data);
	});
}

function populateQueryResult(data) {
	var queryResult = $("#query_result");
	queryResult.empty();
	if (data.error) {
		queryResult.append($("<tr class='error'><td>Query error</td><td>"
				+ data.error + "</td></tr>"));
		return;
	}

	var headerRow = $("<tr></tr>");
	$.each(data.columnNames, function(columnIndex, name) {
		headerRow.append($("<th>" + name + "</td>"));
	});
	queryResult.append(headerRow);

	$.each(data.data, function(rowIndex, rowData) {
		var row = $("<tr></tr>");
		$.each(data.columnNames, function(columnIndex, name) {
			var value = rowData[name];
			row.append($("<td>" + value + "</td>"));
		});
		queryResult.append(row);
	});
}