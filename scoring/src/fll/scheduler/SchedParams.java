/*
 * Copyright (c) 2011 HighTechKids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fll.Utilities;

/**
 * Parameters for the scheduler.
 */
public class SchedParams {

  public static final int DEFAULT_TINC = 5;

  public static final int DEFAULT_MAX_HOURS = 8;

  public static final int DEFAULT_NSUBJECTIVE = 2;

  public static final int DEFAULT_NROUNDS = 3;

  public static final int DEFAULT_SUBJECTIVE_MINUTES = 20;

  public static final int DEFAULT_PERFORMANCE_MINUTES = 5;

  public static final int DEFAULT_CHANGETIME_MINUTES = 15;

  public static final int DEFAULT_PERFORMANCE_CHANGETIME_MINUTES = 45;

  /**
   * @param tinc the number of minutes per time slot
   * @param tmax the number of hours that the tournament should go
   * @param nsubjective the number of subjective rounds
   * @param nrounds the number of performance rounds
   * @param ntables the number of performance tables
   * @param subjectiveMinutes the number of minutes that the subjective judging
   *          takes
   * @param performanceMinutes the number of minutes that the performance
   *          judging takes
   * @param changetimeMinutes the number of minutes between judging stations for
   *          a team
   * @param performanceChangetimeMinutes the number of minutes between runs on
   *          the performance table for a team
   * @param teams a list containing the number of teams in each judging group
   * @throws InconsistentSchedParams if one of the specified times isn't a
   *           multiple of tinc
   */
  public SchedParams(final int tinc,
                     final int maxHours,
                     final List<SubjectiveParams> subjectiveParams,
                     final int nrounds,
                     final int ntables,
                     final int performanceMinutes,
                     final int changetimeMinutes,
                     final int performanceChangetimeMinutes,
                     final List<Integer> teams) throws InconsistentSchedParams {
    mTInc = tinc;
    mMaxHours = maxHours;
    mSubjectiveParams = new ArrayList<SubjectiveParams>(subjectiveParams);
    mNRounds = nrounds;
    mNTables = ntables;
    mPerformanceMinutes = performanceMinutes;
    mChangetimeMinutes = changetimeMinutes;
    mPerformanceChangetimeMinutes = performanceChangetimeMinutes;
    mTeams = new ArrayList<Integer>(teams);

    assertConsistentParameters();
  }

  /**
   * Check internal parameters.
   */
  private void assertConsistentParameters() throws InconsistentSchedParams {
    if (getMaxTimeSlots()
        * getTInc() != Utilities.convertHoursToMinutes(getMaxHours())) {
      throw new InconsistentSchedParams("Max Hours of "
          + getMaxHours() + " is not a multiple of TInc " + getTInc());
    }

    for (int station = 0; station < getNSubjective(); ++station) {
      if (getSubjectiveTimeSlots(station)
          * getTInc() != getSubjectiveMinutes(station)) {
        throw new InconsistentSchedParams("Subjective minutes of "
            + getSubjectiveMinutes(station) + " is not a multiple of TInc " + getTInc() + " for station "
            + getSubjectiveName(station));
      }
    }

    if (getPerformanceTimeSlots()
        * getTInc() != getPerformanceMinutes()) {
      throw new InconsistentSchedParams("Performance minutes of "
          + getPerformanceMinutes() + " is not a multiple of TInc " + getTInc());
    }

    if (getChangetimeSlots()
        * getTInc() != getChangetimeMinutes()) {
      throw new InconsistentSchedParams("Changetime minutes of "
          + getChangetimeMinutes() + " is not a multiple of TInc " + getTInc());
    }

    if (getPerformanceChangetimeSlots()
        * getTInc() != getPerformanceChangetimeMinutes()) {
      throw new InconsistentSchedParams("Performance Changetime minutes of "
          + getPerformanceChangetimeMinutes() + " is not a multiple of TInc " + getTInc());
    }

  }

  private final int mTInc;

  /**
   * Number of minutes per time slot.
   */
  public int getTInc() {
    return mTInc;
  }

  private final int mMaxHours;

  /**
   * Number of minutes per time slot.
   */
  public int getMaxHours() {
    return mMaxHours;
  }

  public int getMaxTimeSlots() {
    return Utilities.convertHoursToMinutes(getMaxHours())
        / getTInc();
  }

  private final int mNRounds;

  /**
   * Number of performance rounds.
   */
  public int getNRounds() {
    return mNRounds;
  }

  private final int mNTables;

  /**
   * Number of tables.
   */
  public int getNTables() {
    return mNTables;
  }

  private final int mPerformanceMinutes;

  /**
   * Number of minutes per performance run.
   */
  public int getPerformanceMinutes() {
    return mPerformanceMinutes;
  }

  public int getPerformanceTimeSlots() {
    return getPerformanceMinutes()
        / getTInc();
  }

  private final int mChangetimeMinutes;

  /**
   * Number of minutes between judging stations for each team.
   */
  public int getChangetimeMinutes() {
    return mChangetimeMinutes;
  }

  public int getChangetimeSlots() {
    return getChangetimeMinutes()
        / getTInc();
  }

  private int mPerformanceChangetimeMinutes;

  /**
   * Number of minutes between performance rounds for a team.
   */
  public int getPerformanceChangetimeMinutes() {
    return mPerformanceChangetimeMinutes;
  }

  public int getPerformanceChangetimeSlots() {
    return getPerformanceChangetimeMinutes()
        / getTInc();
  }

  private final List<Integer> mTeams;

  /**
   * The number of teams for each judging group.
   * 
   * @return unmodifiable list
   */
  public List<Integer> getTeams() {
    return Collections.unmodifiableList(mTeams);
  }

  private final List<SubjectiveParams> mSubjectiveParams;

  /**
   * Number of subjective judging stations.
   */
  public int getNSubjective() {
    return mSubjectiveParams.size();
  }

  public String getSubjectiveName(final int station) {
    return mSubjectiveParams.get(station).getName();
  }

  /**
   * Number of minutes for a subjective judging.
   */
  public int getSubjectiveMinutes(final int station) {
    return mSubjectiveParams.get(station).getDurationMinutes();
  }

  public int getSubjectiveTimeSlots(final int station) {
    return getSubjectiveMinutes(station)
        / getTInc();
  }

  /**
   * Parameters for a subjective judging station.
   */
  public static final class SubjectiveParams {
    public SubjectiveParams(final String name,
                            final int durationMinutes) {
      mName = name;
      mDurationMinutes = durationMinutes;
    }

    public String getName() {
      return mName;
    }

    private final String mName;

    public int getDurationMinutes() {
      return mDurationMinutes;
    }

    private final int mDurationMinutes;
  }

}
