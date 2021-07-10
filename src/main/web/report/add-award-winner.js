/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";


document.addEventListener('DOMContentLoaded', function() {

    document.getElementById("cancel").addEventListener('click', function() {
        location.href = "edit-award-winners.jsp";
    });

    fllTeams.loadTeams().catch(function() {
        alert("Error loading teams from the server");
    }).then(function() {
        const numEle = document.getElementById("teamNumber");
        const nameEle = document.getElementById("teamName");
        const orgEle = document.getElementById("organization");
        const agEle = document.getElementById("awardGroup");

        fllTeams.autoPopulate(numEle, nameEle, orgEle, agEle, awardGroup);

        // call onchange to populate the other fields in case the team number is populated for edit
        numEle.onchange();
    });

});
