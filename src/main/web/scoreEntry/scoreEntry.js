/*
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

document.addEventListener("DOMContentLoaded", function() {
    init();

    // if set, return true to submit, false to skip submit
    var yesCallback = null;

    document.getElementById("verification-warning").style.visibility = "hidden";

    document.getElementById("yesno-dialog_yes").addEventListener("click", function() {
        document.getElementById("yesno-dialog").style.visibility = "hidden";

        let dosubmit = true;
        if (null != yesCallback) {
            dosubmit = yesCallback();
        }

        if (dosubmit) {
            document.getElementById("scoreEntry").submit();
        }
    });

    document.getElementById("yesno-dialog_no").addEventListener("click", function() {
        document.getElementById("yesno-dialog").style.visibility = "hidden";
    });

    document.getElementById("submit_score").addEventListener("click", function() {
        yesCallback = null;

        var text = "";
        if (EditFlag) {
            if (Verified == 1) {
                if (savedTotalScore != document.scoreEntry.totalScore.value) {
                    text = "You are changing and verifying a score -- are you sure?";
                } else {
                    text = "You are verifying a score -- are you sure?";
                }
            } else {
                text = "You are submitting a score without verification -- are you sure?";
            }
        } else {
            text = "Submit Data -- Are you sure?";
        }

        document.getElementById("yesno-dialog_text").innerText = text;
        document.getElementById("yesno-dialog").style.visibility = "visible";
    });

    document.getElementById("submit_delete").addEventListener("click", function() {
        yesCallback = function() {
            document.getElementById("delete").value = "true";
            return true;
        };

        document.getElementById("yesno-dialog_text").innerText =
            "Are you sure you want to delete this score?";
        document.getElementById("yesno-dialog").style.visibility = "visible";
    });

    document.getElementById("no_show").addEventListener("click", function() {
        yesCallback = function() {
            document.getElementById("NoShow").value = "true";
            Verified = 1;
            refresh();
            return true;
        };

        document.getElementById("yesno-dialog_text").innerText = "Are you sure this is a 'No Show'?";
        document.getElementById("yesno-dialog").style.visibility = "visible";
    });

    document.getElementById("cancel").addEventListener("click", function() {
        yesCallback = function() {
            window.location.assign("select_team.jsp");
            return false;
        };

        let text;
        if (EditFlag) {
            text = "Cancel and lose changes?";
        } else {
            text = "Cancel and lose data?"
        }
        document.getElementById("yesno-dialog_text").innerText = text;
        document.getElementById("yesno-dialog").style.visibility = "visible";
    });

}); // end ready function
