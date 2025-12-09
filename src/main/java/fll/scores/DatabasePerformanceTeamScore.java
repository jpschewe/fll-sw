/*
 * This code is released under GPL; see LICENSE for details.
 */

package fll.scores;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.checkerframework.checker.nullness.qual.Nullable;

import fll.db.GenerateDB;
import fll.web.playoff.PerformanceTeamScore;

/**
 * Performance score from the database.
 */
public class DatabasePerformanceTeamScore extends BasePerformanceTeamScore {

  /**
   * Create a database team score object for a performance score, for use
   * when the result set is already available.
   *
   * @param teamNumber see {@link #getTeamNumber()}
   * @param runNumber see {@link #getRunNumber()}
   * @param rs the {@link ResultSet} to pull the scores from, the object is to be
   *          closed by the caller. The {@link ResultSet} is not stored.
   * @throws SQLException if there is an error getting the data
   */
  public DatabasePerformanceTeamScore(final int teamNumber,
                                      final int runNumber,
                                      final ResultSet rs)
      throws SQLException {
    super(teamNumber, runNumber);
    delegate = new DatabaseTeamScore(teamNumber, rs);
  }

  private final DatabaseTeamScore delegate;

  @Override
  public boolean isNoShow() {
    return delegate.isNoShow();
  }

  @Override
  public boolean isBye() {
    return delegate.getBoolean("Bye");
  }

  @Override
  public boolean isVerified() {
    return delegate.getBoolean("Verified");
  }

  @Override
  public String getTable() {
    final @Nullable String table = delegate.getString("tablename");
    if (null == table) {
      return "UNKNOWN";
    } else {
      return table;
    }
  }

  @Override
  public double getRawScore(String goalName) {
    return delegate.getRawScore(goalName);
  }

  @Override
  public @Nullable String getEnumRawScore(String goalName) {
    return delegate.getEnumRawScore(goalName);
  }

  /**
   * Create a database team score object for a performance score.
   * 
   * @param tournament the tournament ID
   * @param teamNumber see {@link #getTeamNumber()}
   * @param runNumber see {@link #getRunNumber()}
   * @param connection the connection to get the data from
   * @return the {@link TeamScore} or {@code null} if no score exists matching the
   *         supplied criteria
   * @throws SQLException if there is an error getting the data
   */
  public static @Nullable PerformanceTeamScore fetchTeamScore(final int tournament,
                                                              final int teamNumber,
                                                              final int runNumber,
                                                              final Connection connection)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT * FROM "
        + GenerateDB.PERFORMANCE_TABLE_NAME
        + " WHERE TeamNumber = ? AND Tournament = ?"
        + " AND RunNumber = ?")) {
      prep.setInt(3, runNumber);

      prep.setInt(1, teamNumber);
      prep.setInt(2, tournament);
      try (ResultSet result = prep.executeQuery()) {
        if (result.next()) {
          return new DatabasePerformanceTeamScore(teamNumber, runNumber, result);
        } else {
          return null;
        }
      }
    }
  }

}
