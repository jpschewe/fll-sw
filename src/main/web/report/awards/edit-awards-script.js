"use strict";


const awardsScriptModule = {};

{

    function syncSponsorsDisabledState() {
        const specifiedEle = document.getElementById("sponsors_specified");
        const addSponsorButton = document.getElementById("add_sponsor");
        sponsors.querySelectorAll("input, button").forEach(ele => { ele.disabled = !specifiedEle.checked; })
        addSponsorButton.disabled = !specifiedEle.checked;
    }

    /**
     * @param text the name that appears in text
     * @param title the user-friendly name of the macro
     */
    awardsScriptModule.createMacro = function(text, title) {
        return {
            text: text,
            title: title
        };
    };



    /**
     * @param macros array of macro objects
     * @param sectionName name of the section, must be a valid HTML identifier
     * @param sectionSpecified true if the section is currently specified 
     */
    awardsScriptModule.configureTextEntry = function(macros, sectionName, sectionSpecified) {
        const textElement = document.getElementById(sectionName + "_text");
        const macrosList = document.getElementById(sectionName + "_macros");

        const checkbox = document.getElementById(sectionName + "_specified");
        if (checkbox) {
            checkbox.addEventListener('change', () => {
                textElement.disabled = !checkbox.checked;

                if (macrosList) {
                    macros.forEach(function(macro) {
                        const buttonId = sectionName + "_" + macro.text;
                        const button = document.getElementById(buttonId);
                        button.disabled = !checkbox.checked;
                    });
                }
            });

            checkbox.checked = sectionSpecified;
        }

        if (macrosList) {
            // some sections don't have the macro buttons

            removeChildren(macrosList);

            const macrosLabel = document.createElement("div");
            macrosList.appendChild(macrosLabel);
            macrosLabel.innerText = "Macros";

            macros.forEach(function(macro) {
                const div = document.createElement("div");
                macrosList.appendChild(div);

                const button = document.createElement("button");
                div.appendChild(button);
                button.setAttribute("type", "button");
                button.setAttribute("id", sectionName + "_" + macro.text);
                button.innerText = macro.title;
                button.addEventListener('click', () => {
                    insertAtCaret(textElement, '${' + macro.text + '}');
                });

            });

            macros.forEach(function(macro) {
                const buttonId = sectionName + "_" + macro.text;
                const button = document.getElementById(buttonId);
                button.disabled = !sectionSpecified;
            });
        }

        textElement.disabled = !sectionSpecified;
    };

    /**
     * Configure the checkbox and text entry for a presenter. Looks for 
     * sectionName_presenter_text and sectionName_presenter_specified.
     * 
     * @param sectionName name of the section, must be a valid HTML identifier
     * @param presenterSpecified true if the presenter is currently specified 
     */
    awardsScriptModule.configurePresenterEntry = function(sectionName, presenterSpecified) {
        const textElement = document.getElementById(sectionName + "_presenter_text");

        const checkbox = document.getElementById(sectionName + "_presenter_specified");
        if (checkbox) {
            checkbox.addEventListener('change', () => {
                textElement.disabled = !checkbox.checked;
            });

            checkbox.checked = presenterSpecified;
        }

        textElement.disabled = !presenterSpecified;
    };

    awardsScriptModule.configureMacroEntry = function(macro, macroSpecified, macroValue) {
        const checkbox = document.getElementById(macro.text + "_specified");
        if (checkbox) {
            const input = document.getElementById(macro.text + "_value");

            checkbox.addEventListener('change', () => {
                input.disabled = !checkbox.checked;
            });

            if (macroSpecified) {
                input.value = macroValue;
            }
            checkbox.checked = macroSpecified;
            input.disabled = !macroSpecified;
        }
    };

    awardsScriptModule.setSponsorsSpecified = function(sponsors_specified) {
        const specifiedEle = document.getElementById("sponsors_specified");
        specifiedEle.checked = sponsors_specified;
        syncSponsorsDisabledState();
    };

    awardsScriptModule.addSponsor = function(sponsor_name) {
        const sponsors = document.getElementById("sponsors");

        const sponsor = document.createElement("div");
        sponsors.appendChild(sponsor);

        const moveUp = document.createElement("button");
        sponsor.appendChild(moveUp);
        moveUp.setAttribute("type", "button");
        moveUp.innerText = "Move Up";
        moveUp.addEventListener("click", () => {
            const prev = sponsor.previousElementSibling;
            if (prev) {
                sponsors.insertBefore(sponsor, prev);
            }
        });

        const moveDown = document.createElement("button");
        sponsor.appendChild(moveDown);
        moveDown.setAttribute("type", "button");
        moveDown.innerText = "Move Down";
        moveDown.addEventListener("click", () => {
            const next = sponsor.nextElementSibling;
            if (next) {
                sponsors.insertBefore(next, sponsor);
            }
        });

        const deleteButton = document.createElement("button");
        sponsor.appendChild(deleteButton);
        deleteButton.setAttribute("type", "button");
        deleteButton.innerText = "Delete";
        deleteButton.addEventListener("click", () => {
            sponsors.removeChild(sponsor);
        });

        const sponsorInput = document.createElement("input");
        sponsor.appendChild(sponsorInput);
        if (sponsor_name) {
            sponsorInput.value = sponsor_name;
        }

        syncSponsorsDisabledState();
    };

    /**
     * Make any needed changes to fields before submitting the form. 
     */
    function finalizeFormData() {
        finalizeSponsorFields();
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

    function initSponsorsFields() {
        const specifiedEle = document.getElementById("sponsors_specified");
        const addSponsorButton = document.getElementById("add_sponsor");

        specifiedEle.addEventListener("change", () => {
            const sponsors = document.getElementById("sponsors");
            sponsors.querySelectorAll("input, button").forEach(ele => { ele.disabled = !specifiedEle.checked; });
            addSponsorButton.disabled = !specifiedEle.checked;
        });


        addSponsorButton.addEventListener("click", () => {
            awardsScriptModule.addSponsor();
        });
    }

    document.addEventListener('DOMContentLoaded', () => {
        document.getElementById("submit_data").addEventListener("click", finalizeFormData);

        initSponsorsFields();

        awardsScriptModule.init();
    });

}