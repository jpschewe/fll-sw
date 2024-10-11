"use strict";

/**
 * Make any needed changes to fields before submitting the form. 
 */
function finalizeFormData() {

    const container = document.getElementById("award_order");
    const children = container.children;
    for (let index = 0; index < children.length; ++index) {
        const div = children[index];
        const inputElement = div.querySelectorAll("input")[0]
        inputElement.setAttribute("value", index);
    }

}

function move_up_clicked() {

}

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById("submit_data").addEventListener("click", finalizeFormData);

    const container = document.getElementById("award_order");

    for (const move_up_button of container.querySelectorAll(".move_up")) {
        move_up_button.addEventListener("click", () => {
            const div = move_up_button.parentElement;
            const prev = div.previousElementSibling;
            if (prev) {
                container.insertBefore(div, prev);
            }
        });
    }

    for (const move_down_button of container.querySelectorAll(".move_down")) {
        move_down_button.addEventListener("click", () => {
            const div = move_down_button.parentElement;
            const next = div.nextElementSibling;
            if (next) {
                container.insertBefore(next, div);
            }
        });
    }


});
