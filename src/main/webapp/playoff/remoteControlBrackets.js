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

  var length = allBracketData.length
  for (var i = 0; i < length; i++) {
    // Check if we have nested arrays
    var oldBracketInfo = allBracketData[i];
    var newBracketInfo = newAllBracketData[i];

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

function getTeamNameString(teamNumber, teamName) {
  var text = "";
  if (teamNumber >= 0) {
    text += "<span class=\"TeamNumber\">#" + teamNumber + "</span> "
  }
  text += "<span class=\"TeamName\">" + parseTeamName(teamName) + "</span>";
  return text;
}

function getTeamNameAndScoreString(teamNumber, teamName, scoreData) {
  return "<span class=\"TeamNumber\">#" + teamNumber
      + "</span> <span class=\"TeamName\">" + parseTeamName(teamName)
      + "</span><span class=\"TeamScore\"> Score: " + scoreData + "</span>";
}

function populateLeaf(leafId, bracketUpdate) {
  var text = "";

  // Some of these if/else statements can be collapsed, however I've broken them
  // out to make it clear what each case is.

  if (null != bracketUpdate.teamNumber && "" != bracketUpdate.teamNumber) {
    // have a valid team number, otherwise clear the cell

    if (bracketUpdate.teamNumber < 0) {
      // tie or bye
      text = getTeamNameString(bracketUpdate.teamNumber, bracketUpdate.teamName);
    } else if (bracketUpdate.playoffRound >= bracketUpdate.maxPlayoffRound) {

      // finals, don't display the finals
      text = "";

    } else if (!bracketUpdate.verified) {

      // score isn't verified, only show the team name
      text = getTeamNameString(bracketUpdate.teamNumber, bracketUpdate.teamName);

    } else if (null == bracketUpdate.score || "" == bracketUpdate.score) {

      // no score, only show the team name
      text = getTeamNameString(bracketUpdate.teamNumber, bracketUpdate.teamName);

    } else if (bracketUpdate.playoffRound == bracketUpdate.maxPlayoffRound - 1) {

      // scores from the last head to head round can't be displayed, because
      // then the winner would be known, only show team name
      text = getTeamNameString(bracketUpdate.teamNumber, bracketUpdate.teamName);

    } else if (bracketUpdate.noShow) {

      // no show, explicitly set score text
      text = getTeamNameAndScoreString(bracketUpdate.teamNumber,
          bracketUpdate.teamName, "No Show");

    } else {

      // verified score
      text = getTeamNameAndScoreString(bracketUpdate.teamNumber,
          bracketUpdate.teamName, bracketUpdate.score);

    }
  } else {
    console.log("No team number, clearing leaf: " + leafId);
  }

  $("#" + leafId).html(text);
}

function placeTableLabel(lid, table) {
  if (null != table && "" != table) {
    $("#" + lid + "-table").text(table);
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

  $(".table_assignment")
      .each(
          function(index, label) {
            // Sane color? Let's start by splitting the label text
            if ($.inArray(label.innerHTML.split(" ")[0].toLowerCase(),
                validColors) > 0) {
              label.style.borderColor = label.innerHTML.split(" ")[0];
              label.style.borderStyle = "solid";
            }
          });
}

function handleBracketUpdate(bracketUpdate) {
  console.log("Received bracket update");

  $.each(allBracketData, function(index, bracketData) {

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

  console.log("received: " + event.data);
  var bracketMessage = JSON.parse(event.data);
  if (bracketMessage.isDisplayUpdate) {
    if (isDisplayInfoDifferent(bracketMessage.allBracketInfo)) {
      // reload
      location.reload();
    }
  }
  if (bracketMessage.isBracketUpdate) {
    handleBracketUpdate(bracketMessage.bracketUpdate);
  }
}

function socketOpened(event) {
  console.log("Socket opened");

  var str = JSON.stringify(allBracketData);
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
