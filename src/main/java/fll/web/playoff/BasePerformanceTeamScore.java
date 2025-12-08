/*
 * This code is released under GPL; see LICENSE for details.
 */

package fll.web.playoff;

import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 * Base implementation of {@link PerformanceTeamScore}.
 */
public abstract class BasePerformanceTeamScore extends BaseTeamScore implements PerformanceTeamScore {

  /**
   * @param teamNumber see {@link #getTeamNumber()}
   * @param runNumber see {@link #getRunNumber()}
   */
  public BasePerformanceTeamScore(int teamNumber,
                                  int runNumber) {
    super(teamNumber);
    this.runNumber = runNumber;
  }

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

}
