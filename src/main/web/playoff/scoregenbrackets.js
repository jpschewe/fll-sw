/*
 * Copyright (c) 2019 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

function checkSomethingToPrint() {
    let somethingToPrint = false;
    // htmlunit doesn't handle const in a for loop, so skip it here - https://github.com/HtmlUnit/htmlunit/issues/449
    for (checkbox of document.querySelectorAll('input[type=checkbox]')) {
        if (checkbox.checked) {
            somethingToPrint = true;
        }
    }

    if (somethingToPrint) {
        return true;
    } else {
        alert("There is nothing to print.");
        return false;
    }
}
