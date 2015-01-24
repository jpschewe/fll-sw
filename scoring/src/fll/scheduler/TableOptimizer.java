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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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

import fll.Utilities;
import fll.scheduler.TournamentSchedule.ColumnInformation;
import fll.util.CSVCellReader;
import fll.util.CellFileReader;
import fll.util.ExcelCellReader;
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

  /**
   * The best schedule found so far. Starts out at null and
   * is modified by {@link #optimize()}.
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
        * 1000 + tableUseScore;
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
   * Compute the best table ordering for a set of teams at the
   * specified time.
   */
  private void computeBestTableOrdering(final List<Integer> teams,
                                        final List<PerformanceTime> times) {
    if (teams.size() != times.size()) {
      throw new IllegalArgumentException("teams and times must be the same length");
    }
    if (teams.isEmpty()) {
      throw new IllegalArgumentException("Must have some teams to check");
    }

    List<Integer> bestPermutation = null;
    int bestScore = computeScheduleScore();
    if (bestScore == 0) {
      // already best score
      return;
    }

    final List<List<Integer>> permutations = permutate(teams.size());
    for (final List<Integer> possibleValue : permutations) {
      applyPerformanceOrdering(teams, times, possibleValue);

      // check for better value
      final int score = computeScheduleScore();
      if (null == bestPermutation
          || score < bestScore) {
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
        }
        bestPermutation = possibleValue;
        bestScore = score;

        if (bestScore == 0) {
          break;
        }
      }
    }

    if (null == bestPermutation) {
      throw new RuntimeException("Internal error, bestPermutation should not be null here");
    }

    // assign the best value
    applyPerformanceOrdering(teams, times, bestPermutation);
  }

  private void applyPerformanceOrdering(final List<Integer> teams,
                                        final List<PerformanceTime> times,
                                        final List<Integer> possibleValue) {
    for (int i = 0; i < teams.size(); ++i) {
      final int team = teams.get(i);
      final int timeIndex = possibleValue.get(i);
      final PerformanceTime perfTime = times.get(timeIndex);
      schedule.reassignTable(team, perfTime.getTime(), perfTime);
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
  private Set<Date> gatherPerformanceTimes(final Collection<ConstraintViolation> violations) {
    final Set<Date> perfTimes = new HashSet<Date>();
    for (final ConstraintViolation violation : violations) {
      final Date d = violation.getPerformance();
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

      final TableOptimizer optimizer = new TableOptimizer(params, schedule, schedfile.getAbsoluteFile().getParentFile());
      final long start = System.currentTimeMillis();
      optimizer.optimize();
      final long stop = System.currentTimeMillis();
      LOGGER.info("Optimization took: "
          + (stop - start) / 1000.0 + " seconds");

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
   * @param params the schedule parameters
   * @param schedule the schedule to optimize (will be modified)
   * @param basedir the directory to store better schedules in
   * @throws IllegalArgumentException if the schedule has hard constraint
   *           violations
   */
  public TableOptimizer(final SchedParams params,
                        final TournamentSchedule schedule,
                        final File basedir) throws IllegalArgumentException {
    this.schedule = schedule;
    this.basedir = basedir;
    this.checker = new ScheduleChecker(params, schedule);

    final List<ConstraintViolation> violations = checker.verifySchedule();
    for (final ConstraintViolation v : violations) {
      if (v.isHard()) {
        throw new IllegalArgumentException("Should not have any hard constraint violations: "
            + v.getMessage());
      }
    }

    if (!this.basedir.isDirectory()) {
      throw new IllegalArgumentException("Basedir must be a directory");
    }
  }

  public void optimize() {
    final Set<Integer> optimizedTeams = new HashSet<Integer>();

    List<ConstraintViolation> teamViolations = pickTeamWithMostViolations(optimizedTeams);
    while (!teamViolations.isEmpty()) {
      final int team = teamViolations.get(0).getTeam();
      optimizedTeams.add(team);

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Optimize tables for team: "
            + team);
      }

      final Set<Date> perfTimes = gatherPerformanceTimes(teamViolations);
      optimize(perfTimes);

      teamViolations = pickTeamWithMostViolations(optimizedTeams);
    } // while team violations

    final Set<Date> perfTimes = findNonFullTableTimes();
    optimize(perfTimes);

  }

  /**
   * Find all times in the schedule where the number of teams
   * competing doesn't equal the number of tables available.
   */
  private Set<Date> findNonFullTableTimes() {
    final Map<Date, Integer> perfCounts = new HashMap<>();
    for (final TeamScheduleInfo ti : this.schedule.getSchedule()) {
      for (int round = 0; round < ti.getNumberOfRounds(); ++round) {
        final Date time = ti.getPerfTime(round);
        int count = 0;
        if (perfCounts.containsKey(time)) {
          count = perfCounts.get(time);
        }
        ++count;
        perfCounts.put(time, count);
      }
    }

    final Set<Date> perfTimes = new HashSet<>();

    // 2 teams on each table at a given time
    final int expectedTableUse = this.schedule.getTableColors().size() * 2;
    for (final Map.Entry<Date, Integer> entry : perfCounts.entrySet()) {
      if (entry.getValue() < expectedTableUse) {
        perfTimes.add(entry.getKey());
      }
    }

    return perfTimes;
  }

  /**
   * Optimize the table use at the specified times.
   * 
   * @param perfTimes
   */
  private void optimize(final Set<Date> perfTimes) {
    for (final Date time : perfTimes) {
      final List<Integer> teams = new ArrayList<Integer>();
      final List<PerformanceTime> times = new ArrayList<PerformanceTime>();

      for (final TeamScheduleInfo si : schedule.getSchedule()) {
        for (int round = 0; round < schedule.getNumberOfRounds(); ++round) {
          final PerformanceTime pt = si.getPerf(round);
          if (time.equals(pt.getTime())) {
            teams.add(si.getTeamNumber());
            times.add(pt);
          }
        }
      } // foreach schedule item
      computeBestTableOrdering(teams, times);
    } // foreach time
  }

}
