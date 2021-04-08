/*
 * Copyright (c) 2021 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

const nonNumericNominees = {}

{

    function uploadData() {
        $("#wait-dialog_text").text("Saving data. Please wait...");

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
        waitList.push($.finalist.uploadNonNumericNominees(
            nonNumericSuccess, nonNumericFail));

        $("#wait-dialog").dialog("open");
        $.when.apply($, waitList).done(function() {
            $("#wait-dialog").dialog("close");
        });
    }


    $(document).ready(
        function() {
            nonNumericUi.setUseStorage(false);
            nonNumericUi.setUseScheduledFlag(false);

            $("#wait-dialog").dialog({
                autoOpen: false,
                modal: true,
                dialogClass: "no-close",
                closeOnEscape: false
            });

            $("#wait-dialog_text").text("Loading data. Please wait...");
            $("#wait-dialog").dialog("open");

            $("#nominees_store").click(function() {
                uploadData();
            });

            // Some things need to be loaded first
            const waitList1 = [];

            const teamsPromise = $.finalist.loadTournamentTeams();
            teamsPromise.fail(function() {
                alert("Failure loading : teams");
            })
            waitList1.push(teamsPromise);

            const nonNumericCategoriesPromise = $.finalist.loadNonNumericCategories();
            nonNumericCategoriesPromise.fail(function() {
                alert("Failure loading : non-numeric categories");
            });
            waitList1.push(nonNumericCategoriesPromise);

            const awardgroupsPromise = $.finalist.loadAwardGroups();
            awardgroupsPromise.fail(function() {
                alert("Failure loading : award groups");
            });
            waitList1.push(awardgroupsPromise);

            $.when.apply($, waitList1).done(function() {

                // everything else can be loaded in parallel
                const waitList = [];

                const nonNumericNomineesPromise = $.finalist.loadNonNumericNominees();
                nonNumericNomineesPromise.fail(function() {
                    alert("Failure loading : non-numeric nominees");
                })
                waitList.push(nonNumericNomineesPromise);

                $.when.apply($, waitList).done(function() {
                    $("#wait-dialog").dialog("close");

                    nonNumericUi.initialize();
                });

            });

        }); // ready

} // local scope