/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

/**
 * Create the JSON object to download and set it as the href. 
 * This is meant to be called from the onclick handler of the anchor.
 *
 * @param anchor the anchor to set the href on 
 */
function setOfflineDownloadUrl(anchor) {
    const offline = $.subjective.getOfflineDownloadObject()
    const data = JSON.stringify(offline)
    const blob = new Blob([data], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    anchor.setAttribute("href", url);
}

function populateChooseJudgingGroup() {
    $.subjective.log("choose judging group populate");

    const judgingGroupsContainer = document.getElementById("choose-judging-group_judging-groups");
    removeChildren(judgingGroupsContainer);

    const judgingGroups = $.subjective.getJudgingGroups();
    for (const group of judgingGroups) {
        const button = document.createElement("a");
        judgingGroupsContainer.appendChild(button);
        button.classList.add("wide");
        button.classList.add("center");
        button.innerText = group;
        button.addEventListener('click', function() {
            $.subjective.setCurrentJudgingGroup(group);
            updateMainHeader();
            window.location = "#choose-category";
        });
    }
}

function populateChooseCategory() {
    const container = document.getElementById("choose-category_categories");
    removeChildren(container);

    const categories = $.subjective.getSubjectiveCategories();
    for (const category of categories) {
        const button = document.createElement("a");
        container.appendChild(button);
        button.classList.add("wide");
        button.classList.add("center");
        button.innerText = category.title;
        button.addEventListener('click', function() {
            $.subjective.setCurrentCategory(category);
            updateMainHeader();
            window.location = "#choose-judge";
        });
    }
}

function populateChooseJudge() {
    document.getElementById("choose-judge_new-judge-name").classList.add('fll-sw-ui-inactive');

    const container = document.getElementById("choose-judge_judges")
    removeChildren(container);

    const newJudgeLabel = document.createElement("label");
    container.appendChild(newJudgeLabel);
    newJudgeLabel.classList.add("wide");

    const newJudgeOption = document.createElement("input");
    newJudgeLabel.appendChild(newJudgeOption);
    newJudgeOption.setAttribute("type", "radio");
    newJudgeOption.setAttribute("name", "judge");
    newJudgeOption.setAttribute("value", "new-judge");
    newJudgeOption.setAttribute("id", "choose-judge_new-judge");

    const newJudgeLabelSpan = document.createElement("span");
    newJudgeLabel.appendChild(newJudgeLabelSpan);
    newJudgeLabelSpan.innerText = "New Judge";

    newJudgeOption.addEventListener('change', function() {
        if (newJudgeOption.checked) {
            document.getElementById("choose-judge_new-judge-name").classList.remove('fll-sw-ui-inactive');
        }
    });


    let currentJudge = $.subjective.getCurrentJudge();
    let currentJudgeValid = false;
    const seenJudges = [];
    const judges = $.subjective.getPossibleJudges();
    for (const judge of judges) {
        if (!seenJudges.includes(judge.id)) {
            seenJudges.push(judge.id);

            const judgeLabel = document.createElement("label");
            container.appendChild(judgeLabel);
            judgeLabel.classList.add("wide");

            const judgeInput = document.createElement("input");
            judgeLabel.appendChild(judgeInput);
            judgeInput.setAttribute("type", "radio");
            judgeInput.setAttribute("name", "judge");
            judgeInput.setAttribute("value", judge.id);

            const judgeSpan = document.createElement("span");
            judgeLabel.appendChild(judgeSpan);
            judgeSpan.innerText = judge.id;

            if (null != currentJudge && currentJudge.id == judge.id) {
                currentJudgeValid = true;
                judgeInput.checked = true;
            }


            judgeInput.addEventListener('change', function() {
                if (judgeInput.checked) {
                    document.getElementById("choose-judge_new-judge-name").classList.add('fll-sw-ui-inactive');
                }
            });
        }
    }
    if (!currentJudgeValid) {
        document.getElementById("choose-judge_new-judge-name").classList.remove('fll-sw-ui-inactive');
        newJudgeOption.checked = true;
    }
}

function setJudge() {
    let judgeID = null;
    for (const radio of document.querySelectorAll('input[name="judge"]')) {
        if (radio.checked) {
            judgeID = radio.value;
        }
    }

    if ('new-judge' == judgeID) {
        judgeID = document.getElementById("choose-judge_new-judge-name").value;
        if (null == judgeID || "" == judgeID.trim()) {
            alert("You must enter a name");
            return;
        }
        judgeID = judgeID.trim().toUpperCase();

        $.subjective.addJudge(judgeID);
    }

    $.subjective.setCurrentJudge(judgeID);

    location.href = '#teams-list';
}

function selectTeam(team) {
    $.subjective.setCurrentTeam(team);

    $.subjective.setScoreEntryBackPage("#teams-list-page");
    $.mobile.navigate("#enter-score-page");
}

function populateTeams() {
    $("#teams-list_teams").empty();
    const timeFormatter = JSJoda.DateTimeFormatter.ofPattern("HH:mm");
    const teams = $.subjective.getCurrentTeams();
    $.each(teams, function(i, team) {
        var time = $.subjective.getScheduledTime(team.teamNumber);
        var timeStr = null;
        if (null != time) {
            timeStr = time.format(timeFormatter);
        }

        var scoreStr;
        var score = $.subjective.getScore(team.teamNumber);
        if (!$.subjective.isScoreCompleted(score)) {
            scoreStr = "";
        } else if (score.noShow) {
            scoreStr = "<span class='no-show'>No Show</span> - ";
        } else {
            var computedScore = $.subjective.computeScore(score);
            scoreStr = "<span class='score'>Score: " + computedScore + "</span> - ";
        }

        var label = "";
        if (null != timeStr) {
            label = label + timeStr + " - ";
        }
        label = label + scoreStr;
        label = label + team.teamNumber;
        label = label + " - " + team.teamName;
        if (null != team.organization) {
            label = label + " - " + team.organization;
        }
        var button = $("<button class='ui-btn ui-corner-all text-left'>" //
            + label //
            + "</button>");
        $("#teams-list_teams").append(button);
        button.click(function() {
            selectTeam(team);
        });

    });

    $("#teams-list-page").trigger("create");
}

$(document).on("pagebeforeshow", "#teams-list-page", function(event) {
    var currentJudgingGroup = $.subjective.getCurrentJudgingGroup();
    $("#teams-list_judging-group").text(currentJudgingGroup);

    var currentCategory = $.subjective.getCurrentCategory();
    $("#teams-list_category").text(currentCategory.title);

    var currentJudge = $.subjective.getCurrentJudge();
    $("#teams-list_judge").text(currentJudge.id);

    displayTournamentName($("#teams-list_tournament"));

    populateTeams();
});

$(document).on("pageshow", "#teams-list-page", function(event) {
    $.mobile.loading("hide");
});

function createNewScore() {
    const score = new Object();
    score.modified = false;
    score.deleted = false;
    score.noShow = false;
    score.standardSubScores = {};
    score.enumSubScores = {};
    score.judge = $.subjective.getCurrentJudge().id;
    score.teamNumber = $.subjective.getCurrentTeam().teamNumber;
    score.note = null;
    score.goalComments = {};
    score.commentThinkAbout = null;
    score.commentGreatJob = null;

    return score;
}

/**
 * Save the state of the current page's goals to the specified score object. If
 * null, do nothing.
 */
function saveToScoreObject(score) {
    if (null == score) {
        return;
    }

    $.each($.subjective.getCurrentCategory().allGoals, function(index, goal) {
        if (goal.enumerated) {
            alert("Enumerated goals not supported: " + goal.name);
        } else {
            var subscore = Number($("#" + getScoreItemName(goal)).val());
            score.standardSubScores[goal.name] = subscore;

            var goalComment = $("#enter-score-comment-" + goal.name + "-text").val();
            if (null == score.goalComments) {
                score.goalComments = {};
            }
            score.goalComments[goal.name] = goalComment;
        }
    });
}

/**
 * 
 * @param goal
 *          the goal to get the score item name for
 * @returns the name/id used for the input element that holds the score
 */
function getScoreItemName(goal) {
    return "enter-score_" + goal.name;
}

function recomputeTotal() {
    var currentTeam = $.subjective.getCurrentTeam();
    var score = $.subjective.getScore(currentTeam.teamNumber);

    var total = 0;
    $.each($.subjective.getCurrentCategory().allGoals, function(index, goal) {
        if (goal.enumerated) {
            alert("Enumerated goals not supported: " + goal.name);
        } else {
            var subscore = Number($("#" + getScoreItemName(goal)).val());
            var multiplier = Number(goal.multiplier);
            total = total + subscore * multiplier;
        }
    });
    $("#enter-score_total-score").text(total);
}

function addGoalHeaderToScoreEntry(table, totalColumns, goal) {
    var goalDescriptionRow = $("<tr></tr>");
    var goalDescriptionCell = $("<td colspan='" + totalColumns + "'></td>");
    var goalDescTable = $("<table></table>");
    var goalDescRow = $("<tr></tr>");
    goalDescRow.append($("<th width='25%'>" + goal.title + "</th>"));
    goalDescRow.append($("<td width='5%'>&nbsp;</td>"));
    goalDescRow.append($("<td width='70%'>" + goal.description + "</td>"));
    goalDescTable.append(goalDescRow);
    goalDescriptionCell.append(goalDescTable);
    goalDescriptionRow.append(goalDescriptionCell);
    table.append(goalDescriptionRow);
}

function getRubricCellId(goal, rangeIndex) {
    return goal.name + "_" + rangeIndex;
}

function addRubricToScoreEntry(table, goal, ranges) {

    var row = $("<tr></tr>");

    $
        .each(
            ranges,
            function(index, range) {
                var borderClass;
                var commentButton;
                if (index >= ranges.length - 1) {
                    // skip the right border on the last cell
                    borderClass = "";

                    commentButton = "<a id='enter-score-comment-"
                        + goal.name
                        + "-button'"
                        + " href='#enter-score-comment-"
                        + goal.name
                        + "'"
                        + " data-rel='popup' data-position-to='window'" // 
                        + " class='ui-btn ui-mini ui-corner-all ui-shadow ui-btn-inline'>Comment</a>";

                    var popup = $("<div id='enter-score-comment-"
                        + goal.name
                        + "'"
                        + " class='ui-content' data-role='popup' data-dismissible='false'>"
                        + " </div>");

                    var textarea = $("<textarea id='enter-score-comment-" + goal.name
                        + "-text'" + " rows='20' cols='60'></textarea>");
                    popup.append(textarea);

                    var link = $(" <a href='#' data-rel='back' id='enter-score-"
                        + goal.name
                        + "-close'"
                        + " class='ui-btn ui-corner-all ui-shadow ui-btn-inline ui-btn-b'>Close</a>");
                    popup.append(link);

                    $("#enter-score-goal-comments").append(popup);

                    $("#enter-score-" + goal.name + "-close")
                        .click(function() {
                            var comment = $("#enter-score-comment-" + goal.name + "-text").val();
                            var openCommentButton = $("#enter-score-comment-"
                                + goal.name
                                + "-button");
                            if (!isBlank(comment)) {
                                openCommentButton.addClass("comment-entered");
                            } else {
                                openCommentButton.removeClass("comment-entered");
                            }
                        });

                    $("#enter-score-comment-" + goal.name).popup({
                        afteropen: function(event, ui) {
                            $("#enter-score-comment-" + goal.name + "-text").focus();
                        }
                    });

                } else {
                    borderClass = "border-right";
                    commentButton = "";
                }

                var numColumns = range.max - range.min + 1;
                var cell = $("<td colspan='" + numColumns + "' class='"
                    + borderClass + " center' id='" + getRubricCellId(goal, index)
                    + "'>" + range.shortDescription + commentButton + "</td>");
                row.append(cell);
            });

    table.append(row);

}

function addSliderToScoreEntry(table, goal, totalColumns, ranges, subscore) {
    var initValue;
    if (null == subscore) {
        initValue = goal.initialValue;
    } else {
        initValue = subscore;
    }

    var row = $("<tr></tr>");
    var cell = $("<td class='score-slider-cell' colspan='" + totalColumns
        + "'></td>");

    var sliderId = getScoreItemName(goal);

    var $sliderContainer = $("<div id='" + sliderId + "-container'></div>");
    cell.append($sliderContainer);

    var $slider = $("<input type='range' class='ui-hidden-accessible' name='"
        + sliderId + "' id='" + sliderId + "' min='" + goal.min + "' max='"
        + goal.max + "' value='" + initValue + "'></input>");

    $sliderContainer.append($slider);

    row.append(cell);

    table.append(row);

    highlightRubric(goal, ranges, initValue);
}

function setSliderTicks(goal) {
    var sliderId = getScoreItemName(goal);

    var $slider = $("#" + sliderId);
    var $track = $("#" + sliderId + "-container .ui-slider-track");

    var max = goal.max;
    var min = goal.min;
    var spacing = 100 / (max - min);

    /*
     * $.subjective.log("Creating slider ticks for " + goal.name + " min: " + min + "
     * max: " + max + " spacing: " + spacing);
     */

    $slider.find('.sliderTickMark').remove();
    for (var i = 0; i <= max - min; i++) {
        var $tick = $('<span class="sliderTickMark">&nbsp;</span>');
        $tick.css('left', (spacing * i) + '%');
        $track.prepend($tick);
    }

}

function highlightRubric(goal, ranges, value) {

    $.each(ranges, function(index, range) {
        var rangeCell = $("#" + getRubricCellId(goal, index));
        if (range.min <= value && value <= range.max) {
            rangeCell.addClass("selected-range");
        } else {
            rangeCell.removeClass("selected-range");
        }
    });

}

function addEventsToSlider(goal, ranges) {

    var ranges = goal.rubric;
    ranges.sort(rangeSort);

    var sliderId = getScoreItemName(goal);
    var $slider = $("#" + sliderId);

    $slider.slider({
        highlight: true
    });

    setSliderTicks(goal);

    $slider.on("change", function() {
        var value = $slider.val();
        highlightRubric(goal, ranges, value);

        recomputeTotal();
    });
}

function createScoreRows(table, totalColumns, goal, subscore) {
    addGoalHeaderToScoreEntry(table, totalColumns, goal);

    var ranges = goal.rubric;
    ranges.sort(rangeSort);

    addRubricToScoreEntry(table, goal, ranges);

    addSliderToScoreEntry(table, goal, totalColumns, ranges, subscore);

    table.append($("<tr><td colspan='" + totalColumns + "'>&nbsp;</td></tr>"));
}

/**
 * 
 * @param table
 *          where to put the information
 * @param hidden
 *          true if the row should be hidden
 * @returns total number of columns to represent all scores in the rubric ranges
 */
function populateEnterScoreRubricTitles(table, hidden) {
    var firstGoal = $.subjective.getCurrentCategory().allGoals[0];

    var ranges = firstGoal.rubric;
    ranges.sort(rangeSort);

    var totalColumns = 0;

    var row = $("<tr></tr>");
    if (hidden) {
        row.addClass('hidden');
    }

    $.each(ranges, function(index, range) {
        var numColumns = range.max - range.min + 1;
        var cell = $("<th colspan='" + numColumns + "'>" + range.title + "</th>");
        row.append(cell);

        totalColumns += numColumns;
    });

    table.append(row);

    return totalColumns;
}

/**
 * Create rows for a goal group and it's goals.
 */
function createGoalGroupRows(table, totalColumns, score, goalGroup) {
    var groupText = goalGroup.title;
    if (goalGroup.description) {
        groupText = groupText + " - " + goalGroup.description;
    }

    var bar = $("<tr><td colspan='" + totalColumns + "' class='ui-bar-a'>"
        + groupText + "</td></tr>");
    table.append(bar);

    $.each(goalGroup.goals, function(index, goal) {
        if (goal.enumerated) {
            alert("Enumerated goals not supported: " + goal.name);
        } else {
            var goalScore = null;
            if ($.subjective.isScoreCompleted(score)) {
                goalScore = score.standardSubScores[goal.name];
            }
            createScoreRows(table, totalColumns, goal, goalScore);
        }
    });

}

function updateGreatJobButtonBackground() {
    var greatJobButton = $("#enter-score-comment-great-job-button");
    var comment = $("#enter-score-comment-great-job-text").val();
    if (!isBlank(comment)) {
        greatJobButton.addClass("comment-entered");
    } else {
        greatJobButton.removeClass("comment-entered");
    }
}

function updateThinkAboutButtonBackground() {
    var button = $("#enter-score-comment-think-about-button");
    var comment = $("#enter-score-comment-think-about-text").val();
    if (!isBlank(comment)) {
        button.addClass("comment-entered");
    } else {
        button.removeClass("comment-entered");
    }
}

$(document)
    .on(
        "pagebeforeshow",
        "#enter-score-page",
        function(event) {

            var currentTeam = $.subjective.getCurrentTeam();
            $("#enter-score_team-number").text(currentTeam.teamNumber);
            $("#enter-score_team-name").text(currentTeam.teamName);

            var score = $.subjective.getScore(currentTeam.teamNumber);
            if (null != score) {
                $("#enter-score-note-text").val(score.note);
                $("#enter-score-comment-great-job-text").val(score.commentGreatJob);
                $("#enter-score-comment-think-about-text").val(
                    score.commentThinkAbout);
            } else {
                $("#enter-score-note-text").val("");
                $("#enter-score-comment-great-job-text").val("");
                $("#enter-score-comment-think-about-text").val("");
            }
            updateGreatJobButtonBackground();
            updateThinkAboutButtonBackground();

            var table = $("#enter-score_score-table");
            table.empty();

            $("#enter-score-goal-comments").empty();

            var totalColumns = populateEnterScoreRubricTitles(table, true);

            // put rubric titles in the top header
            var headerTable = $("#enter-score_score-table_header");
            headerTable.empty();
            populateEnterScoreRubricTitles(headerTable, false)

            $.each($.subjective.getCurrentCategory().goalElements, function(
                index, ge) {
                if (ge.goalGroup) {
                    createGoalGroupRows(table, totalColumns, score, ge)
                } else if (ge.goal) {
                    if (ge.enumerated) {
                        alert("Enumerated goals not supported: " + goal.name);
                    } else {
                        var goalScore = null;
                        if ($.subjective.isScoreCompleted(score)) {
                            goalScore = score.standardSubScores[goal.name];
                        }

                        createScoreRows(table, totalColumns, ge, goalScore);
                    }
                }
            });

            // add the non-numeric categories that teams can be nominated for
            $("#enter-score_nominates").empty();
            $("#enter-score_nominates").hide();
            $.each($.subjective.getCurrentCategory().nominates, function(index,
                nominate) {
                $("#enter-score_nominates").show();

                var grid = $("<div class=\"ui-grid-a split_30_70\"></div>");
                $("#enter-score_nominates").append(grid);

                var blockA = $("<div class=\"ui-block-a\">");
                grid.append(blockA);

                var label = $("<label>" + nominate.nonNumericCategoryTitle
                    + "</label>");
                blockA.append(label);
                var checkbox = $("<input type='checkbox' id='enter-score_nominate_"
                    + index + "' />");
                if (null != score
                    && score.nonNumericNominations
                        .includes(nominate.nonNumericCategoryTitle)) {
                    checkbox.prop("checked", true);
                }
                label.append(checkbox);

                var blockB = $("<div class=\"ui-block-b\">");
                grid.append(blockB);

                var nonNumericCategory = $.subjective.getNonNumericCategory(nominate.nonNumericCategoryTitle);
                if (nonNumericCategory) {
                    blockB.text(nonNumericCategory.description);
                }
            });

            // read the intial value
            recomputeTotal();

            $("#enter-score-page").trigger("create");

            // events need to be added after the page create
            $.each($.subjective.getCurrentCategory().allGoals, function(index,
                goal) {
                addEventsToSlider(goal);

                if (null != score) {
                    var openCommentButton = $("#enter-score-comment-"
                        + goal.name
                        + "-button");

                    var comment;
                    if (score.goalComments) {
                        comment = score.goalComments[goal.name];
                    } else {
                        comment = "";
                    }
                    if (!isBlank(comment)) {
                        openCommentButton.addClass("comment-entered");
                    } else {
                        openCommentButton.removeClass("comment-entered");
                    }

                    $("#enter-score-comment-" + goal.name + "-text").val(
                        comment);
                } else {
                    $("#enter-score-comment-" + goal.name + "-text").val("");
                }

            });

        });

function saveScore() {
    var currentTeam = $.subjective.getCurrentTeam();
    var score = $.subjective.getScore(currentTeam.teamNumber);
    if (null == score) {
        score = createNewScore();
    }
    score.modified = true;
    score.deleted = false;
    score.noShow = false;

    score.note = $("#enter-score-note-text").val();
    $.subjective.log("note text: " + score.note);
    score.commentGreatJob = $("#enter-score-comment-great-job-text").val();
    score.commentThinkAbout = $("#enter-score-comment-think-about-text").val();

    saveToScoreObject(score);

    $.subjective.saveScore(score);

    // save non-numeric nominations
    score.nonNumericNominations = [];
    $.each($.subjective.getCurrentCategory().nominates,
        function(index, nominate) {
            $("#enter-score_nominates").show();

            var checkbox = $("#enter-score_nominate_" + index);
            if (checkbox.prop("checked")) {
                score.nonNumericNominations.push(nominate.nonNumericCategoryTitle);
            }
        });

    $.mobile.navigate($.subjective.getScoreEntryBackPage());
}

function enterNoShow() {
    var currentTeam = $.subjective.getCurrentTeam();
    var score = $.subjective.getScore(currentTeam.teamNumber);
    if (null == score) {
        score = createNewScore();
    }
    score.modified = true;
    score.noShow = true;
    score.deleted = false;
    $.subjective.saveScore(score);

    $.mobile.navigate($.subjective.getScoreEntryBackPage());
}

$(document).on("pageshow", "#enter-score-page", function(_) {
    window.onbeforeunload = function() {
        // most browsers won't show the custom message, but we can try
        // returning anything other than undefined will cause the user to be prompted
        return "Are you sure you want to leave?";
    };
})

$(document).on("pagehide", "#enter-score-page", function(_) {
    window.onbeforeunload = undefined;
})

$(document).on("pageinit", "#enter-score-page", function(_) {
    $("#enter-score_save-score").click(function() {

        var totalScore = parseInt($("#enter-score_total-score").text());
        if (totalScore == 0) {
            $("#enter-score_confirm-zero").popup("open");
        } else {
            saveScore();
        }
    });

    $("#enter-score_cancel-score").click(function() {
        $.mobile.navigate($.subjective.getScoreEntryBackPage());
    });

    $("#enter-score_delete-score").click(function() {
        if (confirm("Are you sure you want to delete a score?")) {
            var currentTeam = $.subjective.getCurrentTeam();
            var score = $.subjective.getScore(currentTeam.teamNumber);
            if (null == score) {
                score = createNewScore();
            }
            score.modified = true;
            score.noShow = false;
            score.deleted = true;
            $.subjective.saveScore(score);

            $.mobile.navigate($.subjective.getScoreEntryBackPage());
        }
    });

    $("#enter-score_noshow-score").click(function() {
        enterNoShow();
    });

    $("#enter-score_confirm-zero_yes").click(function() {
        saveScore();
    });

    $("#enter-score_confirm-zero_no-show").click(function() {
        enterNoShow();
    });

    $("#enter-score-comment-great-job-close").click(
        updateGreatJobButtonBackground);
    $("#enter-score-comment-think-about-close").click(
        updateThinkAboutButtonBackground);

    $("#enter-score-comment-great-job").popup({
        afteropen: function(event, ui) {
            $("#enter-score-comment-great-job-text").focus();
        }
    });

    $("#enter-score-comment-think-about").popup({
        afteropen: function(event, ui) {
            $("#enter-score-comment-think-about-text").focus();
        }
    });

    $("#enter-score-note").popup({
        afteropen: function(event, ui) {
            $("#enter-score-note-text").focus();
        }
    });

});

function rangeSort(a, b) {
    if (a.min < b.min) {
        return -1;
    } else if (a.min > b.min) {
        return 1;
    } else {
        return 0;
    }
}

function getScoreRange(goal) {
    return goal.max - goal.min + 1;
}

function populateScoreSummary() {
    $("#score-summary_content").empty();

    var teamScores = {};
    var teamsWithScores = [];

    var teams = $.subjective.getCurrentTeams();
    $.each(teams, function(i, team) {
        var score = $.subjective.getScore(team.teamNumber);
        if ($.subjective.isScoreCompleted(score)) {
            teamsWithScores.push(team);

            var computedScore = $.subjective.computeScore(score);
            teamScores[team.teamNumber] = computedScore;
        }
    });

    teamsWithScores.sort(function(a, b) {
        var scoreA = teamScores[a.teamNumber];
        var scoreB = teamScores[b.teamNumber];
        return scoreA < scoreB ? 1 : scoreA > scoreB ? -1 : 0;
    });

    var rank = 0;
    var rankOffset = 1;
    $.each(teamsWithScores, function(i, team) {
        var computedScore = teamScores[team.teamNumber];

        var prevScore = null;
        if (i > 0) {
            var prevTeam = teamsWithScores[i - 1];
            prevScore = teamScores[prevTeam.teamNumber];
        }

        var nextScore = null;
        if (i + 1 < teamsWithScores.length) {
            var nextTeam = teamsWithScores[i + 1];
            nextScore = teamScores[nextTeam.teamNumber];
        }

        // determine tie for highlighting
        var tieClass = "";
        if (prevScore == computedScore) {
            tieClass = "tie";
        } else if (nextScore == computedScore) {
            tieClass = "tie";
        }

        // determine rank
        if (prevScore == computedScore) {
            rankOffset = rankOffset + 1;
        } else {
            rank = rank + rankOffset;
            rankOffset = 1;
        }

        var nominations = "";
        var score = $.subjective.getScore(team.teamNumber);
        if (score.nonNumericNominations.length > 0) {
            nominations = " - " + score.nonNumericNominations.join(", ");
        }

        var teamRow = $("<div class=\"ui-grid-b ui-responsive\"></div>");

        var teamBlock = $("<div class=\"ui-block-a team-info\">" + rank + " - #"
            + team.teamNumber + "  - " + team.teamName + nominations + "</div>");
        teamRow.append(teamBlock);

        var scoreText;
        if (null == score) {
            scoreText = "";
        } else if (score.noShow) {
            scoreText = "No Show";
        } else {
            scoreText = computedScore;
        }

        var scoreBlock = $("<div class=\"ui-block-b score " + tieClass + "\">"
            + scoreText + "</div>");
        teamRow.append(scoreBlock);

        var editBlock = $("<div class=\"ui-block-c edit\"></div>");
        var editButton = $("<button class=\"ui-btn ui-mini\">Edit</button>");
        editBlock.append(editButton);
        teamRow.append(editBlock);
        editButton.click(function() {
            $.subjective.setCurrentTeam(team);

            $.subjective.setScoreEntryBackPage("#score-summary-page");
            $.mobile.navigate("#enter-score-page");
        });

        $("#score-summary_content").append(teamRow);

        var score = $.subjective.getScore(team.teamNumber);
        var noteRow;
        if (null != score.note) {
            noteRow = $("<div>" + score.note + "</div>");
        } else {
            noteRow = $("<div>No notes</div>");
        }
        $("#score-summary_content").append(noteRow);
        $("#score-summary_content").append($("<hr/>"));

        prevScore = computedScore;
    });

    $("#score-summary-page").trigger("create");
}

$(document).on("pagebeforeshow", "#score-summary-page", function(event) {
    displayTournamentName($("#score-summary_tournament"));

    var currentJudgingGroup = $.subjective.getCurrentJudgingGroup();
    $("#score-summary_judging-group").text(currentJudgingGroup);

    var currentCategory = $.subjective.getCurrentCategory();
    $("#score-summary_category").text(currentCategory.title);

    var currentJudge = $.subjective.getCurrentJudge();
    $("#score-summary_judge").text(currentJudge.id);

    populateScoreSummary();
});

$(document).on("pageshow", "#score-summary-page", function(event) {
    $.mobile.loading("hide");
});

function updateMainHeader() {
    const tournament = $.subjective.getTournament();
    let tournamentName;
    if (null == tournament) {
        tournamentName = "No Tournament";
    } else {
        tournamentName = tournament.name;
    }
    document.getElementById("header-main_tournament-name").innerText = tournamentName;

    document.getElementById("header-main_judging-group-name").innerText = $.subjective.getCurrentJudgingGroup();

    const category = $.subjective.getCurrentCategory();
    let categoryTitle;
    if (null == category) {
        categoryTitle = "No Category";
    } else {
        categoryTitle = category.title;
    }
    document.getElementById("header-main_category-name").innerText = categoryTitle;

    const judge = $.subjective.getCurrentJudge();
    let judgeName;
    if (null == judge) {
        judgeName = "No Judge";
    } else {
        judgeName = judge.id;
    }
    document.getElementById("header-main_judge-name").innerText = judgeName;
}

function updateHeaderPadding(header) {
    const content = document.getElementById("content");
    content.style.paddingTop = computeHeight(header);
}

function updateFooterPadding(footer) {
    const content = document.getElementById("content");
    content.style.paddingBottom = computeHeight(footer);
}

function hideAll() {
    document.querySelectorAll(".fll-sw-ui-footer,.fll-sw-ui-header,.fll-sw-ui-content").forEach(function(el) {
        el.classList.add('fll-sw-ui-inactive');
    });
}

/**
 * Use the anchor portion of the current location to determine which page to display.
 */
function navigateToPage() {
    // always close the panel
    const sidePanel = document.getElementById("side-panel");
    sidePanel.classList.remove('open');

    const pageName = window.location.hash.substring(1)
    console.log("Navigating to page '" + pageName + "'");

    switch (pageName) {
        case "choose-judging-group":
            displayPageChooseJudgingGroup();
            break;
        case "choose-category":
            displayPageChooseCategory();
            break;
        case "choose-judge":
            displayPageChooseJudge();
            break;
        case "score-summary":
            displayPageScoreSummary();
            break;
        case "enter-scores":
            displayPageEnterScores();
            break;
        case "team-score":
            displayPageTeamScore();
            break;
        case "teams-list":
            displayPageTeamsList();
            break;
        default:
            console.log("Unknown page name '" + pageName + "'");
        case "top":
            displayPageTop();
            break;
    }
}

function displayPage(header, content, footer) {
    hideAll();

    header.classList.remove('fll-sw-ui-inactive');
    content.classList.remove('fll-sw-ui-inactive');
    footer.classList.remove('fll-sw-ui-inactive');

    content.style.paddingTop = computeHeight(header) + "px";
    content.style.paddingBottom = computeHeight(footer) + "px";
}

function displayPageTop() {
    document.getElementById("header-main_title").innerText = "Load subjective data";

    displayPage(document.getElementById("header-main"), document.getElementById("content-top"), document.getElementById("footer-main"));

    document.getElementById("header-main_tournament").classList.remove('fll-sw-ui-inactive');
    document.getElementById("header-main_judging-group").classList.add('fll-sw-ui-inactive');
    document.getElementById("header-main_category").classList.add('fll-sw-ui-inactive');
    document.getElementById("header-main_judge").classList.add('fll-sw-ui-inactive');

    document.getElementById("side-panel_top").parentNode.classList.remove('fll-sw-ui-inactive');
    document.getElementById("side-panel_choose-judging-group").parentNode.classList.add('fll-sw-ui-inactive');
    document.getElementById("side-panel_choose-category").parentNode.classList.add('fll-sw-ui-inactive');
    document.getElementById("side-panel_choose-judge").parentNode.classList.add('fll-sw-ui-inactive');
    document.getElementById("side-panel_score-summary").parentNode.classList.add('fll-sw-ui-inactive');
    document.getElementById("side-panel_enter-scores").parentNode.classList.add('fll-sw-ui-inactive');

    removeChildren(document.getElementById("index-page_messages"));
    $.subjective.checkServerStatus(serverLoadPage, promptForJudgingGroup);
    updateMainHeader();
}

function displayPageChooseJudgingGroup() {
    document.getElementById("header-main_title").innerText = "Choose judging group";

    displayPage(document.getElementById("header-main"), document.getElementById("content-choose-judging-group"), document.getElementById("footer-main"));

    document.getElementById("header-main_tournament").classList.remove('fll-sw-ui-inactive');
    document.getElementById("header-main_judging-group").classList.add('fll-sw-ui-inactive');
    document.getElementById("header-main_category").classList.add('fll-sw-ui-inactive');
    document.getElementById("header-main_judge").classList.add('fll-sw-ui-inactive');

    document.getElementById("side-panel_top").parentNode.classList.remove('fll-sw-ui-inactive');
    document.getElementById("side-panel_choose-judging-group").parentNode.classList.add('fll-sw-ui-inactive');
    document.getElementById("side-panel_choose-category").parentNode.classList.add('fll-sw-ui-inactive');
    document.getElementById("side-panel_choose-judge").parentNode.classList.add('fll-sw-ui-inactive');
    document.getElementById("side-panel_score-summary").parentNode.classList.add('fll-sw-ui-inactive');
    document.getElementById("side-panel_enter-scores").parentNode.classList.add('fll-sw-ui-inactive');

    populateChooseJudgingGroup();
    updateMainHeader();
}

function displayPageChooseCategory() {
    document.getElementById("header-main_title").innerText = "Choose category";

    displayPage(document.getElementById("header-main"), document.getElementById("content-choose-category"), document.getElementById("footer-main"));

    document.getElementById("header-main_tournament").classList.remove('fll-sw-ui-inactive');
    document.getElementById("header-main_judging-group").classList.remove('fll-sw-ui-inactive');
    document.getElementById("header-main_category").classList.add('fll-sw-ui-inactive');
    document.getElementById("header-main_judge").classList.add('fll-sw-ui-inactive');

    document.getElementById("side-panel_top").parentNode.classList.remove('fll-sw-ui-inactive');
    document.getElementById("side-panel_choose-judging-group").parentNode.classList.remove('fll-sw-ui-inactive');
    document.getElementById("side-panel_choose-category").parentNode.classList.add('fll-sw-ui-inactive');
    document.getElementById("side-panel_choose-judge").parentNode.classList.add('fll-sw-ui-inactive');
    document.getElementById("side-panel_score-summary").parentNode.classList.add('fll-sw-ui-inactive');
    document.getElementById("side-panel_enter-scores").parentNode.classList.add('fll-sw-ui-inactive');

    populateChooseCategory();
    updateMainHeader();
}

function displayPageChooseJudge() {
    document.getElementById("header-main_title").innerText = "Choose judge";

    displayPage(document.getElementById("header-main"), document.getElementById("content-choose-judge"), document.getElementById("footer-main"));

    document.getElementById("header-main_tournament").classList.remove('fll-sw-ui-inactive');
    document.getElementById("header-main_judging-group").classList.remove('fll-sw-ui-inactive');
    document.getElementById("header-main_category").classList.remove('fll-sw-ui-inactive');
    document.getElementById("header-main_judge").classList.add('fll-sw-ui-inactive');

    document.getElementById("side-panel_top").parentNode.classList.remove('fll-sw-ui-inactive');
    document.getElementById("side-panel_choose-judging-group").parentNode.classList.remove('fll-sw-ui-inactive');
    document.getElementById("side-panel_choose-category").parentNode.classList.remove('fll-sw-ui-inactive');
    document.getElementById("side-panel_choose-judge").parentNode.classList.add('fll-sw-ui-inactive');
    document.getElementById("side-panel_score-summary").parentNode.classList.add('fll-sw-ui-inactive');
    document.getElementById("side-panel_enter-scores").parentNode.classList.add('fll-sw-ui-inactive');

    populateChooseJudge();
    updateMainHeader();
}

function displayPageTeamsList() {
    document.getElementById("header-main_title").innerText = "Select team to score";

    displayPage(document.getElementById("header-main"), document.getElementById("content-teams-list"), document.getElementById("footer-main"));

    document.getElementById("header-main_tournament").classList.remove('fll-sw-ui-inactive');
    document.getElementById("header-main_judging-group").classList.remove('fll-sw-ui-inactive');
    document.getElementById("header-main_category").classList.remove('fll-sw-ui-inactive');
    document.getElementById("header-main_judge").classList.remove('fll-sw-ui-inactive');

    document.getElementById("side-panel_top").parentNode.classList.remove('fll-sw-ui-inactive');
    document.getElementById("side-panel_choose-judging-group").parentNode.classList.remove('fll-sw-ui-inactive');
    document.getElementById("side-panel_choose-category").parentNode.classList.remove('fll-sw-ui-inactive');
    document.getElementById("side-panel_choose-judge").parentNode.classList.remove('fll-sw-ui-inactive');
    document.getElementById("side-panel_score-summary").parentNode.classList.remove('fll-sw-ui-inactive');
    document.getElementById("side-panel_enter-scores").parentNode.classList.add('fll-sw-ui-inactive');

    populateChooseJudge();
    updateMainHeader();
}

function displayPageScoreSummary() {
    displayPage(document.getElementById("header-main"), document.getElementById("content-score-summary"), document.getElementById("footer-main"));

    document.getElementById("header-main_tournament").classList.remove('fll-sw-ui-inactive');
    document.getElementById("header-main_judging-group").classList.remove('fll-sw-ui-inactive');
    document.getElementById("header-main_category").classList.remove('fll-sw-ui-inactive');
    document.getElementById("header-main_judge").classList.remove('fll-sw-ui-inactive');

    document.getElementById("side-panel_top").parentNode.classList.remove('fll-sw-ui-inactive');
    document.getElementById("side-panel_choose-judging-group").parentNode.classList.remove('fll-sw-ui-inactive');
    document.getElementById("side-panel_choose-category").parentNode.classList.remove('fll-sw-ui-inactive');
    document.getElementById("side-panel_choose-judge").parentNode.classList.remove('fll-sw-ui-inactive');
    document.getElementById("side-panel_score-summary").parentNode.classList.add('fll-sw-ui-inactive');
    document.getElementById("side-panel_enter-scores").parentNode.classList.remove('fll-sw-ui-inactive');
}

function displayPageEnterScores() {
    displayPage(document.getElementById("header-main"), document.getElementById("content-enter-scores"), document.getElementById("footer-main"));

    document.getElementById("header-main_tournament").classList.remove('fll-sw-ui-inactive');
    document.getElementById("header-main_judging-group").classList.remove('fll-sw-ui-inactive');
    document.getElementById("header-main_category").classList.remove('fll-sw-ui-inactive');
    document.getElementById("header-main_judge").classList.remove('fll-sw-ui-inactive');

    document.getElementById("side-panel_top").parentNode.classList.remove('fll-sw-ui-inactive');
    document.getElementById("side-panel_choose-judging-group").parentNode.classList.remove('fll-sw-ui-inactive');
    document.getElementById("side-panel_choose-category").parentNode.classList.remove('fll-sw-ui-inactive');
    document.getElementById("side-panel_choose-judge").parentNode.classList.remove('fll-sw-ui-inactive');
    document.getElementById("side-panel_score-summary").parentNode.classList.remove('fll-sw-ui-inactive');
    document.getElementById("side-panel_enter-scores").parentNode.classList.add('fll-sw-ui-inactive');
}

function displayPageTeamScore() {
    displayPage(document.getElementById("header-team-score"), document.getElementById("content-team-score"), document.getElementById("footer-team-score"));
}


document.addEventListener("DOMContentLoaded", () => {

    const openButton = document.getElementById("side-panel_open");
    const sidePanel = document.getElementById("side-panel");

    // handlers for buttons and links that don't navigate to another page
    openButton.addEventListener('click', () => {
        sidePanel.classList.add('open');
    });

    document.getElementById("side-panel_close").addEventListener('click', () => {
        sidePanel.classList.remove('open');
    });

    document.getElementById("side-panel_synchronize").addEventListener('click', () => {
        sidePanel.classList.remove('open');

        const waitDialog = document.getElementById("wait-dialog");
        waitDialog.style.visibility = "visible";

        $.subjective.uploadData(function(result) {
            // scoresSuccess
            alert("Uploaded " + result.numModified + " scores. message: "
                + result.message);
        }, //
            function(result) {
                // scoresFail

                let message;
                if (null == result) {
                    message = "Unknown server error";
                } else {
                    message = result.message;
                }

                alert("Failed to upload scores: " + message);
            }, //
            function(result) {
                // judgesSuccess
                $.subjective.log("Judges modified: " + result.numModifiedJudges
                    + " new: " + result.numNewJudges);
            }

            ,//
            function(result) {
                // judgesFail
                let message;
                if (null == result) {
                    message = "Unknown server error";
                } else {
                    message = result.message;
                }

                alert("Failed to upload judges: " + message);
            }, //
            function() {
                // loadSuccess
                populateChooseJudgingGroup();
                waitDialog.style.visibility = "hidden";
            }, //
            function(message) {
                // loadFail
                populateChooseJudgingGroup();

                waitDialog.style.visibility = "hidden";

                alert("Failed to load scores from server: " + message);
            });
    });

    document.getElementById("side-panel_offline-download").addEventListener('click', () => {
        sidePanel.classList.remove('open');
        // FIXME: need to set this at the top of every page load?
        setOfflineDownloadUrl(document.getElementById("side-panel_offline-download"));
    });


    document.getElementById("team-score_save").addEventListener('click', () => {
        console.log("Saving score");
        window.location = "#enter-scores";
    });
    document.getElementById("team-score_cancel").addEventListener('click', () => {
        console.log("Canceling score entry");
        window.location = "#enter-scores";
    });
    document.getElementById("team-score_delete").addEventListener('click', () => {
        console.log("Deleting score");
        window.location = "#enter-scores";
    });
    document.getElementById("team-score_no-show").addEventListener('click', () => {
        console.log("Mark no show");
        window.location = "#enter-scores";
    });


    document.getElementById("choose-judge_submit").addEventListener('click', function() {
        setJudge();
    });


    // handlers for links that need additional logic
    //FIXME: add listener when creating elements, do the same for other choose pages
    document.getElementById("enter-scores_team-1").addEventListener('click', () => {
        console.log("Store information about what team to score");
    });


    // navigate to pages when the anchor changes
    window.addEventListener('hashchange', navigateToPage);
});

window.addEventListener('load', () => {
    // initial state
    updateMainHeader();
    navigateToPage();
});
