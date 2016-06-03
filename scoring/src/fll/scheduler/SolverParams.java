/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.text.ParseException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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
   * Format for the number of breaks property. Expected to be
   * used with String.format() and one argument that is the
   * type of break "subjective" or "performance".
   */
  private static final String numBreaksFormat = "num_%s_breaks";

  /**
   * Format for the start of break property. Expected to be
   * used with String.format() and two arguments that is the
   * type of break "subjective" or "performance" and then the index of the
   * break (starting at 0).
   */
  private static final String startFormat = "%s_break_%d_start";

  /**
   * Format for the duration of break property. Expected to be
   * used with String.format() and two arguments that is the
   * type of break "subjective" or "performance" and then the index of the
   * break (starting at 0).
   */
  private static final String durationFormat = "%s_break_%d_duration";

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

    final String startTimeStr = properties.getProperty(GreedySolver.START_TIME_KEY, null);
    if (null != startTimeStr) {
      this.startTime = TournamentSchedule.parseTime(startTimeStr);
    }

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
                                                              this.perfAttemptOffsetMinutes);

    this.subjectiveAttemptOffsetMinutes = Utilities.readIntProperty(properties,
                                                                    GreedySolver.SUBJECTIVE_ATTEMPT_OFFSET_MINUTES_KEY,
                                                                    this.subjectiveAttemptOffsetMinutes);

    this.numTables = Utilities.readIntProperty(properties, GreedySolver.NTABLES_KEY, this.numTables);

    this.tmaxHours = Utilities.readIntProperty(properties, GreedySolver.TMAX_HOURS_KEY);
    this.tmaxMinutes = Utilities.readIntProperty(properties, GreedySolver.TMAX_MINUTES_KEY);

    parseBreaks(properties);

  }

  @Override
  public void save(final Properties properties) {
    super.save(properties);

    properties.setProperty(GreedySolver.START_TIME_KEY, TournamentSchedule.formatTime(this.startTime));

    properties.setProperty(GreedySolver.GROUP_COUNTS_KEY, Arrays.toString(this.groupCounts));

    properties.setProperty(GreedySolver.ALTERNATE_TABLES_KEY, Boolean.toString(this.alternate));

    properties.setProperty(GreedySolver.NROUNDS_KEY, Integer.toString(this.numPerformanceRounds));

    properties.setProperty(GreedySolver.SUBJECTIVE_FIRST_KEY, Boolean.toString(this.subjectiveFirst));

    properties.setProperty(GreedySolver.PERF_ATTEMPT_OFFSET_MINUTES_KEY,
                           Integer.toString(this.perfAttemptOffsetMinutes));

    properties.setProperty(GreedySolver.SUBJECTIVE_ATTEMPT_OFFSET_MINUTES_KEY,
                           Integer.toString(this.subjectiveAttemptOffsetMinutes));

    properties.setProperty(GreedySolver.NTABLES_KEY, Integer.toString(this.numTables));

    properties.setProperty(GreedySolver.TMAX_HOURS_KEY, Integer.toString(this.tmaxHours));
    properties.setProperty(GreedySolver.TMAX_MINUTES_KEY, Integer.toString(this.tmaxMinutes));

    saveBreaks(properties);

  }

  private LocalTime startTime = LocalTime.of(8, 0);

  /**
   * The start time of the tournament. Nothing is scheduled before this time.
   * Defaults to 8:00.
   */
  public final LocalTime getStartTime() {
    return startTime;
  }

  /**
   * @see #getStartTime()
   */
  public final void setStartTime(final LocalTime v) {
    this.startTime = v;
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

  private int perfAttemptOffsetMinutes = 1;

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

  private int subjectiveAttemptOffsetMinutes = 1;

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

  /**
   * Maximum duration the tournament should run. This
   * is used to limit the search space when generating a schedule.
   * 
   * @see #getTMaxHours()
   * @see #getTMaxMinutes()
   */
  public final Duration getMaxDuration() {
    final Duration d = Duration.ofHours(getTMaxHours()).plusMinutes(getTMaxMinutes());
    return d;
  }

  /**
   * Maximum duration the tournament should run. This
   * is used to limit the search space when generating a schedule.
   *
   * @param duration the new duration, will be truncated to even minutes
   * @see #getTMaxHours()
   * @see #getTMaxMinutes()
   */
  public final void setMaxDuration(final Duration duration) {
    final int hours = (int) duration.toHours();
    final long allMinutes = duration.toMinutes();
    final int minutes = (int) (allMinutes
        - (hours
            * 60));
    setTMaxHours(hours);
    setTMaxMinutes(minutes);
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

  // TODO would like to move this to a duration type
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

  private final List<ScheduledBreak> subjectiveBreaks = new LinkedList<ScheduledBreak>();

  /**
   * @return Read-only list of the subjective breaks
   */
  public List<ScheduledBreak> getSubjectiveBreaks() {
    return Collections.unmodifiableList(this.subjectiveBreaks);
  }
  
  public void setSubjectiveBreaks(final List<ScheduledBreak> breaks) {
    subjectiveBreaks.clear();
    subjectiveBreaks.addAll(breaks);
  }

  private final List<ScheduledBreak> performanceBreaks = new LinkedList<ScheduledBreak>();

  /**
   * @return Read-only list of the performance breaks
   */
  public List<ScheduledBreak> getPerformanceBreaks() {
    return Collections.unmodifiableList(this.performanceBreaks);
  }

  public void setPerformanceBreaks(final List<ScheduledBreak> breaks) {
    performanceBreaks.clear();
    performanceBreaks.addAll(breaks);
  }

  /**
   * Read the breaks out of the data file.
   * 
   * @throws ParseException
   */
  private void parseBreaks(final Properties properties) throws ParseException {
    subjectiveBreaks.addAll(parseBreaks(properties, "subjective"));
    performanceBreaks.addAll(parseBreaks(properties, "performance"));
  }

  private List<ScheduledBreak> parseBreaks(final Properties properties,
                                           final String breakType)
      throws ParseException {
    final List<ScheduledBreak> breaks = new LinkedList<ScheduledBreak>();

    final int numBreaks = Integer.parseInt(properties.getProperty(String.format(numBreaksFormat, breakType), "0"));
    for (int i = 0; i < numBreaks; ++i) {
      final String startStr = properties.getProperty(String.format(startFormat, breakType, i), null);
      final String durationStr = properties.getProperty(String.format(durationFormat, breakType, i), null);
      if (null == startStr
          || null == durationStr) {
        throw new FLLRuntimeException(String.format("Missing start or duration for %s break %d", breakType, i));
      }

      final LocalTime breakStart = TournamentSchedule.parseTime(startStr);
      final int breakDurationMinutes = Integer.parseInt(durationStr);

      breaks.add(new ScheduledBreak(breakStart, Duration.ofMinutes(breakDurationMinutes)));
    }
    return breaks;
  }

  /**
   * Save the breaks to the properties object.
   */
  private void saveBreaks(final Properties properties) {
    saveBreaks(properties, "subjective", subjectiveBreaks);
    saveBreaks(properties, "performance", performanceBreaks);
  }

  private void saveBreaks(final Properties properties,
                          final String breakType,
                          final List<ScheduledBreak> breaks) {
    final int numBreaks = breaks.size();
    properties.setProperty(String.format(numBreaksFormat, breakType), Integer.toString(numBreaks));

    for (int i = 0; i < numBreaks; ++i) {
      final ScheduledBreak sbreak = breaks.get(i);
      final String startStr = String.format(startFormat, breakType, i);
      final String formattedStart = TournamentSchedule.formatTime(sbreak.getStart());
      properties.setProperty(startStr, formattedStart);

      final String durationStr = String.format(durationFormat, breakType, i);
      properties.setProperty(durationStr, Long.toString(sbreak.getDuration().toMinutes()));
    }

  }

  @Override
  public List<String> isValid() {
    final List<String> errors = super.isValid();

    if (getAlternateTables()) {
      // make sure performanceDuration is even
      final int performanceDurationMinutes = getPerformanceMinutes();

      if ((performanceDurationMinutes
          & 1) == 1) {
        errors.add("Number of timeslots for performance duration minutes ("
            + performanceDurationMinutes + ") is not even and must be to alternate tables.");
      }

      // make sure num tables is even
      if ((getNumTables()
          & 1) == 1) {
        errors.add("Number of tables ("
            + getNumTables() + ") is not even and must be to alternate tables.");
      }

    }

    return errors;
  }

}
