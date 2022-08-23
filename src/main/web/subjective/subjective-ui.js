/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

let alertCallback = null;

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
        const button = document.createElement("a");
        container.appendChild(button);
        button.classList.add("wide");
        button.classList.add("center");
        button.classList.add("fll-sw-button");
        button.innerText = category.title;
        button.addEventListener('click', function() {
            subjective_module.setCurrentCategory(category);
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


    let currentJudge = subjective_module.getCurrentJudge();
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
            document.getElementById('alert-dialog_text').innerText = "You must enter a name";
            document.getElementById('alert-dialog').classList.remove("fll-sw-ui-inactive");
            return;
        }
        judgeID = judgeID.trim().toUpperCase();

        subjective_module.addJudge(judgeID);
    }

    subjective_module.setCurrentJudge(judgeID);

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

    const timeFormatter = JSJoda.DateTimeFormatter.ofPattern("HH:mm");
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
    score.judge = subjective_module.getCurrentJudge().id;
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

function addRubricToScoreEntry(table, goal, goalComment, ranges) {

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
            });

            commentButton.addEventListener("click", function() {
                popup.classList.remove("fll-sw-ui-inactive");
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
function createScoreRows(table, totalColumns, score, goal) {
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

    addGoalHeaderToScoreEntry(table, totalColumns, goal);

    const ranges = goal.rubric;
    ranges.sort(rangeSort);

    addRubricToScoreEntry(table, goal, goalComment, ranges);

    addSliderToScoreEntry(table, goal, totalColumns, ranges, goalScore);

    const commentsRow = document.createElement("tr");
    table.appendChild(commentsRow);
    commentsRow.classList.add("comments-display")
    const commentsCell = document.createElement("td");
    commentsRow.appendChild(commentsCell);
    commentsCell.setAttribute("colspan", totalColumns);
    commentsCell.innerText = "comments go here";

    const row = document.createElement("tr");
    table.appendChild(row);

    const cell = document.createElement("td");
    row.appendChild(cell);
    cell.setAttribute("colspan", totalColumns);
    cell.innerHtml = "&nbsp;";

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
    barCell.classList.add("goal-group-title");
    barCell.innerText = groupText;

    for (const goal of goalGroup.goals) {
        if (goal.enumerated) {
            document.getElementById('alert-dialog_text').innerText = "Enumerated goals not supported: " + goal.name;
            document.getElementById('alert-dialog').classList.remove("fll-sw-ui-inactive");
        } else {
            createScoreRows(table, totalColumns, score, goal);
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


        const noteRow = document.createElement("div");
        if (score && score.note) {
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
        categoryTitle = category.title;
    }
    document.getElementById("header-main_category-name").innerText = categoryTitle;

    const judge = subjective_module.getCurrentJudge();
    let judgeName;
    if (null == judge) {
        judgeName = "No Judge";
    } else {
        judgeName = judge.id;
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
    subjective_module.checkServerStatus(serverLoadPage, promptForJudgingGroup);
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

    const score = subjective_module.getScore(currentTeam.teamNumber);
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

    for (const ge of subjective_module.getCurrentCategory().goalElements) {
        if (ge.goalGroup) {
            createGoalGroupRows(table, totalColumns, score, ge)
        } else if (ge.goal) {
            if (ge.enumerated) {
                document.getElementById('alert-dialog_text').innerText = "Enumerated goals not supported: " + goal.name;
                document.getElementById('alert-dialog').classList.remove("fll-sw-ui-inactive");
            } else {
                createScoreRows(table, totalColumns, score, ge);
            }
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
    hideComments();

    // needs to be after displayPage as that uninstalls the warning
    installWarnOnReload();
}


document.addEventListener("DOMContentLoaded", () => {
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

    document.getElementById("side-panel_synchronize").addEventListener('click', () => {
        sidePanel.classList.remove('open');

        const waitDialog = document.getElementById("wait-dialog");
        waitDialog.classList.remove("fll-sw-ui-inactive");

        subjective_module.uploadData(function(result) {
            // scoresSuccess
            document.getElementById('alert-dialog_text').innerText = "Uploaded " + result.numModified + " scores. message: "
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

    document.getElementById("enter-score_toggle-review-mode").addEventListener('click', () => {
        const glassPane = document.getElementById("review-mode_glasspane");
        if (glassPane.style.zIndex > 0) {
            glassPane.style.zIndex = -1;
        } else {
            glassPane.style.zIndex = 10;
        }
    });

    document.getElementById("enter-score_show-comments").addEventListener('click', function() {
        toggleComments();
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

    // navigate to pages when the anchor changes
    window.addEventListener('hashchange', navigateToPage);
});

function displayComments() {
    displayOrHideComments(false);
}

function hideComments() {
    displayOrHideComments(true);
}

function toggleComments() {
    const displayCommentsButton = document.getElementById("enter-score_show-comments");
    const hide = displayCommentsButton.classList.contains("fll-sw-button-pressed");
    displayOrHideComments(hide);
}

/**
 * @param hide if true, hide the comments
 */
function displayOrHideComments(hide) {
    const displayCommentsButton = document.getElementById("enter-score_show-comments");

    if (hide) {
        displayCommentsButton.classList.remove("fll-sw-button-pressed");
    } else {
        displayCommentsButton.classList.add("fll-sw-button-pressed");
    }

    for (const element of document.querySelectorAll('.comments-display')) {
        if (hide) {
            element.classList.add("fll-sw-ui-inactive");
        } else {
            element.classList.remove("fll-sw-ui-inactive");
        }
    }
}


window.addEventListener('load', () => {
    // initial state
    updateMainHeader();
    navigateToPage();
});
