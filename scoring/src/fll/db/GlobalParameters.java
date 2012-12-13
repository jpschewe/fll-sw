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

  public static final String SCORESHEET_LAYOUT_NUP = "ScoresheetLayoutNUp";

  public static final int SCORESHEET_LAYOUT_NUP_DEFAULT = 2;

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
                                                          final String paramName) throws SQLException {
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
                                                final String parameter) throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = getGlobalParameterStmt(connection, parameter);
      rs = prep.executeQuery();
      if (rs.next()) {
        return rs.getDouble(1);
      } else {
        throw new IllegalArgumentException("Can't find '"
            + parameter + "' in global_parameters");
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
                                          final String parameter) throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = getGlobalParameterStmt(connection, parameter);
      rs = prep.executeQuery();
      if (rs.next()) {
        return rs.getInt(1);
      } else {
        throw new IllegalArgumentException("Can't find '"
            + parameter + "' in global_parameters");
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Check if a value exists for a global parameter.
   */
  public static boolean globalParameterExists(final Connection connection,
                                              final String paramName) throws SQLException {
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
                                              final String paramValue) throws SQLException {
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
                                              final double paramValue) throws SQLException {
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
                                           final int paramValue) throws SQLException {
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

}
