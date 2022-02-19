/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

document.addEventListener("DOMContentLoaded", function() {
    document.getElementById("execute_query").addEventListener("click", function() {
        executeQuery();
    });
}); // end ready function

function executeQuery() {
    const queryString = document.getElementById("query").value.trim();
    if (queryString.length < 1) {
        alert("Query is empty");
        return;
    }

    const queryResultTable = document.getElementById("query_result");
    removeChildren(queryResultTable);
    queryResultTable.insertRow().insertCell().innerText = "Processing Query";

    const formData = new FormData();
    formData.append("query", queryString);

    fetch("QueryHandler", {
        method: "POST",
        body: new URLSearchParams(formData),
    }).then(response => {
        return response.json();
    }).then(data => {
        populateQueryResult(data);
    }).catch(err => {
        alert("Error executing query: " + err);
    });
}

function populateQueryResult(data) {
    const queryResultTable = document.getElementById("query_result");
    removeChildren(queryResultTable);
    if (data.error) {
        const cell = queryResultTable.insertRow().insertCell();
        cell.innerText = "Processing Query";
        cell.classList.add("error");
        return;
    }

    const headerRow = queryResultTable.insertRow();
    for (const name of data.columnNames) {
        const header = document.createElement("th");
        headerRow.appendChild(header);
        header.innerText = name;
    }

    for (const rowData of data.data) {
        const row = queryResultTable.insertRow();
        for (const name of data.columnNames) {
            const cell = row.insertCell();
            cell.innerText = rowData[name];
        }
    }
}