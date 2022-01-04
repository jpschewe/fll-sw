/*
 * Copyright (c) 2022 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use-strict";

// set the font size based on the screen width

const setFontSize = {}

{
    setFontSize.doit = function() {
        // width of the screen that the scoreboard is designed for
        const baseScreenWidth = 1024;

        let fontSize;
        if (screen.width < baseScreenWidth) {
            // never shrink
            fontSize = 1;
        } else {
            fontSize = screen.width / baseScreenWidth;
        }
        document.body.style.fontSize = `${fontSize}em`;
    };
}

document.addEventListener('DOMContentLoaded', setFontSize.doit);
