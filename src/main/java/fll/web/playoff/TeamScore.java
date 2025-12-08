/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.playoff;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 * Represents a score for a team.
 */
public interface TeamScore {

  /**
   * The team that this score is for.
   *
   * @return the team
   */
  @SideEffectFree
  int getTeamNumber();

  /**
   * Is this score a no show?
   *
   * @return true if this score is a no show
   */
  @SideEffectFree
  boolean isNoShow();

  /**
   * The raw score for a particular simple goal, as a double.
   *
   * @param goalName the goal to get the score for
   * @return the score, NaN if there is no score for the specified goal name
   */
  @SideEffectFree
  double getRawScore(String goalName);

  /**
   * The raw score for a particular enumerated goal, as a String.
   *
   * @param goalName the goal to get the score for
   * @return the score, null if there is no score for the specified goal name
   */
  @SideEffectFree
  @Nullable
  String getEnumRawScore(String goalName);

}
