/*
 * Copyright (c) 2021 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

/* UI functions for non-numeric nominees */

"use strict";

const nonNumericUi = {}

{
    function handleDivisionChange() {
        const divIndex = $("#divisions").val();
        const div = finalist_module.getDivisionByIndex(divIndex);
        finalist_module.setCurrentDivision(div);
        updateTeams();
        if (_useStorage) {
            finalist_module.saveToLocalStorage();
        }
    }

    let _useStorage = true;

    /**
     * @param value if true, then use local storage, if false everything is stored only in memory. Defaults to true.
     */
    nonNumericUi.setUseStorage = function(value) {
        _useStorage = value;
    }

    let _useScheduledFlag = true;

    /**
     * @param value if true then show the checkboxes for marking scheduled categories. Defaults to true.
     */
    nonNumericUi.setUseScheduledFlag = function(value) {
        _useScheduledFlag = value;
    }

    /**
     * Initialize the UI for non-numeric nominees.
     * If local storage is not to be used, make sure to call setUseStorage(false) first.
     */
    nonNumericUi.initialize =
        function() {
            if (_useStorage) {
                finalist_module.loadFromLocalStorage();
            }

            $("#divisions").empty();
            $.each(finalist_module.getDivisions(), function(i, division) {
                let selected = "";
                if (division == finalist_module.getCurrentDivision()) {
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
        }; // end initialize function

    function updateTeams() {
        $("#categories").empty();
        $("#overall-categories").empty();

        $.each(finalist_module.getNonNumericCategories(), function(_, category) {
            addCategoryElement(category);

            let addedTeam = false;
            $.each(category.teams,
                function(_, teamNum) {
                    const team = finalist_module.lookupTeam(teamNum);
                    if (category.overall
                        || finalist_module.isTeamInDivision(team, finalist_module
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

    function addCategoryElement(category) {
        const catEle = $("<li></li>");
        if (category.overall) {
            $("#overall-categories").append(catEle);
        } else {
            $("#categories").append(catEle);
        }

        if (_useScheduledFlag) {
            const scheduledCheckbox = $("<input type='checkbox' id='scheduled_"
                + category.catId + "'/>");
            catEle.append(scheduledCheckbox);
            scheduledCheckbox.change(function() {
                finalist_module.setCategoryScheduled(category, $(this).prop("checked"));
                roomEle.prop("disabled", !(scheduledCheckbox.prop("checked")));
                if (_useStorage) {
                    finalist_module.saveToLocalStorage();
                }
            });
            scheduledCheckbox.attr("checked", finalist_module.isCategoryScheduled(category));
        }

        catEle.append(category.name);
        catEle.append(" - ")

        catEle.append("Room number: ");
        const roomEle = $("<input type='text' id='room_" + category.catId + "'/>");
        catEle.append(roomEle);
        roomEle.change(function() {
            const roomNumber = roomEle.val();
            finalist_module.setRoom(category, finalist_module.getCurrentDivision(), roomNumber);
            if (_useStorage) {
                finalist_module.saveToLocalStorage();
            }
        });
        roomEle.val(finalist_module.getRoom(category, finalist_module.getCurrentDivision()));
        roomEle.prop("disabled", !finalist_module.isCategoryScheduled(category));

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

        const judges = finalist_module.getNominatingJudges(category, team.num);
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
            let teamNum = $(this).val();
            const prevTeam = $(this).data('oldVal');
            if ("" == teamNum) {
                finalist_module.removeTeamFromCategory(category, prevTeam);
                $("#" + teamNameId(category.catId, teamIdx)).val("");
                $("#" + teamOrgId(category.catId, teamIdx)).val("");
                $("#" + teamJudgingStationId(category.catId, teamIdx)).val("");
                if (_useStorage) {
                    finalist_module.saveToLocalStorage();
                }
            } else if (teamNum != prevTeam) {
                finalist_module.removeTeamFromCategory(category, prevTeam);

                const team = finalist_module.lookupTeam(teamNum);
                if (typeof (team) == 'undefined') {
                    alert("Team number " + teamNum + " does not exist");
                    $(this).val(prevTeam);
                    teamNum = prevTeam; // for the set of oldVal below
                } else {
                    populateTeamInformation(category, teamIdx, team);
                    finalist_module.addTeamToCategory(category, teamNum);
                    if (_useStorage) {
                        finalist_module.saveToLocalStorage();
                    }
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
                const reallyDelete = confirm("Are you sure you want to delete this team?");
                if (reallyDelete) {
                    finalist_module.removeTeamFromCategory(category, teamNum);
                    teamEle.remove();
                    if (_useStorage) {
                        finalist_module.saveToLocalStorage();
                    }
                }
            } else {
                finalist_module.removeTeamFromCategory(category, teamNum);
                teamEle.remove();
                if (_useStorage) {
                    finalist_module.saveToLocalStorage();
                }
            }
        });

        return teamIdx;
    }

} // scope