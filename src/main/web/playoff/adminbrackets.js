/*
 * Copyright (c) 2017 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

function populateLeaf(leafId, teamNumber, teamName, score, verified) {
    const leafEle = document.getElementById(leafId);
    removeChildren(leafEle);

    if (null != teamNumber && "" != teamNumber && teamNumber >= 0) {
        const teamNumberEle = document.createElement("span");
        leafEle.appendChild(teamNumberEle);
        teamNumberEle.innerText = "#" + teamNumber;
        teamNumberEle.classList.add("TeamNumber");
    }

    if (null != teamName && "NULL" != teamName) {
        const teamNameEle = document.createElement("span");
        leafEle.appendChild(teamNameEle);
        teamNameEle.innerText = " " + teamName;
        teamNameEle.classList.add("TeamName");
    }

    if (null != score && "" != score) {
        const outerSpan = document.createElement("span");
        leafEle.appendChild(outerSpan);
        if (!verified) {
            outerSpan.style.color = "red";
        }

        const scoreEle = document.createElement("span");
        outerSpan.appendChild(scoreEle);
        scoreEle.classList.add("TeamScore");
        scoreEle.innerText = " Score: " + score;
    }
}

function placeTableLabel(lid, table) {
    if (null != table && "" != table) {
        document.getElementById(lid + "-table").innerText = table;
    }
}

function messageReceived(event) {
    const message = JSON.parse(event.data);
    if (message.type == DISPLAY_UPDATE_MESSAGE_TYPE) {
        // currently ignored, but may be useful in the future
        //console.log("Display update: " + bracketMessage.allBracketInfo);
    } else if (message.type == BRACKET_UPDATE_MESSAGE_TYPE) {
        if (message.bracketUpdate.bracketName != bracketInfo.bracketName) {
            // not for us
            return;
        }

        const leafId = constructLeafId(bracketInfo.bracketIndex,
            message.bracketUpdate.dbLine,
            message.bracketUpdate.playoffRound);

        populateLeaf(leafId, message.bracketUpdate.teamNumber,
            message.bracketUpdate.teamName,
            message.bracketUpdate.score,
            message.bracketUpdate.verified);

        placeTableLabel(leafId, bracketMessage.bracketUpdate.table);
    } else {
        console.log("Ignoring unexpected message type: " + message.type);
        console.log("Full message: " + event.data);
    }
}

function socketOpened(_) {
    console.log("Socket opened");

    const message = new Object();
    message.type = REGISTER_MESSAGE_TYPE;
    message.displayUuid = ""; // no display associated
    message.bracketInfo = [bracketInfo];

    const str = JSON.stringify(message);
    this.send(str);
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
        + "/H2HUpdateWebSocket";

    const socket = new WebSocket(webSocketAddress);
    socket.onmessage = messageReceived;
    socket.onopen = socketOpened;
    socket.onclose = socketClosed;
}

document.addEventListener("DOMContentLoaded", function() {
    openSocket();
});
