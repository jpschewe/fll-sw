/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

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
  
  public static void main(final String[] args) {
    LogUtils.initializeLogging();

    if (args.length != 2) {
      LOGGER.fatal("You must specify the start time (HH:MM) and data file");
      System.exit(1);
    }

    try {
      final Date startTime = TournamentSchedule.OUTPUT_DATE_FORMAT.get().parse(args[0]);

      final File datafile = new File(args[1]);
      if (!datafile.canRead()) {
        LOGGER.fatal(datafile.getAbsolutePath()
            + " is not readable");
        System.exit(4);
      }

      final GreedySolver solver = new GreedySolver(startTime, datafile);
      final long start = System.currentTimeMillis();
      solver.solve();
      final long stop = System.currentTimeMillis();
      LOGGER.info("Solve took: "
          + (stop - start) / 1000.0 + " seconds");

    } catch (final IOException e) {
      LOGGER.fatal("Error reading file", e);
      System.exit(4);
    } catch (final ParseException e) {
      LOGGER.fatal(e.getMessage());
      System.exit(2);
    }
  }

  /**
   * @param datafile the minizinc datafile for the schedule to solve
   */
  public GreedySolver(final Date startTime,
                      final File datafile) throws IOException {
    this.datafile = datafile;

    this.startTime = startTime;

    final Properties properties = ParseMinizinc.parseMinizincData(datafile);
    LOGGER.debug(properties.toString());

    // TODO validate that all times are divisible by tinc

    tinc = ParseMinizinc.readIntProperty(properties, "TInc");
    ngroups = ParseMinizinc.readIntProperty(properties, "NGroups");

    numPerformanceRounds = ParseMinizinc.readIntProperty(properties, "NRounds");
    numTables = ParseMinizinc.readIntProperty(properties, "NTables");
    final int tmaxHours = ParseMinizinc.readIntProperty(properties, "TMax_hours");
    final int tmaxMinutes = ParseMinizinc.readIntProperty(properties, "TMax_minutes");
    numTimeslots = (tmaxHours * 60 + tmaxMinutes)
        / tinc;

    numSubjectiveStations = ParseMinizinc.readIntProperty(properties, "NSubjective");
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
      subjectiveDurations[station] = Integer.valueOf(subjDurs[station].trim())
          / tinc;
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
    sz = new boolean[groups.length][][][];
    sy = new boolean[groups.length][][][];
    pz = new boolean[groups.length][][][][];
    py = new boolean[groups.length][][][][];
    subjectiveScheduled = new boolean[groups.length][][];
    subjectiveStations = new int[groups.length][getNumSubjectiveStations()];
    performanceScheduled = new int[groups.length][];
    performanceTables = new int[getNumTables()];
    Arrays.fill(performanceTables, 0);
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

    performanceDuration = ParseMinizinc.readIntProperty(properties, "alpha_perf_minutes")
        / tinc;
    changetime = ParseMinizinc.readIntProperty(properties, "ct_minutes")
        / tinc;
    performanceChangetime = ParseMinizinc.readIntProperty(properties, "pct_minutes")
        / tinc;

    // sort list of teams to make sure that the scheduler is deterministic
    Collections.sort(teams, lowestTeamIndex);
  }

  private boolean assignSubjective(final int group,
                                   final int team,
                                   final int station,
                                   final int timeslot) {
    for (int otherCat = 0; otherCat < getNumSubjectiveStations(); ++otherCat) {
      if (!checkSubjFree(group, team, otherCat, timeslot, getSubjectiveDuration(station))) {
        return false;
      }
    }
    if (!checkPerfFree(group, team, timeslot, getSubjectiveDuration(station))) {
      return false;
    }
    // check all other teams at this station
    if (!checkSubjStationNoOverlap(group, station, timeslot)) {
      return false;
    }

    if (timeslot
        + getSubjectiveDuration(station) >= getNumTimeslots()) {
      return false;
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Assigning subjective group: "
          + group + " team: " + team + " station: " + station + " time: " + timeslot);
    }
    subjectiveScheduled[group][team][station] = true;
    sz[group][team][station][timeslot] = true;
    for (int slot = timeslot; slot < timeslot
        + getSubjectiveDuration(station); ++slot) {
      sy[group][team][station][slot] = true;
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
        for (int slot = timeslot; slot < timeslot
            + getSubjectiveDuration(station); ++slot) {
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
  private boolean checkPerfNoOverlap(final int group,
                                     final int table,
                                     final int side,
                                     final int timeslot) {
    for (final SchedTeam team : getAllTeams()) {
      if (team.getGroup() == group) {
        for (int slot = timeslot; slot < timeslot
            + getPerformanceDuration(); ++slot) {
          if (py[group][team.getIndex()][table][side][slot]) {
            return false;
          }
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
    for (int station = 0; station < getNumSubjectiveStations(); ++station) {
      if (!checkSubjFree(group, team, station, timeslot, getPerformanceDuration())) {
        return false;
      }
    }
    if (!checkPerfFree(group, team, timeslot, getPerformanceDuration())) {
      return false;
    }
    if (!checkPerfChangetime(group, team, timeslot)) {
      return false;
    }
    if (timeslot
        + getPerformanceDuration() >= getNumTimeslots()) {
      return false;
    }
    if (!checkPerfNoOverlap(group, table, side, timeslot)) {
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

  private void schedPerf(final int table,
                         final int timeslot) {
    final List<SchedTeam> teams = getPossiblePerformanceTeams();
    SchedTeam team1 = null;
    SchedTeam team2 = null;
    for (final SchedTeam team : teams) {
      if (null == team1) {
        if (assignPerformance(team.getGroup(), team.getIndex(), timeslot, table, 0)) {
          team1 = team;
        }
      } else if (null == team2) {
        if (assignPerformance(team.getGroup(), team.getIndex(), timeslot, table, 1)) {
          team2 = team;
          if (!scheduleNextStation()) {
            // if we get to this point we sould look for another solution
            unassignPerformance(team1.getGroup(), team1.getIndex(), timeslot, table, 0);
            unassignPerformance(team2.getGroup(), team2.getIndex(), timeslot, table, 1);
            team1 = null;
            team2 = null;
          } else {
            return;
          }
        }
      }
    }

    // undo partial assignment
    if (null != team1) {
      unassignPerformance(team1.getGroup(), team1.getIndex(), timeslot, table, 0);
    }
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

  private void schedSubj(final int group,
                         final int station,
                         final int timeslot) {
    final List<SchedTeam> teams = getPossibleSubjectiveTeams(group, station);
    for (final SchedTeam team : teams) {
      if (assignSubjective(team.getGroup(), team.getIndex(), station, timeslot)) {
        if (!scheduleNextStation()) {
          unassignSubjective(team.getGroup(), team.getIndex(), station, timeslot);
        } else {
          return;
        }
      }
    }
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
   * @return the number of solutions found
   */
  public int solve() {
    scheduleNextStation();
    
    if(solutionsFound < 1) {
      LOGGER.info("Infeasible problem, no solutions found");
    } else {
      LOGGER.info("Found " + solutionsFound + " solutions");
    }
    return solutionsFound;
  }

  private boolean scheduleNextStation() {
    if (scheduleFinished()) {
      ++solutionsFound;
      
      // TODO if solving for all schedules, need to track an index so that we
      // don't clobber files
      LOGGER.info("Schedule finished");
      try {
        outputSchedule();
      } catch (final IOException ioe) {
        throw new RuntimeException(ioe);
      }

      // TODO this is where we would decide if we're going to optimize or not,
      // if so, then tighten the objective bound and return false
      return true;
    }

    while (!scheduleFinished()) {
      int nextAvailableSlot = Integer.MAX_VALUE;
      int subjectiveStation = -1;
      int subjectiveGroup = -1;
      for (int group = 0; group < getNumGroups(); ++group) {
        for (int station = 0; station < getNumSubjectiveStations(); ++station) {
          if (subjectiveStations[group][station] < nextAvailableSlot) {
            nextAvailableSlot = subjectiveStations[group][station];
            subjectiveStation = station;
            subjectiveGroup = group;
          }
        }
      }

      int performanceTable = -1;
      for (int table = 0; table < getNumTables(); ++table) {
        if (performanceTables[table] < nextAvailableSlot) {
          nextAvailableSlot = performanceTables[table];
          performanceTable = table;
        }
      }

      if (nextAvailableSlot >= getNumTimeslots()) {
        // no more room
        LOGGER.info("Hit max timeslots");
        return false;
      }

      if (-1 == performanceTable) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("subjective group: "
              + subjectiveGroup + " station: " + subjectiveStation + " next available: " + nextAvailableSlot);
        }

        subjectiveStations[subjectiveGroup][subjectiveStation] += getSubjectiveAttemptOffset();
        schedSubj(subjectiveGroup, subjectiveStation, nextAvailableSlot);
      } else {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("performance table: "
              + performanceTable + " next available: " + nextAvailableSlot);
        }

        performanceTables[performanceTable] += getPerformanceAttemptOffset();
        schedPerf(performanceTable, nextAvailableSlot);
      }
    }

    // TODO if we want to try all possibilities this should return false
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Hit end of scheduleNextStation");
    }
    return true;
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
    return 1;
  }

  /**
   * Number of timeslots to increment by when trying the next performance time
   * slot. To try all possible combinations, this should be set to 1.
   */
  private int getPerformanceAttemptOffset() {
    return 1;
  }

  private final int numTimeslots;

  /**
   * The number of timeslots available to schedule in.
   */
  private int getNumTimeslots() {
    return numTimeslots;
  }

  private void outputSchedule() throws IOException {
    final File schedule = new File(datafile.getAbsolutePath() + "-" + solutionsFound
        + ".csv");
    LOGGER.info("Solution output to "
        + schedule.getAbsolutePath());

    BufferedWriter writer = null;
    try {
      writer = new BufferedWriter(new FileWriter(schedule));
      writer.write(TournamentSchedule.TEAM_NUMBER_HEADER
          + ",");
      writer.write(TournamentSchedule.TEAM_NAME_HEADER
          + ",");
      writer.write(TournamentSchedule.ORGANIZATION_HEADER
          + ",");
      writer.write(TournamentSchedule.DIVISION_HEADER
          + ",");
      writer.write(TournamentSchedule.JUDGE_GROUP_HEADER);
      for (int subj = 0; subj < getNumSubjectiveStations(); ++subj) {
        writer.write(",Subj"
            + (subj + 1));
      }
      for (int round = 0; round < getNumPerformanceRounds(); ++round) {
        writer.write(","
            + String.format(TournamentSchedule.PERF_HEADER_FORMAT, round + 1));
        writer.write(","
            + String.format(TournamentSchedule.TABLE_HEADER_FORMAT, round + 1));
      }
      writer.newLine();

      for (final SchedTeam team : getAllTeams()) {
        final int teamNum = (team.getGroup() + 1)
            * 100 + team.getIndex();
        final int judgingGroup = team.getGroup() + 1;
        writer.write(String.format("%d,Team %d, Org %d, D%d, G%d", teamNum, teamNum, teamNum, judgingGroup,
                                   judgingGroup));
        for (int subj = 0; subj < getNumSubjectiveStations(); ++subj) {
          final Date time = getTime(sz[team.getGroup()][team.getIndex()][subj], 1);
          if (null == time) {
            throw new RuntimeException("Could not find a subjective start for group: "
                + (team.getGroup() + 1) + " team: " + (team.getIndex() + 1) + " subj: " + (subj + 1));
          }
          writer.write(",");
          writer.write(TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(time));
        }

        // find all performances for a team and then sort by time
        final SortedSet<PerformanceTime> perfTimes = new TreeSet<PerformanceTime>();
        for (int round = 0; round < getNumPerformanceRounds(); ++round) {
          for (int table = 0; table < getNumTables(); ++table) {
            for (int side = 0; side < 2; ++side) {
              final Date time = getTime(pz[team.getGroup()][team.getIndex()][table][side], round + 1);
              if (null != time) {
                perfTimes.add(new PerformanceTime(round, time, "Table"
                    + (table + 1), (side + 1)));
              }
            }
          }
        }
        if (perfTimes.size() != getNumPerformanceRounds()) {
          throw new FLLRuntimeException("Expecting "
              + getNumPerformanceRounds() + " performance times, but only found " + perfTimes.size() + " group: "
              + (team.getGroup() + 1) + " team: " + (team.getIndex() + 1));
        }
        for (final PerformanceTime perfTime : perfTimes) {
          writer.write(",");
          writer.write(TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(perfTime.getTime()));
          writer.write(",");
          writer.write(perfTime.getTable()
              + " " + perfTime.getSide());
        }

        writer.newLine();
      }
    } finally {
      if (null != writer) {
        writer.close();
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

}
