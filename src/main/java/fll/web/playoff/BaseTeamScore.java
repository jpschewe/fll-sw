/*
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.playoff;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 * Represents a score for a team.
 */
public abstract class BaseTeamScore implements TeamScore {

  /**
   * Create a non-performance TeamScore for the specified team.
   *
   * @param teamNumber {@link #getTeamNumber()}
   */
  public BaseTeamScore(final int teamNumber) {
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
   * {@code true}, otherwise return {@link #internalGetRawScore(String)}.
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
   * {@code true}, otherwise return {@link #internalGetEnumRawScore(String)}.
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
