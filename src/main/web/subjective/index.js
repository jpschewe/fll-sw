/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

function loadData() {
  $.mobile.loading("show");

  $.subjective.loadFromServer(
      function() {
        subjectiveCategories = $.subjective.getSubjectiveCategories();

        $.mobile.loading("hide");
        $("#index-page_choose_clear").hide();

        if (0 == subjectiveCategories.length) {
          alert("No subjective data loaded from server");
        } else {
          $("#index-page_messages").append(
              "Loaded " + subjectiveCategories.length
                  + " categories from the server<br/>");
        }
        $("#index-page_messages").append(
            "Current tournament is " + $.subjective.getTournament().name
                + "<br/>");

        promptForJudgingGroup();
      }, function(message) {
        $.mobile.loading("hide");

        alert("Error getting data from server: " + message);
      });
}

function checkStoredData() {
  if ($.subjective.storedDataExists()) {
    checkTournament();
  } else {
    loadData();
  }
}

function promptForJudgingGroup() {
  $.mobile.navigate("#choose-judging-group-page");
}

function promptForReload() {
  $("#index-page_choose_clear").show();
}

function reloadData() {
  if ($.subjective.checkForModifiedScores()) {
    var answer = confirm("You have modified scores, this will remove them. Are you sure?")
    if (!answer) {
      return;
    }
  }
  $.subjective.clearAllData();
  loadData();
}

function checkTournament() {
  $.mobile.loading("show");

  $.subjective.getServerTournament(function(serverTournament) {
    $.mobile.loading("hide");

    var storedTournament = $.subjective.getTournament();
    if (null == storedTournament) {
      reloadData();
    } else if (storedTournament.name != serverTournament.name
        || storedTournament.tournamentID != serverTournament.tournamentID) {
      reloadData();
    } else {
      promptForReload();
    }
  }, function() {
    alert("Error getting data from server");
  });
}

function checkServerStatus() {
  $.mobile.loading("show");

  $.subjective.log("Checking server status");
  $.ajax({
    url : "../images/blank.gif",
    type : "GET",
    cache : false,
    timeout : 1000,
    success : function(response) {
      $.subjective.log("server online");
      $.subjective.checkServerVersion(serverLoadPage);
    },
    error : function(x, t, m) {
      $.subjective.log("server offline");

      promptForJudgingGroup();
    }
  });

}

function serverLoadPage() {
  $("#index-page_choose_clear").hide();

  $.getJSON("../api/CheckAuth", function(data) {
    $.subjective.log("data: " + $.toJSON(data));

    if (data.authenticated) {
      $("#index-page_clear").click(function() {
        $("#index-page_choose_clear").hide();
        reloadData();
      });
      $("#index-page_keep").click(function() {
        $("#index-page_choose_clear").hide();
        promptForJudgingGroup();
      });

      checkStoredData();
    } else {
      location.href = "Auth";
    }
  });

}

$(document).on("pagebeforeshow", "#index-page", function() {
  $.subjective.log("before page show index-page");

  $("#index-page_messages").empty();

  displayTournamentName($("#index-page_tournament"));
});

$(document).on("pageshow", "#index-page", function(event) {
  $.subjective.log("pageshow index-page");

  checkServerStatus();
});
