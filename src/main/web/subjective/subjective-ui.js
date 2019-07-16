/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

function selectJudgingGroup(group) {
  $.subjective.setCurrentJudgingGroup(group);
  $.mobile.navigate("#choose-category-page");
}

$(document).on(
    "pageinit",
    "#choose-judging-group-page",
    function(event) {
      $.subjective.log("choose judging group pageinit");

      $("#choose-judging-group_version").text($.subjective.getVersion());

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
      $("#choose-category_version").text($.subjective.getVersion());

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
      $("#choose-judge_version").text($.subjective.getVersion());

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
          "<input type='radio' name='judge' id='choose-judge_new-judge' value='new-judge'>");
  $("#choose-judge_judges").append(
      "<label for='choose-judge_new-judge'>New Judge</label>");

  var currentJudge = $.subjective.getCurrentJudge();
  var currentJudgeValid = false;
  var judges = $.subjective.getPossibleJudges();
  $.each(judges, function(i, judge) {
    if (null != currentJudge && currentJudge.id == judge.id) {
      currentJudgeValid = true;
    }
    $("#choose-judge_judges").append(
        "<input type='radio' name='judge' id='choose-judge_" + judge.id
            + "' value='" + judge.id + "'>");
    $("#choose-judge_judges").append(
        "<label for='choose-judge_" + judge.id + "'>" + judge.id + "</label>");
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
      $("#teams-list_version").text($.subjective.getVersion());

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
 * Save the state of the current page to the specified score object. If null, do
 * nothing.
 */
function saveToScoreObject(score) {
  if (null == score) {
    return;
  }

  $.each($.subjective.getCurrentCategory().goals, function(index, goal) {
    if (goal.enumerated) {
      alert("Enumerated goals not supported: " + goal.name);
    } else {
      var subscore = Number($("#enter-score_" + goal.name).val());
      score.standardSubScores[goal.name] = subscore;
    }
  });
}

function recomputeTotal() {
  var currentTeam = $.subjective.getCurrentTeam();
  var score = $.subjective.getScore(currentTeam.teamNumber);

  var total = 0;
  $.each($.subjective.getCurrentCategory().goals, function(index, goal) {
    if (goal.enumerated) {
      alert("Enumerated goals not supported: " + goal.name);
    } else {
      var subscore = Number($("#enter-score_" + goal.name).val());
      var multiplier = Number(goal.multiplier);
      total = total + subscore * multiplier;
    }
  });
  $("#enter-score_total-score").text(total);
}

function createScoreRows(table, totalColumns, goal, subscore) {
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
  
  /*
  var row = $("<div class=\"ui-grid-b ui-responsive\"></div>");

  var categoryBlock = $("<div class=\"ui-block-a\"></div>");
  var categoryLabel = $("<p>" + goal.category + "</p>");
  categoryBlock.append(categoryLabel);
  row.append(categoryBlock);

  var titleBlock = $("<div class=\"ui-block-b\"></div>");
  var titleLabel = $("<p>" + goal.title + "</p>");
  titleBlock.append(titleLabel);
  row.append(titleBlock);

  var rightBlock = $("<div class=\"ui-block-c\"></div>");
  var rightContainer = $("<div class=\"ui-grid-a ui-responsive\"></div>");
  rightBlock.append(rightContainer);

  var scoreBlock = $("<div class=\"ui-block-a\"></div>");
  var scoreSelect = $("<select id=\"enter-score_" + goal.name + "\"></select>");
  scoreSelect.change(function() {
    recomputeTotal();
  });
  if (goal.scoreType == "INTEGER") {
    for (var v = Number(goal.min); v <= Number(goal.max); ++v) {
      var selected = "";
      if (null != subscore && subscore == v) {
        selected = "selected";
      }
      var option = $("<option value=\"" + v + "\" " + selected + " >" + v
          + "</option>");
      scoreSelect.append(option);
    }
  } else {
    alert("Non-integer goals are not supported: " + goal.name);
  }

  scoreBlock.append(scoreSelect);
  rightContainer.append(scoreBlock);

  var rubricBlock = $("<div class=\"ui-block-b\"></div>");
  rightContainer.append(rubricBlock);
  var rubricButton = $("<button class=\"ui-btn ui-corner-all ui-icon-arrow-r ui-btn-icon-notext ui-btn-inline\"></button>");
  rubricBlock.append(rubricButton);
  rubricButton.click(function() {
    var currentTeam = $.subjective.getCurrentTeam();
    var score = $.subjective.getScore(currentTeam.teamNumber);
    if (null == score) {
      score = createNewScore();
    }

    var scoreCopy = $.extend(true, {}, score);
    saveToScoreObject(scoreCopy);
    scoreCopy.deleted = false;
    $.subjective.setTempScore(scoreCopy);
    $.subjective.setCurrentGoal(goal);
    $.mobile.navigate("#rubric-page");
  });

  row.append(rightContainer);

  $("#enter-score_score-content").append(row);
  */
}

/**
 * 
 * @returns total number of columns to represent all scores in the rubric ranges
 */
function populateEnterScoreRubricTitles(table) {
  var firstGoal = $.subjective.getCurrentCategory().goals[0];
  
  var ranges = firstGoal.rubric;
  ranges.sort(rangeSort);

  var totalColumns = 0;
  
  var row = $("<tr></tr>");
  row.empty();
  $.each(ranges, function(index, range) {
    var numColumns = range.max - range.min + 1;
    var cell = $("<th colspan='" + numColumns + "'>" + range.title + "</th>");
    row.append(cell);
  
    totalColumns += numColumns;
  });
  
  table.append(row);
  
  return totalColumns;
}

$(document).on("pagebeforeshow", "#enter-score-page", function(event) {

  var currentTeam = $.subjective.getCurrentTeam();
  $("#enter-score_team-number").text(currentTeam.teamNumber);
  $("#enter-score_team-name").text(currentTeam.teamName);

  // load the saved score if needed
  var score = $.subjective.getTempScore();
  if (null == score) {
    score = $.subjective.getScore(currentTeam.teamNumber);
  }

  if (null != score) {
    $("#enter-score-note-text").val(score.note);
  } else {
    $("#enter-score-note-text").val("");
  }

  var table = $("#enter-score_score-table");
  table.empty();

  var totalColumns = populateEnterScoreRubricTitles(table);
  
  var prevCategory = null;
  $.each($.subjective.getCurrentCategory().goals, function(index, goal) {
    if (goal.enumerated) {
      alert("Enumerated goals not supported: " + goal.name);
    } else {
      var subscore = null;
      if ($.subjective.isScoreCompleted(score)) {
        subscore = score.standardSubScores[goal.name];
      }

      if (prevCategory != goal.category) {
        if (goal.category != null && "" != goal.category) {
          var bar = $("<tr><td colspan='" + totalColumns + "' class='ui-bar-a'>" + goal.category + "</td></tr>");
          table.append(bar);
        }
      }
      
      createScoreRows(table, totalColumns, goal, subscore);
      
      prevCategory = goal.category;
    }
  });

  
  
  /*    
  var prevCategory = null;
  $.each($.subjective.getCurrentCategory().goals, function(index, goal) {
    if (goal.enumerated) {
      alert("Enumerated goals not supported: " + goal.name);
    } else {
      var subscore = null;
      if ($.subjective.isScoreCompleted(score)) {
        subscore = score.standardSubScores[goal.name];
      }

      if (prevCategory != goal.category) {
        if (goal.category != null && "" != goal.category) {
          var separator = $("<div class='ui-bar-a'>" + goal.category + "</div>");
          $("#enter-score_score-content").append(separator);
        }
      }
      createScoreRow(goal, subscore);
      prevCategory = goal.category;
    }
  });

  recomputeTotal();

  // clear out temp state so that we don't get it again
  $.subjective.setTempScore(null);
  $.subjective.setCurrentGoal(null);
*/
  
  $("#enter-score-page").trigger("create");
});

$(document).on("pageinit", "#enter-score-page", function(event) {
  $("#enter-score_save-score").click(function() {

    var currentTeam = $.subjective.getCurrentTeam();
    var score = $.subjective.getScore(currentTeam.teamNumber);
    if (null == score) {
      score = createNewScore();
    }
    score.modified = true;
    score.deleted = false;
    score.noShow = false;

    var text = $("#enter-score-note-text").val();
    score.note = text;
    $.subjective.log("note text: " + score.note);

    saveToScoreObject(score);

    $.subjective.saveScore(score);

    $.mobile.navigate($.subjective.getScoreEntryBackPage());
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
  });

});

function setRubricScore(value) {
  $("#rubric-score").val(value).selectmenu("refresh", true);
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

function populateRubric(goal) {
  $("#rubric-content").empty();

  var ranges = goal.rubric;
  ranges.sort(rangeSort);

  var ndGrid = $("<div class=\"ui-grid-a\"></div>");
  var ndBlockA = $("<div class=\"ui-grid-a rubric-not-done\">Not Done</div>");
  var ndBlockB = $("<div class=\"ui-grid-b\"></div>");
  var ndButton = $("<button>Set</button>");
  ndButton.click(function() {
    setRubricScore(0);
  });
  ndBlockB.append(ndButton);
  ndGrid.append(ndBlockA);
  ndGrid.append(ndBlockB);
  $("#rubric-content").append(ndGrid);

  $.each(ranges, function(index, range) {
    var titleDiv = $("<div class=\"rubric-title\">" + range.title + " ("
        + range.min + "-" + range.max + ")<div>");
    $("#rubric-content").append(titleDiv);

    var grid = $("<div class=\"ui-grid-a\"></div>");
    var blockA = $("<div class=\"ui-grid-a\"></div>");
    blockA.text(range.fullDescription);

    var blockB = $("<div class=\"ui-grid-b\"></div>");
    var button = $("<button>Set</button>");
    button.click(function() {
      var mid = Math.floor((range.min + range.max) / 2);
      setRubricScore(mid);
    });
    blockB.append(button);

    grid.append(blockA);
    grid.append(blockB);
    $("#rubric-content").append(grid);

  });
}

$(document).on("pagebeforeshow", "#rubric-page", function(event) {

  var goal = $.subjective.getCurrentGoal();

  if (goal.scoreType == "INTEGER") {
    for (var v = Number(goal.min); v <= Number(goal.max); ++v) {
      var option = $("<option value=\"" + v + "\">" + v + "</option>");
      $("#rubric-score").append(option);
    }
  } else {
    alert("Non-integer goals are not supported: " + goal.name);
  }

  $("#rubric-category").text(goal.category);
  $("#rubric-goal-title").text(goal.title);
  $("#rubric-description").text(goal.description);

  populateRubric(goal);

  $("#rubric-page").trigger("create");
});

$(document).on("pageshow", "#rubric-page", function(event) {
  var score = $.subjective.getTempScore();
  var goal = $.subjective.getCurrentGoal();

  var subscore;
  if (goal.enumerated) {
    subscore = null;
    $.subjective.log("enumerated score: " + score.enumSubScores[goal.name]);
  } else {
    subscore = score.standardSubScores[goal.name];
  }
  setRubricScore(subscore);

});

$(document).on("pageinit", "#rubric-page", function(event) {
  $("#rubric-save-score").click(function() {
    var score = $.subjective.getTempScore();
    var goal = $.subjective.getCurrentGoal();
    if (goal.enumerated) {
      alert("Enumerated unsupported");
    } else {
      score.standardSubScores[goal.name] = $("#rubric-score").val();
      $.subjective.setTempScore(score);
      $.mobile.navigate("#enter-score-page");
    }

  });

  $("#rubric-cancel-score").click(function() {
    $.mobile.navigate("#enter-score-page");
  });

});

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
      $("#score-summary_version").text($.subjective.getVersion());

      $("#score-summary_upload-scores").click(
          function() {
            $.mobile.loading("show", {
              text : "Uploading Scores..."
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
