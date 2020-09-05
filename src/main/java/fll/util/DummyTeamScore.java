/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.util;

import java.util.HashMap;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

import fll.web.playoff.Playoff;
import fll.web.playoff.TeamScore;

/**
 * Team score that is populated from a map of values. This is most often used
 * for testing, but is also useful for
 * {@link Playoff#finishBracket(java.sql.Connection,
 * fll.xml.ChallengeDescription, fll.Tournament, String)}.
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
    this.simpleGoals = new HashMap<>(simpleGoals);
    this.enumGoals = new HashMap<>(enumGoals);
    this.noShow = noShow;
    this.bye = bye;
  }

  @Override
  public @Nullable String getEnumRawScore(final String goalName) {
    if (!scoreExists()) {
      return null;
    } else {
      if (enumGoals.containsKey(goalName)) {
        return enumGoals.get(goalName);
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
      if (simpleGoals.containsKey(goalName)) {
        return simpleGoals.get(goalName);
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

  private final Map<String, Double> simpleGoals;

  private final Map<String, String> enumGoals;
}
