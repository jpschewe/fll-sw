/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.util.FLLInternalException;
import fll.util.LogUtils;

/**
 * Constants for the tournament parameters in the database.
 */
public final class TournamentParameters {
  private TournamentParameters() {
    // no instances
  }

  public static final String SEEDING_ROUNDS = "SeedingRounds";

  public static final int SEEDING_ROUNDS_DEFAULT = 3;

  public static final String MAX_SCOREBOARD_ROUND = "MaxScoreboardRound";

  public static final int MAX_SCOREBOARD_ROUND_DEFAULT = SEEDING_ROUNDS_DEFAULT;

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Get the default value of a tournament parameter
   * 
   * @param connection
   * @param paramName
   * @throws SQLException
   */
  public static double getDoubleTournamentParameterDefault(final Connection connection,
                                                           final String paramName) throws SQLException {
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      prep = TournamentParameters.getTournamentParameterStmt(connection, GenerateDB.INTERNAL_TOURNAMENT_ID, paramName);
      rs = prep.executeQuery();
      if (rs.next()) {
        final double value = rs.getDouble(1);
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("getIntTournamentParameterDefault"
              + " param: " + paramName + " value: " + value);
        }
        return value;
      } else {
        throw new FLLInternalException("There is no default value for tournament parameter: "
            + paramName);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Get the value of a tournament parameter
   * 
   * @param connection
   * @param paramName
   * @throws SQLException
   */
  public static int getIntTournamentParameter(final Connection connection,
                                              final int tournament,
                                              final String paramName) throws SQLException {
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      prep = TournamentParameters.getTournamentParameterStmt(connection, tournament, paramName);
      rs = prep.executeQuery();
      if (rs.next()) {
        final int value = rs.getInt(1);
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("getIntTournamentParameter tournament: "
              + tournament + " param: " + paramName + " value: " + value);
        }
        return value;
      } else {
        throw new FLLInternalException("There is no default value for tournament parameter: "
            + paramName);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Get the default value of a tournament parameter
   * 
   * @param connection
   * @param paramName
   * @throws SQLException
   */
  public static int getIntTournamentParameterDefault(final Connection connection,
                                                     final String paramName) throws SQLException {
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      prep = TournamentParameters.getTournamentParameterStmt(connection, GenerateDB.INTERNAL_TOURNAMENT_ID, paramName);
      rs = prep.executeQuery();
      if (rs.next()) {
        final int value = rs.getInt(1);
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("getIntTournamentParameterDefault"
              + " param: " + paramName + " value: " + value);
        }
        return value;
      } else {
        throw new FLLInternalException("There is no default value for tournament parameter: "
            + paramName);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Check if a value exists for a parameter for a tournament.
   * 
   * @param connection the database connection
   * @param tournament the tournament to check
   * @param paramName the parameter to check for
   * @return true if there is a tournament specific value set
   * @throws SQLException
   */
  public static boolean tournamentParameterValueExists(final Connection connection,
                                                       final int tournament,
                                                       final String paramName) throws SQLException {
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("SELECT param_value FROM tournament_parameters WHERE param = ? AND tournament = ?");
      prep.setString(1, paramName);
      prep.setInt(2, tournament);
      rs = prep.executeQuery();
      return rs.next();
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Unset a tournament parameter.
   * 
   * @param connection
   * @param tournament
   * @param paramName
   * @throws SQLException
   * @throws IllegalArgumentException if the tournament is the internal
   *           tournament
   */
  public static void unsetTournamentParameter(final Connection connection,
                                              final int tournament,
                                              final String paramName) throws SQLException {
    if (tournament == GenerateDB.INTERNAL_TOURNAMENT_ID) {
      throw new IllegalArgumentException("Cannot unset a value for the internal tournament");
    }

    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("DELETE FROM tournament_parameters WHERE tournament = ? AND param = ?");
      prep.setInt(1, tournament);
      prep.setString(2, paramName);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  public static void setDoubleDefaultParameter(final Connection connection,
                                               final String paramName,
                                               final double paramValue) throws SQLException {
    setDoubleTournamentParameter(connection, GenerateDB.INTERNAL_TOURNAMENT_ID, paramName, paramValue);
  }

  public static void setDoubleTournamentParameter(final Connection connection,
                                                  final int tournament,
                                                  final String paramName,
                                                  final double paramValue) throws SQLException {
    final boolean paramExists = tournamentParameterValueExists(connection, tournament, paramName);
    PreparedStatement prep = null;
    try {
      if (!paramExists) {
        prep = connection.prepareStatement("INSERT INTO tournament_parameters (param, param_value, tournament) VALUES (?, ?, ?)");
      } else {
        prep = connection.prepareStatement("UPDATE tournament_parameters SET param_value = ? WHERE param = ? AND tournament = ?");
      }
      prep.setString(1, paramName);
      prep.setDouble(2, paramValue);
      prep.setInt(3, tournament);

      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  public static void setIntDefaultParameter(final Connection connection,
                                            final String paramName,
                                            final int paramValue) throws SQLException {
    setIntTournamentParameter(connection, GenerateDB.INTERNAL_TOURNAMENT_ID, paramName, paramValue);
  }

  public static void setIntTournamentParameter(final Connection connection,
                                               final int tournament,
                                               final String paramName,
                                               final int paramValue) throws SQLException {
    final boolean paramExists = tournamentParameterValueExists(connection, tournament, paramName);
    PreparedStatement prep = null;
    try {
      if (!paramExists) {
        prep = connection.prepareStatement("INSERT INTO tournament_parameters (param_value, param, tournament) VALUES (?, ?, ?)");
      } else {
        prep = connection.prepareStatement("UPDATE tournament_parameters SET param_value = ? WHERE param = ? AND tournament = ?");
      }
      prep.setInt(1, paramValue);
      prep.setString(2, paramName);
      prep.setInt(3, tournament);

      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  /**
   * Get the value of a tournament parameter
   * 
   * @param connection
   * @param paramName
   * @throws SQLException
   */
  public static double getDoubleTournamentParameter(final Connection connection,
                                                    final int tournament,
                                                    final String paramName) throws SQLException {
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      prep = TournamentParameters.getTournamentParameterStmt(connection, tournament, paramName);
      rs = prep.executeQuery();
      if (rs.next()) {
        return rs.getDouble(1);
      } else {
        throw new FLLInternalException("There is no default value for tournament parameter: "
            + paramName);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * @param connection
   * @return the prepared statement for getting a tournament parameter with the
   *         values already filled in
   * @throws SQLException
   */
  static PreparedStatement getTournamentParameterStmt(final Connection connection,
                                                      final int tournament,
                                                      final String paramName) throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("SELECT param_value FROM tournament_parameters WHERE param = ? AND (tournament = ? OR tournament = ?) ORDER BY tournament DESC");
      prep.setString(1, paramName);
      prep.setInt(2, tournament);
      prep.setInt(3, GenerateDB.INTERNAL_TOURNAMENT_ID);
      return prep;
    } catch (final SQLException e) {
      SQLFunctions.close(prep);
      throw e;
    }
  }

}
