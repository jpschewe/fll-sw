/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use-strict";

function selectJudgingGroup(group) {
    $.subjective.setCurrentJudgingGroup(group);
    $.mobile.navigate("#choose-category-page");
}

$(document).on(
    "pageinit",
    "#choose-judging-group-page",
    function(event) {
        $.subjective.log("choose judging group pageinit");

        $("#choose-judging-group_upload-scores").click(
            function() {
                $.mobile.loading("show");

                $.subjective.uploadData(function(result) {
                    // scoresSuccess
                    alert("Uploaded " + result.numModified + " scores. message: "
                        + result.message);
                }, //
                    function(result) {
                        // scoresFail

                        var message;
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
                        var message;
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
                        $.mobile.loading("hide");
                    }, //
                    function(message) {
                        // loadFail
                        populateChooseJudgingGroup();

                        $.mobile.loading("hide");

                        alert("Failed to load scores from server: " + message);
                    });
            });
    });

function populateChooseJudgingGroup() {
    $.subjective.log("choose judging group populate");

    displayTournamentName($("#choose-judging-group_tournament"));

    $("#choose-judging-group_judging-groups").empty();

    var judgingGroups = $.subjective.getJudgingGroups();
    $.each(judgingGroups, function(i, group) {
        var button = $("<button class='ui-btn ui-corner-all'>" + group
            + "</button>");
        $("#choose-judging-group_judging-groups").append(button);
        button.click(function() {
            selectJudgingGroup(group);
        });

    });

    $("#choose-judging-group-page").trigger("create");
}

$(document).on("pagebeforeshow", "#choose-judging-group-page",
    populateChooseJudgingGroup);

function selectCategory(category) {
    $.subjective.setCurrentCategory(category);
    $.mobile.navigate("#choose-judge-page");
}

$(document).on("pageshow", "#choose-category-page", function(event) {
    $.mobile.loading("hide");
});

$(document).on(
    "pageinit",
    "#choose-category-page",
    function(event) {
        $("#choose-category_upload-scores").click(
            function() {
                $.mobile.loading("show");

                $.subjective.uploadData(function(result) {
                    // scoresSuccess
                    alert("Uploaded " + result.numModified + " scores. message: "
                        + result.message);
                }, //
                    function(result) {
                        // scoresFail

                        var message;
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
                        var message;
                        if (null == result) {
                            message = "Unknown server error";
                        } else {
                            message = result.message;
                        }

                        alert("Failed to upload judges: " + message);
                    }, //
                    function() {
                        // loadSuccess
                        populateChooseCategory();

                        $.mobile.loading("hide");
                    }, //
                    function(message) {
                        // loadFail
                        populateChooseCategory();

                        $.mobile.loading("hide");

                        alert("Failed to load scores from server: " + message);
                    });
            });
    });

function populateChooseCategory() {
    $("#choose-category_categories").empty();

    displayTournamentName($("#choose-category_tournament"));

    var categories = $.subjective.getSubjectiveCategories();
    $.each(categories, function(i, category) {
        var button = $("<button class='ui-btn ui-corner-all'>" + category.title
            + "</button>");
        $("#choose-category_categories").append(button);
        button.click(function() {
            selectCategory(category);
        });
        button.trigger("updateLayout");
    });

    var currentJudgingGroup = $.subjective.getCurrentJudgingGroup();
    $("#choose-category_judging-group").text(currentJudgingGroup);

    $("#choose-category-page").trigger("create");
}

$(document).on("pagebeforeshow", "#choose-category-page",
    populateChooseCategory);

$(document).on("pageshow", "#choose-judge-page", function(event) {
    $.mobile.loading("hide");
});

$(document).on(
    "pageinit",
    "#choose-judge-page",
    function(event) {
        $("#choose-judge_upload-scores").click(
            function() {
                $.mobile.loading("show");

                $.subjective.uploadData(function(result) {
                    // scoresSuccess

                    alert("Uploaded " + result.numModified + " scores. message: "
                        + result.message);
                }, //
                    function(result) {
                        // scoresFail

                        var message;
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
                        var message;
                        if (null == result) {
                            message = "Unknown server error";
                        } else {
                            message = result.message;
                        }

                        alert("Failed to upload judges: " + message);
                    }, //
                    function() {
                        // loadSuccess
                        populateChooseJudge();

                        $.mobile.loading("hide");
                    }, //
                    function(message) {
                        // loadFail
                        populateChooseJudge();

                        $.mobile.loading("hide");

                        alert("Failed to load scores from server: " + message);
                    });
            });
    });

function populateChooseJudge() {
    displayTournamentName($("#choose-judge_tournament"));

    $("#choose-judge_judges").empty();
    $("#choose-judge_judges")
        .append(
            "<input type='radio' name='judge' value='new-judge' id='choose-judge_new-judge'>");
    $("#choose-judge_judges").append(
        "<label for='choose-judge_new-judge'>New Judge</label>");

    var currentJudge = $.subjective.getCurrentJudge();
    var currentJudgeValid = false;
    var seenJudges = [];
    var judges = $.subjective.getPossibleJudges();
    $.each(judges, function(index, judge) {
        if (!seenJudges.includes(judge.id)) {
            seenJudges.push(judge.id);

            if (null != currentJudge && currentJudge.id == judge.id) {
                currentJudgeValid = true;
            }
            $("#choose-judge_judges").append(
                "<input type='radio' name='judge' id='choose-judge_" + index
                + "' value='" + judge.id + "'>");
            $("#choose-judge_judges").append(
                "<label for='choose-judge_" + index + "'>" + judge.id + "</label>");
        }
    });
    if (!currentJudgeValid) {
        currentJudge = null;
    }

    var currentJudgingGroup = $.subjective.getCurrentJudgingGroup();
    $("#choose-judge_judging-group").text(currentJudgingGroup);

    var currentCategory = $.subjective.getCurrentCategory();
    $("#choose-judge_category").text(currentCategory.title);

    if (null != currentJudge) {
        $("input:radio[value=\"" + currentJudge.id + "\"]").prop('checked', true);
    } else {
        $("input:radio[value='new-judge']").prop('checked', true);
        $("#choose-judge_new-judge-info").show();
    }

    $("input[name=judge]:radio").change(function() {
        var judgeID = $("input:radio[name='judge']:checked").val();
        if ('new-judge' == judgeID) {
            $("#choose-judge_new-judge-info").show();
        } else {
            $("#choose-judge_new-judge-info").hide();
        }
    });

    $("#choose-judge-page").trigger("create");
}

$(document).on("pagebeforeshow", "#choose-judge-page", function(event) {
    $("#choose-judge_new-judge-info").hide();
    populateChooseJudge();
});

$(document).on("pageinit", "#choose-judge-page", function(event) {

    $("#choose-judge_judge-submit").click(function() {
        setJudge();
    });

});

function setJudge() {
    var judgeID = $("input:radio[name='judge']:checked").val();
    if ('new-judge' == judgeID) {
        judgeID = $("#choose-judge_new-judge-name").val();
        if (null == judgeID || "" == judgeID.trim()) {
            alert("You must enter a name");
            return;
        }
        judgeID = judgeID.trim().toUpperCase();

        $.subjective.addJudge(judgeID);
    }

    $.subjective.setCurrentJudge(judgeID);

    $.mobile.navigate("#teams-list-page");
}

function selectTeam(team) {
    $.subjective.setCurrentTeam(team);

    $.subjective.setScoreEntryBackPage("#teams-list-page");
    $.mobile.navigate("#enter-score-page");
}

function populateTeams() {
    $("#teams-list_teams").empty();
    var teams = $.subjective.getCurrentTeams();
    $.each(teams, function(i, team) {
        var time = $.subjective.getScheduledTime(team.teamNumber);
        var timeStr = null;
        if (null != time) {
            timeStr = time.getHours().toString().padL(2, "0") + ":"
                + time.getMinutes().toString().padL(2, "0");
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

$(document).on(
    "pageinit",
    "#teams-list-page",
    function(event) {
        $("#teams-list_upload-scores").click(
            function() {
                $.mobile.loading("show");

                $.subjective.uploadData(function(result) {
                    // scoresSuccess
                    populateTeams();

                    alert("Uploaded " + result.numModified + " scores. message: "
                        + result.message);
                }, //
                    function(result) {
                        // scoresFail
                        populateTeams();

                        var message;
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
                        var message;
                        if (null == result) {
                            message = "Unknown server error";
                        } else {
                            message = result.message;
                        }

                        alert("Failed to upload judges: " + message);
                    }, //
                    function() {
                        // loadSuccess
                        $.mobile.loading("hide");
                    }, //
                    function(message) {
                        // loadFail
                        $.mobile.loading("hide");

                        alert("Failed to load scores from server: " + message);
                    });
            });
    });

function createNewScore() {
    score = new Object();
    score.modified = false;
    score.deleted = false;
    score.noShow = false;
    score.standardSubScores = {};
    score.enumSubScores = {};
    score.judge = $.subjective.getCurrentJudge().id;
    score.teamNumber = $.subjective.getCurrentTeam().teamNumber;
    score.note = null;

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
                        + "-save'"
                        + " class='ui-btn ui-corner-all ui-shadow ui-btn-inline ui-btn-b'>Save</a>");
                    popup.append(link);

                    $("#enter-score-goal-comments").append(popup);

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
                    $("#enter-score-comment-" + goal.name + "-text").val(
                        score.goalComments[goal.name]);
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

$(document).on("pageinit", "#enter-score-page", function(event) {
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

        var teamRow = $("<div class=\"ui-grid-b ui-responsive\"></div>");

        var teamBlock = $("<div class=\"ui-block-a team-info\">" + rank + " - #"
            + team.teamNumber + "  - " + team.teamName + "</div>");
        teamRow.append(teamBlock);

        var score = $.subjective.getScore(team.teamNumber);
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

$(document).on(
    "pageinit",
    "#score-summary-page",
    function(event) {
        $("#score-summary_upload-scores").click(
            function() {
                $.mobile.loading("show", {
                    text: "Uploading Scores..."
                });

                $.subjective.uploadData(function(result) {
                    // scoresSuccess
                    populateScoreSummary();

                    alert("Uploaded " + result.numModified + " scores. message: "
                        + result.message);
                }, //
                    function(result) {
                        // scoresFail
                        populateScoreSummary();

                        var message;
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
                        var message;
                        if (null == result) {
                            message = "Unknown server error";
                        } else {
                            message = result.message;
                        }

                        alert("Failed to upload judges: " + message);
                    }, //
                    function() {
                        // loadSuccess
                        $.mobile.loading("hide");
                    }, //
                    function(message) {
                        // loadFail
                        $.mobile.loading("hide");

                        alert("Failed to load scores from server: " + message);
                    });
            });
    });

function displayTournamentName(displayElement) {
    var tournament = $.subjective.getTournament();
    var tournamentName;
    if (null == tournament) {
        tournamentName = "None";
    } else {
        tournamentName = tournament.name;
    }
    displayElement.text("Tournament: " + tournamentName);
}

window.onbeforeunload = function() {
    // most browsers won't show the custom message, but we can try
    // returning anything other than undefined will cause the user to be prompted
    return "Are you sure you want to leave?";
};
