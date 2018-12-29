/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

import java.util.HashMap;
import java.util.Map;

/**
 * A subjective score from the database.
 */
public class SubjectiveScore {

  public SubjectiveScore() {
    mNoShow = false;
    mModified = false;
    mDeleted = false;
    mJudge = null;
    mTeamNumber = Team.NULL_TEAM_NUMBER;
    mNote = null;
    mStandardSubScores = new HashMap<String, Double>();
    mEnumSubScores = new HashMap<String, String>();
    mScoreOnServer = false;
  }

  private boolean mScoreOnServer;

  public boolean getScoreOnServer() {
    return mScoreOnServer;
  }

  public void setScoreOnServer(final boolean v) {
    mScoreOnServer = v;
  }

  private boolean mNoShow;

  public boolean getNoShow() {
    return mNoShow;
  }

  public void setNoShow(final boolean v) {
    mNoShow = v;
  }

  private boolean mModified;

  /**
   * Has the score been modified since being pulled
   * from the database?
   */
  public boolean getModified() {
    return mModified;
  }

  public void setModified(final boolean v) {
    mModified = v;
  }

  private boolean mDeleted;

  /**
   * Should the score be deleted from the database?
   */
  public boolean getDeleted() {
    return mDeleted;
  }

  public void setDeleted(final boolean v) {
    mDeleted = v;
  }

  private String mJudge;

  /**
   * ID of the judge that created the scores.
   */
  public String getJudge() {
    return mJudge;
  }

  public void setJudge(final String v) {
    mJudge = v;
  }

  private int mTeamNumber;

  public int getTeamNumber() {
    return mTeamNumber;
  }

  public void setTeamNumber(final int v) {
    mTeamNumber = v;
  }

  private Map<String, Double> mStandardSubScores;

  public Map<String, Double> getStandardSubScores() {
    return mStandardSubScores;
  }

  public void setStandardSubScores(final Map<String, Double> v) {
    mStandardSubScores = v;
  }

  private Map<String, String> mEnumSubScores;

  public Map<String, String> getEnumSubScores() {
    return mEnumSubScores;
  }

  public void setEnumSubScores(final Map<String, String> v) {
    mEnumSubScores = v;
  }

  private String mNote;

  public String getNote() {
    return mNote;
  }

  public void setNote(final String v) {
    mNote = v;
  }

}
