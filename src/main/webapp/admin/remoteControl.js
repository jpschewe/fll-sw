/*
 * Copyright (c) 2016 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use-strict";

/**
 * The prefix used on variables based on the display name.
 * Needs to match DisplayInfo.getFormParamPrefix() in the Java code. 
 * 
 * @param displayName
 *          string display name from the list displayNames
 * @returns
 */
function displayPrefix(displayName) {
  if ("Default" == displayName) {
    return "";
  } else {
    return displayName + "_";
  }
}

/**
 * Get the number of brackets for the specified display.
 * 
 * @param displayName the name of the display
 * @returns an integer number of brackets
 */
function getNumBracketsForDisplay(displayName) {
  var lDisplayPrefix = displayPrefix(displayName);
  var numBrackets = parseInt($("#" + lDisplayPrefix + "numBrackets").val());
  return numBrackets;
}

/**
 * Set the number of brackets for the specified display.
 * 
 * @param displayName the name of the display
 * @param numBrackets the number of brackets
 */
function setNumBracketsForDisplay(displayName, numBrackets) {
  var lDisplayPrefix = displayPrefix(displayName);
  $("#" + lDisplayPrefix + "numBrackets").val(numBrackets);
}

function updateButtonStates() {
  $.each(displayNames, function(index, displayName) {
    var remove_button_id = displayPrefix(displayName) + "remove_bracket";
    var nBrackets = getNumBracketsForDisplay(displayName);
    if (nBrackets < 2) {
      $("#" + remove_button_id).prop('disabled', true);
    } else {
      $("#" + remove_button_id).prop('disabled', false);
    }
  });
}

/**
 * Add a bracket to the specified display.
 * 
 * @param displayName the name of the display
 */
function addBracket(displayName) {
  var lDisplayPrefix = displayPrefix(displayName);
  var bracketIndex = getNumBracketsForDisplay(displayName);
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

  setNumBracketsForDisplay(displayName, bracketIndex+1);
  updateButtonStates();
}

/**
 * Remove the last bracket for the specified display.
 * 
 * @param displayName the name of the display
 */
function removeBracket(displayName) {
  var nBrackets = getNumBracketsForDisplay(displayName);
  var lastBracketIndex = nBrackets - 1;
  var lDisplayPrefix = displayPrefix(displayName);
  var div = $("#" + lDisplayPrefix + "bracket_" + lastBracketIndex);

  div.remove();

  setNumBracketsForDisplay(displayName, lastBracketIndex);
  updateButtonStates();
}

$(document).ready(function() {
  updateButtonStates();

  $.each(displayNames, function(index, displayName) {
    var prefix = displayPrefix(displayName);
    $("#" + prefix + "add_bracket").click(function() {
      addBracket(displayName);
    });

    $("#" + prefix + "remove_bracket").click(function() {
      removeBracket(displayName);
    });

  });
});
