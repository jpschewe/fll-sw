/*
 * Copyright (c) 2022 HighTechKids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

document.addEventListener('DOMContentLoaded', function() {
    const select = document.getElementById('teamNumber');
    const searchBox = document.getElementById('team_search');
    searchBox.addEventListener('keyup', () => {
        const searchText = searchBox.value;
        if (searchText) {
            const searchTextLower = searchText.toLowerCase();
            for (const option of select.options) {
                const compareText = option.text.toLowerCase();
                if (compareText.includes(searchTextLower)) {
                    option.classList.remove('fll-sw-ui-inactive');
                } else {
                    option.classList.add('fll-sw-ui-inactive');
                }
            }

        } else {
            for (const option of select.options) {
                option.classList.remove('fll-sw-ui-inactive');
            }
        }
    });
});
