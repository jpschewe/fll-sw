/*
 * Copyright (c) 2023 HighTechKids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

let scoreEventSource = null;
let displayOrganization = true;

let prevScrollTimestamp = 0;
// if less than 1, then Chromebooks don't appear to scroll
const pixelsToScroll = 2;
let scrollingDown = true;

function allTeamsScoreCellId(teamNumber, runNumber) {
    return "all_teams_" + teamNumber + "_run_" + runNumber + "_score";
}

function allTeamsScoreRowId(teamNumber, runNumber) {
    return "all_teams_" + teamNumber + "_run_" + runNumber;
}

function allTeamsEnsureScoreRowExists(teamNumber, runNumber) {
    let row = document.getElementById("all_teams_" + teamNumber + "_run_" + runNumber);
    if (null == row) {
        if (runNumber > 1) {
            // ensure previous run exists
            allTeamsEnsureScoreRowExists(teamNumber, runNumber - 1);
        }
        // create element
        /*
                                            <tr id="all_teams_${team.teamNumber}_run_${runNumber}" class='right'>
                                        <td>
                                            <img class='run_spacer'
                                                src='<c:url value="/images/blank.gif"/>'
                                                />
                                        </td>
                                        <td>${score.runNumber }</td>
                                        <td>
                                            <img class='score_spacer'
                                                src='<c:url value="/images/blank.gif"/>'
                                                 />
                                        </td>
                                        <td>${score.scoreString }</td>
                                    </tr>
*/
        const scoreTable = document.getElementById("all_teams_" + teamNumber + "_scores");
        row = scoreTable.insertRow();
        row.id = allTeamsScoreRowId(teamNumber, runNumber);
        row.classList.add("right");
        row.classList.add("fll-sw-ui-inactive");

        const runSpacerCell = row.insertCell();
        const runSpacer = document.createElement("img");
        runSpacerCell.appendChild(runSpacer);
        runSpacer.classList.add("run_spacer");
        runSpacer.setAttribute("src", "/images/blank.gif");

        const runCell = row.insertCell();
        runCell.innerText = runNumber;

        const scoreSpacerCell = row.insertCell();
        const scoreSpacer = document.createElement("img");
        scoreSpacerCell.appendChild(scoreSpacer);
        scoreSpacer.classList.add("score_spacer");
        scoreSpacer.setAttribute("src", "/images/blank.gif");

        const scoreCell = row.insertCell();
        scoreCell.id = allTeamsScoreCellId(teamNumber, runNumber);
    }
}

function addToAllTeams(scoreUpdate) {
    // make sure the row exists
    allTeamsEnsureScoreRowExists(scoreUpdate.team.teamNumber, scoreUpdate.runNumber);

    // populate the score
    const scoreCell = document.getElementById(allTeamsScoreCellId(scoreUpdate.team.teamNumber, scoreUpdate.runNumber));
    scoreCell.innerText = scoreUpdate.formattedScore;

    // make the row visible
    const row = document.getElementById(allTeamsScoreRowId(scoreUpdate.team.teamNumber, scoreUpdate.runNumber));
    row.classList.remove("fll-sw-ui-inactive");
}

function allTeamsDoScroll(timestamp) {
    const diff = timestamp - prevScrollTimestamp;
    if (diff / 1000.0 >= secondsBetweenScrolls) {
        if (scrollingDown && elementIsVisible(document.getElementById("all_teams_bottom"))) {
            scrollingDown = false;
        } else if (!scrollingDown
            && elementIsVisible(document.getElementById("all_teams_top"))) {
            scrollingDown = true;
        }

        const scrollAmount = scrollingDown ? pixelsToScroll : -1 * pixelsToScroll;
        document.getElementById("all_teams").scrollBy(0, scrollAmount);
        prevScrollTimestamp = timestamp;
    }

    requestAnimationFrame(allTeamsDoScroll);
}

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
            addToAllTeams(scoreUpdate);
        }

    }, true);

    requestAnimationFrame(allTeamsDoScroll);
});
