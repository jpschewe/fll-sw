/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;

/**
 * Custom solver for scheduling tournaments.
 */
public class GreedySolver {

  private static final Logger LOGGER = LogUtils.getLogger();

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

  private final int ngroups;

  private int getNumGroups() {
    return ngroups;
  }

  private final int tinc;

  private final Date startTime;

  private int solutionsFound = 0;

  private ObjectiveValue bestObjective = null;

  private final boolean optimize;

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

  public static void main(final String[] args) {
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
      solver.solve();
      final long stop = System.currentTimeMillis();
      LOGGER.info("Solve took: "
          + (stop - start) / 1000.0 + " seconds");

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
    FileReader reader = null;
    try {
      reader = new FileReader(datafile);
      properties.load(reader);
    } finally {
      if (null != reader) {
        reader.close();
      }
    }
    LOGGER.debug(properties.toString());

    this.startTime = TournamentSchedule.OUTPUT_DATE_FORMAT.get().parse(properties.getProperty("start_time"));

    tinc = Utilities.readIntProperty(properties, "TInc");
    ngroups = Utilities.readIntProperty(properties, "NGroups");

    final int alternateValue = Integer.valueOf(properties.getProperty("alternate_tables", "0").trim());
    final boolean alternate = alternateValue == 1;
    LOGGER.debug("Alternate is: "
        + alternate);

    final int perfOffsetMinutes = Integer.valueOf(properties.getProperty("perf_attempt_offset_minutes",
                                                                         String.valueOf(tinc)).trim());
    performanceAttemptOffset = perfOffsetMinutes
        / tinc;
    if (perfOffsetMinutes != performanceAttemptOffset
        * tinc) {
      throw new FLLRuntimeException("perf_attempt_offset_minutes isn't divisible by tinc");
    }
    LOGGER.debug("Performance attempt offset: "
        + performanceAttemptOffset);

    final int subjOffsetMinutes = Integer.valueOf(properties.getProperty("subjective_attempt_offset_minutes",
                                                                         String.valueOf(tinc)).trim());
    subjectiveAttemptOffset = subjOffsetMinutes
        / tinc;
    if (subjOffsetMinutes != subjectiveAttemptOffset
        * tinc) {
      throw new FLLRuntimeException("subjective_attempt_offset_minutes isn't divisible by tinc");
    }

    numPerformanceRounds = Utilities.readIntProperty(properties, "NRounds");
    numTables = Utilities.readIntProperty(properties, "NTables");
    final int tmaxHours = Utilities.readIntProperty(properties, "TMax_hours");
    final int tmaxMinutes = Utilities.readIntProperty(properties, "TMax_minutes");
    numTimeslots = (tmaxHours * 60 + tmaxMinutes)
        / tinc;

    numSubjectiveStations = Utilities.readIntProperty(properties, "NSubjective");
    final String subjDurationStr = properties.getProperty("subj_minutes");
    int lbracket = subjDurationStr.indexOf('[');
    if (-1 == lbracket) {
      throw new FLLRuntimeException("No '[' found in subj_minutes: '"
          + subjDurationStr + "'");
    }
    int rbracket = subjDurationStr.indexOf(']', lbracket);
    if (-1 == rbracket) {
      throw new FLLRuntimeException("No ']' found in subj_minutes: '"
          + subjDurationStr + "'");
    }
    final String[] subjDurs = subjDurationStr.substring(lbracket + 1, rbracket).split(",");
    if (subjDurs.length != numSubjectiveStations) {
      throw new FLLRuntimeException("Number of subjective stations not consistent with subj_minutes array size");
    }
    subjectiveDurations = new int[getNumSubjectiveStations()];
    for (int station = 0; station < subjDurs.length; ++station) {
      final int durationMinutes = Integer.valueOf(subjDurs[station].trim());
      subjectiveDurations[station] = durationMinutes
          / tinc;
      if (durationMinutes != subjectiveDurations[station]
          * tinc) {
        throw new FLLRuntimeException("Subjective duration for station "
            + station + " isn't divisible by tinc");
      }
    }

    final String groupCountsStr = properties.getProperty("group_counts");
    lbracket = groupCountsStr.indexOf('[');
    if (-1 == lbracket) {
      throw new FLLRuntimeException("No '[' found in group_counts: '"
          + groupCountsStr + "'");
    }
    rbracket = groupCountsStr.indexOf(']', lbracket);
    if (-1 == rbracket) {
      throw new FLLRuntimeException("No ']' found in group_counts: '"
          + groupCountsStr + "'");
    }
    final String[] groups = groupCountsStr.substring(lbracket + 1, rbracket).split(",");
    if (groups.length != ngroups) {
      throw new FLLRuntimeException("Num groups and group_counts array not consistent");
    }

    final int performanceDurationMinutes = Utilities.readIntProperty(properties, "alpha_perf_minutes");
    performanceDuration = performanceDurationMinutes
        / tinc;
    if (performanceDurationMinutes != performanceDuration
        * tinc) {
      throw new FLLRuntimeException("Performance duration isn't divisible by tinc");
    }

    final int changetimeMinutes = Utilities.readIntProperty(properties, "ct_minutes");
    changetime = changetimeMinutes
        / tinc;
    if (changetimeMinutes != changetime
        * tinc) {
      throw new FLLRuntimeException("Changetime isn't divisible by tinc");
    }

    final int performanceChangetimeMinutes = Utilities.readIntProperty(properties, "pct_minutes");
    performanceChangetime = performanceChangetimeMinutes
        / tinc;
    if (performanceChangetimeMinutes != performanceChangetime
        * tinc) {
      throw new FLLRuntimeException("Performance changetime isn't divisible by tinc");
    }

    if (alternate) {
      // make sure performanceDuration is even
      if ((performanceDuration & 1) == 1) {
        throw new FLLRuntimeException("Number of timeslots for performance duration ("
            + performanceDuration + ") is not even and must be to alternate tables.");
      }

      // make sure num tables is even
      if ((getNumTables() & 1) == 1) {
        throw new FLLRuntimeException("Number of tables ("
            + getNumTables() + ") is not even and must be to alternate tables.");
      }

    }

    sz = new boolean[groups.length][][][];
    sy = new boolean[groups.length][][][];
    pz = new boolean[groups.length][][][][];
    py = new boolean[groups.length][][][][];
    subjectiveScheduled = new boolean[groups.length][][];
    subjectiveStations = new int[groups.length][getNumSubjectiveStations()];
    performanceScheduled = new int[groups.length][];
    performanceTables = new int[getNumTables()];
    if (alternate) {
      for (int table = 0; table < performanceTables.length; ++table) {
        // even is 0, odd is 1/2 performance duration
        if ((table & 1) == 1) {
          performanceTables[table] = performanceDuration / 2;
        } else {
          performanceTables[table] = 0;
        }
        LOGGER.debug("Setting table "
            + table + " start to " + performanceTables[table]);
      }
    } else {
      Arrays.fill(performanceTables, 0);
    }
    for (int group = 0; group < groups.length; ++group) {
      final int count = Integer.valueOf(groups[group].trim());
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

    parseBreaks(properties, startTime, tinc);

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

    final int numBreaks = Integer.valueOf(properties.getProperty(String.format("num_%s_breaks", breakType), "0"));
    final String startFormat = "%s_break_%d_start";
    final String durationFormat = "%s_break_%d_duration";
    for (int i = 0; i < numBreaks; ++i) {
      final String startStr = properties.getProperty(String.format(startFormat, breakType, i));
      final String durationStr = properties.getProperty(String.format(durationFormat, breakType, i));
      if (null == startStr
          || null == durationStr) {
        throw new FLLRuntimeException(String.format("Missing start or duration for %s break %d", breakType, i));
      }

      final Date start = TournamentSchedule.OUTPUT_DATE_FORMAT.get().parse(startStr);
      final int startMinutes = (int) ((start.getTime() - startTime.getTime())
          / Utilities.MILLISECONDS_PER_SECOND / Utilities.SECONDS_PER_MINUTE);
      final int startInc = startMinutes
          / tinc;
      if (startMinutes != startInc
          * tinc) {
        throw new FLLRuntimeException(String.format("%s break %d start isn't divisible by tinc", breakType, i));
      }

      final int durationMinutes = Integer.valueOf(durationStr);
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

  private final int[] subjectiveDurations;

  /**
   * Get the duration for the given subjective station in time increments.
   */
  private int getSubjectiveDuration(final int station) {
    return subjectiveDurations[station];
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
        - getChangetime()); slot < Math.min(getNumTimeslots(), timeslot
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
        - getChangetime()); slot < Math.min(getNumTimeslots(), timeslot
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
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Attempting to assigning performance group: "
          + group + " team: " + team + " table: " + table + " side: " + side + " time: " + timeslot);
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
    if (!checkPerfChangetime(group, team, timeslot)) {
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
    return true;
  }

  private boolean checkPerfChangetime(final int group,
                                      final int team,
                                      final int timeslot) {
    for (int slot = Math.max(0, timeslot
        - getPerformanceChangetime()); slot < Math.min(getNumTimeslots(), timeslot
        + getPerformanceChangetime() + getPerformanceDuration()); ++slot) {
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

  private boolean schedPerf(final int table,
                            final int timeslot) {
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
            unassignPerformance(team1.getGroup(), team1.getIndex(), timeslot, table, 0);
            unassignPerformance(team.getGroup(), team.getIndex(), timeslot, table, 1);
            team1 = null;

            if (timeslot
                + getPerformanceDuration() >= getNumTimeslots()) {
              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Hit max timeslots - perf");
              }
              return false;
            }
          } else {
            return true;
          }
        }
      }
    }

    // undo partial assignment
    if (null != team1) {
      unassignPerformance(team1.getGroup(), team1.getIndex(), timeslot, table, 0);
      team1 = null;
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
                            final int timeslot) {
    final List<SchedTeam> teams = getPossibleSubjectiveTeams(group, station);
    for (final SchedTeam team : teams) {
      if (assignSubjective(team.getGroup(), team.getIndex(), station, timeslot)) {
        final boolean result = scheduleNextStation();
        if (!result
            || optimize) {
          unassignSubjective(team.getGroup(), team.getIndex(), station, timeslot);
          if (timeslot
              + getSubjectiveDuration(station) >= getNumTimeslots()) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Hit max timeslots - subj");
            }
            return false;
          }
        } else {
          return true;
        }
      }
    }
    return false;
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

  /**
   * Solve the problem.
   * 
   * @return the number of solutions found
   */
  public int solve() {
    LOGGER.info("Starting solve");
    scheduleNextStation();

    if (solutionsFound < 1) {
      LOGGER.info("Infeasible problem, no solutions found");
    } else {
      LOGGER.info("Found "
          + solutionsFound + " solutions");
    }
    return solutionsFound;
  }

  private int getNumWarnings(final File scheduleFile) {
    final List<SubjectiveStation> subjectiveParams = new LinkedList<SubjectiveStation>();
    final Collection<String> subjectiveHeaders = new LinkedList<String>();
    for (int subj = 0; subj < getNumSubjectiveStations(); ++subj) {
      final String header = "Subj"
          + (subj + 1);
      subjectiveHeaders.add(header);
      final SubjectiveStation station = new SubjectiveStation(header, getSubjectiveDuration(subj)
          * tinc);
      subjectiveParams.add(station);
    }

    try {
      final TournamentSchedule schedule = new TournamentSchedule(datafile.getName(), scheduleFile, subjectiveHeaders);

      final SchedParams params = new SchedParams(subjectiveParams, getPerformanceDuration()
          * tinc, getChangetime()
          * tinc, getPerformanceChangetime()
          * tinc);
      final ScheduleChecker checker = new ScheduleChecker(params, schedule);
      final List<ConstraintViolation> violations = checker.verifySchedule();
      for (final ConstraintViolation violation : violations) {
        if (violation.isHard()) {
          throw new FLLRuntimeException("Should not have any hard constraint violations from autosched: "
              + violation.getMessage());
        }
      }
      return violations.size();
    } catch (final IOException e) {
      throw new FLLRuntimeException("Should not have an IOException trying to get warnings from CSV file", e);
    } catch (ParseException e) {
      throw new FLLRuntimeException("Should not have an ParseException trying to get warnings from CSV file", e);
    } catch (ScheduleParseException e) {
      throw new FLLRuntimeException("Should not have an ScheduleParseException trying to get warnings from CSV file", e);
    }
  }

  private ObjectiveValue computeObjectiveValue(final File scheduleFile) {
    final int[] numTeams = new int[getNumGroups()];
    final int[] latestSubjectiveTime = new int[getNumGroups()];
    for (int group = 0; group < numTeams.length; ++group) {
      numTeams[group] = subjectiveScheduled[group].length;
      latestSubjectiveTime[group] = findLatestSubjectiveTime(group);
    }
    return new ObjectiveValue(solutionsFound, findLatestPerformanceTime(), numTeams, latestSubjectiveTime,
                              getNumWarnings(scheduleFile));
  }

  /**
   * The slot that has the latest subjective time for a group of teams.
   */
  private int findLatestSubjectiveTime(final int group) {
    for (int slot = getNumTimeslots() - 1; slot >= 0; --slot) {
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
    for (int slot = getNumTimeslots() - 1; slot >= 0; --slot) {
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

  private boolean scheduleNextStation() {
    if (scheduleFinished()) {
      ++solutionsFound;

      LOGGER.info("Schedule finished num solutions: "
          + solutionsFound);
      outputCurrentSolution();

      return true;
    }

    // find possible values
    int nextAvailablePerfSlot = Integer.MAX_VALUE;
    int nextAvailableSubjSlot = Integer.MAX_VALUE;
    final List<Integer> possibleSubjectiveStations = new ArrayList<Integer>();
    final List<Integer> subjectiveGroups = new ArrayList<Integer>();
    for (int group = 0; group < getNumGroups(); ++group) {
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

    final List<Integer> possiblePerformanceTables = new LinkedList<Integer>();
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

    if (Math.min(nextAvailablePerfSlot, nextAvailableSubjSlot) >= getNumTimeslots()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Hit max timeslots");
      }
      return false;
    }

    // try possible values
    if (nextAvailableSubjSlot < nextAvailablePerfSlot) {
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
      // need to undo assignments
      for (int i = 0; i < possibleSubjectiveStations.size(); ++i) {
        final int station = possibleSubjectiveStations.get(i);
        final int group = subjectiveGroups.get(i);
        subjectiveStations[group][station] -= getSubjectiveAttemptOffset();
      }

      for (final int table : possiblePerformanceTables) {
        performanceTables[table] -= getPerformanceAttemptOffset();
      }
    }
    return result;

  }

  private void outputCurrentSolution() {
    final File scheduleFile = new File(datafile.getAbsolutePath()
        + "-" + solutionsFound + ".csv");
    final File objectiveFile = new File(datafile.getAbsolutePath()
        + "-" + solutionsFound + ".obj");

    LOGGER.info("Solution output to "
        + scheduleFile.getAbsolutePath());

    try {
      outputSchedule(scheduleFile);
    } catch (final IOException ioe) {
      throw new FLLRuntimeException("Error writing schedule", ioe);
    }

    final ObjectiveValue objective = computeObjectiveValue(scheduleFile);

    FileWriter objectiveWriter = null;
    try {
      objectiveWriter = new FileWriter(objectiveFile);
      objectiveWriter.write(objective.toString());
      objectiveWriter.close();
    } catch (final IOException e) {
      throw new FLLRuntimeException("Error writing objective", e);
    } finally {
      try {
        if (null != objectiveWriter) {
          objectiveWriter.close();
        }
      } catch (final IOException e2) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(e2);
        }
      }
    }

    if (null == bestObjective
        || -1 == objective.compareTo(bestObjective)) {
      LOGGER.info("Schedule provides a better objective value");
      bestObjective = objective;
      // tighten down the constraints so that we find a better solution
      final int newNumTimeslots = objective.getLatestPerformanceTime() + 1;
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Tightening numTimeslots from "
            + numTimeslots + " to " + newNumTimeslots);
      }
      numTimeslots = newNumTimeslots;
    }
  }

  private final int numSubjectiveStations;

  private int getNumSubjectiveStations() {
    return numSubjectiveStations;
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
    CSVWriter csv = null;
    try {
      csv = new CSVWriter(new FileWriter(schedule));
      final List<String> line = new ArrayList<String>();
      line.add(TournamentSchedule.TEAM_NUMBER_HEADER);
      line.add(TournamentSchedule.DIVISION_HEADER);
      line.add(TournamentSchedule.TEAM_NAME_HEADER);
      line.add(TournamentSchedule.ORGANIZATION_HEADER);
      line.add(TournamentSchedule.JUDGE_GROUP_HEADER);
      for (int subj = 0; subj < getNumSubjectiveStations(); ++subj) {
        line.add("Subj"
            + (subj + 1));
      }
      for (int round = 0; round < getNumPerformanceRounds(); ++round) {
        line.add(String.format(TournamentSchedule.PERF_HEADER_FORMAT, round + 1));
        line.add(String.format(TournamentSchedule.TABLE_HEADER_FORMAT, round + 1));
      }
      csv.writeNext(line.toArray(new String[line.size()]));
      line.clear();

      for (final SchedTeam team : getAllTeams()) {
        final int teamNum = (team.getGroup() + 1)
            * 100 + team.getIndex();
        final int judgingGroup = team.getGroup() + 1;
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
                + (team.getGroup() + 1) + " team: " + (team.getIndex() + 1) + " subj: " + (subj + 1));
          }
          line.add(TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(time));
        }

        // find all performances for a team and then sort by time
        final SortedSet<PerformanceTime> perfTimes = new TreeSet<PerformanceTime>();
        for (int round = 0; round < getNumPerformanceRounds(); ++round) {
          for (int table = 0; table < getNumTables(); ++table) {
            for (int side = 0; side < 2; ++side) {
              final Date time = getTime(pz[team.getGroup()][team.getIndex()][table][side], round + 1);
              if (null != time) {
                perfTimes.add(new PerformanceTime(time, "Table"
                    + (table + 1), (side + 1)));
              }
            }
          }
        }
        if (perfTimes.size() != getNumPerformanceRounds()) {
          throw new FLLRuntimeException("Expecting "
              + getNumPerformanceRounds() + " performance times, but found " + perfTimes.size() + " group: "
              + (team.getGroup() + 1) + " team: " + (team.getIndex() + 1) + " perfs: " + perfTimes);
        }
        for (final PerformanceTime perfTime : perfTimes) {
          line.add(TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(perfTime.getTime()));
          line.add(perfTime.getTable()
              + " " + perfTime.getSide());
        }

        csv.writeNext(line.toArray(new String[line.size()]));
        line.clear();
      }
    } finally {
      if (null != csv) {
        csv.close();
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
          cal.setTime(startTime);
          cal.add(Calendar.MINUTE, i
              * tinc);
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
