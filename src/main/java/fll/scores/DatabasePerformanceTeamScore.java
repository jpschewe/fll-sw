/*
 * This code is released under GPL; see LICENSE for details.
 */

package fll.scores;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.db.GenerateDB;

/**
 * Performance score from the database.
 */
public final class DatabasePerformanceTeamScore {

  private DatabasePerformanceTeamScore() {
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

    try (PreparedStatement prep = connection.prepareStatement("SELECT NoShow, Bye, Verified, Tablename FROM "
        + GenerateDB.PERFORMANCE_TABLE_NAME
        + " WHERE TeamNumber = ? AND Tournament = ?"
        + " AND RunNumber = ?")) {

      prep.setInt(1, teamNumber);
      prep.setInt(2, tournament);
      prep.setInt(3, runNumber);
      try (ResultSet result = prep.executeQuery()) {
        if (result.next()) {
          final boolean noShow = result.getBoolean(1);
          final boolean bye = result.getBoolean(2);
          final boolean verified = result.getBoolean(3);
          final String tablename = castNonNull(result.getString(4));

          final Map<String, Double> simpleGoals = fetchSimpleGoals(tournament, teamNumber, runNumber, connection);
          final Map<String, String> enumGoals = fetchEnumGoals(tournament, teamNumber, runNumber, connection);

          return new DefaultPerformanceTeamScore(teamNumber, runNumber, simpleGoals, enumGoals, tablename, noShow, bye,
                                                 verified);
        } else {
          return null;
        }
      }
    }
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
