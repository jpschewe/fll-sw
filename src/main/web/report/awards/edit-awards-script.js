"use strict";


const awardsScriptModule = {};

{

    function syncSponsorsDisabledState() {
        const sponsors = document.getElementById("sponsors");
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

            const macrosContainer = document.createElement("div");
            macrosList.appendChild(macrosContainer);
            macrosContainer.classList.add("macros-container");

            macros.forEach(function(macro) {
                const div = document.createElement("div");
                macrosContainer.appendChild(div);

                const button = document.createElement("button");
                div.appendChild(button);
                button.classList.add('macro-button');
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

    /**
     * @param baseName the base name for the field, _specified and _value will be appended
     * @param specified boolean stating if the parameter has been specified
     * @param value the value of the parameter 
     */
    awardsScriptModule.configureParameterEntry = function(baseName, specified, value) {
        const checkbox = document.getElementById(baseName + "_specified");
        if (checkbox) {
            const input = document.getElementById(baseName + "_value");

            checkbox.addEventListener('change', () => {
                input.disabled = !checkbox.checked;
            });

            input.value = value;
            checkbox.checked = specified;
            input.disabled = !specified;
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

    awardsScriptModule.setAwardOrderSpecified = function(specified) {
        const specifiedEle = document.getElementById("awardOrder_specified");
        specifiedEle.checked = specified;
        syncAwardOrderDisabledState();
    };

    function syncAwardOrderDisabledState() {
        const container = document.getElementById("award_order");
        const specifiedEle = document.getElementById("awardOrder_specified");
        container.querySelectorAll("button").forEach(ele => { ele.disabled = !specifiedEle.checked; })
    }

    awardsScriptModule.addToAwardOrder = function(category_title) {
        const container = document.getElementById("award_order");

        const element = document.createElement("div");
        container.appendChild(element);

        const moveUp = document.createElement("button");
        element.appendChild(moveUp);
        moveUp.setAttribute("type", "button");
        moveUp.innerText = "Move Up";
        moveUp.addEventListener("click", () => {
            const prev = element.previousElementSibling;
            if (prev) {
                container.insertBefore(element, prev);
            }
        });

        const moveDown = document.createElement("button");
        element.appendChild(moveDown);
        moveDown.setAttribute("type", "button");
        moveDown.innerText = "Move Down";
        moveDown.addEventListener("click", () => {
            const next = element.nextElementSibling;
            if (next) {
                container.insertBefore(next, element);
            }
        });

        const hiddenInput = document.createElement("input");
        element.appendChild(hiddenInput);
        hiddenInput.value = category_title;
        hiddenInput.setAttribute("type", "hidden");

        const text = document.createTextNode(category_title);
        element.appendChild(text);

        syncAwardOrderDisabledState();
    };

}