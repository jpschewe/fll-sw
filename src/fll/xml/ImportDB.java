/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

import fll.Queries;
import fll.Utilities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Import scores from a tournament database into a master score database.
 *
 * <p>Example arguments: jdbc:mysql://localhost/centennial_10?autoReconnect=true&user=root&password=rootpw "Centennial Dec10" jdbc:hsqldb:file:/tmp/flldb;shutdown=true
 *
 * @version $Revision$
 */
public final class ImportDB {

  private static final Logger LOG = Logger.getLogger(ImportDB.class);
  
  public static void main(final String[] args) {
    try {
      if(args.length != 3) {
        System.err.println("You must specify <source uri> <tournament> <destination uri>");
        System.exit(1);
      } else {
        final ClassLoader classLoader = ChallengeParser.class.getClassLoader();

        final String sourceURI = args[0];
        //remove quotes from tournament if they exist
        int substringStart = 0;
        int substringEnd = args[1].length();
        if(args[1].charAt(0) == '"' || args[1].charAt(0) == '\'') {
          substringStart = 1;
        }
        if(args[1].charAt(substringEnd-1) == '"' || args[1].charAt(substringEnd-1) == '\'') {
          substringEnd = substringEnd-1;
        }
        final String tournament = args[1].substring(substringStart, substringEnd);
        final String destinationURI = args[2];

        try {
          Class.forName("org.hsqldb.jdbcDriver").newInstance();
          Class.forName("org.gjt.mm.mysql.Driver").newInstance();
        } catch(final ClassNotFoundException e){
          throw new RuntimeException("Unable to load driver.", e);
        } catch(final InstantiationException ie) {
          throw new RuntimeException("Unable to load driver.", ie);
        } catch(final IllegalAccessException iae) {
          throw new RuntimeException("Unable to load driver.", iae);
        }
        
        final Connection sourceConnection = DriverManager.getConnection(sourceURI);
        final Connection destinationConnection = DriverManager.getConnection(destinationURI);
        Statement stmt1 = null;
        Statement stmt2 = null;
        try {
          try {
            stmt1 = sourceConnection.createStatement();
            stmt1.executeUpdate("SET WRITE_DELAY 1 MILLIS");
          } catch(final SQLException sqle) {
            LOG.info("Source either isn't HSQLDB or there is a problem", sqle);
          }
          try {
            stmt2 = destinationConnection.createStatement();
            stmt2.executeUpdate("SET WRITE_DELAY 1 MILLIS");
          } catch(final SQLException sqle) {
            LOG.info("Destination either isn't HSQLDB or there is a problem", sqle);
          }          
        } finally {
          Utilities.closeStatement(stmt1);
          Utilities.closeStatement(stmt2);
        }

        
        final boolean differences = checkForDifferences(sourceConnection, destinationConnection, tournament);
        if(!differences) {
          System.out.println("Importing data for " + tournament + " from " + sourceURI + " to " + destinationURI);
          final Document challengeDocument = Queries.getChallengeDocument(destinationConnection);
          importDatabase(sourceConnection, destinationConnection, tournament, challengeDocument);
        } else {
          System.out.println("Import aborted due to differences in databases");
        }

        try {
          try {
            stmt1 = sourceConnection.createStatement();
            stmt1.executeUpdate("SHUTDOWN COMPACT");
          } catch(final SQLException sqle) {
            LOG.info("Source either isn't HSQLDB or there is a problem", sqle);
          }
          try {
            stmt2 = destinationConnection.createStatement();
            stmt2.executeUpdate("SHUTDOWN COMPACT");
          } catch(final SQLException sqle) {
            LOG.info("Destination either isn't HSQLDB or there is a problem", sqle);
          }          
        } finally {
          Utilities.closeStatement(stmt1);
          Utilities.closeStatement(stmt2);
        }
        
      }
    } catch(final Exception e) {
      e.printStackTrace();
    }
  }
  
  private ImportDB() {
    // no instances
  }

  /**
   * Import scores from database for tournament into the database for
   * connection.
   *
   * @param sourceConnection a connection to the source database
   * @param destinationConnection a connection to the destination database
   * @param tournament the tournament that the scores are for
   * @param document the XML document that describes the tournament
   */
  public static void importDatabase(final Connection sourceConnection,
                                    final Connection destinationConnection,
                                    final String tournament,
                                    final Document document)
    throws SQLException {

    PreparedStatement destPrep = null;
    PreparedStatement sourcePrep = null;
    ResultSet sourceRS = null;
    try {
      final Element rootElement = document.getDocumentElement();
      
      //judges
      System.out.print("Importing Judges");
      destPrep = destinationConnection.prepareStatement("DELETE FROM Judges WHERE Tournament = ?");
      destPrep.setString(1, tournament);
      destPrep.executeUpdate();
      Utilities.closePreparedStatement(destPrep);

      destPrep = destinationConnection.prepareStatement("INSERT INTO Judges (id, category, Division, Tournament) VALUES (?, ?, ?, ?)");
      destPrep.setString(4, tournament);

      sourcePrep = sourceConnection.prepareStatement("SELECT id, category, Division FROM Judges WHERE Tournament = ?");
      sourcePrep.setString(1, tournament);
      sourceRS = sourcePrep.executeQuery();
      while(sourceRS.next()) {
        destPrep.setString(1, sourceRS.getString(1));
        destPrep.setString(2, sourceRS.getString(2));
        destPrep.setString(3, sourceRS.getString(3));
        destPrep.executeUpdate();
        System.out.print(".");
      }
      Utilities.closeResultSet(sourceRS);
      Utilities.closePreparedStatement(sourcePrep);
      Utilities.closePreparedStatement(destPrep);
      System.out.println();

      //performance
      {
        System.out.print("Importing performance scores");
        final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);
        final String tableName = "Performance";
        destPrep = destinationConnection.prepareStatement("DELETE FROM " + tableName + " WHERE Tournament = ?");
        destPrep.setString(1, tournament);
        destPrep.executeUpdate();
        Utilities.closePreparedStatement(destPrep);
        
        final StringBuffer columns = new StringBuffer();
        columns.append(" TeamNumber,");
        columns.append(" Tournament,");
        columns.append(" RunNumber,");
        columns.append(" TimeStamp,");
        final NodeList goals = performanceElement.getElementsByTagName("goal");
        final int numColumns = goals.getLength() + 6;
        for(int i=0; i<goals.getLength(); i++) {
          final Element element = (Element)goals.item(i);
          columns.append(" " + element.getAttribute("name") + ",");
        }
        columns.append(" NoShow,");
        columns.append(" Bye");

        StringBuffer sql = new StringBuffer();
        sql.append("INSERT INTO ");
        sql.append(tableName);
        sql.append(" (");
        sql.append(columns.toString());
        sql.append(") VALUES (");
        for(int i=0; i<numColumns; i++) {
          if(i > 0) {
            sql.append(", ");
          }
          sql.append("?");
        }
        sql.append(")");
        destPrep = destinationConnection.prepareStatement(sql.toString());

        sourcePrep = sourceConnection.prepareStatement("SELECT " + columns.toString() + " FROM " + tableName + " WHERE Tournament = ?");
        sourcePrep.setString(1, tournament);
        sourceRS = sourcePrep.executeQuery();
        while(sourceRS.next()) {
          for(int i=0; i<numColumns; i++) {
            destPrep.setObject(i+1, sourceRS.getObject(i+1));
          }
          destPrep.executeUpdate();
          System.out.print(".");
        }
        Utilities.closeResultSet(sourceRS);
        Utilities.closePreparedStatement(sourcePrep);
        Utilities.closePreparedStatement(destPrep);
        System.out.println();
      }
      
      //loop over each subjective category
      final NodeList subjectiveCategories = rootElement.getElementsByTagName("subjectiveCategory");
      for(int cat=0; cat<subjectiveCategories.getLength(); cat++) {
        final Element categoryElement = (Element)subjectiveCategories.item(cat);
        final String tableName = categoryElement.getAttribute("name");
        System.out.print("Importing " + tableName);
        
        destPrep = destinationConnection.prepareStatement("DELETE FROM " + tableName + " WHERE Tournament = ?");
        destPrep.setString(1, tournament);
        destPrep.executeUpdate();
        Utilities.closePreparedStatement(destPrep);
        
        final StringBuffer columns = new StringBuffer();
        columns.append(" TeamNumber,");
        columns.append(" Tournament,");
        final NodeList goals = categoryElement.getElementsByTagName("goal");
        final int numColumns = goals.getLength() + 3;
        for(int i=0; i<goals.getLength(); i++) {
          final Element element = (Element)goals.item(i);
          columns.append(" " + element.getAttribute("name") + ",");
        }
        columns.append(" Judge");

        StringBuffer sql = new StringBuffer();
        sql.append("INSERT INTO ");
        sql.append(tableName);
        sql.append(" (");
        sql.append(columns.toString());
        sql.append(") VALUES (");
        for(int i=0; i<numColumns; i++) {
          if(i > 0) {
            sql.append(", ");
          }
          sql.append("?");
        }
        sql.append(")");
        destPrep = destinationConnection.prepareStatement(sql.toString());

        sourcePrep = sourceConnection.prepareStatement("SELECT " + columns.toString() + " FROM " + tableName + " WHERE Tournament = ?");
        sourcePrep.setString(1, tournament);
        sourceRS = sourcePrep.executeQuery();
        while(sourceRS.next()) {
          for(int i=0; i<numColumns; i++) {
            destPrep.setObject(i+1, sourceRS.getObject(i+1));
          }
          destPrep.executeUpdate();
          System.out.print(".");
        }
        Utilities.closeResultSet(sourceRS);
        Utilities.closePreparedStatement(sourcePrep);
        Utilities.closePreparedStatement(destPrep);
        System.out.println();
      }
      
    } finally {
      Utilities.closeResultSet(sourceRS);
      Utilities.closePreparedStatement(sourcePrep);
      Utilities.closePreparedStatement(destPrep);
    }
  }

  /**
   * Check for differences between two tournaments in team information.
   *
   * @return true if there are differences
   */
  public static boolean checkForDifferences(final Connection sourceConnection,
                                            final Connection destinationConnection,
                                            final String tournament)
    throws SQLException {
    PreparedStatement sourcePrep = null;
    PreparedStatement destPrep = null;
    ResultSet sourceRS = null;
    ResultSet destRS = null;
    boolean retval = false;
    try {
      // check names and regions and make sure that each team in the source
      // tournament is in the destination tournament
      destPrep = destinationConnection.prepareStatement("SELECT TeamName, Region FROM Teams WHERE TeamNumber = ?");

      sourcePrep = sourceConnection.prepareStatement("SELECT Teams.TeamNumber, Teams.TeamName, Teams.Region FROM Teams, TournamentTeams WHERE Teams.TeamNumber = TournamentTeams.TeamNumber AND TournamentTeams.Tournament = ?");
      sourcePrep.setString(1, tournament);
      sourceRS = sourcePrep.executeQuery();
      while(sourceRS.next()) {
        final int num = sourceRS.getInt(1);
        final String sourceName = sourceRS.getString(2);
        final String sourceRegion = sourceRS.getString(3);
        destPrep.setInt(1, num);
        destRS = destPrep.executeQuery();
        if(destRS.next()) {
          final String destName = destRS.getString(1);
          if(!Utilities.safeEquals(destName, sourceName)) {
            retval = true;
            System.out.println("There is a team with a different name in the source database that in the destination database.  Number: " + num + " source name: " + sourceName + " dest name: " + destName);
          } else {
            final String destRegion = destRS.getString(2);
            if(!Utilities.safeEquals(destRegion, sourceRegion)) {
              retval = true;
              System.out.println("There is a team with a different region in the source database that in the destination database.  Number: " + num + " source region: " + sourceRegion + " dest region: " + destRegion);
            }
          }
        } else {
          retval = true;
          System.out.println("There is a team in the source database that isn't in the destination database. Number: " + num + " name: " + sourceName);
        }
        Utilities.closeResultSet(destRS);
      }

      // check divisions
      // FIX this doesn't work if divisions don't change, needs to check age group eventually
//       sql = "SELECT " + destination + ".Teams.TeamNumber, " + destination + ".Teams.Division, " + source + ".Teams.Division"
//         + " FROM " + destination + ".Teams, " + source + ".Teams, " + source + ".TournamentTeams"
//         + " WHERE " + destination + ".Teams.TeamNumber = " + source + ".Teams.TeamNumber"
//         + " AND " + destination + ".Teams.Division <> " + source + ".Teams.Division"
//         + " AND " + source + ".Teams.TeamNumber = " + source + ".TournamentTeams.TeamNumber"
//         + " AND " + source + ".TournamentTeams.Tournament = '" + tournament + "'";

//       rs = stmt.executeQuery(sql);
//       if(rs.next()) {
//         retval = true;
//         System.out.println("There are teams from tournament " + source + " with different division than the destination database (" + destination + ")");
//         do {
//           System.out.println(rs.getInt(1) + ": " + rs.getString(2) + " -> " + rs.getString(3));
//         } while(rs.next());
//         Utilities.closeResultSet(rs);
//       }

      //check tournaments
      //FIX this dosn't work with tournament advancement
//       sql = "SELECT " + destination + ".Teams.TeamNumber, " + destination + ".TournamentTeams.Tournament, " + source + ".TournamentTeams.Tournament"
//         + " FROM " + destination + ".Teams, " + destination + ".TournamentTeams, " + source + ".TournamentTeams"
//         + " WHERE " + destination + ".TournamentTeams.TeamNumber = " + source + ".TournamentTeams.TeamNumber"
//         + " AND " + destination + ".TournamentTeams.Tournament <> " + source + ".TournamentTeams.Tournament"
//         + " AND " + source + ".TournamentTeams.Tournament = '" + tournament + "'"
//         + " AND " + destination + ".Teams.TeamNumber = " + source + ".TournamentTeams.TeamNumber";
      
//       rs = stmt.executeQuery(sql);
//       if(rs.next()) {
//         retval = true;
//         System.out.println("There are teams from tournament " + source + " with a different tournament than the destination database (" + destination + ")");
//         do {
//           System.out.println(rs.getInt(1) + ": " + rs.getString(2) + " -> " + rs.getString(3));
//         } while(rs.next());
//         Utilities.closeResultSet(rs);
//       }      

      //FIX check documents
      
    } finally {
      Utilities.closeResultSet(sourceRS);
      Utilities.closeResultSet(destRS);
      Utilities.closePreparedStatement(sourcePrep);
      Utilities.closePreparedStatement(destPrep);
    }
    return retval;
  }
  
}
