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
    divisions.forEach(function(division, i) {
        const divisionOption = document.createElement("option");
        divisionOption.setAttribute("value", i);
        if (division == finalist_module.getCurrentDivision()) {
            divisionOption.setAttribute("selected", "true");
        }
        divisionsElement.appendChild(divisionOption);

        // initialize categories with the auto selected teams
        const scoreGroups = finalist_module.getScoreGroups(teams, division);
        const numericCategories = finalist_module.getNumericCategories();
        numericCategories.forEach(function(category, _) {
            finalist_module.initializeTeamsInNumericCategory(division, category,
                teams, scoreGroups);
            finalist_module.saveToLocalStorage();
        });// foreach numeric category
    }); // foreach division

    finalist_module.setCurrentDivision(finalist_module.getDivisionByIndex(divisionsElement.value));

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

    finalistParamsModule.populateHeadToHeadTimes();

    finalist_module.saveToLocalStorage();

    finalist_module.displayNavbar();

}); // end ready function
