/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

document.addEventListener('DOMContentLoaded', function() {
    nonNumericUi.initialize();

    document.getElementById("previous").addEventListener("click", function() {
        location.href = "params.html";
    });

    document.getElementById("next").addEventListener("click", function() {
        // find the first numeric category
        let first = null;
        for (const category of finalist_module.getNumericCategories()) {
            if (category.name != finalist_module.CHAMPIONSHIP_NAME) {
                if (null == first) {
                    first = category;
                }
            }
        }

        if (null != first) {
            finalist_module.setCurrentCategoryName(first.name);
            finalist_module.saveToLocalStorage();
            location.href = "numeric.html";
        } else {
            alert("Internal error, cannot find next category");
        }
    });

    finalist_module.displayNavbar();
}); // end ready function

