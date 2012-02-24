/*
 * Copyright (c) 2012INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

$(document).ready(
		function() {
			$("#division").text($.finalist.getCurrentDivisionName());
			
			$("#categories").empty();

			$.each($.finalist.getNonNumericCategories(), function(i, category) {
				console.log("Category name is " + category.name + " id: "
						+ category.catId);
				addCategoryElement(category);
				addedCategory = true;

				var addedTeam = false;
				$.each(category.teams, function(j, teamNum) {
					console.log("  Team : " + teamNum);
					addedTeam = true;
					var teamIdx = addTeam(category);
					var team = $.finalist.lookupTeam(teamNum);
					populateTeamInformation(category, teamIdx, team);

				});
				if (!addedTeam) {
					addTeam(category);
				}
			});

			$("#add-category").click(function() {
				addCategory();
			});

		}); // end ready function

function addCategoryElement(category) {
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
	nameEle.val(category.name);

	var teamList = $("<ul id='category_" + category.catId + "'></ul>");
	catEle.append(teamList);

	var addButton = $("<button id='add-team_" + category.catId
			+ "'>Add Team</button>");
	catEle.append(addButton);
	addButton.click(function() {
		addTeam(category);
	});
}

/**
 * Add a new empty category to the page.
 * 
 * @return the category index
 */
function addCategory() {
	var category = $.finalist.addCategory("", false);
	if (null == category) {
		return;
	}

	addCategoryElement(category);

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

function populateTeamInformation(category, teamIdx, team) {
	$("#" + teamNumId(category.catId, teamIdx)).val(team.num);
	$("#" + teamNumId(category.catId, teamIdx)).data('oldVal', team.num);
	$("#" + teamNameId(category.catId, teamIdx)).val(team.name);
	$("#" + teamOrgId(category.catId, teamIdx)).val(team.org);
}

/**
 * Add a new empty team to the page for the specified category
 * 
 * @return the index for the team which can be used to populate the elements
 *         later
 */
function addTeam(category) {
	var catEle = $("#category_" + category.catId);
	var teamIdx = catEle.children().size() + 1;

	var teamEle = $("<li></li>");
	catEle.append(teamEle);

	var numEle = $("<input type='text' id='"
			+ teamNumId(category.catId, teamIdx) + "'/>");
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
				populateTeamInformation(category, teamIdx, team);
				$.finalist.addTeamToCategory(category, teamNum);
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
