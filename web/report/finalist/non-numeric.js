/*
 * Copyright (c) 2012INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

function handleDivisionChange() {
  var divIndex = $("#divisions").val();
  var div = $.finalist.getDivisionByIndex(divIndex);
  $.finalist.setCurrentDivision(div);
  updateTeams();
}

$(document).ready(
    function() {

      $("#divisions").empty();
      $.each($.finalist.getDivisions(), function(i, division) {
        var selected = "";
        if (division == $.finalist.getCurrentDivision()) {
          selected = " selected ";
        }
        var divisionOption = $("<option value='" + i + "'" + selected + ">"
            + division + "</option>");
        $("#divisions").append(divisionOption);
      }); // foreach division
      $("#divisions").change(function() {
        handleDivisionChange();
      });
      handleDivisionChange();

      updateTeams();

      $("#add-category").click(function() {
        addCategory();
      });

      $.finalist.displayNavbar();
    }); // end ready function

function updateTeams() {
  $("#categories").empty();
  $.each($.finalist.getNonNumericCategories(), function(i, category) {
    addCategoryElement(category);
    addedCategory = true;

    var addedTeam = false;
    $.each(category.teams, function(j, teamNum) {
      var team = $.finalist.lookupTeam(teamNum);
      if ($.finalist.isTeamInDivision(team, $.finalist.getCurrentDivision())) {
        addedTeam = true;
        var teamIdx = addTeam(category);
        populateTeamInformation(category, teamIdx, team);
      }

    });
    if (!addedTeam) {
      addTeam(category);
    }
  });
}

function checkCategoryEmpty(category) {
  return category.teams.length == 0;
}

function addCategoryElement(category) {
  var catEle = $("<li></li>");
  $("#categories").append(catEle);

  var nameEle = $("<input class='category_name' type='text' id='name_"
      + category.catId + "'/>");
  catEle.append(nameEle);
  nameEle.change(function() {
    var newName = nameEle.val();
    if (null == newName || "" == newName) {
      alert("All categories must have non-empty names");
      nameEle.val(category.name);
    }
    if (!$.finalist.setCategoryName(category, newName)) {
      alert("There already exists a category with the name '" + newName + "'");
      nameEle.val(category.name);
    }
  });
  nameEle.val(category.name);

  catEle.append("Room number: ");
  var roomEle = $("<input type='text' id='room_" + category.catId + "'/>");
  catEle.append(roomEle);
  roomEle.change(function() {
    var roomNumber = roomEle.val();
    $.finalist.setRoom(category, $.finalist.getCurrentDivision(), roomNumber);
  });
  roomEle.val($.finalist.getRoom(category, $.finalist.getCurrentDivision()));

  var deleteButton = $("<button id='delete_" + category.catId + "'>Delete</button>");
  catEle.append(deleteButton);
  deleteButton.click(function() {
    // check all teams being empty
    var categoryIsEmpty = checkCategoryEmpty(category);
    if(!categoryIsEmpty) {
      var reallyDelete = confirm("Are you sure you want to delete category " + category.name + "?");
      if(reallyDelete) {
        $.finalist.removeCategory(category);
        catEle.remove();
      }
    } else {
      $.finalist.removeCategory(category);
      catEle.remove();
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
  $.finalist.setCategoryName(category, "Category " + category.catId);

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

function teamJudgingStationId(category, teamIdx) {
  return "judgingStation_" + category + "_" + teamIdx;
}

function teamDeleteId(category, teamIdx) {
  return "delete_" + category + "_" + teamIdx;
}

function populateTeamInformation(category, teamIdx, team) {
  $("#" + teamNumId(category.catId, teamIdx)).val(team.num);
  $("#" + teamNumId(category.catId, teamIdx)).data('oldVal', team.num);
  $("#" + teamNameId(category.catId, teamIdx)).val(team.name);
  $("#" + teamOrgId(category.catId, teamIdx)).val(team.org);
  $("#" + teamJudgingStationId(category.catId, teamIdx)).val(
      team.judgingGroup);
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

  var judgingStationEle = $("<input id='"
      + teamJudgingStationId(category.catId, teamIdx) + "' readonly/>");
  teamEle.append(judgingStationEle);
  
  var deleteButton = $("<button id='" + teamDeleteId(category.catId, teamIdx) + "'>Delete</button>");
  teamEle.append(deleteButton);
  deleteButton.click(function() {
    var teamNum = numEle.val();
    if("" != teamNum) {
      var reallyDelete = confirm("Are you sure you want to delete this team?");
      if(reallyDelete) {
        $.finalist.removeTeamFromCategory(category, teamNum);
        teamEle.remove();
      }
    } else {
      $.finalist.removeTeamFromCategory(category, teamNum);
      teamEle.remove();
    }
  });

  return teamIdx;
}
