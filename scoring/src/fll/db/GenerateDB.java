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
import fll.xml.XMLUtils;
import fll.xml.XMLWriter;

/**
 * Generate tables for fll tournament from XML document
 * 
 * @version $Revision$
 */
public final class GenerateDB {

  /**
   * Version of the database that will be created.
   */
  public static final int DATABASE_VERSION = 1;

  private static final Logger LOGGER = Logger.getLogger(GenerateDB.class);

  /**
   * Region name for internal teams. These teams should not be deleted.
   */
  public static final String INTERNAL_REGION = "INTERNAL";

  private GenerateDB() {

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

  /**
   * Generate a completely new DB from document. This also stores the document
   * in the database for later use.
   * 
   * @param document and XML document that describes a tournament
   * @param connection connection to the database to create the tables in
   * @param forceRebuild recreate all tables from scratch, if false don't
   *          recreate the tables that hold team information
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", }, justification = "Need dynamic data for default values")
  public static void generateDB(final Document document, final Connection connection, final boolean forceRebuild) throws SQLException,
      UnsupportedEncodingException {

    Statement stmt = null;
    PreparedStatement prep = null;
    try {
      stmt = connection.createStatement();

      stmt.executeUpdate("SET WRITE_DELAY 100 MILLIS");

      final Collection<String> tables = Queries.getTablesInDB(connection);

      globalParameters(connection, forceRebuild, tables);

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

      // Table structure for table 'TournamentParameters'
      if (forceRebuild) {
        stmt.executeUpdate("DROP TABLE IF EXISTS TournamentParameters CASCADE");
      }
      if (forceRebuild
          || !tables.contains("TournamentParameters".toLowerCase())) {
        stmt.executeUpdate("CREATE TABLE TournamentParameters ("
            + "  Param varchar(64) NOT NULL," //
            + "  Value longvarchar NOT NULL," //
            + "  CONSTRAINT tournament_parameters_pk PRIMARY KEY  (Param)" + ")");

        // populate tournament parameters with default values

        // inverted order of Value and Param so that the update statement and
        // the insert statement both have the same order of parameters
        prep = connection.prepareStatement("INSERT INTO TournamentParameters (Value, Param) VALUES (?, ?)");
        prep.setString(2, TournamentParameters.CURRENT_TOURNAMENT);
        prep.setInt(1, Queries.getTournamentID(connection, "DUMMY"));
        prep.executeUpdate();

        prep.setString(2, TournamentParameters.SEEDING_ROUNDS);
        prep.setInt(1, TournamentParameters.SEEDING_ROUNDS_DEFAULT);
        prep.executeUpdate();

        prep.setString(2, TournamentParameters.STANDARDIZED_MEAN);
        prep.setDouble(1, TournamentParameters.STANDARDIZED_MEAN_DEFAULT);
        prep.executeUpdate();

        prep.setString(2, TournamentParameters.STANDARDIZED_SIGMA);
        prep.setDouble(1, TournamentParameters.STANDARDIZED_SIGMA_DEFAULT);
        prep.executeUpdate();

        prep.setString(2, TournamentParameters.CHALLENGE_DOCUMENT);
      } else {
        prep = connection.prepareStatement("UPDATE TournamentParameters SET Value = ? WHERE Param = ?");
        prep.setString(2, TournamentParameters.CHALLENGE_DOCUMENT);
      }

      // dump the document into a byte array so we can push it into the database
      final XMLWriter xmlwriter = new XMLWriter();
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      xmlwriter.setOutput(baos, "UTF8");
      xmlwriter.write(document);
      final byte[] bytes = baos.toByteArray();
      final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      prep.setAsciiStream(1, bais, bytes.length);
      prep.executeUpdate();
      SQLFunctions.closePreparedStatement(prep);

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
      stmt.executeUpdate("DROP VIEW IF EXISTS performance_seeding_max");
      stmt
          .executeUpdate("CREATE VIEW performance_seeding_max AS SELECT TeamNumber, Tournament, Max(ComputedTotal) As Score FROM Performance WHERE NoShow = 0 AND RunNumber <= (SELECT Value FROM TournamentParameters WHERE TournamentParameters.Param = 'SeedingRounds') GROUP BY TeamNumber, Tournament");

      // current tournament teams
      stmt.executeUpdate("DROP VIEW IF EXISTS current_tournament_teams");
      stmt
          .executeUpdate("CREATE VIEW current_tournament_teams AS SELECT * FROM TournamentTeams WHERE Tournament IN (SELECT Value FROM TournamentParameters WHERE Param = 'CurrentTournament')");

      // verified performance scores
      stmt.executeUpdate("DROP VIEW IF EXISTS verified_performance");
      stmt.executeUpdate("CREATE VIEW verified_performance AS SELECT "
          + performanceColumns.toString() + " FROM Performance WHERE Verified = TRUE");

    } finally {
      SQLFunctions.closeStatement(stmt);
      SQLFunctions.closePreparedStatement(prep);
    }

  }

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
      }
    } finally {
      SQLFunctions.closeStatement(stmt);
    }
  }

  /* package */static void globalParameters(final Connection connection, final boolean forceRebuild, final Collection<String> tables) throws SQLException {
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

      insertPrep = connection.prepareStatement("INSERT INTO global_parameters (param_value, param) VALUES (?, ?)");
      deletePrep = connection.prepareStatement("DELETE FROM global_parameters WHERE param = ?");

      // set database version
      deletePrep.setString(1, GlobalParameters.DATABASE_VERSION);
      deletePrep.executeUpdate();
      insertPrep.setInt(1, DATABASE_VERSION);
      insertPrep.setString(2, GlobalParameters.DATABASE_VERSION);
      insertPrep.executeUpdate();

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
