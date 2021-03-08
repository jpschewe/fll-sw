/*
 * Copyright (c) 2012 High Tech Kids.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

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
    $.each($.finalist.getAllScheduledCategories(), function(j, category) {
      if ($.finalist.isTeamInCategory(category, team.num)) {
        numFinalists = numFinalists + 1;
      }
    });
    $("#" + getNumFinalistsId(team)).text(numFinalists);
  });
}

function createTeamTable(teams, currentDivision, currentCategory) {
  $.each(teams, function(i, team) {
    if (currentCategory.overall
        || $.finalist.isTeamInDivision(team, currentDivision)) {
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

$(document)
    .ready(
        function() {
          $("#previous")
              .click(
                  function() {
                    var prev = null;
                    var foundCurrent = false;
                    $
                        .each(
                            $.finalist.getNumericCategories(),
                            function(i, category) {
                              if (category.name != $.finalist.CHAMPIONSHIP_NAME) {
                                if ($.finalist.getCurrentCategoryId() == category.catId) {
                                  foundCurrent = true;
                                } // current category
                                if (!foundCurrent) {
                                  prev = category;
                                }
                              } // not championship
                            });

                    if (foundCurrent) {
                      if (null == prev) {
                        location.href = "non-numeric.html";
                      } else {
                        $.finalist.setCurrentCategoryId(prev.catId);
                        location.href = "numeric.html";
                      }
                    } else {
                      var championshipCategory = $.finalist
                          .getCategoryByName($.finalist.CHAMPIONSHIP_NAME);
                      if ($.finalist.getCurrentCategoryId() == championshipCategory.catId) {
                        $.finalist.setCurrentCategoryId(prev.catId);
                        location.href = "numeric.html";
                      }
                    }

                  });

          $("#next")
              .click(
                  function() {
                    var championshipCategory = $.finalist
                        .getCategoryByName($.finalist.CHAMPIONSHIP_NAME);
                    if ($.finalist.getCurrentCategoryId() == championshipCategory.catId) {
                      location.href = "schedule.html";
                    } else {
                      var foundCurrent = false;
                      var next = null;
                      $
                          .each(
                              $.finalist.getNumericCategories(),
                              function(i, category) {
                                if (category.name != $.finalist.CHAMPIONSHIP_NAME) {
                                  if (foundCurrent && null == next) {
                                    next = category;
                                  } else if ($.finalist.getCurrentCategoryId() == category.catId) {
                                    foundCurrent = true;
                                  }
                                }
                              });
                      if (null == next) {
                        $.finalist
                            .setCurrentCategoryId(championshipCategory.catId);
                        location.href = "numeric.html";
                      } else {
                        $.finalist.setCurrentCategoryId(next.catId);
                        location.href = "numeric.html";
                      }
                    }
                  });

          var categoryId = $.finalist.getCurrentCategoryId();
          var currentCategory = $.finalist.getCategoryById(categoryId);
          if (null == currentCategory) {
            alert("Invalid category ID found: " + categoryId);
            return;
          }

          $("#deselect-all").click(function() {
            $(":checkbox").each(function() {
              if ($(this).prop('checked')) {
                $(this).trigger('click');
              }
            });
          });

          $("#reselect").click(
              function() {
                var teams = $.finalist.getAllTeams();
                var scoreGroups = $.finalist.getScoreGroups(teams, division);
                var division = $.finalist.getCurrentDivision();

                $.finalist.unsetCategoryVisited(currentCategory, division);

                $.finalist.initializeTeamsInNumericCategory(division,
                    currentCategory, teams, scoreGroups);
                updatePage();
              });

          $("#category-name").text(currentCategory.name);

          var roomEle = $("#room");
          roomEle.change(function() {
            var roomNumber = roomEle.val();
            $.finalist.setRoom(currentCategory,
                $.finalist.getCurrentDivision(), roomNumber);
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
