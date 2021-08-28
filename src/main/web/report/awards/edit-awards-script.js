"use strict";


const awardsScriptModule = {};

{

    /**
     * @param name the macro name that codes in the text
     * @param title the user-friendly name of the macro
     */
    awardsScriptModule.createMacro = function(name, title) {
        return {
            name: name,
            title: title
        };
    }


    /**
     * @param macros array of macro objects
     * @param sectionName name of the section, must be a valid HTML identifier
     * @param sectionSpecified true if the section is currently specified 
     */
    awardsScriptModule.configureTextEntry = function(macros, sectionName, sectionSpecified) {
        const textElement = document.getElementById(sectionName + "_text");

        const checkbox = document.getElementById(sectionName + "_specified");
        if (checkbox) {
            checkbox.addEventListener('change', () => {
                textElement.disabled = !checkbox.checked;
                macros.forEach(function(macro) {
                    const buttonId = sectionName + "_" + macro.name;
                    const button = document.getElementById(buttonId);
                    button.disabled = !checkbox.checked;
                });
            });

            checkbox.checked = sectionSpecified;
        }

        const macrosList = document.getElementById(sectionName + "_macros");
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
            button.setAttribute("id", sectionName + "_" + macro.name);
            button.innerText = macro.title;
            button.addEventListener('click', () => {
                insertAtCaret(textElement, '${' + macro.name + '}');
            });

        });

        textElement.disabled = !sectionSpecified;

        macros.forEach(function(macro) {
            const buttonId = sectionName + "_" + macro.name;
            const button = document.getElementById(buttonId);
            button.disabled = !sectionSpecified;
        });

    };

    document.addEventListener('DOMContentLoaded', () => {
        awardsScriptModule.init();
    });

}