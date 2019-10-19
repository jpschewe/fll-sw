/*
 * This code is released under GPL; see LICENSE.txt for details.
 */

$(document)
    .ready(
        function() {
          init();

          var yesCallback = null;

          $("#verification-warning").hide();

          $("#yesno-dialog").dialog({
            autoOpen : false,
            modal : true,
            buttons : [ {
              text : "Yes",
              id : "yesno-dialog_yes",
              click : function() {
                $(this).dialog("close");

                if (null != yesCallback) {
                  yesCallback();
                }

                $("#scoreEntry").submit();
              }
            }, {
              text : "No",
              id : "yesno-dialog_no",
              click : function() {
                $(this).dialog("close");
              }
            } ]
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
            };

            $("#yesno-dialog_text").text("Are you sure this is a 'No Show'?");
            $("#yesno-dialog").dialog("open");
          });

        }); // end ready function
