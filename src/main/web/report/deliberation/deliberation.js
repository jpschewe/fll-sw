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
     * Name of the performance category. Used to display in the table header and to reference teams. Should not need to match the category name in the challenge description.
     */
    deliberationModule.PERFORMANCE_CATEGORY_NAME = "Performance";

    /**
     * {Array} of finalist {Category} objects sorted in the order for the table. 
     */
    let sortedCategories = [];

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
            if (category.name == deliberationModule.PERFORMANCE_CATEGORY_NAME) {
                header.classList.add("performance");
            }
            else if (category.scheduled) {
                header.classList.add("scheduled");
            } else {
                header.classList.add("non-scheduled");
            }

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

    //FIXME needs new implementation
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

        //FIXME

        document.getElementById("wait-dialog").classList.remove("fll-sw-ui-inactive");
        Promise.all(waitList).then(function() {
            document.getElementById("wait-dialog").classList.add("fll-sw-ui-inactive");
        });
    }

    function createSortedCategories() {
        sortedCategories = [];

        for (const category of finalist_module.getAllCategories()) {
            sortedCategories.push(category);
        }
        let performanceCategory = finalist_module.getCategoryByName(deliberationModule.PERFORMANCE_CATEGORY_NAME);
        if (null == performanceCategory) {
            performanceCategory = finalist_module.addCategory(deliberationModule.PERFORMANCE_CATEGORY_NAME, true, true);
            sortedCategories.push(performanceCategory);
        }

        sortedCategories.sort(function(a, b) {
            if (a.name == finalist_module.CHAMPIONSHIP_NAME && b.name != finalist_module.CHAMPIONSHIP_NAME) {
                return -1;
            } else if (a.name == finalist_module.CHAMPIONSHIP_NAME && b.name == finalist_module.CHAMPIONSHIP_NAME) {
                // shouldn't happen
                return 0;
            } else if (a.name != finalist_module.CHAMPIONSHIP_NAME && b.name == finalist_module.CHAMPIONSHIP_NAME) {
                return 1;
            } else if (a.name == deliberationModule.PERFORMANCE_CATEGORY_NAME && b.name == deliberationModule.PERFORMANCE_CATEGORY_NAME) {
                // shouldn't happen
                return 0;
            } else if (a.name == deliberationModule.PERFORMANCE_CATEGORY_NAME && b.name != deliberationModule.PERFORMANCE_CATEGORY_NAME && !b.scheduled) {
                return -1;
            } else if (a.name == deliberationModule.PERFORMANCE_CATEGORY_NAME && b.name != deliberationModule.PERFORMANCE_CATEGORY_NAME && b.scheduled) {
                return 1;
            } else if (a.name != deliberationModule.PERFORMANCE_CATEGORY_NAME && a.scheduled && b.name == deliberationModule.PERFORMANCE_CATEGORY_NAME) {
                return -1;
            } else if (a.name != deliberationModule.PERFORMANCE_CATEGORY_NAME && !a.scheduled && b.name == deliberationModule.PERFORMANCE_CATEGORY_NAME) {
                return 1;
            } else if (a.scheduled && !b.scheduled) {
                return -1;
            } else if (!a.scheduled && b.scheduled) {
                return 1;
            } else {
                // sort by name
                return a.name.localeCompare(b.name);
            }
        });
    }

    function addPlaceRows(body) {
        for (let place = 1; place <= 4; ++place) {
            const row = document.createElement("div");
            row.classList.add("rTableRow");
            body.appendChild(row);

            const placeCell = document.createElement("div");
            row.appendChild(placeCell);
            placeCell.classList.add("rTableCell");
            placeCell.innerText = place;

            for (let i = 0; i < sortedCategories.length; ++i) {
                const cell = document.createElement("div");
                row.appendChild(cell);
                cell.classList.add("rTableCell");
            }

        }
    }

    function addSeparator(body) {
        const row = document.createElement("div");
        row.classList.add("rTableRow");
        body.appendChild(row);

        // place, all categories
        const numColumns = 1 + sortedCategories.length;
        for (let i = 0; i < numColumns; ++i) {
            const cell = document.createElement("div");
            row.appendChild(cell);
            cell.classList.add("rTableCell");
            cell.classList.add("separator");
        }
    }

    function writing1Identifier(category) {
        return "writing1_" + category.catId;
    }

    function writing2Identifier(category) {
        return "writing2_" + category.catId;
    }

    function addWriters(body) {
        const row1 = document.createElement("div");
        row1.classList.add("rTableRow");
        body.appendChild(row1);

        const row2 = document.createElement("div");
        row2.classList.add("rTableRow");
        body.appendChild(row2);

        const judgeCell = document.createElement("div");
        row1.appendChild(judgeCell);
        judgeCell.classList.add("rTableCell");
        judgeCell.innerText = "Judge"

        const writingCell = document.createElement("div");
        row2.appendChild(writingCell);
        writingCell.classList.add("rTableCell");
        writingCell.innerText = "Writing script";

        for (const [i, category] of enumerate(sortedCategories)) {
            const cell1 = document.createElement("div");
            row1.appendChild(cell1);
            cell1.classList.add("rTableCell");
            const input1 = document.createElement("input");
            cell1.appendChild(input1);
            input1.setAttribute("type", "text");
            input1.setAttribute("id", writing1Identifier(category));
            input1.setAttribute("name", writing1Identifier(category));
            input1.setAttribute("size", "10");

            const cell2 = document.createElement("div");
            row2.appendChild(cell2);
            cell2.classList.add("rTableCell");
            const input2 = document.createElement("input");
            cell2.appendChild(input2);
            input2.setAttribute("type", "text");
            input2.setAttribute("id", writing2Identifier(category));
            input2.setAttribute("name", writing2Identifier(category));
            input2.setAttribute("size", "10");
        }
    }

    function updatePage() {
        createSortedCategories();

        // output header
        updateHeader();

        const currentDivision = finalist_module.getCurrentDivision()
        const body = document.getElementById("deliberation_body");
        // FIXME add row for number of awards given
        removeChildren(body);
        addPlaceRows(body);
        addSeparator(body);
        addWriters(body);
        addSeparator(body);
        // FIXME add teams
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