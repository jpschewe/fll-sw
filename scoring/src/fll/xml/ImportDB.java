/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

import fll.Queries;
import fll.Utilities;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Import scores from a tournament database into a master score database.
 *
 * @version $Revision$
 */
public final class ImportDB {
   
  /**
   * Import a database, both databases are assumed to be on the same machine.
   * The challenge document from the destination (master) database is used to
   * determine the categories and goals.
   *
   * @param args 
   */
  public static void main(final String[] args) {
    try {
      if(args.length != 6) {
        System.err.println("You must specify <hostname> <user> <password> <source database> <tournament> <destination database>");
        System.exit(1);
      } else {
        final ClassLoader classLoader = ChallengeParser.class.getClassLoader();

        final String host = args[0];
        final String user = args[1];
        final String password = args[2];
        final String source = args[3];
        //remove quotes from tournament if they exist
        int substringStart = 0;
        int substringEnd = args[4].length();
        if(args[4].charAt(0) == '"' || args[4].charAt(0) == '\'') {
          substringStart = 1;
        }
        if(args[4].charAt(substringEnd-1) == '"' || args[4].charAt(substringEnd-1) == '\'') {
          substringEnd = substringEnd-1;
        }
        final String tournament = args[4].substring(substringStart, substringEnd);
        final String destination = args[5];

        
        final Connection connection = Utilities.createDBConnection(host, user, password, destination);
        
        final boolean differences = checkForDifferences(connection, source, destination, tournament);
        if(!differences) {
          System.out.println("Importing data for " + tournament + " from " + source + " to " + destination);
          final Document challengeDocument = Queries.getChallengeDocument(connection);
          importDatabase(connection, source, tournament, challengeDocument);
        }
      }
    } catch(final Exception e) {
      e.printStackTrace();
    }
  }
  
  private ImportDB() {
     
  }

  /**
   * Import scores from database for tournament into the database for
   * connection.
   *
   * @param connection a connection to the destination database
   * @param database the name of the source database
   * @param tournament the tournament that the scores are for
   * @param document the XML document that describes the tournament
   */
  public static void importDatabase(final Connection connection,
                                    final String database,
                                    final String tournament,
                                    final Document document)
    throws SQLException {
    Statement stmt = null;
    
    try {
      stmt = connection.createStatement();
      
      final Element rootElement = document.getDocumentElement();
      
      //judges
      stmt.executeUpdate("DELETE FROM Judges WHERE Tournament = '" + tournament + "'");
      stmt.executeUpdate("INSERT INTO Judges (id, category, Tournament) SELECT id,category,Tournament FROM " + database + ".Judges WHERE Tournament = '" + tournament + "'");

      //performance
      {
        final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);
        final String tableName = "Performance";
        stmt.executeUpdate("DELETE FROM " + tableName + " WHERE Tournament = '" + tournament + "'");

        final StringBuffer columns = new StringBuffer();
        columns.append(" TeamNumber,");
        columns.append(" Tournament,");
        columns.append(" RunNumber,");
        columns.append(" TimeStamp,");
        final NodeList goals = performanceElement.getElementsByTagName("goal");
        for(int i=0; i<goals.getLength(); i++) {
          final Element element = (Element)goals.item(i);
          columns.append(" " + element.getAttribute("name") + ",");
        }
        columns.append(" NoShow,");
        columns.append(" Bye");
        
        final String sql = "INSERT INTO " + tableName + " (" + columns.toString() + ") SELECT " + columns.toString() + " FROM " + database + "." + tableName + " WHERE Tournament = '" + tournament + "'";
        stmt.executeUpdate(sql);
      }
      
      //loop over each subjective category
      final NodeList subjectiveCategories = rootElement.getElementsByTagName("subjectiveCategory");
      for(int cat=0; cat<subjectiveCategories.getLength(); cat++) {
        final Element categoryElement = (Element)subjectiveCategories.item(cat);
        final String tableName = categoryElement.getAttribute("name");

        stmt.executeUpdate("DELETE FROM " + tableName + " WHERE Tournament = '" + tournament + "'");
        
        final StringBuffer columns = new StringBuffer();
        
        columns.append(" TeamNumber,");
        columns.append(" Tournament,");
        final NodeList goals = categoryElement.getElementsByTagName("goal");
        for(int i=0; i<goals.getLength(); i++) {
          final Element element = (Element)goals.item(i);
          columns.append(" " + element.getAttribute("name") + ",");
        }
        columns.append(" Judge");

        final String sql = "INSERT INTO " + tableName + " (" + columns.toString() + ") SELECT " + columns.toString() + " FROM " + database + "." + tableName + " WHERE Tournament = '" + tournament + "'";
        stmt.executeUpdate(sql);
      }
      
    } finally {
      Utilities.closeStatement(stmt);
    }
  }

  /**
   * Check for differences between two tournaments in team information.
   *
   * @return true if there are differences
   */
  public static boolean checkForDifferences(final Connection connection,
                                            final String source,
                                            final String destination,
                                            final String tournament)
    throws SQLException {
    System.out.println("Comparing team data from " + source + " against " + destination + " for " + tournament);

    Statement stmt = null;
    ResultSet rs = null;
    boolean retval = false;
    try {
      stmt = connection.createStatement();

      //check name
      String sql = "SELECT " + destination + ".Teams.TeamNumber, " + destination + ".Teams.TeamName, " + source + ".Teams.TeamName"
        + " FROM " + destination + ".Teams, " + source + ".Teams, " + source + ".TournamentTeams"
        + " WHERE " + destination + ".Teams.TeamNumber = " + source + ".Teams.TeamNumber"
        + " AND " + destination + ".Teams.TeamName <> " + source + ".Teams.TeamName"
        + " AND " + source + ".Teams.TeamNumber = " + source + ".TournamentTeams.TeamNumber"
        + " AND " + source + ".TournamentTeams.Tournament = '" + tournament + "'";

      rs = stmt.executeQuery(sql);
      if(rs.next()) {
        retval = true;
        System.out.println("There are teams from tournament " + source + " with different names than the destination database (" + destination + ")");
        do {
          System.out.println(rs.getInt(1) + ": " + rs.getString(2) + " -> " + rs.getString(3));
        } while(rs.next());
        Utilities.closeResultSet(rs);
      }

      //check divisions
      sql = "SELECT " + destination + ".Teams.TeamNumber, " + destination + ".Teams.Division, " + source + ".Teams.Division"
        + " FROM " + destination + ".Teams, " + source + ".Teams, " + source + ".TournamentTeams"
        + " WHERE " + destination + ".Teams.TeamNumber = " + source + ".Teams.TeamNumber"
        + " AND " + destination + ".Teams.Division <> " + source + ".Teams.Division"
        + " AND " + source + ".Teams.TeamNumber = " + source + ".TournamentTeams.TeamNumber"
        + " AND " + source + ".TournamentTeams.Tournament = '" + tournament + "'";

      rs = stmt.executeQuery(sql);
      if(rs.next()) {
        retval = true;
        System.out.println("There are teams from tournament " + source + " with different division than the destination database (" + destination + ")");
        do {
          System.out.println(rs.getInt(1) + ": " + rs.getString(2) + " -> " + rs.getString(3));
        } while(rs.next());
        Utilities.closeResultSet(rs);
      }

      //check tournaments
      sql = "SELECT " + destination + ".Teams.TeamNumber, " + destination + ".TournamentTeams.Tournament, " + source + ".TournamentTeams.Tournament"
        + " FROM " + destination + ".Teams, " + destination + ".TournamentTeams, " + source + ".TournamentTeams"
        + " WHERE " + destination + ".TournamentTeams.TeamNumber = " + source + ".TournamentTeams.TeamNumber"
        + " AND " + destination + ".TournamentTeams.Tournament <> " + source + ".TournamentTeams.Tournament"
        + " AND " + source + ".TournamentTeams.Tournament = '" + tournament + "'"
        + " AND " + destination + ".Teams.TeamNumber = " + source + ".TournamentTeams.TeamNumber";
      
      rs = stmt.executeQuery(sql);
      if(rs.next()) {
        retval = true;
        System.out.println("There are teams from tournament " + source + " with a different tournament than the destination database (" + destination + ")");
        do {
          System.out.println(rs.getInt(1) + ": " + rs.getString(2) + " -> " + rs.getString(3));
        } while(rs.next());
        Utilities.closeResultSet(rs);
      }      
      
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
    }
    return retval;
  }
  
}
