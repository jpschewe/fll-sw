/*
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

document.addEventListener("DOMContentLoaded", function() {
    init();

    // if set, return true to submit, false to skip submit
    let yesCallback = null;

    document.getElementById("verification-warning").classList.add("fll-sw-ui-inactive");

    document.getElementById("yesno-dialog_yes").addEventListener("click", function() {
        document.getElementById("yesno-dialog").classList.add("fll-sw-ui-inactive");

        let dosubmit = true;
        if (null != yesCallback) {
            dosubmit = yesCallback();
        }

        if (dosubmit) {
            const storageData = score_entry_module.newStorageData(teamNumber, runNumber, roundText);
            const form = document.getElementById("scoreEntry");
            const formData = new FormData(form);
            for (const pair of formData.entries()) {
                storageData.formValues.set(pair[0], pair[1]);
            }
            score_entry_module.saveStorageData(storageData);
            score_entry_module.uploadScore(storageData);
        }
    });

    document.getElementById("yesno-dialog_no").addEventListener("click", function() {
        document.getElementById("yesno-dialog").classList.add("fll-sw-ui-inactive");
    });

    const submitScoreButton = document.getElementById("submit_score");
    if (submitScoreButton) {
        submitScoreButton.addEventListener("click", function() {
            yesCallback = null;

            let text = "";
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
            document.getElementById("yesno-dialog").classList.remove("fll-sw-ui-inactive");
        });
    }

    const deleteButton = document.getElementById("submit_delete");
    if (deleteButton) {
        deleteButton.addEventListener("click", function() {
            yesCallback = function() {
                document.getElementById("delete").value = "true";
                return true;
            };

            document.getElementById("yesno-dialog_text").innerText =
                "Are you sure you want to delete this score?";
            document.getElementById("yesno-dialog").classList.remove("fll-sw-ui-inactive");
        });
    }

    const noShowButton = document.getElementById("no_show");
    if (noShowButton) {
        noShowButton.addEventListener("click", function() {
            yesCallback = function() {
                document.getElementById("NoShow").value = "true";
                Verified = 1;
                refresh();
                return true;
            };

            document.getElementById("yesno-dialog_text").innerText = "Are you sure this is a 'No Show'?";
            document.getElementById("yesno-dialog").classList.remove("fll-sw-ui-inactive");
        });
    }

    const cancelButton = document.getElementById("cancel");
    if (cancelButton) {
        cancelButton.addEventListener("click", function() {
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
            document.getElementById("yesno-dialog").classList.remove("fll-sw-ui-inactive");
        });
    }


    document.getElementById("score-entry_toggle-review-mode").addEventListener('click', function() {
        const glassPane = document.getElementById("review-mode_glasspane");
        if (glassPane.style.zIndex > 0) {
            glassPane.style.zIndex = -1;
            this.classList.remove("fll-sw-button-pressed");
        } else {
            glassPane.style.zIndex = 10;
            this.classList.add("fll-sw-button-pressed");
        }
    });


}); // end ready function
