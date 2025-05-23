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
 * Start and end times are initialized to null and judgingGroups is empty.
 *
 * Matches fll.db.FinalistGroup.
 */
function FinalistGroup() {
    this.startTime = null;
    this.endTime = null;
    this.judgingGroups = [];
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
 * @param endTime JSJoda.LocalTime, end time of the row
 */
function FinalistDBRow(time, endTime) {
    this.time = time;
    this.endTime = endTime;
    this.categories = {}; // categoryName -> teamNumber
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
 * @param categories list of FinalistCategory
 * @param schedule list of FinalistDBRow
 */
function FinalistSchedule(categories, schedule) {
    this.categories = categories;
    this.schedule = schedule;
}

/**
 * Needs to match fll.db.FinalistNonNumericNominees.
 */
function FinalistNonNumericNominees(categoryName, nominees) {
    this.categoryName = categoryName;
    this.nominees = nominees;
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
