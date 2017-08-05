/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

var draggingTeam = null;
var draggingCategory = null;
var draggingTeamDiv = null;
var schedule = null;
/**
 * Mapping of team numbers to categories.
 */
var finalistsCount = {};

/**
 * Map time string to map of category id to div cell.
 */
var timeToCells = {};

/**
 * Find the cell for the specified time and category id. This does a lookup by
 * comparing the times, so slots added later are ok as long as a cell has been
 * added for the specified time.
 * 
 * @param searchTime
 *          the time to search for
 * @param serachCatId
 *          the category id to search for
 * @return the cell or null if not found
 */
function getCellForTimeAndCategory(searchTime, searchCatId) {
  var searchTimeStr = $.finalist.timeToDisplayString(searchTime);

  var foundCell = null;

  $.each(timeToCells, function(timeStr, categoryToCells) {
    if (searchTimeStr == timeStr) {

      $.each(categoryToCells, function(catId, cell) {
        if (catId == searchCatId) {
          foundCell = cell;
        }
      });

    }
  });

  return foundCell;
}

function handleDivisionChange() {
  var divIndex = $("#divisions").val();
  var div = $.finalist.getDivisionByIndex(divIndex);
  $.finalist.setCurrentDivision(div);
  updatePage();
}

function updateHeader() {
  var headerRow = $("#schedule_header");
  headerRow.empty();

  headerRow.append($("<div class='rTableHead'>Time Slot</div>"));

  headerRow.append($("<div class='rTableHead'>Head to Head</div>"));

  $.each($.finalist.getAllCategories(), function(i, category) {
    var room = $.finalist.getRoom(category, $.finalist.getCurrentDivision());
    var header;
    if (room == undefined || "" == room) {
      header = $("<div class='rTableHead'>" + category.name + "</div>");
    } else {
      header = $("<div class='rTableHead'>" + category.name + "<br/>Room: "
          + room + "</div>");
    }
    headerRow.append(header);
  });
}

/**
 * Create the div element for a team in a particular category.
 * 
 * @param team
 *          a Team object
 * @param category
 *          a Category object
 * @returns a jquery div object
 */
function createTeamDiv(team, category) {
  var teamCategories = finalistsCount[team.num];
  var numCategories = teamCategories.length;
  var group = team.judgingGroup;
  var teamDiv = $("<div draggable='true'>" + team.num + " - " + team.name
      + " (" + group + ", " + numCategories + ")</div>");

  teamDiv.on('dragstart', function(e) {
    var rawEvent;
    if (e.originalEvent) {
      rawEvent = e.originalEvent;
    } else {
      rawEvent = e;
    }
    // rawEvent.target is the source node.

    $(teamDiv).css('opacity', '0.4');

    var dataTransfer = rawEvent.dataTransfer;

    draggingTeam = team;
    draggingCategory = category;
    draggingTeamDiv = teamDiv;

    dataTransfer.effectAllowed = 'move';

    // need something to transfer, otherwise the browser won't let us drag
    dataTransfer.setData('text/text', "true");
  });
  teamDiv.on('dragend', function(e) {
    // rawEvent.target is the source node.
    $(teamDiv).css('opacity', '1');
  });

  return teamDiv;
}

/**
 * 
 * @param slot
 *          Timeslot object
 * @param category
 *          Category object
 * @returns jquery div object
 */
function createTimeslotCell(slot, category) {
  var cell = $("<div class='rTableCell'></div>");

  cell.on('dragover', function(e) {
    var rawEvent;
    if (e.originalEvent) {
      rawEvent = e.originalEvent;
    } else {
      rawEvent = e;
    }

    if (category == draggingCategory) {
      if (rawEvent.preventDefault) {
        rawEvent.preventDefault(); // Necessary. Allows us to drop.
      }

      rawEvent.dataTransfer.dropEffect = 'move'; // See the section on the
      // DataTransfer object.

      var transferObj = rawEvent.dataTransfer
          .getData('application/x-fll-finalist');

      return false;
    } else {
      return true;
    }
  });

  cell.on('drop', function(e) {
    var rawEvent;
    if (e.originalEvent) {
      rawEvent = e.originalEvent;
    } else {
      rawEvent = e;
    }

    // rawEvent.target is current target element.

    if (rawEvent.stopPropagation) {
      rawEvent.stopPropagation(); // Stops some browsers from redirecting.
    }

    if (cell.children().length > 0) {
      // move current team to old parent
      var oldTeamDiv = cell.children().first();
      var draggingParent = draggingTeamDiv.parent();
      draggingParent.append(oldTeamDiv);
    }

    // Add team to the current cell
    cell.append(draggingTeamDiv);

    // updates the schedule
    moveTeam(draggingTeam, draggingCategory, slot);

    draggingTeam = null;
    draggingCategory = null;
    draggingTeamDiv = null;
  });

  return cell;
}

/**
 * Convert schedule to the JSON required to send to the server.
 */
function updateScheduleToSend() {

  var schedRows = [];
  $.each(schedule, function(i, slot) {
    $.each($.finalist.getAllCategories(), function(i, category) {
      var teamNum = slot.categories[category.catId];
      if (teamNum != null) {
        var dbrow = new FinalistDBRow(category.name, slot.time.hour,
            slot.time.minute, teamNum);
        schedRows.push(dbrow);
      }
    }); // foreach category

  }); // foreach timeslot

  $('#sched_data').val($.toJSON(schedRows));
}

/**
 * Move a team from it's current timeslot to the newly specified timeslot for
 * the specified category. If the specified timeslot contains another team, then
 * the two teams are swapped.
 * 
 * @param team
 *          a Team object to move
 * @param category
 *          a Category object that the team is moving in
 * @param newSlot
 *          the new slot to put the team in
 */
function moveTeam(team, category, newSlot) {
  var destSlot = null;
  var srcSlot = null;

  // remove team from all slots with this category
  $.each(schedule, function(i, slot) {
    var foundTeam = false;
    $.each(slot.categories, function(categoryId, teamNumber) {
      if (categoryId == category.catId && teamNumber == team.num) {
        foundTeam = true;
      }
    }); // foreach category

    if ($.finalist.compareTimes(slot.time, newSlot.time) == 0) {
      destSlot = slot;
    }

    if (foundTeam) {
      srcSlot = slot;
    }
  }); // foreach timeslot

  if (null == destSlot) {
    alert("Internal error, can't find destination slot in schedule");
    return;
  }

  if ($.finalist.compareTimes(srcSlot.time, destSlot.time) == 0) {
    // dropping on the same slot where the team already was
    return;
  }

  // remove warning from source cell as it may become empty
  var srcCell = getCellForTimeAndCategory(srcSlot.time, category.catId);
  srcCell.removeClass("overlap-schedule");
  srcCell.removeClass("overlap-playoff");

  if (null == destSlot.categories[category.catId]) {
    // no team in the destination, just delete this team from the old slot
    delete srcSlot.categories[category.catId];
  } else {
    var oldTeamNumber = destSlot.categories[category.catId];
    var oldTeam = $.finalist.lookupTeam(oldTeamNumber);

    // clear the destination slot so that the warning check sees the correct
    // state for this team
    delete destSlot.categories[category.catId];

    // move team to the source slot
    srcSlot.categories[category.catId] = oldTeamNumber;

    // check new position to set warnings
    checkForTimeOverlap(srcSlot, oldTeamNumber);

    // check old position to clear warnings
    checkForTimeOverlap(destSlot, oldTeamNumber);

    if ($.finalist.hasPlayoffConflict(oldTeam, srcSlot)) {
      srcCell.addClass("overlap-playoff");
    }
  }

  // add team to new slot, reference actual slot in case
  // newSlot and destSlot are not the same instance.
  // 12/23/2015 JPS - not sure how this could happen, but I must have thought it
  // possible.
  destSlot.categories[category.catId] = team.num;

  // check where the team now is to see what warnings are needed
  checkForTimeOverlap(destSlot, team.num);

  // need to check where the team was to clear the warning
  checkForTimeOverlap(srcSlot, team.num);

  $.finalist.setSchedule($.finalist.getCurrentDivision(), schedule);
}

/**
 * See if the specified team is in the same slot in multiple categories or has a
 * conflict with playoff times.
 * 
 * @param slot
 *          the slot to check
 * @param teamNumber
 *          the team number to check for
 */
function checkForTimeOverlap(slot, teamNumber) {
  var team = $.finalist.lookupTeam(teamNumber);

  var numCategories = 0;
  $.each(slot.categories, function(categoryId, checkTeamNumber) {
    if (checkTeamNumber == teamNumber) {
      numCategories = numCategories + 1;
    }
  });

  var hasPlayoffConflict = $.finalist.hasPlayoffConflict(team, slot);

  $.each(slot.categories, function(categoryId, checkTeamNumber) {
    if (checkTeamNumber == teamNumber) {
      var cell = getCellForTimeAndCategory(slot.time, categoryId);
      if (null != cell) {
        if (numCategories > 1) {
          cell.addClass('overlap-schedule');
          /*
           * alert("Found " + teamNumber + " in multiple categories at the same
           * time");
           */
        } else {
          cell.removeClass('overlap-schedule');
        }

        if (hasPlayoffConflict) {
          cell.addClass('overlap-playoff');
        } else {
          cell.removeClass('overlap-playoff');
        }

      } // found cell
      else {
        alert("Can't find cell for " + time.hour + ":" + time.minute + " cat: "
            + categoryId);
        return;
      }
    } // team number matches
  });

}

/**
 * Add a row to the schedule table for the specified slot.
 */
function addRowForSlot(slot) {
  var row = $("<div class='rTableRow'></div>");
  $("#schedule_body").append(row);

  row.append($("<div class='rTableCell'>"
      + $.finalist.timeToDisplayString(slot.time) + "</div>"));

  var playoffCell = $("<div class='rTableCell'></div>");
  row.append(playoffCell);
  var first = true;
  $.each($.finalist.getPlayoffDivisions(), function(index, playoffDivision) {
    if ($.finalist.slotHasPlayoffConflict(playoffDivision, slot)) {
      if (first) {
        first = false;
      } else {
        playoffCell.append(",");
      }
      playoffCell.append(playoffDivision);
    }
  });

  var categoriesToCells = {};
  var teamsInSlot = {};
  $.each($.finalist.getAllCategories(), function(i, category) {
    var cell = createTimeslotCell(slot, category);
    row.append(cell);

    categoriesToCells[category.catId] = cell;

    var teamNum = slot.categories[category.catId];
    if (teamNum != null) {
      var team = $.finalist.lookupTeam(teamNum);
      var teamDiv = createTeamDiv(team, category);
      cell.append(teamDiv);
      teamsInSlot[teamNum] = true;
    }
  }); // foreach category

  timeToCells[$.finalist.timeToDisplayString(slot.time)] = categoriesToCells;

  // now check for overlaps in the loaded schedule
  $.each(teamsInSlot, function(teamNum, ignore) {
    checkForTimeOverlap(slot, teamNum);
  });
}

function updatePage() {

  // output header
  updateHeader();

  schedule = $.finalist.getSchedule($.finalist.getCurrentDivision());
  finalistsCount = $.finalist.getTeamToCategoryMap($.finalist.getCurrentDivision());

  $("#schedule_body").empty();
  $.each(schedule, function(i, slot) {
    addRowForSlot(slot);
  }); // foreach timeslot

  var categoryRows = [];
  $.each($.finalist.getAllCategories(), function(i, category) {
    var cat = new FinalistCategory(category.name, category.isPublic, $.finalist
        .getRoom(category, $.finalist.getCurrentDivision()));
    categoryRows.push(cat);
  }); // foreach category
  $('#category_data').val($.toJSON(categoryRows));
  $('#division_data').val($.finalist.getCurrentDivision());
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

      // force an update to generate the initial page
      handleDivisionChange();

      // doesn't depend on the division, so can be done only once
      var nonNumericNominees = [];
      $.each($.finalist.getNonNumericCategories(), function(i, category) {
        var teamNumbers = [];
        $.each(category.teams, function(j, team) {
          teamNumbers.push(team);
        }); // foreach team
        var nominees = new NonNumericNominees(category.name, teamNumbers);
        nonNumericNominees.push(nominees);
      }); // foreach category
      $('#non-numeric-nominees_data').val($.toJSON(nonNumericNominees));

      // update the schedule data before submitting the form
      $('#get_sched_data').submit(updateScheduleToSend);

      $('#regenerate_schedule').click(function() {
        $.finalist.setSchedule($.finalist.getCurrentDivision(), null);
        updatePage();
      });

      $('#add_timeslot').click(function() {
        var newSlot = $.finalist.addSlotToSchedule(schedule);
        addRowForSlot(newSlot);
      });

      $.finalist.displayNavbar();
    });