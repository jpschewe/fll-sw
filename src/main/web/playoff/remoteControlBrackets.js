/*
 * Copyright (c) 2017 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

function isDisplayInfoDifferent(newAllBracketData) {

    if (allBracketData.length != newAllBracketData.length) {
        return true;
    }

    const length = allBracketData.length
    for (let i = 0;i < length;i++) {
        // Check if we have nested arrays
        const oldBracketInfo = allBracketData[i];
        const newBracketInfo = newAllBracketData[i];

        if (oldBracketInfo.bracketName != newBracketInfo.bracketName) {
            return true;
        }
        if (oldBracketInfo.firstRound != newBracketInfo.firstRound) {
            return true;
        }
    }

    return false;
}

function parseTeamName(teamName) {
    if (teamName.length > maxNameLength) {
        return teamName.substring(0, maxNameLength - 3) + "...";
    } else {
        return teamName;
    }
}

function setTeamName(leaf, teamNumber, teamName) {
    if (teamNumber >= 0) {
        const teamNumberEle = document.createElement("span");
        leaf.appendChild(teamNumberEle)
        teamNumberEle.classList.add("TeamNumber");
        teamNumberEle.innerText = "#" + teamNumber;
    }

    const teamNameEle = document.createElement("span");
    leaf.appendChild(teamNameEle)
    teamNameEle.classList.add("TeamName");
    teamNameEle.innerText = parseTeamName(teamName);
}

function setTeamNameAndScore(leaf, teamNumber, teamName, scoreData) {
    setTeamName(leaf, teamNumber, teamName);

    const scoreEle = document.createElement("span");
    leaf.appendChild(scoreEle);
    scoreEle.classList.add("TeamScore");
    scoreEle.innerText = "Score: " + scoreData;
}

function populateLeaf(leafId, bracketUpdate) {
    const leaf = document.getElementById(leafId);
    removeChildren(leaf);

    // Some of these if/else statements can be collapsed, however I've broken them
    // out to make it clear what each case is.

    if (null != bracketUpdate.teamNumber && "" != bracketUpdate.teamNumber) {
        // have a valid team number, otherwise clear the cell

        if (bracketUpdate.teamNumber < 0) {
            // tie or bye
            setTeamName(leaf, bracketUpdate.teamNumber, bracketUpdate.teamName);
        } else if (bracketUpdate.playoffRound >= bracketUpdate.maxPlayoffRound) {
            // finals, don't display the finals
        } else if (!bracketUpdate.verified) {
            // score isn't verified, only show the team name
            setTeamName(leaf, bracketUpdate.teamNumber, bracketUpdate.teamName);
        } else if (null == bracketUpdate.score || "" == bracketUpdate.score) {
            // no score, only show the team name
            setTeamName(leaf, bracketUpdate.teamNumber, bracketUpdate.teamName);
        } else if (bracketUpdate.playoffRound == bracketUpdate.maxPlayoffRound - 1) {
            // scores from the last head to head round can't be displayed, because
            // then the winner would be known, only show team name
            setTeamName(leaf, bracketUpdate.teamNumber, bracketUpdate.teamName);
        } else if (bracketUpdate.noShow) {
            // no show, explicitly set score text
            setTeamNameAndScore(leaf, bracketUpdate.teamNumber,
                bracketUpdate.teamName, "No Show");
        } else {
            // verified score
            setTeamNameAndScore(leaf, bracketUpdate.teamNumber,
                bracketUpdate.teamName, bracketUpdate.score);
        }
    }
}

function placeTableLabel(lid, table) {
    if (null != table && "" != table) {
        document.getElementById(lid + "-table").innerText = table;
    }
}

function colorTableLabels() {

    var validColors = new Array();
    validColors[0] = "maroon";
    validColors[1] = "red";
    validColors[2] = "orange";
    validColors[3] = "yellow";
    validColors[4] = "olive";
    validColors[5] = "purple";
    validColors[6] = "fuchsia";
    validColors[7] = "white";
    validColors[8] = "lime";
    validColors[9] = "green";
    validColors[10] = "navy";
    validColors[11] = "blue";
    validColors[12] = "aqua";
    validColors[13] = "teal";
    validColors[14] = "black";
    validColors[15] = "silver";
    validColors[16] = "gray";
    // colors sourced from http://www.w3.org/TR/CSS2/syndata.html#color-units

    document.querySelectorAll(".table_assignment")
        .forEach(function(label) {
            // Sane color? Let's start by splitting the label text
            const tableColor = label.innerHTML.split(" ")[0];
            if (validColors.indexOf(tableColor.toLowerCase()) > 0) {
                label.style.borderColor = tableColor;
                label.style.borderStyle = "solid";
            }
        });
}

function handleBracketUpdate(bracketUpdate) {
    console.log("Received bracket update");

    allBracketData.forEach(function(bracketData, index) {

        if (bracketData.bracketName == bracketUpdate.bracketName) {
            // update bracket

            var leafId = constructLeafId(bracketData.bracketIndex,
                bracketUpdate.dbLine, bracketUpdate.playoffRound);

            console.log("Update for bracket: " + bracketData.bracketName + " index: "
                + index + " leafId: " + leafId);

            populateLeaf(leafId, bracketUpdate);

            placeTableLabel(leafId, bracketUpdate.table);

        }

    });

    colorTableLabels();
}

function messageReceived(event) {
    const message = JSON.parse(event.data);
    if (message.type == DISPLAY_UPDATE_MESSAGE_TYPE) {
        if (isDisplayInfoDifferent(message.allBracketInfo)) {
            // reload
            location.reload();
        }
    } else if (message.type == BRACKET_UPDATE_MESSAGE_TYPE) {
        handleBracketUpdate(message.bracketUpdate);
    } else {
        console.log("Ignoring unexpected message type: " + message.type);
        console.log("Full message: " + event.data);
    }
}

function socketOpened(_event) {
    console.log("Socket opened");


    const message = new Object();
    message.type = REGISTER_MESSAGE_TYPE;
    message.displayUuid = displayUuid;
    message.bracketInfo = allBracketData;

    const str = JSON.stringify(message);
    this.send(str);
}

function socketClosed(_event) {
    console.log("Socket closed");

    // open the socket a second later
    setTimeout(openSocket, 1000);
}

function openSocket() {
    console.log("opening socket");

    const url = window.location.pathname;
    const directory = url.substring(0, url.lastIndexOf('/'));
    const webSocketAddress = getWebsocketProtocol() + "//" + window.location.host + directory
        + "/H2HUpdateWebSocket";

    const socket = new WebSocket(webSocketAddress);
    socket.onmessage = messageReceived;
    socket.onopen = socketOpened;
    socket.onclose = socketClosed;
}

function startScrolling() {
    const topElement = document.getElementById("top");
    const bottomElement = document.getElementById("bottom");
    const scrollElement = window;
    const endPauseSeconds = 3;
    // if less than 1, then Chromebooks don't appear to scroll
    const pixelsToScroll = 2;

    startEndlessScroll(scrollElement, topElement, bottomElement, endPauseSeconds, pixelsToScroll, secondsBetweenScrolls)
}

document.addEventListener("DOMContentLoaded", function() {
    console.log("Top of ready");
    openSocket();
});
