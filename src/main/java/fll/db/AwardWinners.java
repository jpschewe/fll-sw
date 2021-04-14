/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Methods for loading and storing {@link AwardWinner} and
 * {@link OverallAwardWinner}.
 */
public final class AwardWinners {
  private AwardWinners() {
  }

  /**
   * Store the winners of awards for the subjective categories.
   * 
   * @param connection where to store
   * @param tournamentId the tournament
   * @param winners the winners
   * @throws SQLException if an error occurs talking to the database
   */
  public static void storeChallengeAwardWinners(final Connection connection,
                                                final int tournamentId,
                                                final Collection<AwardWinner> winners)
      throws SQLException {
    storeAwardWinners(connection, tournamentId, winners, "subjective_challenge_award");
  }

  /**
   * @param connection database connection
   * @param tournamentId the tournament to get winners for
   * @return the winners sorted by category, award group, team number
   * @throws SQLException if an error occurs talking to the database
   * @see #storeChallengeAwardWinners(Connection, int, Collection)
   */
  public static List<AwardWinner> getChallengeAwardWinners(final Connection connection,
                                                           final int tournamentId)
      throws SQLException {
    return getAwardWinners(connection, tournamentId, "subjective_challenge_award");
  }

  /**
   * Store the winners of additional awards that are not in the challenge
   * description and are handed out per award group.
   * 
   * @param connection where to store
   * @param tournamentId the tournament
   * @param winners the winners
   * @throws SQLException if an error occurs talking to the database
   */
  public static void storeExtraAwardWinners(final Connection connection,
                                            final int tournamentId,
                                            final Collection<AwardWinner> winners)
      throws SQLException {
    storeAwardWinners(connection, tournamentId, winners, "subjective_extra_award");
  }

  /**
   * @param connection database connection
   * @param tournamentId the tournament to get winners for
   * @return the winners sorted by category, award group, team number
   * @throws SQLException if an error occurs talking to the database
   * @see #storeExtraAwardWinners(Connection, int, Collection)
   */
  public static List<AwardWinner> getExtraAwardWinners(final Connection connection,
                                                       final int tournamentId)
      throws SQLException {
    return getAwardWinners(connection, tournamentId, "subjective_extra_award");
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Table name is passed to method")
  private static List<AwardWinner> getAwardWinners(final Connection connection,
                                                   final int tournamentId,
                                                   final String tablename)
      throws SQLException {
    final List<AwardWinner> result = new LinkedList<>();
    try (PreparedStatement prep = connection.prepareStatement("SELECT name, team_number, description, award_group FROM "
        + tablename
        + " WHERE tournament_id = ? ORDER BY name, award_group, team_number")) {
      prep.setInt(1, tournamentId);

      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String name = castNonNull(rs.getString(1));
          final int teamNumber = rs.getInt(2);
          final String description = rs.getString(3);
          final String awardGroup = castNonNull(rs.getString(4));

          final AwardWinner winner = new AwardWinner(name, awardGroup, teamNumber, description);
          result.add(winner);
        }
      }
    }
    return result;
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Table name is passed to method")
  private static void storeAwardWinners(final Connection connection,
                                        final int tournamentId,
                                        final Collection<AwardWinner> winners,
                                        final String tablename)
      throws SQLException {
    try (PreparedStatement delete = connection.prepareStatement("DELETE FROM "
        + tablename
        + " WHERE tournament_id = ?")) {
      delete.setInt(1, tournamentId);
      delete.executeUpdate();
    }

    try (PreparedStatement prep = connection.prepareStatement("INSERT INTO "
        + tablename //
        + " ( tournament_id, name, team_number, description, award_group)" //
        + " VALUES(?, ?, ?, ?, ?)" //
    )) {
      prep.setInt(1, tournamentId);

      for (final AwardWinner winner : winners) {
        prep.setString(2, winner.getName());
        prep.setInt(3, winner.getTeamNumber());

        final String winnerDescription = winner.getDescription();
        if (null != winnerDescription
            && !winnerDescription.trim().isEmpty()) {
          prep.setString(4, winnerDescription.trim());
        } else {
          prep.setNull(4, Types.LONGVARCHAR);
        }
        prep.setString(5, winner.getAwardGroup());
        prep.executeUpdate();
      }
    }
  }

  /**
   * @param connection database connection
   * @param tournamentId the tournament to get winners for
   * @return the winners sorted by category, team number
   * @throws SQLException if an error occurs talking to the database
   * @see #storeOverallAwardWinners(Connection, int, Collection)
   */
  public static List<OverallAwardWinner> getOverallAwardWinners(final Connection connection,
                                                                final int tournamentId)
      throws SQLException {
    final List<OverallAwardWinner> result = new LinkedList<>();
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT name, team_number, description FROM subjective_overall_award"
            + " WHERE tournament_id = ? ORDER BY name, team_number")) {
      prep.setInt(1, tournamentId);

      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String name = castNonNull(rs.getString(1));
          final int teamNumber = rs.getInt(2);
          final String description = rs.getString(3);

          final OverallAwardWinner winner = new OverallAwardWinner(name, teamNumber, description);
          result.add(winner);
        }
      }
    }
    return result;
  }

  /**
   * Store the winners of additional awards that are not in the challenge
   * description and are handed out per tournament.
   * 
   * @param connection where to store
   * @param tournamentId the tournament
   * @param winners the winners
   * @throws SQLException if an error occurs talking to the database
   */
  public static void storeOverallAwardWinners(final Connection connection,
                                              final int tournamentId,
                                              final Collection<OverallAwardWinner> winners)
      throws SQLException {
    try (PreparedStatement delete = connection.prepareStatement("DELETE FROM subjective_overall_award"
        + " WHERE tournament_id = ?")) {
      delete.setInt(1, tournamentId);
      delete.executeUpdate();
    }

    try (PreparedStatement prep = connection.prepareStatement("INSERT INTO subjective_overall_award"
        + " ( tournament_id, name, team_number, description)" //
        + " VALUES(?, ?, ?, ?)" //
    )) {
      prep.setInt(1, tournamentId);

      for (final OverallAwardWinner winner : winners) {
        prep.setString(2, winner.getName());
        prep.setInt(3, winner.getTeamNumber());
        final String winnerDescription = winner.getDescription();
        if (null != winnerDescription
            && !winnerDescription.trim().isEmpty()) {
          prep.setString(4, winnerDescription.trim());
        } else {
          prep.setNull(4, Types.LONGVARCHAR);
        }
        prep.executeUpdate();
      }
    }
  }

}
