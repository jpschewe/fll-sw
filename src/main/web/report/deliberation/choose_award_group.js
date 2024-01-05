/*
 * Copyright (c) 2024 HighTechKids.  All rights reserved.
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

document.addEventListener("DOMContentLoaded", function() {
    finalist_module.loadFromLocalStorage();

    const awardGroupsElement = document.getElementById("award_groups");
    removeChildren(awardGroupsElement);
    const divisions = finalist_module.getDivisions();
    for (const [i, division] of enumerate(divisions)) {
        const divisionOption = document.createElement("option");
        divisionOption.setAttribute("value", i);
        divisionOption.innerText = division;
        if (division == finalist_module.getCurrentDivision()) {
            divisionOption.setAttribute("selected", "true");
        }
        awardGroupsElement.appendChild(divisionOption);
    }
    document.getElementById("select_award_group").addEventListener("click", () => {
        const divIndex = awardGroupsElement.value;
        const div = finalist_module.getDivisionByIndex(divIndex);
        finalist_module.setCurrentDivision(div);
        finalist_module.saveToLocalStorage();

        window.location.assign("deliberation.html");
    });

});