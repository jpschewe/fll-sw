/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.playoff;

import fll.Team;

/**
 * Represents a performance score for a team and a particular run.
 * 
 * @author jpschewe
 * @version $Revision$
 *
 */
/*package*/ abstract class TeamScore {

  public TeamScore(final Team team, final int runNumber) {
    _team = team;
    _runNumber = runNumber;
  }
  
  /**
   * The team that this score is for.
   * 
   * @return the team
   */
  public final Team getTeam() {
    return _team;
  }
  private final Team _team;
  
  /**
   * Check if the score exists.  If it doesn't exist, the other score methods will throw a RuntimeException
   * 
   * @return true if the score exists
   */
  abstract boolean scoreExists();
  
  /**
   * Is this score a no show?
   * 
   * @return true if this score is a no show 
   */
  abstract boolean isNoShow();
  
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
   * The total score for the team
   * 
   * @return the total score
   */
  abstract int getTotalScore();
  
  /**
   * The score for a particular simple goal, as an int.
   * 
   * @param goalName the goal to get the score for
   * @return the score
   */
  abstract int getIntScore(String goalName);
  
  /**
   * The score for a particular enumerated goal, as a String.
   * 
   * @param goalName the goal to get the score for
   * @return the score
   */
  abstract String getEnumScore(String goalName);
  
  /**
   * Cleanup any resources used.  The object is no longer valid after a call to cleanup.
   */
  public void cleanup() {
    // nothing by default
  }
  
}
