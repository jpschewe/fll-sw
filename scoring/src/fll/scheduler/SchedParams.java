/*
 * Copyright (c) 2011 HighTechKids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.scheduler;

import java.util.ArrayList;
import java.util.List;

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
   * @param subjectiveParams the parameters for the subjective categories, one
   *          entry for each subjective category
   * @param performanceMinutes the number of minutes that the performance
   *          judging takes
   * @param changetimeMinutes the number of minutes between judging stations for
   *          a team
   * @param performanceChangetimeMinutes the number of minutes between runs on
   *          the performance table for a team
   */
  public SchedParams(final List<SubjectiveStation> subjectiveParams,
                     final int performanceMinutes,
                     final int changetimeMinutes,
                     final int performanceChangetimeMinutes) {
    mSubjectiveStations = new ArrayList<SubjectiveStation>(subjectiveParams);
    mPerformanceMinutes = performanceMinutes;
    mChangetimeMinutes = changetimeMinutes;
    mPerformanceChangetimeMinutes = performanceChangetimeMinutes;
  }

  private final int mPerformanceMinutes;

  /**
   * Number of minutes per performance run.
   */
  public int getPerformanceMinutes() {
    return mPerformanceMinutes;
  }

  private final int mChangetimeMinutes;

  /**
   * Number of minutes between judging stations for each team.
   */
  public int getChangetimeMinutes() {
    return mChangetimeMinutes;
  }

  private int mPerformanceChangetimeMinutes;

  /**
   * Number of minutes between performance rounds for a team.
   */
  public int getPerformanceChangetimeMinutes() {
    return mPerformanceChangetimeMinutes;
  }

  private final List<SubjectiveStation> mSubjectiveStations;

  /**
   * Number of subjective judging stations.
   */
  public int getNSubjective() {
    return mSubjectiveStations.size();
  }

  public String getSubjectiveName(final int station) {
    return mSubjectiveStations.get(station).getName();
  }

  /**
   * Number of minutes for a subjective judging.
   */
  public long getSubjectiveMinutes(final int station) {
    return mSubjectiveStations.get(station).getDurationMinutes();
  }

  /**
   * Find a station by name.
   * 
   * @return the station, null if no station by name found
   */
  public SubjectiveStation getStationByName(final String name) {
    for (final SubjectiveStation station : mSubjectiveStations) {
      if (station.getName().equals(name)) {
        return station;
      }
    }
    return null;
  }

}
