/*
 * Copyright (c) 2012INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

$(document).ready(
		function() {
			$("#divisions").empty();

			$.each($.finalist.getDivisions(), function(i, division) {
				console.log("Division " + division);
				var selected = "";
				if (division == $.finalist.getCurrentDivision()) {
					selected = " selected ";
				}
				var divisionOption = $("<option value='" + i + "'" + selected
						+ ">" + division + "</option>");
				$("#divisions").append(divisionOption);
			});
			$.finalist.setCurrentDivision($.finalist.getDivisionByIndex($(
					"#divisions").val()));

			$("#divisions").change(function() {
				var divIndex = $(this).val();
				var div = $.finalist.getDivisionByIndex(divIndex);
				$.finalist.setCurrentDivision(div);
			});

			$.finalist.displayNavbar();

		}); // end ready function
