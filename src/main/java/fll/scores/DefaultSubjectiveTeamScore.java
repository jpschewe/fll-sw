/*
 * This code is released under GPL; see LICENSE for details.
 */

package fll.scores;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Team;
import fll.Tournament;
import fll.TournamentTeam;
import fll.db.GenerateDB;
import fll.db.NonNumericNominees;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.xml.AbstractGoal;
import fll.xml.SubjectiveScoreCategory;

/**
 * Subjective score populated from values.
 */
public final class DefaultSubjectiveTeamScore extends DefaultTeamScore implements SubjectiveTeamScore {

  /**
   * @param teamNumber see {@link #getTeamNumber()}
   * @param simpleGoals simple goal values
   * @param enumGoals enum goal values
   * @param noShow see {@link #isNoShow()}
   * @param judge see {@link #getJudge()}
   * @param commentGreatJob see {@link #getCommentGreatJob()}
   * @param commentThinkAbout see {@link #getCommentThinkAbout()}
   * @param goalComments see {@link #getGoalComment(String)}
   * @param note {@link #getNote()}
   * @param nonNumericNominations see {@link #getNonNumericNominations()}
   */
  private DefaultSubjectiveTeamScore(final int teamNumber,
                                     final Map<String, Double> simpleGoals,
                                     final Map<String, String> enumGoals,
                                     final boolean noShow,
                                     final String judge,
                                     final @Nullable String commentGreatJob,
                                     final @Nullable String commentThinkAbout,
                                     final Map<String, @Nullable String> goalComments,
                                     final String note,
                                     final Set<String> nonNumericNominations) {
    super(teamNumber, simpleGoals, enumGoals, noShow);
    this.judge = judge;
    this.commentGreatJob = commentGreatJob;
    this.commentThinkAbout = commentThinkAbout;
    this.goalComments = new HashMap<>(goalComments);
    this.note = note;
    this.nonNumericNominations = Collections.unmodifiableSet(new HashSet<>(nonNumericNominations));
  }

  private final String judge;

  @Override
  public String getJudge() {
    return judge;
  }

  private final @Nullable String commentGreatJob;

  @Override
  public @Nullable String getCommentGreatJob() {
    return commentGreatJob;
  }

  private final @Nullable String commentThinkAbout;

  @Override
  public @Nullable String getCommentThinkAbout() {
    return commentThinkAbout;
  }

  private final Map<String, @Nullable String> goalComments;

  @Override
  public @Nullable String getGoalComment(final String goalName) {
    return goalComments.get(goalName);
  }

  private final Set<String> nonNumericNominations;

  @Override
  public Set<String> getNonNumericNominations() {
    return nonNumericNominations;
  }

  private final @Nullable String note;

  @Override
  public @Nullable String getNote() {
    return note;
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

    try (PreparedStatement prep = connection.prepareStatement("SELECT * FROM "
        + category.getName()
        + " WHERE Tournament = ?" //
        + " AND teamnumber = ?" //
        + " AND judge = ?" //
    )) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setInt(2, team.getTeamNumber());
      prep.setString(3, judge);

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

    try (PreparedStatement prep = connection.prepareStatement("SELECT * FROM "
        + category.getName()
        + " WHERE Tournament = ?" //
        + " AND teamnumber = ?" //
    )) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setInt(2, team.getTeamNumber());

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

    try (PreparedStatement prep = connection.prepareStatement("SELECT * FROM "
        + category.getName()
        + " WHERE Tournament = ?")) {
      prep.setInt(1, tournament.getTournamentID());

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

    final String teamNumbersStr = Queries.getTournamentTeams(connection, tournament.getTournamentID())//
                                         .entrySet().stream() //
                                         .map(Map.Entry::getValue) //
                                         .filter(t -> t.getAwardGroup().equals(awardGroup)) //
                                         .map(t -> String.valueOf(t.getTeamNumber())) //
                                         .collect(Collectors.joining(", "));

    try (PreparedStatement prep = connection.prepareStatement("SELECT * FROM "
        + category.getName()
        + " WHERE Tournament = ? AND teamnumber IN ( "
        + teamNumbersStr
        + " )")) {
      prep.setInt(1, tournament.getTournamentID());

      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final SubjectiveTeamScore score = fromResultSet(connection, category, tournament, rs);

          scores.add(score);
        } // foreach result

      } // allocate result set
    } // allocate prep

    return scores;
  }

  private static SubjectiveTeamScore fromResultSet(final Connection connection,
                                                   final SubjectiveScoreCategory category,
                                                   final Tournament tournament,
                                                   final ResultSet rs)
      throws SQLException {

    final String judge = castNonNull(rs.getString("Judge"));

    final int teamNumber = rs.getInt("TeamNumber");
    final boolean noShow = rs.getBoolean("NoShow");

    final @Nullable String commentGreatJob = rs.getString("comment_great_job");
    final @Nullable String commentThinkAbout = rs.getString("comment_think_about");
    final @Nullable String note = rs.getString("note");

    final Map<String, Double> simpleGoals = new HashMap<>();
    final Map<String, String> enumGoals = new HashMap<>();
    final Map<String, @Nullable String> goalComments = new HashMap<>();
    for (final AbstractGoal goal : category.getAllGoals()) {
      if (goal.isEnumerated()) {
        final String value = rs.getString(goal.getName());
        if (null == value) {
          throw new FLLInternalException("Found enumerated goal '"
              + goal.getName()
              + "' with null value in the database");
        }
        enumGoals.put(goal.getName(), value);
      } else {
        final double value = rs.getDouble(goal.getName());
        simpleGoals.put(goal.getName(), value);
      }

      final String commentColumn = GenerateDB.getGoalCommentColumnName(goal);
      final @Nullable String comment = rs.getString(commentColumn);
      if (!StringUtils.isBlank(comment)) {
        goalComments.put(goal.getName(), comment);
      }
    } // foreach goal

    final Set<String> nominatedCategories = NonNumericNominees.getNomineesByJudgeForTeam(connection, tournament, judge,
                                                                                         teamNumber);

    final SubjectiveTeamScore score = new DefaultSubjectiveTeamScore(teamNumber, simpleGoals, enumGoals, noShow, judge,
                                                                     commentGreatJob, commentThinkAbout, goalComments,
                                                                     note, nominatedCategories);
    return score;
  }

}
