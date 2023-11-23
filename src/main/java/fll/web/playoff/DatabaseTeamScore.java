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
import java.util.Collections;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import fll.Utilities;

/**
 * TeamScore implementation for a performance score in the database.
 */
public class DatabaseTeamScore extends TeamScore {

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
    scoreExists = true;
  }

  /**
   * Create a database team score object for a performance score.
   *
   * @param categoryName the category to delete the score from
   * @param tournament the tournament
   * @param connection the connection to get the data from
   * @param teamNumber passed to superclass
   * @param runNumber passed to superclass
   * @throws SQLException if there is an error getting the data
   */
  public DatabaseTeamScore(final String categoryName,
                           final int tournament,
                           final int teamNumber,
                           final int runNumber,
                           final Connection connection)
      throws SQLException {
    super(teamNumber, runNumber);

    try (PreparedStatement prep = connection.prepareStatement("SELECT * FROM "
        + categoryName
        + " WHERE TeamNumber = ? AND Tournament = ?"
        + (NON_PERFORMANCE_RUN_NUMBER == getRunNumber() ? "" : " AND RunNumber = ?"))) {
      if (NON_PERFORMANCE_RUN_NUMBER != getRunNumber()) {
        prep.setInt(3, getRunNumber());
      }

      prep.setInt(1, getTeamNumber());
      prep.setInt(2, tournament);
      try (ResultSet result = prep.executeQuery()) {
        if (result.next()) {
          scoreExists = true;
          data = Utilities.resultSetRowToMap(result);
        } else {
          scoreExists = false;
          data = Collections.emptyMap();
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
    scoreExists = true;
  }

  private @Nullable String getString(final String goalName) {
    final Object value = data.get(goalName);
    if (null == value) {
      return null;
    } else {
      return value.toString();
    }
  }

  public double getDouble(final String goalName) {
    final Object value = data.get(goalName);
    if (null == value) {
      return Double.NaN;
    } else if (value instanceof Number) {
      return ((Number) value).doubleValue();
    } else {
      return Double.parseDouble(value.toString());
    }
  }

  public boolean getBoolean(final String goalName) {
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
    if (!scoreExists()) {
      return null;
    } else {
      return getString(goalName);
    }
  }

  @Override
  public double getRawScore(final String goalName) {
    if (!scoreExists()) {
      return Double.NaN;
    } else {
      return getDouble(goalName);
    }
  }

  @Override
  public boolean isNoShow() {
    if (!scoreExists()) {
      return false;
    } else {
      return getBoolean("NoShow");
    }
  }

  @Override
  public boolean isBye() {
    if (!scoreExists()) {
      return false;
    } else {
      return getBoolean("Bye");
    }
  }

  @Override
  public boolean isVerified() {
    if (!scoreExists()) {
      return false;
    } else {
      return getBoolean("Verified");
    }
  }

  @Override
  public boolean scoreExists() {
    return scoreExists;
  }

  private final boolean scoreExists;

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
