/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

function loadData() {
    const waitDialog = document.getElementById("wait-dialog");
    waitDialog.classList.remove("fll-sw-ui-inactive");

    subjective_module.loadFromServer(
        function() {
            const subjectiveCategories = subjective_module.getSubjectiveCategories();

            waitDialog.classList.add("fll-sw-ui-inactive");
            document.getElementById("index-page_choose_clear").classList.add("fll-sw-ui-inactive");

            const messages = document.getElementById("index-page_messages");
            if (0 == subjectiveCategories.length) {
                document.getElementById('alert-dialog_text').innerText = "No subjective data loaded from server";
                document.getElementById('alert-dialog').classList.remove("fll-sw-ui-inactive");
            } else {
                const message = document.createElement("div");
                messages.appendChild(message);
                message.innerText = "Loaded " + subjectiveCategories.length
                    + " categories from the server";
            }

            const tournamentMessage = document.createElement("div");
            messages.appendChild(tournamentMessage);
            messages.innerText = "Current tournament is " + subjective_module.getTournament().name;

            updateMainHeader();

            promptForJudgingGroup();
        }, function(message) {
            waitDialog.classList.add("fll-sw-ui-inactive");

            document.getElementById('alert-dialog_text').innerText = "Error getting data from server: " + message;
            document.getElementById('alert-dialog').classList.remove("fll-sw-ui-inactive");
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
    document.getElementById("wait-dialog").classList.add("fll-sw-ui-inactive");

    window.location = "#choose-judging-group";
}

function promptForReload() {
    document.getElementById("index-page_choose_clear").classList.remove("fll-sw-ui-inactive");
}

function reloadData() {
    if (subjective_module.checkForModifiedScores()) {
        document.getElementById("confirm-modified-scores-dialog").classList.remove("fll-sw-ui-inactive");
    } else {
        reloadDataConfirmed();
    }
}

function reloadDataConfirmed() {
    document.getElementById("index-page_choose_clear").classList.add("fll-sw-ui-inactive");

    subjective_module.clearAllData();
    loadData();
}

function checkTournament() {
    const waitDialog = document.getElementById("wait-dialog");
    waitDialog.classList.remove("fll-sw-ui-inactive");

    subjective_module.getServerTournament(function(serverTournament) {
        waitDialog.classList.add("fll-sw-ui-inactive");

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
        document.getElementById('alert-dialog_text').innerText = "Error getting data from server";
        document.getElementById('alert-dialog').classList.remove("fll-sw-ui-inactive");
    });
}

function serverLoadPage() {
    server_online = true;
    postServerStatusCallback();

    document.getElementById("wait-dialog").classList.add("fll-sw-ui-inactive");

    const chooseClear = document.getElementById("index-page_choose_clear");
    chooseClear.classList.add("fll-sw-ui-inactive");

    fetch("CheckAuth").then(checkJsonResponse).then(function(data) {
        subjective_module.log("data: " + JSON.stringify(data));

        if (data.authenticated) {
            document.getElementById("index-page_clear").addEventListener('click', function() {
                reloadData();
            });
            document.getElementById("index-page_keep").addEventListener('click', function() {
                chooseClear.classList.add("fll-sw-ui-inactive");
                promptForJudgingGroup();
            });

            checkStoredData();
        } else {
            location.href = "Auth";
        }
    });

}
