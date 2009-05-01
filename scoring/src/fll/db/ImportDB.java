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
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.mtu.eggplant.util.Functions;
import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Utilities;
import fll.xml.ChallengeParser;
import fll.xml.XMLUtils;

/**
 * Import scores from a tournament database into a master score database.
 * <p>
 * Example arguments: jdbc:hsqldb:file:/source;shutdown=true "Centennial Dec10"
 * jdbc:hsqldb:file:/destination;shutdown=true
 * 
 * @version $Revision$
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
          final Document challengeDocument = Queries.getChallengeDocument(destinationConnection);
          importDatabase(sourceConnection, destinationConnection, tournament, challengeDocument);
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
    } catch (final Exception e) {
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
      memStmt = memConnection.createStatement();

      final Document challengeDocument = loadDatabaseDump(zipfile, memConnection);
      GenerateDB.generateDB(challengeDocument, database, true);

      destConnection = Utilities.createDataSource(database).getConnection();

      // load the teams table into the destination database
      destStmt = destConnection.createStatement();
      memRS = memStmt.executeQuery("SELECT TeamNumber, TeamName, Organization, Division, Region FROM Teams");
      destPrep = destConnection.prepareStatement("INSERT INTO Teams (TeamNumber, TeamName, Organization, Division, Region) VALUES (?, ?, ?, ?, ?)");
      while (memRS.next()) {
        destPrep.setInt(1, memRS.getInt(1));
        destPrep.setString(2, memRS.getString(2));
        destPrep.setString(3, memRS.getString(3));
        destPrep.setString(4, memRS.getString(4));
        destPrep.setString(5, memRS.getString(5));
        destPrep.executeUpdate();
      }

      // for each tournament listed in the dump file, import it
      insertPrep = destConnection.prepareStatement("INSERT INTO Tournaments (Name, Location, NextTournament) VALUES(?, ?, ?)");
      checkPrep = destConnection.prepareStatement("SELECT Name FROM Tournaments WHERE Name = ?");
      memRS = memStmt.executeQuery("SELECT Name, Location, NextTournament FROM Tournaments");
      while (memRS.next()) {
        final String tournament = memRS.getString(1);
        final String location = memRS.getString(2);
        final String nextTournament = memRS.getString(3);

        // add the tournament to the tournaments table if it doesn't already
        // exist
        checkPrep.setString(1, tournament);
        checkRS = checkPrep.executeQuery();
        if (!checkRS.next()) {
          insertPrep.setString(1, tournament);
          insertPrep.setString(2, location);
          insertPrep.setString(3, nextTournament);
          insertPrep.executeUpdate();
        }
        SQLFunctions.closeResultSet(checkRS);

        // import the data from the tournament
        importDatabase(memConnection, destConnection, tournament, challengeDocument);
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
   * Keep track of the number of database imports so that the database names are
   * unique.
   */
  private static int _importdbCount = 0;

  /**
   * Load a database dumped as a zipfile into an existing empty database. No
   * checks are done, csv files are expected to be in the zipfile and they are
   * used as table names and table data in the database.
   * 
   * @param zipfile the database dump
   * @return the challenge document
   * @throws IOException if there is an error reading the zipfile
   * @throws SQLException if there is an error loading the data into the
   *           database
   */
  public static Document loadDatabaseDump(final ZipInputStream zipfile, final Connection connection) throws IOException, SQLException {
    Document challengeDocument = null;

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
      } else {
        LOG.warn("Unexpected file found in imported zip file, skipping: "
            + name);
      }
      zipfile.closeEntry();
    }

    if (null == challengeDocument) {
      throw new RuntimeException("Cannot find challenge document in the zipfile");
    }
    return challengeDocument;
  }

  /**
   * Import scores from database for tournament into the database for
   * connection. This method does no checking for differences, it is assumed you
   * have taken care of this already. This method will delete all information
   * related to the specified tournament from the destination database and then
   * copy the information from the source database.
   * 
   * @param sourceConnection a connection to the source database
   * @param destinationConnection a connection to the destination database
   * @param tournament the tournament that the scores are for
   * @param document the XML document that describes the tournament
   */
  public static void importDatabase(final Connection sourceConnection, final Connection destinationConnection, final String tournament, final Document document)
      throws SQLException {
    PreparedStatement destPrep = null;
    PreparedStatement sourcePrep = null;
    ResultSet sourceRS = null;
    try {
      final Element rootElement = document.getDocumentElement();

      // judges
      LOG.info("Importing Judges");
      destPrep = destinationConnection.prepareStatement("DELETE FROM Judges WHERE Tournament = ?");
      destPrep.setString(1, tournament);
      destPrep.executeUpdate();
      SQLFunctions.closePreparedStatement(destPrep);

      destPrep = destinationConnection.prepareStatement("INSERT INTO Judges (id, category, event_division, Tournament) VALUES (?, ?, ?, ?)");
      destPrep.setString(4, tournament);

      sourcePrep = sourceConnection.prepareStatement("SELECT id, category, event_division FROM Judges WHERE Tournament = ?");
      sourcePrep.setString(1, tournament);
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        destPrep.setString(1, sourceRS.getString(1));
        destPrep.setString(2, sourceRS.getString(2));
        destPrep.setString(3, sourceRS.getString(3));
        destPrep.executeUpdate();
      }
      SQLFunctions.closeResultSet(sourceRS);
      SQLFunctions.closePreparedStatement(sourcePrep);
      SQLFunctions.closePreparedStatement(destPrep);

      // import tournament teams
      LOG.info("Importing TournamentTeams");
      destPrep = destinationConnection.prepareStatement("DELETE FROM TournamentTeams WHERE Tournament = ?");
      destPrep.setString(1, tournament);
      destPrep.executeUpdate();
      SQLFunctions.closePreparedStatement(destPrep);
      sourcePrep = sourceConnection.prepareStatement("SELECT TeamNumber, event_division FROM TournamentTeams WHERE Tournament = ?");
      sourcePrep.setString(1, tournament);
      destPrep = destinationConnection.prepareStatement("INSERT INTO TournamentTeams (Tournament, TeamNumber, event_division) VALUeS (?, ?, ?)");
      destPrep.setString(1, tournament);
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        destPrep.setInt(2, sourceRS.getInt(1));
        destPrep.setString(3, sourceRS.getString(2));
        destPrep.executeUpdate();
      }
      SQLFunctions.closeResultSet(sourceRS);
      SQLFunctions.closePreparedStatement(sourcePrep);
      SQLFunctions.closePreparedStatement(destPrep);

      // performance
      {
        LOG.info("Importing performance scores");
        final Element performanceElement = (Element) rootElement.getElementsByTagName("Performance").item(0);
        final String tableName = "Performance";
        destPrep = destinationConnection.prepareStatement("DELETE FROM "
            + tableName + " WHERE Tournament = ?");
        destPrep.setString(1, tournament);
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
        sourcePrep.setString(1, tournament);
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
        SQLFunctions.closeResultSet(sourceRS);
        SQLFunctions.closePreparedStatement(sourcePrep);
        SQLFunctions.closePreparedStatement(destPrep);
      }

      // loop over each subjective category
      for (final Element categoryElement : XMLUtils.filterToElements(rootElement.getElementsByTagName("subjectiveCategory"))) {
        final String tableName = categoryElement.getAttribute("name");
        LOG.info("Importing "
            + tableName);

        destPrep = destinationConnection.prepareStatement("DELETE FROM "
            + tableName + " WHERE Tournament = ?");
        destPrep.setString(1, tournament);
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
        sourcePrep.setString(1, tournament);
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

      // TableNames
      {
        LOG.info("Importing tablenames");
        destPrep = destinationConnection.prepareStatement("DELETE FROM tablenames WHERE Tournament = ?");
        destPrep.setString(1, tournament);
        destPrep.executeUpdate();
        SQLFunctions.closePreparedStatement(destPrep);

        sourcePrep = sourceConnection.prepareStatement("SELECT Tournament, PairID, SideA, SideB "
            + "FROM tablenames WHERE Tournament=?");
        sourcePrep.setString(1, tournament);
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
        SQLFunctions.closeResultSet(sourceRS);
        SQLFunctions.closePreparedStatement(sourcePrep);
        SQLFunctions.closePreparedStatement(destPrep);
      }

      // PlayoffData
      {
        LOG.info("Importing PlayoffData");
        destPrep = destinationConnection.prepareStatement("DELETE FROM PlayoffData WHERE Tournament = ?");
        destPrep.setString(1, tournament);
        destPrep.executeUpdate();
        SQLFunctions.closePreparedStatement(destPrep);

        sourcePrep = sourceConnection.prepareStatement("SELECT event_division, Tournament, PlayoffRound, LineNumber, Team, AssignedTable, Printed "
            + "FROM PlayoffData WHERE Tournament=?");
        sourcePrep.setString(1, tournament);
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

  /**
   * Check for differences between two tournaments in team information.
   * 
   * @return true if there are differences
   */
  public static boolean checkForDifferences(final Connection sourceConnection, final Connection destConnection, final String tournament) throws SQLException {
    PreparedStatement sourcePrep = null;
    PreparedStatement destPrep = null;
    ResultSet sourceRS = null;
    ResultSet destRS = null;
    boolean differencesFound = false;
    try {
      // check that the tournament exists
      destPrep = destConnection.prepareStatement("SELECT Name FROM Tournaments WHERE Name = ?");
      destPrep.setString(1, tournament);
      if (!destPrep.executeQuery().next()) {
        LOG.error("Tournament: "
            + tournament + " doesn't exist in the destination database!");
        return true;
      }
      SQLFunctions.closePreparedStatement(destPrep);

      sourcePrep = sourceConnection.prepareStatement("SELECT Name FROM Tournaments WHERE Name = ?");
      sourcePrep.setString(1, tournament);
      if (!sourcePrep.executeQuery().next()) {
        LOG.error("Tournament: "
            + tournament + " doesn't exist in the source database!");
        return true;
      }
      SQLFunctions.closePreparedStatement(sourcePrep);

      // check names and regions and make sure that each team in the source
      // tournament is in the destination tournament
      destPrep = destConnection.prepareStatement("SELECT Teams.TeamName, Teams.Region, Teams.Division, Teams.Organization"
          + " FROM Teams" + " WHERE Teams.TeamNumber = ?");

      sourcePrep = sourceConnection.prepareStatement("SELECT Teams.TeamNumber, Teams.TeamName, Teams.Region, Teams.Division, Teams.Organization"
          + " FROM Teams, TournamentTeams" + " WHERE Teams.TeamNumber = TournamentTeams.TeamNumber" + " AND TournamentTeams.Tournament = ?");

      sourcePrep.setString(1, tournament);
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        final int num = sourceRS.getInt(1);
        final String sourceName = sourceRS.getString(2);
        final String sourceRegion = sourceRS.getString(3);
        final String sourceDivision = sourceRS.getString(4);
        final String sourceOrganization = sourceRS.getString(5);
        destPrep.setInt(1, num);
        destRS = destPrep.executeQuery();
        if (destRS.next()) {
          final String destName = destRS.getString(1);
          if (!Functions.safeEquals(destName, sourceName)) {
            differencesFound = true;
            LOG.error("There is a team with a different name in the source database that in the destination database.  Number: "
                + num + " source name: " + sourceName + " dest name: " + destName);
          }
          final String destRegion = destRS.getString(2);
          if (!Functions.safeEquals(destRegion, sourceRegion)) {
            differencesFound = true;
            LOG.error("There is a team with a different region in the source database that in the destination database.  Number: "
                + num + " source region: " + sourceRegion + " dest region: " + destRegion);
          }
          final String destDivision = destRS.getString(3);
          if (!Functions.safeEquals(destDivision, sourceDivision)) {
            differencesFound = true;
            LOG.error("There is a team with a different division in the source database that in the destination database.  Number: "
                + num + " source division: " + sourceDivision + " dest division: " + destDivision);
          }
          final String destOrganization = destRS.getString(4);
          if (!Functions.safeEquals(destOrganization, sourceOrganization)) {
            differencesFound = true;
            LOG.error("There is a team with a different organization in the source database that in the destination database.  Number: "
                + num + " source organization: " + sourceOrganization + " dest organization: " + destOrganization);
          }

        } else {
          differencesFound = true;
          LOG.error("There is a team in the source database that isn't in the destination database. Number: "
              + num + " name: " + sourceName);
        }
        SQLFunctions.closeResultSet(destRS);
      }
      SQLFunctions.closeResultSet(destRS);
      SQLFunctions.closeResultSet(sourceRS);
      SQLFunctions.closePreparedStatement(destPrep);
      SQLFunctions.closePreparedStatement(sourcePrep);

      // check that the source database has the same teams in TournamentTeams
      // (for the given tournament) as destination database
      destPrep = destConnection.prepareStatement("SELECT Tournament FROM TournamentTeams WHERE TeamNumber = ?");
      sourcePrep = sourceConnection.prepareStatement("SELECT TeamNumber FROM TournamentTeams WHERE Tournament = ?");
      sourcePrep.setString(1, tournament);
      sourceRS = sourcePrep.executeQuery();
      while (sourceRS.next()) {
        final int teamNumber = sourceRS.getInt(1);
        boolean found = false;
        destPrep.setInt(1, teamNumber);
        destRS = destPrep.executeQuery();
        while (!found
            && destRS.next()) {
          if (tournament.equals(destRS.getString(1))) {
            found = true;
          }
        }
        SQLFunctions.closeResultSet(destRS);
        if (!found) {
          LOG.error("Team "
              + teamNumber + " is in tournament " + tournament + " in the source database, but not in the destination database.");
          differencesFound = true;
        }
      }
      SQLFunctions.closeResultSet(sourceRS);
      SQLFunctions.closePreparedStatement(destPrep);
      SQLFunctions.closePreparedStatement(sourcePrep);

      // check that the destination database has the same teams in
      // TournamentTeams (for the given tournament) as the source database
      sourcePrep = sourceConnection.prepareStatement("SELECT Tournament FROM TournamentTeams WHERE TeamNumber = ?");
      destPrep = destConnection.prepareStatement("SELECT TeamNumber FROM TournamentTeams WHERE Tournament = ?");
      destPrep.setString(1, tournament);
      destRS = destPrep.executeQuery();
      while (destRS.next()) {
        final int teamNumber = destRS.getInt(1);
        boolean found = false;
        sourcePrep.setInt(1, teamNumber);
        sourceRS = sourcePrep.executeQuery();
        while (!found
            && sourceRS.next()) {
          if (tournament.equals(sourceRS.getString(1))) {
            found = true;
          }
        }
        SQLFunctions.closeResultSet(sourceRS);
        if (!found) {
          LOG.error("Team "
              + teamNumber + " is in tournament " + tournament + " in the destination database, but not in the source database.");
          differencesFound = true;
        }
      }
      SQLFunctions.closeResultSet(destRS);
      SQLFunctions.closePreparedStatement(destPrep);
      SQLFunctions.closePreparedStatement(sourcePrep);

      // TODO check documents bug: 2126186

    } finally {
      SQLFunctions.closeResultSet(sourceRS);
      SQLFunctions.closeResultSet(destRS);
      SQLFunctions.closePreparedStatement(sourcePrep);
      SQLFunctions.closePreparedStatement(destPrep);
    }
    return differencesFound;
  }

  private static final SimpleDateFormat CSV_TIMESTAMP_FORMATTER = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");

}
