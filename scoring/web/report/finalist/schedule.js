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

function updatePage() {

  $("#schedule").empty();

  // output header
  var headerRow = $("<tr></tr>");
  $("#schedule").append(headerRow);

  headerRow.append($("<th>Time Slot</th>"));
  $.each($.finalist.getAllCategories(), function(i, category) {
    var room = $.finalist.getRoom(category, $.finalist.getCurrentDivision());
    var header;
    if (room == undefined || "" == room) {
      header = $("<th>" + category.name + "</th>");
    } else {
      header = $("<th>" + category.name + "<br/>Room: " + room + "</th>");
    }
    headerRow.append(header);
  });

  var schedule = $.finalist.scheduleFinalists();

  var schedRows = [];
  $.each(schedule, function(i, slot) {
    var row = $("<tr></tr>");
    $("#schedule").append(row);

    row.append($("<td>" + slot.time.getHours().toString().padL(2, "0") + ":"
        + slot.time.getMinutes().toString().padL(2, "0") + "</td>"));

    $.each($.finalist.getAllCategories(), function(i, category) {
      var teamNum = slot.categories[category.catId];
      if (teamNum == null) {
        row.append($("<td>&nbsp;</td>"));
      } else {
        var team = $.finalist.lookupTeam(teamNum);
        var group = team.judgingStation;
        row.append($("<td>" + teamNum + " - " + team.name + " (" + group
            + ")</td>"));

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