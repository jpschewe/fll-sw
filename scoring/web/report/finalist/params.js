/*
 * Copyright (c) 2012INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

$(document).ready(
		function() {
			$.finalist.setupAppCache();

			$("#divisions").empty();

			var teams = $.finalist.getAllTeams();

			$.each($.finalist.getDivisions(), function(i, division) {
				var selected = "";
				if (division == $.finalist.getCurrentDivision()) {
					selected = " selected ";
				}
				var divisionOption = $("<option value='" + i + "'" + selected
						+ ">" + division + "</option>");
				$("#divisions").append(divisionOption);

				// initialize categories with the auto selected teams
				var scoreGroups = $.finalist.getScoreGroups(teams, division);

				$.each($.finalist.getNumericCategories(),
						function(i, category) {
							$.finalist.initializeTeamsInCategory(division,
									category, teams, scoreGroups);
						});// foreach numeric category
			}); // foreach division

			$.finalist.setCurrentDivision($.finalist.getDivisionByIndex($(
					"#divisions").val()));

			$("#hour").val($.finalist.getStartHour());
			$("#minute").val($.finalist.getStartMinute());
			$("#duration").val($.finalist.getDuration());

			$("#hour").change(function() {
				var hour = parseInt($(this).val(), 10);
				if (isNaN(hour)) {
					alert("Hour must be an integer");
					$("#hour").val($.finalist.getStartHour());
				} else {
					$.finalist.setStartHour(hour);
				}
			});

			$("#minute").change(function() {
				var minute = parseInt($(this).val(), 10);
				if (isNaN(minute)) {
					alert("Minute must be an integer");
					$("#minute").val($.finalist.getStartMinute());
				} else {
					$.finalist.setStartMinute(minute);
				}
			});

			$("#duration").change(function() {
				var duration = parseInt($(this).val(), 10);
				if (isNaN(duration)) {
					alert("Duration must be an integer");
					$("#duration").val($.finalist.getDuration());
				} else {
					$.finalist.setDuration(duration);
				}
			});

			$("#divisions").change(function() {
				var divIndex = $(this).val();
				var div = $.finalist.getDivisionByIndex(divIndex);
				$.finalist.setCurrentDivision(div);
			});

			$.finalist.displayNavbar();

		}); // end ready function
