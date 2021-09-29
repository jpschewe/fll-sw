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
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.sql.DataSource;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Team;
import fll.Tournament;
import fll.TournamentLevel;
import fll.TournamentLevel.NoSuchTournamentLevelException;
import fll.TournamentTeam;
import fll.Utilities;
import fll.db.TeamPropertyDifference.TeamProperty;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.GatherBugReport;
import fll.web.UserRole;
import fll.web.developer.importdb.ImportDBDump;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.NonNumericCategory;
import fll.xml.PerformanceScoreCategory;
import fll.xml.SubjectiveScoreCategory;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Import scores from a tournament database
 * into a master score database.
 */
public final class ImportDB {

  private static final Logger LOGGER = LoggerFactory.getLogger(ImportDB.class);

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
   * @see ImportDB#importDatabase(Connection, Connection, String, boolean,
   *      boolean, boolean)
   */

  public static ImportDB.ImportResult loadFromDumpIntoNewDB(final ZipInputStream zipfile,
                                                            final Connection destConnection)
      throws IOException, SQLException {
    final String databaseName = "dbimport"
        + String.valueOf(ImportDBDump.getNextDBCount());
    final DataSource memSource = Utilities.createMemoryDataSource(databaseName);
    try (Connection sourceConnection = memSource.getConnection();
        Statement memStmt = sourceConnection.createStatement()) {

      final ImportDB.ImportResult importResult = loadDatabaseDump(zipfile, sourceConnection);
      final ChallengeDescription challengeDescription = GlobalParameters.getChallengeDescription(sourceConnection);
      GenerateDB.generateDB(challengeDescription, destConnection);

      // load the teams table into the destination database
      try (ResultSet memRS = memStmt.executeQuery("SELECT TeamNumber, TeamName, Organization FROM Teams");
          PreparedStatement destPrep = destConnection.prepareStatement("INSERT INTO Teams (TeamNumber, TeamName, Organization) VALUES (?, ?, ?)")) {
        while (memRS.next()) {
          final int num = memRS.getInt(1);
          final String name = memRS.getString(2);
          final String org = memRS.getString(3);
          if (!Team.isInternalTeamNumber(num)) {
            LOGGER.debug("Inserting into teams: {}, {}, {}", num, name, org);
            destPrep.setInt(1, num);
            destPrep.setString(2, name == null
                || "".equals(name) ? GenerateDB.DEFAULT_TEAM_NAME : name);
            destPrep.setString(3, org);
            destPrep.executeUpdate();
          }
        }
      }

      // create all of the tournament levels
      for (final TournamentLevel sourceLevel : TournamentLevel.getAllLevels(sourceConnection)) {
        if (!TournamentLevel.levelExists(destConnection, sourceLevel.getName())) {
          TournamentLevel.createTournamentLevel(destConnection, sourceLevel.getName());
        }
      }
      // set next level for destination levels
      for (final TournamentLevel sourceLevel : TournamentLevel.getAllLevels(sourceConnection)) {
        if (TournamentLevel.NO_NEXT_LEVEL_ID != sourceLevel.getNextLevelId()) {
          final TournamentLevel sourceNextLevel = TournamentLevel.getById(sourceConnection,
                                                                          sourceLevel.getNextLevelId());
          final TournamentLevel destLevel = TournamentLevel.getByName(destConnection, sourceLevel.getName());
          final TournamentLevel destNextLevel = TournamentLevel.getByName(destConnection, sourceNextLevel.getName());
          TournamentLevel.updateTournamentLevel(destConnection, destLevel.getId(), destLevel.getName(), destNextLevel);
        }
      }

      // load all of the tournaments
      // don't worry about bringing the times over, this way they will all be
      // null and this will force score summarization
      for (final Tournament sourceTournament : Tournament.getTournaments(sourceConnection)) {
        if (GenerateDB.INTERNAL_TOURNAMENT_ID != sourceTournament.getTournamentID()) {
          createTournament(sourceTournament, destConnection);
        }
      }

      importAuthentication(sourceConnection, destConnection);

      // for each tournament listed in the dump file, import it
      for (final Tournament sourceTournament : Tournament.getTournaments(sourceConnection)) {
        final String tournament = sourceTournament.getName();
        // import the data from the tournament
        importDatabase(sourceConnection, destConnection, tournament, true, true, true);
      }

      // for each tournament level import the data relating to the tournament level
      // NOTE: all levels were created above
      for (final TournamentLevel sourceLevel : TournamentLevel.getAllLevels(sourceConnection)) {
        final TournamentLevel destLevel = TournamentLevel.getByName(destConnection, sourceLevel.getName());
        importTournamentLevelData(sourceConnection, destConnection, sourceLevel.getId(), destLevel.getId());
      }

      final String sourceSelectedTournamentName = Tournament.getCurrentTournament(sourceConnection).getName();
      final Tournament destSelectedTournament = Tournament.findTournamentByName(destConnection,
                                                                                sourceSelectedTournamentName);
      Queries.setCurrentTournament(destConnection, destSelectedTournament.getTournamentID());

      // remove in-memory database
      memStmt.executeUpdate("SHUTDOWN");

      return importResult;
    }
  }

  private static void importAuthentication(final Connection sourceConnection,
                                           final Connection destConnection)
      throws SQLException {
    try (Statement source = sourceConnection.createStatement();
        ResultSet sourceData = source.executeQuery("SELECT fll_user, fll_pass FROM fll_authentication");
        PreparedStatement dest = destConnection.prepareStatement("INSERT INTO fll_authentication (fll_user, fll_pass) VALUES(?, ?)")) {
      while (sourceData.next()) {
        final String user = sourceData.getString("fll_user");
        final String pass = sourceData.getString("fll_pass");
        dest.setString(1, user);
        dest.setString(2, pass);
        dest.executeUpdate();
      }
    }

    try (Statement source = sourceConnection.createStatement();
        ResultSet sourceData = source.executeQuery("SELECT fll_user, fll_role FROM auth_roles");
        PreparedStatement dest = destConnection.prepareStatement("INSERT INTO auth_roles (fll_user, fll_role) VALUES(?, ?)")) {
      while (sourceData.next()) {
        final String user = sourceData.getString("fll_user");
        final String role = sourceData.getString("fll_role");
        dest.setString(1, user);
        dest.setString(2, role);
        dest.executeUpdate();
      }
    }
  }

  /**
   * Create a tournament in the destination matching the source tournament.
   */
  private static void createTournament(final Tournament sourceTournament,
                                       final Connection destConnection)
      throws SQLException {
    // add the tournament to the tournaments table if it doesn't already
    // exist
    if (!Tournament.doesTournamentExist(destConnection, sourceTournament.getName())) {
      Tournament.createTournament(destConnection, sourceTournament.getName(), sourceTournament.getDescription(),
                                  sourceTournament.getDate(), sourceTournament.getLevel());
    }
  }

  /**
   * Load type info from reader and return in a Map.
   *
   * @param reader where to read the data from
   * @return key is column name, value is type
   */
  private static Map<String, String> loadTypeInfo(final Reader reader) throws IOException {
    try {
      final CSVReader csvreader = new CSVReader(reader);
      final Map<String, String> columnTypes = new HashMap<>();

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
    } catch (final CsvValidationException e) {
      throw new IOException("Error parsing line", e);
    }
  }

  private static final String LOGS_DIRECTORY_WINDOWS = "logs\\";

  private static final String BUGS_DIRECTORY_WINDOWS = "bugs\\";

  /**
   * <p>
   * Load a database dumped as a zipfile into an existing empty database. No
   * checks are done, csv files are expected to be in the zipfile and they are
   * used as table names and table data in the database.
   * </p>
   * <p>
   * Once the database has been loaded it will be upgraded to the current
   * version using
   * {@link #upgradeDatabase(Connection, ChallengeDescription)}.
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
    ChallengeDescription description = null;
    final Path importDirectory = Paths.get("import_"
        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")));
    boolean hasBugs = false;

    final Map<String, Map<String, String>> typeInfo = new HashMap<>();
    ZipEntry entry;
    final Map<String, String> tableData = new HashMap<>();
    while (null != (entry = zipfile.getNextEntry())) {
      final String name = entry.getName();
      if ("challenge.xml".equals(name)) {
        final Reader reader = new InputStreamReader(zipfile, Utilities.DEFAULT_CHARSET);
        description = ChallengeParser.parse(reader);
      } else if (name.endsWith(".csv")) {
        final String tablename = name.substring(0, name.indexOf(".csv")).toLowerCase();
        final Reader reader = new InputStreamReader(zipfile, Utilities.DEFAULT_CHARSET);

        final StringWriter writer = new StringWriter();
        reader.transferTo(writer);
        final String content = writer.toString();
        tableData.put(tablename, content);
      } else if (name.endsWith(".types")) {
        final String tablename = name.substring(0, name.indexOf(".types")).toLowerCase();
        final Reader reader = new InputStreamReader(zipfile, Utilities.DEFAULT_CHARSET);
        final Map<String, String> columnTypes = loadTypeInfo(reader);
        typeInfo.put(tablename, columnTypes);
      } else if (name.startsWith(GatherBugReport.LOGS_DIRECTORY)
          || name.startsWith(LOGS_DIRECTORY_WINDOWS)) {
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
      } else if (name.startsWith(DumpDB.BUGS_DIRECTORY)
          || name.startsWith(BUGS_DIRECTORY_WINDOWS)) {
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

    if (null == description) {
      throw new RuntimeException("Cannot find challenge document in the zipfile");
    }

    if (typeInfo.isEmpty()) {
      // before types were added, assume version 0 types
      createVersion0TypeInfo(typeInfo, description);
    }
    for (final Map.Entry<String, String> tableEntry : tableData.entrySet()) {
      final String tablename = tableEntry.getKey();

      if (null != tablename) {
        final String content = tableEntry.getValue();
        final Map<String, String> tableTypes = typeInfo.getOrDefault(tablename, Collections.emptyMap());

        Utilities.loadCSVFile(connection, tablename, tableTypes, new StringReader(content));
      }
    }

    final int dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion > GenerateDB.DATABASE_VERSION) {
      throw new FLLRuntimeException("Database dump too new. Current known database version : "
          + GenerateDB.DATABASE_VERSION
          + " dump version: "
          + dbVersion);
    }

    upgradeDatabase(connection, description);

    return new ImportResult(importDirectory, hasBugs);
  }

  /**
   * Setup typeInfo for a version 0 database (before type information was
   * stored).
   */
  private static void createVersion0TypeInfo(final Map<String, Map<String, String>> typeInfo,
                                             final ChallengeDescription description) {
    final Map<String, String> tournaments = new HashMap<>();
    tournaments.put("Name".toLowerCase(), "varchar(128)");
    tournaments.put("Location".toLowerCase(), "longvarchar");
    typeInfo.put("Tournaments".toLowerCase(), tournaments);

    final Map<String, String> teams = new HashMap<>();
    teams.put("TeamNumber".toLowerCase(), "integer");
    teams.put("TeamName".toLowerCase(), "varchar(255)");
    teams.put("Organization".toLowerCase(), "varchar(255)");
    teams.put("Division".toLowerCase(), "varchar(32)");
    teams.put("Region".toLowerCase(), "varchar(255)");
    typeInfo.put("Teams".toLowerCase(), teams);

    final Map<String, String> tablenames = new HashMap<>();
    tablenames.put("Tournament".toLowerCase(), "varchar(128)");
    tablenames.put("PairID".toLowerCase(), "integer");
    tablenames.put("SideA".toLowerCase(), "varchar(64)");
    tablenames.put("SideB".toLowerCase(), "varchar(64)");
    typeInfo.put("tablenames".toLowerCase(), tablenames);

    final Map<String, String> playoffData = new HashMap<>();
    playoffData.put("event_division".toLowerCase(), "varchar(32)");
    playoffData.put("Tournament".toLowerCase(), "varchar(128)");
    playoffData.put("PlayoffRound".toLowerCase(), "integer");
    playoffData.put("LineNumber".toLowerCase(), "integer");
    playoffData.put("Team".toLowerCase(), "integer");
    playoffData.put("AssignedTable".toLowerCase(), "varchar(64)");
    playoffData.put("Printed".toLowerCase(), "boolean");
    typeInfo.put("PlayoffData".toLowerCase(), playoffData);

    final Map<String, String> tournamentTeams = new HashMap<>();
    tournamentTeams.put("TeamNumber".toLowerCase(), "integer");
    tournamentTeams.put("Tournament".toLowerCase(), "varchar(128)");
    tournamentTeams.put("event_division".toLowerCase(), "varchar(32)");
    typeInfo.put("TournamentTeams".toLowerCase(), tournamentTeams);

    final Map<String, String> judges = new HashMap<>();
    judges.put("id".toLowerCase(), "varchar(64)");
    judges.put("category".toLowerCase(), "varchar(64)");
    judges.put("Tournament".toLowerCase(), "varchar(128)");
    judges.put("event_division".toLowerCase(), "varchar(32)");
    typeInfo.put("Judges".toLowerCase(), judges);

    final Map<String, String> performance = new HashMap<>();
    performance.put("TeamNumber".toLowerCase(), "integer");
    performance.put("Tournament".toLowerCase(), "varchar(128)");
    performance.put("RunNumber".toLowerCase(), "integer");
    performance.put("TimeStamp".toLowerCase(), "timestamp");
    performance.put("NoShow".toLowerCase(), "boolean");
    performance.put("Bye".toLowerCase(), "boolean");
    performance.put("Verified".toLowerCase(), "boolean");

    final PerformanceScoreCategory performanceElement = description.getPerformance();
    for (final AbstractGoal element : performanceElement.getAllGoals()) {
      if (!element.isComputed()) {
        final String goalName = element.getName();
        final String type = GenerateDB.getTypeForGoalColumn(element);
        performance.put(goalName.toLowerCase(), type);
      }
    }
    performance.put("ComputedTotal".toLowerCase(), "float");
    performance.put("StandardizedScore".toLowerCase(), "float");
    typeInfo.put("Performance".toLowerCase(), performance);

    final Map<String, String> finalScores = new HashMap<>();
    finalScores.put("TeamNumber".toLowerCase(), "integer");
    finalScores.put("Tournament".toLowerCase(), "varchar(128)");
    for (final SubjectiveScoreCategory categoryElement : description.getSubjectiveCategories()) {
      final String tableName = categoryElement.getName();

      final Map<String, String> subjective = new HashMap<>();
      subjective.put("TeamNumber".toLowerCase(), "integer");
      subjective.put("Tournament".toLowerCase(), "varchar(128)");
      subjective.put("Judge".toLowerCase(), "varchar(64)");
      subjective.put("NoShow".toLowerCase(), "boolean");

      for (final AbstractGoal element : categoryElement.getAllGoals()) {
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
   * @param descriptionFor0to1Upgrade the challenge description read from the
   *          loaded file, only
   *          use this for adding to the database, any upgrades to the description
   *          should read from the database
   * @param descriptionFor0to1Upgrade a developer friendly version of
   *          challengeDocument
   * @throws SQLException on an error
   * @throws IllegalArgumentException if the database cannot be upgraded for
   *           some reason
   */
  private static void upgradeDatabase(final Connection connection,
                                      final ChallengeDescription descriptionFor0to1Upgrade)
      throws SQLException, IllegalArgumentException {
    int dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 1) {
      upgrade0To1(connection, descriptionFor0to1Upgrade);
    }

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
      upgrade11To12(connection);
    }
    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 13) {
      upgrade12To13(connection);
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
    if (dbVersion < 16) {
      upgrade15To16(connection);
    }

    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 17) {
      upgrade16To17(connection);
    }

    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 18) {
      upgrade17To18(connection);
    }

    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 19) {
      upgrade18To19(connection);
    }

    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 20) {
      upgrade19To20(connection);
    }

    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 21) {
      upgrade20To21(connection);
    }

    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 22) {
      upgrade21To22(connection);
    }

    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 23) {
      upgrade22To23(connection);
    }

    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 24) {
      upgrade23To24(connection);
    }

    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 25) {
      upgrade24To25(connection);
    }

    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 26) {
      upgrade25To26(connection);
    }

    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 27) {
      upgrade26To27(connection);
    }

    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 28) {
      upgrade27To28(connection);
    }

    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 29) {
      upgrade28To29(connection);
    }

    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 30) {
      upgrade29To30(connection);
    }

    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 31) {
      upgrade30To31(connection);
    }

    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 32) {
      upgrade31To32(connection);
    }

    GenerateDB.setDefaultParameters(connection, true);

    dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < GenerateDB.DATABASE_VERSION) {
      throw new RuntimeException("Internal error, database version not updated to current instead was: "
          + dbVersion);
    }
  }

  private static void upgrade1To2(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 1 to 2");

    GenerateDB.createScheduleTables(connection, false);

    // set the version to 2 - this will have been set while creating
    // global_parameters, but we need to force it to 2 for later upgrade
    // functions to not be confused
    setDBVersion(connection, 2);
  }

  private static void upgrade8To9(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 8 to 9");

    GenerateDB.createFinalistScheduleTables(connection, false);

    setDBVersion(connection, 9);
  }

  private static void upgrade9To10(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 9 to 10");

    GenerateDB.createTableDivision(connection, false);

    setDBVersion(connection, 10);
  }

  @SuppressFBWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE" }, justification = "Table names come from category names")
  private static void upgrade10To11(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 10 to 11");

    try (Statement stmt = connection.createStatement()) {

      final ChallengeDescription description = GlobalParameters.getChallengeDescription(connection);
      for (final SubjectiveScoreCategory categoryElement : description.getSubjectiveCategories()) {
        final String tableName = categoryElement.getName();

        stmt.executeUpdate("ALTER TABLE "
            + tableName
            + " ADD COLUMN note longvarchar DEFAULT NULL");
      }

      setDBVersion(connection, 11);
    }
  }

  /**
   * @param category the category to match
   * @param scheduleColumns the schedule columns
   * @return the column name or null if no mapping can be found
   */
  private static @Nullable String findScheduleColumnForCategory(final SubjectiveScoreCategory category,
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
    LOGGER.debug("Upgrading database from 13 to 14");

    try (Statement stmt = connection.createStatement()) {

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
    }
  }

  /**
   * Adds playoff_bracket_teams table.
   */
  private static void upgrade14To15(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 14 to 15");

    GenerateDB.createPlayoffBracketTeams(connection);

    try (
        PreparedStatement prep = connection.prepareStatement("INSERT INTO playoff_bracket_teams (tournament_id, bracket_name, team_number) VALUES(?, ?, ?)");
        Statement stmt = connection.createStatement()) {

      try (ResultSet rs = stmt.executeQuery("SELECT DISTINCT tournament, event_division, team FROM PlayoffData")) {
        while (rs.next()) {
          final int tournament = rs.getInt(1);
          final String bracketName = rs.getString(2);
          final int team = rs.getInt(3);
          if (!Team.isInternalTeamNumber(team)) {

            LOGGER.trace("Adding to playoff_bracket_names tournament: {} bracketName: {} team: {}", +tournament
                + bracketName
                + team);

            prep.setInt(1, tournament);
            prep.setString(2, bracketName);
            prep.setInt(3, team);
            prep.executeUpdate();
          }
        }
      } // result set

      setDBVersion(connection, 15);
    }
  }

  private static void upgrade15To16(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 15 to 16");

    try (Statement stmt = connection.createStatement()) {
      // if the database was upgraded from version 0, then the column already exists,
      // so check for it
      if (!checkForColumnInTable(connection, "tournaments", "tournament_date")) {
        stmt.executeUpdate("ALTER TABLE tournaments ADD COLUMN tournament_date DATE DEFAULT NULL");
      }
      setDBVersion(connection, 16);
    }
  }

  private static void upgrade16To17(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 16 to 17");

    // all old databases are running head to head
    TournamentParameters.setDefaultRunningHeadToHead(connection, true);
    setDBVersion(connection, 17);
  }

  private static void upgrade17To18(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 17 to 18");

    try (Statement stmt = connection.createStatement()) {
      // need to check if practice column exists as this can be added in the upgrade
      // from 1 to 2
      if (!checkForColumnInTable(connection, "sched_perf_rounds", "practice")) {
        stmt.executeUpdate("ALTER TABLE sched_perf_rounds ADD COLUMN practice BOOLEAN DEFAULT FALSE NOT NULL");
      }
    }

    setDBVersion(connection, 18);
  }

  private static void upgrade18To19(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 18 to 19");

    GenerateDB.createSubjectiveComputedScoresTable(connection, false);
    GenerateDB.createFinalScoresTable(connection, false);
    GenerateDB.createOverallScoresTable(connection, false);

    setDBVersion(connection, 19);
  }

  private static void upgrade19To20(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 19 to 20");

    GenerateDB.createSubjectiveAwardWinnerTables(connection, false);
    GenerateDB.createAdvancingTeamsTable(connection, false);

    // add level and next_level to Tournaments
    try (Statement stmt = connection.createStatement()) {
      // need to check if practice column exists as this can be added in the upgrade
      // from 0 to 1
      if (!checkForColumnInTable(connection, "tournaments", "level")) {
        stmt.executeUpdate("ALTER TABLE tournaments ADD COLUMN level VARCHAR(128) DEFAULT NULL");
      }
      // need to check if practice column exists as this can be added in the upgrade
      // from 0 to 1
      if (!checkForColumnInTable(connection, "tournaments", "next_level")) {
        stmt.executeUpdate("ALTER TABLE tournaments ADD COLUMN next_level VARCHAR(128) DEFAULT NULL");
      }
    }

    setDBVersion(connection, 20);
  }

  private static void upgrade20To21(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 20 to 21");

    GenerateDB.createAutomaticFinishedPlayoffTable(connection, false);

    setDBVersion(connection, 21);
  }

  private static void upgrade21To22(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 21 to 22");

    GenerateDB.createAwardGroupOrder(connection, false);

    setDBVersion(connection, 22);
  }

  @SuppressFBWarnings(value = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", justification = "Category name determines the table name")
  private static void upgrade22To23(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 22 to 23");

    final ChallengeDescription description = GlobalParameters.getChallengeDescription(connection);

    try (Statement stmt = connection.createStatement()) {
      for (final SubjectiveScoreCategory categoryElement : description.getSubjectiveCategories()) {

        String tableName = categoryElement.getName();

        // add _comment for each goal
        for (final AbstractGoal element : categoryElement.getAllGoals()) {
          final String goalName = element.getName();

          stmt.executeUpdate(String.format("ALTER TABLE %s ADD COLUMN %s_comment longvarchar DEFAULT NULL", tableName,
                                           goalName));
        }

        // add comment_great_job
        stmt.executeUpdate(String.format("ALTER TABLE %s ADD COLUMN comment_great_job longvarchar DEFAULT NULL",
                                         tableName));
        // add comment_think_about
        stmt.executeUpdate(String.format("ALTER TABLE %s ADD COLUMN comment_think_about longvarchar DEFAULT NULL",
                                         tableName));
      } // foreach category
    } // allocate statement

    setDBVersion(connection, 23);
  }

  private static void upgrade23To24(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 23 to 24");

    if (!checkForColumnInTable(connection, "non_numeric_nominees", "judge")) {
      try (Statement stmt = connection.createStatement()) {
        stmt.executeUpdate("ALTER TABLE non_numeric_nominees ADD COLUMN judge VARCHAR(64) DEFAULT NULL");
      }
    }

    setDBVersion(connection, 24);
  }

  private static void upgrade24To25(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 24 to 25");

    GenerateDB.createDelayedPerformanceTable(connection, false);

    setDBVersion(connection, 25);
  }

  private static void upgrade25To26(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 25 to 26");

    final Collection<String> tables = SQLFunctions.getTablesInDB(connection);
    if (!tables.contains("fll_authentication")) {
      GenerateDB.createAuthentication(connection);
    }

    GenerateDB.createAuthenticationRoles(connection, false);

    // all existing users have admin privileges
    for (final String user : Authentication.getUsers(connection)) {
      LOGGER.debug("Adding admin role to {}", user);
      Authentication.setRoles(connection, user, Collections.singleton(UserRole.ADMIN));
    }

    setDBVersion(connection, 26);
  }

  private static void upgrade26To27(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 26to 27");

    try (Statement stmt = connection.createStatement()) {
      // need to check for columns as the upgrade from 25 to 26 may do this
      if (!checkForColumnInTable(connection, "fll_authentication", "num_failures")) {
        stmt.executeUpdate("ALTER TABLE fll_authentication ADD COLUMN num_failures INTEGER DEFAULT 0 NOT NULL");
      }
      if (!checkForColumnInTable(connection, "fll_authentication", "last_failure")) {
        stmt.executeUpdate("ALTER TABLE fll_authentication ADD COLUMN last_failure TIMESTAMP DEFAULT NULL");
      }
    }

    setDBVersion(connection, 27);
  }

  private static void upgrade27To28(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 27 to 28");

    GenerateDB.createFinalistParameterTables(connection, false);

    if (!checkForColumnInTable(connection, "finalist_schedule", "judge_end_time")) {
      try (Statement stmt = connection.createStatement()) {
        stmt.executeUpdate("ALTER TABLE finalist_schedule ADD COLUMN judge_end_time TIME DEFAULT NULL");
        stmt.executeUpdate("UPDATE finalist_schedule SET judge_end_time = judge_time + 20 MINUTE WHERE judge_end_time IS NULL");
      }
    }

    final ChallengeDescription description = GlobalParameters.getChallengeDescription(connection);
    boolean modified = false;

    try (
        PreparedStatement prep = connection.prepareStatement("SELECT Name, tournament_id FROM tournaments WHERE tournament_id <> ?")) {
      prep.setInt(1, GenerateDB.INTERNAL_TOURNAMENT_ID);

      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String name = rs.getString("Name");
          final int tournamentId = rs.getInt("tournament_id");

          LOGGER.trace("Upgrading non-numeric categories in tournamnet {}", name);

          // make sure that all non-numeric categories are in the challenge description
          final Set<String> nonNumericCategoriesInDatabase = NonNumericNominees.getCategories(connection, tournamentId);
          for (final String categoryTitle : nonNumericCategoriesInDatabase) {
            LOGGER.trace("Looking for database category '{}'", categoryTitle);

            if (null == description.getNonNumericCategoryByTitle(categoryTitle)) {
              LOGGER.trace("Creating category '{}' and adding to challenge description", categoryTitle);

              final NonNumericCategory newCategory = new NonNumericCategory(categoryTitle, true);
              description.addNonNumericCategory(newCategory);
              modified = true;
            }
          } // foreach category in database
        }
      } // result set
    } // prepared statement1

    if (modified) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Inserting challenge into database. Non-numeric categories: {}",
                     description.getNonNumericCategories().stream().map(NonNumericCategory::getTitle)
                                .collect(Collectors.joining(", ")));
      }

      GenerateDB.insertOrUpdateChallengeDocument(description, connection);

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Non-numeric categories read back from database: {}",
                     GlobalParameters.getChallengeDescription(connection).getNonNumericCategories().stream()
                                     .map(NonNumericCategory::getTitle).collect(Collectors.joining(", ")));
      }
    }

    setDBVersion(connection, 28);
  }

  private static void upgrade28To29(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 28 to 29");

    if (!checkForColumnInTable(connection, "subjective_overall_award", "place")) {
      try (Statement stmt = connection.createStatement()) {
        stmt.executeUpdate("ALTER TABLE subjective_overall_award ADD COLUMN place INTEGER DEFAULT 1");
      }
    }

    if (!checkForColumnInTable(connection, "subjective_extra_award", "place")) {
      try (Statement stmt = connection.createStatement()) {
        stmt.executeUpdate("ALTER TABLE subjective_extra_award ADD COLUMN place INTEGER DEFAULT 1");
      }
    }

    if (!checkForColumnInTable(connection, "subjective_challenge_award", "place")) {
      try (Statement stmt = connection.createStatement()) {
        stmt.executeUpdate("ALTER TABLE subjective_challenge_award ADD COLUMN place INTEGER DEFAULT 1");
      }
    }

    setDBVersion(connection, 29);
  }

  private static void upgrade29To30(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 29 to 30");

    int mismatchCount = 0;

    // table can be created during upgrade from 0 to 1 to support creating the
    // default tournament
    final Collection<String> tables = SQLFunctions.getTablesInDB(connection);
    if (!tables.contains("tournament_level")) {
      // create the table and the default level
      GenerateDB.createTournamentLevelsTable(connection, false);
    }

    // the column can exist in a 0 to 1 upgrade
    if (!checkForColumnInTable(connection, "tournaments", "level_id")) {
      try (Statement stmt = connection.createStatement()) {
        // add level_id column to tournaments table
        stmt.executeUpdate("ALTER TABLE tournaments ADD COLUMN level_id INTEGER");

        final TournamentLevel defaultLevel = TournamentLevel.getByName(connection,
                                                                       TournamentLevel.DEFAULT_TOURNAMENT_LEVEL_NAME);

        // create all levels
        final Map<String, @Nullable String> nextLevels = new HashMap<>();
        final Map<String, TournamentLevel> createdLevels = new HashMap<>();

        final Map<ImmutablePair<@Nullable String, @Nullable String>, TournamentLevel> levelAssignments = new HashMap<>();
        try (ResultSet rs = stmt.executeQuery("SELECT DISTINCT level, next_level FROM Tournaments")) {
          while (rs.next()) {
            final @Nullable String levelName = rs.getString("level");
            final @Nullable String nextLevelName = rs.getString("next_level");

            final ImmutablePair<@Nullable String, @Nullable String> levelPair = ImmutablePair.of(levelName,
                                                                                                 nextLevelName);
            final TournamentLevel level;
            if (null == levelName) {
              level = defaultLevel;
            } else {
              if (!createdLevels.containsKey(levelName)) {
                final String newLevelName;
                if (!nextLevels.containsKey(levelName)) {
                  newLevelName = levelName;
                  nextLevels.put(levelName, nextLevelName);
                } else {
                  // check for mismatch
                  final @Nullable String nextLevelNameStored = nextLevels.get(levelName);
                  if (!Objects.equals(nextLevelName, nextLevelNameStored)) {
                    // mismatch
                    // need to specify a new name for level
                    newLevelName = String.format("%s_%d", levelName, mismatchCount);
                    ++mismatchCount;
                  } else {
                    newLevelName = levelName;
                    nextLevels.put(levelName, nextLevelName);
                  }
                }

                level = TournamentLevel.createTournamentLevel(connection, newLevelName);
                createdLevels.put(newLevelName, level);
              } else {
                level = createdLevels.get(levelName);
              }

              if (null != nextLevelName) {
                final TournamentLevel nextLevel;
                if (!createdLevels.containsKey(nextLevelName)) {
                  nextLevel = TournamentLevel.createTournamentLevel(connection, nextLevelName);
                  createdLevels.put(nextLevelName, nextLevel);
                } else {
                  nextLevel = TournamentLevel.getByName(connection, nextLevelName);
                }
                TournamentLevel.updateTournamentLevel(connection, level.getId(), levelName, nextLevel);
              }
            } // non-null level

            // track the level object for the pair
            levelAssignments.put(levelPair, level);
          } // foreach pair
        } // result set

        // assign levels to tournaments
        try (
            ResultSet rs = stmt.executeQuery("SELECT tournament_id, Name, Location, tournament_date, level, next_level FROM Tournaments")) {
          while (rs.next()) {
            final int tournamentId = rs.getInt("tournament_id");
            final @Nullable String levelName = rs.getString("level");
            final @Nullable String nextLevelName = rs.getString("next_level");
            final @Nullable String location = rs.getString("Location");
            final String name = castNonNull(rs.getString("Name"));
            final java.sql.@Nullable Date d = rs.getDate("tournament_date");
            final LocalDate date = null == d ? null : d.toLocalDate();

            final ImmutablePair<@Nullable String, @Nullable String> levelPair = ImmutablePair.of(levelName,
                                                                                                 nextLevelName);

            TournamentLevel level;
            if (GenerateDB.INTERNAL_TOURNAMENT_ID == tournamentId) {
              level = TournamentLevel.getById(connection, GenerateDB.INTERNAL_TOURNAMENT_LEVEL_ID);
            } else {
              level = levelAssignments.get(levelPair);
              if (null == level) {
                LOGGER.warn("Unable to find created level for tournament with id: {} levelName: {} nextLevelName: {}, using default level",
                            tournamentId, levelName, nextLevelName);
                level = defaultLevel;
              }
            }
            Tournament.updateTournament(connection, tournamentId, name, location, date, level);
          }
        } // result set

      } // statement

    } // if level_id column doesn't exist in tournaments table

    setDBVersion(connection, 30);
  }

  /**
   * Adds categories_ignored table that tracks categories to ignore per tournament
   * level.
   */
  private static void upgrade30To31(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 30 to 31");

    GenerateDB.createCategoriesIgnored(connection, false);

    setDBVersion(connection, 31);
  }

  /**
   * Adds awards script tables.
   */
  private static void upgrade31To32(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 31 to 32");

    GenerateDB.createAwardsScriptTables(connection, false);

    setDBVersion(connection, 32);
  }

  /**
   * Check for a column in a table. This checks table names both upper and lower
   * case.
   * This also checks column names ignoring case.
   *
   * @param connection database connection
   * @param table the table to find
   * @param needle the column to find
   * @return true if the column was found
   */
  private static boolean checkForColumnInTable(final Connection connection,
                                               final String table,
                                               final String needle)
      throws SQLException {
    LOGGER.trace("Looking for column {} in table {}", needle, table);

    final DatabaseMetaData md = connection.getMetaData();

    LOGGER.trace("Checking uppercase table name");
    try (ResultSet metaData = md.getColumns(null, null, table.toUpperCase(), "%")) {
      while (metaData.next()) {
        final String column = metaData.getString("COLUMN_NAME");
        LOGGER.trace("Saw column {} in table {}", column, table);

        if (needle.equalsIgnoreCase(column)) {
          return true;
        }
      }
    }

    LOGGER.trace("Checking lowercase table name");
    try (ResultSet metaData = md.getColumns(null, null, table.toLowerCase(), "%")) {
      while (metaData.next()) {
        final String column = metaData.getString("COLUMN_NAME");
        LOGGER.trace("Saw column {} in table {}", column, table);
        if (needle.equalsIgnoreCase(column)) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Add non_numeric_nominees table and make sure that it's consistent with
   * finalist_categories.
   * Add room to finalist_categories table.
   */
  private static void upgrade12To13(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 12 to 13");

    final ChallengeDescription description = GlobalParameters.getChallengeDescription(connection);

    final Set<String> challengeSubjectiveCategories = new HashSet<>();
    for (final SubjectiveScoreCategory cat : description.getSubjectiveCategories()) {
      challengeSubjectiveCategories.add(cat.getTitle());
    }

    try (Statement stmt = connection.createStatement()) {

      // need to check if the column exists as some version 12 databases got
      // created with the column
      if (!checkForColumnInTable(connection, "finalist_categories", "room")) {
        stmt.executeUpdate("ALTER TABLE finalist_categories ADD COLUMN room VARCHAR(32) DEFAULT NULL");
      }

      GenerateDB.createNonNumericNomineesTables(connection, false);

      try (PreparedStatement insert = connection.prepareStatement("INSERT INTO non_numeric_nominees " //
          + " (tournament, category, team_number)" //
          + " VALUES (?, ?, ?)");
          PreparedStatement getTournaments = connection.prepareStatement("SELECT tournament_id from Tournaments");
          PreparedStatement getFinalistSchedule = connection.prepareStatement("SELECT DISTINCT category, team_number " //
              + " FROM finalist_schedule" //
              + " WHERE tournament = ?")) {

        try (ResultSet tournaments = getTournaments.executeQuery()) {
          while (tournaments.next()) {
            final int tournament = tournaments.getInt(1);
            insert.setInt(1, tournament);
            getFinalistSchedule.setInt(1, tournament);

            try (ResultSet scheduleRows = getFinalistSchedule.executeQuery()) {
              while (scheduleRows.next()) {
                final String categoryTitle = castNonNull(scheduleRows.getString(1));
                final int team = scheduleRows.getInt(2);

                if (!challengeSubjectiveCategories.contains(categoryTitle)) {
                  insert.setString(2, categoryTitle);
                  insert.setInt(3, team);
                  insert.executeUpdate();
                }
              }
            } // schedule rows
          }
        } // tournament results
      } // prepared statements
      setDBVersion(connection, 13);
    }

  }

  /**
   * Add mapping between schedule columns and subjective categories.
   */
  private static void upgrade11To12(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 11 to 12");

    final ChallengeDescription description = GlobalParameters.getChallengeDescription(connection);

    GenerateDB.createSubjectiveCategoryScheduleColumnMappingTables(connection);
    try (PreparedStatement insert = connection.prepareStatement("INSERT INTO category_schedule_column " //
        + " (tournament, category, schedule_column)" //
        + " VALUES (?, ?, ?)");

        PreparedStatement getTournaments = connection.prepareStatement("SELECT tournament_id from Tournaments");
        ResultSet tournaments = getTournaments.executeQuery();
        PreparedStatement getSubjectiveStations = connection.prepareStatement("SELECT DISTINCT name from sched_subjective WHERE tournament = ?");) {
      while (tournaments.next()) {
        final int tournament = tournaments.getInt(1);

        insert.setInt(1, tournament);

        // get schedule columns
        getSubjectiveStations.setInt(1, tournament);
        final Collection<String> scheduleColumns = new LinkedList<>();
        try (ResultSet stations = getSubjectiveStations.executeQuery()) {
          while (stations.next()) {
            final String name = castNonNull(stations.getString(1));
            scheduleColumns.add(name);
          }
        } // stations

        LOGGER.trace("Tournament {} has {} schedule columns", tournament, scheduleColumns);

        for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
          final String column = findScheduleColumnForCategory(category, scheduleColumns);

          LOGGER.trace("Category {} maps to column {}", category.getName(), column);

          if (null != column) {
            insert.setString(2, category.getName());
            insert.setString(3, column);
            insert.executeUpdate();
          }

        } // foreach category

      } // foreach tournament

      setDBVersion(connection, 12);
    }
  }

  private static void upgrade2To6(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 2 to 6");

    try (Statement stmt = connection.createStatement()) {
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
      final boolean foundPresentationColumn = checkForColumnInTable(connection, "schedule", "presentation");

      if (foundPresentationColumn) {
        try (PreparedStatement prep = connection.prepareStatement("INSERT INTO sched_subjective" //
            + " (tournament, team_number, name, subj_time)" //
            + " VALUES(?, ?, ?, ?)");
            ResultSet rs = stmt.executeQuery("SELECT tournament, team_number, presentation, technical FROM schedule")) {
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
        } // prepared statement and result set
      }

      setDBVersion(connection, 6);
    }
  }

  /**
   * Add run_number to the playoff table.
   */
  private static void upgrade7To8(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 7 to 8");

    try (Statement stmt = connection.createStatement()) {
      stmt.executeUpdate("ALTER TABLE PlayoffData ADD COLUMN run_number integer");

      // need to create prepared statement after adding column
      try (
          PreparedStatement prep = connection.prepareStatement("UPDATE PlayoffData SET run_number = ? + PlayoffRound WHERE tournament = ?")) {

        try (ResultSet rs = stmt.executeQuery("SELECT tournament_id from tournaments")) {
          while (rs.next()) {
            final int tournamentId = rs.getInt(1);
            final int seedingRounds = TournamentParameters.getNumSeedingRounds(connection, tournamentId);

            prep.setInt(1, seedingRounds);
            prep.setInt(2, tournamentId);
            prep.executeUpdate();
          }
        } // result set
      } // prepared statement

      setDBVersion(connection, 8);
    } // statement
  }

  /**
   * Add judging_station to TournamentTeams. Rename event_division to station in
   * Judges
   */
  private static void upgrade6To7(final Connection connection) throws SQLException {
    LOGGER.debug("Upgrading database from 6 to 7");

    try (Statement stmt = connection.createStatement();

        PreparedStatement prep = connection.prepareStatement("UPDATE TournamentTeams SET event_division = ? WHERE TeamNumber = ? AND Tournament = ?");

        // set event_division to the default
        ResultSet rs = stmt.executeQuery("SELECT TeamNumber, Tournament FROM TournamentTeams WHERE event_division is NULL")) {
      while (rs.next()) {
        final int teamNumber = rs.getInt(1);
        final int tournament = rs.getInt(2);
        final String division = GenerateDB.DEFAULT_TEAM_DIVISION;
        prep.setInt(2, teamNumber);
        prep.setInt(3, tournament);
        prep.setString(1, division);
        prep.executeUpdate();
      }

      // add score_group column
      stmt.executeUpdate("ALTER TABLE TournamentTeams ADD COLUMN judging_station varchar(64)");

      // set score_group equal to event division
      stmt.executeUpdate("UPDATE TournamentTeams SET judging_station = event_division");

      // rename event_division to station in Judges
      stmt.executeUpdate("ALTER TABLE Judges ALTER COLUMN event_division RENAME TO station");

      setDBVersion(connection, 7);
    }
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Dynamic based upon tables in the database")
  private static void upgrade0To1(final Connection connection,
                                  ChallengeDescription description)
      throws SQLException {
    LOGGER.debug("Upgrading database from 0 to 1");

    try (Statement stmt = connection.createStatement()) {

      stmt.executeUpdate("DROP Table IF EXISTS TournamentParameters");

      // add the global_parameters table
      GenerateDB.createGlobalParameters(description, connection);

      // ---- switch from string tournament names to integers ----

      // get all data from Tournaments table
      final Map<String, @Nullable String> nameLocation = new HashMap<>();
      try (ResultSet rs = stmt.executeQuery("SELECT Name, Location FROM Tournaments")) {
        while (rs.next()) {
          final String name = castNonNull(rs.getString(1));
          final String location = rs.getString(2);
          nameLocation.put(name, location);
        }
      }

      // drop Tournaments table
      stmt.executeUpdate("DROP TABLE Tournaments");

      GenerateDB.createTournamentLevelsTable(connection, false);
      GenerateDB.tournaments(connection);

      try {
        final TournamentLevel defaultLevel = TournamentLevel.getByName(connection,
                                                                       TournamentLevel.DEFAULT_TOURNAMENT_LEVEL_NAME);

        // add all tournaments back
        for (final Map.Entry<String, @Nullable String> entry : nameLocation.entrySet()) {
          if (!GenerateDB.INTERNAL_TOURNAMENT_NAME.equals(entry.getKey())) {
            if (!Tournament.doesTournamentExist(connection, entry.getKey())) {
              Tournament.createTournament(connection, entry.getKey(), entry.getValue(), null, defaultLevel);
            }
          }
        }

      } catch (final NoSuchTournamentLevelException e) {
        throw new FLLInternalException("Cannot find the default tournament level named", e);
      }

      // get map of names to ids
      final Map<String, Integer> nameID = new HashMap<>();
      try (ResultSet rs = stmt.executeQuery("SELECT Name, tournament_id FROM Tournaments")) {
        while (rs.next()) {
          final String name = castNonNull(rs.getString(1));
          final int id = rs.getInt(2);
          nameID.put(name, id);
        }
      }

      LOGGER.trace("Map from names to tournament IDs: {}", nameID);

      // update all table columns
      final List<String> tablesToModify = new LinkedList<>();
      tablesToModify.add("Judges");
      tablesToModify.add("tablenames");
      tablesToModify.add("TournamentTeams");
      tablesToModify.add("FinalScores");
      tablesToModify.add("Performance");
      tablesToModify.add("PlayoffData");
      tablesToModify.addAll(description.getSubjectiveCategories().stream().map(SubjectiveScoreCategory::getName)
                                       .collect(Collectors.toList()));
      for (final String table : tablesToModify) {
        try (
            PreparedStatement stringsToInts = connection.prepareStatement(String.format("UPDATE %s SET Tournament = ? WHERE Tournament = ?",
                                                                                        table))) {
          for (final Map.Entry<String, Integer> entry : nameID.entrySet()) {
            stringsToInts.setInt(1, entry.getValue());
            stringsToInts.setString(2, entry.getKey());
            stringsToInts.executeUpdate();
          }
        }
      }

      // create new tournament parameters table
      GenerateDB.tournamentParameters(connection);

      // head to head was always run in the early tournaments
      // needs to execute after the tournaments and tournament_parameters tables are
      // created
      GenerateDB.setDefaultParameters(connection, true);

      // set the version to 1 - this will have been set while creating
      // global_parameters, but we need to force it to 1 for later upgrade
      // functions to not be confused
      setDBVersion(connection, 1);
    }
  }

  private static void setDBVersion(final Connection connection,
                                   final int version)
      throws SQLException {
    try (
        PreparedStatement setVersion = connection.prepareStatement("UPDATE global_parameters SET param_value = ? WHERE param = ?")) {
      setVersion.setInt(1, version);
      setVersion.setString(2, GlobalParameters.DATABASE_VERSION);
      setVersion.executeUpdate();
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
   * The destination database needs the tournament and it's levels already
   * created.
   *
   * @param sourceConnection a connection to the source database
   * @param destinationConnection a connection to the destination database
   * @param tournamentName the tournament that the scores are for
   * @param importPerformance if the performance data, including playoffs, should
   *          be imported
   * @param importSubjective if the subjective data should be imported
   * @param importFinalist if the finalist schedule should be imported
   * @throws SQLException on a database error
   */
  public static void importDatabase(final Connection sourceConnection,
                                    final Connection destinationConnection,
                                    final String tournamentName,
                                    final boolean importPerformance,
                                    final boolean importSubjective,
                                    final boolean importFinalist)
      throws SQLException {

    final ChallengeDescription description = GlobalParameters.getChallengeDescription(destinationConnection);

    final Tournament sourceTournament = Tournament.findTournamentByName(sourceConnection, tournamentName);
    final int sourceTournamentID = sourceTournament.getTournamentID();
    final Tournament destTournament = Tournament.findTournamentByName(destinationConnection, tournamentName);
    final int destTournamentID = destTournament.getTournamentID();

    LOGGER.debug("Importing tournament {} sourceId: {} destId: {}", tournamentName, sourceTournamentID,
                 destTournamentID);

    importTournamentData(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);

    if (importPerformance) {
      importPerformanceData(sourceConnection, destinationConnection, description, sourceTournamentID, destTournamentID);
    }

    if (importSubjective) {
      importSubjectiveData(sourceConnection, destinationConnection, description, sourceTournamentID, destTournamentID);
    }

    if (importFinalist) {
      importFinalistSchedule(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);
    }

    // update score totals
    Queries.updateScoreTotals(description, destinationConnection, destTournamentID);
  }

  private static void importSubjectiveData(final Connection sourceConnection,
                                           final Connection destinationConnection,
                                           final ChallengeDescription description,
                                           final int sourceTournamentID,
                                           final int destTournamentID)
      throws SQLException {
    importJudges(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);
    importSubjective(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID, description);
    importSubjectiveNominees(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);
    importAdvancingTeams(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);
    importAwardWinners(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);
    importAwardReportGroupSort(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);
  }

  private static void importPerformanceData(final Connection sourceConnection,
                                            final Connection destinationConnection,
                                            final ChallengeDescription description,
                                            final int sourceTournamentID,
                                            final int destTournamentID)
      throws SQLException {
    importPerformance(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID, description);

    importPlayoffData(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);
    importPlayoffTeams(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);

    importDelayedPerformance(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);
  }

  private static void importDelayedPerformance(final Connection sourceConnection,
                                               final Connection destinationConnection,
                                               final int sourceTournamentID,
                                               final int destTournamentID)
      throws SQLException {
    LOGGER.debug("Importing delayed_performance");

    try (
        PreparedStatement destPrep = destinationConnection.prepareStatement("DELETE FROM delayed_performance WHERE tournament_id = ?")) {
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
    }

    try (PreparedStatement sourcePrep = sourceConnection.prepareStatement("SELECT run_number, delayed_until "
        + "FROM delayed_performance WHERE tournament_id = ?");
        PreparedStatement destPrep = destinationConnection.prepareStatement("INSERT INTO delayed_performance (tournament_id, run_number, delayed_until)"
            + "VALUES (?, ?, ?)")) {
      sourcePrep.setInt(1, sourceTournamentID);
      destPrep.setInt(1, destTournamentID);
      try (ResultSet sourceRS = sourcePrep.executeQuery()) {
        while (sourceRS.next()) {
          final int runNumber = sourceRS.getInt(1);
          final Timestamp delayedUntil = sourceRS.getTimestamp(2);

          LOGGER.trace("run {} delayedUntil {}", runNumber, delayedUntil);

          destPrep.setInt(2, runNumber);
          destPrep.setTimestamp(3, delayedUntil);
          destPrep.executeUpdate();
        }
      } // result set
    } // prepared statements

  }

  private static void importTournamentLevelData(final Connection sourceConnection,
                                                final Connection destConnection,
                                                final int sourceLevelId,
                                                final int destLevelId)
      throws SQLException {
    importIgnoredCategories(sourceConnection, destConnection, sourceLevelId, destLevelId);

  }

  private static void importIgnoredCategories(final Connection sourceConnection,
                                              final Connection destConnection,
                                              final int sourceLevelId,
                                              final int destLevelId)
      throws SQLException {
    try (
        PreparedStatement delete = destConnection.prepareStatement("DELETE FROM categories_ignored WHERE level_id = ?")) {
      delete.setInt(1, destLevelId);
      delete.executeUpdate();
    }

    try (PreparedStatement sourcePrep = sourceConnection.prepareStatement("SELECT category_identifier, category_type" //
        + " FROM categories_ignored WHERE level_id = ?");
        PreparedStatement destPrep = destConnection.prepareStatement("INSERT INTO categories_ignored" //
            + " (level_id, category_identifier, category_type)" //
            + " VALUES (?, ?, ?)")) {

      sourcePrep.setInt(1, sourceLevelId);
      destPrep.setInt(1, destLevelId);

      destPrep.setInt(1, destLevelId);
      copyData(sourcePrep, destPrep);
    }
  }

  private static void copyData(final PreparedStatement sourcePrep,
                               final PreparedStatement destPrep)
      throws SQLException {
    copyData(sourcePrep, 0, destPrep, 1, -1);
  }

  private static void copyData(final PreparedStatement sourcePrep,
                               final int sourceOffset,
                               final PreparedStatement destPrep,
                               final int destOffset,
                               final int columnCountOverride)
      throws SQLException {
    try (ResultSet sourceRS = sourcePrep.executeQuery()) {
      final int columnCount = -1 == columnCountOverride ? sourceRS.getMetaData().getColumnCount() : columnCountOverride;

      boolean needsExecute = false;
      while (sourceRS.next()) {
        for (int i = 1; i <= columnCount; i++) {
          Object sourceObj = sourceRS.getObject(i
              + sourceOffset);
          if ("".equals(sourceObj)) {
            sourceObj = null;
          }
          destPrep.setObject(i
              + destOffset, sourceObj);
        }
        needsExecute = true;
        destPrep.addBatch();
      }

      if (needsExecute) {
        destPrep.executeBatch();
      }
    } // sourceRs
  }

  private static void importTournamentData(final Connection sourceConnection,
                                           final Connection destinationConnection,
                                           final int sourceTournamentID,
                                           final int destTournamentID)
      throws SQLException {
    // Tournaments table isn't imported as it's expected to already be populated
    // with the tournament
    importTournamentParameters(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);
    importTournamentTeams(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);
    importTableNames(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);
    importSchedule(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);
    importCategoryScheduleMapping(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);

    importAwardsScriptData(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);
  }

  private static void importSchedule(final Connection sourceConnection,
                                     final Connection destinationConnection,
                                     final int sourceTournamentID,
                                     final int destTournamentID)
      throws SQLException {
    LOGGER.debug("Importing Schedule");

    try (
        PreparedStatement destPrep = destinationConnection.prepareStatement("DELETE FROM sched_perf_rounds WHERE Tournament = ?")) {
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
    }

    try (
        PreparedStatement destPrep = destinationConnection.prepareStatement("DELETE FROM sched_subjective WHERE Tournament = ?")) {
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
    }

    try (
        PreparedStatement destPrep = destinationConnection.prepareStatement("DELETE FROM schedule WHERE Tournament = ?")) {
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
    }

    try (PreparedStatement sourcePrep = sourceConnection.prepareStatement("SELECT team_number, judging_station" //
        + " FROM schedule WHERE tournament=?");
        PreparedStatement destPrep = destinationConnection.prepareStatement("INSERT INTO schedule" //
            + " (tournament, team_number, judging_station)" //
            + " VALUES (?, ?, ?)")) {

      sourcePrep.setInt(1, sourceTournamentID);

      destPrep.setInt(1, destTournamentID);
      copyData(sourcePrep, destPrep);
    }

    try (
        PreparedStatement sourcePrep = sourceConnection.prepareStatement("SELECT team_number, practice, perf_time, table_color, table_side" //
            + " FROM sched_perf_rounds WHERE tournament=?");
        PreparedStatement destPrep = destinationConnection.prepareStatement("INSERT INTO sched_perf_rounds" //
            + " (tournament, team_number, practice, perf_time, table_color, table_side)" //
            + " VALUES (?, ?, ?, ?, ?, ?)")) {
      sourcePrep.setInt(1, sourceTournamentID);
      destPrep.setInt(1, destTournamentID);
      copyData(sourcePrep, destPrep);
    }

    try (PreparedStatement sourcePrep = sourceConnection.prepareStatement("SELECT team_number, name, subj_time" //
        + " FROM sched_subjective WHERE tournament=?");
        PreparedStatement destPrep = destinationConnection.prepareStatement("INSERT INTO sched_subjective" //
            + " (tournament, team_number, name, subj_time)" //
            + " VALUES (?, ?, ?, ?)")) {

      sourcePrep.setInt(1, sourceTournamentID);
      destPrep.setInt(1, destTournamentID);
      copyData(sourcePrep, destPrep);
    }
  }

  private static void importAwardsScriptData(final Connection sourceConnection,
                                             final Connection destinationConnection,
                                             final int sourceTournamentID,
                                             final int destTournamentID)
      throws SQLException {
    LOGGER.debug("Importing awards script data");
    importAwardsScriptTable(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID,
                            "awards_script_text", "section_name", "text");

    importAwardsScriptTable(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID,
                            "awards_script_parameters", "param_name", "param_value");
    importAwardsScriptTable(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID,
                            "awards_script_subjective_text", "category_name", "text");
    importAwardsScriptTable(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID,
                            "awards_script_subjective_presenter", "category_name", "presenter");
    importAwardsScriptTable(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID,
                            "awards_script_nonnumeric_text", "category_title", "text");
    importAwardsScriptTable(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID,
                            "awards_script_nonnumeric_presenter", "category_title", "presenter");
    importAwardsScriptTable(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID,
                            "awards_script_award_order", "award", "award_rank");
    importAwardsScriptTable(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID,
                            "awards_script_sponsor_order", "sponsor", "sponsor_rank");

  }

  @SuppressFBWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE" }, justification = "table and columns are passed in")
  private static void importAwardsScriptTable(final Connection sourceConnection,
                                              final Connection destinationConnection,
                                              final int sourceTournamentID,
                                              final int destTournamentID,
                                              final String tableName,
                                              final String keyColumn,
                                              final String rankColumn)
      throws SQLException {
    LOGGER.debug("Importing awards script data");
    try (
        PreparedStatement destPrep = destinationConnection.prepareStatement(String.format("DELETE FROM %s WHERE tournament_id = ?",
                                                                                          tableName))) {
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
    }

    try (
        PreparedStatement sourcePrep = sourceConnection.prepareStatement(String.format("SELECT tournament_level_id, layer_rank, %s, %s",
                                                                                       keyColumn, rankColumn)
            + String.format(" FROM %s WHERE tournament_id = ?", tableName));
        PreparedStatement destPrep = destinationConnection.prepareStatement(String.format("INSERT INTO %s (tournament_id, tournament_level_id, layer_rank, %s, %s) ",
                                                                                          tableName, keyColumn,
                                                                                          rankColumn)
            + "VALUES (?, ?, ?, ?, ?)")) {
      sourcePrep.setInt(1, sourceTournamentID);
      destPrep.setInt(1, destTournamentID);
      copyData(sourcePrep, destPrep);
    }
  }

  private static void importCategoryScheduleMapping(final Connection sourceConnection,
                                                    final Connection destinationConnection,
                                                    final int sourceTournamentID,
                                                    final int destTournamentID)
      throws SQLException {
    LOGGER.debug("Importing table_division");
    try (
        PreparedStatement destPrep = destinationConnection.prepareStatement("DELETE FROM category_schedule_column WHERE tournament = ?")) {
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
    }

    try (PreparedStatement sourcePrep = sourceConnection.prepareStatement("SELECT category, schedule_column"
        + " FROM category_schedule_column WHERE tournament = ?");
        PreparedStatement destPrep = destinationConnection.prepareStatement("INSERT INTO category_schedule_column (tournament, category, schedule_column) "
            + "VALUES (?, ?, ?)")) {
      sourcePrep.setInt(1, sourceTournamentID);
      destPrep.setInt(1, destTournamentID);
      copyData(sourcePrep, destPrep);
    }
  }

  private static void importPlayoffData(final Connection sourceConnection,
                                        final Connection destinationConnection,
                                        final int sourceTournamentID,
                                        final int destTournamentID)
      throws SQLException {
    LOGGER.debug("Importing PlayoffData");

    try (
        PreparedStatement destPrep = destinationConnection.prepareStatement("DELETE FROM PlayoffData WHERE Tournament = ?")) {
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
    }

    try (
        PreparedStatement sourcePrep = sourceConnection.prepareStatement("SELECT event_division, PlayoffRound, LineNumber, Team, AssignedTable, Printed, run_number "
            + "FROM PlayoffData WHERE Tournament=?");
        PreparedStatement destPrep = destinationConnection.prepareStatement("INSERT INTO PlayoffData (Tournament, event_division, PlayoffRound,"
            + "LineNumber, Team, AssignedTable, Printed, run_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
      sourcePrep.setInt(1, sourceTournamentID);
      destPrep.setInt(1, destTournamentID);
      copyData(sourcePrep, destPrep);
    }
  }

  private static void importPlayoffTeams(final Connection sourceConnection,
                                         final Connection destinationConnection,
                                         final int sourceTournamentID,
                                         final int destTournamentID)
      throws SQLException {
    LOGGER.debug("Importing PlayoffTeams");

    try (
        PreparedStatement destPrep = destinationConnection.prepareStatement("DELETE FROM playoff_bracket_teams WHERE tournament_id = ?")) {
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
    }

    try (PreparedStatement sourcePrep = sourceConnection.prepareStatement("SELECT bracket_name, team_number "
        + "FROM playoff_bracket_teams WHERE tournament_id=?");
        PreparedStatement destPrep = destinationConnection.prepareStatement("INSERT INTO playoff_bracket_teams (tournament_id, bracket_name, team_number)"
            + "VALUES (?, ?, ?)")) {
      sourcePrep.setInt(1, sourceTournamentID);
      destPrep.setInt(1, destTournamentID);
      try (ResultSet sourceRS = sourcePrep.executeQuery()) {
        while (sourceRS.next()) {
          final String bracketName = sourceRS.getString(1);
          final int team = sourceRS.getInt(2);
          destPrep.setString(2, bracketName);
          destPrep.setInt(3, team);
          destPrep.executeUpdate();
        }
      }
    }
  }

  private static void importTableNames(final Connection sourceConnection,
                                       final Connection destinationConnection,
                                       final int sourceTournamentID,
                                       final int destTournamentID)
      throws SQLException {
    LOGGER.debug("Importing tablenames");

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
      copyData(sourcePrep, destPrep);
    } // prepared statements

    try (PreparedStatement sourcePrep = sourceConnection.prepareStatement("SELECT playoff_division, table_id"
        + " FROM table_division WHERE tournament=?");
        PreparedStatement destPrep = destinationConnection.prepareStatement("INSERT INTO table_division (tournament, playoff_division, table_id) "
            + "VALUES (?, ?, ?)")) {
      sourcePrep.setInt(1, sourceTournamentID);

      destPrep.setInt(1, destTournamentID);
      copyData(sourcePrep, destPrep);
    } // prepared statements

  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Dynamic table based upon categories")
  private static void importSubjective(final Connection sourceConnection,
                                       final Connection destinationConnection,
                                       final int sourceTournamentID,
                                       final int destTournamentID,
                                       final ChallengeDescription description)
      throws SQLException {
    // loop over each subjective category
    for (final SubjectiveScoreCategory categoryElement : description.getSubjectiveCategories()) {
      final String tableName = categoryElement.getName();
      LOGGER.debug("Importing {}", tableName);

      try (PreparedStatement destPrep = destinationConnection.prepareStatement("DELETE FROM "
          + tableName
          + " WHERE Tournament = ?")) {
        destPrep.setInt(1, destTournamentID);
        destPrep.executeUpdate();
      }

      final StringBuilder columns = new StringBuilder();
      int numColumns = 0;
      columns.append(" Tournament,");
      ++numColumns;
      columns.append(" TeamNumber,");
      ++numColumns;
      columns.append(" NoShow,");
      ++numColumns;
      final List<AbstractGoal> goals = categoryElement.getAllGoals();
      for (final AbstractGoal element : goals) {
        if (!element.isComputed()) {
          columns.append(" "
              + element.getName()
              + ",");
          ++numColumns;

          columns.append(" "
              + GenerateDB.getGoalCommentColumnName(element)
              + ",");
          ++numColumns;
        }
      }
      columns.append(" note,");
      ++numColumns;
      columns.append(" Judge,");
      ++numColumns;
      columns.append(" comment_great_job,");
      ++numColumns;
      columns.append(" comment_think_about");
      ++numColumns;

      importCommon(columns, tableName, numColumns, destinationConnection, destTournamentID, sourceConnection,
                   sourceTournamentID);
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
    try (PreparedStatement destPrep = destinationConnection.prepareStatement(sql.toString());

        PreparedStatement sourcePrep = sourceConnection.prepareStatement("SELECT "
            + columns.toString()
            + " FROM "
            + tableName
            + " WHERE Tournament = ?")) {

      destPrep.setInt(1, destTournamentID);
      sourcePrep.setInt(1, sourceTournamentID);
      copyData(sourcePrep, 1, destPrep, 1, numColumns
          - 1);
    }

  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Dynamic table based upon category")
  private static void importPerformance(final Connection sourceConnection,
                                        final Connection destinationConnection,
                                        final int sourceTournamentID,
                                        final int destTournamentID,
                                        final ChallengeDescription description)
      throws SQLException {
    LOGGER.debug("Importing performance scores");
    final PerformanceScoreCategory performanceElement = description.getPerformance();
    final String tableName = "Performance";
    try (PreparedStatement destPrep = destinationConnection.prepareStatement("DELETE FROM "
        + tableName
        + " WHERE Tournament = ?")) {
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
    }

    final StringBuilder columns = new StringBuilder();
    columns.append(" Tournament,");
    columns.append(" TeamNumber,");
    columns.append(" RunNumber,");
    // Note: If TimeStamp is no longer the 3rd element, then the hack below
    // needs to be modified
    columns.append(" TimeStamp,");
    final List<AbstractGoal> goals = performanceElement.getAllGoals();
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
    LOGGER.debug("Importing TournamentTeams");
    try (
        PreparedStatement destPrep = destinationConnection.prepareStatement("DELETE FROM TournamentTeams WHERE Tournament = ?")) {
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
    }

    try (
        PreparedStatement sourcePrep = sourceConnection.prepareStatement("SELECT TeamNumber, event_division, judging_station FROM TournamentTeams WHERE Tournament = ?");
        PreparedStatement destPrep = destinationConnection.prepareStatement("INSERT INTO TournamentTeams (Tournament, TeamNumber, event_division, judging_station) VALUES (?, ?, ?, ?)")) {
      sourcePrep.setInt(1, sourceTournamentID);
      destPrep.setInt(1, destTournamentID);
      try (ResultSet sourceRS = sourcePrep.executeQuery()) {
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
      }
    }
  }

  private static void importTournamentParameters(final Connection sourceConnection,
                                                 final Connection destinationConnection,
                                                 final int sourceTournamentID,
                                                 final int destTournamentID)
      throws SQLException {
    LOGGER.debug("Importing tournament_parameters");

    // use the "get" methods rather than generic SQL query to ensure that the
    // default value is picked up in case the default value has been changed
    final int seedingRounds = TournamentParameters.getNumSeedingRounds(sourceConnection, sourceTournamentID);
    final boolean runningHeadToHead = TournamentParameters.getRunningHeadToHead(sourceConnection, sourceTournamentID);
    TournamentParameters.setNumSeedingRounds(destinationConnection, destTournamentID, seedingRounds);
    TournamentParameters.setRunningHeadToHead(destinationConnection, destTournamentID, runningHeadToHead);
  }

  private static void importJudges(final Connection sourceConnection,
                                   final Connection destinationConnection,
                                   final int sourceTournamentID,
                                   final int destTournamentID)
      throws SQLException {
    LOGGER.debug("Importing Judges");

    try (
        PreparedStatement destPrep = destinationConnection.prepareStatement("DELETE FROM Judges WHERE Tournament = ?")) {
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
    }

    try (
        PreparedStatement destPrep = destinationConnection.prepareStatement("INSERT INTO Judges (id, category, station, Tournament) VALUES (?, ?, ?, ?)");

        PreparedStatement sourcePrep = sourceConnection.prepareStatement("SELECT id, category, station FROM Judges WHERE Tournament = ?")) {

      destPrep.setInt(4, destTournamentID);
      sourcePrep.setInt(1, sourceTournamentID);
      try (ResultSet sourceRS = sourcePrep.executeQuery()) {
        while (sourceRS.next()) {
          destPrep.setString(1, sourceRS.getString(1));
          destPrep.setString(2, sourceRS.getString(2));
          destPrep.setString(3, sourceRS.getString(3));
          destPrep.executeUpdate();
        }
      }
    }
  }

  private static void importSubjectiveNominees(final Connection sourceConnection,
                                               final Connection destinationConnection,
                                               final int sourceTournamentID,
                                               final int destTournamentID)
      throws SQLException {
    LOGGER.debug("Importing subjective nominees");

    // do drops first
    try (
        PreparedStatement destPrep = destinationConnection.prepareStatement("DELETE FROM non_numeric_nominees WHERE tournament = ?")) {
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
    }

    // insert
    try (
        PreparedStatement destPrep = destinationConnection.prepareStatement("INSERT INTO non_numeric_nominees (tournament, category, team_number, judge) VALUES(?, ?, ?, ?)");

        PreparedStatement sourcePrep = sourceConnection.prepareStatement("SELECT category, team_number, judge FROM non_numeric_nominees WHERE tournament = ?")) {

      destPrep.setInt(1, destTournamentID);
      sourcePrep.setInt(1, sourceTournamentID);
      try (ResultSet sourceRS = sourcePrep.executeQuery()) {
        while (sourceRS.next()) {
          destPrep.setString(2, sourceRS.getString(1));
          destPrep.setInt(3, sourceRS.getInt(2));
          destPrep.setString(4, sourceRS.getString(3));
          destPrep.executeUpdate();
        }
      }
    }
  }

  private static void importFinalistSchedule(final Connection sourceConnection,
                                             final Connection destinationConnection,
                                             final int sourceTournamentID,
                                             final int destTournamentID)
      throws SQLException {
    LOGGER.debug("Importing finalist schedule");

    // do deletes first
    try (
        PreparedStatement destPrep = destinationConnection.prepareStatement("DELETE FROM finalist_schedule WHERE tournament = ?")) {
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
    }

    try (
        PreparedStatement destPrep = destinationConnection.prepareStatement("DELETE FROM finalist_categories WHERE tournament = ?")) {
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
    }

    try (
        PreparedStatement destPrep = destinationConnection.prepareStatement("DELETE FROM playoff_schedules WHERE tournament_id = ?")) {
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
    }

    // insert categories next
    try (
        PreparedStatement destPrep = destinationConnection.prepareStatement("INSERT INTO finalist_categories (tournament, category, division, room) VALUES(?, ?, ?, ?)");

        PreparedStatement sourcePrep = sourceConnection.prepareStatement("SELECT category, division, room FROM finalist_categories WHERE tournament = ?")) {

      destPrep.setInt(1, destTournamentID);
      sourcePrep.setInt(1, sourceTournamentID);
      try (ResultSet sourceRS = sourcePrep.executeQuery()) {
        while (sourceRS.next()) {
          destPrep.setString(2, sourceRS.getString(1));
          destPrep.setString(3, sourceRS.getString(2));
          destPrep.setString(4, sourceRS.getString(3));
          destPrep.executeUpdate();
        }
      }
    }

    // insert schedule values last
    try (
        PreparedStatement destPrep = destinationConnection.prepareStatement("INSERT INTO finalist_schedule (tournament, category, judge_time, judge_end_time, team_number, division) VALUES(?, ?, ?, ?, ?, ?)");

        PreparedStatement sourcePrep = sourceConnection.prepareStatement("SELECT category, judge_time, judge_end_time, team_number, division FROM finalist_schedule WHERE tournament = ?")) {

      destPrep.setInt(1, destTournamentID);
      sourcePrep.setInt(1, sourceTournamentID);
      try (ResultSet sourceRS = sourcePrep.executeQuery()) {
        while (sourceRS.next()) {
          destPrep.setString(2, sourceRS.getString(1));
          destPrep.setTime(3, sourceRS.getTime(2));
          destPrep.setTime(4, sourceRS.getTime(3));
          destPrep.setInt(5, sourceRS.getInt(4));
          destPrep.setString(6, sourceRS.getString(5));
          destPrep.executeUpdate();
        }
      }
    }

    // insert playoff schedules
    try (
        PreparedStatement destPrep = destinationConnection.prepareStatement("INSERT INTO playoff_schedules (tournament_id, bracket_name, start_time, end_time) VALUES(?, ?, ?, ?)");

        PreparedStatement sourcePrep = sourceConnection.prepareStatement("SELECT bracket_name, start_time, end_time FROM playoff_schedules WHERE tournament_id = ?")) {

      destPrep.setInt(1, destTournamentID);
      sourcePrep.setInt(1, sourceTournamentID);
      try (ResultSet sourceRS = sourcePrep.executeQuery()) {
        while (sourceRS.next()) {
          destPrep.setString(2, sourceRS.getString(1));
          destPrep.setTime(3, sourceRS.getTime(2));
          destPrep.setTime(4, sourceRS.getTime(3));
          destPrep.executeUpdate();
        }
      }

    }
  }

  /**
   * Check for differences between two tournaments in team information.
   *
   * @param sourceConnection the incoming database
   * @param destConnection the database to insert the data into
   * @param tournament the name of the tournament to import
   * @return true if there are differences
   * @throws SQLException on a database error
   */
  public static boolean checkForDifferences(final Connection sourceConnection,
                                            final Connection destConnection,
                                            final String tournament)
      throws SQLException {

    // check that the tournament exists
    if (!Tournament.doesTournamentExist(destConnection, tournament)) {
      LOGGER.error("Tournament: "
          + tournament
          + " doesn't exist in the destination database!");
      return true;
    }

    if (!Tournament.doesTournamentExist(sourceConnection, tournament)) {
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

    return differencesFound;
  }

  /**
   * @param sourceConnection source connection
   * @param destConnection destination connection
   * @param tournament the tournament to check
   * @return the differences, empty list if no differences
   * @throws SQLException on a database error
   */
  public static List<TeamPropertyDifference> checkTeamInfo(final Connection sourceConnection,
                                                           final Connection destConnection,
                                                           final String tournament)
      throws SQLException {
    final List<TeamPropertyDifference> differences = new LinkedList<>();

    try (PreparedStatement destPrep = destConnection.prepareStatement("SELECT Teams.TeamName, Teams.Organization"
        + " FROM Teams"
        + " WHERE Teams.TeamNumber = ?");

        PreparedStatement sourcePrep = sourceConnection.prepareStatement("SELECT Teams.TeamNumber, Teams.TeamName, Teams.Organization"
            + " FROM Teams, TournamentTeams, Tournaments" //
            + " WHERE Teams.TeamNumber = TournamentTeams.TeamNumber" //
            + " AND TournamentTeams.Tournament = Tournaments.tournament_id" //
            + " AND Tournaments.Name = ?")) {

      sourcePrep.setString(1, tournament);
      try (ResultSet sourceRS = sourcePrep.executeQuery()) {
        while (sourceRS.next()) {
          final int teamNumber = sourceRS.getInt(1);
          final String sourceName = castNonNull(sourceRS.getString(2));
          final String sourceOrganization = sourceRS.getString(3);
          destPrep.setInt(1, teamNumber);

          try (ResultSet destRS = destPrep.executeQuery()) {
            if (destRS.next()) {
              final String destName = castNonNull(destRS.getString(1));
              if (!Objects.equals(destName, sourceName)) {
                differences.add(new TeamPropertyDifference(teamNumber, TeamProperty.NAME, sourceName, destName));
              }
              final String destOrganization = destRS.getString(2);
              if (!Objects.equals(destOrganization, sourceOrganization)) {
                differences.add(new TeamPropertyDifference(teamNumber, TeamProperty.ORGANIZATION, sourceOrganization,
                                                           destOrganization));
              }
            }
            // else handled by findMissingTeams
          } // dest result set
        }
      } // source result set
    }
    return differences;
  }

  private static void importAdvancingTeams(final Connection sourceConnection,
                                           final Connection destinationConnection,
                                           final int sourceTournamentID,
                                           final int destTournamentID)
      throws SQLException {
    try (
        PreparedStatement destDelete = destinationConnection.prepareStatement("DELETE FROM advancing_teams WHERE tournament_id = ?");
        PreparedStatement destPrep = destinationConnection.prepareStatement("INSERT INTO advancing_teams (tournament_id, team_number, award_group) VALUES(?, ?, ?)");
        PreparedStatement sourcePrep = sourceConnection.prepareStatement("SELECT team_number, award_group FROM advancing_teams WHERE tournament_id = ?")) {
      LOGGER.debug("Importing advancing teams");

      // do drops first
      destDelete.setInt(1, destTournamentID);
      destDelete.executeUpdate();

      // insert
      destPrep.setInt(1, destTournamentID);

      sourcePrep.setInt(1, sourceTournamentID);
      try (ResultSet sourceRS = sourcePrep.executeQuery()) {
        while (sourceRS.next()) {
          destPrep.setInt(2, sourceRS.getInt(1));
          destPrep.setString(3, sourceRS.getString(2));
          destPrep.executeUpdate();
        }
      }
    }
  }

  private static void importAwardWinners(final Connection sourceConnection,
                                         final Connection destinationConnection,
                                         final int sourceTournamentID,
                                         final int destTournamentID)
      throws SQLException {
    importSubjectiveOverallWinners(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);
    importSubjectiveExtraWinners(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);
    importSubjectiveChallengeWinners(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);
  }

  private static void importSubjectiveOverallWinners(final Connection sourceConnection,
                                                     final Connection destinationConnection,
                                                     final int sourceTournamentID,
                                                     final int destTournamentID)
      throws SQLException {
    try (
        PreparedStatement destDelete = destinationConnection.prepareStatement("DELETE FROM subjective_overall_award WHERE tournament_id = ?");
        PreparedStatement destPrep = destinationConnection.prepareStatement("INSERT INTO subjective_overall_award (tournament_id, name, team_number, description, place) VALUES(?, ?, ?, ?, ?)");
        PreparedStatement sourcePrep = sourceConnection.prepareStatement("SELECT name, team_number, description, place FROM subjective_overall_award WHERE tournament_id = ?")) {
      LOGGER.debug("Importing subjective overall award");

      // do drops first
      destDelete.setInt(1, destTournamentID);
      destDelete.executeUpdate();

      // insert
      destPrep.setInt(1, destTournamentID);

      sourcePrep.setInt(1, sourceTournamentID);
      try (ResultSet sourceRS = sourcePrep.executeQuery()) {
        while (sourceRS.next()) {
          destPrep.setString(2, sourceRS.getString(1));
          destPrep.setInt(3, sourceRS.getInt(2));
          destPrep.setString(4, sourceRS.getString(3));
          destPrep.setInt(5, sourceRS.getInt(4));
          destPrep.executeUpdate();
        }
      }
    }
  }

  private static void importSubjectiveExtraWinners(final Connection sourceConnection,
                                                   final Connection destinationConnection,
                                                   final int sourceTournamentID,
                                                   final int destTournamentID)
      throws SQLException {
    importSubjectiveAwardGroupWinners("subjective_extra_award", sourceConnection, destinationConnection,
                                      sourceTournamentID, destTournamentID);
  }

  private static void importSubjectiveChallengeWinners(final Connection sourceConnection,
                                                       final Connection destinationConnection,
                                                       final int sourceTournamentID,
                                                       final int destTournamentID)
      throws SQLException {
    importSubjectiveAwardGroupWinners("subjective_challenge_award", sourceConnection, destinationConnection,
                                      sourceTournamentID, destTournamentID);
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Table name is passed in")
  private static void importSubjectiveAwardGroupWinners(final String tablename,
                                                        final Connection sourceConnection,
                                                        final Connection destinationConnection,
                                                        final int sourceTournamentID,
                                                        final int destTournamentID)
      throws SQLException {
    try (PreparedStatement destDelete = destinationConnection.prepareStatement("DELETE FROM "
        + tablename
        + " WHERE tournament_id = ?");
        PreparedStatement destPrep = destinationConnection.prepareStatement("INSERT INTO "
            + tablename
            + " (tournament_id, name, team_number, description, award_group, place) VALUES(?, ?, ?, ?, ?, ?)");
        PreparedStatement sourcePrep = sourceConnection.prepareStatement("SELECT name, team_number, description, award_group, place FROM "
            + tablename
            + " WHERE tournament_id = ?")) {

      // do drops first
      destDelete.setInt(1, destTournamentID);
      destDelete.executeUpdate();

      // insert
      destPrep.setInt(1, destTournamentID);

      sourcePrep.setInt(1, sourceTournamentID);
      try (ResultSet sourceRS = sourcePrep.executeQuery()) {
        while (sourceRS.next()) {
          destPrep.setString(2, sourceRS.getString(1));
          destPrep.setInt(3, sourceRS.getInt(2));
          destPrep.setString(4, sourceRS.getString(3));
          destPrep.setString(5, sourceRS.getString(4));
          destPrep.setInt(6, sourceRS.getInt(5));
          destPrep.executeUpdate();
        }
      }
    }
  }

  private static void importAwardReportGroupSort(final Connection sourceConnection,
                                                 final Connection destinationConnection,
                                                 final int sourceTournamentID,
                                                 final int destTournamentID)
      throws SQLException {
    try (
        PreparedStatement destDelete = destinationConnection.prepareStatement("DELETE FROM award_group_order WHERE tournament_id = ?");
        PreparedStatement destPrep = destinationConnection.prepareStatement("INSERT INTO award_group_order (tournament_id, award_group, sort_order) VALUES(?, ?, ?)");
        PreparedStatement sourcePrep = sourceConnection.prepareStatement("SELECT award_group, sort_order FROM award_group_order WHERE tournament_id = ?")) {
      LOGGER.debug("Importing award report group sort");

      // do drops first
      destDelete.setInt(1, destTournamentID);
      destDelete.executeUpdate();

      // insert
      destPrep.setInt(1, destTournamentID);

      sourcePrep.setInt(1, sourceTournamentID);
      try (ResultSet sourceRS = sourcePrep.executeQuery()) {
        while (sourceRS.next()) {
          destPrep.setString(2, sourceRS.getString("award_group"));
          destPrep.setInt(3, sourceRS.getInt("sort_order"));
          destPrep.executeUpdate();
        }
      }
    }
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
   * @throws SQLException on a database error
   */
  public static List<Team> findMissingTeams(final Connection sourceConnection,
                                            final Connection destConnection,
                                            final String tournament)
      throws SQLException {
    final List<Team> missingTeams = new LinkedList<>();

    final Tournament sourceTournament = Tournament.findTournamentByName(sourceConnection, tournament);

    final Map<Integer, TournamentTeam> sourceTeams = Queries.getTournamentTeams(sourceConnection,
                                                                                sourceTournament.getTournamentID());

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
  // See https://github.com/typetools/checker-framework/issues/979
  @SuppressWarnings("nullness")
  public static final ThreadLocal<DateFormat> CSV_TIMESTAMP_FORMATTER = new ThreadLocal<DateFormat>() {
    @Override
    protected DateFormat initialValue() {
      return new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
    }
  };

  /**
   * Time format found in the CSV dump files.
   */
  // See https://github.com/typetools/checker-framework/issues/979
  @SuppressWarnings("nullness")
  public static final ThreadLocal<DateFormat> CSV_TIME_FORMATTER = new ThreadLocal<DateFormat>() {
    @Override
    protected DateFormat initialValue() {
      return new SimpleDateFormat("HH:mm:ss");
    }
  };

  /**
   * Date format found in the CSV dump files.
   */
  // See https://github.com/typetools/checker-framework/issues/979
  @SuppressWarnings("nullness")
  public static final ThreadLocal<DateFormat> CSV_DATE_FORMATTER = new ThreadLocal<DateFormat>() {
    @Override
    protected DateFormat initialValue() {
      return new SimpleDateFormat("dd-MMM-yyyy");
    }
  };

  /**
   * The result of
   * {@link ImportDB#importDatabase(Connection, Connection, String, boolean, boolean, boolean)}.
   */
  public static final class ImportResult {

    /**
     * @param importDirectory {@link #getImportDirectory()}
     * @param hasBugs {@link #hasBugs()}
     */
    public ImportResult(final Path importDirectory,
                        final boolean hasBugs) {
      this.importDirectory = importDirectory;
      this.hasBugs = hasBugs;
    }

    private final Path importDirectory;

    /**
     * Any logs or bug reports will be in this directory. The directory may not
     * exist if there is no data for it.
     *
     * @return the directory where extra data is stored
     */

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
