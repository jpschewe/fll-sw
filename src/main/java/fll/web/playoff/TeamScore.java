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
   * Check if the score exists. If it doesn't exist, the other score methods
   * will throw a RuntimeException
   *
   * @return true if the score exists
   */
  @SideEffectFree
  boolean scoreExists();

  /**
   * Is this score a no show?
   *
   * @return true if this score is a no show
   */
  @SideEffectFree
  boolean isNoShow();

  /**
   * Is this score a bye?
   *
   * @return true if this score is a no show
   */
  boolean isBye();

  /**
   * Is the score verified.
   * 
   * @return true if this score has been verified
   */
  @SideEffectFree
  boolean isVerified();

  /**
   * When the score is entered from a tablet where the table is known, this has
   * the table name, otherwise the value is "ALL" meaning that the person entering
   * the score has "all tables" selected.
   * 
   * @return the table that the score was entered from.
   */
  @SideEffectFree
  String getTable();

  /**
   * What run do these scores apply to?
   * This is a 1-based number. It will be {@link #NON_PERFORMANCE_RUN_NUMBER} if
   * this is not a performance score.
   *
   * @return the run for the scores
   */
  @SideEffectFree
  int getRunNumber();

  /**
   * The raw score for a particular simple goal, as a double.
   *
   * @param goalName the goal to get the score for
   * @return the score, NaN if there is no score for the specified goal name or
   *         {@link #scoreExists()} returns {@code false}.
   */
  @SideEffectFree
  double getRawScore(String goalName);

  /**
   * The raw score for a particular enumerated goal, as a String.
   *
   * @param goalName the goal to get the score for
   * @return the score, null if there is no score for the specified goal name or
   *         {@link #scoreExists()} returns {@code false}.
   */
  @SideEffectFree
  @Nullable
  String getEnumRawScore(String goalName);

}
