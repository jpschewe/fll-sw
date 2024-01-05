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

    // id -> Category
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
                && _categories.get(category_id); category_id = category_id + 1)
                ;

            if (category_id == Number.MAX_VALUE || category_id + 1 == Number.MAX_VALUE) {
                throw new Error("No free category IDs");
            }

            this.name = name;
            this.catId = category_id;
            this.nominees = new Map(); // award group -> list of team numbers
            this.potentialWinners = new Map() // award group -> list of team numbers
            this.winners = new Map(); // award group -> list of team numbers, null for empty cells
            this.scheduled = scheduled;
            this.writer1 = new Map(); // award group -> string
            this.writer2 = new Map(); // award group -> string

            _categories.set(this.catId, this);
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

        getPotentialWinners() {
            const value = this.potentialWinners.get(finalist_module.getCurrentDivision());
            if (null == value) {
                return [];
            } else {
                return value;
            }
        }
        setPotentialWinners(v) {
            this.potentialWinners.set(finalist_module.getCurrentDivision(), v);
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

    let draggingTeamDiv = null;

    const SECTION_NOMINEES = "nominees";
    const SECTION_POTENTIAL_WINNERS = "potential_winners";
    const SECTION_WINNERS = "winners";

    const DATA_CATEGORY_ID = "data-categoryId";
    const DATA_SECTION = "data-section";
    const DATA_JUDGING_GROUP = "data-judgingGroup";
    const DATA_TEAM_NUMBER = "data-teamNumber";
    const DATA_PLACE = "data-place";

    const TRANSFER_TEAM_NUMBER = 'text/x-fllsw-teamNumber';
    const TRANSFER_CATEGORY_ID = 'text/x-fllsw-categoryId';
    const TRANSFER_SECTION = 'text/x-fllsw-section';

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
    function addToTeamDivs(teamNumber, teamDiv) {
        let divs = teamDivs.get(teamNumber);
        if (null == divs) {
            divs = [];
        }
        divs.push(teamDiv);
        teamDivs.set(teamNumber, divs);
    }

    function removeFromTeamDivs(teamNumber, teamDiv) {
        const divs = teamDivs.get(teamNumber);
        if (null != divs) {
            removeFromArray(divs, teamDiv);
        }
        teamDiv.remove();
    }

    function getTeamDivs(teamNumber) {
        const divs = teamDivs.get(teamNumber);
        if (null == divs) {
            return [];
        } else {
            return divs;
        }
    }

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
        teamWinnersCount.set(teamNumber, count);

        updateTeamDivForWinners(teamNumber);
    }

    function removeTeamFromWinners(teamNumber) {
        let count = getTeamWinnersCount(teamNumber);
        --count;
        count = Math.max(0, count); // just in case
        teamWinnersCount.set(teamNumber, count);

        updateTeamDivForWinners(teamNumber);
    }

    function updateTeamDivForWinners(teamNumber) {
        const count = getTeamWinnersCount(teamNumber);

        const divs = teamDivs.get(teamNumber);
        if (null != divs) {
            for (const div of divs) {
                const cell = div.parentNode;
                if (cell) {
                    const row = cell.parentNode;
                    if (row) {
                        const section = row.getAttribute(DATA_SECTION);
                        if (section == SECTION_POTENTIAL_WINNERS) {
                            if (count > 0) {
                                div.classList.add("winner");
                            } else {
                                div.classList.remove("winner");
                            }
                        }
                    }
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
    }

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
     * @param {string} section which section of the page this div belongs to (SECTION_NOMINEES, SECTION_POTENTIAL_WINNERS, SECTION_WINNERS)
     * @returns an HTML div element for the team
     */
    function createTeamDiv(team, category, section) {
        const group = team.judgingGroup;
        const teamDiv = document.createElement("div");
        teamDiv.setAttribute("draggable", "true");
        teamDiv.innerText = team.num + " - " + team.name + " (" + group + ")";
        teamDiv.setAttribute(DATA_TEAM_NUMBER, team.num);

        teamDiv.addEventListener('dragstart', function(e) {
            teamDiv.classList.add("dragging");

            const dataTransfer = e.dataTransfer;

            draggingTeamDiv = teamDiv;

            // need something to transfer, otherwise the browser won't let us drag
            dataTransfer.setData(TRANSFER_TEAM_NUMBER, team.num);
            dataTransfer.setData(TRANSFER_CATEGORY_ID, category.catId);
            dataTransfer.setData(TRANSFER_SECTION, section);
        });

        teamDiv.addEventListener('dragend', function(_) {
            teamDiv.classList.remove("dragging");
        });

        setTeamColorStyle(team.num, teamDiv);
        addToTeamDivs(team.num, teamDiv);

        return teamDiv;
    }

    /**
     * Given a drag and drop event, find the table cell.
     */
    function dndFindCell(e) {
        let potentialCell = e.target;
        while (potentialCell != null) {
            if (potentialCell.getAttribute && null != potentialCell.getAttribute(DATA_CATEGORY_ID)) {
                return potentialCell;
            }
            potentialCell = potentialCell.parentNode;
        }
        throw new Error("Unable to find table cell in " + e);
    }

    function dragOver(e) {
        // prevent default to allow drop
        e.preventDefault();

        const sourceCategoryId = parseInt(e.dataTransfer.getData(TRANSFER_CATEGORY_ID), 10);
        const sourceSection = e.dataTransfer.getData(TRANSFER_SECTION);

        const destCell = dndFindCell(e);
        const destCategoryId = parseInt(destCell.getAttribute(DATA_CATEGORY_ID), 10);
        const destSection = destCell.parentNode.getAttribute(DATA_SECTION);

        console.log(`DEBUG: dragOver: sourceCategory: ${sourceCategoryId} sourceSection: ${sourceSection} destCategory: ${destCategoryId} destSection: ${destSection}`);

        let dropzoneCell = null;
        if (sourceCategoryId == destCategoryId) {
            if (sourceSection == SECTION_NOMINEES && destSection == SECTION_POTENTIAL_WINNERS) {
                dropzoneCell = destCell;
            } else if (sourceSection == SECTION_POTENTIAL_WINNERS && destSection == SECTION_NOMINEES) {
                dropzoneCell = destCell;
            } else if (sourceSection == SECTION_POTENTIAL_WINNERS && destSection == SECTION_POTENTIAL_WINNERS) {
                dropzoneCell = destCell;
            } else if (sourceSection == SECTION_POTENTIAL_WINNERS && destSection == SECTION_WINNERS) {
                // ensure that we don't make a team a winner twice
                if (!draggingTeamDiv.classList.contains("winner")) {
                    dropzoneCell = destCell;
                }
            } else if (sourceSection == SECTION_WINNERS && destSection == SECTION_WINNERS) {
                dropzoneCell = destCell;
            } else if (sourceSection == SECTION_WINNERS && destSection == SECTION_POTENTIAL_WINNERS) {
                dropzoneCell = destCell;
            }
        }

        // Using dragover rather than dragenter and dragleave due to race conditions
        //   with the order that enter/leave are executed.

        // clear dropzone from all other cells and add to the current cell
        const body = document.getElementById("deliberation_body");
        for (const c of body.querySelectorAll(".dropzone").values()) {
            if (c != destCell) {
                c.classList.remove("dropzone");
            }
        }
        if (null != dropzoneCell) {
            // dropEffect must be one of the allowedEffects set in dragstart, otherwise the drop won't work
            // if allowedEffects isn't set in dragstart, then all effects are allowed
            destCell.classList.add("dropzone");
            // using copy adds a plus icon to the cursor, this should help let the user know they can drop
            e.dataTransfer.dropEffect = "copy";
        } else {
            e.dataTransfer.dropEffect = "none";
        }

    }

    function dragLeave(_) {
        const body = document.getElementById("deliberation_body");
        for (const c of body.querySelectorAll(".dropzone").values()) {
            c.classList.remove("dropzone");
        }
    }

    function drop(e) {
        e.preventDefault();

        const destCell = dndFindCell(e);

        const sourceCategoryId = parseInt(e.dataTransfer.getData(TRANSFER_CATEGORY_ID), 10);
        const sourceSection = e.dataTransfer.getData(TRANSFER_SECTION);
        const teamNum = parseInt(e.dataTransfer.getData(TRANSFER_TEAM_NUMBER), 10);

        const destCategoryId = parseInt(destCell.getAttribute(DATA_CATEGORY_ID), 10);
        const destSection = destCell.parentNode.getAttribute(DATA_SECTION);

        if (sourceCategoryId == destCategoryId) {
            const category = _categories.get(sourceCategoryId);
            const team = finalist_module.lookupTeam(teamNum);

            if (sourceSection == SECTION_NOMINEES) {
                if (destSection == SECTION_POTENTIAL_WINNERS) {
                    dropNomineeToPotentialWinners(destCell, category, team);

                    // disable dragging team up again  
                    draggingTeamDiv.setAttribute("draggable", "false");
                    draggingTeamDiv.classList.add("potential_winner");
                } else {
                    console.log(`No drop - section nominees wrong section dest: ${destSection}`);
                }
            } else if (sourceSection == SECTION_POTENTIAL_WINNERS) {
                if (destSection == SECTION_NOMINEES) {
                    removeFromTeamDivs(teamNum, draggingTeamDiv)

                    // find teamDiv in nominees section and allow drag again
                    for (const teamDiv of getTeamDivs(teamNum)) {
                        const cell = teamDiv.parentNode;
                        if (cell) {
                            const row = cell.parentNode;
                            if (row) {
                                const section = row.getAttribute(DATA_SECTION);
                                if (section == SECTION_NOMINEES) {
                                    teamDiv.setAttribute("draggable", "true");
                                    teamDiv.classList.remove("potential_winner");
                                }
                            }
                        }
                    }
                } else if (destSection == SECTION_POTENTIAL_WINNERS) {
                    dropReorderPotentialWinners(destCell, draggingTeamDiv, category);
                } else if (destSection == SECTION_WINNERS) {
                    if (draggingTeamDiv.classList.contains("winner")) {
                        console.log(`No drop - team is already a winner: ${teamNum}`);
                    } else {
                        dropPotentialWinnerToWinners(destCell, category, team);
                    }
                } else {
                    console.log(`No drop - section potential winners wrong section dest: ${destSection}`);
                }
            } else if (sourceSection == SECTION_WINNERS) {
                if (destSection == SECTION_POTENTIAL_WINNERS) {
                    dropRemoveFromWinners(draggingTeamDiv);
                } else if (destSection == SECTION_WINNERS) {
                    dropReorderWinners(destCell, draggingTeamDiv, category);
                } else {
                    console.log(`No drop - section winners wrong section dest: ${destSection}`);
                }
            }
        } else {
            console.log(`No drop - wrong category source: ${sourceCategoryId} dest: ${destCategoryId}`);
        }

        const body = document.getElementById("deliberation_body");
        for (const c of body.querySelectorAll(".dropzone").values()) {
            c.classList.remove("dropzone");
        }

        draggingTeamDiv = null;
    }

    function dropReorderPotentialWinners(destCell, teamDiv, category) {
        if (destCell == teamDiv.parentNode) {
            // nothing to do
            return;
        }

        if (destCell.children.length > 0) {
            const divToMove = destCell.children[0];

            // move divToMove down one
            const rowPlace = parseInt(destCell.parentNode.getAttribute(DATA_PLACE), 10);
            const nextCell = findOrCreatePotentiallWinnerCell(category, rowPlace + 1);
            dropReorderPotentialWinners(nextCell, divToMove, category);
        }
        destCell.appendChild(teamDiv);
    }

    /**
     * Handle drop for moving a nominee to potential winners.
     * 
     * @param {HTMLElement} destCell the table cell to put the team into
     * @param {Category} category the category
     * @param {finalist_module.Team} team the team being moved
     */
    function dropNomineeToPotentialWinners(destCell, category, team) {
        const teamDiv = createTeamDiv(team, category, SECTION_POTENTIAL_WINNERS);
        dropReorderPotentialWinners(destCell, teamDiv, category);
        // the new div may need a strike through
        updateTeamDivForWinners(team.num);
    }

    function dropRemoveFromWinners(teamDiv) {
        const teamNumberToRemove = parseInt(teamDiv.getAttribute(DATA_TEAM_NUMBER), 10);
        removeFromTeamDivs(teamNumberToRemove, teamDiv);
        removeTeamFromWinners(teamNumberToRemove);
    }

    function dropReorderWinners(destCell, teamDiv, category) {
        if (destCell == teamDiv.parentNode) {
            // nothing to do
            return;
        }

        if (destCell.children.length > 0) {
            const divToMove = destCell.children[0];

            // move divToMove down one
            const rowPlace = parseInt(destCell.parentNode.getAttribute(DATA_PLACE), 10);
            const nextCell = document.getElementById(placeCellIdentifier(category, rowPlace + 1));
            if (null == nextCell || nextCell.classList.contains("unavailable")) {
                dropRemoveFromWinners(divToMove);
            } else {
                dropReorderWinners(nextCell, divToMove, category);
            }
        }
        destCell.appendChild(teamDiv);
    }

    /**
     * Handle drop for moving a potential winner to winners.
     * 
     * @param {HTMLElement} destCell the table cell to put the team into
     * @param {Category} category the category
     * @param {finalist_module.Team} team the team being moved
     */
    function dropPotentialWinnerToWinners(destCell, category, team) {
        const teamDiv = createTeamDiv(team, category, SECTION_WINNERS);
        dropReorderWinners(destCell, teamDiv, category);
        addTeamToWinners(team.num);
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
        row.setAttribute(DATA_SECTION, SECTION_WINNERS);
        row.setAttribute(DATA_PLACE, place);

        const placeCell = document.createElement("div");
        row.appendChild(placeCell);
        placeCell.classList.add("rTableCell");
        placeCell.innerText = place;

        for (const category of sortedCategories) {
            const cell = document.createElement("div");
            row.appendChild(cell);
            cell.classList.add("rTableCell");
            cell.setAttribute("id", placeCellIdentifier(category, place));
            cell.setAttribute(DATA_CATEGORY_ID, category.catId);

            cell.addEventListener('dragover', dragOver);
            cell.addEventListener('dragleave', dragLeave);
            cell.addEventListener('drop', drop);
        }
    }

    /**
     * @returns the row that was added
     */
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

        return row;
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

    function findAllJudgingGroups() {
        const judgingGroups = [];
        for (const team of finalist_module.getAllTeams()) {
            if (!judgingGroups.includes(team.judgingGroup)) {
                judgingGroups.push(team.judgingGroup);
            }
        }
        judgingGroups.sort();
        return judgingGroups;
    }

    function addInitialNomineeRows() {
        let prevRow = document.getElementById("after_potential_winners");

        const judgingGroups = findAllJudgingGroups();
        for (const judgingGroup of judgingGroups) {
            prevRow = addNomineeRow(judgingGroup, prevRow);
        }
    }

    /**
     * @param {string} judgingGroup the judging group
     * @param {HTMLELement} prevRow the row to add after
     * @return the created row {HTMLElement}
     */
    function addNomineeRow(judgingGroup, prevRow) {
        const body = document.getElementById("deliberation_body");
        const row = document.createElement("div");
        row.classList.add("rTableRow");
        body.insertBefore(row, prevRow.nextSibling)
        row.setAttribute(DATA_JUDGING_GROUP, judgingGroup);
        row.setAttribute(DATA_SECTION, SECTION_NOMINEES);
        row.classList.add("nominee_row");

        const placeCell = document.createElement("div");
        row.appendChild(placeCell);
        placeCell.classList.add("rTableCell");
        placeCell.innerText = judgingGroup;

        for (const category of sortedCategories) {
            const cell = document.createElement("div");
            row.appendChild(cell);
            cell.classList.add("rTableCell");
            cell.setAttribute(DATA_CATEGORY_ID, category.catId);

            cell.addEventListener('dragover', dragOver);
            cell.addEventListener('dragleave', dragLeave);
            cell.addEventListener('drop', drop);
        }
        return row;
    }

    /**
     * Find the cell in row that is used for the specified category.
     * @param {HTMLElement} row the row element
     * @param {Category} category the category we are interested in
     * @return the cell or null if no cell is found
     */
    function findCategoryCell(row, category) {
        for (const cell of row.children) {
            const catId = cell.getAttribute(DATA_CATEGORY_ID);
            if (catId == category.catId) {
                return cell;
            }
        }
        console.log("Could not find cell for category " + category.catId + " in row: " + row);
        return null;
    }

    function findOrCreateEmptyNomineeCell(judgingGroup, category) {
        const body = document.getElementById("deliberation_body");
        let prevJsRow = null;
        for (const row of body.children) {
            const js = row.getAttribute(DATA_JUDGING_GROUP);
            if (js == judgingGroup) {
                const cell = findCategoryCell(row, category);
                if (cell.children.length == 0) {
                    return cell;
                }
                prevJsRow = row;
            } else if (prevJsRow != null) {
                // moved to the next judging group and didn't find a cell, need to add a new row
                const newRow = addNomineeRow(judgingGroup, prevJsRow);
                const cell = findCategoryCell(newRow, category);
                return cell;
            }
        }
        // handle last judging group
        if (prevJsRow != null) {
            // moved to the next judging group and didn't find a cell, need to add a new row
            const newRow = addNomineeRow(judgingGroup, prevJsRow);
            const cell = findCategoryCell(newRow, category);
            return cell;
        }
        throw new Error("Unable to find place to add row");
    }

    function addNominee(category, team) {
        const judgingGroup = team.judgingGroup;
        const cell = findOrCreateEmptyNomineeCell(judgingGroup, category);
        const teamDiv = createTeamDiv(team, category, SECTION_NOMINEES);
        cell.appendChild(teamDiv);
        cell.setAttribute(DATA_CATEGORY_ID, category.catId);
        cell.classList.add("nominee");
    }

    /** 
     * @param {Category} category the category that we are interested in
     * @param {int} place the place that we are inerested in 
     * @returns potential winner cell for the specified category and place
    */
    function findOrCreatePotentiallWinnerCell(category, place) {
        const row = ensurePotentialWinnersRowExists(place);
        for (const cell of row.children) {
            const categoryId = parseInt(cell.getAttribute(DATA_CATEGORY_ID), 10);
            if (categoryId == category.catId) {
                return cell;
            }
        }
        throw new Error(`Internal error, unable to find cell for ${category.catId}`);
    }

    function ensurePotentialWinnersRowExists(place) {
        place = parseInt(place, 10);

        let prevRow = null;
        if (place > 1) {
            // make sure the rows are created in order
            prevRow = ensurePotentialWinnersRowExists(place - 1);
        } else {
            prevRow = document.getElementById("after_writers");
        }

        const body = document.getElementById("deliberation_body");
        for (const row of body.children) {
            const rowSection = row.getAttribute(DATA_SECTION);
            if (rowSection == SECTION_POTENTIAL_WINNERS) {
                const rowPlace = row.getAttribute(DATA_PLACE);
                if (null != rowPlace && place == parseInt(rowPlace, 10)) {
                    return row;
                }
            }
        }
        return addPotentialWinnersRow(place, prevRow);
    }

    /**
     * @param {int} place place for the potential winners row
     * @param {HTMLElement} prevRow the row to add after
     * @returns the row {HTMLElement} 
     */
    function addPotentialWinnersRow(place, prevRow) {
        const body = document.getElementById("deliberation_body");
        const row = document.createElement("div");
        row.classList.add("rTableRow");
        row.classList.add("potential_winner_row");
        row.setAttribute(DATA_SECTION, SECTION_POTENTIAL_WINNERS);
        row.setAttribute(DATA_PLACE, place);
        body.insertBefore(row, prevRow.nextSibling)

        const placeCell = document.createElement("div");
        row.appendChild(placeCell);
        placeCell.classList.add("rTableCell");
        placeCell.innerText = place;

        for (const category of sortedCategories) {
            const cell = document.createElement("div");
            row.appendChild(cell);
            cell.classList.add("rTableCell");
            cell.setAttribute(DATA_CATEGORY_ID, category.catId);

            cell.addEventListener('dragover', dragOver);
            cell.addEventListener('dragleave', dragLeave);
            cell.addEventListener('drop', drop);
        }
        return row;
    }

    function populatePotentialWinners() {
        ensurePotentialWinnersRowExists(1);
    }

    /**
     * Group teams by judging group.
     */
    function populateNomineeTeams() {
        addInitialNomineeRows();

        for (const category of sortedCategories) {
            const nominees = category.getNominees();
            for (const teamNumber of nominees) {
                const team = finalist_module.lookupTeam(teamNumber);
                addNominee(category, team);
            }
        }
    }

    function updatePage() {
        teamDivs = new Map();
        teamWinnersCount = new Map();
        teamColors = new Map();


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

        const afterWinners = addSeparator(body);
        afterWinners.setAttribute("id", "after_winners");

        addWriters(body);

        const afterWriters = addSeparator(body);
        afterWriters.setAttribute("id", "after_writers");

        populatePotentialWinners();

        const afterPotentialWinners = addSeparator(body);
        afterPotentialWinners.setAttribute("id", "after_potential_winners");

        populateNomineeTeams();
        // FIXME add section for teams by judging group

        // FIXME add "Add Team" buttons to each category at the bottom

        // FIXME populate winners in table
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