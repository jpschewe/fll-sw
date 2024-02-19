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
    // awardGroup -> [ [teamNumber, ...], ...]
    let _rankedPerformanceTeams;

    // [categoryTitle, categoryTitle, ...]
    let _awardOrder;

    function _init_variables() {
        _categories = new Map();
        _rankedPerformanceTeams = new Map();
        _awardOrder = []
    }

    function _save() {
        fllStorage.set(STORAGE_PREFIX, "_categories", _categories);
        fllStorage.set(STORAGE_PREFIX, "_rankedPerformanceTeams", _rankedPerformanceTeams);
        fllStorage.set(STORAGE_PREFIX, "_awardOrder", _awardOrder);
    }

    function _load() {
        _init_variables();

        let value = fllStorage.get(STORAGE_PREFIX, "_categories");
        if (null != value) {
            for (const [_, obj] of value.entries()) {
                Category.deserialize(obj);
            }
        }

        value = fllStorage.get(STORAGE_PREFIX, "_rankedPerformanceTeams");
        if (null != value) {
            _rankedPerformanceTeams = value;
        }

        value = fllStorage.get(STORAGE_PREFIX, "_awardOrder");
        if (null != value) {
            _awardOrder = value;
        }

    }

    function _clear_local_storage() {
        fllStorage.clearNamespace(STORAGE_PREFIX);
    }


    class Category {
        /**
         * Constructor for a category.
         * 
         * @param {String} name
         *          the name of the category
         * @param {boolean} scheduled
         *          boolean stating if this is a scheduled category
         * @param {boolean} overall
         *          true if a category that is awarded for the tournament rather than
         *          an award group
         * @param {boolean} numeric if true, the rank is based on scores and is a subjective category in the challenge description
         */
        constructor(name, scheduled, overall, numeric, catId) {
            this.name = name;
            this.overall = overall;
            this.numeric = numeric;
            this.catId = catId;
            this.nominees = new Map(); // award group -> list of team numbers
            this.potentialWinners = new Map() // award group -> list of team numbers
            this.winners = new Map(); // award group -> list of team numbers, null for empty cells
            this.scheduled = scheduled;
            this.writer1 = new Map(); // award group -> string
            this.writer2 = new Map(); // award group -> string
            this.serverWinningTeams = new Map(); // award group -> list of team numbers
        }

        /**
         * Create a category. Finds the first free ID and assigns it to this
         * new category. Updates _categories. 
         * 
         * Arguments are passed to the constructor.
         */
        static create(name, scheduled, overall, numeric) {
            let category_id;
            // find the next available id
            for (category_id = 0; category_id < Number.MAX_VALUE
                && _categories.get(category_id); category_id = category_id + 1)
                ;

            if (category_id == Number.MAX_VALUE || category_id + 1 == Number.MAX_VALUE) {
                throw new Error("No free category IDs");
            }

            const category = new Category(name, scheduled, overall, numeric, category_id);
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
            const category = new Category(obj.name, obj.scheduled, obj.overall, obj.numeric, obj.catId);
            category.nominees = obj.nominees;
            category.potentialWinners = obj.potentialWinners;
            category.winners = obj.winners;
            category.writer1 = obj.writer1;
            category.writer2 = obj.writer2;
            category.serverWinningTeams = obj.serverWinningTeams;

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
            // ensure the team isn't in 2 places
            this.removePotentialWinner(teamNumber);

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
            if (null == teamNum) {
                return;
            }

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
         * @param grow if true, then allow the number of winners to grow
         * @throws Error If the place is outside the bounds of the number of awards
         */
        setWinner(place, teamNumber, grow) {
            // ensure the team isn't in 2 places
            this.removeWinner(teamNumber);

            const winners = this.getWinners();
            if (place < 1) {
                throw new Error("Place must be greater than 0");
            }
            if (grow) {
                while (winners.length < place) {
                    winners.push(null);
                }
            } else {
                if (place > winners.length) {
                    throw new Error("Place must less than or equal to the number of awards");
                }
            }

            const placeIndex = place - 1;
            winners[placeIndex] = teamNumber;
            this.setWinners(winners);
        }
        /**
         * Remove all instances of teamNum from winners.
         */
        removeWinner(teamNum) {
            if (null == teamNum) {
                return;
            }

            const winners = this.getWinners();
            let idx = winners.indexOf(teamNum);
            while (idx != -1) {
                winners[idx] = null;
                idx = winners.indexOf(teamNum);
            }
            this.setWinners(winners);
        }


        /**
         * Teams that were listed as winners in this category when data was last loaded from the server.
         */
        getServerWinningTeams() {
            const value = this.serverWinningTeams.get(finalist_module.getCurrentDivision());
            if (null == value) {
                return [null];
            } else {
                return value;
            }
        }
        setServerWinningTeams(v) {
            this.serverWinningTeams.set(finalist_module.getCurrentDivision(), v);
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
    deliberationModule.findOrCreateCategory = function(name, scheduled, overall, numeric) {
        let category = deliberationModule.getCategoryByName(name);
        if (null == category) {
            category = Category.create(name, scheduled, overall, numeric);
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
            const dCategory = deliberationModule.findOrCreateCategory(category.name, category.scheduled, category.overall, category.numeric);

            if (category.scheduled) {
                // only load the nominees for scheduled categories
                for (const teamNumber of category.teams) {
                    dCategory.addNominee(teamNumber);
                }
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

    function loadAwardOrder() {
        return fetch("../../api/AwardsScript/AwardOrder").then(checkJsonResponse).then((result) => {
            _awardOrder = result;
        });
    }

    function loadNumPerformanceAwards() {
        return fetch("../../api/AwardsScript/NumPerformanceAwards").then(checkJsonResponse).then((result) => {
            const category = deliberationModule.getCategoryByName(deliberationModule.PERFORMANCE_CATEGORY_NAME);
            category.setNumAwards(parseInt(result, 10));
        });
    }

    function loadTopPerformanceScores() {
        return fetch("../../api/TopPerformanceScores").then(checkJsonResponse).then((result) => {
            _rankedPerformanceTeams = new Map();

            for (const [awardGroup, entries] of Object.entries(result)) {
                let index = -1; // start at -1 to simplify increment logic in loop
                let prevPlace = null;
                let rankedPerformanceTeams = [];
                for (const [_, scoreEntry] of Object.entries(entries)) {
                    const place = parseInt(scoreEntry.rank, 10);

                    if (prevPlace != place) {
                        rankedPerformanceTeams.push([]);
                        ++index;
                    }

                    const teamNumbers = rankedPerformanceTeams[index];
                    teamNumbers.push(parseInt(scoreEntry.teamNumber, 10));
                    rankedPerformanceTeams[index] = teamNumbers;

                    prevPlace = parseInt(scoreEntry.rank, 10);
                }
                _rankedPerformanceTeams.set(awardGroup, rankedPerformanceTeams);
            }
        });
    }

    /**
     * Common processing of winners across non-numeric, non-numeric overall and subjective.
     * For this code they can all be handled the same way.
     */
    function processAwardWinners(result) {
        // data comes in sorted by category, then place

        // group the winners per category and only capture those for the current award group
        // note that winner.awardGroup won't be set for overall winners, they are considered part of any award group

        // category name -> [winner, winner]
        // filtered down to teams in the current award group
        const perCategory = new Map();

        // category name -> [winner.teamNumber, winner.teamNumber ]
        const perCategoryAllWinningTeams = new Map();

        for (const winner of result) {
            const categoryName = winner.name;
            const teamNumber = parseInt(winner.teamNumber, 10);

            let winningTeams = perCategoryAllWinningTeams.get(categoryName);
            if (null == winningTeams) {
                winningTeams = [];
            }
            winningTeams.push(teamNumber);
            perCategoryAllWinningTeams.set(categoryName, winningTeams);

            const team = finalist_module.lookupTeam(teamNumber);
            if (!team) {
                alert(`Cannot find team with number ${teamNumber}`);
                continue;
            }

            if (team.awardGroup == finalist_module.getCurrentDivision()) {
                let winners = perCategory.get(categoryName);
                if (null == winners) {
                    winners = [];
                }
                winners.push(winner);
                perCategory.set(categoryName, winners);
            }
        }

        // break down the information into what is needed and restructure to handle 1 winner per place and nulls to skip places
        for (const [categoryName, winners] of perCategory) {
            const category = deliberationModule.getCategoryByName(categoryName);
            if (null == category) {
                alert(`Unable to find category with name "${categoryName}"`);
                continue;
            }

            const winningTeams = perCategoryAllWinningTeams.get(categoryName);
            category.setServerWinningTeams(winningTeams);

            for (const winner of winners) {
                const winnerPlace = parseInt(winner.place, 10);
                const teamNumber = parseInt(winner.teamNumber, 10);
                category.setWinner(winnerPlace, teamNumber, true);

                // make sure they are a nominee
                category.addNominee(teamNumber);

                // make sure they are a potential winner
                const potentialWinners = category.getPotentialWinners();
                if (!potentialWinners.includes(teamNumber)) {
                    potentialWinners.push(teamNumber);
                    category.setPotentialWinners(potentialWinners);
                }
            }
        }
    }

    function loadNonNumericAwardWinners() {
        return fetch("../../api/AwardsScript/NonNumericAwardWinners").then(checkJsonResponse).then((result) => {
            processAwardWinners(result);
        });
    }

    function loadNonNumericOverallAwardWinners() {
        return fetch("../../api/AwardsScript/NonNumericOverallAwardWinners").then(checkJsonResponse).then((result) => {
            processAwardWinners(result);
        });
    }

    /**
     * @param {Array} waitList list to add the promises to for AJAX calls 
     */
    function uploadAwardGroupWinners(waitList) {
        for (const [_, category] of _categories) {
            if (category.name == deliberationModule.PERFORMANCE_CATEGORY_NAME) {
                // these are only read from the server, never written
                continue;
            }

            let awardUrl = null;
            let awardGroup = null;
            if (category.name == finalist_module.CHAMPIONSHIP_NAME) {
                // Championship is special, it's kind of numeric and kind of not.
                // It's stored in the database with the non-numeric winners.
                awardUrl = "NonNumericAwardWinners";
                awardGroup = finalist_module.getCurrentDivision();
            } else if (category.overall) {
                awardUrl = "NonNumericOverallAwardWinners";
                awardGroup = null;
            } else if (category.numeric) {
                awardUrl = "SubjectiveAwardWinners";
                awardGroup = finalist_module.getCurrentDivision();
            } else {
                awardUrl = "NonNumericAwardWinners";
                awardGroup = finalist_module.getCurrentDivision();
            }

            const winnersTeamNumbers = [];
            const winners = category.getWinners();
            for (const [index, winnerTeamNumber] of winners.entries()) {
                if (null == winnerTeamNumber) {
                    // skip over blank winners
                    continue;
                }

                winnersTeamNumbers.push(winnerTeamNumber);

                const putData = new Object();
                putData.place = index + 1;
                putData.awardGroup = awardGroup;
                putData.descriptionSpecified = false;
                const promise = uploadJsonData(`../../api/AwardsScript/${awardUrl}/${category.name}/${winnerTeamNumber}`, "PUT", putData).then(checkJsonResponse);
                waitList.push(promise);
            }

            const serverWinners = category.getServerWinningTeams();
            for (const serverTeamNumber of serverWinners) {
                if (null != serverTeamNumber) {
                    const idx = winnersTeamNumbers.indexOf(serverTeamNumber);
                    if (-1 == idx) {
                        const promise = fetch(`../../api/AwardsScript/${awardUrl}/${category.name}/${serverTeamNumber}`, {
                            method: "DELETE",
                        }).then(checkJsonResponse);
                        waitList.push(promise);
                    }
                }
            }
        }
    }

    /**
      * Upload data to the server.
      * 
      * @param doneCallback
      *          called with no arguments on success
      * @param failCallback
      *          called with message on failure
      */
    deliberationModule.uploadData = function(doneCallback, failCallback) {
        const waitList = [];

        uploadAwardGroupWinners(waitList);

        waitList.push(uploadNumAwards());
        waitList.push(uploadWriters());
        waitList.push(uploadPotentialWinners());

        Promise.all(waitList)
            .catch((error) => {
                failCallback(`Error uploading data: ${error}`);
            })
            .then((_) => {
                doneCallback();
            });
    };

    function loadSubjectiveAwardWinners() {
        return fetch("../../api/AwardsScript/SubjectiveAwardWinners").then(checkJsonResponse).then((result) => {
            processAwardWinners(result);
        });
    }

    function loadNumAwards() {
        return fetch("../../api/deliberation/NumAwards").then(checkJsonResponse).then((result) => {
            for (const numAward of result) {
                if (numAward.awardGroup == finalist_module.getCurrentDivision()) {
                    const category = deliberationModule.getCategoryByName(numAward.categoryName);
                    if (null == category) {
                        alert(`Unable to find category with name "${numAward.categoryName}"`);
                        continue;
                    }
                    const num = parseInt(numAward.numAwards, 10);
                    category.setNumAwards(num);
                }
            }
        });
    }

    function uploadNumAwards() {
        const data = [];
        for (const category of deliberationModule.getAllCategories()) {
            const categoryData = new Object();
            categoryData.awardGroup = finalist_module.getCurrentDivision();
            categoryData.categoryName = category.name;
            categoryData.numAwards = category.getNumAwards();
            data.push(categoryData);
        }
        return uploadJsonData(`../../api/deliberation/NumAwards`, "POST", data).then(checkJsonResponse);
    }

    function loadWriters() {
        return fetch("../../api/deliberation/Writers").then(checkJsonResponse).then((result) => {
            for (const writer of result) {
                if (writer.awardGroup == finalist_module.getCurrentDivision()) {
                    const category = deliberationModule.getCategoryByName(writer.categoryName);
                    if (null == category) {
                        alert(`Unable to find category with name "${writer.categoryName}"`);
                        continue;
                    }
                    const writerNum = parseInt(writer.number, 10);
                    if (writerNum == 1) {
                        category.setWriter1(writer.name)
                    } else if (writerNum == 2) {
                        category.setWriter2(writer.name)
                    } else {
                        alert(`Writers 1 and 2 are supported, but found ${writerNum}`);
                    }
                }
            }
        });
    }

    function uploadWriters() {
        const data = [];
        for (const category of deliberationModule.getAllCategories()) {
            const writer1Name = category.getWriter1();
            if (writer1Name) {
                const writer1 = new Object();
                writer1.awardGroup = finalist_module.getCurrentDivision();
                writer1.categoryName = category.name;
                writer1.number = 1;
                writer1.name = writer1Name;
                data.push(writer1);
            }

            const writer2Name = category.getWriter2();
            if (writer2Name) {
                const writer2 = new Object();
                writer2.awardGroup = finalist_module.getCurrentDivision();
                writer2.categoryName = category.name;
                writer2.number = 2;
                writer2.name = writer2Name;
                data.push(writer2);
            }
        }
        return uploadJsonData(`../../api/deliberation/Writers`, "POST", data).then(checkJsonResponse);
    }

    function loadPotentialWinners() {
        return fetch("../../api/deliberation/PotentialWinners").then(checkJsonResponse).then((result) => {
            for (const pWinner of result) {
                if (pWinner.awardGroup == finalist_module.getCurrentDivision()) {
                    const category = deliberationModule.getCategoryByName(pWinner.categoryName);
                    if (null == category) {
                        alert(`Unable to find category with name "${pWinner.categoryName}"`);
                        continue;
                    }
                    const place = parseInt(pWinner.place, 10);
                    const teamNumber = parseInt(pWinner.teamNumber, 10);
                    category.setPotentialWinner(place, teamNumber);
                }
            }
        });
    }

    function uploadPotentialWinners() {
        const data = [];
        for (const category of deliberationModule.getAllCategories()) {
            for (const [index, teamNumber] of category.getPotentialWinners().entries()) {
                if (teamNumber != null) {
                    const potentialWinner = new Object();
                    potentialWinner.awardGroup = finalist_module.getCurrentDivision();
                    potentialWinner.categoryName = category.name;
                    potentialWinner.place = index + 1;
                    potentialWinner.teamNumber = teamNumber;
                    data.push(potentialWinner);
                }
            }
        }
        return uploadJsonData(`../../api/deliberation/PotentialWinners`, "POST", data).then(checkJsonResponse);
    }

    function loadDeliberationData(doneCallback, failCallback) {
        loadDeliberationData1(doneCallback, failCallback);
    }

    function loadDeliberationData1(doneCallback, failCallback) {
        const waitList = [];

        const numAwards = loadNumAwards();
        numAwards.catch((error) => {
            failCallback("Category num awards failed to load: " + error);
        });
        waitList.push(numAwards);

        const writers = loadWriters();
        writers.catch((error) => {
            failCallback("Category writers failed to load: " + error);
        });
        waitList.push(writers);
        
        Promise.all(waitList).then((_) => {
            loadDeliberationData2(doneCallback, failCallback);
        });
    }

    function loadDeliberationData2(doneCallback, failCallback) {
        const waitList = [];

        const potentialWinners = loadPotentialWinners();
        potentialWinners.catch((error) => {
            failCallback("Potential winners failed to load: " + error);
        });
        waitList.push(potentialWinners);

        Promise.all(waitList).then((_) => {
            loadDeliberationData3(doneCallback, failCallback);
        });
    }

    function loadDeliberationData3(doneCallback, failCallback) {
        const waitList = [];

        const nonNumericAwardWinners = loadNonNumericAwardWinners();
        nonNumericAwardWinners.catch((error) => {
            failCallback("Non-numeric award winners failed to load: " + error);
        })
        waitList.push(nonNumericAwardWinners);

        const nonNumericOverallAwardWinners = loadNonNumericOverallAwardWinners();
        nonNumericOverallAwardWinners.catch((error) => {
            failCallback("Non-numeric overall award winners failed to load: " + error);
        })
        waitList.push(nonNumericOverallAwardWinners);

        const subjectiveAwardWinners = loadSubjectiveAwardWinners();
        subjectiveAwardWinners.catch((error) => {
            failCallback("Subjective award winners failed to load: " + error);
        })
        waitList.push(subjectiveAwardWinners);

        Promise.all(waitList).then((_) => {
            deliberationModule.saveToLocalStorage();
            doneCallback();
        });
    }

    function postFinalistLoad(doneCallback, failCallback) {
        createCategories();

        const waitList = [];

        const topPerformanceScoresPromise = loadTopPerformanceScores();
        topPerformanceScoresPromise.catch((error) => {
            failCallback("Top performance scores failed to load: " + error);
        })
        waitList.push(topPerformanceScoresPromise);

        const awardOrderPromise = loadAwardOrder();
        awardOrderPromise.catch((error) => {
            failCallback("Award order failed to load: " + error);
        })
        waitList.push(awardOrderPromise);

        const numPerformanceAwards = loadNumPerformanceAwards();
        numPerformanceAwards.catch((error) => {
            failCallback("Number of performance awards failed to load: " + error);
        })
        waitList.push(numPerformanceAwards);

        Promise.all(waitList).then((_) => {
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

        postFinalistLoad(() => {
            loadDeliberationData(doneCallback, failCallback)
        }, failCallback);
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

    /**
     * @return array of arrays of teams in each place in the performance category. Includes an array at each index to handle ties.
     */
    deliberationModule.getRankedPerformanceTeams = function() {
        const value = _rankedPerformanceTeams.get(finalist_module.getCurrentDivision());
        if (null == value) {
            return [];
        } else {
            return value;
        }
    };

    /**
     * @returns list of category titles in the order that they will be presented
     */
    deliberationModule.getAwardOrder = function() {
        return _awardOrder;
    };

    // always need to initialize variables
    _init_variables();
}