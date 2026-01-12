/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * High Tech Kids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

const width = screen.width - 10;
const height = screen.height - 10;
let displayWindow = null;
const windowOptions = 'height='
    + height
    + ',width='
    + width
    + ',toolbar=0,menubar=0,scrollbars=0,location=0,directories=0,status=0,resizable=0,fullscreen=1,left=0,screenX=0,top=0,screenY=0';

let connected = true;
let socket = null;

let displayUuid = null;
let displayUrl = null;
let displayName = "";

let pingIntervalId = null;

function messageReceived(event) {
    const message = JSON.parse(event.data);
    if (message.type == ASSIGN_UUID_MESSAGE_TYPE) {
        displayUuid = message.uuid;
    } else if (message.type == DISPLAY_URL_MESSAGE_TYPE) {
        displayPage(message.url);
    } else {
        console.log("Ignoring unexpected message type: " + message.type);
        console.log("Full message: " + event.data);
    }
}

function socketOpened(_event) {
    console.log("Socket opened");
    sendRegisterDisplay();

    if (null == pingIntervalId) {
        // send ping every minute
        pingIntervalId = setInterval(sendPing, 60 * 1000);
    }
}

function socketClosed(_event) {
    console.log("Socket closed");
    socket = null;

    if (null != pingIntervalId) {
        // stop sending pings until the socket is opened again
        clearInterval(pingIntervalId);
        pingIntervalId = null;
    }

    // open the socket a second later
    setTimeout(openSocket, 1000);
}

function openSocket() {
    console.log("opening socket");

    const url = window.location.pathname;
    const directory = url.substring(0, url.lastIndexOf('/'));
    const webSocketAddress = getWebsocketProtocol() + "//" + window.location.host + directory
        + "/DisplayEndpoint";

    socket = new WebSocket(webSocketAddress);
    socket.onmessage = messageReceived;
    socket.onopen = socketOpened;
    socket.onclose = socketClosed;
}


function displayPage(urlStr) {
    if (!displayUuid) {
        console.log("Error: trying to display page without UUID being set. URL: " + urlStr);
        return;
    }

    const newUrl = new URL(urlStr, window.location);
    const newParams = new URLSearchParams(newUrl.search);

    // add UUID to the URL so that the display page can use it to get the DisplayInfo object       
    newParams.append(DISPLAY_UUID_PARAMETER_NAME, displayUuid);
    newUrl.search = newParams;

    displayWindow = window.open(newUrl, 'displayWindow', windowOptions);
    if (!displayWindow || displayWindow.closed || typeof displayWindow.closed == 'undefined') {
        alert("For this page to work you need to disable the popup blocker for this site");
    }
}

function sendRegisterDisplay() {
    displayName = document.getElementById("name").value;
    if (displayName) {
        const message = new Object()
        message.type = REGISTER_DISPLAY_MESSAGE_TYPE;
        message.uuid = displayUuid;
        message.name = displayName;

        socket.send(JSON.stringify(message));
    }
}

function sendPing() {
    if (displayUuid) {
        const message = new Object()
        message.type = PING_MESSAGE_TYPE;
        message.uuid = displayUuid;

        socket.send(JSON.stringify(message));
    }
}

function updateNameInstructions() {
    if (displayName) {
        document.getElementById("display_name").innerText = displayName;
        document.getElementById("name_needed").classList.add("fll-sw-ui-inactive");
        document.getElementById("name_set").classList.remove("fll-sw-ui-inactive");
    } else {
        document.getElementById("name_needed").classList.remove("fll-sw-ui-inactive");
        document.getElementById("name_set").classList.add("fll-sw-ui-inactive");
    }
}

document.addEventListener("DOMContentLoaded", () => {
    // start out using the UUID from the parameters, if one was in the parameters
    displayUuid = PARAM_DISPLAY_UUID;

    updateNameInstructions();

    const nameButton = document.getElementById("name_button");
    nameButton.addEventListener("click", () => {
        const newName = document.getElementById("name").value;
        if (displayName != newName) {
            sendRegisterDisplay();
            updateNameInstructions();
        }
    });

    document.getElementById("name").addEventListener("keypress", (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            nameButton.click();
        }
    });

    openSocket();
});
