/*
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

const finalistScheduleLoad = {}

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
                    waitDialog.style.visibility = "hidden";
                    finalist_module.saveToLocalStorage();
                    location.href = "params.html";
                }, function(msg) {
                    // error
                    waitDialog.style.visibility = "hidden";
                    alert("Failure loading nominees and schedules: " + msg);
                });
            }, function(msg) {
                // error
                waitDialog.style.visibility = "hidden";
                alert("Failure loading categories and scores: " + msg);
            });
        }, function(msg) {
            // failure
            waitDialog.style.visibility = "hidden";
            alert("Failure loading current tournament: " + msg);
        });

    }

    function refreshData() {
        const waitDialog = document.getElementById("wait-dialog");

        finalist_module.loadCategoriesAndScores(function() {
            // success
            waitDialog.style.visibility = "hidden";
            finalist_module.saveToLocalStorage();
            location.href = "params.html";
        }, function(msg) {
            // error
            waitDialog.style.visibility = "hidden";
            alert("Failure loading categories and scores: " + msg);
        });
    }

    document.addEventListener('DOMContentLoaded', function() {
        const waitDialog = document.getElementById("wait-dialog");

        const chooseClear = document.getElementById("choose_clear");
        chooseClear.style.visibility = "hidden";

        document.getElementById("clear").addEventListener('click', function() {
            clearAndLoad();
        });

        document.getElementById("keep").addEventListener('click', function() {
            refreshData();
        });

        waitDialog.style.visibility = "visible";

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
                    waitDialog.style.visibility = "hidden";
                    chooseClear.style.visibility = "visible";
                }
            } else {
                clearAndLoad();
            }
        }, function(msg) {
            // failure
            waitDialog.style.visibility = "hidden";
            alert("Failure loading current tournament: " + msg);
        });

    });

} // end scope
