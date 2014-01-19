/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import fll.Team;
import fll.Tournament;
import fll.Utilities;
import fll.util.LogUtils;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.PerformanceScoreCategory;
import fll.xml.ScoreCategory;
import fll.xml.XMLUtils;

/**
 * Generate tables for tournament from XML document
 */
public final class GenerateDB {

  /**
   * Version of the database that will be created.
   */
  public static final int DATABASE_VERSION = 11;

  private static final Logger LOGGER = LogUtils.getLogger();

  private GenerateDB() {
    // no instances
  }

  public static final String DEFAULT_TEAM_NAME = "<No Name>";

  public static final String DEFAULT_TEAM_DIVISION = "1";

  public static final String DUMMY_TOURNAMENT_NAME = "DUMMY";

  public static final String DROP_TOURNAMENT_NAME = "DROP";

  public static final int INTERNAL_TOURNAMENT_ID = -1;

  public static final String INTERNAL_TOURNAMENT_NAME = "INTERNAL";

  /**
   * Generate a completely new DB from document. This also stores the document
   * in the database for later use.
   * 
   * @param document and XML document that describes a tournament
   * @param connection connection to the database to create the tables in
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE",
                                                             "OBL_UNSATISFIED_OBLIGATION" }, justification = "Need dynamic data for default values, Bug in findbugs - ticket:2924739")
  public static void generateDB(final Document document,
                                final Connection connection) throws SQLException, UnsupportedEncodingException {

    Statement stmt = null;
    PreparedStatement prep = null;
    try {
      stmt = connection.createStatement();

      // write to disk regularly in case of a crash
      stmt.executeUpdate("SET WRITE_DELAY 100 MILLIS");

      // use MVCC transaction model to handle high rate of updates from multiple
      // threads
      stmt.executeUpdate("SET DATABASE TRANSACTION CONTROL MVCC");

      createGlobalParameters(document, connection);

      final ChallengeDescription description = new ChallengeDescription(document.getDocumentElement());

      // authentication tables
      createAuthentication(connection);
      createValidLogin(connection);

      // Table structure for table 'Tournaments'
      tournaments(connection);

      // Table structure for table 'Teams'
      stmt.executeUpdate("DROP TABLE IF EXISTS Teams CASCADE");
      stmt.executeUpdate("CREATE TABLE Teams ("
          + "  TeamNumber integer NOT NULL," //
          + "  TeamName varchar(255) default '" + DEFAULT_TEAM_NAME + "' NOT NULL," //
          + "  Organization varchar(255)," //
          + "  Division varchar(32) default '" + DEFAULT_TEAM_DIVISION + "' NOT NULL," //
          + "  CONSTRAINT teams_pk PRIMARY KEY (TeamNumber)" + ")");

      // add the bye team so that references work
      prep = connection.prepareStatement("INSERT INTO Teams(TeamNumber, TeamName) VALUES(?, ?)");
      prep.setInt(1, Team.BYE.getTeamNumber());
      prep.setString(2, Team.BYE.getTeamName());
      prep.executeUpdate();

      // add the tie team so that references work
      prep.setInt(1, Team.TIE.getTeamNumber());
      prep.setString(2, Team.TIE.getTeamName());
      prep.executeUpdate();

      // add the null team so that references work
      prep.setInt(1, Team.NULL.getTeamNumber());
      prep.setString(2, Team.NULL.getTeamName());
      prep.executeUpdate();

      SQLFunctions.close(prep);

      // Table structure for table 'tablenames'
      stmt.executeUpdate("DROP TABLE IF EXISTS tablenames CASCADE");
      stmt.executeUpdate("CREATE TABLE tablenames ("
          + "  Tournament INTEGER NOT NULL," //
          + "  PairID INTEGER NOT NULL," //
          + "  SideA varchar(64) NOT NULL," //
          + "  SideB varchar(64) NOT NULL," //
          + "  CONSTRAINT tablenames_pk PRIMARY KEY (Tournament,PairID)" //
          + " ,CONSTRAINT tablenames_fk1 FOREIGN KEY(Tournament) REFERENCES Tournaments(tournament_id)" + ")");

      createTableDivision(connection, true);
      
      // table to hold head-to-head playoff meta-data
      stmt.executeUpdate("DROP TABLE IF EXISTS PlayoffData CASCADE");
      stmt.executeUpdate("CREATE TABLE PlayoffData ("
          + " event_division varchar(32) NOT NULL," //
          + " Tournament INTEGER  NOT NULL," //
          + " PlayoffRound integer NOT NULL," // round of this set of playoff
                                              // brackets
          + " LineNumber integer NOT NULL," // the line in the brackets that are
                                            // displayed
          + " Team integer default " + Team.NULL_TEAM_NUMBER + "," //
          + " AssignedTable varchar(64) default NULL," //
          + " Printed boolean default FALSE," //
          + " run_number integer NOT NULL," // the performance run number for
                                            // this score
          + " CONSTRAINT playoff_data_pk PRIMARY KEY (event_division, Tournament, PlayoffRound, LineNumber)" //
          + ",CONSTRAINT playoff_data_fk1 FOREIGN KEY(Tournament) REFERENCES Tournaments(tournament_id)" //
          + ",CONSTRAINT playoff_data_fk2 FOREIGN KEY(Team) REFERENCES Teams(TeamNumber)" //
          + ")");

      // table to hold team numbers of teams in this tournament
      stmt.executeUpdate("DROP TABLE IF EXISTS TournamentTeams CASCADE");
      stmt.executeUpdate("CREATE TABLE TournamentTeams ("
          + "  TeamNumber integer NOT NULL" //
          + " ,Tournament INTEGER NOT NULL" //
          + " ,event_division varchar(32) default '" + DEFAULT_TEAM_DIVISION
          + "' NOT NULL" //
          + " ,judging_station varchar(64) NOT NULL"
          + " ,CONSTRAINT tournament_teams_pk PRIMARY KEY (TeamNumber, Tournament)" //
          + " ,CONSTRAINT tournament_teams_fk1 FOREIGN KEY(TeamNumber) REFERENCES Teams(TeamNumber)" //
          + " ,CONSTRAINT tournament_teams_fk2 FOREIGN KEY(Tournament) REFERENCES Tournaments(tournament_id)" + ")");

      tournamentParameters(connection);

      createScheduleTables(connection, true);

      createFinalistScheduleTables(connection, true);

      // Table structure for table 'Judges'
      stmt.executeUpdate("DROP TABLE IF EXISTS Judges CASCADE");
      stmt.executeUpdate("CREATE TABLE Judges ("
          + "  id varchar(64) NOT NULL,"//
          + "  category varchar(64) NOT NULL," //
          + "  Tournament INTEGER NOT NULL," //
          + "  station varchar(64) NOT NULL," //
          + "  phone varchar(15) default NULL,"
          + "  CONSTRAINT judges_pk PRIMARY KEY (id,category,Tournament,station)"//
          + " ,CONSTRAINT judges_fk1 FOREIGN KEY(Tournament) REFERENCES Tournaments(tournament_id)" //
          + ")");

      final StringBuilder createStatement = new StringBuilder();

      // performance
      final StringBuilder performanceColumns = new StringBuilder(); // used for
      // view
      // below
      {
        final PerformanceScoreCategory performanceElement = description.getPerformance();
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
        for (final AbstractGoal element : performanceElement.getGoals()) {
          if (!element.isComputed()) {
            final String columnDefinition = generateGoalColumnDefinition(element);
            createStatement.append(" "
                + columnDefinition + ",");
            performanceColumns.append(element.getName()
                + ",");
          }
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
      for (final ScoreCategory categoryElement : description.getSubjectiveCategories()) {
        createStatement.setLength(0);

        final String tableName = categoryElement.getName();

        stmt.executeUpdate("DROP TABLE IF EXISTS "
            + tableName + " CASCADE");
        createStatement.append("CREATE TABLE "
            + tableName + " (");
        createStatement.append(" TeamNumber INTEGER NOT NULL,");
        createStatement.append(" Tournament INTEGER NOT NULL,");
        createStatement.append(" Judge VARCHAR(64) NOT NULL,");
        createStatement.append(" NoShow boolean DEFAULT FALSE NOT NULL,");
        for (final AbstractGoal element : categoryElement.getGoals()) {
          final String columnDefinition = generateGoalColumnDefinition(element);
          createStatement.append(" "
              + columnDefinition + ",");
        }
        createStatement.append(" note longvarchar DEFAULT NULL,");
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

      // --------------- create views ---------------

      // max seeding round score for the current tournament
      stmt.executeUpdate("DROP VIEW IF EXISTS performance_seeding_max");
      stmt.executeUpdate("CREATE VIEW performance_seeding_max AS "//
          + " SELECT TeamNumber, Tournament, Max(ComputedTotal) As Score, AVG(ComputedTotal) As average" //
          + " FROM Performance" //
          + " WHERE " //
          + " RunNumber <= ("//
          // compute the run number for the current tournament
          + "   SELECT CONVERT(param_value, INTEGER) FROM tournament_parameters" //
          + "     WHERE param = 'SeedingRounds' AND tournament = ("
          + "       SELECT MAX(tournament) FROM tournament_parameters"//
          + "         WHERE param = 'SeedingRounds'"//
          + "           AND ( tournament = -1 OR tournament = ("//
          // current tournament
          + "             SELECT CONVERT(param_value, INTEGER) FROM global_parameters"//
          + "               WHERE  param = '" + GlobalParameters.CURRENT_TOURNAMENT + "'  )"//
          + "        ) )" + " ) GROUP BY TeamNumber, Tournament");

      // current tournament teams
      stmt.executeUpdate("DROP VIEW IF EXISTS current_tournament_teams");
      prep = connection.prepareStatement("CREATE VIEW current_tournament_teams AS "//
          + " SELECT * FROM TournamentTeams" //
          + " WHERE Tournament IN " //
          + " (SELECT CONVERT(param_value, INTEGER) " // " +
          + "      FROM global_parameters " //
          + "      WHERE param = '" + GlobalParameters.CURRENT_TOURNAMENT + "'"//
          + "  )");
      prep.executeUpdate();

      // verified performance scores
      stmt.executeUpdate("DROP VIEW IF EXISTS verified_performance");
      stmt.executeUpdate("CREATE VIEW verified_performance AS SELECT "
          + performanceColumns.toString() + " FROM Performance WHERE Verified = TRUE");

      setDefaultParameters(connection);

    } finally {
      SQLFunctions.close(stmt);
      SQLFunctions.close(prep);
    }

  }

  /**
   * Create finalist schedule tables.
   * 
   * @param connection
   * @param forceRebuild
   * @param tables
   * @param createConstraints if false, don't create foreign key constraints
   */
  /* package */static void createFinalistScheduleTables(final Connection connection,
                                                        final boolean createConstraints) throws SQLException {
    Statement stmt = null;
    try {
      stmt = connection.createStatement();

      stmt.executeUpdate("DROP TABLE IF EXISTS finalist_schedule");
      stmt.executeUpdate("DROP TABLE IF EXISTS finalist_categories");
      final StringBuilder sql = new StringBuilder();
      sql.append("CREATE TABLE finalist_categories (");
      sql.append("  tournament INTEGER NOT NULL");
      sql.append(" ,category LONGVARCHAR NOT NULL");
      sql.append(" ,is_public BOOLEAN NOT NULL");
      sql.append(" ,division VARCHAR(32) NOT NULL");
      sql.append(" ,CONSTRAINT finalist_categories_pk PRIMARY KEY (tournament, category, division)");
      if (createConstraints) {
        sql.append(" ,CONSTRAINT finalist_categories_fk1 FOREIGN KEY(tournament) REFERENCES Tournaments(tournament_id)");
      }
      sql.append(")");
      stmt.executeUpdate(sql.toString());

      final StringBuilder scheduleSql = new StringBuilder();
      scheduleSql.append("CREATE TABLE finalist_schedule (");
      scheduleSql.append("  tournament INTEGER NOT NULL");
      scheduleSql.append(" ,category LONGVARCHAR NOT NULL");
      scheduleSql.append(" ,judge_time TIME NOT NULL");
      scheduleSql.append(" ,team_number INTEGER NOT NULL");
      scheduleSql.append(" ,division VARCHAR(32) NOT NULL");
      scheduleSql.append(" ,CONSTRAINT finalist_schedule_pk PRIMARY KEY (tournament, category, division, judge_time)");
      if (createConstraints) {
        scheduleSql.append(" ,CONSTRAINT finalist_schedule_fk1 FOREIGN KEY(tournament) REFERENCES Tournaments(tournament_id)");
        scheduleSql.append(" ,CONSTRAINT finalist_schedule_fk2 FOREIGN KEY(team_number) REFERENCES Teams(TeamNumber)");
        scheduleSql.append(" ,CONSTRAINT finalist_schedule_fk3 FOREIGN KEY(tournament, category, division) REFERENCES finalist_categories(tournament, category, division)");
      }
      scheduleSql.append(")");
      stmt.executeUpdate(scheduleSql.toString());

    } finally {
      SQLFunctions.close(stmt);
      stmt = null;
    }
  }

  /**
   * Create the 'authentication' table. Drops the table if it exists.
   */
  public static void createAuthentication(final Connection connection) throws SQLException {
    Statement stmt = null;
    try {
      stmt = connection.createStatement();

      stmt.executeUpdate("DROP TABLE IF EXISTS fll_authentication CASCADE");
      connection.commit();
      stmt.executeUpdate("CREATE TABLE fll_authentication ("
          + "  fll_user varchar(64) NOT NULL" //
          + " ,fll_pass char(32)"//
          + " ,CONSTRAINT fll_authentication_pk PRIMARY KEY (fll_user)" //
          + ")");
    } finally {
      SQLFunctions.close(stmt);
    }
  }

  /**
   * Create the 'valid_login' table. Drops the table if it exists.
   */
  public static void createValidLogin(final Connection connection) throws SQLException {
    Statement stmt = null;
    try {
      stmt = connection.createStatement();

      stmt.executeUpdate("DROP TABLE IF EXISTS valid_login CASCADE");
      stmt.executeUpdate("CREATE TABLE valid_login ("
          + "  fll_user varchar(64) NOT NULL" //
          + " ,magic_key varchar(64) NOT NULL" //
          + " ,CONSTRAINT valid_login_pk PRIMARY KEY (fll_user, magic_key)" //
          + ")");
    } finally {
      SQLFunctions.close(stmt);
    }
  }

  /** Table structure for table 'tournament_parameters' */
  /* package */static void tournamentParameters(final Connection connection) throws SQLException {
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      stmt.executeUpdate("DROP TABLE IF EXISTS tournament_parameters CASCADE");
      stmt.executeUpdate("CREATE TABLE tournament_parameters ("
          + "  param varchar(64) NOT NULL" //
          + " ,param_value longvarchar NOT NULL" //
          + " ,tournament integer NOT NULL" //
          + " ,CONSTRAINT tournament_parameters_pk PRIMARY KEY  (param, tournament)" //
          + " ,CONSTRAINT tournament_parameters_fk1 FOREIGN KEY(tournament) REFERENCES Tournaments(tournament_id)" //
          + ")");
    } finally {
      SQLFunctions.close(stmt);
    }
  }

  /**
   * Create Tournaments table.
   */
  /* package */static void tournaments(final Connection connection) throws SQLException {
    Statement stmt = null;
    try {
      stmt = connection.createStatement();

      stmt.executeUpdate("DROP TABLE IF EXISTS Tournaments CASCADE");

      stmt.executeUpdate("CREATE TABLE Tournaments ("
          + "tournament_id INTEGER GENERATED BY DEFAULT AS IDENTITY" //
          + ",Name varchar(128) NOT NULL" //
          + ",Location longvarchar" //
          + ",NextTournament INTEGER default NULL" //
          + ",CONSTRAINT tournaments_pk PRIMARY KEY (tournament_id)" //
          + ",CONSTRAINT name_unique UNIQUE(Name)" //
          + ")");
      Tournament.createTournament(connection, DUMMY_TOURNAMENT_NAME, "Default dummy tournament");
      Tournament.createTournament(connection, DROP_TOURNAMENT_NAME, "Dummy tournament for teams that drop out");

      // add internal tournament for default values and such
      createInternalTournament(connection);

    } finally {
      SQLFunctions.close(stmt);
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
      SQLFunctions.close(prep);
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
        if (importedInternal != INTERNAL_TOURNAMENT_ID) {
          renameTournament(connection, importedInternal, "Imported-"
              + INTERNAL_TOURNAMENT_NAME);
        }
      }
      SQLFunctions.close(rs);
      rs = null;
      SQLFunctions.close(prep);
      prep = null;

      // check if a tournament exists with the internal id
      prep = connection.prepareStatement("SELECT Name FROM Tournaments WHERE tournament_id = ?");
      prep.setInt(1, INTERNAL_TOURNAMENT_ID);
      rs = prep.executeQuery();
      if (!rs.next()) {
        SQLFunctions.close(prep);
        prep = null;
        SQLFunctions.close(rs);
        rs = null;

        // need to create
        prep = connection.prepareStatement("INSERT INTO Tournaments (tournament_id, Name) VALUES(?, ?)");
        prep.setInt(1, INTERNAL_TOURNAMENT_ID);
        prep.setString(2, INTERNAL_TOURNAMENT_NAME);
        prep.executeUpdate();
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Put the default parameters in the database if they don't already exist.
   */
  /* package */static void setDefaultParameters(final Connection connection) throws SQLException {
    createInternalTournament(connection);

    PreparedStatement globalInsert = null;
    boolean check;
    try {
      globalInsert = connection.prepareStatement("INSERT INTO global_parameters (param_value, param) VALUES (?, ?)");

      check = GlobalParameters.globalParameterExists(connection, GlobalParameters.CURRENT_TOURNAMENT);
      if (!check) {
        final Tournament dummyTournament = Tournament.findTournamentByName(connection, DUMMY_TOURNAMENT_NAME);
        globalInsert.setString(2, GlobalParameters.CURRENT_TOURNAMENT);
        globalInsert.setInt(1, dummyTournament.getTournamentID());
        globalInsert.executeUpdate();
      }

      check = GlobalParameters.globalParameterExists(connection, GlobalParameters.STANDARDIZED_MEAN);
      if (!check) {
        globalInsert.setString(2, GlobalParameters.STANDARDIZED_MEAN);
        globalInsert.setDouble(1, GlobalParameters.STANDARDIZED_MEAN_DEFAULT);
        globalInsert.executeUpdate();
      }

      check = GlobalParameters.globalParameterExists(connection, GlobalParameters.STANDARDIZED_SIGMA);
      if (!check) {
        globalInsert.setString(2, GlobalParameters.STANDARDIZED_SIGMA);
        globalInsert.setDouble(1, GlobalParameters.STANDARDIZED_SIGMA_DEFAULT);
        globalInsert.executeUpdate();
      }

      check = GlobalParameters.globalParameterExists(connection, GlobalParameters.DIVISION_FLIP_RATE);
      if (!check) {
        globalInsert.setString(2, GlobalParameters.DIVISION_FLIP_RATE);
        globalInsert.setInt(1, GlobalParameters.DIVISION_FLIP_RATE_DEFAULT);
        globalInsert.executeUpdate();
      }

      Queries.setNumSeedingRounds(connection, INTERNAL_TOURNAMENT_ID, TournamentParameters.SEEDING_ROUNDS_DEFAULT);
      Queries.setMaxScorebaordPerformanceRound(connection, INTERNAL_TOURNAMENT_ID,
                                               TournamentParameters.MAX_SCOREBOARD_ROUND_DEFAULT);
    } finally {
      SQLFunctions.close(globalInsert);
    }
  }

  public static void insertOrUpdateChallengeDocument(final Document document,
                                                     final Connection connection) throws SQLException {
    PreparedStatement challengePrep = null;
    try {
      final boolean check = GlobalParameters.globalParameterExists(connection, GlobalParameters.CHALLENGE_DOCUMENT);
      if (check) {
        challengePrep = connection.prepareStatement("UPDATE global_parameters SET param_value = ? WHERE param = ?");
      } else {
        challengePrep = connection.prepareStatement("INSERT INTO global_parameters (param_value, param) VALUES (?, ?)");
      }

      challengePrep.setString(2, GlobalParameters.CHALLENGE_DOCUMENT);

      // get the challenge descriptor put in the database
      // dump the document into a byte array so we can push it into the
      // database
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      XMLUtils.writeXML(document, new OutputStreamWriter(baos, Utilities.DEFAULT_CHARSET),
                        Utilities.DEFAULT_CHARSET.name());
      final byte[] bytes = baos.toByteArray();
      final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      challengePrep.setAsciiStream(1, bais, bytes.length);
      challengePrep.executeUpdate();
      SQLFunctions.close(challengePrep);
    } finally {
      SQLFunctions.close(challengePrep);
    }
  }

  /* package */static void createGlobalParameters(final Document document,
                                                  final Connection connection) throws SQLException {
    Statement stmt = null;
    PreparedStatement insertPrep = null;
    PreparedStatement deletePrep = null;
    try {
      stmt = connection.createStatement();

      stmt.executeUpdate("DROP TABLE IF EXISTS global_parameters CASCADE");

      stmt.executeUpdate("CREATE TABLE global_parameters (" //
          + "  param varchar(64) NOT NULL" //
          + " ,param_value longvarchar NOT NULL" //
          + " ,CONSTRAINT global_parameters_pk PRIMARY KEY (param)" //
          + ")");

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
      SQLFunctions.close(stmt);
      SQLFunctions.close(insertPrep);
      SQLFunctions.close(deletePrep);
    }
  }

  /**
   * Get SQL type for a given goal.
   */
  public static String getTypeForGoalColumn(final AbstractGoal goalElement) {
    if (goalElement.isEnumerated()) {
      // enumerated
      // HSQLDB doesn't support enum
      return "longvarchar";
    } else {
      return "float";
    }
  }

  /**
   * Generate the definition of a column for the given goal element.
   * 
   * @param goalElement element that represents the goal
   * @return the column definition
   */
  public static String generateGoalColumnDefinition(final AbstractGoal goalElement) {
    final String goalName = goalElement.getName();

    String definition = goalName;
    definition += " "
        + getTypeForGoalColumn(goalElement);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("GoalColumnDefinition: "
          + definition);
    }

    return definition;
  }

  /* package */static void createTableDivision(final Connection connection,
                                               final boolean createConstraints) throws SQLException {
    Statement stmt = null;
    try {
      stmt = connection.createStatement();

      stmt.executeUpdate("DROP TABLE IF EXISTS table_division CASCADE");
      final StringBuilder sql = new StringBuilder();
      sql.append("CREATE TABLE table_division (");
      sql.append("  playoff_division VARCHAR(32) NOT NULL");
      sql.append(" ,tournament INTEGER NOT NULL");
      sql.append(" ,table_id INTEGER NOT NULL");
      sql.append(" ,CONSTRAINT table_division_pk PRIMARY KEY (playoff_division, tournament, table_id)");
      if (createConstraints) {
        sql.append(" ,CONSTRAINT table_division_fk1 FOREIGN KEY(tournament, table_id) REFERENCES tablenames(tournament, PairID)");
      }
      sql.append(")");
      stmt.executeUpdate(sql.toString());

    } finally {
      SQLFunctions.close(stmt);
      stmt = null;
    }
  }

  /**
   * Create tables for schedule data
   * 
   * @param connection
   * @param forceRebuild
   * @param tables
   * @param createConstraints if false, don't create foreign key constraints
   * @throws SQLException
   */
  /* package */static void createScheduleTables(final Connection connection,
                                                final boolean createConstraints) throws SQLException {
    Statement stmt = null;
    try {
      stmt = connection.createStatement();

      stmt.executeUpdate("DROP TABLE IF EXISTS schedule CASCADE");
      final StringBuilder sql = new StringBuilder();
      sql.append("CREATE TABLE schedule (");
      sql.append("  tournament INTEGER NOT NULL");
      sql.append(" ,team_number INTEGER NOT NULL");
      sql.append(" ,judging_station LONGVARCHAR NOT NULL");
      sql.append(" ,presentation TIME");
      sql.append(" ,technical TIME");
      sql.append(" ,CONSTRAINT schedule_pk PRIMARY KEY (tournament, team_number)");
      if (createConstraints) {
        sql.append(" ,CONSTRAINT schedule_fk1 FOREIGN KEY(tournament) REFERENCES Tournaments(tournament_id)");
        sql.append(" ,CONSTRAINT schedule_fk2 FOREIGN KEY(team_number) REFERENCES Teams(TeamNumber)");
      }
      sql.append(")");
      stmt.executeUpdate(sql.toString());

      stmt.executeUpdate("DROP TABLE IF EXISTS sched_perf_rounds CASCADE");
      final StringBuilder perfRoundsSql = new StringBuilder();
      perfRoundsSql.append("CREATE TABLE sched_perf_rounds (");
      perfRoundsSql.append("  tournament INTEGER NOT NULL");
      perfRoundsSql.append(" ,team_number INTEGER NOT NULL");
      perfRoundsSql.append(" ,round INTEGER NOT NULL");
      perfRoundsSql.append(" ,perf_time TIME NOT NULL");
      perfRoundsSql.append(" ,table_color LONGVARCHAR NOT NULL");
      perfRoundsSql.append(" ,table_side INTEGER NOT NULL");
      perfRoundsSql.append(" ,CONSTRAINT sched_perf_rounds_pk PRIMARY KEY (tournament, team_number, round)");
      if (createConstraints) {
        perfRoundsSql.append(" ,CONSTRAINT sched_perf_rounds_fk1 FOREIGN KEY(tournament, team_number) REFERENCES schedule(tournament, team_number)");
      }
      perfRoundsSql.append(")");
      stmt.executeUpdate(perfRoundsSql.toString());

      stmt.executeUpdate("DROP TABLE IF EXISTS sched_subjective CASCADE");
      final StringBuilder subjectiveSql = new StringBuilder();
      subjectiveSql.append("CREATE TABLE sched_subjective (");
      subjectiveSql.append("  tournament INTEGER NOT NULL");
      subjectiveSql.append(" ,team_number INTEGER NOT NULL");
      subjectiveSql.append(" ,name LONGVARCHAR NOT NULL");
      subjectiveSql.append(" ,subj_time TIME NOT NULL");
      subjectiveSql.append(" ,CONSTRAINT sched_subjective_pk PRIMARY KEY (tournament, team_number, name)");
      if (createConstraints) {
        subjectiveSql.append(" ,CONSTRAINT sched_subjective_fk1 FOREIGN KEY(tournament, team_number) REFERENCES schedule(tournament, team_number)");
      }
      subjectiveSql.append(")");
      stmt.executeUpdate(subjectiveSql.toString());

    } finally {
      SQLFunctions.close(stmt);
      stmt = null;
    }
  }

}
