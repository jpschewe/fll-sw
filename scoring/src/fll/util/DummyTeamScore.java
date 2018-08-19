/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.util;

import java.util.HashMap;
import java.util.Map;

import fll.web.playoff.Playoff;
import fll.web.playoff.TeamScore;

/**
 * Team score that is populated from a map of values. This is most often used
 * for testing, but is also useful for
 * {@link Playoff#finishBracket(java.sql.Connection, fll.xml.ChallengeDescription, int, String)}.
 * 
 * @author jpschewe
 */
public class DummyTeamScore extends TeamScore {

  /**
   * @param teamNumber the team number the score is for
   * @param runNumber the run number the score is for
   * @param simpleGoals the simple goal values
   * @param enumGoals the enum goal values
   */
  public DummyTeamScore(final int teamNumber,
                        final int runNumber,
                        final Map<String, Double> simpleGoals,
                        final Map<String, String> enumGoals) {
    this(teamNumber, runNumber, simpleGoals, enumGoals, false, false);
  }

  /**
   * @param teamNumber the team number the score is for
   * @param runNumber the run number the score is for
   * @param simpleGoals the simple goal values
   * @param enumGoals the enum goal values
   * @param noShow true if this is a no show
   * @param bye true if this is a bye
   */
  public DummyTeamScore(final int teamNumber,
                        final int runNumber,
                        final Map<String, Double> simpleGoals,
                        final Map<String, String> enumGoals,
                        final boolean noShow,
                        final boolean bye) {
    super(teamNumber, runNumber);
    _simpleGoals = new HashMap<String, Double>(simpleGoals);
    _enumGoals = new HashMap<String, String>(enumGoals);
    this.noShow = noShow;
    this.bye = bye;
  }

  @Override
  public String getEnumRawScore(final String goalName) {
    if (!scoreExists()) {
      return null;
    } else {
      if (_enumGoals.containsKey(goalName)) {
        return _enumGoals.get(goalName);
      } else {
        return null;
      }
    }
  }

  @Override
  public double getRawScore(final String goalName) {
    if (!scoreExists()) {
      return Double.NaN;
    } else {
      if (_simpleGoals.containsKey(goalName)) {
        return _simpleGoals.get(goalName);
      } else {
        return Double.NaN;
      }
    }
  }

  private final boolean noShow;

  @Override
  public boolean isNoShow() {
    return noShow;
  }

  private final boolean bye;

  @Override
  public boolean isBye() {
    return bye;
  }

  @Override
  public boolean scoreExists() {
    return true;
  }

  private final Map<String, Double> _simpleGoals;

  private final Map<String, String> _enumGoals;
}
