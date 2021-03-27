/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

function handleDivisionChange() {
    var divIndex = $("#divisions").val();
    var div = $.finalist.getDivisionByIndex(divIndex);
    $.finalist.setCurrentDivision(div);
    updateTeams();
}

$(document).ready(
    function() {

        $("#previous").click(function() {
            location.href = "params.html";
        });
        $("#next").click(function() {
            // find the first numeric category
            let first = null;
            $.each($.finalist.getNumericCategories(), function(i, category) {
                if (category.name != $.finalist.CHAMPIONSHIP_NAME) {
                    if (null == first) {
                        first = category;
                    }
                }
            });
            if (null != first) {
                $.finalist.setCurrentCategoryName(first.name);
                location.href = "numeric.html";
            } else {
                alert("Internal error, cannot find next category");
            }
        });


        $("#divisions").empty();
        $.each($.finalist.getDivisions(), function(i, division) {
            let selected = "";
            if (division == $.finalist.getCurrentDivision()) {
                selected = " selected ";
            }
            const divisionOption = $("<option value='" + i + "'" + selected + ">"
                + division + "</option>");
            $("#divisions").append(divisionOption);
        }); // foreach division
        $("#divisions").change(function() {
            handleDivisionChange();
        });
        handleDivisionChange();

        updateTeams();

        $.finalist.displayNavbar();
    }); // end ready function

function updateTeams() {
    $("#categories").empty();
    $("#overall-categories").empty();

    $.each($.finalist.getNonNumericCategories(), function(i, category) {
        addCategoryElement(category);

        let addedTeam = false;
        $.each(category.teams,
            function(j, teamNum) {
                const team = $.finalist.lookupTeam(teamNum);
                if (category.overall
                    || $.finalist.isTeamInDivision(team, $.finalist
                        .getCurrentDivision())) {
                    addedTeam = true;
                    const teamIdx = addTeam(category);
                    populateTeamInformation(category, teamIdx, team);
                }

            });
        if (!addedTeam) {
            addTeam(category);
        }
    });
}

function checkCategoryEmpty(category) {
    return category.teams.length == 0;
}

function addCategoryElement(category) {
    var catEle = $("<li></li>");
    if (category.overall) {
        $("#overall-categories").append(catEle);
    } else {
        $("#categories").append(catEle);
    }

    const scheduledCheckbox = $("<input type='checkbox' id='scheduled_"
        + category.catId + "'/>");
    catEle.append(scheduledCheckbox);
    scheduledCheckbox.change(function() {
        $.finalist.setCategoryScheduled(category, $(this).prop("checked"));
        roomEle.prop("disabled", !(scheduledCheckbox.prop("checked")));
    });
    scheduledCheckbox.attr("checked", $.finalist.isCategoryScheduled(category));

    catEle.append(category.name);
    catEle.append(" - ")

    catEle.append("Room number: ");
    const roomEle = $("<input type='text' id='room_" + category.catId + "'/>");
    catEle.append(roomEle);
    roomEle.change(function() {
        var roomNumber = roomEle.val();
        $.finalist.setRoom(category, $.finalist.getCurrentDivision(), roomNumber);
    });
    roomEle.val($.finalist.getRoom(category, $.finalist.getCurrentDivision()));
    roomEle.prop("disabled", !$.finalist.isCategoryScheduled(category));

    const teamList = $("<ul id='category_" + category.catId + "'></ul>");
    catEle.append(teamList);

    const addButton = $("<button id='add-team_" + category.catId
        + "'>Add Team</button>");
    catEle.append(addButton);
    addButton.click(function() {
        addTeam(category);
    });

}

function teamNumId(category, teamIdx) {
    return "num_" + category + "_" + teamIdx;
}

function teamNameId(category, teamIdx) {
    return "name_" + category + "_" + teamIdx;
}

function teamOrgId(category, teamIdx) {
    return "org_" + category + "_" + teamIdx;
}

function teamJudgingStationId(category, teamIdx) {
    return "judgingStation_" + category + "_" + teamIdx;
}

function teamJudgesId(category, teamIdx) {
    return "judges_" + category + "_" + teamIdx;
}

function teamDeleteId(category, teamIdx) {
    return "delete_" + category + "_" + teamIdx;
}

function populateTeamInformation(category, teamIdx, team) {
    $("#" + teamNumId(category.catId, teamIdx)).val(team.num);
    $("#" + teamNumId(category.catId, teamIdx)).data('oldVal', team.num);
    $("#" + teamNameId(category.catId, teamIdx)).val(team.name);
    $("#" + teamOrgId(category.catId, teamIdx)).val(team.org);
    $("#" + teamJudgingStationId(category.catId, teamIdx)).val(team.judgingGroup);

    const judges = $.finalist.getNominatingJudges(category, team.num);
    let judgesStr;
    if (!judges) {
        judgesStr = "";
    } else {
        judgesStr = judges.filter(x => x).join(", ");
    }
    $("#" + teamJudgesId(category.catId, teamIdx)).val(judgesStr);
}

/**
 * Add a new empty team to the page for the specified category
 * 
 * @return the index for the team which can be used to populate the elements
 *         later
 */
function addTeam(category) {
    const catEle = $("#category_" + category.catId);
    const teamIdx = catEle.children().size() + 1;

    const teamEle = $("<li></li>");
    catEle.append(teamEle);

    const numEle = $("<input type='text' id='" + teamNumId(category.catId, teamIdx)
        + "'/>");
    teamEle.append(numEle);
    numEle.change(function() {
        const teamNum = $(this).val();
        const prevTeam = $(this).data('oldVal');
        if ("" == teamNum) {
            $.finalist.removeTeamFromCategory(category, prevTeam);
            $("#" + teamNameId(category.catId, teamIdx)).val("");
            $("#" + teamOrgId(category.catId, teamIdx)).val("");
            $("#" + teamJudgingStationId(category.catId, teamIdx)).val("");
        } else if (teamNum != prevTeam) {
            $.finalist.removeTeamFromCategory(category, prevTeam);

            var team = $.finalist.lookupTeam(teamNum);
            if (typeof (team) == 'undefined') {
                alert("Team number " + teamNum + " does not exist");
                $(this).val(prevTeam);
                teamNum = prevTeam; // for the set of oldVal below
            } else {
                populateTeamInformation(category, teamIdx, team);
                $.finalist.addTeamToCategory(category, teamNum);
            }
        }
        $(this).data('oldVal', teamNum);
    });

    const nameEle = $("<input id='" + teamNameId(category.catId, teamIdx)
        + "' readonly disabled/>");
    teamEle.append(nameEle);
    const orgEle = $("<input id='" + teamOrgId(category.catId, teamIdx)
        + "' readonly disabled/>");
    teamEle.append(orgEle);

    const judgingStationEle = $("<input id='"
        + teamJudgingStationId(category.catId, teamIdx) + "' readonly disabled/>");
    teamEle.append(judgingStationEle);

    const judgesEle = $("<input id='"
        + teamJudgesId(category.catId, teamIdx) + "' readonly disabled/>");
    teamEle.append(judgesEle);

    const deleteButton = $("<button id='" + teamDeleteId(category.catId, teamIdx)
        + "'>Delete</button>");
    teamEle.append(deleteButton);
    deleteButton.click(function() {
        const teamNum = numEle.val();
        if ("" != teamNum) {
            var reallyDelete = confirm("Are you sure you want to delete this team?");
            if (reallyDelete) {
                $.finalist.removeTeamFromCategory(category, teamNum);
                teamEle.remove();
            }
        } else {
            $.finalist.removeTeamFromCategory(category, teamNum);
            teamEle.remove();
        }
    });

    return teamIdx;
}
