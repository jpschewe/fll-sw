/*
 * This code is released under GPL; see LICENSE for details.
 */

package fll.scores;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Team;
import fll.Tournament;
import fll.TournamentTeam;
import fll.db.NonNumericNominees;
import fll.xml.SubjectiveScoreCategory;

/**
 * Access {@link SubjectiveTeamScore} objects from the database.
 */
public final class DatabaseSubjectiveTeamScore {

  private DatabaseSubjectiveTeamScore() {
  }

  private static Map<String, Double> fetchSimpleGoals(final int tournament,
                                                      final String categoryName,
                                                      final String judge,
                                                      final int teamNumber,
                                                      final Connection connection,
                                                      final Map<String, @Nullable String> goalComments)
      throws SQLException {
    final Map<String, Double> values = new HashMap<>();
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT goal_name, goal_value, comment FROM subjective_goals" //
            + " WHERE tournament_id = ?" //
            + " AND category_name = ?" //
            + " AND judge = ?" //
            + " AND team_number = ?" //
        )) {
      prep.setInt(1, tournament);
      prep.setString(2, categoryName);
      prep.setString(3, judge);
      prep.setInt(4, teamNumber);
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String name = castNonNull(rs.getString(1));
          final double value = rs.getDouble(2);
          values.put(name, value);

          final @Nullable String comment = rs.getString(3);
          if (!StringUtils.isBlank(comment)) {
            goalComments.put(name, comment);
          }
        }
      }
    }
    return values;
  }

  private static Map<String, String> fetchEnumGoals(final int tournament,
                                                    final String categoryName,
                                                    final String judge,
                                                    final int teamNumber,
                                                    final Connection connection,
                                                    final Map<String, @Nullable String> goalComments)
      throws SQLException {
    final Map<String, String> values = new HashMap<>();
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT goal_name, goal_value, comment FROM subjective_enum_goals" //
            + " WHERE tournament_id = ?" //
            + " AND category_name = ?" //
            + " AND judge = ?" //
            + " AND team_number = ?" //
        )) {
      prep.setInt(1, tournament);
      prep.setString(2, categoryName);
      prep.setString(3, judge);
      prep.setInt(4, teamNumber);
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String name = castNonNull(rs.getString(1));
          final String value = castNonNull(rs.getString(2));
          values.put(name, value);

          final @Nullable String comment = rs.getString(3);
          if (!StringUtils.isBlank(comment)) {
            goalComments.put(name, comment);
          }
        }
      }
    }
    return values;
  }

  /**
   * Assumes the result set is created from {@link #STANDARD_SUBJECTIVE_COLUMNS}.
   */
  private static SubjectiveTeamScore fromResultSet(final Connection connection,
                                                   final SubjectiveScoreCategory category,
                                                   final Tournament tournament,
                                                   final ResultSet rs)
      throws SQLException {

    final String judge = castNonNull(rs.getString(1));
    final int teamNumber = rs.getInt(2);
    final boolean noShow = rs.getBoolean(3);
    final @Nullable String note = rs.getString(4);
    final @Nullable String commentGreatJob = rs.getString(5);
    final @Nullable String commentThinkAbout = rs.getString(6);

    final Map<String, @Nullable String> goalComments = new HashMap<>();
    final Map<String, Double> simpleGoals = fetchSimpleGoals(tournament.getTournamentID(), category.getName(), judge,
                                                             teamNumber, connection, goalComments);
    final Map<String, String> enumGoals = fetchEnumGoals(tournament.getTournamentID(), category.getName(), judge,
                                                         teamNumber, connection, goalComments);

    final Set<String> nominatedCategories = NonNumericNominees.getNomineesByJudgeForTeam(connection, tournament, judge,
                                                                                         teamNumber);

    final SubjectiveTeamScore score = new DefaultSubjectiveTeamScore(teamNumber, simpleGoals, enumGoals, noShow, judge,
                                                                     commentGreatJob, commentThinkAbout, goalComments,
                                                                     note, nominatedCategories);
    return score;
  }

  /**
   * Check if a category has any scores.
   * 
   * @param connection database
   * @param category the category to check
   * @param tournament the tournament
   * @return true if there are scores for the category in the specified tournament
   * @throws SQLException on a database error
   */
  public static boolean categoryHasScores(final Connection connection,
                                          final SubjectiveScoreCategory category,
                                          final Tournament tournament)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT COUNT(*) FROM subjective"
        + " WHERE tournament_id = ?" //
        + " AND category_name = ?"//
    )) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setString(2, category.getName());
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1) > 0;
        } else {
          return false;
        }
      }
    }
  }

  private static final String STANDARD_SUBJECTIVE_COLUMNS = "judge, team_number, NoShow, note, comment_great_job, comment_think_about";

  /**
   * @param connection database connection
   * @param tournament the tournament
   * @param category the category to get scores for
   * @param awardGroup the award group to get scores for
   * @return scores
   * @throws SQLException on a database error
   */
  public static Collection<SubjectiveTeamScore> getScoresForCategoryAndAwardGroup(final Connection connection,
                                                                                  final Tournament tournament,
                                                                                  final SubjectiveScoreCategory category,
                                                                                  final String awardGroup)
      throws SQLException {
    final Collection<SubjectiveTeamScore> scores = new LinkedList<>();

    try (PreparedStatement prep = connection.prepareStatement("SELECT "
        + STANDARD_SUBJECTIVE_COLUMNS //
        + " FROM subjective"
        + " WHERE tournament_id = ?" //
        + " AND category_name = ?" //
        + " AND team_number IN ( " //
        + "  SELECT TeamNumber FROM TournamentTeams"//
        + "    WHERE Tournament = ?" //
        + "    AND event_division = ?" //
        + "  )" //
    )) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setString(2, category.getName());
      prep.setInt(3, tournament.getTournamentID());
      prep.setString(4, awardGroup);

      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final SubjectiveTeamScore score = fromResultSet(connection, category, tournament, rs);

          scores.add(score);
        } // foreach result

      } // allocate result set
    } // allocate prep

    return scores;
  }

  /**
   * Get all scores for a category at a tournament.
   * 
   * @param connection database connection
   * @param tournament the tournament
   * @param category the category to get scores for
   * @return scores
   * @throws SQLException on a database error
   */
  public static Collection<SubjectiveTeamScore> getScoresForCategory(final Connection connection,
                                                                     final Tournament tournament,
                                                                     final SubjectiveScoreCategory category)
      throws SQLException {
    final Collection<SubjectiveTeamScore> scores = new LinkedList<>();

    try (PreparedStatement prep = connection.prepareStatement("SELECT  "
        + STANDARD_SUBJECTIVE_COLUMNS //
        + " FROM subjective" //
        + " WHERE tournament_id = ?" //
        + " AND category_name = ?" //
    )) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setString(2, category.getName());

      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final SubjectiveTeamScore score = fromResultSet(connection, category, tournament, rs);

          scores.add(score);
        } // foreach result

      } // allocate result set
    } // allocate prep

    return scores;
  }

  /**
   * @param connection database
   * @param category category to get scores for
   * @param tournament the tournament to get scores for
   * @param team the team to get scores for
   * @return scores
   * @throws SQLException on a database error
   */
  public static Collection<SubjectiveTeamScore> getScoresForTeam(final Connection connection,
                                                                 final SubjectiveScoreCategory category,
                                                                 final Tournament tournament,
                                                                 final Team team)
      throws SQLException {
    final Collection<SubjectiveTeamScore> scores = new LinkedList<>();

    try (PreparedStatement prep = connection.prepareStatement("SELECT "
        + STANDARD_SUBJECTIVE_COLUMNS //
        + " FROM  subjective" //
        + " WHERE tournament_id = ?" //
        + " AND category_name = ?" //
        + " AND team_number = ?" //
    )) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setString(2, category.getName());
      prep.setInt(3, team.getTeamNumber());

      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final SubjectiveTeamScore score = fromResultSet(connection, category, tournament, rs);

          scores.add(score);
        } // foreach result

      } // allocate result set
    } // allocate prep

    return scores;
  }

  /**
   * @param connection database
   * @param category category to get scores for
   * @param tournament the tournament to get scores for
   * @param team the team to get scores for
   * @param judge the judge that created the scores
   * @return scores
   * @throws SQLException on a database error
   */
  public static Collection<SubjectiveTeamScore> getScoresForTeamAndJudge(final Connection connection,
                                                                         final SubjectiveScoreCategory category,
                                                                         final Tournament tournament,
                                                                         final TournamentTeam team,
                                                                         final String judge)
      throws SQLException {
    final Collection<SubjectiveTeamScore> scores = new LinkedList<>();

    try (PreparedStatement prep = connection.prepareStatement("SELECT "
        + STANDARD_SUBJECTIVE_COLUMNS
        + STANDARD_SUBJECTIVE_COLUMNS //
        + " FROM  subjective" //
        + " WHERE tournament_id = ?" //
        + " AND category_name = ?" //
        + " AND team_number = ?" //
        + " AND judge = ?" //
    )) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setString(2, category.getName());
      prep.setInt(3, team.getTeamNumber());
      prep.setString(4, judge);

      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final SubjectiveTeamScore score = fromResultSet(connection, category, tournament, rs);

          scores.add(score);
        } // foreach result

      } // allocate result set
    } // allocate prep

    return scores;
  }
}
