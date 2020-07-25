/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import fll.xml.Goal;

/**
 * A subjective score from the database.
 * Primarily used to exchange data with the subjective web application.
 */
public class SubjectiveScore {

  public SubjectiveScore() {
    noShow = false;
    modified = false;
    deleted = false;
    judge = null;
    teamNumber = Team.NULL_TEAM_NUMBER;
    note = null;
    standardSubScores = new HashMap<String, Double>();
    enumSubScores = new HashMap<String, String>();
    scoreOnServer = false;
    commentGreatJob = null;
    commentThinkAbout = null;
    goalComments = new HashMap<String, String>();
  }

  private boolean scoreOnServer;

  /**
   * @return does the score exist on the server?
   */
  public boolean getScoreOnServer() {
    return scoreOnServer;
  }

  /**
   * @param v see {@Link #getScoreOnServer()}
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

  private @Nullable String judge;

  /**
   * @return ID of the judge that created the scores.
   */
  public @Nullable String getJudge() {
    return judge;
  }

  /**
   * @param v see {@Link #getJudge()}
   */
  public void setJudge(final @Nullable String v) {
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

  private final Map<String, Double> standardSubScores;

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

  private final Map<String, String> enumSubScores;

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
   * @param v see {@Link #getCommentGreatJob()}
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

  private final Map<String, String> goalComments;

  /**
   * The comments specific to each goal.
   * 
   * @return goal name, comment
   */
  public Map<String, String> getGoalComments() {
    return goalComments;
  }

  /**
   * @param v see {@link #getGoalComments()}
   */
  public void setGoalComments(final @Nullable Map<String, String> v) {
    goalComments.clear();
    if (null != v) {
      goalComments.putAll(v);
    }
  }

}
