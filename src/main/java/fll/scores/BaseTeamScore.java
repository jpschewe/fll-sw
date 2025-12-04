/*
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.scores;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 * Represents a score for a team.
 */
/* package */ abstract class BaseTeamScore implements TeamScore {

  /**
   * Create a non-performance TeamScore for the specified team.
   *
   * @param teamNumber {@link #getTeamNumber()}
   */
  BaseTeamScore(final int teamNumber) {
    this.teamNumber = teamNumber;
  }

  /**
   * The team that this score is for.
   *
   * @return the team
   */
  @Override
  @SideEffectFree
  public final int getTeamNumber() {
    return teamNumber;
  }

  private final int teamNumber;

  @Override
  @SideEffectFree
  public abstract boolean isNoShow();

  /**
   * Implemented to return {@link Double#NaN} if {@link #isNoShow()} returns
   * {@code true}, otherwise return the value.
   */
  @Override
  @SideEffectFree
  public double getRawScore(final String goalName) {
    if (isNoShow()) {
      return Double.NaN;
    } else {
      return internalGetRawScore(goalName);
    }
  }

  protected abstract double internalGetRawScore(String goalName);

  /**
   * Implemented to return {@code null} if {@link #isNoShow()} returns
   * {@code true}, otherwise return the value.
   */
  @Override
  @SideEffectFree
  public @Nullable String getEnumRawScore(final String goalName) {
    if (isNoShow()) {
      return null;
    } else {
      return internalGetEnumRawScore(goalName);
    }
  }

  protected abstract @Nullable String internalGetEnumRawScore(String goalName);

}
