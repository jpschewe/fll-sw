/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A team with tournament information.
 */
@SuppressFBWarnings(value = { "EQ_DOESNT_OVERRIDE_EQUALS" }, justification = "Equality doesn't change in this subclass of Team")
public class TournamentTeam extends Team {

  public TournamentTeam(@JsonProperty("teamNumber") final int teamNumber,
                        @JsonProperty("organization") final String org,
                        @JsonProperty("teamName") final String name,
                        @JsonProperty("awardGroup") final String awardGroup,
                        @JsonProperty("judgingGroup") final String judgingGroup) {
    super(teamNumber, org, name);
    _awardGroup = awardGroup;
    _judgingGroup = judgingGroup;
  }

  private final String _awardGroup;

  /**
   * @return The award group that the team is in.
   */
  public String getAwardGroup() {
    return _awardGroup;
  }

  private final String _judgingGroup;

  /**
   * @return The judging group for the team.
   */
  public String getJudgingGroup() {
    return _judgingGroup;
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
      final String eventDivision = t.getAwardGroup();
      if (!eventDivision.equals(divisionStr)) {
        iter.remove();
      }
    }
  }

}
