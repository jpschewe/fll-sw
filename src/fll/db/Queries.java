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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.CategoryRank;
import fll.Team;
import fll.TeamRanking;
import fll.Tournament;
import fll.TournamentTeam;
import fll.Utilities;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
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
import net.mtu.eggplant.util.ComparisonUtils;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Does all of our queries.
 */
public final class Queries {

  private static final Logger LOGGER = LogUtils.getLogger();

  private Queries() {
    // no instances
  }

  /**
   * Compute the score group for a team. Normally this comes from the schedule,
   * but it may need to be computed off of the judges.
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Need to generate table name from category")
  public static String computeScoreGroupForTeam(final Connection connection,
                                                final int tournament,
                                                final String categoryName,
                                                final int teamNumber)
      throws SQLException {
    // otherwise build up the score group name based upon the judges
    final StringBuilder scoreGroup = new StringBuilder();
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT Judge FROM "
          + categoryName
          + " WHERE TeamNumber = ? AND Tournament = ? AND ComputedTotal IS NOT NULL ORDER BY Judge");
      prep.setInt(2, tournament);
      prep.setInt(1, teamNumber);
      rs = prep.executeQuery();
      boolean first = true;
      while (rs.next()) {
        if (!first) {
          scoreGroup.append("-");
        } else {
          first = false;
        }
        scoreGroup.append(rs.getString(1));
      }
      SQLFunctions.close(rs);
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
    return scoreGroup.toString();
  }

  /**
   * Compute the score groups that each team are in for a given category.
   * 
   * @param connection the connection to the database
   * @param tournament the tournament to work within
   * @param division the division to compute the score groups for
   * @param categoryName the database name of the category
   * @return Score groups. Map is name of score group to collection of teams in
   *         that score group
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category determines the table name")
  private static Map<String, Collection<Integer>> computeScoreGroups(final Connection connection,
                                                                     final int tournament,
                                                                     final String division,
                                                                     final String categoryName)
      throws SQLException {
    final Map<String, Collection<Integer>> scoreGroups = new HashMap<String, Collection<Integer>>();

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT DISTINCT Judges.station"
          + " FROM "
          + categoryName
          + ", Judges" //
          + " WHERE TeamNumber = ?" //
          + " AND Judges.Tournament = ?" //
          + " AND Judges.id = "
          + categoryName
          + ".Judge" //
          + " AND Judges.Tournament = "
          + categoryName
          + ".Tournament" //
          + " AND Judges.category = ?" //
          + " AND ComputedTotal IS NOT NULL");
      prep.setInt(2, tournament);
      prep.setString(3, categoryName);

      // foreach team, put the team in a score group
      for (final TournamentTeam team : Queries.getTournamentTeams(connection).values()) {
        // only show the teams for the division that we are looking at right
        // now
        if (division.equals(team.getAwardGroup())) {
          final int teamNum = team.getTeamNumber();
          final StringBuilder scoreGroup = new StringBuilder();
          prep.setInt(1, teamNum);
          rs = prep.executeQuery();
          boolean first = true;
          while (rs.next()) {
            if (!first) {
              scoreGroup.append("-");
            } else {
              first = false;
            }
            scoreGroup.append(rs.getString(1));
          }
          SQLFunctions.close(rs);

          final String scoreGroupStr = scoreGroup.toString();
          if (!scoreGroups.containsKey(scoreGroupStr)) {
            scoreGroups.put(scoreGroupStr, new LinkedList<Integer>());
          }
          scoreGroups.get(scoreGroupStr).add(teamNum);
        }
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }

    return scoreGroups;
  }

  /**
   * Get a map of teams for this tournament keyed on team number. Uses the table
   * TournamentTeams to determine which teams should be included.
   */
  public static Map<Integer, TournamentTeam> getTournamentTeams(final Connection connection) throws SQLException {
    return getTournamentTeams(connection, getCurrentTournament(connection));
  }

  /**
   * Get a map of teams for the specified tournament keyed on team number. Uses
   * the table TournamentTeams to determine which teams should be included.
   */
  public static Map<Integer, TournamentTeam> getTournamentTeams(final Connection connection,
                                                                final int tournamentID)
      throws SQLException {
    final SortedMap<Integer, TournamentTeam> tournamentTeams = new TreeMap<Integer, TournamentTeam>();
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("SELECT Teams.TeamNumber, Teams.Organization"//
          + ", Teams.TeamName"//
          + ", TournamentTeams.event_division" //
          + ", TournamentTeams.judging_station" //
          + " FROM Teams, TournamentTeams" //
          + " WHERE Teams.TeamNumber = TournamentTeams.TeamNumber"//
          + " AND TournamentTeams.Tournament = ?");
      prep.setInt(1, tournamentID);
      rs = prep.executeQuery();
      while (rs.next()) {
        final int teamNumber = rs.getInt("TeamNumber");
        final String org = rs.getString("Organization");
        final String name = rs.getString("TeamName");
        final String eventDivision = rs.getString("event_division");
        final String judgingStation = rs.getString("judging_station");

        final TournamentTeam team = new TournamentTeam(teamNumber, org, name, eventDivision, judgingStation);
        tournamentTeams.put(teamNumber, team);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
    return tournamentTeams;
  }

  /**
   * @see #getAwardGroups(Connection, int)
   * @see #getCurrentTournament(Connection)
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
   * @return the List of divisions. List of strings. Sorted by name.
   * @throws SQLException on a database error
   * @see #getCurrentTournament(Connection)
   */
  public static List<String> getAwardGroups(final Connection connection,
                                            final int tournament)
      throws SQLException {
    final List<String> list = new LinkedList<String>();

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT DISTINCT event_division FROM TournamentTeams WHERE Tournament = ? ORDER BY event_division");
      prep.setInt(1, tournament);
      rs = prep.executeQuery();
      while (rs.next()) {
        final String division = rs.getString(1);
        list.add(division);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
    return list;
  }

  /**
   * Get the list of team numbers that are in the specified event division.
   * 
   * @param connection
   * @param tournament
   * @param division
   * @throws SQLException
   */
  public static Set<Integer> getTeamNumbersInEventDivision(final Connection connection,
                                                           final int tournament,
                                                           final String division)
      throws SQLException {
    final Set<Integer> teamNumbers = new HashSet<>();
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT TeamNumber FROM TournamentTeams" //
          + " WHERE Tournament = ?" //
          + " AND event_division = ?");
      prep.setInt(1, tournament);
      prep.setString(2, division);
      rs = prep.executeQuery();
      while (rs.next()) {
        final int teamNumber = rs.getInt(1);
        teamNumbers.add(teamNumber);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
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
   */
  public static List<String> getJudgingStations(final Connection connection,
                                                final int tournament)
      throws SQLException {
    final List<String> result = new LinkedList<String>();

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT DISTINCT judging_station FROM TournamentTeams WHERE tournament = ? ORDER BY judging_station");
      prep.setInt(1, tournament);
      rs = prep.executeQuery();
      while (rs.next()) {
        final String station = rs.getString(1);
        result.add(station);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
    return result;
  }

  /**
   * Get the ranking of all teams in all categories.
   * 
   * @return Map with key of team number and value is the ranking information
   *         for that team.
   */
  public static Map<Integer, TeamRanking> getTeamRankings(final Connection connection,
                                                          final ChallengeDescription challengeDescription)
      throws SQLException {
    final Map<Integer, TeamRanking> teamRankings = new HashMap<Integer, TeamRanking>();
    final int tournament = getCurrentTournament(connection);
    final List<String> divisions = getAwardGroups(connection);

    final WinnerType winnerCriteria = challengeDescription.getWinner();
    final String ascDesc = winnerCriteria.getSortString();

    // find the performance ranking
    determinePerformanceRanking(connection, ascDesc, tournament, divisions, teamRankings);

    // find the subjective category rankings
    determineSubjectiveRanking(connection, ascDesc, tournament, divisions, challengeDescription, teamRankings);

    // find the overall ranking
    determineOverallRanking(connection, ascDesc, tournament, divisions, teamRankings);

    return teamRankings;
  }

  /**
   * Determine the subjective category ranking for all teams at a tournament.
   * 
   * @param connection
   * @param tournament
   * @param divisions
   * @param rankingMap
   * @throws SQLException
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Need to generate select statement")
  private static void determineSubjectiveRanking(final Connection connection,
                                                 final String ascDesc,
                                                 final int tournament,
                                                 final List<String> divisions,
                                                 final ChallengeDescription challengeDescription,
                                                 final Map<Integer, TeamRanking> teamRankings)
      throws SQLException {

    // cache the subjective categories title->dbname
    final Map<String, String> subjectiveCategories = new HashMap<String, String>();
    for (final SubjectiveScoreCategory cat : challengeDescription.getSubjectiveCategories()) {
      final String title = cat.getTitle();
      final String name = cat.getName();
      subjectiveCategories.put(title, name);
    }

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      for (final String division : divisions) {

        // foreach subjective category
        for (final Map.Entry<String, String> entry : subjectiveCategories.entrySet()) {
          final String categoryTitle = entry.getKey();
          final String categoryName = entry.getValue();

          final Map<String, Collection<Integer>> scoreGroups = Queries.computeScoreGroups(connection, tournament,
                                                                                          division, categoryName);

          // select from FinalScores
          for (final Map.Entry<String, Collection<Integer>> sgEntry : scoreGroups.entrySet()) {
            final Collection<Integer> teamScores = sgEntry.getValue();

            final String teamSelect = StringUtils.join(teamScores.iterator(), ", ");
            prep = connection.prepareStatement("SELECT Teams.TeamNumber,FinalScores."
                + categoryName //
                + " FROM Teams, FinalScores" //
                + " WHERE FinalScores.TeamNumber IN ( "
                + teamSelect
                + ")" //
                + " AND Teams.TeamNumber = FinalScores.TeamNumber" //
                + " AND FinalScores.Tournament = ?" //
                + " ORDER BY" //
                + " CASE when FinalScores."
                + categoryName
                + " IS NULL THEN 1 ELSE 0 END ASC" //
                + ",FinalScores."
                + categoryName
                + " "
                + ascDesc //
                + ",Teams.TeamNumber");

            prep.setInt(1, tournament);
            rs = prep.executeQuery();
            final String rankingGroup = String.format("award group %s judging group %s", division, sgEntry.getKey());

            processTeamRankings(teamRankings, categoryTitle, rankingGroup, rs);
          } // end foreach score group
        } // end foreach category
      } // end foreach division
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Determine the overall ranking for all teams at a tournament.
   * 
   * @param connection
   * @param tournament
   * @param divisions
   * @param rankingMap
   * @throws SQLException
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Need to compute sort order")
  private static void determineOverallRanking(final Connection connection,
                                              final String ascDesc,
                                              final int tournament,
                                              final List<String> divisions,
                                              final Map<Integer, TeamRanking> teamRankings)
      throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT Teams.TeamNumber, FinalScores.OverallScore" //
          + " FROM Teams,FinalScores,current_tournament_teams" //
          + " WHERE FinalScores.TeamNumber = Teams.TeamNumber" //
          + " AND FinalScores.Tournament = ?"//
          + " AND current_tournament_teams.event_division = ?" //
          + " AND current_tournament_teams.TeamNumber = Teams.TeamNumber" //
          + " ORDER BY" //
          + " CASE when FinalScores.OverallScore IS NULL THEN 1 ELSE 0 END ASC" //
          + ",FinalScores.OverallScore "
          + ascDesc //
          + ",Teams.TeamNumber");
      prep.setInt(1, tournament);
      for (final String division : divisions) {
        prep.setString(2, division);
        rs = prep.executeQuery();
        final String rankingGroup = String.format("award group %s", division);
        processTeamRankings(teamRankings, CategoryRank.OVERALL_CATEGORY_NAME, rankingGroup, rs);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Process the team rankings from the executed query. It is assumed that the
   * query returns first an int that is the team number and then a double that
   * is the score. <code>teamMap</code> is populated with the data. The
   * ResultSet is closed by this function.
   */
  private static void processTeamRankings(final Map<Integer, TeamRanking> teamRankings,
                                          final String categoryTitle,
                                          final String rankingGroup,
                                          final ResultSet rs)
      throws SQLException {
    final List<Integer> ranks = new LinkedList<Integer>();
    final List<Integer> teams = new LinkedList<Integer>();

    int numTeams = 0;
    int tieRank = 1;
    int rank = 1;
    double prevScore = Double.NaN;
    while (rs.next()) {
      final int team = rs.getInt(1);
      double score = rs.getDouble(2);
      teams.add(team);
      if (rs.wasNull()) {
        ranks.add(CategoryRank.NO_SHOW_RANK);
      } else if (Math.abs(score
          - prevScore) < 0.001) {
        // 3 decimal places should be considered equal
        ranks.add(tieRank);
      } else {
        tieRank = rank;
        ranks.add(rank);
      }

      // setup for next round
      prevScore = score;

      // increment rank counter
      ++rank;
      ++numTeams;
    } // end score group rank

    for (int i = 0; i < ranks.size(); ++i) {
      final CategoryRank catRank = new CategoryRank(rankingGroup, categoryTitle, ranks.get(i), numTeams);
      TeamRanking teamRank = teamRankings.get(teams.get(i));
      if (null == teamRank) {
        teamRank = new TeamRanking(teams.get(i));
        teamRankings.put(teams.get(i), teamRank);
      }
      teamRank.setRankForCategory(categoryTitle, catRank);
    }

    SQLFunctions.close(rs);
  }

  /**
   * Determine the performance ranking for all teams at a tournament.
   * 
   * @param connection
   * @param tournament
   * @param divisions
   * @param rankingMap
   * @throws SQLException
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Need to compute sort order")
  private static void determinePerformanceRanking(final Connection connection,
                                                  final String ascDesc,
                                                  final int tournament,
                                                  final List<String> divisions,
                                                  final Map<Integer, TeamRanking> teamRankings)
      throws SQLException {

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT Teams.TeamNumber, FinalScores.performance" //
          + " FROM Teams,FinalScores,current_tournament_teams" //
          + " WHERE FinalScores.TeamNumber = Teams.TeamNumber" //
          + " AND FinalScores.Tournament = ?" //
          + " AND current_tournament_teams.event_division = ?" //
          + " AND current_tournament_teams.TeamNumber = Teams.TeamNumber"//
          + " ORDER BY" //
          + " CASE when FinalScores.performance IS NULL THEN 1 ELSE 0 END ASC" //
          + ",FinalScores.performance "
          + ascDesc //
          + ",Teams.TeamNumber");

      prep.setInt(1, tournament);
      for (final String division : divisions) {
        prep.setString(2, division);
        rs = prep.executeQuery();
        final String rankingGroup = String.format("award group %s", division);
        processTeamRankings(teamRankings, CategoryRank.PERFORMANCE_CATEGORY_NAME, rankingGroup, rs);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Figure out the next run number for teamNumber. Does not ignore unverified
   * scores.
   */
  public static int getNextRunNumber(final Connection connection,
                                     final int teamNumber)
      throws SQLException {
    final int currentTournament = getCurrentTournament(connection);
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT COUNT(TeamNumber) FROM Performance WHERE Tournament = ?"
          + " AND TeamNumber = ?");
      prep.setInt(1, currentTournament);
      prep.setInt(2, teamNumber);
      rs = prep.executeQuery();
      final int runNumber;
      if (rs.next()) {
        runNumber = rs.getInt(1);
      } else {
        runNumber = 0;
      }
      return runNumber
          + 1;
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Figure out the highest run number a team has completed. This should be the
   * same as next run number -1, but sometimes we get non-consecutive runs in
   * and this just finds the max run number. Does not ignore unverified scores.
   */
  public static int getMaxRunNumber(final Connection connection,
                                    final int teamNumber)
      throws SQLException {
    final int currentTournament = getCurrentTournament(connection);
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT MAX(RunNumber) FROM Performance WHERE Tournament = ?"
          + " AND TeamNumber = ?");
      prep.setInt(1, currentTournament);
      prep.setInt(2, teamNumber);
      rs = prep.executeQuery();
      final int runNumber;
      if (rs.next()) {
        runNumber = rs.getInt(1);
      } else {
        runNumber = 0;
      }
      return runNumber;
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Insert or update a performance score.
   * 
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
    final int teamNumber = Utilities.INTEGER_NUMBER_FORMAT_INSTANCE.parse(teamNumberStr).intValue();

    final String runNumberStr = request.getParameter("RunNumber");
    if (null == runNumberStr) {
      throw new RuntimeException("Missing parameter: RunNumber");
    }
    final int runNumber = Utilities.INTEGER_NUMBER_FORMAT_INSTANCE.parse(runNumberStr).intValue();

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
    for (final AbstractGoal element : performanceElement.getGoals()) {
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
    if (teamScore.getRunNumber() > numSeedingRounds) {
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
    } else {
      tournament.recordPerformanceSeedingModified(connection);
    }

    // notify that there may be more runs to verify
    UnverifiedRunsWebSocket.notifyToUpdate();
  }

  public static boolean isThirdPlaceEnabled(final Connection connection,
                                            final int tournament,
                                            final String division)
      throws SQLException {
    final int finalRound = getNumPlayoffRounds(connection, tournament, division);

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT count(*) FROM PlayoffData" //
          + " WHERE Tournament= ?" //
          + " AND event_division= ?" //
          + " AND PlayoffRound= ?");
      prep.setInt(1, tournament);
      prep.setString(2, division);
      prep.setInt(3, finalRound);
      rs = prep.executeQuery();
      if (rs.next()) {
        return rs.getInt(1) == 4;
      } else {
        return false;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Update a performance score in the database. All of the values are expected
   * to be in request.
   * 
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
    final int teamNumber = Utilities.INTEGER_NUMBER_FORMAT_INSTANCE.parse(teamNumberStr).intValue();

    final String runNumberStr = request.getParameter("RunNumber");
    if (null == runNumberStr) {
      throw new FLLRuntimeException("Missing parameter: RunNumber");
    }
    final int runNumber = Utilities.INTEGER_NUMBER_FORMAT_INSTANCE.parse(runNumberStr).intValue();

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
    for (final AbstractGoal element : performanceElement.getGoals()) {
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
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      numRowsUpdated = stmt.executeUpdate(sql.toString());
    } finally {
      SQLFunctions.close(stmt);
    }

    if (numRowsUpdated > 0) {
      // Check if we need to update the PlayoffData table
      final int numSeedingRounds = TournamentParameters.getNumSeedingRounds(connection, currentTournament);
      if (runNumber > numSeedingRounds) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Updating playoff score from updatePerformanceScore");
        }

        final boolean verified = "1".equals(request.getParameter("Verified"));
        updatePlayoffScore(connection, verified, currentTournament, winnerCriteria, performanceElement,
                           tiebreakerElement, teamNumber, runNumber, teamScore);
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
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
          prep = connection.prepareStatement("SELECT TeamNumber FROM Performance" //
              + " WHERE TeamNumber = ?" //
              + " AND RunNumber > ?" //
              + " AND Tournament = ?");
          if (newWinner != null
              && oldWinnerTeamNumber != newWinner.getTeamNumber()) {
            // This score update changes the result of the match, so make sure
            // no other scores exist in later round for either of these 2 teams.
            if (getPlayoffTableLineNumber(connection, currentTournament, teamNumber, runNumber
                + 1) > 0) {
              prep.setInt(1, teamNumber);
              prep.setInt(2, runNumber);
              prep.setInt(3, currentTournament);
              rs = prep.executeQuery();
              if (rs.next()) {
                throw new FLLRuntimeException("Unable to update score for team number "
                    + teamNumber
                    + " in performance run "
                    + runNumber
                    + " because that team has scores entered in subsequent playoff rounds which would become inconsistent. "
                    + "Delete those scores and then you may update this score.");
              }
              SQLFunctions.close(rs);
              rs = null;
            }
            if (getPlayoffTableLineNumber(connection, currentTournament, siblingTeam, runNumber
                + 1) > 0) {
              prep.setInt(1, siblingTeam);
              prep.setInt(2, runNumber);
              prep.setInt(3, currentTournament);
              rs = prep.executeQuery();
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
              SQLFunctions.close(rs);
              rs = null;
            }
          }

        } finally {
          SQLFunctions.close(rs);
          SQLFunctions.close(prep);
        }

        // If the second-check flag is NO or the opposing team is not
        // verified, we set the match "winner" (possibly back) to NULL.
        if (!verified
            || !(Queries.performanceScoreExists(connection, currentTournament, teamB, runNumber)
                && Queries.isVerified(connection, currentTournament, teamB, runNumber))) {
          removePlayoffScore(connection, division, currentTournament, runNumber, ptLine);
        } else {
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
   * Delete a performance score in the database. All of the values are expected
   * to be in request.
   * 
   * @throws RuntimeException if a parameter is missing or if the playoff meta
   *           data would become inconsistent due to the deletion.
   * @throws ParseException if the numbers in the request can't be parsed as
   *           numbers
   */
  @SuppressFBWarnings(value = "OBL_UNSATISFIED_OBLIGATION", justification = "Bug in findbugs - ticket:2924739")
  public static void deletePerformanceScore(final Connection connection,
                                            final HttpServletRequest request)
      throws SQLException, RuntimeException, ParseException {
    final int currentTournament = getCurrentTournament(connection);

    final String teamNumberStr = request.getParameter("TeamNumber");
    if (null == teamNumberStr) {
      throw new RuntimeException("Missing parameter: TeamNumber");
    }
    final int teamNumber = Utilities.INTEGER_NUMBER_FORMAT_INSTANCE.parse(teamNumberStr).intValue();

    final String runNumber = request.getParameter("RunNumber");
    if (null == runNumber) {
      throw new RuntimeException("Missing parameter: RunNumber");
    }
    final int irunNumber = Utilities.INTEGER_NUMBER_FORMAT_INSTANCE.parse(runNumber).intValue();
    final int numSeedingRounds = TournamentParameters.getNumSeedingRounds(connection, currentTournament);

    final int dbLine = getPlayoffTableLineNumber(connection, currentTournament, teamNumber, irunNumber);
    final String division = Playoff.getPlayoffDivision(connection, currentTournament, teamNumber, irunNumber);

    // Check if we need to update the PlayoffData table
    try (PreparedStatement prep = connection.prepareStatement("SELECT TeamNumber FROM Performance" //
        + " WHERE TeamNumber = ?" //
        + " AND RunNumber > ?" //
        + " AND Tournament = ?")) {

      if (irunNumber > numSeedingRounds) {
        if (dbLine > 0) {
          final int siblingDbLine = dbLine
              % 2 == 0 ? dbLine
                  - 1
                  : dbLine
                      + 1;
          final int siblingTeam = getTeamNumberByPlayoffLine(connection, currentTournament, division, siblingDbLine,
                                                             irunNumber);

          if (siblingTeam != Team.NULL_TEAM_NUMBER) {
            // See if either teamNumber or siblingTeam has a score entered in
            // subsequent rounds
            if (getPlayoffTableLineNumber(connection, currentTournament, teamNumber, irunNumber
                + 1) > 0) {
              prep.setInt(1, teamNumber);
              prep.setInt(2, irunNumber);
              prep.setInt(3, currentTournament);
              try (ResultSet rs = prep.executeQuery()) {
                if (rs.next()) {
                  throw new RuntimeException("Unable to delete score for team number "
                      + teamNumber
                      + " in performance run "
                      + irunNumber
                      + " because that team "
                      + " has scores in subsequent playoff rounds which would become inconsistent. "
                      + "Delete those scores and then you may delete this score.");
                }
              }
            }
            if (getPlayoffTableLineNumber(connection, currentTournament, siblingTeam, irunNumber
                + 1) > 0) {
              prep.setInt(1, siblingTeam);
              prep.setInt(2, irunNumber);
              prep.setInt(3, currentTournament);
              try (ResultSet rs = prep.executeQuery()) {
                if (rs.next()) {
                  throw new RuntimeException("Unable to delete score for team number "
                      + teamNumber
                      + " in performance run "
                      + irunNumber
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
      deletePrep.setInt(3, irunNumber);

      deletePrep.executeUpdate();

      if (irunNumber > numSeedingRounds) {
        final Document document = GlobalParameters.getChallengeDocument(connection);
        final ChallengeDescription description = new ChallengeDescription(document.getDocumentElement());
        final PerformanceScoreCategory performance = description.getPerformance();
        final ScoreType performanceScoreType = performance.getScoreType();

        final Team team = Team.getTeamFromDatabase(connection, teamNumber);

        // if the delete of the performance score succeeded it's save to remove the
        // information from the playoff table
        removePlayoffScore(connection, division, currentTournament, irunNumber, dbLine);

        // update the display for the deleted score
        H2HUpdateWebSocket.updateBracket(connection, performanceScoreType, division, team, irunNumber);
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

    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("UPDATE PlayoffData" //
          + " SET Team = ?" //
          + ", Printed = ?" //
          + " WHERE event_division = ?" //
          + " AND Tournament = ?" //
          + " AND run_number = ?" //
          + " AND LineNumber = ?");
      prep.setInt(1, team.getTeamNumber());
      prep.setBoolean(2, false);
      prep.setString(3, division);
      prep.setInt(4, currentTournament);
      prep.setInt(5, runNumber);
      prep.setInt(6, lineNumber);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
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

    final Document document = GlobalParameters.getChallengeDocument(connection);
    final ChallengeDescription description = new ChallengeDescription(document.getDocumentElement());
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
   * @return the updates to send to the display
   * @throws SQLException
   */
  public static Collection<BracketUpdate> getH2HBracketData(final Connection connection,
                                                            final int currentTournament,
                                                            final String bracketName,
                                                            final int firstPlayoffRound,
                                                            final int lastPlayoffRound)
      throws SQLException {
    final int maxPlayoffRound = Playoff.getMaxPlayoffRound(connection, currentTournament, bracketName);

    final Document document = GlobalParameters.getChallengeDocument(connection);
    final ChallengeDescription challengeDescription = new ChallengeDescription(document.getDocumentElement());
    final ScoreType performanceScoreType = challengeDescription.getPerformance().getScoreType();

    final Collection<BracketUpdate> updates = new LinkedList<>();
    try (
        final PreparedStatement prep = connection.prepareStatement("SELECT PlayoffData.PlayoffRound, PlayoffData.LineNumber, Teams.TeamNumber, Teams.TeamName, Performance.ComputedTotal, Performance.Verified, PlayoffData.AssignedTable, Performance.NoShow" //
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

      try (final ResultSet rs = prep.executeQuery()) {
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
   * @param teamNumber the team's number
   * @return the event division for the team
   * @throws SQLException on a database error
   * @throws RuntimeException if <code>teamNumber</code> cannot be found in
   *           TournamenTeams for the current tournament
   */
  public static String getEventDivision(final Connection connection,
                                        final int teamNumber)
      throws SQLException, RuntimeException {
    return getEventDivision(connection, teamNumber, getCurrentTournament(connection));
  }

  /**
   * Get the division that a team is in for the specified tournament.
   * 
   * @param teamNumber the team's number
   * @param tournamentID ID of tournament
   * @return the event division for the team or null if the team cannot be found
   *         in the list of tournament teams
   * @throws SQLException on a database error
   */
  public static String getEventDivision(final Connection connection,
                                        final int teamNumber,
                                        final int tournamentID)
      throws SQLException, RuntimeException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT event_division FROM TournamentTeams WHERE TeamNumber = ? AND Tournament = ?");
      prep.setInt(1, teamNumber);
      prep.setInt(2, tournamentID);
      rs = prep.executeQuery();
      if (rs.next()) {
        return rs.getString(1);
      } else {
        return null;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Get the judging group that a team is in for the specified tournament.
   * 
   * @param teamNumber the team's number
   * @param tournamentID ID of tournament
   * @return the judging group for the team or null if not found
   * @throws SQLException on a database error
   */
  public static String getJudgingGroup(final Connection connection,
                                       final int teamNumber,
                                       final int tournamentID)
      throws SQLException, RuntimeException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT judging_station FROM TournamentTeams WHERE TeamNumber = ? AND Tournament = ?");
      prep.setInt(1, teamNumber);
      prep.setInt(2, tournamentID);
      rs = prep.executeQuery();
      if (rs.next()) {
        return rs.getString(1);
      } else {
        return null;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Set judging station for a team.
   */
  public static void setJudgingGroup(final Connection connection,
                                     final int teamNumber,
                                     final int tournament,
                                     final String judgingStation)
      throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("UPDATE TournamentTeams SET judging_station = ? WHERE TeamNumber = ? AND Tournament = ?");
      prep.setString(1, judgingStation);
      prep.setInt(2, teamNumber);
      prep.setInt(3, tournament);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
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

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT TeamNumber,Count(*) FROM "
          + view //
          + " WHERE Tournament = ? GROUP BY TeamNumber" //
          + " HAVING Count(*) < ?");
      prep.setInt(1, currentTournament);
      prep.setInt(2, TournamentParameters.getNumSeedingRounds(connection, currentTournament));

      rs = prep.executeQuery();
      return collectTeamsFromQuery(tournamentTeams, rs);
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * The {@link ResultSet} contains a single parameter that is the team number.
   * These numbers are mapped to team objects through
   * <code>tournamentTeams</code>.
   * 
   * @throws RuntimeException if a team couldn't be found in the map
   */
  private static List<Team> collectTeamsFromQuery(final Map<Integer, ? extends Team> tournamentTeams,
                                                  final ResultSet rs)
      throws SQLException {
    final List<Team> list = new LinkedList<Team>();
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
   * @throws SQLException on a database error
   * @throws RuntimeException if a team can't be found in tournamentTeams
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Need to choose ascending or descending order based upon winner criteria")
  public static List<Team> getPlayoffSeedingOrder(final Connection connection,
                                                  final WinnerType winnerCriteria,
                                                  final Collection<? extends Team> teams)
      throws SQLException, RuntimeException {

    final List<Integer> teamNumbers = new LinkedList<Integer>();
    for (final Team t : teams) {
      teamNumbers.add(t.getTeamNumber());
    }

    final String teamNumbersStr = StringUtils.join(teamNumbers, ",");

    final List<Team> retval = new ArrayList<Team>();

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT performance_seeding_max.TeamNumber, performance_seeding_max.Score as score, RAND() as random"
          + " FROM performance_seeding_max, current_tournament_teams" //
          + " WHERE score IS NOT NULL" // exclude no shows
          + " AND performance_seeding_max.TeamNumber = current_tournament_teams.TeamNumber" //
          + " AND current_tournament_teams.TeamNumber IN ( "
          + teamNumbersStr
          + " )" //
          + " ORDER BY score "
          + winnerCriteria.getSortString() //
          + ", performance_seeding_max.average "
          + winnerCriteria.getSortString() //
          + ", random");

      rs = prep.executeQuery();
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
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
    return retval;
  }

  /**
   * Get the current tournament from the database.
   * 
   * @return the tournament, or DUMMY if not set. There should always be a DUMMY
   *         tournament in the Tournaments table. If DUMMY is returned the
   *         current tournament is set to 'DUMMY'
   */
  public static String getCurrentTournamentName(final Connection connection) throws SQLException {
    final int currentTournamentID = getCurrentTournament(connection);
    final Tournament currentTournament = Tournament.findTournamentByID(connection, currentTournamentID);
    return currentTournament.getName();
  }

  /**
   * Get the current tournament from the database.
   * 
   * @return the tournament ID
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
   * @param teamNumber team to delete
   * @param connection connection to database, needs delete privileges
   * @throws SQLException on an error talking to the database
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category name determines table")
  public static void deleteTeam(final int teamNumber,
                                final ChallengeDescription description,
                                final Connection connection)
      throws SQLException {
    PreparedStatement prep = null;
    final boolean autoCommit = connection.getAutoCommit();
    try {
      connection.setAutoCommit(false);

      // delete from TournamentTeams
      prep = connection.prepareStatement("DELETE FROM TournamentTeams WHERE TeamNumber = ?");
      prep.setInt(1, teamNumber);
      prep.executeUpdate();
      SQLFunctions.close(prep);
      prep = null;

      // delete from subjective categories
      for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
        final String name = category.getName();
        prep = connection.prepareStatement("DELETE FROM "
            + name
            + " WHERE TeamNumber = ?");
        prep.setInt(1, teamNumber);
        prep.executeUpdate();
        SQLFunctions.close(prep);
        prep = null;
      }

      // delete from Performance
      prep = connection.prepareStatement("DELETE FROM Performance WHERE TeamNumber = ?");
      prep.setInt(1, teamNumber);
      prep.executeUpdate();
      SQLFunctions.close(prep);
      prep = null;

      // delete from FinalScores
      prep = connection.prepareStatement("DELETE FROM FinalScores WHERE TeamNumber = ?");
      prep.setInt(1, teamNumber);
      prep.executeUpdate();
      SQLFunctions.close(prep);
      prep = null;

      // delete from schedule
      prep = connection.prepareStatement("DELETE FROM sched_perf_rounds WHERE team_number = ?");
      prep.setInt(1, teamNumber);
      prep.executeUpdate();
      SQLFunctions.close(prep);
      prep = null;
      prep = connection.prepareStatement("DELETE FROM sched_subjective WHERE team_number = ?");
      prep.setInt(1, teamNumber);
      prep.executeUpdate();
      SQLFunctions.close(prep);
      prep = null;
      prep = connection.prepareStatement("DELETE FROM schedule WHERE team_number = ?");
      prep.setInt(1, teamNumber);
      prep.executeUpdate();
      SQLFunctions.close(prep);
      prep = null;

      // delete from Teams
      prep = connection.prepareStatement("DELETE FROM Teams WHERE TeamNumber = ?");
      prep.setInt(1, teamNumber);
      prep.executeUpdate();
      SQLFunctions.close(prep);
      prep = null;

      connection.commit();
    } finally {
      try {
        connection.setAutoCommit(autoCommit);
      } catch (final SQLException e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(e, e);
        }
      }
      SQLFunctions.close(prep);
    }
  }

  /**
   * Defaults to current tournament.
   * 
   * @see #updateScoreTotals(ChallengeDescription, Connection, int)
   */
  public static void updateScoreTotals(final ChallengeDescription description,
                                       final Connection connection)
      throws SQLException {
    final int tournament = getCurrentTournament(connection);
    updateScoreTotals(description, connection, tournament);
  }

  /**
   * Total the scores in the database for the specified tournament.
   * 
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
   * 
   * @param connection
   * @throws SQLException
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category determines table name")
  private static void updateSubjectiveScoreTotals(final ChallengeDescription description,
                                                  final Connection connection,
                                                  final int tournament)
      throws SQLException {
    PreparedStatement updatePrep = null;
    PreparedStatement selectPrep = null;
    ResultSet rs = null;
    try {
      // Subjective ---
      for (final SubjectiveScoreCategory subjectiveElement : description.getSubjectiveCategories()) {
        final String categoryName = subjectiveElement.getName();

        // build up the SQL
        updatePrep = connection.prepareStatement("UPDATE "//
            + categoryName //
            + " SET ComputedTotal = ? WHERE TeamNumber = ? AND Tournament = ? AND Judge = ?");
        selectPrep = connection.prepareStatement("SELECT * FROM " //
            + categoryName //
            + " WHERE Tournament = ?");
        selectPrep.setInt(1, tournament);
        updatePrep.setInt(3, tournament);
        rs = selectPrep.executeQuery();
        while (rs.next()) {
          final int teamNumber = rs.getInt("TeamNumber");
          final TeamScore teamScore = new DatabaseTeamScore(teamNumber, rs);
          final double computedTotal;
          if (teamScore.isNoShow()) {
            computedTotal = Double.NaN;
          } else {
            computedTotal = subjectiveElement.evaluate(teamScore);
          }
          if (Double.isNaN(computedTotal)) {
            updatePrep.setNull(1, Types.DOUBLE);
          } else {
            updatePrep.setDouble(1, computedTotal);
          }
          updatePrep.setInt(2, teamNumber);
          final String judge = rs.getString("Judge");
          updatePrep.setString(4, judge);
          updatePrep.executeUpdate();
        }
        rs.close();
        updatePrep.close();
        selectPrep.close();
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(updatePrep);
      SQLFunctions.close(selectPrep);
    }
  }

  /**
   * Compute the total scores for all entered performance scores. Uses both
   * verified and unverified scores.
   * 
   * @param connection connection to the database
   * @param tournament the tournament to update scores for.
   * @throws SQLException
   */
  private static void updatePerformanceScoreTotals(final ChallengeDescription description,
                                                   final Connection connection,
                                                   final int tournament)
      throws SQLException {
    PreparedStatement updatePrep = null;
    PreparedStatement selectPrep = null;
    ResultSet rs = null;
    try {

      // build up the SQL
      updatePrep = connection.prepareStatement("UPDATE Performance SET ComputedTotal = ? WHERE TeamNumber = ? AND Tournament = ? AND RunNumber = ?");
      updatePrep.setInt(3, tournament);
      selectPrep = connection.prepareStatement("SELECT * FROM Performance WHERE Tournament = ?");
      selectPrep.setInt(1, tournament);

      final PerformanceScoreCategory performanceElement = description.getPerformance();
      final double minimumPerformanceScore = performanceElement.getMinimumScore();
      rs = selectPrep.executeQuery();
      while (rs.next()) {
        if (!rs.getBoolean("Bye")) {
          final int teamNumber = rs.getInt("TeamNumber");
          final int runNumber = rs.getInt("RunNumber");
          final TeamScore teamScore = new DatabaseTeamScore(teamNumber, runNumber, rs);
          final double computedTotal;
          if (teamScore.isNoShow()) {
            computedTotal = Double.NaN;
          } else {
            computedTotal = performanceElement.evaluate(teamScore);
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
      rs.close();
      updatePrep.close();
      selectPrep.close();
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(updatePrep);
      SQLFunctions.close(selectPrep);
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
    PreparedStatement prep = null;
    ResultSet rs = null;
    final Collection<Integer> tournaments = new LinkedList<>();
    try {
      prep = connection.prepareStatement("SELECT Tournament" //
          + " FROM TournamentTeams" //
          + " WHERE TeamNumber = ?");
      prep.setInt(1, teamNumber);
      rs = prep.executeQuery();
      while (rs.next()) {
        final int id = rs.getInt(1);
        tournaments.add(id);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
    return tournaments;
  }

  /**
   * Get the current tournament that this team is at.
   */
  public static int getTeamCurrentTournament(final Connection connection,
                                             final int teamNumber)
      throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT Tournaments.tournament_id, Tournaments.NextTournament" //
          + " FROM TournamentTeams, Tournaments" //
          + " WHERE TournamentTeams.TeamNumber = ?" //
          + " AND TournamentTeams.Tournament = Tournaments.tournament_id");
      prep.setInt(1, teamNumber);
      rs = prep.executeQuery();
      final List<Integer> tournaments = new LinkedList<Integer>();
      final List<Integer> nextTournaments = new LinkedList<Integer>();
      while (rs.next()) {
        final int tournament = rs.getInt(1);
        if (rs.wasNull()) {
          tournaments.add(null);
        } else {
          tournaments.add(tournament);
        }
        final int next = rs.getInt(2);
        if (rs.wasNull()) {
          nextTournaments.add(null);
        } else {
          nextTournaments.add(next);
        }
      }

      final Iterator<Integer> iter = nextTournaments.iterator();
      for (int i = 0; iter.hasNext(); i++) {
        final Integer nextTournament = iter.next();
        if (null == nextTournament) {
          // if no next tournament then this must be the current one since a
          // team can't advance any further.
          return tournaments.get(i);
        } else if (!tournaments.contains(nextTournament)) {
          // team hasn't advanced past this tournament yet
          return tournaments.get(i);
        }
      }

      LOGGER.error("getTeamCurrentTournament - Cannot determine current tournament for team: "
          + teamNumber
          + " tournamentNames: "
          + tournaments
          + " nextTournaments: "
          + nextTournaments
          + " - using DUMMY tournament as default");
      final Tournament dummyTournament = Tournament.findTournamentByName(connection, GenerateDB.DUMMY_TOURNAMENT_NAME);
      if (null == dummyTournament) {
        throw new FLLInternalException("Dummy tournament doesn't exist");
      }
      return dummyTournament.getTournamentID();
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Set the judging station for a given team at the specified tournament.
   * 
   * @param connection db connection
   * @param teamNumber the team's number
   * @param tournamentID the tournament
   * @param judgingStation the new judging station
   * @return true if the update occurrred, false if the team isn't in the
   *         tournament
   */
  public static boolean updateTeamJudgingGroups(final Connection connection,
                                                final int teamNumber,
                                                final int tournamentID,
                                                final String judgingStation)
      throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("UPDATE TournamentTeams SET judging_station = ? WHERE TeamNumber = ? AND Tournament = ?");
      prep.setString(1, judgingStation);
      prep.setInt(2, teamNumber);
      prep.setInt(3, tournamentID);
      return prep.executeUpdate() > 0;
    } finally {
      SQLFunctions.close(prep);
    }
  }

  /**
   * Delete all record of a team from a tournament. This includes the scores and
   * the TournamentTeams table.
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category determines table name")
  public static void deleteTeamFromTournament(final Connection connection,
                                              final ChallengeDescription description,
                                              final int teamNumber,
                                              final int currentTournament)
      throws SQLException {
    PreparedStatement prep = null;
    try {
      // delete from subjective categories
      for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
        final String name = category.getName();
        prep = connection.prepareStatement("DELETE FROM "
            + name
            + " WHERE TeamNumber = ? AND Tournament = ?");
        prep.setInt(1, teamNumber);
        prep.setInt(2, currentTournament);
        prep.executeUpdate();
        SQLFunctions.close(prep);
      }

      // delete from Performance
      prep = connection.prepareStatement("DELETE FROM Performance WHERE TeamNumber = ? AND Tournament = ?");
      prep.setInt(1, teamNumber);
      prep.setInt(2, currentTournament);
      prep.executeUpdate();
      SQLFunctions.close(prep);

      // delete from FinalScores
      prep = connection.prepareStatement("DELETE FROM FinalScores WHERE TeamNumber = ? AND Tournament = ?");
      prep.setInt(1, teamNumber);
      prep.setInt(2, currentTournament);
      prep.executeUpdate();
      SQLFunctions.close(prep);

      // delete from PlayoffData
      prep = connection.prepareStatement("DELETE FROM PlayoffData WHERE Team = ? AND Tournament = ?");
      prep.setInt(1, teamNumber);
      prep.setInt(2, currentTournament);
      prep.executeUpdate();
      SQLFunctions.close(prep);

      // delete from schedule
      prep = connection.prepareStatement("DELETE FROM sched_perf_rounds WHERE team_number = ? AND tournament = ?");
      prep.setInt(1, teamNumber);
      prep.setInt(2, currentTournament);
      prep.executeUpdate();
      SQLFunctions.close(prep);
      prep = connection.prepareStatement("DELETE FROM sched_subjective WHERE team_number = ? AND tournament = ?");
      prep.setInt(1, teamNumber);
      prep.setInt(2, currentTournament);
      prep.executeUpdate();
      SQLFunctions.close(prep);
      prep = connection.prepareStatement("DELETE FROM schedule WHERE team_number = ? AND tournament = ?");
      prep.setInt(1, teamNumber);
      prep.setInt(2, currentTournament);
      prep.executeUpdate();
      SQLFunctions.close(prep);

      // delete from TournamentTeams
      prep = connection.prepareStatement("DELETE FROM TournamentTeams WHERE TeamNumber = ? AND Tournament = ?");
      prep.setInt(1, teamNumber);
      prep.setInt(2, currentTournament);
      prep.executeUpdate();
      SQLFunctions.close(prep);

    } finally {
      SQLFunctions.close(prep);
    }

  }

  /**
   * Get the previous tournament for this team, given the current tournament.
   * 
   * @param connection the database connection
   * @param teamNumber the team number
   * @param currentTournament the current tournament to use to find the previous
   *          tournament, generally this is the return value of
   *          getTeamCurrentTournament
   * @return the tournament ID, or null if no such tournament exists
   * @see #getTeamCurrentTournament(Connection, int)
   */
  public static Integer getTeamPrevTournament(final Connection connection,
                                              final int teamNumber,
                                              final int currentTournament)
      throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT Tournaments.tournament_id" //
          + " FROM TournamentTeams, Tournaments" //
          + " WHERE TournamentTeams.TeamNumber = ?" //
          + " AND TournamentTeams.Tournament = Tournaments.tournament_id" //
          + " AND Tournaments.NextTournament = ?");
      prep.setInt(1, teamNumber);
      prep.setInt(2, currentTournament);
      rs = prep.executeQuery();
      if (rs.next()) {
        return rs.getInt(1);
      } else {
        return null;
      }

    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }

  }

  /**
   * Add a team to the database.
   * 
   * @return null on success, the name of the other team with the same team
   *         number on an error
   * @throws FLLRuntimeException if the team number is an internal team number
   */
  public static String addTeam(final Connection connection,
                               final int number,
                               final String name,
                               final String organization)
      throws SQLException {
    if (Team.isInternalTeamNumber(number)) {
      throw new FLLRuntimeException("Cannot create team with an internal number: "
          + number);
    }

    // TODO this should probably be in a transaction as the insert depends on
    // the same state as the select

    // need to check for duplicate teamNumber
    try (
        final PreparedStatement checkDuplicate = connection.prepareStatement("SELECT TeamName FROM Teams WHERE TeamNumber = ?")) {
      checkDuplicate.setInt(1, number);
      try (final ResultSet rs = checkDuplicate.executeQuery()) {
        if (rs.next()) {
          final String dup = rs.getString(1);
          return dup;
        }
      }
    }

    try (
        final PreparedStatement insert = connection.prepareStatement("INSERT INTO Teams (TeamName, Organization, TeamNumber) VALUES (?, ?, ?)")) {
      insert.setString(1, name);
      insert.setString(2, organization);
      insert.setInt(3, number);
      insert.executeUpdate();
    }

    return null;
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
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("INSERT INTO TournamentTeams (Tournament, TeamNumber, event_division, judging_station) VALUES (?, ?, ?, ?)");
      prep.setInt(1, tournament);
      prep.setInt(2, teamNumber);
      prep.setString(3, eventDivision);
      prep.setString(4, judgingStation);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }

  }

  /**
   * Set event division for a team.
   */
  public static void setEventDivision(final Connection connection,
                                      final int teamNumber,
                                      final int tournament,
                                      final String eventDivision)
      throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("UPDATE TournamentTeams SET event_division = ? WHERE TeamNumber = ? AND Tournament = ?");
      prep.setString(1, eventDivision);
      prep.setInt(2, teamNumber);
      prep.setInt(3, tournament);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  /**
   * Update a team in the database.
   */
  public static void updateTeam(final Connection connection,
                                final int number,
                                final String name,
                                final String organization)
      throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("UPDATE Teams SET TeamName = ?, Organization = ? WHERE TeamNumber = ?");
      prep.setString(1, name);
      prep.setString(2, organization);
      prep.setInt(3, number);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  /**
   * Update a team event division.
   */
  public static void updateTeamEventDivision(final Connection connection,
                                             final int number,
                                             final int tournamentID,
                                             final String eventDivision)
      throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("UPDATE TournamentTeams SET event_division = ? WHERE TeamNumber = ? AND Tournament = ?");
      prep.setString(1, eventDivision);
      prep.setInt(2, number);
      prep.setInt(3, tournamentID);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  /**
   * Update a team name.
   */
  public static void updateTeamName(final Connection connection,
                                    final int number,
                                    final String name)
      throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("UPDATE Teams SET TeamName = ? WHERE TeamNumber = ?");
      prep.setString(1, name);
      prep.setInt(2, number);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  /**
   * Update a team organization.
   */
  public static void updateTeamOrganization(final Connection connection,
                                            final int number,
                                            final String organization)
      throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("UPDATE Teams SET Organization = ? WHERE TeamNumber = ?");
      prep.setString(1, organization);
      prep.setInt(2, number);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  /**
   * Make sure all of the judges are properly assigned for the current
   * tournament
   * 
   * @param connection the database connection
   * @return true if everything is ok
   */
  public static boolean isJudgesProperlyAssigned(final Connection connection,
                                                 final ChallengeDescription challengeDescription)
      throws SQLException {

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT id FROM Judges WHERE Tournament = ? AND category = ?");
      prep.setInt(1, getCurrentTournament(connection));

      for (final SubjectiveScoreCategory element : challengeDescription.getSubjectiveCategories()) {
        final String categoryName = element.getName();
        prep.setString(2, categoryName);
        rs = prep.executeQuery();
        if (!rs.next()) {
          return false;
        }
        SQLFunctions.close(rs);
        rs = null;
      }
      return true;
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
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
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT Count(*) FROM PlayoffData"
          + " WHERE Tournament = ?"//
          + " AND event_division = ?");
      prep.setInt(1, tournamentID);
      prep.setString(2, division);
      rs = prep.executeQuery();
      if (!rs.next()) {
        throw new RuntimeException("Query to obtain count of PlayoffData entries returned no data");
      } else {
        return rs.getInt(1) > 0;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Check if any playoff bracket is initialized for the specified tournament.
   * 
   * @param connection database connection
   * @param tournamentID tournament ID
   * @return true if any playoff bracket is initialized in the tournament
   * @throws SQLException if the database connection fails
   */
  public static boolean isPlayoffDataInitialized(final Connection connection,
                                                 final int tournamentID)
      throws SQLException, RuntimeException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT Count(*) FROM PlayoffData"
          + " WHERE Tournament = ?");
      prep.setInt(1, tournamentID);
      rs = prep.executeQuery();
      if (!rs.next()) {
        throw new RuntimeException("Query to obtain count of PlayoffData entries returned no data");
      } else {
        return rs.getInt(1) > 0;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Query for whether the specified team has advanced to the specified
   * (playoff) round.
   * 
   * @param connection The database connection to use.
   * @param roundNumber The round number to check. Must be greater than # of
   *          seeding rounds.
   * @return true if team has entry in playoff table for the given round.
   * @throws SQLException if database access fails.
   * @throws RuntimeException
   */
  public static boolean didTeamReachPlayoffRound(final Connection connection,
                                                 final int roundNumber,
                                                 final int teamNumber)
      throws SQLException, RuntimeException {
    return didTeamReachPlayoffRound(connection, getCurrentTournament(connection), roundNumber, teamNumber);
  }

  public static boolean didTeamReachPlayoffRound(final Connection connection,
                                                 final int tournamentID,
                                                 final int roundNumber,
                                                 final int teamNumber)
      throws SQLException, RuntimeException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT Count(*) FROM PlayoffData"
          + " WHERE Tournament = ?" //
          + " AND run_number = ?" //
          + " AND Team = ?");
      prep.setInt(1, tournamentID);
      prep.setInt(2, roundNumber);
      prep.setInt(3, teamNumber);
      rs = prep.executeQuery();
      if (!rs.next()) {
        throw new RuntimeException("Query to check for team # "
            + Integer.toString(teamNumber)
            + "in round "
            + Integer.toString(roundNumber)
            + " failed.");
      } else {
        return rs.getInt(1) == 1;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Colors for index into a list.
   * Below are the colors used.
   * <table>
   * <td>
   * <td bgcolor="#800000">0 - #800000</td>
   * </tr>
   * <td>
   * <td bgcolor="#008000">1 - #008000</td>
   * </tr>
   * <td>
   * <td bgcolor="#CC6600">2 - #CC6600</td>
   * </tr>
   * <td>
   * <td bgcolor="#FF00FF">3 - #FF00FF</td>
   * </tr>
   * <td>
   * <td>continue at the top</td>
   * </tr>
   * </ol>
   * 
   * @param index the division index
   */
  public static String getColorForIndex(final int index) throws SQLException {
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
      throw new RuntimeException("Internal error, cannot choose color");
    }
  }

  /**
   * Get the value of Bye for the given team number, tournament and run number
   * 
   * @return true if the score is a bye, false if it's not a bye or the score
   *         does not exist
   * @throws SQLException on a database error
   */
  public static boolean isBye(final Connection connection,
                              final int tournament,
                              final int teamNumber,
                              final int runNumber)
      throws SQLException, IllegalArgumentException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = getScoreStatsPrep(connection);
      prep.setInt(1, tournament);
      prep.setInt(2, teamNumber);
      prep.setInt(3, runNumber);
      rs = prep.executeQuery();
      if (rs.next()) {
        return rs.getBoolean("Bye");
      } else {
        return false;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Get the value of NoShow for the given team number, tournament and run
   * number
   * 
   * @return true if the score is a No Show, false if it's not a bye or the
   *         score does not exist
   * @throws SQLException on a database error
   */
  public static boolean isNoShow(final Connection connection,
                                 final int tournament,
                                 final int teamNumber,
                                 final int runNumber)
      throws SQLException, IllegalArgumentException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = getScoreStatsPrep(connection);
      prep.setInt(1, tournament);
      prep.setInt(2, teamNumber);
      prep.setInt(3, runNumber);
      rs = prep.executeQuery();
      if (rs.next()) {
        return rs.getBoolean("NoShow");
      } else {
        return false;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Returns true if the score has been verified, i.e. double-checked.
   */
  public static boolean isVerified(final Connection connection,
                                   final int tournament,
                                   final int teamNumber,
                                   final int runNumber)
      throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = getScoreStatsPrep(connection);
      prep.setInt(1, tournament);
      prep.setInt(2, teamNumber);
      prep.setInt(3, runNumber);
      rs = prep.executeQuery();
      if (rs.next()) {
        return rs.getBoolean("Verified");
      } else {
        return false;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Get prepared statement that gets Verified, NoShow, Bye columns for a score.
   * 
   * @param connection
   * @return 1 is tournament, 2 is teamNumber, 3 is runNumber
   * @throws SQLException
   */
  @SuppressFBWarnings(value = { "NP_LOAD_OF_KNOWN_NULL_VALUE" }, justification = "Findbugs bug 3477957")
  private static PreparedStatement getScoreStatsPrep(final Connection connection) throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("SELECT Bye, NoShow, Verified FROM Performance WHERE Tournament = ? AND TeamNumber = ? AND RunNumber = ?");
    } catch (final SQLException e) {
      SQLFunctions.close(prep);
      throw e;
    }
    return prep;
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

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT LineNumber FROM PlayoffData"
          + " WHERE Team = ?"//
          + " AND Tournament = ?" //
          + " AND run_number = ?");
      prep.setInt(1, teamNumber);
      prep.setInt(2, tournament);
      prep.setInt(3, runNumber);
      rs = prep.executeQuery();
      if (rs.next()) {
        return rs.getInt(1);
      } else {
        return -1; // indicates team not present in this run
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
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
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT Team FROM PlayoffData" //
          + " WHERE event_division = ?" //
          + " AND Tournament = ?" //
          + " AND LineNumber = ?" //
          + " AND run_number = ?");
      prep.setString(1, division);
      prep.setInt(2, tournament);
      prep.setInt(3, lineNumber);
      prep.setInt(4, runNumber);
      rs = prep.executeQuery();
      if (rs.next()) {
        final int retVal = rs.getInt(1);
        if (rs.wasNull()) {
          return Team.NULL_TEAM_NUMBER;
        } else {
          return retVal;
        }
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
    return Team.NULL_TEAM_NUMBER;
  }

  /**
   * Returns the number of playoff rounds for the specified division. Depends on
   * the PlayoffData table having been initialized for that division.
   * 
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
   * @return The size of the first round of the playoffs. This is always a power
   *         of 2, and is greater than the number of teams in the tournament by
   *         the number of byes in the first round.
   * @throws SQLException on database error.
   */
  public static int getFirstPlayoffRoundSize(final Connection connection,
                                             final int tournament,
                                             final String division)
      throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT count(*) FROM PlayoffData" //
          + " WHERE Tournament= ?" //
          + " AND event_division= ?" //
          + " AND PlayoffRound=1");
      prep.setInt(1, tournament);
      prep.setString(2, division);
      rs = prep.executeQuery();
      if (rs.next()) {
        return rs.getInt(1);
      } else {
        return 0;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Returns the table assignment string for the given tournament, bracket name,
   * playoff round number, and database line number. If the table assignment is
   * NULL,
   * returns null.
   */
  public static String getAssignedTable(final Connection connection,
                                        final int tournament,
                                        final String eventDivision,
                                        final int round,
                                        final int line)
      throws SQLException {
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("SELECT AssignedTable FROM PlayoffData WHERE Tournament= ?" //
          + " AND event_division= ?" //
          + " AND PlayoffRound= ?"//
          + " AND LineNumber= ?" //
          + " AND AssignedTable IS NOT NULL");
      prep.setInt(1, tournament);
      prep.setString(2, eventDivision);
      prep.setInt(3, round);
      prep.setInt(4, line);
      rs = prep.executeQuery();
      if (rs.next()) {
        return rs.getString(1);
      } else {
        return null;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Get the database version. If no version information exists in the database,
   * the version is 0.
   * 
   * @param connection the database to check
   * @return the database version
   * @throws SQLException
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
   * @param connection
   * @return all team numbers
   */
  public static Collection<Integer> getAllTeamNumbers(final Connection connection) throws SQLException {
    final Set<Integer> allTeamNumbers = new HashSet<Integer>();
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT TeamNumber FROM Teams");
      while (rs.next()) {
        allTeamNumbers.add(rs.getInt(1));
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
    }
    return allTeamNumbers;
  }

  /**
   * Test if a performance score exists for the given team, tournament and run
   * number
   * 
   * @throws SQLException on a database error
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
  public static Double getPerformanceScore(final Connection connection,
                                           final int tournament,
                                           final int teamNumber,
                                           final int runNumber)
      throws SQLException {

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT ComputedTotal FROM Performance"
          + " WHERE TeamNumber = ? AND Tournament = ? AND RunNumber = ?");
      prep.setInt(1, teamNumber);
      prep.setInt(2, tournament);
      prep.setInt(3, runNumber);
      rs = prep.executeQuery();
      if (rs.next()) {
        return rs.getDouble(1);
      } else {
        return null;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Get the max performance run number completed for the specified team in the
   * current tournament. This does not check the verified flag.
   * 
   * @param connection database connection
   * @param teamNumber the team to check
   * @return the max run number or 0 if no performance runs have been completed
   * @throws SQLException
   */
  public static int maxPerformanceRunNumberCompleted(final Connection connection,
                                                     final int teamNumber)
      throws SQLException {
    final int tournament = getCurrentTournament(connection);

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT MAX(RunNumber) FROM Performance"
          + " WHERE TeamNumber = ? AND Tournament = ?");
      prep.setInt(1, teamNumber);
      prep.setInt(2, tournament);
      rs = prep.executeQuery();
      if (rs.next()) {
        final int runNumber = rs.getInt(1);
        return runNumber;
      } else {
        return 0;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }

  }

  /**
   * Returns true if the score has been verified, i.e. double-checked.
   */
  public static boolean isVerified(final Connection connection,
                                   final int tournament,
                                   final Team team,
                                   final int runNumber)
      throws SQLException {
    return isVerified(connection, tournament, team.getTeamNumber(), runNumber);
  }

  /**
   * If team is not null, calls performanceScoreExists(connection,
   * team.getTeamNumber(), runNumber), otherwise returns false.
   */
  public static boolean performanceScoreExists(final Connection connection,
                                               final int tournament,
                                               final Team team,
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
   */
  public static Time dateToTime(final Date date) {
    if (null == date) {
      return null;
    } else {
      return new Time(date.getTime());
    }
  }

  /**
   * Convert {@link java.sql.Time} to {@link java.util.Date}.
   */
  public static Date timeToDate(final Time t) {
    if (null == t) {
      return null;
    } else {
      return new Date(t.getTime());
    }
  }

  /**
   * Update the information for a tournament.
   * 
   * @param tournamentID which tournament to modify
   * @param name new name
   * @param location new location
   * @param date the new tournament date
   * @throws SQLException
   */
  public static void updateTournament(final Connection connection,
                                      final int tournamentID,
                                      final String name,
                                      final String location,
                                      final LocalDate date)
      throws SQLException {
    PreparedStatement updatePrep = null;
    try {
      updatePrep = connection.prepareStatement("UPDATE Tournaments SET Name = ?, Location = ?, tournament_date = ? WHERE tournament_id = ?");
      updatePrep.setString(1, name);
      updatePrep.setString(2, location);
      if (null == date) {
        updatePrep.setNull(3, Types.DATE);
      } else {
        updatePrep.setDate(3, java.sql.Date.valueOf(date));
      }
      updatePrep.setInt(4, tournamentID);
      updatePrep.executeUpdate();
    } finally {
      SQLFunctions.close(updatePrep);
    }
  }

  /**
   * Check if the authentication table is empty or doesn't exist. This will
   * create the authentication table if it doesn't exist.
   * 
   * @return true if the authentication table is missing or empty
   */
  public static boolean isAuthenticationEmpty(final Connection connection) throws SQLException {
    final Collection<String> tables = SQLFunctions.getTablesInDB(connection);
    if (!tables.contains("fll_authentication")) {
      GenerateDB.createAuthentication(connection);
      return true;
    }

    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT * from fll_authentication");
      if (rs.next()) {
        return false;
      } else {
        return true;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
    }
  }

  /**
   * Get the hashed password for a user for checking.
   * 
   * @param connection
   * @param user
   * @return the password or null
   * @throws SQLException
   */
  public static String getHashedPassword(final Connection connection,
                                         final String user)
      throws SQLException {
    final Collection<String> tables = SQLFunctions.getTablesInDB(connection);
    if (!tables.contains("valid_login")) {
      GenerateDB.createValidLogin(connection);
    }

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT fll_pass FROM fll_authentication WHERE fll_user = ?");
      prep.setString(1, user);
      rs = prep.executeQuery();
      if (rs.next()) {
        final String pass = rs.getString(1);
        return pass;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
    return null;
  }

  /**
   * Get the authentication information.
   * 
   * @param connection
   * @return key is user, value is hashed pass
   */
  public static Map<String, String> getAuthInfo(final Connection connection) throws SQLException {
    final Collection<String> tables = SQLFunctions.getTablesInDB(connection);
    if (!tables.contains("valid_login")) {
      GenerateDB.createValidLogin(connection);
    }

    Statement stmt = null;
    ResultSet rs = null;
    Map<String, String> retval = new HashMap<String, String>();
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT fll_user, fll_pass FROM fll_authentication");
      while (rs.next()) {
        final String user = rs.getString(1);
        final String pass = rs.getString(2);
        retval.put(user, pass);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
    }
    return retval;
  }

  /**
   * Add a valid login to the database.
   * 
   * @param magicKey
   */
  public static void addValidLogin(final Connection connection,
                                   final String user,
                                   final String magicKey)
      throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("INSERT INTO valid_login (fll_user, magic_key) VALUES(?, ?)");
      prep.setString(1, user);
      prep.setString(2, magicKey);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  /**
   * Check if any of the specified login keys matches one that was stored.
   * 
   * @param keys the keys to check
   * @return the username that the key matches, null otherwise
   */
  public static String checkValidLogin(final Connection connection,
                                       final Collection<String> keys)
      throws SQLException {
    // not doing the comparison with SQL to avoid SQL injection attack
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT fll_user, magic_key FROM valid_login");
      while (rs.next()) {
        final String user = rs.getString(1);
        final String compare = rs.getString(2);
        for (final String magicKey : keys) {
          if (ComparisonUtils.safeEquals(magicKey, compare)) {
            return user;
          }
        }
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
    }
    return null;
  }

  /**
   * Remove a valid login by magic key.
   */
  public static void removeValidLoginByKey(final Connection connection,
                                           final String magicKey)
      throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("DELETE FROM valid_login WHERE magic_key = ?");
      prep.setString(1, magicKey);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  public static void changePassword(final Connection connection,
                                    final String user,
                                    final String passwordHash)
      throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("UPDATE fll_authentication SET fll_pass = ? WHERE fll_user = ?");
      prep.setString(1, passwordHash);
      prep.setString(2, user);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  /**
   * Remove a valid login by user.
   */
  public static void removeValidLoginByUser(final Connection connection,
                                            final String user)
      throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("DELETE FROM valid_login WHERE fll_user = ?");
      prep.setString(1, user);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  /**
   * Log everyone out.
   */
  public static void logoutAll(final Connection connection) throws SQLException {
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      stmt.executeUpdate("DELETE FROM valid_login");
    } finally {
      SQLFunctions.close(stmt);
    }
  }

  /**
   * Remove a user.
   */
  public static void removeUser(final Connection connection,
                                final String user)
      throws SQLException {
    PreparedStatement removeKeys = null;
    PreparedStatement removeUser = null;
    try {
      removeKeys = connection.prepareStatement("DELETE FROM valid_login where fll_user = ?");
      removeKeys.setString(1, user);
      removeKeys.executeUpdate();

      removeUser = connection.prepareStatement("DELETE FROM fll_authentication where fll_user = ?");
      removeUser.setString(1, user);
      removeUser.executeUpdate();
    } finally {
      SQLFunctions.close(removeKeys);
      SQLFunctions.close(removeUser);
    }
  }

  /**
   * Get the list of current users known to the system.
   */
  public static Collection<String> getUsers(final Connection connection) throws SQLException {
    final Collection<String> users = new LinkedList<String>();
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT fll_user from fll_authentication");
      while (rs.next()) {
        final String user = rs.getString(1);
        users.add(user);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
    }
    return users;
  }

  /**
   * Delete all subjective scores for the specified team in the
   * specified category.
   * 
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
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("DELETE FROM "
          + categoryName
          + " WHERE Tournament = ? AND TeamNumber = ?");
      prep.setInt(1, tournamentID);
      prep.setInt(2, teamNumber);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }
}
