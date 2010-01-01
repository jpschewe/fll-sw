/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.developer.importdb;

import fll.db.TeamPropertyDifference;

/**
 * Represents a difference in the tournament that a team is in.
 * <p>
 * Implementation note. This class could be combined with
 * {@link TeamPropertyDifference}, however I wanted to ensure that the pages
 * that handle differences in team information don't have to handle tournament
 * differences. I see tournament differences as a differnt class of difference
 * that needs special handling.
 * </p>
 */
public class TournamentDifference {

  public TournamentDifference(final int teamNumber, final String sourceTournament, final String destTournament) {
    this.teamNumber = teamNumber;
    this.sourceTournament = sourceTournament;
    this.destTournament = destTournament;
  }

  private final int teamNumber;

  public int getTeamNumber() {
    return teamNumber;
  }

  private final String sourceTournament;

  public String getSourceTournament() {
    return sourceTournament;
  }

  private final String destTournament;

  public String getDestTournament() {
    return destTournament;
  }

}
