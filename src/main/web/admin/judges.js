/*
 * Copyright (c) 2012INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

document.addEventListener("DOMContentLoaded", function() {

    document.getElementById("total_num_rows").value = maxIndex;

    document.getElementById("add_rows").addEventListener("click", function() {
        const numRowsStr = document.getElementById("num_rows").value;
        let numRows = 1;
        if (isNumeric(numRowsStr)) {
            numRows = parseInt(numRowsStr);
        }

        addRows(numRows);
    });

    const numCategories = Object.entries(categories).length;

    const missingRows = (judging_stations.length * numCategories) - maxIndex;
    if (missingRows > 0) {
        addRows(missingRows);
    }

}); // end ready function

function addRows(numRows) {
    const table = document.getElementById("data");

    let rowIndex;
    for (rowIndex = 0; rowIndex < numRows; ++rowIndex) {
        const judgeIdx = maxIndex + 1;
        maxIndex = judgeIdx;
        document.getElementById("total_num_rows").value = maxIndex;

        const row = table.insertRow();

        const idCol = row.insertCell();
        const id = document.createElement("input");
        idCol.appendChild(id);
        id.setAttribute("type", "text");
        id.setAttribute("name", "id" + judgeIdx);

        const categoryCol = row.insertCell();
        const category = document.createElement("select");
        categoryCol.appendChild(category);
        category.setAttribute("name", "cat" + judgeIdx);

        for (const [catId, catName] of Object.entries(categories)) {
            const option = document.createElement("option");
            category.appendChild(option);
            option.setAttribute("value", catId);
            option.innerText = catName;
        }

        const stationCol = row.insertCell();
        const station = document.createElement("select");
        stationCol.appendChild(station);
        station.setAttribute("name", "station" + judgeIdx);
        for (const stationName of judging_stations) {
            const option = document.createElement("option");
            station.appendChild(option);
            option.setAttribute("value", stationName);
            option.innerText = stationName;
        }

    }
}