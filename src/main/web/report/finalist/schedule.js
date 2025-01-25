/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

const finalistScheduleModule = {};

{
    let draggingTeam = null;
    let draggingCategory = null;
    let draggingTeamDiv = null;
    let schedule = null;

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

    function handleDivisionChange() {
        const divIndex = document.getElementById("divisions").value;
        const div = finalist_module.getDivisionByIndex(divIndex);
        finalist_module.setCurrentDivision(div);
        updatePage();
        finalist_module.saveToLocalStorage();
    }

    function updateHeader() {
        const headerRow = document.getElementById("schedule_header");
        removeChildren(headerRow);

        const timeSlotHeader = document.createElement("div");
        headerRow.appendChild(timeSlotHeader);
        timeSlotHeader.classList.add("rTableHead");
        timeSlotHeader.innerText = "Time Slot";

        const h2hHeader = document.createElement("div");
        headerRow.appendChild(h2hHeader);
        h2hHeader.classList.add("rTableHead");
        if (finalist_module.getRunningHead2Head()) {
            h2hHeader.innerText = "Head to Head";
        } else {
            h2hHeader.innerText = "Finalist Group";
        }

        for (const category of finalist_module.getAllScheduledCategories()) {
            const room = finalist_module.getRoom(category, finalist_module.getCurrentDivision());
            const header = document.createElement("div");
            headerRow.appendChild(header);
            header.classList.add("rTableHead");

            const categoryNameEle = document.createElement("div");
            header.appendChild(categoryNameEle);
            categoryNameEle.innerText = category.name;

            if (null != room && "" != room) {
                const roomEle = document.createElement("div");
                categoryNameEle.appendChild(roomEle);
                roomEle.innerText = "Room: " + room;
            }
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

        const finalistGroupsSuccess = function(_) {
            _log("Finalist groups upload success")
        };
        const finalistGroupsFail = function(result) {
            let message;
            if (null == result) {
                message = "Unknown server error";
            } else {
                message = result.message;
            }

            alert("Finalist groups upload failure: " + message);
        }
        waitList.push(finalist_module.uploadFinalistGroups(
            finalistGroupsSuccess, finalistGroupsFail));

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
            if (finalist_module.outsideFinalistGroup(oldTeam, srcSlot)) {
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
     * See if the specified team is in the same slot in multiple categories or has a
     * conflict with playoff times.
     * 
     * @param slot
     *          the slot to check
     * @param teamNumber
     *          the team number to check for
     */
    function checkForTimeOverlap(slot, teamNumber) {
        const team = finalist_module.lookupTeam(teamNumber);

        let numCategories = 0;
        for (const [_, checkTeamNumber] of Object.entries(slot.categories)) {
            if (checkTeamNumber == teamNumber) {
                numCategories = numCategories + 1;
            }
        }

        const hasPlayoffConflict = finalist_module.hasPlayoffConflict(team, slot);
        const outsideFinalistGroup = finalist_module.outsideFinalistGroup(team, slot);

        for (const [name, checkTeamNumber] of Object.entries(slot.categories)) {
            if (checkTeamNumber == teamNumber) {
                const cell = getCellForTimeAndCategory(slot.time, name);
                if (null != cell) {
                    if (numCategories > 1) {
                        cell.classList.add('overlap-schedule');
                        /*
                         * alert("Found " + teamNumber + " in multiple categories at the same
                         * time");
                         */
                    } else {
                        cell.classList.remove('overlap-schedule');
                    }

                    if (hasPlayoffConflict) {
                        cell.classList.add('overlap-playoff');
                    } else {
                        cell.classList.remove('overlap-playoff');
                    }

                    if (outsideFinalistGroup) {
                        cell.classList.add('overlap-playoff');
                    } else {
                        cell.classList.remove('overlap-playoff');
                    }

                } // found cell
                else {
                    alert("Can't find cell for " + slot.time + " cat: "
                        + name);
                    return;
                }
            } // team number matches
        }

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
        let playoffCellText = "";
        if (finalist_module.getRunningHead2Head()) {
            let first = true;
            for (const [bracketName, playoffSchedule] of Object.entries(finalist_module.getPlayoffSchedules())) {
                if (finalist_module.slotHasPlayoffConflict(playoffSchedule, slot)) {
                    if (first) {
                        first = false;
                    } else {
                        playoffCellText = playoffCellText + ",";
                    }
                    playoffCellText = playoffCellText + bracketName;
                }
            }
        } else {
            let first = true;
            for (const [name, finalistGroup] of Object.entries(finalist_module.getFinalistGroups())) {
                if (finalistGroup.startTime && finalistGroup.endTime) {
                    if (slot.time.isBefore(finalistGroup.endTime) && finalistGroup.startTime.isBefore(slot.endTime)) {
                        if (first) {
                            first = false;
                        } else {
                            playoffCellText = playoffCellText + ",";
                        }
                        playoffCellText = playoffCellText + name;
                    }
                }
            }
        }
        playoffCell.innerText = playoffCellText;

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

    function updatePage() {

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

        removeChildren(document.getElementById("schedule_body"));
        for (const slot of schedule) {
            addRowForSlot(slot);
        } // foreach timeslot
    }

    document.addEventListener("DOMContentLoaded", function() {
        finalist_module.loadFromLocalStorage();

        document.getElementById("previous").addEventListener("click", function() {
            const championshipCategory = finalist_module
                .getCategoryByName(finalist_module.CHAMPIONSHIP_NAME);
            finalist_module.setCurrentCategoryName(championshipCategory.name);
            finalist_module.saveToLocalStorage();
            location.href = "numeric.html";
        });

        const divisionsElement = document.getElementById("divisions");
        removeChildren(divisionsElement);
        finalist_module.getDivisions().forEach(function(division, i) {
            const divisionOption = document.createElement("option");
            divisionOption.setAttribute("value", i);
            divisionOption.innerText = division;
            if (division == finalist_module.getCurrentDivision()) {
                divisionOption.selected = true;
            }

            divisionsElement.appendChild(divisionOption);
        }); // foreach division
        divisionsElement.addEventListener("change", function() {
            handleDivisionChange();
        });

        // force an update to generate the initial page
        handleDivisionChange();

        document.getElementById('regenerate_schedule').addEventListener("click", function() {
            finalist_module.setSchedule(finalist_module.getCurrentDivision(), null);
            finalist_module.saveToLocalStorage();
            updatePage();
        });

        document.getElementById('add_timeslot').addEventListener("click", function() {
            const newSlot = finalist_module.addSlotToSchedule(schedule);
            finalist_module.saveToLocalStorage();
            addRowForSlot(newSlot);
        });

        document.getElementById("upload").addEventListener("click", function() {
            uploadData();
        });

        finalist_module.displayNavbar();
    });
}