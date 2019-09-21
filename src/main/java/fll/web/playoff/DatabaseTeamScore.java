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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * TeamScore implementation for a performance score in the database. Note that
 * this object is only valid as long as the {@link ResultSet} used to create it
 * is valid.
 */
public class DatabaseTeamScore extends TeamScore implements AutoCloseable {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private final boolean closeResultSet;

  /**
   * Create a database team score object for a non-performance score, for use
   * when the result set is already available.
   *
   * @param teamNumber passed to superclass
   * @param rs the {@link ResultSet} to pull the scores from, the object is to be
   *          closed by the caller
   * @throws SQLException if there is an error getting the current tournament
   */
  public DatabaseTeamScore(final int teamNumber,
                           final ResultSet rs)
      throws SQLException {
    super(teamNumber);

    _result = rs;
    _scoreExists = true;
    closeResultSet = false;
  }

  /**
   * Create a database team score object for a performance score.
   *
   * @param connection the connection to get the data from
   * @param teamNumber passed to superclass
   * @param runNumber passed to superclass
   * @throws SQLException if there is an error getting the current tournament
   */
  public DatabaseTeamScore(final String categoryName,
                           final int tournament,
                           final int teamNumber,
                           final int runNumber,
                           final Connection connection)
      throws SQLException {
    super(teamNumber, runNumber);

    _result = createResultSet(connection, tournament, categoryName);
    _scoreExists = _result.next();
    closeResultSet = true;
  }

  /**
   * Create a database team score object for a performance score, for use when
   * the result set is already available.
   *
   * @param teamNumber passed to superclass
   * @param runNumber passed to superclass
   * @param rs the {@link ResultSet} to pull the scores from, the object is to be
   *          closed by the caller
   * @throws SQLException if there is an error getting the current tournament
   */
  public DatabaseTeamScore(final int teamNumber,
                           final int runNumber,
                           final ResultSet rs)
      throws SQLException {
    super(teamNumber, runNumber);

    _result = rs;
    _scoreExists = true;
    closeResultSet = false;
  }

  @Override
  public String getEnumRawScore(final String goalName) {
    if (!scoreExists()) {
      return null;
    } else {
      try {
        return getResultSet().getString(goalName);
      } catch (final SQLException sqle) {
        throw new RuntimeException(sqle);
      }
    }
  }

  @Override
  public double getRawScore(final String goalName) {
    if (!scoreExists()) {
      return Double.NaN;
    } else {
      try {
        final double val = getResultSet().getDouble(goalName);
        if (getResultSet().wasNull()) {
          return Double.NaN;
        } else {
          return val;
        }
      } catch (final SQLException sqle) {
        throw new RuntimeException(sqle);
      }
    }
  }

  @Override
  public boolean isNoShow() {
    if (!scoreExists()) {
      return false;
    } else {
      try {
        return getResultSet().getBoolean("NoShow");
      } catch (final SQLException sqle) {
        throw new RuntimeException(sqle);
      }
    }
  }

  @Override
  public boolean isBye() {
    if (!scoreExists()) {
      return false;
    } else {
      try {
        return getResultSet().getBoolean("Bye");
      } catch (final SQLException sqle) {
        throw new RuntimeException(sqle);
      }
    }
  }

  /**
   * @see fll.web.playoff.TeamScore#scoreExists()
   */
  @Override
  public boolean scoreExists() {
    return _scoreExists;
  }

  private final boolean _scoreExists;

  @Override
  protected void finalize() {
    try {
      close();
    } catch (final SQLException e) {
      LOGGER.error("Error cleaning up", e);
    }
  }

  private final ResultSet _result;

  private ResultSet getResultSet() {
    return _result;
  }

  private PreparedStatement _prep = null;

  /**
   * Create the result set.
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category determines table")
  private ResultSet createResultSet(final Connection connection,
                                    final int tournament,
                                    final String categoryName)
      throws SQLException {
    ResultSet result;
    if (NON_PERFORMANCE_RUN_NUMBER == getRunNumber()) {
      _prep = connection.prepareStatement("SELECT * FROM "
          + categoryName
          + " WHERE TeamNumber = ? AND Tournament = ?");
    } else {
      _prep = connection.prepareStatement("SELECT * FROM "
          + categoryName
          + " WHERE TeamNumber = ? AND Tournament = ? AND RunNumber = ?");
      _prep.setInt(3, getRunNumber());
    }
    _prep.setInt(1, getTeamNumber());
    _prep.setInt(2, tournament);
    result = _prep.executeQuery();
    return result;
  }

  // AutoCloseable
  @Override
  public void close() throws SQLException {
    if (closeResultSet) {
      _result.close();
    }

    if (null != _prep) {
      _prep.close();
      _prep = null;
    }
  }
  // end AutoCloseable

}
