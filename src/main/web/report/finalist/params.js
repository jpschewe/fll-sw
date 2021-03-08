/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";

const finalistParamsModule = {
    initTimePicker: function(field, scrollDefault) {
        field.timepicker({
            step: 5,
            minTime: '8:00am',
            maxTime: '10:00pm',
            scrollDefault: scrollDefault
        });
    },

    setTimeField: function(field, value) {
        if (value == undefined || value == null) {
            field.timepicker('setTime', null);
        } else {
            const newDate = JSJoda.convert(value.atDate(JSJoda.LocalDate.now())).toDate();
            field.timepicker('setTime', newDate);
        }
    },

    populateHeadToHeadTimes: function() {
        $("#head_head_times").empty();

        const defaultTime = new Date();

        $
            .each(
                $.finalist.getPlayoffSchedules(),
                function(bracketName, playoffSchedule) {
                    const startTimeElement = $("<input type='text' size='8'/>");
                    finalistParamsModule.initTimePicker(startTimeElement, defaultTime);

                    startTimeElement.on('changeTime', function() {
                        const newDate = startTimeElement.timepicker('getTime');
                        const newLocalTime = JSJoda.LocalTime.of(newDate.getHours(), newDate.getMinutes());
                        $.finalist.setPlayoffStartTime(bracketName, newLocalTime);
                    });

                    finalistParamsModule.setTimeField(startTimeElement, playoffSchedule.startTime);


                    const endTimeElement = $("<input type='text' size='8' />");
                    finalistParamsModule.initTimePicker(endTimeElement, defaultTime);

                    endTimeElement.on('changeTime', function() {
                        const newDate = endTimeElement.timepicker('getTime');
                        const newLocalTime = JSJoda.LocalTime.of(newDate.getHours(), newDate.getMinutes());
                        $.finalist.setPlayoffEndTime(bracketName, newLocalTime);
                    });

                    finalistParamsModule.setTimeField(endTimeElement, playoffSchedule.endTime);

                    const paragraph = $("<p></p>");
                    paragraph.append("<b>Head to Head bracket " + bracketName
                        + " times</b><br/>");
                    paragraph.append("Start: ");
                    paragraph.append(startTimeElement);
                    paragraph.append("<br/>");

                    paragraph.append("End: ");
                    paragraph.append(endTimeElement);
                    paragraph.append("<br/>");
                    $("#head_head_times").append(paragraph);

                });

    },

    updateDivision: function() {
        const startTime = $.finalist.getStartTime($.finalist.getCurrentDivision());
        const startTimeDate = JSJoda.convert(startTime.atDate(JSJoda.LocalDate.now())).toDate();
        $("#startTime").timepicker('setTime', startTimeDate);

        const duration = $.finalist.getDuration($.finalist.getCurrentDivision());
        finalistParamsModule.setDurationDisplay(duration);
    },

    setDurationDisplay: function(duration) {
        const durationMinutes = duration.toMinutes();
        $("#duration").val(durationMinutes);
    }

} // end scope for page

$(document).ready(
    function() {
        $("#divisions").empty();

        const teams = $.finalist.getAllTeams();
        $.each($.finalist.getDivisions(), function(i, division) {
            let selected = "";
            if (division == $.finalist.getCurrentDivision()) {
                selected = " selected ";
            }
            const divisionOption = $("<option value='" + i + "'" + selected + ">"
                + division + "</option>");
            $("#divisions").append(divisionOption);

            // initialize categories with the auto selected teams
            const scoreGroups = $.finalist.getScoreGroups(teams, division);

            $.each($.finalist.getNumericCategories(), function(i, category) {
                $.finalist.initializeTeamsInNumericCategory(division, category,
                    teams, scoreGroups);
            });// foreach numeric category
        }); // foreach division

        $.finalist.setCurrentDivision($.finalist.getDivisionByIndex($(
            "#divisions").val()));

        $("#startTime").timepicker({
            step: 5,
            minTime: '8:00am',
            maxTime: '10:00pm',
        });

        // before change listeners to avoid loop
        finalistParamsModule.updateDivision();


        $("#startTime").on('changeTime', function() {
            const newDate = $('#startTime').timepicker('getTime');
            const newLocalTime = JSJoda.LocalTime.of(newDate.getHours(), newDate.getMinutes());
            $.finalist.setStartTime($.finalist.getCurrentDivision(), newLocalTime);
        });

        $("#duration").change(function() {
            const minutes = parseInt($(this).val(), 10);
            if (isNaN(minutes)) {
                alert("Duration must be an integer");
                finalistParamsModule.setDurationDisplay($.finalist.getDuration($.finalist.getCurrentDivision()));
            } else if (minutes < 5) {
                alert("Duration must be at least 5 minutes");
                finalistParamsModule.setDurationDisplay($.finalist.getDuration($.finalist.getCurrentDivision()));
            } else {
                const duration = JSJoda.Duration.ofMinutes(minutes);
                $.finalist.setDuration($.finalist.getCurrentDivision(), duration);
            }
        });

        $("#divisions").change(function() {
            const divIndex = $(this).val();
            const div = $.finalist.getDivisionByIndex(divIndex);
            $.finalist.setCurrentDivision(div);
            finalistParamsModule.updateDivision();
        });

        $("#next").click(function() {
            location.href = "non-numeric.html";
        });

        finalistParamsModule.populateHeadToHeadTimes();

        $.finalist.displayNavbar();

    }); // end ready function
