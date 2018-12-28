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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

  public static final String START_TIME_KEY = "start_time";

  public static final String ALTERNATE_TABLES_KEY = "alternate_tables";

  public static final String SUBJECTIVE_FIRST_KEY = "subjective_first";

  public static final String PERF_ATTEMPT_OFFSET_MINUTES_KEY = "perf_attempt_offset_minutes";

  public static final String SUBJECTIVE_ATTEMPT_OFFSET_MINUTES_KEY = "subjective_attempt_offset_minutes";

  @Deprecated
  private static final String NROUNDS_KEY = "NRounds";

  public static final String NTABLES_KEY = "NTables";

  public static final String TMAX_HOURS_KEY = "TMax_hours";

  public static final String TMAX_MINUTES_KEY = "TMax_minutes";

  public static final String GROUP_COUNTS_KEY = "group_counts";

  public static final String GROUP_NAMES_KEY = "group_names";

  public static final String PERF_EARLIEST_START_KEY = "perf_earliest_start";

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
    perfAttemptOffsetMinutes.add(5);

    // default to 3 rounds with no limmits
    performanceEarliestStarts.add(null);
    performanceEarliestStarts.add(null);
    performanceEarliestStarts.add(null);

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

    final String startTimeStr = properties.getProperty(START_TIME_KEY, null);
    if (null != startTimeStr) {
      this.startTime = TournamentSchedule.parseTime(startTimeStr);
    }

    final String groupCountsStr = properties.getProperty(GROUP_COUNTS_KEY);
    int[] groupCounts = Utilities.parseListOfIntegers(groupCountsStr);
    final String groupNamesStr = properties.getProperty(GROUP_NAMES_KEY, null);
    String[] groupNames = Utilities.parseListOfStrings(groupNamesStr);
    judgingGroups = new HashMap<>();
    for (int i = 0; i < groupCounts.length; ++i) {
      final String name;
      if (i < groupNames.length) {
        name = groupNames[i];
      } else {
        name = String.format("G%d", i);
      }
      judgingGroups.put(name, groupCounts[i]);
    }

    this.alternate = Utilities.readBooleanProperty(properties, ALTERNATE_TABLES_KEY, false);
    LOGGER.debug("Alternate is: "
        + alternate);

    loadPerformanceRounds(properties);

    this.subjectiveFirst = Utilities.readBooleanProperty(properties, SUBJECTIVE_FIRST_KEY, false);
    LOGGER.debug("Subjective first is: "
        + this.subjectiveFirst);

    final String perfOffsetStr = properties.getProperty(PERF_ATTEMPT_OFFSET_MINUTES_KEY, null);
    if (null != perfOffsetStr) {
      this.perfAttemptOffsetMinutes.clear();
      try {
        // try single value first to handle old properties files
        final Integer duration = Integer.parseInt(perfOffsetStr.trim());
        this.perfAttemptOffsetMinutes.add(duration);
      } catch (final NumberFormatException e) {
        // assume list
        int[] perfOffset = Utilities.parseListOfIntegers(perfOffsetStr);
        for (final int offset : perfOffset) {
          this.perfAttemptOffsetMinutes.add(offset);
        }
      }
    }

    this.subjectiveAttemptOffsetMinutes = Utilities.readIntProperty(properties, SUBJECTIVE_ATTEMPT_OFFSET_MINUTES_KEY,
                                                                    this.subjectiveAttemptOffsetMinutes);

    this.numTables = Utilities.readIntProperty(properties, NTABLES_KEY, this.numTables);

    this.tmaxHours = Utilities.readIntProperty(properties, TMAX_HOURS_KEY);
    this.tmaxMinutes = Utilities.readIntProperty(properties, TMAX_MINUTES_KEY);

    parseBreaks(properties);
  }

  @Override
  public void save(final Properties properties) {
    super.save(properties);

    properties.setProperty(START_TIME_KEY, TournamentSchedule.formatTime(this.startTime));

    final int[] judgingGroupCounts = new int[judgingGroups.size()];
    final String[] judgingGroupNames = new String[judgingGroups.size()];
    int judgingGroupIndex = 0;
    for (final Map.Entry<String, Integer> entry : judgingGroups.entrySet()) {
      judgingGroupCounts[judgingGroupIndex] = entry.getValue();
      judgingGroupNames[judgingGroupIndex] = entry.getKey();

      ++judgingGroupIndex;
    }
    properties.setProperty(GROUP_COUNTS_KEY, Arrays.toString(judgingGroupCounts));
    properties.setProperty(GROUP_NAMES_KEY, Arrays.toString(judgingGroupNames));

    properties.setProperty(ALTERNATE_TABLES_KEY, Boolean.toString(this.alternate));

    savePerformanceRounds(properties);

    properties.setProperty(SUBJECTIVE_FIRST_KEY, Boolean.toString(this.subjectiveFirst));

    final Integer[] perfOffset = this.perfAttemptOffsetMinutes.toArray(new Integer[0]);
    properties.setProperty(PERF_ATTEMPT_OFFSET_MINUTES_KEY, Arrays.toString(perfOffset));

    properties.setProperty(SUBJECTIVE_ATTEMPT_OFFSET_MINUTES_KEY,
                           Integer.toString(this.subjectiveAttemptOffsetMinutes));

    properties.setProperty(NTABLES_KEY, Integer.toString(this.numTables));

    properties.setProperty(TMAX_HOURS_KEY, Integer.toString(this.tmaxHours));
    properties.setProperty(TMAX_MINUTES_KEY, Integer.toString(this.tmaxMinutes));

    saveBreaks(properties);
  }

  /**
   * @param properties
   */
  private void savePerformanceRounds(final Properties properties) {
    final String[] timeStr = performanceEarliestStarts.stream().map(time -> TournamentSchedule.formatTime(time))
                                                      .toArray(size -> new String[size]);
    final String str = Arrays.toString(timeStr);
    properties.setProperty(PERF_EARLIEST_START_KEY, str);
  }

  private void loadPerformanceRounds(final Properties properties) {
    final String earliestStartStr = properties.getProperty(PERF_EARLIEST_START_KEY, null);
    if (null == earliestStartStr) {
      final int numRounds = Utilities.readIntProperty(properties, NROUNDS_KEY, getNumPerformanceRounds());

      performanceEarliestStarts.clear();
      for (int i = 0; i < numRounds; ++i) {
        performanceEarliestStarts.add(null);
      }
    } else {
      final String[] timesStr = Utilities.parseListOfStrings(earliestStartStr);

      performanceEarliestStarts.clear();
      for (final String str : timesStr) {
        final LocalTime time = TournamentSchedule.parseTime(str);
        performanceEarliestStarts.add(time);
      }
    }
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

  private Map<String, Integer> judgingGroups = new HashMap<>();

  /**
   * The judging groups.
   * 
   * @param judgingGroups key is name, value is number of teams
   */
  public void setJudgingGroups(final Map<String, Integer> judgingGroups) {
    this.judgingGroups = new HashMap<>(judgingGroups);
  }

  /**
   * The judging groups.
   * 
   * @return unmodifiable map, judgingGroups key is name, value is number of
   *         teams
   */
  public Map<String, Integer> getJudgingGroups() {
    return Collections.unmodifiableMap(this.judgingGroups);
  }

  /**
   * Number of groups of teams.
   * Defaults to 0.
   */
  public final int getNumGroups() {
    return judgingGroups.size();
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

  private final List<LocalTime> performanceEarliestStarts = new LinkedList<>();

  /**
   * The number of performance rounds.
   * Defaults to 3.
   */
  public final int getNumPerformanceRounds() {
    return performanceEarliestStarts.size();
  }

  /**
   * The earliest start time for each performance round.
   * 
   * @return unmodifiable list, index is round number, value is the earliest
   *         start time (may be null, meaning no limit)
   */
  public List<LocalTime> getPerformanceRoundEarliestStartTimes() {
    return Collections.unmodifiableList(performanceEarliestStarts);
  }

  public void setPerformanceRoundEarliestStartTimes(final List<LocalTime> v) {
    performanceEarliestStarts.clear();
    performanceEarliestStarts.addAll(v);
  }

  private boolean subjectiveFirst = true;

  /**
   * If true, schedule the subjective stations before the performance.
   * Defaults to true.
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

  private final List<Integer> perfAttemptOffsetMinutes = new LinkedList<>();

  /**
   * If a performance round cannot be scheduled at a time, how many
   * minutes later should the next time to try be.
   * This is a list specifying the pattern. In most cases this list
   * should only contain one element. However some tournaments may want to
   * specify a pattern such as 7 and then 8 so that there are 2 timeslots
   * available every 15 minutes.
   * Defaults to a list with a single element of 5.
   * 
   * @return read-only list of the performance offset times
   */
  public final List<Integer> getPerformanceAttemptOffsetMinutes() {
    return Collections.unmodifiableList(this.perfAttemptOffsetMinutes);
  }

  /**
   * @see #getPerformanceAttemptOffsetMinutes()
   */
  public final void setPerformanceAttemptOffsetMinutes(final List<Integer> v) {
    this.perfAttemptOffsetMinutes.clear();
    this.perfAttemptOffsetMinutes.addAll(v);
  }

  private int subjectiveAttemptOffsetMinutes = 5;

  /**
   * If a subjective round cannot be scheduled at a time, how many
   * minutes later should the next time to try be.
   * Defaults to 5.
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
            * 60L));
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

  private final LinkedList<ScheduledBreak> subjectiveBreaks = new LinkedList<ScheduledBreak>();

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

  private final LinkedList<ScheduledBreak> performanceBreaks = new LinkedList<ScheduledBreak>();

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
    subjectiveBreaks.clear();
    subjectiveBreaks.addAll(parseBreaks(properties, "subjective"));

    performanceBreaks.clear();
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
