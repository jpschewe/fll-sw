
/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll;

import fll.xml.ChallengeParser;
import fll.xml.XMLUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.text.NumberFormat;
import java.text.ParseException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * Does all of our queries.
 *
 * @version $Revision$
 */
public class Queries {

  private static final Logger LOG = Logger.getLogger(Queries.class);
  
  /**
   * For debugging
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    try {
      final ClassLoader classLoader = ChallengeParser.class.getClassLoader();
      final Document challengeDocument = ChallengeParser.parse(classLoader.getResourceAsStream("resources/challenge.xml"));
      
      if(null == challengeDocument) {
        throw new RuntimeException("Error parsing challenge.xml");
      }
      final Connection connection = Utilities.createDBConnection("disk");

      //initializeTournamentTeams(connection);
      //System.out.println(getTournamentTeams(connection));
      //updateScoreTotals(challengeDocument, connection, "Monticello");
      System.out.println(getDivisions(connection));
    } catch(final Exception e) {
      e.printStackTrace();
    }

  }
  
  private Queries() {
     
  }

  /**
   * Populates the tournament teams if they don't realdy exist.  Defaults
   * force to false.
   *
   * @see #populateTournamentTeams(ServletContext, boolean)
   */
  public static void ensureTournamentTeamsPopulated(final ServletContext application) throws SQLException {
    populateTournamentTeams(application, false);
  }

  /**
   * Forces the tournament teams to be populated.  Defaults force to true.
   *
   * @see #populateTournamentTeams(ServletContext, boolean)
   */
  public static void populateTournamentTeams(final ServletContext application) throws SQLException {
    populateTournamentTeams(application, true);
  }
  
  /**
   * Get a map of teams for this tournament keyed on team number and put it in
   * application under the key "tournamentTeams".
   *
   * @param application where to get the currentTournament and connection variables
   * @param force if true, overwrite the value in application
   */
  private static void populateTournamentTeams(final ServletContext application,
                                              final boolean force) throws SQLException {
    if(force || null == application.getAttribute("tournamentTeams")) {
      final Connection connection = (Connection)application.getAttribute("connection");
      application.setAttribute("tournamentTeams", getTournamentTeams(connection));
    }
  }

  /**
   * Get a map of teams for this tournament keyed on team number.  Uses the
   * table TournamentTeams to determine which teams should be included.
   */     
  public static Map getTournamentTeams(final Connection connection) throws SQLException {
    final SortedMap tournamentTeams = new TreeMap();
    
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();

      final String sql = "SELECT Teams.TeamNumber, Teams.Organization, Teams.TeamName, Teams.EntryTournament, Teams.Division"
        + " FROM Teams, TournamentTeams"
        + " WHERE Teams.TeamNumber = TournamentTeams.TeamNumber";
      rs = stmt.executeQuery(sql);
      while(rs.next()) {
        final Team team = new Team();
        team.setTeamNumber(rs.getInt("TeamNumber"));
        team.setOrganization(rs.getString("Organization"));
        team.setTeamName(rs.getString("TeamName"));
        team.setEntryTournament(rs.getString("EntryTournament"));
        team.setDivision(rs.getInt("Division"));
        tournamentTeams.put(new Integer(team.getTeamNumber()), team);
      }
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
    }
    return tournamentTeams;
  }

  /**
   * Populate the TournamentTeams table with the team numbers of the teams for
   * this tournament.  This will delete whatever is in TournamentTeams.  The
   * current tournament is pulled from the TournamentParameters table with
   * getCurrentTournament()
   *
   * @param connection the database connection
   * @throws SQLException on a database error
   * @see #getCurrentTournament(connection)
   */     
  public static void initializeTournamentTeams(final Connection connection)
    throws SQLException {
    final String currentTournament = getCurrentTournament(connection);
    
    Statement stmt = null;
    try {
      stmt = connection.createStatement();

      stmt.executeUpdate("DELETE FROM TournamentTeams");
      
      final String sql = "SELECT Teams.TeamNumber,Teams.EntryTournament FROM Teams WHERE Teams.CurrentTournament = '" + currentTournament + "'";

      stmt.executeUpdate("INSERT INTO TournamentTeams (TeamNumber, EntryTournament) " + sql);
    } finally {
      Utilities.closeStatement(stmt);
    }
  }
  

  /**
   * Get the list of divisions at this tournament as a List of Strings.  Uses
   * getCurrentTournament to determine the tournament.
   *
   * @param connection the database connection
   * @return the List of divisions
   * @throws SQLException on a database error
   * @see #getCurrentTournament(connection)
   */     
  public static List getDivisions(final Connection connection)
    throws SQLException {
    final String currentTournament = getCurrentTournament(connection);

    final List list = new LinkedList();
    
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      
      final String sql = "SELECT Division FROM Teams WHERE Teams.CurrentTournament = '" + currentTournament + "' GROUP BY Division ORDER BY Division";
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
  public static int getNextRunNumber(final ServletContext application,
                                     final int teamNumber) throws SQLException {
    final String currentTournament = (String)application.getAttribute("currentTournament");
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = ((Connection)application.getAttribute("connection")).createStatement();

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
   * Insert a performance score into the database.  All of the values are
   * expected to be in request.
   *
   * @return the SQL executed
   * @throws RuntimeException if a parameter is missing
   */
  public static String insertPerformanceScore(final Document document,
                                              final ServletContext application,
                                              final HttpServletRequest request) throws SQLException, RuntimeException {
    final String currentTournament = (String)application.getAttribute("currentTournament");
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
    values.append(", \"" + currentTournament + "\"");
    
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
      values.append(", " + value);
    }
    
    final String sql = "INSERT INTO Performance"
      + " ( " + columns.toString() + ") "
      + "VALUES ( " + values.toString() + ")";
    final Connection connection = (Connection)application.getAttribute("connection");
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
                                              final ServletContext application,
                                              final HttpServletRequest request) throws SQLException, RuntimeException {
    final String currentTournament = (String)application.getAttribute("currentTournament");
    
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
      sql.append(", " + name + " = " + value);
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
    
    final Connection connection = (Connection)application.getAttribute("connection");
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
  public static String deletePerformanceScore(final ServletContext application,
                                              final HttpServletRequest request)
    throws SQLException, RuntimeException {
    final String currentTournament = (String)application.getAttribute("currentTournament");
    
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
    
    final Connection connection = (Connection)application.getAttribute("connection");
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
   * @param currentTournament the current tournament
   * @param tournamentTeams keyed by team number
   * @return a List of Team objects
   * @throws SQLException on a database error
   * @throws RuntimeException if a team can't be found in tournamentTeams
   */
  public static List getTeamsNeedingSeedingRuns(final Connection connection,
                                                final String currentTournament,
                                                final Map tournamentTeams)
    throws SQLException, RuntimeException {
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
   * @param currentTournament the current tournament
   * @param tournamentTeams keyed by team number
   * @return a List of team numbers as Integers
   * @throws SQLException on a database error
   * @throws RuntimeException if a team can't be found in tournamentTeams
   */
  public static List getTeamsWithExtraRuns(final Connection connection,
                                           final String currentTournament,
                                           final Map tournamentTeams)
    throws SQLException, RuntimeException {
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
   * @param currentTournament the current tournament
   * @param divisionStr the division to generate brackets for, as a String
   * @param tournamentTeams keyed by team number
   * @return a List of team numbers as Integers
   * @throws SQLException on a database error
   * @throws RuntimeException if a team can't be found in tournamentTeams
   */
  public static List getPlayoffSeedingOrder(final Connection connection,
                                            final String currentTournament,
                                            final String divisionStr,
                                            final Map tournamentTeams)
    throws SQLException, RuntimeException {
    final String sql = "SELECT Performance.TeamNumber,MAX(Performance.ComputedTotal) AS Score FROM Performance,Teams"
      + " WHERE Performance.RunNumber <= " + getNumSeedingRounds(connection)
      + " AND Performance.Tournament = '" + currentTournament + "'"
      + " AND Teams.TeamNumber = Performance.TeamNumber"
      + " AND Teams.Division = " + divisionStr
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
   * Get a list of tournament names in the DB.
   *
   * @return list of tournament names as strings
   */
  public static List getTournamentNames(final Connection connection) throws SQLException {
    final List retval = new LinkedList();
    Statement stmt = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT Name FROM Tournaments");
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

      application.removeAttribute("tournamentTeams");
        
    } finally {
      Utilities.closeStatement(stmt);
    }
  }

  /**
   * Total the scores in the database for tournament.  Just totals each row
   * for tournament using document for the appropriate multipliers.
   *
   * <p><b>NOTE</b>: Doesn't handle enumerated goals.  They are just skipped
   * when found</p>
   *
   * @param document the challenge document
   * @param connection connection to database, needs write privileges
   * @param tournament the current tournament
   * @throws SQLException if an error occurs
   * @throws NumberFormantException if document has invalid numbers
   */
  public static void updateScoreTotals(final Document document,
                                       final Connection connection,
                                       final String tournament)
    throws SQLException, ParseException {
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      
      //Performance ---
    
      //build up the SQL command
      final StringBuffer sql = new StringBuffer();
      sql.append("UPDATE Performance SET ComputedTotal = ");
    
      final Element rootElement = document.getDocumentElement();
      final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);
      final int minimumPerformanceScore = NumberFormat.getInstance().parse(performanceElement.getAttribute("minimumScore")).intValue();
      final NodeList goals = performanceElement.getElementsByTagName("goal");
      boolean first = true;
      for(int i=0; i<goals.getLength(); i++) {
        final Element goal = (Element)goals.item(i);
        final NodeList values = goal.getElementsByTagName("value");
        if(values.getLength() == 0) {
          final String goalName = goal.getAttribute("name");
          final String multiplier = goal.getAttribute("multiplier");
          if(first) {
            first = false;
          } else {
            sql.append(" + ");
          }
          sql.append(multiplier + " * " + goalName);
        }
      }
      sql.append(" WHERE Tournament = \"" + tournament + "\"");

      stmt.executeUpdate(sql.toString());

      stmt.executeUpdate("UPDATE Performance SET ComputedTotal = " + minimumPerformanceScore + " WHERE ComputedTotal < " + minimumPerformanceScore);
      
      //Subjective ---
      final NodeList subjectiveCategories = rootElement.getElementsByTagName("subjectiveCategory");
      for(int catIndex=0; catIndex<subjectiveCategories.getLength(); catIndex++) {
        final Element subjectiveElement = (Element)subjectiveCategories.item(catIndex);
        final String categoryName = subjectiveElement.getAttribute("name");

        sql.setLength(0);
        sql.append("UPDATE " + categoryName + " SET ComputedTotal = ");
      
        final NodeList subjectiveGoals = subjectiveElement.getElementsByTagName("goal");
        first = true;
        for(int goalIndex=0; goalIndex<subjectiveGoals.getLength(); goalIndex++) {
          final Element goalElement = (Element)subjectiveGoals.item(goalIndex);
          final NodeList values = goalElement.getElementsByTagName("value");
          if(values.getLength() == 0) {
            //not enumerated
            final String goalName = goalElement.getAttribute("name");
            final String multiplier = goalElement.getAttribute("multiplier");
            if(first) {
              first = false;
            } else {
              sql.append(" + ");
            }
            sql.append(multiplier + " * " + goalName);
          }
        }
        sql.append(" WHERE Tournament = \"" + tournament + "\"");
      
        stmt.executeUpdate(sql.toString());
      }
    } finally {
      Utilities.closeStatement(stmt);
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
}
