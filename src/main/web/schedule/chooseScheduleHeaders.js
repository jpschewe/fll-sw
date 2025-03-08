"use strict";

function setTableValue(timeSelect, tableSelect) {
    let found = false;
    for (const option of timeSelect.options) {
        if (option.selected) {
            found = true;
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
        // sync table with initial value
        setTableValue(timeSelect, tableSelect);

        // update table when time is selected
        timeSelect.addEventListener('change', () => {
            setTableValue(timeSelect, tableSelect);
        })
    }

});
