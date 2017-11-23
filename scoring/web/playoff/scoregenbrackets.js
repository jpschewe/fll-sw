"use strict";

function checkSomethingToPrint() {
  var somethingToPrint = false;
  $('input[type=checkbox]').each(function() {
    if (this.checked) {
      somethingToPrint = true;
    }
  });

  if (somethingToPrint) {
    return true;
  } else {
    alert("There is nothing to print.");
    return false;
  }
}
