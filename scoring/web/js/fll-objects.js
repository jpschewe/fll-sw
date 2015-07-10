/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

/**
 * Used for packaging up and sending to the server to put in the database. Needs
 * to match fll.web.report.finalist.FinalistDBRow.
 */
function FinalistDBRow(categoryName, hour, minute, teamNumber) {
  this.categoryName = categoryName;
  this.hour = hour;
  this.minute = minute;
  this.teamNumber = teamNumber;
}

/**
 * Used for packaging up and sending to the server to put in the database. Needs
 * to match fll.web.report.finalist.FinalistCategory.
 */
function FinalistCategory(categoryName, isPublic, room) {
  this.categoryName = categoryName;
  this.isPublic = isPublic;
  this.room = room;
}

/**
 * Needs to match fll.db.NonNumericNominees.
 */
function NonNumericNominees(categoryName, teamNumbers) {
  this.categoryName = categoryName;
  this.teamNumbers = teamNumbers;
}
