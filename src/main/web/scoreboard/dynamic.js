/*
 * Copyright (c) 2023 HighTechKids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

let scoreEventSource = null;
let displayOrganization = true;

function addToMostRecent(tableBody, scoreUpdate) {
    const trElement = tableBody.insertRow(0);

    const teamNumElement = trElement.insertCell();
    teamNumElement.classList.add("left");
    teamNumElement.innerText = scoreUpdate.team.teamNumber;

    const teamName = trElement.insertCell();
    teamName.classList.add('left');
    teamName.classList.add('truncate');
    teamName.innerText = scoreUpdate.team.teamName;

    if (displayOrganization) {
        const org = trElement.insertCell();
        org.classList.add('left');
        org.classList.add('truncate');
        org.innerText = scoreUpdate.team.organization;
    }

    const awardGroup = trElement.insertCell();
    awardGroup.classList.add('right');
    awardGroup.classList.add('truncate');
    awardGroup.innerText = scoreUpdate.team.awardGroup;

    const score = trElement.insertCell();
    score.classList.add('right');
    score.innerText = scoreUpdate.formattedScore;

    // limit the table size to 20 rows
    while (tableBody.rows.length > 20) {
        tableBody.deleteRow(tableBody.rows.length - 1);
    }
}

function initMostRecent() {
    const mostRecentTable = document.getElementById("most_recent_table");
    removeChildren(mostRecentTable);

    displayOrganization = window.innerWidth >= 1024;

    // setup columns
    const colGroup = document.createElement("colgroup");
    mostRecentTable.appendChild(colGroup);

    const teamNumCol = document.createElement("col");
    colGroup.appendChild(teamNumCol);
    teamNumCol.setAttribute("width", "75px");

    const teamNameCol = document.createElement("col");
    colGroup.appendChild(teamNameCol);

    if (displayOrganization) {
        const orgCol = document.createElement("col");
        colGroup.appendChild(orgCol);
    }

    const awardGroupCol = document.createElement("col");
    colGroup.appendChild(awardGroupCol);
    awardGroupCol.setAttribute("width", "100Fpx");

    const scoreCol = document.createElement("col");
    colGroup.appendChild(scoreCol);
    scoreCol.setAttribute("width", "75px");

    // header
    const headerRow = mostRecentTable.createTHead().insertRow();
    const header = document.createElement("th");
    headerRow.appendChild(header);
    if (displayOrganization) {
        header.setAttribute("colspan", "5");
    } else {
        header.setAttribute("colspan", "4");
    }
    header.setAttribute("bgcolor", "#800080");
    header.innerText = "Most Recent Performance Scores";

    const tableBody = mostRecentTable.createTBody();
    return tableBody;
}


document.addEventListener("DOMContentLoaded", () => {

    const mostRecentTableBody = initMostRecent();

    scoreEventSource = new EventSource('/scoreboard/SubscribeScoreUpdate');
    scoreEventSource.addEventListener('score_update', function(e) {
        //console.log("Received score update: " + e.data);

        const scoreUpdate = JSON.parse(e.data);

        if (!scoreUpdate.bye && !scoreUpdate.noShow) {
            addToMostRecent(mostRecentTableBody, scoreUpdate);
        }

    }, true);
});
