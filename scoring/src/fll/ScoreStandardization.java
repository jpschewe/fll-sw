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

import java.text.NumberFormat;
import java.text.ParseException;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * Does score standardization routines from the web.
 *
 * @version $Revision$
 */
public final class ScoreStandardization {

  /**
   * For debugging
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    try {
      final String tournament = "state";
      final Connection connection = Utilities.createDBConnection("fll", "fll", "tomcat/webapps/fll-sw/WEB-INF/flldb");
      final Document challengeDocument = Queries.getChallengeDocument(connection);
      Queries.updateScoreTotals(challengeDocument, connection);
      
      standardizeSubjectiveScores(connection, challengeDocument, tournament);
      summarizeScores(connection, challengeDocument, tournament);
      
      updateTeamTotalScores(connection, challengeDocument, tournament);
      final String error = checkDataConsistency(connection);
      if(null != error) {
        System.out.println("data consistency error: " + error);
      }
    } catch(final Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    System.exit(0);
  }
  
  private ScoreStandardization() {
    // no instances
  }

  /**
   * Summarize the scores for the given tournament.  This puts the
   * standardized scores in the FinalScores table to be weighted and then
   * summed.
   *
   * @param connection connection to the database with delete and insert
   * privileges
   * @param document the challenge document
   * @param tournament which tournament to summarize scores for
   */
  public static void summarizeScores(final Connection connection,
                                     final Document document,
                                     final String tournament)
    throws SQLException, ParseException {
    Statement stmt = null;
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      stmt = connection.createStatement();

      //delete all rows matching this tournament
      stmt.executeUpdate("DELETE FROM FinalScores WHERE Tournament = '" + tournament + "'");

      // performance
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
      
      prep = connection.prepareStatement("INSERT INTO FinalScores "
                                         + " ( TeamNumber, Tournament, performance ) "
                                         + " SELECT TeamNumber"
                                         + ", Tournament"
                                         + ", ((Score - ?) * (" + sigma + " / ?)"
                                         + " ) + " + mean
                                         + " FROM performance_seeding_max"
                                         + " WHERE Tournament = '" + tournament + "'");
      rs = stmt.executeQuery("SELECT "
                             + " Avg(Score) AS sg_mean,"
                             + " Count(Score) AS sg_count,"
                             + " stddev_pop(Score) AS sg_stdev"
                             + " FROM performance_seeding_max"
                             + " WHERE Tournament = '" + tournament + "'"
                             );
      if(rs.next()) {
        final int sgCount = rs.getInt(2);
        if(sgCount > 1) {
          final double sgMean = rs.getDouble(2);
          final double sgStdev = rs.getDouble(3);
          
          prep.setDouble(1, sgMean);
          prep.setDouble(2, sgStdev);
          prep.executeUpdate();
        } else {
          throw new RuntimeException("Not enough scores for in category: Performance");
        }
      
      
      // subjective
      final Element rootElement = document.getDocumentElement();
      final NodeList subjectiveCategories = rootElement.getElementsByTagName("subjectiveCategory");
      for(int cat=0; cat < subjectiveCategories.getLength(); cat++) {
        final Element catElement = (Element)subjectiveCategories.item(cat);
        final String catName = catElement.getAttribute("name");
    
        //insert rows from the current tournament and category, keeping team
        //number and score group as well as computing the average (across
        //judges)
        stmt.executeUpdate("UPDATE FinalScores"
                           + " SET " + catName + " = "
                           + " ( SELECT "
                           + "   Avg(StandardizedScore)"
                           + "   FROM " + catName
                           + "   WHERE Tournament = '" + tournament + "'"
                           + "   AND StandardizedScore IS NOT NULL"
                           + "   AND FinalScores.TeamNumber = " + catName + ".TeamNumber"
                           + "   GROUP BY TeamNumber )");
      }

      } else {
        throw new RuntimeException("No performance scores for standardization");
      }
      Utilities.closeResultSet(rs);
      
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
      Utilities.closePreparedStatement(prep);
    }
  }

  /**
   * Populate the StandardizedScore column of each subjective table.
   */
  public static void standardizeSubjectiveScores(final Connection connection,
                                                 final Document document,
                                                 final String tournament)
    throws SQLException {
    ResultSet rs = null;
    Statement stmt = null;
    PreparedStatement updatePrep = null;
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

      final Element rootElement = document.getDocumentElement();
      
      //subjective categories
      final NodeList subjectiveCategories = rootElement.getElementsByTagName("subjectiveCategory");
      for(int cat=0; cat < subjectiveCategories.getLength(); cat++) {
        final Element catElement = (Element)subjectiveCategories.item(cat);
        final String category = catElement.getAttribute("name");
      
        /*
          Update StandardizedScore for each team in the ScoreGroup
          formula: SS = Std_Mean + (Z-Score-of-ComputedTotal * Std_Sigma)

          ( (  (ComputedTotal - ScoreGroup_Mean)  )
          SS = Standard_Mean + ( (  ---------------------------------  ) * Standard_StandardDeviation )
          ( (     ScoreGroup_StandardDeviation    )


      


        */
        // 1 - sg_mean
        // 2 - sg_stdev
        // 3 - judge
        updatePrep = connection.prepareStatement("UPDATE " + category
                                                 + " SET StandardizedScore ="
                                                 + " ((ComputedTotal - ?) * (" + sigma + " / ?)"
                                                 + " ) + " + mean
                                                 + " WHERE Judge = ?"
                                                 + " AND Tournament = '" + tournament + "'");

        rs = stmt.executeQuery("SELECT Judge,"
                               + " Avg(ComputedTotal) AS sg_mean,"
                               + " Count(ComputedTotal) AS sg_count,"
                               + " stddev_pop(ComputedTotal) AS sg_stdev"
                               + " FROM " + category
                               + " WHERE Tournament = '" + tournament + "'"
                               + " GROUP BY Judge");
        while(rs.next()) {
          final String judge = rs.getString(1);
          final int sgCount = rs.getInt(3);
          if(sgCount > 1) {
            final double sgMean = rs.getDouble(2);
            final double sgStdev = rs.getDouble(4);
            updatePrep.setDouble(1, sgMean);
            updatePrep.setDouble(2, sgStdev);
            updatePrep.setString(3, judge);
            updatePrep.executeUpdate();
          } else {
            throw new RuntimeException("Not enough scores for Judge: " + judge + " in category: " + category);
          }
        }
        Utilities.closeResultSet(rs);

      }
      
    } finally {
      Utilities.closeResultSet(rs);
      Utilities.closeStatement(stmt);
      Utilities.closePreparedStatement(updatePrep);
    }
    
  }

  /**
   * Updates FinalScores with the sum of the the scores times the weights for
   * the given tournament.
   *
   * @param connection database connection
   * @param document the challenge document
   * @param tournament the tournament to add scores for
   * @throws SQLException on an error talking to the database
   */
  public static void updateTeamTotalScores(final Connection connection,
                                           final Document document,
                                           final String tournament)
    throws SQLException, ParseException {
    Statement stmt = null;
    try {
      final StringBuilder sql = new StringBuilder();
      sql.append("UPDATE FinalScores SET OverallScore = ( ");
      
      final Element rootElement = document.getDocumentElement();
      final Element performanceElement = (Element)rootElement.getElementsByTagName("Performance").item(0);
      final double performanceWeight = NumberFormat.getInstance().parse(performanceElement.getAttribute("weight")).doubleValue();
        
      // performance
      sql.append("performance * " + performanceWeight);

      
      // subjective categories
      final NodeList subjectiveCategories = rootElement.getElementsByTagName("subjectiveCategory");
      for(int cat=0; cat < subjectiveCategories.getLength(); cat++) {
        final Element catElement = (Element)subjectiveCategories.item(cat);
        final String catName = catElement.getAttribute("name");
        final double catWeight = NumberFormat.getInstance().parse(catElement.getAttribute("weight")).doubleValue();

        sql.append(" + " + catName + " * " + catWeight);
      }

      sql.append(" ) WHERE Tournament = '" + tournament + "'");
      
      stmt = connection.createStatement();
      
      stmt.executeUpdate(sql.toString());
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
//     Statement stmt = null;
//     ResultSet rs = null;
//     ResultSet rs2 = null;
//     try {
//       stmt = connection.createStatement();
//       rs = stmt.executeQuery("SELECT TeamNumber, Tournament, Category, Count(Category) AS CountOfRecords FROM Scores GROUP BY TeamNumber, Tournament, Category HAVING Count(Category) <> 1");
//       if(rs.next()) {
//         final StringBuffer errorStr = new StringBuffer();
//         errorStr.append("Summarized Data appears to be inconsistent: " + System.getProperty("line.separator"));
        
        
//         return errorStr.toString();
//       } else {
//         return null;
//       }
//     } finally {
//       Utilities.closeResultSet(rs);
//       Utilities.closeResultSet(rs2);
//       Utilities.closeStatement(stmt);
//     }
    //FIX need some better error reporting here.  See the Access VB code.
    //I'm not sure the best way to select from a ResultSet...
    return null;
  }

}
