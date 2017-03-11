/*
 * Copyright (c) 2017 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

function populateLeaf(leafId, teamNumber, teamName, score, verified) {
  var text = "";
  if (null != teamNumber && "" != teamNumber && teamNumber >= 0) {
    text = "<span class='TeamNumber'>#" + teamNumber + "</span>";

    text = text + "&nbsp;<span class='TeamName'>" + teamName + "</span>";

    if (null != score && "" != score) {
      if (!verified) {
        text = text + "<span style='color:red'>";
      }

      text = text + "<span class='TeamScore'>&nbsp;Score: " + score + "</span>";

      if (!verified) {
        text = text + "</span>";
      }
    }
  }

  $("#" + leafId).html(text);
}

function messageReceived(event) {

  console.log("received: " + event.data);
  var bracketUpdate = JSON.parse(event.data);
  var leafId = constructLeafId(bracketIndex, bracketUpdate.dbLine,
      bracketUpdate.playoffRound);

  populateLeaf(leafId, bracketUpdate.teamNumber, bracketUpdate.teamName,
      bracketUpdate.score, bracketUpdate.verified);
}

function socketOpened(event) {
  console.log("Socket opened");

  // needs to match H2HUpdateWebSocket.BracketInfo
  var bracketInfo = [ {
    bracketName : bracketName,
    firstRound : firstRound,
    lastRound : lastRound
  } ];

  var str = JSON.stringify(bracketInfo);
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
