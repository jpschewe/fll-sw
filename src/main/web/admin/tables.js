"use strict";

function computeNextId() {
    let maxId = null;
    document.querySelectorAll("input.SideA").forEach((element) => {
        const id = element.name.substring("SideA".length);
        maxId = Math.max(maxId, id);
    });
    return maxId + 1;
}

function computeNextSortOrder() {
    let maxSort = null;
    document.querySelectorAll("input.SortOrder").forEach((element) => {
        maxSort = Math.max(maxSort, element.value);
    });
    return maxSort + 1;
}

function addNewRow() {
    const nextId = computeNextId();
    const sortOrder = computeNextSortOrder();

    const table = document.getElementById('tables_table');
    const row = table.insertRow(-1);

    const sideAcell = row.insertCell(-1);
    const sideAEle = document.createElement('input');
    sideAcell.appendChild(sideAEle);
    sideAEle.setAttribute('type', 'text');
    sideAEle.setAttribute('name', `SideA${nextId}`);
    sideAEle.classList.add('SideA');

    const sideBcell = row.insertCell(-1);
    const sideBEle = document.createElement('input');
    sideBcell.appendChild(sideBEle);
    sideBEle.setAttribute('type', 'text');
    sideBEle.setAttribute('name', `SideB${nextId}`);
    sideBEle.classList.add('SideB');

    const sortOrderCell = row.insertCell(-1);
    const sortOrderEle = document.createElement('input');
    sortOrderCell.appendChild(sortOrderEle);
    sortOrderEle.setAttribute('type', 'number');
    sortOrderEle.setAttribute('value', sortOrder);
    sortOrderEle.setAttribute('name', `sortOrder${nextId}`);
    sortOrderEle.classList.add('SortOrder');

    const deleteCell = row.insertCell(-1);
    const deleteEle = document.createElement('input');
    deleteCell.appendChild(deleteEle);
    deleteEle.setAttribute('type', 'checkbox');
    deleteEle.setAttribute('value', 'checked');
    deleteEle.setAttribute('name', `delete${nextId}`);
}

document.addEventListener("DOMContentLoaded", function() {
    document.getElementById("add_row").addEventListener("click", function() {
        addNewRow();
    });
});