/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import fll.Team;
import fll.Utilities;
import fll.scheduler.TournamentSchedule.ColumnInformation;
import fll.util.CSVCellReader;
import fll.util.CellFileReader;
import fll.util.CheckCanceled;
import fll.util.ExcelCellReader;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;

/**
 * Optimize a schedule by rearranging the sides of tables used and the tables
 * used. No times will be changed.
 */
public class TableOptimizer {

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final String SCHED_FILE_OPTION = "s";

  private final TournamentSchedule schedule;

  private final File basedir;

  private int numSolutions = 0;

  private File mBestScheduleOutputFile = null;

  private Map<PerformanceTime, Integer> bestPermutation = null;

  private int bestScore;

  /**
   * List of table colors from the schedule. Each list inside the list is a
   * group of tables that are scheduled together.
   * If not alternating tables then the outer list will have a length of 1.
   */
  private final List<List<String>> tableGroups;

  /**
   * The best schedule found so far. Starts out at null and
   * is modified by {@link #optimize(CheckCanceled)}.
   * 
   * @return the file containing the best schedule or null if no such schedule
   *         has been found
   */
  public File getBestScheduleOutputFile() {
    return mBestScheduleOutputFile;
  }

  private final ScheduleChecker checker;

  private static boolean isPerformanceViolation(final ConstraintViolation violation) {
    return null != violation.getPerformance();
  }

  /**
   * Compute score for the current schedule. The lowest score is best.
   */
  private int computeScheduleScore() {
    final int numWarnings = checker.verifySchedule().size();

    final int tableUseScore = computeTableUseScore();

    // warnings is most important, then table use
    return numWarnings
        * 1000
        + tableUseScore;
  }

  /**
   * Compute the table use score. This is the difference between the minimum
   * number of times any table is used and the maximum number of times any table
   * is used. This should even out the table use.
   * 
   * @return score, lower is better
   */
  private int computeTableUseScore() {
    final Map<String, Integer> tableUse = new HashMap<>();
    for (final TeamScheduleInfo ti : this.schedule.getSchedule()) {
      for (int round = 0; round < ti.getNumberOfRounds(); ++round) {
        final String tableColor = ti.getPerfTableColor(round);
        int count;
        if (tableUse.containsKey(tableColor)) {
          count = tableUse.get(tableColor);
        } else {
          count = 0;
        }
        ++count;
        tableUse.put(tableColor, count);
      } // foreach round
    } // foreach team

    int minUse = Integer.MAX_VALUE;
    int maxUse = 0;
    for (Map.Entry<String, Integer> entry : tableUse.entrySet()) {
      minUse = Math.min(minUse, entry.getValue());
      maxUse = Math.max(maxUse, entry.getValue());
    } // foreach table

    if (0 == maxUse) {
      return 0;
    } else {
      return maxUse
          - minUse;
    }
  }

  /**
   * Get the current table assignments for the specified time so that
   * they can be re-applied if needed.
   * 
   * @return key=table info, value=team number
   */
  private Map<PerformanceTime, Integer> getCurrentTableAssignments(final LocalTime time) {
    final Map<PerformanceTime, Integer> assignments = new HashMap<>();

    for (final TeamScheduleInfo si : this.schedule.getSchedule()) {
      for (int round = 0; round < this.schedule.getNumberOfRounds(); ++round) {
        final PerformanceTime pt = si.getPerf(round);
        if (time.equals(pt.getTime())) {
          assignments.put(pt, si.getTeamNumber());
        }
      }
    }

    return assignments;
  }

  /**
   * Compute the best table ordering for a set of teams at the
   * specified time.
   * 
   * @param checkCanceled if non-null, check if the optimization should finish
   *          early
   * @return best score found
   */
  private void computeBestTableOrdering(final List<Integer> teams,
                                        final LocalTime time,
                                        final List<String> tables,
                                        final CheckCanceled checkCanceled) {
    if (teams.isEmpty()) {
      throw new IllegalArgumentException("Must have some teams to check");
    }

    if (null == bestPermutation) {
      bestPermutation = getCurrentTableAssignments(time);
      bestScore = computeScheduleScore();
    }

    final List<Map<PerformanceTime, Integer>> possibleValues = computePossibleValues(teams, time, tables);
    for (final Map<PerformanceTime, Integer> possibleValue : possibleValues) {
      if (null != checkCanceled
          && checkCanceled.isCanceled()) {
        // user interrupt
        break;
      }

      applyPerformanceOrdering(possibleValue);

      // check for better value
      final int score = computeScheduleScore();
      if (score < bestScore) {
        try {
          final File outputFile = new File(basedir, String.format("%s-opt-%d.csv", schedule.getName(), numSolutions));
          LOGGER.info(String.format("Found better schedule (%d -> %d), writing to: %s", bestScore, score,
                                    outputFile.getAbsolutePath()));
          schedule.writeToCSV(outputFile);

          ++numSolutions;

          if (null != mBestScheduleOutputFile) {
            if (!mBestScheduleOutputFile.delete()) {
              mBestScheduleOutputFile.deleteOnExit();
            }
          }
          mBestScheduleOutputFile = outputFile;
        } catch (final IOException e) {
          throw new RuntimeException(e);
        }

        bestPermutation = possibleValue;
        bestScore = score;

        if (bestScore == 0) {
          break;
        }
      }
    }

    // assign the best value
    applyPerformanceOrdering(bestPermutation);
  }

  /**
   * Compute all possible orderings of teams on the current set
   * of tables.
   * 
   * @param teams
   * @return list of possible orderings, key=table information, value=team
   *         number
   */
  private List<Map<PerformanceTime, Integer>> computePossibleValues(final List<Integer> teams,
                                                                    final LocalTime time,
                                                                    final List<String> tables) {
    final List<Map<PerformanceTime, Integer>> possibleValues = new LinkedList<>();

    final boolean oddNumberOfTeams = Utilities.isOdd(teams.size());

    final List<List<Integer>> possibleTableOrderings = permutate(tables.size()
        * 2);
    for (final List<Integer> ordering : possibleTableOrderings) {
      if (ordering.size() != tables.size()
          * 2) {
        throw new FLLInternalException(String.format("All possible orderings must be twice the number of tables. ordering.size: %d tableColors: %d",
                                                     ordering.size(), tables.size()));
      }

      boolean validToHaveNullTeam = oddNumberOfTeams;

      // convert to valid ordering
      final Map<PerformanceTime, Integer> assignments = new HashMap<>();
      for (int tableColorIndex = 0; tableColorIndex < tables.size(); ++tableColorIndex) {
        final String tableColor = tables.get(tableColorIndex);

        // check in pairs to make sure we have a valid table assignment
        final int side1 = 1;
        final int orderIndex1 = tableColorIndex
            * 2;
        final int teamIndex1 = ordering.get(orderIndex1);
        final int teamNumber1 = getTeamNumber(teams, teamIndex1);

        final int side2 = 2;
        final int orderIndex2 = orderIndex1
            + 1;
        final int teamIndex2 = ordering.get(orderIndex2);
        final int teamNumber2 = getTeamNumber(teams, teamIndex2);

        if (Team.NULL_TEAM_NUMBER != teamNumber1
            && Team.NULL_TEAM_NUMBER != teamNumber2) {
          assignments.put(new PerformanceTime(time, tableColor, side1), teamNumber1);
          assignments.put(new PerformanceTime(time, tableColor, side2), teamNumber2);
        } else if (Team.NULL_TEAM_NUMBER != teamNumber1
            && Team.NULL_TEAM_NUMBER == teamNumber2
            && validToHaveNullTeam) {
          assignments.put(new PerformanceTime(time, tableColor, side1), teamNumber1);

          // can only have 1 uneven pairing at a time
          validToHaveNullTeam = false;
        } else if (Team.NULL_TEAM_NUMBER == teamNumber1
            && Team.NULL_TEAM_NUMBER != teamNumber2
            && validToHaveNullTeam) {
          assignments.put(new PerformanceTime(time, tableColor, side2), teamNumber2);

          // can only have 1 uneven pairing at a time
          validToHaveNullTeam = false;
        }

        if (!assignments.isEmpty()) {
          possibleValues.add(assignments);
        }
      } // foreach table

    } // foreach possible ordering

    return possibleValues;
  }

  /**
   * Get team number from teams using teamIndex.
   * 
   * @param teams list of teams
   * @param teamIndex index into teams
   * @return the team number or {@see Team#NULL_TEAM_NUMBER} if the index is
   *         larger than the list of teams
   */
  private static int getTeamNumber(final List<Integer> teams,
                                   final int teamIndex) {
    final int teamNumber;
    if (teamIndex < teams.size()) {
      teamNumber = teams.get(teamIndex);
    } else {
      teamNumber = Team.NULL_TEAM_NUMBER;
    }
    return teamNumber;
  }

  private void applyPerformanceOrdering(final Map<PerformanceTime, Integer> possibleValue) {
    for (Map.Entry<PerformanceTime, Integer> entry : possibleValue.entrySet()) {
      final int teamNumber = entry.getValue();
      final PerformanceTime perfTime = entry.getKey();

      // can use perfTime.getTime() as oldTime since we know that we're just
      // moving teams across tables
      schedule.reassignTable(teamNumber, perfTime.getTime(), perfTime);
    }

  }

  /**
   * Compute permutations of the integers [0, numElements]
   * 
   * @param numElements how many elements to be permuted
   * @return all possible orderings
   */
  static public List<List<Integer>> permutate(final int numElements) {
    final List<Integer> allElements = new ArrayList<>();
    for (int i = 0; i < numElements; ++i) {
      allElements.add(i);
    }

    final List<Integer> order = new ArrayList<>(numElements);
    for (int i = 0; i < numElements; ++i) {
      order.add(i);
    }

    final List<List<Integer>> permutations = new LinkedList<>();
    permutate(numElements, allElements, order, permutations);
    return permutations;
  }

  /**
   * Recursive function that computes permutations. To
   * be called from {@see #permutate(int)}.
   * 
   * @param arrayCount
   * @param elements the elements to compute permutations of
   * @param order
   * @param permutations the resulting permutations
   */
  static private void permutate(final int arrayCount,
                                final List<Integer> elements,
                                final List<Integer> order,
                                final List<List<Integer>> permutations) {
    if (elements.isEmpty()) {
      throw new IllegalArgumentException("Cannot permutate 0 elements");
    }

    final int position = arrayCount
        - elements.size();

    if (elements.size() == 1) {
      order.set(position, elements.get(0));
      permutations.add(order);
    } else {
      for (int i = 0; i < elements.size(); ++i) {
        final int element = elements.get(i);
        final List<Integer> newOrder = new ArrayList<Integer>(order);
        newOrder.set(position, element);

        final List<Integer> newElements = new ArrayList<Integer>(elements);
        newElements.remove(i);
        permutate(arrayCount, newElements, newOrder, permutations);
      }
    }
  }

  /**
   * Gather up all performance times in the specified list of violations.
   */
  private Set<LocalTime> gatherPerformanceTimes(final Collection<ConstraintViolation> violations) {
    final Set<LocalTime> perfTimes = new HashSet<>();
    for (final ConstraintViolation violation : violations) {
      final LocalTime d = violation.getPerformance();
      if (null != d) {
        perfTimes.add(d);
      }
    }
    return perfTimes;
  }

  /**
   * Pick team with most violations and isn't in the set of optimizedTeams.
   */
  private List<ConstraintViolation> pickTeamWithMostViolations(final Set<Integer> optimizedTeams) {
    final List<ConstraintViolation> violations = checker.verifySchedule();
    // team->violations
    final Map<Integer, List<ConstraintViolation>> teamViolations = new HashMap<Integer, List<ConstraintViolation>>();
    for (final ConstraintViolation violation : violations) {
      if (isPerformanceViolation(violation)) {
        final List<ConstraintViolation> vs;
        if (teamViolations.containsKey(violation.getTeam())) {
          vs = teamViolations.get(violation.getTeam());
        } else {
          vs = new LinkedList<ConstraintViolation>();
          teamViolations.put(violation.getTeam(), vs);
        }
        vs.add(violation);
      }
    }

    // find max
    List<ConstraintViolation> retval = new LinkedList<ConstraintViolation>();
    for (final Map.Entry<Integer, List<ConstraintViolation>> entry : teamViolations.entrySet()) {
      if (!optimizedTeams.contains(entry.getKey())) {
        if (entry.getValue().size() > retval.size()) {
          retval = entry.getValue();
        }
      }
    }

    return retval;
  }

  private static Options buildOptions() {
    final Options options = new Options();
    Option option = new Option(SCHED_FILE_OPTION, "schedfile", true, "<file> the schedule file ");
    option.setRequired(true);
    options.addOption(option);

    return options;
  }

  private static void usage(final Options options) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("TableOptimizer", options);
  }

  /**
   * @param args
   */
  public static void main(final String[] args) {
    LogUtils.initializeLogging();

    File schedfile = null;
    final Options options = buildOptions();
    try {
      final CommandLineParser parser = new PosixParser();
      final CommandLine cmd = parser.parse(options, args);

      schedfile = new File(cmd.getOptionValue(SCHED_FILE_OPTION));

    } catch (final org.apache.commons.cli.ParseException pe) {
      LOGGER.error(pe.getMessage());
      usage(options);
      System.exit(1);
    }

    FileInputStream fis = null;
    try {
      if (!schedfile.canRead()) {
        LOGGER.fatal(schedfile.getAbsolutePath()
            + " is not readable");
        System.exit(4);
      }

      final boolean csv = schedfile.getName().endsWith("csv");
      final CellFileReader reader;
      final String sheetName;
      if (csv) {
        reader = new CSVCellReader(schedfile);
        sheetName = null;
      } else {
        sheetName = SchedulerUI.promptForSheetName(schedfile);
        if (null == sheetName) {
          return;
        }
        fis = new FileInputStream(schedfile);
        reader = new ExcelCellReader(fis, sheetName);
      }

      final ColumnInformation columnInfo = TournamentSchedule.findColumns(reader, new LinkedList<String>());
      if (null != fis) {
        fis.close();
        fis = null;
      }

      final List<SubjectiveStation> subjectiveStations = SchedulerUI.gatherSubjectiveStationInformation(null,
                                                                                                        columnInfo);

      // not bothering to get the schedule params as we're just tweaking table
      // assignments, which wont't be effected by the schedule params.
      final SchedParams params = new SchedParams(subjectiveStations, SchedParams.DEFAULT_PERFORMANCE_MINUTES,
                                                 SchedParams.MINIMUM_CHANGETIME_MINUTES,
                                                 SchedParams.MINIMUM_PERFORMANCE_CHANGETIME_MINUTES);
      final List<String> subjectiveHeaders = new LinkedList<String>();
      for (final SubjectiveStation station : subjectiveStations) {
        subjectiveHeaders.add(station.getName());
      }

      final String name = Utilities.extractBasename(schedfile);

      final TournamentSchedule schedule;
      if (csv) {
        schedule = new TournamentSchedule(name, schedfile, subjectiveHeaders);
      } else {
        fis = new FileInputStream(schedfile);
        schedule = new TournamentSchedule(name, fis, sheetName, subjectiveHeaders);
      }

      final TableOptimizer optimizer = new TableOptimizer(params, schedule,
                                                          schedfile.getAbsoluteFile().getParentFile());
      final long start = System.currentTimeMillis();
      optimizer.optimize(null);
      final long stop = System.currentTimeMillis();
      LOGGER.info("Optimization took: "
          + (stop
              - start)
              / 1000.0
          + " seconds");

    } catch (final ParseException e) {
      LOGGER.fatal(e, e);
      System.exit(5);
    } catch (final ScheduleParseException e) {
      LOGGER.fatal(e, e);
      System.exit(6);
    } catch (final IOException e) {
      LOGGER.fatal("Error reading file", e);
      System.exit(4);
    } catch (final RuntimeException e) {
      LOGGER.fatal(e, e);
      throw e;
    } catch (InvalidFormatException e) {
      LOGGER.fatal(e, e);
      System.exit(7);
    } finally {
      try {
        if (null != fis) {
          fis.close();
        }
      } catch (final IOException e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Error closing stream", e);
        }
      }
    }

  }

  /**
   * Compute map of tables at each time.
   * 
   * @param schedule the schedule to work with
   * @return key=time, value=tables used at this time
   */
  private static Map<LocalTime, Set<String>> gatherTablesAtTime(final TournamentSchedule schedule) {
    final Map<LocalTime, Set<String>> tablesAtTime = new HashMap<>();

    for (int round = 0; round < schedule.getNumberOfRounds(); ++round) {
      for (final TeamScheduleInfo si : schedule.getSchedule()) {
        final PerformanceTime perf = si.getPerf(round);

        Set<String> tables;
        if (tablesAtTime.containsKey(perf.getTime())) {
          tables = tablesAtTime.get(perf.getTime());
        } else {
          tables = new HashSet<>();
        }
        tables.add(perf.getTable());
        tablesAtTime.put(perf.getTime(), tables);
      }
    }

    return tablesAtTime;
  }

  /**
   * Check if any elements in set2 are in set1.
   */
  private static boolean containsAny(final Collection<String> set1,
                                     final Collection<String> set2) {
    for (final String needle : set2) {
      if (set1.contains(needle)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Walk over the schedule and figure out how tables are grouped. If the
   * schedule uses alternating tables the returned list will have more than 1
   * element.
   */
  private static List<List<String>> determineTableGroups(final TournamentSchedule schedule) {
    final Map<LocalTime, Set<String>> tablesAtTime = gatherTablesAtTime(schedule);

    final List<Set<String>> tableGroups = new ArrayList<>();
    for (final Map.Entry<LocalTime, Set<String>> entry : tablesAtTime.entrySet()) {
      final Set<String> toFind = entry.getValue();

      boolean found = false;
      for (int i = 0; i < tableGroups.size()
          && !found; ++i) {
        final Set<String> group = tableGroups.get(i);
        if (containsAny(group, toFind)) {
          group.addAll(toFind);
          found = true;
        }
      } // foreach known table group

      if (!found) {
        // create new grouping
        tableGroups.add(toFind);
      }
    } // foreach group of tables in the schedule

    // consolidate the existing groups
    final List<List<String>> finalGroups = new ArrayList<>();
    if (tableGroups.size() > 1) {
      final List<String> firstGroup = new ArrayList<>(tableGroups.remove(0));
      finalGroups.add(firstGroup);

      while (!tableGroups.isEmpty()) {
        final List<String> toFind = new ArrayList<>(tableGroups.remove(0));

        boolean found = false;
        for (int i = 0; i < finalGroups.size()
            && !found; ++i) {
          final List<String> group = finalGroups.get(i);
          if (containsAny(group, toFind)) {
            group.addAll(toFind);
            found = true;
          }
        } // foreach known table group

        if (!found) {
          // create new grouping
          finalGroups.add(toFind);
        }
      }

    } else {
      for (final Set<String> group : tableGroups) {
        finalGroups.add(new ArrayList<String>(group));
      }
    }
    return finalGroups;

  }

  /**
   * @param params the schedule parameters
   * @param schedule the schedule to optimize (will be modified)
   * @param basedir the directory to store better schedules in
   * @throws IllegalArgumentException if the schedule has hard constraint
   *           violations
   */
  public TableOptimizer(final SchedParams params,
                        final TournamentSchedule schedule,
                        final File basedir)
      throws IllegalArgumentException {
    this.schedule = schedule;
    this.basedir = basedir;
    this.checker = new ScheduleChecker(params, schedule);
    this.tableGroups = determineTableGroups(schedule);

    if (tableGroups.isEmpty()) {
      throw new FLLInternalException("Something went wrong. Table groups list is empty");
    }

    final List<ConstraintViolation> violations = checker.verifySchedule();
    for (final ConstraintViolation v : violations) {
      if (ConstraintViolation.Type.HARD == v.getType()) {
        throw new IllegalArgumentException("Should not have any hard constraint violations: "
            + v.getMessage());
      }
    }

    if (!this.basedir.isDirectory()) {
      throw new IllegalArgumentException("Basedir must be a directory");
    }
  }

  /**
   * Run the table optimizer.
   * 
   * @param checkCanceled if non-null, checked to see if the optimizer should
   *          exit early
   */
  public void optimize(final CheckCanceled checkCanceled) {
    final Set<Integer> optimizedTeams = new HashSet<Integer>();
    final Set<LocalTime> optimizedTimes = new HashSet<>();

    List<ConstraintViolation> teamViolations = pickTeamWithMostViolations(optimizedTeams);
    while ((null != checkCanceled
        && !checkCanceled.isCanceled())
        && !teamViolations.isEmpty()) {
      final int team = teamViolations.get(0).getTeam();
      optimizedTeams.add(team);

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Optimize tables for team: "
            + team);
      }

      final Set<LocalTime> perfTimes = gatherPerformanceTimes(teamViolations);
      optimize(perfTimes, checkCanceled);

      optimizedTimes.addAll(perfTimes);

      teamViolations = pickTeamWithMostViolations(optimizedTeams);
    } // while team violations

    if (null != checkCanceled
        && !checkCanceled.isCanceled()) {
      // optimize non-full table times if we haven't already touched them while
      // optimizing teams
      final Set<LocalTime> perfTimes = findNonFullTableTimes();
      perfTimes.removeAll(optimizedTimes);
      if (!perfTimes.isEmpty()) {
        optimize(perfTimes, checkCanceled);
      }
    }

  }

  /**
   * Find all times in the schedule where the number of teams
   * competing doesn't equal the number of tables available.
   */
  private Set<LocalTime> findNonFullTableTimes() {
    final Map<LocalTime, Integer> perfCounts = new HashMap<>();
    final Map<LocalTime, List<String>> perfTables = new HashMap<>();
    for (final TeamScheduleInfo ti : this.schedule.getSchedule()) {
      for (int round = 0; round < ti.getNumberOfRounds(); ++round) {
        final PerformanceTime pt = ti.getPerf(round);
        final LocalTime time = pt.getTime();

        List<String> tables = perfTables.get(time);
        for (int i = 0; null == tables
            && i < tableGroups.size(); ++i) {
          final List<String> group = this.tableGroups.get(i);
          if (group.contains(pt.getTable())) {
            tables = group;
          }
        }
        perfTables.put(time, tables);

        int count = 0;
        if (perfCounts.containsKey(time)) {
          count = perfCounts.get(time);
        }
        ++count;
        perfCounts.put(time, count);
      }
    }

    final Set<LocalTime> perfTimes = new HashSet<>();

    for (final Map.Entry<LocalTime, Integer> entry : perfCounts.entrySet()) {
      final LocalTime time = entry.getKey();
      final int useCount = entry.getValue();

      final List<String> tables = perfTables.get(time);
      if (tables.isEmpty()) {
        throw new FLLInternalException("No tables found at time: "
            + TournamentSchedule.formatTime(time));
      }

      // 2 teams on each table at a given time
      final int expectedTableUse = tables.size()
          * 2;

      if (useCount < expectedTableUse) {
        perfTimes.add(time);
      }
    }

    return perfTimes;
  }

  /**
   * Optimize the table use at the specified times.
   * 
   * @param perfTimes the times to optimize at
   * @param checkCancled used to check if the optimization should exit early
   */
  private void optimize(final Set<LocalTime> perfTimes,
                        final CheckCanceled checkCanceled) {
    for (final LocalTime time : perfTimes) {
      final List<Integer> teams = new ArrayList<Integer>();

      List<String> tables = null;
      for (final TeamScheduleInfo si : schedule.getSchedule()) {
        for (int round = 0; round < schedule.getNumberOfRounds(); ++round) {
          final PerformanceTime pt = si.getPerf(round);
          if (time.equals(pt.getTime())) {
            teams.add(si.getTeamNumber());

            // choose the tables to use for assignments
            if (null == tables) {
              for (int i = 0; null == tables
                  && i < tableGroups.size(); ++i) {
                final List<String> group = this.tableGroups.get(i);
                if (group.contains(pt.getTable())) {
                  tables = group;
                }
              }
              if (null == tables) {
                throw new FLLRuntimeException("Cannot find table group for "
                    + pt.getTable());
              }
            }

          }
        }
      } // foreach schedule item

      computeBestTableOrdering(teams, time, tables, checkCanceled);

    } // foreach time
  }

}
