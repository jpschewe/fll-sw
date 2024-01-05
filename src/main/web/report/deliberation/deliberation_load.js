/*
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

const deliberationLoad = {}

{
    function clearAndLoad() {
        const waitDialog = document.getElementById("wait-dialog");

        finalist_module.clearAllData();

        // need to load the tournament again since everything was just cleared
        finalist_module.loadTournament(function() {
            // success            

            finalist_module.loadCategoriesAndScores(function() {
                // success
                finalist_module.loadNominieesAndSchedules(function() {
                    // success
                    waitDialog.classList.add("fll-sw-ui-inactive");
                    finalist_module.saveToLocalStorage();
                    window.location.assign("choose_award_group.html");
                }, function(msg) {
                    // error
                    waitDialog.classList.add("fll-sw-ui-inactive");
                    alert("Failure loading nominees and schedules: " + msg);
                });
            }, function(msg) {
                // error
                waitDialog.classList.add("fll-sw-ui-inactive");
                alert("Failure loading categories and scores: " + msg);
            });
        }, function(msg) {
            // failure
            waitDialog.classList.add("fll-sw-ui-inactive");
            alert("Failure loading current tournament: " + msg);
        });

    }

    function refreshData() {
        const waitDialog = document.getElementById("wait-dialog");

        finalist_module.loadCategoriesAndScores(function() {
            // success
            waitDialog.classList.add("fll-sw-ui-inactive");
            finalist_module.saveToLocalStorage();
            window.location.assign("choose_award_group.html");
        }, function(msg) {
            // error
            waitDialog.classList.add("fll-sw-ui-inactive");
            alert("Failure loading categories and scores: " + msg);
        });
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
