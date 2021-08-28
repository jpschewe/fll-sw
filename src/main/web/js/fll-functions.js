"use-script";

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

/**
 * Return a promise that on success passes along the JSON from the response and rejects on an invalid HTTP response.
 */
function checkJsonResponse(response) {
    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }
    return response.json();
}

/**
 * Remove all children from an element.
 */
function removeChildren(element) {
    while (element.firstChild) {
        element.removeChild(element.lastChild);
    }
}


/**
 * Insert text at the caret position in a text area.
 *
 * @param area the text area element
 * @param text the text to insert
 */
function insertAtCaret(area, text) {
    var scrollPos = area.scrollTop;
    var strPos = 0;
    var br = ((area.selectionStart || area.selectionStart == '0') ? "ff" : (document.selection ? "ie" : false));
    if (br == "ie") {
        area.focus();
        var range = document.selection.createRange();
        range.moveStart('character', -(area.value.length));
        strPos = range.text.length;
    } else if (br == "ff") {
        strPos = area.selectionStart;
    }

    var front = (area.value).substring(0, strPos);
    var back = (area.value).substring(strPos, area.value.length);
    area.value = front + text + back;
    strPos = strPos + text.length;
    if (br == "ie") {
        area.focus();
        var range = document.selection.createRange();
        range.moveStart('character', -(area.value.length));
        range.moveStart('character', strPos);
        range.moveEnd('character', 0);
        range.select();
    } else if (br == "ff") {
        area.selectionStart = strPos;
        area.selectionEnd = strPos;
        area.focus();
    }
    area.scrollTop = scrollPos;
}

