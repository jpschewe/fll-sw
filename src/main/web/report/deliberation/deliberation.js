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
            for (const [_, obj] of value.entries()) {
                Category.deserialize(obj);
            }
        }
    }

    function _clear_local_storage() {
        fllStorage.clearNamespace(STORAGE_PREFIX);
    }


    class Category {
        /**
         * Constructor for a category.
         * 
         * @param name
         *          the name of the category
         * @param scheduled
         *          boolean stating if this is a scheduled category
         */
        constructor(name, scheduled, catId) {
            this.name = name;
            this.catId = catId;
            this.nominees = new Map(); // award group -> list of team numbers
            this.potentialWinners = new Map() // award group -> list of team numbers
            this.winners = new Map(); // award group -> list of team numbers, null for empty cells
            this.scheduled = scheduled;
            this.writer1 = new Map(); // award group -> string
            this.writer2 = new Map(); // award group -> string
        }

        /**
         * Create a category. Finds the first free ID and assigns it to this
         * new category. Updates _categories. 
         * 
         * Arguments are passed to the constructor.
         */
        static create(name, scheduled) {
            let category_id;
            // find the next available id
            for (category_id = 0; category_id < Number.MAX_VALUE
                && _categories.get(category_id); category_id = category_id + 1)
                ;

            if (category_id == Number.MAX_VALUE || category_id + 1 == Number.MAX_VALUE) {
                throw new Error("No free category IDs");
            }

            const category = new Category(name, scheduled, category_id);
            _categories.set(category.catId, category);
            return category;
        }

        /**
         * Convert an object that was deserialized into a Category object.
         * Updates _categories.
         * 
         * @param {Object} obj an Category object that was converted to JSON and back using fll-storage
         */
        static deserialize(obj) {
            const category = new Category(obj.name, obj.scheduled, obj.catId);
            category.nominees = obj.nominees;
            category.potentialWinners = obj.potentialWinners;
            category.winners = obj.winners;
            category.writer1 = obj.writer1;
            category.writer2 = obj.writer2;

            _categories.set(category.catId, category);
            return category;
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
        /**
         * Set the potential winner at the specified place. The array is automatically resized to fit.
         * 
         * @param place the place (1-based) of the team
         * @param teamNumber the team number in this place
         */
        setPotentialWinner(place, teamNumber) {
            const potentialWinners = this.getPotentialWinners();
            const placeIndex = place - 1;

            // grow as needed
            while (potentialWinners.length <= placeIndex) {
                potentialWinners.push(null);
            }

            potentialWinners[placeIndex] = teamNumber;
            this.setPotentialWinners(potentialWinners);
        }
        /**
         * Remove all instances of teamNum from winners.
         */
        removePotentialWinner(teamNum) {
            const potentialWinners = this.getPotentialWinners();
            let idx = potentialWinners.indexOf(teamNum);
            while (idx != -1) {
                potentialWinners[idx] = null;
                idx = potentialWinners.indexOf(teamNum);
            }

            // clean up extra rows in the array
            while (potentialWinners.length > 0 && potentialWinners[potentialWinners.length - 1] == null) {
                potentialWinners.pop();
            }
            this.setPotentialWinners(potentialWinners);
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
         * Set the specified team as a winner with the specified place. 
         * @param place the place (1-based) of the team
         * @param teamNumber the team number in this place
         * @throws Error If the place is outside the bounds of the number of awards
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
            const winners = this.getWinners();
            if (place < 1) {
                throw new Error("Place must be greater than 0");
            }
            if (place > winners.length) {
                throw new Error("Place must less than or equal to the number of awards");
            }

            const placeIndex = place - 1;
            winners[placeIndex] = null;
        }
        /**
         * Remove all instances of teamNum from winners.
         */
        removeWinner(teamNum) {
            const winners = this.getWinners();
            let idx = winners.indexOf(teamNum);
            while (idx != -1) {
                winners[idx] = null;
                idx = winners.indexOf(teamNum);
            }
            this.setWinners(winners);
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

    /**
     * Name of the performance category. Used to display in the table header and to reference teams. Should not need to match the category name in the challenge description.
     */
    deliberationModule.PERFORMANCE_CATEGORY_NAME = "Performance";

    /**
      * Get a category by ID.
      * 
      * @param toFind
      *          the ID to find
      * @returns the category or null
      */
    deliberationModule.getCategoryById = function(toFind) {
        return _categories.get(toFind);
    };
    /**
      * Get a category by name
      * 
      * @param toFind
      *          the name to find
      * @returns the category or null
      */
    deliberationModule.getCategoryByName = function(toFind) {
        let category = null;
        for (const [_, val] of _categories) {
            if (val.name == toFind) {
                category = val;
            }
        }
        return category;
    };

    /**
     * Find a category by name or create it if needed.
     * 
     * @param {string} name name of the category
     * @param {boolean} scheduled if this is a category scheduled in finalist scheduling
     * @returns {Category} object 
     */
    deliberationModule.findOrCreateCategory = function(name, scheduled) {
        let category = deliberationModule.getCategoryByName(name);
        if (null == category) {
            category = Category.create(name, scheduled);
        }
        return category;
    };

    /**
     * @returns {Array} of all {Category} objects
     */
    deliberationModule.getAllCategories = function() {
        return Array.from(_categories.values());
    };

    /**
     * Create the {Category} objects used for deliberations from the finalist data.
     * The teams from the finalist category become nominees in the deliberation category.
     * Any nominees stored are replaced with those from the finalist data. Any stored potential 
     * winners and winners are added as nominees as well.
     */
    function createCategories() {
        for (const category of finalist_module.getAllCategories()) {
            const dCategory = deliberationModule.findOrCreateCategory(category.name, category.scheduled);

            for (const teamNumber of category.teams) {
                dCategory.addNominee(teamNumber);
            }
            for (const [index, teamNumber] of dCategory.getPotentialWinners().entries()) {
                const place = index + 1;
                dCategory.setPotentialWinner(place, teamNumber);
            }
            for (const [index, teamNumber] of dCategory.getWinners().entries()) {
                const place = index + 1;
                dCategory.setWinner(place, teamNumber);
            }
        }
        let performanceCategory = deliberationModule.getCategoryByName(deliberationModule.PERFORMANCE_CATEGORY_NAME);
        if (null == performanceCategory) {
            performanceCategory = Category.create(deliberationModule.PERFORMANCE_CATEGORY_NAME, true);
            //FIXME need to load performance winners from somewhere
        }
    }

    /**
     * Load data from local storage.
     */
    deliberationModule.loadFromLocalStorage = function() {
        finalist_module.loadFromLocalStorage();
        _load();
    };

    /**
     * Save data to local storage.
     */
    deliberationModule.saveToLocalStorage = function() {
        finalist_module.saveToLocalStorage();
        _save();
    };

    function loadTopPerformanceScores() {
        return fetch("../../api/TopPerformanceScores").then(checkJsonResponse).then((result) => {
            console.log(result);
        });
    }

    function postFinalistLoad(doneCallback, failCallback) {
        const waitList = [];

        const topPerformanceScoresPromise = loadTopPerformanceScores();
        topPerformanceScoresPromise.catch((error) => {
            failCallback("Top performance scores failed to load: " + error);
        })
        waitList.push(topPerformanceScoresPromise);

        Promise.all(waitList).then((_) => {
            createCategories();
            deliberationModule.saveToLocalStorage();
            doneCallback();
        });
    }

    /**
     * Clear all local data and reload from the server.
     * Local storage is saved when this method completes with success.
     * 
     * @param doneCallback
     *          called with no arguments on success
     * @param failCallback
     *          called with message on failure
     */
    deliberationModule.clearAndLoad = function(doneCallback, failCallback) {
        _clear_local_storage();

        //FIXME will need to load more data from the database, perhaps change method called...
        postFinalistLoad(doneCallback, failCallback);
    };

    /**
     * Refresh category and score data from the server.
     * Local storage is saved when this method completes with success.
     * 
     * @param doneCallback
     *          called with no arguments on success
     * @param failCallback
     *          called with message on failure
     */
    deliberationModule.refreshData = function(doneCallback, failCallback) {
        postFinalistLoad(doneCallback, failCallback);
    };

    /**
     * Get all teams in the current award group.
     *
     * @return Array of teams
     */
    deliberationModule.getAllTeams = function() {
        const teams = [];
        for (const team of finalist_module.getAllTeams()) {
            if (team.awardGroup == finalist_module.getCurrentDivision()) {
                teams.push(team);
            }
        }
        return teams;
    };

    // always need to initialize variables
    _init_variables();
}