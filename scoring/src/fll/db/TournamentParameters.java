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
import fll.xml.BracketSortType;

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

  public static final String BRACKET_SORT = "BracketSort";

  public static final BracketSortType BRACKET_SORT_DEFAULT = BracketSortType.SEEDING;

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Get the default value of a tournament parameter
   * 
   * @param connection
   * @param paramName
   * @throws SQLException
   */
  private static double getDoubleTournamentParameterDefault(final Connection connection,
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
  private static int getIntTournamentParameter(final Connection connection,
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
  private static int getIntTournamentParameterDefault(final Connection connection,
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
  private static void unsetParameter(final Connection connection,
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

  private static void setDoubleParameterDefault(final Connection connection,
                                                final String paramName,
                                                final double paramValue) throws SQLException {
    setDoubleParameter(connection, GenerateDB.INTERNAL_TOURNAMENT_ID, paramName, paramValue);
  }

  private static void setDoubleParameter(final Connection connection,
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

  private static void setIntParameterDefault(final Connection connection,
                                             final String paramName,
                                             final int paramValue) throws SQLException {
    setIntParameter(connection, GenerateDB.INTERNAL_TOURNAMENT_ID, paramName, paramValue);
  }

  private static void setIntParameter(final Connection connection,
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
  private static double getDoubleTournamentParameter(final Connection connection,
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
  private static PreparedStatement getTournamentParameterStmt(final Connection connection,
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

  public static BracketSortType getBracketSort(final Connection connection,
                                               final int tournament) throws SQLException {
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      prep = TournamentParameters.getTournamentParameterStmt(connection, tournament, BRACKET_SORT);
      rs = prep.executeQuery();
      if (rs.next()) {
        final String str = rs.getString(1);
        return BracketSortType.valueOf(str);
      } else {
        throw new FLLInternalException("There is no default value for tournament parameter: "
            + BRACKET_SORT);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  public static void setDefaultBracketSort(final Connection connection,
                                           final BracketSortType paramValue) throws SQLException {
    setBracketSort(connection, GenerateDB.INTERNAL_TOURNAMENT_ID, paramValue);
  }

  public static void setBracketSort(final Connection connection,
                                    final int tournament,
                                    final BracketSortType paramValue) throws SQLException {
    final boolean paramExists = tournamentParameterValueExists(connection, tournament, BRACKET_SORT);
    PreparedStatement prep = null;
    try {
      if (!paramExists) {
        prep = connection.prepareStatement("INSERT INTO tournament_parameters (param, param_value, tournament) VALUES (?, ?, ?)");
      } else {
        prep = connection.prepareStatement("UPDATE tournament_parameters SET param_value = ? WHERE param = ? AND tournament = ?");
      }
      prep.setString(1, BRACKET_SORT);
      prep.setString(2, paramValue.name());
      prep.setInt(3, tournament);

      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  public static void unsetBracketSort(final Connection connection,
                                      final int tournament) throws SQLException {
    unsetParameter(connection, tournament, BRACKET_SORT);
  }

  /**
   * Get the number of seeding rounds from the database.
   * 
   * @return the number of seeding rounds
   * @throws SQLException on a database error
   */
  public static int getNumSeedingRounds(final Connection connection,
                                        final int tournament) throws SQLException {
    return getIntTournamentParameter(connection, tournament, SEEDING_ROUNDS);
  }

  /**
   * Get default numbers of seeding rounds for the database.
   */
  public static int getDefaultNumSeedRounds(final Connection connection) throws SQLException {
    return getIntTournamentParameterDefault(connection, SEEDING_ROUNDS);
  }

  /**
   * Set the number of seeding rounds.
   * 
   * @param connection the connection
   * @param newSeedingRounds the new value of seeding rounds
   */
  public static void setNumSeedingRounds(final Connection connection,
                                         final int tournament,
                                         final int newSeedingRounds) throws SQLException {
    setIntParameter(connection, tournament, SEEDING_ROUNDS, newSeedingRounds);
  }

  public static void unsetNumSeedingRounds(final Connection connection,
                                           final int tournament) throws SQLException {
    unsetParameter(connection, tournament, SEEDING_ROUNDS);
  }

  public static void setDefaultNumSeedingRounds(final Connection connection,
                                                final int newSeedingRounds) throws SQLException {
    setIntParameterDefault(connection, SEEDING_ROUNDS, newSeedingRounds);
  }

  /**
   * Get the maximum performance round number to display on the scoreboard
   * pages.
   */
  public static int getMaxScoreboardPerformanceRound(final Connection connection,
                                                     final int tournament) throws SQLException {
    return getIntTournamentParameter(connection, tournament, MAX_SCOREBOARD_ROUND);
  }

  /**
   * @see #getMaxScoreboardPerformanceRound(Connection, int)
   */
  public static void setMaxScoreboardPerformanceRound(final Connection connection,
                                                      final int tournament,
                                                      final int value) throws SQLException {
    setIntParameter(connection, tournament, MAX_SCOREBOARD_ROUND, value);
  }

  public static void unsetMaxScoreboardPerformanceRound(final Connection connection,
                                                        final int tournament) throws SQLException {
    unsetParameter(connection, tournament, MAX_SCOREBOARD_ROUND);
  }

  public static int getDefaultMaxScoreboardPerformanceRound(final Connection connection) throws SQLException {
    return getIntTournamentParameterDefault(connection, MAX_SCOREBOARD_ROUND);
  }

  public static void setDefaultMaxScoreboardPerformanceRound(final Connection connection,
                                                             final int value) throws SQLException {
    setIntParameterDefault(connection, MAX_SCOREBOARD_ROUND, value);
  }

}
