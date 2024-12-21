/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";


// start online under the assumption that it's just been loaded from the server
let server_online = true;

let alertCallback = null;

// common time formatter
const timeFormatter = JSJoda.DateTimeFormatter.ofPattern("HH:mm");

/**
 * Create the JSON object to download and set it as the href. 
 * This is meant to be called from the onclick handler of the anchor.
 *
 * @param anchor the anchor to set the href on 
 */
function setOfflineDownloadUrl(anchor) {
    const offline = subjective_module.getOfflineDownloadObject()
    const data = JSON.stringify(offline)
    const blob = new Blob([data], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    anchor.setAttribute("href", url);
    anchor.setAttribute("download", "subjective-data.json");
}

function populateChooseJudgingGroup() {
    subjective_module.log("choose judging group populate");

    const judgingGroupsContainer = document.getElementById("choose-judging-group_judging-groups");
    removeChildren(judgingGroupsContainer);

    const judgingGroups = subjective_module.getJudgingGroups();
    for (const group of judgingGroups) {
        const button = document.createElement("a");
        judgingGroupsContainer.appendChild(button);
        button.classList.add("wide");
        button.classList.add("center");
        button.classList.add("fll-sw-button");
        button.innerText = group;
        button.addEventListener('click', function() {
            subjective_module.setCurrentJudgingGroup(group);
            updateMainHeader();
            window.location = "#choose-category";
        });
    }
}

function populateChooseCategory() {
    const container = document.getElementById("choose-category_categories");
    removeChildren(container);

    const categories = subjective_module.getSubjectiveCategories();
    for (const category of categories) {
        let columns = subjective_module
            .getScheduleColumnsForCategory(category.name);

        if (columns.length < 1) {
            // in case there isn't a schedule we need columns to have at least 1 element
            columns = [null];
        }

        for (const column of columns) {
            const button = document.createElement("a");
            container.appendChild(button);
            button.classList.add("wide");
            button.classList.add("center");
            button.classList.add("fll-sw-button");

            if (columns.length > 1) {
                button.innerText = category.title + " - " + column;
            } else {
                button.innerText = category.title;
            }
            button.addEventListener('click', function() {
                subjective_module.setCurrentCategory(category, column);
                updateMainHeader();
                window.location = "#choose-judge";
            });
        }
    }
}

function populateChooseJudge() {
    document.getElementById("choose-judge_new-judge-name").classList.add('fll-sw-ui-inactive');

    const container = document.getElementById("choose-judge_judges")
    removeChildren(container);

    const newJudgeLabel = document.createElement("label");
    container.appendChild(newJudgeLabel);
    newJudgeLabel.classList.add("wide");
    newJudgeLabel.classList.add("fll-sw-button");

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


    let currentJudgeId = subjective_module.getCurrentJudgeId();
    let currentJudgeValid = false;
    const seenJudges = [];
    const judges = subjective_module.getPossibleJudges();
    for (const judge of judges) {
        if (!seenJudges.includes(judge.id)) {
            seenJudges.push(judge.id);

            const judgeLabel = document.createElement("label");
            container.appendChild(judgeLabel);
            judgeLabel.classList.add("wide");
            judgeLabel.classList.add("fll-sw-button");

            const judgeInput = document.createElement("input");
            judgeLabel.appendChild(judgeInput);
            judgeInput.setAttribute("type", "radio");
            judgeInput.setAttribute("name", "judge");
            judgeInput.setAttribute("value", judge.id);

            const judgeSpan = document.createElement("span");
            judgeLabel.appendChild(judgeSpan);
            judgeSpan.innerText = judge.id;

            if (currentJudgeId == judge.id) {
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
    if (null == judgeID) {
        return;
    }

    if ('new-judge' == judgeID) {
        judgeID = document.getElementById("choose-judge_new-judge-name").value;
        if (null == judgeID || "" == judgeID.trim()) {
            document.getElementById('alert-dialog_text').innerText = "You must enter a name";
            document.getElementById('alert-dialog').classList.remove("fll-sw-ui-inactive");
            return;
        }
        judgeID = judgeID.trim().toUpperCase();

        subjective_module.addJudge(judgeID);
        document.getElementById("side-panel_final-scores").classList.remove("fll-sw-button-pressed");
    } else {
        // sync final checkbox with current state of the judge
        const judge = subjective_module.getJudge(judgeID);
        if (!judge) {
            throw new Error(`ERROR: Unable to find existing judge with ID ${judgeID}.`)
        }
        if (judge.finalScores) {
            document.getElementById("side-panel_final-scores").classList.add("fll-sw-button-pressed");
        } else {
            document.getElementById("side-panel_final-scores").classList.remove("fll-sw-button-pressed");
        }
    }

    subjective_module.setCurrentJudgeId(judgeID);
    document.getElementById("side-panel_final-scores").classList.remove("fll-sw-ui-inactive");

    location.href = '#teams-list';
}

function selectTeam(team) {
    subjective_module.setCurrentTeam(team);

    subjective_module.setScoreEntryBackPage("#teams-list");
    window.location = "#enter-score";
}

function populateTeams() {
    const teamsList = document.getElementById("teams-list_teams");
    removeChildren(teamsList);

    const teams = subjective_module.getCurrentTeams();
    for (const team of teams) {
        const time = subjective_module.getScheduledTime(team.teamNumber);
        let timeStr = null;
        if (null != time) {
            timeStr = time.format(timeFormatter);
        }

        const button = document.createElement("a");
        teamsList.appendChild(button);
        button.classList.add("wide");
        button.classList.add("fll-sw-button");

        if (null != timeStr) {
            const span = document.createElement("span");
            button.appendChild(span);
            span.innerText = timeStr + " - ";
        }

        const score = subjective_module.getScore(team.teamNumber);
        if (!subjective_module.isScoreCompleted(score)) {
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
            const computedScore = subjective_module.computeScore(score);
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
    score.judge = subjective_module.getCurrentJudgeId;
    score.teamNumber = subjective_module.getCurrentTeam().teamNumber;
    score.note = null;
    score.goalComments = {};
    score.commentThinkAbout = null;
    score.commentGreatJob = null;

    return score;
}

function getGoalTextId(goal) {
    return "enter-score-comment-" + goal.name + "-text";
}

function getGoalDisplayCommentsId(goal) {
    return "enter-score-comment-" + goal.name + "-display";
}

/**
 * Save the state of the current page's goals to the specified score object. If
 * null, do nothing.
 */
function saveToScoreObject(score) {
    if (null == score) {
        return;
    }

    for (const goal of subjective_module.getCurrentCategory().allGoals) {
        if (goal.enumerated) {
            document.getElementById('alert-dialog_text').innerText = "Enumerated goals not supported: " + goal.name;
            document.getElementById('alert-dialog').classList.remove("fll-sw-ui-inactive");
        } else {
            const subscore = Number(document.getElementById(getScoreItemName(goal)).value);
            score.standardSubScores[goal.name] = subscore;

            if (null == score.goalComments) {
                score.goalComments = {};
            }
            const goalComment = document.getElementById(getGoalTextId(goal)).value;
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
    for (const goal of subjective_module.getCurrentCategory().allGoals) {
        if (goal.enumerated) {
            document.getElementById('alert-dialog_text').innerText = "Enumerated goals not supported: " + goal.name;
            document.getElementById('alert-dialog').classList.remove("fll-sw-ui-inactive");
        } else {
            const subscore = Number(document.getElementById(getScoreItemName(goal)).value);
            const multiplier = Number(goal.multiplier);
            total = total + subscore * multiplier;
        }
    }
    document.getElementById("enter-score_total-score").innerText = total;
}

function addGoalHeaderToScoreEntry(table, totalColumns, goal, rowClass) {
    const goalDescriptionRow = document.createElement("tr");
    goalDescriptionRow.classList.add(rowClass);

    const goalDescriptionCell = document.createElement("td");
    goalDescriptionCell.setAttribute("colspan", totalColumns);
    const goalDescTable = document.createElement("table");
    const goalDescRow = document.createElement("tr");

    const goalTitle = document.createElement("td");
    goalDescRow.appendChild(goalTitle);
    goalTitle.innerText = goal.title;
    goalTitle.style.width = "25%";
    goalTitle.classList.add("goal-title");

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

function addRubricToScoreEntry(table, goal, goalComment, ranges, rowClass) {

    const row = document.createElement("tr");
    row.classList.add(rowClass);

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
            commentButton.classList.add("fll-sw-button");

            const popup = document.createElement("div");
            popup.classList.add("fll-sw-ui-dialog");
            popup.classList.add("fll-sw-ui-inactive");
            popup.id = "enter-score-comment-" + goal.name;

            const popupContent = document.createElement("div");
            popup.appendChild(popupContent);
            popupContent.classList.add('comment-dialog')

            const textarea = document.createElement("textarea");
            popupContent.appendChild(textarea);
            textarea.id = getGoalTextId(goal);
            textarea.classList.add('comment-text');
            textarea.setAttribute("rows", "20");
            textarea.setAttribute("cols", "60");
            if (!isBlank(goalComment)) {
                textarea.value = goalComment;
                commentButton.classList.add("comment-entered");
            }

            const closeButton = document.createElement("button");
            popupContent.appendChild(closeButton);
            closeButton.setAttribute("type", "button");
            closeButton.id = "enter-score-comment-" + goal.name + "-close";
            closeButton.innerText = 'Done';

            document.getElementById("enter-score-goal-comments").appendChild(popup);

            closeButton.addEventListener("click", function() {
                popup.classList.add("fll-sw-ui-inactive");

                const comment = textarea.value;
                if (!isBlank(comment)) {
                    commentButton.classList.add("comment-entered");
                } else {
                    commentButton.classList.remove("comment-entered");
                }

                // make comment available for display on main page
                document.getElementById(getGoalDisplayCommentsId(goal)).innerText = comment;
            });

            commentButton.addEventListener("click", function() {
                // position the dialog so that it allows the judge to see the goal being commented on
                const rowRect = row.getBoundingClientRect();
                if (rowRect.top >= window.innerHeight / 2) {
                    const height = rowRect.top - 140;
                    popupContent.style.marginTop = "10px";
                    popupContent.style.height = height + "px";
                } else {
                    const offset = rowRect.top + rowRect.height + 40;
                    const height = window.innerHeight - offset - 40;
                    popupContent.style.marginTop = offset + "px";
                    popupContent.style.height = height + "px";
                }
                
                popup.classList.remove("fll-sw-ui-inactive");
                textarea.focus();
            });

        } else {
            cell.classList.add("border-right");
        }

    }

    table.appendChild(row);
}

function addSliderToScoreEntry(table, goal, totalColumns, ranges, subscore, rowClass) {
    let initValue;
    if (null == subscore) {
        initValue = goal.initialValue;
    } else {
        initValue = subscore;
    }

    const row = document.createElement("tr");
    row.classList.add(rowClass);
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

    cell.appendChild(slider);

    row.appendChild(cell);

    table.appendChild(row);

    highlightRubric(goal, ranges, initValue);
}

function setSliderTicks(goal) {
    const sliderId = getScoreItemName(goal);

    const sliderContainer = document.getElementById(sliderId + "-container");

    const max = goal.max;
    const min = goal.min;
    const offsetPercent = 0.5; // handle space at the start of the range
    const spacing = (100 - offsetPercent - offsetPercent) / (max - min);

    /*
     * subjective_module.log("Creating slider ticks for " + goal.name + " min: " + min + "
     * max: " + max + " spacing: " + spacing);
     */

    for (let i = 0; i <= max - min; i++) {
        const tick = document.createElement("span");
        tick.classList.add("sliderTickMark");
        //tick.innerHtml = "&nbsp;";
        tick.style.left = ((spacing * i) + offsetPercent) + '%';
        sliderContainer.appendChild(tick);
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
    const sliderId = getScoreItemName(goal);
    const slider = document.getElementById(sliderId);

    setSliderTicks(goal);

    slider.addEventListener('change', function() {
        const value = slider.value;
        highlightRubric(goal, ranges, value);

        recomputeTotal();
    });
}


/**
 * Create the rows for a single goal.
 */
function createScoreRows(table, totalColumns, score, goal, rowClass) {
    let goalScore = null;
    if (subjective_module.isScoreCompleted(score)) {
        goalScore = score.standardSubScores[goal.name];
    }

    let goalComment;
    if (score && score.goalComments) {
        goalComment = score.goalComments[goal.name];
    } else {
        goalComment = "";
    }

    addGoalHeaderToScoreEntry(table, totalColumns, goal, rowClass);

    const ranges = goal.rubric;
    ranges.sort(rangeSort);

    addRubricToScoreEntry(table, goal, goalComment, ranges, rowClass);

    addSliderToScoreEntry(table, goal, totalColumns, ranges, goalScore, rowClass);

    const commentsRow = document.createElement("tr");
    commentsRow.classList.add(rowClass);
    table.appendChild(commentsRow);
    commentsRow.classList.add("comments-display")
    const commentsCell = document.createElement("td");
    commentsRow.appendChild(commentsCell);
    commentsCell.setAttribute("colspan", totalColumns);
    commentsCell.id = getGoalDisplayCommentsId(goal);
    commentsCell.innerText = goalComment;

    const dividerRow = document.createElement("tr");
    table.appendChild(dividerRow);

    const cell = document.createElement("td");
    dividerRow.appendChild(cell);
    cell.setAttribute("colspan", totalColumns);
    cell.appendChild(document.createElement("hr"));

    addEventsToSlider(goal, ranges);

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
    const firstGoal = subjective_module.getCurrentCategory().allGoals[0];

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
function createGoalGroupRows(table, totalColumns, score, goalGroup, rowClass) {
    let groupText = goalGroup.title;
    if (goalGroup.description) {
        groupText = groupText + " - " + goalGroup.description;
    }

    const barRow = document.createElement("tr");
    table.appendChild(barRow);
    const barCell = document.createElement("td");
    barRow.appendChild(barCell);
    barCell.setAttribute("colspan", totalColumns);
    barCell.classList.add("goal-group-title");
    barCell.innerText = groupText;

    for (const goal of goalGroup.goals) {
        if (goal.enumerated) {
            document.getElementById('alert-dialog_text').innerText = "Enumerated goals not supported: " + goal.name;
            document.getElementById('alert-dialog').classList.remove("fll-sw-ui-inactive");
        } else {
            createScoreRows(table, totalColumns, score, goal, rowClass);
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
    document.getElementById("enter-score-comment-great-job-display").innerText = comment;
}

function updateThinkAboutButtonBackground() {
    const button = document.getElementById("enter-score-comment-think-about-button");
    const comment = document.getElementById("enter-score-comment-think-about-text").value;
    if (!isBlank(comment)) {
        button.classList.add("comment-entered");
    } else {
        button.classList.remove("comment-entered");
    }
    document.getElementById("enter-score-comment-think-about-display").innerText = comment;
}


/**
 * Save the score to storage and go back the to the score entry back page.
 */
function saveScore() {
    var currentTeam = subjective_module.getCurrentTeam();
    var score = subjective_module.getScore(currentTeam.teamNumber);
    if (null == score) {
        score = createNewScore();
    }
    score.modified = true;
    score.deleted = false;
    score.noShow = false;

    score.note = document.getElementById("enter-score-note-text").value;
    subjective_module.log("note text: " + score.note);
    score.commentGreatJob = document.getElementById("enter-score-comment-great-job-text").value;
    score.commentThinkAbout = document.getElementById("enter-score-comment-think-about-text").value;

    saveToScoreObject(score);

    subjective_module.saveScore(score);

    // save non-numeric nominations
    score.nonNumericNominations = [];
    subjective_module.getCurrentCategory().nominates.forEach(function(nominate, index, _) {
        const checkbox = document.getElementById("enter-score_nominate_" + index);
        if (checkbox.checked) {
            score.nonNumericNominations.push(nominate.nonNumericCategoryTitle);
        }
    });

    window.location = subjective_module.getScoreEntryBackPage();
}

/**
 * Save a no show and go back to the score entry back page.
 */
function enterNoShow() {
    var currentTeam = subjective_module.getCurrentTeam();
    var score = subjective_module.getScore(currentTeam.teamNumber);
    if (null == score) {
        score = createNewScore();
    }
    score.modified = true;
    score.noShow = true;
    score.deleted = false;
    subjective_module.saveScore(score);

    window.location = subjective_module.getScoreEntryBackPage();
}

/**
 * Install warning for user.
 */
function installWarnOnReload() {
    console.log("Installing warning on reload");
    window.onbeforeunload = function() {
        // most browsers won't show the custom message, but we can try
        // returning anything other than undefined will cause the user to be prompted
        return "Are you sure you want to leave? You will likely lose data.";
    };
}

/**
 * Remove the user warning, if the server is online
 */
function uninstallWarnOnReload() {
    if (server_online) {
        window.onbeforeunload = undefined;
    }
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

    const teams = subjective_module.getCurrentTeams();
    for (const team of teams) {
        const score = subjective_module.getScore(team.teamNumber);
        if (subjective_module.isScoreCompleted(score)) {
            teamsWithScores.push(team);

            const computedScore = subjective_module.computeScore(score);
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
        const score = subjective_module.getScore(team.teamNumber);

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
            scoreBlock.classList.add("no-show");
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
        if (score && score.nonNumericNominations && score.nonNumericNominations.length > 0) {
            nominations = " - " + score.nonNumericNominations.join(", ");
        }

        teamBlock.innerText = rank + " - #" + team.teamNumber + "  - " + team.teamName + nominations;

        const editButton = document.createElement("button");
        rightBlock.appendChild(editButton);
        editButton.innerText = "Edit";
        editButton.classList.add("score-summary-right-elements");

        editButton.addEventListener("click", function() {
            subjective_module.setCurrentTeam(team);

            subjective_module.setScoreEntryBackPage("#score-summary");
            window.location = "#enter-score";
        });


        if (score && score.note) {
            const noteRow = document.createElement("div");
            scoreSummaryContent.appendChild(noteRow);
            noteRow.classList.add("score-summary_note");
            noteRow.classList.add("fll-sw-ui-inactive");
            noteRow.innerText = score.note;
        }

        const commentRow = document.createElement("div");
        scoreSummaryContent.appendChild(commentRow);
        commentRow.classList.add("score-summary_comment");
        commentRow.classList.add("fll-sw-ui-inactive");
        if (score && score.commentGreatJob) {
            const row = document.createElement("div");
            commentRow.appendChild(row);
            row.innerText = score.commentGreatJob;
        }
        if (score && score.commentThinkAbout) {
            const row = document.createElement("div");
            commentRow.appendChild(row);
            row.innerText = score.commentThinkAbout;
        }
        if (score && score.goalComments) {
            for (const [_, goalComment] of Object.entries(score.goalComments)) {
                const row = document.createElement("div");
                commentRow.appendChild(row);
                row.innerText = goalComment;
            }
        }

        scoreSummaryContent.appendChild(document.createElement("hr"));

        prevScore = computedScore;
    }
}

function updateMainHeader() {
    const tournament = subjective_module.getTournament();
    let tournamentName;
    if (null == tournament) {
        tournamentName = "No Tournament";
    } else {
        tournamentName = tournament.name;
    }
    document.getElementById("header-main_tournament-name").innerText = tournamentName;

    document.getElementById("header-main_judging-group-name").innerText = subjective_module.getCurrentJudgingGroup();

    const category = subjective_module.getCurrentCategory();
    let categoryTitle;
    if (null == category) {
        categoryTitle = "No Category";
    } else {
        const columns = subjective_module
            .getScheduleColumnsForCategory(category.name);
        if (columns.length > 1) {
            categoryTitle = category.title + " " + subjective_module.getCurrentCategoryColumn();
        } else {
            categoryTitle = category.title;
        }
    }
    document.getElementById("header-main_category-name").innerText = categoryTitle;

    const judgeId = subjective_module.getCurrentJudgeId();
    let judgeName;
    if (judgeId) {
        judgeName = judgeId;
    } else {
        judgeName = "No Judge";
    }
    document.getElementById("header-main_judge-name").innerText = judgeName;
}

/**
 * Enable the specified footer element and hide all other footer elements.
 */
function enableFooter(footerElement) {
    enableContentElement(footerElement, 'footer');
}

/**
 * Enable the specified header element and hide all other header elements.
 */
function enableHeader(headerElement) {
    enableContentElement(headerElement, 'header');
}

/**
 * Enable the specified content element and hide all other content elements.
 */
function enableContent(contentElement) {
    enableContentElement(contentElement, 'main');
}

function enableContentElement(element, tagName) {
    for (const e of document.getElementsByTagName(tagName)) {
        for (const child of e.children) {
            if (child == element) {
                child.classList.remove('fll-sw-ui-inactive');
            } else {
                child.classList.add('fll-sw-ui-inactive');
            }
        }
    }
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

    enableHeader(header);
    enableContent(content);
    enableFooter(footer);
}

function displayPageTop() {
    subjective_module.setCurrentJudgeId(null);
    document.getElementById("side-panel_final-scores").classList.add("fll-sw-ui-inactive");
    document.getElementById("side-panel_final-scores").classList.remove("fll-sw-button-pressed");


    if (!server_online) {
        document.getElementById('alert-dialog_text').innerText = "Server is offline, cannot reload the application.";
        document.getElementById('alert-dialog').classList.remove("fll-sw-ui-inactive");
        return;
    }

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

    document.getElementById("wait-dialog").classList.remove("fll-sw-ui-inactive");
    subjective_module.checkServerStatus(true, serverLoadPage, function() {
        server_online = false;
        postServerStatusCallback();
        promptForJudgingGroup()
    });
    updateMainHeader();
}

function displayPageChooseJudgingGroup() {
    subjective_module.setCurrentJudgeId(null);
    document.getElementById("side-panel_final-scores").classList.add("fll-sw-ui-inactive");
    document.getElementById("side-panel_final-scores").classList.remove("fll-sw-button-pressed");

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
    subjective_module.setCurrentJudgeId(null);
    document.getElementById("side-panel_final-scores").classList.add("fll-sw-ui-inactive");
    document.getElementById("side-panel_final-scores").classList.remove("fll-sw-button-pressed");


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
    subjective_module.setCurrentJudgeId(null);
    document.getElementById("side-panel_final-scores").classList.add("fll-sw-ui-inactive");
    document.getElementById("side-panel_final-scores").classList.remove("fll-sw-button-pressed");

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

    displayPage(document.getElementById("header-main"), document.getElementById("content-teams-list"), document.getElementById("footer-entry"));

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
    document.getElementById("header-main_title").innerText = "Score Summary";

    displayPage(document.getElementById("header-main"), document.getElementById("content-score-summary"), document.getElementById("footer-summary"));

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
    // populate the header before displaying the page to ensure that the header height is properly computed
    const currentTeam = subjective_module.getCurrentTeam();
    document.getElementById("enter-score_team-number").innerText = currentTeam.teamNumber;
    document.getElementById("enter-score_team-name").innerText = currentTeam.teamName;

    const time = subjective_module.getScheduledTime(currentTeam.teamNumber);
    if (null != time) {
        const timeStr = time.format(timeFormatter);
        document.getElementById("enter-score_scheduled-time").innerText = timeStr;
    } else {
        document.getElementById("enter-score_scheduled-time").innerText = "";
    }

    const score = subjective_module.getScore(currentTeam.teamNumber);
    if (null != score) {
        document.getElementById("enter-score-note-text").value = score.note;
        document.getElementById("enter-score-comment-great-job-text").value = score.commentGreatJob;
        document.getElementById("enter-score-comment-great-job-display").innerText = score.commentGreatJob;
        document.getElementById("enter-score-comment-think-about-text").value =
            score.commentThinkAbout;
        document.getElementById("enter-score-comment-think-about-display").innerText = score.commentThinkAbout;
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

    let rowClass = "odd";
    for (const ge of subjective_module.getCurrentCategory().goalElements) {
        if (ge.goalGroup) {
            createGoalGroupRows(table, totalColumns, score, ge, rowClass);
        } else if (ge.goal) {
            if (ge.enumerated) {
                document.getElementById('alert-dialog_text').innerText = "Enumerated goals not supported: " + goal.name;
                document.getElementById('alert-dialog').classList.remove("fll-sw-ui-inactive");
            } else {
                createScoreRows(table, totalColumns, score, ge, rowClass);
            }
        }
        if (rowClass == "odd") {
            rowClass = "even";
        } else {
            rowClass = "odd";
        }
    }

    // add the non-numeric categories that teams can be nominated for
    const nominatesContainer = document.getElementById("enter-score_nominates");
    removeChildren(nominatesContainer);
    subjective_module.getCurrentCategory().nominates.forEach(function(nominate, index, _) {
        const nominateRow = document.createElement("div");
        nominatesContainer.appendChild(nominateRow);

        const nominateCheckLabel = document.createElement("label");
        nominateRow.appendChild(nominateCheckLabel);
        nominateCheckLabel.classList.add("nominate-label");

        const checkbox = document.createElement("input");
        nominateCheckLabel.appendChild(checkbox);
        checkbox.setAttribute("type", "checkbox");
        checkbox.id = "enter-score_nominate_" + index;

        const nominateCheckText = document.createElement("span");
        nominateCheckLabel.appendChild(nominateCheckText);
        nominateCheckText.innerText = nominate.nonNumericCategoryTitle;

        if (null != score
            && score.nonNumericNominations
                .includes(nominate.nonNumericCategoryTitle)) {
            checkbox.checked = true;
        }

        const nominateDescription = document.createElement("span");
        nominateRow.appendChild(nominateDescription);
        nominateDescription.classList.add("nominate-description");
        const nonNumericCategory = subjective_module.getNonNumericCategory(nominate.nonNumericCategoryTitle);
        if (nonNumericCategory) {
            nominateDescription.innerText = nonNumericCategory.description;
        }
    });

    updateGreatJobButtonBackground();
    updateThinkAboutButtonBackground();

    recomputeTotal();

    // hide comments by default
    hideScoreEntryComments();

    // needs to be after displayPage as that uninstalls the warning
    installWarnOnReload();
}

function synchronizeData() {
    const waitDialog = document.getElementById("wait-dialog");

    subjective_module.uploadData(function(result) {
        // scoresSuccess
        document.getElementById('alert-dialog_text').innerText = "Uploaded " + result.numModified + " scores."
            + result.message;
        document.getElementById('alert-dialog').classList.remove("fll-sw-ui-inactive");
    }, //
        function(result) {
            // scoresFail

            let message;
            if (null == result) {
                message = "Unknown server error";
            } else {
                message = result.message;
            }

            document.getElementById('alert-dialog_text').innerText = "Failed to upload scores: " + message;
            document.getElementById('alert-dialog').classList.remove("fll-sw-ui-inactive");
        }, //
        function(result) {
            // judgesSuccess
            subjective_module.log("Judges modified: " + result.numModifiedJudges
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

            document.getElementById('alert-dialog_text').innerText = "Failed to upload judges: " + message
            document.getElementById('alert-dialog').classList.remove("fll-sw-ui-inactive");
        }, //
        function() {
            // loadSuccess
            populateChooseJudgingGroup();
            waitDialog.classList.add("fll-sw-ui-inactive");
        }, //
        function(message) {
            // loadFail
            populateChooseJudgingGroup();

            waitDialog.classList.add("fll-sw-ui-inactive");

            document.getElementById('alert-dialog_text').innerText = "Failed to load scores from server: " + message
            document.getElementById('alert-dialog').classList.remove("fll-sw-ui-inactive");
        });
}

function setupAfterContentLoaded() {
    const sidePanel = document.getElementById("side-panel");

    // handlers for buttons and links that don't navigate to another page

    document.querySelectorAll('.side-panel_open')
        .forEach(element => {
            element.addEventListener('click', () => {
                sidePanel.classList.add('open');
            });
        })

    document.getElementById("side-panel_close").addEventListener('click', () => {
        sidePanel.classList.remove('open');
    });

    const sidePanelFinalScores = document.getElementById("side-panel_final-scores");
    sidePanelFinalScores.addEventListener('click', () => {
        const syncFinalDialog = document.getElementById("sync-final-question");
        syncFinalDialog.classList.remove("fll-sw-ui-inactive");
        sidePanel.classList.remove('open');
    });

    document.getElementById("sync-final-question_yes").addEventListener('click', () => {
        const syncFinalDialog = document.getElementById("sync-final-question");
        syncFinalDialog.classList.add("fll-sw-ui-inactive");

        // block the user while uploading
        const waitDialog = document.getElementById("wait-dialog");
        waitDialog.classList.remove("fll-sw-ui-inactive");

        const judgeId = subjective_module.getCurrentJudgeId();
        if (judgeId) {
            const judge = subjective_module.getJudge(judgeId);
            if (judge) {
                judge.finalScores = true;
                sidePanelFinalScores.classList.add("fll-sw-button-pressed");
                subjective_module.save();
            } else {
                throw new Error(`ERROR: Cannot find judge with id ${judgeId} while saving finalScores flag.`);
            }
        } else {
            throw new Error("ERROR: No current judge found, cannot save finalScores flag.");
        }

        synchronizeData();
    });

    document.getElementById("sync-final-question_no").addEventListener('click', () => {
        const syncFinalDialog = document.getElementById("sync-final-question");
        syncFinalDialog.classList.add("fll-sw-ui-inactive");
    });


    document.getElementById("side-panel_synchronize").addEventListener('click', () => {
        sidePanel.classList.remove('open');

        subjective_module.checkServerStatus(true, function() {
            server_online = true;
            postServerStatusCallback();

            // block the user while uploading
            const waitDialog = document.getElementById("wait-dialog");
            waitDialog.classList.remove("fll-sw-ui-inactive");
            synchronizeData();
        },
            function() {
                server_online = false;
                postServerStatusCallback();

                document.getElementById('alert-dialog_text').innerText = "Server is offline, cannot synchronize.";
                document.getElementById('alert-dialog').classList.remove("fll-sw-ui-inactive");
            });
    });

    document.getElementById("side-panel_offline-download").addEventListener('click', () => {
        sidePanel.classList.remove('open');
        setOfflineDownloadUrl(document.getElementById("side-panel_offline-download"));
    });


    document.getElementById("enter-score_save").addEventListener('click', () => {
        console.log("Saving score");

        const totalScore = parseInt(document.getElementById("enter-score_total-score").innerText);
        if (totalScore == 0) {
            document.getElementById("enter-score_confirm-zero").classList.remove("fll-sw-ui-inactive");
        } else {
            saveScore();
        }
    });

    document.getElementById("enter-score_cancel").addEventListener('click', () => {
        console.log("Canceling score entry");
        window.location = subjective_module.getScoreEntryBackPage();
    });

    document.getElementById("enter-score_delete").addEventListener('click', () => {
        console.log("Deleting score");

        document.getElementById("confirm-delete-dialog").classList.remove("fll-sw-ui-inactive");
    });
    document.getElementById("confirm-delete-dialog_yes").addEventListener("click", () => {
        document.getElementById("confirm-delete-dialog").classList.add("fll-sw-ui-inactive");

        const currentTeam = subjective_module.getCurrentTeam();
        let score = subjective_module.getScore(currentTeam.teamNumber);
        if (null == score) {
            score = createNewScore();
        }
        score.modified = true;
        score.noShow = false;
        score.deleted = true;
        subjective_module.saveScore(score);

        window.location = subjective_module.getScoreEntryBackPage();
    });
    document.getElementById("confirm-delete-dialog_no").addEventListener("click", () => {
        document.getElementById("confirm-delete-dialog").classList.add("fll-sw-ui-inactive");
    });

    document.getElementById("enter-score_no-show").addEventListener('click', () => {
        console.log("Mark no show");
        document.getElementById("confirm-noshow-dialog").classList.remove("fll-sw-ui-inactive");
    });
    document.getElementById("confirm-noshow-dialog_yes").addEventListener("click", () => {
        document.getElementById("confirm-noshow-dialog").classList.add("fll-sw-ui-inactive");
        enterNoShow();
    });
    document.getElementById("confirm-noshow-dialog_no").addEventListener("click", () => {
        document.getElementById("confirm-noshow-dialog").classList.add("fll-sw-ui-inactive");
    });

    document.getElementById("enter-score_toggle-review-mode").addEventListener('click', function() {
        const glassPane = document.getElementById("review-mode_glasspane");
        if (glassPane.style.zIndex > 0) {
            glassPane.style.zIndex = -1;
            this.classList.remove("fll-sw-button-pressed");
        } else {
            glassPane.style.zIndex = 10;
            this.classList.add("fll-sw-button-pressed");
        }
    });

    document.getElementById("enter-score_show-comments").addEventListener('click', function() {
        toggleScoreEntryComments();
    });

    document.getElementById("enter-score_confirm-zero_yes").addEventListener('click', function() {
        document.getElementById("enter-score_confirm-zero").classList.add("fll-sw-ui-inactive");
        saveScore();
    });
    document.getElementById("enter-score_confirm-zero_no-show").addEventListener('click', function() {
        document.getElementById("enter-score_confirm-zero").classList.add("fll-sw-ui-inactive");
        enterNoShow();
    });

    document.getElementById("enter-score-comment-great-job-close").addEventListener('click', function() {
        document.getElementById("enter-score-comment-great-job").classList.add("fll-sw-ui-inactive");
        updateGreatJobButtonBackground();
    });
    document.getElementById("enter-score-comment-think-about-close").addEventListener('click', function() {
        document.getElementById("enter-score-comment-think-about").classList.add("fll-sw-ui-inactive");
        updateThinkAboutButtonBackground();
    });

    document.getElementById("enter-score-comment-great-job-button").addEventListener('click', function() {
        document.getElementById("enter-score-comment-great-job").classList.remove("fll-sw-ui-inactive");
        document.getElementById("enter-score-comment-great-job-text").focus();
    });

    document.getElementById("enter-score-comment-think-about-button").addEventListener('click', function() {
        document.getElementById("enter-score-comment-think-about").classList.remove("fll-sw-ui-inactive");
        document.getElementById("enter-score-comment-think-about-text").focus();
    });
    document.getElementById("enter-score-note-button").addEventListener('click', function() {
        document.getElementById("enter-score-note").classList.remove("fll-sw-ui-inactive");
        document.getElementById("enter-score-note-text").focus();
    });
    document.getElementById("enter-score-note-close").addEventListener('click', function() {
        document.getElementById("enter-score-note").classList.add("fll-sw-ui-inactive");
    });

    document.getElementById("choose-judge_submit").addEventListener('click', function() {
        setJudge();
    });

    document.getElementById("alert-dialog_close").addEventListener('click', function() {
        document.getElementById('alert-dialog').classList.add("fll-sw-ui-inactive");
        if (null != alertCallback) {
            alertCallback();
        }
        alertCallback = null;
    });

    document.getElementById("confirm-modified-scores-dialog_yes").addEventListener('click', function() {
        document.getElementById('confirm-modified-scores-dialog').classList.add("fll-sw-ui-inactive");
        reloadDataConfirmed();
    });
    document.getElementById("confirm-modified-scores-dialog_no").addEventListener('click', function() {
        document.getElementById('confirm-modified-scores-dialog').classList.add("fll-sw-ui-inactive");
    });

    document.getElementById("score-summary_show-comments").addEventListener('click', function() {
        toggleScoreSummaryComments();
    });

    document.getElementById("score-summary_show-notes").addEventListener('click', function() {
        toggleScoreSummaryNotes();
    });


    // navigate to pages when the anchor changes
    window.addEventListener('hashchange', navigateToPage);
}

function preventMultipleWindows(successFunction, failureFunction) {
    // don't let the user interact with things
    const waitDialog = document.getElementById("wait-dialog");
    waitDialog.classList.remove("fll-sw-ui-inactive");

    const active_window_key = "fll-sw.subjective.active-window";

    const uid = (Math.random() * 0xffffffff >>> 0);
    const bc = new BroadcastChannel("fll-sw.subjective.unique");
    let responseHandler = null;

    bc.onmessage = (event) => {
        const message = event.data;
        if (message.msg == "ping" && message.uid == uid) {
            const response = new Object();
            response.msg = "pong";
            response.uid = uid;
            bc.postMessage(response);
            alert("Please use this window");
        } else if (message.msg == "pong" && message.uid != uid) {
            console.log("Received response from: " + message.uid);
            if (responseHandler) {
                responseHandler();
            }
        }
    };

    window.addEventListener('pagehide', (_) => {
        const otherUid = localStorage.getItem(active_window_key);
        console.log("Unload called");
        if (otherUid == uid) {
            localStorage.removeItem(active_window_key);
            console.log("Unload cleared window");
        }
        bc.close();
    });

    const success = function() {
        localStorage.setItem(active_window_key, uid)
        waitDialog.classList.add("fll-sw-ui-inactive");
        successFunction();
    }


    const otherUid = localStorage.getItem(active_window_key);
    if (typeof otherUid == 'undefined' || otherUid == 'undefined' || otherUid == null) {
        console.log("No other window found");
        success();
    } else {
        responseHandler = function() {
            // clear the handler
            responseHandler = null;

            // tell the user something
            console.log("Found other window: " + otherUid);
            failureFunction();
        };

        const message = new Object();
        message.uid = otherUid;
        message.msg = "ping";
        bc.postMessage(message);
        // setup timeout for no response for 5 seconds
        setTimeout(() => {
            if (responseHandler) {
                responseHandler = null;
                console.log("Timed out waiting for response from other window, continuing with this window");
                success();
            }
        }, 5000);

    }
}

document.addEventListener("DOMContentLoaded", () => {
    setupAfterContentLoaded();
});

function hideScoreEntryComments() {
    const displayCommentsButton = document.getElementById("enter-score_show-comments");
    const hide = true;
    displayOrHideCommentsOrNotes(hide, displayCommentsButton, '.comments-display');
}

function toggleScoreEntryComments() {
    const displayCommentsButton = document.getElementById("enter-score_show-comments");
    const hide = displayCommentsButton.classList.contains("fll-sw-button-pressed");
    displayOrHideCommentsOrNotes(hide, displayCommentsButton, '.comments-display');
}

function toggleScoreSummaryComments() {
    const button = document.getElementById("score-summary_show-comments");
    const hide = button.classList.contains("fll-sw-button-pressed");
    displayOrHideCommentsOrNotes(hide, button, '.score-summary_comment');
}

function toggleScoreSummaryNotes() {
    const button = document.getElementById("score-summary_show-notes");
    const hide = button.classList.contains("fll-sw-button-pressed");
    displayOrHideCommentsOrNotes(hide, button, '.score-summary_note');
}

/**
 * @param hide if true, hide the comments or notes
 * @param button the button that is toggled
 * @param selector CSS selector to find all elements to toggle
 */
function displayOrHideCommentsOrNotes(hide, button, selector) {
    if (hide) {
        button.classList.remove("fll-sw-button-pressed");
    } else {
        button.classList.add("fll-sw-button-pressed");
    }

    for (const element of document.querySelectorAll(selector)) {
        if (hide) {
            element.classList.add("fll-sw-ui-inactive");
        } else {
            element.classList.remove("fll-sw-ui-inactive");
        }
    }
}

function postServerStatusCallback() {
    const sidePanelServerStatus = document.getElementById('side-panel_server-status');
    if (server_online) {
        uninstallWarnOnReload();
        sidePanelServerStatus.innerText = "Online";
        sidePanelServerStatus.classList.add("online");
    } else {
        installWarnOnReload();
        sidePanelServerStatus.innerText = "Offline";
        sidePanelServerStatus.classList.remove("online");
    }

    // schedule another update check in 30 seconds
    setTimeout(updateServerStatus, 30 * 1000);
}

function updateServerStatus() {
    subjective_module.checkServerStatus(false,
        () => {
            if (!server_online) {
                subjective_module.log("Server is now online");
            }
            server_online = true;
            postServerStatusCallback();
        },
        () => {
            if (server_online) {
                subjective_module.log("Server is now offline");
            }
            server_online = false;
            postServerStatusCallback();
        }
    );
}


// fires after DOMContentLoaded and all resources are loaded
window.addEventListener('load', () => {
    preventMultipleWindows(() => {
        // initial state
        updateMainHeader();
        navigateToPage();
        updateServerStatus();
    },
        () => {
            const message = "You already have the subjective application open in another window, this is not supported, please close this window!";
            alert(message);
            removeChildren(document.body);
            const messageElement = document.createElement("div");
            messageElement.innerText = message;
            document.body.appendChild(messageElement);
            window.close();
        });
});
