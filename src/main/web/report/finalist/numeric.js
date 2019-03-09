/*
 * Copyright (c) 2012 High Tech Kids.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

function handleDivisionChange() {
  var divIndex = $("#divisions").val();
  var div = $.finalist.getDivisionByIndex(divIndex);
  $.finalist.setCurrentDivision(div);
  updatePage();
}

function getNumFinalistsId(team) {
  return "num_finalists_" + team.num;
}

function initializeFinalistCounts(teams) {
  $.each(teams, function(i, team) {
    // initialize to 0
    var numFinalists = 0;
    $.each($.finalist.getAllCategories(), function(j, category) {
      if ($.finalist.isTeamInCategory(category, team.num)) {
        numFinalists = numFinalists + 1;
      }
    });
    $("#" + getNumFinalistsId(team)).text(numFinalists);
  });
}

function createTeamTable(teams, currentDivision, currentCategory) {
  $.each(teams, function(i, team) {
    if ($.finalist.isTeamInDivision(team, currentDivision)) {
      var row = $("<tr></tr>");
      $("#data").append(row);

      var finalistCol = $("<td></td>");
      row.append(finalistCol);
      var finalistCheck = $("<input type='checkbox'/>");
      finalistCol.append(finalistCheck);
      finalistCheck.change(function() {
        var finalistDisplay = $("#" + getNumFinalistsId(team));
        var numFinalists = parseInt(finalistDisplay.text(), 10);
        if ($(this).prop("checked")) {
          $.finalist.addTeamToCategory(currentCategory, team.num);
          numFinalists = numFinalists + 1;
        } else {
          $.finalist.removeTeamFromCategory(currentCategory, team.num);
          numFinalists = numFinalists - 1;
        }
        finalistDisplay.text(numFinalists);
      });
      if ($.finalist.isTeamInCategory(currentCategory, team.num)) {
        finalistCheck.attr("checked", true);
      }

      var sgCol = $("<td></td>");
      row.append(sgCol);
      var group = team.judgingGroup;
      sgCol.text(group);

      var numCol = $("<td></td>");
      row.append(numCol);
      numCol.text(team.num);

      var nameCol = $("<td></td>");
      row.append(nameCol);
      nameCol.text(team.name);

      var scoreCol = $("<td></td>");
      row.append(scoreCol);
      scoreCol.text($.finalist.getCategoryScore(team, currentCategory));

      var numFinalistCol = $("<td id='" + getNumFinalistsId(team) + "'></td>");
      row.append(numFinalistCol);
    } // in correct division
  }); // build data for each team
}

function updatePage() {
  var categoryId = $.finalist.getCurrentCategoryId();
  var currentCategory = $.finalist.getCategoryById(categoryId);
  if (null == currentCategory) {
    alert("Invalid category ID found: " + categoryId);
    return;
  }

  // note that this category has been visited so that it
  // doesn't get initialized again
  $.finalist.setCategoryVisited(currentCategory, $.finalist
      .getCurrentDivision());

  $("#data").empty();

  var headerRow = $("<tr><th>Finalist?</th><th>Judging Group</th><th>Team #</th><th>Team Name</th><th>Score</th><th>Num Categories</th></tr>");
  $("#data").append(headerRow);

  var teams = $.finalist.getAllTeams();
  $.finalist.sortTeamsByCategory(teams, currentCategory);

  createTeamTable(teams, $.finalist.getCurrentDivision(), currentCategory);

  initializeFinalistCounts(teams);
}

$(document).ready(
    function() {
      var categoryId = $.finalist.getCurrentCategoryId();
      var currentCategory = $.finalist.getCategoryById(categoryId);
      if (null == currentCategory) {
        alert("Invalid category ID found: " + categoryId);
        return;
      }

      $("#category-name").text(currentCategory.name);

      var roomEle = $("#room");
      roomEle.change(function() {
        var roomNumber = roomEle.val();
        $.finalist.setRoom(currentCategory, $.finalist.getCurrentDivision(),
            roomNumber);
      });
      roomEle.val($.finalist.getRoom(currentCategory, $.finalist
          .getCurrentDivision()));

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

      updatePage();

      $.finalist.displayNavbar();
    }); // end ready function
