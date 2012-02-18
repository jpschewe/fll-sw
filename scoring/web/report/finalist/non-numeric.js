/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

$(document).ready(function() {
	// FIXME populate with current values
	var savedCategories = [];

	// if(savedCategories.length == 0) {
	// var catIdx = addCategory();
	// addTeam(catIdx);
	// }

	$("#add-category").click(function() {
		addCategory();
	});

}); // end ready function

/**
 * Create the elements for a category
 * 
 * @return the category index
 */
function addCategory() {
	var catEle = $("<li></li>");
	$("#categories").append(catEle);

	var catIdx = $("#categories").children().size() + 1;

	var nameEle = $("<input class='category_name' type='text' id='name_"
			+ catIdx + "'/>");
	catEle.append(nameEle);

	var teamList = $("<ul id='category_" + catIdx + "'></ul>");
	catEle.append(teamList);

	var addButton = $("<button id='add-team_" + catIdx + "'>Add Team</button>");
	catEle.append(addButton);
	addButton.click(function() {
		addTeam(catIdx);
	});

	addTeam(catIdx);
	return catIdx;
}

function teamNumId(category, teamIdx) {
	return "num_" + category + "_" + teamIdx;
}

function teamNameId(category, teamIdx) {
	return "name_" + category + "_" + teamIdx;
}

function teamOrgId(category, teamIdx) {
	return "org_" + category + "_" + teamIdx;
}

/**
 * Add a new team element to the category.
 * 
 * @return the index for the team which can be used to populate the elements
 *         later
 */
function addTeam(category) {
	var catEle = $("#category_" + category);
	var teamIdx = catEle.children().size() + 1;

	var teamEle = $("<li></li>");
	catEle.append(teamEle);

	var numEle = $("<input type='text' id='" + teamNumId(category, teamIdx)
			+ "'/>");
	teamEle.append(numEle);
	teamEle.change(function() {
		var teamNum = $("#" + teamNumId(category, teamIdx)).val();
		var team = $.finalists.lookupTeam(teamNum);
		if (typeof (team) == 'undefined') {
			alert("Team number " + teamNum + " does not exist");
		} else {
			$("#" + teamNameId(category, teamIdx)).val(team.name);
			$("#" + teamOrgId(category, teamIdx)).val(team.org);
		}
	});

	var nameEle = $("<input id='" + teamNameId(category, teamIdx)
			+ "' readonly/>");
	teamEle.append(nameEle);
	var orgEle = $("<input id='" + teamOrgId(category, teamIdx)
			+ "' readonly/>");
	teamEle.append(orgEle);

	return teamIdx;
}

/*
 * var num = document.getElementById(num_field).value var name =
 * $.finalists.lookup_name(num); if(typeof(name) != 'undefined') {
 * document.getElementById(name_field).value = name; } else {
 * document.getElementById(name_field).value = ''; alert("Team number " + num + "
 * does not exist"); }
 */
