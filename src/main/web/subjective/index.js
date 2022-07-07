/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

function loadData() {
    const waitDialog = document.getElementById("wait-dialog");
    waitDialog.style.visibility = "visible";

    subjective_module.loadFromServer(
        function() {
            const subjectiveCategories = subjective_module.getSubjectiveCategories();

            waitDialog.style.visibility = "hidden";
            $("#index-page_choose_clear").hide();

            if (0 == subjectiveCategories.length) {
                alert("No subjective data loaded from server");
            } else {
                $("#index-page_messages").append(
                    "Loaded " + subjectiveCategories.length
                    + " categories from the server<br/>");
            }
            $("#index-page_messages").append(
                "Current tournament is " + subjective_module.getTournament().name
                + "<br/>");

            updateMainHeader();

            promptForJudgingGroup();
        }, function(message) {
            waitDialog.style.visibility = "hidden";

            alert("Error getting data from server: " + message);
        });
}

function checkStoredData() {
    if (subjective_module.storedDataExists()) {
        checkTournament();
    } else {
        loadData();
    }
}

function promptForJudgingGroup() {
    window.location = "#choose-judging-group";
}

function promptForReload() {
    $("#index-page_choose_clear").show();
}

function reloadData() {
    if (subjective_module.checkForModifiedScores()) {
        const answer = confirm("You have modified scores, this will remove them. Are you sure?")
        if (!answer) {
            return;
        }
    }
    subjective_module.clearAllData();
    loadData();
}

function checkTournament() {
    const waitDialog = document.getElementById("wait-dialog");
    waitDialog.style.visibility = "visible";

    subjective_module.getServerTournament(function(serverTournament) {
        waitDialog.style.visibility = "hidden";

        const storedTournament = subjective_module.getTournament();
        if (null == storedTournament) {
            reloadData();
        } else if (storedTournament.name != serverTournament.name
            || storedTournament.tournamentID != serverTournament.tournamentID) {
            reloadData();
        } else if (!subjective_module.checkForModifiedScores()) {
            // nothing is modified, just reload
            reloadData();
        } else {
            promptForReload();
        }
    }, function() {
        alert("Error getting data from server");
    });
}

function serverLoadPage() {
    $("#index-page_choose_clear").hide();

    $.getJSON("CheckAuth", function(data) {
        subjective_module.log("data: " + JSON.stringify(data));

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
