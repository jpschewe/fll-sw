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
   * Run number used for team scores that are not performance scores.
   */
  public static final int NON_PERFORMANCE_RUN_NUMBER = -1;

  /**
   * Create a non-performance TeamScore for the specified team.
   *
   * @param teamNumber {@link #getTeamNumber()}
   */
  public BaseTeamScore(final int teamNumber) {
    this(teamNumber, NON_PERFORMANCE_RUN_NUMBER);
  }

  /**
   * Create a performance TeamScore for the specified team and run number.
   *
   * @param teamNumber the team the score is for
   * @param runNumber 1-based run number
   */
  public BaseTeamScore(final int teamNumber,
                       final int runNumber) {
    this.teamNumber = teamNumber;
    this.runNumber = runNumber;
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
  public abstract boolean isBye();

  @Override
  @SideEffectFree
  public abstract boolean isVerified();

  @Override
  @SideEffectFree
  public abstract String getTable();

  @Override
  @SideEffectFree
  public final int getRunNumber() {
    return runNumber;
  }

  private final int runNumber;

  @Override
  @SideEffectFree
  public abstract double getRawScore(String goalName);

  @Override
  @SideEffectFree
  public abstract @Nullable String getEnumRawScore(String goalName);

}
