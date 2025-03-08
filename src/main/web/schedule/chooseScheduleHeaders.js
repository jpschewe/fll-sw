"use strict";

function setTableValue(timeSelect, tableSelect, nameInput) {
    let found = false;
    for (const option of timeSelect.options) {
        if (option.selected) {
            found = true;
            nameInput.value = option.value;
        } else if (found) {
            // assume the table is the next value
            setSelectValue(tableSelect, option.value);
            break;
        }
    }
}

document.addEventListener("DOMContentLoaded", () => {
    for (let i = 0; i < numPerformanceRuns; ++i) {
        const roundNumber = i + 1;
        const timeSelect = document.getElementById(`perf${roundNumber}_time`);
        const tableSelect = document.getElementById(`perf${roundNumber}_table`);
        const nameInput = document.getElementById(`perf${roundNumber}_name`);
        // sync table with initial value
        setTableValue(timeSelect, tableSelect, nameInput);

        // update table when time is selected
        timeSelect.addEventListener('change', () => {
            setTableValue(timeSelect, tableSelect, nameInput);
        })
    }

});
