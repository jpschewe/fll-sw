/*
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

/*
 * Wrapper around local storage that decodes additional types and makes using namespaces easier.
 */
const fllStorage = {};

{
    if (typeof localStorage != 'object') {
        throw new Error("Local storage is not supported, cannot continue");
    }
    if (typeof JSON != 'object') {
        throw new Error("JSON support is missing, cannot continue");
    }

    function _reviver(_, value) {
        if (value === undefined) {
            return undefined;
        } else if (value === null) {
            return null;
        } else if (typeof value === 'object') {
            if (typeof JSJoda != undefined) {
                switch (value.dataType) {
                    case 'LocalDate':
                        return JSJoda.LocalDate.parse(value.value);
                    case 'LocalTime':
                        return JSJoda.LocalTime.parse(value.value);
                    case 'LocalDateTime':
                        return JSJoda.LocalDateTime.parse(value.value);
                    case 'Period':
                        return JSJoda.Period.parse(value.value);
                    case 'Duration':
                        return JSJoda.Duration.parse(value.value);
                }
            } // JSJoda support
        }

        return value;
    }

    function _replacer(key, value) {
        const originalObject = this[key];
        if (typeof JSJoda != undefined) {
            if (originalObject instanceof JSJoda.LocalDate) {
                return {
                    dataType: 'LocalDate',
                    value: originalObject.toJSON()
                };
            } else if (originalObject instanceof JSJoda.LocalTime) {
                return {
                    dataType: 'LocalTime',
                    value: originalObject.toJSON()
                };
            } else if (originalObject instanceof JSJoda.LocalDateTime) {
                return {
                    dataType: 'LocalDateTime',
                    value: originalObject.toJSON()
                };
            } else if (originalObject instanceof JSJoda.Period) {
                return {
                    dataType: 'Period',
                    value: originalObject.toJSON()
                };
            } else if (originalObject instanceof JSJoda.Duration) {
                return {
                    dataType: 'Duration',
                    value: originalObject.toJSON()
                };
            }
        } // JSJoda support

        return value;
    }

    /**
     * @param namespace the namespace used to avoid collisions with other storage
     * @param key where to store the data 
     * @param obj the value to store, must be convertable to JSON
     */
    fllStorage.set = function(namespace, key, obj) {
        const jsonData = JSON.stringify(obj, _replacer);
        try {
            localStorage.setItem(namespace + "." + key, jsonData);
        } catch (e) {
            throw new Error("Error storing data to local storage: " + e);
        }
    }

    /**
     * @param namespace the namespace used to avoid collisions with other storage
     * @param key where to retrieve the data from 
     * @return the value retrieved from storage, null if the key doesn't exist
     */
    fllStorage.get = function(namespace, key) {
        const jsonData = localStorage.getItem(namespace + "." + key);
        if (typeof jsonData == 'undefined' || jsonData == 'undefined') {
            return null;
        } else {
            const obj = JSON.parse(jsonData, _reviver);
            return obj;
        }
    }

    /**
     * Remove the value assocated with the specified key in the namespace.
     */
    fllStorage.remove = function(namespace, key) {
        localStorage.removeItem(namespace + "." + key);
    }

    /**
     * Clear all values in the specified namespace.
     */
    fllStorage.clearNamespace = function(namespace) {
        const prefix = namespace + ".";

        const toDelete = [];
        for (let i = 0; i < localStorage.length; ++i) {
            const key = localStorage.key(i);
            if (key && key.startsWith(prefix)) {
                toDelete.push(key);
            }
        }
        toDelete.forEach(key => localStorage.removeItem(key));
    }

}
