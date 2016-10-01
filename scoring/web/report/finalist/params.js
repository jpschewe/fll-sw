/*
 * Copyright (c) 2012INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

function setTimeField(field, value) {
  if (-1 == value) {
    field.val("");
  } else {
    field.val(value);
  }
}

function populateHeadToHeadTimes() {
  $("#head_head_times").empty();

  $
      .each(
          $.finalist.getPlayoffDivisions(),
          function(index, division) {
            var startHourElement = $("<input type='text' id='start_hour_"
                + index + "' size='2' maxlength='2'/>");
            startHourElement
                .change(function() {
                  var hour = parseInt($(this).val(), 10);
                  if (!/\S/.test($(this).val())) {
                    $.finalist.setPlayoffStartHour(division, -1);
                  } else if (isNaN(hour) || hour < 0 || hour > 23) {
                    alert("Hour must be a positive integer [0 - 23]");
                    setTimeField($(this), $.finalist
                        .getPlayoffStartHour(division));
                  } else {
                    $.finalist.setPlayoffStartHour(division, hour);
                  }
                });
            setTimeField(startHourElement, $.finalist
                .getPlayoffStartHour(division));

            var startMinuteElement = $("<input type='text' id='start_minute_"
                + index + "' size='2' maxlength='2'/>");
            startMinuteElement.change(function() {
              var value = parseInt($(this).val(), 10);
              if (!/\S/.test($(this).val())) {
                $.finalist.setPlayoffStartMinute(division, -1);
              } else if (isNaN(value) || value < 0 || value > 59) {
                alert("Minute must be a positive integer [0 - 59]");
                setTimeField($(this), $.finalist
                    .getPlayoffStartMinute(division));
              } else {
                $.finalist.setPlayoffStartMinute(division, value);
              }
            });
            setTimeField(startMinuteElement, $.finalist
                .getPlayoffStartMinute(division));

            var endHourElement = $("<input type='text' id='end_hour_" + index
                + "' size='2' maxlength='2'/>");
            endHourElement.change(function() {
              var hour = parseInt($(this).val(), 10);
              if (!/\S/.test($(this).val())) {
                $.finalist.setPlayoffEndHour(division, -1);
              } else if (isNaN(hour) || hour < 0 || hour > 23) {
                alert("Hour must be a positive integer [0 - 23]");
                setTimeField($(this), $.finalist.getPlayoffEndHour(division));
              } else {
                $.finalist.setPlayoffEndHour(division, hour);
              }
            });
            setTimeField(endHourElement, $.finalist.getPlayoffEndHour(division));

            var endMinuteElement = $("<input type='text' id='end_minute_"
                + index + "' size='2' maxlength='2'/>");
            endMinuteElement
                .change(function() {
                  var value = parseInt($(this).val(), 10);
                  if (!/\S/.test($(this).val())) {
                    $.finalist.setPlayoffEndMinute(division, -1);
                  } else if (isNaN(value) || value < 0 || value > 59) {
                    alert("Minute must be an integer [0 - 59]");
                    setTimeField($(this), $.finalist
                        .getPlayoffEndMinute(division));
                  } else {
                    $.finalist.setPlayoffEndMinute(division, value);
                  }
                });
            setTimeField(endMinuteElement, $.finalist
                .getPlayoffEndMinute(division));

            var paragraph = $("<p></p>");
            paragraph.append("<b>Head to Head bracket " + division + " times</b><br/>");
            paragraph.append("Start: ");
            paragraph.append(startHourElement);
            paragraph.append(" : ");
            paragraph.append(startMinuteElement);
            paragraph.append("<br/>");

            paragraph.append("End: ");
            paragraph.append(endHourElement);
            paragraph.append(" : ");
            paragraph.append(endMinuteElement);
            paragraph.append("<br/>");
            $("#head_head_times").append(paragraph);

          });

}

$(document).ready(
    function() {
      $.finalist.setupAppCache();

      $("#divisions").empty();

      var teams = $.finalist.getAllTeams();
      $.each($.finalist.getDivisions(), function(i, division) {
        var selected = "";
        if (division == $.finalist.getCurrentDivision()) {
          selected = " selected ";
        }
        var divisionOption = $("<option value='" + i + "'" + selected + ">"
            + division + "</option>");
        $("#divisions").append(divisionOption);

        // initialize categories with the auto selected teams
        var scoreGroups = $.finalist.getScoreGroups(teams, division);

        $.each($.finalist.getNumericCategories(), function(i, category) {
          $.finalist.initializeTeamsInNumericCategory(division, category,
              teams, scoreGroups);
        });// foreach numeric category
      }); // foreach division

      $.finalist.setCurrentDivision($.finalist.getDivisionByIndex($(
          "#divisions").val()));

      $("#hour").val($.finalist.getStartHour());
      $("#minute").val($.finalist.getStartMinute());
      $("#duration").val($.finalist.getDuration());

      $("#hour").change(function() {
        var hour = parseInt($(this).val(), 10);
        if (isNaN(hour)) {
          alert("Hour must be an integer");
          $("#hour").val($.finalist.getStartHour());
        } else {
          $.finalist.setStartHour(hour);
        }
      });

      $("#minute").change(function() {
        var minute = parseInt($(this).val(), 10);
        if (isNaN(minute)) {
          alert("Minute must be an integer");
          $("#minute").val($.finalist.getStartMinute());
        } else {
          $.finalist.setStartMinute(minute);
        }
      });

      $("#duration").change(function() {
        var duration = parseInt($(this).val(), 10);
        if (isNaN(duration)) {
          alert("Duration must be an integer");
          $("#duration").val($.finalist.getDuration());
        } else if (duration < 5) {
          alert("Duration must be at least 5 minutes");
          $("#duration").val($.finalist.getDuration());
        } else {
          $.finalist.setDuration(duration);
        }
      });

      $("#divisions").change(function() {
        var divIndex = $(this).val();
        var div = $.finalist.getDivisionByIndex(divIndex);
        $.finalist.setCurrentDivision(div);
      });

      populateHeadToHeadTimes();

      $.finalist.displayNavbar();

    }); // end ready function
