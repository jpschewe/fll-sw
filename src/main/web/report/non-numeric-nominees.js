/*
 * Copyright (c) 2021 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

const nonNumericNominees = {}

{

    function uploadData() {
        document.getElementById("wait-dialog_text").innerText = "Saving data. Please wait...";

        const waitList = [];

        const nonNumericSuccess = function(_) {
            _log("Non-numeric upload success")
        };
        const nonNumericFail = function(result) {
            let message;
            if (null == result) {
                message = "Unknown server error";
            } else {
                message = result.message;
            }

            alert("Non-numeric nominees upload failure: " + message);
        }
        waitList.push(finalist_module.uploadNonNumericNominees(
            nonNumericSuccess, nonNumericFail));

        document.getElementById("wait-dialog").classList.remove("fll-sw-ui-inactive");
        Promise.all(waitList).then(function(_) {
            document.getElementById("wait-dialog").classList.add("fll-sw-ui-inactive");
        });
    }


    document.addEventListener("DOMContentLoaded", function() {
        nonNumericUi.setUseStorage(false);
        nonNumericUi.setUseScheduledFlag(false);

        const waitDialog = document.getElementById("wait-dialog");
        document.getElementById("wait-dialog_text").innerText = "Loading data. Please wait...";
        waitDialog.classList.remove("fll-sw-ui-inactive");

        document.getElementById("nominees_store").addEventListener("click", function() {
            uploadData();
        });

        // Some things need to be loaded first
        const waitList1 = [];

        const teamsPromise = finalist_module.loadTournamentTeams();
        teamsPromise.catch(function() {
            alert("Failure loading : teams");
        })
        waitList1.push(teamsPromise);

        const nonNumericCategoriesPromise = finalist_module.loadNonNumericCategories();
        nonNumericCategoriesPromise.catch(function() {
            alert("Failure loading : non-numeric categories");
        });
        waitList1.push(nonNumericCategoriesPromise);

        const awardgroupsPromise = finalist_module.loadAwardGroups();
        awardgroupsPromise.catch(function() {
            alert("Failure loading : award groups");
        });
        waitList1.push(awardgroupsPromise);

        Promise.all(waitList1).then(function() {

            // everything else can be loaded in parallel
            const waitList = [];

            const nonNumericNomineesPromise = finalist_module.loadNonNumericNominees();
            nonNumericNomineesPromise.catch(function() {
                alert("Failure loading : non-numeric nominees");
            })
            waitList.push(nonNumericNomineesPromise);

            Promise.all(waitList).then(function() {
                waitDialog.classList.add("fll-sw-ui-inactive");

                nonNumericUi.initialize();
            });
        });

    }); // ready

} // local scope