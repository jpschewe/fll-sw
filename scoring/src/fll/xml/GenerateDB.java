/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

import fll.Queries;
import fll.Utilities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Generate tables for fll tournament from XML document
 *
 * @version $Revision$
 */
final public class GenerateDB {
   
  /**
   * Generate a new database
   *
   * @param args 0 -> host, 1 -> root user, 2 -> root password
   */
  public static void main(final String[] args) {
    try {
      if(args.length != 3) {
        System.err.println("You must specify <hostname> <root user> <root password>");
        System.exit(1);
      } else {
        final ClassLoader classLoader = ChallengeParser.class.getClassLoader();
        final Document challengeDocument = ChallengeParser.parse(classLoader.getResourceAsStream("resources/challenge.xml"));
        generateDB(challengeDocument, args[0], args[1], args[2], "fll");

        final Connection connection = Utilities.createDBConnection(args[0], "fll", "fll", "fll");
        final Document document = Queries.getChallengeDocument(connection);
        System.out.println("Title: " + document.getDocumentElement().getAttribute("title"));
        connection.close();
      }
    } catch(final Exception e) {
      e.printStackTrace();
    }
  }
  
  private GenerateDB() {
     
  }

  /**
   * Generate a completly new DB from document.  This also stores the document
   * in the database for later use.
   *
   * @param document and XML document that describes a tournament
   * @param host database host
   * @param user root user that has full control
   * @param password password of root user
   * @param database name for the database to generate 
   */
  public static void generateDB(final Document document,
                                final String host,
                                final String user,
                                final String password,
                                final String database)
    throws SQLException, UnsupportedEncodingException {

    Connection connection = null;
    Statement stmt = null;
    try {
      //first create the database
      connection = Utilities.createDBConnection(host, user, password, "mysql");
      
      stmt = connection.createStatement();

      //delete the old database
      stmt.executeUpdate("DROP DATABASE IF EXISTS " + database);
      
      //create the database
      stmt.executeUpdate("CREATE DATABASE " + database);

      //give fll_admin full privileges and use password fll_admin
      stmt.execute("GRANT ALL PRIVILEGES ON " + database + ".* TO fll_admin IDENTIFIED BY 'fll_admin'");
      stmt.execute("GRANT ALL PRIVILEGES ON " + database + ".* TO fll_admin@localhost IDENTIFIED BY 'fll_admin'");
      stmt.executeUpdate("GRANT ALL PRIVILEGES ON " + database + ".* TO fll IDENTIFIED BY 'fll'");
      stmt.executeUpdate("GRANT ALL PRIVILEGES ON " + database + ".* TO fll@localhost IDENTIFIED BY 'fll'");

    } finally {
      Utilities.closeStatement(stmt);
      Utilities.closeConnection(connection);
    }

    PreparedStatement prep = null;
    try {
      connection = Utilities.createDBConnection(host, "fll_admin", "fll_admin", database);
      
      stmt = connection.createStatement();
      
      //Table structure for table 'Regions'
      stmt.executeUpdate("DROP TABLE IF EXISTS Regions");
      stmt.executeUpdate("CREATE TABLE Regions ("
                         + "Region varchar(16) NOT NULL,"
                         + "Location mediumtext,"
                         + "Date datetime default NULL,"
                         + "Contact varchar(50) default NULL,"
                         + "Phone varchar(20) default NULL,"
                         + "Description mediumtext,"
                         + "Directions mediumtext,"
                         + "PRIMARY KEY (Region)"
                         +")");
      stmt.executeUpdate("INSERT INTO Regions (Region, Description) VALUES ('DUMMY', 'Default dummy tournament')");
      stmt.executeUpdate("INSERT INTO Regions (Region, Description) VALUES ('DROP', 'Dummy tournament for teams that drop out')");

      
      //Table structure for table 'SummarizedScores'
      stmt.executeUpdate("DROP TABLE IF EXISTS SummarizedScores");
      stmt.executeUpdate("CREATE TABLE SummarizedScores ("
                         + "TeamNumber int(11) NOT NULL,"
                         + "Tournament varchar(64) NOT NULL,"
                         + "Category varchar(32) NOT NULL,"
                         + "RawScore float default NULL,"
                         + "StandardizedScore float default NULL,"
                         + "ScoreGroup text default NULL,"
                         + "WeightedScore float default NULL,"
                         + "PRIMARY KEY (TeamNumber, Tournament, Category)"
                         + ")");


      // Table for final scores
      stmt.executeUpdate("DROP TABLE IF EXISTS FinalScores");
      stmt.executeUpdate("CREATE TABLE FinalScores ("
                         + "  TeamNumber integer NOT NULL,"
                         + "  Tournament varchar(64) NOT NULL,"
                         + "  OverallScore float,"
                         + "  PRIMARY KEY (TeamNumber, Tournament)"
                         + ")");

      // Table structure for table 'Teams'
      stmt.executeUpdate("DROP TABLE IF EXISTS Teams");
      stmt.executeUpdate("CREATE TABLE Teams ("
                         + "  TeamNumber int(11) NOT NULL,"
                         + "  TeamName varchar(255) NOT NULL default '',"
                         + "  Organization varchar(255),"
                         + "  Region varchar(64) NOT NULL default 'DUMMY',"
                         + "  Division int NOT NULL default 1,"
                         + "  Coach varchar(255),"
                         + "  Email varchar(255),"
                         + "  Phone varchar(50),"
                         + "  City varchar(255),"
                         + "  NumBoys INT,"
                         + "  NumGirls INT,"
                         + "  NumMedals INT,"
                         + "  HowFoundOut text,"
                         + "  ToState BOOL NOT NULL,"
                         + "  PRIMARY KEY  (TeamNumber)"
                         + ")");

      // table to hold team numbers of teams in this tournament
      stmt.executeUpdate("DROP TABLE IF EXISTS TournamentTeams");
      stmt.executeUpdate("CREATE TABLE TournamentTeams ("
                         + "  TeamNumber int(11) NOT NULL,"
                         + "  Region varchar(64) NOT NULL default 'DUMMY',"
                         + "  PRIMARY KEY (TeamNumber)"
                         + ")");

      // Table structure for table 'TournamentParameters'
      stmt.executeUpdate("DROP TABLE IF EXISTS TournamentParameters");
      stmt.executeUpdate("CREATE TABLE TournamentParameters ("
                         + "  Param varchar(64) NOT NULL,"
                         + "  Value TEXT NOT NULL,"
                         + "  Description varchar(255) default NULL,"
                         + "  PRIMARY KEY  (Param)"
                         + ")");

      //populate tournament parameters with default values
      stmt.executeUpdate("INSERT INTO TournamentParameters (Param, Value, Description) VALUES ('Region', 'DUMMY', 'This is the currently running tournament name - see Regions table')");
      stmt.executeUpdate("INSERT INTO TournamentParameters (Param, Value, Description) VALUES ('SeedingRounds', 3, 'Number of seeding rounds before elimination round - used to downselect top performance scores in queries')");
      stmt.executeUpdate("INSERT INTO TournamentParameters (Param, Value, Description) VALUES ('StandardizedMean', 100, 'Standard mean for computing the standardized scores')");
      stmt.executeUpdate("INSERT INTO TournamentParameters (Param, Value, Description) VALUES ('StandardizedSigma', 20, 'Standard deviation for computing the standardized scores')");

      prep = connection.prepareStatement("INSERT INTO TournamentParameters (Param, Value, Description) VALUES ('ChallengeDocument', ?, 'The XML document describing the challenge')");
      
      //dump the document into a byte array so we can push it into the database
      final XMLWriter xmlwriter = new XMLWriter();
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      xmlwriter.setOutput(baos, "UTF8");
      xmlwriter.write(document);
      final byte[] bytes = baos.toByteArray();
      final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      prep.setAsciiStream(1, bais, bytes.length);
      prep.executeUpdate();
      
      // Table structure for table 'Judges'
      stmt.executeUpdate("DROP TABLE IF EXISTS Judges");
      stmt.executeUpdate("CREATE TABLE Judges ("
                         + "  id varchar(64) NOT NULL,"
                         + "  category varchar(64) NOT NULL,"
                         + "  Tournament varchar(64) NOT NULL,"
                         + "  Division varchar(32) NOT NULL,"
                         + "  PRIMARY KEY  (id,category,Tournament,Division)"
                         + ")");
      
      final Element rootElement = document.getDocumentElement();
      
      final StringBuffer createStatement = new StringBuffer();
      
      //performance
      {
        final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);
        final String tableName = "Performance";
        stmt.executeUpdate("DROP TABLE IF EXISTS " + tableName);
        createStatement.append("CREATE TABLE " + tableName + " (");
        createStatement.append(" TeamNumber INTEGER NOT NULL,");
        createStatement.append(" Tournament varchar(64) NOT NULL,");
        createStatement.append(" RunNumber INTEGER NOT NULL,");
        createStatement.append(" TimeStamp TIMESTAMP NOT NULL,");
        createStatement.append(" NoShow INTEGER DEFAULT 0 NOT NULL,");
        createStatement.append(" Bye INTEGER DEFAULT 0 NOT NULL,");
        final NodeList goals = performanceElement.getElementsByTagName("goal");
        for(int i=0; i<goals.getLength(); i++) {
          final Element element = (Element)goals.item(i);
          final String columnDefinition = generateGoalColumnDefinition(element);
          createStatement.append(" " + columnDefinition + ",");
        }
        createStatement.append(" Verified INTEGER DEFAULT 0 NOT NULL,");
        createStatement.append(" Corrected INTEGER DEFAULT 0 NOT NULL,");
        createStatement.append(" ComputedTotal INTEGER DEFAULT NULL,");
        createStatement.append(" PRIMARY KEY (TeamNumber, Tournament, RunNumber)");
        createStatement.append(");");
        stmt.executeUpdate(createStatement.toString());
      }
      
      //loop over each subjective category
      final NodeList subjectiveCategories = rootElement.getElementsByTagName("subjectiveCategory");
      for(int cat=0; cat<subjectiveCategories.getLength(); cat++) {
        createStatement.setLength(0);

        final Element categoryElement = (Element)subjectiveCategories.item(cat);
        final String tableName = categoryElement.getAttribute("name");

        stmt.executeUpdate("DROP TABLE IF EXISTS " + tableName);
        createStatement.append("CREATE TABLE " + tableName + " (");
        createStatement.append(" TeamNumber INTEGER NOT NULL,");
        createStatement.append(" Tournament varchar(64) NOT NULL,");
        createStatement.append(" Judge VARCHAR(64) NOT NULL,");
        final NodeList goals = categoryElement.getElementsByTagName("goal");
        for(int i=0; i<goals.getLength(); i++) {
          final Element element = (Element)goals.item(i);
          final String columnDefinition = generateGoalColumnDefinition(element);
          createStatement.append(" " + columnDefinition + ",");
        }
        createStatement.append(" ComputedTotal INTEGER DEFAULT NULL,");
        createStatement.append(" ScoreGroup TEXT DEFAULT NULL,");
        createStatement.append(" PRIMARY KEY (TeamNumber, Tournament, Judge)");
        createStatement.append(");");
        stmt.executeUpdate(createStatement.toString());
      }
      
    } finally {
      Utilities.closeStatement(stmt);
      Utilities.closePreparedStatement(prep);
      Utilities.closeConnection(connection);
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

    //check if there are any subelements to determine if this goal is enumerated or not

    String definition = goalName;
    if(goalElement.hasChildNodes()) {
      //enumerated
      definition += " ENUM(";

      boolean first = true;
      Node posValue = goalElement.getFirstChild();
      while(null != posValue) {
        if("value".equals(posValue.getNodeName())) {
          if(first) {
            first = false;
          } else {
            definition += ", ";
          }
          
          final String enumValue = posValue.getFirstChild().getNodeValue();
          definition += "'" + enumValue + "'";
        }
        posValue = posValue.getNextSibling();
      }
      definition += ")";
    } else {
      definition += " INTEGER";
    }
    return definition;
  }

}
