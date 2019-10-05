/*
 * This code is released under GPL; see LICENSE.txt for details.
 */

$(document).ready(function() {
  init();

  $("#verification-warning").hide();

  $("#confirm-score-submit").dialog({
    autoOpen : false,
    buttons : [ {
      text : "Yes",
      click : function() {
        $(this).dialog("close");
        $("#scoreEntry").submit();
      }
    }, {
      text : "No",
      click : function() {
        $(this).dialog("close");
      }
    } ]
  })

  $("#submit_score").click(function(e) {
    $("#confirm-score-submit").dialog("open");
  });


}); // end ready function
