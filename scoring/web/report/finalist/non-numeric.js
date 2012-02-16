/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

$(document).ready(function() {
    //FIXME populate with current values
    
    $("#add-category").click(function() {
	//FIXME need to add category entry here
	//var cat_element = $("
    });

    
}); // end ready function

function addTeam(category) {
    var catEle = $("#category_" + category);
    var teamIdx = catEle.children().size() + 1;
    
    var teamEle = $("<li class='team_" + category + "'>" //
		    + "<input type='text' id='num_" + category + "_" + teamIdx + "' onblur='lookup(1,2)'/>" //
		    + "<input id='name_" + category + "_" + teamIdx + "' readonly/>" //
		    + "<input id='org_" + category + "_" + teamIdx + "' readonly/>" //
		    + "</li>");
    catEle.append(teamEle);
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
