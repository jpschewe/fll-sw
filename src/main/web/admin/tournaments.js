/*
 * Copyright (c) 2019 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

// keep the keys of new tournaments unique
var nextNewTournamentKey = 1;

// row index keeps counting up, even if rows are deleted
var nextRowIndex = 1;

var tournamentsWithScores = [];

function addNewTournament() {
    const keyIndex = nextNewTournamentKey;
    ++nextNewTournamentKey;

    const name = "Tournament " + keyIndex;
    const key = NEW_TOURNAMENT_PREFIX + keyIndex;
    addTournament(key, name, null, null, null);
}

function addTournament(key, name, description, date, level) {
    const rowIndex = nextRowIndex;
    ++nextRowIndex;

    const table = document.getElementById("tournamentsTable");

    const row = document.createElement("tr");
    table.append(row);

    const dateCell = document.createElement("td");
    row.appendChild(dateCell);
    const keyElement = document.createElement("input");
    dateCell.appendChild(keyElement);
    keyElement.setAttribute("type", "hidden");
    keyElement.setAttribute("name", KEY_PREFIX + rowIndex);
    keyElement.setAttribute("id", KEY_PREFIX + rowIndex);
    keyElement.setAttribute("value", key);

    const dateElement = document.createElement("input");
    dateCell.appendChild(dateElement);
    dateElement.setAttribute("type", "date");
    dateElement.setAttribute("name", DATE_PREFIX + rowIndex);
    dateElement.setAttribute("id", DATE_PREFIX + rowIndex);
    if (date) {
        dateElement.value = date;
    }

    const nameCell = document.createElement("td");
    row.appendChild(nameCell);
    const nameElement = document.createElement("input");
    nameCell.appendChild(nameElement);
    nameElement.setAttribute("type", "text");
    nameElement.setAttribute("name", NAME_PREFIX + rowIndex);
    nameElement.setAttribute("id", NAME_PREFIX + rowIndex);
    nameElement.setAttribute("maxlength", "128");
    nameElement.setAttribute("size", "20");
    nameElement.value = name;


    const descriptionCell = document.createElement("td");
    row.appendChild(descriptionCell);
    const descriptionElement = document.createElement("input");
    descriptionCell.appendChild(descriptionElement);
    descriptionElement.setAttribute("type", "text");
    descriptionElement.setAttribute("name", DESCRIPTION_PREFIX + rowIndex);
    descriptionElement.setAttribute("id", DESCRIPTION_PREFIX + rowIndex);
    descriptionElement.setAttribute("size", "64");
    if (description) {
        descriptionElement.value = description;
    }

    const levelCell = document.createElement("td");
    row.appendChild(levelCell);
    const levelElement = document.createElement("select");
    levelCell.appendChild(levelElement);
    levelElement.setAttribute("name", LEVEL_PREFIX + rowIndex);
    levelElement.setAttribute("id", LEVEL_PREFIX + rowIndex);
    populateLevelSelect(levelElement);
    if (level) {
        levelElement.value = level;
    }

    const deleteCell = document.createElement("td");
    row.appendChild(deleteCell);
    if (key == currentTournamentId) {
        deleteCell.innerHTML = "Current Tournament";
    } else if (tournamentsWithScores.includes(key)) {
        deleteCell.innerHTML = "Has Scores";
    } else {
        const deleteButton = document.createElement("button");
        deleteCell.appendChild(deleteButton);
        deleteButton.setAttribute("type", "button");
        deleteButton.innerHTML = "Delete";
        deleteButton.addEventListener("click", function() {
            row.remove();
        });
    }
}

function checkTournamentNames() {
    const tournamentsSeen = [];

    const inputs = document.getElementsByTagName("input");
    for (var i = 0; i < inputs.length; ++i) {
        const input = inputs[i];
        const elementName = input.getAttribute("name");
        if (elementName && elementName.startsWith(NAME_PREFIX)) {
            const name = input.value;
            if (name) {
                _log("Checking name: " + name + " against: "
                    + tournamentsSeen);
                if (tournamentsSeen.includes(name)) {
                    alert("Multiple tournaments have the name '" + name + "'");
                    return false;
                }
                tournamentsSeen.push(name);
            } else {
                alert("All tournaments must have names");
                return false;
            }
        }
    }

    return true;
}

