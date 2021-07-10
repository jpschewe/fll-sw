/*
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

/*
 * Methods for dealing with teams and populating elements based on the team number
 */
const fllTeams = {};

{

    // team number -> team  
    var _teams = {}

    /**
     * Load the teams from the server. This is needed for the other functions to work.
     *
     * @return promise for loading the teams
     */
    fllTeams.loadTeams = function() {
        _teams = {};

        return fetch("/api/TournamentTeams").then(checkJsonResponse).then(function(teams) {
            teams.forEach(function(team) {
                _teams[team.teamNumber] = team;
            });
        });
    }

    /**
     * When numEle changes, check for a valid team number and populate the other elements with the team information.
     *
     * @param numEle watched for the team number
     * @param nameEle populated with the team name
     * @param orgEle populated with the organization
     * @param agEle populated with the award group
     * @param awardGroup if specified, limit teams to the specified award group
     */
    fllTeams.autoPopulate = function(numEle, nameEle, orgEle, agEle, awardGroup) {
        var prevTeam = "";
        numEle.onchange = function() {
            var teamNum = numEle.value;
            if (!teamNum || "" == teamNum) {
                nameEle.value = "";
                orgEle.value = "";
                agEle.value = "";
            } else {
                var team = _teams[teamNum];
                if (typeof (team) == 'undefined') {
                    alert("Team number " + teamNum + " does not exist");
                    numEle.value = prevTeam;
                    teamNum = prevTeam; // for storing at the bottom of the method
                } else {
                    if (awardGroup && awardGroup != team.awardGroup) {
                        alert("Team number " + teamNum + " is in the award group '" + team.awardGroup + "' and only teams in award group '" + awardGroup + "' are allowed'");
                        numEle.value = prevTeam;
                        teamNum = prevTeam; // for storing at the bottom of the method
                    } else {
                        nameEle.value = team.teamName;
                        orgEle.value = team.organization;
                        agEle.value = team.awardGroup;
                    }
                }
            }
            prevTeam = teamNum;
        };
    }

}