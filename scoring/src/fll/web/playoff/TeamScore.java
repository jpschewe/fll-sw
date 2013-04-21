/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.playoff;

import fll.util.ScoreUtils;

/**
 * Represents a score for a team. Only the values of simple goals are available
 * through this object.
 */
public abstract class TeamScore {

  /**
   * Run number used for team scores that are not performance scores.
   */
  public static final int NON_PERFORMANCE_RUN_NUMBER = -1;

  public TeamScore(final int teamNumber) {
    this(teamNumber, NON_PERFORMANCE_RUN_NUMBER);
  }

  public TeamScore(final int teamNumber,
                   final int runNumber) {
    _teamNumber = teamNumber;
    _runNumber = runNumber;
  }

  /**
   * The team that this score is for.
   * 
   * @return the team
   */
  public final int getTeamNumber() {
    return _teamNumber;
  }

  private final int _teamNumber;

  /**
   * Check if the score exists. If it doesn't exist, the other score methods
   * will throw a RuntimeException
   * 
   * @return true if the score exists
   */
  public abstract boolean scoreExists();

  /**
   * Is this score a no show?
   * 
   * @return true if this score is a no show
   */
  public abstract boolean isNoShow();

  /**
   * What run do these scores apply to?
   * 
   * @return the run for the scores
   */
  public final int getRunNumber() {
    return _runNumber;
  }

  private final int _runNumber;

  /**
   * The raw score for a particular simple goal, as a double.
   * 
   * @param goalName the goal to get the score for
   * @return the score, NaN if there is no score for the specified name
   */
  public abstract double getRawScore(final String goalName);

  /**
   * The raw score for a particular enumerated goal, as a String.
   * 
   * @param goalName the goal to get the score for
   * @return the score, null if there is no score for the specified name
   */
  public abstract String getEnumRawScore(String goalName);

  /**
   * Cleanup any resources used. The object is no longer valid after a call to
   * cleanup.
   */
  public void cleanup() {
    // nothing by default
  }

}
