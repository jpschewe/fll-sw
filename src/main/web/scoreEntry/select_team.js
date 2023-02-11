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

/**
 * Prefer the selected table, then sort by next performance time.
 */
function compareByPerformanceTime(teamDataA, teamDataB) {
    const oneTable = teamDataA.nextTableAndSide;
    const twoTable = teamDataB.nextTableAndSide;

    if (oneTable == scoreEntrySelectedTable
        && twoTable != scoreEntrySelectedTable) {
        // prefer selected table
        return -1;
    } else if (oneTable != scoreEntrySelectedTable
        && twoTable == scoreEntrySelectedTable) {
        // prefer selected table
        return 1;
    } else {
        if (null == teamDataA.nextPerformance
            && null == teamDataB.nextPerformance) {
            if (teamDataA.nextRunNumber == teamDataB.nextRunNumber) {
                return 0;
            } else if (teamDataA.nextRunNumber < teamDataB.nextRunNumber) {
                return -1;
            } else {
                return 1;
            }
        } else if (null == teamDataA.nextPerformance) {
            // this is after other
            return 1;
        } else if (null == teamDataB.nextPerformance) {
            // this is before other
            return -1;
        } else {
            const perfCompare = comparePerformanceTimes(teamDataA.nextPerformance, teamDataB.nextPerformance);
            if (0 == perfCompare) {
                if (teamDataA.team.teamNumber < teamDataB.team.teamNumber) {
                    return -1;
                } else if (teamDataA.team.teamNumber > teamDataB.team.teamNumber) {
                    return 1;
                } else {
                    return 0;
                }
            } else {
                return perfCompare;
            }
        }
    }
}

/**
 * Compare PerformanceTime objects.
 */
function comparePerformanceTimes(ptimeA, ptimeB) {
    if (ptimeA.time < ptimeB.time) {
        return -1;
    } else if (ptimeA.time > ptimeB.time) {
        return 1;
    } else {
        if (ptimeA.table < ptimeB.table) {
            return -1;
        } else if (ptimeA.table > ptimeB.table) {
            return 1;
        } else {
            if (ptimeA.practice && !ptimeB.practice) {
                return -1;
            } else if (!ptimeA.practice && ptimeB.practice) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}

function compareByTeamNumber(teamDataA, teamDataB) {
    const oneTable = teamDataA.nextTableAndSide;
    const twoTable = teamDataB.nextTableAndSide;

    if (oneTable == scoreEntrySelectedTable
        && twoTable != scoreEntrySelectedTable) {
        // prefer selected table
        return -1;
    } else if (oneTable != scoreEntrySelectedTable
        && twoTable == scoreEntrySelectedTable) {
        // prefer selected table
        return 1;
    } else {
        if (teamDataA.team.teamNumber < teamDataB.team.teamNumber) {
            return -1;
        } else if (teamDataA.team.teamNumber > teamDataB.team.teamNumber) {
            return 1;
        } else {
            return 0;
        }
    }
}

function compareByTeamName(teamDataA, teamDataB) {
    const oneTable = teamDataA.nextTableAndSide;
    const twoTable = teamDataB.nextTableAndSide;

    if (oneTable == scoreEntrySelectedTable
        && twoTable != scoreEntrySelectedTable) {
        // prefer selected table
        return -1;
    } else if (oneTable != scoreEntrySelectedTable
        && twoTable == scoreEntrySelectedTable) {
        // prefer selected table
        return 1;
    } else {
        if (teamDataA.team.teamName < teamDataB.team.teamName) {
            return -1;
        } else if (teamDataA.team.teamName > teamDataB.team.teamName) {
            return 1;
        } else {
            return 0;
        }
    }
}

function compareByOrganization(teamDataA, teamDataB) {
    const oneTable = teamDataA.nextTableAndSide;
    const twoTable = teamDataB.nextTableAndSide;

    if (oneTable == scoreEntrySelectedTable
        && twoTable != scoreEntrySelectedTable) {
        // prefer selected table
        return -1;
    } else if (oneTable != scoreEntrySelectedTable
        && twoTable == scoreEntrySelectedTable) {
        // prefer selected table
        return 1;
    } else {
        if (teamDataA.team.organization < teamDataB.team.organization) {
            return -1;
        } else if (teamDataA.team.organization > teamDataB.team.organization) {
            return 1;
        } else {
            return 0;
        }
    }
}


document.addEventListener('DOMContentLoaded', function() {
    if (!tabletMode) {
        editFlagBoxClicked();
    }
    teamSelectData.sort(compareByPerformanceTime);
    populateTeamsSelect();

    if (!scoreEntrySelectedTable) {
        document.getElementById("sort-team-name").addEventListener('click', () => {
            teamSelectData.sort(compareByTeamName);
            populateTeamsSelect();
        });
        document.getElementById("sort-team-number").addEventListener('click', () => {
            teamSelectData.sort(compareByTeamNumber);
            populateTeamsSelect();
        });
        document.getElementById("sort-organization").addEventListener('click', () => {
            teamSelectData.sort(compareByOrganization);
            populateTeamsSelect();
        });
        document.getElementById("sort-next-perf").addEventListener('click', () => {
            teamSelectData.sort(compareByPerformanceTime);
            populateTeamsSelect();
        });

        // only use unverified code when not using the tablets 

        reloadRuns();
        openSocket();
    }
});