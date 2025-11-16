/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.scores;

import java.util.HashMap;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Team score that is populated from a map of values.
 *
 * @author jpschewe
 */
/* package */ class DefaultTeamScore extends BaseTeamScore {

  /**
   * @param teamNumber the team number the score is for
   * @param simpleGoals the simple goal values
   * @param enumGoals the enum goal values
   * @param noShow true if this is a no show
   */
  public DefaultTeamScore(final int teamNumber,
                          final Map<String, Double> simpleGoals,
                          final Map<String, String> enumGoals,
                          final boolean noShow) {
    super(teamNumber);
    this.simpleGoals = new HashMap<>(simpleGoals);
    this.enumGoals = new HashMap<>(enumGoals);
    this.noShow = noShow;
  }

  @Override
  protected @Nullable String internalGetEnumRawScore(final String goalName) {
    if (enumGoals.containsKey(goalName)) {
      return enumGoals.get(goalName);
    } else {
      return null;
    }
  }

  @Override
  protected double internalGetRawScore(final String goalName) {
    if (simpleGoals.containsKey(goalName)) {
      return simpleGoals.get(goalName);
    } else {
      return Double.NaN;
    }
  }

  private final boolean noShow;

  @Override
  public boolean isNoShow() {
    return noShow;
  }

  private final Map<String, Double> simpleGoals;

  private final Map<String, String> enumGoals;

}
