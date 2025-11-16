/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.scores;

import java.util.Map;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Team score that is populated from a map of values.
 *
 * @author jpschewe
 */
public class DefaultPerformanceTeamScore extends BasePerformanceTeamScore {

  /**
   * Sets {@link #isNoShow()} and {@link #isBye()} to {@code false} and
   * {@link #getTable()} to {@link PerformanceTeamScore#ALL_TABLE} and
   * {@link #isVerified()} to {@code true}
   * 
   * @param teamNumber the team number the score is for
   * @param runNumber the run number the score is for
   * @param simpleGoals the simple goal values
   * @param enumGoals the enum goal values
   */
  public DefaultPerformanceTeamScore(final int teamNumber,
                                     final int runNumber,
                                     final Map<String, Double> simpleGoals,
                                     final Map<String, String> enumGoals) {
    this(teamNumber, runNumber, simpleGoals, enumGoals, PerformanceTeamScore.ALL_TABLE, false, false, true);
  }

  /**
   * @param teamNumber see {@link #getTeamNumber()}
   * @param runNumber see {@link #getRunNumber()}
   * @param simpleGoals the simple goal values
   * @param enumGoals the enum goal values
   * @param noShow see {@link #isNoShow()}
   * @param bye see {@link #isBye()}
   * @param tablename see {@link #getTable()}
   * @param verified see {@link #isVerified()}
   */
  public DefaultPerformanceTeamScore(final int teamNumber,
                                     final int runNumber,
                                     final Map<String, Double> simpleGoals,
                                     final Map<String, String> enumGoals,
                                     final String tablename,
                                     final boolean noShow,
                                     final boolean bye,
                                     final boolean verified) {
    super(teamNumber, runNumber);
    this.delegate = new DefaultTeamScore(teamNumber, simpleGoals, enumGoals, noShow);
    this.bye = bye;
    this.tablename = tablename;
    this.verified = verified;
  }

  private final DefaultTeamScore delegate;

  @Override
  protected @Nullable String internalGetEnumRawScore(final String goalName) {
    return delegate.getEnumRawScore(goalName);
  }

  @Override
  protected double internalGetRawScore(final String goalName) {
    return delegate.getRawScore(goalName);
  }

  @Override
  public boolean isNoShow() {
    return delegate.isNoShow();
  }

  private final boolean bye;

  @Override
  public boolean isBye() {
    return bye;
  }

  private final boolean verified;

  @Override
  public boolean isVerified() {
    return verified;
  }

  private final String tablename;

  @Override
  public String getTable() {
    return tablename;
  }
}
