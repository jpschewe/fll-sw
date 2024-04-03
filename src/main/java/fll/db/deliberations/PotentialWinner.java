/*
 * Copyright (c) 2024 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db.deliberations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;

import com.fasterxml.jackson.annotation.JsonProperty;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Tournament;

/**
 * A potential winner and their place.
 */
public final class PotentialWinner extends CommonBase {
  /**
   * @param awardGroup {@link CommonBase#getAwardGroup()}
   * @param categoryName {@link CommonBase#getCategoryName()}
   * @param place {@link #getPlace()}
   * @param teamNumber {@link #getTeamNumber()}
   */
  public PotentialWinner(@JsonProperty("awardGroup") final String awardGroup,
                         @JsonProperty("categoryName") final String categoryName,
                         @JsonProperty("place") final int place,
                         @JsonProperty("teamNumber") final int teamNumber) {
    super(awardGroup, categoryName);
    this.place = place;
    this.teamNumber = teamNumber;
  }

  private final int place;

  /**
   * @return place in the potential winners
   */
  public int getPlace() {
    return place;
  }

  private final int teamNumber;

  /**
   * @return the team in the potential winners
   */
  public int getTeamNumber() {
    return teamNumber;
  }

  /**
   * @param connection the database connection
   * @param tournament the tournament
   * @return the deliberation potential winners for the tournament
   * @throws SQLException on a database error
   */
  public static Collection<PotentialWinner> getPotentialWinners(final Connection connection,
                                                                final Tournament tournament)
      throws SQLException {
    final Collection<PotentialWinner> result = new LinkedList<>();
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT award_group, category_name, place, team_number FROM deliberation_potential_winners WHERE tournament_id = ?")) {
      prep.setInt(1, tournament.getTournamentID());
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String awardGroup = castNonNull(rs.getString(1));
          final String categoryName = castNonNull(rs.getString(2));
          final int place = rs.getInt(3);
          final int teamNumber = rs.getInt(4);

          final PotentialWinner winner = new PotentialWinner(awardGroup, categoryName, place, teamNumber);
          result.add(winner);
        }
      }
    }
    return result;
  }

  /**
   * @param connection database connection
   * @param tournament tournament to update
   * @param potentialWinners new data
   * @throws SQLException on a database error
   */
  public static void setPotentialWinners(final Connection connection,
                                         final Tournament tournament,
                                         final Collection<PotentialWinner> potentialWinners)
      throws SQLException {
    try (
        PreparedStatement delete = connection.prepareStatement("DELETE FROM deliberation_potential_winners WHERE tournament_id = ?");
        PreparedStatement insert = connection.prepareStatement("INSERT INTO deliberation_potential_winners (tournament_id, award_group, category_name, place, team_number) VALUES(?, ?, ?, ?, ?)")) {
      delete.setInt(1, tournament.getTournamentID());
      delete.executeUpdate();

      insert.setInt(1, tournament.getTournamentID());
      for (final PotentialWinner winner : potentialWinners) {
        insert.setString(2, winner.getAwardGroup());
        insert.setString(3, winner.getCategoryName());
        insert.setInt(4, winner.getPlace());
        insert.setInt(5, winner.getTeamNumber());
        insert.executeUpdate();
      }
    }
  }

}