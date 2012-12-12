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
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import net.mtu.eggplant.util.sql.SQLFunctions;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.db.GlobalParameters;
import fll.db.Queries;
import fll.util.FLLRuntimeException;

/**
 * Does score standardization routines from the web.
 */
public final class ScoreStandardization {

  private ScoreStandardization() {
    // no instances
  }

  /**
   * Summarize the scores for the given tournament. This puts the standardized
   * scores in the FinalScores table to be weighted and then summed.
   * 
   * @param connection connection to the database with delete and insert
   *          privileges
   * @param document the challenge document
   * @param tournament which tournament to summarize scores for
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Can't use variable param for column to set")
  public static void summarizeScores(final Connection connection,
                                     final Document document,
                                     final int tournament) throws SQLException, ParseException {
    if (tournament != Queries.getCurrentTournament(connection)) {
      throw new FLLRuntimeException(
                                    "Cannot compute summarized scores for a tournament other than the current tournament");
    }

    Statement stmt = null;
    PreparedStatement deletePrep = null;
    PreparedStatement insertPrep = null;
    PreparedStatement selectPrep = null;
    ResultSet rs = null;
    PreparedStatement updatePrep = null;
    try {
      stmt = connection.createStatement();

      // delete all rows matching this tournament
      deletePrep = connection.prepareStatement("DELETE FROM FinalScores WHERE Tournament = ?");
      deletePrep.setInt(1, tournament);
      deletePrep.executeUpdate();

      // performance
      final double mean = getStandardizedMean(connection);
      final double sigma = getStandardizedSigma(connection);

      insertPrep = connection.prepareStatement("INSERT INTO FinalScores "//
          + " ( TeamNumber, Tournament, performance ) " //
          + " SELECT TeamNumber" //
          + ", Tournament" //
          + ", ((Score - ?) * ?) + ? "//
          + " FROM performance_seeding_max" //
          + " WHERE Tournament = ?");
      insertPrep.setDouble(3, mean);
      insertPrep.setInt(4, tournament);

      selectPrep = connection.prepareStatement("SELECT " //
          + " Avg(Score) AS sg_mean," //
          + " Count(Score) AS sg_count," //
          + " stddev_pop(Score) AS sg_stdev" //
          + " FROM performance_seeding_max"//
          + " WHERE Tournament = ?");
      selectPrep.setInt(1, tournament);
      rs = selectPrep.executeQuery();
      if (rs.next()) {
        final int sgCount = rs.getInt(2);
        if (0 == sgCount) {
          // nothing to do
          return;
        } else if (sgCount > 1) {
          final double sgMean = rs.getDouble(1);
          final double sgStdev = rs.getDouble(3);
          insertPrep.setDouble(1, sgMean);
          insertPrep.setDouble(2, sigma
              / sgStdev);
          insertPrep.executeUpdate();
        } else {
          throw new RuntimeException("Not enough scores for in category: Performance");
        }

        // subjective
        final Element rootElement = document.getDocumentElement();
        for (final Element catElement : new NodelistElementCollectionAdapter(
                                                                             rootElement.getElementsByTagName("subjectiveCategory"))) {
          final String catName = catElement.getAttribute("name");

          // insert rows from the current tournament and category, keeping team
          // number and score group as well as computing the average (across
          // judges)
          updatePrep = connection.prepareStatement("UPDATE FinalScores" //
              + " SET " + catName + " = " //
              + " ( SELECT " //
              + "   Avg(StandardizedScore)" //
              + "   FROM " + catName //
              + "   WHERE Tournament = ?" //
              + "   AND StandardizedScore IS NOT NULL" //
              + "   AND FinalScores.TeamNumber = " + catName + ".TeamNumber" //
              + "   GROUP BY TeamNumber )");
          updatePrep.setInt(1, tournament);
          updatePrep.executeUpdate();
        }

      } else {
        throw new RuntimeException("No performance scores for standardization");
      }
      SQLFunctions.close(rs);

    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
      SQLFunctions.close(insertPrep);
      SQLFunctions.close(deletePrep);
      SQLFunctions.close(selectPrep);
    }
  }

  private static double getStandardizedMean(final Connection connection) throws SQLException {
    return GlobalParameters.getDoubleGlobalParameter(connection, GlobalParameters.STANDARDIZED_MEAN);
  }

  private static double getStandardizedSigma(final Connection connection) throws SQLException {
    return GlobalParameters.getDoubleGlobalParameter(connection, GlobalParameters.STANDARDIZED_SIGMA);
  }

  /**
   * Populate the StandardizedScore column of each subjective table.
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Can't use variable for column name in update")
  public static void standardizeSubjectiveScores(final Connection connection,
                                                 final Document document,
                                                 final int tournament) throws SQLException {
    ResultSet rs = null;
    PreparedStatement updatePrep = null;
    PreparedStatement selectPrep = null;
    try {
      final double mean = getStandardizedMean(connection);
      final double sigma = getStandardizedSigma(connection);

      final Element rootElement = document.getDocumentElement();

      // subjective categories
      for (final Element catElement : new NodelistElementCollectionAdapter(
                                                                           rootElement.getElementsByTagName("subjectiveCategory"))) {
        final String category = catElement.getAttribute("name");

        /*
         * Update StandardizedScore for each team in the ScoreGroup formula: SS
         * = Std_Mean + (Z-Score-of-ComputedTotal Std_Sigma) ( ( (ComputedTotal
         * - ScoreGroup_Mean) ) SS = Standard_Mean + ( (
         * --------------------------------- ) Standard_StandardDeviation ) ( (
         * ScoreGroup_StandardDeviation )
         */
        // 1 - sg_mean
        // 2 - sg_stdev
        // 3 - judge
        updatePrep = connection.prepareStatement("UPDATE "
            + category + " SET StandardizedScore = ((ComputedTotal - ?) * ? ) + ?  WHERE Judge = ?"
            + " AND Tournament = ?");
        updatePrep.setDouble(3, mean);
        updatePrep.setInt(5, tournament);

        selectPrep = connection.prepareStatement("SELECT Judge," //
            + " Avg(ComputedTotal) AS sg_mean," //
            + " Count(ComputedTotal) AS sg_count," //
            + " stddev_pop(ComputedTotal) AS sg_stdev" //
            + " FROM " + category //
            + " WHERE Tournament = ?" //
            + " and ComputedTotal IS NOT NULL AND NoShow = false GROUP BY Judge");
        selectPrep.setInt(1, tournament);
        rs = selectPrep.executeQuery();
        while (rs.next()) {
          final String judge = rs.getString(1);
          final int sgCount = rs.getInt(3);
          if (sgCount > 1) {
            final double sgMean = rs.getDouble(2);
            final double sgStdev = rs.getDouble(4);
            updatePrep.setDouble(1, sgMean);
            updatePrep.setDouble(2, sigma
                / sgStdev);
            updatePrep.setString(4, judge);
            updatePrep.executeUpdate();
          } else { // if(sgCount == 1) {
            throw new RuntimeException("Not enough scores for Judge: "
                + judge + " in category: " + category);
          } // ignore 0 in a judging group
        }
        SQLFunctions.close(rs);

      }

    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(selectPrep);
      SQLFunctions.close(updatePrep);
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
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Can't use variable for column name in select")
  public static void updateTeamTotalScores(final Connection connection,
                                           final Document document,
                                           final int tournament) throws SQLException, ParseException {
    final Map<Integer, Team> tournamentTeams = Queries.getTournamentTeams(connection, tournament);

    PreparedStatement update = null;
    ResultSet rs = null;
    PreparedStatement perfSelect = null;
    final Collection<PreparedStatement> subjectiveSelects = new LinkedList<PreparedStatement>();
    try {
      final Element rootElement = document.getDocumentElement();
      for (final Element catElement : new NodelistElementCollectionAdapter(
                                                                           rootElement.getElementsByTagName("subjectiveCategory"))) {
        final String catName = catElement.getAttribute("name");
        final double catWeight = Utilities.NUMBER_FORMAT_INSTANCE.parse(catElement.getAttribute("weight"))
                                                                 .doubleValue();
        final PreparedStatement prep = connection.prepareStatement("SELECT "
            + catName + " * " + catWeight + " FROM FinalScores WHERE Tournament = ? AND TeamNumber = ?");
        prep.setInt(1, tournament);
        subjectiveSelects.add(prep);
      }

      final Element performanceElement = (Element) rootElement.getElementsByTagName("Performance").item(0);
      final double performanceWeight = Utilities.NUMBER_FORMAT_INSTANCE.parse(performanceElement.getAttribute("weight"))
                                                                       .doubleValue();
      perfSelect = connection.prepareStatement("SELECT performance * "
          + performanceWeight + " FROM FinalScores WHERE TOurnament = ? AND TeamNumber = ?");
      perfSelect.setInt(1, tournament);

      update = connection.prepareStatement("UPDATE FinalScores SET OverallScore = ? WHERE Tournament = ? AND TeamNumber = ?");
      update.setInt(2, tournament);

      // compute scores for all teams treating NULL as 0
      for (final int teamNumber : tournamentTeams.keySet()) {
        double overallScore = 0;

        perfSelect.setInt(2, teamNumber);
        rs = perfSelect.executeQuery();
        if (rs.next()) {
          final double score = rs.getDouble(1);
          if (!rs.wasNull()) {
            overallScore += score;
          }
        }
        SQLFunctions.close(rs);
        rs = null;

        for (final PreparedStatement prep : subjectiveSelects) {
          prep.setInt(2, teamNumber);
          rs = prep.executeQuery();
          if (rs.next()) {
            final double value = rs.getDouble(1);
            if (!rs.wasNull()) {
              overallScore += value;
            }
          }
          SQLFunctions.close(rs);
          rs = null;
        }

        update.setDouble(1, overallScore);
        update.setInt(3, teamNumber);
        update.executeUpdate();
      }

    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(update);
      SQLFunctions.close(perfSelect);
      for (final PreparedStatement prep : subjectiveSelects) {
        SQLFunctions.close(prep);
      }
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
    // Statement stmt = null;
    // ResultSet rs = null;
    // ResultSet rs2 = null;
    // try {
    // stmt = connection.createStatement();
    // rs = stmt.executeQuery("SELECT TeamNumber, Tournament, Category,
    // Count(Category) AS CountOfRecords FROM Scores GROUP BY TeamNumber,
    // Tournament, Category HAVING Count(Category) <> 1");
    // if(rs.next()) {
    // final StringBuffer errorStr = new StringBuffer();
    // errorStr.append("Summarized Data appears to be inconsistent: " +
    // System.getProperty("line.separator"));

    // return errorStr.toString();
    // } else {
    // return null;
    // }
    // } finally {
    // SQLFunctions.closeResultSet(rs);
    // SQLFunctions.closeResultSet(rs2);
    // SQLFuctions.closeStatement(stmt);
    // }
    // TODO ticket:84 need some better error reporting here. See the Access VB
    // code.
    // I'm not sure the best way to select from a ResultSet...
    return null;
  }

}
