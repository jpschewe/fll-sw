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
        //FIXME try creating dialog dynamically
        alert("FIXME need to submit data here");
        
        
            // FIXME: show wait dialog
            // FIXME: send AJAX request
            // FIXME: on response show message dialog and redirect to select_team.jsp
            // FIXME: on success deleteStorageData
    }

}
