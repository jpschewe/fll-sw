"use strict";


/**
 * Make any needed changes to fields before submitting the form. 
 */
function finalizeFormData() {
    finalizeSponsorFields();
    finalizeAwardOrderFields();
}


/** 
 * Set names of award order fields to sort properly when parsed on the server.
 */
function finalizeAwardOrderFields() {
    const container = document.getElementById("award_order");
    const children = container.children;
    for (var index = 0; index < children.length; ++index) {
        const div = children[index];
        const inputElement = div.querySelectorAll("input")[0]
        inputElement.setAttribute("name", "award_order_" + index);
    }
}


/** 
 * Set names of sponsor fields to sort properly when parsed on the server.
 */
function finalizeSponsorFields() {
    const sponsors = document.getElementById("sponsors");
    const children = sponsors.children;
    for (var index = 0; index < children.length; ++index) {
        const sponsor = children[index];
        const sponsorInput = sponsor.querySelectorAll("input")[0]
        sponsorInput.setAttribute("name", "sponsor_" + index);
    }
}

function initAwardOrder() {
    const awardOrderSpecifiedEle = document.getElementById("awardOrder_specified");
    awardOrderSpecifiedEle.addEventListener("change", () => {
        awardsScriptModule.setAwardOrderSpecified(awardOrderSpecifiedEle.checked);
    });
}

function initSponsorsFields() {
    const specifiedEle = document.getElementById("sponsors_specified");
    const addSponsorButton = document.getElementById("add_sponsor");

    specifiedEle.addEventListener("change", () => {
        awardsScriptModule.setSponsorsSpecified(specifiedEle.checked);
    });


    addSponsorButton.addEventListener("click", () => {
        awardsScriptModule.addSponsor();
    });
}

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById("submit_data").addEventListener("click", finalizeFormData);

    initSponsorsFields();

    initAwardOrder();

    pageInit();
});
