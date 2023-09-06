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

// maximum number of rows in the most recent table
const MOST_RECENT_MAX_ROWS = 20;

const teamMaxScore = new Map();

// how many ranks to display unless told to show all
const TOP_SCORES_MAX_RANK = 5;

let topScoresAwardGroup = null;
let topScoresDisplayAllTeams = false;

const awardGroupColors = new Map();

function topScoresChangeAwardGroup() {
    if (awardGroupColors.size > 0) {
        let changed = false;
        if (null == topScoresAwardGroup) {
            // set current award group to start
            topScoresAwardGroup = awardGroupColors.keys().next().value;
            changed = true;
        } else if (awardGroupColors.size > 1) {
            // move to the next award group if there are more award groups

            let done = false;
            const awardGroups = Array.from(awardGroupColors.keys());
            let idx = 0;
            let loopCount = 0;
            while (!done) {
                const ag = awardGroups[idx];
                if (ag == topScoresAwardGroup) {
                    done = true;
                }

                // make sure there is a next value
                ++idx;
                if (idx >= awardGroups.length) {
                    idx = 0;
                    if (loopCount >= 1) {
                        console.log("Error: found infinite loop finding award group");
                        break;
                    }
                    ++loopCount;
                }
            }
            // next value is what we want
            const ag = awardGroups[idx];
            if (ag != topScoresAwardGroup) {
                topScoresAwardGroup = ag;
                changed = true;
            }
        }
        if (changed) {
            topScoresDisplay();
        }
    } else {
        console.log("Warning: no award groups found");
    }
    setTimeout(topScoresChangeAwardGroup, divisionFlipRate * 1000);
}

function topScoresAddScore(scoreUpdate) {
    let changed = false;
    if (!teamMaxScore.has(scoreUpdate.team.teamNumber)) {
        teamMaxScore.set(scoreUpdate.team.teamNumber, scoreUpdate);
        changed = true;
    } else {
        const prevScoreUpdate = teamMaxScore.get(scoreUpdate.team.teamNumber);
        if (scoreUpdate.score > prevScoreUpdate.score) {
            teamMaxScore.set(scoreUpdate.team.teamNumber, scoreUpdate);
            changed = true;
        }
    }

    if (changed && topScoresAwardGroup == scoreUpdate.team.awardGroup) {
        // TODO: consider being smarter and only rebuilding when the score will be displayed
        topScoresDisplay();
    }
}

function topScoresDisplay() {
    const topScoresTable = document.getElementById("top_scores_table");
    removeChildren(topScoresTable);

    // setup columns
    const colGroup = document.createElement("colgroup");
    topScoresTable.appendChild(colGroup);

    const rankCol = document.createElement("col");
    colGroup.appendChild(rankCol);
    rankCol.setAttribute("width", "30px");

    const teamNumCol = document.createElement("col");
    colGroup.appendChild(teamNumCol);
    teamNumCol.setAttribute("width", "75px");

    const teamNameCol = document.createElement("col");
    colGroup.appendChild(teamNameCol);

    if (displayOrganization) {
        const orgCol = document.createElement("col");
        colGroup.appendChild(orgCol);
    }

    const scoreCol = document.createElement("col");
    colGroup.appendChild(scoreCol);
    scoreCol.setAttribute("width", "70px");

    // header
    const headerRow = topScoresTable.createTHead().insertRow();
    const header = document.createElement("th");
    headerRow.appendChild(header);
    if (displayOrganization) {
        header.setAttribute("colspan", "5");
    } else {
        header.setAttribute("colspan", "4");
    }
    if (!awardGroupColors.has(topScoresAwardGroup)) {
        console.log("Warning cannot find color for award group '" + topScoresAwardGroup + "' in: " + awardGroupColors);
    } else {
        const color = awardGroupColors.get(topScoresAwardGroup)
        header.setAttribute("bgcolor", color);
    }
    header.innerText = "Top Performance Scores: " + topScoresAwardGroup;

    const tableBody = topScoresTable.createTBody();

    // display scores
    const allScores = Array.from(teamMaxScore.values());
    const awardGroupScores = allScores.filter((scoreUpdate) => scoreUpdate.team.awardGroup == topScoresAwardGroup);
    // sort descending by score
    awardGroupScores.sort(topScoresComparator);

    // output in order based on score
    let prevScoreFormatted = null;
    let rank = 0;
    for (const scoreUpdate of awardGroupScores) {
        if (prevScoreFormatted != scoreUpdate.formattedScore) {
            ++rank;
        }

        const row = tableBody.insertRow();

        const rankColumn = row.insertCell();
        rankColumn.classList.add("center");
        rankColumn.innerText = rank;

        const teamNumberColumn = row.insertCell();
        teamNumberColumn.classList.add("right");
        teamNumberColumn.innerText = scoreUpdate.team.teamNumber;

        const teamNameColumn = row.insertCell();
        teamNameColumn.classList.add("left");
        teamNameColumn.classList.add("truncate");
        teamNameColumn.innerText = scoreUpdate.team.teamName;

        if (displayOrganization) {
            const orgColumn = row.insertCell();
            orgColumn.classList.add("left");
            orgColumn.classList.add("truncate");
            orgColumn.innerText = scoreUpdate.team.organization;
        }

        const scoreColumn = row.insertCell();
        scoreColumn.classList.add("right");
        scoreColumn.innerText = scoreUpdate.formattedScore;

        prevScoreFormatted = scoreUpdate.formattedScore;
        if (!topScoresDisplayAllTeams && rank >= TOP_SCORES_MAX_RANK) {
            break;
        }
    }
}

function topScoresComparator(a, b) {
    if (a.score > b.score) {
        return -1;
    } else if (b.score > a.score) {
        return 1;
    } else {
        return 0;
    }
}

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

        // create elements
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

    // make team table visible
    const teamTable = document.getElementById("all_teams_" + scoreUpdate.team.teamNumber);
    teamTable.classList.remove("fll-sw-ui-inactive");
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

    // limit the table size
    while (tableBody.rows.length > MOST_RECENT_MAX_ROWS) {
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
    scoreCol.setAttribute("width", "70px");

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
    tableBody.id = "most_recent_table_body";
    return tableBody;
}


function socketOpened(_) {
    console.log("Socket opened");

    const message = new Object()
    message.type = REGISTER_MESSAGE_TYPE;
    message.displayUuid = displayUuid;

    this.send(JSON.stringify(message));
}

function socketClosed(_) {
    console.log("Socket closed");

    // open the socket a second later
    setTimeout(openSocket, 1000);
}

function openSocket() {
    console.log("opening socket");

    const url = window.location.pathname;
    const directory = url.substring(0, url.lastIndexOf('/'));
    const webSocketAddress = getWebsocketProtocol() + "//" + window.location.host + directory
        + "/ScoreboardEndpoint";

    const socket = new WebSocket(webSocketAddress);
    socket.onmessage = messageReceived;
    socket.onopen = socketOpened;
    socket.onclose = socketClosed;
}

function messageReceived(event) {
    const message = JSON.parse(event.data);
    if (message.type == UPDATE_MESSAGE_TYPE) {
        if (!message.bye && !message.noShow) {
            const mostRecentTableBody = document.getElementById("most_recent_table_body");
            addToMostRecent(mostRecentTableBody, message);
            addToAllTeams(message);
            topScoresAddScore(message);
        }
    } else if (message.type == DELETE_MESSAGE_TYPE) {
        location.reload();
    } else if (message.type == RELOAD_MESSAGE_TYPE) {
        location.reload();
    } else {
        console.log("Ignoring unexpected message type: " + message.type);
        console.log("Full message: " + event.data);
    }
}

document.addEventListener("DOMContentLoaded", () => {
    initMostRecent();

    topScoresChangeAwardGroup();

    if ("all_teams_top_scores" == layout) {
        document.getElementById("most_recent").classList.add("fll-sw-ui-inactive");

        topScoresDisplayAllTeams = true;

        document.getElementById("all_teams").classList.add("automatic_scroll");
        requestAnimationFrame(allTeamsDoScroll);
    } else if ("all_teams_no_scroll" == layout) {
        document.getElementById("left").classList.add("fll-sw-ui-inactive");
        document.getElementById("right").classList.add("fll-sw-ui-inactive");
        const allTeams = document.getElementById("all_teams");
        const container = document.getElementById("container");
        container.appendChild(allTeams);

        document.getElementById("all_teams").classList.add("manual_scroll");
    } else if ("all_teams_auto_scroll" == layout) {
        document.getElementById("left").classList.add("fll-sw-ui-inactive");
        document.getElementById("right").classList.add("fll-sw-ui-inactive");
        const allTeams = document.getElementById("all_teams");
        const container = document.getElementById("container");
        container.appendChild(allTeams);

        document.getElementById("all_teams").classList.add("automatic_scroll");
        requestAnimationFrame(allTeamsDoScroll);
    } else if ("top_scores" == layout) {
        document.getElementById("left").classList.add("fll-sw-ui-inactive");
        document.getElementById("right").classList.add("fll-sw-ui-inactive");
        const allTeams = document.getElementById("top_scores");
        const container = document.getElementById("container");
        container.appendChild(allTeams);
    } else if ("top_scores_all" == layout) {
        document.getElementById("left").classList.add("fll-sw-ui-inactive");
        document.getElementById("right").classList.add("fll-sw-ui-inactive");
        const allTeams = document.getElementById("top_scores");
        const container = document.getElementById("container");
        container.appendChild(allTeams);

        topScoresDisplayAllTeams = true;
    } else if ("most_recent" == layout) {
        document.getElementById("left").classList.add("fll-sw-ui-inactive");
        document.getElementById("right").classList.add("fll-sw-ui-inactive");
        const allTeams = document.getElementById("most_recent");
        const container = document.getElementById("container");
        container.appendChild(allTeams);
    } else {
        document.getElementById("all_teams").classList.add("automatic_scroll");
        requestAnimationFrame(allTeamsDoScroll);
    }

    openSocket();
});
