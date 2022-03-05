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

        const teamNameEle = document.createElement("span");
        leafEle.appendChild(teamNameEle);
        teamNameEle.innerText = " " + teamName;
        teamNameEle.classList.add("TeamName");

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
}

function messageReceived(event) {

    console.log("received: " + event.data);
    const bracketMessage = JSON.parse(event.data);
    if (bracketMessage.isBracketUpdate) {
        if (bracketMessage.bracketUpdate.bracketName != bracketInfo.bracketName) {
            // not for us
            return;
        }

        const leafId = constructLeafId(bracketInfo.bracketIndex,
            bracketMessage.bracketUpdate.dbLine,
            bracketMessage.bracketUpdate.playoffRound);

        populateLeaf(leafId, bracketMessage.bracketUpdate.teamNumber,
            bracketMessage.bracketUpdate.teamName,
            bracketMessage.bracketUpdate.score,
            bracketMessage.bracketUpdate.verified);
    }
    if (bracketMessage.isDisplayUpdate) {
        // currently ignored, but may be useful in the future
        //console.log("Display update: " + bracketMessage.allBracketInfo);
    }
}

function socketOpened(_) {
    console.log("Socket opened");

    const allBracketInfo = [bracketInfo];

    const str = JSON.stringify(allBracketInfo);
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
