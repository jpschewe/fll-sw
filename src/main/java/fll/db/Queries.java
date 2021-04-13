/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Types;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Team;
import fll.Tournament;
import fll.TournamentTeam;
import fll.Utilities;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.playoff.BracketUpdate;
import fll.web.playoff.DatabaseTeamScore;
import fll.web.playoff.H2HUpdateWebSocket;
import fll.web.playoff.HttpTeamScore;
import fll.web.playoff.Playoff;
import fll.web.playoff.TeamScore;
import fll.web.scoreEntry.UnverifiedRunsWebSocket;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.PerformanceScoreCategory;
import fll.xml.ScoreType;
import fll.xml.SubjectiveScoreCategory;
import fll.xml.TiebreakerTest;
import fll.xml.WinnerType;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Does all of our queries.
 */
public final class Queries {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private Queries() {
    // no instances
  }

  /**
   * Get a map of teams for this tournament keyed on team number. Uses the table
   * TournamentTeams to determine which teams should be included.
   * 
   * @param connection database connection
   * @return team number, team
   * @throws SQLException on a database error
   */
  public static Map<Integer, TournamentTeam> getTournamentTeams(final Connection connection) throws SQLException {
    return getTournamentTeams(connection, getCurrentTournament(connection));
  }

  /**
   * Get a map of teams for the specified tournament keyed on team number. Uses
   * the table TournamentTeams to determine which teams should be included.
   * 
   * @param connection database connection
   * @param tournamentID the tournament to get the teams for
   * @return team number, team
   * @throws SQLException on a database error
   */
  public static Map<Integer, TournamentTeam> getTournamentTeams(final Connection connection,
                                                                final int tournamentID)
      throws SQLException {
    final SortedMap<Integer, TournamentTeam> tournamentTeams = new TreeMap<>();
    try (PreparedStatement prep = connection.prepareStatement("SELECT Teams.TeamNumber, Teams.Organization"//
        + ", Teams.TeamName"//
        + ", TournamentTeams.event_division" //
        + ", TournamentTeams.judging_station" //
        + " FROM Teams, TournamentTeams" //
        + " WHERE Teams.TeamNumber = TournamentTeams.TeamNumber"//
        + " AND TournamentTeams.Tournament = ?")) {
      prep.setInt(1, tournamentID);
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final int teamNumber = rs.getInt("TeamNumber");
          final String org = rs.getString("Organization");
          final String name = castNonNull(rs.getString("TeamName"));
          final String eventDivision = castNonNull(rs.getString("event_division"));
          final String judgingStation = castNonNull(rs.getString("judging_station"));

          final TournamentTeam team = new TournamentTeam(teamNumber, org, name, eventDivision, judgingStation);
          tournamentTeams.put(teamNumber, team);
        }
      }
    }
    return tournamentTeams;
  }

  /**
   * @param connection database connection
   * @return see {@link #getAwardGroups(Connection, int)}
   * @see #getAwardGroups(Connection, int)
   * @see #getCurrentTournament(Connection)
   * @throws SQLException on a database error
   */
  public static List<String> getAwardGroups(final Connection connection) throws SQLException {
    final int currentTournament = getCurrentTournament(connection);
    return getAwardGroups(connection, currentTournament);
  }

  /**
   * Get the list of event divisions at the specified tournament as a List of
   * Strings.
   *
   * @param connection the database connection
   * @param tournament the tournament to get the award groups for
   * @return the List of award groups as strings. Sorted by name.
   * @throws SQLException on a database error
   * @see #getCurrentTournament(Connection)
   */
  public static List<String> getAwardGroups(final Connection connection,
                                            final int tournament)
      throws SQLException {
    final List<String> list = new LinkedList<>();

    try (
        PreparedStatement prep = connection.prepareStatement("SELECT DISTINCT event_division FROM TournamentTeams WHERE Tournament = ? ORDER BY event_division")) {
      prep.setInt(1, tournament);
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String division = castNonNull(rs.getString(1));
          list.add(division);
        }
      }
    }
    return list;
  }

  /**
   * Get the list of team numbers that are in the specified event division.
   *
   * @param connection database connection
   * @param tournament the tournament to work with
   * @param division the award group to get the teams for
   * @return team numbers
   * @throws SQLException on a database error
   */
  public static Set<Integer> getTeamNumbersInEventDivision(final Connection connection,
                                                           final int tournament,
                                                           final String division)
      throws SQLException {
    final Set<Integer> teamNumbers = new HashSet<>();
    try (PreparedStatement prep = connection.prepareStatement("SELECT TeamNumber FROM TournamentTeams" //
        + " WHERE Tournament = ?" //
        + " AND event_division = ?")) {
      prep.setInt(1, tournament);
      prep.setString(2, division);
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final int teamNumber = rs.getInt(1);
          teamNumbers.add(teamNumber);
        }
      }
    }

    return teamNumbers;
  }

  /**
   * Get the list of judging stations for the specified tournament as a List of
   * Strings.
   *
   * @param connection database connection
   * @param tournament the tournament to get the stations for
   * @return the judging stations
   * @throws SQLException on a database error
   */
  public static List<String> getJudgingStations(final Connection connection,
                                                final int tournament)
      throws SQLException {
    final List<String> result = new LinkedList<>();

    try (
        PreparedStatement prep = connection.prepareStatement("SELECT DISTINCT judging_station FROM TournamentTeams WHERE tournament = ? ORDER BY judging_station")) {
      prep.setInt(1, tournament);
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String station = castNonNull(rs.getString(1));
          result.add(station);
        }
      }
    }
    return result;
  }

  /**
   * Figure out the next run number for teamNumber. Does not ignore unverified
   * scores.
   * 
   * @param connection database connection
   * @param teamNumber the team to get the next run for
   * @return the next run number
   * @throws SQLException on a database error
   */
  public static int getNextRunNumber(final Connection connection,
                                     final int teamNumber)
      throws SQLException {
    final int currentTournament = getCurrentTournament(connection);
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT COUNT(TeamNumber) FROM Performance WHERE Tournament = ?"
            + " AND TeamNumber = ?")) {
      prep.setInt(1, currentTournament);
      prep.setInt(2, teamNumber);
      try (ResultSet rs = prep.executeQuery()) {
        final int runNumber;
        if (rs.next()) {
          runNumber = rs.getInt(1);
        } else {
          runNumber = 0;
        }
        return runNumber
            + 1;
      }
    }
  }

  /**
   * Figure out the highest run number a team has completed. This should be the
   * same as next run number -1, but sometimes we get non-consecutive runs in
   * and this just finds the max run number. Does not ignore unverified scores.
   * 
   * @param connection database connection
   * @param teamNumber team number
   * @return the highest run number that the team has completed
   * @throws SQLException on a database error
   */
  public static int getMaxRunNumber(final Connection connection,
                                    final int teamNumber)
      throws SQLException {
    final int currentTournament = getCurrentTournament(connection);
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT MAX(RunNumber) FROM Performance WHERE Tournament = ?"
            + " AND TeamNumber = ?")) {
      prep.setInt(1, currentTournament);
      prep.setInt(2, teamNumber);
      try (ResultSet rs = prep.executeQuery()) {
        final int runNumber;
        if (rs.next()) {
          runNumber = rs.getInt(1);
        } else {
          runNumber = 0;
        }
        return runNumber;
      }
    }
  }

  /**
   * Insert or update a performance score.
   *
   * @param description challenge description
   * @param connection database connection
   * @param request HTTP request with all of the data
   * @throws SQLException on a database error.
   * @throws RuntimeException if a parameter is missing.
   * @throws ParseException if the team number cannot be parsed
   */
  public static void insertOrUpdatePerformanceScore(final ChallengeDescription description,
                                                    final Connection connection,
                                                    final HttpServletRequest request)
      throws SQLException, ParseException, RuntimeException {
    final int oldTransactionIsolation = connection.getTransactionIsolation();
    final boolean oldAutoCommit = connection.getAutoCommit();
    try {
      // make sure that we don't get into a race with another thread
      connection.setAutoCommit(false);
      connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

      final int rowsUpdated = updatePerformanceScore(description, connection, request);
      if (rowsUpdated < 1) {
        insertPerformanceScore(description, connection, request);
      }
      connection.commit();
    } finally {
      connection.setTransactionIsolation(oldTransactionIsolation);
      connection.setAutoCommit(oldAutoCommit);
    }

    // notify that there may be more runs to verify
    UnverifiedRunsWebSocket.notifyToUpdate();
  }

  /**
   * Insert a performance score into the database. All of the values are
   * expected to be in request.
   *
   * @param description describes the challenge
   * @param connection database connection
   * @param request HTTP request with all of the data
   * @throws SQLException on a database error.
   * @throws RuntimeException if a parameter is missing.
   * @throws ParseException if the team number cannot be parsed
   */
  @SuppressFBWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE" }, justification = "Goals determine columns")
  public static void insertPerformanceScore(final ChallengeDescription description,
                                            final Connection connection,
                                            final HttpServletRequest request)
      throws SQLException, ParseException, RuntimeException {
    final int currentTournament = getCurrentTournament(connection);
    final Tournament tournament = Tournament.findTournamentByID(connection, currentTournament);

    final String teamNumberStr = request.getParameter("TeamNumber");
    if (null == teamNumberStr) {
      throw new RuntimeException("Missing parameter: TeamNumber");
    }
    final int teamNumber = Utilities.getIntegerNumberFormat().parse(teamNumberStr).intValue();

    final String runNumberStr = request.getParameter("RunNumber");
    if (null == runNumberStr) {
      throw new RuntimeException("Missing parameter: RunNumber");
    }
    final int runNumber = Utilities.getIntegerNumberFormat().parse(runNumberStr).intValue();

    final String noShow = request.getParameter("NoShow");
    if (null == noShow) {
      throw new RuntimeException("Missing parameter: NoShow");
    }

    final boolean verified = "1".equals(request.getParameter("Verified"));

    final TeamScore teamScore = new HttpTeamScore(teamNumber, runNumber, request);

    insertPerformanceScore(connection, description, tournament, verified, teamScore);
  }

  /**
   * Insert a performance score into the database and do all appropriate updates
   * to the playoff tables and notifications to the UI code.
   *
   * @param connection the database connection
   * @param description the challenge description
   * @param tournament which tournament
   * @param verified if the run is verified
   * @param teamScore the team score
   * @throws SQLException on a database error
   * @throws ParseException on an error parsing the score data
   */
  @SuppressFBWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE" }, justification = "Need to generate list of columns off the goals")
  public static void insertPerformanceScore(final Connection connection,
                                            final ChallengeDescription description,
                                            final Tournament tournament,
                                            final boolean verified,
                                            final TeamScore teamScore)
      throws SQLException, ParseException {
    final WinnerType winnerCriteria = description.getWinner();
    final PerformanceScoreCategory performanceElement = description.getPerformance();
    final List<TiebreakerTest> tiebreakerElement = performanceElement.getTiebreaker();

    final StringBuffer columns = new StringBuffer();
    final StringBuffer values = new StringBuffer();

    columns.append("TeamNumber");
    values.append(teamScore.getTeamNumber());
    columns.append(", Tournament");
    values.append(", "
        + tournament.getTournamentID());

    columns.append(", ComputedTotal");
    if (teamScore.isNoShow()) {
      values.append(", NULL");
    } else {
      values.append(", "
          + performanceElement.evaluate(teamScore));
    }

    columns.append(", RunNumber");
    values.append(", "
        + teamScore.getRunNumber());

    // TODO: this should be reworked to use ? in the prepared statement

    columns.append(", NoShow");
    values.append(", "
        + (teamScore.isNoShow() ? "1" : "0"));

    columns.append(", Verified");
    values.append(", "
        + (verified ? "1" : "0"));

    // now do each goal
    for (final AbstractGoal element : performanceElement.getAllGoals()) {
      if (!element.isComputed()) {
        final String name = element.getName();

        columns.append(", "
            + name);
        if (element.isEnumerated()) {
          // enumerated
          values.append(", '"
              + teamScore.getEnumRawScore(name)
              + "'");
        } else {
          values.append(", "
              + teamScore.getRawScore(name));
        }
      } // !computed
    } // foreach goal

    final String sql = "INSERT INTO Performance"
        + " ( "
        + columns.toString()
        + ") "
        + "VALUES ( "
        + values.toString()
        + ")";
    try (Statement stmt = connection.createStatement()) {
      stmt.executeUpdate(sql);
    }

    // Perform updates to the playoff data table if in playoff rounds.
    final int numSeedingRounds = TournamentParameters.getNumSeedingRounds(connection, tournament.getTournamentID());
    final boolean runningHeadToHead = TournamentParameters.getRunningHeadToHead(connection,
                                                                                tournament.getTournamentID());
    if (teamScore.getRunNumber() > numSeedingRounds) {
      if (runningHeadToHead) {
        if (verified) {
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Updating playoff score from insert");
          }
          updatePlayoffScore(connection, verified, tournament.getTournamentID(), winnerCriteria, performanceElement,
                             tiebreakerElement, teamScore.getTeamNumber(), teamScore.getRunNumber(), teamScore);
        } else {
          // send H2H update that this team's score is entered
          final String bracketName = Playoff.getPlayoffDivision(connection, tournament.getTournamentID(),
                                                                teamScore.getTeamNumber(), teamScore.getRunNumber());
          final Team team = Team.getTeamFromDatabase(connection, teamScore.getTeamNumber());

          H2HUpdateWebSocket.updateBracket(connection, performanceElement.getScoreType(), bracketName, team,
                                           teamScore.getRunNumber());
        }
      } // running head to head
    } else {
      tournament.recordPerformanceSeedingModified(connection);
    }

    // notify that there may be more runs to verify
    UnverifiedRunsWebSocket.notifyToUpdate();
  }

  /**
   * @param connection the database connection
   * @param tournament the tournament
   * @param division the head to head bracket
   * @return true if third place is enabled for the specified bracket
   * @throws SQLException on a database error
   */
  public static boolean isThirdPlaceEnabled(final Connection connection,
                                            final int tournament,
                                            final String division)
      throws SQLException {
    final int finalRound = getNumPlayoffRounds(connection, tournament, division);

    try (PreparedStatement prep = connection.prepareStatement("SELECT count(*) FROM PlayoffData" //
        + " WHERE Tournament= ?" //
        + " AND event_division= ?" //
        + " AND PlayoffRound= ?")) {
      prep.setInt(1, tournament);
      prep.setString(2, division);
      prep.setInt(3, finalRound);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1) == 4;
        } else {
          return false;
        }
      }
    }
  }

  /**
   * Update a performance score in the database. All of the values are expected
   * to be in request.
   *
   * @param description
   *          description of the challenge
   * @param connection database connection
   * @param request HTTP request that contains the expected values
   * @return the number of rows updated, should be 0 or 1
   * @throws SQLException on a database error.
   * @throws ParseException if the XML document is invalid.
   * @throws RuntimeException if a parameter is missing.
   */
  @SuppressFBWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE" }, justification = "Need to generate list of columns off the goals")
  public static int updatePerformanceScore(final ChallengeDescription description,
                                           final Connection connection,
                                           final HttpServletRequest request)
      throws SQLException, ParseException, RuntimeException {
    final int currentTournament = getCurrentTournament(connection);
    final Tournament tournament = Tournament.findTournamentByID(connection, currentTournament);

    final WinnerType winnerCriteria = description.getWinner();
    final PerformanceScoreCategory performanceElement = description.getPerformance();
    final List<TiebreakerTest> tiebreakerElement = performanceElement.getTiebreaker();

    final String teamNumberStr = request.getParameter("TeamNumber");
    if (null == teamNumberStr) {
      throw new FLLRuntimeException("Missing parameter: TeamNumber");
    }
    final int teamNumber = Utilities.getIntegerNumberFormat().parse(teamNumberStr).intValue();

    final String runNumberStr = request.getParameter("RunNumber");
    if (null == runNumberStr) {
      throw new FLLRuntimeException("Missing parameter: RunNumber");
    }
    final int runNumber = Utilities.getIntegerNumberFormat().parse(runNumberStr).intValue();

    final String noShow = request.getParameter("NoShow");
    if (null == noShow) {
      throw new FLLRuntimeException("Missing parameter: NoShow");
    }

    final TeamScore teamScore = new HttpTeamScore(teamNumber, runNumber, request);

    final StringBuffer sql = new StringBuffer();

    sql.append("UPDATE Performance SET ");

    sql.append("NoShow = "
        + noShow);

    sql.append(", TIMESTAMP = CURRENT_TIMESTAMP");

    if (teamScore.isNoShow()) {
      sql.append(", ComputedTotal = NULL");
    } else {
      sql.append(", ComputedTotal = "
          + performanceElement.evaluate(teamScore));
    }

    // now do each goal
    for (final AbstractGoal element : performanceElement.getAllGoals()) {
      if (!element.isComputed()) {
        final String name = element.getName();

        final String value = request.getParameter(name);
        if (null == value) {
          throw new FLLRuntimeException("Missing parameter: "
              + name);
        }
        if (element.isEnumerated()) {
          // enumerated
          sql.append(", "
              + name
              + " = '"
              + value
              + "'");
        } else {
          sql.append(", "
              + name
              + " = "
              + value);
        }
      } // !computed
    } // foreach goal

    sql.append(", Verified = "
        + request.getParameter("Verified"));

    sql.append(" WHERE TeamNumber = "
        + teamNumber);

    sql.append(" AND RunNumber = "
        + runNumberStr);
    sql.append(" AND Tournament = "
        + currentTournament);

    int numRowsUpdated = 0;
    try (Statement stmt = connection.createStatement()) {
      numRowsUpdated = stmt.executeUpdate(sql.toString());
    }

    if (numRowsUpdated > 0) {
      // Check if we need to update the PlayoffData table
      final int numSeedingRounds = TournamentParameters.getNumSeedingRounds(connection, currentTournament);
      if (runNumber > numSeedingRounds) {
        final boolean runningHeadToHead = TournamentParameters.getRunningHeadToHead(connection, currentTournament);
        if (runningHeadToHead) {
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Updating playoff score from updatePerformanceScore");
          }

          final boolean verified = "1".equals(request.getParameter("Verified"));
          updatePlayoffScore(connection, verified, currentTournament, winnerCriteria, performanceElement,
                             tiebreakerElement, teamNumber, runNumber, teamScore);
        }
      } else {
        tournament.recordPerformanceSeedingModified(connection);
      }
    }

    // notify that there may be more runs to verify
    UnverifiedRunsWebSocket.notifyToUpdate();

    return numRowsUpdated;
  }

  /**
   * Note that a performance score has changed and update the playoff table with
   * this new information.
   */
  private static void updatePlayoffScore(final Connection connection,
                                         final boolean verified,
                                         final int currentTournament,
                                         final WinnerType winnerCriteria,
                                         final PerformanceScoreCategory performanceElement,
                                         final List<TiebreakerTest> tiebreakerElement,
                                         final int teamNumber,
                                         final int runNumber,
                                         final TeamScore teamScore)
      throws SQLException, ParseException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Updating playoff score for team: "
          + teamNumber
          + " run: "
          + runNumber);
    }

    final Team team = Team.getTeamFromDatabase(connection, teamNumber);

    final int ptLine = getPlayoffTableLineNumber(connection, currentTournament, teamNumber, runNumber);
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("line: "
          + ptLine);
    }

    final String division = Playoff.getPlayoffDivision(connection, currentTournament, teamNumber, runNumber);
    if (ptLine > 0) {
      final double score = performanceElement.evaluate(teamScore);

      // this makes sure that scores get pushed through to the displays
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Sending H2HUpdate with score: "
            + score);
      }

      H2HUpdateWebSocket.updateBracket(connection, performanceElement.getScoreType(), division, team, runNumber);

      final int siblingDbLine = ptLine
          % 2 == 0 ? ptLine
              - 1
              : ptLine
                  + 1;
      final int siblingTeam = getTeamNumberByPlayoffLine(connection, currentTournament, division, siblingDbLine,
                                                         runNumber);

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Sibling is: "
            + siblingTeam
            + " division: "
            + division);
      }

      // If sibling team is the NULL team, then updating this score is okay,
      // and no playoff meta data needs updating.
      if (Team.NULL_TEAM_NUMBER != siblingTeam) {
        // Sibling team is not null so we have to check if update can happen
        // anyway

        // See if the modification affects the result of the playoff match
        final int winnerDbLine = (ptLine
            + 1)
            / 2;
        final int winnerRunNumber = runNumber
            + 1;
        final int oldWinnerTeamNumber = Queries.getTeamNumberByPlayoffLine(connection, currentTournament, division,
                                                                           winnerDbLine, winnerRunNumber);

        final Team teamB = Team.getTeamFromDatabase(connection, siblingTeam);
        if (teamB == null) {
          throw new FLLRuntimeException("Unable to find team number in the database: "
              + teamNumber);
        }
        final Team newWinner = Playoff.pickWinner(connection, currentTournament, performanceElement, tiebreakerElement,
                                                  winnerCriteria, teamB, team, teamScore, runNumber);
        try (PreparedStatement prep = connection.prepareStatement("SELECT TeamNumber FROM Performance" //
            + " WHERE TeamNumber = ?" //
            + " AND RunNumber > ?" //
            + " AND Tournament = ?")) {
          if (newWinner != null
              && oldWinnerTeamNumber != newWinner.getTeamNumber()) {
            // This score update changes the result of the match, so make sure
            // no other scores exist in later round for either of these 2 teams.
            if (getPlayoffTableLineNumber(connection, currentTournament, teamNumber, runNumber
                + 1) > 0) {
              prep.setInt(1, teamNumber);
              prep.setInt(2, runNumber);
              prep.setInt(3, currentTournament);
              try (ResultSet rs = prep.executeQuery()) {
                if (rs.next()) {
                  throw new FLLRuntimeException("Unable to update score for team number "
                      + teamNumber
                      + " in performance run "
                      + runNumber
                      + " because that team has scores entered in subsequent playoff rounds which would become inconsistent. "
                      + "Delete those scores and then you may update this score.");
                }
              }
            }
            if (getPlayoffTableLineNumber(connection, currentTournament, siblingTeam, runNumber
                + 1) > 0) {
              prep.setInt(1, siblingTeam);
              prep.setInt(2, runNumber);
              prep.setInt(3, currentTournament);
              try (ResultSet rs = prep.executeQuery()) {
                if (rs.next()) {
                  throw new FLLRuntimeException("Unable to update score for team number "
                      + teamNumber
                      + " in performance run "
                      + runNumber
                      + " because opponent team "
                      + siblingTeam
                      + " has scores in subsequent playoff rounds which would become inconsistent. "
                      + "Delete those scores and then you may update this score.");
                }
              }
            }
          } // winner of the match changed

        } // PreparedStatement

        // If the second-check flag is NO or the opposing team is not
        // verified, we set the match "winner" (possibly back) to NULL.
        if (!verified
            || !(Queries.performanceScoreExists(connection, currentTournament, teamB, runNumber)
                && Queries.isVerified(connection, currentTournament, teamB, runNumber))) {
          removePlayoffScore(connection, division, currentTournament, runNumber, ptLine);
        } else if (null != newWinner) {
          // have a winner to record
          final Team newLoser;
          if (newWinner.equals(team)) {
            newLoser = teamB;
          } else {
            newLoser = team;
          }
          updatePlayoffData(connection, division, currentTournament, runNumber, ptLine, newWinner, newLoser);

        } // verified score
      } // no sibling
    } else {
      throw new FLLRuntimeException("Team "
          + teamNumber
          + " could not be found in the playoff table for performance run "
          + runNumber);
    }
  }

  /**
   * Delete a performance score in the database in the current tournament.
   *
   * @param connection database connection
   * @param teamNumber team to delete the score for
   * @param runNumber run number to delete
   * @throws SQLException on a database error
   */
  @SuppressFBWarnings(value = "OBL_UNSATISFIED_OBLIGATION", justification = "Bug in findbugs - ticket:2924739")
  public static void deletePerformanceScore(final Connection connection,
                                            final int teamNumber,
                                            final int runNumber)
      throws SQLException {
    final int currentTournament = getCurrentTournament(connection);

    final int numSeedingRounds = TournamentParameters.getNumSeedingRounds(connection, currentTournament);

    final int dbLine = getPlayoffTableLineNumber(connection, currentTournament, teamNumber, runNumber);
    final String division = Playoff.getPlayoffDivision(connection, currentTournament, teamNumber, runNumber);

    // Check if we need to update the PlayoffData table
    try (PreparedStatement prep = connection.prepareStatement("SELECT TeamNumber FROM Performance" //
        + " WHERE TeamNumber = ?" //
        + " AND RunNumber > ?" //
        + " AND Tournament = ?")) {

      if (runNumber > numSeedingRounds) {
        if (dbLine > 0) {
          final int siblingDbLine = dbLine
              % 2 == 0 ? dbLine
                  - 1
                  : dbLine
                      + 1;
          final int siblingTeam = getTeamNumberByPlayoffLine(connection, currentTournament, division, siblingDbLine,
                                                             runNumber);

          if (siblingTeam != Team.NULL_TEAM_NUMBER) {
            // See if either teamNumber or siblingTeam has a score entered in
            // subsequent rounds
            if (getPlayoffTableLineNumber(connection, currentTournament, teamNumber, runNumber
                + 1) > 0) {
              prep.setInt(1, teamNumber);
              prep.setInt(2, runNumber);
              prep.setInt(3, currentTournament);
              try (ResultSet rs = prep.executeQuery()) {
                if (rs.next()) {
                  throw new RuntimeException("Unable to delete score for team number "
                      + teamNumber
                      + " in performance run "
                      + runNumber
                      + " because that team "
                      + " has scores in subsequent playoff rounds which would become inconsistent. "
                      + "Delete those scores and then you may delete this score.");
                }
              }
            }
            if (getPlayoffTableLineNumber(connection, currentTournament, siblingTeam, runNumber
                + 1) > 0) {
              prep.setInt(1, siblingTeam);
              prep.setInt(2, runNumber);
              prep.setInt(3, currentTournament);
              try (ResultSet rs = prep.executeQuery()) {
                if (rs.next()) {
                  throw new RuntimeException("Unable to delete score for team number "
                      + teamNumber
                      + " in performance run "
                      + runNumber
                      + " because opposing team "
                      + siblingTeam
                      + " has scores in subsequent playoff rounds which would become inconsistent. "
                      + "Delete those scores and then you may delete this score.");
                }
              }
            }
          } // sibling not null
        } else {
          // Do nothing - team didn't get entered into the PlayoffData table.
          // This should not happen, but we also cannot get here unless a score
          // got entered for the team in the Performance table, in which case we
          // want to allow the web interface to be able to delete that score to
          // remove the score from the Performance table.
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Deleting a score that wasn't in the PlayoffData table");
          }
        }
      }
    }

    try (PreparedStatement deletePrep = connection.prepareStatement("DELETE FROM Performance " //
        + " WHERE Tournament = ?"
        + " AND TeamNumber = ?"
        + " AND RunNumber = ?")) {
      deletePrep.setInt(1, currentTournament);
      deletePrep.setInt(2, teamNumber);
      deletePrep.setInt(3, runNumber);

      deletePrep.executeUpdate();

      if (runNumber > numSeedingRounds) {
        final ChallengeDescription description = GlobalParameters.getChallengeDescription(connection);
        final PerformanceScoreCategory performance = description.getPerformance();
        final ScoreType performanceScoreType = performance.getScoreType();

        final Team team = Team.getTeamFromDatabase(connection, teamNumber);

        // if the delete of the performance score succeeded it's save to remove the
        // information from the playoff table
        removePlayoffScore(connection, division, currentTournament, runNumber, dbLine);

        // update the display for the deleted score
        H2HUpdateWebSocket.updateBracket(connection, performanceScoreType, division, team, runNumber);
      }
    }

    // notify that the list of unverified runs may have changed
    UnverifiedRunsWebSocket.notifyToUpdate();
  }

  /**
   * Update a row in the playoff table. Assign the specified team and printed
   * flags for the row found by (event_division, Tournament, PlayoffRound,
   * LineNumber).
   */
  private static void updatePlayoffTable(final Connection connection,
                                         final Team team,
                                         final String division,
                                         final int currentTournament,
                                         final int runNumber,
                                         final int lineNumber)
      throws SQLException {

    try (PreparedStatement prep = connection.prepareStatement("UPDATE PlayoffData" //
        + " SET Team = ?" //
        + ", Printed = ?" //
        + " WHERE event_division = ?" //
        + " AND Tournament = ?" //
        + " AND run_number = ?" //
        + " AND LineNumber = ?")) {
      prep.setInt(1, team.getTeamNumber());
      prep.setBoolean(2, false);
      prep.setString(3, division);
      prep.setInt(4, currentTournament);
      prep.setInt(5, runNumber);
      prep.setInt(6, lineNumber);
      prep.executeUpdate();
    }

    final int playoffRound = Playoff.getPlayoffRound(connection, currentTournament, division, runNumber);

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Sending H2H update" //
          + " team: "
          + team.getTeamNumber() //
          + " bracket: "
          + division //
          + " dbLine: "
          + lineNumber //
          + " playoffRound: "
          + playoffRound //
      );

    }

    final ChallengeDescription description = GlobalParameters.getChallengeDescription(connection);
    final PerformanceScoreCategory performance = description.getPerformance();
    final ScoreType performanceScoreType = performance.getScoreType();

    H2HUpdateWebSocket.updateBracket(connection, performanceScoreType, division, team, runNumber, lineNumber);
  }

  /**
   * Remove the playoff score for the next run.
   */
  private static void removePlayoffScore(final Connection connection,
                                         final String division,
                                         final int currentTournament,
                                         final int runNumber,
                                         final int ptLine)
      throws SQLException {
    // winner and loser are both null now
    updatePlayoffData(connection, division, currentTournament, runNumber, ptLine, Team.NULL, Team.NULL);
  }

  private static void updatePlayoffData(final Connection connection,
                                        final String bracketName,
                                        final int tournamentId,
                                        final int runNumber,
                                        final int dbLine,
                                        final Team winner,
                                        final Team loser)
      throws SQLException {
    final int nextRunNumber = runNumber
        + 1;
    final int nextDbLine = ((dbLine
        + 1)
        / 2);

    updatePlayoffTable(connection, winner, bracketName, tournamentId, nextRunNumber, nextDbLine);

    final int semiFinalRound = getNumPlayoffRounds(connection, tournamentId, bracketName)
        - 1;
    final int playoffRun = Playoff.getPlayoffRound(connection, tournamentId, bracketName, runNumber);
    if (playoffRun == semiFinalRound
        && isThirdPlaceEnabled(connection, tournamentId, bracketName)) {
      final int thirdPlaceDbLine = Playoff.computeThirdPlaceDbLine(dbLine);
      updatePlayoffTable(connection, loser, bracketName, tournamentId, nextRunNumber, thirdPlaceDbLine);
    }
  }

  /**
   * Get head to head bracket update data for initializing a display.
   *
   * @param connection the database connection
   * @param bracketName the bracket name
   * @param firstPlayoffRound the first playoff round to include
   * @param lastPlayoffRound the last playoff round to include
   * @param currentTournament the tournament to work with
   * @return the updates to send to the display
   * @throws SQLException on a database error
   */
  public static Collection<BracketUpdate> getH2HBracketData(final Connection connection,
                                                            final int currentTournament,
                                                            final String bracketName,
                                                            final int firstPlayoffRound,
                                                            final int lastPlayoffRound)
      throws SQLException {
    final int maxPlayoffRound = Playoff.getMaxPlayoffRound(connection, currentTournament, bracketName);

    final ChallengeDescription challengeDescription = GlobalParameters.getChallengeDescription(connection);
    final ScoreType performanceScoreType = challengeDescription.getPerformance().getScoreType();

    final Collection<BracketUpdate> updates = new LinkedList<>();
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT PlayoffData.PlayoffRound, PlayoffData.LineNumber, Teams.TeamNumber, Teams.TeamName, Performance.ComputedTotal, Performance.Verified, PlayoffData.AssignedTable, Performance.NoShow" //
            + " FROM PlayoffData" //
            + " JOIN Teams ON (PlayoffData.team = Teams.TeamNumber)"//
            + " LEFT OUTER JOIN Performance ON (Performance.TeamNumber = PlayoffData.team"//
            + " AND Performance.Tournament = PlayoffData.Tournament"//
            + " AND Performance.RunNumber = PlayoffData.run_number)"//
            + " WHERE PlayoffData.Tournament = ?" //
            + " AND PlayoffData.event_division = ?" //
            + " AND PlayoffData.PlayoffRound >= ?" //
            + " AND PlayoffData.PlayoffRound <= ?" //
        )) {
      prep.setInt(1, currentTournament);
      prep.setString(2, bracketName);
      prep.setInt(3, firstPlayoffRound);
      prep.setInt(4, lastPlayoffRound);

      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final int playoffRound = rs.getInt(1);
          final int dbLine = rs.getInt(2);
          final String teamName;
          Integer teamNumber = rs.getInt(3);
          if (rs.wasNull()
              || teamNumber == Team.NULL_TEAM_NUMBER) {
            teamNumber = null;
            teamName = null;
          } else {
            if (teamNumber == Team.TIE_TEAM_NUMBER) {
              teamName = Team.TIE.getTeamName();
            } else {
              teamName = rs.getString(4);
            }
          }
          Double score = rs.getDouble(5);
          final boolean verified;
          if (rs.wasNull()) {
            score = null;
            verified = true;
          } else {
            verified = rs.getBoolean(6);
          }

          final String table = rs.getString(7);
          final boolean noShow = rs.getBoolean(8);

          final BracketUpdate update = new BracketUpdate(bracketName, dbLine, playoffRound, maxPlayoffRound, teamNumber,
                                                         teamName, score, performanceScoreType, noShow, verified,
                                                         table);

          updates.add(update);
        } // foreach result
      } // try ResultSet
    } // try PreparedStatement

    return updates;
  }

  /**
   * Get the division that a team is in for the current tournament.
   *
   * @param connection database connection
   * @param teamNumber the team's number
   * @return the event division for the team
   * @throws SQLException on a database error
   */
  public static @Nullable String getEventDivision(final Connection connection,
                                                  final int teamNumber)
      throws SQLException {
    return getEventDivision(connection, teamNumber, getCurrentTournament(connection));
  }

  /**
   * Get the division that a team is in for the specified tournament.
   *
   * @param connection database connection
   * @param teamNumber the team's number
   * @param tournamentID ID of tournament
   * @return the event division for the team or null if the team cannot be found
   *         in the list of tournament teams
   * @throws SQLException on a database error
   */
  public static @Nullable String getEventDivision(final Connection connection,
                                                  final int teamNumber,
                                                  final int tournamentID)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT event_division FROM TournamentTeams WHERE TeamNumber = ? AND Tournament = ?")) {
      prep.setInt(1, teamNumber);
      prep.setInt(2, tournamentID);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return castNonNull(rs.getString(1));
        } else {
          return null;
        }
      }
    }
  }

  /**
   * Get the judging group that a team is in for the specified tournament.
   *
   * @param connection database connection
   * @param teamNumber the team's number
   * @param tournamentID ID of tournament
   * @return the judging group for the team or null if not found
   * @throws SQLException on a database error
   */
  public static @Nullable String getJudgingGroup(final Connection connection,
                                                 final int teamNumber,
                                                 final int tournamentID)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT judging_station FROM TournamentTeams WHERE TeamNumber = ? AND Tournament = ?")) {
      prep.setInt(1, teamNumber);
      prep.setInt(2, tournamentID);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return rs.getString(1);
        } else {
          return null;
        }
      }
    }
  }

  /**
   * Get a list of team numbers that have fewer runs than seeding rounds. This
   * uses only verified performance scores, so scores that have not been
   * double-checked will show up in this report as not entered.
   *
   * @param connection connection to the database
   * @param tournamentTeams keyed by team number
   * @param verifiedScoresOnly True if the database query should use only
   *          verified scores, false if it should use all scores.
   * @return a List of Team objects
   * @throws SQLException on a database error
   * @throws RuntimeException if a team can't be found in tournamentTeams
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Need to pick view dynamically")
  public static List<Team> getTeamsNeedingSeedingRuns(final Connection connection,
                                                      final Map<Integer, ? extends Team> tournamentTeams,
                                                      final boolean verifiedScoresOnly)
      throws SQLException, RuntimeException {
    final int currentTournament = getCurrentTournament(connection);
    final String view;

    if (verifiedScoresOnly) {
      view = "verified_performance";
    } else {
      view = "Performance";
    }

    try (PreparedStatement prep = connection.prepareStatement("SELECT TeamNumber,Count(*) FROM "
        + view //
        + " WHERE Tournament = ? GROUP BY TeamNumber" //
        + " HAVING Count(*) < ?")) {
      prep.setInt(1, currentTournament);
      prep.setInt(2, TournamentParameters.getNumSeedingRounds(connection, currentTournament));

      try (ResultSet rs = prep.executeQuery()) {
        return collectTeamsFromQuery(tournamentTeams, rs);
      }
    }
  }

  /**
   * The {@link ResultSet} contains a single parameter that is the team number.
   * These numbers are mapped to team objects through
   * <code>tournamentTeams</code>.
   *
   * @throws RuntimeException if a team couldn't be found in the map
   * @throws SQLException on a database error
   */
  private static List<Team> collectTeamsFromQuery(final Map<Integer, ? extends Team> tournamentTeams,
                                                  final ResultSet rs)
      throws SQLException {
    final List<Team> list = new LinkedList<>();
    while (rs.next()) {
      final int teamNumber = rs.getInt(1);
      final Team team = tournamentTeams.get(teamNumber);
      if (null == team) {
        throw new RuntimeException("Couldn't find team number "
            + teamNumber
            + " in the list of tournament teams!");
      }
      list.add(team);
    }
    return list;
  }

  /**
   * Get the order of the teams as seeded in the performance rounds. This will
   * include unverified scores, the assumption being that if you performed the
   * seeding round checks, which exclude unverified scores, you really do want
   * to advance teams.
   *
   * @param connection connection to the database
   * @param winnerCriteria what determines a winner
   * @return a List of teams
   * @param teams teh teams to seed
   * @throws SQLException on a database error
   * @throws RuntimeException if a team can't be found in tournamentTeams
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Need to choose ascending or descending order based upon winner criteria")
  public static List<Team> getPlayoffSeedingOrder(final Connection connection,
                                                  final WinnerType winnerCriteria,
                                                  final Collection<? extends Team> teams)
      throws SQLException, RuntimeException {
    final Tournament tournament = Tournament.getCurrentTournament(connection);

    final List<Integer> teamNumbers = new LinkedList<>();
    for (final Team t : teams) {
      teamNumbers.add(t.getTeamNumber());
    }

    final String teamNumbersStr = StringUtils.join(teamNumbers, ",");

    final List<Team> retval = new ArrayList<>();

    try (
        PreparedStatement prep = connection.prepareStatement("SELECT performance_seeding_max.TeamNumber, performance_seeding_max.Score as score, RAND() as random"
            + " FROM performance_seeding_max, TournamentTeams" //
            + " WHERE score IS NOT NULL" // exclude no shows
            + " AND performance_seeding_max.TeamNumber = TournamentTeams.TeamNumber" //
            + " AND TournamentTeams.tournament = ?" //
            + " AND TournamentTeams.tournament = performance_seeding_max.tournament" //
            + " AND TournamentTeams.TeamNumber IN ( "
            + teamNumbersStr
            + " )" //
            + " ORDER BY score "
            + winnerCriteria.getSortString() //
            + ", performance_seeding_max.average "
            + winnerCriteria.getSortString() //
            + ", random")) {
      prep.setInt(1, tournament.getTournamentID());

      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final int teamNumber = rs.getInt(1);
          final Team team = Team.getTeamFromDatabase(connection, teamNumber);
          if (null == team) {
            throw new RuntimeException("Couldn't find team number "
                + teamNumber
                + " in the list of tournament teams!");
          }
          retval.add(team);
        }
      }
    }
    return retval;
  }

  /**
   * @param connection database conenction
   * @return the current tournament from the database
   * @throws SQLException on a database error
   */
  public static int getCurrentTournament(final Connection connection) throws SQLException {
    if (!GlobalParameters.globalParameterExists(connection, GlobalParameters.CURRENT_TOURNAMENT)) {
      final Tournament dummyTournament = Tournament.findTournamentByName(connection, GenerateDB.DUMMY_TOURNAMENT_NAME);
      // Call setGlobalParameter directly to avoid infinite recursion
      GlobalParameters.setStringGlobalParameter(connection, GlobalParameters.CURRENT_TOURNAMENT,
                                                String.valueOf(dummyTournament.getTournamentID()));
    }
    return GlobalParameters.getIntGlobalParameter(connection, GlobalParameters.CURRENT_TOURNAMENT);
  }

  /**
   * Set the current tournament in the database.
   *
   * @param connection db connection
   * @param tournamentID the new value for the current tournament
   * @throws SQLException on a database error
   */
  public static void setCurrentTournament(final Connection connection,
                                          final int tournamentID)
      throws SQLException {
    final int currentID = getCurrentTournament(connection);
    if (currentID != tournamentID) {
      GlobalParameters.setIntGlobalParameter(connection, GlobalParameters.CURRENT_TOURNAMENT, tournamentID);
    }
  }

  /**
   * Delete a team from the database. This clears team from the Teams table and
   * all tables specified by the challengeDocument. It is not an error if the
   * team doesn't exist.
   *
   * @param description describes the scoring of the tournament
   * @param teamNumber team to delete
   * @param connection connection to database, needs delete privileges
   * @throws SQLException on an error talking to the database
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category name determines table")
  public static void deleteTeam(final int teamNumber,
                                final ChallengeDescription description,
                                final Connection connection)
      throws SQLException {
    final boolean autoCommit = connection.getAutoCommit();
    try {
      connection.setAutoCommit(false);

      try (PreparedStatement prep = connection.prepareStatement("DELETE FROM TournamentTeams WHERE TeamNumber = ?")) {

        // delete from TournamentTeams

        prep.setInt(1, teamNumber);
        prep.executeUpdate();
      }

      // delete from subjective categories
      for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
        final String name = category.getName();
        try (PreparedStatement prep = connection.prepareStatement("DELETE FROM "
            + name
            + " WHERE TeamNumber = ?")) {
          prep.setInt(1, teamNumber);
          prep.executeUpdate();
        }
      }

      // delete from Performance
      try (PreparedStatement prep = connection.prepareStatement("DELETE FROM Performance WHERE TeamNumber = ?")) {
        prep.setInt(1, teamNumber);
        prep.executeUpdate();
      }

      // delete from final_scores
      try (PreparedStatement prep = connection.prepareStatement("DELETE FROM final_scores WHERE team_number = ?")) {
        prep.setInt(1, teamNumber);
        prep.executeUpdate();
      }

      // delete from overall_scores
      try (PreparedStatement prep = connection.prepareStatement("DELETE FROM overall_scores WHERE team_number = ?")) {
        prep.setInt(1, teamNumber);
        prep.executeUpdate();
      }

      // delete from overall_scores
      try (
          PreparedStatement prep = connection.prepareStatement("DELETE FROM subjective_computed_scores WHERE team_number = ?")) {
        prep.setInt(1, teamNumber);
        prep.executeUpdate();
      }

      // delete from schedule
      try (
          PreparedStatement prep = connection.prepareStatement("DELETE FROM sched_perf_rounds WHERE team_number = ?")) {
        prep.setInt(1, teamNumber);
        prep.executeUpdate();
      }
      try (PreparedStatement prep = connection.prepareStatement("DELETE FROM sched_subjective WHERE team_number = ?")) {
        prep.setInt(1, teamNumber);
        prep.executeUpdate();
      }
      try (PreparedStatement prep = connection.prepareStatement("DELETE FROM schedule WHERE team_number = ?")) {
        prep.setInt(1, teamNumber);
        prep.executeUpdate();
      }

      // delete from Teams
      try (PreparedStatement prep = connection.prepareStatement("DELETE FROM Teams WHERE TeamNumber = ?")) {
        prep.setInt(1, teamNumber);
        prep.executeUpdate();
      }

      connection.commit();
    } finally {
      try {
        connection.setAutoCommit(autoCommit);
      } catch (final SQLException e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(e, e);
        }
      }
    }
  }

  /**
   * Total the scores in the database for the specified tournament.
   *
   * @param description describes the scoring for the challenge
   * @param connection connection to database, needs write privileges
   * @param tournament tournament to update score totals for
   * @throws SQLException if an error occurs
   * @see #updatePerformanceScoreTotals(ChallengeDescription, Connection, int)
   * @see #updateSubjectiveScoreTotals(ChallengeDescription, Connection, int)
   */
  public static void updateScoreTotals(final ChallengeDescription description,
                                       final Connection connection,
                                       final int tournament)
      throws SQLException {
    updatePerformanceScoreTotals(description, connection, tournament);

    updateSubjectiveScoreTotals(description, connection, tournament);
  }

  /**
   * Compute the total scores for all entered subjective scores.
   * This populates the table subjecive_computed_scores.
   *
   * @param connection
   * @throws SQLException
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category determines table name")
  private static void updateSubjectiveScoreTotals(final ChallengeDescription description,
                                                  final Connection connection,
                                                  final int tournament)
      throws SQLException {

    try (
        PreparedStatement deletePrep = connection.prepareStatement("DELETE FROM subjective_computed_scores WHERE tournament = ?")) {
      deletePrep.setInt(1, tournament);
      deletePrep.executeUpdate();
    }

    for (final SubjectiveScoreCategory subjectiveElement : description.getSubjectiveCategories()) {
      final String categoryName = subjectiveElement.getName();

      try (PreparedStatement insertPrep = connection.prepareStatement("INSERT INTO subjective_computed_scores"//
          + " (category, goal_group, tournament, team_number, judge, computed_total, no_show) " //
          + " VALUES(?, ?, ?, ?, ?, ?, ?)");
          PreparedStatement selectPrep = connection.prepareStatement("SELECT * FROM " //
              + categoryName //
              + " WHERE Tournament = ?")) {
        selectPrep.setInt(1, tournament);

        insertPrep.setString(1, categoryName);
        insertPrep.setInt(3, tournament);

        try (ResultSet rs = selectPrep.executeQuery()) {
          while (rs.next()) {
            final int teamNumber = rs.getInt("TeamNumber");
            insertPrep.setInt(4, teamNumber);

            try (DatabaseTeamScore teamScore = new DatabaseTeamScore(teamNumber, rs)) {
              final double computedTotal;
              if (teamScore.isNoShow()) {
                computedTotal = Double.NaN;
              } else {
                computedTotal = subjectiveElement.evaluate(teamScore);
              }

              final String judge = rs.getString("Judge");
              insertPrep.setString(5, judge);

              insertPrep.setBoolean(7, teamScore.isNoShow());

              // insert category score
              insertPrep.setString(2, "");
              if (Double.isNaN(computedTotal)) {
                insertPrep.setNull(6, Types.DOUBLE);
              } else {
                insertPrep.setDouble(6, computedTotal);
              }
              insertPrep.executeUpdate();

              // insert goal group scores
              final Map<String, Double> goalGroupScores = subjectiveElement.getGoalGroupScores(teamScore);
              for (final Map.Entry<String, Double> entry : goalGroupScores.entrySet()) {
                final String group = entry.getKey();
                final double score = entry.getValue();

                insertPrep.setString(2, group);
                if (Double.isNaN(score)) {
                  insertPrep.setNull(6, Types.DOUBLE);
                } else {
                  insertPrep.setDouble(6, score);
                }
                insertPrep.executeUpdate();
              }
            } // team score
          } // foreach result
        } // ResultSet
      } // prepared statements
    } // foreach category
  }

  /**
   * Compute the total scores for all entered performance scores. Uses both
   * verified and unverified scores.
   *
   * @param description description of the challenge
   * @param connection connection to the database
   * @param tournament the tournament to update scores for.
   * @throws SQLException on a database error
   */
  private static void updatePerformanceScoreTotals(final ChallengeDescription description,
                                                   final Connection connection,
                                                   final int tournament)
      throws SQLException {
    try (
        PreparedStatement updatePrep = connection.prepareStatement("UPDATE Performance SET ComputedTotal = ? WHERE TeamNumber = ? AND Tournament = ? AND RunNumber = ?");
        PreparedStatement selectPrep = connection.prepareStatement("SELECT * FROM Performance WHERE Tournament = ?")) {

      updatePrep.setInt(3, tournament);
      selectPrep.setInt(1, tournament);

      final PerformanceScoreCategory performanceElement = description.getPerformance();
      final double minimumPerformanceScore = performanceElement.getMinimumScore();
      try (ResultSet rs = selectPrep.executeQuery()) {
        while (rs.next()) {
          if (!rs.getBoolean("Bye")) {
            final int teamNumber = rs.getInt("TeamNumber");
            final int runNumber = rs.getInt("RunNumber");
            final double computedTotal;

            try (DatabaseTeamScore teamScore = new DatabaseTeamScore(teamNumber, runNumber, rs)) {
              if (teamScore.isNoShow()) {
                computedTotal = Double.NaN;
              } else {
                computedTotal = performanceElement.evaluate(teamScore);
              }
            }

            if (LOGGER.isTraceEnabled()) {
              LOGGER.trace("Updating performance score for "
                  + teamNumber
                  + " run: "
                  + runNumber
                  + " total: "
                  + computedTotal);
            }

            if (!Double.isNaN(computedTotal)) {
              updatePrep.setDouble(1, Math.max(computedTotal, minimumPerformanceScore));
            } else {
              updatePrep.setNull(1, Types.DOUBLE);
            }
            updatePrep.setInt(2, teamNumber);
            updatePrep.setInt(4, runNumber);
            updatePrep.executeUpdate();
          }
        }
      }
    }
  }

  /**
   * Get all tournament IDs that this team is in.
   *
   * @param connection database connection
   * @param teamNumber team number to search for
   * @return collection of all tournament IDs that this team is in
   * @throws SQLException if there is a database error
   */
  public static Collection<Integer> getAllTournamentsForTeam(final Connection connection,
                                                             final int teamNumber)
      throws SQLException {
    final Collection<Integer> tournaments = new LinkedList<>();
    try (PreparedStatement prep = connection.prepareStatement("SELECT Tournament" //
        + " FROM TournamentTeams" //
        + " WHERE TeamNumber = ?")) {
      prep.setInt(1, teamNumber);
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final int id = rs.getInt(1);
          tournaments.add(id);
        }
      }
    }
    return tournaments;
  }

  /**
   * Set the judging station for a given team at the specified tournament.
   *
   * @param connection db connection
   * @param teamNumber the team's number
   * @param tournamentID the tournament
   * @param judgingStation the new judging station
   * @return true if the update occurred, false if the team isn't in the
   *         tournament
   * @throws SQLException on a database error
   */
  public static boolean updateTeamJudgingGroups(final Connection connection,
                                                final int teamNumber,
                                                final int tournamentID,
                                                final String judgingStation)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("UPDATE TournamentTeams SET judging_station = ? WHERE TeamNumber = ? AND Tournament = ?")) {
      prep.setString(1, judgingStation);
      prep.setInt(2, teamNumber);
      prep.setInt(3, tournamentID);
      return prep.executeUpdate() > 0;
    }
  }

  /**
   * Delete all record of a team from a tournament. This includes the scores and
   * the TournamentTeams table.
   * 
   * @param connection database connection
   * @param description used to get the subjective category names
   * @param teamNumber team to delete
   * @param currentTournament tournament to delete the team from
   * @throws SQLException on a database error
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category determines table name")
  public static void deleteTeamFromTournament(final Connection connection,
                                              final ChallengeDescription description,
                                              final int teamNumber,
                                              final int currentTournament)
      throws SQLException {

    // delete from subjective categories
    for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
      final String name = category.getName();
      try (PreparedStatement prep = connection.prepareStatement("DELETE FROM "
          + name
          + " WHERE TeamNumber = ? AND Tournament = ?")) {
        prep.setInt(1, teamNumber);
        prep.setInt(2, currentTournament);
        prep.executeUpdate();
      }
    }

    // delete from Performance
    try (
        PreparedStatement prep = connection.prepareStatement("DELETE FROM Performance WHERE TeamNumber = ? AND Tournament = ?")) {
      prep.setInt(1, teamNumber);
      prep.setInt(2, currentTournament);
      prep.executeUpdate();
    }

    // delete from final_scores
    try (
        PreparedStatement prep = connection.prepareStatement("DELETE FROM final_scores WHERE team_number = ? AND tournament = ?")) {
      prep.setInt(1, teamNumber);
      prep.setInt(2, currentTournament);
      prep.executeUpdate();
    }

    // delete from overall_scores
    try (
        PreparedStatement prep = connection.prepareStatement("DELETE FROM overall_scores WHERE team_number = ? AND tournament = ?")) {
      prep.setInt(1, teamNumber);
      prep.setInt(2, currentTournament);
      prep.executeUpdate();
    }

    // delete from subjective_computed_scores
    try (
        PreparedStatement prep = connection.prepareStatement("DELETE FROM subjective_computed_scores WHERE team_number = ? AND tournament = ?")) {
      prep.setInt(1, teamNumber);
      prep.setInt(2, currentTournament);
      prep.executeUpdate();
    }

    // delete from PlayoffData
    try (
        PreparedStatement prep = connection.prepareStatement("DELETE FROM PlayoffData WHERE Team = ? AND Tournament = ?")) {
      prep.setInt(1, teamNumber);
      prep.setInt(2, currentTournament);
      prep.executeUpdate();
    }

    // delete from schedule
    try (
        PreparedStatement prep = connection.prepareStatement("DELETE FROM sched_perf_rounds WHERE team_number = ? AND tournament = ?")) {
      prep.setInt(1, teamNumber);
      prep.setInt(2, currentTournament);
      prep.executeUpdate();
    }
    try (
        PreparedStatement prep = connection.prepareStatement("DELETE FROM sched_subjective WHERE team_number = ? AND tournament = ?")) {
      prep.setInt(1, teamNumber);
      prep.setInt(2, currentTournament);
      prep.executeUpdate();
    }
    try (
        PreparedStatement prep = connection.prepareStatement("DELETE FROM schedule WHERE team_number = ? AND tournament = ?")) {
      prep.setInt(1, teamNumber);
      prep.setInt(2, currentTournament);
      prep.executeUpdate();
    }

    // delete from TournamentTeams
    try (
        PreparedStatement prep = connection.prepareStatement("DELETE FROM TournamentTeams WHERE TeamNumber = ? AND Tournament = ?")) {
      prep.setInt(1, teamNumber);
      prep.setInt(2, currentTournament);
      prep.executeUpdate();
    }

  }

  /**
   * Add a team to the database.
   *
   * @param connection the database connection
   * @param number team number
   * @param name team name
   * @param organization organization
   * @return null on success, the name of the other team with the same team
   *         number on an error
   * @throws FLLRuntimeException if the team number is an internal team number
   * @throws SQLException on a database error
   */
  public static @Nullable String addTeam(final Connection connection,
                                         final int number,
                                         final String name,
                                         final @Nullable String organization)
      throws SQLException {
    if (Team.isInternalTeamNumber(number)) {
      throw new FLLRuntimeException("Cannot create team with an internal number: "
          + number);
    }

    final boolean autoCommit = connection.getAutoCommit();
    try {
      connection.setAutoCommit(false);
      // this is in a transaction as the insert depends on
      // the same state as the select

      // need to check for duplicate teamNumber
      try (
          PreparedStatement checkDuplicate = connection.prepareStatement("SELECT TeamName FROM Teams WHERE TeamNumber = ?")) {
        checkDuplicate.setInt(1, number);
        try (ResultSet rs = checkDuplicate.executeQuery()) {
          if (rs.next()) {
            final String dup = rs.getString(1);
            return dup;
          }
        }
      }

      try (
          PreparedStatement insert = connection.prepareStatement("INSERT INTO Teams (TeamName, Organization, TeamNumber) VALUES (?, ?, ?)")) {
        insert.setString(1, name);
        insert.setString(2, organization);
        insert.setInt(3, number);
        insert.executeUpdate();
      }
    } finally {
      try {
        connection.setAutoCommit(autoCommit);
      } catch (final SQLException e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(e, e);
        }
      }
    }
    return null;
  }

  /**
   * Check if a team is assigned to a tournament. If you are checking for a number
   * of teams against the same tournament, then
   * {@link #getTournamentTeams(Connection, int)} will be more efficient.
   *
   * @param connection the database connection
   * @param teamNumber the team number to check
   * @param tournament the tournament ID to check
   * @return true if the team is in the tournament
   * @throws SQLException on a database error
   */
  public static boolean isTeamInTournament(final Connection connection,
                                           final int teamNumber,
                                           final int tournament)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT TeamNumber FROM TournamentTeams WHERE TeamNumber = ? and Tournament = ?")) {
      prep.setInt(1, teamNumber);
      prep.setInt(2, tournament);
      try (ResultSet rs = prep.executeQuery()) {
        return rs.next();
      }
    }
  }

  /**
   * Add a team to a tournament.
   *
   * @param connection database connection
   * @param teamNumber the team to add
   * @param tournament the tournament id of the tournament to be added to
   * @param eventDivision the event division the team is in for this tournament
   * @param judgingStation the judging station for the team in this tournament
   * @throws SQLException if a database problem occurs, including the team
   *           already being in the tournament
   */
  public static void addTeamToTournament(final Connection connection,
                                         final int teamNumber,
                                         final int tournament,
                                         final String eventDivision,
                                         final String judgingStation)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("INSERT INTO TournamentTeams (Tournament, TeamNumber, event_division, judging_station) VALUES (?, ?, ?, ?)")) {
      prep.setInt(1, tournament);
      prep.setInt(2, teamNumber);
      prep.setString(3, eventDivision);
      prep.setString(4, judgingStation);
      prep.executeUpdate();
    }

  }

  /**
   * Update a team in the database.
   *
   * @param organization the new organization
   * @param connection the database connection
   * @param number the team number
   * @param name the new name
   * @throws SQLException if there is a database error
   */
  public static void updateTeam(final Connection connection,
                                final int number,
                                final String name,
                                final @Nullable String organization)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("UPDATE Teams SET TeamName = ?, Organization = ? WHERE TeamNumber = ?")) {
      prep.setString(1, name);
      prep.setString(2, organization);
      prep.setInt(3, number);
      prep.executeUpdate();
    }
  }

  /**
   * Update a team award group (event division).
   * 
   * @param connection database connection
   * @param number team number
   * @param tournamentID the tournament to work with
   * @param eventDivision the new award group
   * @throws SQLException on a database error
   */
  public static void updateTeamEventDivision(final Connection connection,
                                             final int number,
                                             final int tournamentID,
                                             final String eventDivision)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("UPDATE TournamentTeams SET event_division = ? WHERE TeamNumber = ? AND Tournament = ?")) {
      prep.setString(1, eventDivision);
      prep.setInt(2, number);
      prep.setInt(3, tournamentID);
      prep.executeUpdate();
    }
  }

  /**
   * Update a team name.
   * 
   * @throws SQLException on a database error
   * @param connection database connection
   * @param number team number
   * @param name the new name
   */
  public static void updateTeamName(final Connection connection,
                                    final int number,
                                    final String name)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("UPDATE Teams SET TeamName = ? WHERE TeamNumber = ?")) {
      prep.setString(1, name);
      prep.setInt(2, number);
      prep.executeUpdate();
    }
  }

  /**
   * Update a team organization.
   * 
   * @param connection database connection
   * @param number team number
   * @param organization the new organization
   * @throws SQLException on a database error
   */
  public static void updateTeamOrganization(final Connection connection,
                                            final int number,
                                            final @Nullable String organization)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("UPDATE Teams SET Organization = ? WHERE TeamNumber = ?")) {
      prep.setString(1, organization);
      prep.setInt(2, number);
      prep.executeUpdate();
    }
  }

  /**
   * Make sure all of the judges are properly assigned for the current
   * tournament.
   *
   * @param connection the database connection
   * @param challengeDescription used to determine the categories
   * @return true if everything is ok
   * @throws SQLException on a database error
   */
  public static boolean isJudgesProperlyAssigned(final Connection connection,
                                                 final ChallengeDescription challengeDescription)
      throws SQLException {

    try (
        PreparedStatement prep = connection.prepareStatement("SELECT id FROM Judges WHERE Tournament = ? AND category = ?")) {
      prep.setInt(1, getCurrentTournament(connection));

      for (final SubjectiveScoreCategory element : challengeDescription.getSubjectiveCategories()) {
        final String categoryName = element.getName();
        prep.setString(2, categoryName);
        try (ResultSet rs = prep.executeQuery()) {
          if (!rs.next()) {
            return false;
          }
        }
      }
      return true;
    }
  }

  /**
   * Determines whether or not the playoff data table has been initialized for
   * the specified division. Uses the current tournament value obtained from
   * getCurrentTournament().
   *
   * @param connection The database connection to use.
   * @param division The division to check in the current tournament.
   * @return A boolean, true if the PlayoffData table has been initialized,
   *         false if it has not.
   * @throws SQLException if database access fails.
   * @throws RuntimeException if query returns empty results.
   * @see #isPlayoffDataInitialized(Connection, int, String)
   */
  public static boolean isPlayoffDataInitialized(final Connection connection,
                                                 final String division)
      throws SQLException, RuntimeException {
    final int curTourney = getCurrentTournament(connection);
    return isPlayoffDataInitialized(connection, curTourney, division);
  }

  /**
   * Check if playoff data is initialized for the specified tournament and
   * division.
   *
   * @param connection The database connection to use.
   * @param tournamentID The tournament to check
   * @param division The division to check in the current tournament.
   * @return A boolean, true if the PlayoffData table has been initialized,
   *         false if it has not.
   * @throws SQLException if database access fails.
   * @throws RuntimeException if query returns empty results.
   */
  public static boolean isPlayoffDataInitialized(final Connection connection,
                                                 final int tournamentID,
                                                 final String division)
      throws SQLException, RuntimeException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT Count(*) FROM PlayoffData"
        + " WHERE Tournament = ?"//
        + " AND event_division = ?")) {
      prep.setInt(1, tournamentID);
      prep.setString(2, division);
      try (ResultSet rs = prep.executeQuery()) {
        if (!rs.next()) {
          throw new RuntimeException("Query to obtain count of PlayoffData entries returned no data");
        } else {
          return rs.getInt(1) > 0;
        }
      }
    }
  }

  /**
   * Check if any playoff bracket is initialized for the specified tournament.
   *
   * @param connection database connection
   * @param tournamentID tournament ID
   * @return true if any playoff bracket is initialized in the tournament
   * @throws SQLException if the database connection fails
   * @throws RuntimeException if there is no playoff data for this tournament
   */
  public static boolean isPlayoffDataInitialized(final Connection connection,
                                                 final int tournamentID)
      throws SQLException, RuntimeException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT Count(*) FROM PlayoffData"
        + " WHERE Tournament = ?")) {
      prep.setInt(1, tournamentID);
      try (ResultSet rs = prep.executeQuery()) {
        if (!rs.next()) {
          throw new RuntimeException("Query to obtain count of PlayoffData entries returned no data");
        } else {
          return rs.getInt(1) > 0;
        }
      }
    }
  }

  /**
   * Query for whether the specified team has advanced to the specified
   * (playoff) round.
   *
   * @param connection The database connection to use.
   * @param roundNumber The round number to check. Must be greater than # of
   *          seeding rounds.
   * @param teamNumber the team to check
   * @return true if team has entry in playoff table for the given round.
   * @throws SQLException on a database error
   * @throws RuntimeException if the query failed
   * @see #didTeamReachPlayoffRound(Connection, int, int, int)
   */
  public static boolean didTeamReachPlayoffRound(final Connection connection,
                                                 final int roundNumber,
                                                 final int teamNumber)
      throws SQLException, RuntimeException {
    return didTeamReachPlayoffRound(connection, getCurrentTournament(connection), roundNumber, teamNumber);
  }

  /**
   * @param connection database connection
   * @param tournamentID the tournament to work with
   * @param roundNumber the round to check
   * @param teamNumber the team
   * @return true if the team reached the specified play off round
   * @throws SQLException on a database error
   * @throws RuntimeException if the query failed
   */
  public static boolean didTeamReachPlayoffRound(final Connection connection,
                                                 final int tournamentID,
                                                 final int roundNumber,
                                                 final int teamNumber)
      throws SQLException, RuntimeException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT Count(*) FROM PlayoffData"
        + " WHERE Tournament = ?" //
        + " AND run_number = ?" //
        + " AND Team = ?")) {
      prep.setInt(1, tournamentID);
      prep.setInt(2, roundNumber);
      prep.setInt(3, teamNumber);
      try (ResultSet rs = prep.executeQuery()) {
        if (!rs.next()) {
          throw new RuntimeException("Query to check for team # "
              + Integer.toString(teamNumber)
              + "in round "
              + Integer.toString(roundNumber)
              + " failed.");
        } else {
          return rs.getInt(1) == 1;
        }
      }
    }
  }

  /**
   * Colors for index into a list.
   * Below are the colors used in order.
   * <ol>
   * <li>#800000</li>
   * <li>#008000</li>
   * <li>#CC6600</li>
   * <li>#FF00FF</li>
   * </ol>
   *
   * @param index the division index
   * @return color in a format suitable for use in an HTML document
   */
  public static String getColorForIndex(final int index) {
    final int idx = index
        % 4;
    switch (idx) {
    case 0:
      return "#800000";
    case 1:
      return "#008000";
    case 2:
      return "#CC6600";
    case 3:
      return "#FF00FF";
    default:
      throw new FLLInternalException("Internal error, cannot choose color");
    }
  }

  /**
   * Get the value of Bye for the given team number, tournament and run number.
   *
   * @return true if the score is a bye, false if it's not a bye or the score
   *         does not exist
   * @throws SQLException on a database error
   * @param connection database connection
   * @param tournament tournament to work with
   * @param teamNumber the team to check
   * @param runNumber the run to check
   */
  public static boolean isBye(final Connection connection,
                              final int tournament,
                              final int teamNumber,
                              final int runNumber)
      throws SQLException {
    try (PreparedStatement prep = getScoreStatsPrep(connection)) {
      prep.setInt(1, tournament);
      prep.setInt(2, teamNumber);
      prep.setInt(3, runNumber);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return rs.getBoolean("Bye");
        } else {
          return false;
        }
      }
    }
  }

  /**
   * Get the value of NoShow for the given team number, tournament and run
   * number.
   *
   * @param connection database connection
   * @param tournament the tournament to work with
   * @param teamNumber the team to check
   * @param runNumber the run to check
   * @return true if the score is a No Show, false if it's not a bye or the
   *         score does not exist
   * @throws SQLException on a database error
   */
  public static boolean isNoShow(final Connection connection,
                                 final int tournament,
                                 final int teamNumber,
                                 final int runNumber)
      throws SQLException {
    try (PreparedStatement prep = getScoreStatsPrep(connection)) {
      prep.setInt(1, tournament);
      prep.setInt(2, teamNumber);
      prep.setInt(3, runNumber);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return rs.getBoolean("NoShow");
        } else {
          return false;
        }
      }
    }
  }

  /**
   * @return true if the score has been verified, i.e. double-checked.
   * @throws SQLException on a database error
   * @param connection the database connection
   * @param tournament the tournament to work with
   * @param teamNumber the team to check
   * @param runNumber the run to check
   */
  public static boolean isVerified(final Connection connection,
                                   final int tournament,
                                   final int teamNumber,
                                   final int runNumber)
      throws SQLException {
    try (PreparedStatement prep = getScoreStatsPrep(connection)) {
      prep.setInt(1, tournament);
      prep.setInt(2, teamNumber);
      prep.setInt(3, runNumber);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return rs.getBoolean("Verified");
        } else {
          return false;
        }
      }
    }
  }

  /**
   * Get prepared statement that gets Verified, NoShow, Bye columns for a score.
   *
   * @param connection
   * @return 1 is tournament, 2 is teamNumber, 3 is runNumber
   * @throws SQLException on a database error
   */
  @SuppressFBWarnings(value = { "NP_LOAD_OF_KNOWN_NULL_VALUE" }, justification = "Findbugs bug 3477957")
  private static PreparedStatement getScoreStatsPrep(final Connection connection) throws SQLException {
    return connection.prepareStatement("SELECT Bye, NoShow, Verified FROM Performance WHERE Tournament = ? AND TeamNumber = ? AND RunNumber = ?");
  }

  /**
   * Used to get the line number of a team from the playoff table for a specific
   * round of the playoff bracket.
   * This method cannot be used for internal team numbers as they may reference
   * multiple lines.
   *
   * @param connection Database connection to use.
   * @param tournament Tournament identifier.
   * @param teamNumber Team number for which to look.
   * @param runNumber Run number, based at 1, counted from start of tournament
   * @return The line number of the playoff bracket in which the team number is
   *         found, or a -1 if the team number was not found in the specified
   *         round of the PlayoffData table.
   * @throws SQLException on a database error
   * @throws IllegalArgumentException if teamNumber is for an internal team
   * @see Team#isInternalTeamNumber(int)
   */
  public static int getPlayoffTableLineNumber(final Connection connection,
                                              final int tournament,
                                              final int teamNumber,
                                              final int runNumber)
      throws SQLException {
    if (Team.isInternalTeamNumber(teamNumber)) {
      throw new IllegalArgumentException("Cannot reliably determine playoff dbline for internal teams");
    }

    try (PreparedStatement prep = connection.prepareStatement("SELECT LineNumber FROM PlayoffData"
        + " WHERE Team = ?"//
        + " AND Tournament = ?" //
        + " AND run_number = ?")) {
      prep.setInt(1, teamNumber);
      prep.setInt(2, tournament);
      prep.setInt(3, runNumber);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        } else {
          return -1; // indicates team not present in this run
        }
      }
    }
  }

  /**
   * Gets the number of the team from the PlayoffData table given the
   * tournament, division, line number, and playoff round.
   *
   * @param connection Database connection.
   * @param tournament Tournament identifier.
   * @param division Division string.
   * @param lineNumber Line number of the playoff bracket, based at 1.
   * @param runNumber performance run number, based at 1.
   * @return The team number located at the specified location in the playoff
   *         bracket.
   * @throws SQLException if there is a database error.
   */
  public static int getTeamNumberByPlayoffLine(final Connection connection,
                                               final int tournament,
                                               final String division,
                                               final int lineNumber,
                                               final int runNumber)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT Team FROM PlayoffData" //
        + " WHERE event_division = ?" //
        + " AND Tournament = ?" //
        + " AND LineNumber = ?" //
        + " AND run_number = ?")) {
      prep.setString(1, division);
      prep.setInt(2, tournament);
      prep.setInt(3, lineNumber);
      prep.setInt(4, runNumber);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final int retVal = rs.getInt(1);
          if (rs.wasNull()) {
            return Team.NULL_TEAM_NUMBER;
          } else {
            return retVal;
          }
        }
      }
    }
    return Team.NULL_TEAM_NUMBER;
  }

  /**
   * Returns the number of playoff rounds for the specified division. Depends on
   * the PlayoffData table having been initialized for that division.
   *
   * @param tournament the tournament to work with
   * @param connection The database connection.
   * @param division The division for which to get the number of playoff rounds.
   * @return The number of playoff rounds in the specified division, or 0 if
   *         brackets have not been initialized.
   * @throws SQLException on database errors.
   */
  public static int getNumPlayoffRounds(final Connection connection,
                                        final int tournament,
                                        final String division)
      throws SQLException {
    final int x = getFirstPlayoffRoundSize(connection, tournament, division);
    if (x > 0) {
      return (int) Math.round(Math.log(x)
          / Math.log(2));
    } else {
      return 0;
    }
  }

  /**
   * Returns the max number of playoff rounds all divisions. Depends on the
   * PlayoffData table having been initialized for that division.
   *
   * @param tournament the tournament to work with
   * @param connection The database connection.
   * @return The maximum number of playoff rounds in all divisions, or 0 if
   *         brackets have not been initialized.
   * @throws SQLException on database errors.
   */
  public static int getNumPlayoffRounds(final Connection connection,
                                        final int tournament)
      throws SQLException {
    int numRounds = 0;
    for (final String division : Playoff.getPlayoffBrackets(connection, tournament)) {
      final int x = getFirstPlayoffRoundSize(connection, tournament, division);
      if (x > 0) {
        numRounds = Math.max((int) Math.round(Math.log(x)
            / Math.log(2)), numRounds);
      }
    }
    return numRounds;
  }

  /**
   * Get size of first playoff round.
   *
   * @param connection Database connection to use.
   * @param division The playoff division for which to look up round 1 size.
   * @param tournament the routnament to work with
   * @return The size of the first round of the playoffs. This is always a power
   *         of 2, and is greater than the number of teams in the tournament by
   *         the number of byes in the first round.
   * @throws SQLException on database error.
   */
  public static int getFirstPlayoffRoundSize(final Connection connection,
                                             final int tournament,
                                             final String division)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT count(*) FROM PlayoffData" //
        + " WHERE Tournament= ?" //
        + " AND event_division= ?" //
        + " AND PlayoffRound=1")) {
      prep.setInt(1, tournament);
      prep.setString(2, division);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1);
        } else {
          return 0;
        }
      }
    }
  }

  /**
   * Returns the table assignment string for the given tournament, bracket name,
   * playoff round number, and database line number. If the table assignment is
   * NULL, returns null.
   * 
   * @param connection database connection
   * @param tournament the tournament to work with
   * @param eventDivision the head to head bracket to work with
   * @param round the playoff round number
   * @param line the line number in the head to head bracket
   * @return the table assignment
   * @throws SQLException on a database error
   */
  public static @Nullable String getAssignedTable(final Connection connection,
                                                  final int tournament,
                                                  final String eventDivision,
                                                  final int round,
                                                  final int line)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT AssignedTable FROM PlayoffData WHERE Tournament= ?" //
            + " AND event_division= ?" //
            + " AND PlayoffRound= ?"//
            + " AND LineNumber= ?" //
            + " AND AssignedTable IS NOT NULL")) {
      prep.setInt(1, tournament);
      prep.setString(2, eventDivision);
      prep.setInt(3, round);
      prep.setInt(4, line);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return rs.getString(1);
        } else {
          return null;
        }
      }
    }
  }

  /**
   * Get the database version. If no version information exists in the database,
   * the version is 0.
   *
   * @param connection the database to check
   * @return the database version
   * @throws SQLException on a database error
   */
  public static int getDatabaseVersion(final Connection connection) throws SQLException {
    final Collection<String> tables = SQLFunctions.getTablesInDB(connection);
    if (!tables.contains("global_parameters")) {
      return 0;
    } else {
      return GlobalParameters.getIntGlobalParameter(connection, GlobalParameters.DATABASE_VERSION);
    }
  }

  /**
   * Get all team numbers.
   *
   * @param connection database connection
   * @return all team numbers
   * @throws SQLException on a database error
   */
  public static Collection<Integer> getAllTeamNumbers(final Connection connection) throws SQLException {
    final Set<Integer> allTeamNumbers = new HashSet<>();
    try (Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT TeamNumber FROM Teams")) {
      while (rs.next()) {
        allTeamNumbers.add(rs.getInt(1));
      }
    }
    return allTeamNumbers;
  }

  /**
   * Test if a performance score exists for the given team, tournament and run
   * number.
   *
   * @param connection database connection
   * @param tournament to check
   * @param teamNumber team number to check
   * @param runNumber run number to check
   * @return true if the score exists
   * @throws SQLException on a database error
   * @see #getPerformanceScore(Connection, int, int, int)
   */
  public static boolean performanceScoreExists(final Connection connection,
                                               final int tournament,
                                               final int teamNumber,
                                               final int runNumber)
      throws SQLException {
    return null != getPerformanceScore(connection, tournament, teamNumber, runNumber);
  }

  /**
   * Get a team's performance score for a specific tournament.
   *
   * @param connection database connection
   * @param tournament the id of the tournament to work with
   * @param teamNumber which team to get the score for
   * @param runNumber which run number to get the score for
   * @return the score if it exists, null if it doesn't exist
   * @throws SQLException on a database error
   */
  public static @Nullable Double getPerformanceScore(final Connection connection,
                                                     final int tournament,
                                                     final int teamNumber,
                                                     final int runNumber)
      throws SQLException {

    try (PreparedStatement prep = connection.prepareStatement("SELECT ComputedTotal FROM Performance"
        + " WHERE TeamNumber = ? AND Tournament = ? AND RunNumber = ?")) {
      prep.setInt(1, teamNumber);
      prep.setInt(2, tournament);
      prep.setInt(3, runNumber);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return rs.getDouble(1);
        } else {
          return null;
        }
      }
    }
  }

  /**
   * Get the max performance run number completed for the specified team in the
   * current tournament. This does not check the verified flag.
   *
   * @param connection database connection
   * @param teamNumber the team to check
   * @return the max run number or 0 if no performance runs have been completed
   * @throws SQLException on a database error
   */
  public static int maxPerformanceRunNumberCompleted(final Connection connection,
                                                     final int teamNumber)
      throws SQLException {
    final int tournament = getCurrentTournament(connection);

    try (PreparedStatement prep = connection.prepareStatement("SELECT MAX(RunNumber) FROM Performance"
        + " WHERE TeamNumber = ? AND Tournament = ?")) {
      prep.setInt(1, teamNumber);
      prep.setInt(2, tournament);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final int runNumber = rs.getInt(1);
          return runNumber;
        } else {
          return 0;
        }
      }
    }

  }

  /**
   * Get the max performance run number completed for all teams in the
   * current tournament. This does not check the verified flag.
   *
   * @param connection database connection
   * @return the max run number or 0 if no performance runs have been completed
   * @throws SQLException on a database error
   */
  public static int maxPerformanceRunNumberCompleted(final Connection connection) throws SQLException {
    final int tournament = getCurrentTournament(connection);

    try (PreparedStatement prep = connection.prepareStatement("SELECT MAX(RunNumber) FROM Performance"
        + " WHERE Tournament = ?")) {
      prep.setInt(1, tournament);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final int runNumber = rs.getInt(1);
          return runNumber;
        } else {
          return 0;
        }
      }
    }

  }

  /**
   * Returns true if the score has been verified, i.e. double-checked.
   * 
   * @param connection database connection
   * @param tournament the tournament to work with
   * @param team the team number
   * @param runNumber the run number
   * @return if the run has been verified
   * @throws SQLException on a database error
   */
  public static boolean isVerified(final Connection connection,
                                   final int tournament,
                                   final Team team,
                                   final int runNumber)
      throws SQLException {
    return isVerified(connection, tournament, team.getTeamNumber(), runNumber);
  }

  /**
   * If team is not null, calls
   * {@link #performanceScoreExists(Connection, int, int, int)}, otherwise returns
   * false.
   * 
   * @param connection database connection
   * @param tournament the tournament to work with
   * @param team the team number
   * @param runNumber the run number
   * @return if the performance score exists
   * @throws SQLException on a database error
   */
  public static boolean performanceScoreExists(final Connection connection,
                                               final int tournament,
                                               final @Nullable Team team,
                                               final int runNumber)
      throws SQLException {
    if (null == team) {
      return false;
    } else {
      return performanceScoreExists(connection, tournament, team.getTeamNumber(), runNumber);
    }
  }

  /**
   * Convert {@link java.util.Date} to {@link java.sql.Time}.
   * 
   * @param date value to be converted
   * @return converted value
   */
  public static @PolyNull Time dateToTime(final @PolyNull Date date) {
    if (null == date) {
      return null;
    } else {
      return new Time(date.getTime());
    }
  }

  /**
   * Convert {@link java.sql.Time} to {@link java.util.Date}.
   * 
   * @param t value to be converted
   * @return {@link java.util.Date}
   */
  public static @PolyNull Date timeToDate(final @PolyNull Time t) {
    if (null == t) {
      return null;
    } else {
      return new Date(t.getTime());
    }
  }

  /**
   * Delete all subjective scores for the specified team in the
   * specified category.
   *
   * @param connection the database connection
   * @param categoryName the name of the category to delete scores from
   * @param teamNumber the team number
   * @param tournamentID the id of the tournament
   * @throws SQLException if a database error occurs
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Can't use variable param for table to modify")
  public static void deleteSubjectiveScores(final Connection connection,
                                            final String categoryName,
                                            final int teamNumber,
                                            final int tournamentID)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("DELETE FROM "
        + categoryName
        + " WHERE Tournament = ? AND TeamNumber = ?")) {
      prep.setInt(1, tournamentID);
      prep.setInt(2, teamNumber);
      prep.executeUpdate();
    }
  }
}
