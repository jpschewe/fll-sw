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

    $.subjective.setScoreEntryBackPage("#teams-list");
    window.location = "#enter-score";
}

function populateTeams() {
    const teamsList = document.getElementById("teams-list_teams");
    removeChildren(teamsList);

    const timeFormatter = JSJoda.DateTimeFormatter.ofPattern("HH:mm");
    const teams = $.subjective.getCurrentTeams();
    for (const team of teams) {
        const time = $.subjective.getScheduledTime(team.teamNumber);
        let timeStr = null;
        if (null != time) {
            timeStr = time.format(timeFormatter);
        }

        const button = document.createElement("a");
        teamsList.appendChild(button);
        button.classList.add("wide");

        if (null != timeStr) {
            const span = document.createElement("span");
            button.appendChild(span);
            span.innerText = timeStr + " - ";
        }

        const score = $.subjective.getScore(team.teamNumber);
        if (!$.subjective.isScoreCompleted(score)) {
            // nothing
        } else if (score.noShow) {
            const span = document.createElement("span");
            button.appendChild(span);
            span.innerText = "No Show";
            span.classList.add("no-show");

            const spacerSpan = document.createElement("span");
            button.appendChild(spacerSpan);
            spacerSpan.innerText = " - ";
        } else {
            const computedScore = $.subjective.computeScore(score);
            const span = document.createElement("span");
            button.appendChild(span);
            span.innerText = "Score: " + computedScore;
            span.classList.add("score");

            const spacerSpan = document.createElement("span");
            button.appendChild(spacerSpan);
            spacerSpan.innerText = " - ";
        }

        let teamInformation = team.teamNumber + " - " + team.teamName;
        if (null != team.organization) {
            teamInformation = teamInformation + " - " + team.organization;
        }
        const teamInformationSpan = document.createElement("span");
        button.appendChild(teamInformationSpan);
        teamInformationSpan.innerText = teamInformation;

        button.addEventListener('click', function() {
            selectTeam(team);
        });

    }
}

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

    for (const goal of $.subjective.getCurrentCategory().allGoals) {
        if (goal.enumerated) {
            alert("Enumerated goals not supported: " + goal.name);
        } else {
            const subscore = Number(document.getElementById(getScoreItemName(goal)).value);
            score.standardSubScores[goal.name] = subscore;

            const goalComment = document.getElementById("enter-score-comment-" + goal.name + "-text").value;
            if (null == score.goalComments) {
                score.goalComments = {};
            }
            score.goalComments[goal.name] = goalComment;
        }
    }
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
    let total = 0;
    for (const goal of $.subjective.getCurrentCategory().allGoals) {
        if (goal.enumerated) {
            alert("Enumerated goals not supported: " + goal.name);
        } else {
            const subscore = Number(document.getElementById(getScoreItemName(goal)).value);
            const multiplier = Number(goal.multiplier);
            total = total + subscore * multiplier;
        }
    }
    document.getElementById("enter-score_total-score").innerText = total;
}

function addGoalHeaderToScoreEntry(table, totalColumns, goal) {
    const goalDescriptionRow = document.createElement("tr");
    const goalDescriptionCell = document.createElement("td");
    goalDescriptionCell.setAttribute("colspan", totalColumns);
    const goalDescTable = document.createElement("table");
    const goalDescRow = document.createElement("tr");

    const goalTitle = document.createElement("td");
    goalDescRow.appendChild(goalTitle);
    goalTitle.innerText = goal.title;
    goalTitle.style.width = "25%";

    const spacer = document.createElement("td");
    goalDescRow.appendChild(spacer);
    spacer.innerHtml = "&nbsp;";
    spacer.style.width = "5%";

    const goalDescription = document.createElement("td");
    goalDescRow.appendChild(goalDescription);
    goalDescription.innerText = goal.description;
    goalDescription.style.width = "70%";

    goalDescTable.appendChild(goalDescRow);
    goalDescriptionCell.appendChild(goalDescTable);
    goalDescriptionRow.appendChild(goalDescriptionCell);
    table.appendChild(goalDescriptionRow);
}

function getRubricCellId(goal, rangeIndex) {
    return goal.name + "_" + rangeIndex;
}

function addRubricToScoreEntry(table, goal, ranges) {

    const row = document.createElement("tr");

    for (let index = 0; index < ranges.length; ++index) {
        const range = ranges[index];

        const numColumns = range.max - range.min + 1;
        const cell = document.createElement("td");
        row.appendChild(cell);
        cell.setAttribute("colspan", numColumns);
        cell.classList.add("center");
        cell.id = getRubricCellId(goal, index);
        const cellText = document.createElement("span");
        cell.appendChild(cellText);
        cellText.innerText = range.shortDescription;

        if (index >= ranges.length - 1) {
            const commentButton = document.createElement("button");
            cell.appendChild(commentButton);
            commentButton.id = "enter-score-comment-" + goal.name + "-button'";
            commentButton.innerText = "Comment";

            const popup = document.createElement("div");
            popup.classList.add("dialog");
            popup.id = "enter-score-comment-" + goal.name;

            const popupContent = document.createElement("div");
            popup.appendChild(popupContent);
            popupContent.classList.add('comment-dialog')

            const textarea = document.createElement("textarea");
            popupContent.appendChild(textarea);
            textarea.id = "enter-score-comment-" + goal.name + "-text";
            textarea.classList.add('comment-text');
            textarea.setAttribute("rows", "20");
            textarea.setAttribute("cols", "60");

            const closeButton = document.createElement("button");
            popupContent.appendChild(closeButton);
            closeButton.setAttribute("type", "button");
            closeButton.id = "enter-score-comment-" + goal.name + "-close";
            closeButton.innerText = 'Close';

            document.getElementById("enter-score-goal-comments").appendChild(popup);

            closeButton.addEventListener("click", function() {
                popup.style.visibility = "hidden";

                const comment = textarea.value;
                if (!isBlank(comment)) {
                    commentButton.classList.add("comment-entered");
                } else {
                    commentButton.classList.remove("comment-entered");
                }
            });

            commentButton.addEventListener("click", function() {
                popup.style.visibility = "visible";
                textarea.focus();
            });

        } else {
            cell.classList.add("border-right");
        }

    }

    table.appendChild(row);

}

function addSliderToScoreEntry(table, goal, totalColumns, ranges, subscore) {
    let initValue;
    if (null == subscore) {
        initValue = goal.initialValue;
    } else {
        initValue = subscore;
    }

    const row = document.createElement("tr");
    const cell = document.createElement("td");
    cell.setAttribute("colspan", totalColumns);
    cell.classList.add("score-slider-cell");

    const sliderId = getScoreItemName(goal);

    const sliderContainer = document.createElement("div");
    sliderContainer.id = sliderId + "-container";
    cell.appendChild(sliderContainer);

    const slider = document.createElement("input");
    slider.setAttribute("type", "range");
    slider.name = sliderId;
    slider.id = sliderId;
    slider.setAttribute("min", goal.min);
    slider.setAttribute("max", goal.max);
    slider.value = initValue;

    sliderContainer.appendChild(slider);

    row.appendChild(cell);

    table.appendChild(row);

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
    for (let index = 0; index < ranges.length; ++index) {
        const range = ranges[index];

        const rangeCell = document.getElementById(getRubricCellId(goal, index));
        if (range.min <= value && value <= range.max) {
            rangeCell.classList.add("selected-range");
        } else {
            rangeCell.classList.remove("selected-range");
        }
    }
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

    const ranges = goal.rubric;
    ranges.sort(rangeSort);

    addRubricToScoreEntry(table, goal, ranges);

    addSliderToScoreEntry(table, goal, totalColumns, ranges, subscore);

    const row = document.createElement("tr");
    table.appendChild(row);

    const cell = document.createElement("td");
    row.appendChild(cell);
    cell.setAttribute("colspan", totalColumns);
    cell.innerHtml = "&nbsp;";
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
    const firstGoal = $.subjective.getCurrentCategory().allGoals[0];

    const ranges = firstGoal.rubric;
    ranges.sort(rangeSort);

    let totalColumns = 0;

    const row = document.createElement("tr");
    if (hidden) {
        row.classList.add('hidden');
    }

    for (const range of ranges) {
        const numColumns = range.max - range.min + 1;
        const cell = document.createElement("th");
        cell.setAttribute("colspan", numColumns);
        cell.innerText = range.title;
        row.appendChild(cell);

        totalColumns += numColumns;
    }

    table.appendChild(row);

    return totalColumns;
}

/**
 * Create rows for a goal group and it's goals.
 */
function createGoalGroupRows(table, totalColumns, score, goalGroup) {
    let groupText = goalGroup.title;
    if (goalGroup.description) {
        groupText = groupText + " - " + goalGroup.description;
    }

    const barRow = document.createElement("tr");
    table.appendChild(barRow);
    const barCell = document.createElement("td");
    barRow.appendChild(barCell);
    barCell.setAttribute("colspan", totalColumns);
    barCell.innerText = groupText;

    for (const goal of goalGroup.goals) {
        if (goal.enumerated) {
            alert("Enumerated goals not supported: " + goal.name);
        } else {
            let goalScore = null;
            if ($.subjective.isScoreCompleted(score)) {
                goalScore = score.standardSubScores[goal.name];
            }
            createScoreRows(table, totalColumns, goal, goalScore);
        }
    }

}

function updateGreatJobButtonBackground() {
    const greatJobButton = document.getElementById("enter-score-comment-great-job-button");
    const comment = document.getElementById("enter-score-comment-great-job-text").value;
    if (!isBlank(comment)) {
        greatJobButton.classList.add("comment-entered");
    } else {
        greatJobButton.classList.remove("comment-entered");
    }
}

function updateThinkAboutButtonBackground() {
    const button = document.getElementById("enter-score-comment-think-about-button");
    const comment = document.getElementById("enter-score-comment-think-about-text").value;
    if (!isBlank(comment)) {
        button.classList.add("comment-entered");
    } else {
        button.classList.remove("comment-entered");
    }
}


/**
 * Save the score to storage and go back the to the score entry back page.
 */
function saveScore() {
    var currentTeam = $.subjective.getCurrentTeam();
    var score = $.subjective.getScore(currentTeam.teamNumber);
    if (null == score) {
        score = createNewScore();
    }
    score.modified = true;
    score.deleted = false;
    score.noShow = false;

    score.note = document.getElementById("enter-score-note-text").value;
    $.subjective.log("note text: " + score.note);
    score.commentGreatJob = document.getElementById("enter-score-comment-great-job-text").value;
    score.commentThinkAbout = document.getElementById("enter-score-comment-think-about-text").value;

    saveToScoreObject(score);

    $.subjective.saveScore(score);

    // save non-numeric nominations
    score.nonNumericNominations = [];
    //FIXME jquery
    $.each($.subjective.getCurrentCategory().nominates,
        function(index, nominate) {
            $("#enter-score_nominates").show();

            var checkbox = $("#enter-score_nominate_" + index);
            if (checkbox.prop("checked")) {
                score.nonNumericNominations.push(nominate.nonNumericCategoryTitle);
            }
        });

    window.location = $.subjective.getScoreEntryBackPage();
}

/**
 * Save a no show and go back to the score entry back page.
 */
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

    window.location = $.subjective.getScoreEntryBackPage();
}

function installWarnOnReload() {
    window.onbeforeunload = function() {
        // most browsers won't show the custom message, but we can try
        // returning anything other than undefined will cause the user to be prompted
        return "Are you sure you want to leave?";
    };
}

function uninstallWarnOnReload() {
    window.onbeforeunload = undefined;
}

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
    const scoreSummaryContent = document.getElementById("score-summary_content");
    removeChildren(scoreSummaryContent);

    const teamScores = {};
    const teamsWithScores = [];

    const teams = $.subjective.getCurrentTeams();
    for (const team of teams) {
        const score = $.subjective.getScore(team.teamNumber);
        if ($.subjective.isScoreCompleted(score)) {
            teamsWithScores.push(team);

            const computedScore = $.subjective.computeScore(score);
            teamScores[team.teamNumber] = computedScore;
        }
    }

    teamsWithScores.sort(function(a, b) {
        var scoreA = teamScores[a.teamNumber];
        var scoreB = teamScores[b.teamNumber];
        return scoreA < scoreB ? 1 : scoreA > scoreB ? -1 : 0;
    });

    let rank = 0;
    let rankOffset = 1;
    for (let i = 0; i < teamsWithScores.length; ++i) {
        const team = teamsWithScores[i];
        const computedScore = teamScores[team.teamNumber];
        const score = $.subjective.getScore(team.teamNumber);

        let prevScore = null;
        if (i > 0) {
            const prevTeam = teamsWithScores[i - 1];
            prevScore = teamScores[prevTeam.teamNumber];
        }

        let nextScore = null;
        if (i + 1 < teamsWithScores.length) {
            const nextTeam = teamsWithScores[i + 1];
            nextScore = teamScores[nextTeam.teamNumber];
        }

        const teamRow = document.createElement("div");
        scoreSummaryContent.appendChild(teamRow);

        const teamBlock = document.createElement("span");
        teamRow.appendChild(teamBlock);

        const rightBlock = document.createElement("span");
        teamRow.appendChild(rightBlock);
        rightBlock.classList.add("right-align");

        const scoreBlock = document.createElement("span");
        rightBlock.appendChild(scoreBlock);
        let scoreText;
        if (null == score) {
            scoreText = "";
        } else if (score.noShow) {
            scoreText = "No Show";
        } else {
            scoreText = computedScore;
        }
        scoreBlock.innerText = scoreText;
        scoreBlock.classList.add("score");
        scoreBlock.classList.add("score-summary-right-elements");

        // determine tie for highlighting
        if (prevScore == computedScore) {
            scoreBlock.classList.add("tie");
        } else if (nextScore == computedScore) {
            scoreBlock.classList.add("tie");
        }

        // determine rank
        if (prevScore == computedScore) {
            rankOffset = rankOffset + 1;
        } else {
            rank = rank + rankOffset;
            rankOffset = 1;
        }

        let nominations = "";
        if (score.nonNumericNominations.length > 0) {
            nominations = " - " + score.nonNumericNominations.join(", ");
        }

        teamBlock.innerText = rank + " - #" + team.teamNumber + "  - " + team.teamName + nominations;

        const editButton = document.createElement("button");
        rightBlock.appendChild(editButton);
        editButton.innerText = "Edit";
        editButton.classList.add("score-summary-right-elements");

        editButton.addEventListener("click", function() {
            $.subjective.setCurrentTeam(team);

            $.subjective.setScoreEntryBackPage("#score-summary");
            window.location = "#enter-score";
        });


        const noteRow = document.createElement("div");
        if (null != score.note) {
            noteRow.innerText = score.note;
        } else {
            noteRow.innerText = "No notes";
        }
        scoreSummaryContent.appendChild(noteRow);
        scoreSummaryContent.appendChild(document.createElement("hr"));

        prevScore = computedScore;
    }
}

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
        case "enter-score":
            displayPageEnterScore();
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
    uninstallWarnOnReload();
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

    populateTeams();

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

    populateScoreSummary();

    updateMainHeader();
}

function displayPageEnterScore() {
    installWarnOnReload();

    // populate the header before displaying the page to ensure that the header height is properly computed
    const currentTeam = $.subjective.getCurrentTeam();
    document.getElementById("enter-score_team-number").innerText = currentTeam.teamNumber;
    document.getElementById("enter-score_team-name").innerText = currentTeam.teamName;

    const score = $.subjective.getScore(currentTeam.teamNumber);
    if (null != score) {
        document.getElementById("enter-score-note-text").value = score.note;
        document.getElementById("enter-score-comment-great-job-text").value = score.commentGreatJob;
        document.getElementById("enter-score-comment-think-about-text").value =
            score.commentThinkAbout;
    } else {
        document.getElementById("enter-score-note-text").value = "";
        document.getElementById("enter-score-comment-great-job-text").value = "";
        document.getElementById("enter-score-comment-think-about-text").value = "";
    }

    // put rubric titles in the top header
    const headerTable = document.getElementById("enter-score_score-table_header");
    removeChildren(headerTable);
    populateEnterScoreRubricTitles(headerTable, false)

    displayPage(document.getElementById("header-enter-score"), document.getElementById("content-enter-score"), document.getElementById("footer-enter-score"));

    removeChildren(document.getElementById("enter-score-goal-comments"));

    const table = document.getElementById("enter-score_score-table");
    removeChildren(table);

    const totalColumns = populateEnterScoreRubricTitles(table, true);

    for (const ge of $.subjective.getCurrentCategory().goalElements) {
        if (ge.goalGroup) {
            createGoalGroupRows(table, totalColumns, score, ge)
        } else if (ge.goal) {
            if (ge.enumerated) {
                alert("Enumerated goals not supported: " + goal.name);
            } else {
                let goalScore = null;
                if ($.subjective.isScoreCompleted(score)) {
                    goalScore = score.standardSubScores[goal.name];
                }

                createScoreRows(table, totalColumns, ge, goalScore);
            }
        }
    }

    //FIXME
    /*
    
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
    
    //FIXME: add content here from appropriate on function
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
    */

    updateGreatJobButtonBackground();
    updateThinkAboutButtonBackground();

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


    document.getElementById("enter-score_save").addEventListener('click', () => {
        console.log("Saving score");

        const totalScore = parseInt(document.getElementById("enter-score_total-score").innerText);
        if (totalScore == 0) {
            document.getElementById("enter-score_confirm-zero").style.visibility = 'visible';
        } else {
            saveScore();
        }
    });
    document.getElementById("enter-score_cancel").addEventListener('click', () => {
        console.log("Canceling score entry");
        window.location = $.subjective.getScoreEntryBackPage();
    });
    document.getElementById("enter-score_delete").addEventListener('click', () => {
        console.log("Deleting score");

        //FIXME need confirm dialog

        if (confirm("Are you sure you want to delete a score?")) {
            const currentTeam = $.subjective.getCurrentTeam();
            let score = $.subjective.getScore(currentTeam.teamNumber);
            if (null == score) {
                score = createNewScore();
            }
            score.modified = true;
            score.noShow = false;
            score.deleted = true;
            $.subjective.saveScore(score);

            window.location = $.subjective.getScoreEntryBackPage();
        }
    });
    document.getElementById("enter-score_no-show").addEventListener('click', () => {
        console.log("Mark no show");

        //FIXME need confirm dialog

        enterNoShow();
    });
    document.getElementById("enter-score_confirm-zero_yes").addEventListener('click', function() {
        document.getElementById("enter-score_confirm-zero").style.visibility = 'hidden';
        saveScore();
    });
    document.getElementById("enter-score_confirm-zero_no-show").addEventListener('click', function() {
        document.getElementById("enter-score_confirm-zero").style.visibility = 'hidden';
        enterNoShow();
    });

    document.getElementById("enter-score-comment-great-job-close").addEventListener('click', function() {
        document.getElementById("enter-score-comment-great-job").style.visibility = 'hidden';
        updateGreatJobButtonBackground();
    });
    document.getElementById("enter-score-comment-think-about-close").addEventListener('click', function() {
        document.getElementById("enter-score-comment-think-about").style.visibility = 'hidden';
        updateThinkAboutButtonBackground();
    });

    document.getElementById("enter-score-comment-great-job-button").addEventListener('click', function() {
        document.getElementById("enter-score-comment-great-job").style.visibility = 'visible';
        document.getElementById("enter-score-comment-great-job-text").focus();
    });

    document.getElementById("enter-score-comment-think-about-button").addEventListener('click', function() {
        document.getElementById("enter-score-comment-think-about").style.visibility = 'visible';
        document.getElementById("enter-score-comment-think-about-text").focus();
    });
    document.getElementById("enter-score-note-button").addEventListener('click', function() {
        document.getElementById("enter-score-note").style.visibility = 'visible';
        document.getElementById("enter-score-note-text").focus();
    });
    document.getElementById("enter-score-note-close").addEventListener('click', function() {
        document.getElementById("enter-score-note").style.visibility = 'hidden';
    });

    document.getElementById("choose-judge_submit").addEventListener('click', function() {
        setJudge();
    });

    // navigate to pages when the anchor changes
    window.addEventListener('hashchange', navigateToPage);
});

window.addEventListener('load', () => {
    // initial state
    updateMainHeader();
    navigateToPage();
});
