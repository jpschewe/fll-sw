/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

import fll.Utilities;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Import scores from a tournament database into a master score database.
 *
 * @version $Revision$
 */
public class ImportDB {
   
  /**
   * Import a database
   *
   * @param args 0 -> host, 1 -> user, 2 -> password
   */
  public static void main(final String[] args) {
    try {
      if(args.length != 6) {
        System.err.println("You must specify <hostname> <user> <password> <source database> <tournament> <destination database>");
        System.exit(1);
      } else {
        final ClassLoader classLoader = ChallengeParser.class.getClassLoader();
        final Document challengeDocument = ChallengeParser.parse(classLoader.getResourceAsStream("resources/challenge.xml"));

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

        
        System.out.println("Importing data for " + tournament + " from " + source + " to " + destination);
        final Connection connection = Utilities.createDBConnection(host, user, password, destination);
        importDatabase(connection, source, tournament, challengeDocument);
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
      //FIX assumes tournament column is missing in source database
      stmt.executeUpdate("DELETE FROM Judges WHERE Tournament = '" + tournament + "'");
      stmt.executeUpdate("INSERT INTO Judges (id, category, Tournament) SELECT id,category,'" + tournament + "' FROM " + database + ".Judges");

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
        columns.append(" NoShow,");
        columns.append(" Bye,");
        final NodeList goals = performanceElement.getElementsByTagName("goal");
        for(int i=0; i<goals.getLength(); i++) {
          final Element element = (Element)goals.item(i);
          columns.append(" " + element.getAttribute("name") + ",");
        }
        columns.append(" Verified,");
        columns.append(" Corrected ");

        stmt.executeUpdate("INSERT INTO " + tableName + " (" + columns.toString() + ") SELECT " + columns.toString() + " FROM " + database + "." + tableName + " WHERE Tournament = '" + tournament + "'");
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
        
        stmt.executeUpdate("INSERT INTO " + tableName + " (" + columns.toString() + ") SELECT " + columns.toString() + " FROM " + database + "." + tableName + " WHERE Tournament = '" + tournament + "'");
      }
      
    } finally {
      Utilities.closeStatement(stmt);
    }
  }  
}
