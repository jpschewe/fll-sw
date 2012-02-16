/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

$(document).ready(function() {
    //FIXME populate with current values
    
    $("#add-category").click(function() {
	var catEle = $("<li></li>");
	$("#categories").append(catEle);
	
	var catIdx = $("#categories").children().size() + 1;
	
	var nameEle = $("<input class='category_name' type='text' id='name_" + catIdx + "'/>");
	catEle.append(nameEle);
	
	var teamList = $("<ul id='category_" + catIdx + "'></ul>");
	catEle.append(teamList);

	var addButton = $("<button id='add-team_" + catIdx + "'>Add Team</button>");
	catEle.append(addButton);
	addButton.click(function() {
	    addTeam(catIdx);
	});

    });

    
}); // end ready function

/**
 * Add a new team element to the category.
 *
 * @return the index for the team which can be used to populate the elements later
 */
function addTeam(category) {
    var catEle = $("#category_" + category);
    var teamIdx = catEle.children().size() + 1;
    
    var teamEle = $("<li class='team_" + category + "'></li>");
    catEle.append(teamEle);
    
    var numEle = $("<input type='text' id='num_" + category + "_" + teamIdx + "'/>");
    teamEle.append(numEle);
    //FIXME
    //teamEle.focusLost(function() {
    //    alert("Here");
    //});
    
    var nameEle = $("<input id='name_" + category + "_" + teamIdx + "' readonly/>");
    teamEle.append(nameEle);
    var orgEle = $("<input id='org_" + category + "_" + teamIdx + "' readonly/>");
    teamEle.append(orgEle);
    
    
    return teamIdx;
}

/*
var num = document.getElementById(num_field).value
var name = $.finalists.lookup_name(num);
if(typeof(name) != 'undefined') {
    document.getElementById(name_field).value = name;
} else {
    document.getElementById(name_field).value = '';
    alert("Team number " + num + " does not exist");
}
*/
