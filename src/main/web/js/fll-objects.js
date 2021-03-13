/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

"use strict";


/**
 * Start and end times are initialized to null.
 *
 * Matches fll.db.PlayoffSchedule.
 */
function PlayoffSchedule() {
    this.startTime = null;
    this.endTime = null;
}

/**
 * Start time initialized to 2:00pm.
 * Duration initialized to 20 minutes.
 *
 * Matches fll.web.report.finalist.FinalistScheduleParameters.
 */
function FinalistScheduleParameters() {
    this.startTime = JSJoda.LocalTime.of(14, 0);
    this.intervalMinutes = 20;
}

/**
 * Used for packaging up and sending to the server to put in the database. Needs
 * to match fll.web.report.finalist.FinalistDBRow.
 
 * @param time JSJoda.LocalTime, time of the row
 * @param categoryName the category of the row
 * @param teamNumber the team in the row
 */
function FinalistDBRow(categoryName, time, teamNumber) {
    this.categoryName = categoryName;
    this.time = time;
    this.teamNumber = teamNumber;
}

/**
 * Used for packaging up and sending to the server to put in the database. Needs
 * to match fll.web.report.finalist.FinalistCategory.
 */
function FinalistCategory(categoryName, room) {
    this.categoryName = categoryName;
    this.room = room;
}

/**
 * Matches fll.web.report.finalist.FinalistSchedule.
 *
 * @param division award group the schedule is for
 * @param categories list of FinalistCategory
 * @param schedule list of FinalistDBRow
 */
function FinalistSchedule(divison, categories, schedule) {
    this.division = division;
    this.categories = categories;
    this.schedule = schedule;
}

/**
 * Needs to match fll.db.NonNumericNominees.
 */
function NonNumericNominees(categoryName, nominees) {
    this.categoryName = categoryName;
    this.nominees = nominees;
}

/**
 * Needs to match fll.db.NonNumericNominees.Nominee
 */
function Nominee(teamNumber, judges) {
    this.teamNumber = teamNumber;
    this.judges = judges;
    if (typeof this.judges == 'undefined') {
        this.judges = [null]
    }
}

/**
 * Javascript parallel of fll.db.AwardWinner.
 */
function AwardWinner(name, awardGroup, teamNumber, description) {
    this.name = name;
    this.awardGroup = awardGroup;
    this.teamNumber = teamNumber;
    this.description = description;
}

/**
 * Javascript parallel of fll.db.OverallAwardWinner.
 */
function OverallAwardWinner(name, teamNumber, description) {
    this.name = name;
    this.teamNumber = teamNumber;
    this.description = description;
}

/**
 * Javascript parallel of fll.db.AdvancingTeam.
 */
function AdvancingTeam(teamNumber, group) {
    this.teamNumber = teamNumber;
    this.group = group;
}
