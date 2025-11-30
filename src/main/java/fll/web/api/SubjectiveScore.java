/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.api;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.Team;
import fll.Tournament;
import fll.db.NonNumericNominees;
import fll.scores.DefaultSubjectiveTeamScore;
import fll.scores.SubjectiveTeamScore;
import fll.scores.TeamScore;
import fll.util.FLLInternalException;
import fll.xml.AbstractGoal;
import fll.xml.Goal;
import fll.xml.NonNumericCategory;
import fll.xml.SubjectiveScoreCategory;

/**
 * A subjective score from the database.
 * Primarily used to exchange data with the subjective web application.
 * This object allows both read and write, unlike the implementations of
 * {@link TeamScore}. Used by {@code subjective.js}.
 */
public final class SubjectiveScore {

  /**
   * Constructor that sets all default values.
   */
  private SubjectiveScore() {
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
   * @return unmodifiable set of {@link NonNumericCategory#getTitle()}
   */
  public Set<String> getNonNumericNominations() {
    return Collections.unmodifiableSet(nonNumericNominations);
  }

  /**
   * @param v see {@link #getNonNumericNominations()}
   */
  private void setNonNumericNominations(final Set<String> v) {
    nonNumericNominations.clear();
    nonNumericNominations.addAll(v);
  }

  /**
   * @param connection database
   * @param category the category to get scores for
   * @param tournament the tournament to get scores for
   * @return the scores
   * @throws SQLException on a database error
   */
  public static Collection<SubjectiveScore> getCategoryScores(final Connection connection,
                                                              final SubjectiveScoreCategory category,
                                                              final Tournament tournament)
      throws SQLException {
    final Collection<SubjectiveScore> scores = new LinkedList<>();

    for (final SubjectiveTeamScore dbScore : DefaultSubjectiveTeamScore.getScoresForCategory(connection, tournament,
                                                                                             category)) {
      final SubjectiveScore score = new SubjectiveScore();
      score.setScoreOnServer(true);

      final int teamNumber = dbScore.getTeamNumber();
      final String judge = dbScore.getJudge();

      score.setTeamNumber(teamNumber);
      score.setJudge(judge);
      score.setNoShow(dbScore.isNoShow());
      score.setNote(dbScore.getNote());
      score.setCommentGreatJob(dbScore.getCommentGreatJob());
      score.setCommentThinkAbout(dbScore.getCommentThinkAbout());

      final Map<String, Double> standardSubScores = new HashMap<>();
      final Map<String, String> enumSubScores = new HashMap<>();
      final Map<String, String> goalComments = new HashMap<>();
      for (final AbstractGoal goal : category.getAllGoals()) {
        if (goal.isEnumerated()) {
          final @Nullable String value = dbScore.getEnumRawScore(goal.getName());
          if (null == value) {
            throw new FLLInternalException("Found enumerated goal '"
                + goal.getName()
                + "' with null value in the database");
          }
          enumSubScores.put(goal.getName(), value);
        } else {
          final double value = dbScore.getRawScore(goal.getName());
          standardSubScores.put(goal.getName(), value);
        }

        final String comment = dbScore.getGoalComment(goal.getName());
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

      scores.add(score);
    }
    return scores;
  }

}
