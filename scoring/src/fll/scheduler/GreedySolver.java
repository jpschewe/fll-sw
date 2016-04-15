/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

import au.com.bytecode.opencsv.CSVWriter;
import fll.Utilities;
import fll.util.CheckCanceled;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;

/**
 * Custom solver for scheduling tournaments.
 */
public class GreedySolver {

  /**
   * Prefix for columns of subjective stations.
   */
  public static final String SUBJECTIVE_COLUMN_PREFIX = "Subj";

  private static final Logger LOGGER = LogUtils.getLogger();

  private final SolverParams solverParameters;

  /**
   * group, team, station
   */
  private final boolean[][][] subjectiveScheduled;

  /**
   * group, team
   */
  private final int[][] performanceScheduled;

  /**
   * group, team, station, time
   */
  private final boolean[][][][] sy;

  /**
   * group, team, station, time
   */
  private final boolean[][][][] sz;

  /**
   * group, team, table, side, time
   */
  private final boolean[][][][][] py;

  /**
   * group, team, table, side, time
   */
  private final boolean[][][][][] pz;

  /**
   * next available time for group, station
   */
  private final int[][] subjectiveStations;

  /**
   * next available time for table
   */
  private final int[] performanceTables;

  private final File datafile;

  private File mBestSchedule = null;

  /**
   * File that contains the best schedule found.
   * 
   * @return
   */
  public File getBestSchedule() {
    return mBestSchedule;
  }

  private int solutionsFound = 0;

  private File mBestObjectiveFile = null;

  public File getBestObjectiveFile() {
    return mBestObjectiveFile;
  }

  private ObjectiveValue bestObjective = null;

  private final boolean optimize;

  private final boolean subjectiveFirst;

  private static final String OPTIMIZE_OPTION = "o";

  private static final String DATA_FILE_OPTION = "d";

  private final Collection<ScheduledBreak> subjectiveBreaks = new LinkedList<ScheduledBreak>();

  private final Collection<ScheduledBreak> performanceBreaks = new LinkedList<ScheduledBreak>();

  private static Options buildOptions() {
    final Options options = new Options();
    Option option = new Option(DATA_FILE_OPTION, "datafile", true, "<file> the file ");
    option.setRequired(true);
    options.addOption(option);

    option = new Option(OPTIMIZE_OPTION, "optimize", false, "Turn on optimization (default: false)");
    options.addOption(option);

    return options;
  }

  private static void usage(final Options options) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("GreedySolver", options);
  }

  public static void main(final String[] args) throws InterruptedException {
    LogUtils.initializeLogging();

    final Options options = buildOptions();

    // parse options
    boolean optimize = false;
    File datafile = null;
    try {
      final CommandLineParser parser = new PosixParser();
      final CommandLine cmd = parser.parse(options, args);

      if (cmd.hasOption(OPTIMIZE_OPTION)) {
        optimize = true;
      }

      datafile = new File(cmd.getOptionValue(DATA_FILE_OPTION));
    } catch (final org.apache.commons.cli.ParseException pe) {
      LOGGER.error(pe.getMessage());
      usage(options);
      System.exit(1);
    }

    try {
      if (!datafile.canRead()) {
        LOGGER.fatal(datafile.getAbsolutePath()
            + " is not readable");
        System.exit(4);
      }

      final GreedySolver solver = new GreedySolver(datafile, optimize);
      final long start = System.currentTimeMillis();
      solver.solve(null);
      final long stop = System.currentTimeMillis();
      LOGGER.info("Solve took: "
          + (stop
              - start)
              / 1000.0
          + " seconds");

    } catch (final ParseException e) {
      LOGGER.fatal(e, e);
      System.exit(5);
    } catch (final IOException e) {
      LOGGER.fatal("Error reading file", e);
      System.exit(4);
    } catch (final RuntimeException e) {
      LOGGER.fatal(e, e);
      throw e;
    }
  }

  public static final String TINC_KEY = "TInc";

  public static final String START_TIME_KEY = "start_time";

  public static final String ALTERNATE_TABLES_KEY = "alternate_tables";

  public static final String SUBJECTIVE_FIRST_KEY = "sujective_first";

  public static final String PERF_ATTEMPT_OFFSET_MINUTES_KEY = "perf_attempt_offset_minutes";

  public static final String SUBJECTIVE_ATTEMPT_OFFSET_MINUTES_KEY = "subjective_attempt_offset_minutes";

  public static final String NROUNDS_KEY = "NRounds";

  public static final String NTABLES_KEY = "NTables";

  public static final String TMAX_HOURS_KEY = "TMax_hours";

  public static final String TMAX_MINUTES_KEY = "TMax_minutes";

  public static final String SUBJ_MINUTES_KEY = "subj_minutes";

  public static final String GROUP_COUNTS_KEY = "group_counts";

  public static final String ALPHA_PERF_MINUTES_KEY = "alpha_perf_minutes";

  public static final String CT_MINUTES_KEY = "ct_minutes";

  public static final String PCT_MINUTES_KEY = "pct_minutes";

  /**
   * @param datafile the datafile for the schedule to solve
   * @throws ParseException
   */
  public GreedySolver(final File datafile,
                      final boolean optimize) throws IOException, ParseException {
    this.datafile = datafile;
    this.optimize = optimize;
    if (this.optimize) {
      LOGGER.info("Optimization is turned on");
    }

    final Properties properties = new Properties();
    try (final Reader reader = new InputStreamReader(new FileInputStream(datafile), Utilities.DEFAULT_CHARSET)) {
      properties.load(reader);
    }
    LOGGER.debug(properties.toString());

    this.solverParameters = new SolverParams(properties);

    final int alternateValue = Integer.parseInt(properties.getProperty(ALTERNATE_TABLES_KEY, "0").trim());
    final boolean alternate = alternateValue == 1;
    LOGGER.debug("Alternate is: "
        + alternate);

    final int subjectiveFirst = Integer.parseInt(properties.getProperty(SUBJECTIVE_FIRST_KEY, "0").trim());
    this.subjectiveFirst = subjectiveFirst == 1;
    LOGGER.debug("Subjective first is: "
        + this.subjectiveFirst);

    final int perfOffsetMinutes = Integer.parseInt(properties.getProperty(PERF_ATTEMPT_OFFSET_MINUTES_KEY,
                                                                          String.valueOf(solverParameters.getTimeIncrement()))
                                                             .trim());
    performanceAttemptOffset = perfOffsetMinutes
        / solverParameters.getTimeIncrement();
    if (perfOffsetMinutes != performanceAttemptOffset
        * solverParameters.getTimeIncrement()) {
      throw new FLLRuntimeException("perf_attempt_offset_minutes isn't divisible by tinc");
    }
    LOGGER.debug("Performance attempt offset: "
        + performanceAttemptOffset);

    final int subjOffsetMinutes = Integer.parseInt(properties.getProperty(SUBJECTIVE_ATTEMPT_OFFSET_MINUTES_KEY,
                                                                          String.valueOf(solverParameters.getTimeIncrement()))
                                                             .trim());
    subjectiveAttemptOffset = subjOffsetMinutes
        / solverParameters.getTimeIncrement();
    if (subjOffsetMinutes != subjectiveAttemptOffset
        * solverParameters.getTimeIncrement()) {
      throw new FLLRuntimeException("subjective_attempt_offset_minutes isn't divisible by tinc");
    }

    numPerformanceRounds = Utilities.readIntProperty(properties, NROUNDS_KEY);
    numTables = Utilities.readIntProperty(properties, NTABLES_KEY);
    final int tmaxHours = Utilities.readIntProperty(properties, TMAX_HOURS_KEY);
    final int tmaxMinutes = Utilities.readIntProperty(properties, TMAX_MINUTES_KEY);
    numTimeslots = (tmaxHours
        * 60
        + tmaxMinutes)
        / solverParameters.getTimeIncrement();

    for (final SubjectiveStation station : this.solverParameters.getSubjectiveStations()) {
      if (0 != station.getDurationMinutes()
          % solverParameters.getTimeIncrement()) {
        throw new FLLRuntimeException("Subjective duration for station "
            + station.getName() + " isn't divisible by the specified time increment "
            + solverParameters.getTimeIncrement());
      }
    }

    performanceDuration = this.solverParameters.getPerformanceMinutes()
        / solverParameters.getTimeIncrement();
    if (this.solverParameters.getPerformanceMinutes() != performanceDuration
        * solverParameters.getTimeIncrement()) {
      throw new FLLRuntimeException("Performance duration isn't divisible by tinc");
    }

    changetime = this.solverParameters.getChangetimeMinutes()
        / solverParameters.getTimeIncrement();
    if (this.solverParameters.getChangetimeMinutes() != changetime
        * solverParameters.getTimeIncrement()) {
      throw new FLLRuntimeException("Changetime isn't divisible by tinc");
    }
    if (this.solverParameters.getChangetimeMinutes() < SchedParams.MINIMUM_CHANGETIME_MINUTES) {
      throw new FLLRuntimeException("Change time between events is too short, cannot be less than "
          + SchedParams.MINIMUM_CHANGETIME_MINUTES + " minutes");
    }

    performanceChangetime = this.solverParameters.getPerformanceChangetimeMinutes()
        / solverParameters.getTimeIncrement();
    if (this.solverParameters.getPerformanceChangetimeMinutes() != performanceChangetime
        * solverParameters.getTimeIncrement()) {
      throw new FLLRuntimeException("Performance changetime isn't divisible by tinc");
    }
    if (this.solverParameters.getPerformanceChangetimeMinutes() < SchedParams.MINIMUM_PERFORMANCE_CHANGETIME_MINUTES) {
      throw new FLLRuntimeException("Change time between performance rounds is too short, cannot be less than "
          + SchedParams.MINIMUM_PERFORMANCE_CHANGETIME_MINUTES + " minutes");
    }

    if (alternate) {
      // make sure performanceDuration is even
      if ((performanceDuration
          & 1) == 1) {
        throw new FLLRuntimeException("Number of timeslots for performance duration ("
            + performanceDuration + ") is not even and must be to alternate tables.");
      }

      // make sure num tables is even
      if ((getNumTables()
          & 1) == 1) {
        throw new FLLRuntimeException("Number of tables ("
            + getNumTables() + ") is not even and must be to alternate tables.");
      }

    }

    sz = new boolean[solverParameters.getNumGroups()][][][];
    sy = new boolean[solverParameters.getNumGroups()][][][];
    pz = new boolean[solverParameters.getNumGroups()][][][][];
    py = new boolean[solverParameters.getNumGroups()][][][][];
    subjectiveScheduled = new boolean[solverParameters.getNumGroups()][][];
    subjectiveStations = new int[solverParameters.getNumGroups()][getNumSubjectiveStations()];
    performanceScheduled = new int[solverParameters.getNumGroups()][];
    performanceTables = new int[getNumTables()];
    if (alternate) {
      for (int table = 0; table < performanceTables.length; ++table) {
        // even is 0, odd is 1/2 performance duration
        if ((table
            & 1) == 1) {
          performanceTables[table] = performanceDuration
              / 2;
        } else {
          performanceTables[table] = 0;
        }
        LOGGER.debug("Setting table "
            + table + " start to " + performanceTables[table]);
      }
    } else {
      Arrays.fill(performanceTables, 0);
    }
    for (int group = 0; group < solverParameters.getNumGroups(); ++group) {
      final int count = solverParameters.getNumTeamsInGroup(group);
      sz[group] = new boolean[count][getNumSubjectiveStations()][getNumTimeslots()];
      sy[group] = new boolean[count][getNumSubjectiveStations()][getNumTimeslots()];
      pz[group] = new boolean[count][getNumTables()][2][getNumTimeslots()];
      py[group] = new boolean[count][getNumTables()][2][getNumTimeslots()];
      subjectiveScheduled[group] = new boolean[count][getNumSubjectiveStations()];
      for (int team = 0; team < count; ++team) {
        teams.add(new SchedTeam(team, group));

        for (int station = 0; station < getNumSubjectiveStations(); ++station) {
          Arrays.fill(sz[group][team][station], false);
          Arrays.fill(sy[group][team][station], false);
        }

        for (int table = 0; table < getNumTables(); ++table) {
          Arrays.fill(pz[group][team][table][0], false);
          Arrays.fill(pz[group][team][table][1], false);
          Arrays.fill(py[group][team][table][0], false);
          Arrays.fill(py[group][team][table][1], false);
        }
        Arrays.fill(subjectiveScheduled[group][team], false);
        Arrays.fill(subjectiveStations[group], 0);
      }

      performanceScheduled[group] = new int[count];
      Arrays.fill(performanceScheduled[group], 0);
    }

    parseBreaks(properties, solverParameters.getStartTime(), solverParameters.getTimeIncrement());

    // sort list of teams to make sure that the scheduler is deterministic
    Collections.sort(teams, lowestTeamIndex);
  }

  /**
   * Read the breaks out of the data file.
   * 
   * @throws ParseException
   */
  private void parseBreaks(final Properties properties,
                           final Date startTime,
                           final int tinc) throws ParseException {
    subjectiveBreaks.addAll(parseBreaks(properties, startTime, tinc, "subjective"));
    performanceBreaks.addAll(parseBreaks(properties, startTime, tinc, "performance"));
  }

  private Collection<ScheduledBreak> parseBreaks(final Properties properties,
                                                 final Date startTime,
                                                 final int tinc,
                                                 final String breakType) throws ParseException {
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

      final Date start = TournamentSchedule.parseDate(startStr);
      final int startMinutes = (int) ((start.getTime()
          - startTime.getTime())
          / Utilities.MILLISECONDS_PER_SECOND / Utilities.SECONDS_PER_MINUTE);
      final int startInc = startMinutes
          / tinc;
      if (startMinutes != startInc
          * tinc) {
        throw new FLLRuntimeException(String.format("%s break %d start isn't divisible by tinc", breakType, i));
      }

      final int durationMinutes = Integer.parseInt(durationStr);
      final int durationInc = durationMinutes
          / tinc;
      if (durationMinutes != durationInc
          * tinc) {
        throw new FLLRuntimeException(String.format("%s break %d duration isn't divisible by tinc", breakType, i));
      }

      breaks.add(new ScheduledBreak(startInc, startInc
          + durationInc));
    }
    return breaks;
  }

  private boolean assignSubjective(final int group,
                                   final int team,
                                   final int station,
                                   final int timeslot) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Attempting to assigning subjective group: "
          + group + " team: " + team + " station: " + station + " time: " + timeslot);
    }

    if (timeslot
        + getSubjectiveDuration(station) >= getNumTimeslots()) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("FAILED: too close to EOS");
      }
      return false;
    }

    for (int otherCat = 0; otherCat < getNumSubjectiveStations(); ++otherCat) {
      if (!checkSubjFree(group, team, otherCat, timeslot, getSubjectiveDuration(station))) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("FAILED: overlap with other subjective category: "
              + otherCat);
        }
        return false;
      }
    }
    if (!checkPerfFree(group, team, timeslot, getSubjectiveDuration(station))) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("FAILED: overlap with performance");
      }
      return false;
    }
    // check all other teams at this station
    if (!checkSubjStationNoOverlap(group, station, timeslot)) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("FAILED: overlap on this station");
      }
      return false;
    }

    subjectiveScheduled[group][team][station] = true;
    sz[group][team][station][timeslot] = true;
    for (int slot = timeslot; slot < timeslot
        + getSubjectiveDuration(station); ++slot) {
      sy[group][team][station][slot] = true;
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("SUCCESS");
    }
    return true;
  }

  /**
   * Check that there isn't an overlap at timeslot on the specified station
   */
  private boolean checkSubjStationNoOverlap(final int group,
                                            final int station,
                                            final int timeslot) {
    for (final SchedTeam team : getAllTeams()) {
      if (team.getGroup() == group) {
        for (int slot = timeslot; slot < Math.min(getNumTimeslots(), timeslot
            + getSubjectiveDuration(station)); ++slot) {
          if (sy[group][team.getIndex()][station][slot]) {
            return false;
          }
        }
      }
    }
    return true;
  }

  /**
   * Check that there isn't an overlap at timeslot on the specified table side.
   */
  private boolean checkPerfNoOverlap(final int table,
                                     final int side,
                                     final int timeslot) {
    for (final SchedTeam team : getAllTeams()) {
      for (int slot = timeslot; slot < Math.min(getNumTimeslots(), timeslot
          + getPerformanceDuration()); ++slot) {
        if (py[team.getGroup()][team.getIndex()][table][side][slot]) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Get the duration for the given subjective station in time increments.
   */
  public int getSubjectiveDuration(final int station) {
    return this.solverParameters.getSubjectiveMinutes(station)
        / solverParameters.getTimeIncrement();
  }

  /**
   * @param station zero based
   * @return
   */
  public static String getSubjectiveColumnName(final int station) {
    return String.format("%s%d", SUBJECTIVE_COLUMN_PREFIX, station
        + 1);
  }

  private void unassignSubjective(final int group,
                                  final int team,
                                  final int station,
                                  final int timeslot) {

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("UN-Assigning subjective group: "
          + group + " team: " + team + " station: " + station + " time: " + timeslot);
    }

    subjectiveScheduled[group][team][station] = false;
    sz[group][team][station][timeslot] = false;
    for (int slot = timeslot; slot < timeslot
        + getSubjectiveDuration(station); ++slot) {
      sy[group][team][station][slot] = false;
    }
  }

  /**
   * Make sure that the station for a given team is available and there is
   * enough changetime.
   */
  private boolean checkSubjFree(final int group,
                                final int team,
                                final int station,
                                final int timeslot,
                                final int duration) {
    for (int slot = Math.max(0, timeslot
        - getChangetime()); slot < Math.min(getNumTimeslots(),
                                            timeslot
                                                + duration + getChangetime()); ++slot) {
      if (sy[group][team][station][slot]) {
        return false;
      }
    }
    return true;
  }

  private final int changetime;

  /**
   * Changetime between judging stations in time increments.
   */
  private int getChangetime() {
    return changetime;
  }

  /**
   * Make sure performance is free for the given team including changetime.
   */
  private boolean checkPerfFree(final int group,
                                final int team,
                                final int timeslot,
                                final int duration) {
    for (int slot = Math.max(0, timeslot
        - getChangetime()); slot < Math.min(getNumTimeslots(),
                                            timeslot
                                                + duration + getChangetime()); ++slot) {
      for (int table = 0; table < getNumTables(); ++table) {
        if (py[group][team][table][0][slot]) {
          return false;
        }
        if (py[group][team][table][1][slot]) {
          return false;
        }
      }
    }
    return true;
  }

  private final int numTables;

  private int getNumTables() {
    return numTables;
  }

  private boolean assignPerformance(final int group,
                                    final int team,
                                    final int timeslot,
                                    final int table,
                                    final int side) {
    return assignPerformance(group, team, timeslot, table, side, true, false);
  }

  /**
   * Assign a team to a performance slot if possible.
   * 
   * @param group
   * @param team
   * @param timeslot
   * @param table
   * @param side
   * @param doAssignment if we should actually do the assignment, useful for
   *          checking extra runs
   * @param ignoreChangeTime if scheduling a team staying for an extra run, then
   *          the performance change time doesn't matter
   * @return
   */
  private boolean assignPerformance(final int group,
                                    final int team,
                                    final int timeslot,
                                    final int table,
                                    final int side,
                                    final boolean doAssignment,
                                    final boolean ignoreChangetime) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Attempting to assigning performance group: "
          + group + " team: " + team + " table: " + table + " side: " + side + " time: " + timeslot + " doAssignment: "
          + doAssignment);
    }

    if (timeslot
        + getPerformanceDuration() >= getNumTimeslots()) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("FAILED: too close to EOS");
      }
      return false;
    }
    for (int station = 0; station < getNumSubjectiveStations(); ++station) {
      if (!checkSubjFree(group, team, station, timeslot, getPerformanceDuration())) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("FAILED: overlap with subjective station: "
              + station);
        }
        return false;
      }
    }
    if (!checkPerfFree(group, team, timeslot, getPerformanceDuration())) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("FAILED: overlap with other performance");
      }
      return false;
    }
    if (!ignoreChangetime
        && !checkPerfChangetime(group, team, timeslot)) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("FAILED: performance changetime");
      }
      return false;
    }
    if (!checkPerfNoOverlap(table, side, timeslot)) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("FAILED: performance overlap");
      }
      return false;
    }

    if (doAssignment) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Assigning performance group: "
            + group + " team: " + team + " table: " + table + " side: " + side + " time: " + timeslot);
      }

      ++performanceScheduled[group][team];
      pz[group][team][table][side][timeslot] = true;
      for (int slot = timeslot; slot < timeslot
          + getPerformanceDuration(); ++slot) {
        py[group][team][table][side][slot] = true;
      }

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("SUCCESS");
      }
    }

    return true;
  }

  private boolean checkPerfChangetime(final int group,
                                      final int team,
                                      final int timeslot) {
    for (int slot = Math.max(0, timeslot
        - getPerformanceChangetime()); slot < Math.min(getNumTimeslots(),
                                                       timeslot
                                                           + getPerformanceChangetime()
                                                           + getPerformanceDuration()); ++slot) {
      for (int table = 0; table < getNumTables(); ++table) {
        if (py[group][team][table][0][slot]) {
          return false;
        }
        if (py[group][team][table][1][slot]) {
          return false;
        }
      }
    }
    return true;
  }

  private final int performanceChangetime;

  /**
   * Time between performance rounds in time increments.
   */
  private int getPerformanceChangetime() {
    return performanceChangetime;
  }

  private void unassignPerformance(final int group,
                                   final int team,
                                   final int timeslot,
                                   final int table,
                                   final int side) {

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("UN-Assigning performance group: "
          + group + " team: " + team + " table: " + table + " side: " + side + " time: " + timeslot);
    }

    --performanceScheduled[group][team];
    pz[group][team][table][side][timeslot] = false;
    for (int slot = timeslot; slot < timeslot
        + getPerformanceDuration(); ++slot) {
      py[group][team][table][side][slot] = false;
    }
  }

  private final int performanceDuration;

  /**
   * Performance duration in time increments.
   */
  private int getPerformanceDuration() {
    return performanceDuration;
  }

  private boolean dummyPerformanceSlotUsed = false;

  /**
   * Check if we're allowed to have a table assigned with no second team.
   * This will be true if there is an odd number of teams and an odd number
   * performance rounds and the dummy slot hasn't been used.
   */
  private boolean partialPerformanceAssignmentAllowed() {
    final boolean oddPerfRounds = (numPerformanceRounds
        & 1) == 1;
    final boolean oddTeams = (getAllTeams().size()
        & 1) == 1;
    return !dummyPerformanceSlotUsed
        && oddTeams && oddPerfRounds;
  }

  private boolean schedPerf(final int table,
                            final int timeslot) throws InterruptedException {
    final List<SchedTeam> teams = getPossiblePerformanceTeams();
    SchedTeam team1 = null;

    final List<SchedTeam> possibleValues = new LinkedList<SchedTeam>();
    for (final SchedTeam team : teams) {
      if (null == team1) {
        if (assignPerformance(team.getGroup(), team.getIndex(), timeslot, table, 0)) {
          if (optimize) {
            // just build up list of possible values
            possibleValues.add(team);
            unassignPerformance(team.getGroup(), team.getIndex(), timeslot, table, 0);
          } else {
            team1 = team;
          }
        }
      } else {
        if (assignPerformance(team.getGroup(), team.getIndex(), timeslot, table, 1)) {
          final boolean result = scheduleNextStation();
          if (!result) {
            // if we get to this point we should look for another solution
            unassignPerformance(team.getGroup(), team.getIndex(), timeslot, table, 1);

            // unassignPerformance(team1.getGroup(), team1.getIndex(), timeslot,
            // table, 0);
            // team1 = null;

            // if (timeslot
            // + getPerformanceDuration() >= getNumTimeslots()) {
            // if (LOGGER.isDebugEnabled()) {
            // LOGGER.debug("Hit max timeslots - perf");
            // }
            // return false;
            // }
          } else {
            return true;
          }
        }
      }
    }

    // TODO find prev team on each table and see if any of them can be assigned,
    // if so, keep going
    // not working yet...

    // undo partial assignment if not allowed
    if (null != team1) {
      final boolean lastRoundForTeam1 = performanceScheduled[team1.getGroup()][team1.getIndex()] == getNumPerformanceRounds();

      boolean foundOtherTeam = false;
      if (lastRoundForTeam1
          && partialPerformanceAssignmentAllowed()) {
        for (int otable = 0; !foundOtherTeam
            && otable < getNumTables(); ++otable) {
          final SchedTeam prevTeamOnTable0 = findPrevTeamOnTable(timeslot, table, 0);
          if (null != prevTeamOnTable0) {
            if (assignPerformance(prevTeamOnTable0.getGroup(), prevTeamOnTable0.getIndex(), timeslot, table, 1, false,
                                  true)) {
              if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Choose dummy group: "
                    + prevTeamOnTable0.getGroup() + " team: " + prevTeamOnTable0.getIndex());
              }
              foundOtherTeam = true;
            }
          }

          if (!foundOtherTeam) {
            final SchedTeam prevTeamOnTable1 = findPrevTeamOnTable(timeslot, table, 1);
            if (null != prevTeamOnTable1) {
              if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Choose dummy group: "
                    + prevTeamOnTable1.getGroup() + " team: " + prevTeamOnTable1.getIndex());
              }
              if (assignPerformance(prevTeamOnTable1.getGroup(), prevTeamOnTable1.getIndex(), timeslot, table, 1, false,
                                    true)) {
                foundOtherTeam = true;
              }
            }
          }
        }
      }

      final SchedTeam prevTeamOnTable = findPrevTeamOnTable(timeslot, table, 1);
      if (partialPerformanceAssignmentAllowed()
          && null != prevTeamOnTable
          /*
           * commenting this out makes search go crazy on odd number of teams,
           * leaving it in seems to cause problems as well
           */
          && foundOtherTeam
      /*
       * && assignPerformance(prevTeamOnTable.getGroup(),
       * prevTeamOnTable.getIndex(), timeslot, table, 1, false)
       */
      ) {
        // use a dummy team as the other team

        dummyPerformanceSlotUsed = true;
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Scheduling dummy slot");
        }

        final boolean result = scheduleNextStation();
        if (!result) {
          dummyPerformanceSlotUsed = false;
          unassignPerformance(team1.getGroup(), team1.getIndex(), timeslot, table, 0);
          team1 = null;
        } else {
          return true;
        }

      } else {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Partial table assignment, unassigning group: "
              + team1.getGroup() //
              + " team: " + team1.getIndex() //
              + " slot: " + timeslot //
              + " table: " + table //
              + " prevTeamOnTable null?: " + (null == prevTeamOnTable) //
              + " partial allowed: " + partialPerformanceAssignmentAllowed() //
          );
        }
        unassignPerformance(team1.getGroup(), team1.getIndex(), timeslot, table, 0);
        team1 = null;
      }
    }

    if (optimize
        && possibleValues.size() > 1) {
      // try all possible values
      for (final SchedTeam t1 : possibleValues) {
        if (!assignPerformance(t1.getGroup(), t1.getIndex(), timeslot, table, 0)) {
          throw new FLLRuntimeException("Internal error, should not have trouble assigning values here - 1");
        }
        for (final SchedTeam t2 : possibleValues) {
          if (!t1.equals(t2)) {
            if (!assignPerformance(t2.getGroup(), t2.getIndex(), timeslot, table, 1)) {
              throw new FLLRuntimeException("Internal error, should not have trouble assigning values here - 2");
            }
            // ignore result as we want to try all values
            scheduleNextStation();

            unassignPerformance(t2.getGroup(), t2.getIndex(), timeslot, table, 1);

            // in case a better answer was found
            if (timeslot
                + getPerformanceDuration() >= getNumTimeslots()) {
              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Hit max timeslots - perf optimize");
              }
              unassignPerformance(t1.getGroup(), t1.getIndex(), timeslot, table, 0);
              return false;
            }
          }
        }
        unassignPerformance(t1.getGroup(), t1.getIndex(), timeslot, table, 0);
      }
    }

    return false;
  }

  /**
   *  
   */

  /**
   * Find the team that is on the table and side prior to timeslot.
   * 
   * @param timeslot
   * @param table
   * @param side
   * @return the team or null if no team can be found
   */
  private SchedTeam findPrevTeamOnTable(final int timeslot,
                                        final int table,
                                        final int side) {
    for (int slot = timeslot
        - 1; slot >= 0; --slot) {
      for (final SchedTeam team : getAllTeams()) {
        if (py[team.getGroup()][team.getIndex()][table][side][slot]) {
          return team;
        }
      }
    }
    return null;
  }

  /**
   * Get all teams that need scheduling in the specified station sorted by
   * number of assignments.
   */
  private List<SchedTeam> getPossibleSubjectiveTeams(final int group,
                                                     final int station) {
    List<SchedTeam> possibles = new LinkedList<SchedTeam>();
    for (final SchedTeam team : getAllTeams()) {
      if (team.getGroup() == group
          && !subjectiveScheduled[team.getGroup()][team.getIndex()][station]) {
        possibles.add(team);
      }
    }
    Collections.sort(possibles, fewestAssignments);

    if (!possibles.isEmpty()) {
      // if this is the first assignment to any station in this group, then only
      // return 1
      // possible value so that we don't try all teams.
      boolean firstAssignment = true;
      for (int s = 0; s < getNumSubjectiveStations()
          && firstAssignment; ++s) {
        for (final SchedTeam team : getAllTeams()) {
          if (team.getGroup() == group) {
            if (subjectiveScheduled[group][team.getIndex()][s]) {
              firstAssignment = false;
            }
          }
        }
      }
      if (firstAssignment) {
        return Collections.singletonList(possibles.get(0));
      }
    }

    return possibles;
  }

  /**
   * Get all teams that need scheduling in performance sorted by number of
   * assignments.
   */
  private List<SchedTeam> getPossiblePerformanceTeams() {
    List<SchedTeam> possibles = new LinkedList<SchedTeam>();
    for (final SchedTeam team : getAllTeams()) {
      if (performanceScheduled[team.getGroup()][team.getIndex()] < getNumPerformanceRounds()) {
        possibles.add(team);
      }
    }
    Collections.sort(possibles, fewestAssignments);
    return possibles;
  }

  private boolean schedSubj(final int group,
                            final int station,
                            final int timeslot) throws InterruptedException {
    final List<SchedTeam> teams = getPossibleSubjectiveTeams(group, station);
    for (final SchedTeam team : teams) {
      if (assignSubjective(team.getGroup(), team.getIndex(), station, timeslot)) {
        final boolean result = scheduleNextStation();
        if (!result
            || optimize) {
          unassignSubjective(team.getGroup(), team.getIndex(), station, timeslot);

          // if (timeslot
          // + getSubjectiveDuration(station) >= getNumTimeslots()) {
          // if (LOGGER.isDebugEnabled()) {
          // LOGGER.debug("Hit max timeslots - subj");
          // }
          // return false;
          // }
        } else {
          return true;
        }
      }
    }
    return false;
  }

  private boolean subjectiveFinished() {
    for (final SchedTeam team : getAllTeams()) {
      for (int station = 0; station < getNumSubjectiveStations(); ++station) {
        if (!subjectiveScheduled[team.getGroup()][team.getIndex()][station]) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean scheduleFinished() {
    for (final SchedTeam team : getAllTeams()) {
      for (int station = 0; station < getNumSubjectiveStations(); ++station) {
        if (!subjectiveScheduled[team.getGroup()][team.getIndex()][station]) {
          return false;
        }
      }

      for (int table = 0; table < getNumTables(); ++table) {
        if (performanceScheduled[team.getGroup()][team.getIndex()] < getNumPerformanceRounds()) {
          return false;
        }
      }

    }
    return true;
  }

  private final List<SchedTeam> teams = new LinkedList<SchedTeam>();

  /**
   * @return unmodifiable list of all teams
   */
  private List<SchedTeam> getAllTeams() {
    return Collections.unmodifiableList(teams);
  }

  private final int numPerformanceRounds;

  private int getNumPerformanceRounds() {
    return numPerformanceRounds;
  }

  private CheckCanceled checkCanceled = null;

  /**
   * Solve the problem.
   * 
   * @param checkCanceled if non-null, used to check if the schedule should be
   *          interrupted
   * @return the number of solutions found
   */
  public int solve(final CheckCanceled checkCanceled) {
    this.checkCanceled = checkCanceled;

    try {
      LOGGER.info("Starting solve");
      scheduleNextStation();
    } catch (final InterruptedException e) {
      LOGGER.debug("Solver interrupted");
    }

    if (solutionsFound < 1) {
      if (null != checkCanceled
          && checkCanceled.isCanceled()) {
        LOGGER.info("Solver canceled before a solution was found");
      } else {
        LOGGER.info("Infeasible problem, no solutions found");
      }
    } else {
      LOGGER.info("Found "
          + solutionsFound + " solutions");
    }
    return solutionsFound;
  }

  /**
   * Get the number of warnings.
   * 
   * @param scheduleFile
   * @return the number of warnings or -1 if there are hard violations
   */
  private int getNumWarnings(final File scheduleFile) {
    final List<SubjectiveStation> subjectiveParams = new LinkedList<SubjectiveStation>();
    final Collection<String> subjectiveHeaders = new LinkedList<String>();
    for (int subj = 0; subj < getNumSubjectiveStations(); ++subj) {
      final String header = "Subj"
          + (subj
              + 1);
      subjectiveHeaders.add(header);
      final SubjectiveStation station = new SubjectiveStation(header, getSubjectiveDuration(subj)
          * solverParameters.getTimeIncrement());
      subjectiveParams.add(station);
    }

    try {
      final TournamentSchedule schedule = new TournamentSchedule(datafile.getName(), scheduleFile, subjectiveHeaders);

      final SchedParams params = new SchedParams(subjectiveParams, getPerformanceDuration()
          * solverParameters.getTimeIncrement(), getChangetime()
              * solverParameters.getTimeIncrement(), getPerformanceChangetime()
                  * solverParameters.getTimeIncrement());
      final ScheduleChecker checker = new ScheduleChecker(params, schedule);
      final List<ConstraintViolation> violations = checker.verifySchedule();
      for (final ConstraintViolation violation : violations) {
        if (violation.isHard()) {
          LOGGER.debug("Found hard constraint violations from autosched: "
              + violation.getMessage());
          return -1;
        }
      }
      return violations.size();
    } catch (final IOException e) {
      throw new FLLRuntimeException("Should not have an IOException trying to get warnings from CSV file", e);
    } catch (ParseException e) {
      throw new FLLRuntimeException("Should not have an ParseException trying to get warnings from CSV file", e);
    } catch (ScheduleParseException e) {
      throw new FLLRuntimeException("Should not have an ScheduleParseException trying to get warnings from CSV file",
                                    e);
    }
  }

  /**
   * @param scheduleFile
   * @return the objective value, null on failure
   */
  private ObjectiveValue computeObjectiveValue(final File scheduleFile) {
    final int[] numTeams = new int[solverParameters.getNumGroups()];
    final int[] latestSubjectiveTime = new int[solverParameters.getNumGroups()];
    for (int group = 0; group < numTeams.length; ++group) {
      numTeams[group] = subjectiveScheduled[group].length;
      latestSubjectiveTime[group] = findLatestSubjectiveTime(group);
    }
    final int numWarnings = getNumWarnings(scheduleFile);
    if (numWarnings == -1) {
      return null;
    }
    return new ObjectiveValue(solutionsFound, findLatestPerformanceTime(), numTeams, latestSubjectiveTime, numWarnings);
  }

  /**
   * The slot that has the latest subjective time for a group of teams.
   */
  private int findLatestSubjectiveTime(final int group) {
    for (int slot = getNumTimeslots()
        - 1; slot >= 0; --slot) {
      for (int station = 0; station < getNumSubjectiveStations(); ++station) {
        for (final SchedTeam team : getAllTeams()) {
          if (team.getGroup() == group) {
            if (sy[team.getGroup()][team.getIndex()][station][slot]) {
              return slot;
            }
          }
        }
      }
    }
    LOGGER.warn("Got to end of findLatestSubjectiveTime("
        + group + "), this implies that nothing was scheduled");
    return 0;
  }

  /**
   * The slot that has the last performance time.
   */
  private int findLatestPerformanceTime() {
    for (int slot = getNumTimeslots()
        - 1; slot >= 0; --slot) {
      for (int table = 0; table < getNumTables(); ++table) {
        for (final SchedTeam team : getAllTeams()) {
          if (py[team.getGroup()][team.getIndex()][table][0][slot]) {
            return slot;
          }
          if (py[team.getGroup()][team.getIndex()][table][1][slot]) {
            return slot;
          }
        }
      }
    }
    LOGGER.warn("Got to end of findLatestPerformanceTime, this implies that nothing was scheduled");
    return 0;
  }

  /**
   * @return if a solution has been found
   * @throws InterruptedException if the solver was canceled
   */
  private boolean scheduleNextStation() throws InterruptedException {
    if (null != checkCanceled
        && checkCanceled.isCanceled()) {
      throw new InterruptedException();
    }

    if (scheduleFinished()) {
      if (outputCurrentSolution()) {
        ++solutionsFound;
        LOGGER.info("Schedule finished num solutions: "
            + solutionsFound);

        return true;
      } else {
        return false;
      }
    }

    // find possible values
    final List<Integer> possibleSubjectiveStations = new ArrayList<Integer>();
    final List<Integer> subjectiveGroups = new ArrayList<Integer>();
    final int nextAvailableSubjSlot = findNextAvailableSubjectiveSlot(possibleSubjectiveStations, subjectiveGroups);

    final List<Integer> possiblePerformanceTables = new LinkedList<Integer>();
    final int nextAvailablePerfSlot = findNextAvailablePerformanceSlot(possiblePerformanceTables);

    if (Math.min(nextAvailablePerfSlot, nextAvailableSubjSlot) >= getNumTimeslots()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Hit max timeslots");
      }
      return false;
    }

    // try possible values
    if ((subjectiveFirst
        && !subjectiveFinished())
        || (nextAvailableSubjSlot <= nextAvailablePerfSlot)) {
      for (int i = 0; i < possibleSubjectiveStations.size(); ++i) {
        final int station = possibleSubjectiveStations.get(i);
        final int group = subjectiveGroups.get(i);

        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("subjective group: "
              + group + " station: " + station + " next available: " + nextAvailableSubjSlot);
        }

        subjectiveStations[group][station] += getSubjectiveAttemptOffset();
        if (checkSubjectiveBreaks(station, nextAvailableSubjSlot)) {
          final boolean result = schedSubj(group, station, nextAvailableSubjSlot);
          if (result) {
            return true;
          } else if (nextAvailableSubjSlot >= getNumTimeslots()) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Hit max timeslots - schedNext subj");
            }
            return false;
          }

        } else {
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Overlaps breaks, skipping");
          }
        }
      }
    } else {
      for (final int table : possiblePerformanceTables) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("performance table: "
              + table + " next available: " + nextAvailablePerfSlot);
        }

        performanceTables[table] += getPerformanceAttemptOffset();
        if (checkPerformanceBreaks(nextAvailablePerfSlot)) {
          final boolean result = schedPerf(table, nextAvailablePerfSlot);
          if (result) {
            return true;
          } else if (nextAvailablePerfSlot >= getNumTimeslots()) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Hit max timeslots - schedNext perf");
            }
            return false;
          }
        } else {
          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Overlaps breaks, skipping");
          }
        }
      }
    }

    final boolean result = scheduleNextStation();
    if (!result
        || optimize) {
      // undo changes made above
      if (nextAvailableSubjSlot <= nextAvailablePerfSlot) {
        for (int i = 0; i < possibleSubjectiveStations.size(); ++i) {
          final int station = possibleSubjectiveStations.get(i);
          final int group = subjectiveGroups.get(i);
          subjectiveStations[group][station] -= getSubjectiveAttemptOffset();
        }
      } else {
        for (final int table : possiblePerformanceTables) {
          performanceTables[table] -= getPerformanceAttemptOffset();
        }
      }
    }
    return result;

  }

  private int findNextAvailablePerformanceSlot(final List<Integer> possiblePerformanceTables) {
    int nextAvailablePerfSlot = Integer.MAX_VALUE;
    for (int table = 0; table < getNumTables(); ++table) {
      if (performanceTables[table] <= nextAvailablePerfSlot) {
        if (performanceTables[table] < nextAvailablePerfSlot) {
          // previous values are no longer valid
          possiblePerformanceTables.clear();
        }
        nextAvailablePerfSlot = performanceTables[table];
        possiblePerformanceTables.add(table);
      }
    }
    return nextAvailablePerfSlot;
  }

  private int findNextAvailableSubjectiveSlot(final List<Integer> possibleSubjectiveStations,
                                              final List<Integer> subjectiveGroups) {
    int nextAvailableSubjSlot = Integer.MAX_VALUE;
    for (int group = 0; group < solverParameters.getNumGroups(); ++group) {
      for (int station = 0; station < getNumSubjectiveStations(); ++station) {
        if (subjectiveStations[group][station] <= nextAvailableSubjSlot) {
          if (subjectiveStations[group][station] < nextAvailableSubjSlot) {
            // previous subjective stations are no longer valid for this time
            possibleSubjectiveStations.clear();
            subjectiveGroups.clear();
          }
          nextAvailableSubjSlot = subjectiveStations[group][station];
          possibleSubjectiveStations.add(station);
          subjectiveGroups.add(group);
        }
      }
    }
    return nextAvailableSubjSlot;
  }

  private boolean outputCurrentSolution() {
    final File scheduleFile = new File(Utilities.extractAbsoluteBasename(datafile)
        + "-" + solutionsFound + ".csv");
    final File objectiveFile = new File(Utilities.extractAbsoluteBasename(datafile)
        + "-" + solutionsFound + ".obj.txt");

    try {
      outputSchedule(scheduleFile);
    } catch (final IOException ioe) {
      throw new FLLRuntimeException("Error writing schedule", ioe);
    }

    LOGGER.info("Solution output to "
        + scheduleFile.getAbsolutePath());

    final ObjectiveValue objective = computeObjectiveValue(scheduleFile);
    if (null == objective) {
      LOGGER.info("Objective is null, solution is not valid");
      if (!scheduleFile.delete()) {
        scheduleFile.deleteOnExit();
      }
      return false;
    }

    if (null == bestObjective
        || objective.compareTo(bestObjective) < 0) {
      LOGGER.info("Schedule provides a better objective value");
      bestObjective = objective;

      try (final Writer objectiveWriter = new OutputStreamWriter(new FileOutputStream(objectiveFile),
                                                                 Utilities.DEFAULT_CHARSET)) {
        objectiveWriter.write(objective.toString());
        objectiveWriter.close();
      } catch (final IOException e) {
        throw new FLLRuntimeException("Error writing objective", e);
      }

      if (null != mBestSchedule) {
        if (!mBestSchedule.delete()) {
          mBestSchedule.deleteOnExit();
        }
      }
      mBestSchedule = scheduleFile;

      if (null != mBestObjectiveFile) {
        if (!mBestObjectiveFile.delete()) {
          mBestObjectiveFile.deleteOnExit();
        }
      }
      mBestObjectiveFile = objectiveFile;

      // tighten down the constraints so that we find a better solution
      final int newNumTimeslots = objective.getLatestPerformanceTime()
          + 1;
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Tightening numTimeslots from "
            + numTimeslots + " to " + newNumTimeslots);
      }
      numTimeslots = newNumTimeslots;
    } else {
      if (!scheduleFile.delete()) {
        scheduleFile.deleteOnExit();
      }
    }

    return true;
  }

  public int getNumSubjectiveStations() {
    return this.solverParameters.getNumSubjectiveStations();
  }

  /**
   * Number of timeslots to increment by when trying the next subjective time
   * slot. To try all possible combinations, this should be set to 1.
   */
  private int getSubjectiveAttemptOffset() {
    return subjectiveAttemptOffset;
  }

  private final int subjectiveAttemptOffset;

  /**
   * Number of timeslots to increment by when trying the next performance time
   * slot. To try all possible combinations, this should be set to 1.
   */
  private int getPerformanceAttemptOffset() {
    return performanceAttemptOffset;
  }

  private final int performanceAttemptOffset;

  private int numTimeslots;

  /**
   * The number of timeslots available to schedule in.
   */
  private int getNumTimeslots() {
    return numTimeslots;
  }

  private void outputSchedule(final File schedule) throws IOException {
    try (final CSVWriter csv = new CSVWriter(new OutputStreamWriter(new FileOutputStream(schedule),
                                                                    Utilities.DEFAULT_CHARSET))) {
      final List<String> line = new ArrayList<String>();
      line.add(TournamentSchedule.TEAM_NUMBER_HEADER);
      line.add(TournamentSchedule.DIVISION_HEADER);
      line.add(TournamentSchedule.TEAM_NAME_HEADER);
      line.add(TournamentSchedule.ORGANIZATION_HEADER);
      line.add(TournamentSchedule.JUDGE_GROUP_HEADER);
      for (int subj = 0; subj < getNumSubjectiveStations(); ++subj) {
        line.add(getSubjectiveColumnName(subj));
      }
      for (int round = 0; round < getNumPerformanceRounds(); ++round) {
        line.add(String.format(TournamentSchedule.PERF_HEADER_FORMAT, round
            + 1));
        line.add(String.format(TournamentSchedule.TABLE_HEADER_FORMAT, round
            + 1));
      }
      csv.writeNext(line.toArray(new String[line.size()]));
      line.clear();

      for (final SchedTeam team : getAllTeams()) {
        final int teamNum = (team.getGroup()
            + 1)
            * 100
            + team.getIndex();
        final int judgingGroup = team.getGroup()
            + 1;
        line.add(String.valueOf(teamNum));
        line.add("D"
            + judgingGroup);
        line.add("Team "
            + teamNum);
        line.add("Org "
            + teamNum);
        line.add("G"
            + judgingGroup);
        for (int subj = 0; subj < getNumSubjectiveStations(); ++subj) {
          final Date time = getTime(sz[team.getGroup()][team.getIndex()][subj], 1);
          if (null == time) {
            throw new RuntimeException("Could not find a subjective start for group: "
                + (team.getGroup()
                    + 1)
                + " team: " + (team.getIndex()
                    + 1)
                + " subj: " + (subj
                    + 1));
          }
          line.add(TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(time));
        }

        // find all performances for a team and then sort by time
        final SortedSet<PerformanceTime> perfTimes = new TreeSet<PerformanceTime>();
        for (int round = 0; round < getNumPerformanceRounds(); ++round) {
          for (int table = 0; table < getNumTables(); ++table) {
            for (int side = 0; side < 2; ++side) {
              final Date time = getTime(pz[team.getGroup()][team.getIndex()][table][side], round
                  + 1);
              if (null != time) {
                perfTimes.add(new PerformanceTime(time, "Table"
                    + (table
                        + 1), (side
                            + 1)));
              }
            }
          }
        }
        if (perfTimes.size() != getNumPerformanceRounds()) {
          throw new FLLRuntimeException("Expecting "
              + getNumPerformanceRounds() + " performance times, but found " + perfTimes.size() + " group: "
              + (team.getGroup()
                  + 1)
              + " team: " + (team.getIndex()
                  + 1)
              + " perfs: " + perfTimes);
        }
        for (final PerformanceTime perfTime : perfTimes) {
          line.add(TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(perfTime.getTime()));
          line.add(perfTime.getTable()
              + " " + perfTime.getSide());
        }

        csv.writeNext(line.toArray(new String[line.size()]));
        line.clear();
      }
    }
  }

  /**
   * Get the nth time from slot that is true.
   * 
   * @param slots the slots to look in
   * @param count which time to find, 1 based count
   * @return
   */
  private Date getTime(final boolean[] slots,
                       final int count) {
    int n = 0;
    for (int i = 0; i < slots.length; ++i) {
      if (slots[i]) {
        ++n;
        if (n == count) {
          final Calendar cal = Calendar.getInstance();
          cal.setTime(solverParameters.getStartTime());
          cal.add(Calendar.MINUTE, i
              * solverParameters.getTimeIncrement());
          return cal.getTime();
        }
      }
    }
    return null;
  }

  /**
   * Check if the specified timeslot will overlap the subjective breaks.
   */
  private boolean checkSubjectiveBreaks(final int station,
                                        final int timeslot) {
    final int begin = timeslot;
    final int end = timeslot
        + getSubjectiveDuration(station);

    return checkBreak(begin, end, subjectiveBreaks);
  }

  private boolean checkBreak(final int begin,
                             final int end,
                             final Collection<ScheduledBreak> breaks) {
    for (final ScheduledBreak b : breaks) {
      if (b.end > begin
          && b.start < end) {
        return false;
      }
    }

    return true;
  }

  /**
   * Check if the specified timeslot will overlap the performance breaks.
   */
  private boolean checkPerformanceBreaks(final int timeslot) {
    final int begin = timeslot;
    final int end = timeslot
        + getPerformanceDuration();

    return checkBreak(begin, end, performanceBreaks);
  }

  private static final Comparator<SchedTeam> lowestTeamIndex = new Comparator<SchedTeam>() {
    @Override
    public int compare(final SchedTeam one,
                       final SchedTeam two) {
      if (one.equals(two)) {
        return 0;
      } else if (one.getGroup() < two.getGroup()) {
        return -1;
      } else if (one.getGroup() > two.getGroup()) {
        return 1;
      } else if (one.getIndex() < two.getIndex()) {
        return -1;
      } else if (one.getIndex() > two.getIndex()) {
        return 1;
      } else {
        return 0;
      }
    }
  };

  private final Comparator<SchedTeam> fewestAssignments = new Comparator<SchedTeam>() {
    @Override
    public int compare(final SchedTeam one,
                       final SchedTeam two) {
      if (one.equals(two)) {
        return 0;
      } else {
        int oneAssignments = 0;
        int twoAssignments = 0;

        for (int station = 0; station < getNumSubjectiveStations(); ++station) {
          if (subjectiveScheduled[one.getGroup()][one.getIndex()][station]) {
            ++oneAssignments;
          }
          if (subjectiveScheduled[two.getGroup()][two.getIndex()][station]) {
            ++twoAssignments;
          }
        }

        for (int table = 0; table < getNumTables(); ++table) {
          oneAssignments += performanceScheduled[one.getGroup()][one.getIndex()];
          twoAssignments += performanceScheduled[two.getGroup()][two.getIndex()];
        }

        if (oneAssignments < twoAssignments) {
          return -1;
        } else if (oneAssignments > twoAssignments) {
          return 1;
        } else {
          return 0;
        }
      }
    }
  };

  private static final class ScheduledBreak {
    public ScheduledBreak(final int start,
                          final int end) {
      this.start = start;
      this.end = end;
    }

    public final int start;

    public final int end;
  }
}
