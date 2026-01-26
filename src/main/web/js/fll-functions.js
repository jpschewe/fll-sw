"use strict";

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
    for (var i = 0;i < ar.length;i++) {
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
        throw new Error(`HTTP error! status: ${response.statusText} (${response.status}) from ${response.url}`);
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

/**
 * @return true if val is a number 
 */
function isNumeric(val) {
    return Number(parseFloat(val)) === val;
}

/**
 * Compute the height of an element including the margins.
 */
function computeHeight(element) {
    const styles = window.getComputedStyle(element);
    const margin = parseFloat(styles['marginTop']) + parseFloat(styles['marginBottom']);
    const height = element.offsetHeight + margin;
    return height;
}

/**
 * Open the link in a new window without any browser buttons. Will fall back to standard link
 * opening if the popup is blocked. The window will attempt to be maximized and put in the top left of the screen.
 * 
 * @param link an anchor DOM object
 */
function openMinimalBrowser(link) {
    const w = window.open(link.href,
        link.target || "_blank",
        'menubar=no,toolbar=no,location=no,directories=no,status=no,scrollbars=no,resizable=no,dependent,left=0,top=0,fullscreen=yes');
    if (w) {
        w.moveTo(0, 0);
        if (w.outerWidth < screen.availWidth || w.outerHeight < screen.availHeight) {
            w.resizeTo(screen.availWidth, screen.availHeight);
        }
    }
    return w ? false : true; // allow the link to work if popup is blocked
}

/**
 * Generates [index, value] from iterable.
 * Based on code from https://stackoverflow.com/questions/10179815/get-loop-counter-index-using-for-of-syntax-in-javascript
 */
function* enumerate(iterable) {
    let i = 0;
    for (const x of iterable) {
        yield [i, x];
        ++i;
    }
}

/**
 * Parse a boolean from a string. Matches Java's Boolean.parseBoolean().
 */
function parseBoolean(str) {
    return /^true$/i.test(str);
}

/**
 * If value matches an option in select, select it, otherwise do nothing.
 */
function setSelectValue(select, value) {
    for (const option of select.options) {
        if (option.value == value) {
            select.value = option.value;
            return;
        }
    }
}

/**
 * Send a JSON payload to the specified URL.
 * 
 * @param {String} url the URL to send the data to
 * @param {String} method the HTTP method to use
 * @param {Object} payloadObject the object to send, will be converted to JSON
 * @returns promise from fetch
 */
function uploadJsonData(url, method, payloadObject) {
    const dataToUpload = JSON.stringify(payloadObject);
    return fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: dataToUpload
    });
}


/**
 * Center text within width with non-breaking spaces on either side.
 * If text is wider than width, return text.
 */
function centerText(text, width) {
    const spaceLength = width - text.length;
    if (spaceLength > 0) {
        const paddingLeft = Math.floor(spaceLength / 2);
        const paddingRight = spaceLength - paddingLeft;
        const paddedText = '&nbsp;'.repeat(paddingLeft) + text + '&nbsp;'.repeat(paddingRight);
        return paddedText;
    } else {
        return text;
    }
}

function parseBoolean(str) {
    if (str === undefined || str === null) {
        return false;
    }
    const lowerCaseStr = str.toLowerCase();
    return (lowerCaseStr === "true" || lowerCaseStr === "yes" || lowerCaseStr === "1");
}



/**
 * @param {Element} scrollElement the element to scroll
 * @param {Element} topElement the element inside of scrollElement that signals the top of the scroll area
 * @param {Element} bottomElement the element inside of scrollElement that signals the bottom of the scroll area
 * @param {number} endPauseSeconds the number of seconds to pause at the top and bottom before switching directions
 * @param {number} pixelsToScroll the number of pixels to scroll each secondsBetweenScrolls
 * @param {number} secondsBetweenScrolls the number of seconds to pause between executing scroll commands
 */
function startEndlessScroll(scrollElement, topElement, bottomElement, endPauseSeconds, pixelsToScroll, secondsBetweenScrolls) {
    let scrollingDown = true;
    let scrollingPause = false;
    let scrollingPauseStartTimestamp = 0;
    let prevScrollTimestamp = 0;

    const callback = (entries, observer) => {
        let topVisible = false;
        let bottomVisible = false;
        entries.forEach((entry) => {
            //const isTop = topElement == entry.target;
            //const isBottom = bottomElement == entry.target;
            //console.log(`Intersecting: ${entry.isIntersecting} ratio: ${entry.intersectionRatio} element: ${entry.target} top: ${isTop} bottom ${isBottom}`);
            if (entry.isIntersecting) {
                if (topElement == entry.target) {
                    topVisible = true;
                } else if (bottomElement == entry.target) {
                    bottomVisible = true;
                }
            }
        });

        if (topVisible && bottomVisible) {
            // Handle initial state where both top and bottom are marked as visible.
            // In this case we want to scroll down.
            scrollingDown = true;
        } else if (topVisible) {
            // hit the top, time to start scrolling down
            scrollingPause = true;
            scrollingDown = true;
        } else if (bottomVisible) {
            // hit the bottom, time to start scrolling up
            scrollingPause = true;
            scrollingDown = false;
        }
    };

    const options = {
        root: document.getElementById("all_teams"),
        threshold: 1.0
    }
    const observer = new IntersectionObserver(callback, options);
    observer.observe(topElement);
    observer.observe(bottomElement);

    const animationCallback = (timestamp) => {
        if (scrollingPause) {
            if (scrollingPauseStartTimestamp > 0) {
                const pauseDiffSeconds = (timestamp - scrollingPauseStartTimestamp) / 1000.0;
                if (pauseDiffSeconds > endPauseSeconds) {
                    scrollingPause = false;
                    scrollingPauseStartTimestamp = 0;
                }
            } else {
                scrollingPauseStartTimestamp = timestamp;
            }
        }


        const scrollDiffSeconds = (timestamp - prevScrollTimestamp) / 1000.0;
        if (!scrollingPause && scrollDiffSeconds >= secondsBetweenScrolls) {
            const scrollAmount = scrollingDown ? pixelsToScroll : -1 * pixelsToScroll;
            scrollElement.scrollBy(0, scrollAmount);
            prevScrollTimestamp = timestamp;
        }

        requestAnimationFrame(animationCallback);
    };

    requestAnimationFrame(animationCallback);
}
