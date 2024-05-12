/*
 * Copyright (c) 2011 HighTechKids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.scheduler;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.checkerframework.checker.nullness.qual.Nullable;

import fll.Tournament;
import fll.Utilities;

/**
 * Parameters for the scheduler.
 */
public class SchedParams implements Serializable {

  private static final String SUBJ_MINUTES_KEY = "subj_minutes";

  private static final String SUBJ_NAMES_KEY = "subj_names";

  private static final String ALPHA_PERF_MINUTES_KEY = "alpha_perf_minutes";

  private static final String CT_MINUTES_KEY = "ct_minutes";

  private static final String PCT_MINUTES_KEY = "pct_minutes";

  private static final String SUBJECTIVE_CHANGETIME_MINUTES_KEY = "subjective_changetime_minutes";

  /**
   * Default number of minutes for a subjective judging session.
   */
  public static final int DEFAULT_SUBJECTIVE_MINUTES = 20;

  /**
   * Default number of minutes for a performance session.
   */
  public static final int DEFAULT_PERFORMANCE_MINUTES = 5;

  /**
   * Default number of minutes for a team to get subjective to performance.
   */
  public static final int DEFAULT_CHANGETIME_MINUTES = 15;

  /**
   * Default number of minutes for a team between subjective stations.
   */
  public static final int DEFAULT_SUBJECTIVE_CHANGETIME_MINUTES = 15;

  /**
   * Default number of minutes that a team should have between performance
   * sessions.
   */
  public static final int DEFAULT_PERFORMANCE_CHANGETIME_MINUTES = 30;

  /**
   * Thrown when the parameters are not valid.
   */
  public static class InvalidParametersException extends Exception {
    /**
     * @param errors the errors, typically from {@link SchedParams#isValid()}
     */
    public InvalidParametersException(final List<String> errors) {
      super("Parameters are invalid:\n"
          + String.join("\n", errors));
    }
  }

  /**
   * Create object with all default values.
   */
  public SchedParams() {

  }

  /**
   * Load the parameters from a properties object.
   * 
   * @param properties where to load the parameters from
   * @throws ParseException if there is an error parsing the properties
   */
  public void load(final Properties properties) throws ParseException {
    final String subjDurationStr = properties.getProperty(SUBJ_MINUTES_KEY);
    final int[] subjectiveDurations = Utilities.parseListOfIntegers(subjDurationStr);

    final String subjectiveNamesStr = properties.getProperty(SUBJ_NAMES_KEY, null);
    final String[] subjectiveNames = Utilities.parseListOfStrings(subjectiveNamesStr);

    mPerformanceMinutes = Utilities.readIntProperty(properties, ALPHA_PERF_MINUTES_KEY, DEFAULT_PERFORMANCE_MINUTES);
    mChangetimeMinutes = Utilities.readIntProperty(properties, CT_MINUTES_KEY, DEFAULT_CHANGETIME_MINUTES);
    mPerformanceChangetimeMinutes = Utilities.readIntProperty(properties, PCT_MINUTES_KEY,
                                                              DEFAULT_PERFORMANCE_CHANGETIME_MINUTES);
    mSubjectiveChangetimeMinutes = Utilities.readIntProperty(properties, SUBJECTIVE_CHANGETIME_MINUTES_KEY,
                                                             DEFAULT_SUBJECTIVE_CHANGETIME_MINUTES);

    mSubjectiveStations = new ArrayList<>();
    for (int i = 0; i < subjectiveDurations.length; ++i) {
      final String name;
      if (i < subjectiveNames.length) {
        name = subjectiveNames[i];
      } else {
        name = GreedySolver.getSubjectiveColumnName(i);
      }
      final SubjectiveStation station = new SubjectiveStation(name, subjectiveDurations[i]);
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
    final String[] subjectiveNames = new String[mSubjectiveStations.size()];
    for (int index = 0; index < subjectiveDurations.length; ++index) {
      final SubjectiveStation station = mSubjectiveStations.get(index);
      subjectiveDurations[index] = station.getDurationMinutes();
      subjectiveNames[index] = station.getName();
    }
    properties.setProperty(SUBJ_MINUTES_KEY, Arrays.toString(subjectiveDurations));
    properties.setProperty(SUBJ_NAMES_KEY, Arrays.toString(subjectiveNames));

    properties.setProperty(ALPHA_PERF_MINUTES_KEY, Integer.toString(mPerformanceMinutes));

    properties.setProperty(CT_MINUTES_KEY, Integer.toString(mChangetimeMinutes));
    properties.setProperty(PCT_MINUTES_KEY, Integer.toString(mPerformanceChangetimeMinutes));
    properties.setProperty(SUBJECTIVE_CHANGETIME_MINUTES_KEY, Integer.toString(mSubjectiveChangetimeMinutes));
  }

  private int mPerformanceMinutes = DEFAULT_PERFORMANCE_MINUTES;

  /**
   * @return Number of minutes per performance run. This is how long the team is
   *         expected to be at the table.
   *         Defaults to {@link #DEFAULT_PERFORMANCE_MINUTES}
   */
  public final int getPerformanceMinutes() {
    return mPerformanceMinutes;
  }

  /**
   * @param v see {@link #getPerformanceMinutes()}
   */
  public final void setPerformanceMinutes(final int v) {
    mPerformanceMinutes = v;
  }

  private int mChangetimeMinutes = DEFAULT_CHANGETIME_MINUTES;

  /**
   * @return Number of minutes between performance and subjective events for each
   *         team.
   *         Default is {@link #DEFAULT_CHANGETIME_MINUTES}
   */
  public final int getChangetimeMinutes() {
    return mChangetimeMinutes;
  }

  /**
   * @param v see {@link #getChangetimeMinutes()}
   */
  public final void setChangetimeMinutes(final int v) {
    mChangetimeMinutes = v;
  }

  private int mSubjectiveChangetimeMinutes = DEFAULT_SUBJECTIVE_CHANGETIME_MINUTES;

  /**
   * @return Number of minutes between subjective events for each team.
   *         Default is {@link #DEFAULT_CHANGETIME_MINUTES}
   */
  public final int getSubjectiveChangetimeMinutes() {
    return mSubjectiveChangetimeMinutes;
  }

  /**
   * @param v see {@link #getChangetimeMinutes()}
   */
  public final void setSubjectiveChangetimeMinutes(final int v) {
    mSubjectiveChangetimeMinutes = v;
  }

  private int mPerformanceChangetimeMinutes = DEFAULT_PERFORMANCE_CHANGETIME_MINUTES;

  /**
   * @return Number of minutes between performance rounds for a team.
   *         Default value is {@link #DEFAULT_PERFORMANCE_CHANGETIME_MINUTES}.
   */
  public final int getPerformanceChangetimeMinutes() {
    return mPerformanceChangetimeMinutes;
  }

  /**
   * @param v see {@link #getPerformanceChangetimeMinutes()}
   */
  public final void setPerformanceChangetimeMinutes(final int v) {
    mPerformanceChangetimeMinutes = v;
  }

  private ArrayList<SubjectiveStation> mSubjectiveStations = new ArrayList<>();

  /**
   * @return Number of subjective judging stations.
   *         Defaults to 0.
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
  public final void setSubjectiveStations(final List<SubjectiveStation> v) {
    if (null == v) {
      mSubjectiveStations = new ArrayList<>();
    } else {
      mSubjectiveStations = new ArrayList<>(v);
    }
  }

  /**
   * @param station the subjective station to get the time for
   * @return Number of minutes for a subjective judging.
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
   * @param name thename of the station to find
   * @return the station, null if no station by name found
   */
  public final @Nullable SubjectiveStation getStationByName(final String name) {
    for (final SubjectiveStation station : mSubjectiveStations) {
      if (station.getName().equals(name)) {
        return station;
      }
    }
    return null;
  }

  /**
   * Check if the parameters are valid.
   * 
   * @return a list of errors, the list is empty if the parameters are valid
   */
  public List<String> isValid() {
    final List<String> errors = new LinkedList<>();

    if (getChangetimeMinutes() < 0) {
      errors.add("Change time between events must be a non-negative value");
    }

    if (getPerformanceChangetimeMinutes() < 0) {
      errors.add("Change time between performance rounds must be a non-negative value");
    }

    return errors;
  }

  /**
   * Store the schedule parameters to the database.
   * 
   * @param connection database connection
   * @param tournament the tournament to store the parameters for
   * @throws SQLException on a database error
   */
  public void save(final Connection connection,
                   final Tournament tournament)
      throws SQLException {
    try (
        PreparedStatement delete = connection.prepareStatement("DELETE FROM sched_durations WHERE tournament_id = ?")) {
      delete.setInt(1, tournament.getTournamentID());
      delete.executeUpdate();
    }

    try (
        PreparedStatement insert = connection.prepareStatement("INSERT INTO sched_durations (tournament_id, key, duration_minutes) VALUES(?, ?, ?)")) {
      insert.setInt(1, tournament.getTournamentID());

      insert.setString(2, ALPHA_PERF_MINUTES_KEY);
      insert.setInt(3, mPerformanceMinutes);
      insert.executeUpdate();

      insert.setString(2, CT_MINUTES_KEY);
      insert.setInt(3, mChangetimeMinutes);
      insert.executeUpdate();

      insert.setString(2, PCT_MINUTES_KEY);
      insert.setInt(3, mPerformanceChangetimeMinutes);
      insert.executeUpdate();

      insert.setString(2, SUBJECTIVE_CHANGETIME_MINUTES_KEY);
      insert.setInt(3, mSubjectiveChangetimeMinutes);
      insert.executeUpdate();

      for (final SubjectiveStation station : mSubjectiveStations) {
        insert.setString(2, station.getName());
        insert.setInt(3, station.getDurationMinutes());
        insert.executeUpdate();
      }
    }
  }

  /**
   * Load the schedule parameters from the database.
   * 
   * @param connection database connection
   * @param tournament the tournament that the parameters are for
   * @param schedule used to get the subjective station names
   * @throws SQLException on a database error
   */
  public void load(final Connection connection,
                   final Tournament tournament,
                   final TournamentSchedule schedule)
      throws SQLException {
    try (
        PreparedStatement select = connection.prepareStatement("SELECT duration_minutes FROM sched_durations WHERE tournament_id = ? and key = ?")) {
      select.setInt(1, tournament.getTournamentID());

      select.setString(2, ALPHA_PERF_MINUTES_KEY);
      try (ResultSet rs = select.executeQuery()) {
        if (rs.next()) {
          mPerformanceMinutes = rs.getInt(1);
        } else {
          mPerformanceMinutes = DEFAULT_PERFORMANCE_MINUTES;
        }
      }

      select.setString(2, CT_MINUTES_KEY);
      try (ResultSet rs = select.executeQuery()) {
        if (rs.next()) {
          mChangetimeMinutes = rs.getInt(1);
        } else {
          mChangetimeMinutes = DEFAULT_CHANGETIME_MINUTES;
        }
      }

      select.setString(2, PCT_MINUTES_KEY);
      try (ResultSet rs = select.executeQuery()) {
        if (rs.next()) {
          mPerformanceChangetimeMinutes = rs.getInt(1);
        } else {
          mPerformanceChangetimeMinutes = DEFAULT_PERFORMANCE_CHANGETIME_MINUTES;
        }
      }

      select.setString(2, SUBJECTIVE_CHANGETIME_MINUTES_KEY);
      try (ResultSet rs = select.executeQuery()) {
        if (rs.next()) {
          mSubjectiveChangetimeMinutes = rs.getInt(1);
        } else {
          mSubjectiveChangetimeMinutes = DEFAULT_SUBJECTIVE_CHANGETIME_MINUTES;
        }
      }

      // how to get the list of subjective stations?
      mSubjectiveStations = new ArrayList<>();
      for (final String stationName : schedule.getSubjectiveStations()) {
        select.setString(2, stationName);
        try (ResultSet rs = select.executeQuery()) {
          final int duration;
          if (rs.next()) {
            duration = rs.getInt(1);
          } else {
            duration = DEFAULT_SUBJECTIVE_MINUTES;
          }
          final SubjectiveStation station = new SubjectiveStation(stationName, duration);
          mSubjectiveStations.add(station);
        }
      }
    }
  }
}
