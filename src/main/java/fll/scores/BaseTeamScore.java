/*
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.scores;

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

  @Override
  @SideEffectFree
  public abstract double getRawScore(String goalName);

  @Override
  @SideEffectFree
  public abstract @Nullable String getEnumRawScore(String goalName);

}
