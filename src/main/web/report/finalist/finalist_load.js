/*
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

const finalistScheduleLoad = {}

{

    function clearAndLoad() {
        finalist_module.clearAllData();

        // need to load the tournament again since everything was just cleared
        finalist_module.loadTournament(function() {
            // success            

            finalist_module.loadCategoriesAndScores(function() {
                // success
                finalist_module.loadNominieesAndSchedules(function() {
                    // success
                    $("#wait-dialog").dialog("close");
                    finalist_module.saveToLocalStorage();
                    location.href = "params.html";
                }, function(msg) {
                    // error
                    $("#wait-dialog").dialog("close");
                    alert("Failure loading nominees and schedules: " + msg);
                });
            }, function(msg) {
                // error
                $("#wait-dialog").dialog("close");
                alert("Failure loading categories and scores: " + msg);
            });
        }, function(msg) {
            // failure
            $("#wait-dialog").dialog("close");
            alert("Failure loading current tournament: " + msg);
        });

    }

    function refreshData() {
        finalist_module.loadCategoriesAndScores(function() {
            // success
            $("#wait-dialog").dialog("close");
            finalist_module.saveToLocalStorage();
            location.href = "params.html";
        }, function(msg) {
            // error
            $("#wait-dialog").dialog("close");
            alert("Failure loading categories and scores: " + msg);
        });
    }

    $(document).ready(function() {
        $("#choose_clear").hide();

        $("#clear").click(function() {
            clearAndLoad();
        });

        $("#keep").click(function() {
            refreshData();
        });

        $("#wait-dialog").dialog({
            autoOpen: false,
            modal: true,
            dialogClass: "no-close",
            closeOnEscape: false
        });

        $("#wait-dialog").dialog("open");

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
                    $("#wait-dialog").dialog("close");
                    $("#choose_clear").show();
                }
            } else {
                clearAndLoad();
            }
        }, function(msg) {
            // failure
            $("#wait-dialog").dialog("close");
            alert("Failure loading current tournament: " + msg);
        });

    });

} // end scope
