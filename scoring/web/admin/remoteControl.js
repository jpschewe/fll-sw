/*
 * Copyright (c) 2016 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use-strict";

/**
 * The prefix used on variables based on the display name.
 * 
 * @param displayName
 *          string display name from the dictionary numBrackets
 * @returns
 */
function displayPrefix(displayName) {
  if ("default" == displayName) {
    return "";
  } else {
    return displayName + "_";
  }
}

function updateButtonStates() {
  $.each(numBrackets, function(displayName, nBrackets) {
    var remove_button_id = displayPrefix(displayName) + "remove_bracket";
    if (nBrackets < 2) {
      $("#" + remove_button_id).prop('disabled', true);
    } else {
      $("#" + remove_button_id).prop('disabled', false);
    }
  });
}

function addBracket(displayName) {
  var lDisplayPrefix = displayPrefix(displayName);
  var bracketIndex = numBrackets[displayName];
  var div = $("<div id='" + lDisplayPrefix + "bracket_" + bracketIndex
      + "'></div>");
  $("#" + lDisplayPrefix + "bracket_selection").append(div);

  var bracketSelect = $("<select name='" + lDisplayPrefix
      + "playoffDivision_" + bracketIndex + "'></select>");
  $.each(divisions,
      function(index, division) {
        var option = $("<option value='" + division + "'>" + division
            + "</option>");
        bracketSelect.append(option);
      });
  div.append("Bracket: ");
  div.append(bracketSelect);
  div.append("<br/>");

  var roundSelect = $("<select name='" + lDisplayPrefix
      + "playoffRoundNumber_" + bracketIndex + "'></select>");
  var round;
  for (round = 1; round <= numPlayoffRounds; ++round) {
    var option = $("<option value='" + round + "'>" + round + "</option>");
    roundSelect.append(option);
  }
  div.append("Round: ");
  div.append(roundSelect);

  div.append($("<hr/>"));

  // now one more bracket defined
  numBrackets[displayName] = bracketIndex + 1;
  $("#" + lDisplayPrefix + "numBrackets").val(numBrackets[displayName]);

  updateButtonStates();
}

function removeBracket(displayName) {
  var nBrackets = numBrackets[displayName];
  var lastBracketIndex = nBrackets - 1;
  var lDisplayPrefix = displayPrefix(displayName);
  var div = $("#" + lDisplayPrefix + "bracket_" + lastBracketIndex);

  div.remove();

  // now one less bracket defined
  numBrackets[displayName] = nBrackets - 1;
  $("#" + lDisplayPrefix + "numBrackets").val(numBrackets[displayName]);

  updateButtonStates();
}

$(document).ready(function() {
  updateButtonStates();

  $.each(numBrackets, function(displayName, nBrackets) {
    $("#" + displayPrefix(displayName) + "add_bracket").click(function() {
      addBracket(displayName);
    });

    $("#" + displayPrefix(displayName) + "remove_bracket").click(function() {
      removeBracket(displayName);
    });

  });
});
