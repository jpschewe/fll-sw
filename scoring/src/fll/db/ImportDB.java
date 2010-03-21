/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.db;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.mtu.eggplant.util.Functions;
import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import au.com.bytecode.opencsv.CSVReader;
import fll.Team;
import fll.Tournament;
import fll.Utilities;
import fll.db.TeamPropertyDifference.TeamProperty;
import fll.web.developer.importdb.TournamentDifference;
import fll.xml.ChallengeParser;
import fll.xml.XMLUtils;

/**
 * database, otherwise need to map... Import scores from a tournament database
 * into a master score database.
 * <p>
 * Example arguments: jdbc:hsqldb:file:/source;shutdown=true "Centennial Dec10"
 * jdbc:hsqldb:file:/destination;shutdown=true
 */
public final class ImportDB {

  private static final Logger LOG = Logger.getLogger(ImportDB.class);

  /**
   * Import tournament data from one database to another database
   * 
   * @param args source tournament destination
   */
  public static void main(final String[] args) {
    try {
      if (args.length != 3) {
        LOG.error("You must specify <source uri> <tournament> <destination uri>");
        for (final String arg : args) {
          LOG.error("arg: "
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
        if (args[1].charAt(substringEnd - 1) == '"'
            || args[1].charAt(substringEnd - 1) == '\'') {
          substringEnd = substringEnd - 1;
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
            LOG.info("Source either isn't HSQLDB or there is a problem", sqle);
          }
          try {
            stmt2 = destinationConnection.createStatement();
            stmt2.executeUpdate("SET WRITE_DELAY 1 MILLIS");
          } catch (final SQLException sqle) {
            LOG.info("Destination either isn't HSQLDB or there is a problem", sqle);
          }
        } finally {
          SQLFunctions.closeStatement(stmt1);
          SQLFunctions.closeStatement(stmt2);
        }

        final boolean differences = checkForDifferences(sourceConnection, destinationConnection, tournament);
        if (!differences) {
          LOG.info("Importing data for "
              + tournament + " from " + sourceURI + " to " + destinationURI);
          importDatabase(sourceConnection, destinationConnection, tournament);
          LOG.info("Data successfully imported");
        } else {
          LOG.error("Import aborted due to differences in databases");
        }

        try {
          try {
            stmt1 = sourceConnection.createStatement();
            stmt1.executeUpdate("SHUTDOWN COMPACT");
          } catch (final SQLException sqle) {
            LOG.info("Source either isn't HSQLDB or there is a problem", sqle);
          }
          try {
            stmt2 = destinationConnection.createStatement();
            stmt2.executeUpdate("SHUTDOWN COMPACT");
          } catch (final SQLException sqle) {
            LOG.info("Destination either isn't HSQLDB or there is a problem", sqle);
          }
        } finally {
          SQLFunctions.closeStatement(stmt1);
          SQLFunctions.closeStatement(stmt2);
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
   * <code>database</code>.
   * 
   * @param zipfile the dump file to read
   * @param database the path to the database to load into
   * @throws IOException if there is an error reading the dump file
   * @throws SQLException if there is an error importing the data
   */
  public static void loadFromDumpIntoNewDB(final ZipInputStream zipfile, final String database) throws IOException, SQLException {
    Connection destConnection = null;
    Statement destStmt = null;
    PreparedStatement destPrep = null;
    Connection memConnection = null;
    Statement memStmt = null;
    ResultSet memRS = null;
    PreparedStatement insertPrep = null;
    PreparedStatement checkPrep = null;
    ResultSet checkRS = null;
    Utilities.loadDBDriver();

    try {
      final String url = "jdbc:hsqldb:mem:dbimport"
          + String.valueOf(_importdbCount++);
      memConnection = DriverManager.getConnection(url);

      final Document challengeDocument = loadDatabaseDump(zipfile, memConnection);
      GenerateDB.generateDB(challengeDocument, database, true);

      destConnection = Utilities.createDataSource(database).getConnection();

      memStmt = memConnection.createStatement();

      // load the teams table into the destination database
      destStmt = destConnection.createStatement();
      memRS = memStmt.executeQuery("SELECT TeamNumber, TeamName, Organization, Division, Region FROM Teams");
      destPrep = destConnection.prepareStatement("INSERT INTO Teams (TeamNumber, TeamName, Organization, Division, Region) VALUES (?, ?, ?, ?, ?)");
      while (memRS.next()) {
        final int num = memRS.getInt(1);
        final String name = memRS.getString(2);
        final String org = memRS.getString(3);
        final String div = memRS.getString(4);
        final String region = memRS.getString(5);
        if (!Team.isInternalTeamNumber(num)) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Inserting into teams: "
                + num + ", " + name + ", " + org + ", " + div + ", " + region);
          }
          destPrep.setInt(1, num);
          destPrep.setString(2, name == null ? GenerateDB.DEFAULT_TEAM_NAME : name);
          destPrep.setString(3, org);
          destPrep.setString(4, div == null ? GenerateDB.DEFAULT_TEAM_DIVISION : div);
          destPrep.setString(5, region == null ? GenerateDB.DEFAULT_TEAM_REGION : region);
          destPrep.executeUpdate();
        }
      }
      SQLFunctions.closeResultSet(memRS);
      memRS = null;
      SQLFunctions.closePreparedStatement(destPrep);
      destPrep = null;

      // load all of the tournaments
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

      // shutdown new database
      destStmt.executeUpdate("SHUTDOWN COMPACT");
    } finally {
      SQLFunctions.closeResultSet(memRS);
      SQLFunctions.closeStatement(memStmt);
      SQLFunctions.closeConnection(memConnection);

      SQLFunctions.closePreparedStatement(insertPrep);

      SQLFunctions.closeResultSet(checkRS);
      SQLFunctions.closePreparedStatement(checkPrep);

      SQLFunctions.closePreparedStatement(destPrep);
      SQLFunctions.closeStatement(destStmt);
      SQLFunctions.closeConnection(destConnection);
    }
  }

  /**
   * Recursively create a tournament and it's next tournament.
   */
  private static void createTournament(final Tournament sourceTournament, final Connection destConnection) throws SQLException {
    // add the tournament to the tournaments table if it doesn't already
    // exist
    final Tournament destTournament = Tournament.findTournamentByName(destConnection, sourceTournament.getName());
    if (null == destTournament) {
      if (null == sourceTournament.getNextTournament()) {
        Tournament.createTournament(destConnection, sourceTournament.getName(), sourceTournament.getLocation());
      } else {
        createTournament(sourceTournament.getNextTournament(), destConnection);
        final Tournament nextTournament = Tournament.findTournamentByName(destConnection, sourceTournament.getNextTournament().getName());
        Tournament.createTournament(destConnection, sourceTournament.getName(), sourceTournament.getLocation(), nextTournament.getTournamentID());
      }
    }
  }

  /**
   * Keep track of the number of database imports so that the database names are
   * unique.
   */
  private static int _importdbCount = 0;

  /**
   * <p>
   * Load a database dumped as a zipfile into an existing empty database. No
   * checks are done, csv files are expected to be in the zipfile and they are
   * used as table names and table data in the database.
   * </p>
   * <p>
   * Once the database has been loaded it will be upgraded to the current
   * version using {@link #upgradeDatabase(Connection, Document)}.
   * </p>
   * 
   * @param zipfile the database dump
   * @return the challenge document
   * @throws IOException if there is an error reading the zipfile
   * @throws SQLException if there is an error loading the data into the
   *           database
   */
  public static Document loadDatabaseDump(final ZipInputStream zipfile, final Connection connection) throws IOException, SQLException {
    Document challengeDocument = null;

    final Map<String, Map<String, String>> typeInfo = new HashMap<String, Map<String, String>>();
    ZipEntry entry;
    while (null != (entry = zipfile.getNextEntry())) {
      final String name = entry.getName();
      if ("challenge.xml".equals(name)) {
        final Reader reader = new InputStreamReader(zipfile);
        challengeDocument = ChallengeParser.parse(reader);
      } else if (name.endsWith(".csv")) {
        final String tablename = name.substring(0, name.indexOf(".csv"));
        final Reader reader = new InputStreamReader(zipfile);
        Utilities.loadCSVFile(connection, tablename, reader);
      } else if (name.endsWith(".types")) {
        final String tablename = name.substring(0, name.indexOf(".types"));
        final Reader reader = new InputStreamReader(zipfile);
        final CSVReader csvreader = new CSVReader(reader);
        final Map<String, String> columnTypes = new HashMap<String, String>();

        String[] line;
        while (null != (line = csvreader.readNext())) {
          if (line.length != 2) {
            throw new RuntimeException(name
                + " has incorrect number of columns, should be 2");
          }
          columnTypes.put(line[0], line[1]);
        }

        typeInfo.put(tablename, columnTypes);
      } else {
        LOG.warn("Unexpected file found in imported zip file, skipping: "
            + name);
      }
      zipfile.closeEntry();
    }

    fixTableTypes(connection, typeInfo);

    upgradeDatabase(connection, challengeDocument);

    if (null == challengeDocument) {
      throw new RuntimeException("Cannot find challenge document in the zipfile");
    }
    return challengeDocument;
  }

  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE" }, justification = "Dynamic based upon tables in the database")
  private static void fixTableTypes(final Connection connection, final Map<String, Map<String, String>> typeInfo) throws SQLException {
    Statement stmt = null;
    try {
      stmt = connection.createStatement();

      for (final Map.Entry<String, Map<String, String>> tableEntry : typeInfo.entrySet()) {
        final String tableName = tableEntry.getKey();

        for (final Map.Entry<String, String> columnEntry : tableEntry.getValue().entrySet()) {
          final String columnName = columnEntry.getKey();
          final String columnType = columnEntry.getValue();

          // convert empty strings to null
          final String nullSQL = String.format("UPDATE %s SET %s = NULL WHERE %s = ''", tableName, columnName, columnName);
          stmt.executeUpdate(nullSQL);

          final String typeSQL = String.format("ALTER TABLE %s ALTER COLUMN %s %s", tableName, columnName, columnType);
          stmt.executeUpdate(typeSQL);
        }
      }
    } finally {
      SQLFunctions.closeStatement(stmt);
    }
  }

  /**
   * Upgrade the specified database to the current version. This is working
   * under the assumption that the current database is loaded from a set of CSV
   * files and therefore don't have any referential integrity constraints, so
   * we're only fixing up column names and the data in the column.
   * 
   * @param connection the database to upgrade
   * @throws SQLException on an error
   * @throws IllegalArgumentException if the database cannot be upgraded for
   *           some reason
   */
  private static void upgradeDatabase(final Connection connection, final Document challengeDocument) throws SQLException, IllegalArgumentException {
    final int dbVersion = Queries.getDatabaseVersion(connection);
    if (dbVersion < 1) {
      upgrade0To1(connection, challengeDocument);
    }
    final int newVersion = Queries.getDatabaseVersion(connection);
    if (newVersion < GenerateDB.DATABASE_VERSION) {
      throw new RuntimeException("Internal error, database version not updated to current instead was: "
          + newVersion);
    }
  }

  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Dynamic based upon tables in the database")
  private static void upgrade0To1(final Connection connection, final Document challengeDocument) throws SQLException {
    Statement stmt = null;
    ResultSet rs = null;
    PreparedStatement stringsToInts = null;
    try {
      stmt = connection.createStatement();

      stmt.executeUpdate("DROP Table IF EXISTS TournamentParameters");

      // add the global_parameters table
      GenerateDB.createGlobalParameters(challengeDocument, connection, true, Queries.getTablesInDB(connection));

      // ---- switch from string tournament names to integers ----

      // get all data from Tournaments table
      final Map<String, String> nameLocation = new HashMap<String, String>();
      final Map<String, String> nameNext = new HashMap<String, String>();
      rs = stmt.executeQuery("SELECT Name, Location, NextTournament FROM Tournaments");
      while (rs.next()) {
        final String name = rs.getString(1);
        final String location = rs.getString(2);
        final String nextName = rs.getString(3);
        nameLocation.put(name, location);
        nameNext.put(name, nextName);
      }
      SQLFunctions.closeResultSet(rs);

      // drop Tournaments table
      stmt.executeUpdate("DROP TABLE Tournaments");

      // create Tournaments table
      GenerateDB.tournaments(connection, true, Queries.getTablesInDB(connection));

      // add all tournaments back
      for (final Map.Entry<String, String> entry : nameLocation.entrySet()) {
        if (!GenerateDB.INTERNAL_TOURNAMENT_NAME.equals(entry.getKey())) {
          final Tournament tournament = Tournament.findTournamentByName(connection, entry.getKey());
          if (null == tournament) {
            Tournament.createTournament(connection, entry.getKey(), entry.getValue());
          }
        }
      }
      // set next tournaments
      for (final Map.Entry<String, String> entry : nameNext.entrySet()) {
        if (!GenerateDB.INTERNAL_TOURNAMENT_NAME.equals(entry.getKey())) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Setting next tournament of #"
                + entry.getKey() + "# to #" + entry.getValue() + "#");
          }
          Tournament.setNextTournament(connection, entry.getKey(), entry.getValue());
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
      SQLFunctions.closeResultSet(rs);

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
        stringsToInts = connection.prepareStatement(String.format("UPDATE %s SET Tournament = ? WHERE Tournament = ?", table));
        for (final Map.Entry<String, Integer> entry : nameID.entrySet()) {
          stringsToInts.setInt(1, entry.getValue());
          stringsToInts.setString(2, entry.getKey());
          stringsToInts.executeUpdate();
        }
        SQLFunctions.closePreparedStatement(stringsToInts);
      }

      // create new tournament parameters table
      GenerateDB.tournamentParameters(connection, true, Queries.getTablesInDB(connection));

      GenerateDB.setDefaultParameters(connection);

      // set the version to 1 - this will have been set while creating
      // global_parameters, but we need to force it to 1 for later upgrade
      // functions to not be confused
      setDBVersion(connection, 1);

    } finally {
      SQLFunctions.closeResultSet(rs);
      SQLFunctions.closeStatement(stmt);
      SQLFunctions.closePreparedStatement(stringsToInts);
    }
  }

  private static void setDBVersion(final Connection connection, final int version) throws SQLException {
    PreparedStatement setVersion = null;
    try {
      setVersion = connection.prepareStatement("UPDATE global_parameters SET param_value = ? WHERE param = ?");
      setVersion.setInt(1, version);
      setVersion.setString(2, GlobalParameters.DATABASE_VERSION);
      setVersion.executeUpdate();
    } finally {
      SQLFunctions.closePreparedStatement(setVersion);
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
  public static void importDatabase(final Connection sourceConnection, final Connection destinationConnection, final String tournamentName) throws SQLException {

    final Document document = Queries.getChallengeDocument(destinationConnection);
    final Element rootElement = document.getDocumentElement();

    final Tournament sourceTournament = Tournament.findTournamentByName(sourceConnection, tournamentName);
    final int sourceTournamentID = sourceTournament.getTournamentID();
    final Tournament destTournament = Tournament.findTournamentByName(destinationConnection, tournamentName);
    final int destTournamentID = destTournament.getTournamentID();

    importJudges(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);

    importTournamentTeams(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);

    importPerformance(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID, rootElement);

    importSubjective(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID, rootElement);

    importTableNames(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);

    importPlayoffData(sourceConnection, destinationConnection, sourceTournamentID, destTournamentID);
  }

  private static void importPlayoffData(final Connection sourceConnection,
                                        final Connection destinationConnection,
                                        final int sourceTournamentID,
                                        final int destTournamentID) throws SQLException {
    LOG.info("Importing PlayoffData");

    PreparedStatement destPrep = null;
    PreparedStatement sourcePrep = null;
    ResultSet sourceRS = null;
    try {
      destPrep = destinationConnection.prepareStatement("DELETE FROM PlayoffData WHERE Tournament = ?");
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
      SQLFunctions.closePreparedStatement(destPrep);

      sourcePrep = sourceConnection.prepareStatement("SELECT event_division, Tournament, PlayoffRound, LineNumber, Team, AssignedTable, Printed "
          + "FROM PlayoffData WHERE Tournament=?");
      sourcePrep.setInt(1, sourceTournamentID);
      destPrep = destinationConnection.prepareStatement("INSERT INTO PlayoffData (event_division, Tournament, PlayoffRound,"
          + "LineNumber, Team, AssignedTable, Printed) VALUES (?, ?, ?, ?, ?, ?, ?)");
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        for (int i = 1; i < 8; i++) {
          Object sourceObj = sourceRS.getObject(i);
          if ("".equals(sourceObj)) {
            sourceObj = null;
          }
          destPrep.setObject(i, sourceObj);
        }
        destPrep.executeUpdate();
      }
    } finally {
      SQLFunctions.closeResultSet(sourceRS);
      SQLFunctions.closePreparedStatement(sourcePrep);
      SQLFunctions.closePreparedStatement(destPrep);
    }
  }

  private static void importTableNames(final Connection sourceConnection,
                                       final Connection destinationConnection,
                                       final int sourceTournamentID,
                                       final int destTournamentID) throws SQLException {
    PreparedStatement destPrep = null;
    PreparedStatement sourcePrep = null;
    ResultSet sourceRS = null;
    try {
      LOG.info("Importing tablenames");
      destPrep = destinationConnection.prepareStatement("DELETE FROM tablenames WHERE Tournament = ?");
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
      SQLFunctions.closePreparedStatement(destPrep);

      sourcePrep = sourceConnection.prepareStatement("SELECT Tournament, PairID, SideA, SideB "
          + "FROM tablenames WHERE Tournament=?");
      sourcePrep.setInt(1, sourceTournamentID);
      destPrep = destinationConnection.prepareStatement("INSERT INTO tablenames (Tournament, PairID, SideA, SideB) "
          + "VALUES (?, ?, ?, ?)");
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        for (int i = 1; i <= 4; i++) {
          Object sourceObj = sourceRS.getObject(i);
          if ("".equals(sourceObj)) {
            sourceObj = null;
          }
          destPrep.setObject(i, sourceObj);
        }
        destPrep.executeUpdate();
      }
    } finally {
      SQLFunctions.closeResultSet(sourceRS);
      SQLFunctions.closePreparedStatement(sourcePrep);
      SQLFunctions.closePreparedStatement(destPrep);
    }
  }

  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Dynamic based upon categories and goal")
  private static void importSubjective(final Connection sourceConnection,
                                       final Connection destinationConnection,
                                       final int sourceTournamentID,
                                       final int destTournamentID,
                                       final Element rootElement) throws SQLException {
    PreparedStatement destPrep = null;
    PreparedStatement sourcePrep = null;
    ResultSet sourceRS = null;
    try {
      // loop over each subjective category
      for (final Element categoryElement : XMLUtils.filterToElements(rootElement.getElementsByTagName("subjectiveCategory"))) {
        final String tableName = categoryElement.getAttribute("name");
        LOG.info("Importing "
            + tableName);

        destPrep = destinationConnection.prepareStatement("DELETE FROM "
            + tableName + " WHERE Tournament = ?");
        destPrep.setInt(1, destTournamentID);
        destPrep.executeUpdate();
        SQLFunctions.closePreparedStatement(destPrep);

        final StringBuffer columns = new StringBuffer();
        columns.append(" TeamNumber,");
        columns.append(" Tournament,");
        final List<Element> goals = XMLUtils.filterToElements(categoryElement.getElementsByTagName("goal"));
        final int numColumns = goals.size() + 3;
        for (final Element element : goals) {
          columns.append(" "
              + element.getAttribute("name") + ",");
        }
        columns.append(" Judge");

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

        sourcePrep = sourceConnection.prepareStatement("SELECT "
            + columns.toString() + " FROM " + tableName + " WHERE Tournament = ?");
        sourcePrep.setInt(1, sourceTournamentID);
        sourceRS = sourcePrep.executeQuery();
        while (sourceRS.next()) {
          for (int i = 0; i < numColumns; i++) {
            Object sourceObj = sourceRS.getObject(i + 1);
            if ("".equals(sourceObj)) {
              sourceObj = null;
            }
            destPrep.setObject(i + 1, sourceObj);
          }
          destPrep.executeUpdate();
        }
        SQLFunctions.closeResultSet(sourceRS);
        SQLFunctions.closePreparedStatement(sourcePrep);
        SQLFunctions.closePreparedStatement(destPrep);
      }
    } finally {
      SQLFunctions.closeResultSet(sourceRS);
      SQLFunctions.closePreparedStatement(sourcePrep);
      SQLFunctions.closePreparedStatement(destPrep);
    }
  }

  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Dynamic based upon goals")
  private static void importPerformance(final Connection sourceConnection,
                                        final Connection destinationConnection,
                                        final int sourceTournamentID,
                                        final int destTournamentID,
                                        final Element rootElement) throws SQLException {
    PreparedStatement destPrep = null;
    PreparedStatement sourcePrep = null;
    ResultSet sourceRS = null;
    try {
      LOG.info("Importing performance scores");
      final Element performanceElement = (Element) rootElement.getElementsByTagName("Performance").item(0);
      final String tableName = "Performance";
      destPrep = destinationConnection.prepareStatement("DELETE FROM "
          + tableName + " WHERE Tournament = ?");
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
      SQLFunctions.closePreparedStatement(destPrep);

      final StringBuffer columns = new StringBuffer();
      columns.append(" TeamNumber,");
      columns.append(" Tournament,");
      columns.append(" RunNumber,");
      // Note: If TimeStamp is no longer the 4th element, then the hack below
      // needs to be modified
      columns.append(" TimeStamp,");
      final List<Element> goals = XMLUtils.filterToElements(performanceElement.getElementsByTagName("goal"));
      final int numColumns = goals.size() + 7;
      for (final Element element : goals) {
        columns.append(" "
            + element.getAttribute("name") + ",");
      }
      columns.append(" NoShow,");
      columns.append(" Bye,");
      columns.append(" Verified");

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

      sourcePrep = sourceConnection.prepareStatement("SELECT "
          + columns.toString() + " FROM " + tableName + " WHERE Tournament = ?");
      sourcePrep.setInt(1, sourceTournamentID);
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        for (int i = 0; i < numColumns; i++) {
          Object sourceObj = sourceRS.getObject(i + 1);
          if ("".equals(sourceObj)) {
            sourceObj = null;
          }
          // FIXME Hack for timestamps - need a better solution
          if (3 == i) {
            // timestamp column of the performance table
            if (sourceObj instanceof String) {

              try {
                sourceObj = new Timestamp(CSV_TIMESTAMP_FORMATTER.parse((String) sourceObj).getTime());
              } catch (final ParseException pe) {
                LOG.warn("Got an error parsing performance timestamps, this is probably because of the hack in here.", pe);
              }
            }
          }
          destPrep.setObject(i + 1, sourceObj);
        }
        destPrep.executeUpdate();
      }
    } finally {
      SQLFunctions.closeResultSet(sourceRS);
      SQLFunctions.closePreparedStatement(sourcePrep);
      SQLFunctions.closePreparedStatement(destPrep);
    }
  }

  private static void importTournamentTeams(final Connection sourceConnection,
                                            final Connection destinationConnection,
                                            final int sourceTournamentID,
                                            final int destTournamentID) throws SQLException {
    PreparedStatement destPrep = null;
    PreparedStatement sourcePrep = null;
    ResultSet sourceRS = null;
    try {
      LOG.info("Importing TournamentTeams");
      destPrep = destinationConnection.prepareStatement("DELETE FROM TournamentTeams WHERE Tournament = ?");
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
      SQLFunctions.closePreparedStatement(destPrep);
      sourcePrep = sourceConnection.prepareStatement("SELECT TeamNumber, event_division FROM TournamentTeams WHERE Tournament = ?");
      sourcePrep.setInt(1, sourceTournamentID);
      destPrep = destinationConnection.prepareStatement("INSERT INTO TournamentTeams (Tournament, TeamNumber, event_division) VALUES (?, ?, ?)");
      destPrep.setInt(1, destTournamentID);
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        final int teamNumber = sourceRS.getInt(1);
        if (!Team.isInternalTeamNumber(teamNumber)) {
          final String eventDivision = sourceRS.getString(2);
          destPrep.setInt(2, teamNumber);
          destPrep.setString(3, eventDivision == null ? GenerateDB.DEFAULT_TEAM_DIVISION : eventDivision);
          destPrep.executeUpdate();
        }
      }
    } finally {
      SQLFunctions.closeResultSet(sourceRS);
      SQLFunctions.closePreparedStatement(sourcePrep);
      SQLFunctions.closePreparedStatement(destPrep);
    }
  }

  private static void importJudges(final Connection sourceConnection,
                                   final Connection destinationConnection,
                                   final int sourceTournamentID,
                                   final int destTournamentID) throws SQLException {
    PreparedStatement destPrep = null;
    PreparedStatement sourcePrep = null;
    ResultSet sourceRS = null;
    try {
      LOG.info("Importing Judges");

      destPrep = destinationConnection.prepareStatement("DELETE FROM Judges WHERE Tournament = ?");
      destPrep.setInt(1, destTournamentID);
      destPrep.executeUpdate();
      SQLFunctions.closePreparedStatement(destPrep);

      destPrep = destinationConnection.prepareStatement("INSERT INTO Judges (id, category, event_division, Tournament) VALUES (?, ?, ?, ?)");
      destPrep.setInt(4, destTournamentID);

      sourcePrep = sourceConnection.prepareStatement("SELECT id, category, event_division FROM Judges WHERE Tournament = ?");
      sourcePrep.setInt(1, sourceTournamentID);
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        destPrep.setString(1, sourceRS.getString(1));
        destPrep.setString(2, sourceRS.getString(2));
        destPrep.setString(3, sourceRS.getString(3));
        destPrep.executeUpdate();
      }
    } finally {
      SQLFunctions.closeResultSet(sourceRS);
      SQLFunctions.closePreparedStatement(sourcePrep);
      SQLFunctions.closePreparedStatement(destPrep);
    }
  }

  /**
   * Check for differences between two tournaments in team information.
   * 
   * @return true if there are differences
   */
  public static boolean checkForDifferences(final Connection sourceConnection, final Connection destConnection, final String tournament) throws SQLException {

    // check that the tournament exists
    final Tournament destTournament = Tournament.findTournamentByName(destConnection, tournament);
    if (null == destTournament) {
      LOG.error("Tournament: "
          + tournament + " doesn't exist in the destination database!");
      return true;
    }

    final Tournament sourceTournament = Tournament.findTournamentByName(sourceConnection, tournament);
    if (null == sourceTournament) {
      LOG.error("Tournament: "
          + tournament + " doesn't exist in the source database!");
      return true;
    }

    boolean differencesFound = false;
    // check for missing teams
    final List<Team> missingTeams = findMissingTeams(sourceConnection, destConnection, tournament);
    if (!missingTeams.isEmpty()) {
      for (final Team team : missingTeams) {
        LOG.error(new Formatter().format("Team %d is in the source database, but not the dest database", team.getTeamNumber()));
      }
      differencesFound = true;
    }

    // check team info
    final List<TeamPropertyDifference> teamDifferences = checkTeamInfo(sourceConnection, destConnection, tournament);
    if (!teamDifferences.isEmpty()) {
      for (final TeamPropertyDifference diff : teamDifferences) {
        LOG.error(new Formatter().format("%s is different for team %d source value: %s dest value: %s", diff.getProperty(), diff.getTeamNumber(),
                                         diff.getSourceValue(), diff.getDestValue()));
      }
      differencesFound = true;
    }

    // check tournament teams
    final List<TournamentDifference> tournamentDifferences = computeMissingFromTournamentTeams(sourceConnection, destConnection, tournament);
    if (!tournamentDifferences.isEmpty()) {
      differencesFound = true;
      for (final TournamentDifference diff : tournamentDifferences) {
        LOG.error(new Formatter().format("%d is in tournament %s in the source database and tournament %s in the dest database", diff.getTeamNumber(),
                                         diff.getSourceTournament(), diff.getDestTournament()));
      }
    }

    // TODO check documents bug: 2126186

    return differencesFound;
  }

  /**
   * Compute the teams that are missing from TournamenTeams between the two
   * databases.
   * 
   * @param sourceConnection
   * @param destConnection
   * @param tournament
   * @return the differences
   * @throws SQLException
   */
  public static List<TournamentDifference> computeMissingFromTournamentTeams(final Connection sourceConnection,
                                                                             final Connection destConnection,
                                                                             final String tournament) throws SQLException {
    final Set<Integer> sourceMissing = new HashSet<Integer>();
    final Set<Integer> destMissing = new HashSet<Integer>();

    // check that tournament teams is the same for both databases
    final Set<Integer> sourceTeams = getTournamentTeams(sourceConnection, tournament);
    final Set<Integer> destTeams = getTournamentTeams(destConnection, tournament);

    // diff the lists
    destMissing.addAll(sourceTeams);
    sourceMissing.addAll(destTeams);
    destMissing.removeAll(destTeams);
    sourceMissing.removeAll(sourceTeams);

    final Set<Integer> differences = new HashSet<Integer>();
    differences.addAll(sourceMissing);
    differences.addAll(destMissing);
    final List<TournamentDifference> tournamentDifferences = new LinkedList<TournamentDifference>();
    for (final int teamNumber : differences) {
      final int sourceTournament = Queries.getTeamCurrentTournament(sourceConnection, teamNumber);
      final String sourceName = Tournament.findTournamentByID(sourceConnection, sourceTournament).getName();
      final int destTournament = Queries.getTeamCurrentTournament(destConnection, teamNumber);
      final String destName = Tournament.findTournamentByID(destConnection, destTournament).getName();
      tournamentDifferences.add(new TournamentDifference(teamNumber, sourceName, destName));
    }
    return tournamentDifferences;
  }

  private static Set<Integer> getTournamentTeams(final Connection connection, final String tournament) throws SQLException {
    final Set<Integer> teams = new HashSet<Integer>();
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT TeamNumber FROM TournamentTeams WHERE Tournament = ?");
      prep.setString(1, tournament);
      rs = prep.executeQuery();
      while (rs.next()) {
        final int teamNumber = rs.getInt(1);
        teams.add(teamNumber);
      }
    } finally {
      SQLFunctions.closeResultSet(rs);
      SQLFunctions.closePreparedStatement(prep);
    }
    return teams;
  }

  /**
   * @param sourceConnection source connection
   * @param destConnection destination connection
   * @param tournament the tournament to check
   * @return the differences, empty list if no differences
   * @throws SQLException
   */
  public static List<TeamPropertyDifference> checkTeamInfo(final Connection sourceConnection, final Connection destConnection, final String tournament)
      throws SQLException {
    final List<TeamPropertyDifference> differences = new LinkedList<TeamPropertyDifference>();

    PreparedStatement sourcePrep = null;
    PreparedStatement destPrep = null;
    ResultSet sourceRS = null;
    ResultSet destRS = null;
    try {
      destPrep = destConnection.prepareStatement("SELECT Teams.TeamName, Teams.Region, Teams.Division, Teams.Organization"
          + " FROM Teams" + " WHERE Teams.TeamNumber = ?");

      sourcePrep = sourceConnection.prepareStatement("SELECT Teams.TeamNumber, Teams.TeamName, Teams.Region, Teams.Division, Teams.Organization"
          + " FROM Teams, TournamentTeams" + " WHERE Teams.TeamNumber = TournamentTeams.TeamNumber" + " AND TournamentTeams.Tournament = ?");

      sourcePrep.setString(1, tournament);
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        final int teamNumber = sourceRS.getInt(1);
        final String sourceName = sourceRS.getString(2);
        final String sourceRegion = sourceRS.getString(3);
        final String sourceDivision = sourceRS.getString(4);
        final String sourceOrganization = sourceRS.getString(5);
        destPrep.setInt(1, teamNumber);
        destRS = destPrep.executeQuery();
        if (destRS.next()) {
          final String destName = destRS.getString(1);
          if (!Functions.safeEquals(destName, sourceName)) {
            differences.add(new TeamPropertyDifference(teamNumber, TeamProperty.NAME, sourceName, destName));
          }
          final String destRegion = destRS.getString(2);
          if (!Functions.safeEquals(destRegion, sourceRegion)) {
            differences.add(new TeamPropertyDifference(teamNumber, TeamProperty.REGION, sourceRegion, destRegion));
          }
          final String destDivision = destRS.getString(3);
          if (!Functions.safeEquals(destDivision, sourceDivision)) {
            differences.add(new TeamPropertyDifference(teamNumber, TeamProperty.DIVISION, sourceDivision, destDivision));
          }
          final String destOrganization = destRS.getString(4);
          if (!Functions.safeEquals(destOrganization, sourceOrganization)) {
            differences.add(new TeamPropertyDifference(teamNumber, TeamProperty.ORGANIZATION, sourceOrganization, destOrganization));
          }
        }
        // else handled by findMissingTeams

        SQLFunctions.closeResultSet(destRS);
      }
    } finally {
      SQLFunctions.closeResultSet(destRS);
      SQLFunctions.closeResultSet(sourceRS);
      SQLFunctions.closePreparedStatement(destPrep);
      SQLFunctions.closePreparedStatement(sourcePrep);
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
   * @return the teams in the source database and not in the dest database
   * @throws SQLException
   */
  public static List<Team> findMissingTeams(final Connection sourceConnection, final Connection destConnection, final String tournament) throws SQLException {
    final List<Team> missingTeams = new LinkedList<Team>();

    PreparedStatement sourcePrep = null;
    PreparedStatement destPrep = null;
    ResultSet sourceRS = null;
    ResultSet destRS = null;
    try {
      destPrep = destConnection.prepareStatement("SELECT Teams.TeamName"
          + " FROM Teams WHERE Teams.TeamNumber = ?");

      sourcePrep = sourceConnection.prepareStatement("SELECT Teams.TeamNumber FROM Teams, TournamentTeams"
          + " WHERE Teams.TeamNumber = TournamentTeams.TeamNumber" + " AND TournamentTeams.Tournament = ?");
      sourcePrep.setString(1, tournament);
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        final int teamNumber = sourceRS.getInt(1);
        destPrep.setInt(1, teamNumber);
        destRS = destPrep.executeQuery();
        if (!destRS.next()) {
          missingTeams.add(Team.getTeamFromDatabase(sourceConnection, teamNumber));
        }
        SQLFunctions.closeResultSet(destRS);
      }
    } finally {
      SQLFunctions.closeResultSet(destRS);
      SQLFunctions.closeResultSet(sourceRS);
      SQLFunctions.closePreparedStatement(destPrep);
      SQLFunctions.closePreparedStatement(sourcePrep);
    }
    return missingTeams;
  }

  private static final SimpleDateFormat CSV_TIMESTAMP_FORMATTER = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");

}
