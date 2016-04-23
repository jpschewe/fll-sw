/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.Properties;

import org.apache.log4j.Logger;

import fll.Utilities;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;

/**
 * Parameters for {@link GreedySolver}.
 */
public class SolverParams extends SchedParams {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Create the object with all default values.
   */
  public SolverParams() {
  }
  
  /**
   * Populate the parameters from the properties object.
   * 
   * @param properties
   * @throws ParseException if there is a problem parsing the properties
   */
  @Override
  public void load(Properties properties) throws ParseException {
    super.load(properties);

    this.startTime = TournamentSchedule.parseDate(properties.getProperty(GreedySolver.START_TIME_KEY,
                                                                         startTime.toString()));

    this.tinc = Utilities.readIntProperty(properties, GreedySolver.TINC_KEY, this.tinc);

    final String groupCountsStr = properties.getProperty(GreedySolver.GROUP_COUNTS_KEY);
    this.groupCounts = Utilities.parseListOfIntegers(groupCountsStr);

    this.alternate = Utilities.readBooleanProperty(properties, GreedySolver.ALTERNATE_TABLES_KEY, false);
    LOGGER.debug("Alternate is: "
        + alternate);

    this.numPerformanceRounds = Utilities.readIntProperty(properties, GreedySolver.NROUNDS_KEY,
                                                          this.numPerformanceRounds);

    this.subjectiveFirst = Utilities.readBooleanProperty(properties, GreedySolver.SUBJECTIVE_FIRST_KEY, false);
    LOGGER.debug("Subjective first is: "
        + this.subjectiveFirst);

    this.perfAttemptOffsetMinutes = Utilities.readIntProperty(properties, GreedySolver.PERF_ATTEMPT_OFFSET_MINUTES_KEY,
                                                              this.tinc);

    this.subjectiveAttemptOffsetMinutes = Utilities.readIntProperty(properties,
                                                                    GreedySolver.SUBJECTIVE_ATTEMPT_OFFSET_MINUTES_KEY,
                                                                    this.tinc);

    this.numTables = Utilities.readIntProperty(properties, GreedySolver.NTABLES_KEY, this.numTables);

    this.tmaxHours = Utilities.readIntProperty(properties, GreedySolver.TMAX_HOURS_KEY);
    this.tmaxMinutes = Utilities.readIntProperty(properties, GreedySolver.TMAX_MINUTES_KEY);

    parseBreaks(properties, getStartTime(), getTimeIncrement());

  }

  // TODO: replace with LocalTime type
  private Date startTime = new Date(1970, 1, 1, 8, 0);

  /**
   * The start time of the tournament. Nothing is scheduled before this time.
   * Defaults to 8:00.
   */
  public final Date getStartTime() {
    return null == startTime ? null : new Date(startTime.getTime());
  }

  /**
   * @see #getStartTime()
   */
  public final void setStartTime(final Date v) {
    this.startTime = null == v ? null : new Date(v.getTime());
  }

  private int tinc = 1;

  /**
   * The number of minutes to use as the base time
   * type. Normally this is set to 1, however if all activities
   * should be scheduled on 5 minute intervals, then setting this to 5 would
   * ensure that and speed up the solver. All specified time intervals must
   * be even divisible by this number.
   * Defaults to 1.
   */
  public final int getTimeIncrement() {
    return tinc;
  }

  /**
   * @see #getTimeIncrement()
   */
  public final void setTimeIncrement(int v) {
    tinc = v;
  }

  private int[] groupCounts = new int[0];

  /**
   * Set the number of teams in each grouping.
   * This also defines the number of groups.
   * 
   * @param groupCounts the number of teams in each group, cannot be null
   * @throws NullPointerException if groupCounts is null
   */
  public final void setGroupCounts(final int[] groupCounts) {
    if (null == groupCounts) {
      throw new NullPointerException("groupCounts cannot be null");
    }

    this.groupCounts = new int[groupCounts.length];
    System.arraycopy(groupCounts, 0, this.groupCounts, 0, groupCounts.length);
  }

  /**
   * Number of groups of teams.
   * Defaults to 0.
   */
  public final int getNumGroups() {
    return groupCounts.length;
  }

  /**
   * @param index the index into groupCounts
   * @return the number of teams in the specified group
   * @throws IndexOutOfBoundsException if index is not a valid index for
   *           groupCounts
   */
  public final int getNumTeamsInGroup(int index) {
    return this.groupCounts[index];
  }

  private boolean alternate = false;

  /**
   * Defaults to false.
   * 
   * @return true if the solver should alternate tables
   */
  public final boolean getAlternateTables() {
    return alternate;
  }

  /**
   * @see #getAlternateTables()
   */
  public final void setAlternateTables(final boolean v) {
    this.alternate = v;
  }

  private int numPerformanceRounds = 3;

  /**
   * The number of performance rounds.
   * Defaults to 3.
   */
  public final int getNumPerformanceRounds() {
    return numPerformanceRounds;
  }

  /**
   * @see #getNumPerformanceRounds()
   */
  public final void setNumPerformanceRounds(final int v) {
    numPerformanceRounds = v;
  }

  private boolean subjectiveFirst = false;

  /**
   * If true, schedule the subjective stations before the performance.
   * Defaults to false.
   */
  public final boolean getSubjectiveFirst() {
    return this.subjectiveFirst;
  }

  /**
   * @see #getSubjectiveFirst()
   */
  public final void setSubjectiveFirst(final boolean v) {
    this.subjectiveFirst = v;
  }

  private int perfAttemptOffsetMinutes;

  /**
   * If a performance round cannot be scheduled at a time, how many
   * minutes later should the next time to try be.
   * Defaults to {@link #getTimeIncrement()}.
   */
  public final int getPerformanceAttemptOffsetMinutes() {
    return this.perfAttemptOffsetMinutes;
  }

  /**
   * @see #getPerformanceAttemptOffsetMinutes()
   */
  public final void setPerformanceAttemptOffsetMinutes(final int v) {
    this.perfAttemptOffsetMinutes = v;
  }

  private int subjectiveAttemptOffsetMinutes;

  /**
   * If a subjective round cannot be scheduled at a time, how many
   * minutes later should the next time to try be.
   * Defaults to {@link #getTimeIncrement()}.
   */
  public final int getSubjectiveAttemptOffsetMinutes() {
    return this.subjectiveAttemptOffsetMinutes;
  }

  /**
   * @see #getSubjectiveAttemptOffsetMinutes()
   */
  public final void setSubjectiveAttemptOffsetMinutes(final int v) {
    this.subjectiveAttemptOffsetMinutes = v;
  }

  private int numTables = 1;

  /**
   * The number of performance tables.
   * Defaults to 1.
   */
  public final int getNumTables() {
    return this.numTables;
  }

  /**
   * @see #getNumTables()
   */
  public final void setNumTables(final int v) {
    this.numTables = v;
  }

  private int tmaxHours = 8;

  /**
   * Maximum number of hours the tournament should run. This
   * is used to limit the search space when generating a schedule.
   * Defaults to 8.
   */
  public final int getTMaxHours() {
    return this.tmaxHours;
  }

  /**
   * @see #getTMaxHours()
   */
  public final void setTMaxHours(final int v) {
    this.tmaxHours = v;
  }

  //TODO would like to move this to a duration type
  private int tmaxMinutes = 0;

  /**
   * This property is combined with {@link #getTMaxHours()} to create
   * the limit on how long the tournament can be.
   * Defaults to 0.
   */
  public final int getTMaxMinutes() {
    return this.tmaxMinutes;
  }

  /**
   * @see #getTMaxMinutes()
   */
  public final void setTMaxMinutes(final int v) {
    this.tmaxMinutes = v;
  }


  private final Collection<ScheduledBreak> subjectiveBreaks = new LinkedList<ScheduledBreak>();
  /**
   * @return Read-only collection of the subjective breaks
   */
  public Collection<ScheduledBreak> getSubjectiveBreaks() {
    return Collections.unmodifiableCollection(this.subjectiveBreaks);
  }

  private final Collection<ScheduledBreak> performanceBreaks = new LinkedList<ScheduledBreak>();

  /**
   * @return Read-only collection of the performance breaks
   */
  public Collection<ScheduledBreak> getPerformanceBreaks() {
    return Collections.unmodifiableCollection(this.performanceBreaks);
  }

  /**
   * Read the breaks out of the data file.
   * 
   * @throws ParseException
   */
  private void parseBreaks(final Properties properties,
                           final Date startTime,
                           final int tinc)
      throws ParseException {
    subjectiveBreaks.addAll(parseBreaks(properties, startTime, tinc, "subjective"));
    performanceBreaks.addAll(parseBreaks(properties, startTime, tinc, "performance"));
  }

  private Collection<ScheduledBreak> parseBreaks(final Properties properties,
                                                 final Date startTime,
                                                 final int tinc,
                                                 final String breakType)
      throws ParseException {
    final Collection<ScheduledBreak> breaks = new LinkedList<ScheduledBreak>();

    final int numBreaks = Integer.parseInt(properties.getProperty(String.format("num_%s_breaks", breakType), "0"));
    final String startFormat = "%s_break_%d_start";
    final String durationFormat = "%s_break_%d_duration";
    for (int i = 0; i < numBreaks; ++i) {
      final String startStr = properties.getProperty(String.format(startFormat, breakType, i));
      final String durationStr = properties.getProperty(String.format(durationFormat, breakType, i));
      if (null == startStr
          || null == durationStr) {
        throw new FLLRuntimeException(String.format("Missing start or duration for %s break %d", breakType, i));
      }

      final Date breakStart = TournamentSchedule.parseDate(startStr);
      final int breakStartMinutes = (int) ((breakStart.getTime()
          - startTime.getTime())
          / Utilities.MILLISECONDS_PER_SECOND / Utilities.SECONDS_PER_MINUTE);
      final int breakStartInc = breakStartMinutes
          / tinc;
      if (breakStartMinutes != breakStartInc
          * tinc) {
        throw new FLLRuntimeException(String.format("%s break %d start isn't divisible by tinc", breakType, i));
      }

      final int breakDurationMinutes = Integer.parseInt(durationStr);
      final int breakDurationInc = breakDurationMinutes
          / tinc;
      if (breakDurationMinutes != breakDurationInc
          * tinc) {
        throw new FLLRuntimeException(String.format("%s break %d duration isn't divisible by tinc", breakType, i));
      }

      breaks.add(new ScheduledBreak(breakStartInc, breakStartInc
          + breakDurationInc));
    }
    return breaks;
  }

}
