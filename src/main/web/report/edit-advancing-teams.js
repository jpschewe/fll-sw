/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

// list of names
let _awardGroups = [];

// list of AdvancingTeam
let _advancingTeams = [];
// list of AdvancingTeamData
let _advancingTeamData = [];

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
    const teamNumber = data.teamElement.value;
    if (teamNumber && teamNumber != "") {
        const categoryName = data.nameFunc();
        return new AwardWinner(categoryName, data.awardGroup, teamNumber,
            data.descriptionElement.value);
    } else {
        return null;
    }
}

document.addEventListener('DOMContentLoaded', function() {

    loadFromServer(function() {
        initPage();
    }, function(message) {
        alert("Error getting data from server: " + message);
    });

    document.getElementById("store_winners").addEventListener('click', function() {
        const fail_callback = function(msg) {
            alert(msg);
        };

        const promises = [];
        const advancingPromise = storeAdvancingTeams(fail_callback);
        promises.push(advancingPromise);

        const sortedGroupsPromise = storeSortedGroups(fail_callback);
        promises.push(sortedGroupsPromise);

        Promise.all(promises).catch(function(msg) {
            alert("Error storing data: " + msg);
        }).then(function(_) {
            alert("Advancing Teams stored on the server")
        });

    });

    let advancingGroupCount = 1;
    document.getElementById("advancing-teams_add-group").addEventListener('click', function() {
        addAdvancingGroup("Group " + advancingGroupCount, true);
        advancingGroupCount = advancingGroupCount + 1;
    });

}); // end ready function


/**
 * @return a promise that stores the advancing teams
 */
function storeAdvancingTeams() {
    const advancing = [];
    _advancingTeamData.forEach(function(data) {
        const adTeam = createAdvancingTeam(data);
        if (adTeam) {
            advancing.push(adTeam);
        }
    });

    // send to server
    return uploadJsonData("/api/AdvancingTeams", "POST", advancing)
        .then(checkJsonResponse)
        .catch(error => { throw new Error("Error sending group advancing teams: " + error) });
}

/**
 * Load the award groups.
 * 
 * @returns the promise for the ajax query
 */
function loadAwardGroups() {
    _awardGroups = [];

    return fetch("/api/AwardGroups").then(checkJsonResponse).then(function(data) {
        _awardGroups = data;
    });
}

function loadAdvancingTeams() {
    _advancingTeams = [];

    return fetch("/api/AdvancingTeams").then(checkJsonResponse).then(function(data) {
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

    const waitList = []

    const teamsPromise = fllTeams.loadTeams();
    teamsPromise.catch(function() {
        failCallback("Teams");
    });
    waitList.push(teamsPromise);

    const loadAwardGroupsPromise = loadAwardGroups();
    loadAwardGroupsPromise.catch(function() {
        failCallback("Award groups");
    });
    waitList.push(loadAwardGroupsPromise);

    const loadAdvancingTeamsPromise = loadAdvancingTeams();
    loadAdvancingTeamsPromise.catch(function() {
        failCallback("Advancing Teams");
    });
    waitList.push(loadAdvancingTeamsPromise);

    const loadSortedGroupsPromise = loadSortedGroups();
    loadSortedGroupsPromise.catch(function() {
        failCallback("Sorted groups");
    });
    waitList.push(loadSortedGroupsPromise);

    Promise.all(waitList).then(function(_) {
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
    const teamNumber = data.teamElement.value;
    if (teamNumber && teamNumber != "") {
        const groupName = data.nameFunc();
        return new AdvancingTeam(teamNumber, groupName);
    } else {
        return null;
    }
}

function initPage() {
    _advancingTeamData = [];

    const advancingTeamsEle = document.getElementById("advancing-teams");
    while (advancingTeamsEle.firstChild) {
        advancingTeamsEle.removeChild(advancingTeamsEle.firstChild);
    }

    _awardGroups.forEach(function(group) {
        addAdvancingGroup(group, false);
    });

    const knownGroups = [];
    _advancingTeams.forEach(function(advancing) {
        if (!knownGroups.includes(advancing.group)
            && !_awardGroups.includes(advancing.group)) {
            knownGroups.push(advancing.group);
        }
    });

    knownGroups.forEach(function(group) {
        addAdvancingGroup(group, true);
    });

    const awardGroupOrder = document.getElementById("award-group-order");
    removeChildren(awardGroupOrder);

    if (_initialSortedGroups) {
        _initialSortedGroups.forEach(function(group) {
            addGroupToSort(group);
            if (!knownGroups.includes(group)
                && !_awardGroups.includes(group)) {
                addAdvancingGroup(group, true);
                knownGroups.push(group);
            }
        });
    }

}

function addAdvancingGroup(group, editable) {
    const groupItem = document.createElement("li");

    document.getElementById("advancing-teams").appendChild(groupItem);

    let nameFunc;
    let groupEle;
    if (editable) {
        groupEle = document.createElement("input");
        groupEle.setAttribute("type", "text");
        nameFunc = function() {
            return groupEle.value;
        };
        groupEle.value = group;

        groupEle.data = group;
        groupEle.onchange = function() {
            const newName = groupEle.value;
            const oldValue = groupEle.data;
            if (null == newName || "" == newName) {
                alert("All groups must have non-empty names");
                groupEle.value = oldValue;
            } else {
                // check that the group name isn't a duplicate
                let duplicate = false;
                Array.from(document.getElementById("award-group-order").children).forEach(function(le) {
                    if ("LI" == le.tagName) {
                        const inputEle = le.getElementsByTagName("span")[0];
                        if (inputEle != groupEle) {
                            if (newName == inputEle.textContent) {
                                duplicate = true;
                            }
                        }
                    }
                });

                if (duplicate) {
                    alert("The group name ''" + newName + "' is already in use");
                    groupEle.value = oldValue;
                } else {
                    renameGroupToSort(oldValue, newName);
                    groupEle.data = newName;
                }
            }
        };
    } else {
        if (!group) {
            alert("Internal Error: Cannot have an unnammed non-editable group");
            return;
        }

        groupEle = document.createElement("span");
        groupEle.innerHTML = group;
        nameFunc = function() {
            return group;
        }
    }
    groupItem.appendChild(groupEle);

    const groupList = document.createElement("ul");
    groupItem.appendChild(groupList);

    const addTeamButton = document.createElement("button");
    addTeamButton.innerHTML = "Add Team";
    groupItem.appendChild(addTeamButton);

    const teamList = document.createElement("ul");
    groupItem.appendChild(teamList);

    addTeamButton.addEventListener('click', function() {
        addAdvancingTeam(null, _advancingTeamData, nameFunc, teamList, editable ? null : group);
    });

    let enterNewTeam = true;
    if (group) {
        _advancingTeams.forEach(function(advancing) {
            if (advancing.group == group) {
                addAdvancingTeam(advancing, _advancingTeamData, nameFunc, teamList, editable ? null : group);
                enterNewTeam = false;
            }
        });
    }

    if (enterNewTeam) {
        // add an empty team if there weren't any loaded from the server
        addAdvancingTeam(null, _advancingTeamData, nameFunc, teamList, editable ? null : group);
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
 * @param awardGroup the award group to filter to, may be null
 */
function addAdvancingTeam(advancing, dataList, groupNameFunc, teamList, awardGroup) {

    const teamEle = document.createElement("li");
    teamList.appendChild(teamEle);

    const numEle = document.createElement("input");
    numEle.setAttribute("type", "text");
    teamEle.appendChild(numEle);

    const nameEle = document.createElement("input");
    nameEle.readOnly = true;
    nameEle.disabled = true;
    teamEle.appendChild(nameEle);
    let prevTeam = "";
    numEle.addEventListener(
        'change',
        function() {
            const newTeamNumber = numEle.value;

            let duplicate = false;
            _advancingTeamData.forEach(function(data) {
                if (data.teamElement != numEle) {
                    const compareTeamNumber = data.teamElement.value;
                    if (compareTeamNumber == newTeamNumber) {
                        duplicate = true;
                    }
                }
            });
            if (duplicate) {
                alert("Team " + newTeamNumber + " is already listed as advancing");
                numEle.value = prevTeam;
            } else {
                prevTeam = newTeamNumber;
            }
        });

    const orgEle = document.createElement("input");
    orgEle.readOnly = true;
    orgEle.disabled = true;
    teamEle.appendChild(orgEle);

    const agEle = document.createElement("input");
    agEle.readOnly = true;
    agEle.disabled = true;
    teamEle.appendChild(agEle);

    const data = new AdvancingTeamData(groupNameFunc, numEle);
    dataList.push(data);

    fllTeams.autoPopulate(numEle, nameEle, orgEle, agEle, awardGroup)

    const deleteButton = document.createElement("button");
    deleteButton.innerHTML = "Delete";
    teamEle.appendChild(deleteButton);
    deleteButton.addEventListener('click', function() {
        const teamNum = numEle.value;
        let reallyDelete = false;
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
        numEle.value = advancing.teamNumber;
        numEle.dispatchEvent(new Event("change"));
    }

}

let _initialSortedGroups = [];

function loadSortedGroups() {
    return fetch("/api/AwardsReportSortedGroups").then(checkJsonResponse).then(function(data) {
        _initialSortedGroups = data;
    });
}

function initSortedGroups(sortedGroups) {
    const ele = document.getElementById("award-group-order");
    while (ele.firstChild) {
        ele.removeChild(ele.firstChild);
    }

    if (sortedGroups) {
        sortedGroups.forEach(function(group) {
            addGroupToSort(group);
            if (!_awardGroups.includes(group)) {
                addAdvancingGroup(group, true);
            }
        });
    }
}

function addGroupToSort(group) {
    let numGroups = 0;
    let addGroup = true;
    Array.from(document.getElementById("award-group-order").children).forEach(function(le) {
        if ("LI" == le.tagName) {
            const leGroup = le.getAttribute("data-group-name");
            if (leGroup == group) {
                addGroup = false;
            }
            numGroups = numGroups + 1;
        }
    });

    if (addGroup) {
        const listElement = document.createElement("li");
        listElement.setAttribute("data-group-name", group);
        document.getElementById("award-group-order").appendChild(listElement);

        const indexElement = document.createElement("input");
        indexElement.setAttribute("type", "number");
        indexElement.setAttribute("step", "1");
        indexElement.value = numGroups + 1;
        listElement.appendChild(indexElement);

        const nameElement = document.createElement("span");
        nameElement.innerHTML = group;
        listElement.appendChild(nameElement);
    }
}

function removeGroupToSort(group) {
    let toRemove = null;

    Array.from(document.getElementById("award-group-order").children).forEach(function(le) {
        if ("LI" == le.tagName) {
            const leGroup = le.getAttribute("data-group-name");
            if (leGroup == group) {
                toRemove = le;
            }
        }
    })

    if (toRemove) {
        toRemove.parentNode.removeChild(toRemove);
    }
}

function renameGroupToSort(oldName, newName) {
    let element = null;
    Array.from(document.getElementById("award-group-order").children).forEach(function(le) {
        if ("LI" == le.tagName) {
            const leGroup = le.getAttribute("data-group-name");
            if (leGroup == oldName) {
                element = le;
            }
        }
    })

    if (element) {
        element.setAttribute("data-group-name", newName);
        Array.from(element.children).forEach(function(span) {
            if ("SPAN" == span.tagName) {
                span.innerHTML = newName;
            }
        });
    }
}

/**
 * @return a Promise for storing the sorted groups
 */
function storeSortedGroups() {
    // gather list elements that have the group sort information
    const groupElements = [];
    Array.from(document.getElementById("award-group-order").children).forEach(function(le) {
        if ("LI" == le.tagName) {
            groupElements.push(le);
        }
    });

    // sort the elements
    groupElements.sort(function(a, b) {
        const aIndex = a.getElementsByTagName("input")[0].value;
        const bIndex = b.getElementsByTagName("input")[0].value;
        return aIndex - bIndex;
    });

    // convert to a list of strings
    const groups = [];
    groupElements.forEach(function(le) {
        const group = le.getAttribute("data-group-name");
        groups.push(group);
    });

    // send to server
    return fetch("/api/AwardsReportSortedGroups",
        {
            method: "POST",
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(groups)
        }
    ).then(checkJsonResponse)
        .catch(error => { throw new Error("Error sending group sort information: " + error) });
}
