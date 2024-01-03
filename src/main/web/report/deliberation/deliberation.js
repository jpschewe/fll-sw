/*
 * Copyright (c) 2024 HighTechKids.  All rights reserved.
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

const deliberationModule = {};

{
    let draggingTeam = null;
    let draggingCategory = null;
    let draggingTeamDiv = null;
    let schedule = null;

    /**
     * Categories sorted in the order for the table. These are finalist Category objects. 
     */
    let sortedCategories = [];

    /**
     * Mapping of team numbers to categories.
     */
    let finalistsCount = {};

    /**
     * Team numbers sorted by number of categories.
     */
    let sortedTeams = [];

    /**
     * Map time string to map of category name to div cell.
     */
    const timeToCells = {};

    /**
     * Find the cell for the specified time and category id. This does a lookup by
     * comparing the times, so slots added later are ok as long as a cell has been
     * added for the specified time.
     * 
     * @param searchTime
     *          the time to search for
     * @param serachCatName
     *          the category name to search for
     * @return the cell or null if not found
     */
    function getCellForTimeAndCategory(searchTime, searchCatName) {
        const searchTimeStr = finalist_module.timeToDisplayString(searchTime);

        let foundCell = null;

        for (const [timeStr, categoryToCells] of Object.entries(timeToCells)) {
            if (searchTimeStr == timeStr) {

                for (const [catName, cell] of Object.entries(categoryToCells)) {
                    if (catName == searchCatName) {
                        foundCell = cell;
                    }
                }
            }
        }

        return foundCell;
    }

    function handleAwardGroupChange() {
        const divIndex = document.getElementById("award_groups").value;
        const div = finalist_module.getDivisionByIndex(divIndex);
        finalist_module.setCurrentDivision(div);
        updatePage();
        finalist_module.saveToLocalStorage();
    }

    function updateHeader() {
        const headerRow = document.getElementById("deliberation_header");
        removeChildren(headerRow);

        const placeHeader = document.createElement("div");
        headerRow.appendChild(placeHeader);
        placeHeader.classList.add("rTableHead");
        placeHeader.innerText = "Place";

        for (const category of sortedCategories) {
            const header = document.createElement("div");
            headerRow.appendChild(header);
            header.classList.add("rTableHead");

            const categoryNameEle = document.createElement("div");
            header.appendChild(categoryNameEle);
            categoryNameEle.innerText = category.name;
        }
    }

    /**
     * Create the div element for a team in a particular category.
     * 
     * @param team
     *          a Team object
     * @param category
     *          a Category object
     * @returns an HTML div element for the team
     */
    function createTeamDiv(team, category) {
        const teamCategories = finalistsCount.get(team.num);
        const numCategories = teamCategories.length;
        const group = team.judgingGroup;
        const teamDiv = document.createElement("div");
        teamDiv.setAttribute("draggable", "true");
        teamDiv.innerText = team.num + " - " + team.name + " (" + group + ", " + numCategories + ")";

        // determine the class for the cell. We have 25 colors defined in schedule.css.
        const sortedTeamIndex = sortedTeams.indexOf(team.num);
        if (sortedTeamIndex < 0) {
            _log("Warning: can't find team " + team.num + " in sortedTeams")
        } else if (sortedTeamIndex < 25) {
            const className = "team-" + (sortedTeamIndex + 1);
            teamDiv.classList.add(className);
        }

        teamDiv.addEventListener('dragstart', function(e) {
            let rawEvent;
            if (e.originalEvent) {
                rawEvent = e.originalEvent;
            } else {
                rawEvent = e;
            }
            // rawEvent.target is the source node.

            teamDiv.style.opacity = 0.4;

            const dataTransfer = rawEvent.dataTransfer;

            draggingTeam = team;
            draggingCategory = category;
            draggingTeamDiv = teamDiv;

            dataTransfer.effectAllowed = 'move';

            // need something to transfer, otherwise the browser won't let us drag
            dataTransfer.setData('text/text', "true");
        });

        teamDiv.addEventListener('dragend', function(_) {
            // rawEvent.target is the source node.
            teamDiv.style.opacity = 1;
        });

        return teamDiv;
    }

    /**
     * 
     * @param slot
     *          Timeslot object
     * @param category
     *          Category object
     * @returns HTML div element
     */
    function createTimeslotCell(slot, category) {
        const cell = document.createElement("div");
        cell.classList.add("rTableCell");

        cell.addEventListener('dragover', function(e) {
            let rawEvent;
            if (e.originalEvent) {
                rawEvent = e.originalEvent;
            } else {
                rawEvent = e;
            }

            if (category == draggingCategory) {
                if (rawEvent.preventDefault) {
                    rawEvent.preventDefault(); // Necessary. Allows us to drop.
                }

                rawEvent.dataTransfer.dropEffect = 'move'; // See the section on the
                // DataTransfer object.

                //const transferObj = rawEvent.dataTransfer
                //    .getData('application/x-fll-finalist');

                return false;
            } else {
                return true;
            }
        });

        cell.addEventListener('drop', function(e) {
            let rawEvent;
            if (e.originalEvent) {
                rawEvent = e.originalEvent;
            } else {
                rawEvent = e;
            }

            // rawEvent.target is current target element.

            if (rawEvent.stopPropagation) {
                rawEvent.stopPropagation(); // Stops some browsers from redirecting.
            }

            if (cell.children.length > 0) {
                // move current team to old parent
                const oldTeamDiv = cell.children.item(0);
                const draggingParent = draggingTeamDiv.parentElement;
                draggingParent.appendChild(oldTeamDiv);
            }

            // Add team to the current cell
            cell.appendChild(draggingTeamDiv);

            // updates the schedule
            moveTeam(draggingTeam, draggingCategory, slot);

            draggingTeam = null;
            draggingCategory = null;
            draggingTeamDiv = null;
        });

        return cell;
    }

    function uploadData() {
        const waitList = [];

        const playoffSchedulesSuccess = function(_) {
            _log("Playoff schedules upload success")
        };
        const playoffSchedulesFail = function(result) {
            let message;
            if (null == result) {
                message = "Unknown server error";
            } else {
                message = result.message;
            }

            alert("Playoff schedules upload failure: " + message);
        }
        waitList.push(finalist_module.uploadPlayoffSchedules(
            playoffSchedulesSuccess, playoffSchedulesFail));

        const scheduleParamsSuccess = function(_) {
            _log("Schedule parameters upload success")
        };
        const scheduleParamsFail = function(result) {
            let message;
            if (null == result) {
                message = "Unknown server error";
            } else {
                message = result.message;
            }

            alert("Schedule parameters upload failure: " + message);
        }
        waitList.push(finalist_module.uploadScheduleParameters(
            scheduleParamsSuccess, scheduleParamsFail));

        const schedulesSuccess = function(_) {
            _log("Schedules upload success")
        };
        const schedulesFail = function(result) {
            let message;
            if (null == result) {
                message = "Unknown server error";
            } else {
                message = result.message;
            }

            alert("Schedules upload failure: " + message);
        }
        waitList.push(finalist_module.uploadSchedules(
            schedulesSuccess, schedulesFail));

        const nonNumericSuccess = function(_) {
            _log("Non-numeric upload success")
        };
        const nonNumericFail = function(result) {
            let message;
            if (null == result) {
                message = "Unknown server error";
            } else {
                message = result.message;
            }

            alert("Non-numeric nominees upload failure: " + message);
        }
        waitList.push(finalist_module.uploadNonNumericNominees(
            nonNumericSuccess, nonNumericFail));

        document.getElementById("wait-dialog").classList.remove("fll-sw-ui-inactive");
        Promise.all(waitList).then(function() {
            document.getElementById("wait-dialog").classList.add("fll-sw-ui-inactive");
        });
    }

    /**
     * Move a team from it's current timeslot to the newly specified timeslot for
     * the specified category. If the specified timeslot contains another team, then
     * the two teams are swapped.
     * 
     * @param team
     *          a Team object to move
     * @param category
     *          a Category object that the team is moving in
     * @param newSlot
     *          the new slot to put the team in
     */
    function moveTeam(team, category, newSlot) {
        let destSlot = null;
        let srcSlot = null;

        // remove team from all slots with this category
        for (const slot of schedule) {
            let foundTeam = false;
            for (const [categoryName, teamNumber] of Object.entries(slot.categories)) {
                if (categoryName == category.name && teamNumber == team.num) {
                    foundTeam = true;
                }
            } // foreach category

            if (slot.time.equals(newSlot.time)) {
                destSlot = slot;
            }

            if (foundTeam) {
                srcSlot = slot;
            }
        } // foreach timeslot

        if (null == destSlot) {
            alert("Internal error, can't find destination slot in schedule");
            return;
        }

        if (srcSlot.time.equals(destSlot.time)) {
            // dropping on the same slot where the team already was
            return;
        }

        // remove warning from source cell as it may become empty
        const srcCell = getCellForTimeAndCategory(srcSlot.time, category.name);
        srcCell.classList.remove("overlap-schedule");
        srcCell.classList.remove("overlap-playoff");

        if (null == destSlot.categories[category.name]) {
            // no team in the destination, just delete this team from the old slot
            delete srcSlot.categories[category.name];
        } else {
            const oldTeamNumber = destSlot.categories[category.name];
            const oldTeam = finalist_module.lookupTeam(oldTeamNumber);

            // clear the destination slot so that the warning check sees the correct
            // state for this team
            delete destSlot.categories[category.name];

            // move team to the source slot
            srcSlot.categories[category.name] = oldTeamNumber;

            // check new position to set warnings
            checkForTimeOverlap(srcSlot, oldTeamNumber);

            // check old position to clear warnings
            checkForTimeOverlap(destSlot, oldTeamNumber);

            if (finalist_module.hasPlayoffConflict(oldTeam, srcSlot)) {
                srcCell.classList.add("overlap-playoff");
            }
        }

        // add team to new slot, reference actual slot in case
        // newSlot and destSlot are not the same instance.
        // 12/23/2015 JPS - not sure how this could happen, but I must have thought it
        // possible.
        destSlot.categories[category.name] = team.num;

        // check where the team now is to see what warnings are needed
        checkForTimeOverlap(destSlot, team.num);

        // need to check where the team was to clear the warning
        checkForTimeOverlap(srcSlot, team.num);

        finalist_module.setSchedule(finalist_module.getCurrentDivision(), schedule);
        finalist_module.saveToLocalStorage();
    }

    /**
     * Add a row to the schedule table for the specified slot.
     */
    function addRowForSlot(slot) {
        const row = document.createElement("div");
        row.classList.add("rTableRow");
        document.getElementById("schedule_body").appendChild(row);

        const timeCell = document.createElement("div");
        row.appendChild(timeCell);
        timeCell.classList.add("rTableCell");
        timeCell.innerText = finalist_module.timeToDisplayString(slot.time);

        const playoffCell = document.createElement("div");
        row.appendChild(playoffCell);
        playoffCell.classList.add("rTableCell");
        let text = "";
        let first = true;
        for (const [bracketName, playoffSchedule] of Object.entries(finalist_module.getPlayoffSchedules())) {
            if (finalist_module.slotHasPlayoffConflict(playoffSchedule, slot)) {
                if (first) {
                    first = false;
                } else {
                    text = text + ",";
                }
                text = text + bracketName;
            }
        }
        playoffCell.innerText = text;

        const categoriesToCells = {};
        const teamsInSlot = {};
        for (const category of finalist_module.getAllScheduledCategories()) {
            const cell = createTimeslotCell(slot, category);
            row.appendChild(cell);

            categoriesToCells[category.name] = cell;

            const teamNum = slot.categories[category.name];
            if (teamNum != null) {
                const team = finalist_module.lookupTeam(teamNum);
                const teamDiv = createTeamDiv(team, category);
                cell.appendChild(teamDiv);
                teamsInSlot[teamNum] = true;
            }
        } // foreach category

        timeToCells[finalist_module.timeToDisplayString(slot.time)] = categoriesToCells;

        // now check for overlaps in the loaded schedule
        for (const [teamNum, _] of Object.entries(teamsInSlot)) {
            checkForTimeOverlap(slot, teamNum);
        }
    }

    function createSortedCategories() {
        sortedCategories = [];

        for (const category of finalist_module.getAllCategories()) {
            sortedCategories.push(category);
        }
        deliberationModule.sortedCategories.sort(function(a, b) {
            if (a.name == finalist_module.CHAMPIONSHIP_NAME && b.name != finalist_module.CHAMPIONSHIP_NAME) {
                return -1;
            } else if (a.name == finalist_module.CHAMPIONSHIP_NAME && b.name == finalist_module.CHAMPIONSHIP_NAME) {
                // shouldn't happen
                return 0;
            } else if (a.name != finalist_module.CHAMPIONSHIP_NAME && b.name == finalist_module.CHAMPIONSHIP_NAME) {
                return 1;
            } else if (a.numeric && !b.numeric) {
                return -1;
            } else if (!a.numeric && b.numeric) {
                return 1;
            } else {
                // sort by name
                return a.name.localeCompare(b.name);
            }
        });
    }

    function updatePage() {
        createSortedCategories();

        // output header
        updateHeader();

        const currentDivision = finalist_module.getCurrentDivision()
        schedule = finalist_module.getSchedule(currentDivision);
        if (null == schedule || 0 == schedule.length) {
            schedule = finalist_module.scheduleFinalists(currentDivision);
            finalist_module.setSchedule(currentDivision, schedule);
            finalist_module.saveToLocalStorage();
        }

        finalistsCount = finalist_module.getTeamToCategoryMap(finalist_module
            .getCurrentDivision());
        sortedTeams = finalist_module.sortTeamsByCategoryCount(finalistsCount);

        removeChildren(document.getElementById("deliberation_body"));
    }

    document.addEventListener("DOMContentLoaded", function() {
        finalist_module.loadFromLocalStorage();

        const awardGroupsElement = document.getElementById("award_groups");
        removeChildren(awardGroupsElement);
        const divisions = finalist_module.getDivisions();
        for (const [i, division] of enumerate(divisions)) {
            const divisionOption = document.createElement("option");
            divisionOption.setAttribute("value", i);
            divisionOption.innerText = division;
            if (division == finalist_module.getCurrentDivision()) {
                divisionOption.setAttribute("selected", "true");
            }
            awardGroupsElement.appendChild(divisionOption);
        }
        awardGroupsElement.addEventListener("change", function() {
            handleAwardGroupChange();
        });

        // force an update to generate the initial page
        handleAwardGroupChange();

        document.getElementById("upload").addEventListener("click", function() {
            uploadData();
        });
    });
}