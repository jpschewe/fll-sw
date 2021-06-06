/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A team with tournament information.
 */
@SuppressFBWarnings(value = { "EQ_DOESNT_OVERRIDE_EQUALS" }, justification = "Equality doesn't change in this subclass of Team")
public class TournamentTeam extends Team {

  /**
   * @param teamNumber {@link #getTeamNumber()}
   * @param org {@link #getOrganization()}
   * @param name {@link #getTeamName()}
   * @param awardGroup {@link #getAwardGroup()}
   * @param judgingGroup {@link #getJudgingGroup()}
   */
  public TournamentTeam(@JsonProperty("teamNumber") final int teamNumber,
                        @JsonProperty("organization") final @Nullable String org,
                        @JsonProperty("teamName") final String name,
                        @JsonProperty("awardGroup") final String awardGroup,
                        @JsonProperty("judgingGroup") final String judgingGroup) {
    super(teamNumber, org, name);
    this.awardGroup = awardGroup;
    this.judgingGroup = judgingGroup;
  }

  private final String awardGroup;

  /**
   * @return The award group that the team is in.
   */
  public String getAwardGroup() {
    return awardGroup;
  }

  private final String judgingGroup;

  /**
   * @return The judging group for the team.
   */
  public String getJudgingGroup() {
    return judgingGroup;
  }

  /**
   * Filter the specified list to just the teams in the specified event
   * division.
   * 
   * @param teams list that is modified
   * @param divisionStr the division to keep
   */
  public static void filterTeamsToEventDivision(final List<TournamentTeam> teams,
                                                final String divisionStr) {
    final Iterator<TournamentTeam> iter = teams.iterator();
    while (iter.hasNext()) {
      final TournamentTeam t = iter.next();
      final String eventDivision = t.getAwardGroup();
      if (!eventDivision.equals(divisionStr)) {
        iter.remove();
      }
    }
  }

  /**
   * Builds a team object from its database info given the team number.
   * 
   * @param connection Database connection.
   * @param teamNumber Number of the team for which to build an object.
   * @param tournament the tournament to find the team at
   * @return The new Team object or null if the team was not found in the
   *         database.
   * @throws SQLException on a database access error.
   * @throws IllegalArgumentException if the team number is for {@link Team#NULL},
   *           {@link Team#TIE} or {@link Team#BYE}
   * @throws IllegalArgumentException if the team cannot be found
   */
  public static TournamentTeam getTournamentTeamFromDatabase(final Connection connection,
                                                             final Tournament tournament,
                                                             final int teamNumber)
      throws SQLException {

    // First, handle known non-database team numbers...
    if (teamNumber == NULL_TEAM_NUMBER) {
      throw new IllegalArgumentException("Cannot get tournament team for null number");
    }
    if (teamNumber == TIE_TEAM_NUMBER) {
      throw new IllegalArgumentException("Cannot get tournament team for tie number");
    }
    if (teamNumber == BYE_TEAM_NUMBER) {
      throw new IllegalArgumentException("Cannot get tournament team for bye number");
    }

    try (PreparedStatement stmt = connection.prepareStatement("SELECT Teams.Organization"//
        + ", Teams.TeamName"//
        + ", TournamentTeams.event_division" //
        + ", TournamentTeams.judging_station" //
        + " FROM Teams, TournamentTeams" //
        + " WHERE Teams.TeamNumber = TournamentTeams.TeamNumber"//
        + " AND TournamentTeams.Tournament = ?" //
        + " AND Teams.TeamNumber = ?")) {
      stmt.setInt(1, tournament.getTournamentID());
      stmt.setInt(2, teamNumber);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          final String org = rs.getString(1);
          final String name = castNonNull(rs.getString(2));
          final String awardGroup = castNonNull(rs.getString(3));
          final String judgingGroup = castNonNull(rs.getString(4));

          final TournamentTeam x = new TournamentTeam(teamNumber, org, name, awardGroup, judgingGroup);
          return x;
        } else {
          throw new IllegalArgumentException("Team "
              + teamNumber
              + " is not in the database");
        }
      }
    }
  }

}
