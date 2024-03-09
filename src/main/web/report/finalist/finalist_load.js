/*
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

const finalistScheduleLoad = {}

{
    function loadSuccess() {
        const waitDialog = document.getElementById("wait-dialog");
        waitDialog.classList.add("fll-sw-ui-inactive");
        window.location.assign("params.html");
    }

    function loadError(msg) {
        const waitDialog = document.getElementById("wait-dialog");
        waitDialog.classList.add("fll-sw-ui-inactive");
        alert("Failure data: " + msg);
    }

    function clearAndLoad() {
        finalist_module.clearAndLoad(loadSuccess, loadError);
    }


    function refreshData() {
        finalist_module.refreshData(loadSuccess, loadError);
    }

    document.addEventListener('DOMContentLoaded', function() {
        const waitDialog = document.getElementById("wait-dialog");

        const chooseClear = document.getElementById("choose_clear");
        chooseClear.classList.add("fll-sw-ui-inactive");

        document.getElementById("clear").addEventListener('click', function() {
            clearAndLoad();
        });

        document.getElementById("keep").addEventListener('click', function() {
            refreshData();
        });

        waitDialog.classList.remove("fll-sw-ui-inactive");

        // get current state before anything loads
        finalist_module.loadFromLocalStorage();
        const currentAllTeams = finalist_module.getAllTeams();
        const currentTournament = finalist_module.getTournament();

        finalist_module.loadTournament(function() {
            // success            
            const loadingTournament = finalist_module.getTournament();

            if (null != currentAllTeams && currentAllTeams.length > 0) {
                if (currentTournament != loadingTournament) {
                    _log("Clearing data for old tournament: " + currentTournament);
                    clearAndLoad();
                } else {
                    waitDialog.classList.add("fll-sw-ui-inactive");
                    chooseClear.classList.remove("fll-sw-ui-inactive");
                }
            } else {
                clearAndLoad();
            }
        }, function(msg) {
            // failure
            waitDialog.classList.add("fll-sw-ui-inactive");
            alert("Failure loading current tournament: " + msg);
        });

    });

} // end scope
