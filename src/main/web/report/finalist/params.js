/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

const finalistParamsModule = {}

{
    const TIME_FORMATTER = JSJoda.DateTimeFormatter.ofPattern("HH:mm");

    /**
     * @param newDateStr string value to parse as a time
     * @return JSJoda.LocalTime or null
     */
    finalistParamsModule.parseTime = function(newDateStr) {
        if (null == newDateStr || undefined == newDateStr || "" == newDateStr) {
            return null;
        } else {
            const newLocalTime = JSJoda.LocalTime.parse(newDateStr);
            return newLocalTime;
        }
    };

    /**
     * @param field HTML Element
     * @param value JSJoda.LocalTime
     */
    finalistParamsModule.setTimeField = function(field, value) {
        if (value == undefined || value == null) {
            field.value = "";
        } else {
            const formatted = value.format(TIME_FORMATTER);
            field.value = formatted;
        }
    };

    finalistParamsModule.populateHeadToHeadTimes = function() {
        removeChildren(document.getElementById("head_head_times"));

        const playoffSchedules = finalist_module.getPlayoffSchedules();
        Object.keys(playoffSchedules).forEach(function(bracketName) {
            const playoffSchedule = playoffSchedules[bracketName];
            const startTimeElement = document.createElement("input");
            startTimeElement.setAttribute("type", "time");
            startTimeElement.addEventListener('change', function() {
                const newLocalTime = finalistParamsModule.parseTime(this.value);
                finalist_module.setPlayoffStartTime(bracketName, newLocalTime);
                finalist_module.saveToLocalStorage();
            });
            finalistParamsModule.setTimeField(startTimeElement, playoffSchedule.startTime);


            const endTimeElement = document.createElement("input");
            endTimeElement.setAttribute("type", "time");
            endTimeElement.addEventListener('change', function() {
                const newLocalTime = finalistParamsModule.parseTime(this.value);
                finalist_module.setPlayoffEndTime(bracketName, newLocalTime);
                finalist_module.saveToLocalStorage();
            });
            finalistParamsModule.setTimeField(endTimeElement, playoffSchedule.endTime);

            const paragraph = document.createElement("p");
            const h2hTimesLabel = document.createElement("b");
            paragraph.appendChild(h2hTimesLabel);
            h2hTimesLabel.appendChild(document.createTextNode("Head to Head bracket " + bracketName
                + " times"));
            paragraph.appendChild(document.createElement("br"));

            paragraph.appendChild(document.createTextNode("Start: "));
            paragraph.appendChild(startTimeElement);
            paragraph.appendChild(document.createElement("br"));

            paragraph.appendChild(document.createTextNode("End: "));
            paragraph.appendChild(endTimeElement);
            paragraph.appendChild(document.createElement("br"));

            document.getElementById("head_head_times").appendChild(paragraph);
        });

    };

    finalistParamsModule.populateFinalistGroupTimes = function() {
        const container = document.getElementById("finalist_group_times");
        removeChildren(container);

        // array of pairs of groupDiv and save function
        const groupData = [];

        const buttonBar = document.createElement("div");
        container.appendChild(buttonBar);

        const addButton = document.createElement("button");
        buttonBar.appendChild(addButton);
        addButton.setAttribute("type", "button");
        addButton.innerText = "Add Finalist Group";
        addButton.addEventListener("click", () => {
            const [groupDiv, saveFunc] = createFinalistGroupBlock(null, null);
            groupData.push([groupDiv, saveFunc]);
            container.appendChild(groupDiv);
        });

        // add existing groups
        for (const [name, group] of Object.entries(finalist_module.getFinalistGroups())) {
            const [groupDiv, saveFunc] = createFinalistGroupBlock(name, group);
            groupData.push([groupDiv, saveFunc]);
            container.appendChild(groupDiv);
        }

        const saveButton = document.createElement("button");
        buttonBar.appendChild(saveButton);
        saveButton.setAttribute("type", "button");
        saveButton.innerText = "Save Finalist Groups";
        saveButton.addEventListener("click", () => {
            const newGroups = {};
            for (const [groupDiv, saveFunc] of groupData) {
                if (groupDiv.parentNode == container) {
                    // still in the container
                    const [name, group] = saveFunc();
                    if (name) {
                        newGroups[name] = group;
                    }
                }
            }
            finalist_module.setFinalistGroups(newGroups);
            finalist_module.saveToLocalStorage();
        });
    };

    /**
     * @param {string} groupName if not null, name of the group for the block
     * @param {FinalistGroup} group if not null, used to populate the block 
     */
    function createFinalistGroupBlock(groupName, group) {
        const groupDiv = document.createElement("div");

        const nameDiv = document.createElement("div");
        groupDiv.appendChild(nameDiv);

        const nameLabel = document.createElement("span");
        nameDiv.appendChild(nameLabel);
        nameLabel.innerText = "Name: ";

        const nameInput = document.createElement("input");
        nameDiv.appendChild(nameInput);
        nameInput.setAttribute("type", "text");
        if (groupName) {
            nameInput.value = groupName;
        }

        //start time
        const startDiv = document.createElement("div");
        groupDiv.appendChild(startDiv);

        const startLabel = document.createElement("span");
        startDiv.appendChild(startLabel);
        startLabel.innerText = "Start Time";

        const startInput = document.createElement("input");
        startDiv.appendChild(startInput);
        startInput.setAttribute("type", "time");
        if (group && group.startTime) {
            finalistParamsModule.setTimeField(startInput, group.startTime);
        }

        //end time
        const endDiv = document.createElement("div");
        groupDiv.appendChild(endDiv);

        const endLabel = document.createElement("span");
        endDiv.appendChild(endLabel);
        endLabel.innerText = "End Time";

        const endInput = document.createElement("input");
        endDiv.appendChild(endInput);
        endInput.setAttribute("type", "time");
        if (group && group.endTime) {
            finalistParamsModule.setTimeField(endInput, group.endTime);
        }


        // checkboxes for judging groups
        const judgingGroupDiv = document.createElement("div");
        groupDiv.appendChild(judgingGroupDiv);
        let judgingGroupIndex = 1;
        const judgingGroupInputs = [];
        for (const judgingGroup of finalist_module.getJudgingGroups()) {
            const div = document.createElement("div");
            judgingGroupDiv.appendChild(div);

            const inputId = `judgingGroup${judgingGroupIndex}`;

            const label = document.createElement("label");
            div.appendChild(label);
            label.innerText = judgingGroup;
            label.setAttribute("for", inputId);

            const input = document.createElement("input");
            div.appendChild(input);
            input.setAttribute("type", "checkbox");
            input.id = inputId;
            input.value = judgingGroup;
            if (group && group.judgingGroups.indexOf(judgingGroup) >= 0) {
                input.checked = true;
            }
            judgingGroupInputs.push(input);
        }


        const buttonBar = document.createElement("div");
        groupDiv.appendChild(buttonBar);

        const removeButton = document.createElement("button");
        buttonBar.appendChild(removeButton);
        removeButton.setAttribute("type", "button");
        removeButton.innerText = "Remove Group";
        removeButton.addEventListener("click", () => {
            const groupParent = groupDiv.parentNode;
            if (groupParent) {
                groupParent.removeChild(groupDiv);
            }
        });

        const saveFunc = function() {
            const groupName = nameInput.value;
            if (!groupName) {
                alert("All groups must be named");
                return [null, null];
            }

            const newGroup = new FinalistGroup();
            newGroup.startTime = finalistParamsModule.parseTime(startInput.value);
            newGroup.endTime = finalistParamsModule.parseTime(endInput.value);

            for (const input of judgingGroupInputs) {
                if (input.checked) {
                    newGroup.judgingGroups.push(input.value);
                }
            }

            return [groupName, newGroup];
        };

        return [groupDiv, saveFunc];
    }

    finalistParamsModule.updateDivision = function() {
        const startTime = finalist_module.getStartTime(finalist_module.getCurrentDivision());
        document.getElementById("startTime").value = startTime.format(TIME_FORMATTER);

        const duration = finalist_module.getDuration(finalist_module.getCurrentDivision());
        finalistParamsModule.setDurationDisplay(duration);
    };

    finalistParamsModule.setDurationDisplay = function(duration) {
        document.getElementById("duration").value = duration;
    };

} // end scope for page

document.addEventListener('DOMContentLoaded', function() {
    finalist_module.loadFromLocalStorage();

    const divisionsElement = document.getElementById("divisions");
    removeChildren(divisionsElement)

    const teams = finalist_module.getAllTeams();
    const divisions = finalist_module.getDivisions();
    for (const [i, division] of enumerate(divisions)) {
        const divisionOption = document.createElement("option");
        divisionOption.setAttribute("value", i);
        divisionOption.innerText = division;
        if (division == finalist_module.getCurrentDivision()) {
            divisionOption.setAttribute("selected", "true");
        }
        divisionsElement.appendChild(divisionOption);

        // initialize categories with the auto selected teams
        const scoreGroups = finalist_module.getScoreGroups(teams, division);
        const numericCategories = finalist_module.getNumericCategories();
        for (const category of numericCategories) {
            finalist_module.initializeTeamsInNumericCategory(division, category,
                teams, scoreGroups);
            finalist_module.saveToLocalStorage();
        } // foreach numeric category
    } // foreach division

    finalist_module.setCurrentDivision(finalist_module.getDivisionByIndex(divisionsElement.value));
    finalist_module.saveToLocalStorage();

    // before change listeners to avoid loop
    finalistParamsModule.updateDivision();


    document.getElementById("startTime").addEventListener('change', function() {
        const newLocalTime = finalistParamsModule.parseTime(this.value);
        finalist_module.setStartTime(finalist_module.getCurrentDivision(), newLocalTime);
        finalist_module.saveToLocalStorage();
    });

    document.getElementById("duration").addEventListener('change', function() {
        const minutes = parseInt(this.value, 10);
        if (isNaN(minutes)) {
            alert("Duration must be an integer");
            finalistParamsModule.setDurationDisplay(finalist_module.getDuration(finalist_module.getCurrentDivision()));
        } else if (minutes < 5) {
            alert("Duration must be at least 5 minutes");
            finalistParamsModule.setDurationDisplay(finalist_module.getDuration(finalist_module.getCurrentDivision()));
        } else {
            finalist_module.setDuration(finalist_module.getCurrentDivision(), minutes);
        }

        finalist_module.saveToLocalStorage();
    });

    document.getElementById("divisions").addEventListener('change', function() {
        const divIndex = this.value;
        const div = finalist_module.getDivisionByIndex(divIndex);
        finalist_module.setCurrentDivision(div);
        finalistParamsModule.updateDivision();

        finalist_module.saveToLocalStorage();
    });

    document.getElementById("next").addEventListener('click', function() {
        location.href = "non-numeric.html";
    });

    if (finalist_module.getRunningHead2Head()) {
        document.getElementById('head_to_head').classList.remove('fll-sw-ui-inactive');
        document.getElementById('finalist_groups').classList.add('fll-sw-ui-inactive');
        finalistParamsModule.populateHeadToHeadTimes();
    } else {
        document.getElementById('head_to_head').classList.add('fll-sw-ui-inactive');
        document.getElementById('finalist_groups').classList.remove('fll-sw-ui-inactive');
        finalistParamsModule.populateFinalistGroupTimes();
    }

    finalist_module.saveToLocalStorage();

    finalist_module.displayNavbar();

}); // end ready function
