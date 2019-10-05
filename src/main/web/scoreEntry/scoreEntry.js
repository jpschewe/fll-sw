/*
 * This code is released under GPL; see LICENSE.txt for details.
 */

$(document).ready(function() {
  init();

  $("#verification-warning").hide();

  $("#confirm-score-submit").dialog({
    autoOpen : false,
    modal : true,
    buttons : [ {
      text : "Yes",
      id: "confirm-score-submit_yes",
      click : function() {
        $(this).dialog("close");
        $("#scoreEntry").submit();
      }
    }, {
      text : "No",
      id: "confirm-score-submit_no",
      click : function() {
        $(this).dialog("close");
      }
    } ]
  })

  $("#submit_score").click(function(e) {
    $("#confirm-score-submit").dialog("open");
  });


}); // end ready function
