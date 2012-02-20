/*
 * Copyright (c) 2012INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

$(document).ready(function() {
	$.each($.finalist)
	// FIXME populate with current values

	// if(savedCategories.length == 0) {
	// var category = addCategory();
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
	var category = $.finalist.addCategory("");
	if (null == category) {
		return;
	}

	var catEle = $("<li></li>");
	$("#categories").append(catEle);

	var nameEle = $("<input class='category_name' type='text' id='name_"
			+ category.catId + "'/>");
	catEle.append(nameEle);
	nameEle.change(function() {
		var newName = nameEle.val();
		if (!$.finalist.setCategoryName(category, newName)) {
			alert("There already exists a category with the name '" + newName
					+ "'");
			nameEle.val(category.name);
		}
	});

	var teamList = $("<ul id='category_" + category.catId + "'></ul>");
	catEle.append(teamList);

	var addButton = $("<button id='add-team_" + category.catId
			+ "'>Add Team</button>");
	catEle.append(addButton);
	addButton.click(function() {
		addTeam(category);
	});

	addTeam(category);
	return category;
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
	var catEle = $("#category_" + category.catId);
	var teamIdx = catEle.children().size() + 1;

	var teamEle = $("<li></li>");
	catEle.append(teamEle);

	var numEle = $("<input type='text' id='" + teamNumId(category.catId, teamIdx)
			+ "'/>");
	teamEle.append(numEle);
	numEle.change(function() {
		var teamNum = $(this).val();
		var prevTeam = $(this).data('oldVal');
		if ("" == teamNum) {
			$.finalist.removeTeamFromCategory(category, prevTeam);
			$("#" + teamNameId(category.catId, teamIdx)).val("");
			$("#" + teamOrgId(category.catId, teamIdx)).val("");
		} else {
			var team = $.finalist.lookupTeam(teamNum);
			if (typeof (team) == 'undefined') {
				alert("Team number " + teamNum + " does not exist");
				$(this).val(prevTeam);
				teamNum = prevTeam; // for the set of oldVal below
			} else {
				$("#" + teamNameId(category.catId, teamIdx)).val(team.name);
				$("#" + teamOrgId(category.catId, teamIdx)).val(team.org);
				$.finalist.addTeamToCategory(teamNum);
			}
		}
		$(this).data('oldVal', teamNum);
	});

	var nameEle = $("<input id='" + teamNameId(category.catId, teamIdx)
			+ "' readonly/>");
	teamEle.append(nameEle);
	var orgEle = $("<input id='" + teamOrgId(category.catId, teamIdx)
			+ "' readonly/>");
	teamEle.append(orgEle);

	return teamIdx;
}
