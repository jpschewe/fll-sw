/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

var draggingTeam = null;
var draggingCategory = null;
var draggingTeamDiv = null;
var schedule = null;

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
  var group = team.judgingStation;
  var teamDiv = $("<div draggable='true'>" + team.num + " - " + team.name
      + " (" + group + ")</div>");

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

    draggingTeamDiv.removeClass('valid-target');

    moveTeam(draggingTeam, draggingCategory, slot);

    // do something with the drop
    cell.append(draggingTeamDiv);

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
        var dbrow = new FinalistDBRow(category.name, slot.time.getHours(),
            slot.time.getMinutes(), teamNum);
        schedRows.push(dbrow);
      }
    }); // foreach category

  }); // foreach timeslot

  $('#sched_data').val($.toJSON(schedRows));
}

/**
 * Move a team from it's current timeslot to the newly specified timeslot for
 * the specified category.
 * 
 * @param team
 *          a Team object to move
 * @param category
 *          a Category object that the team is moving in
 * @param newSlot
 *          the new slot to put the team in
 */
function moveTeam(team, category, newSlot) {

  // remove team from all slots with this category
  $.each(schedule, function(i, slot) {
    var found = false;
    $.each(slot.categories, function(categoryId, teamNumber) {
      if (categoryId == category.catId && teamNumber == team.num) {
        found = true;
      }
    }); // foreach category
    if (found) {
      delete slot.categories[category.catId];
    }
  }); // foreach timeslot

  // add team to new slot
  newSlot.categories[category.catId] = team.num;
}

function updatePage() {

  // output header
  updateHeader();

  schedule = $.finalist.scheduleFinalists();

  $.each(schedule, function(i, slot) {
    var row = $("<div class='rTableRow'></div>");
    $("#schedule_body").append(row);

    row.append($("<div class='rTableCell'>"
        + slot.time.getHours().toString().padL(2, "0") + ":"
        + slot.time.getMinutes().toString().padL(2, "0") + "</div>"));

    $.each($.finalist.getAllCategories(), function(i, category) {
      var cell = createTimeslotCell(slot, category);
      row.append(cell);

      var teamNum = slot.categories[category.catId];
      if (teamNum != null) {
        var team = $.finalist.lookupTeam(teamNum);
        var teamDiv = createTeamDiv(team, category);
        cell.append(teamDiv);
      }
    }); // foreach category

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

      $.finalist.displayNavbar();
    });