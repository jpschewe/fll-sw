/*
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

const finalistScheduleLoad = {}

{

    function clearAndLoad() {
        $.finalist.clearAllData();
        $.finalist.loadCategoriesAndScores(function() {
            // success
            $.finalist.loadNominieesAndSchedules(function() {
                // success
                $("#wait-dialog").dialog("close");
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
    }

    function refreshData() {
        $.finalist.loadCategoriesAndScores(function() {
            // success
            $("#wait-dialog").dialog("close");
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
        const currentAllTeams = $.finalist.getAllTeams();
        const currentTournament = $.finalist.getTournament();

        $.finalist.loadTournament(function() {
            // success            
            const loadingTournament = $.finalist.getTournament();

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
            alert("Failure loading teams and current tournament: " + msg);
        });

    });

} // end scope
