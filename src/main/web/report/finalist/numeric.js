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
        const div = $.finalist.getDivisionByIndex(divIndex);
        $.finalist.setCurrentDivision(div);
        updatePage();
        $.finalist.saveToLocalStorage();
    }

    function getNumFinalistsId(team) {
        return "num_finalists_" + team.num;
    }

    function initializeFinalistCounts(teams) {
        $.each(teams, function(_, team) {
            // initialize to 0
            let numFinalists = 0;
            $.each($.finalist.getAllScheduledCategories(), function(_, category) {
                if ($.finalist.isTeamInCategory(category, team.num)) {
                    numFinalists = numFinalists + 1;
                }
            });
            $("#" + getNumFinalistsId(team)).text(numFinalists);
        });
    }

    function createTeamTable(teams, currentDivision, currentCategory) {
        $.each(teams, function(_, team) {
            if (currentCategory.overall
                || $.finalist.isTeamInDivision(team, currentDivision)) {
                const row = $("<tr></tr>");
                $("#data").append(row);

                const finalistCol = $("<td></td>");
                row.append(finalistCol);
                const finalistCheck = $("<input type='checkbox'/>");
                finalistCol.append(finalistCheck);
                finalistCheck.change(function() {
                    const finalistDisplay = $("#" + getNumFinalistsId(team));
                    let numFinalists = parseInt(finalistDisplay.text(), 10);
                    if ($(this).prop("checked")) {
                        $.finalist.addTeamToCategory(currentCategory, team.num);
                        numFinalists = numFinalists + 1;
                    } else {
                        $.finalist.removeTeamFromCategory(currentCategory, team.num);
                        numFinalists = numFinalists - 1;
                    }
                    $.finalist.saveToLocalStorage();

                    finalistDisplay.text(numFinalists);
                });
                if ($.finalist.isTeamInCategory(currentCategory, team.num)) {
                    finalistCheck.attr("checked", true);
                }

                const sgCol = $("<td></td>");
                row.append(sgCol);
                const group = team.judgingGroup;
                sgCol.text(group);

                const numCol = $("<td></td>");
                row.append(numCol);
                numCol.text(team.num);

                const nameCol = $("<td></td>");
                row.append(nameCol);
                nameCol.text(team.name);

                const scoreCol = $("<td></td>");
                row.append(scoreCol);
                scoreCol.text($.finalist.getCategoryScore(team, currentCategory).toFixed(2));

                const numFinalistCol = $("<td id='" + getNumFinalistsId(team) + "'></td>");
                row.append(numFinalistCol);
            } // in correct division
        }); // build data for each team
    }

    function updatePage() {
        const categoryName = $.finalist.getCurrentCategoryName();
        const currentCategory = $.finalist.getCategoryByName(categoryName);
        if (null == currentCategory) {
            alert("Invalid category ID found: " + categoryId);
            return;
        }

        // note that this category has been visited so that it
        // doesn't get initialized again
        $.finalist.setCategoryVisited(currentCategory, $.finalist
            .getCurrentDivision());

        $("#data").empty();

        const headerRow = $("<tr><th>Finalist?</th><th>Judging Group</th><th>Team #</th><th>Team Name</th><th>Score</th><th>Num Categories</th></tr>");
        $("#data").append(headerRow);

        const teams = $.finalist.getAllTeams();
        $.finalist.sortTeamsByCategory(teams, currentCategory);

        createTeamTable(teams, $.finalist.getCurrentDivision(), currentCategory);

        initializeFinalistCounts(teams);
        $.finalist.saveToLocalStorage();
    }

    $(document)
        .ready(
            function() {
                $.finalist.loadFromLocalStorage();

                $("#previous")
                    .click(
                        function() {
                            let prev = null;
                            let foundCurrent = false;
                            $
                                .each(
                                    $.finalist.getNumericCategories(),
                                    function(_, category) {
                                        if (category.name != $.finalist.CHAMPIONSHIP_NAME) {
                                            if ($.finalist.getCurrentCategoryName() == category.name) {
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
                                    $.finalist.setCurrentCategoryName(prev.name);
                                    $.finalist.saveToLocalStorage();
                                    location.href = "numeric.html";
                                }
                            } else {
                                const championshipCategory = $.finalist
                                    .getCategoryByName($.finalist.CHAMPIONSHIP_NAME);
                                if ($.finalist.getCurrentCategoryName() == championshipCategory.name) {
                                    $.finalist.setCurrentCategoryName(prev.name);
                                    $.finalist.saveToLocalStorage();

                                    location.href = "numeric.html";
                                }
                            }

                        });

                $("#next")
                    .click(
                        function() {
                            const championshipCategory = $.finalist
                                .getCategoryByName($.finalist.CHAMPIONSHIP_NAME);
                            if ($.finalist.getCurrentCategoryName() == championshipCategory.name) {
                                location.href = "schedule.html";
                            } else {
                                let foundCurrent = false;
                                let next = null;
                                $
                                    .each(
                                        $.finalist.getNumericCategories(),
                                        function(_, category) {
                                            if (category.name != $.finalist.CHAMPIONSHIP_NAME) {
                                                if (foundCurrent && null == next) {
                                                    next = category;
                                                } else if ($.finalist.getCurrentCategoryName() == category.name) {
                                                    foundCurrent = true;
                                                }
                                            }
                                        });
                                if (null == next) {
                                    $.finalist
                                        .setCurrentCategoryName(championshipCategory.name);
                                    $.finalist.saveToLocalStorage();

                                    location.href = "numeric.html";
                                } else {
                                    $.finalist.setCurrentCategoryName(next.name);
                                    $.finalist.saveToLocalStorage();

                                    location.href = "numeric.html";
                                }
                            }
                        });

                const categoryName = $.finalist.getCurrentCategoryName();
                const currentCategory = $.finalist.getCategoryByName(categoryName);
                if (null == currentCategory) {
                    alert("Invalid category ID found: " + categoryId);
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
                        const teams = $.finalist.getAllTeams();
                        const division = $.finalist.getCurrentDivision();
                        const scoreGroups = $.finalist.getScoreGroups(teams, division);

                        $.finalist.unsetCategoryVisited(currentCategory, division);

                        $.finalist.initializeTeamsInNumericCategory(division,
                            currentCategory, teams, scoreGroups);
                        updatePage();
                        $.finalist.saveToLocalStorage();
                    });

                $("#category-name").text(currentCategory.name);

                const roomEle = $("#room");
                roomEle.change(function() {
                    const roomNumber = roomEle.val();
                    $.finalist.setRoom(currentCategory,
                        $.finalist.getCurrentDivision(), roomNumber);
                    $.finalist.saveToLocalStorage();
                });
                roomEle.val($.finalist.getRoom(currentCategory, $.finalist
                    .getCurrentDivision()));

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
                handleDivisionChange();

                updatePage();

                $.finalist.displayNavbar();
            }); // end ready function
} // scope
