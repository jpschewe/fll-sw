/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

import fll.util.FLLInternalException;

/**
 * A team advancing to another tournament.
 */
public class AdvancingTeam implements Serializable {

  /**
   * Used for JSON deserialization.
   */
  public static final class AdvancingTeamCollectionTypeInformation extends TypeReference<Collection<AdvancingTeam>> {
    /** single instance. */
    public static final AdvancingTeamCollectionTypeInformation INSTANCE = new AdvancingTeamCollectionTypeInformation();
  }

  /**
   * @param teamNumber see {@link #getTeamNumber()}
   * @param group see {@link #getGroup()}
   */
  public AdvancingTeam(@JsonProperty("teamNumber") final int teamNumber,
                       @JsonProperty("group") final String group) {
    this.teamNumber = teamNumber;
    this.group = group;
  }

  private final int teamNumber;

  /**
   * @return number of the team
   */
  public int getTeamNumber() {
    return teamNumber;
  }

  private final String group;

  /**
   * Note that this may not be the traditional award groups, users can create
   * arbitrary groups such as "wildcard".
   * 
   * @return the group this team advanced through
   */
  public String getGroup() {
    return group;
  }

  /**
   * @param connection database connection
   * @param tournamentId the tournament to load teams for
   * @return the advancing teams sorted by group and then team number
   * @throws SQLException on a database error
   */
  public static List<AdvancingTeam> loadAdvancingTeams(final Connection connection,
                                                       final int tournamentId)
      throws SQLException {
    final List<AdvancingTeam> result = new LinkedList<>();
    try (PreparedStatement prep = connection.prepareStatement("SELECT team_number, award_group" //
        + " FROM advancing_teams" //
        + " WHERE tournament_id = ? ORDER BY award_group, team_number")) {
      prep.setInt(1, tournamentId);
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final int teamNumber = rs.getInt(1);
          final String group = rs.getString(2);
          if (null == group) {
            throw new FLLInternalException("Database consistency error, award_group is null in advancing_teams");
          }
          final AdvancingTeam advancing = new AdvancingTeam(teamNumber, group);
          result.add(advancing);
        }
      }
    }
    return result;
  }

  /**
   * @param connection database connection
   * @param tournamentId tournament to store advancing teams for
   * @param teams the teams to store
   * @throws SQLException on a database error
   */
  public static void storeAdvancingTeams(final Connection connection,
                                         final int tournamentId,
                                         final Collection<AdvancingTeam> teams)
      throws SQLException {
    try (
        PreparedStatement delete = connection.prepareStatement("DELETE FROM advancing_teams WHERE tournament_id = ?")) {
      delete.setInt(1, tournamentId);
      delete.executeUpdate();
    }

    try (PreparedStatement prep = connection.prepareStatement("INSERT INTO advancing_teams" //
        + " (tournament_id, team_number, award_group)"
        + " VALUES(?, ?, ?)")) {
      prep.setInt(1, tournamentId);
      for (final AdvancingTeam advancing : teams) {
        prep.setInt(2, advancing.getTeamNumber());
        prep.setString(3, advancing.getGroup());
        prep.executeUpdate();
      }
    }
  }
}
