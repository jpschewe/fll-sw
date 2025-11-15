/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.playoff;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 * Represents a score for a team.
 */
public abstract class TeamScore {

  /**
   * Run number used for team scores that are not performance scores.
   */
  public static final int NON_PERFORMANCE_RUN_NUMBER = -1;

  /**
   * Create a non-performance TeamScore for the specified team.
   *
   * @param teamNumber {@link #getTeamNumber()}
   */
  public TeamScore(final int teamNumber) {
    this(teamNumber, NON_PERFORMANCE_RUN_NUMBER);
  }

  /**
   * Create a performance TeamScore for the specified team and run number.
   *
   * @param teamNumber the team the score is for
   * @param runNumber 1-based run number
   */
  public TeamScore(final int teamNumber,
                   final int runNumber) {
    this.teamNumber = teamNumber;
    this.runNumber = runNumber;
  }

  /**
   * The team that this score is for.
   *
   * @return the team
   */
  @SideEffectFree
  public final int getTeamNumber(@UnknownInitialization(TeamScore.class) TeamScore this) {
    return teamNumber;
  }

  private final int teamNumber;

  /**
   * Check if the score exists. If it doesn't exist, the other score methods
   * will throw a RuntimeException
   *
   * @return true if the score exists
   */
  @SideEffectFree
  public abstract boolean scoreExists();

  /**
   * Is this score a no show?
   *
   * @return true if this score is a no show
   */
  @SideEffectFree
  public abstract boolean isNoShow();

  /**
   * Is this score a bye?
   *
   * @return true if this score is a no show
   */
  public abstract boolean isBye();

  /**
   * Is the score verified.
   * 
   * @return true if this score has been verified
   */
  @SideEffectFree
  public abstract boolean isVerified();

  /**
   * When the score is entered from a tablet where the table is known, this has
   * the table name, otherwise the value is "ALL" meaning that the person entering
   * the score has "all tables" selected.
   * 
   * @return the table that the score was entered from.
   */
  @SideEffectFree
  public abstract String getTable();

  /**
   * What run do these scores apply to?
   * This is a 1-based number. It will be {@link #NON_PERFORMANCE_RUN_NUMBER} if
   * this is not a performance score.
   *
   * @return the run for the scores
   */
  @SideEffectFree
  public final int getRunNumber(@UnknownInitialization(TeamScore.class) TeamScore this) {
    return runNumber;
  }

  private final int runNumber;

  /**
   * The raw score for a particular simple goal, as a double.
   *
   * @param goalName the goal to get the score for
   * @return the score, NaN if there is no score for the specified goal name or
   *         {@link #scoreExists()} returns {@code false}.
   */
  @SideEffectFree
  public final double getRawScore(String goalName) {
    if (!scoreExists()) {
      return Double.NaN;
    } else {
      return internalGetRawScore(goalName);
    }
  }

  /**
   * Implementation of {@link #getRawScore(String)}. The calling method has
   * already checked {@link #scoreExists()}.
   * 
   * @param goalName the goal to get the score for
   * @return the value or {@link Double#NaN} if there is no value for the goal
   */
  @SideEffectFree
  protected abstract double internalGetRawScore(final String goalName);

  /**
   * The raw score for a particular enumerated goal, as a String.
   *
   * @param goalName the goal to get the score for
   * @return the score, null if there is no score for the specified goal name or
   *         {@link #scoreExists()} returns {@code false}.
   */
  @SideEffectFree
  public final @Nullable String getEnumRawScore(String goalName) {
    if (!scoreExists()) {
      return null;
    } else {
      return internalGetEnumRawScore(goalName);
    }
  }

  /**
   * Implementation of {@link #getEnumRawScore(String)}. The calling method has
   * already checked {@link #scoreExists()}.
   * 
   * @param goalName the goal to get the score for
   * @return the value or {@code null} if there is no value for the goal
   */
  @SideEffectFree
  protected abstract @Nullable String internalGetEnumRawScore(final String goalName);

}
