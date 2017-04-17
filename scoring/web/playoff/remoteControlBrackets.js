/*
 * Copyright (c) 2017 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

function isDisplayInfoDifferent(newBracketInfo) {

  if (allBracketInfo.length != newBracketInfo.length) {
    return true;
  }

  var length = allBracketInfo.length
  for (var i = 0; i < length; i++) {
    // Check if we have nested arrays
    var oldBracketInfo = allBracketInfo[i];
    var newBracketInfo = newBracketInfo[i];

    if (oldBracketInfo.bracketName != newBracketInfo.bracketName) {
      return true;
    }
    if (oldBracketInfo.firstRound != newBracketInfo.firstRound) {
      return true;
    }
  }

  return false;
}

function messageReceived(event) {

  console.log("received: " + event.data);
  var bracketMessage = JSON.parse(event.data);
  if (bracketMessage.isBracketUpdate) {
    console.log("Received bracket update");
    /*
     * if (bracketMessage.bracketUpdate.bracketName != bracketInfo.bracketName) { //
     * not for us return; }
     * 
     * var leafId = constructLeafId(bracketInfo.bracketIndex,
     * bracketMessage.bracketUpdate.dbLine,
     * bracketMessage.bracketUpdate.playoffRound);
     * 
     * populateLeaf(leafId, breacketMessage.bracketUpdate.teamNumber,
     * bracketMessage.bracketUpdate.teamName,
     * bracketMessage.bracketUpdate.score,
     * bracketMessage.bracketUpdate.verified);
     */
  }
  if (bracketMessage.isDisplayUpdate) {
    if (isDisplayInfoDifferent(bracketMessage.allBracketInfo)) {
      // reload
      location.reload();
    }
  }
}

function socketOpened(event) {
  console.log("Socket opened");

  var str = JSON.stringify(allBracketInfo);
  this.send(str);
}

function socketClosed(event) {
  console.log("Socket closed");

  // open the socket a second later
  setTimeout(openSocket, 1000);
}

function openSocket() {
  console.log("opening socket");

  var url = window.location.pathname;
  var directory = url.substring(0, url.lastIndexOf('/'));
  var webSocketAddress = "ws://" + window.location.host + directory
      + "/H2HUpdateWebSocket";

  var socket = new WebSocket(webSocketAddress);
  socket.onmessage = messageReceived;
  socket.onopen = socketOpened;
  socket.onclose = socketClosed;
}

$(document).ready(function() {
  console.log("Top of ready");
  openSocket();
});
