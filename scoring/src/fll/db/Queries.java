/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.db;

import java.io.InputStreamReader;
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

import net.mtu.eggplant.util.ComparisonUtils;
import net.mtu.eggplant.util.sql.SQLFunctions;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Team;
import fll.Tournament;
import fll.Utilities;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.util.ScoreUtils;
import fll.web.playoff.DatabaseTeamScore;
import fll.web.playoff.HttpTeamScore;
import fll.web.playoff.Playoff;
import fll.web.playoff.TeamScore;
import fll.xml.ChallengeParser;
import fll.xml.WinnerType;
import fll.xml.XMLUtils;

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
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Need to generate table name from category")
  public static String computeScoreGroupForTeam(final Connection connection,
                                                final int tournament,
                                                final String categoryName,
                                                final int teamNumber) throws SQLException {
    // otherwise build up the score group name based upon the judges
    final StringBuilder scoreGroup = new StringBuilder();
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT Judge FROM "
          + categoryName + " WHERE TeamNumber = ? AND Tournament = ? AND ComputedTotal IS NOT NULL ORDER BY Judge");
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
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category determines the table name")
  public static Map<String, Collection<Integer>> computeScoreGroups(final Connection connection,
                                                                    final int tournament,
                                                                    final String division,
                                                                    final String categoryName) throws SQLException {
    final Map<String, Collection<Integer>> scoreGroups = new HashMap<String, Collection<Integer>>();

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT Judge FROM "
          + categoryName + " WHERE TeamNumber = ? AND Tournament = ? AND ComputedTotal IS NOT NULL ORDER BY Judge");
      prep.setInt(2, tournament);

      // foreach team, put the team in a score group
      for (final Team team : Queries.getTournamentTeams(connection).values()) {
        // only show the teams for the division that we are looking at right
        // now
        if (division.equals(team.getEventDivision())) {
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
  public static Map<Integer, Team> getTournamentTeams(final Connection connection) throws SQLException {
    return getTournamentTeams(connection, getCurrentTournament(connection));
  }

  /**
   * Get a map of teams for the specified tournament keyed on team number. Uses
   * the table TournamentTeams to determine which teams should be included.
   */
  public static Map<Integer, Team> getTournamentTeams(final Connection connection,
                                                      final int tournamentID) throws SQLException {
    final SortedMap<Integer, Team> tournamentTeams = new TreeMap<Integer, Team>();
    Statement stmt = null;
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      stmt = connection.createStatement();

      prep = connection.prepareStatement("SELECT Teams.TeamNumber, Teams.Organization"//
          + ", Teams.TeamName, Teams.Region"//
          + ", Teams.Division, TournamentTeams.event_division" //
          + " FROM Teams, TournamentTeams" //
          + " WHERE Teams.TeamNumber = TournamentTeams.TeamNumber"//
          + " AND TournamentTeams.Tournament = ?");
      prep.setInt(1, tournamentID);
      rs = prep.executeQuery();
      while (rs.next()) {
        final Team team = new Team();
        team.setTeamNumber(rs.getInt("TeamNumber"));
        team.setOrganization(rs.getString("Organization"));
        team.setTeamName(rs.getString("TeamName"));
        team.setRegion(rs.getString("Region"));
        team.setDivision(rs.getString("Division"));
        team.setEventDivision(rs.getString("event_division"));
        tournamentTeams.put(Integer.valueOf(team.getTeamNumber()), team);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
      SQLFunctions.close(prep);
    }
    return tournamentTeams;
  }

  public static List<String[]> getTournamentTables(final Connection connection) throws SQLException {
    final int currentTournament = getCurrentTournament(connection);

    PreparedStatement prep = null;
    ResultSet rs = null;
    final List<String[]> tableList = new LinkedList<String[]>();
    try {
      prep = connection.prepareStatement("SELECT SideA,SideB FROM tablenames WHERE Tournament = ?");
      prep.setInt(1, currentTournament);
      rs = prep.executeQuery();
      while (rs.next()) {
        final String[] labels = new String[2];
        labels[0] = rs.getString("SideA");
        labels[1] = rs.getString("SideB");
        tableList.add(labels);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }

    return tableList;
  }

  /**
   * Get the list of divisions of all teams.
   * 
   * @param connection the database connection
   * @return the List of divisions. List of strings.
   * @throws SQLException on a database error
   */
  public static List<String> getDivisions(final Connection connection) throws SQLException {
    final List<String> list = new LinkedList<String>();

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT DISTINCT Division FROM Teams ORDER BY Division");
      rs = prep.executeQuery();
      while (rs.next()) {
        final String division = rs.getString(1);
        if (null != division
            && !"".equals(division)) {
          list.add(division);
        }
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
    return list;
  }

  /**
   * Get the list of event divisions at this tournament as a List of Strings.
   * Uses getCurrentTournament to determine the tournament.
   * 
   * @param connection the database connection
   * @return the List of divisions. List of strings.
   * @throws SQLException on a database error
   * @see #getCurrentTournament(Connection)
   */
  public static List<String> getEventDivisions(final Connection connection) throws SQLException {
    final List<String> list = new LinkedList<String>();

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT DISTINCT event_division FROM current_tournament_teams ORDER BY event_division");
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
   * Category name used for the overall rank for a team in the map returned by
   * {@link #getTeamRankings(Connection, Document)}.
   */
  public static final String OVERALL_CATEGORY_NAME = "Overall";

  /**
   * Category name used for the performance rank for a team in the map returned
   * by {@link #getTeamRankings(Connection, Document)}.
   */
  public static final String PERFORMANCE_CATEGORY_NAME = "Performance";

  public static final int NO_SHOW_RANK = -1;

  /**
   * Get the ranking of all teams in all categories.
   * 
   * @return Map with key of division and value is another Map. This Map has a
   *         key of team number and a value of another Map. The key of this Map
   *         is the category name {@link #OVERALL_CATEGORY_NAME} is a special
   *         category and the value is the rank.
   */
  public static Map<String, Map<Integer, Map<String, Integer>>> getTeamRankings(final Connection connection,
                                                                                final Document challengeDocument)
      throws SQLException {
    final Map<String, Map<Integer, Map<String, Integer>>> rankingMap = new HashMap<String, Map<Integer, Map<String, Integer>>>();
    final int tournament = getCurrentTournament(connection);
    final List<String> divisions = getEventDivisions(connection);

    final WinnerType winnerCriteria = XMLUtils.getWinnerCriteria(challengeDocument);
    final String ascDesc = WinnerType.HIGH == winnerCriteria ? "DESC" : "ASC";

    // find the performance ranking
    determinePerformanceRanking(connection, ascDesc, tournament, divisions, rankingMap);

    // find the subjective category rankings
    determineSubjectiveRanking(connection, ascDesc, tournament, divisions, challengeDocument, rankingMap);

    // find the overall ranking
    determineOverallRanking(connection, ascDesc, tournament, divisions, rankingMap);

    return rankingMap;
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
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Need to generate select statement")
  private static void determineSubjectiveRanking(final Connection connection,
                                                 final String ascDesc,
                                                 final int tournament,
                                                 final List<String> divisions,
                                                 final Document challengeDocument,
                                                 final Map<String, Map<Integer, Map<String, Integer>>> rankingMap)
      throws SQLException {

    // cache the subjective categories title->dbname
    final Map<String, String> subjectiveCategories = new HashMap<String, String>();
    for (final Element subjectiveElement : new NodelistElementCollectionAdapter(
                                                                                challengeDocument.getDocumentElement()
                                                                                                 .getElementsByTagName("subjectiveCategory"))) {
      final String title = subjectiveElement.getAttribute("title");
      final String name = subjectiveElement.getAttribute("name");
      subjectiveCategories.put(title, name);
    }

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      for (final String division : divisions) {
        final Map<Integer, Map<String, Integer>> teamMap;
        if (rankingMap.containsKey(division)) {
          teamMap = rankingMap.get(division);
        } else {
          teamMap = new HashMap<Integer, Map<String, Integer>>();
          rankingMap.put(division, teamMap);
        }

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
                + categoryName
                + " FROM Teams, FinalScores WHERE FinalScores.TeamNumber IN ( "
                + teamSelect
                + ") AND Teams.TeamNumber = FinalScores.TeamNumber AND FinalScores.Tournament = ? ORDER BY FinalScores."
                + categoryName + " " + ascDesc);
            prep.setInt(1, tournament);
            rs = prep.executeQuery();
            processTeamRankings(teamMap, categoryTitle, rs);
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
  private static void determineOverallRanking(final Connection connection,
                                              final String ascDesc,
                                              final int tournament,
                                              final List<String> divisions,
                                              final Map<String, Map<Integer, Map<String, Integer>>> rankingMap)
      throws SQLException {
    final StringBuilder query = new StringBuilder();
    query.append("SELECT Teams.TeamNumber, FinalScores.OverallScore");
    query.append(" FROM Teams,FinalScores,current_tournament_teams");
    query.append(" WHERE FinalScores.TeamNumber = Teams.TeamNumber");
    query.append(" AND FinalScores.Tournament = ?");
    query.append(" AND current_tournament_teams.event_division = ?");
    query.append(" AND current_tournament_teams.TeamNumber = Teams.TeamNumber");
    query.append(" ORDER BY FinalScores.OverallScore "
        + ascDesc + ", Teams.TeamNumber");
    computeRanking(connection, tournament, divisions, rankingMap, query.toString(), OVERALL_CATEGORY_NAME);
  }

  /**
   * Assumes that <code>query</code> has 1 parameter, the tournament, and that
   * the result set will have two columns. The first column is the team number
   * and the second is the score.
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "SQL is passed in")
  private static void computeRanking(final Connection connection,
                                     final int tournament,
                                     final List<String> divisions,
                                     final Map<String, Map<Integer, Map<String, Integer>>> rankingMap,
                                     final String query,
                                     final String categoryTitle) throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement(query);
      prep.setInt(1, tournament);
      for (final String division : divisions) {
        final Map<Integer, Map<String, Integer>> teamMap;
        if (rankingMap.containsKey(division)) {
          teamMap = rankingMap.get(division);
        } else {
          teamMap = new HashMap<Integer, Map<String, Integer>>();
          rankingMap.put(division, teamMap);
        }

        prep.setString(2, division);
        rs = prep.executeQuery();
        processTeamRankings(teamMap, categoryTitle, rs);
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
   * 
   * @param categoryName the category name to put the ranks into teamRanks as
   */
  private static void processTeamRankings(final Map<Integer, Map<String, Integer>> teamMap,
                                          final String categoryTitle,
                                          final ResultSet rs) throws SQLException {
    int tieRank = 1;
    int rank = 1;
    double prevScore = Double.NaN;
    while (rs.next()) {
      final int team = rs.getInt(1);
      double score = rs.getDouble(2);
      if (rs.wasNull()) {
        score = Double.NaN;
      }

      final Map<String, Integer> teamRanks;
      if (teamMap.containsKey(team)) {
        teamRanks = teamMap.get(team);
      } else {
        teamRanks = new HashMap<String, Integer>();
        teamMap.put(team, teamRanks);
      }
      if (Double.isNaN(score)) {
        teamRanks.put(categoryTitle, NO_SHOW_RANK);
      } else if (Math.abs(score
          - prevScore) < 0.001) {
        // 3 decimal places should be considered equal
        teamRanks.put(categoryTitle, tieRank);
      } else {
        tieRank = rank;
        teamRanks.put(categoryTitle, rank);
      }

      // setup for next round
      prevScore = score;

      // increment rank counter
      ++rank;
    } // end score group rank
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
  private static void determinePerformanceRanking(final Connection connection,
                                                  final String ascDesc,
                                                  final int tournament,
                                                  final List<String> divisions,
                                                  final Map<String, Map<Integer, Map<String, Integer>>> rankingMap)
      throws SQLException {

    final StringBuilder query = new StringBuilder();
    query.append("SELECT Teams.TeamNumber, FinalScores.performance");
    query.append(" FROM Teams,FinalScores,current_tournament_teams");
    query.append(" WHERE FinalScores.TeamNumber = Teams.TeamNumber");
    query.append(" AND FinalScores.Tournament = ?");
    query.append(" AND current_tournament_teams.event_division = ?");
    query.append(" AND current_tournament_teams.TeamNumber = Teams.TeamNumber");
    query.append(" ORDER BY FinalScores.performance "
        + ascDesc + ", Teams.TeamNumber");
    computeRanking(connection, tournament, divisions, rankingMap, query.toString(), PERFORMANCE_CATEGORY_NAME);
  }

  /**
   * Figure out the next run number for teamNumber. Does not ignore unverified
   * scores.
   */
  public static int getNextRunNumber(final Connection connection,
                                     final int teamNumber) throws SQLException {
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
      return runNumber + 1;
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
                                    final int teamNumber) throws SQLException {
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
   * Get the number of scoresheets to print on a single sheet of paper.
   */
  public static int getScoresheetLayoutNUp(final Connection connection) throws SQLException {
    return getIntGlobalParameter(connection, GlobalParameters.SCORESHEET_LAYOUT_NUP);
  }

  /**
   * Insert a performance score into the database. All of the values are
   * expected to be in request.
   * 
   * @return the SQL executed
   * @throws SQLException on a database error.
   * @throws RuntimeException if a parameter is missing.
   * @throws ParseException if the XML document is invalid.
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE" }, justification = "Goals determine columns")
  public static String insertPerformanceScore(final Document document,
                                              final Connection connection,
                                              final HttpServletRequest request) throws SQLException, ParseException,
      RuntimeException {
    final int currentTournament = getCurrentTournament(connection);
    final WinnerType winnerCriteria = XMLUtils.getWinnerCriteria(document);
    final Element performanceElement = (Element) document.getDocumentElement().getElementsByTagName("Performance")
                                                         .item(0);
    final Element tiebreakerElement = (Element) performanceElement.getElementsByTagName("tiebreaker").item(0);

    final String teamNumberStr = request.getParameter("TeamNumber");
    if (null == teamNumberStr) {
      throw new RuntimeException("Missing parameter: TeamNumber");
    }
    final int teamNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(teamNumberStr).intValue();

    final String runNumberStr = request.getParameter("RunNumber");
    if (null == runNumberStr) {
      throw new RuntimeException("Missing parameter: RunNumber");
    }
    final int runNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(runNumberStr).intValue();

    final String noShow = request.getParameter("NoShow");
    if (null == noShow) {
      throw new RuntimeException("Missing parameter: NoShow");
    }

    final int numSeedingRounds = getNumSeedingRounds(connection, currentTournament);

    final TeamScore teamScore = new HttpTeamScore(performanceElement, teamNumber, runNumber, request);

    final StringBuffer columns = new StringBuffer();
    final StringBuffer values = new StringBuffer();

    columns.append("TeamNumber");
    values.append(teamNumber);
    columns.append(", Tournament");
    values.append(", "
        + currentTournament);

    columns.append(", ComputedTotal");
    values.append(", "
        + ScoreUtils.computeTotalScore(teamScore));

    columns.append(", RunNumber");
    values.append(", "
        + runNumberStr);

    columns.append(", NoShow");
    values.append(", "
        + noShow);

    columns.append(", Verified");
    values.append(", "
        + request.getParameter("Verified"));

    // now do each goal
    for (final Element element : new NodelistElementCollectionAdapter(performanceElement.getElementsByTagName("goal"))) {
      final String name = element.getAttribute("name");

      final String value = request.getParameter(name);
      if (null == value) {
        throw new RuntimeException("Missing parameter: "
            + name);
      }
      columns.append(", "
          + name);
      final Iterator<Element> valueChildren = new NodelistElementCollectionAdapter(
                                                                                   element.getElementsByTagName("value"));
      if (valueChildren.hasNext()) {
        // enumerated
        values.append(", '"
            + value + "'");
      } else {
        values.append(", "
            + value);
      }
    }

    final String sql = "INSERT INTO Performance"
        + " ( " + columns.toString() + ") " + "VALUES ( " + values.toString() + ")";
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      stmt.executeUpdate(sql);
    } finally {
      SQLFunctions.close(stmt);
    }

    // Perform updates to the playoff data table if in playoff rounds.
    if ((runNumber > numSeedingRounds)
        && "1".equals(request.getParameter("Verified"))) {
      updatePlayoffScore(connection, request, currentTournament, winnerCriteria, performanceElement, tiebreakerElement,
                         teamNumber, runNumber, numSeedingRounds, teamScore);
    }

    return sql;
  }

  public static boolean isThirdPlaceEnabled(final Connection connection,
                                            final String division) throws SQLException {
    final int finalRound = getNumPlayoffRounds(connection, division);

    final int tournament = getCurrentTournament(connection);

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
   * @return the SQL executed
   * @throws SQLException on a database error.
   * @throws ParseException if the XML document is invalid.
   * @throws RuntimeException if a parameter is missing.
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE" }, justification = "Need to generate list of columns off the goals")
  public static String updatePerformanceScore(final Document document,
                                              final Connection connection,
                                              final HttpServletRequest request) throws SQLException, ParseException,
      RuntimeException {
    final int currentTournament = getCurrentTournament(connection);
    final WinnerType winnerCriteria = XMLUtils.getWinnerCriteria(document);
    final Element performanceElement = (Element) document.getDocumentElement().getElementsByTagName("Performance")
                                                         .item(0);
    final Element tiebreakerElement = (Element) performanceElement.getElementsByTagName("tiebreaker").item(0);

    final String teamNumberStr = request.getParameter("TeamNumber");
    if (null == teamNumberStr) {
      throw new FLLRuntimeException("Missing parameter: TeamNumber");
    }
    final int teamNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(teamNumberStr).intValue();

    final String runNumberStr = request.getParameter("RunNumber");
    if (null == runNumberStr) {
      throw new FLLRuntimeException("Missing parameter: RunNumber");
    }
    final int runNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(runNumberStr).intValue();

    final String noShow = request.getParameter("NoShow");
    if (null == noShow) {
      throw new FLLRuntimeException("Missing parameter: NoShow");
    }

    final int numSeedingRounds = getNumSeedingRounds(connection, currentTournament);
    final TeamScore teamScore = new HttpTeamScore(performanceElement, teamNumber, runNumber, request);

    final StringBuffer sql = new StringBuffer();

    sql.append("UPDATE Performance SET ");

    sql.append("NoShow = "
        + noShow);

    sql.append(", ComputedTotal = "
        + ScoreUtils.computeTotalScore(teamScore));

    // now do each goal
    for (final Element element : new NodelistElementCollectionAdapter(performanceElement.getElementsByTagName("goal"))) {
      final String name = element.getAttribute("name");

      final String value = request.getParameter(name);
      if (null == value) {
        throw new FLLRuntimeException("Missing parameter: "
            + name);
      }
      final Iterator<Element> valueChildren = new NodelistElementCollectionAdapter(
                                                                                   element.getElementsByTagName("value"));
      if (valueChildren.hasNext()) {
        // enumerated
        sql.append(", "
            + name + " = '" + value + "'");
      } else {
        sql.append(", "
            + name + " = " + value);
      }
    }

    sql.append(", Verified = "
        + request.getParameter("Verified"));

    sql.append(" WHERE TeamNumber = "
        + teamNumber);

    sql.append(" AND RunNumber = "
        + runNumberStr);
    sql.append(" AND Tournament = "
        + currentTournament);

    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      stmt.executeUpdate(sql.toString());
    } finally {
      SQLFunctions.close(stmt);
    }

    // Check if we need to update the PlayoffData table
    if (runNumber > numSeedingRounds) {
      updatePlayoffScore(connection, request, currentTournament, winnerCriteria, performanceElement, tiebreakerElement,
                         teamNumber, runNumber, numSeedingRounds, teamScore);
    }

    return sql.toString();
  }

  /**
   * Note that a performance score has changed and update the playoff table with
   * this new information.
   * 
   * @param connection
   * @param request
   * @param currentTournament
   * @param winnerCriteria
   * @param performanceElement
   * @param tiebreakerElement
   * @param teamNumber
   * @param runNumber
   * @param numSeedingRounds
   * @param team
   * @param teamScore
   * @throws SQLException
   * @throws ParseException
   */
  private static void updatePlayoffScore(final Connection connection,
                                         final HttpServletRequest request,
                                         final int currentTournament,
                                         final WinnerType winnerCriteria,
                                         final Element performanceElement,
                                         final Element tiebreakerElement,
                                         final int teamNumber,
                                         final int runNumber,
                                         final int numSeedingRounds,
                                         final TeamScore teamScore) throws SQLException, ParseException {
    final Team team = Team.getTeamFromDatabase(connection, teamNumber);

    final int playoffRun = runNumber
        - numSeedingRounds;
    final int ptLine = getPlayoffTableLineNumber(connection, currentTournament, teamNumber, playoffRun);
    final String division = getEventDivision(connection, teamNumber);
    if (ptLine > 0) {
      final int siblingTeam = getTeamNumberByPlayoffLine(connection, currentTournament, division,
                                                         (ptLine % 2 == 0 ? ptLine - 1 : ptLine + 1), playoffRun);

      // If sibling team is the NULL team, then updating this score is okay,
      // and no playoff meta data needs updating.
      if (Team.NULL_TEAM_NUMBER != siblingTeam) {
        // Sibling team is not null so we have to check if update can happen
        // anyway

        // See if the modification affects the result of the playoff match
        final Team teamA = Team.getTeamFromDatabase(connection, teamNumber);
        final Team teamB = Team.getTeamFromDatabase(connection, siblingTeam);
        if (teamA == null
            || teamB == null) {
          throw new FLLRuntimeException("Unable to find one of these team numbers in the database: "
              + teamNumber + " and " + siblingTeam);
        }
        final Team oldWinner = Playoff.pickWinner(connection, currentTournament, performanceElement, tiebreakerElement,
                                                  winnerCriteria, teamA, teamB, runNumber);
        final Team newWinner = Playoff.pickWinner(connection, currentTournament, performanceElement, tiebreakerElement,
                                                  winnerCriteria, teamB, team, teamScore, runNumber);
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
          prep = connection.prepareStatement("SELECT TeamNumber FROM Performance" //
              + " WHERE TeamNumber = ?" //
              + " AND RunNumber > ?" //
              + " AND Tournament = ?");
          if (oldWinner != null
              && newWinner != null && !oldWinner.equals(newWinner)) {
            // This score update changes the result of the match, so make sure
            // no other scores exist in later round for either of these 2 teams.
            if (getPlayoffTableLineNumber(connection, currentTournament, teamNumber, playoffRun + 1) > 0) {
              prep.setInt(1, teamNumber);
              prep.setInt(2, runNumber);
              prep.setInt(3, currentTournament);
              rs = prep.executeQuery();
              if (rs.next()) {
                throw new FLLRuntimeException("Unable to update score for team number "
                    + teamNumber + " in playoff round " + playoffRun
                    + " because that team has scores entered in subsequent rounds which would become inconsistent. "
                    + "Delete those scores and then you may update this score.");
              }
              SQLFunctions.close(rs);
              rs = null;
            }
            if (getPlayoffTableLineNumber(connection, currentTournament, siblingTeam, playoffRun + 1) > 0) {
              prep.setInt(1, siblingTeam);
              prep.setInt(2, runNumber);
              prep.setInt(3, currentTournament);
              rs = prep.executeQuery();
              if (rs.next()) {
                throw new FLLRuntimeException("Unable to update score for team number "
                    + teamNumber + " in playoff round " + playoffRun + " because opponent team " + siblingTeam
                    + " has scores in subsequent rounds which would become inconsistent. "
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
        if ("0".equals(request.getParameter("Verified"))
            || !(Queries.performanceScoreExists(connection, teamB, runNumber) && Queries.isVerified(connection,
                                                                                                    currentTournament,
                                                                                                    teamB, runNumber))) {
          removePlayoffScore(connection, division, currentTournament, playoffRun, ptLine);
        } else {
          updatePlayoffTable(connection, newWinner.getTeamNumber(), division, currentTournament, (playoffRun + 1),
                             ((ptLine + 1) / 2));
          final int semiFinalRound = getNumPlayoffRounds(connection, division) - 1;
          if (playoffRun == semiFinalRound
              && isThirdPlaceEnabled(connection, division)) {
            final Team newLoser;
            if (newWinner.equals(teamA)) {
              newLoser = teamB;
            } else {
              newLoser = teamA;
            }
            updatePlayoffTable(connection, newLoser.getTeamNumber(), division, currentTournament, (playoffRun + 1),
                               ((ptLine + 5) / 2));
          }
        }
      }
    } else {
      throw new FLLRuntimeException("Team "
          + teamNumber + " could not be found in the playoff table for playoff round " + playoffRun);
    }
  }

  /**
   * Delete a performance score in the database. All of the values are expected
   * to be in request.
   * 
   * @throws RuntimeException if a parameter is missing or if the playoff meta
   *           data would become inconsistent due to the deletion.
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "OBL_UNSATISFIED_OBLIGATION", justification = "Bug in findbugs - ticket:2924739")
  public static void deletePerformanceScore(final Connection connection,
                                            final HttpServletRequest request) throws SQLException, RuntimeException,
      ParseException {
    final int currentTournament = getCurrentTournament(connection);

    final String teamNumberStr = request.getParameter("TeamNumber");
    if (null == teamNumberStr) {
      throw new RuntimeException("Missing parameter: TeamNumber");
    }
    final int teamNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(teamNumberStr).intValue();

    final String runNumber = request.getParameter("RunNumber");
    if (null == runNumber) {
      throw new RuntimeException("Missing parameter: RunNumber");
    }
    final int irunNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(runNumber).intValue();
    final int numSeedingRounds = getNumSeedingRounds(connection, currentTournament);

    // Check if we need to update the PlayoffData table
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT TeamNumber FROM Performance" //
          + " WHERE TeamNumber = ?" //
          + " AND RunNumber > ?" //
          + " AND Tournament = ?");
      if (irunNumber > numSeedingRounds) {
        final int playoffRun = irunNumber
            - numSeedingRounds;
        final int ptLine = getPlayoffTableLineNumber(connection, currentTournament, teamNumber, playoffRun);
        final String division = getEventDivision(connection, teamNumber);
        if (ptLine > 0) {
          final int siblingLine = ptLine % 2 == 0 ? ptLine - 1 : ptLine + 1;
          final int siblingTeam = getTeamNumberByPlayoffLine(connection, currentTournament, division, siblingLine,
                                                             playoffRun);

          if (siblingTeam != Team.NULL_TEAM_NUMBER) {
            // See if either teamNumber or siblingTeam has a score entered in
            // subsequent rounds
            if (getPlayoffTableLineNumber(connection, currentTournament, teamNumber, playoffRun + 1) > 0) {
              prep.setInt(1, teamNumber);
              prep.setInt(2, irunNumber);
              prep.setInt(3, currentTournament);
              rs = prep.executeQuery();
              if (rs.next()) {
                throw new RuntimeException("Unable to delete score for team number "
                    + teamNumber + " in playoff round " + playoffRun + " because that team "
                    + " has scores in subsequent rounds which would become inconsistent. "
                    + "Delete those scores and then you may delete this score.");
              }
            }
            if (getPlayoffTableLineNumber(connection, currentTournament, siblingTeam, playoffRun + 1) > 0) {
              prep.setInt(1, siblingTeam);
              prep.setInt(2, irunNumber);
              prep.setInt(3, currentTournament);
              rs = prep.executeQuery();
              if (rs.next()) {
                throw new RuntimeException("Unable to delete score for team number "
                    + teamNumber + " in playoff round " + playoffRun + " because opposing team " + siblingTeam
                    + " has scores in subsequent rounds which would become inconsistent. "
                    + "Delete those scores and then you may delete this score.");
              }
            }
            // No dependent score was found, so we can update the playoff table
            // to
            // reflect the deletion of this score by removing the team from the
            // next run column in the bracket
            removePlayoffScore(connection, division, currentTournament, playoffRun, ptLine);

          }
        } else {
          // Do nothing - team didn't get entered into the PlayoffData table.
          // This should not happen, but we also cannot get here unless a score
          // got entered for the team in the Performance table, in which case we
          // want to allow the web interface to be able to delete that score to
          // remove the score from the Performance table.
          if (LOGGER.isDebugEnabled()) {
            LOGGER.trace("Deleting a score that wasn't in the PlayoffData table");
          }
        }
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }

    PreparedStatement deletePrep = null;
    try {
      deletePrep = connection.prepareStatement("DELETE FROM Performance " //
          + " WHERE Tournament = ?" + " AND TeamNumber = ?" + " AND RunNumber = ?");
      deletePrep.setInt(1, currentTournament);
      deletePrep.setInt(2, teamNumber);
      deletePrep.setInt(3, irunNumber);

      deletePrep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  /**
   * Update a row in the playoff table.
   */
  private static void updatePlayoffTable(final Connection connection,
                                         final int teamNumber,
                                         final String division,
                                         final int currentTournament,
                                         final int playoffRound,
                                         final int lineNumber) throws SQLException {
    PreparedStatement prep = null;
    try {
      // TODO ticket:5 cache this for later, should make Queries be an
      // instantiated
      // class...

      prep = connection.prepareStatement("UPDATE PlayoffData" //
          + " SET Team = ?" //
          + ", Printed = ?" //
          + " WHERE event_division = ?" //
          + " AND Tournament = ?" //
          + " AND PlayoffRound = ?" //
          + " AND LineNumber = ?");
      prep.setInt(1, teamNumber);
      prep.setBoolean(2, false);
      prep.setString(3, division);
      prep.setInt(4, currentTournament);
      prep.setInt(5, playoffRound);
      prep.setInt(6, lineNumber);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  private static void removePlayoffScore(final Connection connection,
                                         final String division,
                                         final int currentTournament,
                                         final int playoffRun,
                                         final int ptLine) throws SQLException {
    updatePlayoffTable(connection, Team.NULL_TEAM_NUMBER, division, currentTournament, (playoffRun + 1),
                       ((ptLine + 1) / 2));

    final int semiFinalRound = getNumPlayoffRounds(connection, division) - 1;
    if (playoffRun == semiFinalRound
        && isThirdPlaceEnabled(connection, division)) {
      updatePlayoffTable(connection, Team.NULL_TEAM_NUMBER, division, currentTournament, (playoffRun + 1),
                         ((ptLine + 5) / 2));
    }
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
                                        final int teamNumber) throws SQLException, RuntimeException {
    return getEventDivision(connection, teamNumber, getCurrentTournament(connection));
  }

  /**
   * Get the division that a team is in for the specified tournament.
   * 
   * @param teamNumber the team's number
   * @param tournamentID ID of tournament
   * @return the event division for the team
   * @throws SQLException on a database error
   * @throws RuntimeException if <code>teamNumber</code> cannot be found in
   *           TournamenTeams for the specified tournament
   */
  public static String getEventDivision(final Connection connection,
                                        final int teamNumber,
                                        final int tournamentID) throws SQLException, RuntimeException {
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
        throw new RuntimeException("Couldn't find team number "
            + teamNumber + " in the list of tournament teams! Tournament: " + tournamentID);
      }
    } finally {
      SQLFunctions.close(rs);
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
   * @param division String with the division to query on, or the special string
   *          "__all__" if all divisions should be queried.
   * @param verifiedScoresOnly True if the database query should use only
   *          verified scores, false if it should use all scores.
   * @return a List of Team objects
   * @throws SQLException on a database error
   * @throws RuntimeException if a team can't be found in tournamentTeams
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Need to pick view dynamically")
  public static List<Team> getTeamsNeedingSeedingRuns(final Connection connection,
                                                      final Map<Integer, Team> tournamentTeams,
                                                      final String division,
                                                      final boolean verifiedScoresOnly) throws SQLException,
      RuntimeException {
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
      if ("__all__".equals(division)) {
        prep = connection.prepareStatement("SELECT TeamNumber,Count(*) FROM "
            + view + " WHERE Tournament = ? GROUP BY TeamNumber" + " HAVING Count(*) < ?");
        prep.setInt(1, currentTournament);
        prep.setInt(2, getNumSeedingRounds(connection, currentTournament));
      } else {
        prep = connection.prepareStatement("SELECT "
            + view + ".TeamNumber,Count(" + view + ".TeamNumber) FROM " + view + ",current_tournament_teams WHERE "
            + view
            + ".TeamNumber = current_tournament_teams.TeamNumber AND current_tournament_teams.event_division = ?"
            + " AND " + view + ".Tournament = ? GROUP BY " + view + ".TeamNumber HAVING Count(" + view
            + ".TeamNumber) < ?");
        prep.setString(1, division);
        prep.setInt(2, currentTournament);
        prep.setInt(3, getNumSeedingRounds(connection, currentTournament));
      }

      rs = prep.executeQuery();
      return collectTeamsFromQuery(tournamentTeams, rs);
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Convenience function that defaults to querying all scores, not just those
   * that are verified.
   */
  public static List<Team> getTeamsNeedingSeedingRuns(final Connection connection,
                                                      final Map<Integer, Team> tournamentTeams,
                                                      final String division) throws SQLException, RuntimeException {
    return getTeamsNeedingSeedingRuns(connection, tournamentTeams, division, false);
  }

  /**
   * Get a list of team numbers that have more runs than seeding rounds.
   * 
   * @param connection connection to the database
   * @param tournamentTeams keyed by team number
   * @param division String with the division to query on, or the special string
   *          "__all__" if all divisions should be queried.
   * @param verifiedScoresOnly True if the database query should use only
   *          verified scores, false if it should use all scores.
   * @return a List of Team objects
   * @throws SQLException on a database error
   * @throws RuntimeException if a team can't be found in tournamentTeams
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Dynamically pick view.")
  public static List<Team> getTeamsWithExtraRuns(final Connection connection,
                                                 final Map<Integer, Team> tournamentTeams,
                                                 final String division,
                                                 final boolean verifiedScoresOnly) throws SQLException,
      RuntimeException {
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
      if ("__all__".equals(division)) {
        prep = connection.prepareStatement("SELECT TeamNumber,Count(*) FROM "
            + view + " WHERE Tournament = ? GROUP BY TeamNumber" + " HAVING Count(*) > ?");
        prep.setInt(1, currentTournament);
        prep.setInt(2, getNumSeedingRounds(connection, currentTournament));
      } else {
        prep = connection.prepareStatement("SELECT "
            + view + ".TeamNumber,Count(" + view + ".TeamNumber) FROM " + view + ",current_tournament_teams WHERE "
            + view + ".TeamNumber = current_tournament_teams.TeamNumber"
            + " AND current_tournament_teams.event_division = ? AND " + view + ".Tournament = ? GROUP BY " + view
            + ".TeamNumber" + " HAVING Count(" + view + ".TeamNumber) > ?");
        prep.setString(1, division);
        prep.setInt(2, currentTournament);
        prep.setInt(3, getNumSeedingRounds(connection, currentTournament));
      }

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
  private static List<Team> collectTeamsFromQuery(final Map<Integer, Team> tournamentTeams,
                                                  final ResultSet rs) throws SQLException {
    final List<Team> list = new LinkedList<Team>();
    while (rs.next()) {
      final int teamNumber = rs.getInt(1);
      final Team team = tournamentTeams.get(teamNumber);
      if (null == team) {
        throw new RuntimeException("Couldn't find team number "
            + teamNumber + " in the list of tournament teams!");
      }
      list.add(team);
    }
    return list;
  }

  /**
   * Convenience function that defaults to querying all scores, not just those
   * that are verified.
   */
  public static List<Team> getTeamsWithExtraRuns(final Connection connection,
                                                 final Map<Integer, Team> tournamentTeams,
                                                 final String division) throws SQLException, RuntimeException {
    return getTeamsWithExtraRuns(connection, tournamentTeams, division, false);
  }

  /**
   * Get the order of the teams as seeded in the performance rounds. This will
   * include unverified scores, the assumption being that if you performed the
   * seeding round checks, which exclude unverified scores, you really do want
   * to advance teams.
   * 
   * @param connection connection to the database
   * @param winnerCriteria what determines a winner
   * @param divisionStr the division to generate brackets for, as a String
   * @param tournamentTeams keyed by team number
   * @return a List of team numbers as Integers
   * @throws SQLException on a database error
   * @throws RuntimeException if a team can't be found in tournamentTeams
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Need to choose ascending or descending order based upon winner criteria")
  public static List<Team> getPlayoffSeedingOrder(final Connection connection,
                                                  final WinnerType winnerCriteria,
                                                  final String divisionStr,
                                                  final Map<Integer, Team> tournamentTeams) throws SQLException,
      RuntimeException {

    final List<Team> retval = new ArrayList<Team>();
    final int currentTournament = getCurrentTournament(connection);

    final String ascDesc = WinnerType.HIGH == winnerCriteria ? "DESC" : "ASC";

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT performance_seeding_max.TeamNumber, performance_seeding_max.Score, RAND() as random"
          + " FROM performance_seeding_max, current_tournament_teams" //
          + " WHERE performance_seeding_max.Tournament = ?" //
          + " AND performance_seeding_max.TeamNumber = current_tournament_teams.TeamNumber" //
          + " AND current_tournament_teams.event_division = ?" //
          + " ORDER BY performance_seeding_max.Score " + ascDesc //
          + ", performance_seeding_max.average " + ascDesc //
          + ", random");
      prep.setInt(1, currentTournament);
      prep.setString(2, divisionStr);

      rs = prep.executeQuery();
      while (rs.next()) {
        final int teamNumber = rs.getInt(1);
        final Team team = tournamentTeams.get(Integer.valueOf(teamNumber));
        if (null == team) {
          throw new RuntimeException("Couldn't find team number "
              + teamNumber + " in the list of tournament teams!");
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
   * Get the number of seeding rounds from the database.
   * 
   * @return the number of seeding rounds
   * @throws SQLException on a database error
   */
  public static int getNumSeedingRounds(final Connection connection,
                                        final int tournament) throws SQLException {
    return getIntTournamentParameter(connection, tournament, TournamentParameters.SEEDING_ROUNDS);
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
    setIntTournamentParameter(connection, tournament, TournamentParameters.SEEDING_ROUNDS, newSeedingRounds);
  }

  /**
   * Set the number of scoresheets per printed page.
   * 
   * @param connection The database connection.
   * @param newNup The new number of scoresheets per printed page. Currently
   *          must be 1 or 2.
   * @throws SQLException
   */
  public static void setScoresheetLayoutNUp(final Connection connection,
                                            final int newNup) throws SQLException {
    setGlobalParameter(connection, GlobalParameters.SCORESHEET_LAYOUT_NUP, String.valueOf(newNup));
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
    if (!Queries.globalParameterExists(connection, GlobalParameters.CURRENT_TOURNAMENT)) {
      final Tournament dummyTournament = Tournament.findTournamentByName(connection, GenerateDB.DUMMY_TOURNAMENT_NAME);
      setCurrentTournament(connection, dummyTournament.getTournamentID());
    }
    return getIntGlobalParameter(connection, GlobalParameters.CURRENT_TOURNAMENT);
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
      prep = getTournamentParameterStmt(connection, tournament, paramName);
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
    // TODO this should really be cached
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
      prep = getTournamentParameterStmt(connection, tournament, paramName);
      rs = prep.executeQuery();
      if (rs.next()) {
        int value = rs.getInt(1);
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
   * Set the current tournament in the database.
   * 
   * @param connection db connection
   * @param tournamentID the new value for the current tournament
   */
  public static void setCurrentTournament(final Connection connection,
                                          final int tournamentID) throws SQLException {
    final int currentID = getCurrentTournament(connection);
    if (currentID != tournamentID) {
      setGlobalParameter(connection, GlobalParameters.CURRENT_TOURNAMENT, String.valueOf(tournamentID));
    }
  }

  /**
   * Get a list of regions in the DB ordered by region.
   * 
   * @return list of regions as strings
   */
  public static List<String> getRegions(final Connection connection) throws SQLException {
    final List<String> retval = new LinkedList<String>();
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT DISTINCT Region FROM Teams ORDER BY Region");
      while (rs.next()) {
        final String region = rs.getString(1);
        retval.add(region);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
    }
    return retval;
  }

  /**
   * Delete a team from the database. This clears team from the Teams table and
   * all tables specified by the challengeDocument. It is not an error if the
   * team doesn't exist.
   * 
   * @param teamNumber team to delete
   * @param document the challenge document
   * @param connection connection to database, needs delete privileges
   * @throws SQLException on an error talking to the database
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category name determines table")
  public static void deleteTeam(final int teamNumber,
                                final Document document,
                                final Connection connection) throws SQLException {
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
      for (final Element category : new NodelistElementCollectionAdapter(
                                                                         document.getDocumentElement()
                                                                                 .getElementsByTagName("subjectiveCategory"))) {
        final String name = category.getAttribute("name");
        prep = connection.prepareStatement("DELETE FROM "
            + name + " WHERE TeamNumber = ?");
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

      // delete from Teams
      prep = connection.prepareStatement("DELETE FROM Teams WHERE TeamNumber = ?");
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
   * @see #updateScoreTotals(Document, Connection, int)
   */
  public static void updateScoreTotals(final Document document,
                                       final Connection connection) throws SQLException {
    final int tournament = getCurrentTournament(connection);
    updateScoreTotals(document, connection, tournament);
  }

  /**
   * Total the scores in the database for the specified tournament.
   * 
   * @param document the challenge document
   * @param connection connection to database, needs write privileges
   * @param tournament tournament to update score totals for
   * @throws SQLException if an error occurs
   * @see #updatePerformanceScoreTotals(Document, Connection, int)
   * @see #updateSubjectiveScoreTotals(Document, Connection, int)
   */
  public static void updateScoreTotals(final Document document,
                                       final Connection connection,
                                       final int tournament) throws SQLException {
    updatePerformanceScoreTotals(document, connection, tournament);

    updateSubjectiveScoreTotals(document, connection, tournament);
  }

  /**
   * Compute the total scores for all entered subjective scores.
   * 
   * @param document
   * @param connection
   * @throws SQLException
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category determines table name")
  public static void updateSubjectiveScoreTotals(final Document document,
                                                 final Connection connection,
                                                 final int tournament) throws SQLException {
    final Element rootElement = document.getDocumentElement();

    PreparedStatement updatePrep = null;
    PreparedStatement selectPrep = null;
    ResultSet rs = null;
    try {
      // Subjective ---
      for (final Element subjectiveElement : new NodelistElementCollectionAdapter(
                                                                                  rootElement.getElementsByTagName("subjectiveCategory"))) {
        final String categoryName = subjectiveElement.getAttribute("name");

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
          final double computedTotal = ScoreUtils.computeTotalScore(new DatabaseTeamScore(subjectiveElement,
                                                                                          teamNumber, rs));
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
    } catch (final ParseException e) {
      throw new FLLRuntimeException("Error parsing data in the challenge descriptor, this shouldn't happen", e);
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
   * @param document the challenge document
   * @param connection connection to the database
   * @param tournament the tournament to update scores for.
   * @throws SQLException
   */
  public static void updatePerformanceScoreTotals(final Document document,
                                                  final Connection connection,
                                                  final int tournament) throws SQLException {
    final Element rootElement = document.getDocumentElement();

    PreparedStatement updatePrep = null;
    PreparedStatement selectPrep = null;
    ResultSet rs = null;
    try {

      // build up the SQL
      updatePrep = connection.prepareStatement("UPDATE Performance SET ComputedTotal = ? WHERE TeamNumber = ? AND Tournament = ? AND RunNumber = ?");
      updatePrep.setInt(3, tournament);
      selectPrep = connection.prepareStatement("SELECT * FROM Performance WHERE Tournament = ?");
      selectPrep.setInt(1, tournament);

      final Element performanceElement = (Element) rootElement.getElementsByTagName("Performance").item(0);
      final double minimumPerformanceScore = Utilities.NUMBER_FORMAT_INSTANCE.parse(performanceElement.getAttribute("minimumScore"))
                                                                             .doubleValue();
      rs = selectPrep.executeQuery();
      while (rs.next()) {
        if (!rs.getBoolean("Bye")) {
          final int teamNumber = rs.getInt("TeamNumber");
          final int runNumber = rs.getInt("RunNumber");
          final double computedTotal = ScoreUtils.computeTotalScore(new DatabaseTeamScore(performanceElement,
                                                                                          teamNumber, runNumber, rs));
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
    } catch (final ParseException e) {
      throw new FLLRuntimeException("Error parsing data in the challenge descriptor, this shouldn't happen", e);
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(updatePrep);
      SQLFunctions.close(selectPrep);
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

      prep = getGlobalParameterStmt(connection, GlobalParameters.CHALLENGE_DOCUMENT);
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

  /**
   * Advance a team to the next tournament.
   * 
   * @param connection the database connection
   * @param teamNumber the team to advance
   * @return true on success. Failure indicates that no next tournament exists
   */
  public static boolean advanceTeam(final Connection connection,
                                    final int teamNumber) throws SQLException {

    final int currentTournamentID = getTeamCurrentTournament(connection, teamNumber);
    final Tournament currentTournament = Tournament.findTournamentByID(connection, currentTournamentID);
    if (null == currentTournament.getNextTournament()) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("advanceTeam - No next tournament exists for tournament: "
            + currentTournament.getName() + " team: " + teamNumber);
      }
      return false;
    } else {
      PreparedStatement prep = null;
      try {
        prep = connection.prepareStatement("INSERT INTO TournamentTeams (TeamNumber, Tournament, event_division) VALUES (?, ?, ?)");
        prep.setInt(1, teamNumber);
        prep.setInt(2, currentTournament.getNextTournament().getTournamentID());
        prep.setString(3, getDivisionOfTeam(connection, teamNumber));
        prep.executeUpdate();

        return true;
      } finally {
        SQLFunctions.close(prep);
      }
    }
  }

  /**
   * Get the current tournament that this team is at.
   */
  public static int getTeamCurrentTournament(final Connection connection,
                                             final int teamNumber) throws SQLException {
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
          + teamNumber + " tournamentNames: " + tournaments + " nextTournaments: " + nextTournaments
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
   * Change the current tournament for a team. This will delete all scores for
   * the team in it's current tournament.
   * 
   * @param connection db connection
   * @param teamNumber the team
   * @param newTournament the new current tournament for this team
   */
  public static void changeTeamCurrentTournament(final Connection connection,
                                                 final int teamNumber,
                                                 final int newTournament) throws SQLException {

    final Document document = getChallengeDocument(connection);

    final int currentTournament = getTeamCurrentTournament(connection, teamNumber);

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("changeTeamCurrentTournament teamNumber: "
          + teamNumber + " newTournament: " + newTournament + " current tournament: " + currentTournament);
    }

    PreparedStatement prep = null;
    try {
      deleteTeamFromTournamet(connection, document, teamNumber, currentTournament);

      // set new tournament
      prep = connection.prepareStatement("INSERT INTO TournamentTeams (TeamNumber, Tournament, event_division) VALUES (?, ?, ?)");
      prep.setInt(1, teamNumber);
      prep.setInt(2, newTournament);
      final String division = getDivisionOfTeam(connection, teamNumber);
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Division for team "
            + teamNumber + " is " + division);
      }
      prep.setString(3, division);
      prep.executeUpdate();
      SQLFunctions.close(prep);

    } finally {
      SQLFunctions.close(prep);
    }
  }

  /**
   * Demote the team to it's previous tournament. This will delete all scores
   * for the team in it's current tournament.
   * 
   * @param connection db connection
   * @param document the description of the tournament, used to determine what
   *          tables scores exist in
   * @param teamNumber the team
   */
  public static void demoteTeam(final Connection connection,
                                final Document document,
                                final int teamNumber) throws SQLException {
    final int currentTournament = getTeamCurrentTournament(connection, teamNumber);
    deleteTeamFromTournamet(connection, document, teamNumber, currentTournament);
  }

  /**
   * Delete all record of a team from a tournament. This includes the scores and
   * the TournamentTeams table.
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category determines table name")
  private static void deleteTeamFromTournamet(final Connection connection,
                                              final Document document,
                                              final int teamNumber,
                                              final int currentTournament) throws SQLException {
    // TODO ticket:5 this could be cached
    PreparedStatement prep = null;
    try {
      // delete from subjective categories
      for (final Element category : new NodelistElementCollectionAdapter(
                                                                         document.getDocumentElement()
                                                                                 .getElementsByTagName("subjectiveCategory"))) {
        final String name = category.getAttribute("name");
        prep = connection.prepareStatement("DELETE FROM "
            + name + " WHERE TeamNumber = ? AND Tournament = ?");
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

      // delete from TournamentTeams
      prep = connection.prepareStatement("DELETE FROM TournamentTeams WHERE TeamNumber = ? AND Tournament = ?");
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
                                              final int currentTournament) throws SQLException {
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
   * Add a team to the database. Automatically adds the team to the current
   * tournament as well.
   * 
   * @return null on success, the name of the other team with the same team
   *         number on an error
   */
  public static String addTeam(final Connection connection,
                               final int number,
                               final String name,
                               final String organization,
                               final String region,
                               final String division) throws SQLException {
    return addTeam(connection, number, name, organization, region, division, getCurrentTournament(connection));
  }

  public static String addTeam(final Connection connection,
                               final int number,
                               final String name,
                               final String organization,
                               final String region,
                               final String division,
                               final int tournament) throws SQLException {
    if (Team.isInternalTeamNumber(number)) {
      throw new RuntimeException("Cannot create team with an internal number: "
          + number);
    }

    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      // need to check for duplicate teamNumber
      prep = connection.prepareStatement("SELECT TeamName FROM Teams WHERE TeamNumber = ?");
      prep.setInt(1, number);
      rs = prep.executeQuery();
      if (rs.next()) {
        prep = null;
        final String dup = rs.getString(1);
        return dup;
      } else {
        SQLFunctions.close(rs);
        rs = null;
      }
      SQLFunctions.close(prep);

      prep = connection.prepareStatement("INSERT INTO Teams (TeamName, Organization, Region, Division, TeamNumber) VALUES (?, ?, ?, ?, ?)");
      prep.setString(1, name);
      prep.setString(2, organization);
      prep.setString(3, region == null
          || "".equals(region) ? GenerateDB.DEFAULT_TEAM_REGION : region);
      prep.setString(4, division);
      prep.setInt(5, number);
      prep.executeUpdate();
      SQLFunctions.close(prep);

      prep = connection.prepareStatement("INSERT INTO TournamentTeams (Tournament, TeamNumber, event_division) VALUES(?, ?, ?)");
      prep.setInt(1, tournament);
      prep.setInt(2, number);
      prep.setString(3, division);
      prep.executeUpdate();
      SQLFunctions.close(prep);

      return null;
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Update a team in the database.
   */
  public static void updateTeam(final Connection connection,
                                final int number,
                                final String name,
                                final String organization,
                                final String region,
                                final String division) throws SQLException {

    final String prevDivision = getDivisionOfTeam(connection, number);

    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("UPDATE Teams SET TeamName = ?, Organization = ?, Region = ?, Division = ? WHERE TeamNumber = ?");
      prep.setString(1, name);
      prep.setString(2, organization);
      prep.setString(3, region == null
          || "".equals(region) ? GenerateDB.DEFAULT_TEAM_REGION : region);
      prep.setString(4, division);
      prep.setInt(5, number);
      prep.executeUpdate();
      SQLFunctions.close(prep);
      prep = null;

      // update event divisions that were referencing the old division
      prep = connection.prepareStatement("UPDATE TournamentTeams SET event_division = ? WHERE TeamNumber = ? AND event_division = ?");
      prep.setString(1, division);
      prep.setInt(2, number);
      prep.setString(3, prevDivision);
      prep.executeUpdate();
      SQLFunctions.close(prep);
      prep = null;

    } finally {
      SQLFunctions.close(prep);
    }
  }

  /**
   * Update a team division.
   */
  public static void updateTeamDivision(final Connection connection,
                                        final int number,
                                        final String division) throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("UPDATE Teams SET Division = ? WHERE TeamNumber = ?");
      prep.setString(1, division);
      prep.setInt(2, number);
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
                                             final String eventDivision) throws SQLException {
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
                                    final String name) throws SQLException {
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
                                            final String organization) throws SQLException {
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
   * Update a team region.
   */
  public static void updateTeamRegion(final Connection connection,
                                      final int number,
                                      final String region) throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("UPDATE Teams SET Region = ? WHERE TeamNumber = ?");
      prep.setString(1, region);
      prep.setInt(2, number);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  /**
   * Insert a tournament for each region found in the teams table, if it doesn't
   * already exist. Sets name and location equal to the region name.
   */
  public static void insertTournamentsForRegions(final Connection connection) throws SQLException {
    ResultSet rs = null;
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT DISTINCT Teams.Region FROM Teams LEFT JOIN Tournaments ON Teams.Region = Tournaments.Name WHERE Tournaments.Name IS NULL");
      while (rs.next()) {
        final String region = rs.getString(1);
        if (null != region
            && region.trim().length() > 0) {
          Tournament.createTournament(connection, region, region.trim());
        }
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
    }
  }

  /**
   * Make sure all of the judges are properly assigned for the current
   * tournament
   * 
   * @param connection the database connection
   * @param document XML document to describe the tournament
   * @return true if everything is ok
   */
  public static boolean isJudgesProperlyAssigned(final Connection connection,
                                                 final Document document) throws SQLException {

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT id FROM Judges WHERE Tournament = ? AND category = ?");
      prep.setInt(1, getCurrentTournament(connection));

      for (final Element element : new NodelistElementCollectionAdapter(
                                                                        document.getDocumentElement()
                                                                                .getElementsByTagName("subjectiveCategory"))) {
        final String categoryName = element.getAttribute("name");
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
                                                 final String division) throws SQLException, RuntimeException {
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
                                                 final String division) throws SQLException, RuntimeException {
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

  public static boolean isPlayoffDataInitialized(final Connection connection,
                                                 final int tournamentID) throws SQLException, RuntimeException {
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
   * @param division The division to check in the current tournament.
   * @return true if team has entry in playoff table for the given round.
   * @throws SQLException if database access fails.
   * @throws RuntimeException
   */
  public static boolean didTeamReachPlayoffRound(final Connection connection,
                                                 final int roundNumber,
                                                 final int teamNumber,
                                                 final String division) throws SQLException, RuntimeException {
    return didTeamReachPlayoffRound(connection, getCurrentTournament(connection), roundNumber, teamNumber, division);
  }

  public static boolean didTeamReachPlayoffRound(final Connection connection,
                                                 final int tournamentID,
                                                 final int roundNumber,
                                                 final int teamNumber,
                                                 final String division) throws SQLException, RuntimeException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT Count(*) FROM PlayoffData"
          + " WHERE Tournament = ?" + " AND PlayoffRound = ?" + " AND Team = ?" + " AND event_division = ?");
      prep.setInt(1, tournamentID);
      prep.setInt(2, roundNumber
          - getNumSeedingRounds(connection, tournamentID));
      prep.setInt(3, teamNumber);
      prep.setString(4, division);
      rs = prep.executeQuery();
      if (!rs.next()) {
        throw new RuntimeException("Query to check for team # "
            + Integer.toString(teamNumber) + "in round " + Integer.toString(roundNumber) + " failed.");
      } else {
        return rs.getInt(1) == 1;
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Get the color for a division index. Below are the colors used.
   * <table>
   * <td>
   * <td bgcolor="#800000">0 - #800000</td> </tr>
   * <td>
   * <td bgcolor="#008000">1 - #008000</td> </tr>
   * <td>
   * <td bgcolor="#CC6600">2 - #CC6600</td> </tr>
   * <td>
   * <td bgcolor="#FF00FF">3 - #FF00FF</td> </tr>
   * <td>
   * <td>continue at the top</td> </tr> </ol>
   * 
   * @param index the division index
   */
  public static String getColorForDivisionIndex(final int index) throws SQLException {
    final int idx = index % 4;
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
                              final int runNumber) throws SQLException, IllegalArgumentException {
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
                                 final int runNumber) throws SQLException, IllegalArgumentException {
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
                                   final int runNumber) throws SQLException {
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
        throw new RuntimeException("No score exists for tournament: "
            + tournament + " teamNumber: " + teamNumber + " runNumber: " + runNumber);
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
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "NP_LOAD_OF_KNOWN_NULL_VALUE" }, justification = "Findbugs bug 3477957")
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
   * 
   * @param connection Database connection to use.
   * @param tournament Tournament identifier.
   * @param teamNumber Team number for which to look.
   * @param playoffRunNumber Playoff round number to search. Based at 1.
   * @return The line number of the playoff bracket in which the team number is
   *         found, or a -1 if the team number was not found in the specified
   *         round of the PlayoffData table.
   * @throws SQLException on a database error.
   */
  public static int getPlayoffTableLineNumber(final Connection connection,
                                              final int tournament,
                                              final int teamNumber,
                                              final int playoffRunNumber) throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT LineNumber FROM PlayoffData"
          + " WHERE Team = ?"//
          + " AND Tournament = ?" //
          + " AND PlayoffRound = ?");
      prep.setInt(1, teamNumber);
      prep.setInt(2, tournament);
      prep.setInt(3, playoffRunNumber);
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
   * @param playoffRunNumber Run number of the playoff bracket, based at 1.
   * @return The team number located at the specified location in the playoff
   *         bracket.
   * @throws SQLException if there is a database error.
   */
  public static int getTeamNumberByPlayoffLine(final Connection connection,
                                               final int tournament,
                                               final String division,
                                               final int lineNumber,
                                               final int playoffRunNumber) throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT Team FROM PlayoffData" //
          + " WHERE event_division = ?" //
          + " AND Tournament = ?" //
          + " AND LineNumber = ?" //
          + " AND PlayoffRound = ?");
      prep.setString(1, division);
      prep.setInt(2, tournament);
      prep.setInt(3, lineNumber);
      prep.setInt(4, playoffRunNumber);
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
   * Get the division that a team is registered in. This is different from
   * {@link #getEventDivision(Connection, int)} in that it doesn't change
   * throughout the season.
   * 
   * @param connection Database connection.
   * @param teamNumber Number of the team from which to look up the division.
   * @return String containing the division for the specified teams.
   * @throws SQLException on database errors.
   * @throws RuntimeException if the team number is not found in the Teams
   *           table.
   */
  public static String getDivisionOfTeam(final Connection connection,
                                         final int teamNumber) throws SQLException, RuntimeException {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.prepareStatement("SELECT Division FROM Teams WHERE TeamNumber = ?");
      stmt.setInt(1, teamNumber);
      rs = stmt.executeQuery();
      if (rs.next()) {
        return rs.getString(1);
      } else {
        throw new RuntimeException("Unable to find team number "
            + teamNumber + "in the database.");
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
    }
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
                                        final String division) throws SQLException {
    final int x = getFirstPlayoffRoundSize(connection, division);
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
  public static int getNumPlayoffRounds(final Connection connection) throws SQLException {
    int numRounds = 0;
    for (final String division : getEventDivisions(connection)) {
      final int x = getFirstPlayoffRoundSize(connection, division);
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
   * @param division The division for which to look up round 1 size.
   * @return The size of the first round of the playoffs. This is always a power
   *         of 2, and is greater than the number of teams in the tournament by
   *         the number of byes in the first round.
   * @throws SQLException on database error.
   */
  public static int getFirstPlayoffRoundSize(final Connection connection,
                                             final String division) throws SQLException {
    final int tournament = getCurrentTournament(connection);
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

  public static int getTableAssignmentCount(final Connection connection,
                                            final int tournament,
                                            final String division) throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT count(*) FROM PlayoffData" //
          + " WHERE Tournament= ?" //
          + " AND event_division= ?" //
          + " AND AssignedTable IS NOT NULL");
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
   * Returns the table assignment string for the given tournament, event
   * division, round number, and line number. If the table assignment is NULL,
   * returns null.
   */
  public static String getAssignedTable(final Connection connection,
                                        final int tournament,
                                        final String eventDivision,
                                        final int round,
                                        final int line) throws SQLException {
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
      return getIntGlobalParameter(connection, GlobalParameters.DATABASE_VERSION);
    }
  }

  /**
   * @param connection
   * @return the prepared statement for getting a global parameter with the
   *         values already filled in
   * @throws SQLException
   */
  private static PreparedStatement getGlobalParameterStmt(final Connection connection,
                                                          final String paramName) throws SQLException {
    // TODO this should really be cached
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
  /* package */static boolean globalParameterExists(final Connection connection,
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

  public static void setGlobalParameter(final Connection connection,
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
                                               final int teamNumber,
                                               final int runNumber) throws SQLException {
    final int tournament = getCurrentTournament(connection);

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT ComputedTotal FROM Performance"
          + " WHERE TeamNumber = ? AND Tournament = ? AND RunNumber = ?");
      prep.setInt(1, teamNumber);
      prep.setInt(2, tournament);
      prep.setInt(3, runNumber);
      rs = prep.executeQuery();
      return rs.next();
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
                                   final int runNumber) throws SQLException {
    return isVerified(connection, tournament, team.getTeamNumber(), runNumber);
  }

  /**
   * If team is not null, calls performanceScoreExists(connection,
   * team.getTeamNumber(), runNumber), otherwise returns false.
   */
  public static boolean performanceScoreExists(final Connection connection,
                                               final Team team,
                                               final int runNumber) throws SQLException {
    if (null == team) {
      return false;
    } else {
      return performanceScoreExists(connection, team.getTeamNumber(), runNumber);
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
   * Create a tournament in the database.
   * 
   * @param name the name of the tournament, must not be null
   * @param location the location of the tournament, may be null
   */
  public static void createTournament(final Connection connection,
                                      final String name,
                                      final String location) throws SQLException {
    PreparedStatement insertPrep = null;
    try {
      insertPrep = connection.prepareStatement("INSERT INTO Tournaments (Name, Location) VALUES(?, ?)");
      insertPrep.setString(1, name);
      insertPrep.setString(2, location);
      insertPrep.executeUpdate();
    } finally {
      SQLFunctions.close(insertPrep);
    }
  }

  public static void deleteTournament(final Connection connection,
                                      final int tournamentID) throws SQLException {
    PreparedStatement deletePrep = null;
    try {
      deletePrep = connection.prepareStatement("DELETE FROM Tournaments WHERE tournament_id = ?");
      deletePrep.setInt(1, tournamentID);
    } finally {
      SQLFunctions.close(deletePrep);
    }
  }

  public static void updateTournament(final Connection connection,
                                      final int tournamentID,
                                      final String name,
                                      final String location) throws SQLException {
    PreparedStatement updatePrep = null;
    try {
      updatePrep = connection.prepareStatement("UPDATE Tournaments SET Name = ?, Location = ? WHERE tournament_id = ?");
      updatePrep.setString(1, name);
      updatePrep.setString(2, location);
      updatePrep.setInt(3, tournamentID);
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
    if (!tables.contains("authentication")) {
      GenerateDB.createAuthentication(connection);
      return true;
    }

    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT * from authentication");
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
      rs = stmt.executeQuery("SELECT user, pass FROM authentication");
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
                                   final String magicKey) throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("INSERT INTO valid_login (magic_key) VALUES(?)");
      prep.setString(1, magicKey);
      prep.executeUpdate();
    } finally {
      SQLFunctions.close(prep);
    }
  }

  /**
   * Check if any of the specified login keys matches one that was stored.
   * 
   * @param keys teh keys to check
   * @return true if it matches on in the database, false otherwise
   */
  public static boolean checkValidLogin(final Connection connection,
                                        final Collection<String> keys) throws SQLException {
    // not doing the comparison with SQL to avoid SQL injection attack
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT magic_key FROM valid_login");
      while (rs.next()) {
        final String compare = rs.getString(1);
        for (final String magicKey : keys) {
          if (ComparisonUtils.safeEquals(magicKey, compare)) {
            return true;
          }
        }
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
    }
    return false;
  }

  /**
   * Remove a valid login.
   */
  public static void removeValidLogin(final Connection connection,
                                      final String magicKey) throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("DELETE FROM valid_login WHERE magic_key = ?");
      prep.setString(1, magicKey);
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
}
