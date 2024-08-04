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

import fll.db.Queries;
import fll.xml.ChallengeDescription;
import fll.xml.PerformanceScoreCategory;
import fll.xml.ScoreCategory;
import fll.xml.SubjectiveGoalRef;
import fll.xml.SubjectiveScoreCategory;
import fll.xml.VirtualSubjectiveScoreCategory;

/**
 * Does score standardization routines from the web.
 */
public final class ScoreStandardization {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private ScoreStandardization() {
    // no instances
  }

  private static void summarizePerformanceScores(final Connection connection,
                                                 final int tournament,
                                                 final ChallengeDescription challengeDescription,
                                                 final double maximumScore)
      throws SQLException {

    final PerformanceScoreCategory category = challengeDescription.getPerformance();
    final double categoryMaximumScore = category.getMaximumScore();

    try (
        PreparedStatement select = connection.prepareStatement("SELECT TeamNumber, Score FROM performance_seeding_max WHERE performance_seeding_max.tournament = ?");
        PreparedStatement insert = connection.prepareStatement("INSERT INTO final_scores (category, tournament, team_number, final_score) VALUES(?, ?, ?, ?)")) {
      insert.setString(1, PerformanceScoreCategory.CATEGORY_NAME);
      insert.setInt(2, tournament);

      select.setInt(1, tournament);
      try (ResultSet perfScores = select.executeQuery()) {
        while (perfScores.next()) {
          final int teamNumber = perfScores.getInt(1);
          final double rawScore = perfScores.getDouble(2);
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Team: "
                + teamNumber
                + " rawScore: "
                + rawScore);
          }

          final double scaledScore = (rawScore
              * maximumScore)
              / categoryMaximumScore;

          insert.setInt(3, teamNumber);
          insert.setDouble(4, scaledScore);
          insert.executeUpdate();
        } // foreach team's score
      } // select team scores
    } // allocate statements
  }

  private static void summarizeSubjectiveScores(final Connection connection,
                                                final ChallengeDescription challengeDescription,
                                                final double maximumScore,
                                                final int tournament)
      throws SQLException {
    try (
        PreparedStatement updatePrep = connection.prepareStatement("INSERT INTO final_scores (category, tournament, team_number, final_score) VALUES(?, ?, ?, ?)");
        PreparedStatement selectPrep = connection.prepareStatement("SELECT team_number, AVG(computed_total) FROM subjective_computed_scores" //
            + " WHERE computed_total IS NOT NULL" //
            + " AND tournament = ?" //
            + " AND category = ?" //
            + " GROUP BY team_number")) {
      updatePrep.setInt(2, tournament);

      for (final SubjectiveScoreCategory category : challengeDescription.getSubjectiveCategories()) {
        final double categoryMaximumScore = category.getMaximumScore();

        selectPrep.setInt(1, tournament);
        selectPrep.setString(2, category.getName());

        updatePrep.setString(1, category.getName());

        try (ResultSet rs = selectPrep.executeQuery()) {
          while (rs.next()) {
            final int teamNumber = rs.getInt(1);
            final double rawScore = rs.getDouble(2);

            final double scaledScore = (rawScore
                * maximumScore)
                / categoryMaximumScore;
            updatePrep.setInt(3, teamNumber);
            updatePrep.setDouble(4, scaledScore);
            updatePrep.executeUpdate();
          }
        }
      }
    }
  }

  private static void populateVirtualSubjectiveCategories(final Connection connection,
                                                          final ChallengeDescription description,
                                                          final int tournamentId)
      throws SQLException {
    try (
        PreparedStatement delete = connection.prepareStatement("DELETE FROM virtual_subjective_category WHERE tournament_id = ?")) {
      delete.setInt(1, tournamentId);
      delete.executeUpdate();
    }

    for (VirtualSubjectiveScoreCategory category : description.getVirtualSubjectiveCategories()) {
      for (SubjectiveGoalRef ref : category.getGoalReferences()) {

        try (
            PreparedStatement insert = connection.prepareStatement("INSERT INTO virtual_subjective_category (tournament_id, category_name, source_category_name, goal_name, team_number, goal_score)"
                + " SELECT CAST(? AS INTEGER), CAST(? AS LONGVARCHAR), CAST(? AS LONGVARCHAR), CAST(? AS LONGVARCHAR), TeamNumber, AVG("
                + ref.getGoalName()
                + ") FROM "
                + ref.getCategory().getName()
                + " WHERE Tournament = ?"
                + " GROUP BY TeamNumber")) {
          insert.setInt(1, tournamentId);
          insert.setString(2, category.getName());
          insert.setString(3, ref.getCategory().getName());
          insert.setString(4, ref.getGoalName());
          insert.setInt(5, tournamentId);

          insert.executeUpdate();
        }
      }
    }
  }

  private static void summarizeVirtualSubjectiveCategories(final Connection connection,
                                                           final int tournament)
      throws SQLException {
    try (
        PreparedStatement prep = connection.prepareStatement("INSERT INTO final_scores (tournament, category, team_number, final_score)"
            + " SELECT tournament_id, category_name, team_number, sum(goal_score) as computed_total"
            + "  FROM virtual_subjective_category"
            + "    WHERE tournament_id = ?"
            + "  GROUP BY team_number, tournament, category_name")) {
      prep.setInt(1, tournament);
      prep.executeUpdate();
    }
  }

  /**
   * Summarize the scores for the given tournament. This puts the standardized
   * scores in the final_scores table to be weighted and then summed.
   *
   * @param connection connection to the database with delete and insert
   *          privileges
   * @param challengeDescription used to get scaling information
   * @param tournament which tournament to summarize scores for
   * @throws SQLException a database error
   */
  public static void summarizeScores(final Connection connection,
                                     final ChallengeDescription challengeDescription,
                                     final int tournament)
      throws SQLException {

    // delete all final scores for the tournament
    try (PreparedStatement deletePrep = connection.prepareStatement("DELETE FROM final_scores WHERE tournament = ?")) {
      deletePrep.setInt(1, tournament);
      deletePrep.executeUpdate();
    }

    final double maxScoreRangeSize = challengeDescription.getMaximumScore();

    summarizePerformanceScores(connection, tournament, challengeDescription, maxScoreRangeSize);

    summarizeSubjectiveScores(connection, challengeDescription, maxScoreRangeSize, tournament);

    populateVirtualSubjectiveCategories(connection, challengeDescription, tournament);
    summarizeVirtualSubjectiveCategories(connection, tournament);
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
        PreparedStatement selectPrep = connection.prepareStatement("SELECT category, final_score FROM final_scores WHERE tournament = ? AND team_number = ?");
        PreparedStatement insertPrep = connection.prepareStatement("INSERT INTO overall_scores (tournament, team_number, overall_score) VALUES (?, ?, ?)")) {
      selectPrep.setInt(1, currentTournament.getTournamentID());
      insertPrep.setInt(1, currentTournament.getTournamentID());

      // compute scores for all teams treating NULL as 0
      for (final int teamNumber : tournamentTeams.keySet()) {
        double overallScore = 0;

        selectPrep.setInt(2, teamNumber);
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
