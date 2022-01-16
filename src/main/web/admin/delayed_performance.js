/*
 * Copyright (c) 2019 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

function addRow() {
    const numRowsEle = document.getElementById("numRows");
    const numRows = parseInt(numRowsEle.value);

    const delayedPerformanceTable = document.getElementById("delayedPerformanceTable");
    const tbody = delayedPerformanceTable.getElementsByTagName('tbody')[0];
    const trElement = tbody.insertRow();

    const td1Element = trElement.insertCell();
    const runNumberElement = document.createElement("input");
    runNumberElement.setAttribute("type", "number");
    runNumberElement.setAttribute("name", "runNumber" + numRows);
    runNumberElement.setAttribute("id", "runNumber" + numRows);
    runNumberElement.setAttribute("size", "8");
    runNumberElement.setAttribute("required", "true");
    runNumberElement.setAttribute("min", "1");
    td1Element.appendChild(runNumberElement);

    const td2Element = trElement.insertCell();
    const dateElement = document.createElement("input");
    dateElement.setAttribute("type", "datetime-local");
    dateElement.setAttribute("name", "datetime" + numRows);
    dateElement.setAttribute("id", "datetime" + numRows);
    dateElement.setAttribute("required", "true");
    td2Element.appendChild(dateElement);

    numRowsEle.value = numRows + 1;
}

function validateData() {
    const numRowsEle = document.getElementById("numRows");
    const numRows = parseInt(numRowsEle.value);

    const runNumbersSeen = [];
    for (let idx = 0; idx < numRows; ++idx) {
        const runNumberStr = document.getElementById("runNumber" + idx).value;
        _log("Checking index: " + idx + " runNumber: " + runNumberStr
            + " against: " + runNumbersSeen);
        if (runNumberStr) {
            const runNumber = parseInt(runNumberStr);
            if (runNumbersSeen.includes(runNumber)) {
                alert("Multiple instances of run number  " + runNumber);
                return false;
            }
            runNumbersSeen.push(runNumber);
        }
    }

    return true;
}

document.addEventListener('DOMContentLoaded', function() {
    const addRowButton = document.getElementById("addRow");
    addRowButton.addEventListener("click", function(e) {
        addRow();
    });
}); // end ready function
