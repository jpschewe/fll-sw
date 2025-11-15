/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.playoff;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import fll.Utilities;

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
   * @param goalName the goal to get as a string
   * @return the value or {@code null} if not found
   */
  protected final @Nullable String getString(final String goalName) {
    final Object value = data.get(goalName);
    if (null == value) {
      return null;
    } else {
      return value.toString();
    }
  }

  /**
   * @param goalName the goal to get as a string
   * @return the value or {@link Double#NaN} if not found
   */
  protected final double getDouble(final String goalName) {
    final Object value = data.get(goalName);
    if (null == value) {
      return Double.NaN;
    } else if (value instanceof Number) {
      return ((Number) value).doubleValue();
    } else {
      return Double.parseDouble(value.toString());
    }
  }

  /**
   * @param goalName the goal to get as a string
   * @return the value or {@code false} if not found
   */
  protected final boolean getBoolean(final String goalName) {
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

}
