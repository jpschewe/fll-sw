/*
 * Copyright (c) 2011 HighTechKids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.scheduler;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import fll.Utilities;

/**
 * Parameters for the scheduler.
 */
public class SchedParams implements Serializable {

  public static final int DEFAULT_TINC = 5;

  public static final int DEFAULT_MAX_HOURS = 8;

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
   * Create object with all default values.
   */
  public SchedParams() {
    
  }
  
  /**
   * Load the parameters from a properties object.
   * 
   * @param properies where to load the parameters from
   */
  public void load(final Properties properties) throws ParseException {
    final String subjDurationStr = properties.getProperty(GreedySolver.SUBJ_MINUTES_KEY);
    final int[] subjectiveDurations = Utilities.parseListOfIntegers(subjDurationStr);

    mPerformanceMinutes = Utilities.readIntProperty(properties, GreedySolver.ALPHA_PERF_MINUTES_KEY,
                                                    DEFAULT_PERFORMANCE_MINUTES);
    mChangetimeMinutes = Utilities.readIntProperty(properties, GreedySolver.CT_MINUTES_KEY, MINIMUM_CHANGETIME_MINUTES);
    mPerformanceChangetimeMinutes = Utilities.readIntProperty(properties, GreedySolver.PCT_MINUTES_KEY,
                                                              MINIMUM_PERFORMANCE_CHANGETIME_MINUTES);

    mSubjectiveStations = new ArrayList<>();
    for (int i = 0; i < subjectiveDurations.length; ++i) {
      final SubjectiveStation station = new SubjectiveStation(GreedySolver.getSubjectiveColumnName(i),
                                                              subjectiveDurations[i]);
      mSubjectiveStations.add(station);
    }
  }
  
  /**
   * Save this object to the specified properties object.
   * 
   * @param properties where to store the data
   */
  public void save(final Properties properties) {
    final int[] subjectiveDurations = new int[mSubjectiveStations.size()];
    for(int index =0; index<subjectiveDurations.length; ++index) {
      subjectiveDurations[index] = mSubjectiveStations.get(index).getDurationMinutes();
    }    
    properties.setProperty(GreedySolver.SUBJ_MINUTES_KEY, Arrays.toString(subjectiveDurations));
    
    properties.setProperty(GreedySolver.ALPHA_PERF_MINUTES_KEY, Integer.toString(mPerformanceMinutes));
    
    properties.setProperty(GreedySolver.CT_MINUTES_KEY, Integer.toString(mChangetimeMinutes));
    properties.setProperty(GreedySolver.PCT_MINUTES_KEY, Integer.toString(mPerformanceChangetimeMinutes));    
  }

  private int mPerformanceMinutes = DEFAULT_PERFORMANCE_MINUTES;

  /**
   * Number of minutes per performance run.
   * Defaults to {@link #DEFAULT_PERFORMANCE_MINUTES}
   */
  public final int getPerformanceMinutes() {
    return mPerformanceMinutes;
  }

  /**
   * @see #getPerformanceMinutes()
   */
  public final void setPerformanceMinutes(final int v) {
    mPerformanceMinutes = v;
  }

  private int mChangetimeMinutes = MINIMUM_CHANGETIME_MINUTES;

  /**
   * Number of minutes between judging stations for each team.
   * Default is {@link #MINIMUM_CHANGETIME_MINUTES}
   */
  public final int getChangetimeMinutes() {
    return mChangetimeMinutes;
  }

  /**
   * @see #getChangetimeMinutes()
   */
  public final void setChangetimeMinutes(final int v) {
    mChangetimeMinutes = v;
  }

  private int mPerformanceChangetimeMinutes = MINIMUM_PERFORMANCE_CHANGETIME_MINUTES;

  /**
   * Number of minutes between performance rounds for a team.
   * Default value is {@link #MINIMUM_PERFORMANCE_CHANGETIME_MINUTES}.
   */
  public final int getPerformanceChangetimeMinutes() {
    return mPerformanceChangetimeMinutes;
  }

  /**
   * @see #getPerformanceChangetimeMinutes()
   */
  public final void setPerformanceChangetimeMinutes(final int v) {
    mPerformanceChangetimeMinutes = v;
  }

  private ArrayList<SubjectiveStation> mSubjectiveStations = new ArrayList<>();

  /**
   * Number of subjective judging stations.
   * Defaults to 0.
   */
  public final int getNSubjective() {
    return mSubjectiveStations.size();
  }

  /**
   * Get the name of the specified station.
   * 
   * @param station an index into the list of stations
   * @return the name
   * @throws IndexOutOfBoundsException when the station is outside the bounds of
   *           the list
   */
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
