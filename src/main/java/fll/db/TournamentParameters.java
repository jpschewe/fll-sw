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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.util.FLLInternalException;

/**
 * Constants for the tournament parameters in the database.
 */
public final class TournamentParameters {
  private TournamentParameters() {
    // no instances
  }

  /**
   * Parameter name for {@link #getNumSeedingRounds(Connection, int)}.
   */
  public static final String SEEDING_ROUNDS = "SeedingRounds";

  /**
   * Default value for {@link #SEEDING_ROUNDS}.
   */
  public static final int SEEDING_ROUNDS_DEFAULT = 3;

  /**
   * Parameter name for {@link #getNumPracticeRounds(Connection, int)}.
   */
  public static final String PRACTICE_ROUNDS = "PracticeRounds";

  /**
   * Default value for {@link #PRACTICE_ROUNDS}.
   */
  public static final int PRACTICE_ROUNDS_DEFAULT = 1;

  /**
   * Parameter name for
   * {@link #getPerformanceAdvancementPercentage(Connection, int)}.
   */
  public static final String PERFORMANCE_ADVANCEMENT_PERCENTAGE = "PerformanceAdvancementPercentage";

  /**
   * Default value for {@link #PERFORMANCE_ADVANCEMENT_PERCENTAGE}.
   */
  public static final int PERFORMANCE_ADVANCEMENT_PERCENTAGE_DEFAULT = 0;

  /**
   * Parameter name for {@link #getRunningHeadToHead(Connection, int)}.
   */
  public static final String RUNNING_HEAD_2_HEAD = "RunningHeadToHead";

  /**
   * Default value for {@link #RUNNING_HEAD_2_HEAD}.
   */
  public static final boolean RUNNING_HEAD_2_HEAD_DEFAULT = false;

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Get the value of a tournament parameter
   */
  private static int getIntTournamentParameter(final Connection connection,
                                               final int tournament,
                                               final String paramName)
      throws SQLException {
    try (PreparedStatement prep = TournamentParameters.getTournamentParameterStmt(connection, tournament, paramName);
        ResultSet rs = prep.executeQuery()) {
      if (rs.next()) {
        final int value = rs.getInt(1);
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("getIntTournamentParameter tournament: "
              + tournament
              + " param: "
              + paramName
              + " value: "
              + value);
        }
        return value;
      } else {
        throw new FLLInternalException("There is no default value for tournament parameter: "
            + paramName);
      }
    }
  }

  /**
   * Get the value of a tournament parameter
   */
  private static boolean getBooleanTournamentParameter(final Connection connection,
                                                       final int tournament,
                                                       final String paramName)
      throws SQLException {
    try (PreparedStatement prep = TournamentParameters.getTournamentParameterStmt(connection, tournament, paramName);
        ResultSet rs = prep.executeQuery()) {
      if (rs.next()) {
        final boolean value = rs.getBoolean(1);
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("getBooleanTournamentParameter tournament: "
              + tournament
              + " param: "
              + paramName
              + " value: "
              + value);
        }
        return value;
      } else {
        throw new FLLInternalException("There is no default value for tournament parameter: "
            + paramName);
      }
    }
  }

  /**
   * Get the default value of a tournament parameter
   */
  private static int getIntTournamentParameterDefault(final Connection connection,
                                                      final String paramName)
      throws SQLException {
    try (
        PreparedStatement prep = TournamentParameters.getTournamentParameterStmt(connection,
                                                                                 GenerateDB.INTERNAL_TOURNAMENT_ID,
                                                                                 paramName);
        ResultSet rs = prep.executeQuery()) {
      if (rs.next()) {
        final int value = rs.getInt(1);
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("getIntTournamentParameterDefault"
              + " param: "
              + paramName
              + " value: "
              + value);
        }
        return value;
      } else {
        throw new FLLInternalException("There is no default value for tournament parameter: "
            + paramName);
      }
    }
  }

  /**
   * Get the default value of a tournament parameter
   */
  private static boolean getBooleanTournamentParameterDefault(final Connection connection,
                                                              final String paramName)
      throws SQLException {
    try (
        PreparedStatement prep = TournamentParameters.getTournamentParameterStmt(connection,
                                                                                 GenerateDB.INTERNAL_TOURNAMENT_ID,
                                                                                 paramName);
        ResultSet rs = prep.executeQuery()) {
      if (rs.next()) {
        final boolean value = rs.getBoolean(1);
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("getBooleanTournamentParameterDefault"
              + " param: "
              + paramName
              + " value: "
              + value);
        }
        return value;
      } else {
        throw new FLLInternalException("There is no default value for tournament parameter: "
            + paramName);
      }
    }
  }

  /**
   * Check if a value exists for a parameter for a tournament.
   *
   * @param connection the database connection
   * @param tournament the tournament to check
   * @param paramName the parameter to check for
   * @return true if there is a tournament specific value set
   * @throws SQLException on a database error
   */
  public static boolean tournamentParameterValueExists(final Connection connection,
                                                       final int tournament,
                                                       final String paramName)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT param_value FROM tournament_parameters WHERE param = ? AND tournament = ?")) {
      prep.setString(1, paramName);
      prep.setInt(2, tournament);
      try (ResultSet rs = prep.executeQuery()) {
        return rs.next();
      }
    }
  }

  /**
   * Unset a tournament parameter.
   *
   * @throws IllegalArgumentException if the tournament is the internal
   *           tournament
   */
  private static void unsetParameter(final Connection connection,
                                     final int tournament,
                                     final String paramName)
      throws SQLException {
    if (tournament == GenerateDB.INTERNAL_TOURNAMENT_ID) {
      throw new IllegalArgumentException("Cannot unset a value for the internal tournament");
    }

    try (
        PreparedStatement prep = connection.prepareStatement("DELETE FROM tournament_parameters WHERE tournament = ? AND param = ?")) {
      prep.setInt(1, tournament);
      prep.setString(2, paramName);
      prep.executeUpdate();
    }
  }

  private static void setIntParameterDefault(final Connection connection,
                                             final String paramName,
                                             final int paramValue)
      throws SQLException {
    setIntParameter(connection, GenerateDB.INTERNAL_TOURNAMENT_ID, paramName, paramValue);
  }

  private static void setBooleanParameterDefault(final Connection connection,
                                                 final String paramName,
                                                 final boolean paramValue)
      throws SQLException {
    setBooleanParameter(connection, GenerateDB.INTERNAL_TOURNAMENT_ID, paramName, paramValue);
  }

  private static void setIntParameter(final Connection connection,
                                      final int tournament,
                                      final String paramName,
                                      final int paramValue)
      throws SQLException {
    final boolean paramExists = tournamentParameterValueExists(connection, tournament, paramName);
    if (!paramExists) {
      insertIntParameter(connection, tournament, paramName, paramValue);
    } else {
      updateIntParameter(connection, tournament, paramName, paramValue);
    }
  }

  private static void insertIntParameter(final Connection connection,
                                         final int tournament,
                                         final String paramName,
                                         final int paramValue)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("INSERT INTO tournament_parameters (param_value, param, tournament) VALUES (?, ?, ?)")) {
      prep.setInt(1, paramValue);
      prep.setString(2, paramName);
      prep.setInt(3, tournament);

      prep.executeUpdate();
    }
  }

  private static void updateIntParameter(final Connection connection,
                                         final int tournament,
                                         final String paramName,
                                         final int paramValue)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("UPDATE tournament_parameters SET param_value = ? WHERE param = ? AND tournament = ?")) {
      prep.setInt(1, paramValue);
      prep.setString(2, paramName);
      prep.setInt(3, tournament);

      prep.executeUpdate();
    }
  }

  private static void setBooleanParameter(final Connection connection,
                                          final int tournament,
                                          final String paramName,
                                          final boolean paramValue)
      throws SQLException {
    final boolean paramExists = tournamentParameterValueExists(connection, tournament, paramName);
    if (!paramExists) {
      insertBooleanParameter(connection, tournament, paramName, paramValue);
    } else {
      updateBooleanParameter(connection, tournament, paramName, paramValue);
    }
  }

  private static void insertBooleanParameter(final Connection connection,
                                             final int tournament,
                                             final String paramName,
                                             final boolean paramValue)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("INSERT INTO tournament_parameters (param_value, param, tournament) VALUES (?, ?, ?)")) {
      prep.setBoolean(1, paramValue);
      prep.setString(2, paramName);
      prep.setInt(3, tournament);

      prep.executeUpdate();
    }
  }

  private static void updateBooleanParameter(final Connection connection,
                                             final int tournament,
                                             final String paramName,
                                             final boolean paramValue)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("UPDATE tournament_parameters SET param_value = ? WHERE param = ? AND tournament = ?")) {
      prep.setBoolean(1, paramValue);
      prep.setString(2, paramName);
      prep.setInt(3, tournament);

      prep.executeUpdate();
    }
  }

  /**
   * @param connection database connection
   * @return the prepared statement for getting a tournament parameter with the
   *         values already filled in, the caller must close this object on all
   *         execution paths
   * @throws SQLException on a database error
   */
  @SuppressFBWarnings(value = "OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE", justification = "All callers clean up the PreparedStatement")
  private static PreparedStatement getTournamentParameterStmt(final Connection connection,
                                                              final int tournament,
                                                              final String paramName)
      throws SQLException {
    final PreparedStatement prep = connection.prepareStatement("SELECT param_value FROM tournament_parameters WHERE param = ? AND (tournament = ? OR tournament = ?) ORDER BY tournament DESC");
    prep.setString(1, paramName);
    prep.setInt(2, tournament);
    prep.setInt(3, GenerateDB.INTERNAL_TOURNAMENT_ID);
    return prep;
  }

  /**
   * @param connection database connection
   * @param tournament the tournament ID
   * @return true if there is a tournament specific value
   * @throws SQLException on a database error
   */
  public static boolean isNumSeedingRoundsSet(final Connection connection,
                                              final int tournament)
      throws SQLException {
    return tournamentParameterValueExists(connection, tournament, SEEDING_ROUNDS);
  }

  /**
   * @param connection database connection
   * @param tournament the tournament ID
   * @return the number of seeding rounds
   * @throws SQLException on a database error
   */
  public static int getNumSeedingRounds(final Connection connection,
                                        final int tournament)
      throws SQLException {
    return getIntTournamentParameter(connection, tournament, SEEDING_ROUNDS);
  }

  /**
   * Get default numbers of seeding rounds for the database.
   * 
   * @param connection database connection
   * @return the default number of seeding rounds
   * @throws SQLException on a database error
   */
  public static int getDefaultNumSeedingRounds(final Connection connection) throws SQLException {
    return getIntTournamentParameterDefault(connection, SEEDING_ROUNDS);
  }

  /**
   * Set the number of seeding rounds.
   *
   * @param tournament the tournament ID
   * @param connection the connection
   * @param value the new value
   * @throws SQLException on a database error
   */
  public static void setNumSeedingRounds(final Connection connection,
                                         final int tournament,
                                         final int value)
      throws SQLException {
    setIntParameter(connection, tournament, SEEDING_ROUNDS, value);
  }

  /**
   * @param connection database connection
   * @param tournament the tournament ID
   * @throws SQLException on a database error
   * @see #setNumSeedingRounds(Connection, int, int)
   */
  public static void unsetNumSeedingRounds(final Connection connection,
                                           final int tournament)
      throws SQLException {
    unsetParameter(connection, tournament, SEEDING_ROUNDS);
  }

  /**
   * Set the default value for the number of seeding rounds.
   * 
   * @param connection database connection
   * @param newSeedingRounds the new value
   * @throws SQLException on a database error
   */
  public static void setDefaultNumSeedingRounds(final Connection connection,
                                                final int newSeedingRounds)
      throws SQLException {
    setIntParameterDefault(connection, SEEDING_ROUNDS, newSeedingRounds);
  }

  /**
   * @param connection database connection
   * @param tournament the tournament ID
   * @return true if there is a tournament specific value
   * @throws SQLException on a database error
   */
  public static boolean isNumPracticeRoundsSet(final Connection connection,
                                               final int tournament)
      throws SQLException {
    return tournamentParameterValueExists(connection, tournament, PRACTICE_ROUNDS);
  }

  /**
   * Get the number of practice rounds from the database.
   *
   * @param connection database connection
   * @param tournament the tournament ID
   * @return the number of practice rounds
   * @throws SQLException on a database error
   */
  public static int getNumPracticeRounds(final Connection connection,
                                         final int tournament)
      throws SQLException {
    return getIntTournamentParameter(connection, tournament, PRACTICE_ROUNDS);
  }

  /**
   * Get default numbers of practice rounds for the database.
   * 
   * @param connection database connection
   * @return the default number of practice rounds
   * @throws SQLException on a database error
   */
  public static int getDefaultNumPracticeRounds(final Connection connection) throws SQLException {
    return getIntTournamentParameterDefault(connection, PRACTICE_ROUNDS);
  }

  /**
   * Set the number of practice rounds.
   *
   * @param tournament the tournament ID
   * @param connection the connection
   * @param value the new value of practice rounds
   * @throws SQLException on a database error
   */
  public static void setNumPracticeRounds(final Connection connection,
                                          final int tournament,
                                          final int value)
      throws SQLException {
    setIntParameter(connection, tournament, PRACTICE_ROUNDS, value);
  }

  /**
   * @param connection database connection
   * @param tournament the tournament ID
   * @throws SQLException on a database error
   * @see #setNumPracticeRounds(Connection, int, int)
   */
  public static void unsetNumPracticeRounds(final Connection connection,
                                            final int tournament)
      throws SQLException {
    unsetParameter(connection, tournament, PRACTICE_ROUNDS);
  }

  /**
   * Set the default value for the number of practice rounds.
   * 
   * @param connection database connection
   * @param newSeedingRounds the new value
   * @throws SQLException on a database error
   */
  public static void setDefaultNumPracticeRounds(final Connection connection,
                                                 final int newSeedingRounds)
      throws SQLException {
    setIntParameterDefault(connection, PRACTICE_ROUNDS, newSeedingRounds);
  }

  /**
   * Check if a default value for the specified parameter exists.
   * This is to be used when upgrading databases.
   * 
   * @param connection the database connection
   * @param paramName the name of the parameter to check
   * @return true if a default value exists for the parameter
   * @throws SQLException on a database error
   */
  public static boolean defaultParameterExists(final Connection connection,
                                               final String paramName)
      throws SQLException {
    try (
        PreparedStatement prep = TournamentParameters.getTournamentParameterStmt(connection,
                                                                                 GenerateDB.INTERNAL_TOURNAMENT_ID,
                                                                                 paramName);
        ResultSet rs = prep.executeQuery()) {
      return rs.next();
    }
  }

  /**
   * Get the performance advancement threshold percentage.
   * To advance to the next level of competition teams
   * should be in the top X% of performance score.
   *
   * @param connection database connection
   * @param tournament tournament id
   * @return integer percentage, 40 means 40%
   * @throws SQLException on a database error
   */
  public static int getPerformanceAdvancementPercentage(final Connection connection,
                                                        final int tournament)
      throws SQLException {
    return getIntTournamentParameter(connection, tournament, PERFORMANCE_ADVANCEMENT_PERCENTAGE);
  }

  /**
   * @see #getPerformanceAdvancementPercentage(Connection, int)
   * @param connection database connection
   * @return the default value for
   *         {@link #getPerformanceAdvancementPercentage(Connection, int)}
   * @throws SQLException on a database error
   */
  public static int getDefaultPerformanceAdvancementPercentage(final Connection connection) throws SQLException {
    return getIntTournamentParameterDefault(connection, PERFORMANCE_ADVANCEMENT_PERCENTAGE);
  }

  /**
   * @param connection the connection
   * @param tournament the tournament id
   * @param newValue the new value
   * @throws SQLException on a database error
   * @see #getPerformanceAdvancementPercentage(Connection, int)
   */
  public static void setPerformanceAdvancementPercentage(final Connection connection,
                                                         final int tournament,
                                                         final int newValue)
      throws SQLException {
    setIntParameter(connection, tournament, PERFORMANCE_ADVANCEMENT_PERCENTAGE, newValue);
  }

  /**
   * @param connection database connection
   * @param tournament the tournament ID
   * @see #getPerformanceAdvancementPercentage(Connection, int)
   * @throws SQLException on a database error
   */
  public static void unsetPerformanceAdvancementPercentage(final Connection connection,
                                                           final int tournament)
      throws SQLException {
    unsetParameter(connection, tournament, PERFORMANCE_ADVANCEMENT_PERCENTAGE);
  }

  /**
   * @param connection the database connection
   * @param newValue the value to set
   * @throws SQLException on a database error
   * @see #getPerformanceAdvancementPercentage(Connection, int)
   */
  public static void setDefaultPerformanceAdvancementPercentage(final Connection connection,
                                                                final int newValue)
      throws SQLException {
    setIntParameterDefault(connection, PERFORMANCE_ADVANCEMENT_PERCENTAGE, newValue);
  }

  /**
   * @param connection the database connection
   * @param tournament the tournament
   * @return if head to head is being used
   * @throws SQLException on a database error
   */
  public static boolean getRunningHeadToHead(final Connection connection,
                                             final int tournament)
      throws SQLException {
    return getBooleanTournamentParameter(connection, tournament, RUNNING_HEAD_2_HEAD);
  }

  /**
   * @param connection database connection
   * @param tournament the tournament ID
   * @param value the new value
   * @throws SQLException on a database error
   * @see #getRunningHeadToHead(Connection, int)
   */
  public static void setRunningHeadToHead(final Connection connection,
                                          final int tournament,
                                          final boolean value)
      throws SQLException {
    setBooleanParameter(connection, tournament, RUNNING_HEAD_2_HEAD, value);
  }

  /**
   * Use the default value instead of the tournament-specific value.
   * 
   * @param connection the database connection
   * @param tournament the tournament ID
   * @throws SQLException on a database error
   * @see #getRunningHeadToHead(Connection, int)
   */
  public static void unsetRunningHeadToHead(final Connection connection,
                                            final int tournament)
      throws SQLException {
    unsetParameter(connection, tournament, RUNNING_HEAD_2_HEAD);
  }

  /**
   * @param connection the database connection
   * @return the default value
   * @throws SQLException on a database error
   * @see #getRunningHeadToHead(Connection, int)
   */
  public static boolean getDefaultRuningHeadToHead(final Connection connection) throws SQLException {
    return getBooleanTournamentParameterDefault(connection, RUNNING_HEAD_2_HEAD);
  }

  /**
   * Set the default value.
   * 
   * @param connection the database connection
   * @param value the new default value
   * @throws SQLException on a database error
   * @see #getRunningHeadToHead(Connection, int)
   */
  public static void setDefaultRunningHeadToHead(final Connection connection,
                                                 final boolean value)
      throws SQLException {
    setBooleanParameterDefault(connection, RUNNING_HEAD_2_HEAD, value);
  }

}
