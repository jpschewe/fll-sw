/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.checkerframework.checker.nullness.qual.Nullable;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Utilities;
import fll.scheduler.TournamentSchedule.ColumnInformation;
import fll.util.CellFileReader;
import fll.util.CheckCanceled;
import fll.util.ExcelCellReader;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;

/**
 * Optimize a schedule by rearranging the sides of tables used and the tables
 * used. No times will be changed.
 */
public class TableOptimizer {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static final String SCHED_FILE_OPTION = "s";

  private final TournamentSchedule schedule;

  private final File basedir;

  private int numSolutions = 0;

  private @Nullable File mBestScheduleOutputFile = null;

  private Map<PerformanceTime, TeamScheduleInfo> bestPermutation = Collections.emptyMap();

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
  public @Nullable File getBestScheduleOutputFile() {
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

      ti.allPerformances().forEach(performance -> {
        final String tableColor = performance.getTable();
        int count;
        if (tableUse.containsKey(tableColor)) {
          count = tableUse.get(tableColor);
        } else {
          count = 0;
        }
        ++count;
        tableUse.put(tableColor, count);
      });
    } // foreach team

    int minUse = Integer.MAX_VALUE;
    int maxUse = 0;
    for (final Map.Entry<String, Integer> entry : tableUse.entrySet()) {
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
  private Map<PerformanceTime, TeamScheduleInfo> getCurrentTableAssignments(final LocalTime time) {
    final Map<PerformanceTime, TeamScheduleInfo> assignments = new HashMap<>();

    for (final TeamScheduleInfo si : this.schedule.getSchedule()) {
      si.allPerformances().filter(pt -> pt.getTime().equals(time)).forEach(pt -> assignments.put(pt, si));
    }

    return assignments;
  }

  /**
   * Compute the best table ordering for a set of teams at the
   * specified time.
   *
   * @param checkCanceled if non-null, check if the optimization should finish
   *          early
   */
  private void computeBestTableOrdering(final Map<TeamScheduleInfo, PerformanceTime> originalValues,
                                        final LocalTime time,
                                        final List<String> tables,
                                        final CheckCanceled checkCanceled) {
    if (originalValues.isEmpty()) {
      throw new IllegalArgumentException("Must have some teams to check");
    }

    if (bestPermutation.isEmpty()) {
      bestPermutation = getCurrentTableAssignments(time);
      bestScore = computeScheduleScore();
    }

    final List<Map<PerformanceTime, TeamScheduleInfo>> possibleValues = computePossibleValues(originalValues, time,
                                                                                              tables);
    for (final Map<PerformanceTime, TeamScheduleInfo> possibleValue : possibleValues) {
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
   *         schedule information
   */
  private List<Map<PerformanceTime, TeamScheduleInfo>> computePossibleValues(final Map<TeamScheduleInfo, PerformanceTime> originalValues,
                                                                             final LocalTime time,
                                                                             final List<String> tables) {
    final List<TeamScheduleInfo> teams = new LinkedList<>(originalValues.keySet());

    final List<Map<PerformanceTime, TeamScheduleInfo>> possibleValues = new LinkedList<>();

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
      final Map<PerformanceTime, TeamScheduleInfo> assignments = new HashMap<>();
      for (int tableColorIndex = 0; tableColorIndex < tables.size(); ++tableColorIndex) {
        final String tableColor = tables.get(tableColorIndex);

        // check in pairs to make sure we have a valid table assignment
        final int side1 = 1;
        final int orderIndex1 = tableColorIndex
            * 2;
        final int teamIndex1 = ordering.get(orderIndex1);
        final TeamScheduleInfo team1 = getTeam(teams, teamIndex1);

        final int side2 = 2;
        final int orderIndex2 = orderIndex1
            + 1;
        final int teamIndex2 = ordering.get(orderIndex2);
        final TeamScheduleInfo team2 = getTeam(teams, teamIndex2);

        // if the values are not null they are guaranteed to be keys for originalValues
        if (null != team1
            && null != team2) {
          assignments.put(new PerformanceTime(time, tableColor, side1,
                                              castNonNull(originalValues.get(team1)).isPractice()),
                          team1);
          assignments.put(new PerformanceTime(time, tableColor, side2,
                                              castNonNull(originalValues.get(team2)).isPractice()),
                          team2);
        } else if (null != team1
            && null == team2
            && validToHaveNullTeam) {
          assignments.put(new PerformanceTime(time, tableColor, side1,
                                              castNonNull(originalValues.get(team1)).isPractice()),
                          team1);

          // can only have 1 uneven pairing at a time
          validToHaveNullTeam = false;
        } else if (null == team1
            && null != team2
            && validToHaveNullTeam) {
          assignments.put(new PerformanceTime(time, tableColor, side2,
                                              castNonNull(originalValues.get(team2)).isPractice()),
                          team2);

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
   * Get team from teams using teamIndex.
   *
   * @param teams list of teams
   * @param teamIndex index into teams
   * @return the team number or null if the index is
   *         larger than the list of teams
   */
  private static @Nullable TeamScheduleInfo getTeam(final List<TeamScheduleInfo> teams,
                                                    final int teamIndex) {
    if (teamIndex < teams.size()) {
      return teams.get(teamIndex);
    } else {
      return null;
    }
  }

  private void applyPerformanceOrdering(final Map<PerformanceTime, TeamScheduleInfo> possibleValue) {
    for (final Map.Entry<PerformanceTime, TeamScheduleInfo> entry : possibleValue.entrySet()) {
      final TeamScheduleInfo team = entry.getValue();
      final PerformanceTime perfTime = entry.getKey();

      // can use perfTime.getTime() as oldTime since we know that we're just
      // moving teams across tables
      schedule.reassignTable(team, perfTime.getTime(), perfTime);
    }

  }

  /**
   * Compute permutations of the integers [0, numElements].
   *
   * @param numElements how many elements to be permuted
   * @return all possible orderings
   */
  public static List<List<Integer>> permutate(final int numElements) {
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
  private static void permutate(final int arrayCount,
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
        final List<Integer> newOrder = new ArrayList<>(order);
        newOrder.set(position, element);

        final List<Integer> newElements = new ArrayList<>(elements);
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
    final Map<Integer, List<ConstraintViolation>> teamViolations = new HashMap<>();
    for (final ConstraintViolation violation : violations) {
      if (isPerformanceViolation(violation)) {
        final List<ConstraintViolation> vs;
        final Integer violationTeam = violation.getTeam();
        if (teamViolations.containsKey(violationTeam)) {
          vs = teamViolations.get(violationTeam);
        } else {
          vs = new LinkedList<>();
          teamViolations.put(violationTeam, vs);
        }
        vs.add(violation);
      }
    }

    // find max
    List<ConstraintViolation> retval = new LinkedList<>();
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
    final Option option = new Option(SCHED_FILE_OPTION, "schedfile", true, "<file> the schedule file ");
    option.setRequired(true);
    options.addOption(option);

    return options;
  }

  private static void usage(final Options options) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("TableOptimizer", options);
  }

  /**
   * @param args ignored
   */
  public static void main(final String[] args) {
    File schedfile = null;
    final Options options = buildOptions();
    try {
      final CommandLineParser parser = new DefaultParser();
      final CommandLine cmd = parser.parse(options, args);

      schedfile = new File(cmd.getOptionValue(SCHED_FILE_OPTION));
    } catch (final org.apache.commons.cli.ParseException pe) {
      LOGGER.error(pe.getMessage());
      usage(options);
      System.exit(1);
    }

    try {
      if (!schedfile.canRead()) {
        LOGGER.fatal(schedfile.getAbsolutePath()
            + " is not readable");
        System.exit(4);
      }

      final String sheetName;
      if (!ExcelCellReader.isExcelFile(schedfile)) {
        sheetName = "ignored";
      } else {
        sheetName = SchedulerUI.promptForSheetName(schedfile);
        if (null == sheetName) {
          return;
        }
      }

      final ColumnInformation columnInfo = TournamentSchedule.findColumns(CellFileReader.createCellReader(schedfile,
                                                                                                          sheetName),
                                                                          new LinkedList<String>());

      final List<SubjectiveStation> subjectiveStations = SchedulerUI.gatherSubjectiveStationInformation(null,
                                                                                                        columnInfo);

      // not bothering to get the schedule params as we're just tweaking table
      // assignments, which wont't be effected by the schedule params.
      final SchedParams params = new SchedParams(subjectiveStations, SchedParams.DEFAULT_PERFORMANCE_MINUTES,
                                                 SchedParams.MINIMUM_CHANGETIME_MINUTES,
                                                 SchedParams.MINIMUM_PERFORMANCE_CHANGETIME_MINUTES);
      final List<String> subjectiveHeaders = new LinkedList<>();
      for (final SubjectiveStation station : subjectiveStations) {
        subjectiveHeaders.add(station.getName());
      }

      final String name = Utilities.extractBasename(schedfile);

      final TournamentSchedule schedule = new TournamentSchedule(name,
                                                                 CellFileReader.createCellReader(schedfile, sheetName),
                                                                 subjectiveHeaders);

      final File schedfileDirectory = schedfile.getAbsoluteFile().getParentFile();
      if (null == schedfileDirectory) {
        throw new FLLRuntimeException("No directory for '"
            + schedfile.getAbsolutePath()
            + "'");
      }
      final TableOptimizer optimizer = new TableOptimizer(params, schedule, schedfileDirectory);
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
    } catch (final InvalidFormatException e) {
      LOGGER.fatal(e, e);
      System.exit(7);
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

    for (final TeamScheduleInfo si : schedule.getSchedule()) {
      for (final PerformanceTime perf : si.getAllPerformances()) {

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
        finalGroups.add(new ArrayList<>(group));
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
  public void optimize(final @Nullable CheckCanceled checkCanceled) {
    final Set<Integer> optimizedTeams = new HashSet<>();
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
      for (final PerformanceTime pt : ti.getAllPerformances()) {
        final LocalTime time = pt.getTime();

        List<String> tables = perfTables.get(time);
        for (int i = 0; null == tables
            && i < tableGroups.size(); ++i) {
          final List<String> group = this.tableGroups.get(i);
          if (group.contains(pt.getTable())) {
            tables = group;
          }
        }
        if (null == tables) {
          throw new FLLInternalException("No tables found");
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
      if (null == tables) {
        throw new FLLInternalException("Cannot find tables at time: "
            + TournamentSchedule.formatTime(time));
      }
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
      final Map<TeamScheduleInfo, PerformanceTime> originalPerformances = new HashMap<>();

      List<String> tables = null;
      for (final TeamScheduleInfo si : schedule.getSchedule()) {
        final List<PerformanceTime> pts = si.allPerformances().filter(pt -> pt.getTime().equals(time))
                                            .collect(Collectors.toList());
        if (pts.size() > 1) {
          throw new FLLRuntimeException("Found multiple performances for "
              + si.getTeamNumber()
              + " at "
              + time);
        } else if (!pts.isEmpty()) {
          final PerformanceTime pt = pts.get(0);
          originalPerformances.put(si, pt);

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

      } // foreach schedule item

      if (null == tables) {
        throw new FLLRuntimeException("Cannot find any tables");
      } else {
        computeBestTableOrdering(originalPerformances, time, tables, checkCanceled);
      }

    } // foreach time
  }

}
