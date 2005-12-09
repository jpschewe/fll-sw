/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll;

import fll.xml.XMLUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import java.text.ParseException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Does all of our queries.
 *
 * @version $Revision$
 */
public final class Queries {

  private static final Logger LOG = Logger.getLogger(Queries.class);
  
  /**
   * For debugging
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    try {
      final ClassLoader classLoader = Queries.class.getClassLoader();
      final Connection connection = Utilities.createDBConnection("netserver");
      
      final Document challengeDocument = getChallengeDocument(connection);
      
      //LOG.info("Tournaments: " + getTournamentNames(connection));
      //updateScoreTotals(challengeDocument, connection);
      //System.out.println(getDivisions(connection));

      //LOG.info("Before Tournaments: " + getTournamentNames(connection));
      //insertTournamentsForRegions(connection);
      //LOG.info("After Tournaments: " + getTournamentNames(connection));
      ScoreStandardization.standardizeScores(connection, challengeDocument, "test");
      
//       for(int i=0; i<10; i++) {
//         LOG.info(i + " " + getColorForDivisionIndex(i));
//       }
    } catch(final Exception e) {
      e.printStackTrace();
    }

  }
  
  private Queries() {
    //no instances
  }

  /**
   * Get a map of teams for this tournament keyed on team number.  Uses the
   * table TournamentTeams to determine which teams should be included.
   */     
  public static Map getTournamentTeams(final Connection connection) throws SQLException {
    final String currentTournament = getCurrentTournament(connection);
    
    final SortedMap tournamentTeams = new TreeMap();
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();

      final String sql = "SELECT Teams.TeamNumber, Teams.Organization, Teams.TeamName, Teams.Region, Teams.Division"
        + " FROM Teams, TournamentTeams"
        + " WHERE Teams.TeamNumber = TournamentTeams.TeamNumber"
        + " AND TournamentTeams.Tournament = '" + currentTournament + "'";
      rs = stmt.executeQuery(sql);
      while(rs.next()) {
        final Team team = new Team();
        team.setTeamNumber(rs.getInt("TeamNumber"));
        team.setOrganization(rs.getString("Organization"));
        team.setTeamName(rs.getString("TeamName"));
        team.setRegion(rs.getString("Region"));
        team.setDivision(rs.getString("Division"));
        tournamentTeams.put(new Integer(team.getTeamNumber()), team);
      }
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
    }
    return tournamentTeams;
  }

  /**
   * Get the list of divisions at this tournament as a List of Strings.  Uses
   * getCurrentTournament to determine the tournament.
   *
   * @param connection the database connection
   * @return the List of divisions.  List of strings.
   * @throws SQLException on a database error
   * @see #getCurrentTournament(Connection)
   */     
  public static List getDivisions(final Connection connection)
    throws SQLException {
    final String currentTournament = getCurrentTournament(connection);

    final List list = new LinkedList();
    
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();

      final String sql = "SELECT Teams.Division FROM Teams, TournamentTeams WHERE Teams.TeamNumber = TournamentTeams.TeamNumber AND  TournamentTeams.Tournament = '" + currentTournament + "' GROUP BY Division ORDER BY Division";
      rs = stmt.executeQuery(sql);
      while(rs.next()) {
        final String division = rs.getString(1);
        list.add(division);
      }
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
    }
    return list;
  }
  
  /**
   * Figure out the next run number for teamNumber.
   */
  public static int getNextRunNumber(final Connection connection,
                                     final int teamNumber) throws SQLException {
    final String currentTournament = getCurrentTournament(connection);
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();

      rs = stmt.executeQuery("SELECT COUNT(TeamNumber) FROM Performance WHERE Tournament = \"" + currentTournament + "\" AND TeamNumber = " + teamNumber);
      final int runNumber;
      if(rs.next()) {
        runNumber = rs.getInt(1);
      } else {
        runNumber = 0;
      }
      return runNumber + 1;
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
    }
  }

  /**
   * Figure out the highest run number a team has completed.  This should be
   * the same as next run number -1, but sometimes we get non-consecutive runs
   * in and this just finds the max run number.
   */
  public static int getMaxRunNumber(final Connection connection,
                                    final int teamNumber) throws SQLException {
    final String currentTournament = getCurrentTournament(connection);
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();

      rs = stmt.executeQuery("SELECT MAX(RunNumber) FROM Performance WHERE Tournament = \"" + currentTournament + "\" AND TeamNumber = " + teamNumber);
      final int runNumber;
      if(rs.next()) {
        runNumber = rs.getInt(1);
      } else {
        runNumber = 0;
      }
      return runNumber;
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
    }
  }
  
  /**
   * Insert a performance score into the database.  All of the values are
   * expected to be in request.
   *
   * @return the SQL executed
   * @throws RuntimeException if a parameter is missing
   */
  public static String insertPerformanceScore(final Document document,
                                              final Connection connection,
                                              final HttpServletRequest request)
    throws SQLException, RuntimeException {
    final String currentTournament = getCurrentTournament(connection);
    final StringBuffer columns = new StringBuffer();
    final StringBuffer values = new StringBuffer();

    //TeamNumber has to exist too
    final String teamNumber = request.getParameter("TeamNumber");
    if(null == teamNumber) {
      throw new RuntimeException("Missing parameter: TeamNumber");
    }
    columns.append("TeamNumber");
    values.append(teamNumber);

    columns.append(", Tournament");
    values.append(", '" + currentTournament + "'");
    
    columns.append(", ComputedTotal");
    values.append(", " + request.getParameter("totalScore"));
    
    final String runNumber = request.getParameter("RunNumber");
    if(null == runNumber) {
      throw new RuntimeException("Missing parameter: RunNumber");
    }
    columns.append(", RunNumber");
    values.append(", " + runNumber);
    
    //NoShow isn't in the document specification, but needs to be entered as well 
    final String noShow = request.getParameter("NoShow");
    if(null == noShow) {
      throw new RuntimeException("Missing parameter: NoShow");
    }
    columns.append(", NoShow");
    values.append(", " + noShow);

    //now do each goal
    final Element rootElement = document.getDocumentElement();
    final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);
    final NodeList goals = performanceElement.getElementsByTagName("goal");
    for(int i=0; i<goals.getLength(); i++) {
      final Element element = (Element)goals.item(i);
      final String name = element.getAttribute("name");

      final String value = request.getParameter(name);
      if(null == value) {
        throw new RuntimeException("Missing parameter: " + name);
      }
      columns.append(", " + name);
      final NodeList valueChildren = element.getElementsByTagName("value");
      if(valueChildren.getLength() > 0) {
        //enumerated
        values.append(", '" + value + "'");
      } else {
        values.append(", " + value);
      }
    }
    
    final String sql = "INSERT INTO Performance"
      + " ( " + columns.toString() + ") "
      + "VALUES ( " + values.toString() + ")";
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      stmt.executeUpdate(sql);
    } finally {
      Utilities.closeStatement(stmt);
    }

    return sql;
  }

  /**
   * Update a performance score in the database.  All of the values are
   * expected to be in request.
   *
   * @return the SQL executed
   * @throws RuntimeException if a parameter is missing
   */
  public static String updatePerformanceScore(final Document document,
                                              final Connection connection,
                                              final HttpServletRequest request)
    throws SQLException, RuntimeException {
    final String currentTournament = getCurrentTournament(connection);
    
    final StringBuffer sql = new StringBuffer();
    sql.append("UPDATE Performance SET ");

    //NoShow isn't in the document specification, but needs to be entered as well 
    final String noShow = request.getParameter("NoShow");
    if(null == noShow) {
      throw new RuntimeException("Missing parameter: NoShow");
    }
    sql.append("NoShow = " + noShow);

    sql.append(", ComputedTotal = " + request.getParameter("totalScore"));
    
    //now do each goal
    final Element rootElement = document.getDocumentElement();
    final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);
    final NodeList goals = performanceElement.getElementsByTagName("goal");
    for(int i=0; i<goals.getLength(); i++) {
      final Element element = (Element)goals.item(i);
      final String name = element.getAttribute("name");

      final String value = request.getParameter(name);
      if(null == value) {
        throw new RuntimeException("Missing parameter: " + name);
      }
      final NodeList valueChildren = element.getElementsByTagName("value");
      if(valueChildren.getLength() > 0) {
        //enumerated
        sql.append(", " + name + " = '" + value + "'");
      } else {
        sql.append(", " + name + " = " + value);
      }
    }

    //TeamNumber has to exist too
    final String teamNumber = request.getParameter("TeamNumber");
    if(null == teamNumber) {
      throw new RuntimeException("Missing parameter: TeamNumber");
    }
    sql.append(" WHERE TeamNumber = " + teamNumber);

    final String runNumber = request.getParameter("RunNumber");
    if(null == runNumber) {
      throw new RuntimeException("Missing parameter: RunNumber");
    }
    sql.append(" AND RunNumber = " + runNumber);
    
    sql.append(" AND Tournament = \"" + currentTournament + "\"");
    
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      stmt.executeUpdate(sql.toString());
    } finally {
      Utilities.closeStatement(stmt);
    }

    return sql.toString();
  }

  /**
   * Delete a performance score in the database.  All of the values are
   * expected to be in request.
   *
   * @return the SQL executed
   * @throws RuntimeException if a parameter is missing
   */
  public static String deletePerformanceScore(final Connection connection,
                                              final HttpServletRequest request)
    throws SQLException, RuntimeException {
    final String currentTournament = getCurrentTournament(connection);
    
    final StringBuffer sql = new StringBuffer();
    sql.append("DELETE FROM Performance ");

    //TeamNumber has to exist too
    final String teamNumber = request.getParameter("TeamNumber");
    if(null == teamNumber) {
      throw new RuntimeException("Missing parameter: TeamNumber");
    }
    sql.append(" WHERE TeamNumber = " + teamNumber);

    final String runNumber = request.getParameter("RunNumber");
    if(null == runNumber) {
      throw new RuntimeException("Missing parameter: RunNumber");
    }
    sql.append(" AND RunNumber = " + runNumber);
    
    sql.append(" AND Tournament = '" + currentTournament + "'");
    
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      stmt.executeUpdate(sql.toString());
    } finally {
      Utilities.closeStatement(stmt);
    }

    return sql.toString();
  }
  
  /**
   * Get a list of team numbers that have less runs than seeding rounds
   *
   * @param connection connection to the database
   * @param tournamentTeams keyed by team number
   * @return a List of Team objects
   * @throws SQLException on a database error
   * @throws RuntimeException if a team can't be found in tournamentTeams
   */
  public static List getTeamsNeedingSeedingRuns(final Connection connection,
                                                final Map tournamentTeams)
    throws SQLException, RuntimeException {
    final String currentTournament = getCurrentTournament(connection);
    final String sql = "SELECT TeamNumber,Count(*) AS count FROM Performance"
      + " WHERE Tournament = '" + currentTournament + "'"
      + " GROUP BY TeamNumber"
      + " HAVING count < " + getNumSeedingRounds(connection);
    
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery(sql);
      final List list = new LinkedList();
      while(rs.next()) {
        final int teamNumber = rs.getInt(1);
        final Team team = (Team)tournamentTeams.get(new Integer(teamNumber));
        if(null == team) {
          throw new RuntimeException("Couldn't find team number " + teamNumber + " in the list of tournament teams!");
        }
        list.add(team);
      }
      return list;
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
    }
  }

  /**
   * Get a list of team numbers that have more runs than seeding rounds
   *
   * @param connection connection to the database
   * @param tournamentTeams keyed by team number
   * @return a List of team numbers as Integers
   * @throws SQLException on a database error
   * @throws RuntimeException if a team can't be found in tournamentTeams
   */
  public static List getTeamsWithExtraRuns(final Connection connection,
                                           final Map tournamentTeams)
    throws SQLException, RuntimeException {
    final String currentTournament = getCurrentTournament(connection);
    final String sql = "SELECT TeamNumber,Count(*) AS count FROM Performance"
      + " WHERE Tournament = '" + currentTournament + "'"
      + " GROUP BY TeamNumber"
      + " HAVING count > " + getNumSeedingRounds(connection);
    
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery(sql);
      final List list = new LinkedList();
      while(rs.next()) {
        final int teamNumber = rs.getInt(1);
        final Team team = (Team)tournamentTeams.get(new Integer(teamNumber));
        if(null == team) {
          throw new RuntimeException("Couldn't find team number " + teamNumber + " in the list of tournament teams!");
        }
        list.add(team);
      }
      return list;
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
    }
  }
  
    
  /**
   * Get the order of the teams as seeded in the performance rounds.
   *
   * @param connection connection to the database
   * @param divisionStr the division to generate brackets for, as a String
   * @param tournamentTeams keyed by team number
   * @return a List of team numbers as Integers
   * @throws SQLException on a database error
   * @throws RuntimeException if a team can't be found in tournamentTeams
   */
  public static List getPlayoffSeedingOrder(final Connection connection,
                                            final String divisionStr,
                                            final Map tournamentTeams)
    throws SQLException, RuntimeException {
    final String currentTournament = getCurrentTournament(connection);
    final String sql = "SELECT Performance.TeamNumber,MAX(Performance.ComputedTotal) AS Score FROM Performance,Teams"
      + " WHERE Performance.RunNumber <= " + getNumSeedingRounds(connection)
      + " AND Performance.Tournament = '" + currentTournament + "'"
      + " AND Teams.TeamNumber = Performance.TeamNumber"
      + " AND Teams.Division = '" + divisionStr + "'"
      + " GROUP BY Performance.TeamNumber"
      + " ORDER BY Score DESC, Performance.TeamNumber";

    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery(sql);
      final List list = new ArrayList();
      while(rs.next()) {
        final int teamNumber = rs.getInt(1);
        final Team team = (Team)tournamentTeams.get(new Integer(teamNumber));
        if(null == team) {
          throw new RuntimeException("Couldn't find team number " + teamNumber + " in the list of tournament teams!");
        }
        list.add(team);
      }
      return list;
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
    }
  }
  
  /**
   * Get the number of seeding rounds from the database.  This value is stored
   * in the table TournamentParameters with the Param of SeedingRounds.  If no
   * such value exists a value of 3 is inserted and then returned.
   *
   * @return the number of seeding rounds
   * @throws SQLException on a database error
   */
  public static int getNumSeedingRounds(final Connection connection)
    throws SQLException {
    ResultSet rs = null;
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT Value FROM TournamentParameters WHERE TournamentParameters.Param = 'SeedingRounds'");
      if(rs.next()) {
        return rs.getInt(1);
      } else {
        //insert default entry
        setNumSeedingRounds(connection, 3);
        return 3;
      }
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
    }
  }

  /**
   * Set the number of seeding rounds.
   *
   * @param connection the connection
   * @param newSeedingRounds the new value of seeding rounds
   * @see #getNumSeedingRounds(Connection)
   */
  public static void setNumSeedingRounds(final Connection connection,
                                         final int newSeedingRounds)
    throws SQLException {
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      stmt.executeUpdate("REPLACE TournamentParameters (Param, Value, Description) VALUES ('SeedingRounds', " + newSeedingRounds + ", 'Number of seeding rounds before elimination round - used to downselect top performance scores in queries')");
    } finally {
      Utilities.closeStatement(stmt);
    }

  }
  
  /**
   * Get the current tournament from the database.
   *
   * @return the tournament, or DUMMY if not set.  There should always be a
   * DUMMY tournament in the Tournaments table.
   */
  public static String getCurrentTournament(final Connection connection) throws SQLException {
    ResultSet rs = null;
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT Value FROM TournamentParameters WHERE TournamentParameters.Param = 'CurrentTournament'");
      if(rs.next()) {
        return rs.getString(1);
      } else {
        //insert DUMMY tournament entry
        stmt.executeUpdate("INSERT INTO TournamentParameters (Param, Value, Description) VALUES ('CurrentTournament', 'DUMMY', 'Current running tournemnt name - see Tournaments table')");
        return "DUMMY";
      }
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
    }
  }

  /**
   * Set the current tournament in the database.
   *
   * @param connection db connection
   * @param currentTournament the new value for the current tournament
   * @return true if everything is fine, false if the value is not in the
   * Tournaments table and therefore not set
   */
  public static boolean setCurrentTournament(final Connection connection,
                                             final String currentTournament) throws SQLException {
    ResultSet rs = null;
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT Name FROM Tournaments WHERE Name = '" + currentTournament + "'");
      if(rs.next()) {
        //getCurrentTournament ensures that a value already exists here, so
        //update will work
        stmt.executeUpdate("UPDATE TournamentParameters SET Value = '" + currentTournament + "' WHERE Param = 'CurrentTournament'");
        return true;
      } else {
        return false;
      }
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
    }
  }

  /**
   * Get a list of tournament names in the DB ordered by name.
   *
   * @return list of tournament names as strings
   */
  public static List getTournamentNames(final Connection connection) throws SQLException {
    final List retval = new LinkedList();
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT Name FROM Tournaments ORDER BY Name");
      while(rs.next()) {
        final String tournamentName = rs.getString(1);
        retval.add(tournamentName);
      }
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
    }
    return retval;
  }

  /**
   * Get a list of regions in the DB ordered by region.
   *
   * @return list of regions as strings
   */
  public static List getRegions(final Connection connection) throws SQLException {
    final List retval = new LinkedList();
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT DISTINCT Region FROM Teams ORDER BY Region");
      while(rs.next()) {
        final String region = rs.getString(1);
        retval.add(region);
      }
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
    }
    return retval;
  }
  
  /**
   * Delete a team from the database.  This clears team from the Teams table
   * and all tables specified by the challengeDocument.  It is not an error if
   * the team doesn't exist.
   *
   * @param teamNumber team to delete
   * @param document the challenge document
   * @param connection connection to database, needs delete privileges
   * @param application needed to remove cached team data
   * @throws SQLException on an error talking to the database
   */
  public static void deleteTeam(final int teamNumber,
                                final Document document,
                                final Connection connection,
                                final ServletContext application) throws SQLException {
    Statement stmt = null;
    try {
      stmt = connection.createStatement();

      //delete from subjective categories
      final NodeList subjectiveCategories = document.getDocumentElement().getElementsByTagName("subjectiveCategory");
      for(int i=0; i<subjectiveCategories.getLength(); i++) {
        final Element category = (Element)subjectiveCategories.item(i);
        final String name = category.getAttribute("name");
        stmt.executeUpdate("DELETE FROM " + name + " WHERE TeamNumber = " + teamNumber);
      }

      //delete from Performance
      stmt.executeUpdate("DELETE FROM Performance WHERE TeamNumber = " + teamNumber);

      //delete from Teams
      stmt.executeUpdate("DELETE FROM Teams WHERE TeamNumber = " + teamNumber);

      //delete from TournamentTeams
      stmt.executeUpdate("DELETE FROM TournamentTeams WHERE TeamNumber = " + teamNumber);

      //delete from FinalScores
      stmt.executeUpdate("DELETE FROM FinalScores WHERE TeamNumber = " + teamNumber);

      //delete from SummarizedScores
      stmt.executeUpdate("DELETE FROM SummarizedScores WHERE TeamNumber = " + teamNumber);
      
    } finally {
      Utilities.closeStatement(stmt);
    }
  }

  /**
   * Total the scores in the database for tournament.  Just totals each row
   * for tournament using document for the appropriate multipliers.
   *
   * @param document the challenge document
   * @param connection connection to database, needs write privileges
   * @throws SQLException if an error occurs
   * @throws NumberFormantException if document has invalid numbers
   */
  public static void updateScoreTotals(final Document document,
                                       final Connection connection)
    throws SQLException, ParseException {
    final String tournament = getCurrentTournament(connection);
    final Element rootElement = document.getDocumentElement();
    
    PreparedStatement updatePrep = null;
    PreparedStatement selectPrep = null;
    ResultSet rs = null;
    try {
      //Performance ---

      //build up the SQL
      updatePrep = connection.prepareStatement("UPDATE Performance SET ComputedTotal = ? WHERE TeamNumber = ? AND Tournament = ? AND RunNumber = ?");
      selectPrep = connection.prepareStatement("SELECT * FROM Performance WHERE Tournament = ?");
      selectPrep.setString(1, tournament);
      updatePrep.setString(3, tournament);

      final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);
      final int minimumPerformanceScore = Utilities.NUMBER_FORMAT_INSTANCE.parse(performanceElement.getAttribute("minimumScore")).intValue();
      final NodeList goals = performanceElement.getElementsByTagName("goal");
      rs = selectPrep.executeQuery();
      while(rs.next()) {
        if(!rs.getBoolean("Bye")) {
          final int computedTotal = computeTotalScore(rs, goals);
          if(Integer.MIN_VALUE != computedTotal) {
            updatePrep.setInt(1, Math.max(computedTotal, minimumPerformanceScore));
          } else {
            updatePrep.setNull(1, Types.INTEGER);
          }
          updatePrep.setInt(2, rs.getInt("TeamNumber"));
          updatePrep.setInt(4, rs.getInt("RunNumber"));
          updatePrep.executeUpdate();
        }
      }
      rs.close();
      updatePrep.close();
      selectPrep.close();
      
      //Subjective ---
      final NodeList subjectiveCategories = rootElement.getElementsByTagName("subjectiveCategory");
      for(int catIndex=0; catIndex<subjectiveCategories.getLength(); catIndex++) {
        final Element subjectiveElement = (Element)subjectiveCategories.item(catIndex);
        final String categoryName = subjectiveElement.getAttribute("name");
        final NodeList subjectiveGoals = subjectiveElement.getElementsByTagName("goal");

        //build up the SQL
        updatePrep = connection.prepareStatement("UPDATE " + categoryName + " SET ComputedTotal = ? WHERE TeamNumber = ? AND Tournament = ? AND Judge = ?");
        selectPrep = connection.prepareStatement("SELECT * FROM " + categoryName + " WHERE Tournament = ?");
        selectPrep.setString(1, tournament);
        updatePrep.setString(3, tournament);
        rs = selectPrep.executeQuery();
        while(rs.next()) {
          final int computedTotal = computeTotalScore(rs, subjectiveGoals);
          if(Integer.MIN_VALUE != computedTotal) {
            updatePrep.setInt(1, computedTotal);
          } else {
            updatePrep.setNull(1, Types.INTEGER);
          }
          updatePrep.setInt(2, rs.getInt("TeamNumber"));
          final String judge = rs.getString("Judge");
          updatePrep.setString(4, judge);
          updatePrep.executeUpdate();
        }
        rs.close();
        updatePrep.close();
        selectPrep.close();
      }
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closePreparedStatement(updatePrep);
      Utilities.closePreparedStatement(selectPrep);
    }
  }

  /**
   * Get the challenge document out of the database.  This method doesn't
   * validate the document, since it's assumed that the document was validated
   * before it was put in the database.
   *
   * @param connection connection to the database
   * @return the document
   * @throws RuntimeException if the document cannot be found
   * @throws SQLException on a database error
   */
  public static Document getChallengeDocument(final Connection connection)
    throws SQLException, RuntimeException {

    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT Value FROM TournamentParameters WHERE Param = 'ChallengeDocument'");
      if(rs.next()) {
        return XMLUtils.parseXMLDocument(rs.getAsciiStream(1));
      } else {
        throw new RuntimeException("Could not find challenge document in database");
      }
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
    }
  }

  /**
   * Advance a team to the next tournament.
   *
   * @param connection the database connection
   * @param teamNumber the team to advance
   * @return true on success.  Failure indicates that no next tournament
   * exists
   */
  public static boolean advanceTeam(final Connection connection,
                                    final int teamNumber)
    throws SQLException {

    final String currentTournament = getTeamCurrentTournament(connection, teamNumber);
    final String nextTournament = getNextTournament(connection, currentTournament);
    if(null == nextTournament) {
      LOG.info("advanceTeam - No next tournament exists for tournament: " + currentTournament + " team: " + teamNumber);
      return false;
    } else {
      PreparedStatement prep = null;
      try {
        prep = connection.prepareStatement("INSERT INTO TournamentTeams (TeamNumber, Tournament) VALUES (?, ?)");
        prep.setInt(1, teamNumber);
        prep.setString(2, nextTournament);
        prep.executeUpdate();

        return true;
      } finally {
        Utilities.closePreparedStatement(prep);
      }
    }
  }

  /**
   * Get the current tournament that this team is at.  
   */
  public static String getTeamCurrentTournament(final Connection connection,
                                                final int teamNumber)
    throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT Tournaments.Name, Tournaments.NextTournament"
                                         + " FROM TournamentTeams, Tournaments"
                                         + " WHERE TournamentTeams.TeamNumber = ?"
                                         + " AND TournamentTeams.Tournament = Tournaments.Name");
      prep.setInt(1, teamNumber);
      rs = prep.executeQuery();
      final List tournamentNames = new LinkedList();
      final List nextTournaments = new LinkedList();
      while(rs.next()) {
        tournamentNames.add(rs.getString(1));
        nextTournaments.add(rs.getString(2));
      }


      final Iterator iter = nextTournaments.iterator();
      for(int i=0; iter.hasNext(); i++) {
        final String nextTournament = (String)iter.next();
        if(null == nextTournament) {
          //if no next tournament then this must be the current one since a
          //team can't advance any further.
          return (String)tournamentNames.get(i);
        } else if(!tournamentNames.contains(nextTournament)) {
          //team hasn't advanced past this tournament yet
          return (String)tournamentNames.get(i);
        }
      }
      
      throw new RuntimeException("Internal Error: getTeamCurrentTournament - Cannot determine current tournament for team: " + teamNumber
                                 + " tournamentNames: " + tournamentNames
                                 + " nextTournaments: " + nextTournaments);
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closePreparedStatement(prep);
    }
  }

  /**
   * Change the current tournament for a team.  This will delete all scores
   * for the team in it's current tournament.
   *
   * @param connection db connection
   * @param document the description of the tournament, used to determine what
   * tables scores exist in
   * @param teamNumber the team
   * @param newTournament the new current tournament for this team
   */
  public static void changeTeamCurrentTournament(final Connection connection,
                                                 final Document document,
                                                 final int teamNumber,
                                                 final String newTournament)
    throws SQLException {

    final String currentTournament = getTeamCurrentTournament(connection, teamNumber);
    
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      
      //delete from subjective categories
      final NodeList subjectiveCategories = document.getDocumentElement().getElementsByTagName("subjectiveCategory");
      for(int i=0; i<subjectiveCategories.getLength(); i++) {
        final Element category = (Element)subjectiveCategories.item(i);
        final String name = category.getAttribute("name");
        stmt.executeUpdate("DELETE FROM " + name + " WHERE TeamNumber = " + teamNumber + " AND Tournament = '" + currentTournament + "'");
      }

      //delete from Performance
      stmt.executeUpdate("DELETE FROM Performance WHERE TeamNumber = " + teamNumber + " AND Tournament = '" + currentTournament + "'");

      //delete from TournamentTeams
      stmt.executeUpdate("DELETE FROM TournamentTeams WHERE TeamNumber = " + teamNumber + " AND Tournament = '" + currentTournament + "'");
      
      //delete from FinalScores
      stmt.executeUpdate("DELETE FROM FinalScores WHERE TeamNumber = " + teamNumber + " AND Tournament = '" + currentTournament + "'");

      //delete from SummarizedScores
      stmt.executeUpdate("DELETE FROM SummarizedScores WHERE TeamNumber = " + teamNumber + " AND Tournament = '" + currentTournament + "'");

      //set new tournament
      stmt.executeUpdate("INSERT INTO TournamentTeams (Tournament, TeamNumber) VALUES('" + newTournament + "', " + teamNumber + ")");
      
    } finally {
      Utilities.closeStatement(stmt);
    }
  }

  /**
   * Demote the team to it's previous tournament.  This will delete all scores
   * for the team in it's current tournament.
   *
   * @param connection db connection
   * @param document the description of the tournament, used to determine what
   * tables scores exist in
   * @param teamNumber the team
   */
  public static void demoteTeam(final Connection connection,
                                final Document document,
                                final int teamNumber)
    throws SQLException {

    final String currentTournament = getTeamCurrentTournament(connection, teamNumber);
    
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      
      //delete from subjective categories
      final NodeList subjectiveCategories = document.getDocumentElement().getElementsByTagName("subjectiveCategory");
      for(int i=0; i<subjectiveCategories.getLength(); i++) {
        final Element category = (Element)subjectiveCategories.item(i);
        final String name = category.getAttribute("name");
        stmt.executeUpdate("DELETE FROM " + name + " WHERE TeamNumber = " + teamNumber + " AND Tournament = '" + currentTournament + "'");
      }

      //delete from Performance
      stmt.executeUpdate("DELETE FROM Performance WHERE TeamNumber = " + teamNumber + " AND Tournament = '" + currentTournament + "'");

      //delete from TournamentTeams
      stmt.executeUpdate("DELETE FROM TournamentTeams WHERE TeamNumber = " + teamNumber + " AND Tournament = '" + currentTournament + "'");
      
      //delete from FinalScores
      stmt.executeUpdate("DELETE FROM FinalScores WHERE TeamNumber = " + teamNumber + " AND Tournament = '" + currentTournament + "'");

      //delete from SummarizedScores
      stmt.executeUpdate("DELETE FROM SummarizedScores WHERE TeamNumber = " + teamNumber + " AND Tournament = '" + currentTournament + "'");

    } finally {
      Utilities.closeStatement(stmt);
    }
  }
  
  /**
   * Get the previous tournament for this team, given the current tournament.
   *
   * @param connection the database connection
   * @param teamNumber the team number
   * @param currentTournament the current tournament to use to find the
   * previous tournament, generally this is the return value of
   * getTeamCurrentTournament
   * @return the tournament, or null if no such tournament exists
   * @see #getTeamCurrentTournament(Connection, int)
   */
  public static String getTeamPrevTournament(final Connection connection,
                                             final int teamNumber,
                                             final String currentTournament)
    throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT Tournaments.Name"
                                         + " FROM TournamentTeams, Tournaments"
                                         + " WHERE TournamentTeams.TeamNumber = ?"
                                         + " AND TournamentTeams.Tournament = Tournaments.Name"
                                         + " AND Tournaments.NextTournament = ?");
      prep.setInt(1, teamNumber);
      prep.setString(2, currentTournament);
      rs = prep.executeQuery();
      if(rs.next()) {
        return rs.getString(1);
      } else {
        return null;
      }

    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closePreparedStatement(prep);
    }
      
  }
  
  /**
   * Get the next tournament for the given tournament.
   *
   * @param connection the database connection
   * @param tournament the tournament to find the next tournament for
   * @return the next tournament or null if no such tournament exists
   */
  public static String getNextTournament(final Connection connection,
                                         final String tournament)
    throws SQLException {
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("SELECT NextTournament FROM Tournaments WHERE Name = ?");
      prep.setString(1, tournament);
      rs = prep.executeQuery();
      if(rs.next()) {
        return rs.getString(1);
      } else {
        return null;
      }
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closePreparedStatement(prep);
    }
  }

  /**
   * Add a team to the database
   *
   * @return null on success, the name of the other team with the same team
   * number on an error
   */
  public static String addTeam(final Connection connection,
                               final int number,
                               final String name,
                               final String organization,
                               final String region,
                               final String division)
    throws SQLException {
    Statement stmt = null;
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      //need to check for duplicate teamNumber
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT TeamName FROM Teams WHERE TeamNumber = " + number);
      if(rs.next()) {
        prep = null;
        final String dup = rs.getString(1);
        return dup;
      } else {
        Utilities.closeResultSet(rs);
        rs = null;
      }

      prep = connection.prepareStatement("INSERT INTO Teams (TeamName, Organization, Region, Division, TeamNumber) VALUES (?, ?, ?, ?, ?)");
      prep.setString(1, name);
      prep.setString(2, organization);
      prep.setString(3, region);
      prep.setString(4, division);
      prep.setInt(5, number);
      prep.executeUpdate();

      stmt.executeUpdate("INSERT INTO TournamentTeams (Tournament, TeamNumber) VALUES('" + getCurrentTournament(connection) + "', " + number + ")");
      
      return null;
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
      Utilities.closePreparedStatement(prep);
    }
  }

  /**
   * Update a team in the database
   */
  public static void updateTeam(final Connection connection,
                                final int number,
                                final String name,
                                final String organization,
                                final String region,
                                final String division)
    throws SQLException {
    
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("UPDATE Teams SET TeamName = ?, Organization = ?, Region = ?, Division = ? WHERE TeamNumber = ?");
      prep.setString(1, name);
      prep.setString(2, organization);
      prep.setString(3, region);
      prep.setString(4, division);
      prep.setInt(5, number);
      prep.executeUpdate();
    } finally {
      Utilities.closePreparedStatement(prep);
    }
  }

  /**
   * Insert a tournament for each region found in the teams table, if it
   * doesn't already exist.  Sets name and location equal to the region name.
   */
  public static void insertTournamentsForRegions(final Connection connection)
    throws SQLException {
    ResultSet rs = null;
    Statement stmt = null;
    PreparedStatement insertPrep = null;
    try {
      insertPrep = connection.prepareStatement("INSERT INTO Tournaments (Name, Location) VALUES(?, ?)");
      
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT DISTINCT Teams.Region FROM Teams LEFT JOIN Tournaments ON Teams.REgion = Tournaments.Name WHERE Tournaments.Name IS NULL");
      while(rs.next()) {
        final String region = rs.getString(1);
        insertPrep.setString(1, region);
        insertPrep.setString(2, region);
        insertPrep.executeUpdate();
      }
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
      Utilities.closePreparedStatement(insertPrep);
    }
  }

  /**
   * Make sure all of the judges are properly assigned for the current tournament
   *
   * @param connection the database connection
   * @param document XML document to describe the tournament
   * @return true if everything is ok
   */
  public static boolean isJudgesProperlyAssigned(final Connection connection,
                                                 final Document document)
    throws SQLException {

    final NodeList subjectiveCategories = document.getDocumentElement().getElementsByTagName("subjectiveCategory");
    
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT id FROM Judges WHERE Tournament = ? AND category = ?");
      prep.setString(1, getCurrentTournament(connection));

      for(int i=0; i<subjectiveCategories.getLength(); i++) {
        final String categoryName = ((Element)subjectiveCategories.item(i)).getAttribute("name");
        prep.setString(2, categoryName);
        rs = prep.executeQuery();
        if(!rs.next()) {
          return false;
        }
        Utilities.closeResultSet(rs);
        rs = null;
      }
      return true;
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closePreparedStatement(prep);
    }
  }

  /**
   * Compute total score for values in current row of rs based on goals from
   * challenge document.
   *
   * @return the score, 0 on a bye, Integer.MIN_VALUE if all scores in a row
   * are null or an exception occurred talking to the database (indicating
   * that this row should be ignored)
   */
  private static int computeTotalScore(final ResultSet rs,
                                       final NodeList goals)
    throws ParseException {
    try {
      int computedTotal = 0;
      boolean rowHasScores = false;
      for(int i=0; i<goals.getLength(); i++) {
        final Element goal = (Element)goals.item(i);
        final String goalName = goal.getAttribute("name");
        final int multiplier = Utilities.NUMBER_FORMAT_INSTANCE.parse(goal.getAttribute("multiplier")).intValue();
        final NodeList values = goal.getElementsByTagName("value");
        if(values.getLength() == 0) {
          final int score = rs.getInt(goalName);
          if(!rs.wasNull()) {
            computedTotal += multiplier * score;
            rowHasScores = true;
          }
        } else {
          //enumerated
          //find value that matches value in DB
          final String enumVal = rs.getString(goalName);
          if(null != enumVal) {
            boolean found = false;
            int score = -1;
            for(int v=0; v<values.getLength() && !found; v++) {
              final Element value = (Element)values.item(v);
              if(value.getAttribute("value").equals(enumVal)) {
                score = Utilities.NUMBER_FORMAT_INSTANCE.parse(value.getAttribute("score")).intValue() * multiplier;
                found = true;
              }
            }
            if(!found) {
              throw new RuntimeException("Error, enum value in database for goal: " + goalName + " is not a valid value");
            } else {
              rowHasScores = true;
            }
            computedTotal += score;
          }
        }
      }
      if(!rowHasScores) {
        return Integer.MIN_VALUE;
      } else {
        return computedTotal;
      }
    } catch(final SQLException sqle) {
      LOG.warn("Caught an SQLException computing total score, skipping row");
      return Integer.MIN_VALUE;
    }
  }

  /**
   * Get the color for a division index.  Below are the colors used.
   *
   * <table>
   *   <td><td bgcolor="#800000">0 - #800000</td></tr>
   *   <td><td bgcolor="#008000">1 - #008000</td></tr>
   *   <td><td bgcolor="#CC6600">2 - #CC6600</td></tr>
   *   <td><td bgcolor="#FF00FF">3 - #FF00FF</td></tr>
   *   <td><td>continue at the top</td></tr>
   * </ol>
   *
   * @param index the division index
   */
  public static String getColorForDivisionIndex(final int index)
    throws SQLException {
    final int idx = index % 4;
    switch(idx) {
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
   * does not exist
   * @throws SQLException on a database error
   */
  public static boolean isBye(final Connection connection,
                              final String tournament,
                              final int teamNumber,
                              final int runNumber)
    throws SQLException, IllegalArgumentException {
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT Bye FROM Performance"
                             + " WHERE TeamNumber = " + teamNumber
                             + " AND Tournament = '" + tournament + "'"
                             + " AND RunNumber = " + runNumber);
      if(rs.next()) {
        return rs.getBoolean(1);
      } else {
        return false;
      }
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
    }
  }  
}
