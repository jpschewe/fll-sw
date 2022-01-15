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

        const playoffSchedules = $.finalist.getPlayoffSchedules();
        Object.keys(playoffSchedules).forEach(function(bracketName) {
            const playoffSchedule = playoffSchedules[bracketName];
            const startTimeElement = document.createElement("input");
            startTimeElement.setAttribute("type", "time");
            startTimeElement.addEventListener('change', function() {
                const newLocalTime = finalistParamsModule.parseTime(this.value);
                $.finalist.setPlayoffStartTime(bracketName, newLocalTime);
                $.finalist.saveToLocalStorage();
            });
            finalistParamsModule.setTimeField(startTimeElement, playoffSchedule.startTime);


            const endTimeElement = document.createElement("input");
            endTimeElement.setAttribute("type", "time");
            endTimeElement.addEventListener('change', function() {
                const newLocalTime = finalistParamsModule.parseTime(this.value);
                $.finalist.setPlayoffEndTime(bracketName, newLocalTime);
                $.finalist.saveToLocalStorage();
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
        const startTime = $.finalist.getStartTime($.finalist.getCurrentDivision());
        document.getElementById("startTime").value = startTime.format(TIME_FORMATTER);

        const duration = $.finalist.getDuration($.finalist.getCurrentDivision());
        finalistParamsModule.setDurationDisplay(duration);
    };

    finalistParamsModule.setDurationDisplay = function(duration) {
        document.getElementById("duration").value = duration;
    };

} // end scope for page

document.addEventListener('DOMContentLoaded', function() {
    $.finalist.loadFromLocalStorage();

    const divisionsElement = document.getElementById("divisions");
    removeChildren(divisionsElement)

    const teams = $.finalist.getAllTeams();
    const divisions = $.finalist.getDivisions();
    divisions.forEach(function(division, i) {
        const divisionOption = document.createElement("option");
        divisionOption.setAttribute("value", i);
        if (division == $.finalist.getCurrentDivision()) {
            divisionOption.setAttribute("selected", "true");
        }
        divisionsElement.appendChild(divisionOption);

        // initialize categories with the auto selected teams
        const scoreGroups = $.finalist.getScoreGroups(teams, division);
        const numericCategories = $.finalist.getNumericCategories();
        numericCategories.forEach(function(category, _) {
            $.finalist.initializeTeamsInNumericCategory(division, category,
                teams, scoreGroups);
            $.finalist.saveToLocalStorage();
        });// foreach numeric category
    }); // foreach division

    $.finalist.setCurrentDivision($.finalist.getDivisionByIndex(divisionsElement.value));

    // before change listeners to avoid loop
    finalistParamsModule.updateDivision();


    document.getElementById("startTime").addEventListener('change', function() {
        const newLocalTime = finalistParamsModule.parseTime(this.value);
        $.finalist.setStartTime($.finalist.getCurrentDivision(), newLocalTime);
        $.finalist.saveToLocalStorage();
    });

    document.getElementById("duration").addEventListener('change', function() {
        const minutes = parseInt(this.value, 10);
        if (isNaN(minutes)) {
            alert("Duration must be an integer");
            finalistParamsModule.setDurationDisplay($.finalist.getDuration($.finalist.getCurrentDivision()));
        } else if (minutes < 5) {
            alert("Duration must be at least 5 minutes");
            finalistParamsModule.setDurationDisplay($.finalist.getDuration($.finalist.getCurrentDivision()));
        } else {
            $.finalist.setDuration($.finalist.getCurrentDivision(), minutes);
        }

        $.finalist.saveToLocalStorage();
    });

    document.getElementById("divisions").addEventListener('change', function() {
        const divIndex = this.value;
        const div = $.finalist.getDivisionByIndex(divIndex);
        $.finalist.setCurrentDivision(div);
        finalistParamsModule.updateDivision();

        $.finalist.saveToLocalStorage();
    });

    document.getElementById("next").addEventListener('click', function() {
        location.href = "non-numeric.html";
    });

    finalistParamsModule.populateHeadToHeadTimes();

    $.finalist.saveToLocalStorage();

    $.finalist.displayNavbar();

}); // end ready function
