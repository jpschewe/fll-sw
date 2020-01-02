/*
 * Copyright (c) 2019 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

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
