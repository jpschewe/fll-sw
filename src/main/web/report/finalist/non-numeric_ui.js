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
        const divIndex = document.getElementById("divisions").value;
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

            const divisionsElement = document.getElementById("divisions");
            removeChildren(divisionsElement);

            finalist_module.getDivisions().forEach(function(division, i) {
                const divisionOption = document.createElement("option");
                divisionOption.setAttribute("value", i);
                if (division == finalist_module.getCurrentDivision()) {
                    divisionOption.setAttribute("selected", "true");
                }
                divisionOption.innerText = division;

                divisionsElement.appendChild(divisionOption);
            }); // foreach division

            divisionsElement.addEventListener("change", () => {
                handleDivisionChange();
            });
            handleDivisionChange();

            updateTeams();
        }; // end initialize function

    function updateTeams() {
        removeChildren(document.getElementById("categories"));
        removeChildren(document.getElementById("overall-categories"));

        for (const category of finalist_module.getNonNumericCategories()) {
            addCategoryElement(category);

            let addedTeam = false;
            for (const teamNum of category.teams) {
                const team = finalist_module.lookupTeam(teamNum);
                if (category.overall
                    || team.awardGroup == finalist_module.getCurrentDivision()) {
                    addedTeam = true;
                    const teamIdx = addTeam(category);
                    populateTeamInformation(category, teamIdx, team);
                }

            }
            if (!addedTeam) {
                addTeam(category);
            }
        }
    }

    function addCategoryElement(category) {
        const catEle = document.createElement("li");
        if (category.overall) {
            document.getElementById("overall-categories").appendChild(catEle);
        } else {
            document.getElementById("categories").appendChild(catEle);
        }

        if (_useScheduledFlag) {
            const scheduledCheckbox = document.createElement("input");
            scheduledCheckbox.setAttribute("type", "checkbox");
            scheduledCheckbox.setAttribute("id", "scheduled_" + category.catId);

            catEle.appendChild(scheduledCheckbox);
            scheduledCheckbox.addEventListener("change", () => {
                finalist_module.setCategoryScheduled(category, scheduledCheckbox.checked);
                roomEle.disabled = !scheduledCheckbox.checked;
                if (_useStorage) {
                    finalist_module.saveToLocalStorage();
                }
            });

            scheduledCheckbox.checked = finalist_module.isCategoryScheduled(category);
        }

        const categoryLabel = document.createElement("span");
        catEle.appendChild(categoryLabel);
        categoryLabel.innerText = category.name + " - ";

        const roomInputId = "room_" + category.catId;
        const roomLabel = document.createElement("label");
        catEle.appendChild(roomLabel);
        roomLabel.setAttribute("for", roomInputId);
        roomLabel.innerText = "Room Number: ";

        const roomEle = document.createElement("input");
        catEle.appendChild(roomEle);
        roomEle.setAttribute("id", roomInputId);
        roomEle.setAttribute("type", "text");

        roomEle.addEventListener("change", () => {
            const roomNumber = roomEle.value;
            finalist_module.setRoom(category, finalist_module.getCurrentDivision(), roomNumber);
            if (_useStorage) {
                finalist_module.saveToLocalStorage();
            }
        });
        const room = finalist_module.getRoom(category, finalist_module.getCurrentDivision());
        if (room) {
            roomEle.value = room;
        } else {
            roomEle.value = "";
        }
        roomEle.disabled = !finalist_module.isCategoryScheduled(category);

        const teamList = document.createElement("ul");
        catEle.appendChild(teamList);
        teamList.setAttribute("id", "category_" + category.catId);

        const addButton = document.createElement("button");
        catEle.appendChild(addButton);
        addButton.setAttribute("id", "add-team_" + category.catId);
        addButton.setAttribute("type", "button");
        addButton.innerText = "Add Team";

        addButton.addEventListener("click", () => {
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

    function teamDeleteId(category, teamIdx) {
        return "delete_" + category + "_" + teamIdx;
    }

    function populateTeamInformation(category, teamIdx, team) {
        const teamNumElement = document.getElementById(teamNumId(category.catId, teamIdx));
        teamNumElement.value = team.num;
        teamNumElement.setAttribute("data-oldVal", team.num);

        const teamNameElement = document.getElementById(teamNameId(category.catId, teamIdx));
        teamNameElement.value = team.name;

        const teamOrgElement = document.getElementById(teamOrgId(category.catId, teamIdx));
        teamOrgElement.value = team.org;
    }

    /**
     * Add a new empty team to the page for the specified category
     * 
     * @return the index for the team which can be used to populate the elements
     *         later
     */
    function addTeam(category) {
        const catEle = document.getElementById("category_" + category.catId);
        const teamIdx = catEle.children.length + 1;

        const teamEle = document.createElement("li");
        catEle.appendChild(teamEle);

        const numEle = document.createElement("input");
        teamEle.appendChild(numEle);
        numEle.setAttribute("type", "text");
        numEle.setAttribute("id", teamNumId(category.catId, teamIdx));

        numEle.addEventListener("change", () => {
            let teamNum = numEle.value;
            const prevTeam = numEle.getAttribute("data-oldVal");
            if ("" == teamNum) {
                finalist_module.removeTeamFromCategory(category, prevTeam);
                document.getElementById(teamNameId(category.catId, teamIdx)).value = "";
                document.getElementById(teamOrgId(category.catId, teamIdx)).value = "";
                if (_useStorage) {
                    finalist_module.saveToLocalStorage();
                }
            } else if (teamNum != prevTeam) {
                finalist_module.removeTeamFromCategory(category, prevTeam);
                if (_useStorage) {
                    finalist_module.saveToLocalStorage();
                }

                const team = finalist_module.lookupTeam(teamNum);
                if (typeof (team) == 'undefined') {
                    alert("Team number " + teamNum + " does not exist");
                    numEle.value = prevTeam;
                    teamNum = prevTeam; // for the set of oldVal below
                } else {
                    populateTeamInformation(category, teamIdx, team);
                    finalist_module.addTeamToCategory(category, teamNum);
                    if (_useStorage) {
                        finalist_module.saveToLocalStorage();
                    }
                }
            }
            numEle.setAttribute("data-oldVal", teamNum);
        });

        const nameEle = document.createElement("input");
        teamEle.appendChild(nameEle);
        nameEle.setAttribute("id", teamNameId(category.catId, teamIdx));
        nameEle.readonly = true;
        nameEle.disabled = true;

        const orgEle = document.createElement("input");
        teamEle.appendChild(orgEle);
        orgEle.setAttribute("id", teamOrgId(category.catId, teamIdx));
        orgEle.readonly = true;
        orgEle.disabled = true;

        const deleteButton = document.createElement("button");
        teamEle.appendChild(deleteButton);
        deleteButton.setAttribute("type", "button");
        deleteButton.setAttribute("id", teamDeleteId(category.catId, teamIdx));
        deleteButton.innerText = "Delete";

        deleteButton.addEventListener("click", () => {
            const teamNum = numEle.value;
            if ("" != teamNum) {
                const reallyDelete = confirm("Are you sure you want to delete this team?");
                if (reallyDelete) {
                    finalist_module.removeTeamFromCategory(category, teamNum);
                    catEle.removeChild(teamEle);
                    if (_useStorage) {
                        finalist_module.saveToLocalStorage();
                    }
                }
            } else {
                finalist_module.removeTeamFromCategory(category, teamNum);
                catEle.removeChild(teamEle);
                if (_useStorage) {
                    finalist_module.saveToLocalStorage();
                }
            }
        });

        return teamIdx;
    }

} // scope
