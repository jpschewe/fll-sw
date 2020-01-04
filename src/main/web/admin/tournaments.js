/*
 * Copyright (c) 2019 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

function addTournament() {
  var numRows = parseInt($('#numRows').val());
  var newTournamentId = $('#newTournamentId').val();

  var trElement = $("<tr></tr>");

  var td1Element = $("<td></td>");
  var keyElement = $("<input type=\"hidden\" name=\"key" + numRows
      + "\" value=\"" + newTournamentId + "\" />");
  td1Element.append(keyElement);

  var dateElement = $("<input type=\"text\" name=\"date" + numRows
      + "\" size=\"8\" />");
  dateElement.datepicker();
  td1Element.append(dateElement);
  trElement.append(td1Element);

  var td2Element = $("<td></td>");
  var nameElement = $("<input type=\"text\" id=\"name" + numRows
      + "\" name=\"name" + numRows + "\" maxlength=\"128\" size=\"20\" />");
  td2Element.append(nameElement);
  trElement.append(td2Element);

  var td3Element = $("<td></td>");
  var descriptionElement = $("<input type=\"text\" name=\"description"
      + numRows + "\" size=\"64\" />");
  td3Element.append(descriptionElement);
  trElement.append(td3Element);

  var td4Element = $("<td></td>");
  var levelElement = $("<input type=\"text\" name=\"level" + numRows
      + "\" size=\"20\" maxlength=\"128\" />");
  td4Element.append(levelElement);
  trElement.append(td4Element);

  var td5Element = $("<td></td>");
  var nextLevelElement = $("<input type=\"text\" name=\"nextLevel" + numRows
      + "\" size=\"20\" maxlength=\"128\" />");
  td5Element.append(nextLevelElement);
  trElement.append(td5Element);

  $('#tournamentsTable tbody').append(trElement);

  $('#numRows').val(numRows + 1);
}

function checkTournamentNames() {
  var numRows = parseInt($('#numRows').val());

  var tournamentsSeen = [];
  for (var idx = 0; idx < numRows; ++idx) {
    var name = $('#name' + idx).val();
    _log("Checking index: " + idx + " name: " + name + " against: "
        + tournamentsSeen);
    if (name) {
      if (tournamentsSeen.includes(name)) {
        alert("Multiple tournaments have the name '" + name + "'");
        return false;
      }
      tournamentsSeen.push(name);
    }
  }

  return true;
}

function setupDatepickers() {
  var numRows = parseInt($('#numRows').val());
  for (var idx = 0; idx < numRows; ++idx) {
    $('#date' + idx).datepicker();
  }
}

$(document).ready(function() {

  $('#addRow').click(function(e) {
    addTournament();
    e.preventDefault();
  });

  setupDatepickers();

}); // end ready function
