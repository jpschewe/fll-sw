/*
 * Copyright (c) 2019 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

function addTournament() {
  var numRows = parseInt($('#numRows').val());
  var newTournamentId = $('#newTournamentId').val();

  var element = $([
      "<tr>",
      "  <td>",
      "    <input type=\"hidden\" name=\"key" + numRows + "\" value=\""
          + newTournamentId + "\" />",
      "    <input type=\"text\" id=\"name" + numRows + "\" name=\"name"
          + numRows + "\" maxlength=\"128\" size=\"16\" />",
      "  </td>",
      "<td><input type=\"text\" name=\"description" + numRows
          + "\" size=\"64\" /></td>",

      "</tr>" ].join("\n"));

  $('#tournamentsTable tbody').append(element);

  $('#numRows').val(numRows + 1);
}

function checkTournamentNames() {
  var numRows = $('#numRows').val();

  var tournamentsSeen = [];
  for (var idx = 0; idx < numRows; ++idx) {
    var name = $('#name' + idx).val();
    console.log("Checking index: " + idx + " name: " + name + " against: "
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

$(document).ready(function() {

  $('#addRow').click(function(e) {
    addTournament();
    e.preventDefault();
  });

}); // end ready function
