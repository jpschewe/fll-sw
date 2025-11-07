"use strict";

document.addEventListener('DOMContentLoaded', function() {

    const table = document.getElementById('run_metadata_table');

    document.getElementById('addRow').addEventListener('click', () => {
        // header plus 1 row for each existing round means that the number of rows is the next round number
        const nextRound = table.getElementsByTagName('tr').length;

        const row = table.insertRow();

        const roundCell = row.insertCell();
        roundCell.innerText = nextRound;

        const nameCell = row.insertCell();
        const nameInput = document.createElement("input");
        nameInput.setAttribute("type", "text");
        nameInput.setAttribute("id", `${nextRound}_name`);
        nameInput.setAttribute("name", `${nextRound}_name`);
        nameCell.appendChild(nameInput);

        const runTypeCell = row.insertCell();
        // clone the first run to pickup all of the options without needing to get them into javascript
        const runTypeSelect = document.getElementById("1_runType").cloneNode(true);
        runTypeSelect.setAttribute("id", `${nextRound}_runType`);
        runTypeSelect.setAttribute("name", `${nextRound}_runType`);
        setSelectValue(runTypeSelect, "OTHER");
        runTypeCell.appendChild(runTypeSelect);

        const scoreboardCell = row.insertCell();
        const scoreboardCheck = document.createElement("input");
        scoreboardCheck.setAttribute("type", "checkbox");
        scoreboardCheck.setAttribute("id", `${nextRound}_scoreboardDisplay`);
        scoreboardCheck.setAttribute("name", `${nextRound}_scoreboardDisplay`);
        scoreboardCell.appendChild(scoreboardCheck);

        const deleteCell = row.insertCell();
        const deleteCheck = document.createElement("input");
        deleteCheck.setAttribute("type", "checkbox");
        deleteCheck.setAttribute("id", `${nextRound}_delete`);
        deleteCheck.setAttribute("name", `${nextRound}_delete`);
        deleteCell.appendChild(deleteCheck);
    });
});