/*
 * Copyright (c) 2012 High Tech Kids.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

const finalistNumericModule = {};

{
    function handleDivisionChange() {
        const divIndex = $("#divisions").val();
        const div = finalist_module.getDivisionByIndex(divIndex);
        finalist_module.setCurrentDivision(div);
        updatePage();
        finalist_module.saveToLocalStorage();
    }

    function getNumFinalistsId(team) {
        return "num_finalists_" + team.num;
    }

    function initializeFinalistCounts(teams) {
        $.each(teams, function(_, team) {
            // initialize to 0
            let numFinalists = 0;
            $.each(finalist_module.getAllScheduledCategories(), function(_, category) {
                if (finalist_module.isTeamInCategory(category, team.num)) {
                    numFinalists = numFinalists + 1;
                }
            });
            $("#" + getNumFinalistsId(team)).text(numFinalists);
        });
    }

    function createTeamTable(teams, currentDivision, currentCategory) {
        let prevJudgingGroup = null;
        let prevScore = 0;
        let topTeams = false;

        $.each(teams, function(_, team) {
            if (currentCategory.overall
                || finalist_module.isTeamInDivision(team, currentDivision)) {
                const row = $("<tr></tr>");
                $("#data").append(row);

                const score = finalist_module.getCategoryScore(team, currentCategory);
                const group = team.judgingGroup;
                //  || (group == prevJudgingGroup && Math.abs(prevScore - score) < 1)
                if (group != prevJudgingGroup) {
                    topTeams = true;
                    row.addClass("top-score");
                    prevJudgingGroup = group;
                } else {
                    // same judging group
                    if (topTeams && Math.abs(prevScore - score) < 1) {
                        // close score
                        row.addClass("top-score");
                    } else {
                        // don't consider other close scores as ties'
                        topTeams = false;
                    }
                }
                prevScore = score;

                const finalistCol = $("<td></td>");
                row.append(finalistCol);
                const finalistCheck = $("<input type='checkbox'/>");
                finalistCol.append(finalistCheck);
                finalistCheck.change(function() {
                    const finalistDisplay = $("#" + getNumFinalistsId(team));
                    let numFinalists = parseInt(finalistDisplay.text(), 10);
                    if ($(this).prop("checked")) {
                        finalist_module.addTeamToCategory(currentCategory, team.num);
                        numFinalists = numFinalists + 1;
                    } else {
                        finalist_module.removeTeamFromCategory(currentCategory, team.num);
                        numFinalists = numFinalists - 1;
                    }
                    finalist_module.saveToLocalStorage();

                    finalistDisplay.text(numFinalists);
                });
                if (finalist_module.isTeamInCategory(currentCategory, team.num)) {
                    finalistCheck.attr("checked", true);
                }

                const sgCol = $("<td></td>");
                row.append(sgCol);
                sgCol.text(group);

                const numCol = $("<td></td>");
                row.append(numCol);
                numCol.text(team.num);

                const nameCol = $("<td></td>");
                row.append(nameCol);
                nameCol.text(team.name);

                const scoreCol = $("<td></td>");
                row.append(scoreCol);
                if (score) {
                    scoreCol.text(score.toFixed(2));
                }

                const numFinalistCol = $("<td id='" + getNumFinalistsId(team) + "'></td>");
                row.append(numFinalistCol);
            } // in correct division
        }); // build data for each team
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

        $("#data").empty();

        const headerRow = $("<tr><th>Finalist?</th><th>Judging Group</th><th>Team #</th><th>Team Name</th><th>Score</th><th>Num Categories</th></tr>");
        $("#data").append(headerRow);

        const teams = finalist_module.getAllTeams();
        finalist_module.sortTeamsByCategory(teams, currentCategory);

        createTeamTable(teams, finalist_module.getCurrentDivision(), currentCategory);

        initializeFinalistCounts(teams);
        finalist_module.saveToLocalStorage();
    }

    $(document)
        .ready(
            function() {
                finalist_module.loadFromLocalStorage();

                $("#previous")
                    .click(
                        function() {
                            let prev = null;
                            let foundCurrent = false;
                            $
                                .each(
                                    finalist_module.getNumericCategories(),
                                    function(_, category) {
                                        if (category.name != finalist_module.CHAMPIONSHIP_NAME) {
                                            if (finalist_module.getCurrentCategoryName() == category.name) {
                                                foundCurrent = true;
                                            } // current category
                                            if (!foundCurrent) {
                                                prev = category;
                                            }
                                        } // not championship
                                    });

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

                $("#next")
                    .click(
                        function() {
                            const championshipCategory = finalist_module
                                .getCategoryByName(finalist_module.CHAMPIONSHIP_NAME);
                            if (finalist_module.getCurrentCategoryName() == championshipCategory.name) {
                                location.href = "schedule.html";
                            } else {
                                let foundCurrent = false;
                                let next = null;
                                $
                                    .each(
                                        finalist_module.getNumericCategories(),
                                        function(_, category) {
                                            if (category.name != finalist_module.CHAMPIONSHIP_NAME) {
                                                if (foundCurrent && null == next) {
                                                    next = category;
                                                } else if (finalist_module.getCurrentCategoryName() == category.name) {
                                                    foundCurrent = true;
                                                }
                                            }
                                        });
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

                $("#deselect-all").click(function() {
                    $(":checkbox").each(function() {
                        if ($(this).prop('checked')) {
                            $(this).trigger('click');
                        }
                    });
                });

                $("#reselect").click(
                    function() {
                        const teams = finalist_module.getAllTeams();
                        const division = finalist_module.getCurrentDivision();
                        const scoreGroups = finalist_module.getScoreGroups(teams, division);

                        finalist_module.unsetCategoryVisited(currentCategory, division);

                        finalist_module.initializeTeamsInNumericCategory(division,
                            currentCategory, teams, scoreGroups);
                        updatePage();
                        finalist_module.saveToLocalStorage();
                    });

                $("#category-name").text(currentCategory.name);

                const roomEle = $("#room");
                roomEle.change(function() {
                    const roomNumber = roomEle.val();
                    finalist_module.setRoom(currentCategory,
                        finalist_module.getCurrentDivision(), roomNumber);
                    finalist_module.saveToLocalStorage();
                });
                roomEle.val(finalist_module.getRoom(currentCategory, finalist_module
                    .getCurrentDivision()));

                $("#divisions").empty();
                $.each(finalist_module.getDivisions(), function(i, division) {
                    let selected = "";
                    if (division == finalist_module.getCurrentDivision()) {
                        selected = " selected ";
                    }
                    const divisionOption = $("<option value='" + i + "'" + selected + ">"
                        + division + "</option>");
                    $("#divisions").append(divisionOption);
                }); // foreach division
                $("#divisions").change(function() {
                    handleDivisionChange();
                });
                handleDivisionChange();

                updatePage();

                finalist_module.displayNavbar();
            }); // end ready function
} // scope
