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
        const searchTimeStr = $.finalist.timeToDisplayString(searchTime);

        let foundCell = null;

        $.each(timeToCells, function(timeStr, categoryToCells) {
            if (searchTimeStr == timeStr) {

                $.each(categoryToCells, function(catName, cell) {
                    if (catName == searchCatName) {
                        foundCell = cell;
                    }
                });

            }
        });

        return foundCell;
    }

    function handleDivisionChange() {
        const divIndex = $("#divisions").val();
        const div = $.finalist.getDivisionByIndex(divIndex);
        $.finalist.setCurrentDivision(div);
        updatePage();
    }

    function updateHeader() {
        const headerRow = $("#schedule_header");
        headerRow.empty();

        headerRow.append($("<div class='rTableHead'>Time Slot</div>"));

        headerRow.append($("<div class='rTableHead'>Head to Head</div>"));

        $.each($.finalist.getAllScheduledCategories(), function(i, category) {
            const room = $.finalist.getRoom(category, $.finalist.getCurrentDivision());
            let header;
            if (room == undefined || "" == room) {
                header = $("<div class='rTableHead'>" + category.name + "</div>");
            } else {
                header = $("<div class='rTableHead'>" + category.name + "<br/>Room: "
                    + room + "</div>");
            }
            headerRow.append(header);
        });
    }

    /**
     * Create the div element for a team in a particular category.
     * 
     * @param team
     *          a Team object
     * @param category
     *          a Category object
     * @returns a jquery div object
     */
    function createTeamDiv(team, category) {
        const teamCategories = finalistsCount[team.num];
        const numCategories = teamCategories.length;
        const group = team.judgingGroup;
        const teamDiv = $("<div draggable='true'>" + team.num + " - " + team.name
            + " (" + group + ", " + numCategories + ")</div>");

        teamDiv.on('dragstart', function(e) {
            let rawEvent;
            if (e.originalEvent) {
                rawEvent = e.originalEvent;
            } else {
                rawEvent = e;
            }
            // rawEvent.target is the source node.

            $(teamDiv).css('opacity', '0.4');

            const dataTransfer = rawEvent.dataTransfer;

            draggingTeam = team;
            draggingCategory = category;
            draggingTeamDiv = teamDiv;

            dataTransfer.effectAllowed = 'move';

            // need something to transfer, otherwise the browser won't let us drag
            dataTransfer.setData('text/text', "true");
        });
        teamDiv.on('dragend', function(e) {
            // rawEvent.target is the source node.
            $(teamDiv).css('opacity', '1');
        });

        return teamDiv;
    }

    /**
     * 
     * @param slot
     *          Timeslot object
     * @param category
     *          Category object
     * @returns jquery div object
     */
    function createTimeslotCell(slot, category) {
        var cell = $("<div class='rTableCell'></div>");

        cell.on('dragover', function(e) {
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

                const transferObj = rawEvent.dataTransfer
                    .getData('application/x-fll-finalist');

                return false;
            } else {
                return true;
            }
        });

        cell.on('drop', function(e) {
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

            if (cell.children().length > 0) {
                // move current team to old parent
                const oldTeamDiv = cell.children().first();
                const draggingParent = draggingTeamDiv.parent();
                draggingParent.append(oldTeamDiv);
            }

            // Add team to the current cell
            cell.append(draggingTeamDiv);

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

        const playoffSchedulesSuccess = function(result) {
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
        waitList.push($.finalist.uploadPlayoffSchedules(
            playoffSchedulesSuccess, playoffSchedulesFail));

        const scheduleParamsSuccess = function(result) {
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
        waitList.push($.finalist.uploadScheduleParameters(
            scheduleParamsSuccess, scheduleParamsFail));

        const schedulesSuccess = function(result) {
            _log("Schdules upload success")
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
        waitList.push($.finalist.uploadSchedules(
            schedulesSuccess, schedulesFail));

        $("#wait-dialog").dialog("open");
        $.when.apply($, waitList).done(function() {
            $("#wait-dialog").dialog("close");
        }).fail(function() {
            $("#wait-dialog").dialog("close");
        });
    }

    /**
     * Convert schedule to the JSON required to send to the server.
     */
    function updateScheduleToSend() {

        const schedRows = [];
        $.each(schedule, function(_, slot) {
            $.each($.finalist.getAllScheduledCategories(), function(_, category) {
                const teamNum = slot.categories[category.name];
                if (teamNum != null) {
                    const dbrow = new FinalistDBRow(category.name, slot.time, teamNum);
                    schedRows.push(dbrow);
                }
            }); // foreach category

        }); // foreach timeslot

        const jsonData = JSON.stringify(schedRows);
        $('#sched_data').val(jsonData);
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
        var destSlot = null;
        var srcSlot = null;

        // remove team from all slots with this category
        $.each(schedule, function(i, slot) {
            let foundTeam = false;
            $.each(slot.categories, function(categoryName, teamNumber) {
                if (categoryName == category.name && teamNumber == team.num) {
                    foundTeam = true;
                }
            }); // foreach category

            if (slot.time.equals(newSlot.time)) {
                destSlot = slot;
            }

            if (foundTeam) {
                srcSlot = slot;
            }
        }); // foreach timeslot

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
        srcCell.removeClass("overlap-schedule");
        srcCell.removeClass("overlap-playoff");

        if (null == destSlot.categories[category.catName]) {
            // no team in the destination, just delete this team from the old slot
            delete srcSlot.categories[category.catName];
        } else {
            const oldTeamNumber = destSlot.categories[category.catName];
            const oldTeam = $.finalist.lookupTeam(oldTeamNumber);

            // clear the destination slot so that the warning check sees the correct
            // state for this team
            delete destSlot.categories[category.catName];

            // move team to the source slot
            srcSlot.categories[category.catName] = oldTeamNumber;

            // check new position to set warnings
            checkForTimeOverlap(srcSlot, oldTeamNumber);

            // check old position to clear warnings
            checkForTimeOverlap(destSlot, oldTeamNumber);

            if ($.finalist.hasPlayoffConflict(oldTeam, srcSlot)) {
                srcCell.addClass("overlap-playoff");
            }
        }

        // add team to new slot, reference actual slot in case
        // newSlot and destSlot are not the same instance.
        // 12/23/2015 JPS - not sure how this could happen, but I must have thought it
        // possible.
        destSlot.categories[category.catName] = team.num;

        // check where the team now is to see what warnings are needed
        checkForTimeOverlap(destSlot, team.num);

        // need to check where the team was to clear the warning
        checkForTimeOverlap(srcSlot, team.num);

        $.finalist.setSchedule($.finalist.getCurrentDivision(), schedule);
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
        const team = $.finalist.lookupTeam(teamNumber);

        let numCategories = 0;
        $.each(slot.categories, function(categoryId, checkTeamNumber) {
            if (checkTeamNumber == teamNumber) {
                numCategories = numCategories + 1;
            }
        });

        const hasPlayoffConflict = $.finalist.hasPlayoffConflict(team, slot);

        $.each(slot.categories, function(name, checkTeamNumber) {
            if (checkTeamNumber == teamNumber) {
                const cell = getCellForTimeAndCategory(slot.time, name);
                if (null != cell) {
                    if (numCategories > 1) {
                        cell.addClass('overlap-schedule');
                        /*
                         * alert("Found " + teamNumber + " in multiple categories at the same
                         * time");
                         */
                    } else {
                        cell.removeClass('overlap-schedule');
                    }

                    if (hasPlayoffConflict) {
                        cell.addClass('overlap-playoff');
                    } else {
                        cell.removeClass('overlap-playoff');
                    }

                } // found cell
                else {
                    alert("Can't find cell for " + time.hour + ":" + time.minute + " cat: "
                        + categoryId);
                    return;
                }
            } // team number matches
        });

    }

    /**
     * Add a row to the schedule table for the specified slot.
     */
    function addRowForSlot(slot) {
        const row = $("<div class='rTableRow'></div>");
        $("#schedule_body").append(row);

        row.append($("<div class='rTableCell'>"
            + $.finalist.timeToDisplayString(slot.time) + "</div>"));

        const playoffCell = $("<div class='rTableCell'></div>");
        row.append(playoffCell);
        let first = true;
        $.each($.finalist.getPlayoffSchedules(), function(bracketName, playoffSchedule) {
            if ($.finalist.slotHasPlayoffConflict(playoffSchedule, slot)) {
                if (first) {
                    first = false;
                } else {
                    playoffCell.append(",");
                }
                playoffCell.append(bracketName);
            }
        });

        const categoriesToCells = {};
        const teamsInSlot = {};
        $.each($.finalist.getAllScheduledCategories(), function(i, category) {
            const cell = createTimeslotCell(slot, category);
            row.append(cell);

            categoriesToCells[category.name] = cell;

            const teamNum = slot.categories[category.name];
            if (teamNum != null) {
                const team = $.finalist.lookupTeam(teamNum);
                const teamDiv = createTeamDiv(team, category);
                cell.append(teamDiv);
                teamsInSlot[teamNum] = true;
            }
        }); // foreach category

        timeToCells[$.finalist.timeToDisplayString(slot.time)] = categoriesToCells;

        // now check for overlaps in the loaded schedule
        $.each(teamsInSlot, function(teamNum, _) {
            checkForTimeOverlap(slot, teamNum);
        });
    }

    function updatePage() {

        // output header
        updateHeader();

        schedule = $.finalist.getSchedule($.finalist.getCurrentDivision());
        finalistsCount = $.finalist.getTeamToCategoryMap($.finalist
            .getCurrentDivision());

        $("#schedule_body").empty();
        $.each(schedule, function(i, slot) {
            addRowForSlot(slot);
        }); // foreach timeslot

        const categoryRows = [];
        $.each($.finalist.getAllScheduledCategories(), function(_, category) {
            const cat = new FinalistCategory(category.name, $.finalist.getRoom(category,
                $.finalist.getCurrentDivision()));
            categoryRows.push(cat);
        }); // foreach category
        $('#category_data').val($.toJSON(categoryRows));
        $('#division_data').val($.finalist.getCurrentDivision());
    }

    $(document).ready(
        function() {
            $("#previous").click(
                function() {
                    const championshipCategory = $.finalist
                        .getCategoryByName($.finalist.CHAMPIONSHIP_NAME);
                    $.finalist.setCurrentCategoryName(championshipCategory.name);
                    location.href = "numeric.html";
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

            // force an update to generate the initial page
            handleDivisionChange();

            // doesn't depend on the division, so can be done only once
            const allNonNumericNominees = $.finalist.prepareNonNumericNomineesToSend();
            $('#non-numeric-nominees_data').val($.toJSON(allNonNumericNominees));

            $('#regenerate_schedule').click(function() {
                $.finalist.setSchedule($.finalist.getCurrentDivision(), null);
                updatePage();
            });

            $('#add_timeslot').click(function() {
                const newSlot = $.finalist.addSlotToSchedule(schedule);
                addRowForSlot(newSlot);
            });

            $("#upload").click(
                function() {
                    uploadData();
                });

            $("#wait-dialog").dialog({
                autoOpen: false,
                modal: true,
                dialogClass: "no-close",
                closeOnEscape: false
            });

            $.finalist.displayNavbar();
        });
}