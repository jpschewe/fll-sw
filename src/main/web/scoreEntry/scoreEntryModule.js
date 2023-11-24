/*
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";


const score_entry_module = {}

{
    if (typeof fllStorage != 'object') {
        throw new Error("fllStorage needs to be loaded!");
    }

    score_entry_module.STORAGE_PREFIX = "fll.score_entry";

    /**
     * Create a new object to store form data in.
     */
    score_entry_module.newStorageData = function(teamNumber, runNumber, roundText) {
        const storageData = new Object();
        storageData.teamNumber = teamNumber;
        storageData.runNumber = runNumber;
        storageData.roundText = roundText;

        storageData.formValues = new Map();
        return storageData;
    }

    /**
     * Store score data. 
     */
    score_entry_module.saveStorageData = function(storageData) {
        const storageKey = storageData.teamNumber + "_" + storageData.runNumber;
        fllStorage.set(score_entry_module.STORAGE_PREFIX, storageKey, storageData);
    }

    /**
     * Delete stored score data.
     */
    score_entry_module.deleteStorageData = function(storageData) {
        const storageKey = storageData.teamNumber + "_" + storageData.runNumber;
        fllStorage.remove(score_entry_module.STORAGE_PREFIX, storageKey);
    }

    /**
     * Get all stored values as an array. May be empty.
     */
    score_entry_module.getAllStorageData = function() {
        const result = [];

        for (const [key, value] of fllStorage.allValues(score_entry_module.STORAGE_PREFIX)) {
            result.push(value);
        }

        return result;
    }

    /**
     * Submit score to the server.
     * Shows the dialog named "upload-dialog" and closes it when done and loads select_team.jsp.
     */
    score_entry_module.uploadScore = function(storageData) {
        const dialog = document.createElement("div");
        dialog.classList.add("fll-sw-ui-dialog");

        const dialogContainer = document.createElement("div");
        dialog.appendChild(dialogContainer);

        const textElement = document.createElement("div");
        dialogContainer.appendChild(textElement);
        textElement.innerHTML = "Please wait while the score is uploaded...";

        const closeButton = document.createElement("button");
        closeButton.setAttribute("type", "button");
        closeButton.innerText = "Close";
        closeButton.classList.add("fll-sw-ui-inactive");
        closeButton.addEventListener("click", () => {
            window.location = "select_team.jsp";
        });
        dialogContainer.appendChild(closeButton);

        document.body.appendChild(dialog);

        // make the map look like an object so that it parses into a Java Map
        const dataToUpload = JSON.stringify(Object.fromEntries(storageData.formValues));
        fetch("SubmitScoreEntry", {
            method: "POST",
            headers: { 'Content-Type': 'application/json' },
            body: dataToUpload
        }).then(checkJsonResponse).then(function(result) {
            if (result.success) {
                textElement.innerHTML = result.message;

                // clear the data as it was successfully submitted
                score_entry_module.deleteStorageData(storageData);
            } else {
                textElement.innerHTML = result.message;
            }
        }).catch(function(result) {
            textElement.innerHTML = "<div class='error'>Error talking to the server. The score data is stored locally. Error: " + result.message + "</div>";
        }).then(function() {
            closeButton.classList.remove("fll-sw-ui-inactive");
        });
    };

}
