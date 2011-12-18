/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

/**
 * Helper class to gather up information about event division changes.
 */
public final class EventDivisionInfo {

  /**
   * @param teamNumber
   * @param teamName
   * @param division
   * @param division2
   */
  public EventDivisionInfo(final int teamNumber,
                           final String teamName,
                           final String division,
                           final String eventDivision) {
    this.teamNumber = teamNumber;
    this.teamName = teamName;
    this.division = division;
    this.eventDivision = eventDivision;
  }

  private final int teamNumber;

  public int getTeamNumber() {
    return teamNumber;
  }

  private final String teamName;

  public String getTeamName() {
    return teamName;
  }

  private final String division;

  public String getDivision() {
    return division;
  }

  private final String eventDivision;

  public String getEventDivision() {
    return eventDivision;
  }

}
