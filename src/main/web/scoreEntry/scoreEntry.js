/*
 * This code is released under GPL; see LICENSE.txt for details.
 */

$(document).ready(function() {
  init();

  $("#verification-warning").hide();

  $("#yesno-dialog").dialog({
    autoOpen : false,
    modal : true,
    buttons : [ {
      text : "Yes",
      id: "yesno-dialog_yes",
      click : function() {
        $(this).dialog("close");
        $("#scoreEntry").submit();
      }
    }, {
      text : "No",
      id: "yesno-dialog_no",
      click : function() {
        $(this).dialog("close");
      }
    } ]
  })

  $("#submit_score").click(function(e) {
    $("#delete").val("false");
    
    $("#yesno-dialog_text").text("Submit Data -- Are you sure?");
    $("#yesno-dialog").dialog("open");
  });

  $("#submit_delete").click(function(e) {
    $("#delete").val("true");
    
    $("#yesno-dialog_text").text("Are you sure you want to delete this score?");
    $("#yesno-dialog").dialog("open");
  });

}); // end ready function
