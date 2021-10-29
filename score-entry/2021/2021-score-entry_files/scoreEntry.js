/*
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

$(document)
    .ready(
        function() {
            init();

            // if set, return true to submit, false to skip submit
            var yesCallback = null;

            $("#verification-warning").hide();

            $("#yesno-dialog").dialog({
                autoOpen: false,
                modal: true,
                position: { my: "center top", at: "center top", of: window, collision: "none" },
                buttons: [{
                    text: "Yes",
                    id: "yesno-dialog_yes",
                    click: function() {
                        $(this).dialog("close");

                        var dosubmit = true;
                        if (null != yesCallback) {
                            dosubmit = yesCallback();
                        }

                        if (dosubmit) {
                            $("#scoreEntry").submit();
                        }
                    }
                }, {
                    text: "No",
                    id: "yesno-dialog_no",
                    click: function() {
                        $(this).dialog("close");
                    }
                }]
            })

            $("#submit_score")
                .click(
                    function(e) {
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

                        $("#yesno-dialog_text").text(text);
                        $("#yesno-dialog").dialog("open");
                    });

            $("#submit_delete").click(
                function(e) {
                    yesCallback = function() {
                        $("#delete").val("true");
                        return true;
                    };

                    $("#yesno-dialog_text").text(
                        "Are you sure you want to delete this score?");
                    $("#yesno-dialog").dialog("open");
                });

            $("#no_show").click(function(e) {
                yesCallback = function() {
                    $("#NoShow").val("true");
                    Verified = 1;
                    refresh();
                    return true;
                };

                $("#yesno-dialog_text").text("Are you sure this is a 'No Show'?");
                $("#yesno-dialog").dialog("open");
            });

            $("#cancel").click(function(e) {
                yesCallback = function() {
                    window.location.href = "select_team.jsp";
                    return false;
                };

                var text;
                if (EditFlag) {
                    text = "Cancel and lose changes?";
                } else {
                    text = "Cancel and lose data?"
                }
                $("#yesno-dialog_text").text(text);
                $("#yesno-dialog").dialog("open");
            });

        }); // end ready function
