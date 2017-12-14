/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.db;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import au.com.bytecode.opencsv.CSVReader;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Team;
import fll.Tournament;
import fll.TournamentTeam;
import fll.Utilities;
import fll.db.TeamPropertyDifference.TeamProperty;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.web.GatherBugReport;
import fll.web.developer.importdb.ImportDBDump;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.PerformanceScoreCategory;
import fll.xml.ScoreCategory;
import fll.xml.XMLUtils;
import net.mtu.eggplant.io.IOUtils;
import net.mtu.eggplant.util.ComparisonUtils;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * database, otherwise need to map... Import scores from a tournament database
 * into a master score database.
 * <p>
 * Example arguments: jdbc:hsqldb:file:/source;shutdown=true "Centennial Dec10"
 * jdbc:hsqldb:file:/destination;shutdown=true
 */
public final class ImportDB {

  private static final Logger LOGGER = LoggerFactory.getLogger(ImportDB.class);

  /**
   * Import tournament data from one database to another database
   * 
   * @param args source tournament destination
   */
  public static void main(final String[] args) {
    LogUtils.initializeLogging();

    try {
      if (args.length != 3) {
        LOGGER.error("You must specify <source uri> <tournament> <destination uri>");
        for (final String arg : args) {
          LOGGER.error("arg: "
              + arg);
        }
        System.exit(1);
      } else {

        final String sourceURI = args[0];
        // remove quotes from tournament if they exist
        int substringStart = 0;
        int substringEnd = args[1].length();
        if (args[1].charAt(0) == '"'
            || args[1].charAt(0) == '\'') {
          substringStart = 1;
        }
        if (args[1].charAt(substringEnd
            - 1) == '"'
            || args[1].charAt(substringEnd
                - 1) == '\'') {
          substringEnd = substringEnd
              - 1;
        }
        final String tournament = args[1].substring(substringStart, substringEnd);
        final String destinationURI = args[2];

        Utilities.loadDBDriver();

        final Connection sourceConnection = DriverManager.getConnection(sourceURI);
        final Connection destinationConnection = DriverManager.getConnection(destinationURI);
        Statement stmt1 = null;
        Statement stmt2 = null;
        try {
          try {
            stmt1 = sourceConnection.createStatement();
            stmt1.executeUpdate("SET WRITE_DELAY 1 MILLIS");
          } catch (final SQLException sqle) {
            LOGGER.info("Source either isn't HSQLDB or there is a problem", sqle);
          }
          try {
            stmt2 = destinationConnection.createStatement();
            stmt2.executeUpdate("SET WRITE_DELAY 1 MILLIS");
          } catch (final SQLException sqle) {
            LOGGER.info("Destination either isn't HSQLDB or there is a problem", sqle);
          }
        } finally {
          SQLFunctions.close(stmt1);
          SQLFunctions.close(stmt2);
        }

        final boolean differences = checkForDifferences(sourceConnection, destinationConnection, tournament);
        if (!differences) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Importing data for "
                + tournament
                + " from "
                + sourceURI
                + " to "
                + destinationURI);
          }
          importDatabase(sourceConnection, destinationConnection, tournament);
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Data successfully imported");
          }
        } else {
          LOGGER.error("Import aborted due to differences in databases");
        }

        try {
          try {
            stmt1 = sourceConnection.createStatement();
            stmt1.executeUpdate("SHUTDOWN COMPACT");
          } catch (final SQLException sqle) {
            LOGGER.info("Source either isn't HSQLDB or there is a problem", sqle);
          }
          try {
            stmt2 = destinationConnection.createStatement();
            stmt2.executeUpdate("SHUTDOWN COMPACT");
          } catch (final SQLException sqle) {
            LOGGER.info("Destination either isn't HSQLDB or there is a problem", sqle);
          }
        } finally {
          SQLFunctions.close(stmt1);
          SQLFunctions.close(stmt2);
        }

      }
    } catch (final Throwable e) {
      e.printStackTrace();
    }
  }

  private ImportDB() {
    // no instances
  }

  /**
   * Load data from a dump file into a new database specified by
   * <code>database</code>. Unlike
   * {@link #loadFromDumpIntoNewDB(ZipInputStream, Connection)}, this
   * will result in a database with all views and generated columns.
   * 
   * @param zipfile the dump file to read
   * @param destConnection where to load the data
   * @return the result of the import
   * @throws IOException if there is an error reading the dump file
   * @throws SQLException if there is an error importing the data
   * @see ImportDB#loadDatabaseDump(ZipInputStream, Connection)
   * @see ImportDB#importDatabase(Connection, Connection, String)
   */
  @Nonnull
  public static ImportDB.ImportResult loadFromDumpIntoNewDB(final ZipInputStream zipfile,
                                                            final Connection destConnection)
      throws IOException, SQLException {
    PreparedStatement destPrep = null;
    Connection memConnection = null;
    Statement memStmt = null;
    ResultSet memRS = null;
    try {
      final String databaseName = "dbimport"
          + String.valueOf(ImportDBDump.getNextDBCount());
      final DataSource memSource = Utilities.createMemoryDataSource(databaseName);
      memConnection = memSource.getConnection();

      final ImportDB.ImportResult importResult = loadDatabaseDump(zipfile, memConnection);
      final Document challengeDocument = importResult.getChallengeDocument();
      GenerateDB.generateDB(challengeDocument, destConnection);

      memStmt = memConnection.createStatement();

      // load the teams table into the destination database
      memRS = memStmt.executeQuery("SELECT TeamNumber, TeamName, Organization FROM Teams");
      destPrep = destConnection.prepareStatement("INSERT INTO Teams (TeamNumber, TeamName, Organization) VALUES (?, ?, ?)");
      while (memRS.next()) {
        final int num = memRS.getInt(1);
        final String name = memRS.getString(2);
        final String org = memRS.getString(3);
        if (!Team.isInternalTeamNumber(num)) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Inserting into teams: "
                + num
                + ", "
                + name
                + ", "
                + org);
          }
          destPrep.setInt(1, num);
          destPrep.setString(2, name == null
              || "".equals(name) ? GenerateDB.DEFAULT_TEAM_NAME : name);
          destPrep.setString(3, org);
          destPrep.executeUpdate();
        }
      }
      SQLFunctions.close(memRS);
      memRS = null;
      SQLFunctions.close(destPrep);
      destPrep = null;

      // load all of the tournaments
      // don't worry about bringing the times over, this way they will all be
      // null and this will force score summarization
      for (final Tournament sourceTournament : Tournament.getTournaments(memConnection)) {
        if (!GenerateDB.INTERNAL_TOURNAMENT_NAME.equals(sourceTournament.getName())
            && GenerateDB.INTERNAL_TOURNAMENT_ID != sourceTournament.getTournamentID()) {
          createTournament(sourceTournament, destConnection);
        }
      }

      // for each tournament listed in the dump file, import it
      for (final Tournament sourceTournament : Tournament.getTournaments(memConnection)) {
        final String tournament = sourceTournament.getName();
        // import the data from the tournament
        importDatabase(memConnection, destConnection, tournament);
      }

      // remove in-memory database
      memStmt.executeUpdate("SHUTDOWN");

      return importResult;
    } finally {
      SQLFunctions.close(memRS);
      SQLFunctions.close(memStmt);
      SQLFunctions.close(memConnection);

      SQLFunctions.close(destPrep);
    }
  }

  /**
   * Recursively create a tournament and it's next tournament.
   */
  private static void createTournament(final Tournament sourceTournament,
                                       final Connection destConnection)
      throws SQLException {
    // add the tournament to the tournaments table if it doesn't already
    // exist
    final Tournament destTournament = Tournament.findTournamentByName(destConnection, sourceTournament.getName());
    if (null == destTournament) {
      Tournament.createTournament(destConnection, sourceTournament.getName(), sourceTournament.getDescription());
    }
  }

  /**
   * Load type info from reader and return in a Map.
   * 
   * @param reader where to read the data from
   * @return key is column name, value is type
   */
  public static Map<String, String> loadTypeInfo(final Reader reader) throws IOException {
    final CSVReader csvreader = new CSVReader(reader);
    final Map<String, String> columnTypes = new HashMap<String, String>();

    String[] line;
    while (null != (line = csvreader.readNext())) {
      if (line.length != 2) {
        throw new RuntimeException("Typeinfo file has incorrect number of columns, should be 2");
      }
      if ("character".equalsIgnoreCase(line[1])) {
        // handle broken dumps from version 7.0
        columnTypes.put(line[0].toLowerCase(), "character(64)");
      } else {
        columnTypes.put(line[0].toLowerCase(), line[1]);
      }
    }
    return columnTypes;
  }

  /**
   * <p>
   * Load a database dumped as a zipfile into an existing empty database. No
   * checks are done, csv files are expected to be in the zipfile and they are
   * used as table names and table data in the database.
   * </p>
   * <p>
   * Once the database has been loaded it will be upgraded to the current
   * version using
   * {@link #upgradeDatabase(Connection, Document, ChallengeDescription)}.
   * </p>
   * <p>
   * The created database does not have constraints, nor does it have the
   * views. The intention is that this database will be migrated
   * into a newly created database.
   * </p>
   * 
   * @param zipfile the database dump
   * @param connection where to store the data
   * @return the challenge document
   * @throws IOException if there is an error reading the zipfile
   * @throws SQLException if there is an error loading the data into the
   *           database
   * @throws FLLRuntimeException if the database version in the dump is too new
   */
  public static ImportResult loadDatabaseDump(final ZipInputStream zipfile,
                                              final Connection connection)
      throws IOException, SQLException {
    Document challengeResult = null;
    final Path importDirectory = Paths.get("import_"
        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")));
    boolean hasBugs = false;

    final Map<String, Map<String, String>> typeInfo = new HashMap<String, Map<String, String>>();
    ZipEntry entry;
    final Map<String, String> tableData = new HashMap<String, String>();
    while (null != (entry = zipfile.getNextEntry())) {
      final String name = entry.getName();
      if ("challenge.xml".equals(name)) {
        final Reader reader = new InputStreamReader(zipfile, Utilities.DEFAULT_CHARSET);
        challengeResult = ChallengeParser.parse(reader);
      } else if (name.endsWith(".csv")) {
        final String tablename = name.substring(0, name.indexOf(".csv")).toLowerCase();
        final Reader reader = new InputStreamReader(zipfile, Utilities.DEFAULT_CHARSET);
        final String content = IOUtils.readIntoString(reader);
        tableData.put(tablename, content);
      } else if (name.endsWith(".types")) {
        final String tablename = name.substring(0, name.indexOf(".types")).toLowerCase();
        final Reader reader = new InputStreamReader(zipfile, Utilities.DEFAULT_CHARSET);
        final Map<String, String> columnTypes = loadTypeInfo(reader);
        typeInfo.put(tablename, columnTypes);
      } else if (name.startsWith(GatherBugReport.LOGS_DIRECTORY)) {
        if (!entry.isDirectory()) {
          LOGGER.trace("Found log file "
              + name);

          final Path outputFileName = importDirectory.resolve(name);
          final Path outputParent = outputFileName.getParent();
          if (null != outputParent) {
            Files.createDirectories(outputParent);
          }
          Files.copy(zipfile, outputFileName);
        }
      } else if (name.startsWith(DumpDB.BUGS_DIRECTORY)) {
        if (!entry.isDirectory()) {
          LOGGER.warn("Found bug report "
              + name);
          hasBugs = true;

          final Path outputFileName = importDirectory.resolve(name);
          final Path outputParent = outputFileName.getParent();
          if (null != outputParent) {
            Files.createDirectories(outputParent);
          }
          Files.copy(zipfile, outputFileName);
        }
      } else {
        LOGGER.warn("Unexpected file found in imported zip file, skipping: "
            + name);
      }
      zipfile.closeEntry();
    }

    if (null == challengeResult) {
      throw new RuntimeException("Cannot find challenge document in the zipfile");
    }

    final ChallengeDescription description = new ChallengeDescription(challengeResult.getDocumentElement());
    if (typeInfo.isEmpty()) {
      // before types were added, assume version 0 types
      createVersion0TypeInfo(typeInfo, description);
    }
    for (Map.Entry<String, String> tableEntry : tableData.entrySet()) {
      final String tablename = tableEntry.getKey();
      final String content = tableEntry.getValue();
      final Map<String, String> tableTypes = typeInfo.get(tablename);

      Utilities.loadCSVFile(connection, tablename, tableTypes, new StringReader(content));
    }

    int dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion > GenerateDB.DATABASE_VERSION) {
      throw new FLLRuntimeException("Database dump too new. Current known database version : "
          + GenerateDB.DATABASE_VERSION
          + " dump version: "
          + dbVersion);
    }

    upgradeDatabase(connection, challengeResult, description);

    return new ImportResult(challengeResult, importDirectory, hasBugs);
  }

  /**
   * Setup typeInfo for a version 0 database (before type information was
   * stored).
   */
  private static void createVersion0TypeInfo(final Map<String, Map<String, String>> typeInfo,
                                             final ChallengeDescription description) {
    final Map<String, String> tournaments = new HashMap<String, String>();
    tournaments.put("Name".toLowerCase(), "varchar(128)");
    tournaments.put("Location".toLowerCase(), "longvarchar");
    typeInfo.put("Tournaments".toLowerCase(), tournaments);

    final Map<String, String> teams = new HashMap<String, String>();
    teams.put("TeamNumber".toLowerCase(), "integer");
    teams.put("TeamName".toLowerCase(), "varchar(255)");
    teams.put("Organization".toLowerCase(), "varchar(255)");
    teams.put("Division".toLowerCase(), "varchar(32)");
    teams.put("Region".toLowerCase(), "varchar(255)");
    typeInfo.put("Teams".toLowerCase(), teams);

    final Map<String, String> tablenames = new HashMap<String, String>();
    tablenames.put("Tournament".toLowerCase(), "varchar(128)");
    tablenames.put("PairID".toLowerCase(), "integer");
    tablenames.put("SideA".toLowerCase(), "varchar(64)");
    tablenames.put("SideB".toLowerCase(), "varchar(64)");
    typeInfo.put("tablenames".toLowerCase(), tablenames);

    final Map<String, String> playoffData = new HashMap<String, String>();
    playoffData.put("event_division".toLowerCase(), "varchar(32)");
    playoffData.put("Tournament".toLowerCase(), "varchar(128)");
    playoffData.put("PlayoffRound".toLowerCase(), "integer");
    playoffData.put("LineNumber".toLowerCase(), "integer");
    playoffData.put("Team".toLowerCase(), "integer");
    playoffData.put("AssignedTable".toLowerCase(), "varchar(64)");
    playoffData.put("Printed".toLowerCase(), "boolean");
    typeInfo.put("PlayoffData".toLowerCase(), playoffData);

    final Map<String, String> tournamentTeams = new HashMap<String, String>();
    tournamentTeams.put("TeamNumber".toLowerCase(), "integer");
    tournamentTeams.put("Tournament".toLowerCase(), "varchar(128)");
    tournamentTeams.put("event_division".toLowerCase(), "varchar(32)");
    typeInfo.put("TournamentTeams".toLowerCase(), tournamentTeams);

    final Map<String, String> judges = new HashMap<String, String>();
    judges.put("id".toLowerCase(), "varchar(64)");
    judges.put("category".toLowerCase(), "varchar(64)");
    judges.put("Tournament".toLowerCase(), "varchar(128)");
    judges.put("event_division".toLowerCase(), "varchar(32)");
    typeInfo.put("Judges".toLowerCase(), judges);

    final Map<String, String> performance = new HashMap<String, String>();
    performance.put("TeamNumber".toLowerCase(), "integer");
    performance.put("Tournament".toLowerCase(), "varchar(128)");
    performance.put("RunNumber".toLowerCase(), "integer");
    performance.put("TimeStamp".toLowerCase(), "timestamp");
    performance.put("NoShow".toLowerCase(), "boolean");
    performance.put("Bye".toLowerCase(), "boolean");
    performance.put("Verified".toLowerCase(), "boolean");

    final PerformanceScoreCategory performanceElement = description.getPerformance();
    for (final AbstractGoal element : performanceElement.getGoals()) {
      if (!element.isComputed()) {
        final String goalName = element.getName();
        final String type = GenerateDB.getTypeForGoalColumn(element);
        performance.put(goalName.toLowerCase(), type);
      }
    }
    performance.put("ComputedTotal".toLowerCase(), "float");
    performance.put("StandardizedScore".toLowerCase(), "float");
    typeInfo.put("Performance".toLowerCase(), performance);

    final Map<String, String> finalScores = new HashMap<String, String>();
    finalScores.put("TeamNumber".toLowerCase(), "integer");
    finalScores.put("Tournament".toLowerCase(), "varchar(128)");
    for (final ScoreCategory categoryElement : description.getSubjectiveCategories()) {
      final String tableName = categoryElement.getName();

      final Map<String, String> subjective = new HashMap<String, String>();
      subjective.put("TeamNumber".toLowerCase(), "integer");
      subjective.put("Tournament".toLowerCase(), "varchar(128)");
      subjective.put("Judge".toLowerCase(), "varchar(64)");
      subjective.put("NoShow".toLowerCase(), "boolean");

      for (final AbstractGoal element : categoryElement.getGoals()) {
        if (!element.isComputed()) {
          final String goalName = element.getName();
          final String type = GenerateDB.getTypeForGoalColumn(element);
          subjective.put(goalName.toLowerCase(), type);
        }
      }
      subjective.put("ComputedTotal".toLowerCase(), "float");
      subjective.put("StandardizedScore".toLowerCase(), "float");
      typeInfo.put(tableName.toLowerCase(), subjective);

      finalScores.put(tableName.toLowerCase(), "float");
    }
    typeInfo.put("FinalScores".toLowerCase(), finalScores);

  }

  /**
   * Upgrade the specified database to the current version. This is working
   * under the assumption that the current database is loaded from a set of CSV
   * files and therefore don't have any referential integrity constraints, so
   * we're only fixing up column names and the data in the column.
   * 
   * @param connection the database to upgrade
   * @param challengeDocument the XML document specifying the challenge
   * @param description a developer friendly version of challengeDocument
   * @throws SQLException on an error
   * @throws IllegalArgumentException if the database cannot be upgraded for
   *           some reason
   */
  private static void upgradeDatabase(final Connection connection,
                                      final Document challengeDocument,
                                      final ChallengeDescription description)
      throws SQLException, IllegalArgumentException {
    int dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 1) {
      upgrade0To1(connection, challengeDocument);
    }

    // tournament parameters existed after version 1
    GenerateDB.setDefaultParameters(connection);

    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 2) {
      upgrade1To2(connection);
    }
    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 6) {
      upgrade2To6(connection);
    }
    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 7) {
      upgrade6To7(connection);
    }
    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 8) {
      upgrade7To8(connection);
    }
    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 9) {
      upgrade8To9(connection);
    }
    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 10) {
      upgrade9To10(connection);
    }
    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 11) {
      upgrade10To11(connection);
    }
    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 12) {
      upgrade11To12(connection, description);
    }
    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 13) {
      upgrade12To13(connection, description);
    }
    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 14) {
      upgrade13To14(connection);
    }
    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 15) {
      upgrade14To15(connection);
    }

    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < GenerateDB.DATABASE_VERSION) {
      throw new RuntimeException("Internal error, database version not updated to current instead was: "
          + dbVersion);
    }
  }

  private static void upgrade1To2(final Connection connection) throws SQLException {
    GenerateDB.createScheduleTables(connection, false);

    // set the version to 2 - this will have been set while creating
    // global_parameters, but we need to force it to 2 for later upgrade
    // functions to not be confused
    setDBVersion(connection, 2);
  }

  private static void upgrade8To9(final Connection connection) throws SQLException {
    GenerateDB.createFinalistScheduleTables(connection, false);

    setDBVersion(connection, 9);
  }

  private static void upgrade9To10(final Connection connection) throws SQLException {
    GenerateDB.createTableDivision(connection, false);

    setDBVersion(connection, 10);
  }

  @SuppressFBWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE" }, justification = "Table names come from category names")
  private static void upgrade10To11(final Connection connection) throws SQLException {
    Statement stmt = null;
    try {
      stmt = connection.createStatement();

      final Document document = GlobalParameters.getChallengeDocument(connection);
      final ChallengeDescription description = new ChallengeDescription(document.getDocumentElement());
      for (final ScoreCategory categoryElement : description.getSubjectiveCategories()) {
        final String tableName = categoryElement.getName();

        stmt.executeUpdate("ALTER TABLE "
            + tableName
            + " ADD COLUMN note longvarchar DEFAULT NULL");
      }

      setDBVersion(connection, 11);
    } finally {
      SQLFunctions.close(stmt);
      stmt = null;
    }
  }

  /**
   * @param category the category to match
   * @param scheduleColumns the schedule columns
   * @return the column name or null if no mapping can be found
   */
  private static String findScheduleColumnForCategory(final ScoreCategory category,
                                                      final Collection<String> scheduleColumns) {

    // first see if there is an exact match to the name or title
    for (final String scheduleColumn : scheduleColumns) {
      if (category.getName().equals(scheduleColumn)) {
        return scheduleColumn;
      } else if (category.getTitle().equals(scheduleColumn)) {
        return scheduleColumn;
      }
    }

    if (category.getName().contains("programming")
        || category.getName().contains("design")) {
      // look for Design
      for (final String scheduleColumn : scheduleColumns) {
        if ("Design".equals(scheduleColumn)) {
          return scheduleColumn;
        }
      }
    }

    // no mapping
    return null;
  }

  /**
   * Adds time columns to tournaments table.
   */
  private static void upgrade13To14(final Connection connection) throws SQLException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Upgrading database from 13 to 14");
    }

    Statement stmt = null;
    try {
      stmt = connection.createStatement();

      // need to check for columns as version 0 databases will automatically get
      // these columns created
      if (!checkForColumnInTable(connection, "Tournaments", "subjective_modified")) {
        stmt.executeUpdate("ALTER TABLE Tournaments ADD COLUMN subjective_modified TIMESTAMP DEFAULT NULL");
      }
      if (!checkForColumnInTable(connection, "Tournaments", "performance_seeding_modified")) {
        stmt.executeUpdate("ALTER TABLE Tournaments ADD COLUMN performance_seeding_modified TIMESTAMP DEFAULT NULL");
      }
      if (!checkForColumnInTable(connection, "Tournaments", "summary_computed")) {
        stmt.executeUpdate("ALTER TABLE Tournaments ADD COLUMN summary_computed TIMESTAMP DEFAULT NULL");
      }

      setDBVersion(connection, 14);
    } finally {
      SQLFunctions.close(stmt);
    }
  }

  /**
   * Adds playoff_bracket_teams table.
   */
  private static void upgrade14To15(final Connection connection) throws SQLException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Upgrading database from 14 to 15");
    }

    GenerateDB.createPlayoffBracketTeams(connection);

    PreparedStatement prep = null;
    Statement stmt = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("INSERT INTO playoff_bracket_teams (tournament_id, bracket_name, team_number) VALUES(?, ?, ?)");

      stmt = connection.createStatement();

      rs = stmt.executeQuery("SELECT DISTINCT tournament, event_division, team FROM PlayoffData");
      while (rs.next()) {
        final int tournament = rs.getInt(1);
        final String bracketName = rs.getString(2);
        final int team = rs.getInt(3);
        if (!Team.isInternalTeamNumber(team)) {

          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Adding to playoff_bracket_names tournament: "
                + tournament
                + " bracketName: "
                + bracketName
                + " team: "
                + team);
          }

          prep.setInt(1, tournament);
          prep.setString(2, bracketName);
          prep.setInt(3, team);
          prep.executeUpdate();
        }
      }

      setDBVersion(connection, 15);
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
      SQLFunctions.close(prep);
    }
  }

  /**
   * Check for a column in a table. This checks table names both upper and lower
   * case.
   * This also checks column names ignoring case.
   * 
   * @param connection database connection
   * @param table the table to find
   * @param column the column to find
   * @return true if the column was found
   */
  private static boolean checkForColumnInTable(final Connection connection,
                                               final String table,
                                               final String column)
      throws SQLException {
    ResultSet metaData = null;
    try {
      final DatabaseMetaData md = connection.getMetaData();

      metaData = md.getColumns(null, null, table.toUpperCase(), "%");
      while (metaData.next()) {
        final String needle = metaData.getString("COLUMN_NAME");
        if (column.equalsIgnoreCase(needle)) {
          return true;
        }
      }
      SQLFunctions.close(metaData);
      metaData = null;

      metaData = md.getColumns(null, null, table.toLowerCase(), "%");
      while (metaData.next()) {
        final String needle = metaData.getString("COLUMN_NAME");
        if (column.equalsIgnoreCase(needle)) {
          return true;
        }
      }
      return false;
    } finally {
      SQLFunctions.close(metaData);
    }
  }

  /**
   * Add non_numeric_nominees table and make sure that it's consistent with
   * finalist_categories.
   * Add room to finalist_categories table.
   * 
   * @param connection
   * @throws SQLException
   */
  private static void upgrade12To13(final Connection connection,
                                    final ChallengeDescription description)
      throws SQLException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Upgrading database from 12 to 13");
    }

    final Set<String> challengeSubjectiveCategories = new HashSet<>();
    for (final ScoreCategory cat : description.getSubjectiveCategories()) {
      challengeSubjectiveCategories.add(cat.getTitle());
    }

    PreparedStatement getTournaments = null;
    ResultSet tournaments = null;
    PreparedStatement insert = null;
    PreparedStatement getFinalistSchedule = null;
    ResultSet scheduleRows = null;
    Statement stmt = null;
    try {
      stmt = connection.createStatement();

      // need to check if the column exists as some version 12 databases got
      // created with the column
      if (!checkForColumnInTable(connection, "finalist_categories", "room")) {
        stmt.executeUpdate("ALTER TABLE finalist_categories ADD COLUMN room VARCHAR(32) DEFAULT NULL");
      }

      GenerateDB.createNonNumericNomineesTables(connection, false);

      insert = connection.prepareStatement("INSERT INTO non_numeric_nominees " //
          + " (tournament, category, team_number)" //
          + " VALUES (?, ?, ?)");

      getTournaments = connection.prepareStatement("SELECT tournament_id from Tournaments");

      getFinalistSchedule = connection.prepareStatement("SELECT DISTINCT category, team_number " //
          + " FROM finalist_schedule" //
          + " WHERE tournament = ?");

      tournaments = getTournaments.executeQuery();
      while (tournaments.next()) {
        final int tournament = tournaments.getInt(1);
        insert.setInt(1, tournament);
        getFinalistSchedule.setInt(1, tournament);

        scheduleRows = getFinalistSchedule.executeQuery();
        while (scheduleRows.next()) {
          final String categoryTitle = scheduleRows.getString(1);
          final int team = scheduleRows.getInt(2);

          if (!challengeSubjectiveCategories.contains(categoryTitle)) {
            insert.setString(2, categoryTitle);
            insert.setInt(3, team);
            insert.executeUpdate();
          }
        }

        SQLFunctions.close(scheduleRows);
        scheduleRows = null;
      }

      setDBVersion(connection, 13);
    } finally {
      SQLFunctions.close(stmt);
      SQLFunctions.close(tournaments);
      SQLFunctions.close(getTournaments);
      SQLFunctions.close(insert);
      SQLFunctions.close(scheduleRows);
      SQLFunctions.close(getFinalistSchedule);
    }

  }

  /**
   * Add mapping between schedule columns and subjective categories.
   * 
   * @param connection
   * @throws SQLException
   */
  private static void upgrade11To12(final Connection connection,
                                    final ChallengeDescription description)
      throws SQLException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Upgrading database from 11 to 12");
    }

    PreparedStatement getTournaments = null;
    ResultSet tournaments = null;
    PreparedStatement insert = null;
    PreparedStatement getSubjectiveStations = null;
    ResultSet stations = null;
    try {

      GenerateDB.createSubjectiveCategoryScheduleColumnMappingTables(connection);

      insert = connection.prepareStatement("INSERT INTO category_schedule_column " //
          + " (tournament, category, schedule_column)" //
          + " VALUES (?, ?, ?)");

      getTournaments = connection.prepareStatement("SELECT tournament_id from Tournaments");
      tournaments = getTournaments.executeQuery();
      while (tournaments.next()) {
        final int tournament = tournaments.getInt(1);

        insert.setInt(1, tournament);

        // get schedule columns
        getSubjectiveStations = connection.prepareStatement("SELECT DISTINCT name from sched_subjective WHERE tournament = ?");
        getSubjectiveStations.setInt(1, tournament);
        final Collection<String> scheduleColumns = new LinkedList<String>();
        stations = getSubjectiveStations.executeQuery();
        while (stations.next()) {
          final String name = stations.getString(1);
          scheduleColumns.add(name);
        }

        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace(String.format("Tournament %d has %s schedule columns", tournament, scheduleColumns.toString()));
        }

        for (final ScoreCategory category : description.getSubjectiveCategories()) {
          final String column = findScheduleColumnForCategory(category, scheduleColumns);

          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(String.format("Category %s maps to column %s", category.getName(), column));
          }

          if (null != column) {
            insert.setString(2, category.getName());
            insert.setString(3, column);
            insert.executeUpdate();
          }

        } // foreach category

      } // foreach tournament

      setDBVersion(connection, 12);
    } finally {
      SQLFunctions.close(stations);
      SQLFunctions.close(getSubjectiveStations);
      SQLFunctions.close(insert);
      SQLFunctions.close(tournaments);
      SQLFunctions.close(getTournaments);
    }
  }

  private static void upgrade2To6(final Connection connection) throws SQLException {
    Statement stmt = null;
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      stmt = connection.createStatement();
      stmt.executeUpdate("DROP TABLE IF EXISTS sched_subjective CASCADE");

      final StringBuilder sql = new StringBuilder();
      sql.append("CREATE TABLE sched_subjective (");
      sql.append("  tournament INTEGER NOT NULL");
      sql.append(" ,team_number INTEGER NOT NULL");
      sql.append(" ,name LONGVARCHAR NOT NULL");
      sql.append(" ,subj_time TIME NOT NULL");
      sql.append(" ,CONSTRAINT sched_subjective_pk PRIMARY KEY (tournament, team_number, name)");
      sql.append(")");
      stmt.executeUpdate(sql.toString());

      // migrate subjective times over if they are there
      boolean foundPresentationColumn = checkForColumnInTable(connection, "schedule", "presentation");

      if (foundPresentationColumn) {
        prep = connection.prepareStatement("INSERT INTO sched_subjective" //
            + " (tournament, team_number, name, subj_time)" //
            + " VALUES(?, ?, ?, ?)");
        rs = stmt.executeQuery("SELECT tournament, team_number, presentation, technical FROM schedule");
        while (rs.next()) {
          final int tournament = rs.getInt(1);
          final int team = rs.getInt(2);
          final Time presentation = rs.getTime(3);
          final Time technical = rs.getTime(4);
          prep.setInt(1, tournament);
          prep.setInt(2, team);
          prep.setString(3, "Technical");
          prep.setTime(4, technical);
          prep.executeUpdate();

          prep.setString(3, "Research");
          prep.setTime(4, presentation);
          prep.executeUpdate();
        }
      }

      setDBVersion(connection, 6);
    } finally {
      SQLFunctions.close(rs);
      rs = null;
      SQLFunctions.close(stmt);
      stmt = null;
      SQLFunctions.close(prep);
      prep = null;
    }
  }

  /**
   * Add run_number to the playoff table.
   */
  private static void upgrade7To8(final Connection connection) throws SQLException {
    Statement stmt = null;
    PreparedStatement prep = null;
    try {
      stmt = connection.createStatement();
      stmt.executeUpdate("ALTER TABLE PlayoffData ADD COLUMN run_number integer");

      final List<Tournament> tournaments = Tournament.getTournaments(connection);
      for (final Tournament tournament : tournaments) {
        final int seedingRounds = TournamentParameters.getNumSeedingRounds(connection, tournament.getTournamentID());

        prep = connection.prepareStatement("UPDATE PlayoffData SET run_number = ? + PlayoffRound");
        prep.setInt(1, seedingRounds);
        prep.executeUpdate();
      }

      setDBVersion(connection, 8);
    } finally {
      SQLFunctions.close(stmt);
      stmt = null;
      SQLFunctions.close(prep);
      prep = null;
    }
  }

  /**
   * Add judging_station to TournamentTeams. Rename event_division to station in
   * Judges
   * 
   * @param connection
   * @throws SQLException
   */
  private static void upgrade6To7(final Connection connection) throws SQLException {
    Statement stmt = null;
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      stmt = connection.createStatement();

      prep = connection.prepareStatement("UPDATE TournamentTeams SET event_division = ? WHERE TeamNumber = ? AND Tournament = ?");

      // set event_division to the default
      rs = stmt.executeQuery("SELECT TeamNumber, Tournament FROM TournamentTeams WHERE event_division is NULL");
      while (rs.next()) {
        final int teamNumber = rs.getInt(1);
        final int tournament = rs.getInt(2);
        final String division = GenerateDB.DEFAULT_TEAM_DIVISION;
        prep.setInt(2, teamNumber);
        prep.setInt(3, tournament);
        prep.setString(1, division);
      }

      // add score_group column
      stmt.executeUpdate("ALTER TABLE TournamentTeams ADD COLUMN judging_station varchar(64)");

      // set score_group equal to event division
      stmt.executeUpdate("UPDATE TournamentTeams SET judging_station = event_division");

      // rename event_division to station in Judges
      stmt.executeUpdate("ALTER TABLE Judges ALTER COLUMN event_division RENAME TO station");

      setDBVersion(connection, 7);
    } finally {
      SQLFunctions.close(rs);
      rs = null;
      SQLFunctions.close(stmt);
      stmt = null;
      SQLFunctions.close(prep);
      prep = null;
    }
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Dynamic based upon tables in the database")
  private static void upgrade0To1(final Connection connection,
                                  final Document challengeDocument)
      throws SQLException {
    Statement stmt = null;
    ResultSet rs = null;
    PreparedStatement stringsToInts = null;
    try {
      stmt = connection.createStatement();

      stmt.executeUpdate("DROP Table IF EXISTS TournamentParameters");

      // add the global_parameters table
      GenerateDB.createGlobalParameters(challengeDocument, connection);

      // ---- switch from string tournament names to integers ----

      // get all data from Tournaments table
      final Map<String, String> nameLocation = new HashMap<String, String>();
      rs = stmt.executeQuery("SELECT Name, Location FROM Tournaments");
      while (rs.next()) {
        final String name = rs.getString(1);
        final String location = rs.getString(2);
        nameLocation.put(name, location);
      }
      SQLFunctions.close(rs);

      // drop Tournaments table
      stmt.executeUpdate("DROP TABLE Tournaments");

      // create Tournaments table
      GenerateDB.tournaments(connection);

      // add all tournaments back
      for (final Map.Entry<String, String> entry : nameLocation.entrySet()) {
        if (!GenerateDB.INTERNAL_TOURNAMENT_NAME.equals(entry.getKey())) {
          final Tournament tournament = Tournament.findTournamentByName(connection, entry.getKey());
          if (null == tournament) {
            Tournament.createTournament(connection, entry.getKey(), entry.getValue());
          }
        }
      }
      // get map of names to ids
      final Map<String, Integer> nameID = new HashMap<String, Integer>();
      rs = stmt.executeQuery("SELECT Name, tournament_id FROM Tournaments");
      while (rs.next()) {
        final String name = rs.getString(1);
        final int id = rs.getInt(2);
        nameID.put(name, id);
      }
      SQLFunctions.close(rs);

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Map from names to tournament IDs: "
            + nameID);
      }

      // update all table columns
      final List<String> tablesToModify = new LinkedList<String>();
      tablesToModify.add("Judges");
      tablesToModify.add("tablenames");
      tablesToModify.add("TournamentTeams");
      tablesToModify.add("FinalScores");
      tablesToModify.add("Performance");
      tablesToModify.add("PlayoffData");
      tablesToModify.addAll(XMLUtils.getSubjectiveCategoryNames(challengeDocument));
      for (final String table : tablesToModify) {
        stringsToInts = connection.prepareStatement(String.format("UPDATE %s SET Tournament = ? WHERE Tournament = ?",
                                                                  table));
        for (final Map.Entry<String, Integer> entry : nameID.entrySet()) {
          stringsToInts.setInt(1, entry.getValue());
          stringsToInts.setString(2, entry.getKey());
          stringsToInts.executeUpdate();
        }
        SQLFunctions.close(stringsToInts);
      }

      // create new tournament parameters table
      GenerateDB.tournamentParameters(connection);

      // set the version to 1 - this will have been set while creating
      // global_parameters, but we need to force it to 1 for later upgrade
      // functions to not be confused
      setDBVersion(connection, 1);

    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
      SQLFunctions.close(stringsToInts);
    }
  }

  private static void setDBVersion(final Connection connection,
                                   final int version)
      throws SQLException {
    PreparedStatement setVersion = null;
    try {
      setVersion = connection.prepareStatement("UPDATE global_parameters SET param_value = ? WHERE param = ?");
      setVersion.setInt(1, version);
      setVersion.setString(2, GlobalParameters.DATABASE_VERSION);
      setVersion.executeUpdate();
    } finally {
      SQLFunctions.close(setVersion);
    }

  }

  /**
   * Import scores from database for tournament into the database for
   * connection. This method does no checking for differences, it is assumed you
   * have taken care of this already with
   * {@link #checkForDifferences(Connection, Connection, String)}. This method
   * will delete all information related to the specified tournament from the
   * destination database and then copy the information from the source
   * database.
   * 
   * @param sourceConnection a connection to the source database
   * @param destinationConnection a connection to the destination database
   * @param tournamentName the tournament that the scores are for
   */
  public static void importDatabase(final Connection sourceConnection,
                                    final Connection destinationConnection,
                                    final String tournamentName)
      throws SQLException {

    final Document document = GlobalParameters.getChallengeDocument(destinationConnection);
    final ChallengeDescription description = new ChallengeDescription(document.getDocumentElement());

    final Tournament sourceTournament = Tournament.findTournamentByName(sourceConnection, tournamentName);
    final int sourceTournamentID = sourceTournament.getTournamentID();
    final Tournament destTournament = Tournament.findTournamentByName(destinationConnection, tournamentName);
    final int destTournamentID = destTournament.getTournamentID();

    // Tournaments table isn't imported as it's expected to already be populated
    // with the tournament

    importTournamentParameters(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);

    importJudges(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);

    importTournamentTeams(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);

    importPerformance(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID, description);

    importSubjective(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID, description);

    importTableNames(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);

    importPlayoffData(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);
    importPlayoffTeams(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);

    importSubjectiveNominees(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);

    importSchedule(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);

    importFinalistSchedule(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);

    importCategoryScheduleMapping(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);

    // update score totals
    Queries.updateScoreTotals(description, destinationConnection, destTournamentID);
  }

  private static void importSchedule(final Connection sourceConnection,
                                     final Connection destinationConnection,
                                     final int sourceTournamentID,
                                     final int destTournamentID)
      throws SQLException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Importing Schedule");
    }

    PreparedStatement destPrep = null;
    PreparedStatement sourcePrep = null;
    ResultSet sourceRS = null;
    try {
      destPrep = destinationConnection.prepareStatement("DELETE FROM sched_perf_rounds WHERE Tournament = ?");
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
      SQLFunctions.close(destPrep);
      destPrep = null;
      destPrep = destinationConnection.prepareStatement("DELETE FROM sched_subjective WHERE Tournament = ?");
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
      SQLFunctions.close(destPrep);
      destPrep = destinationConnection.prepareStatement("DELETE FROM schedule WHERE Tournament = ?");
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
      SQLFunctions.close(destPrep);
      destPrep = null;

      sourcePrep = sourceConnection.prepareStatement("SELECT team_number, judging_station" //
          + " FROM schedule WHERE tournament=?");
      sourcePrep.setInt(1, sourceTournamentID);
      destPrep = destinationConnection.prepareStatement("INSERT INTO schedule" //
          + " (tournament, team_number, judging_station)" //
          + " VALUES (?, ?, ?)");
      destPrep.setInt(1, destTournamentID);
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        for (int i = 1; i <= 2; i++) {
          Object sourceObj = sourceRS.getObject(i);
          if ("".equals(sourceObj)) {
            sourceObj = null;
          }
          destPrep.setObject(i
              + 1, sourceObj);
        }
        destPrep.executeUpdate();
      }
      SQLFunctions.close(sourceRS);
      sourceRS = null;
      SQLFunctions.close(sourcePrep);
      sourcePrep = null;
      SQLFunctions.close(destPrep);
      destPrep = null;

      sourcePrep = sourceConnection.prepareStatement("SELECT team_number, round, perf_time, table_color, table_side" //
          + " FROM sched_perf_rounds WHERE tournament=?");
      sourcePrep.setInt(1, sourceTournamentID);
      destPrep = destinationConnection.prepareStatement("INSERT INTO sched_perf_rounds" //
          + " (tournament, team_number, round, perf_time, table_color, table_side)" //
          + " VALUES (?, ?, ?, ?, ?, ?)");
      destPrep.setInt(1, destTournamentID);
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        for (int i = 1; i <= 5; i++) {
          Object sourceObj = sourceRS.getObject(i);
          if ("".equals(sourceObj)) {
            sourceObj = null;
          }
          destPrep.setObject(i
              + 1, sourceObj);
        }
        destPrep.executeUpdate();
      }
      SQLFunctions.close(sourceRS);
      sourceRS = null;
      SQLFunctions.close(sourcePrep);
      sourcePrep = null;
      SQLFunctions.close(destPrep);
      destPrep = null;

      sourcePrep = sourceConnection.prepareStatement("SELECT team_number, name, subj_time" //
          + " FROM sched_subjective WHERE tournament=?");
      sourcePrep.setInt(1, sourceTournamentID);
      destPrep = destinationConnection.prepareStatement("INSERT INTO sched_subjective" //
          + " (tournament, team_number, name, subj_time)" //
          + " VALUES (?, ?, ?, ?)");
      destPrep.setInt(1, destTournamentID);
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        for (int i = 1; i <= 3; i++) {
          Object sourceObj = sourceRS.getObject(i);
          if ("".equals(sourceObj)) {
            sourceObj = null;
          }
          destPrep.setObject(i
              + 1, sourceObj);
        }
        destPrep.executeUpdate();
      }
      SQLFunctions.close(sourceRS);
      sourceRS = null;
      SQLFunctions.close(sourcePrep);
      sourcePrep = null;
      SQLFunctions.close(destPrep);
      destPrep = null;

    } finally {
      SQLFunctions.close(sourceRS);
      sourceRS = null;
      SQLFunctions.close(sourcePrep);
      sourcePrep = null;
      SQLFunctions.close(destPrep);
      destPrep = null;
    }
  }

  private static void importCategoryScheduleMapping(final Connection sourceConnection,
                                                    final Connection destinationConnection,
                                                    final int sourceTournamentID,
                                                    final int destTournamentID)
      throws SQLException {
    PreparedStatement destPrep = null;
    PreparedStatement sourcePrep = null;
    ResultSet sourceRS = null;
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Importing table_division");
      }
      destPrep = destinationConnection.prepareStatement("DELETE FROM category_schedule_column WHERE tournament = ?");
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
      SQLFunctions.close(destPrep);

      sourcePrep = sourceConnection.prepareStatement("SELECT category, schedule_column"
          + " FROM category_schedule_column WHERE tournament = ?");
      sourcePrep.setInt(1, sourceTournamentID);
      destPrep = destinationConnection.prepareStatement("INSERT INTO category_schedule_column (tournament, category, schedule_column) "
          + "VALUES (?, ?, ?)");
      destPrep.setInt(1, destTournamentID);
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        for (int i = 1; i <= 2; i++) {
          Object sourceObj = sourceRS.getObject(i);
          if ("".equals(sourceObj)) {
            sourceObj = null;
          }
          destPrep.setObject(i
              + 1, sourceObj);
        }
        destPrep.executeUpdate();
      }
    } finally {
      SQLFunctions.close(sourceRS);
      SQLFunctions.close(sourcePrep);
      SQLFunctions.close(destPrep);
    }
  }

  private static void importPlayoffData(final Connection sourceConnection,
                                        final Connection destinationConnection,
                                        final int sourceTournamentID,
                                        final int destTournamentID)
      throws SQLException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Importing PlayoffData");
    }

    PreparedStatement destPrep = null;
    PreparedStatement sourcePrep = null;
    ResultSet sourceRS = null;
    try {
      destPrep = destinationConnection.prepareStatement("DELETE FROM PlayoffData WHERE Tournament = ?");
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
      SQLFunctions.close(destPrep);

      sourcePrep = sourceConnection.prepareStatement("SELECT event_division, PlayoffRound, LineNumber, Team, AssignedTable, Printed, run_number "
          + "FROM PlayoffData WHERE Tournament=?");
      sourcePrep.setInt(1, sourceTournamentID);
      destPrep = destinationConnection.prepareStatement("INSERT INTO PlayoffData (Tournament, event_division, PlayoffRound,"
          + "LineNumber, Team, AssignedTable, Printed, run_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
      destPrep.setInt(1, destTournamentID);
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        for (int i = 1; i <= 7; i++) {
          Object sourceObj = sourceRS.getObject(i);
          if ("".equals(sourceObj)) {
            sourceObj = null;
          }
          destPrep.setObject(i
              + 1, sourceObj);
        }
        destPrep.executeUpdate();
      }
    } finally {
      SQLFunctions.close(sourceRS);
      SQLFunctions.close(sourcePrep);
      SQLFunctions.close(destPrep);
    }
  }

  private static void importPlayoffTeams(final Connection sourceConnection,
                                         final Connection destinationConnection,
                                         final int sourceTournamentID,
                                         final int destTournamentID)
      throws SQLException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Importing PlayoffTeams");
    }

    PreparedStatement destPrep = null;
    PreparedStatement sourcePrep = null;
    ResultSet sourceRS = null;
    try {
      destPrep = destinationConnection.prepareStatement("DELETE FROM playoff_bracket_teams WHERE tournament_id = ?");
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
      SQLFunctions.close(destPrep);

      sourcePrep = sourceConnection.prepareStatement("SELECT bracket_name, team_number "
          + "FROM playoff_bracket_teams WHERE tournament_id=?");
      sourcePrep.setInt(1, sourceTournamentID);
      destPrep = destinationConnection.prepareStatement("INSERT INTO playoff_bracket_teams (tournament_id, bracket_name, team_number)"
          + "VALUES (?, ?, ?)");
      destPrep.setInt(1, destTournamentID);
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        final String bracketName = sourceRS.getString(1);
        final int team = sourceRS.getInt(2);
        destPrep.setString(2, bracketName);
        destPrep.setInt(3, team);
        destPrep.executeUpdate();
      }
    } finally {
      SQLFunctions.close(sourceRS);
      SQLFunctions.close(sourcePrep);
      SQLFunctions.close(destPrep);
    }
  }

  private static void importTableNames(final Connection sourceConnection,
                                       final Connection destinationConnection,
                                       final int sourceTournamentID,
                                       final int destTournamentID)
      throws SQLException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Importing tablenames");
    }

    try (
        PreparedStatement destPrep = destinationConnection.prepareStatement("DELETE FROM table_division WHERE tournament = ?")) {
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
    }

    try (
        PreparedStatement destPrep = destinationConnection.prepareStatement("DELETE FROM tablenames WHERE Tournament = ?")) {
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
    }

    try (PreparedStatement sourcePrep = sourceConnection.prepareStatement("SELECT PairID, SideA, SideB "
        + "FROM tablenames WHERE Tournament=?");
        PreparedStatement destPrep = destinationConnection.prepareStatement("INSERT INTO tablenames (Tournament, PairID, SideA, SideB) "
            + "VALUES (?, ?, ?, ?)")) {

      sourcePrep.setInt(1, sourceTournamentID);
      destPrep.setInt(1, destTournamentID);
      try (ResultSet sourceRS = sourcePrep.executeQuery()) {
        while (sourceRS.next()) {
          for (int i = 1; i <= 3; i++) {
            Object sourceObj = sourceRS.getObject(i);
            if ("".equals(sourceObj)) {
              sourceObj = null;
            }
            destPrep.setObject(i
                + 1, sourceObj);
          }
          destPrep.executeUpdate();
        }
      } // result set
    } // prepared statements

    try (PreparedStatement sourcePrep = sourceConnection.prepareStatement("SELECT playoff_division, table_id"
        + " FROM table_division WHERE tournament=?");
        PreparedStatement destPrep = destinationConnection.prepareStatement("INSERT INTO table_division (tournament, playoff_division, table_id) "
            + "VALUES (?, ?, ?)")) {
      sourcePrep.setInt(1, sourceTournamentID);

      destPrep.setInt(1, destTournamentID);
      try (ResultSet sourceRS = sourcePrep.executeQuery()) {
        while (sourceRS.next()) {
          for (int i = 1; i <= 2; i++) {
            Object sourceObj = sourceRS.getObject(i);
            if ("".equals(sourceObj)) {
              sourceObj = null;
            }
            destPrep.setObject(i
                + 1, sourceObj);
          }
          destPrep.executeUpdate();
        }
      } // result set
    } // prepared statements

  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Dynamic table based upon categories")
  private static void importSubjective(final Connection sourceConnection,
                                       final Connection destinationConnection,
                                       final int sourceTournamentID,
                                       final int destTournamentID,
                                       final ChallengeDescription description)
      throws SQLException {
    PreparedStatement destPrep = null;
    try {
      // loop over each subjective category
      for (final ScoreCategory categoryElement : description.getSubjectiveCategories()) {
        final String tableName = categoryElement.getName();
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Importing "
              + tableName);
        }

        destPrep = destinationConnection.prepareStatement("DELETE FROM "
            + tableName
            + " WHERE Tournament = ?");
        destPrep.setInt(1, destTournamentID);
        destPrep.executeUpdate();
        SQLFunctions.close(destPrep);

        final StringBuilder columns = new StringBuilder();
        columns.append(" Tournament,");
        columns.append(" TeamNumber,");
        columns.append(" NoShow,");
        final List<AbstractGoal> goals = categoryElement.getGoals();
        int numColumns = 5;
        for (final AbstractGoal element : goals) {
          if (!element.isComputed()) {
            columns.append(" "
                + element.getName()
                + ",");
            ++numColumns;
          }
        }
        columns.append(" note,");
        columns.append(" Judge");

        importCommon(columns, tableName, numColumns, destinationConnection, destTournamentID, sourceConnection,
                     sourceTournamentID);
      }
    } finally {
      SQLFunctions.close(destPrep);
    }
  }

  /**
   * Common import code for importSubjective and importPerformance.
   * 
   * @param columns the columns in the table, this should include Tournament,
   *          TeamNumber, NoShow, Judge or Verified, then all elements for
   *          category
   * @param tableName the name of the table to update
   * @param numColumns the number of columns
   * @param destinationConnection
   * @param destTournamentID
   * @param sourceConnection
   * @param sourceTournamentID
   * @throws SQLException
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Dynamic based upon goals and category")
  private static void importCommon(final StringBuilder columns,
                                   final String tableName,
                                   final int numColumns,
                                   final Connection destinationConnection,
                                   final int destTournamentID,
                                   final Connection sourceConnection,
                                   final int sourceTournamentID)
      throws SQLException {
    PreparedStatement destPrep = null;
    PreparedStatement sourcePrep = null;
    ResultSet sourceRS = null;
    try {
      final StringBuffer sql = new StringBuffer();
      sql.append("INSERT INTO ");
      sql.append(tableName);
      sql.append(" (");
      sql.append(columns.toString());
      sql.append(") VALUES (");
      for (int i = 0; i < numColumns; i++) {
        if (i > 0) {
          sql.append(", ");
        }
        sql.append("?");
      }
      sql.append(")");
      destPrep = destinationConnection.prepareStatement(sql.toString());
      destPrep.setInt(1, destTournamentID);

      sourcePrep = sourceConnection.prepareStatement("SELECT "
          + columns.toString()
          + " FROM "
          + tableName
          + " WHERE Tournament = ?");
      sourcePrep.setInt(1, sourceTournamentID);
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        // skip tournament column
        for (int i = 1; i < numColumns; i++) {
          Object sourceObj = sourceRS.getObject(i
              + 1);
          if ("".equals(sourceObj)) {
            sourceObj = null;
          }
          destPrep.setObject(i
              + 1, sourceObj);
        }
        destPrep.executeUpdate();
      }
    } finally {
      SQLFunctions.close(sourceRS);
      SQLFunctions.close(sourcePrep);
      SQLFunctions.close(destPrep);
    }

  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Dynamic table based upon category")
  private static void importPerformance(final Connection sourceConnection,
                                        final Connection destinationConnection,
                                        final int sourceTournamentID,
                                        final int destTournamentID,
                                        final ChallengeDescription description)
      throws SQLException {
    PreparedStatement destPrep = null;
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Importing performance scores");
      }
      final PerformanceScoreCategory performanceElement = description.getPerformance();
      final String tableName = "Performance";
      destPrep = destinationConnection.prepareStatement("DELETE FROM "
          + tableName
          + " WHERE Tournament = ?");
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
      SQLFunctions.close(destPrep);
      destPrep = null;

      final StringBuilder columns = new StringBuilder();
      columns.append(" Tournament,");
      columns.append(" TeamNumber,");
      columns.append(" RunNumber,");
      // Note: If TimeStamp is no longer the 3rd element, then the hack below
      // needs to be modified
      columns.append(" TimeStamp,");
      final List<AbstractGoal> goals = performanceElement.getGoals();
      int numColumns = 7;
      for (final AbstractGoal element : goals) {
        if (!element.isComputed()) {
          columns.append(" "
              + element.getName()
              + ",");
          ++numColumns;
        }
      }
      columns.append(" NoShow,");
      columns.append(" Bye,");
      columns.append(" Verified");

      importCommon(columns, tableName, numColumns, destinationConnection, destTournamentID, sourceConnection,
                   sourceTournamentID);
    } finally {
      SQLFunctions.close(destPrep);
    }
  }

  /**
   * Clear out the tournament teams in destinationConnection for
   * destTournamentID and then insert all of the teams from sourceConnection for
   * sourceTournamentID.
   */
  private static void importTournamentTeams(final Connection sourceConnection,
                                            final Connection destinationConnection,
                                            final int sourceTournamentID,
                                            final int destTournamentID)
      throws SQLException {
    PreparedStatement destPrep = null;
    PreparedStatement sourcePrep = null;
    ResultSet sourceRS = null;
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Importing TournamentTeams");
      }
      destPrep = destinationConnection.prepareStatement("DELETE FROM TournamentTeams WHERE Tournament = ?");
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
      SQLFunctions.close(destPrep);
      sourcePrep = sourceConnection.prepareStatement("SELECT TeamNumber, event_division, judging_station FROM TournamentTeams WHERE Tournament = ?");
      sourcePrep.setInt(1, sourceTournamentID);
      destPrep = destinationConnection.prepareStatement("INSERT INTO TournamentTeams (Tournament, TeamNumber, event_division, judging_station) VALUES (?, ?, ?, ?)");
      destPrep.setInt(1, destTournamentID);
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        final int teamNumber = sourceRS.getInt(1);
        if (!Team.isInternalTeamNumber(teamNumber)) {
          final String eventDivision = sourceRS.getString(2);
          final String judgingStation = sourceRS.getString(3);
          final String actualEventDivision = eventDivision == null ? GenerateDB.DEFAULT_TEAM_DIVISION : eventDivision;
          final String actualJudgingStation = judgingStation == null ? actualEventDivision : judgingStation;
          destPrep.setInt(2, teamNumber);
          destPrep.setString(3, actualEventDivision);
          destPrep.setString(4, actualJudgingStation);
          destPrep.executeUpdate();
        }
      }
    } finally {
      SQLFunctions.close(sourceRS);
      SQLFunctions.close(sourcePrep);
      SQLFunctions.close(destPrep);
    }
  }

  private static void importTournamentParameters(final Connection sourceConnection,
                                                 final Connection destinationConnection,
                                                 final int sourceTournamentID,
                                                 final int destTournamentID)
      throws SQLException {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Importing tournament_parameters");
    }

    // use the "get" methods rather than generic SQL query to ensure that the
    // default value is picked up in case the default value has been changed
    final int seedingRounds = TournamentParameters.getNumSeedingRounds(sourceConnection, sourceTournamentID);
    final int maxPerScoreboardPerformanceRound = TournamentParameters.getMaxScoreboardPerformanceRound(sourceConnection,
                                                                                                       sourceTournamentID);
    TournamentParameters.setNumSeedingRounds(destinationConnection, destTournamentID, seedingRounds);
    TournamentParameters.setMaxScoreboardPerformanceRound(destinationConnection, destTournamentID,
                                                          maxPerScoreboardPerformanceRound);
  }

  private static void importJudges(final Connection sourceConnection,
                                   final Connection destinationConnection,
                                   final int sourceTournamentID,
                                   final int destTournamentID)
      throws SQLException {
    PreparedStatement destPrep = null;
    PreparedStatement sourcePrep = null;
    ResultSet sourceRS = null;
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Importing Judges");
      }

      destPrep = destinationConnection.prepareStatement("DELETE FROM Judges WHERE Tournament = ?");
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
      SQLFunctions.close(destPrep);

      destPrep = destinationConnection.prepareStatement("INSERT INTO Judges (id, category, station, Tournament) VALUES (?, ?, ?, ?)");
      destPrep.setInt(4, destTournamentID);

      sourcePrep = sourceConnection.prepareStatement("SELECT id, category, station FROM Judges WHERE Tournament = ?");
      sourcePrep.setInt(1, sourceTournamentID);
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        destPrep.setString(1, sourceRS.getString(1));
        destPrep.setString(2, sourceRS.getString(2));
        destPrep.setString(3, sourceRS.getString(3));
        destPrep.executeUpdate();
      }
    } finally {
      SQLFunctions.close(sourceRS);
      SQLFunctions.close(sourcePrep);
      SQLFunctions.close(destPrep);
    }
  }

  private static void importSubjectiveNominees(final Connection sourceConnection,
                                               final Connection destinationConnection,
                                               final int sourceTournamentID,
                                               final int destTournamentID)
      throws SQLException {
    PreparedStatement destPrep = null;
    PreparedStatement sourcePrep = null;
    ResultSet sourceRS = null;
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Importing subjective nominees");
      }

      // do drops first
      destPrep = destinationConnection.prepareStatement("DELETE FROM non_numeric_nominees WHERE tournament = ?");
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
      SQLFunctions.close(destPrep);

      // insert
      destPrep = destinationConnection.prepareStatement("INSERT INTO non_numeric_nominees (tournament, category, team_number) VALUES(?, ?, ?)");
      destPrep.setInt(1, destTournamentID);

      sourcePrep = sourceConnection.prepareStatement("SELECT category, team_number FROM non_numeric_nominees WHERE tournament = ?");
      sourcePrep.setInt(1, sourceTournamentID);
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        destPrep.setString(2, sourceRS.getString(1));
        destPrep.setInt(3, sourceRS.getInt(2));
        destPrep.executeUpdate();
      }

    } finally {
      SQLFunctions.close(sourceRS);
      SQLFunctions.close(sourcePrep);
      SQLFunctions.close(destPrep);
    }
  }

  private static void importFinalistSchedule(final Connection sourceConnection,
                                             final Connection destinationConnection,
                                             final int sourceTournamentID,
                                             final int destTournamentID)
      throws SQLException {
    PreparedStatement destPrep = null;
    PreparedStatement sourcePrep = null;
    ResultSet sourceRS = null;
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Importing finalist schedule");
      }

      // do drops first
      destPrep = destinationConnection.prepareStatement("DELETE FROM finalist_schedule WHERE tournament = ?");
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();

      SQLFunctions.close(destPrep);
      destPrep = destinationConnection.prepareStatement("DELETE FROM finalist_categories WHERE tournament = ?");
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();

      SQLFunctions.close(destPrep);
      // insert categories next
      destPrep = destinationConnection.prepareStatement("INSERT INTO finalist_categories (tournament, category, is_public, division, room) VALUES(?, ?, ?, ?, ?)");
      destPrep.setInt(1, destTournamentID);

      sourcePrep = sourceConnection.prepareStatement("SELECT category, is_public, division, room FROM finalist_categories WHERE tournament = ?");
      sourcePrep.setInt(1, sourceTournamentID);
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        destPrep.setString(2, sourceRS.getString(1));
        destPrep.setBoolean(3, sourceRS.getBoolean(2));
        destPrep.setString(4, sourceRS.getString(3));
        destPrep.setString(5, sourceRS.getString(4));
        destPrep.executeUpdate();
      }

      SQLFunctions.close(destPrep);
      // insert schedule values last
      destPrep = destinationConnection.prepareStatement("INSERT INTO finalist_schedule (tournament, category, judge_time, team_number, division) VALUES(?, ?, ?, ?, ?)");
      destPrep.setInt(1, destTournamentID);

      SQLFunctions.close(sourceRS);
      SQLFunctions.close(sourcePrep);
      sourcePrep = sourceConnection.prepareStatement("SELECT category, judge_time, team_number, division FROM finalist_schedule WHERE tournament = ?");
      sourcePrep.setInt(1, sourceTournamentID);
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        destPrep.setString(2, sourceRS.getString(1));
        destPrep.setTime(3, sourceRS.getTime(2));
        destPrep.setInt(4, sourceRS.getInt(3));
        destPrep.setString(5, sourceRS.getString(4));
        destPrep.executeUpdate();
      }
    } finally {
      SQLFunctions.close(sourceRS);
      SQLFunctions.close(sourcePrep);
      SQLFunctions.close(destPrep);
    }
  }

  /**
   * Check for differences between two tournaments in team information.
   * 
   * @return true if there are differences
   */
  public static boolean checkForDifferences(final Connection sourceConnection,
                                            final Connection destConnection,
                                            final String tournament)
      throws SQLException {

    // check that the tournament exists
    final Tournament destTournament = Tournament.findTournamentByName(destConnection, tournament);
    if (null == destTournament) {
      LOGGER.error("Tournament: "
          + tournament
          + " doesn't exist in the destination database!");
      return true;
    }

    final Tournament sourceTournament = Tournament.findTournamentByName(sourceConnection, tournament);
    if (null == sourceTournament) {
      LOGGER.error("Tournament: "
          + tournament
          + " doesn't exist in the source database!");
      return true;
    }

    boolean differencesFound = false;
    // check for missing teams
    final List<Team> missingTeams = findMissingTeams(sourceConnection, destConnection, tournament);
    if (!missingTeams.isEmpty()) {
      for (final Team team : missingTeams) {
        LOGGER.error(String.format("Team %d is in the source database, but not the dest database",
                                   team.getTeamNumber()));
      }
      differencesFound = true;
    }

    // check team info
    final List<TeamPropertyDifference> teamDifferences = checkTeamInfo(sourceConnection, destConnection, tournament);
    if (!teamDifferences.isEmpty()) {
      for (final TeamPropertyDifference diff : teamDifferences) {
        LOGGER.error(String.format("%s is different for team %d source value: %s dest value: %s", diff.getProperty(),
                                   diff.getTeamNumber(), diff.getSourceValue(), diff.getDestValue()));
      }
      differencesFound = true;
    }

    // TODO issue:116 check documents

    return differencesFound;
  }

  /**
   * @param sourceConnection source connection
   * @param destConnection destination connection
   * @param tournament the tournament to check
   * @return the differences, empty list if no differences
   * @throws SQLException
   */
  public static List<TeamPropertyDifference> checkTeamInfo(final Connection sourceConnection,
                                                           final Connection destConnection,
                                                           final String tournament)
      throws SQLException {
    final List<TeamPropertyDifference> differences = new LinkedList<TeamPropertyDifference>();

    PreparedStatement sourcePrep = null;
    PreparedStatement destPrep = null;
    ResultSet sourceRS = null;
    ResultSet destRS = null;
    try {
      destPrep = destConnection.prepareStatement("SELECT Teams.TeamName, Teams.Organization"
          + " FROM Teams"
          + " WHERE Teams.TeamNumber = ?");

      sourcePrep = sourceConnection.prepareStatement("SELECT Teams.TeamNumber, Teams.TeamName, Teams.Organization"
          + " FROM Teams, TournamentTeams, Tournaments" //
          + " WHERE Teams.TeamNumber = TournamentTeams.TeamNumber" //
          + " AND TournamentTeams.Tournament = Tournaments.tournament_id" //
          + " AND Tournaments.Name = ?");

      sourcePrep.setString(1, tournament);
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        final int teamNumber = sourceRS.getInt(1);
        final String sourceName = sourceRS.getString(2);
        final String sourceOrganization = sourceRS.getString(3);
        destPrep.setInt(1, teamNumber);
        destRS = destPrep.executeQuery();
        if (destRS.next()) {
          final String destName = destRS.getString(1);
          if (!ComparisonUtils.safeEquals(destName, sourceName)) {
            differences.add(new TeamPropertyDifference(teamNumber, TeamProperty.NAME, sourceName, destName));
          }
          final String destOrganization = destRS.getString(2);
          if (!ComparisonUtils.safeEquals(destOrganization, sourceOrganization)) {
            differences.add(new TeamPropertyDifference(teamNumber, TeamProperty.ORGANIZATION, sourceOrganization,
                                                       destOrganization));
          }
        }
        // else handled by findMissingTeams

        SQLFunctions.close(destRS);
      }
    } finally {
      SQLFunctions.close(destRS);
      SQLFunctions.close(sourceRS);
      SQLFunctions.close(destPrep);
      SQLFunctions.close(sourcePrep);
    }
    return differences;
  }

  /**
   * Find teams in the source database and not in the dest database. Only checks
   * for teams associated with the specified tournament.
   * 
   * @param sourceConnection source connection
   * @param destConnection destination connection
   * @param tournament the tournament to check
   * @return the teams in the source database and not in the destination
   *         database
   * @throws SQLException
   */
  public static List<Team> findMissingTeams(final Connection sourceConnection,
                                            final Connection destConnection,
                                            final String tournament)
      throws SQLException {
    final List<Team> missingTeams = new LinkedList<>();

    final Tournament sourceTournament = Tournament.findTournamentByName(sourceConnection, tournament);
    if (null == sourceTournament) {
      throw new FLLInternalException("Could not find tournament with name '"
          + tournament
          + "' in the source database. This should have been checked in a previous import step.");
    }

    final Map<Integer, TournamentTeam> sourceTeams = Queries.getTournamentTeams(sourceConnection,
                                                                                sourceTournament.getTournamentID());

    final Tournament destTournament = Tournament.findTournamentByName(destConnection, tournament);
    if (null == destTournament) {
      throw new FLLInternalException("Could not find tournament with name '"
          + tournament
          + "' in the destination database. This should have been checked in a previous import step.");
    }

    final Collection<Integer> destTeams = Queries.getAllTeamNumbers(destConnection);

    for (final Map.Entry<Integer, TournamentTeam> sourceEntry : sourceTeams.entrySet()) {
      if (!destTeams.contains(sourceEntry.getKey())) {
        missingTeams.add(sourceEntry.getValue());
      }
    }
    return missingTeams;
  }

  /**
   * Datetime format found in the CSV dump files.
   */
  public static final ThreadLocal<DateFormat> CSV_TIMESTAMP_FORMATTER = new ThreadLocal<DateFormat>() {
    protected DateFormat initialValue() {
      return new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
    }
  };

  /**
   * Time format found in the CSV dump files.
   */
  public static final ThreadLocal<DateFormat> CSV_TIME_FORMATTER = new ThreadLocal<DateFormat>() {
    protected DateFormat initialValue() {
      return new SimpleDateFormat("HH:mm:ss");
    }
  };

  /**
   * The result of
   * {@link ImportDB#importDatabase(Connection, Connection, String)}.
   */
  public static final class ImportResult {

    public ImportResult(@Nonnull final Document challengeDocument,
                        @Nonnull final Path importDirectory,
                        final boolean hasBugs) {
      this.challengeDocument = challengeDocument;
      this.importDirectory = importDirectory;
      this.hasBugs = hasBugs;

    }

    private final Document challengeDocument;

    public Document getChallengeDocument() {
      return challengeDocument;
    }

    private final Path importDirectory;

    /**
     * Any logs or bug reports will be in this directory. The directory may not
     * exist if there is no data for it.
     * 
     * @return the directory where extra data is stored
     */
    @Nonnull
    public Path getImportDirectory() {
      return importDirectory;
    }

    private final boolean hasBugs;

    /**
     * @return true if the import had bug reports
     */
    public boolean hasBugs() {
      return hasBugs;
    }

  }

}
