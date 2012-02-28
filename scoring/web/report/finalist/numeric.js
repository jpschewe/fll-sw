/*
 * Copyright (c) 2012INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

$(document).ready(function() {
	$("#division").text($.finalist.getCurrentDivision());

	var searchStart = window.location.search.indexOf("?");
	if(-1 == searchStart) {
		alert("You must visit this page from a link that specifies the category!");
		return;
	}
	var categoryId = parseInt(window.location.search.substring(searchStart + 1), 10);
	var currentCategory = $.finalist.getCategoryById(categoryId);
	if(null == currentCategory) {
		alert("Invalid category ID found: " + categoryId);
		return;
	}
	$("#category-name").text(currentCategory.name);
	
	
	
	$.finalist.displayNavbar();
}); // end ready function
