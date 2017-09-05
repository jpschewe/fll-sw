/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.Serializable;

/**
 * Helper class to gather up information about event division changes.
 */
public final class EventDivisionInfo implements Serializable {

  public EventDivisionInfo(final int teamNumber,
                           final String teamName,
                           final String eventDivision) {
    this.teamNumber = teamNumber;
    this.teamName = teamName;
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

  private final String eventDivision;

  public String getEventDivision() {
    return eventDivision;
  }

}
