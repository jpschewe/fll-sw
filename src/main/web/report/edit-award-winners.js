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
// list of AwardWinnerData
var _challengeAwardWinnerData = [];

// list of AwardWinner
var _nonNumericAwardWinners = [];
// list of AwardWinnerData
var _nonNumericAwardWinnerData = [];

// list of OverallAwardWinner
var _nonNumericOverallAwardWinners = [];
// list of OverallAwardWinnerData
var _nonNumericOverallAwardWinnerData = [];

// name -> ScoreCategory
var _subjectiveCategories = {};

// name -> NonNumericCategory
var _nonNumericCategories = {};

// list of names
var _awardGroups = [];

// list of AdvancingTeam
var _advancingTeams = [];
// list of AdvancingTeamData
var _advancingTeamData = [];

function AwardWinnerData(nameFunc, awardGroup, teamElement, descriptionElement) {
    this.nameFunc = nameFunc;
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
        var categoryName = data.nameFunc();
        return new AwardWinner(categoryName, data.awardGroup, teamNumber,
            data.descriptionElement.val());
    } else {
        return null;
    }
}

/**
 * 
 * @param data
 *          the AwardWinnerData
 * @returns OverallAwardWinner or null if the team number is not set
 */
function createOverallAwardWinner(data) {
    var teamNumber = data.teamElement.val();
    if (teamNumber && teamNumber != "") {
        var categoryName = data.nameFunc();
        return new OverallAwardWinner(categoryName, teamNumber,
            data.descriptionElement.val());
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
        storeNonNumericWinners();
        storeNonNumericOverallWinners();
        storeAdvancingTeams();
        storeSortedGroups();
    });

    var advancingGroupCount = 1;
    $("#advancing-teams_add-group").click(function() {
        addAdvancingGroup("Group " + advancingGroupCount, true);
        advancingGroupCount = advancingGroupCount + 1;
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
        _log("No challenge winners to store");
    }
}

function storeNonNumericWinners() {
    var winners = [];
    $.each(_nonNumericAwardWinnerData, function(i, data) {
        var winner = createAwardWinner(data);
        if (winner) {
            winners.push(winner);
        }
    });

    if (winners.length) {
        // send to server
        var jsonData = JSON.stringify(winners);
        $.ajax({
            type: "POST",
            dataType: "json",
            contentType: "application/json",
            url: "/api/NonNumericAwardWinners",
            data: jsonData,
            succes: function(response) {
                if (!response.success) {
                    alert("Error sending extra award winners: " + response.message);
                }
            }
        });
    } else {
        _log("No extra winners to store");
    }
}

function storeNonNumericOverallWinners() {
    var winners = [];
    $.each(_nonNumericOverallAwardWinnerData, function(i, data) {
        var winner = createOverallAwardWinner(data);
        if (winner) {
            winners.push(winner);
        }
    });

    if (winners.length) {
        // send to server
        var jsonData = JSON.stringify(winners);
        $.ajax({
            type: "POST",
            dataType: "json",
            contentType: "application/json",
            url: "/api/NonNumericOverallAwardWinners",
            data: jsonData,
            success: function(response) {
                if (!response.success) {
                    alert("Error sending overall award winners: " + response.message);
                }
            }
        });
    } else {
        _log("No overall winners to store");
    }
}

function storeAdvancingTeams() {
    var advancing = [];
    $.each(_advancingTeamData, function(i, data) {
        var adTeam = createAdvancingTeam(data);
        if (adTeam) {
            advancing.push(adTeam);
        }
    });

    if (advancing.length) {
        // send to server
        var jsonData = JSON.stringify(advancing);
        $.ajax({
            type: "POST",
            dataType: "json",
            contentType: "application/json",
            url: "/api/AdvancingTeams",
            data: jsonData,
            success: function(response) {
                if (!response.success) {
                    alert("Error sending advancing teams: " + response.message);
                }
            }
        });
    } else {
        _log("No advancing teams to store");
    }
}

function initPage() {
    initChallengeWinners();
    initNonNumericWinners();
    initNonNumericOverallWinners();
    initAdvancingTeams();
}

function initChallengeWinners() {
    _challengeAwardWinnerData = [];
    $("#challenge-award-winners").empty();

    $.each(_subjectiveCategories, function(i, category) {
        var name = category.title;

        var categoryNameFunc = function() {
            return category.title;
        };

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
                addTeam(null, _challengeAwardWinnerData, categoryNameFunc, awardGroup,
                    teamList);
            });

            var enterNewTeam = true;
            $.each(_challengeAwardWinners, function(i, winner) {
                if (winner.name == name && winner.awardGroup == awardGroup) {
                    addTeam(winner, _challengeAwardWinnerData, categoryNameFunc,
                        awardGroup, teamList);
                    enterNewTeam = false;
                }
            }); // foreach loaded winner

            if (enterNewTeam) {
                // add an empty team if there weren't any loaded from the server
                addTeam(null, _challengeAwardWinnerData, categoryNameFunc, awardGroup,
                    teamList);
            }

        }); // foreach award group

    }); // foreach category

}

function initNonNumericWinners() {
    _nonNumericAwardWinnerData = [];
    $("#non-numeric-award-winners").empty();

    $.each(_nonNumericCategories, function(i, category) {
        if (category.perAwardGroup) {
            var name = category.title;

            var categoryNameFunc = function() {
                return category.title;
            };

            var categoryItem = $("<li>" + name + "</li>");
            $("#non-numeric-award-winners").append(categoryItem);
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
                    addTeam(null, _nonNumericAwardWinnerData, categoryNameFunc,
                        awardGroup, teamList);
                });

                var enterNewTeam = true;
                $.each(_nonNumericAwardWinnerData, function(i, winner) {
                    if (winner.name == name && winner.awardGroup == awardGroup) {
                        addTeam(winner, _nonNumericAwardWinnerData, categoryNameFunc,
                            awardGroup, teamList);
                        enterNewTeam = false;
                    }
                }); // foreach loaded winner

                if (enterNewTeam) {
                    // add an empty team if there weren't any loaded from the server
                    addTeam(null, _nonNumericAwardWinnerData, categoryNameFunc,
                        awardGroup, teamList);
                }

            }); // foreach award group
        } // per award group
    }); // foreach category

}

function initNonNumericOverallWinners() {
    _nonNumericOverallAwardWinnerData = [];
    $("#non-numeric-overall-award-winners").empty();

    $.each(_nonNumericCategories, function(i, category) {
        if (!category.perAwardGroup) {
            var name = category.title;

            var categoryNameFunc = function() {
                return category.title;
            };

            var categoryItem = $("<li>" + name + "</li>");
            $("#non-numeric-overall-award-winners").append(categoryItem);
            var categoryList = $("<ul></ul>");
            categoryItem.append(categoryList);

            var addTeamButton = $("<button>Add Team</button>");
            categoryItem.append(addTeamButton);

            var teamList = $("<ul></ul>");
            categoryItem.append(teamList);

            addTeamButton.click(function() {
                addTeam(null, _nonNumericOverallAwardWinnerData, categoryNameFunc,
                    null, teamList);
            });

            var enterNewTeam = true;
            $.each(_nonNumericOverallAwardWinnerData, function(i, winner) {
                if (winner.name == name && winner.awardGroup == awardGroup) {
                    addTeam(winner, _nonNumericOverallAwardWinnerData, categoryNameFunc,
                        null, teamList);
                    enterNewTeam = false;
                }
            }); // foreach loaded winner

            if (enterNewTeam) {
                // add an empty team if there weren't any loaded from the server
                addTeam(null, _nonNumericOverallAwardWinnerData, categoryNameFunc,
                    null, teamList);
            }
        } // overall award

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
function addTeam(awardWinner, dataList, categoryNameFunc, awardGroup, teamList) {

    var teamEle = $("<li></li>");
    teamList.append(teamEle);

    var numEle = $("<input type='text' />");
    teamEle.append(numEle);

    var nameEle = $("<input readonly disabled />");
    teamEle.append(nameEle);

    var orgEle = $("<input readonly disabled />");
    teamEle.append(orgEle);

    var agEle = $("<input readonly disabled />");
    teamEle.append(agEle);

    teamEle.append("<br/>");
    var descriptionEle = $("<textarea cols='80' rows='3'/>");
    teamEle.append(descriptionEle);

    var data = new AwardWinnerData(categoryNameFunc, awardGroup, numEle,
        descriptionEle);
    dataList.push(data);

    numEle.change(function() {
        var teamNum = $(this).val();
        var prevTeam = $(this).data('oldVal');
        if (!teamNum || "" == teamNum) {
            nameEle.val("");
            orgEle.val("");
            agEle.val("");
        } else {
            var team = _teams[teamNum];
            if (typeof (team) == 'undefined') {
                alert("Team number " + teamNum + " does not exist");
                $(this).val(prevTeam);
                teamNum = prevTeam; // for the set of oldVal below
            } else {
                nameEle.val(team.teamName);
                orgEle.val(team.organization);
                agEle.val(team.awardGroup);
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
        numEle.change();
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

function loadNonNumericCategories() {
    _nonNumericCategories = {};

    return $.getJSON("/api/ChallengeDescription/NonNumericCategories", function(
        categories) {
        $.each(categories, function(i, category) {
            _nonNumericCategories[category.title] = category;
        });
    });
}

function loadChallengeAwardWinners() {
    _challengeAwardWinners = [];

    return $.getJSON("/api/SubjectiveChallengeAwardWinners", function(winners) {
        _challengeAwardWinners = winners;
    });
}

function loadExtraAwardWinners() {
    _nonNumericAwardWinners = [];

    return $.getJSON("/api/NonNumericAwardWinners", function(winners) {
        _nonNumericAwardWinners = winners;
    });
}

function loadOverallAwardWinners() {
    _nonNumericOverallAwardWinners = [];

    return $.getJSON("/api/NonNumericOverallAwardWinners", function(winners) {
        _nonNumericOverallAwardWinners = winners;
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

function loadAdvancingTeams() {
    _advancingTeams = [];

    return $.getJSON("/api/AdvancingTeams", function(data) {
        _advancingTeams = data;
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

    var nonNumericCategoriesPromise = loadNonNumericCategories();
    nonNumericCategoriesPromise.fail(function() {
        failCallback("Non-numeric Categories");
    });
    waitList.push(nonNumericCategoriesPromise);

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

    var loadExtraAwardWinnersPromise = loadExtraAwardWinners();
    loadExtraAwardWinnersPromise.fail(function() {
        failCallback("Extra award winners");
    });
    waitList.push(loadExtraAwardWinnersPromise);

    var loadOverallAwardWinnersPromise = loadOverallAwardWinners();
    loadOverallAwardWinnersPromise.fail(function() {
        failCallback("Overall award winners");
    });
    waitList.push(loadOverallAwardWinnersPromise);

    var loadAwardGroupsPromise = loadAwardGroups();
    loadAwardGroupsPromise.fail(function() {
        failCallback("Award groups");
    });
    waitList.push(loadAwardGroupsPromise);

    var loadAdvancingTeamsPromise = loadAdvancingTeams();
    loadAdvancingTeamsPromise.fail(function() {
        failCallback("Advancing Teams");
    });
    waitList.push(loadAdvancingTeamsPromise);

    var loadSortedGroupsPromise = loadSortedGroups();
    loadSortedGroupsPromise.fail(function() {
        failCallback("Sorted groups");
    });
    waitList.push(loadSortedGroupsPromise);

    $.when.apply($, waitList).done(function() {
        doneCallback();
    });
}

function AdvancingTeamData(nameFunc, teamElement) {
    this.nameFunc = nameFunc;
    this.teamElement = teamElement;
}

/**
 * 
 * @param data
 *          the AdvancingTeamData
 * @returns AdvancingTeam or null if the team number is not set
 */
function createAdvancingTeam(data) {
    var teamNumber = data.teamElement.val();
    if (teamNumber && teamNumber != "") {
        var groupName = data.nameFunc();
        return new AdvancingTeam(teamNumber, groupName);
    } else {
        return null;
    }
}

function initAdvancingTeams() {
    _advancingTeamData = [];
    $("#advancing-teams").empty();

    $.each(_awardGroups, function(i, group) {
        addAdvancingGroup(group, false);
    });

    var knownGroups = [];
    $.each(_advancingTeams, function(i, advancing) {
        if (!knownGroups.includes(advancing.group)
            && !_awardGroups.includes(advancing.group)) {
            knownGroups.push(advancing.group);
        }
    });

    $.each(knownGroups, function(i, group) {
        addAdvancingGroup(group, true);
    });
}

function addAdvancingGroup(group, editable) {
    var groupItem = $("<li></li>");
    $("#advancing-teams").append(groupItem);

    var nameFunc;
    var groupEle;
    if (editable) {
        groupEle = $("<input type='text' />");
        nameFunc = function() {
            return groupEle.val();
        };
        groupEle.val(group);

        groupEle.data('oldVal', group);
        groupEle.change(function() {
            var newName = $(this).val();
            var oldValue = $(this).data('oldVal');
            if (null == newName || "" == newName) {
                alert("All groups must have non-empty names");
                $(this).val(oldValue);
            } else {
                renameGroupToSort(oldValue, newName);
                $(this).data('oldVal', newName);
            }
        });
    } else {
        if (!group) {
            alert("Internal Error: Cannot have an unnammed non-editable group");
            return;
        }

        groupEle = $("<span>" + group + "</span>");
        nameFunc = function() {
            return group;
        }
    }
    groupItem.append(groupEle);

    var groupList = $("<ul></ul>");
    groupItem.append(groupList);

    var addTeamButton = $("<button>Add Team</button>");
    groupItem.append(addTeamButton);

    var teamList = $("<ul></ul>");
    groupItem.append(teamList);

    addTeamButton.click(function() {
        addAdvancingTeam(null, _advancingTeamData, nameFunc, teamList);
    });

    var enterNewTeam = true;
    if (group) {
        $.each(_advancingTeams, function(i, advancing) {
            if (advancing.group == group) {
                addAdvancingTeam(advancing, _advancingTeamData, nameFunc, teamList);
                enterNewTeam = false;
            }
        }); // foreach loaded winner
    }

    if (enterNewTeam) {
        // add an empty team if there weren't any loaded from the server
        addAdvancingTeam(null, _advancingTeamData, nameFunc, teamList);
    }

    addGroupToSort(nameFunc());
}

/**
 * Add a team section to the advancing team specified list. Update dataList with
 * an instance of AdvancingTeamData for this element.
 * 
 * @param advancing
 *          used to populate the elements (may be null)
 * @param dataList
 *          list of AwardWinnerData objects (modified)
 * @param groupNameFunc
 *          function to get the group name
 * @param teamList
 *          the list element to add to
 */
function addAdvancingTeam(advancing, dataList, groupNameFunc, teamList) {

    var teamEle = $("<li></li>");
    teamList.append(teamEle);

    var numEle = $("<input type='text' />");
    teamEle.append(numEle);

    var nameEle = $("<input readonly disabled />");
    teamEle.append(nameEle);

    var orgEle = $("<input readonly disabled />");
    teamEle.append(orgEle);

    var agEle = $("<input readonly disabled />");
    teamEle.append(agEle);

    var data = new AdvancingTeamData(groupNameFunc, numEle);
    dataList.push(data);

    numEle.change(function() {
        var teamNum = $(this).val();
        var prevTeam = $(this).data('oldVal');
        if (!teamNum || "" == teamNum) {
            nameEle.val("");
            orgEle.val("");
            agEle.val("");
        } else {
            var team = _teams[teamNum];
            if (typeof (team) == 'undefined') {
                alert("Team number " + teamNum + " does not exist");
                $(this).val(prevTeam);
                teamNum = prevTeam; // for the set of oldVal below
            } else {
                nameEle.val(team.teamName);
                orgEle.val(team.organization);
                agEle.val(team.awardGroup);
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

    if (advancing) {
        numEle.val(advancing.teamNumber);
        numEle.change();
    }

}

function loadSortedGroups() {
    return $.getJSON("/api/AwardsReportSortedGroups", function(data) {
        initSortedGroups(data);
    });
}

function initSortedGroups(sortedGroups) {
    $("#award-group-order").empty();
    if (sortedGroups) {
        $.each(sortedGroups, function(i, group) {
            addGroupToSort(group);
        });
    }

}

function addGroupToSort(group) {
    var numGroups = 0;
    var addGroup = true;
    $("#award-group-order").children("li").each(function(i, le) {
        var leGroup = $(le).attr("data-group-name");
        if (leGroup == group) {
            addGroup = false;
        }
        numGroups = numGroups + 1;
    })

    if (addGroup) {
        var listElement = $("<li data-group-name='" + group + "'></li>");
        $("#award-group-order").append(listElement);

        var indexElement = $("<input type='number' step='1' value='"
            + (numGroups + 1) + "'></input>");
        listElement.append(indexElement);

        var nameElement = $("<span>" + group + "</span>");
        listElement.append(nameElement);
    }
}

function removeGroupToSort(group) {
    var toRemove = null;
    $("#award-group-order").children("li").each(function(i, le) {
        var leGroup = $(le).attr("data-group-name");
        if (leGroup == group) {
            toRemove = le;
        }
    })

    if (toRemove) {
        toRemove.parentNode.removeChild(toRemove);
    }
}

function renameGroupToSort(oldName, newName) {
    var element = null;
    $("#award-group-order").children("li").each(function(i, le) {
        var leGroup = $(le).attr("data-group-name");
        if (leGroup == oldName) {
            element = le;
        }
    })

    if (element) {
        $(element).attr("data-group-name", newName);
        $(element).children("span").each(function(i, span) {
            span.innerText = newName;
        });
    }
}

function storeSortedGroups() {
    // gather list elements that have the group sort information
    var groupElements = [];
    $("#award-group-order").children("li").each(function(i, le) {
        groupElements.push(le);
    });

    // sort the elements
    groupElements.sort(function(a, b) {
        var aIndex = $(a.getElementsByTagName("input")[0]).val();
        var bIndex = $(b.getElementsByTagName("input")[0]).val();
        return aIndex - bIndex;
    });

    // convert to a list of strings
    var groups = [];
    $.each(groupElements, function(i, le) {
        var group = $(le).attr("data-group-name");
        groups.push(group);
    });

    // send to server
    var jsonData = JSON.stringify(groups);
    $.ajax({
        type: "POST",
        dataType: "json",
        contentType: "application/json",
        url: "/api/AwardsReportSortedGroups",
        data: jsonData,
        success: function(response) {
            if (!response.success) {
                alert("Error sending group sort information: " + response.message);
            }
        }
    });

}