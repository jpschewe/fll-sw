/*
 * Copyright (c) 2022 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";


function editFlagBoxClicked() {
    const text = document.getElementById('select_number_text');
    if (document.selectTeam.EditFlag.checked) {
        document.selectTeam.RunNumber.disabled = false;
        text.style.color = "black";
    } else {
        document.selectTeam.RunNumber.disabled = true;
        text.style.color = "gray";
    }
}

function reloadRuns() {
    document.body.removeChild(document.getElementById('reloadruns'));
    document.verify.TeamNumber.length = 0;
    const s = document.createElement('script');
    s.type = 'text/javascript';
    s.id = 'reloadruns';
    s.src = 'UpdateUnverifiedRuns?' + Math.random();
    document.body.appendChild(s);
}

function messageReceived(event) {
    console.log("received: " + event.data);

    // data doesn't matter, just reload runs on any message
    reloadRuns();
}

function socketOpened(event) {
    console.log("Socket opened");
}

function socketClosed(event) {
    console.log("Socket closed");

    // open the socket a second later
    setTimeout(openSocket, 1000);
}

function openSocket() {
    console.log("opening socket");

    const url = window.location.pathname;
    const directory = url.substring(0, url.lastIndexOf('/'));
    const webSocketAddress = getWebsocketProtocol() + "//" + window.location.host
        + directory + "/UnverifiedRunsWebSocket";

    const socket = new WebSocket(webSocketAddress);
    socket.onmessage = messageReceived;
    socket.onopen = socketOpened;
    socket.onclose = socketClosed;
}

function populateTeamsSelect() {
    const selectBox = document.getElementById("select-teamnumber");
    removeChildren(selectBox);

    for (const teamData of teamSelectData) {
        const option = document.createElement("option");
        option.value = teamData.team.teamNumber;
        option.innerText = teamData.displayString;
        selectBox.appendChild(option);
    }
}

document.addEventListener('DOMContentLoaded', function() {
    if (!tabletMode) {
        editFlagBoxClicked();
    }
    populateTeamsSelect();

    if (!scoreEntrySelectedTable) {
        // only use unverified code when not using the tablets 

        reloadRuns();
        openSocket();
    }
});