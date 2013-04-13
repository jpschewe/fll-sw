/*
 * Copyright (c) 2012INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

$(document).ready(
		function() {
			$("#categories").empty();

			$.each($.finalist.getAllCategories(), function(i, category) {
				addCategoryElement(category);
			});

			$.finalist.displayNavbar();
		}); // end ready function

function addCategoryElement(category) {
	var catEle = $("<div></div>");
	$("#categories").append(catEle);

	var checkbox = $("<input type='checkbox'/>");
	checkbox.change(function() {
		if ($(this).attr("checked") == undefined) {
			$.finalist.setCategoryPublic(category, false);
		} else {
			$.finalist.setCategoryPublic(category, true);
		}
	});
	if($.finalist.isCategoryPublic(category)) {
		checkbox.attr("checked", true);
	}
	catEle.append(checkbox);

	var labelEle = $("<label for='name_"
			+ category.catId + "'>" + category.name + "</label>");
	catEle.append(labelEle);
	
}
