/*
 * This code is released under GPL; see LICENSE for details.
 */

package fll.scores;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Team;
import fll.Tournament;

/**
 * Performance score from the database.
 */
public final class DatabasePerformanceTeamScore {

  private DatabasePerformanceTeamScore() {
  }

  /**
   * Find all unverified performance scores for a tournament.
   * 
   * @param tournament the tournament
   * @param connection database
   * @return unverified performance scores
   * @throws SQLException on a database error
   */
  public static List<PerformanceTeamScore> fetchUnverifiedScores(final Tournament tournament,
                                                                 final Connection connection)
      throws SQLException {
    final List<PerformanceTeamScore> scores = new LinkedList<>();

    try (
        PreparedStatement prep = connection.prepareStatement("SELECT NoShow, Bye, Verified, Tablename, TimeStamp, RunNumber, TeamNumber" //
            + " FROM performance WHERE Tournament = ? AND Verified <> TRUE ORDER BY RunNumber ASC")) {

      prep.setInt(1, tournament.getTournamentID());
      try (ResultSet result = prep.executeQuery()) {
        while (result.next()) {
          final int runNumber = result.getInt(6);
          final int teamNumber = result.getInt(7);

          final PerformanceTeamScore score = fetchScore(tournament.getTournamentID(), teamNumber, runNumber, connection,
                                                        result);
          scores.add(score);
        }
      }
    }
    return scores;
  }

  /**
   * Fetch all performance scores for a team.
   * 
   * @param tournament the tournament
   * @param team the Team
   * @param connection database
   * @return the scores sorted by {@link PerformanceTeamScore#getRunNumber()},
   *         this may be an empty list
   * @throws SQLException on a database error
   */
  public static List<PerformanceTeamScore> fetchTeamScores(final Tournament tournament,
                                                           final Team team,
                                                           final Connection connection)
      throws SQLException {
    final List<PerformanceTeamScore> scores = new LinkedList<>();

    try (
        PreparedStatement prep = connection.prepareStatement("SELECT NoShow, Bye, Verified, Tablename, TimeStamp, RunNumber" //
            + " FROM performance WHERE TeamNumber = ? AND Tournament = ? ORDER BY RunNumber ASC")) {

      prep.setInt(1, team.getTeamNumber());
      prep.setInt(2, tournament.getTournamentID());
      try (ResultSet result = prep.executeQuery()) {
        while (result.next()) {
          final int runNumber = result.getInt(6);

          final PerformanceTeamScore score = fetchScore(tournament.getTournamentID(), team.getTeamNumber(), runNumber,
                                                        connection, result);
          scores.add(score);
        }
      }
    }
    return scores;
  }

  /**
   * Create a database team score object for a performance score.
   * 
   * @param tournament the tournament ID
   * @param teamNumber see {@link TeamScore#getTeamNumber()}
   * @param runNumber see {@link PerformanceTeamScore#getRunNumber()}
   * @param connection the connection to get the data from
   * @return the {@link PerformanceTeamScore} or {@code null} if no score exists
   *         matching the
   *         supplied criteria
   * @throws SQLException if there is an error getting the data
   */
  public static @Nullable PerformanceTeamScore fetchTeamScore(final int tournament,
                                                              final int teamNumber,
                                                              final int runNumber,
                                                              final Connection connection)
      throws SQLException {

    try (PreparedStatement prep = connection.prepareStatement("SELECT NoShow, Bye, Verified, Tablename, TimeStamp" //
        + " FROM performance WHERE TeamNumber = ? AND Tournament = ?"
        + " AND RunNumber = ?")) {

      prep.setInt(1, teamNumber);
      prep.setInt(2, tournament);
      prep.setInt(3, runNumber);
      try (ResultSet result = prep.executeQuery()) {
        if (result.next()) {
          return fetchScore(tournament, teamNumber, runNumber, connection, result);
        } else {
          return null;
        }
      }
    }
  }

  /**
   * Be very careful using this method to ensure that the columns in the select
   * statement line up.
   */
  private static DefaultPerformanceTeamScore fetchScore(final int tournament,
                                                        final int teamNumber,
                                                        final int runNumber,
                                                        final Connection connection,
                                                        final ResultSet result)
      throws SQLException {
    final boolean noShow = result.getBoolean(1);
    final boolean bye = result.getBoolean(2);
    final boolean verified = result.getBoolean(3);
    final String tablename = castNonNull(result.getString(4));
    final Timestamp ts = (Timestamp) castNonNull(result.getTimestamp(5));
    final LocalDateTime timestamp = ts.toLocalDateTime();

    final Map<String, Double> simpleGoals = fetchSimpleGoals(tournament, teamNumber, runNumber, connection);
    final Map<String, String> enumGoals = fetchEnumGoals(tournament, teamNumber, runNumber, connection);

    return new DefaultPerformanceTeamScore(teamNumber, runNumber, simpleGoals, enumGoals, tablename, noShow, bye,
                                           verified, timestamp);
  }

  private static Map<String, Double> fetchSimpleGoals(final int tournament,
                                                      final int teamNumber,
                                                      final int runNumber,
                                                      final Connection connection)
      throws SQLException {
    final Map<String, Double> values = new HashMap<>();
    try (PreparedStatement prep = connection.prepareStatement("SELECT goal_name, goal_value FROM performance_goals" //
        + " WHERE tournament_id = ?" //
        + " AND team_number = ?" //
        + " AND run_number = ?")) {
      prep.setInt(1, tournament);
      prep.setInt(2, teamNumber);
      prep.setInt(3, runNumber);
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String name = castNonNull(rs.getString(1));
          final double value = rs.getDouble(2);
          values.put(name, value);
        }
      }
    }
    return values;
  }

  private static Map<String, String> fetchEnumGoals(final int tournament,
                                                    final int teamNumber,
                                                    final int runNumber,
                                                    final Connection connection)
      throws SQLException {
    final Map<String, String> values = new HashMap<>();
    try (PreparedStatement prep = connection.prepareStatement("SELECT goal_name, goal_value FROM performance_enum_goals" //
        + " WHERE tournament_id = ?" //
        + " AND team_number = ?" //
        + " AND run_number = ?")) {
      prep.setInt(1, tournament);
      prep.setInt(2, teamNumber);
      prep.setInt(3, runNumber);
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String name = castNonNull(rs.getString(1));
          final String value = castNonNull(rs.getString(2));
          values.put(name, value);
        }
      }
    }
    return values;
  }

}
