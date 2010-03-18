/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Team;
import fll.Utilities;
import fll.util.FLLRuntimeException;
import fll.xml.XMLUtils;
import fll.xml.XMLWriter;

/**
 * Generate tables for tournament from XML document
 */
public final class GenerateDB {

  /**
   * Version of the database that will be created.
   */
  public static final int DATABASE_VERSION = 1;

  private static final Logger LOGGER = Logger.getLogger(GenerateDB.class);

  /**
   * Region name for internal teams. These teams should not be deleted. This is
   * also the name of a special tournament.
   */
  public static final String INTERNAL_REGION = "INTERNAL";

  private GenerateDB() {
    // no instances
  }

  /**
   * Create a new database <code>database</code> and then call
   * {@link #generateDB(Document, Connection, boolean)}.
   * 
   * @param database name for the database to generate
   */
  public static void generateDB(final Document document, final String database, final boolean forceRebuild) throws SQLException, UnsupportedEncodingException {
    Connection connection = null;
    try {
      LOGGER.info("Creating database connection to database: "
          + database);
      connection = Utilities.createDataSource(database).getConnection();
      LOGGER.info("Received connection: "
          + connection);
      generateDB(document, connection, forceRebuild);
    } finally {
      SQLFunctions.closeConnection(connection);
    }
  }

  public static final String DEFAULT_TEAM_NAME = "<No Name>";

  public static final String DEFAULT_TEAM_DIVISION = "1";

  public static final String DEFAULT_TEAM_REGION = "DUMMY";

  public static final int INTERNAL_TOURNAMENT_ID = -1;

  public static final String INTERNAL_TOURNAMENT_NAME = INTERNAL_REGION;

  /**
   * Generate a completely new DB from document. This also stores the document
   * in the database for later use.
   * 
   * @param document and XML document that describes a tournament
   * @param connection connection to the database to create the tables in
   * @param forceRebuild recreate all tables from scratch, if false don't
   *          recreate the tables that hold team information
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", "OBL_UNSATISFIED_OBLIGATION" }, justification = "Need dynamic data for default values, Bug in findbugs - ticket:2924739")
  public static void generateDB(final Document document, final Connection connection, final boolean forceRebuild) throws SQLException,
      UnsupportedEncodingException {

    Statement stmt = null;
    PreparedStatement prep = null;
    try {
      stmt = connection.createStatement();

      stmt.executeUpdate("SET WRITE_DELAY 100 MILLIS");

      final Collection<String> tables = Queries.getTablesInDB(connection);

      createGlobalParameters(document, connection, forceRebuild, tables);

      // Table structure for table 'Tournaments'
      tournaments(connection, forceRebuild, tables);

      // Table structure for table 'Teams'
      if (forceRebuild) {
        stmt.executeUpdate("DROP TABLE IF EXISTS Teams CASCADE");
      }
      if (forceRebuild
          || !tables.contains("Teams".toLowerCase())) {
        stmt.executeUpdate("CREATE TABLE Teams ("
            + "  TeamNumber integer NOT NULL," //
            + "  TeamName varchar(255) default '" + DEFAULT_TEAM_NAME + "' NOT NULL," //
            + "  Organization varchar(255)," //
            + "  Division varchar(32) default '" + DEFAULT_TEAM_DIVISION + "' NOT NULL," //
            + "  Region varchar(255) default '" + DEFAULT_TEAM_REGION + "' NOT NULL," //
            + "  CONSTRAINT teams_pk PRIMARY KEY (TeamNumber)" + ")");

        // add the bye team so that references work
        prep = connection.prepareStatement("INSERT INTO Teams(TeamNumber, TeamName, Region) VALUES(?, ?, ?)");
        prep.setInt(1, Team.BYE.getTeamNumber());
        prep.setString(2, Team.BYE.getTeamName());
        prep.setString(3, INTERNAL_REGION);
        prep.executeUpdate();

        // add the tie team so that references work
        prep = connection.prepareStatement("INSERT INTO Teams(TeamNumber, TeamName, Region) VALUES(?, ?, ?)");
        prep.setInt(1, Team.TIE.getTeamNumber());
        prep.setString(2, Team.TIE.getTeamName());
        prep.setString(3, INTERNAL_REGION);
        prep.executeUpdate();

        // add the null team so that references work
        prep = connection.prepareStatement("INSERT INTO Teams(TeamNumber, TeamName, Region) VALUES(?, ?, ?)");
        prep.setInt(1, Team.NULL.getTeamNumber());
        prep.setString(2, Team.NULL.getTeamName());
        prep.setString(3, INTERNAL_REGION);
        prep.executeUpdate();

        SQLFunctions.closePreparedStatement(prep);
      }

      // Table structure for table 'tablenames'
      stmt.executeUpdate("DROP TABLE IF EXISTS tablenames CASCADE");
      stmt.executeUpdate("CREATE TABLE tablenames ("
          + "  Tournament INTEGER NOT NULL," + "  PairID INTEGER NOT NULL," + "  SideA varchar(64) NOT NULL," + "  SideB varchar(64) NOT NULL,"
          + "  CONSTRAINT tablenames_pk PRIMARY KEY (Tournament,PairID)"
          + " ,CONSTRAINT tablenames_fk1 FOREIGN KEY(Tournament) REFERENCES Tournaments(tournament_id)" + ")");

      // table to hold head-to-head playoff meta-data
      stmt.executeUpdate("DROP TABLE IF EXISTS PlayoffData CASCADE");
      stmt.executeUpdate("CREATE TABLE PlayoffData ("
          + " event_division varchar(32) NOT NULL," //
          + " Tournament INTEGER  NOT NULL," //
          + " PlayoffRound integer NOT NULL," //
          + " LineNumber integer NOT NULL," //
          + " Team integer default " + Team.NULL_TEAM_NUMBER + "," //
          + " AssignedTable varchar(64) default NULL," //
          + " Printed boolean default FALSE," //
          + " CONSTRAINT playoff_data_pk PRIMARY KEY (event_division, Tournament, PlayoffRound, LineNumber)" //
          + ",CONSTRAINT playoff_data_fk1 FOREIGN KEY(Tournament) REFERENCES Tournaments(tournament_id)" //
          + ",CONSTRAINT playoff_data_fk2 FOREIGN KEY(Team) REFERENCES Teams(TeamNumber)" + ")"); //

      // table to hold team numbers of teams in this tournament
      if (forceRebuild) {
        stmt.executeUpdate("DROP TABLE IF EXISTS TournamentTeams CASCADE");
      }
      if (forceRebuild
          || !tables.contains("TournamentTeams".toLowerCase())) {
        stmt.executeUpdate("CREATE TABLE TournamentTeams ("
            + "  TeamNumber integer NOT NULL," //
            + "  Tournament INTEGER NOT NULL," //
            + "  event_division varchar(32) default '" + DEFAULT_TEAM_DIVISION + "' NOT NULL," //
            + "  CONSTRAINT tournament_teams_pk PRIMARY KEY (TeamNumber, Tournament)" //
            + " ,CONSTRAINT tournament_teams_fk1 FOREIGN KEY(TeamNumber) REFERENCES Teams(TeamNumber)" //
            + " ,CONSTRAINT tournament_teams_fk2 FOREIGN KEY(Tournament) REFERENCES Tournaments(tournament_id)" + ")");
      }

      tournamentParameters(connection, forceRebuild, tables);

      // Table structure for table 'Judges'
      stmt.executeUpdate("DROP TABLE IF EXISTS Judges CASCADE");
      stmt.executeUpdate("CREATE TABLE Judges ("
          + "  id varchar(64) NOT NULL,"//
          + "  category varchar(64) NOT NULL," //
          + "  Tournament INTEGER NOT NULL," //
          + "  event_division varchar(32) NOT NULL," //
          + "  CONSTRAINT judges_pk PRIMARY KEY (id,category,Tournament,event_division)"//
          + " ,CONSTRAINT judges_fk1 FOREIGN KEY(Tournament) REFERENCES Tournaments(tournament_id)" //
          + ")");

      final Element rootElement = document.getDocumentElement();
      final StringBuilder createStatement = new StringBuilder();

      // performance
      final StringBuilder performanceColumns = new StringBuilder(); // used for
      // view
      // below
      {
        final Element performanceElement = (Element) rootElement.getElementsByTagName("Performance").item(0);
        final String tableName = "Performance";
        stmt.executeUpdate("DROP VIEW IF EXISTS performance_seeding_max CASCADE");
        stmt.executeUpdate("DROP TABLE IF EXISTS "
            + tableName + " CASCADE");
        createStatement.append("CREATE TABLE "
            + tableName + " (");
        performanceColumns.append("TeamNumber,");
        createStatement.append(" TeamNumber INTEGER NOT NULL,");
        performanceColumns.append("Tournament,");
        createStatement.append(" Tournament INTEGER NOT NULL,");
        performanceColumns.append("RunNumber,");
        createStatement.append(" RunNumber INTEGER NOT NULL,");
        performanceColumns.append("TimeStamp,");
        createStatement.append(" TimeStamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,");
        performanceColumns.append("NoShow,");
        createStatement.append(" NoShow boolean DEFAULT FALSE NOT NULL,");
        performanceColumns.append("Bye,");
        createStatement.append(" Bye boolean DEFAULT FALSE NOT NULL,");
        createStatement.append(" Verified boolean DEFAULT FALSE NOT NULL,");
        for (final Element element : XMLUtils.filterToElements(performanceElement.getElementsByTagName("goal"))) {
          final String columnDefinition = generateGoalColumnDefinition(element);
          createStatement.append(" "
              + columnDefinition + ",");
          performanceColumns.append(element.getAttribute("name")
              + ",");
        }
        performanceColumns.append("ComputedTotal,");
        createStatement.append(" ComputedTotal float DEFAULT NULL,");
        performanceColumns.append("StandardizedScore");
        createStatement.append(" StandardizedScore float default NULL,");
        createStatement.append(" CONSTRAINT "
            + tableName + "_pk PRIMARY KEY (TeamNumber, Tournament, RunNumber)");
        createStatement.append(",CONSTRAINT "
            + tableName + "_fk1 FOREIGN KEY(TeamNumber) REFERENCES Teams(TeamNumber)");
        createStatement.append(",CONSTRAINT "
            + tableName + "_fk2 FOREIGN KEY(Tournament) REFERENCES Tournaments(tournament_id)");
        createStatement.append(");");
        stmt.executeUpdate(createStatement.toString());

      }

      // loop over each subjective category
      final StringBuilder finalScores = new StringBuilder();
      finalScores.append("CREATE TABLE FinalScores (");
      finalScores.append("TeamNumber integer NOT NULL,");
      finalScores.append("Tournament INTEGER NOT NULL,");
      for (final Element categoryElement : XMLUtils.filterToElements(rootElement.getElementsByTagName("subjectiveCategory"))) {
        createStatement.setLength(0);

        final String tableName = categoryElement.getAttribute("name");

        stmt.executeUpdate("DROP TABLE IF EXISTS "
            + tableName + " CASCADE");
        createStatement.append("CREATE TABLE "
            + tableName + " (");
        createStatement.append(" TeamNumber INTEGER NOT NULL,");
        createStatement.append(" Tournament INTEGER NOT NULL,");
        createStatement.append(" Judge VARCHAR(64) NOT NULL,");
        createStatement.append(" NoShow boolean DEFAULT FALSE NOT NULL,");
        for (final Element element : XMLUtils.filterToElements(categoryElement.getElementsByTagName("goal"))) {
          final String columnDefinition = generateGoalColumnDefinition(element);
          createStatement.append(" "
              + columnDefinition + ",");
        }
        createStatement.append(" ComputedTotal float DEFAULT NULL,");
        createStatement.append(" StandardizedScore float default NULL,");
        createStatement.append(" CONSTRAINT "
            + tableName + "_pk PRIMARY KEY (TeamNumber, Tournament, Judge)");
        createStatement.append(",CONSTRAINT "
            + tableName + "_fk1 FOREIGN KEY(TeamNumber) REFERENCES Teams(TeamNumber)");
        createStatement.append(",CONSTRAINT "
            + tableName + "_fk2 FOREIGN KEY(Tournament) REFERENCES Tournaments(tournament_id)");
        createStatement.append(");");
        stmt.executeUpdate(createStatement.toString());

        finalScores.append(tableName
            + " float default NULL,");
      }

      // Table structure for table 'FinalScores'
      finalScores.append(" performance float default NULL,");
      finalScores.append(" OverallScore float,");
      finalScores.append("CONSTRAINT final_scores_pk PRIMARY KEY (TeamNumber, Tournament)");
      finalScores.append(",CONSTRAINT final_scores_fk1 FOREIGN KEY(TeamNumber) REFERENCES Teams(TeamNumber)");
      finalScores.append(",CONSTRAINT final_scores_fk2 FOREIGN KEY(Tournament) REFERENCES Tournaments(tournament_id)");
      finalScores.append(")");
      stmt.executeUpdate("DROP TABLE IF EXISTS FinalScores CASCADE");
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(finalScores.toString());
      }
      stmt.executeUpdate(finalScores.toString());

      // create views

      // max seeding round score
      // FIXME needs to be updated to handle tournament value and default value
      // -- make only in the context of the current tournament
      stmt.executeUpdate("DROP VIEW IF EXISTS performance_seeding_max");
      // TODO: can use PreparedStatement here?
      stmt.executeUpdate("CREATE VIEW performance_seeding_max AS "//
          + " SELECT TeamNumber, Tournament, Max(ComputedTotal) As Score" //
          + " FROM Performance" //
          + " WHERE NoShow = 0" //
          + " AND RunNumber <= "//
          + " (SELECT param_value FROM tournament_parameters " //
          + "       WHERE tournament_parameters.param = 'SeedingRounds'" //
          + " ) GROUP BY TeamNumber, Tournament");

      // current tournament teams
      stmt.executeUpdate("DROP VIEW IF EXISTS current_tournament_teams");
      stmt.executeUpdate("CREATE VIEW current_tournament_teams AS "//
          + " SELECT * FROM TournamentTeams" //
          + " WHERE Tournament IN " //
          + " (SELECT param_value " // " +
          + "      FROM global_parameters " //
          + "      WHERE param = '" + GlobalParameters.CURRENT_TOURNAMENT + "'"//
          + "  )");

      // verified performance scores
      stmt.executeUpdate("DROP VIEW IF EXISTS verified_performance");
      stmt.executeUpdate("CREATE VIEW verified_performance AS SELECT "
          + performanceColumns.toString() + " FROM Performance WHERE Verified = TRUE");

      setDefaultParameters(connection);

    } finally {
      SQLFunctions.closeStatement(stmt);
      SQLFunctions.closePreparedStatement(prep);
    }

  }

  /** Table structure for table 'tournament_parameters' */
  /* package */static void tournamentParameters(final Connection connection, final boolean forceRebuild, final Collection<String> tables) throws SQLException {
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      if (forceRebuild) {
        stmt.executeUpdate("DROP TABLE IF EXISTS tournament_parameters CASCADE");
      }
      if (forceRebuild
          || !tables.contains("tournament_parameters".toLowerCase())) {
        stmt.executeUpdate("CREATE TABLE tournament_parameters ("
            + "  param varchar(64) NOT NULL" //
            + " ,param_value longvarchar NOT NULL" //
            + " ,tournament integer NOT NULL" //
            + " ,CONSTRAINT tournament_parameters_pk PRIMARY KEY  (param, tournament)" //
            + " ,CONSTRAINT tournament_parameters_fk1 FOREIGN KEY(tournament) REFERENCES Tournaments(tournament_id)" //
            + ")");
      }
    } finally {
      SQLFunctions.closeStatement(stmt);
    }
  }

  /**
   * Create Tournaments table.
   */
  /* package */static void tournaments(final Connection connection, final boolean forceRebuild, final Collection<String> tables) throws SQLException {
    Statement stmt = null;
    try {
      stmt = connection.createStatement();

      if (forceRebuild) {
        stmt.executeUpdate("DROP TABLE IF EXISTS Tournaments CASCADE");
      }

      if (forceRebuild
          || !tables.contains("Tournaments".toLowerCase())) {
        stmt.executeUpdate("CREATE TABLE Tournaments ("
            + "tournament_id INTEGER GENERATED BY DEFAULT AS IDENTITY" //
            + ",Name varchar(128) NOT NULL" //
            + ",Location longvarchar" //
            + ",NextTournament INTEGER default NULL" //
            + ",CONSTRAINT tournaments_pk PRIMARY KEY (tournament_id)" //
            + ",CONSTRAINT name_unique UNIQUE(Name)" //
            + ")");
        stmt.executeUpdate("INSERT INTO Tournaments (Name, Location) VALUES ('DUMMY', 'Default dummy tournament')");
        stmt.executeUpdate("INSERT INTO Tournaments (Name, Location) VALUES ('DROP', 'Dummy tournament for teams that drop out')");
        // TODO can we add a constraint that NextTournament must refer to Name
        // and still handle null?

        // add internal tournament for default values and such
        // FIXME add internal tournament if it doesn't exist

      }

    } finally {
      SQLFunctions.closeStatement(stmt);
    }
  }

  private static void renameTournament(final Connection connection,
                                       final int tournament,
                                       final String newName) throws SQLException {
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("UPDATE Tournaments SET Name = ? WHERE tournament_id = ?");
      prep.setString(1, newName);
      prep.setInt(2, tournament);
      prep.executeUpdate();
    } finally {
      SQLFunctions.closePreparedStatement(prep);
    }
  }
  
  /**
   * Create the "internal" tournament used for default parameters, if needed.
   */
  /* package */static void createInternalTournament(final Connection connection) throws SQLException {
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      // make sure another tournament with the internal name doesn't exist
      prep = connection.prepareStatement("SELECT tournament_id FROM Tournaments WHERE Name = ?");
      prep.setString(1, INTERNAL_TOURNAMENT_NAME);
      rs = prep.executeQuery();
      if (rs.next()) {
        final int importedInternal = rs.getInt(1);
        if(importedInternal != INTERNAL_TOURNAMENT_ID) {
          renameTournament(connection, importedInternal, "Imported-" + INTERNAL_TOURNAMENT_NAME);
        }
      }
      SQLFunctions.closeResultSet(rs);
      rs = null;
      SQLFunctions.closePreparedStatement(prep);
      prep = null;

      // check if a tournament exists with the internal id
      prep = connection.prepareStatement("SELECT Name FROM Tournaments WHERE tournament_id = ?");
      prep.setInt(1, INTERNAL_TOURNAMENT_ID);
      rs = prep.executeQuery();
      if (!rs.next()) {
        SQLFunctions.closePreparedStatement(prep);
        prep = null;
        SQLFunctions.closeResultSet(rs);
        rs = null;

        // need to create
        prep = connection.prepareStatement("INSERT INTO Tournaments (tournament_id, Name) VALUES(?, ?)");
        prep.setInt(1, INTERNAL_TOURNAMENT_ID);
        prep.setString(2, INTERNAL_TOURNAMENT_NAME);
        prep.executeUpdate();
      }
    } finally {
      SQLFunctions.closeResultSet(rs);
      SQLFunctions.closePreparedStatement(prep);
    }
  }

  /**
   * Put the default parameters in the database if they don't already exist.
   */
  /* package */static void setDefaultParameters(final Connection connection) throws SQLException {
    createInternalTournament(connection);

    PreparedStatement globalInsert = null;
    PreparedStatement tournamentInsert = null;
    boolean check;
    try {
      globalInsert = connection.prepareStatement("INSERT INTO global_parameters (param_value, param) VALUES (?, ?)");

      check = Queries.globalParameterExists(connection, GlobalParameters.CURRENT_TOURNAMENT);
      if (!check) {
        globalInsert.setString(2, GlobalParameters.CURRENT_TOURNAMENT);
        globalInsert.setInt(1, Queries.getTournamentID(connection, "DUMMY"));
        globalInsert.executeUpdate();
      }

      check = Queries.globalParameterExists(connection, GlobalParameters.STANDARDIZED_MEAN);
      if (!check) {
        globalInsert.setString(2, GlobalParameters.STANDARDIZED_MEAN);
        globalInsert.setDouble(1, GlobalParameters.STANDARDIZED_MEAN_DEFAULT);
        globalInsert.executeUpdate();
      }

      check = Queries.globalParameterExists(connection, GlobalParameters.STANDARDIZED_SIGMA);
      if (!check) {
        globalInsert.setString(2, GlobalParameters.STANDARDIZED_SIGMA);
        globalInsert.setDouble(1, GlobalParameters.STANDARDIZED_SIGMA_DEFAULT);
        globalInsert.executeUpdate();
      }

      check = Queries.globalParameterExists(connection, GlobalParameters.SCORESHEET_LAYOUT_NUP);
      if (!check) {
        globalInsert.setString(2, GlobalParameters.SCORESHEET_LAYOUT_NUP);
        globalInsert.setInt(1, GlobalParameters.SCORESHEET_LAYOUT_NUP_DEFAULT);
        globalInsert.executeUpdate();
      }

      Queries.setNumSeedingRounds(connection, INTERNAL_TOURNAMENT_ID, TournamentParameters.SEEDING_ROUNDS_DEFAULT);
    } finally {
      SQLFunctions.closePreparedStatement(globalInsert);
      SQLFunctions.closePreparedStatement(tournamentInsert);
    }
  }

  private static void insertOrUpdateChallengeDocument(final Document document, final Connection connection) throws SQLException {
    PreparedStatement challengePrep = null;
    try {
      final boolean check = Queries.globalParameterExists(connection, GlobalParameters.CHALLENGE_DOCUMENT);
      if (check) {
        challengePrep = connection.prepareStatement("UPDATE global_parameters SET param_value = ? WHERE param = ?");
      } else {
        challengePrep = connection.prepareStatement("INSERT INTO global_parameters (param_value, param) VALUES (?, ?)");
      }

      challengePrep.setString(2, GlobalParameters.CHALLENGE_DOCUMENT);

      // get the challenge descriptor put in the database
      try {
        // dump the document into a byte array so we can push it into the
        // database
        final XMLWriter xmlwriter = new XMLWriter();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xmlwriter.setOutput(baos, "UTF8");
        xmlwriter.write(document);
        final byte[] bytes = baos.toByteArray();
        final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        challengePrep.setAsciiStream(1, bais, bytes.length);
        challengePrep.executeUpdate();
        SQLFunctions.closePreparedStatement(challengePrep);
      } catch (final UnsupportedEncodingException e) {
        throw new FLLRuntimeException("Internal error, UTF8 not supported as an encoding!", e);
      }
    } finally {
      SQLFunctions.closePreparedStatement(challengePrep);
    }
  }

  /* package */static void createGlobalParameters(final Document document,
                                                  final Connection connection,
                                                  final boolean forceRebuild,
                                                  final Collection<String> tables) throws SQLException {
    Statement stmt = null;
    PreparedStatement insertPrep = null;
    PreparedStatement deletePrep = null;
    try {
      stmt = connection.createStatement();

      if (forceRebuild) {
        stmt.executeUpdate("DROP TABLE IF EXISTS global_parameters CASCADE");
      }

      if (forceRebuild
          || !tables.contains("global_parameters".toLowerCase())) {
        stmt.executeUpdate("CREATE TABLE global_parameters (" //
            + "  param varchar(64) NOT NULL" //
            + " ,param_value longvarchar NOT NULL" //
            + " ,CONSTRAINT global_parameters_pk PRIMARY KEY (param)" //
            + ")");
      }

      // set database version
      deletePrep = connection.prepareStatement("DELETE FROM global_parameters WHERE param = ?");
      deletePrep.setString(1, GlobalParameters.DATABASE_VERSION);
      deletePrep.executeUpdate();
      insertPrep = connection.prepareStatement("INSERT INTO global_parameters (param_value, param) VALUES (?, ?)");
      insertPrep.setInt(1, DATABASE_VERSION);
      insertPrep.setString(2, GlobalParameters.DATABASE_VERSION);
      insertPrep.executeUpdate();

      insertOrUpdateChallengeDocument(document, connection);

    } finally {
      SQLFunctions.closeStatement(stmt);
      SQLFunctions.closePreparedStatement(insertPrep);
      SQLFunctions.closePreparedStatement(deletePrep);
    }
  }

  /**
   * Generate the definition of a column for the given goal element.
   * 
   * @param goalElement element that represents the goal
   * @return the column definition
   */
  private static String generateGoalColumnDefinition(final Element goalElement) {
    final String goalName = goalElement.getAttribute("name");

    // check if there are any subelements to determine if this goal is
    // enumerated or not

    String definition = goalName;
    final List<Element> posValues = XMLUtils.filterToElements(goalElement.getElementsByTagName("value"));
    if (posValues.size() > 0) {
      // enumerated
      // HSQLDB doesn't support enum
      definition += " longvarchar";
    } else {
      definition += " float";
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("GoalColumnDefinition: "
          + definition);
    }

    return definition;
  }

}
