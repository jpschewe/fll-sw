/*
 * Copyright (c) 2016 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

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
    const lDisplayPrefix = displayPrefix(displayName);
    const numBracketsEle = document.getElementById(lDisplayPrefix + "numBrackets");
    const numBrackets = parseInt(numBracketsEle.value);
    return numBrackets;
}

/**
 * Set the number of brackets for the specified display.
 * 
 * @param displayName the name of the display
 * @param numBrackets the number of brackets
 */
function setNumBracketsForDisplay(displayName, numBrackets) {
    const lDisplayPrefix = displayPrefix(displayName);
    const numBracketsEle = document.getElementById(lDisplayPrefix + "numBrackets");
    numBracketsEle.value = numBrackets;
}

function updateButtonStates() {
    for (const displayName of displayNames) {
        const remove_button_id = displayPrefix(displayName) + "remove_bracket";
        const nBrackets = getNumBracketsForDisplay(displayName);
        const removeButton = document.getElementById(remove_button_id);
        if (nBrackets < 2) {
            removeButton.disabled = true;
        } else {
            removeButton.disabled = false;
        }
    }
}

/**
 * Add a bracket to the specified display.
 * 
 * @param displayName the name of the display
 */
function addBracket(displayName) {
    const lDisplayPrefix = displayPrefix(displayName);
    const bracketIndex = getNumBracketsForDisplay(displayName);
    const topDiv = document.createElement("div");
    document.getElementById(lDisplayPrefix + "bracket_selection").appendChild(topDiv);
    topDiv.setAttribute("id", lDisplayPrefix + "bracket_" + bracketIndex);

    const bracketDiv = document.createElement("div");
    topDiv.appendChild(bracketDiv);

    const bracketLabel = document.createElement("span");
    bracketDiv.appendChild(bracketLabel);
    bracketLabel.innerText = "Bracket: ";

    const bracketSelect = document.createElement("select");
    bracketDiv.appendChild(bracketSelect);
    bracketSelect.setAttribute("name", lDisplayPrefix
        + "playoffDivision_" + bracketIndex);
    for (const division of divisions) {
        const option = document.createElement("option");
        bracketSelect.appendChild(option);
        option.setAttribute("value", division);
        option.innerText = division;
    }

    const roundDiv = document.createElement("div");
    topDiv.appendChild(roundDiv);

    const roundLabel = document.createElement("span");
    roundDiv.appendChild(roundLabel);
    roundLabel.innerText = "Round: ";

    const roundSelect = document.createElement("select");
    roundDiv.appendChild(roundSelect);
    roundSelect.setAttribute("name", lDisplayPrefix + "playoffRoundNumber_" + bracketIndex);
    let round;
    for (round = 1; round <= numPlayoffRounds; ++round) {
        const option = document.createElement("option");
        roundSelect.appendChild(option);
        option.setAttribute("value", round);
        option.innerText = round;
    }

    topDiv.appendChild(document.createElement("hr"));

    setNumBracketsForDisplay(displayName, bracketIndex + 1);
    updateButtonStates();
}

/**
 * Remove the last bracket for the specified display.
 * 
 * @param displayName the name of the display
 */
function removeBracket(displayName) {
    const nBrackets = getNumBracketsForDisplay(displayName);
    const lastBracketIndex = nBrackets - 1;
    const lDisplayPrefix = displayPrefix(displayName);
    const div = document.getElementById(lDisplayPrefix + "bracket_" + lastBracketIndex);
    div.parentNode.removeChild(div);

    setNumBracketsForDisplay(displayName, lastBracketIndex);
    updateButtonStates();
}

document.addEventListener("DOMContentLoaded", function() {
    updateButtonStates();

    for (const displayName of displayNames) {
        const prefix = displayPrefix(displayName);
        document.getElementById(prefix + "add_bracket").addEventListener("click", function() {
            addBracket(displayName);
        });

        document.getElementById(prefix + "remove_bracket").addEventListener("click", function() {
            removeBracket(displayName);
        });
    }
});
