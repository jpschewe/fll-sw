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
import java.util.LinkedList;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Methods for loading and storing {@link AwardWinner} and
 * {@link OverallAwardWinner}.
 */
public final class AwardWinners {

  /**
   * Display name for the pre-defined Championship award.
   * This must match the value of CHAMPIONSHIP_NAME in finalist.js.
   */
  public static final String CHAMPIONSHIP_AWARD_NAME = "Championship";

  private AwardWinners() {
  }

  /**
   * @param connection where to store the data
   * @param tournamentId the tournament
   * @param winner the winner to add
   * @throws SQLException on a database error
   */
  public static void addSubjectiveAwardWinner(final Connection connection,
                                              final int tournamentId,
                                              final AwardWinner winner)
      throws SQLException {
    addAwardWinner(connection, tournamentId, winner, "subjective_challenge_award");
  }

  /**
   * @param connection where to store the data
   * @param tournamentId the tournament
   * @param winner the winner to add
   * @throws SQLException on a database error
   */
  public static void updateSubjectiveAwardWinner(final Connection connection,
                                                 final int tournamentId,
                                                 final AwardWinner winner)
      throws SQLException {
    updateAwardWinner(connection, tournamentId, winner, "subjective_challenge_award");
  }

  /**
   * @param connection database connection
   * @param tournamentId tournament to work with
   * @param name {@link AwardWinner#getName()}
   * @param teamNumber {@link AwardWinner#getTeamNumber()}
   * @throws SQLException on a database error
   */
  public static void deleteSubjectiveAwardWinner(final Connection connection,
                                                 final int tournamentId,
                                                 final String name,
                                                 final int teamNumber)
      throws SQLException {
    deleteAwardWinner(connection, tournamentId, name, teamNumber, "subjective_challenge_award");
  }

  /**
   * @param connection database connection
   * @param tournamentId tournament to work with
   * @param name {@link AwardWinner#getName()}
   * @param teamNumber {@link AwardWinner#getTeamNumber()}
   * @return the winner object or null if not found
   * @throws SQLException on a database error
   */
  public static @Nullable AwardWinner getSubjectiveAwardWinner(final Connection connection,
                                                               final int tournamentId,
                                                               final String name,
                                                               final int teamNumber)
      throws SQLException {
    return getAwardWinner(connection, tournamentId, name, teamNumber, "subjective_challenge_award");
  }

  /**
   * @param connection database connection
   * @param tournamentId the tournament to get winners for
   * @return the winners sorted by category, award group, place, team number
   * @throws SQLException if an error occurs talking to the database
   */
  public static List<AwardWinner> getSubjectiveAwardWinners(final Connection connection,
                                                            final int tournamentId)
      throws SQLException {
    return getAwardWinners(connection, tournamentId, "subjective_challenge_award");
  }

  /**
   * @param connection where to store the data
   * @param tournamentId the tournament
   * @param winner the winner to add
   * @throws SQLException on a database error
   */
  public static void addNonNumericAwardWinner(final Connection connection,
                                              final int tournamentId,
                                              final AwardWinner winner)
      throws SQLException {
    addAwardWinner(connection, tournamentId, winner, "subjective_extra_award");
  }

  /**
   * @param connection where to store the data
   * @param tournamentId the tournament
   * @param winner the winner to add
   * @throws SQLException on a database error
   */
  public static void updateNonNumericAwardWinner(final Connection connection,
                                                 final int tournamentId,
                                                 final AwardWinner winner)
      throws SQLException {
    updateAwardWinner(connection, tournamentId, winner, "subjective_extra_award");
  }

  /**
   * @param connection database connection
   * @param tournamentId tournament to work with
   * @param name {@link AwardWinner#getName()}
   * @param teamNumber {@link AwardWinner#getTeamNumber()}
   * @throws SQLException on a database error
   */
  public static void deleteNonNumericAwardWinner(final Connection connection,
                                                 final int tournamentId,
                                                 final String name,
                                                 final int teamNumber)
      throws SQLException {
    deleteAwardWinner(connection, tournamentId, name, teamNumber, "subjective_extra_award");
  }

  /**
   * @param connection database connection
   * @param tournamentId tournament to work with
   * @param name {@link AwardWinner#getName()}
   * @param teamNumber {@link AwardWinner#getTeamNumber()}
   * @return the winner object or null if not found
   * @throws SQLException on a database error
   */
  public static @Nullable AwardWinner getNonNumericAwardWinner(final Connection connection,
                                                               final int tournamentId,
                                                               final String name,
                                                               final int teamNumber)
      throws SQLException {
    return getAwardWinner(connection, tournamentId, name, teamNumber, "subjective_extra_award");
  }

  /**
   * @param connection database connection
   * @param tournamentId the tournament to get winners for
   * @return the winners sorted by category, award group, place, team number
   * @throws SQLException if an error occurs talking to the database
   */
  public static List<AwardWinner> getNonNumericAwardWinners(final Connection connection,
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
  private static @Nullable AwardWinner getAwardWinner(final Connection connection,
                                                      final int tournamentId,
                                                      final String name,
                                                      final int teamNumber,
                                                      final String tablename)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT description, award_group, place FROM "
        + tablename
        + " WHERE tournament_id = ?" //
        + " AND team_number = ?" //
    )) {
      prep.setInt(1, tournamentId);
      prep.setInt(2, teamNumber);

      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final String description = rs.getString(1);
          final String awardGroup = castNonNull(rs.getString(2));
          final int place = rs.getInt(3);

          final AwardWinner winner = new AwardWinner(name, awardGroup, teamNumber, description, place);
          return winner;
        } else {
          return null;
        }
      }
    }
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Table name is passed to method")
  private static void addAwardWinner(final Connection connection,
                                     final int tournamentId,
                                     final AwardWinner winner,
                                     final String tablename)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("INSERT INTO "
        + tablename //
        + " ( tournament_id, name, team_number, description, award_group, place)" //
        + " VALUES(?, ?, ?, ?, ?, ?)" //
    )) {
      prep.setInt(1, tournamentId);
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

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Table name is passed to method")
  private static void updateAwardWinner(final Connection connection,
                                        final int tournamentId,
                                        final OverallAwardWinner winner,
                                        final String tablename)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("UPDATE "
        + tablename //
        + " SET description = ?, place = ?" //
        + " WHERE tournament_id = ?" //
        + " AND name = ?" //
        + " AND team_number = ?" //
    )) {
      prep.setInt(3, tournamentId);
      prep.setString(4, winner.getName());
      prep.setInt(5, winner.getTeamNumber());

      final String winnerDescription = winner.getDescription();
      if (null != winnerDescription
          && !winnerDescription.trim().isEmpty()) {
        prep.setString(1, winnerDescription.trim());
      } else {
        prep.setNull(1, Types.LONGVARCHAR);
      }
      prep.setInt(2, winner.getPlace());
      prep.executeUpdate();
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
   */
  public static List<OverallAwardWinner> getNonNumericOverallAwardWinners(final Connection connection,
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
   * @param connection where to store the data
   * @param tournamentId the tournament
   * @param winner the winner to add
   * @throws SQLException on a database error
   */
  public static void addNonNumerciOverallAwardWinner(final Connection connection,
                                                     final int tournamentId,
                                                     final OverallAwardWinner winner)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("INSERT INTO subjective_overall_award"
        + " ( tournament_id, name, team_number, description, place)" //
        + " VALUES(?, ?, ?, ?, ?)" //
    )) {
      prep.setInt(1, tournamentId);

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

  /**
   * @param connection database connection
   * @param tournamentId tournament to work with
   * @param winner object to update in the database
   * @throws SQLException on a database error
   */
  public static void updateNonNumericOverallAwardWinner(final Connection connection,
                                                        final int tournamentId,
                                                        final OverallAwardWinner winner)
      throws SQLException {
    updateAwardWinner(connection, tournamentId, winner, "subjective_overall_award");
  }

  /**
   * @param connection database connection
   * @param tournamentId tournament to work with
   * @param name {@link OverallAwardWinner#getName()}
   * @param teamNumber {@link OverallAwardWinner#getTeamNumber()}
   * @throws SQLException on a database error
   */
  public static void deleteNonNumericOverallAwardWinner(final Connection connection,
                                                        final int tournamentId,
                                                        final String name,
                                                        final int teamNumber)
      throws SQLException {
    deleteAwardWinner(connection, tournamentId, name, teamNumber, "subjective_overall_award");
  }

  /**
   * @param connection database connection
   * @param tournamentId tournament to work with
   * @param name {@link AwardWinner#getName()}
   * @param teamNumber {@link AwardWinner#getTeamNumber()}
   * @return the winner object or null if not found
   * @throws SQLException on a database error
   */
  public static @Nullable OverallAwardWinner getNonNumericOverallAwardWinner(final Connection connection,
                                                                             final int tournamentId,
                                                                             final String name,
                                                                             final int teamNumber)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT description, place FROM subjective_overall_award"
        + " WHERE tournament_id = ?" //
        + " AND team_number = ?" //
    )) {
      prep.setInt(1, tournamentId);
      prep.setInt(2, teamNumber);

      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final String description = rs.getString(1);
          final int place = rs.getInt(2);

          final OverallAwardWinner winner = new OverallAwardWinner(name, teamNumber, description, place);
          return winner;
        } else {
          return null;
        }
      }
    }
  }

}
