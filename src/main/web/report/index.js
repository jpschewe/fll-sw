/*
 * Copyright (c) 2022 HighTechKids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

function updateRubricJudges() {
    const rubricJudge = document.getElementById('rubric_judge');
    removeChildren(rubricJudge);

    const categoryName = document.getElementById('rubric_category_name').value;

    for (const judge of categoryJudges[categoryName]) {
        const option = document.createElement("option");
        rubricJudge.appendChild(option);
        option.value = judge;
        option.innerText = judge;
    }
}

document.addEventListener('DOMContentLoaded', function() {

    document.getElementById('rubric_category_name').addEventListener('change', () => {
        updateRubricJudges();
    });
});

document.addEventListener('DOMContentLoaded', function() {
    updateRubricJudges();
});
