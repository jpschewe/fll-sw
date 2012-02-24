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
				var divisionOption = $("<option value='" + i + "'>"
						+ division + "</option>");
				$("#divisions").append(divisionOption);
			});
			$.finalist.setCurrentDivisionIndex($("#divisions").val());
			
			$("#divisions").change(function() {
				var div = $(this).val();
				$.finalist.setCurrentDivisionIndex(div);
			});

			$.finalist.displayNavbar();
			
		}); // end ready function
