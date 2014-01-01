/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

$(document).on(
		"pagecreate",
		function() {

			/*FIXME
			var categories = $.subjective.getSubjectiveCategories();
			$.each(categories, function(i, category) {
				var button = $("<button class='ui-btn ui-corner-all'>" + category.title
						+ "</button>");
				$("#categories").append(button);
				button.click(function() {
					selectCategory(category);
				});

			});
			*/
			
			var currentJudgingGroup = $.subjective.getCurrentJudgingGroup();
			$("#judging-group").text(currentJudgingGroup);

			var currentCategory = $.subjective.getCurrentCategory();
			$("#category").text(currentCategory.title);

		});
