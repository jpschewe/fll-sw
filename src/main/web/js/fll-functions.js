; "use-script";

/**
 * Log the console if it's available.
 * 
 * @param str
 *          the message to log
 */
function _log(str) {
    if (typeof (console) != 'undefined') {
        console.log(str);
    }
}

/**
 * Remove obj from the array ar. Uses identify equals (===).
 * 
 * @param ar
 *          the array (modified)
 * @param obj
 *          the object to remove
 */
function removeFromArray(ar, obj) {
    for (var i = 0; i < ar.length; i++) {
        if (ar[i] === obj) {
            ar.splice(i, 1);
        }
    }
}

/**
 * Determine if using TLS or not. Returns "wss:" or "ws:".
 */
function getWebsocketProtocol() {
    if ("https:" == document.location.protocol) {
        return "wss:";
    } else {
        return "ws:";
    }
}

/**
 * Check if a string is empty, null or undefined.
 */
function isBlank(str) {
    return (!str || /^\s*$/.test(str));
}

