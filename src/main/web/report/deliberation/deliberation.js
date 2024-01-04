/*
 * Copyright (c) 2024 HighTechKids.  All rights reserved.
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

const deliberationModule = {};

{
    if (typeof fllStorage != 'object') {
        throw new Error("fllStorage needs to be loaded");
    }
    if (typeof finalist_module != 'object') {
        throw new Error("finalist_modeule needs to be loaded");
    }

    const STORAGE_PREFIX = "fll.deliberation";

    let _categories;

    function _init_variables() {
        _categories = new Map();
    }

    function _save() {
        fllStorage.set(STORAGE_PREFIX, "_categories", _categories);
    }

    function _load() {
        _init_variables();

        let value = fllStorage.get(STORAGE_PREFIX, "_categories");
        if (null != value) {
            _categories = value;
        }
    }

    function _clear_local_storage() {
        fllStorage.clearNamespace(STORAGE_PREFIX);
    }


    class Category {
        /**
         * Constructor for a category. Finds the first free ID and assigns it to this
         * new category.
         * 
         * @param name
         *          the name of the category
         * @param scheduled
         *          boolean stating if this is a scheduled category
         */
        constructor(name, scheduled) {
            let category_id;
            // find the next available id
            for (category_id = 0; category_id < Number.MAX_VALUE
                && _categories[category_id]; category_id = category_id + 1)
                ;

            if (category_id == Number.MAX_VALUE || category_id + 1 == Number.MAX_VALUE) {
                throw new Error("No free category IDs");
            }

            this.name = name;
            this.catId = category_id;
            this.nominees = new Map(); // award group -> list of team numbers
            this.winners = new Map(); // award group -> list of team numbers, null for empty cells
            this.scheduled = scheduled;
            this.writer1 = new Map(); // award group -> string
            this.writer2 = new Map(); // award group -> string

            _categories[this.catId] = this;
        }

        getWriter1() {
            return this.writer1.get(finalist_module.getCurrentDivision());
        }
        setWriter1(v) {
            this.writer1.set(finalist_module.getCurrentDivision(), v);
        }

        getWriter2() {
            return this.writer2.get(finalist_module.getCurrentDivision());
        }
        setWriter2(v) {
            this.writer2.set(finalist_module.getCurrentDivision(), v);
        }

        getNominees() {
            const value = this.nominees.get(finalist_module.getCurrentDivision());
            if (null == value) {
                return [];
            } else {
                return value;
            }
        }
        setNominees(v) {
            this.nominees.set(finalist_module.getCurrentDivision(), v);
        }
        /**
         * @param {int} teamNumber team to add to the list of nominees
         * @return true if this team wasn't in the list of nominees (list was modified)
         */
        addNominee(teamNumber) {
            const value = this.getNominees();
            if (!value.includes(teamNumber)) {
                value.push(teamNumber);
                this.setNominees(value);
                return true;
            } else {
                return false;
            }
        }
        /**
         * @param {int} teamNumber team to remove from the list of nominees
         * @return true if this team was in the list of nominees (list was modified)
         */
        removeNominee(teamNumber) {
            const value = this.getNominees();
            if (value.includes(teamNumber)) {
                removeFromArray(value, teamNumber);
                this.setNominees(value);
                return true;
            } else {
                return false;
            }
        }

        getWinners() {
            const value = this.winners.get(finalist_module.getCurrentDivision());
            if (null == value) {
                return [null];
            } else {
                return value;
            }
        }
        setWinners(v) {
            this.winners.set(finalist_module.getCurrentDivision(), v);
        }
        /**
         * @param place the place (1-based) of the team
         * @param teamNumber the team number in this place
         */
        setWinner(place, teamNumber) {
            const winners = this.getWinners();
            if (place < 1) {
                throw new Error("Place must be greater than 0");
            }
            if (place > winners.length) {
                throw new Error("Place must less than or equal to the number of awards");
            }

            const placeIndex = place - 1;
            winners[placeIndex] = teamNumber;
            this.setWinners(winners);
        }
        /**
         * Remove a winner. Sets the winner to null.
         * 
         * @param place the place (1-based) of the team
         */
        unsetWinner(place) {
            const winners = getWinners();
            if (place < 1) {
                throw new Error("Place must be greater than 0");
            }
            if (place > winners.length) {
                throw new Error("Place must less than or equal to the number of awards");
            }

            const placeIndex = place - 1;
            winners[placeIndex] = null;
        }

        getNumAwards() {
            return this.getWinners().length;
        }
        setNumAwards(v) {
            const winners = this.getWinners();
            while (winners.length > v) {
                winners.pop();
            }
            while (winners.length < v) {
                winners.push(null);
            }
            this.setWinners(winners);
        }

    }

    let draggingTeam = null;
    let draggingCategory = null;
    let draggingTeamDiv = null;

    /**
     * Name of the performance category. Used to display in the table header and to reference teams. Should not need to match the category name in the challenge description.
     */
    deliberationModule.PERFORMANCE_CATEGORY_NAME = "Performance";

    /**
     * {Array} of {Category} objects sorted in the order for the table. 
     */
    let sortedCategories = [];

    /**
     * teamNumber -> list of div elements.
     */
    let teamDivs = new Map();

    /**
     * teamNumber -> count of categories team is a winner for.
     */
    let teamWinnersCount = new Map();

    /**
     * teamNumber -> CSS class name
     */
    let teamColors = new Map();

    let nextTeamColor = 1;

    /**
     * @return the CSS class name to use for the next team color, returns an empty string if there are no more styles left.
     */
    function getNextTeamColor() {
        if (nextTeamColor > 25) {
            return "";
        } else {
            const value = "team-" + nextTeamColor;
            ++nextTeamColor;
            return value;
        }
    }

    /**
     * @param {int} teamNumber team number to set style for
     * @param {HTMLElement} ele HTML element that should have the style set 
     */
    function setTeamColorStyle(teamNumber, ele) {
        let teamColor = teamColors.get(teamNumber);
        if (null == teamColor) {
            teamColor = getNextTeamColor();
            teamColors.set(teamNumber, teamColor);
        }

        if (teamColor.length > 0) {
            ele.classList.add(teamColor);
        }
    }

    function getTeamWinnersCount(teamNumber) {
        const value = teamWinnersCount.get(teamNumber);
        if (null == value) {
            return 0;
        } else {
            return value;
        }
    }

    function addTeamToWinners(teamNumber) {
        let count = getTeamWinnersCount(teamNumber);
        ++count;
        teamWinnersCount.set(count);

        updateTeamDivForWinners(teamNumber);
    }

    function removeTeamFromWinners(teamNumber) {
        let count = getTeamWinnersCount(teamNumber);
        --count;
        count = Math.max(0, count); // just in case
        teamWinnersCount.set(count);

        updateTeamDivForWinners(teamNumber);
    }

    function updateTeamDivForWinners(teamNumber) {
        const count = getTeamWinnersCount(teamNumber);

        const divs = teamDivs.get(teamNumber);
        if (null != divs) {
            for (const div of divs) {
                if (count > 0) {
                    div.classList.add("winner");
                } else {
                    div.classList.remove("winner");
                }
            }
        }
    }

    /**
      * Get a category by name
      * 
      * @param toFind
      *          the name to find
      * @returns the category or null
      */
    function getCategoryByName(toFind) {
        let category = null;
        for (const [_, val] of _categories) {
            if (val.name == toFind) {
                category = val;
            }
        }
        return category;
    };

    function loadFromFinalist() {
        createSortedCategories();
        _save();
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
        headerRow.setAttribute("id", "header_row");

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
     * @param {finalist_module.Team} team
     *          a Team object
     * @param {Category} category
     *          a Category object
     * @returns an HTML div element for the team
     */
    function createTeamDiv(team, category) {
        const group = team.judgingGroup;
        const teamDiv = document.createElement("div");
        teamDiv.setAttribute("draggable", "true");
        teamDiv.innerText = team.num + " - " + team.name + " (" + group + ")";

        teamDiv.addEventListener('dragstart', function(e) {
            let rawEvent;
            if (e.originalEvent) {
                rawEvent = e.originalEvent;
            } else {
                rawEvent = e;
            }
            // rawEvent.target is the source node.

            teamDiv.classList.add("dragging");

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
            teamDiv.classList.remove("dragging");
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
            const dCategory = new Category(category.name, category.scheduled);
            for (const teamNumber of category.teams) {
                dCategory.addNominee(teamNumber);
            }
            sortedCategories.push(dCategory);
        }
        let performanceCategory = getCategoryByName(deliberationModule.PERFORMANCE_CATEGORY_NAME);
        if (null == performanceCategory) {
            performanceCategory = new Category(deliberationModule.PERFORMANCE_CATEGORY_NAME, true);
            sortedCategories.push(performanceCategory);
            //FIXME need to load performance winners from somewhere
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

    function placeRowIdentifier(place) {
        return "place_row_" + place;
    }

    function placeCellIdentifier(category, place) {
        return "place_cell_" + category.catId + "_" + place;
    }

    function addPlaceRow(body, place) {
        const prevRow = place < 2 ? document.getElementById("num_awards_row") : document.getElementById(placeRowIdentifier(place - 1));

        const row = document.createElement("div");
        row.classList.add("rTableRow");
        body.insertBefore(row, prevRow.nextSibling)
        row.setAttribute("id", placeRowIdentifier(place));

        const placeCell = document.createElement("div");
        row.appendChild(placeCell);
        placeCell.classList.add("rTableCell");
        placeCell.innerText = place;

        for (const category of sortedCategories) {
            const cell = document.createElement("div");
            row.appendChild(cell);
            cell.classList.add("rTableCell");
            cell.setAttribute("id", placeCellIdentifier(category, place));
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
        row1.setAttribute("id", "writing1_row");

        const row2 = document.createElement("div");
        row2.classList.add("rTableRow");
        body.appendChild(row2);
        row2.setAttribute("id", "writing2_row");

        const judgeCell = document.createElement("div");
        row1.appendChild(judgeCell);
        judgeCell.classList.add("rTableCell");
        judgeCell.innerText = "Judge"

        const writingCell = document.createElement("div");
        row2.appendChild(writingCell);
        writingCell.classList.add("rTableCell");
        writingCell.innerText = "Writing script";

        for (const category of sortedCategories) {
            const cell1 = document.createElement("div");
            row1.appendChild(cell1);
            cell1.classList.add("rTableCell");
            const input1 = document.createElement("input");
            cell1.appendChild(input1);
            input1.setAttribute("type", "text");
            input1.setAttribute("id", writing1Identifier(category));
            input1.setAttribute("name", writing1Identifier(category));
            input1.setAttribute("size", "10");
            input1.addEventListener("change", () => {
                category.setWriter1(input1.value);
                _save();
            });

            const cell2 = document.createElement("div");
            row2.appendChild(cell2);
            cell2.classList.add("rTableCell");
            const input2 = document.createElement("input");
            cell2.appendChild(input2);
            input2.setAttribute("type", "text");
            input2.setAttribute("id", writing2Identifier(category));
            input2.setAttribute("name", writing2Identifier(category));
            input2.setAttribute("size", "10");
            input2.addEventListener("change", () => {
                category.setWriter2(input1.value);
                _save();
            });

        }
    }

    function numAwardsIdentifier(category) {
        return "numAwards_" + category.catId;
    }

    function addNumAwardsRow(body) {
        const row = document.createElement("div");
        row.classList.add("rTableRow");
        body.appendChild(row);
        row.setAttribute("id", "num_awards_row");

        const numAwardsCell = document.createElement("div");
        row.appendChild(numAwardsCell);
        numAwardsCell.classList.add("rTableCell");
        numAwardsCell.innerText = "# Awards";

        for (const category of sortedCategories) {
            const cell = document.createElement("div");
            row.appendChild(cell);
            cell.classList.add("rTableCell");
            const input = document.createElement("input");
            cell.appendChild(input);
            input.setAttribute("type", "number");
            input.setAttribute("id", numAwardsIdentifier(category));
            input.setAttribute("name", numAwardsIdentifier(category));
            input.setAttribute("min", "1");
            input.setAttribute("size", "3");
            input.setAttribute("required", "true")
            input.value = category.getNumAwards();
            input.addEventListener("change", () => {
                const prevMaxNumAwards = computeMaxNumAwards();
                const newNumAwards = parseInt(input.value, 10);
                const valid = validateNumAwardsChange(category, newNumAwards);
                if (!valid) {
                    input.value = category.getNumAwards();
                } else {
                    category.setNumAwards(newNumAwards);
                    changeNumAwards(category, prevMaxNumAwards);
                }
            });
        }
    }

    function computeMaxNumAwards() {
        let maxNumAwards = 0;
        for (const category of sortedCategories) {
            maxNumAwards = Math.max(maxNumAwards, category.getNumAwards());
        }
        return maxNumAwards;
    }

    /**
     * Update the number of rows and active cells based on the current value of numAwards.
     * 
     * @param {Category} category the category that just had it's number of awards changed
     * @param {int} prevMaxNumAwards previous maximum number of awards  
     */
    function changeNumAwards(category, prevMaxNumAwards) {
        // ensure a row exists for all awards in this category
        const body = document.getElementById("deliberation_body");
        const curMaxNumAwards = computeMaxNumAwards();
        if (curMaxNumAwards > prevMaxNumAwards) {
            for (let place = prevMaxNumAwards + 1; place <= curMaxNumAwards; ++place) {
                addPlaceRow(body, place);
                for (const c of sortedCategories) {
                    if (c.catId != category.catId) {
                        // no other category is going to have this many awards
                        const cellId = placeCellIdentifier(c, place);
                        const cell = document.getElementById(cellId);
                        cell.classList.add("unavailable");
                    }
                }
            }
        }

        // disable/enable place cells as needed
        const categoryNumAwards = category.getNumAwards();
        for (let place = 1; place <= curMaxNumAwards; ++place) {
            const cellId = placeCellIdentifier(category, place);
            const cell = document.getElementById(cellId);
            if (place <= categoryNumAwards) {
                cell.classList.remove("unavailable");
            } else {
                cell.classList.add("unavailable");
            }
        }


        // remove extra rows
        if (curMaxNumAwards < prevMaxNumAwards) {
            for (let place = prevMaxNumAwards; place > curMaxNumAwards; --place) {
                const rowId = placeRowIdentifier(place);
                const row = document.getElementById(rowId);
                body.removeChild(row);
            }
        }
    }


    /**
     * Check that we won't remove cells with teams currently in them.
     * @param {Category} category the category that is changing
     * @param {int} newNumAwards the new number of awards
     * @returns false if there are issues (will also alert the user), true otherwise 
     */
    function validateNumAwardsChange(category, newNumAwards) {
        const curNumAwards = category.getNumAwards();
        if (newNumAwards < curNumAwards) {
            for (let place = curNumAwards; place >= curNumAwards; --place) {
                const cellId = placeCellIdentifier(category, place);
                const cell = document.getElementById(cellId);
                if (cell.children.length > 0) {
                    alert("Cannot lower the number of awards below the current number of winners. Remove the last winner and try again.");
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @param {Category} category category to find the cell for
     * @param {int} index zero-based nominee index
     */
    function nomineeCellIdentifier(category, index) {
        return "nominee_cell_" + category.catId + "_" + index;
    }

    /**
     * @param {int} index zero-based nominee index
     */
    function nomineeRowIdentifier(index) {
        return "nominee_row_" + index;
    }

    /**
     * Ensure that a row exists for the nominee with the specified index.
     * 
     * @param {int} index zero-based nominee index
     */
    function ensureNomineeRowExists(index) {
        const body = document.getElementById("deliberation_body");

        let prevRow = document.getElementById("writing2_row").nextSibling;
        for (let i = 0; i <= index; ++i) {
            const rowId = nomineeRowIdentifier(i);
            let row = document.getElementById(rowId);
            if (null == row) {
                row = document.createElement("div");
                body.insertBefore(row, prevRow.nextSibling);
                row.setAttribute("id", rowId);
                row.classList.add("rTableRow");
                row.classList.add("nominee_row");

                //  cell for place column
                const placeCell = document.createElement("div");
                row.appendChild(placeCell);
                placeCell.classList.add("rTableCell");

                // add cells for all categories                
                for (const category of sortedCategories) {
                    const cell = document.createElement("div");
                    row.appendChild(cell);
                    cell.classList.add("rTableCell");
                    cell.setAttribute("id", nomineeCellIdentifier(category, i));
                }
            }
            prevRow = row;
        }
    }

    function populateNomineeTeams() {
        teamDivs = new Map();
        teamWinnersCount = new Map();
        teamColors = new Map();

        for (const category of sortedCategories) {
            const nominees = category.getNominees();
            for (const [index, teamNumber] of nominees.entries()) {
                const team = finalist_module.lookupTeam(teamNumber);
                ensureNomineeRowExists(index);

                const cellId = nomineeCellIdentifier(category, index);
                const cell = document.getElementById(cellId);
                const teamDiv = createTeamDiv(team, category);
                cell.appendChild(teamDiv);
                setTeamColorStyle(teamNumber, teamDiv);
            }
        }
    }

    function updatePage() {
        // output header
        updateHeader();

        const currentDivision = finalist_module.getCurrentDivision()
        const body = document.getElementById("deliberation_body");
        removeChildren(body);

        addNumAwardsRow(body);

        const maxPlace = computeMaxNumAwards();
        for (let place = 1; place <= maxPlace; ++place) {
            addPlaceRow(body, place);
        }

        addSeparator(body);
        addWriters(body);

        addSeparator(body);
        populateNomineeTeams();

        addSeparator(body);
        // FIXME add section for teams by judging group
    }

    document.addEventListener("DOMContentLoaded", function() {
        //FIXME will need to determine when to load from finalist data and when to just load from local storage
        finalist_module.loadFromLocalStorage();
        loadFromFinalist();

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

    // always need to initialize variables
    _init_variables();
}