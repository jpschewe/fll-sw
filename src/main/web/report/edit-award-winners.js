/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use-strict";

var _tournament = null;

// team number -> team
var _teams = {};

// list of AwardWinner
var _challengeAwardWinners = [];
var _challengeAwardWinnerData = [];

// name -> ScoreCategory
var _subjectiveCategories = {};

// list of names
var _awardGroups = [];

function AwardWinnerData(name, awardGroup, teamElement, descriptionElement) {
  this.name = name;
  this.awardGroup = awardGroup;
  this.teamElement = teamElement;
  this.descriptionElement = descriptionElement;
}

/**
 * 
 * @param data
 *          the AwardWinnerData
 * @returns AwardWinner or null if the team number is not set
 */
function createAwardWinner(data) {
  var teamNumber = data.teamElement.val();
  if (teamNumber && teamNumber != "") {
    return new AwardWinner(data.name, data.awardGroup, teamNumber,
        data.descriptionElement.val())
  } else {
    return null;
  }
}

$(document).ready(function() {

  loadFromServer(function() {
    if (0 == _subjectiveCategories.length) {
      alert("No subjective categories loaded from server");
    } else {
      initPage();
    }
  }, function(message) {
    alert("Error getting data from server: " + message);
  });

  $("#store_winners").click(function() {
    storeChallengeWinners();
  });

}); // end ready function

function storeChallengeWinners() {
  var winners = [];
  $.each(_challengeAwardWinnerData, function(i, data) {
    var winner = createAwardWinner(data);
    if (winner) {
      winners.push(winner);
    }
  });

  if (winners.length) {
    // send to server
    var jsonData = JSON.stringify(winners);
    $
        .post("/api/SubjectiveChallengeAwardWinners", jsonData,
            function(response) {
              if (!response.success) {
                alert("Error sending challenge award winners: "
                    + response.message);
              }
            }, 'json');
  } else {
    _log("No winners to store");
  }
}

function initPage() {
  initChallengeWinners();

}

function initChallengeWinners() {
  _challengeAwardWinnerData = [];
  $("#challenge-award-winners").empty();
  var list = $("<ul></ul>");

  $.each(_subjectiveCategories, function(i, category) {
    var name = category.title;

    var categoryItem = $("<li>" + name + "</li>");
    $("#challenge-award-winners").append(categoryItem);
    var categoryList = $("<ul></ul>");
    categoryItem.append(categoryList);

    $.each(_awardGroups, function(i, awardGroup) {
      var awardGroupItem = $("<li>" + awardGroup + "</li>");
      categoryList.append(awardGroupItem);

      var addTeamButton = $("<button>Add Team</button>");
      awardGroupItem.append(addTeamButton);

      var teamList = $("<ul></ul>");
      awardGroupItem.append(teamList);

      addTeamButton.click(function() {
        addTeam(null, _challengeAwardWinnerData, category.title, awardGroup,
            teamList);
      });

      var enterNewTeam = true;
      $.each(_challengeAwardWinners, function(i, winner) {
        if (winner.name == name && winner.awardGroup == awardGroup) {
          addTeam(winner, _challengeAwardWinnerData, category.title,
              awardGroup, teamList);
          enterNewTeam = false;
        }
      }); // foreach loaded winner

      if (enterNewTeam) {
        // add an empty team if there weren't any loaded from the server
        addTeam(null, _challengeAwardWinnerData, category.title, awardGroup,
            teamList);
      }

    }); // foreach award group

  }); // foreach category

}

/**
 * Add a team section to the specified list. Update dataList with an instance of
 * AwardWinnerData for this element.
 * 
 * @param awardWinner
 *          used to populate the elements (may be null)
 * @param dataList
 *          list of AwardWinnerData objects (modified)
 * @param category
 *          the category
 * @param awardGroup
 *          the award group (may be null)
 * @param teamList
 *          the list element to add to
 */
function addTeam(awardWinner, dataList, category, awardGroup, teamList) {

  var teamEle = $("<li></li>");
  teamList.append(teamEle);

  var numEle = $("<input type='text' />");
  teamEle.append(numEle);

  var nameEle = $("<input readonly disabled />");
  teamEle.append(nameEle);

  var orgEle = $("<input readonly disabled />");
  teamEle.append(orgEle);

  teamEle.append("<br/>");
  var descriptionEle = $("<textarea cols='80' rows='3'/>");
  teamEle.append(descriptionEle);

  var data = new AwardWinnerData(category, awardGroup, numEle, descriptionEle);
  dataList.push(data);

  numEle.change(function() {
    var teamNum = $(this).val();
    var prevTeam = $(this).data('oldVal');
    if (!teamNum || "" == teamNum) {
      nameEle.val("");
      orgEle.val("");
    } else {
      var team = _teams[teamNum];
      if (typeof (team) == 'undefined') {
        alert("Team number " + teamNum + " does not exist");
        $(this).val(prevTeam);
        teamNum = prevTeam; // for the set of oldVal below
      } else {
        nameEle.val(team.teamName);
        orgEle.val(team.organization);
      }
    }
    $(this).data('oldVal', teamNum);
  });

  var deleteButton = $("<button>Delete</button>");
  teamEle.append(deleteButton);
  deleteButton.click(function() {
    var teamNum = numEle.val();
    var reallyDelete = false;
    if ("" != teamNum) {
      reallyDelete = confirm("Are you sure you want to delete this team?");
    } else {
      reallyDelete = true;
    }
    if (reallyDelete) {
      teamEle.remove();
      removeFromArray(dataList, data);
    }
  });

  if (awardWinner) {
    numEle.val(awardWinner.teamNumber);
    descriptionEle.val(awardWinner.description);
  }

}

function loadTournament() {
  _tournament = null;

  return $.getJSON("/api/Tournaments/current", function(tournament) {
    _tournament = tournament;
  });
}

function loadTeams() {
  _teams = {};

  return $.getJSON("/api/TournamentTeams", function(teams) {
    $.each(teams, function(i, team) {
      _teams[team.teamNumber] = team;
    });
  });
}

/**
 * Load the subjective categories.
 * 
 * @returns the promise for the ajax query
 */
function loadSubjectiveCategories() {
  _subjectiveCategories = {};

  return $.getJSON("/api/ChallengeDescription/SubjectiveCategories", function(
      subjectiveCategories) {
    $.each(subjectiveCategories, function(i, scoreCategory) {
      _subjectiveCategories[scoreCategory.name] = scoreCategory;
    });
  });
}

/**
 * Load the challenge award winners.
 * 
 * @returns the promise for the ajax query
 */
function loadChallengeAwardWinners() {
  _challengeAwardWinners = [];

  return $.getJSON("/api/SubjectiveChallengeAwardWinners", function(winners) {
    _challengeAwardWinners = winners;
  });
}

/**
 * Load the award groups.
 * 
 * @returns the promise for the ajax query
 */
function loadAwardGroups() {
  _awardGroups = [];

  return $.getJSON("/api/AwardGroups", function(data) {
    _awardGroups = data;
  });
}

/**
 * Load all data from server.
 * 
 * @param doneCallback
 *          called with no arguments on success
 * @param failCallback
 *          called with no arguments on failure
 */
function loadFromServer(doneCallback, failCallback) {

  _log("Loading from server");

  var waitList = []

  var subjectiveCategoriesPromise = loadSubjectiveCategories();
  subjectiveCategoriesPromise.fail(function() {
    failCallback("Subjective Categories");
  });
  waitList.push(subjectiveCategoriesPromise);

  var tournamentPromise = loadTournament();
  tournamentPromise.fail(function() {
    failCallback("Tournament");
  });
  waitList.push(tournamentPromise);

  var teamsPromise = loadTeams();
  teamsPromise.fail(function() {
    failCallback("Teams");
  });
  waitList.push(teamsPromise);

  var loadChallengeAwardWinnersPromise = loadChallengeAwardWinners();
  loadChallengeAwardWinnersPromise.fail(function() {
    failCallback("Challenge award winners");
  });
  waitList.push(loadChallengeAwardWinnersPromise);

  var loadAwardGroupsPromise = loadAwardGroups();
  loadAwardGroupsPromise.fail(function() {
    failCallback("Award groups");
  });
  waitList.push(loadAwardGroupsPromise);

  $.when.apply($, waitList).done(function() {
    doneCallback();
  });
}
