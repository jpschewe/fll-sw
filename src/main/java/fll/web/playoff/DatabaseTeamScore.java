/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.playoff;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import fll.Utilities;
import fll.db.GenerateDB;

/**
 * TeamScore implementation for a performance score in the database.
 */
public class DatabaseTeamScore extends BaseTeamScore {

  private final Map<String, @Nullable Object> data;

  /**
   * Create a database team score object for a non-performance score, for use
   * when the result set is already available.
   *
   * @param teamNumber passed to superclass
   * @param rs the {@link ResultSet} to pull the scores from, the object is to be
   *          closed by the caller. The {@link ResultSet} is not stored.
   * @throws SQLException if there is an error getting the data
   */
  public DatabaseTeamScore(final int teamNumber,
                           final ResultSet rs)
      throws SQLException {
    super(teamNumber);

    data = Utilities.resultSetRowToMap(rs);
  }

  /**
   * Create a database team score object for a performance score.
   * 
   * @param tournament the tournament ID
   * @param teamNumber see {@link TeamScore#getTeamNumber()}
   * @param runNumber see {@link TeamScore#getRunNumber()}
   * @param connection the connection to get the data from
   * @return the {@link TeamScore} or {@code null} if no score exists matching the
   *         supplied criteria
   * @throws SQLException if there is an error getting the data
   */
  public static @Nullable DatabaseTeamScore fetchTeamScore(final int tournament,
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
          return new DatabaseTeamScore(teamNumber, result);
        } else {
          return null;
        }
      }
    }
  }

  /**
   * Create a database team score object for a performance score, for use when
   * the result set is already available.
   *
   * @param teamNumber passed to superclass
   * @param runNumber passed to superclass
   * @param rs the {@link ResultSet} to pull the scores from, the object is to be
   *          closed by the caller
   * @throws SQLException if there is an error getting the data
   */
  public DatabaseTeamScore(final int teamNumber,
                           final int runNumber,
                           final ResultSet rs)
      throws SQLException {
    super(teamNumber, runNumber);

    data = Utilities.resultSetRowToMap(rs);
  }

  private @Nullable String getString(final String goalName) {
    final Object value = data.get(goalName);
    if (null == value) {
      return null;
    } else {
      return value.toString();
    }
  }

  private double getDouble(final String goalName) {
    final Object value = data.get(goalName);
    if (null == value) {
      return Double.NaN;
    } else if (value instanceof Number) {
      return ((Number) value).doubleValue();
    } else {
      return Double.parseDouble(value.toString());
    }
  }

  private boolean getBoolean(final String goalName) {
    final Object value = data.get(goalName);
    if (null == value) {
      return false;
    } else if (value instanceof Boolean) {
      return ((Boolean) value).booleanValue();
    } else {
      return Boolean.valueOf(value.toString());
    }
  }

  @Override
  public @Nullable String getEnumRawScore(final String goalName) {
    return getString(goalName);
  }

  @Override
  public double getRawScore(final String goalName) {
    return getDouble(goalName);
  }

  @Override
  public boolean isNoShow() {
    return getBoolean("NoShow");
  }

  @Override
  public boolean isBye() {
    return getBoolean("Bye");
  }

  @Override
  public boolean isVerified() {
    return getBoolean("Verified");
  }

  @Override
  public String getTable() {
    final @Nullable String table = getString("tablename");
    if (null == table) {
      return "UNKNOWN";
    } else {
      return table;
    }
  }

}
