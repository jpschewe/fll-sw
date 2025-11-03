"use strict";

document.addEventListener('DOMContentLoaded', () => {
    const passwordField = document.getElementById("pass");

    const showPassword = document.getElementById('show_password');
    showPassword.addEventListener('change', () => {
        if (showPassword.checked) {
            passwordField.setAttribute("type", "text");
        } else {
            passwordField.setAttribute("type", "password");
        }
    });
});
