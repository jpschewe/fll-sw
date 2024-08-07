/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Utilities;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;

/**
 * Constants for the global parameters in the database.
 */
public final class GlobalParameters {
  /**
   * Parameter name for current tournament.
   */
  public static final String CURRENT_TOURNAMENT = "CurrentTournament";

  /**
   * Parameter name for {@link #getChallengeDescription(Connection)}.
   */
  public static final String CHALLENGE_DOCUMENT = "ChallengeDocument";

  /**
   * Parameter name for division flip rate on the score board.
   */
  public static final String DIVISION_FLIP_RATE = "DivisionFlipRate";

  /**
   * Default value for {@link #DIVISION_FLIP_RATE}.
   */
  public static final int DIVISION_FLIP_RATE_DEFAULT = 30;

  private GlobalParameters() {
    // no instances
  }

  /**
   * Parameter name for database version.
   */
  public static final String DATABASE_VERSION = "DatabaseVersion";

  /**
   * @param connection database connection
   * @return the prepared statement for getting a global parameter with the
   *         values already filled in, the calling method must close this on all
   *         execution paths
   * @throws SQLException on a database error
   */
  @SuppressFBWarnings(value = "OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE", justification = "All callers clean up the PreparedStatement")
  private static PreparedStatement getGlobalParameterStmt(final Connection connection,
                                                          final String paramName)
      throws SQLException {
    final PreparedStatement prep = connection.prepareStatement("SELECT param_value FROM global_parameters WHERE param = ?");
    prep.setString(1, paramName);
    return prep;
  }

  /**
   * Get a global parameter from the database that is a double.
   *
   * @param connection the database connection
   * @param parameter the parameter name
   * @return the value
   * @throws SQLException on a database error
   * @throws IllegalArgumentException if the parameter cannot be found
   */
  public static double getDoubleGlobalParameter(final Connection connection,
                                                final String parameter)
      throws SQLException {
    try (PreparedStatement prep = getGlobalParameterStmt(connection, parameter)) {
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return rs.getDouble(1);
        } else {
          throw new IllegalArgumentException("Can't find '"
              + parameter
              + "' in global_parameters");
        }
      }
    }
  }

  /**
   * Get a global parameter from the database that is an int.
   *
   * @param connection the database connection
   * @param parameter the parameter name
   * @return the value
   * @throws SQLException on a database error
   * @throws IllegalArgumentException if the parameter cannot be found
   */
  public static int getIntGlobalParameter(final Connection connection,
                                          final String parameter)
      throws SQLException {
    try (PreparedStatement prep = getGlobalParameterStmt(connection, parameter); ResultSet rs = prep.executeQuery()) {
      if (rs.next()) {
        return rs.getInt(1);
      } else {
        throw new IllegalArgumentException("Can't find '"
            + parameter
            + "' in global_parameters");
      }
    }
  }

  /**
   * Get a string global parameter.
   *
   * @param connection database connection
   * @param parameter the parameter name to find
   * @return the value of the parameter, may be null
   * @throws SQLException on a database error
   * @throws IllegalArgumentException if the parameter cannot be found
   */
  public static String getStringGlobalParameter(final Connection connection,
                                                final String parameter)
      throws SQLException {
    try (PreparedStatement prep = getGlobalParameterStmt(connection, parameter)) {
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return castNonNull(rs.getString(1));
        } else {
          throw new IllegalArgumentException("Can't find '"
              + parameter
              + "' in global_parameters");
        }
      }
    }
  }

  /**
   * Check if a value exists for a global parameter.
   * 
   * @param connection database connection
   * @param paramName the parameter name
   * @return if the parameter exists
   * @throws SQLException on a database error
   */
  public static boolean globalParameterExists(final Connection connection,
                                              final String paramName)
      throws SQLException {
    try (PreparedStatement prep = getGlobalParameterStmt(connection, paramName); ResultSet rs = prep.executeQuery()) {
      return rs.next();
    }
  }

  /**
   * Get the challenge description out of the database. This method doesn't
   * validate the document, since it's assumed that the document was validated
   * before it was put in the database.
   *
   * @param connection connection to the database
   * @return the description
   * @throws FLLRuntimeException if the description cannot be found
   * @throws SQLException on a database error
   * @throws FLLInternalException if the challenge document is not in the
   *           database
   */
  public static ChallengeDescription getChallengeDescription(final Connection connection)
      throws SQLException, FLLInternalException {
    try (PreparedStatement prep = getGlobalParameterStmt(connection, CHALLENGE_DOCUMENT)) {
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return ChallengeParser.parse(new InputStreamReader(castNonNull(rs.getAsciiStream(1)),
                                                             Utilities.DEFAULT_CHARSET));
        } else {
          throw new FLLInternalException("Could not find challenge document in database");
        }
      }
    }
  }

  /**
   * @param connection database connection
   * @param paramName parameter name
   * @param paramValue parameter value
   * @throws SQLException on a database error
   */
  public static void setStringGlobalParameter(final Connection connection,
                                              final String paramName,
                                              final String paramValue)
      throws SQLException {
    final boolean exists = globalParameterExists(connection, paramName);
    if (!exists) {
      insertStringGlobalParameter(connection, paramName, paramValue);
    } else {
      updateStringGlobalParameter(connection, paramName, paramValue);
    }
  }

  private static void insertStringGlobalParameter(final Connection connection,
                                                  final String paramName,
                                                  final String paramValue)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("INSERT INTO global_parameters (param_value, param) VALUES (?, ?)")) {
      prep.setString(1, paramValue);
      prep.setString(2, paramName);
      prep.executeUpdate();
    }
  }

  private static void updateStringGlobalParameter(final Connection connection,
                                                  final String paramName,
                                                  final String paramValue)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("UPDATE global_parameters SET param_value = ? WHERE param = ?")) {
      prep.setString(1, paramValue);
      prep.setString(2, paramName);
      prep.executeUpdate();
    }
  }

  /**
   * @param connection database connection
   * @param paramName parameter name
   * @param paramValue parameter value
   * @throws SQLException on a database error
   */
  public static void setDoubleGlobalParameter(final Connection connection,
                                              final String paramName,
                                              final double paramValue)
      throws SQLException {
    final boolean exists = globalParameterExists(connection, paramName);
    if (!exists) {
      insertDoubleGlobalParameter(connection, paramName, paramValue);
    } else {
      updateDoubleGlobalParameter(connection, paramName, paramValue);
    }
  }

  private static void insertDoubleGlobalParameter(final Connection connection,
                                                  final String paramName,
                                                  final double paramValue)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("INSERT INTO global_parameters (param_value, param) VALUES (?, ?)")) {
      prep.setDouble(1, paramValue);
      prep.setString(2, paramName);
      prep.executeUpdate();
    }
  }

  private static void updateDoubleGlobalParameter(final Connection connection,
                                                  final String paramName,
                                                  final double paramValue)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("UPDATE global_parameters SET param_value = ? WHERE param = ?")) {
      prep.setDouble(1, paramValue);
      prep.setString(2, paramName);
      prep.executeUpdate();
    }
  }

  /**
   * @param connection database connection
   * @param paramName parameter name
   * @param paramValue parameter value
   * @throws SQLException on a database error
   */
  public static void setIntGlobalParameter(final Connection connection,
                                           final String paramName,
                                           final int paramValue)
      throws SQLException {
    final boolean exists = globalParameterExists(connection, paramName);
    if (!exists) {
      insertIntGlobalParameter(connection, paramName, paramValue);
    } else {
      updateIntGlobalParameter(connection, paramName, paramValue);
    }
  }

  private static void insertIntGlobalParameter(final Connection connection,
                                               final String paramName,
                                               final int paramValue)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("INSERT INTO global_parameters (param_value, param) VALUES (?, ?)")) {
      prep.setInt(1, paramValue);
      prep.setString(2, paramName);
      prep.executeUpdate();
    }
  }

  private static void updateIntGlobalParameter(final Connection connection,
                                               final String paramName,
                                               final int paramValue)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("UPDATE global_parameters SET param_value = ? WHERE param = ?")) {
      prep.setInt(1, paramValue);
      prep.setString(2, paramName);
      prep.executeUpdate();
    }
  }

  /**
   * Get a global parameter from the database that is a boolean.
   *
   * @param connection the database connection
   * @param parameter the parameter name
   * @return the value
   * @throws SQLException on a database error
   * @throws IllegalArgumentException if the parameter cannot be found
   */
  public static boolean getBooleanGlobalParameter(final Connection connection,
                                                  final String parameter)
      throws SQLException {
    try (PreparedStatement prep = getGlobalParameterStmt(connection, parameter); ResultSet rs = prep.executeQuery()) {
      if (rs.next()) {
        return rs.getBoolean(1);
      } else {
        throw new IllegalArgumentException("Can't find '"
            + parameter
            + "' in global_parameters");
      }
    }
  }

  /**
   * @param connection database connection
   * @param paramName parameter name
   * @param paramValue parameter value
   * @throws SQLException on a database error
   */
  public static void setBooleanGlobalParameter(final Connection connection,
                                               final String paramName,
                                               final boolean paramValue)
      throws SQLException {
    final boolean exists = globalParameterExists(connection, paramName);
    if (!exists) {
      insertBooleanGlobalParameter(connection, paramName, paramValue);
    } else {
      updateBooleanGlobalParameter(connection, paramName, paramValue);
    }
  }

  private static void insertBooleanGlobalParameter(final Connection connection,
                                                   final String paramName,
                                                   final boolean paramValue)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("INSERT INTO global_parameters (param_value, param) VALUES (?, ?)")) {
      prep.setBoolean(1, paramValue);
      prep.setString(2, paramName);
      prep.executeUpdate();
    }
  }

  private static void updateBooleanGlobalParameter(final Connection connection,
                                                   final String paramName,
                                                   final boolean paramValue)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("UPDATE global_parameters SET param_value = ? WHERE param = ?")) {
      prep.setBoolean(1, paramValue);
      prep.setString(2, paramName);
      prep.executeUpdate();
    }
  }

  /**
   * Parameter name for {@link #getAllTeamScrollRate(Connection)}.
   */
  public static final String ALL_TEAMS_SCROLL_RATE = "AllTeamsScrollRate";

  /**
   * Default value for {@link #ALL_TEAMS_SCROLL_RATE}.
   */
  public static final double ALL_TEAMS_SCROLL_RATE_DEFAULT = 0.03;

  /**
   * Some control over the scroll rate of the all teams part of the score board.
   * The value is the number of seconds between requests to scroll a single pixel.
   *
   * @param connection the database connection
   * @return the nominal scroll rate
   * @throws SQLException if there is a problem talking to the database
   * @see #ALL_TEAMS_SCROLL_RATE_DEFAULT
   */
  public static double getAllTeamScrollRate(final Connection connection) throws SQLException {
    if (!globalParameterExists(connection, ALL_TEAMS_SCROLL_RATE)) {
      return ALL_TEAMS_SCROLL_RATE_DEFAULT;
    } else {
      return getDoubleGlobalParameter(connection, ALL_TEAMS_SCROLL_RATE);
    }
  }

  /**
   * See {@link #getAllTeamScrollRate(Connection)}.
   *
   * @param connection the database connection
   * @param value the new value
   * @throws SQLException if there is a problem talking to the database
   */
  public static void setAllTeamsScrollRate(final Connection connection,
                                           final double value)
      throws SQLException {
    setDoubleGlobalParameter(connection, ALL_TEAMS_SCROLL_RATE, value);
  }

  /**
   * Parameter name for {@link #getHeadToHeadScrollRate(Connection)}.
   */
  public static final String HEAD_TO_HEAD_SCROLL_RATE = "HeadToHeadScrollRate";

  /**
   * Default value for {@link #HEAD_TO_HEAD_SCROLL_RATE}.
   */
  public static final double HEAD_TO_HEAD_SCROLL_RATE_DEFAULT = 0.03;

  /**
   * Some control over the scroll rate of the head to head brackets.
   * See {@link #getAllTeamScrollRate(Connection)} for more information.
   *
   * @param connection the database connection
   * @return the nominal scroll rate
   * @throws SQLException if there is a problem talking to the database
   * @see #HEAD_TO_HEAD_SCROLL_RATE_DEFAULT
   */
  public static double getHeadToHeadScrollRate(final Connection connection) throws SQLException {
    if (!globalParameterExists(connection, HEAD_TO_HEAD_SCROLL_RATE)) {
      return HEAD_TO_HEAD_SCROLL_RATE_DEFAULT;
    } else {
      return getDoubleGlobalParameter(connection, HEAD_TO_HEAD_SCROLL_RATE);
    }
  }

  /**
   * See {@link #getHeadToHeadScrollRate(Connection)}.
   *
   * @param connection the database connection
   * @param value the new value
   * @throws SQLException if there is a problem talking to the database
   */
  public static void setHeadToHeadScrollRate(final Connection connection,
                                             final double value)
      throws SQLException {
    setDoubleGlobalParameter(connection, HEAD_TO_HEAD_SCROLL_RATE, value);
  }

}
