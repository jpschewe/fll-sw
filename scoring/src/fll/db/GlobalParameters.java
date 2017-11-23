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

import javax.annotation.Nonnull;

import org.w3c.dom.Document;

import fll.Utilities;
import fll.util.FLLRuntimeException;
import fll.xml.ChallengeParser;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Constants for the global parameters in the database
 */
public final class GlobalParameters {
  public static final String CURRENT_TOURNAMENT = "CurrentTournament";

  public static final String STANDARDIZED_MEAN = "StandardizedMean";

  public static final double STANDARDIZED_MEAN_DEFAULT = 100;

  public static final String STANDARDIZED_SIGMA = "StandardizedSigma";

  public static final double STANDARDIZED_SIGMA_DEFAULT = 20;

  public static final String CHALLENGE_DOCUMENT = "ChallengeDocument";

  public static final int SCORESHEET_LAYOUT_NUP_DEFAULT = 2;

  public static final String DIVISION_FLIP_RATE = "DivisionFlipRate";

  public static final int DIVISION_FLIP_RATE_DEFAULT = 30;

  public static final String RANKING_REPORT_USE_QUARTILES = "RankingReportUseQuartiles";

  public static final boolean RANKING_REPORT_USE_QUARTILES_DEFAULT = true;

  /**
   * Should the ranking report show quartiles or actual ranks?
   * 
   * @return true to use quartiles
   * @throws SQLException
   */
  public static boolean getUseQuartilesInRankingReport(final Connection connection) throws SQLException {
    if (!globalParameterExists(connection, RANKING_REPORT_USE_QUARTILES)) {
      return RANKING_REPORT_USE_QUARTILES_DEFAULT;
    } else {
      return getBooleanGlobalParameter(connection, RANKING_REPORT_USE_QUARTILES);
    }
  }

  public static void setUseQuartilesInRankingReport(final Connection connection,
                                                    final boolean value)
      throws SQLException {
    setBooleanGlobalParameter(connection, RANKING_REPORT_USE_QUARTILES, value);
  }

  private GlobalParameters() {
    // no instances
  }

  public static final String DATABASE_VERSION = "DatabaseVersion";

  /**
   * @param connection
   * @return the prepared statement for getting a global parameter with the
   *         values already filled in
   * @throws SQLException
   */
  private static PreparedStatement getGlobalParameterStmt(final Connection connection,
                                                          final String paramName)
      throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("SELECT param_value FROM global_parameters WHERE param = ?");
      prep.setString(1, paramName);
      return prep;
    } catch (final SQLException e) {
      SQLFunctions.close(prep);
      throw e;
    }
  }

  /**
   * Get a global parameter from the database that is a double.
   * 
   * @param connection the database connection
   * @param parameter the parameter name
   * @return the value
   * @throws SQLException
   * @throws IllegalArgumentException if the parameter cannot be found
   */
  public static double getDoubleGlobalParameter(final Connection connection,
                                                final String parameter)
      throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = getGlobalParameterStmt(connection, parameter);
      rs = prep.executeQuery();
      if (rs.next()) {
        return rs.getDouble(1);
      } else {
        throw new IllegalArgumentException("Can't find '"
            + parameter
            + "' in global_parameters");
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Get a global parameter from the database that is an int.
   * 
   * @param connection the database connection
   * @param parameter the parameter name
   * @return the value
   * @throws SQLException
   * @throws IllegalArgumentException if the parameter cannot be found
   */
  public static int getIntGlobalParameter(final Connection connection,
                                          final String parameter)
      throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = getGlobalParameterStmt(connection, parameter);
      rs = prep.executeQuery();
      if (rs.next()) {
        return rs.getInt(1);
      } else {
        throw new IllegalArgumentException("Can't find '"
            + parameter
            + "' in global_parameters");
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Get a string global parameter.
   * 
   * @return the value of the parameter, may be null
   * @throws IllegalArgumentException if the parameter cannot be found
   */
  public static String getStringGlobalParameter(final Connection connection,
                                                final String parameter)
      throws SQLException {
    try (PreparedStatement prep = getGlobalParameterStmt(connection, parameter)) {
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return rs.getString(1);
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
   */
  public static boolean globalParameterExists(final Connection connection,
                                              final String paramName)
      throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = getGlobalParameterStmt(connection, paramName);
      rs = prep.executeQuery();
      return rs.next();
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Get the challenge document out of the database. This method doesn't
   * validate the document, since it's assumed that the document was validated
   * before it was put in the database.
   * 
   * @param connection connection to the database
   * @return the document
   * @throws FLLRuntimeException if the document cannot be found
   * @throws SQLException on a database error
   */
  public static Document getChallengeDocument(final Connection connection) throws SQLException, RuntimeException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {

      prep = getGlobalParameterStmt(connection, CHALLENGE_DOCUMENT);
      rs = prep.executeQuery();
      if (rs.next()) {
        return ChallengeParser.parse(new InputStreamReader(rs.getAsciiStream(1), Utilities.DEFAULT_CHARSET));
      } else {
        throw new FLLRuntimeException("Could not find challenge document in database");
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  public static void setStringGlobalParameter(final Connection connection,
                                              final String paramName,
                                              final String paramValue)
      throws SQLException {
    final boolean exists = globalParameterExists(connection, paramName);
    PreparedStatement prep = null;
    try {
      if (!exists) {
        prep = connection.prepareStatement("INSERT INTO global_parameters (param_value, param) VALUES (?, ?)");
      } else {
        prep = connection.prepareStatement("UPDATE global_parameters SET param_value = ? WHERE param = ?");
      }
      prep.setString(1, paramValue);
      prep.setString(2, paramName);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  public static void setDoubleGlobalParameter(final Connection connection,
                                              final String paramName,
                                              final double paramValue)
      throws SQLException {
    final boolean exists = globalParameterExists(connection, paramName);
    PreparedStatement prep = null;
    try {
      if (!exists) {
        prep = connection.prepareStatement("INSERT INTO global_parameters (param_value, param) VALUES (?, ?)");
      } else {
        prep = connection.prepareStatement("UPDATE global_parameters SET param_value = ? WHERE param = ?");
      }
      prep.setDouble(1, paramValue);
      prep.setString(2, paramName);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  public static void setIntGlobalParameter(final Connection connection,
                                           final String paramName,
                                           final int paramValue)
      throws SQLException {
    final boolean exists = globalParameterExists(connection, paramName);
    PreparedStatement prep = null;
    try {
      if (!exists) {
        prep = connection.prepareStatement("INSERT INTO global_parameters (param_value, param) VALUES (?, ?)");
      } else {
        prep = connection.prepareStatement("UPDATE global_parameters SET param_value = ? WHERE param = ?");
      }
      prep.setInt(1, paramValue);
      prep.setString(2, paramName);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  /**
   * Get a global parameter from the database that is a boolean.
   * 
   * @param connection the database connection
   * @param parameter the parameter name
   * @return the value
   * @throws SQLException
   * @throws IllegalArgumentException if the parameter cannot be found
   */
  public static boolean getBooleanGlobalParameter(final Connection connection,
                                                  final String parameter)
      throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = getGlobalParameterStmt(connection, parameter);
      rs = prep.executeQuery();
      if (rs.next()) {
        return rs.getBoolean(1);
      } else {
        throw new IllegalArgumentException("Can't find '"
            + parameter
            + "' in global_parameters");
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  public static void setBooleanGlobalParameter(final Connection connection,
                                               final String paramName,
                                               final boolean paramValue)
      throws SQLException {
    final boolean exists = globalParameterExists(connection, paramName);
    PreparedStatement prep = null;
    try {
      if (!exists) {
        prep = connection.prepareStatement("INSERT INTO global_parameters (param_value, param) VALUES (?, ?)");
      } else {
        prep = connection.prepareStatement("UPDATE global_parameters SET param_value = ? WHERE param = ?");
      }
      prep.setBoolean(1, paramValue);
      prep.setString(2, paramName);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  /**
   * Get the standardized mean used for normalization. All scores are normalized
   * such that the mean of the scores is this value.
   * 
   * @param connection the database connection
   * @return the value (should be greater than 0)
   * @throws SQLException if there is an error talking to the database
   * @see {@link #STANDARDIZED_MEAN}
   * @see {@link #STANDARDIZED_MEAN_DEFAULT}
   */
  public static double getStandardizedMean(@Nonnull final Connection connection) throws SQLException {
    return getDoubleGlobalParameter(connection, STANDARDIZED_MEAN);
  }

  /**
   * Get the standardized sigma. During normalization this value specifies how the
   * number of points for 1 standard deviation from the mean.
   * 
   * @param connection the database connection
   * @return the value (should be greater than 0)
   * @throws SQLException if there is an error talking to the database
   * @see {@link #STANDARDIZED_SIGMA}
   * @see {@link #STANDARDIZED_SIGMA_DEFAULT}
   */
  public static double getStandardizedSigma(@Nonnull final Connection connection) throws SQLException {
    return getDoubleGlobalParameter(connection, STANDARDIZED_SIGMA);
  }

  public static final String ALL_TEAMS_MS_PER_ROW = "AllTeamsMsPerRow";

  public static final int ALL_TEAMS_MS_PER_ROW_DEFAULT = 1000;

  /**
   * Some control over the scroll rate of the all teams part of the score board.
   * The value is nominally the number of milliseconds to display each row of the
   * display for.
   * 
   * @param connection the database connection
   * @return the nominal scroll rate
   * @throws SQLException if there is a problem talking to the database
   */
  public static int getAllTeamsMsPerRow(@Nonnull final Connection connection) throws SQLException {
    if (!globalParameterExists(connection, ALL_TEAMS_MS_PER_ROW)) {
      return ALL_TEAMS_MS_PER_ROW_DEFAULT;
    } else {
      return getIntGlobalParameter(connection, ALL_TEAMS_MS_PER_ROW);
    }
  }

  /**
   * See {@link #getAllTeamsMsPerRow(Connection)}.
   * 
   * @param connection the database connection
   * @param value the new value
   * @throws SQLException if there is a problem talking to the database
   */
  public static void setAllTeamsMsPerRow(@Nonnull final Connection connection,
                                         final int value)
      throws SQLException {
    setIntGlobalParameter(connection, ALL_TEAMS_MS_PER_ROW, value);
  }

  public static final String HEAD_TO_HEAD_MS_PER_ROW = "HeadToHeadMsPerRow";

  public static final int HEAD_TO_HEAD_MS_PER_ROW_DEFAULT = 1000;

  /**
   * Some control over the scroll rate of the head to head brackets.
   * The value is nominally the number of milliseconds to display each row of the
   * display for.
   * 
   * @param connection the database connection
   * @return the nominal scroll rate
   * @throws SQLException if there is a problem talking to the database
   */
  public static int getHeadToHeadMsPerRow(@Nonnull final Connection connection) throws SQLException {
    if (!globalParameterExists(connection, HEAD_TO_HEAD_MS_PER_ROW)) {
      return HEAD_TO_HEAD_MS_PER_ROW_DEFAULT;
    } else {
      return getIntGlobalParameter(connection, HEAD_TO_HEAD_MS_PER_ROW);
    }
  }

  /**
   * See {@link #getHeadToHeadMsPerRow(Connection)}.
   * 
   * @param connection the database connection
   * @param value the new value
   * @throws SQLException if there is a problem talking to the database
   */
  public static void setHeadToHeadMsPerRow(@Nonnull final Connection connection,
                                           final int value)
      throws SQLException {
    setIntGlobalParameter(connection, HEAD_TO_HEAD_MS_PER_ROW, value);
  }

}
