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
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

import au.com.bytecode.opencsv.CSVWriter;
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

  private final ScheduleChecker checker;

  /**
   * Teams that we've already optimized so we shouldn't try again.
   */
  private final Set<Integer> optimizedTeams = new HashSet<Integer>();

  private static boolean isPerformanceViolation(final ConstraintViolation violation) {
    return null != violation.getPerformance();
  }

  private void computeBestTableOrdering(final List<Integer> teams,
                                        final List<PerformanceTime> times) {
    if (teams.size() != times.size()) {
      throw new IllegalArgumentException("teams and times must be the same length");
    }
    if (teams.isEmpty()) {
      throw new IllegalArgumentException("Must have some teams to check");
    }

    List<Integer> bestPermutation = null;
    int minWarnings = checker.verifySchedule().size();
    final List<List<Integer>> permutations = computePossibleOrderings(teams.size());
    for (final List<Integer> possibleValue : permutations) {
      applyPerformanceOrdering(teams, times, possibleValue);

      // check for better value
      final List<ConstraintViolation> newWarnings = checker.verifySchedule();
      if (null == bestPermutation
          || newWarnings.size() < minWarnings) {
        if (newWarnings.size() < minWarnings) {
          try {
            final File outputFile = new File(basedir, String.format("%s-opt-%d.csv", schedule.getName(), numSolutions));
            LOGGER.info(String.format("Found better schedule (%d -> %d), writing to: %s", minWarnings,
                                      newWarnings.size(), outputFile.getAbsolutePath()));
            writeSchedule(outputFile);
            ++numSolutions;
          } catch (final IOException e) {
            throw new RuntimeException(e);
          }
        }
        bestPermutation = possibleValue;
        minWarnings = newWarnings.size();
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

  private List<List<Integer>> computePossibleOrderings(final int numElements) {
    final List<Integer> allElements = new ArrayList<Integer>();
    for (int i = 0; i < numElements; ++i) {
      allElements.add(i);
    }
    final List<List<Integer>> possibleValues = new LinkedList<List<Integer>>();
    final List<Integer> order = new ArrayList<Integer>(numElements);
    for (int i = 0; i < numElements; ++i) {
      order.add(i);
    }
    permutate(numElements, allElements, order, possibleValues);
    return possibleValues;
  }

  private void permutate(final int arrayCount,
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

  private List<ConstraintViolation> pickTeamWithMostViolations() {
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
                                                 SchedParams.DEFAULT_CHANGETIME_MINUTES,
                                                 SchedParams.DEFAULT_PERFORMANCE_CHANGETIME_MINUTES);
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

  /**
   * @param outputFile
   * @throws IOException
   */
  private void writeSchedule(final File outputFile) throws IOException {
    CSVWriter csv = null;
    try {
      csv = new CSVWriter(new OutputStreamWriter(new FileOutputStream(outputFile), Utilities.DEFAULT_CHARSET));

      final List<String> line = new ArrayList<String>();
      line.add(TournamentSchedule.TEAM_NUMBER_HEADER);
      line.add(TournamentSchedule.DIVISION_HEADER);
      line.add(TournamentSchedule.TEAM_NAME_HEADER);
      line.add(TournamentSchedule.ORGANIZATION_HEADER);
      line.add(TournamentSchedule.JUDGE_GROUP_HEADER);
      final List<String> categories = Collections.unmodifiableList(new LinkedList<String>(
                                                                                          schedule.getSubjectiveStations()));
      for (final String category : categories) {
        line.add(category);
      }
      for (int round = 0; round < schedule.getNumberOfRounds(); ++round) {
        line.add(String.format(TournamentSchedule.PERF_HEADER_FORMAT, round + 1));
        line.add(String.format(TournamentSchedule.TABLE_HEADER_FORMAT, round + 1));
      }
      csv.writeNext(line.toArray(new String[line.size()]));
      line.clear();

      for (final TeamScheduleInfo si : schedule.getSchedule()) {
        line.add(String.valueOf(si.getTeamNumber()));
        line.add(si.getDivision());
        line.add(si.getTeamName());
        line.add(si.getOrganization());
        line.add(si.getJudgingStation());
        for (final String category : categories) {
          final Date d = si.getSubjectiveTimeByName(category).getTime();
          line.add(TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(d));
        }
        for (int round = 0; round < schedule.getNumberOfRounds(); ++round) {
          final PerformanceTime p = si.getPerf(round);
          line.add(TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(p.getTime()));
          line.add(p.getTable()
              + " " + p.getSide());
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

  public void optimize() {
    List<ConstraintViolation> teamViolations = pickTeamWithMostViolations();
    while (!teamViolations.isEmpty()) {
      final int team = teamViolations.get(0).getTeam();
      optimizedTeams.add(team);

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Optimize tables for team: "
            + team);
      }

      final Set<Date> perfTimes = gatherPerformanceTimes(teamViolations);
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
        }
        computeBestTableOrdering(teams, times);
      }

      teamViolations = pickTeamWithMostViolations();
    }
  }

}
