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
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.db.GlobalParameters;
import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.xml.ChallengeDescription;
import fll.xml.PerformanceScoreCategory;
import fll.xml.SubjectiveScoreCategory;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Does score standardization routines from the web.
 */
public final class ScoreStandardization {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Thrown when there are not enough scores for a judging group.
   */
  public static class TooFewScoresException extends FLLRuntimeException {
    public TooFewScoresException(final String message) {
      super(message);
    }
  }

  private ScoreStandardization() {
    // no instances
  }

  private static void summarizePerformanceScores(final Connection connection,
                                                 final int tournament)
      throws SQLException, TooFewScoresException {
    final double mean = GlobalParameters.getStandardizedMean(connection);
    final double sigma = GlobalParameters.getStandardizedSigma(connection);

    try (PreparedStatement getParams = connection.prepareStatement("SELECT " //
        + " Avg(Score) AS sg_mean," //
        + " Count(Score) AS sg_count," //
        + " stddev_pop(Score) AS sg_stdev" //
        + " FROM performance_seeding_max")) {
      try (ResultSet params = getParams.executeQuery()) {
        if (params.next()) {
          final int sgCount = params.getInt(2);
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("sgCount: "
                + sgCount);
          }

          if (0 == sgCount) {
            // nothing to do
            LOGGER.error("sgCount is 0, cannot summarize scores");
            return;
          } else if (sgCount > 1) {
            final double sgMean = params.getDouble(1);
            final double sgStdev = params.getDouble(3);
            if (LOGGER.isTraceEnabled()) {
              LOGGER.trace("sgMean: "
                  + sgMean
                  + " sgStdev: "
                  + sgStdev);
            }

            try (
                PreparedStatement select = connection.prepareStatement("SELECT TeamNumber, ((Score - ?) * ?) + ? FROM performance_seeding_max");
                PreparedStatement update = connection.prepareStatement("UPDATE FinalScores SET performance = ? WHERE TeamNumber = ? AND Tournament = ?")) {
              update.setInt(3, tournament);

              select.setDouble(1, sgMean);
              select.setDouble(2, sigma
                  / sgStdev);
              select.setDouble(3, mean);

              try (ResultSet perfStdScores = select.executeQuery()) {
                while (perfStdScores.next()) {
                  final int teamNumber = perfStdScores.getInt(1);
                  final double stdScore = perfStdScores.getDouble(2);
                  if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Team: "
                        + teamNumber
                        + " stdScore: "
                        + stdScore);
                  }

                  update.setDouble(1, stdScore);
                  update.setInt(2, teamNumber);
                  update.executeUpdate();
                } // foreach team's score
              } // select team scores
            } // allocate statements
          } else {
            throw new TooFewScoresException("Not enough scores for in category: Performance");
          } // sgCount
        } else {
          throw new TooFewScoresException("No performance scores for standardization");
        } // params check
      } // select params
    } // allocate params stmt
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Can't use variable param for column to set")
  private static void summarizeSubjectiveScores(final Connection connection,
                                                final int tournament,
                                                final String catName)
      throws SQLException {
    // insert rows from the current tournament and category, keeping team
    // number and score group as well as computing the average (across
    // judges)
    try (PreparedStatement updatePrep = connection.prepareStatement("UPDATE FinalScores" //
        + " SET "
        + catName
        + " = " //
        + " ( SELECT " //
        + "   Avg(StandardizedScore)" //
        + "   FROM "
        + catName //
        + "   WHERE StandardizedScore IS NOT NULL" //
        + "   AND FinalScores.TeamNumber = "
        + catName
        + ".TeamNumber" //
        + "   AND FinalScores.Tournament = "
        + catName
        + ".Tournament" //
        + "   GROUP BY TeamNumber )" //
        + " WHERE Tournament = ?")) {
      updatePrep.setInt(1, tournament);
      updatePrep.executeUpdate();
    }
  }

  /**
   * Summarize the scores for the given tournament. This puts the standardized
   * scores in the FinalScores table to be weighted and then summed.
   * 
   * @param connection connection to the database with delete and insert
   *          privileges
   * @param tournament which tournament to summarize scores for
   * @throws TooFewScoresException if there aren't enough scores in a judging
   *           group
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Can't use variable param for column to set")
  public static void summarizeScores(final Connection connection,
                                     final ChallengeDescription description,
                                     final int tournament)
      throws SQLException, ParseException, TooFewScoresException {
    if (tournament != Queries.getCurrentTournament(connection)) {
      throw new FLLRuntimeException("Cannot compute summarized scores for a tournament other than the current tournament");
    }

    // delete all rows matching this tournament
    try (PreparedStatement deletePrep = connection.prepareStatement("DELETE FROM FinalScores WHERE Tournament = ?")) {
      deletePrep.setInt(1, tournament);
      deletePrep.executeUpdate();
    }

    // insert an empty row for each team
    try (
        PreparedStatement insertTeams = connection.prepareStatement("INSERT INTO FinalScores (TeamNumber, Tournament) SELECT teamnumber, tournament FROM tournamentteams WHERE tournament = ?")) {
      insertTeams.setInt(1, tournament);
      insertTeams.executeUpdate();
    }

    summarizePerformanceScores(connection, tournament);

    for (final SubjectiveScoreCategory catElement : description.getSubjectiveCategories()) {
      final String catName = catElement.getName();
      summarizeSubjectiveScores(connection, tournament, catName);
    }

  }

  /**
   * Populate the StandardizedScore column of each subjective table.
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Can't use variable for column name in update")
  public static void standardizeSubjectiveScores(final Connection connection,
                                                 final ChallengeDescription description,
                                                 final int tournament)
      throws SQLException {
    ResultSet rs = null;
    PreparedStatement updatePrep = null;
    PreparedStatement selectPrep = null;
    try {
      final double mean = GlobalParameters.getStandardizedMean(connection);
      final double sigma = GlobalParameters.getStandardizedSigma(connection);

      // subjective categories
      for (final SubjectiveScoreCategory catElement : description.getSubjectiveCategories()) {
        final String category = catElement.getName();

        /*
         * Update StandardizedScore for each team in the ScoreGroup formula:
         * 
         * SS = ( ( ComputedTotal - sgMean ) * ( sigma / sgStdev ) ) + mean
         * 
         * sgMean = average(all scores from judge)
         * sgStdev = stdev(all scores from judge)
         */
        // 1 - sg_mean
        // 2 - sigma / stStdev
        // 3 - mean
        // 4 - judge
        // 5 - tournament
        updatePrep = connection.prepareStatement("UPDATE "
            + category
            + " SET StandardizedScore = ((ComputedTotal - ?) * ? ) + ?  WHERE Judge = ?"
            + " AND Tournament = ?");
        updatePrep.setDouble(3, mean);
        updatePrep.setInt(5, tournament);

        selectPrep = connection.prepareStatement("SELECT Judge," //
            + " Avg(ComputedTotal) AS sg_mean," //
            + " Count(ComputedTotal) AS sg_count," //
            + " stddev_pop(ComputedTotal) AS sg_stdev" //
            + " FROM "
            + category //
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
            throw new TooFewScoresException("Not enough scores for Judge: "
                + judge
                + " in category: "
                + category);
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
   * @param tournament the tournament to add scores for
   * @throws SQLException on an error talking to the database
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Can't use variable for column name in select")
  public static void updateTeamTotalScores(final Connection connection,
                                           final ChallengeDescription description,
                                           final int tournament)
      throws SQLException, ParseException {
    final Map<Integer, TournamentTeam> tournamentTeams = Queries.getTournamentTeams(connection, tournament);

    final Tournament currentTournament = Tournament.findTournamentByID(connection, tournament);

    PreparedStatement update = null;
    ResultSet rs = null;
    PreparedStatement perfSelect = null;
    final Collection<PreparedStatement> subjectiveSelects = new LinkedList<PreparedStatement>();
    try {
      for (final SubjectiveScoreCategory catElement : description.getSubjectiveCategories()) {
        final String catName = catElement.getName();
        final double catWeight = catElement.getWeight();
        final PreparedStatement prep = connection.prepareStatement("SELECT "
            + catName
            + " * "
            + catWeight
            + " FROM FinalScores WHERE Tournament = ? AND TeamNumber = ?");
        prep.setInt(1, tournament);
        subjectiveSelects.add(prep);
      }

      final PerformanceScoreCategory performanceElement = description.getPerformance();
      final double performanceWeight = performanceElement.getWeight();
      perfSelect = connection.prepareStatement("SELECT performance * "
          + performanceWeight
          + " FROM FinalScores WHERE Tournament = ? AND TeamNumber = ?");
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

      currentTournament.recordScoreSummariesUpdated(connection);

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
    // TODO issue:64 need some better error reporting here. See the Access VB
    // code.
    // I'm not sure the best way to select from a ResultSet...
    return null;
  }

}
