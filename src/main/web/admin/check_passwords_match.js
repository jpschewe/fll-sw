/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

const CheckPasswordsModule = {}

{
    /**
     * Validates elements with ids "pass" and "pass_check" have the same value.
     * @return true if they match
     */
    CheckPasswordsModule.validatePasswordsMatch = function() {
        const passElement = document.getElementById("pass");
        const passCheckElement = document.getElementById("pass_check")
        if (passElement.value != passCheckElement.value) {
            alert("The password entries must match");
            return false;
        } else {
            return true;
        }
    }
}
