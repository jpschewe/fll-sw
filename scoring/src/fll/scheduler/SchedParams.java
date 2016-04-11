/*
 * Copyright (c) 2011 HighTechKids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.scheduler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import fll.Utilities;
import fll.util.FLLRuntimeException;

/**
 * Parameters for the scheduler.
 */
public class SchedParams implements Serializable {

  public static final int DEFAULT_TINC = 5;

  public static final int DEFAULT_MAX_HOURS = 8;

  public static final int DEFAULT_NSUBJECTIVE = 2;

  public static final int DEFAULT_NROUNDS = 3;

  public static final int DEFAULT_SUBJECTIVE_MINUTES = 20;

  public static final int DEFAULT_PERFORMANCE_MINUTES = 5;

  public static final int MINIMUM_CHANGETIME_MINUTES = 15;

  public static final int MINIMUM_PERFORMANCE_CHANGETIME_MINUTES = 30;

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

  /**
   * Load the parameters from a properties object.
   * 
   * @param properies where to load the parameters from
   */
  public SchedParams(final Properties properties) {    
    final int numSubjectiveStations = Utilities.readIntProperty(properties, GreedySolver.NSUBJECTIVE_KEY);
    final String subjDurationStr = properties.getProperty(GreedySolver.SUBJ_MINUTES_KEY);
    final int[] subjectiveDurations = Utilities.parseListOfIntegers(subjDurationStr);
    if (subjectiveDurations.length != numSubjectiveStations) {
      throw new FLLRuntimeException("Number of subjective stations not consistent with subj_minutes array size");
    }
    
    mPerformanceMinutes = Utilities.readIntProperty(properties, GreedySolver.ALPHA_PERF_MINUTES_KEY);
    mChangetimeMinutes = Utilities.readIntProperty(properties, GreedySolver.CT_MINUTES_KEY);
    mPerformanceChangetimeMinutes = Utilities.readIntProperty(properties, GreedySolver.PCT_MINUTES_KEY);
    
    mSubjectiveStations = new ArrayList<>();
    for(int i=0; i<numSubjectiveStations; ++i) {
      final SubjectiveStation station = new SubjectiveStation(GreedySolver.getSubjectiveColumnName(i), subjectiveDurations[i]);
      mSubjectiveStations.add(station);
    }
  }

  private int mPerformanceMinutes;

  /**
   * Number of minutes per performance run.
   */
  public final int getPerformanceMinutes() {
    return mPerformanceMinutes;
  }

  protected final void setPerformanceMinutes(final int v) {
    mPerformanceMinutes = v;
  }

  private int mChangetimeMinutes;

  /**
   * Number of minutes between judging stations for each team.
   */
  public final int getChangetimeMinutes() {
    return mChangetimeMinutes;
  }

  protected final void setChangetimeMinutes(final int v) {
    mChangetimeMinutes = v;
  }

  private int mPerformanceChangetimeMinutes;

  /**
   * Number of minutes between performance rounds for a team.
   */
  public final int getPerformanceChangetimeMinutes() {
    return mPerformanceChangetimeMinutes;
  }

  private ArrayList<SubjectiveStation> mSubjectiveStations;

  /**
   * Number of subjective judging stations.
   */
  public final int getNSubjective() {
    return mSubjectiveStations.size();
  }

  public final String getSubjectiveName(final int station) {
    return mSubjectiveStations.get(station).getName();
  }

  /**
   * @return Read-only copy of the subjective stations
   */
  public final List<SubjectiveStation> getSubjectiveStations() {
    return Collections.unmodifiableList(mSubjectiveStations);
  }

  /**
   * @param v new value for subjective stations
   */
  protected final void setSubjectiveStations(final List<SubjectiveStation> v) {
    mSubjectiveStations = new ArrayList<>(v);
  }

  /**
   * Number of minutes for a subjective judging.
   */
  public final int getSubjectiveMinutes(final int station) {
    return mSubjectiveStations.get(station).getDurationMinutes();
  }

  /**
   * @return the number of subjective stations
   */
  public final int getNumSubjectiveStations() {
    return mSubjectiveStations.size();
  }
  
  /**
   * Find a station by name.
   * 
   * @return the station, null if no station by name found
   */
  public final SubjectiveStation getStationByName(final String name) {
    for (final SubjectiveStation station : mSubjectiveStations) {
      if (station.getName().equals(name)) {
        return station;
      }
    }
    return null;
  }

}
