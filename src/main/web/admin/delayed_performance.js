/*
 * Copyright (c) 2019 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

function addRow() {
  var numRows = parseInt($('#numRows').val());

  var trElement = $("<tr></tr>");

  var td1Element = $("<td></td>");
  var runNumberElement = $("<input type=\"text\" name=\"runNumber" + numRows
      + "\" id=\"runNumber" + numRows + "\" class=\"required digits\" size=\"8\" />");
  td1Element.append(runNumberElement);
  trElement.append(td1Element);

  var td2Element = $("<td></td>");
  var dateElement = $("<input type=\"text\" name=\"date" + numRows
      + "\" size=\"8\" class=\"required\" id=\"date" + numRows + "\" />");
  dateElement.datepicker();
  td2Element.append(dateElement);
  trElement.append(td2Element);

  var td3Element = $("<td></td>");
  var timeElement = $("<input type=\"text\" name=\"time" + numRows
      + "\" class=\"required\" id=\"time" + numRows + "\" size=\"8\" />");
  timeElement.timepicker();
  td3Element.append(timeElement);
  trElement.append(td3Element);

  $('#delayedPerformanceTable tbody').append(trElement);

  $('#numRows').val(numRows + 1);
  
}

function validateData() {
  var numRows = parseInt($('#numRows').val());

  var runNumbersSeen = [];
  for (var idx = 0; idx < numRows; ++idx) {
    var runNumberStr = $('#runNumber' + idx).val();
    _log("Checking index: " + idx + " runNumber: " + runNumberStr
        + " against: " + runNumbersSeen);
    if (runNumberStr) {
      var runNumber = parseInt(runNumberStr);
      if (runNumbersSeen.includes(runNumber)) {
        alert("Multiple instances of run number  " + runNumber);
        return false;
      }
      runNumbersSeen.push(runNumber);
    }
  }

  return true;
}

function setupDatepickers() {
  var numRows = parseInt($('#numRows').val());
  for (var idx = 0; idx < numRows; ++idx) {
    $('#date' + idx).datepicker();
    $('#time' + idx).timepicker();
  }
}

$(document).ready(function() {

  $('#addRow').click(function(e) {
    addRow();
    e.preventDefault();
  });

  setupDatepickers();
  
  $("#delayed_performance").validate();

}); // end ready function
