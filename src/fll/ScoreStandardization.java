/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import fll.Queries;
import fll.Team;
import fll.Utilities;

import fll.xml.ChallengeParser;
import java.text.NumberFormat;
import java.text.ParseException;


/**
 * Does score standardization routines from the web.
 *
 * @version $Revision$
 */
final public class ScoreStandardization {

  /**
   * For debugging
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    try {
      final ClassLoader classLoader = ChallengeParser.class.getClassLoader();
      final Document challengeDocument = ChallengeParser.parse(classLoader.getResourceAsStream("resources/challenge.xml"));

      if(null == challengeDocument) {
        throw new RuntimeException("Error parsing challenge.xml");
      }

      final Connection connection = Utilities.createDBConnection("disk");
      final Map teams = Queries.getTournamentTeams(connection);
      Queries.updateScoreTotals(challengeDocument, connection, "STATE");
      setSubjectiveScoreGroups(connection, challengeDocument, "STATE", teams.values());
      summarizeSubjectiveScores(connection, challengeDocument, "STATE");
      summarizePerformanceScores(connection, "STATE");
      standardizeScores(connection, challengeDocument, "STATE");
      updateTeamTotalScores(connection, "STATE");      
      System.out.println("data consistency error: " + checkDataConsistency(connection));
    } catch(final Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    System.exit(0);
  }
  
  private ScoreStandardization() {
     
  }

  /**
   * Create scoregroups for subjective tables based on which judges have
   * scored which teams.  You must run Queries.updateScoreTotals before
   * running this method.
   *
   * @param connection connection to the database
   * @param document the challenge description
   * @param tournament tournament to work with
   * @param teams collection of {@link Team teams} overwhich to generate score
   * groups
   * @see Queries#updateScoreTotals(Document, Connection, String)
   */
  public static void setSubjectiveScoreGroups(final Connection connection,
                                              final Document document,
                                              final String tournament,
                                              final Collection teams) throws SQLException {
    PreparedStatement selectPrep = null;
    PreparedStatement updatePrep = null;
    ResultSet rs = null;
    try {
      final Element rootElement = document.getDocumentElement();
      final NodeList subjectiveCategories = rootElement.getElementsByTagName("subjectiveCategory");
      for(int cat=0; cat < subjectiveCategories.getLength(); cat++) {
        final Element catElement = (Element)subjectiveCategories.item(cat);
        final String catName = catElement.getAttribute("name");
        selectPrep = connection.prepareStatement("SELECT Judge FROM " + catName + " WHERE TeamNumber = ? AND Tournament = '" + tournament + "' AND ComputedTotal IS NOT NULL ORDER BY Judge");
        updatePrep = connection.prepareStatement("UPDATE " + catName + " SET ScoreGroup = ? WHERE TeamNumber = ? AND Tournament = '" + tournament + "'");
        
        final Iterator teamIter = teams.iterator();
        while(teamIter.hasNext()) {
          final Team team = (Team)teamIter.next();
          final StringBuffer scoreGroup = new StringBuffer();
          selectPrep.setInt(1, team.getTeamNumber());
          boolean first = true;
          rs = selectPrep.executeQuery();
          while(rs.next()) {
            final String judge = rs.getString(1);
            if(!first) {
              scoreGroup.append("_");
            } else {
              first = false;
            }
            scoreGroup.append(judge);
          }
          rs.close();

          updatePrep.setString(1, scoreGroup.toString());
          updatePrep.setInt(2, team.getTeamNumber());
          updatePrep.executeUpdate();
        }
      }
      
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closePreparedStatement(selectPrep);
      Utilities.closePreparedStatement(updatePrep);
    }
  }

  /**
   * Summarize the subjective scores for the given tournament
   *
   * @param connection connection to the database with delete and insert
   * privileges
   * @param document the challenge document
   * @param tournament which tournament to summarize scores for
   */
  public static void summarizeSubjectiveScores(final Connection connection,
                                               final Document document,
                                               final String tournament) throws SQLException {
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      
      final Element rootElement = document.getDocumentElement();
      final NodeList subjectiveCategories = rootElement.getElementsByTagName("subjectiveCategory");
      for(int cat=0; cat < subjectiveCategories.getLength(); cat++) {
        final Element catElement = (Element)subjectiveCategories.item(cat);
        final String catName = catElement.getAttribute("name");
    
        //delete all rows matching this tournament and category
        stmt.executeUpdate("DELETE FROM SummarizedScores WHERE Tournament = '" + tournament + "' AND Category = '" + catName + "'");
                           
        //insert rows from the current tournament and category, keeping team
        //number and score group as well as computing the average (across
        //judges)
        stmt.executeUpdate("INSERT INTO SummarizedScores"
                           + " ( TeamNumber, Tournament, ScoreGroup, Category, RawScore )"
                           + " SELECT TeamNumber,"
                           + " Tournament,"
                           + " ScoreGroup, '" + catName + "' AS Category,"
                           + " Avg(ComputedTotal) AS RawScore"
                           + " FROM " + catName
                           + " WHERE Tournament = '" + tournament + "'"
                           + " AND ComputedTotal IS NOT NULL"
                           + " GROUP BY TeamNumber, Tournament, ScoreGroup");
      }
    } finally {
      Utilities.closeStatement(stmt);
    }
  }

  /**
   * Summarize the performance scores for the given tournament
   *
   * @param connection the database connection
   * @param tournament which tournament to summarize scores for
   */
  public static void summarizePerformanceScores(final Connection connection,
                                                final String tournament) throws SQLException {
    Statement stmt = null;
    try {
      stmt = connection.createStatement();

      final int seedingRounds = Queries.getNumSeedingRounds(connection);

      //delete all rows matching this tournament and table
      stmt.executeUpdate("DELETE FROM SummarizedScores WHERE Tournament = '" + tournament + "' AND Category = 'Performance'");
      
      //insert rows for the current tournament
      stmt.executeUpdate("INSERT INTO SummarizedScores"
                         + " ( TeamNumber, Tournament, ScoreGroup, Category, RawScore )"
                         + " SELECT TeamNumber, Tournament, 'Performance' AS ScoreGroup,"
                         + " 'Performance' AS Category, Max(ComputedTotal) AS RawScore"
                         + " FROM Performance"
                         + " WHERE Tournament = '" + tournament + "' AND NoShow = 0"
                         + " AND RunNumber <= " + seedingRounds
                         + " GROUP BY TeamNumber, Tournament");
      
    } finally {
      Utilities.closeStatement(stmt);
    }
  }

  /**
   * Standardize the scores in the SummarizedScores table.
   *
   * @param connection database connection
   * @param document challenge description
   * @param tournament which tournament to standardize scores for
   * @throws SQLException if there is a problem talking to the database
   * @throws ParseException if there is a problem parsing the weights out of
   * the xml document
   */
  public static void standardizeScores(final Connection connection,
                                      final Document document,
                                      final String tournament) throws SQLException, ParseException {
    ResultSet rs = null;
    Statement stmt = null;
    PreparedStatement selectPrep = null;
    PreparedStatement updatePrep = null;
    PreparedStatement weightedPrep = null;
    try {
      stmt = connection.createStatement();

      rs = stmt.executeQuery("SELECT Value From TournamentParameters WHERE Param = 'StandardizedMean'");
      if(!rs.next()) {
        throw new RuntimeException("Can't find StandardizedMean in TournamentParameters");
      }
      final double mean = rs.getDouble(1);
      Utilities.closeResultSet(rs);
      
      rs = stmt.executeQuery("SELECT Value From TournamentParameters WHERE Param = 'StandardizedSigma'");
      if(!rs.next()) {
        throw new RuntimeException("Can't find StandardizedSigma in TournamentParameters");
      }
      final double sigma = rs.getDouble(1);
      Utilities.closeResultSet(rs);

      // 1 - category
      selectPrep = connection.prepareStatement("SELECT ScoreGroup,"
                                               + " Avg(RawScore) AS sg_mean,"
                                               + " Count(RawScore) AS sg_count,"
                                               + " STD(RawScore) AS sg_stdev"
                                               + " FROM SummarizedScores"
                                               + " WHERE Tournament = '" + tournament + "'"
                                               + " AND Category = ?"
                                               + " GROUP BY ScoreGroup");
      /*
        Update StandardizedScore for each team in the ScoreGroup
        formula: SS = Std_Mean + (Z-Score-of-ComputedTotal * Std_Sigma)

                             ( (  (ComputedTotal - ScoreGroup_Mean)  )
        SS = Standard_Mean + ( (  ---------------------------------  ) * Standard_StandardDeviation )
                             ( (     ScoreGroup_StandardDeviation    )


      


      */
      // 1 - sg_mean
      // 2 - sg_stdev
      // 3 - scoreGroup
      // 4 - category
      updatePrep = connection.prepareStatement("UPDATE SummarizedScores"
                                               + " SET StandardizedScore ="
                                               + " ((RawScore - ?) * (" + sigma + " / ?)"
                                               + " ) + " + mean
                                               + " WHERE ScoreGroup = ?"
                                               + " AND Tournament = '" + tournament + "'"
                                               + " AND Category = ?");

      // computed weighted versions of the scores
      // 1 - weight
      // 2 - category
      weightedPrep = connection.prepareStatement("UPDATE SummarizedScores"
                                               + " SET WeightedScore ="
                                               + " ? * StandardizedScore"
                                               + " WHERE Tournament = '" + tournament + "'"
                                               + " AND Category = ?");
      final Element rootElement = document.getDocumentElement();
      final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);
      final double performanceWeight = NumberFormat.getInstance().parse(performanceElement.getAttribute("weight")).doubleValue();
        
      //performance
      selectPrep.setString(1, "Performance");
      updatePrep.setString(4, "Performance");
      rs = selectPrep.executeQuery();
      while(rs.next()) {
        final String group = rs.getString("ScoreGroup");
        final int sg_count = rs.getInt("sg_count");
        if(sg_count > 1) {
          final double sg_mean = rs.getDouble("sg_mean");
          final double sg_stdev = rs.getDouble("sg_stdev");
          updatePrep.setDouble(1, sg_mean);
          updatePrep.setDouble(2, sg_stdev);
          updatePrep.setString(3, group);
          updatePrep.executeUpdate();
        } else {
          throw new RuntimeException("Not enough scores for ScoreGroup: " + group + " in category: Performance");
        }
      }
      Utilities.closeResultSet(rs);

      weightedPrep.setDouble(1, performanceWeight);
      weightedPrep.setString(2, "Performance");
      weightedPrep.executeUpdate();

      
      //subjective categories
      final NodeList subjectiveCategories = rootElement.getElementsByTagName("subjectiveCategory");
      for(int cat=0; cat < subjectiveCategories.getLength(); cat++) {
        final Element catElement = (Element)subjectiveCategories.item(cat);
        final String catName = catElement.getAttribute("name");
        final double catWeight = NumberFormat.getInstance().parse(catElement.getAttribute("weight")).doubleValue();
        
        selectPrep.setString(1, catName);
        updatePrep.setString(4, catName);
        rs = selectPrep.executeQuery();
        while(rs.next()) {
          final String group = rs.getString("ScoreGroup");
          final int sg_count = rs.getInt("sg_count");
          if(sg_count > 1) {
            final double sg_mean = rs.getDouble("sg_mean");
            final double sg_stdev = rs.getDouble("sg_stdev");
            updatePrep.setDouble(1, sg_mean);
            updatePrep.setDouble(2, sg_stdev);
            updatePrep.setString(3, group);
            updatePrep.executeUpdate();
          } else {
            throw new RuntimeException("Not enough scores for ScoreGroup: " + group + " in category: " + catName);
          }
        }
        Utilities.closeResultSet(rs);

        weightedPrep.setDouble(1, catWeight);
        weightedPrep.setString(2, catName);
        weightedPrep.executeUpdate();
        
      }
      
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
      Utilities.closePreparedStatement(selectPrep);
      Utilities.closePreparedStatement(updatePrep);
      Utilities.closePreparedStatement(weightedPrep);
    }
  }

  /**
   * Updates FinalScores with the sum of the SummarizedScores.WeightedScore
   * columns for the given tournament
   *
   * @param connection database connection
   * @param tournament the tournament to add scores for
   * @throws SQLException on an error talking to the database
   */
  public static void updateTeamTotalScores(final Connection connection,
                                           final String tournament) throws SQLException {
    Statement stmt = null;
    try {
      stmt = connection.createStatement();

      //delete old overall scores
      stmt.executeUpdate("DELETE FROM FinalScores"
                         + " WHERE Tournament = '" + tournament + "'");

      stmt.executeUpdate("INSERT INTO FinalScores (TeamNumber,Tournament,OverallScore)"
                         + " SELECT TeamNumber,'" + tournament + "' AS Tournament,Sum(WeightedScore) AS OverallScore FROM SummarizedScores"
                         + " GROUP BY TeamNumber");
      
    } finally {
      Utilities.closeStatement(stmt);
    }
  }
  
  /**
   * Do a simple check of the summarized score data consistency
   *
   * @param connection connection to the database
   * @return null if the data is consistent, otherwise an error message
   * @throws SQLException on an error talking to the database
   */
  public static String checkDataConsistency(final Connection connection) throws SQLException {
    Statement stmt = null;
    ResultSet rs = null;
    ResultSet rs2 = null;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT TeamNumber, Tournament, Category, Count(Category) AS CountOfRecords FROM SummarizedScores GROUP BY TeamNumber, Tournament, Category HAVING Count(Category) <> 1");
      if(rs.next()) {
        final StringBuffer errorStr = new StringBuffer();
        errorStr.append("Summarized Data appears to be inconsistent: " + System.getProperty("line.separator"));
        //FIX need some better error reporting here.  See the Access VB code.
        //I'm not sure the best way to select from a ResultSet...
        
        
        return errorStr.toString();
      } else {
        return null;
      }
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeResultSet(rs2);
      Utilities.closeStatement(stmt);
    }
  }

}
