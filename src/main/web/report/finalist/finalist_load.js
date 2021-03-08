/*
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

const finalistScheduleLoad = {}

{
    $(document).ready(function() {
        $("#choose_clear").hide();
        $("#clear").click(function() {
            $.finalist.clearAllData();
            loadData();
        });
        $("#keep").click(function() {
            loadData();
        });

        $("#wait-dialog").dialog({
            autoOpen: false,
            modal: true,
            dialogClass: "no-close",
            closeOnEscape: false
        });

        const allTeams = $.finalist.getAllTeams();
        const tournament = $.finalist.getTournament();

        if (null != allTeams && allTeams.length > 0) {
            if (tournament != _loadingTournament) {
                _log("Clearing data for old tournament: " + tournament);
                $.finalist.clearAllData();
                loadData();
            } else {
                $("#choose_clear").show();
            }
        } else {
            loadData();
        }
    });

    finalistScheduleLoad.loadUsingApi = function() {
        _log("Loading finalist scheduling data from the server");

        const failCallback = function(msg) {
            alert("Failure loading data: " + msg);
        }

        const doneCallback = function() {
            $("#wait-dialog").dialog("close");
            $("#choose_clear").hide();
            location.href = "params.html";
        }

        $.finalist.loadFromServer(doneCallback, failCallback);

    }

} // end scope
