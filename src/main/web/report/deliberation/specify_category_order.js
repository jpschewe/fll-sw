/*
 * Copyright (c) 2024
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";


function loadCategoryOrder() {
    showLoadingMessage();

    const awardGroup = finalist_module.getCurrentDivision();
    fetch(`../../api/deliberation/CategoryOrder/${awardGroup}`).then(checkJsonResponse).then((result) => {
        initCategoryOrder(result);
    }).catch((error) => {
        alert("Category order failed to load: " + error);
    }).finally(hideLoadingMessage);
}

function saveCategoryOrder() {
    showSavingMessage();

    // update award order based on what is on the page

    // gather list elements that have the group sort information
    const lineElements = [];
    Array.from(document.getElementById("category-order").children).forEach((le) => {
        if ("LI" == le.tagName) {
            lineElements.push(le);
        }
    });

    // sort the elements
    lineElements.sort(function(a, b) {
        const aIndex = a.getElementsByTagName("input")[0].value;
        const bIndex = b.getElementsByTagName("input")[0].value;
        return aIndex - bIndex;
    });

    // convert to a list of strings
    const newAwardOrder = [];
    lineElements.forEach((le) => {
        const categoryTitle = le.getElementsByTagName("span")[0].innerText;
        newAwardOrder.push(categoryTitle);
    });

    const awardGroup = finalist_module.getCurrentDivision();
    uploadJsonData(`../../api/deliberation/CategoryOrder/${awardGroup}`, "POST", newAwardOrder).then(checkJsonResponse).then(() => {
        alert(`Saved category order for award group ${awardGroup}`);
    }).catch((error) => {
        alert("Category order failed to save: " + error);
    }).finally(hideSavingMessage);
}


function initCategoryOrder(categoryOrder) {
    const categoryOrderList = document.getElementById("category-order");
    removeChildren(categoryOrderList);
    categoryOrder.forEach((categoryTitle, idx) => {
        const line = document.createElement("li");
        categoryOrderList.appendChild(line);

        const input = document.createElement("input");
        line.appendChild(input);
        input.setAttribute("type", "number");
        input.setAttribute("step", "1");
        input.value = idx + 1;

        const label = document.createElement("span");
        line.appendChild(label);
        label.innerText = categoryTitle;
    });

}

function showSavingMessage() {
    const waitDialog = document.getElementById("wait-dialog-save");
    waitDialog.classList.remove("fll-sw-ui-inactive");
}

function hideSavingMessage() {
    const waitDialog = document.getElementById("wait-dialog-save");
    waitDialog.classList.add("fll-sw-ui-inactive");
}

function showLoadingMessage() {
    const waitDialog = document.getElementById("wait-dialog-load");
    waitDialog.classList.add("fll-sw-ui-inactive");
}

function hideLoadingMessage() {
    const waitDialog = document.getElementById("wait-dialog-load");
    waitDialog.classList.add("fll-sw-ui-inactive");
}

function initPage() {
    hideLoadingMessage();

    const awardGroupsElement = document.getElementById("award-groups");
    removeChildren(awardGroupsElement)

    const awardGroups = finalist_module.getDivisions();
    for (const [i, awardGroup] of enumerate(awardGroups)) {
        const awardGroupOption = document.createElement("option");
        awardGroupOption.setAttribute("value", i);
        awardGroupOption.innerText = awardGroup;
        if (awardGroup == finalist_module.getCurrentDivision()) {
            awardGroupOption.setAttribute("selected", "true");
        }
        awardGroupsElement.appendChild(awardGroupOption);
    }

    finalist_module.setCurrentDivision(finalist_module.getDivisionByIndex(awardGroupsElement.value));
    finalist_module.saveToLocalStorage();

    // before change listeners to avoid loop
    loadCategoryOrder();

    awardGroupsElement.addEventListener('change', function() {
        const divIndex = this.value;
        const div = finalist_module.getDivisionByIndex(divIndex);
        finalist_module.setCurrentDivision(div);
        loadCategoryOrder();

        finalist_module.saveToLocalStorage();
    });

    document.getElementById("save").addEventListener('click', () => {
        saveCategoryOrder();
    });

}

document.addEventListener('DOMContentLoaded', () => {
    showLoadingMessage();

    // get current state before anything loads
    finalist_module.loadFromLocalStorage();
    const currentTournament = finalist_module.getTournament();

    finalist_module.loadTournament(function() {
        // success            
        const loadingTournament = finalist_module.getTournament();
        if (currentTournament != loadingTournament) {
            _log("Clearing data for old tournament: " + currentTournament);
            finalist_module.clearAndLoad(initPage, (msg) => {
                hideLoadingMessage();
                alert(`Error loading data: ${msg}`);
            });
        } else {
            finalist_module.refreshData(initPage, (msg) => {
                hideLoadingMessage();
                alert(`Error loading data: ${msg}`);
            });
        }
    }, function(msg) {
        // failure
        hideLoadingMessage();
        alert("Failure loading current tournament: " + msg);
    });

});
