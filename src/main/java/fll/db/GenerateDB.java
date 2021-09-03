/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Formatter;

import org.apache.commons.io.IOUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Team;
import fll.Tournament;
import fll.TournamentLevel;
import fll.TournamentLevel.NoSuchTournamentLevelException;
import fll.Utilities;
import fll.util.FLLInternalException;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.PerformanceScoreCategory;
import fll.xml.SubjectiveScoreCategory;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Generate tables for tournament from challenge description.
 */
public final class GenerateDB {

  /**
   * Version of the database that will be created.
   */
  public static final int DATABASE_VERSION = 32;

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private GenerateDB() {
    // no instances
  }

  /**
   * Default team name.
   */
  public static final String DEFAULT_TEAM_NAME = "<No Name>";

  /**
   * Default team division.
   */
  public static final String DEFAULT_TEAM_DIVISION = "1";

  /**
   * Name of the default tournament.
   */
  public static final String DUMMY_TOURNAMENT_NAME = "DUMMY";

  /**
   * Internal tournament ID.
   */
  public static final int INTERNAL_TOURNAMENT_ID = -1;

  /**
   * Internal tournament name.
   */
  public static final String INTERNAL_TOURNAMENT_NAME = "__INTERNAL__";

  /**
   * ID for {@link #INTERNAL_TOURNAMENT_LEVEL_NAME}}.
   */
  public static final int INTERNAL_TOURNAMENT_LEVEL_ID = INTERNAL_TOURNAMENT_ID;

  /**
   * Name of level for {@link #INTERNAL_TOURNAMENT_NAME}.
   */
  public static final String INTERNAL_TOURNAMENT_LEVEL_NAME = INTERNAL_TOURNAMENT_NAME;

  /**
   * Name of the performance table.
   */
  public static final String PERFORMANCE_TABLE_NAME = PerformanceScoreCategory.CATEGORY_NAME;

  /**
   * Generate a completely new DB from a challenge description. This also stores
   * the description
   * in the database for later use. Any existing data in the database is removed.
   *
   * @param description tournament description
   * @param connection connection to the database to create the tables in
   * @throws SQLException on a database error
   * @throws UnsupportedEncodingException if the challenge description cannot be
   *           decoded
   */
  @SuppressFBWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE",
                                "OBL_UNSATISFIED_OBLIGATION" }, justification = "Need dynamic data for default values, Bug in findbugs - ticket:2924739")
  public static void generateDB(final ChallengeDescription description,
                                final Connection connection)
      throws SQLException, UnsupportedEncodingException {

    try (Statement stmt = connection.createStatement()) {

      // write to disk regularly in case of a crash
      stmt.executeUpdate("SET WRITE_DELAY 100 MILLIS");

      // use MVCC transaction model to handle high rate of updates from multiple
      // threads
      stmt.executeUpdate("SET DATABASE TRANSACTION CONTROL MVCC");

      // make sure the database is empty
      stmt.executeUpdate("DROP SCHEMA PUBLIC CASCADE");

      createGlobalParameters(description, connection);

      // authentication tables
      createAuthentication(connection);
      createAuthenticationRoles(connection, true);

      // Table structure for table 'Tournaments'
      createTournamentLevelsTable(connection, true);
      tournaments(connection);

      // Table structure for table 'Teams'
      stmt.executeUpdate("CREATE TABLE Teams ("
          + "  TeamNumber integer NOT NULL," //
          + "  TeamName varchar(255) default '"
          + DEFAULT_TEAM_NAME
          + "' NOT NULL," //
          + "  Organization varchar(255)," //
          + "  CONSTRAINT teams_pk PRIMARY KEY (TeamNumber)"
          + ")");

      // add the bye team so that references work
      try (
          PreparedStatement prep = connection.prepareStatement("INSERT INTO Teams(TeamNumber, TeamName) VALUES(?, ?)")) {
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
      }

      // Table structure for table 'tablenames'
      stmt.executeUpdate("CREATE TABLE tablenames ("
          + "  Tournament INTEGER NOT NULL," //
          + "  PairID INTEGER NOT NULL," //
          + "  SideA varchar(64) NOT NULL," //
          + "  SideB varchar(64) NOT NULL," //
          + "  CONSTRAINT tablenames_pk PRIMARY KEY (Tournament,PairID)" //
          + " ,CONSTRAINT tablenames_fk1 FOREIGN KEY(Tournament) REFERENCES Tournaments(tournament_id)"
          + ")");

      createTableDivision(connection, true);

      // table to hold head-to-head playoff meta-data
      stmt.executeUpdate("CREATE TABLE PlayoffData ("
          + " event_division varchar(32) NOT NULL," //
          + " Tournament INTEGER  NOT NULL," //
          + " PlayoffRound integer NOT NULL," // round of this set of playoff
                                              // brackets
          + " LineNumber integer NOT NULL," // the line in the brackets that are
                                            // displayed
          + " Team integer default "
          + Team.NULL_TEAM_NUMBER
          + "," //
          + " AssignedTable varchar(64) default NULL," //
          + " Printed boolean default FALSE," //
          + " run_number integer NOT NULL," // the performance run number for
                                            // this score
          + " CONSTRAINT playoff_data_pk PRIMARY KEY (event_division, Tournament, PlayoffRound, LineNumber)" //
          + ",CONSTRAINT playoff_data_fk1 FOREIGN KEY(Tournament) REFERENCES Tournaments(tournament_id)" //
          + ",CONSTRAINT playoff_data_fk2 FOREIGN KEY(Team) REFERENCES Teams(TeamNumber)" //
          + ")");

      // table to track which teams are in which playoff bracket
      createPlayoffBracketTeams(connection);

      // track which brackets are automatically finished
      createAutomaticFinishedPlayoffTable(connection, true);

      // table to hold team numbers of teams in this tournament
      stmt.executeUpdate("CREATE TABLE TournamentTeams ("
          + "  TeamNumber integer NOT NULL" //
          + " ,Tournament INTEGER NOT NULL" //
          + " ,event_division varchar(32) default '"
          + DEFAULT_TEAM_DIVISION
          + "' NOT NULL" //
          + " ,judging_station varchar(64) NOT NULL"
          + " ,CONSTRAINT tournament_teams_pk PRIMARY KEY (TeamNumber, Tournament)" //
          + " ,CONSTRAINT tournament_teams_fk1 FOREIGN KEY(TeamNumber) REFERENCES Teams(TeamNumber)" //
          + " ,CONSTRAINT tournament_teams_fk2 FOREIGN KEY(Tournament) REFERENCES Tournaments(tournament_id)"
          + ")");

      tournamentParameters(connection);

      createScheduleTables(connection, true);

      createSubjectiveCategoryScheduleColumnMappingTables(connection);

      createNonNumericNomineesTables(connection, true);

      createFinalistScheduleTables(connection, true);

      // Table structure for table 'Judges'
      stmt.executeUpdate("CREATE TABLE Judges ("
          + "  id varchar(64) NOT NULL,"//
          + "  category longvarchar NOT NULL," //
          + "  Tournament INTEGER NOT NULL," //
          + "  station varchar(64) NOT NULL," //
          + "  CONSTRAINT judges_pk PRIMARY KEY (id,category,Tournament,station)"//
          + " ,CONSTRAINT judges_fk1 FOREIGN KEY(Tournament) REFERENCES Tournaments(tournament_id)" //
          + ")");

      final StringBuilder createStatement = new StringBuilder();

      // performance

      // used for view below
      final StringBuilder performanceColumns = new StringBuilder();
      {
        final PerformanceScoreCategory performanceElement = description.getPerformance();
        final String tableName = PERFORMANCE_TABLE_NAME;
        createStatement.append("CREATE TABLE "
            + tableName
            + " (");
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
        for (final AbstractGoal element : performanceElement.getAllGoals()) {
          if (!element.isComputed()) {
            final String columnDefinition = generateGoalColumnDefinition(element);
            createStatement.append(" "
                + columnDefinition
                + ",");
            performanceColumns.append(element.getName()
                + ",");
          }
        }
        performanceColumns.append("ComputedTotal,");
        createStatement.append(" ComputedTotal float DEFAULT NULL,");
        performanceColumns.append("StandardizedScore");
        createStatement.append(" StandardizedScore float default NULL,");
        createStatement.append(" CONSTRAINT "
            + tableName
            + "_pk PRIMARY KEY (TeamNumber, Tournament, RunNumber)");
        createStatement.append(",CONSTRAINT "
            + tableName
            + "_fk1 FOREIGN KEY(TeamNumber) REFERENCES Teams(TeamNumber)");
        createStatement.append(",CONSTRAINT "
            + tableName
            + "_fk2 FOREIGN KEY(Tournament) REFERENCES Tournaments(tournament_id)");
        createStatement.append(");");
        stmt.executeUpdate(createStatement.toString());

      }

      // loop over each subjective category and create a table for it
      for (final SubjectiveScoreCategory categoryElement : description.getSubjectiveCategories()) {
        createStatement.setLength(0);

        final String tableName = categoryElement.getName();

        createStatement.append("CREATE TABLE "
            + tableName
            + " (");
        createStatement.append(" TeamNumber INTEGER NOT NULL,");
        createStatement.append(" Tournament INTEGER NOT NULL,");
        createStatement.append(" Judge VARCHAR(64) NOT NULL,");
        createStatement.append(" NoShow boolean DEFAULT FALSE NOT NULL,");
        for (final AbstractGoal element : categoryElement.getAllGoals()) {
          final String columnDefinition = generateGoalColumnDefinition(element);
          createStatement.append(" "
              + columnDefinition
              + ",");

          createStatement.append(String.format(" %s longvarchar DEFAULT NULL,", getGoalCommentColumnName(element)));
        }
        createStatement.append(" note longvarchar DEFAULT NULL,");

        createStatement.append(" comment_great_job longvarchar DEFAULT NULL,");
        createStatement.append(" comment_think_about longvarchar DEFAULT NULL,");

        createStatement.append(" CONSTRAINT "
            + tableName
            + "_pk PRIMARY KEY (TeamNumber, Tournament, Judge)");
        createStatement.append(",CONSTRAINT "
            + tableName
            + "_fk1 FOREIGN KEY(TeamNumber) REFERENCES Teams(TeamNumber)");
        createStatement.append(",CONSTRAINT "
            + tableName
            + "_fk2 FOREIGN KEY(Tournament) REFERENCES Tournaments(tournament_id)");
        createStatement.append(");");
        stmt.executeUpdate(createStatement.toString());
      }

      createSubjectiveComputedScoresTable(connection, true);
      createFinalScoresTable(connection, true);
      createOverallScoresTable(connection, true);

      createSubjectiveAwardWinnerTables(connection, true);
      createAdvancingTeamsTable(connection, true);

      createAwardGroupOrder(connection, true);

      createDelayedPerformanceTable(connection, true);

      createFinalistParameterTables(connection, true);

      createCategoriesIgnored(connection, true);

      createAwardsScriptTables(connection, true);

      // --------------- create views ---------------

      // number of seeding rounds for each tournament
      stmt.executeUpdate("DROP VIEW IF EXISTS tournament_seeding_rounds");
      stmt.executeUpdate("CREATE VIEW tournament_seeding_rounds AS" //
          + " SELECT T1.tournament_id, " //
          + "      (SELECT TP3.param_value FROM tournament_parameters AS TP3" //
          + "   WHERE TP3.param = 'SeedingRounds'" //
          + "      AND TP3.tournament = ( " //
          + "      SELECT MAX(TP2.tournament) FROM tournament_parameters AS TP2 " //
          + "           WHERE TP2.param = '"
          + TournamentParameters.SEEDING_ROUNDS
          + "' " //
          + "           AND TP2.tournament IN (-1, T1.tournament_id ) )) as seeding_rounds" //
          + "      FROM tournaments as T1");

      // max seeding round score for all tournaments
      stmt.executeUpdate("DROP VIEW IF EXISTS performance_seeding_max");
      stmt.executeUpdate("CREATE VIEW performance_seeding_max AS "//
          + "    SELECT MAX(Performance.TeamNumber) AS TeamNumber" //
          + "         , MAX(Performance.ComputedTotal) AS score" //
          + "         , AVG(Performance.ComputedTotal) AS average" //
          + "         , Performance.tournament" //
          + "    FROM Performance, tournament_seeding_rounds AS TSR" //
          + "    WHERE Performance.RunNumber <= TSR.seeding_rounds" //
          + "    GROUP BY Performance.tournament, Performance.TeamNumber");

      // verified performance scores
      stmt.executeUpdate("DROP VIEW IF EXISTS verified_performance");
      stmt.executeUpdate("CREATE VIEW verified_performance AS SELECT "
          + performanceColumns.toString()
          + " FROM Performance WHERE Verified = TRUE");

      setDefaultParameters(connection, TournamentParameters.RUNNING_HEAD_2_HEAD_DEFAULT);

    }

  }

  /**
   * @param goal the goal to get the column name for
   * @return the name of the column that stores the comments for this goal
   */
  public static String getGoalCommentColumnName(final AbstractGoal goal) {
    return goal.getName()
        + "_comment";
  }

  /* package */static void createNonNumericNomineesTables(final Connection connection,
                                                          final boolean createConstraints)
      throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      final StringBuilder sql = new StringBuilder();
      sql.append("CREATE TABLE non_numeric_nominees (");
      sql.append("  tournament INTEGER NOT NULL");
      sql.append(" ,category LONGVARCHAR NOT NULL");
      sql.append(" ,team_number INTEGER NOT NULL");
      sql.append(" ,judge VARCHAR(64) DEFAULT NULL");
      if (createConstraints) {
        sql.append(" ,CONSTRAINT non_numeric_nominees_fk1 FOREIGN KEY(tournament) REFERENCES Tournaments(tournament_id)");
        sql.append(" ,CONSTRAINT non_numeric_nominees_fk2 FOREIGN KEY(team_number) REFERENCES Teams(TeamNumber)");
      }
      sql.append(")");
      stmt.executeUpdate(sql.toString());
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
                                                        final boolean createConstraints)
      throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      final StringBuilder sql = new StringBuilder();
      sql.append("CREATE TABLE finalist_categories (");
      sql.append("  tournament INTEGER NOT NULL");
      sql.append(" ,category LONGVARCHAR NOT NULL");
      sql.append(" ,division VARCHAR(32) NOT NULL");
      sql.append(" ,room VARCHAR(32) DEFAULT NULL");
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
      scheduleSql.append(" ,judge_end_time TIME NOT NULL");
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
    }
  }

  /**
   * Create the 'authentication' table. Drops the table if it exists.
   * 
   * @param connection database connection
   * @throws SQLException on a database error
   */
  public static void createAuthentication(final Connection connection) throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      stmt.executeUpdate("CREATE TABLE fll_authentication ("
          + "  fll_user varchar(64) NOT NULL" //
          + " ,fll_pass char(32)"//
          + " ,num_failures INTEGER DEFAULT 0 NOT NULL" //
          + " ,last_failure TIMESTAMP DEFAULT NULL" //
          + " ,CONSTRAINT fll_authentication_pk PRIMARY KEY (fll_user)" //
          + ")");
    }
  }

  /**
   * Create the 'auth_roles" table. Drops the table if it exists.
   * 
   * @param connection database connection
   * @param createConstraints if true create the constraints
   * @throws SQLException on a database error
   */
  public static void createAuthenticationRoles(final Connection connection,
                                               final boolean createConstraints)
      throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      final StringBuilder sql = new StringBuilder();
      sql.append("CREATE TABLE auth_roles (");
      sql.append("  fll_user VARCHAR(64) NOT NULL");
      sql.append(" ,fll_role VARCHAR(64) NOT NULL");
      if (createConstraints) {
        sql.append(" ,CONSTRAINT auth_roles_pk PRIMARY KEY (fll_user, fll_role)");
        sql.append(" ,CONSTRAINT auth_roles_fk1 FOREIGN KEY(fll_user) REFERENCES fll_authentication(fll_user)");
      }
      sql.append(")");
      stmt.executeUpdate(sql.toString());

    }
  }

  /** Table structure for table 'tournament_parameters' */
  /* package */static void tournamentParameters(final Connection connection) throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      stmt.executeUpdate("CREATE TABLE tournament_parameters ("
          + "  param varchar(64) NOT NULL" //
          + " ,param_value longvarchar NOT NULL" //
          + " ,tournament integer NOT NULL" //
          + " ,CONSTRAINT tournament_parameters_pk PRIMARY KEY  (param, tournament)" //
          + " ,CONSTRAINT tournament_parameters_fk1 FOREIGN KEY(tournament) REFERENCES Tournaments(tournament_id)" //
          + ")");
    }
  }

  /**
   * Create Tournaments table.
   */
  /* package */static void tournaments(final Connection connection) throws SQLException {
    try (Statement stmt = connection.createStatement()) {

      stmt.executeUpdate("CREATE TABLE Tournaments ("
          + "  tournament_id INTEGER GENERATED BY DEFAULT AS IDENTITY" //
          + " ,Name varchar(128) NOT NULL" //
          + " ,Location longvarchar" //
          + " ,performance_seeding_modified TIMESTAMP DEFAULT NULL" //
          + " ,subjective_modified TIMESTAMP DEFAULT NULL" //
          + " ,summary_computed TIMESTAMP DEFAULT NULL" //
          + " ,tournament_date DATE DEFAULT NULL" //
          + " ,level_id INTEGER NOT NULL" //
          + " ,CONSTRAINT tournaments_pk PRIMARY KEY (tournament_id)" //
          + " ,CONSTRAINT name_unique UNIQUE(Name)" //
          + ")");

      try {
        final TournamentLevel defaultLevel = TournamentLevel.getByName(connection,
                                                                       TournamentLevel.DEFAULT_TOURNAMENT_LEVEL_NAME);
        Tournament.createTournament(connection, DUMMY_TOURNAMENT_NAME, "Default dummy tournament", null, defaultLevel);
      } catch (final NoSuchTournamentLevelException e) {
        throw new FLLInternalException("Cannot find the default tournament level named", e);
      }

      // add internal tournament for default values and such
      createInternalTournament(connection);
    }
  }

  /**
   * Create the "internal" tournament used for default parameters, if needed.
   */
  /* package */static void createInternalTournamentLevel(final Connection connection) throws SQLException {
    // make sure another level with the internal name doesn't exist
    if (TournamentLevel.levelExists(connection, INTERNAL_TOURNAMENT_LEVEL_NAME)) {
      final TournamentLevel level = TournamentLevel.getByName(connection, INTERNAL_TOURNAMENT_LEVEL_NAME);
      if (INTERNAL_TOURNAMENT_LEVEL_ID != level.getId()) {
        if (TournamentLevel.NO_NEXT_LEVEL_ID == level.getNextLevelId()) {
          TournamentLevel.updateTournamentLevel(connection, level.getId(), "Imported-"
              + level.getName());
        } else {
          final TournamentLevel nextLevel = TournamentLevel.getById(connection, level.getNextLevelId());
          TournamentLevel.updateTournamentLevel(connection, level.getId(), "Imported-"
              + level.getName(), nextLevel);
        }
      }
    }

    // check for internal tournament level by id
    if (!TournamentLevel.levelExists(connection, INTERNAL_TOURNAMENT_LEVEL_ID)) {
      // cannot use standard method as we need to set the id
      try (
          PreparedStatement prepInsert = connection.prepareStatement("INSERT INTO tournament_level (level_id, level_name, next_level_id) VALUES(?, ?, ?)")) {
        prepInsert.setInt(1, INTERNAL_TOURNAMENT_LEVEL_ID);
        prepInsert.setString(2, INTERNAL_TOURNAMENT_LEVEL_NAME);
        prepInsert.setInt(3, TournamentLevel.NO_NEXT_LEVEL_ID);
        prepInsert.executeUpdate();
      }
    }
  }

  /**
   * Create the "internal" tournament used for default parameters, if needed.
   */
  /* package */static void createInternalTournament(final Connection connection) throws SQLException {
    createInternalTournamentLevel(connection);

    // make sure another tournament with the internal name doesn't exist
    if (Tournament.doesTournamentExist(connection, INTERNAL_TOURNAMENT_NAME)) {
      final Tournament importedInternal = Tournament.findTournamentByName(connection, INTERNAL_TOURNAMENT_NAME);
      if (INTERNAL_TOURNAMENT_ID != importedInternal.getTournamentID()) {
        Tournament.updateTournament(connection, importedInternal.getTournamentID(), "Imported-"
            + importedInternal.getName(), importedInternal.getDescription(), importedInternal.getDate(),
                                    importedInternal.getLevel());
      }
    }

    // check if a tournament exists with the internal id
    if (!Tournament.doesTournamentExist(connection, INTERNAL_TOURNAMENT_ID)) {
      try {
        final TournamentLevel internalLevel = TournamentLevel.getById(connection, INTERNAL_TOURNAMENT_LEVEL_ID);

        // cannot use standard method as we need to set the id
        try (
            PreparedStatement prepInsert = connection.prepareStatement("INSERT INTO Tournaments (tournament_id, Name, level_id) VALUES(?, ?, ?)")) {
          prepInsert.setInt(1, INTERNAL_TOURNAMENT_ID);
          prepInsert.setString(2, INTERNAL_TOURNAMENT_NAME);
          prepInsert.setInt(3, internalLevel.getId());
          prepInsert.executeUpdate();
        }

      } catch (final NoSuchTournamentLevelException e) {
        throw new FLLInternalException("Cannot find the default tournament level", e);
      }
    }
  }

  /**
   * Put the default parameters (global and tournament) in the database if they
   * don't already exist.
   *
   * @param headToHead what to set running head to head to, if upgrading this is
   *          true, otherwise it's the default value
   */
  /* package */static void setDefaultParameters(final Connection connection,
                                                final boolean headToHead)
      throws SQLException {
    try (
        PreparedStatement globalInsert = connection.prepareStatement("INSERT INTO global_parameters (param_value, param) VALUES (?, ?)");
        PreparedStatement findTournament = connection.prepareStatement("SELECT tournament_id FROM Tournaments WHERE Name = ?")) {
      // Global Parameters

      boolean check;
      check = GlobalParameters.globalParameterExists(connection, GlobalParameters.CURRENT_TOURNAMENT);
      if (!check) {
        findTournament.setString(1, DUMMY_TOURNAMENT_NAME);
        try (ResultSet rs = findTournament.executeQuery()) {
          if (rs.next()) {
            final int tournamentId = rs.getInt(1);
            globalInsert.setString(2, GlobalParameters.CURRENT_TOURNAMENT);
            globalInsert.setInt(1, tournamentId);
            globalInsert.executeUpdate();
          }
        }
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

      // Tournament Parameters
      if (!TournamentParameters.defaultParameterExists(connection, TournamentParameters.SEEDING_ROUNDS)) {
        TournamentParameters.setDefaultNumSeedingRounds(connection, TournamentParameters.SEEDING_ROUNDS_DEFAULT);
      }

      if (!TournamentParameters.defaultParameterExists(connection,
                                                       TournamentParameters.PERFORMANCE_ADVANCEMENT_PERCENTAGE)) {
        TournamentParameters.setDefaultPerformanceAdvancementPercentage(connection,
                                                                        TournamentParameters.PERFORMANCE_ADVANCEMENT_PERCENTAGE_DEFAULT);
      }

      if (!TournamentParameters.defaultParameterExists(connection, TournamentParameters.RUNNING_HEAD_2_HEAD)) {
        TournamentParameters.setDefaultRunningHeadToHead(connection, headToHead);
      }
    }
  }

  /**
   * Replace the challenge description in the database. It is assumed that it
   * is compatible with the database.
   * 
   * @param description to put in the database
   * @param connection the database connection
   * @throws SQLException on a database error
   */
  public static void insertOrUpdateChallengeDocument(final ChallengeDescription description,
                                                     final Connection connection)
      throws SQLException {
    final boolean check = GlobalParameters.globalParameterExists(connection, GlobalParameters.CHALLENGE_DOCUMENT);
    LOGGER.trace("Inserting challenge into database. Check: {}", check);

    try (PreparedStatement challengePrep = check //
        ? connection.prepareStatement("UPDATE global_parameters SET param_value = ? WHERE param = ?") //
        : connection.prepareStatement("INSERT INTO global_parameters (param_value, param) VALUES (?, ?)")) {
      challengePrep.setString(2, GlobalParameters.CHALLENGE_DOCUMENT);

      // get the challenge descriptor put in the database
      // dump the document into a byte array so we can push it into the
      // database
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      XMLUtils.writeXML(description.toXml(), new OutputStreamWriter(baos, Utilities.DEFAULT_CHARSET),
                        Utilities.DEFAULT_CHARSET.name());
      final byte[] bytes = baos.toByteArray();
      final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      challengePrep.setAsciiStream(1, bais, bytes.length);
      challengePrep.executeUpdate();
    }
  }

  /* package */static void createGlobalParameters(final ChallengeDescription description,
                                                  final Connection connection)
      throws SQLException {
    try (Statement stmt = connection.createStatement()) {

      stmt.executeUpdate("CREATE TABLE global_parameters (" //
          + "  param varchar(64) NOT NULL" //
          + " ,param_value longvarchar NOT NULL" //
          + " ,CONSTRAINT global_parameters_pk PRIMARY KEY (param)" //
          + ")");
    }

    // set database version
    try (PreparedStatement deletePrep = connection.prepareStatement("DELETE FROM global_parameters WHERE param = ?")) {
      deletePrep.setString(1, GlobalParameters.DATABASE_VERSION);
      deletePrep.executeUpdate();
    }

    try (
        PreparedStatement insertPrep = connection.prepareStatement("INSERT INTO global_parameters (param_value, param) VALUES (?, ?)")) {
      insertPrep.setInt(1, DATABASE_VERSION);
      insertPrep.setString(2, GlobalParameters.DATABASE_VERSION);
      insertPrep.executeUpdate();

      insertOrUpdateChallengeDocument(description, connection);
    }
  }

  /**
   * Get SQL type for a given goal.
   * 
   * @param goalElement the goal to get it's SQL type for
   * @return SQL type
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

    final String definition = String.format("%s %s", goalName, getTypeForGoalColumn(goalElement));

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("GoalColumnDefinition: "
          + definition);
    }

    return definition;
  }

  /* package */static void createTableDivision(final Connection connection,
                                               final boolean createConstraints)
      throws SQLException {
    try (Statement stmt = connection.createStatement()) {

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
    }
  }

  /**
   * Create tables for schedule data
   *
   * @param connection
   * @param createConstraints if false, don't create foreign key constraints
   * @throws SQLException
   */
  /* package */static void createScheduleTables(final Connection connection,
                                                final boolean createConstraints)
      throws SQLException {
    try (Statement stmt = connection.createStatement()) {

      final StringBuilder sql = new StringBuilder();
      sql.append("CREATE TABLE schedule (");
      sql.append("  tournament INTEGER NOT NULL");
      sql.append(" ,team_number INTEGER NOT NULL");
      sql.append(" ,judging_station LONGVARCHAR NOT NULL");
      sql.append(" ,CONSTRAINT schedule_pk PRIMARY KEY (tournament, team_number)");
      if (createConstraints) {
        sql.append(" ,CONSTRAINT schedule_fk1 FOREIGN KEY(tournament) REFERENCES Tournaments(tournament_id)");
        sql.append(" ,CONSTRAINT schedule_fk2 FOREIGN KEY(team_number) REFERENCES Teams(TeamNumber)");
      }
      sql.append(")");
      stmt.executeUpdate(sql.toString());

      final StringBuilder perfRoundsSql = new StringBuilder();
      perfRoundsSql.append("CREATE TABLE sched_perf_rounds (");
      perfRoundsSql.append("  tournament INTEGER NOT NULL");
      perfRoundsSql.append(" ,team_number INTEGER NOT NULL");
      perfRoundsSql.append(" ,perf_time TIME NOT NULL");
      perfRoundsSql.append(" ,table_color LONGVARCHAR NOT NULL");
      perfRoundsSql.append(" ,table_side INTEGER NOT NULL");
      perfRoundsSql.append(" ,practice BOOLEAN NOT NULL");
      perfRoundsSql.append(" ,CONSTRAINT sched_perf_rounds_pk PRIMARY KEY (tournament, team_number, perf_time)");
      if (createConstraints) {
        perfRoundsSql.append(" ,CONSTRAINT sched_perf_rounds_fk1 FOREIGN KEY(tournament, team_number) REFERENCES schedule(tournament, team_number)");
      }
      perfRoundsSql.append(")");
      stmt.executeUpdate(perfRoundsSql.toString());

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
    }
  }

  /**
   * Create the subjective_computed_scores table.
   *
   * @param connection database connection
   * @param createConstraints true if creating constraints, false when using from
   *          {@link ImportDB} to upgrade a database
   */
  /* package */ static void createSubjectiveComputedScoresTable(final Connection connection,
                                                                final boolean createConstraints)
      throws SQLException {
    try (Statement stmt = connection.createStatement()) {

      final StringBuilder sql = new StringBuilder();
      sql.append("CREATE TABLE subjective_computed_scores (");
      sql.append("  category LONGVARCHAR NOT NULL");
      sql.append(" ,goal_group LONGVARCHAR NOT NULL");
      sql.append(" ,tournament INTEGER NOT NULL");
      sql.append(" ,team_number INTEGER NOT NULL");
      sql.append(" ,judge VARCHAR(64) NOT NULL");
      sql.append(" ,computed_total float");
      sql.append(" ,no_show BOOLEAN DEFAULT FALSE NOT NULL");
      sql.append(" ,standardized_score float");
      if (createConstraints) {
        sql.append(" ,CONSTRAINT subjective_computed_scores_pk1 PRIMARY KEY (category, goal_group, team_number, tournament, judge)");
        sql.append(" ,CONSTRAINT subjective_computed_scores_fk1 FOREIGN KEY(team_number) REFERENCES Teams(TeamNumber)");
        sql.append(" ,CONSTRAINT subjective_computed_scores_fk2 FOREIGN KEY(tournament) REFERENCES Tournaments(tournament_id)");
      }
      sql.append(")");

      stmt.executeUpdate(sql.toString());
    }
  }

  /**
   * Create the final_scores table.
   *
   * @param connection database connection
   * @param createConstraints true if creating constraints, false when using from
   *          {@link ImportDB} to upgrade a database
   */
  /* package */ static void createFinalScoresTable(final Connection connection,
                                                   final boolean createConstraints)
      throws SQLException {
    try (Statement stmt = connection.createStatement()) {

      final StringBuilder sql = new StringBuilder();
      sql.append("CREATE TABLE final_scores (");
      sql.append("  category LONGVARCHAR NOT NULL");
      sql.append(" ,goal_group LONGVARCHAR NOT NULL");
      sql.append(" ,tournament INTEGER NOT NULL");
      sql.append(" ,team_number INTEGER NOT NULL");
      sql.append(" ,final_score float");
      if (createConstraints) {
        sql.append(" ,CONSTRAINT final_scores_pk01 PRIMARY KEY (category, goal_group, team_number, tournament)");
        sql.append(" ,CONSTRAINT final_scores_fk01 FOREIGN KEY(team_number) REFERENCES Teams(TeamNumber)");
        sql.append(" ,CONSTRAINT final_scores_fk02 FOREIGN KEY(tournament) REFERENCES Tournaments(tournament_id)");
      }
      sql.append(")");

      stmt.executeUpdate(sql.toString());
    }
  }

  /**
   * Create the overall_scores table.
   *
   * @param connection database connection
   * @param createConstraints true if creating constraints, false when using from
   *          {@link ImportDB} to upgrade a database
   */
  /* package */ static void createOverallScoresTable(final Connection connection,
                                                     final boolean createConstraints)
      throws SQLException {
    try (Statement stmt = connection.createStatement()) {

      final StringBuilder sql = new StringBuilder();
      sql.append("CREATE TABLE overall_scores (");
      sql.append("  tournament INTEGER NOT NULL");
      sql.append(" ,team_number INTEGER NOT NULL");
      sql.append(" ,overall_score float");
      if (createConstraints) {
        sql.append(" ,CONSTRAINT overall_scores_pk PRIMARY KEY (team_number, tournament)");
        sql.append(" ,CONSTRAINT overall_scores_fk1 FOREIGN KEY(team_number) REFERENCES Teams(TeamNumber)");
        sql.append(" ,CONSTRAINT overall_scores_fk2 FOREIGN KEY(tournament) REFERENCES Tournaments(tournament_id)");
      }
      sql.append(")");

      stmt.executeUpdate(sql.toString());
    }
  }

  /**
   * Create tables for mapping schedule columns (schedule columns)
   * to subjective categories.
   *
   * @param connection
   * @throws SQLException
   */
  /* package */static void createSubjectiveCategoryScheduleColumnMappingTables(final Connection connection)
      throws SQLException {
    try (Statement stmt = connection.createStatement()) {

      final StringBuilder sql = new StringBuilder();
      sql.append("CREATE TABLE category_schedule_column (");
      sql.append("  tournament INTEGER NOT NULL");
      sql.append(" ,category LONGVARCHAR NOT NULL");
      sql.append(" ,schedule_column LONGVARCHAR NOT NULL");
      sql.append(" ,CONSTRAINT category_schedule_column_pk PRIMARY KEY (tournament, category)");
      sql.append(")");
      stmt.executeUpdate(sql.toString());

    }
  }

  /**
   * Create the table playoff_bracket_teams.
   *
   * @param connection
   */
  /* package */ static void createPlayoffBracketTeams(final Connection connection) throws SQLException {
    try (Statement stmt = connection.createStatement()) {

      final StringBuilder sql = new StringBuilder();
      sql.append("CREATE TABLE playoff_bracket_teams (");
      sql.append("  tournament_id INTEGER NOT NULL");
      sql.append(" ,bracket_name LONGVARCHAR NOT NULL");
      sql.append(" ,team_number INTEGER NOT NULL");
      sql.append(" ,CONSTRAINT playoff_bracket_teams_pk PRIMARY KEY (tournament_id, bracket_name, team_number)");
      sql.append(")");
      stmt.executeUpdate(sql.toString());

    }
  }

  /* package */ static void createSubjectiveAwardWinnerTables(final Connection connection,
                                                              final boolean createConstraints)
      throws SQLException {
    try (Statement stmt = connection.createStatement()) {

      final StringBuilder subjectiveOverallAward = new StringBuilder();
      subjectiveOverallAward.append("CREATE TABLE subjective_overall_award (");
      subjectiveOverallAward.append("  tournament_id INTEGER NOT NULL");
      subjectiveOverallAward.append(" ,name LONGVARCHAR NOT NULL");
      subjectiveOverallAward.append(" ,team_number INTEGER NOT NULL");
      subjectiveOverallAward.append(" ,description LONGVARCHAR");
      subjectiveOverallAward.append(" ,place INTEGER NOT NULL");
      if (createConstraints) {
        subjectiveOverallAward.append(" ,CONSTRAINT subjective_overall_award_pk PRIMARY KEY (tournament_id, name, team_number)");
        subjectiveOverallAward.append(" ,CONSTRAINT subjective_overall_award_fk1 FOREIGN KEY(tournament_id) REFERENCES Tournaments(tournament_id)");
        subjectiveOverallAward.append(" ,CONSTRAINT subjective_overall_award_fk2 FOREIGN KEY(team_number) REFERENCES Teams(TeamNumber)");
      }
      subjectiveOverallAward.append(")");
      stmt.executeUpdate(subjectiveOverallAward.toString());

      final StringBuilder subjectiveExtraAward = new StringBuilder();
      subjectiveExtraAward.append("CREATE TABLE subjective_extra_award (");
      subjectiveExtraAward.append("  tournament_id INTEGER NOT NULL");
      subjectiveExtraAward.append(" ,name LONGVARCHAR NOT NULL");
      subjectiveExtraAward.append(" ,team_number INTEGER NOT NULL");
      subjectiveExtraAward.append(" ,description LONGVARCHAR");
      subjectiveExtraAward.append(" ,award_group LONGVARCHAR NOT NULL");
      subjectiveExtraAward.append(" ,place INTEGER NOT NULL");
      if (createConstraints) {
        subjectiveExtraAward.append(" ,CONSTRAINT subjective_extra_award_pk PRIMARY KEY (tournament_id, name, team_number)");
        subjectiveExtraAward.append(" ,CONSTRAINT subjective_extra_award_fk1 FOREIGN KEY(tournament_id) REFERENCES Tournaments(tournament_id)");
        subjectiveExtraAward.append(" ,CONSTRAINT subjective_extra_award_fk2 FOREIGN KEY(team_number) REFERENCES Teams(TeamNumber)");
      }
      subjectiveExtraAward.append(")");
      stmt.executeUpdate(subjectiveExtraAward.toString());

      final StringBuilder subjectiveChallengeAward = new StringBuilder();
      subjectiveChallengeAward.append("CREATE TABLE subjective_challenge_award (");
      subjectiveChallengeAward.append("  tournament_id INTEGER NOT NULL");
      subjectiveChallengeAward.append(" ,name LONGVARCHAR NOT NULL");
      subjectiveChallengeAward.append(" ,team_number INTEGER NOT NULL");
      subjectiveChallengeAward.append(" ,description LONGVARCHAR");
      subjectiveChallengeAward.append(" ,award_group LONGVARCHAR NOT NULL");
      subjectiveChallengeAward.append(" ,place INTEGER NOT NULL");
      if (createConstraints) {
        subjectiveChallengeAward.append(" ,CONSTRAINT subjective_challenge_award_pk PRIMARY KEY (tournament_id, name, team_number)");
        subjectiveChallengeAward.append(" ,CONSTRAINT subjective_challenge_award_fk1 FOREIGN KEY(tournament_id) REFERENCES Tournaments(tournament_id)");
        subjectiveChallengeAward.append(" ,CONSTRAINT subjective_challenge_award_fk2 FOREIGN KEY(team_number) REFERENCES Teams(TeamNumber)");
      }
      subjectiveChallengeAward.append(")");
      stmt.executeUpdate(subjectiveChallengeAward.toString());
    }
  }

  /* package */ static void createAdvancingTeamsTable(final Connection connection,
                                                      final boolean createConstraints)
      throws SQLException {
    try (Statement stmt = connection.createStatement()) {

      final StringBuilder sql = new StringBuilder();
      sql.append("CREATE TABLE advancing_teams (");
      sql.append("  tournament_id INTEGER NOT NULL");
      sql.append(" ,team_number INTEGER NOT NULL");
      sql.append(" ,award_group LONGVARCHAR");
      if (createConstraints) {
        sql.append(" ,CONSTRAINT advancing_teams_pk PRIMARY KEY (tournament_id, team_number)");
        sql.append(" ,CONSTRAINT advancing_teams_fk1 FOREIGN KEY(tournament_id) REFERENCES Tournaments(tournament_id)");
        sql.append(" ,CONSTRAINT advancing_teams_fk2 FOREIGN KEY(team_number) REFERENCES Teams(TeamNumber)");
      }
      sql.append(")");
      stmt.executeUpdate(sql.toString());
    }
  }

  /* package */ static void createAutomaticFinishedPlayoffTable(final Connection connection,
                                                                final boolean createConstraints)
      throws SQLException {
    try (Statement stmt = connection.createStatement()) {

      final StringBuilder sql = new StringBuilder();
      sql.append("CREATE TABLE automatic_finished_playoff (");
      sql.append("  tournament_id INTEGER NOT NULL");
      sql.append(" ,bracket_name LONGVARCHAR NOT NULL");
      if (createConstraints) {
        sql.append(" ,CONSTRAINT automatic_finished_playoff_pk PRIMARY KEY (tournament_id, bracket_name)");
        sql.append(" ,CONSTRAINT automatic_finished_playoff_fk1 FOREIGN KEY(tournament_id) REFERENCES Tournaments(tournament_id)");

      }
      sql.append(")");
      stmt.executeUpdate(sql.toString());
    }
  }

  /* package */ static void createAwardGroupOrder(final Connection connection,
                                                  final boolean createConstraints)
      throws SQLException {
    try (Statement stmt = connection.createStatement()) {

      final StringBuilder sql = new StringBuilder();
      sql.append("CREATE TABLE award_group_order (");
      sql.append("  tournament_id INTEGER NOT NULL");
      sql.append(" ,award_group LONGVARCHAR NOT NULL");
      sql.append(" ,sort_order INTEGER NOT NULL");
      if (createConstraints) {
        sql.append(" ,CONSTRAINT award_group_order_pk PRIMARY KEY (tournament_id, award_group)");
        sql.append(" ,CONSTRAINT award_group_order_fk1 FOREIGN KEY(tournament_id) REFERENCES Tournaments(tournament_id)");
      }
      sql.append(")");
      stmt.executeUpdate(sql.toString());
    }
  }

  /* package */ static void createDelayedPerformanceTable(final Connection connection,
                                                          final boolean createConstraints)
      throws SQLException {
    try (Statement stmt = connection.createStatement()) {

      final StringBuilder sql = new StringBuilder();
      sql.append("CREATE TABLE delayed_performance (");
      sql.append("  tournament_id INTEGER NOT NULL");
      sql.append(" ,run_number INTEGER NOT NULL");
      sql.append(" ,delayed_until TIMESTAMP NOT NULL");
      if (createConstraints) {
        sql.append(" ,CONSTRAINT performance_delay_pk PRIMARY KEY(tournament_id, run_number)");
        sql.append(" ,CONSTRAINT performance_delay_fk1 FOREIGN KEY(tournament_id) REFERENCES Tournaments(tournament_id)");
      }
      sql.append(")");

      stmt.executeUpdate(sql.toString());
    }
  }

  /**
   * Create the tables to store finalist parameter information.
   *
   * @param connection
   */
  /* package */ static void createFinalistParameterTables(final Connection connection,
                                                          final boolean createConstraints)
      throws SQLException {
    LOGGER.trace("Creating finalist parameter tables. Create constraints: {}", createConstraints);

    try (Statement stmt = connection.createStatement()) {

      final StringBuilder sql = new StringBuilder();
      sql.append("CREATE TABLE playoff_schedules (");
      sql.append("    tournament_id INTEGER NOT NULL");
      sql.append("   ,bracket_name LONGVARCHAR NOT NULL");
      sql.append("   ,start_time TIME NOT NULL");
      sql.append("   ,end_time TIME NOT NULL");
      if (createConstraints) {
        sql.append("   ,CONSTRAINT playoff_schedules_pk PRIMARY KEY(tournament_id, bracket_name)");
        sql.append("   ,CONSTRAINT playoff_schedules_fk1 FOREIGN KEY(tournament_id) REFERENCES Tournaments(tournament_id)");
      }
      sql.append(")");
      stmt.executeUpdate(sql.toString());

      final StringBuilder sql2 = new StringBuilder();
      sql2.append("CREATE TABLE finalist_parameters (");
      sql2.append("    tournament_id INTEGER NOT NULL");
      sql2.append("   ,award_group LONGVARCHAR NOT NULL");
      sql2.append("   ,start_time TIME NOT NULL");
      sql2.append("   ,slot_duration INTEGER NOT NULL");
      if (createConstraints) {
        sql2.append("   ,CONSTRAINT finalist_parameters_pk PRIMARY KEY(tournament_id, award_group)");
        sql2.append("   ,CONSTRAINT finalist_parameters_fk1 FOREIGN KEY(tournament_id) REFERENCES Tournaments(tournament_id)");
      }
      sql2.append(")");
      stmt.executeUpdate(sql2.toString());

    }
  }

  /**
   * Create the tournament levels table and create the default level.
   * 
   * @param connection database connection
   * @param createConstraints true if constraints shoudl be created, false
   *          typically for import and upgrade
   * @throws SQLException on a database error
   */
  /* package */ static void createTournamentLevelsTable(final Connection connection,
                                                        final boolean createConstraints)
      throws SQLException {
    try (Statement stmt = connection.createStatement()) {

      final StringBuilder sql = new StringBuilder();
      sql.append("CREATE TABLE tournament_level (");
      sql.append("  level_id INTEGER GENERATED BY DEFAULT AS IDENTITY");
      sql.append(" ,level_name LONGVARCHAR NOT NULL");
      sql.append(" ,next_level_id INTEGER NOT NULL");
      if (createConstraints) {
        sql.append(" ,CONSTRAINT tournament_level_pk PRIMARY KEY (level_id)");
        sql.append(" ,CONSTRAINT tournament_level_name_unique UNIQUE(level_name)");
      }
      sql.append(")");

      stmt.executeUpdate(sql.toString());

      createInternalTournamentLevel(connection);

      TournamentLevel.createTournamentLevel(connection, TournamentLevel.DEFAULT_TOURNAMENT_LEVEL_NAME);
    }
  }

  /**
   * Table of categories to not award at a tournament level.
   * 
   * @param connection database connections
   * @param createConstrints if constraints should be created
   * @throws SQLException on a database error
   */
  /* package */ static void createCategoriesIgnored(final Connection connection,
                                                    final boolean createConstrints)
      throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      final StringBuilder sql = new StringBuilder();
      sql.append("CREATE TABLE categories_ignored (");
      sql.append("  level_id INTEGER NOT NULL");
      sql.append(" ,category_identifier LONGVARCHAR NOT NULL");
      sql.append(" ,category_type VARCHAR(64) NOT NULL");
      if (createConstrints) {
        sql.append(" ,CONSTRAINT categories_ignored_pk PRIMARY KEY (level_id, category_identifier)");
        sql.append(" ,CONSTRAINT categories_ignored_level_fk FOREIGN KEY(level_id) REFERENCES tournament_level(level_id) ON DELETE CASCADE");
      }
      sql.append(")");

      stmt.executeUpdate(sql.toString());

    }
  }

  private static void createAwardsScriptTable(final Connection connection,
                                              final boolean createConstraints,
                                              final String tableName,
                                              final String keyColumn,
                                              final String valueColumn)
      throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      final Formatter sql = new Formatter();

      sql.format("CREATE TABLE %s (", tableName);
      sql.format("  tournament_level_id INTEGER NOT NULL");
      sql.format(" ,tournament_id INTEGER NOT NULL");
      sql.format(" ,layer_rank INTEGER NOT NULL");
      sql.format(" ,%s VARCHAR(64) NOT NULL", keyColumn);
      sql.format(" ,%s LONGVARCHAR NOT NULL", valueColumn);
      if (createConstraints) {
        sql.format(" ,CONSTRAINT %s_pk PRIMARY KEY (%s, tournament_level_id, tournament_id, layer_rank)", tableName,
                   keyColumn);
      }
      sql.format(")");
      stmt.executeUpdate(sql.toString());
    }
  }

  /**
   * Create tables used to store information about the awards script.
   * This also populates the default values.
   * 
   * @param connection database connections
   * @param createConstrints if constraints should be created
   * @throws SQLException on a database error
   */
  /* package */ static void createAwardsScriptTables(final Connection connection,
                                                     final boolean createConstraints)
      throws SQLException {
    try (Statement stmt = connection.createStatement()) {

      // store text for sections
      createAwardsScriptTable(connection, createConstraints, "awards_script_text", "section_name", "text");

      try (PreparedStatement insert = connection.prepareStatement("INSERT INTO awards_script_text"
          + " (section_name, tournament_level_id, tournament_id, layer_rank, text)"//
          + " VALUES(?, ?, ?, ?, ?)")) {
        insert.setInt(2, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID);
        insert.setInt(3, GenerateDB.INTERNAL_TOURNAMENT_ID);
        insert.setInt(4, AwardsScript.Layer.SEASON.getRank());

        final ClassLoader loader = Utilities.getClassLoader();

        for (final AwardsScript.Section section : AwardsScript.Section.values()) {
          try (InputStream stream = loader.getResourceAsStream(String.format("fll/resources/awards-script/%s.txt",
                                                                             section.name()))) {
            if (null != stream) {
              final String text = IOUtils.toString(stream, Utilities.DEFAULT_CHARSET);

              insert.setString(1, section.name());
              insert.setString(5, text);
              insert.executeUpdate();
            }
          } catch (final IOException e) {
            LOGGER.warn("Error loading default text for section {}, skipping", section.name(), e);
          }
        }
      }

      // store values for macros
      createAwardsScriptTable(connection, createConstraints, "awards_script_parameters", "param_name", "param_value");

      // store descriptions for categories and presenters
      createAwardsScriptTable(connection, createConstraints, "awards_script_subjective_text", "category_name", "text");
      createAwardsScriptTable(connection, createConstraints, "awards_script_subjective_presenter", "category_name",
                              "presenter");
      createAwardsScriptTable(connection, createConstraints, "awards_script_nonnumeric_text", "category_title", "text");
      createAwardsScriptTable(connection, createConstraints, "awards_script_nonnumeric_presenter", "category_title",
                              "presenter");

      // store awards order
      {
        final StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE awards_script_award_order (");
        sql.append("  tournament_level_id INTEGER NOT NULL");
        sql.append(" ,tournament_id INTEGER NOT NULL");
        sql.append(" ,layer_rank INTEGER NOT NULL");
        sql.append(" ,award LONGVARCHAR NOT NULL");
        sql.append(" ,award_rank INTEGER NOT NULL");
        if (createConstraints) {
          sql.append(" ,CONSTRAINT awards_script_award_order_pk PRIMARY KEY (award, tournament_level_id, tournament_id)");
        }
        sql.append(")");
        stmt.executeUpdate(sql.toString());
      }

      // store sponsor order
      {
        final StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE awards_script_sponsor_order (");
        sql.append("  tournament_level_id INTEGER NOT NULL");
        sql.append(" ,tournament_id INTEGER NOT NULL");
        sql.append(" ,layer_rank INTEGER NOT NULL");
        sql.append(" ,sponsor LONGVARCHAR NOT NULL");
        sql.append(" ,sponsor_rank INTEGER NOT NULL");
        if (createConstraints) {
          sql.append(" ,CONSTRAINT awards_script_sponsor_order_pk PRIMARY KEY (sponsor, tournament_level_id, tournament_id)");
        }
        sql.append(")");
        stmt.executeUpdate(sql.toString());
      }

      // FIXME add these tables to the database import process

    }
  }
}
