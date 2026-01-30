/*
 * Copyright (c) 2012 High Tech Kids.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

const finalistNumericModule = {};

{
    function handleDivisionChange() {
        const divIndex = document.getElementById("divisions").value;
        const div = finalist_module.getDivisionByIndex(divIndex);
        finalist_module.setCurrentDivision(div);
        updatePage();
        finalist_module.saveToLocalStorage();
    }

    function getNumFinalistsId(team) {
        return "num_finalists_" + team.num;
    }

    /**
     * @param teams Array of teams
     */
    function initializeFinalistCounts(teams) {
        for (const team of teams) {
            // initialize to 0
            let numFinalists = 0;
            for (const category of finalist_module.getAllScheduledCategories()) {
                if (finalist_module.isTeamInCategory(category, team.num)) {
                    numFinalists = numFinalists + 1;
                }
            }
            const elementId = getNumFinalistsId(team);
            const finalistCountElement = document.getElementById(elementId);
            if (finalistCountElement) {
                // easy way to skip teams that aren't displayed
                finalistCountElement.innerText = numFinalists;
            }
        }
    }

    function createTeamTable(teams, currentDivision, currentCategory) {
        const prevScore = new Map();
        const topTeams = new Map();
        const prevRow = new Map();

        const dataElement = document.getElementById("data");
        for (const team of teams) {
            if (currentCategory.overall
                || team.awardGroup == currentDivision) {
                const row = document.createElement("tr");
                dataElement.appendChild(row);

                const score = finalist_module.getCategoryScore(team, currentCategory);
                const group = team.judgingGroup;
                //  || (group == prevJudgingGroup && Math.abs(prevScore - score) < 1)
                if (!topTeams.has(group)) {
                    topTeams.set(group, true);
                    row.classList.add("top-score");
                } else {
                    // same judging group
                    if (topTeams.get(group) && Math.abs(prevScore.get(group) - score) < 1) {
                        // close to top score
                        row.classList.add("top-score");
                    } else if (Math.abs(prevScore.get(group) - score) < 1) {
                        // close score
                        row.classList.add("tie-score");
                        if (prevRow.get(group)) {
                            // need to mark previous row as well
                            prevRow.get(group).classList.add("tie-score");
                        }
                    } else {
                        // don't consider other close scores as ties'
                        topTeams.set(group, false);
                    }
                }
                prevScore.set(group, score);
                prevRow.set(group, row);

                const finalistCol = document.createElement("td");
                row.appendChild(finalistCol);

                const finalistCheck = document.createElement("input");
                finalistCol.appendChild(finalistCheck);
                finalistCheck.setAttribute("type", "checkbox");
                finalistCheck.addEventListener("change", function() {
                    const finalistDisplay = document.getElementById(getNumFinalistsId(team));
                    let numFinalists = parseInt(finalistDisplay.innerText, 10);
                    if (finalistCheck.checked) {
                        finalist_module.addTeamToCategory(currentCategory, team.num);
                        numFinalists = numFinalists + 1;
                    } else {
                        finalist_module.removeTeamFromCategory(currentCategory, team.num);
                        numFinalists = numFinalists - 1;
                    }
                    finalist_module.saveToLocalStorage();

                    finalistDisplay.innerText = numFinalists;
                });
                if (finalist_module.isTeamInCategory(currentCategory, team.num)) {
                    finalistCheck.checked = true;
                }

                const sgCol = document.createElement("td");
                row.appendChild(sgCol);
                sgCol.innerText = group;

                const numCol = document.createElement("td");
                row.appendChild(numCol);
                numCol.innerText = team.num;

                const nameCol = document.createElement("td");
                row.appendChild(nameCol);
                nameCol.innerText = team.name;

                const scoreCol = document.createElement("td");
                row.appendChild(scoreCol);
                if (score) {
                    scoreCol.innerText = score.toFixed(2);
                }

                if (currentCategory.name == finalist_module.CHAMPIONSHIP_NAME) {
                    const rank = finalist_module.getWeightedRank(team, currentCategory);

                    const rankCol = document.createElement("td");
                    row.appendChild(rankCol);
                    rankCol.innerText = rank.toFixed(2);
                }

                const numFinalistCol = document.createElement("td");
                row.appendChild(numFinalistCol);
                numFinalistCol.setAttribute("id", getNumFinalistsId(team));
            } // in correct division
        } // build data for each team
    }

    function updatePage() {
        const categoryName = finalist_module.getCurrentCategoryName();
        const currentCategory = finalist_module.getCategoryByName(categoryName);
        if (null == currentCategory) {
            alert("Invalid category ID found: " + categoryId);
            return;
        }

        // note that this category has been visited so that it
        // doesn't get initialized again
        finalist_module.setCategoryVisited(currentCategory, finalist_module
            .getCurrentDivision());

        const dataElement = document.getElementById("data");
        removeChildren(dataElement);

        const headerRow = document.createElement("tr");
        dataElement.appendChild(headerRow);

        const judgingCol = document.createElement("th");
        headerRow.appendChild(judgingCol);
        judgingCol.innerText = "Judging Group";

        const finalistCol = document.createElement("th");
        headerRow.appendChild(finalistCol);
        finalistCol.innerText = "Finalist?";

        const teamNumberCol = document.createElement("th");
        headerRow.appendChild(teamNumberCol);
        teamNumberCol.innerText = "Team #";

        const teamNameCol = document.createElement("th");
        headerRow.appendChild(teamNameCol);
        teamNameCol.innerText = "Team Name";

        const scoreCol = document.createElement("th");
        headerRow.appendChild(scoreCol);
        if (currentCategory.name == finalist_module.CHAMPIONSHIP_NAME) {
            scoreCol.innerText = "Overall Score";
        } else {
            scoreCol.innerText = "Score";
        }

        if (currentCategory.name == finalist_module.CHAMPIONSHIP_NAME) {
            const rankCol = document.createElement("th");
            headerRow.appendChild(rankCol);
            rankCol.innerText = "Weighted Rank By Judging Group"
        }

        const numCategoriesCol = document.createElement("th");
        headerRow.appendChild(numCategoriesCol);
        numCategoriesCol.innerText = "Num Categories";


        const teams = finalist_module.getAllTeams();
        finalist_module.sortTeamsByCategory(teams, currentCategory);

        createTeamTable(teams, finalist_module.getCurrentDivision(), currentCategory);

        initializeFinalistCounts(teams);
        finalist_module.saveToLocalStorage();
    }

    document.addEventListener('DOMContentLoaded', function() {
        finalist_module.loadFromLocalStorage();

        document.getElementById("previous").addEventListener("click", function() {
            let prev = null;
            let foundCurrent = false;
            for (const category of finalist_module.getNumericCategories()) {
                if (category.name != finalist_module.CHAMPIONSHIP_NAME) {
                    if (finalist_module.getCurrentCategoryName() == category.name) {
                        foundCurrent = true;
                    } // current category
                    if (!foundCurrent) {
                        prev = category;
                    }
                } // not championship
            }

            if (foundCurrent) {
                if (null == prev) {
                    location.href = "non-numeric.html";
                } else {
                    finalist_module.setCurrentCategoryName(prev.name);
                    finalist_module.saveToLocalStorage();
                    location.href = "numeric.html";
                }
            } else {
                const championshipCategory = finalist_module
                    .getCategoryByName(finalist_module.CHAMPIONSHIP_NAME);
                if (finalist_module.getCurrentCategoryName() == championshipCategory.name) {
                    finalist_module.setCurrentCategoryName(prev.name);
                    finalist_module.saveToLocalStorage();

                    location.href = "numeric.html";
                }
            }

        });

        document.getElementById("next").addEventListener("click", function() {
            const championshipCategory = finalist_module
                .getCategoryByName(finalist_module.CHAMPIONSHIP_NAME);
            if (finalist_module.getCurrentCategoryName() == championshipCategory.name) {
                location.href = "schedule.html";
            } else {
                let foundCurrent = false;
                let next = null;
                for (const category of finalist_module.getNumericCategories()) {
                    if (category.name != finalist_module.CHAMPIONSHIP_NAME) {
                        if (foundCurrent && null == next) {
                            next = category;
                        } else if (finalist_module.getCurrentCategoryName() == category.name) {
                            foundCurrent = true;
                        }
                    }
                }
                if (null == next) {
                    finalist_module
                        .setCurrentCategoryName(championshipCategory.name);
                    finalist_module.saveToLocalStorage();

                    location.href = "numeric.html";
                } else {
                    finalist_module.setCurrentCategoryName(next.name);
                    finalist_module.saveToLocalStorage();

                    location.href = "numeric.html";
                }
            }
        });

        const categoryName = finalist_module.getCurrentCategoryName();
        const currentCategory = finalist_module.getCategoryByName(categoryName);
        if (null == currentCategory) {
            alert("Invalid category ID found: " + categoryName);
            return;
        }

        document.getElementById("deselect-all").addEventListener("click", function() {
            document.querySelectorAll("[type=checkbox]").forEach((checkbox) => {
                checkbox.checked = false;
            });
        });

        document.getElementById("reselect").addEventListener("click", function() {
            const teams = finalist_module.getAllTeams();
            const division = finalist_module.getCurrentDivision();
            const scoreGroups = finalist_module.getScoreGroups(teams, division);

            finalist_module.unsetCategoryVisited(currentCategory, division);

            finalist_module.initializeTeamsInNumericCategory(division,
                currentCategory, teams, scoreGroups);
            updatePage();
            finalist_module.saveToLocalStorage();
        });

        document.getElementById("category-name").innerText = currentCategory.name;

        const roomEle = document.getElementById("room");
        roomEle.addEventListener("change", function() {
            const roomNumber = roomEle.value;
            finalist_module.setRoom(currentCategory,
                finalist_module.getCurrentDivision(), roomNumber);
            finalist_module.saveToLocalStorage();
        });
        const room = finalist_module.getRoom(currentCategory, finalist_module.getCurrentDivision());
        if (room) {
            roomEle.value = room;
        } else {
            roomEle.value = "";
        }


        const divisionsElement = document.getElementById("divisions");
        removeChildren(divisionsElement);
        finalist_module.getDivisions().forEach(function(division, i) {
            const divisionOption = document.createElement("option");
            divisionsElement.appendChild(divisionOption);
            divisionOption.setAttribute("value", i);
            divisionOption.innerText = division;
            if (division == finalist_module.getCurrentDivision()) {
                divisionOption.selected = true;
            }

        }); // foreach division
        divisionsElement.addEventListener("change", function() {
            handleDivisionChange();
        });
        handleDivisionChange();

        finalist_module.displayNavbar();
    }); // end ready function
} // scope
