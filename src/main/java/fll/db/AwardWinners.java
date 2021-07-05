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
import java.util.Collections;
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
   * @param connection where to store the data
   * @param tournamentId the tournament
   * @param winner the winner to add
   * @throws SQLException on a database error
   * @see #storeChallengeAwardWinners(Connection, int, Collection)
   */
  public static void addChallengeAwardWinner(final Connection connection,
                                             final int tournamentId,
                                             final AwardWinner winner)
      throws SQLException {
    addAwardWinners(connection, tournamentId, Collections.singleton(winner), "subjective_challenge_award");
  }

  /**
   * @param connection database connection
   * @param tournamentId tournament to work with
   * @param name {@link AwardWinner#getName()}
   * @param teamNumber {@link AwardWinner#getTeamNumber()}
   * @throws SQLException on a database error
   */
  public static void deleteChallengeAwardWinner(final Connection connection,
                                                final int tournamentId,
                                                final String name,
                                                final int teamNumber)
      throws SQLException {
    deleteAwardWinner(connection, tournamentId, name, teamNumber, "subjective_challenge_award");
  }

  /**
   * @param connection database connection
   * @param tournamentId the tournament to get winners for
   * @return the winners sorted by category, award group, place, team number
   * @throws SQLException if an error occurs talking to the database
   * @see #storeChallengeAwardWinners(Connection, int, Collection)
   */
  public static List<AwardWinner> getChallengeAwardWinners(final Connection connection,
                                                           final int tournamentId)
      throws SQLException {
    return getAwardWinners(connection, tournamentId, "subjective_challenge_award");
  }

  /**
   * Store the winners of non-numeric awards and are handed out per award group.
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
   * @param connection where to store the data
   * @param tournamentId the tournament
   * @param winner the winner to add
   * @throws SQLException on a database error
   * @see #storeExtraAwardWinners(Connection, int, Collection)
   */
  public static void addExtraAwardWinner(final Connection connection,
                                         final int tournamentId,
                                         final AwardWinner winner)
      throws SQLException {
    addAwardWinners(connection, tournamentId, Collections.singleton(winner), "subjective_extra_award");
  }

  /**
   * @param connection database connection
   * @param tournamentId tournament to work with
   * @param name {@link AwardWinner#getName()}
   * @param teamNumber {@link AwardWinner#getTeamNumber()}
   * @throws SQLException on a database error
   */
  public static void deleteExtraAwardWinner(final Connection connection,
                                            final int tournamentId,
                                            final String name,
                                            final int teamNumber)
      throws SQLException {
    deleteAwardWinner(connection, tournamentId, name, teamNumber, "subjective_extra_award");
  }

  /**
   * @param connection database connection
   * @param tournamentId the tournament to get winners for
   * @return the winners sorted by category, award group, place, team number
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
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT name, team_number, description, award_group, place FROM "
            + tablename
            + " WHERE tournament_id = ? ORDER BY name, award_group, place, team_number")) {
      prep.setInt(1, tournamentId);

      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String name = castNonNull(rs.getString(1));
          final int teamNumber = rs.getInt(2);
          final String description = rs.getString(3);
          final String awardGroup = castNonNull(rs.getString(4));
          final int place = rs.getInt(5);

          final AwardWinner winner = new AwardWinner(name, awardGroup, teamNumber, description, place);
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

    addAwardWinners(connection, tournamentId, winners, tablename);
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Table name is passed to method")
  private static void addAwardWinners(final Connection connection,
                                      final int tournamentId,
                                      final Collection<AwardWinner> winners,
                                      final String tablename)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("INSERT INTO "
        + tablename //
        + " ( tournament_id, name, team_number, description, award_group, place)" //
        + " VALUES(?, ?, ?, ?, ?, ?)" //
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
        prep.setInt(6, winner.getPlace());
        prep.executeUpdate();
      }
    }
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Table name is passed to method")
  private static void deleteAwardWinner(final Connection connection,
                                        final int tournamentId,
                                        final String name,
                                        final int teamNumber,
                                        final String tablename)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("DELETE FROM "
        + tablename //
        + " WHERE tournament_id = ?" //
        + " AND name = ?" //
        + " AND team_number = ? ")) {
      prep.setInt(1, tournamentId);
      prep.setString(2, name);
      prep.setInt(3, teamNumber);
      prep.executeUpdate();
    }
  }

  /**
   * @param connection database connection
   * @param tournamentId the tournament to get winners for
   * @return the winners sorted by category, place, team number
   * @throws SQLException if an error occurs talking to the database
   * @see #storeOverallAwardWinners(Connection, int, Collection)
   */
  public static List<OverallAwardWinner> getOverallAwardWinners(final Connection connection,
                                                                final int tournamentId)
      throws SQLException {
    final List<OverallAwardWinner> result = new LinkedList<>();
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT name, team_number, description, place FROM subjective_overall_award"
            + " WHERE tournament_id = ? ORDER BY name, place, team_number")) {
      prep.setInt(1, tournamentId);

      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String name = castNonNull(rs.getString(1));
          final int teamNumber = rs.getInt(2);
          final String description = rs.getString(3);
          final int place = rs.getInt(4);

          final OverallAwardWinner winner = new OverallAwardWinner(name, teamNumber, description, place);
          result.add(winner);
        }
      }
    }
    return result;
  }

  /**
   * Store the winners of additional non-numeric awards that are handed out per
   * tournament.
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

    addOverallAwardWinners(connection, tournamentId, winners);
  }

  /**
   * @param connection where to store the data
   * @param tournamentId the tournament
   * @param winner the winner to add
   * @throws SQLException on a database error
   * @see #storeOverallAwardWinners(Connection, int, Collection)
   */
  public static void addOverallAwardWinner(final Connection connection,
                                           final int tournamentId,
                                           final OverallAwardWinner winner)
      throws SQLException {
    addOverallAwardWinners(connection, tournamentId, Collections.singleton(winner));
  }

  private static void addOverallAwardWinners(final Connection connection,
                                             final int tournamentId,
                                             final Collection<OverallAwardWinner> winners)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("INSERT INTO subjective_overall_award"
        + " ( tournament_id, name, team_number, description, place)" //
        + " VALUES(?, ?, ?, ?, ?)" //
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
        prep.setInt(5, winner.getPlace());

        prep.executeUpdate();
      }
    }
  }

  /**
   * @param connection database connection
   * @param tournamentId tournament to work with
   * @param name {@link OverallAwardWinner#getName()}
   * @param teamNumber {@link OverallAwardWinner#getTeamNumber()}
   * @throws SQLException on a database error
   */
  public static void deleteOverallAwardWinner(final Connection connection,
                                              final int tournamentId,
                                              final String name,
                                              final int teamNumber)
      throws SQLException {
    deleteAwardWinner(connection, tournamentId, name, teamNumber, "subjective_overall_award");
  }

}
