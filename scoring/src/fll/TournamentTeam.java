/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * A team with tournament information.
 */
@edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "EQ_DOESNT_OVERRIDE_EQUALS" }, justification = "Equality doesn't change in this subclass of Team")
public class TournamentTeam extends Team {

  public TournamentTeam(final int teamNumber, final String org, final String name, final String division, final String eventDivision, final String judgingStation) {
    super(teamNumber, org, name, division);
    _eventDivision = eventDivision;
    _judgingStation = judgingStation;
  }

  private final String _eventDivision;

  /**
   * The event division that a team is entered in.
   * 
   * @return division
   */
  public String getEventDivision() {
    return _eventDivision;
  }

  private final String _judgingStation;

  /**
   * The judging station for the team.
   */
  public String getJudgingStation() {
    return _judgingStation;
  }

  /**
   * Filter the specified list to just the teams in the specified event
   * division.
   * 
   * @param teams list that is modified
   * @param divisionStr the division to keep
   * @throws RuntimeException
   * @throws SQLException
   */
  public static void filterTeamsToEventDivision(final List<TournamentTeam> teams,
                                                final String divisionStr) throws SQLException, RuntimeException {
    final Iterator<TournamentTeam> iter = teams.iterator();
    while (iter.hasNext()) {
      final TournamentTeam t = iter.next();
      final String eventDivision = t.getEventDivision();
      if (!eventDivision.equals(divisionStr)) {
        iter.remove();
      }
    }
  }

}
