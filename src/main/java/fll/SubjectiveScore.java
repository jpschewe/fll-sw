/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.db.GenerateDB;
import fll.db.NonNumericNominees;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.xml.AbstractGoal;
import fll.xml.Goal;
import fll.xml.NonNumericCategory;
import fll.xml.SubjectiveScoreCategory;

/**
 * A subjective score from the database.
 * Primarily used to exchange data with the subjective web application.
 */
public class SubjectiveScore {

  /**
   * Constructor that sets all default values.
   */
  public SubjectiveScore() {
    noShow = false;
    modified = false;
    deleted = false;
    judge = "Unknown Judge";
    teamNumber = Team.NULL_TEAM_NUMBER;
    note = null;
    scoreOnServer = false;
    commentGreatJob = null;
    commentThinkAbout = null;
  }

  private boolean scoreOnServer;

  /**
   * @return does the score exist on the server?
   */
  public boolean getScoreOnServer() {
    return scoreOnServer;
  }

  /**
   * @param v see {@link #getScoreOnServer()}
   */
  public void setScoreOnServer(final boolean v) {
    scoreOnServer = v;
  }

  private boolean noShow;

  /**
   * @return is this a now show?
   */
  public boolean getNoShow() {
    return noShow;
  }

  /**
   * @param v see {@link #getNoShow()}
   */
  public void setNoShow(final boolean v) {
    noShow = v;
  }

  private boolean modified;

  /**
   * @return Has the score been modified since being pulled
   *         from the database?
   */
  public boolean getModified() {
    return modified;
  }

  /**
   * @param v see {@link #getModified()}
   */
  public void setModified(final boolean v) {
    modified = v;
  }

  private boolean deleted;

  /**
   * @return Should the score be deleted from the database?
   */
  public boolean getDeleted() {
    return deleted;
  }

  /**
   * @param v see {@link #getDeleted()}
   */
  public void setDeleted(final boolean v) {
    deleted = v;
  }

  private String judge;

  /**
   * @return ID of the judge that created the scores.
   */
  public String getJudge() {
    return judge;
  }

  /**
   * @param v see {@link #getJudge()}
   */
  public void setJudge(final String v) {
    judge = v;
  }

  private int teamNumber;

  /**
   * @return the team that the score is for
   */
  public int getTeamNumber() {
    return teamNumber;
  }

  /**
   * @param v see {@link #getTeamNumber()}
   */
  public void setTeamNumber(final int v) {
    teamNumber = v;
  }

  private final Map<String, Double> standardSubScores = new HashMap<>();

  /**
   * The scores for goals that are not enumerated.
   * 
   * @return goal name, score (read-only)
   * @see Goal#isEnumerated()
   */
  public Map<String, Double> getStandardSubScores() {
    return Collections.unmodifiableMap(standardSubScores);
  }

  /**
   * @param v see {@link #getStandardSubScores()}
   */
  public void setStandardSubScores(final @Nullable Map<String, Double> v) {
    standardSubScores.clear();
    if (null != v) {
      standardSubScores.putAll(v);
    }
  }

  private final Map<String, String> enumSubScores = new HashMap<>();

  /**
   * The scores for goals that are enumerated.
   * 
   * @return goal name, enumerated value (read-only)
   * @see Goal#isEnumerated()
   * @see #getStandardSubScores()
   */
  public Map<String, String> getEnumSubScores() {
    return Collections.unmodifiableMap(enumSubScores);
  }

  /**
   * @param v see {@link #getEnumSubScores()}
   */
  public void setEnumSubScores(final @Nullable Map<String, String> v) {
    enumSubScores.clear();
    if (null != v) {
      enumSubScores.putAll(v);
    }
  }

  private @Nullable String note;

  /**
   * @return note that the judge uses to remember the team
   */
  public @Nullable String getNote() {
    return note;
  }

  /**
   * @param v see {@link #getNote()}
   */
  public void setNote(final @Nullable String v) {
    note = v;
  }

  private @Nullable String commentGreatJob;

  /**
   * @return the "Great Job..." comment
   */
  public @Nullable String getCommentGreatJob() {
    return commentGreatJob;
  }

  /**
   * @param v see {@link #getCommentGreatJob()}
   */
  public void setCommentGreatJob(final @Nullable String v) {
    commentGreatJob = v;
  }

  private @Nullable String commentThinkAbout;

  /**
   * @return the "Think About..." comment
   */
  public @Nullable String getCommentThinkAbout() {
    return commentThinkAbout;
  }

  /**
   * @param v see {@link #getCommentThinkAbout()}
   */
  public void setCommentThinkAbout(final @Nullable String v) {
    commentThinkAbout = v;
  }

  private final Map<String, String> goalComments = new HashMap<>();

  /**
   * The comments specific to each goal.
   * 
   * @return goal name, comment (read-only)
   */
  public Map<String, String> getGoalComments() {
    return Collections.unmodifiableMap(goalComments);
  }

  /**
   * @param v see {@link #getGoalComments()}
   */
  public void setGoalComments(final Map<String, String> v) {
    goalComments.clear();
    goalComments.putAll(v);
  }

  private final Set<String> nonNumericNominations = new HashSet<>();

  /**
   * @return unmodifiable list of {@link NonNumericCategory#getTitle()}
   */
  public Set<String> getNonNumericNominations() {
    return Collections.unmodifiableSet(nonNumericNominations);
  }

  /**
   * @param v see {@link #getNonNumericNominations()}
   */
  public void setNonNumericNominations(final Set<String> v) {
    nonNumericNominations.clear();
    nonNumericNominations.addAll(v);
  }

  /**
   * Load the scores for a team from the database.
   * 
   * @param connection the database connection
   * @param category the subjective score category to load the scores for
   * @param tournament the tournament
   * @param team the team
   * @return the scores found on the server, possibly an empty list
   * @throws SQLException if there is a problem talking to the database
   */
  @SuppressFBWarnings(value = "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING", justification = "Table name is determined by the category")
  public static Collection<SubjectiveScore> getScoresForTeam(final Connection connection,
                                                             final SubjectiveScoreCategory category,
                                                             final Tournament tournament,
                                                             final Team team)
      throws SQLException {
    final Collection<SubjectiveScore> scores = new LinkedList<>();

    try (PreparedStatement prep = connection.prepareStatement("SELECT * FROM "
        + category.getName()
        + " WHERE Tournament = ? AND teamnumber = ?")) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setInt(2, team.getTeamNumber());

      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final SubjectiveScore score = fromResultSet(connection, category, tournament, rs);

          scores.add(score);
        } // foreach result

      } // allocate result set
    } // allocate prep

    return scores;
  }

  /**
   * Populate a {@link SubjectiveScore} object from the specified database result.
   * One needs to select all columns from the category table for this method to
   * work properly.
   * 
   * @param connection database connection
   * @param category category
   * @param tournament the tournament
   * @param rs the database result to read from
   * @return a newly created object
   * @throws SQLException on a database error
   */
  public static SubjectiveScore fromResultSet(final Connection connection,
                                              final SubjectiveScoreCategory category,
                                              final Tournament tournament,
                                              final ResultSet rs)
      throws SQLException {
    final SubjectiveScore score = new SubjectiveScore();
    score.setScoreOnServer(true);

    final String judge = castNonNull(rs.getString("Judge"));

    score.setTeamNumber(rs.getInt("TeamNumber"));
    score.setJudge(judge);
    score.setNoShow(rs.getBoolean("NoShow"));
    score.setNote(rs.getString("note"));
    score.setCommentGreatJob(rs.getString("comment_great_job"));
    score.setCommentThinkAbout(rs.getString("comment_think_about"));

    final Map<String, Double> standardSubScores = new HashMap<>();
    final Map<String, String> enumSubScores = new HashMap<>();
    final Map<String, String> goalComments = new HashMap<>();
    for (final AbstractGoal goal : category.getAllGoals()) {
      if (goal.isEnumerated()) {
        final String value = rs.getString(goal.getName());
        if (null == value) {
          throw new FLLInternalException("Found enumerated goal '"
              + goal.getName()
              + "' with null value in the database");
        }
        enumSubScores.put(goal.getName(), value);
      } else {
        final double value = rs.getDouble(goal.getName());
        standardSubScores.put(goal.getName(), value);
      }

      final String commentColumn = GenerateDB.getGoalCommentColumnName(goal);
      final String comment = rs.getString(commentColumn);
      if (!StringUtils.isBlank(comment)) {
        goalComments.put(goal.getName(), comment);
      }
    } // foreach goal
    score.setStandardSubScores(standardSubScores);
    score.setEnumSubScores(enumSubScores);
    score.setGoalComments(goalComments);

    final Set<String> nominatedCategories = NonNumericNominees.getNomineesByJudgeForTeam(connection, tournament,
                                                                                         score.getJudge(),
                                                                                         score.getTeamNumber());
    score.setNonNumericNominations(nominatedCategories);
    return score;
  }

  /**
   * Get subjective scores for all teams in the specified award group and
   * category.
   * 
   * @param connection database connection
   * @param tournament
   * @param category the category to get the scores for
   * @param awardGroup the award group to get the scores for
   * @return the subjective scores
   * @throws SQLException on a database error
   */
  @SuppressFBWarnings(value = "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING", justification = "Table name is determined by the category")
  public static Collection<SubjectiveScore> getScoresForCategoryAndAwardGroup(final Connection connection,
                                                                              final Tournament tournament,
                                                                              final SubjectiveScoreCategory category,
                                                                              final String awardGroup)
      throws SQLException {
    final Collection<SubjectiveScore> scores = new LinkedList<>();

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
          final SubjectiveScore score = fromResultSet(connection, category, tournament, rs);

          scores.add(score);
        } // foreach result

      } // allocate result set
    } // allocate prep

    return scores;
  }

}
