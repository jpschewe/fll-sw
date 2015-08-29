/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

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

function updatePage() {

  // $("#schedule").empty();

  // output header
  updateHeader();

  var schedule = $.finalist.scheduleFinalists();

  var schedRows = [];
  $.each(schedule, function(i, slot) {
    var row = $("<div class='rTableRow'></div>");
    $("#schedule_body").append(row);

    row.append($("<div class='rTableCell'>"
        + slot.time.getHours().toString().padL(2, "0") + ":"
        + slot.time.getMinutes().toString().padL(2, "0") + "</div>"));

    $.each($.finalist.getAllCategories(), function(i, category) {
      var teamNum = slot.categories[category.catId];
      if (teamNum == null) {
        row.append($("<div class='rTableCell'>&nbsp;</div>"));
      } else {
        var team = $.finalist.lookupTeam(teamNum);
        var group = team.judgingStation;
        row.append($("<div class='rTableCell'>" + teamNum + " - " + team.name
            + " (" + group + ")</div>"));

        var dbrow = new FinalistDBRow(category.name, slot.time.getHours(),
            slot.time.getMinutes(), teamNum);
        schedRows.push(dbrow);
      }
    }); // foreach category

  }); // foreach timeslot

  $('#sched_data').val($.toJSON(schedRows));

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
      handleDivisionChange();

      updatePage();

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

      $.finalist.displayNavbar();
    });