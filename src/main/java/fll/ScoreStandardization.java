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
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.TournamentData;
import fll.web.playoff.DatabaseTeamScore;
import fll.xml.ChallengeDescription;
import fll.xml.PerformanceScoreCategory;
import fll.xml.ScoreCategory;
import fll.xml.SubjectiveGoalRef;
import fll.xml.SubjectiveScoreCategory;
import fll.xml.VirtualSubjectiveScoreCategory;
import jakarta.servlet.ServletContext;

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
      selectPrep.setInt(1, tournament);

      for (final SubjectiveScoreCategory category : challengeDescription.getSubjectiveCategories()) {
        final double categoryMaximumScore = category.getMaximumScore();

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
      for (final VirtualSubjectiveScoreCategory category : challengeDescription.getVirtualSubjectiveCategories()) {
        final double categoryMaximumScore = category.getMaximumScore();

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

  /**
   * Get all information from application attributes and wrap the
   * {@link SQLException} in {@link FLLRuntimeException}.
   * 
   * @param application get all variables
   */
  public static void computeSummarizedScoresIfNeeded(final ServletContext application) {
    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final DataSource datasource = tournamentData.getDataSource();
    final Tournament tournament = tournamentData.getCurrentTournament();
    try (Connection connection = datasource.getConnection()) {
      computeSummarizedScoresIfNeeded(connection, description, tournament);
    } catch (final SQLException e) {
      throw new FLLRuntimeException(e);
    }

  }

  /**
   * If score summarization is needed, do it.
   * Does not create a transaction.
   * 
   * @param connection database
   * @param challengeDescription challenge
   * @param tournament tournament
   * @throws SQLException on a database error
   * @see Tournament#checkTournamentNeedsSummaryUpdate(Connection)
   */
  public static void computeSummarizedScoresIfNeeded(final Connection connection,
                                                     final ChallengeDescription challengeDescription,
                                                     final Tournament tournament)
      throws SQLException {
    if (tournament.checkTournamentNeedsSummaryUpdate(connection)) {
      ScoreStandardization.updateScoreTotals(challengeDescription, connection, tournament.getTournamentID());

      ScoreStandardization.summarizeScores(connection, challengeDescription, tournament.getTournamentID());
      ScoreStandardization.updateTeamTotalScores(connection, challengeDescription, tournament.getTournamentID());
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
    description.getVirtualSubjectiveCategories().forEach(cat -> categoryWeights.put(cat.getName(), cat.getWeight()));

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

  /**
   * Total the scores in the database for the specified tournament.
   *
   * @param description describes the scoring for the challenge
   * @param connection connection to database, needs write privileges
   * @param tournament tournament to update score totals for
   * @throws SQLException if an error occurs
   * @see #updatePerformanceScoreTotals(ChallengeDescription, Connection, int)
   * @see #updateSubjectiveScoreTotals(ChallengeDescription, Connection, int)
   */
  public static void updateScoreTotals(final ChallengeDescription description,
                                       final Connection connection,
                                       final int tournament)
      throws SQLException {
    updatePerformanceScoreTotals(description, connection, tournament);

    updateSubjectiveScoreTotals(description, connection, tournament);

    populateVirtualSubjectiveCategories(connection, description, tournament);
    summarizeVirtualSubjectiveCategories(connection, tournament);
  }

  /**
   * Compute the total scores for all entered subjective scores.
   * This populates the table subjecive_computed_scores.
   *
   * @param connection
   * @throws SQLException
   */
  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category determines table name")
  private static void updateSubjectiveScoreTotals(final ChallengeDescription description,
                                                  final Connection connection,
                                                  final int tournament)
      throws SQLException {

    try (
        PreparedStatement deletePrep = connection.prepareStatement("DELETE FROM subjective_computed_scores WHERE tournament = ?")) {
      deletePrep.setInt(1, tournament);
      deletePrep.executeUpdate();
    }

    for (final SubjectiveScoreCategory subjectiveElement : description.getSubjectiveCategories()) {
      final String categoryName = subjectiveElement.getName();

      try (PreparedStatement insertPrep = connection.prepareStatement("INSERT INTO subjective_computed_scores"//
          + " (category, tournament, team_number, judge, computed_total, no_show) " //
          + " VALUES(?, ?, ?, ?, ?, ?)");
          PreparedStatement selectPrep = connection.prepareStatement("SELECT * FROM " //
              + categoryName //
              + " WHERE Tournament = ?")) {
        selectPrep.setInt(1, tournament);

        insertPrep.setString(1, categoryName);
        insertPrep.setInt(2, tournament);

        try (ResultSet rs = selectPrep.executeQuery()) {
          while (rs.next()) {
            final int teamNumber = rs.getInt("TeamNumber");
            insertPrep.setInt(3, teamNumber);

            final DatabaseTeamScore teamScore = new DatabaseTeamScore(teamNumber, rs);
            final double computedTotal;
            if (teamScore.isNoShow()) {
              computedTotal = Double.NaN;
            } else {
              computedTotal = subjectiveElement.evaluate(teamScore);
            }

            final String judge = rs.getString("Judge");
            insertPrep.setString(4, judge);

            insertPrep.setBoolean(6, teamScore.isNoShow());

            // insert category score
            if (Double.isNaN(computedTotal)) {
              insertPrep.setNull(5, Types.DOUBLE);
            } else {
              insertPrep.setDouble(5, computedTotal);
            }
            insertPrep.executeUpdate();
          } // foreach result
        } // ResultSet
      } // prepared statements
    } // foreach category
  }

  /**
   * Compute the total scores for all entered performance scores. Uses both
   * verified and unverified scores.
   *
   * @param description description of the challenge
   * @param connection connection to the database
   * @param tournament the tournament to update scores for.
   * @throws SQLException on a database error
   */
  private static void updatePerformanceScoreTotals(final ChallengeDescription description,
                                                   final Connection connection,
                                                   final int tournament)
      throws SQLException {
    try (
        PreparedStatement updatePrep = connection.prepareStatement("UPDATE Performance SET ComputedTotal = ? WHERE TeamNumber = ? AND Tournament = ? AND RunNumber = ?");
        PreparedStatement selectPrep = connection.prepareStatement("SELECT * FROM Performance WHERE Tournament = ?")) {

      updatePrep.setInt(3, tournament);
      selectPrep.setInt(1, tournament);

      final PerformanceScoreCategory performanceElement = description.getPerformance();
      final double minimumPerformanceScore = performanceElement.getMinimumScore();
      try (ResultSet rs = selectPrep.executeQuery()) {
        while (rs.next()) {
          if (!rs.getBoolean("Bye")) {
            final int teamNumber = rs.getInt("TeamNumber");
            final int runNumber = rs.getInt("RunNumber");
            final double computedTotal;

            final DatabaseTeamScore teamScore = new DatabaseTeamScore(teamNumber, runNumber, rs);
            if (teamScore.isNoShow()) {
              computedTotal = Double.NaN;
            } else {
              computedTotal = performanceElement.evaluate(teamScore);
            }

            if (LOGGER.isTraceEnabled()) {
              LOGGER.trace("Updating performance score for "
                  + teamNumber
                  + " run: "
                  + runNumber
                  + " total: "
                  + computedTotal);
            }

            if (!Double.isNaN(computedTotal)) {
              updatePrep.setDouble(1, Math.max(computedTotal, minimumPerformanceScore));
            } else {
              updatePrep.setNull(1, Types.DOUBLE);
            }
            updatePrep.setInt(2, teamNumber);
            updatePrep.setInt(4, runNumber);
            updatePrep.executeUpdate();
          }
        }
      }
    }
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category name and goal name need to be inserted as strings")
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
                + " SELECT CAST(? AS INTEGER), CAST(? AS LONGVARCHAR), CAST(? AS LONGVARCHAR), CAST(? AS LONGVARCHAR), TeamNumber, AVG(IFNULL("
                + ref.getGoalName()
                + ", 0)) FROM "
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
        PreparedStatement prep = connection.prepareStatement("INSERT INTO subjective_computed_scores (tournament, category, team_number, computed_total, judge)"
            + " SELECT tournament_id, category_name, team_number, sum(goal_score), 'virtual'"
            + "  FROM virtual_subjective_category"
            + "    WHERE tournament_id = ?"
            + "  GROUP BY team_number, tournament, category_name")) {
      prep.setInt(1, tournament);
      prep.executeUpdate();
    }
  }

}
