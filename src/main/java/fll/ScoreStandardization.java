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
import java.util.HashMap;
import java.util.Map;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.db.GlobalParameters;
import fll.db.Queries;
import fll.xml.ChallengeDescription;
import fll.xml.PerformanceScoreCategory;
import fll.xml.ScoreCategory;

/**
 * Does score standardization routines from the web.
 */
public final class ScoreStandardization {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private ScoreStandardization() {
    // no instances
  }

  private static void summarizePerformanceScores(final Connection connection,
                                                 final int tournament)
      throws SQLException {

    final double mean = GlobalParameters.getStandardizedMean(connection);
    final double sigma = GlobalParameters.getStandardizedSigma(connection);

    try (PreparedStatement getParams = connection.prepareStatement("SELECT " //
        + " Avg(Score) AS sg_mean," //
        + " Count(Score) AS sg_count," //
        + " stddev_pop(Score) AS sg_stdev" //
        + " FROM performance_seeding_max" //
        + " WHERE performance_seeding_max.tournament = ?")) {
      getParams.setInt(1, tournament);

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
                PreparedStatement select = connection.prepareStatement("SELECT TeamNumber, ((Score - ?) * ?) + ? FROM performance_seeding_max WHERE performance_seeding_max.tournament = ?");
                PreparedStatement insert = connection.prepareStatement("INSERT INTO final_scores (category, goal_group, tournament, team_number, final_score) VALUES(?, ?, ?, ?, ?)")) {
              insert.setString(1, PerformanceScoreCategory.CATEGORY_NAME);
              insert.setString(2, "");
              insert.setInt(3, tournament);

              select.setDouble(1, sgMean);
              select.setDouble(2, sigma
                  / sgStdev);
              select.setDouble(3, mean);
              select.setInt(4, tournament);

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

                  insert.setInt(4, teamNumber);
                  insert.setDouble(5, stdScore);
                  insert.executeUpdate();
                } // foreach team's score
              } // select team scores
            } // allocate statements
          } else {
            LOGGER.error("Not enough scores for in category: Performance. Tournament: {}", tournament);
          } // sgCount
        } else {
          LOGGER.error("No performances score for standardization. Tournament: {}", tournament);
          return;
        } // params check
      } // select params
    } // allocate params stmt
  }

  private static void summarizeSubjectiveScores(final Connection connection,
                                                final int tournament)
      throws SQLException {
    // insert rows from the current tournament and category, keeping team
    // number and score group as well as computing the average (across
    // judges)
    try (PreparedStatement updatePrep = connection.prepareStatement("INSERT INTO final_scores" //
        + " (category, goal_group, tournament, team_number, final_score)"
        + " ( SELECT " //
        + "   category, goal_group, tournament, team_number, Avg(standardized_score)" //
        + "   FROM subjective_computed_scores"
        + "   WHERE standardized_score IS NOT NULL" //
        + "     AND tournament = ?" //
        + "   GROUP BY category, goal_group, tournament, team_number)" //
    )) {
      updatePrep.setInt(1, tournament);
      updatePrep.executeUpdate();
    }
  }

  /**
   * Summarize the scores for the given tournament. This puts the standardized
   * scores in the final_scores table to be weighted and then summed.
   *
   * @param connection connection to the database with delete and insert
   *          privileges
   * @param tournament which tournament to summarize scores for
   * @throws SQLException a database error
   */
  public static void summarizeScores(final Connection connection,
                                     final int tournament)
      throws SQLException {

    // delete all final scores for the tournament
    try (PreparedStatement deletePrep = connection.prepareStatement("DELETE FROM final_scores WHERE tournament = ?")) {
      deletePrep.setInt(1, tournament);
      deletePrep.executeUpdate();
    }

    // compute all final scores for the tournament
    summarizePerformanceScores(connection, tournament);

    summarizeSubjectiveScores(connection, tournament);
  }

  /**
   * Populate the standardized_score column of subjective_computed_scores.
   * 
   * @param connection database connection
   * @param tournament ID of tournament to work on
   * @throws SQLException on a database error
   */
  public static void standardizeSubjectiveScores(final Connection connection,
                                                 final int tournament)
      throws SQLException {
    final double mean = GlobalParameters.getStandardizedMean(connection);
    final double sigma = GlobalParameters.getStandardizedSigma(connection);

    try (PreparedStatement selectPrep = connection.prepareStatement("SELECT category, goal_group, judge," //
        + " Avg(computed_total) AS sg_mean," //
        + " Count(computed_total) AS sg_count," //
        + " stddev_pop(computed_total) AS sg_stdev" //
        + " FROM subjective_computed_scores" //
        + " WHERE tournament = ?" //
        + "   AND computed_total IS NOT NULL" //
        + "   AND no_show = false"//
        + " GROUP BY category, goal_group, judge" //
    );
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
        // 6 - category
        // 7 - goal group
        PreparedStatement updatePrep = connection.prepareStatement("UPDATE subjective_computed_scores " //
            + " SET standardized_score = ((computed_total - ?) * ? ) + ?"
            + " WHERE judge = ?" //
            + " AND tournament = ?" //
            + " AND category = ?" //
            + " AND goal_group = ?" //
        )) {
      selectPrep.setInt(1, tournament);

      updatePrep.setDouble(3, mean);
      updatePrep.setInt(5, tournament);

      try (ResultSet rs = selectPrep.executeQuery()) {
        while (rs.next()) {
          final String category = rs.getString(1);
          final String goalGroup = rs.getString(2);
          final String judge = rs.getString(3);

          final int sgCount = rs.getInt(5);

          if (sgCount > 1) {
            final double sgMean = rs.getDouble(4);
            final double sgStdev = rs.getDouble(6);

            updatePrep.setDouble(1, sgMean);
            updatePrep.setDouble(2, sigma
                / sgStdev);
            updatePrep.setString(4, judge);
            updatePrep.setString(6, category);
            updatePrep.setString(7, goalGroup);
            updatePrep.executeUpdate();
          } else { // if(sgCount == 1) {
            LOGGER.error("Not enough scores for Judge: {} in category: {} goal group: {}", judge, category, goalGroup);
          } // ignore 0 in a judging group

        } // foreach result
      } // result set
    } // prepared statements
  }

  /**
   * Updates overall_scores with the sum of the the scores times the weights for
   * the given tournament.
   *
   * @param description challenge description
   * @param connection database connection
   * @param tournament the tournament to add scores for
   * @throws SQLException on an error talking to the database
   */
  public static void updateTeamTotalScores(final Connection connection,
                                           final ChallengeDescription description,
                                           final int tournament)
      throws SQLException {
    final Map<Integer, TournamentTeam> tournamentTeams = Queries.getTournamentTeams(connection, tournament);

    final Map<String, Double> categoryWeights = new HashMap<>();
    final ScoreCategory performanceCategory = description.getPerformance();
    categoryWeights.put(performanceCategory.getName(), performanceCategory.getWeight());

    description.getSubjectiveCategories().forEach(cat -> categoryWeights.put(cat.getName(), cat.getWeight()));

    final Tournament currentTournament = Tournament.findTournamentByID(connection, tournament);

    // delete old values
    try (
        PreparedStatement deletePrep = connection.prepareStatement("DELETE FROM overall_scores WHERE tournament = ?")) {
      deletePrep.setInt(1, currentTournament.getTournamentID());
      deletePrep.executeUpdate();
    }

    // insert new values
    try (
        PreparedStatement selectPrep = connection.prepareStatement("SELECT category, final_score FROM final_scores WHERE goal_group = ? AND tournament = ? AND team_number = ?");
        PreparedStatement insertPrep = connection.prepareStatement("INSERT INTO overall_scores (tournament, team_number, overall_score) VALUES (?, ?, ?)")) {
      selectPrep.setString(1, "");
      selectPrep.setInt(2, currentTournament.getTournamentID());
      insertPrep.setInt(1, currentTournament.getTournamentID());

      // compute scores for all teams treating NULL as 0
      for (final int teamNumber : tournamentTeams.keySet()) {
        double overallScore = 0;

        selectPrep.setInt(3, teamNumber);
        try (ResultSet selectResult = selectPrep.executeQuery()) {
          while (selectResult.next()) {
            final String categoryName = castNonNull(selectResult.getString(1));

            if (categoryWeights.containsKey(categoryName)) {
              final double weight = categoryWeights.get(categoryName);
              final double score = selectResult.getDouble(2);
              overallScore += score
                  * weight;
            }
          } // foreach result
        } // selectResult

        insertPrep.setInt(2, teamNumber);
        insertPrep.setDouble(3, overallScore);
        insertPrep.executeUpdate();
      } // foreach team

      currentTournament.recordScoreSummariesUpdated(connection);
    } // PreparedStatements
  }

}
