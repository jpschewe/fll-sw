/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * High Tech Kids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

const width = screen.width - 10;
const height = screen.height - 10;
let newWindow = null;
const windowOptions = 'height='
    + height
    + ',width='
    + width
    + ',toolbar=0,menubar=0,scrollbars=0,location=0,directories=0,status=0,resizable=0,fullscreen=1,left=0,screenX=0,top=0,screenY=0';

let connected = true;
let socket = null;

let displayUuid = "";
let displayUrl = null;
let displayName = "";

function messageReceived(event) {
    console.log("received: " + event.data);

    //FIXME read type, expecting ASSIGN_UUID or DISPLAY_URL
    // data doesn't matter, just execute reload on any message    
}

function socketOpened(event) {
    console.log("Socket opened");
    sendRegisterDisplay();
}

function socketClosed(event) {
    console.log("Socket closed");
    socket = null;

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


function displayPage(url) {
    //FIXME need to add the display_uuid parameter
    if (null == newWindow || newWindow.location.pathname != url) {
        newWindow = window.open(url, 'displayWindow', windowOptions);
        if (!newWindow || newWindow.closed || typeof newWindow.closed == 'undefined') {
            alert("For this page to work you need to disable the popup blocker for this site");
        }
    }
}

function pollSuccess(newURL) {
    if (!connected) {
        connected = true;

        // display welcome for a second so that the browser doesn't cache the page currenty displayed
        displayPage('<c:url value="/welcome.jsp"/>');

        // display the page that we want to see
        setTimeout(function() {
            displayPage(newURL);
        }, 1000);
    } else {
        displayPage(newURL);
    }
}

function pollFailure() {
    connected = false;

    console.log("Got failure getting current display page, reconnecting...");

    if (null != socket) {
        socket.onclose = function() {
        }; // ensure that the closeSocket method doesn't get called and cause a race
        socket.close();
        socket = null;
    }

    // open the socket a second later
    setTimeout(openSocket, 1000);
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

    document.getElementById("name_button").addEventListener("click", () => {
        const newName = document.getElementById("name").value;
        if (displayName != newName) {
            sendRegisterDisplay();
            updateNameInstructions();
        }
    });

    openSocket();
});
