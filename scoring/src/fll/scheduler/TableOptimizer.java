/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

import fll.Utilities;
import fll.scheduler.TournamentSchedule.ColumnInformation;
import fll.util.CSVCellReader;
import fll.util.CellFileReader;
import fll.util.ExcelCellReader;
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

  private int minWarnings = Integer.MAX_VALUE;

  private final SchedParams params;

  private int numSolutions = 0;

  public TableOptimizer(final File selectedFile) throws IOException {
    // FIXME prompt user for sheet name if spreadsheet

    // FIXME prompt use for subjective columns

    FileInputStream fis = null;
    try {

      final boolean csv = selectedFile.getName().endsWith("csv");
      final CellFileReader reader;
      final String sheetName;
      if (csv) {
        reader = new CSVCellReader(selectedFile);
        sheetName = null;
      } else {
        sheetName = promptForSheetName(selectedFile);
        if (null == sheetName) {
          return;
        }
        fis = new FileInputStream(selectedFile);
        reader = new ExcelCellReader(fis, sheetName);
      }
      final ColumnInformation columnInfo = TournamentSchedule.findColumns(reader, new LinkedList<String>());
      if (null != fis) {
        fis.close();
        fis = null;
      }

      final List<SubjectiveStation> subjectiveStations = gatherSubjectiveStationInformation(columnInfo);
      schedParams = new SchedParams(subjectiveStations, SchedParams.DEFAULT_PERFORMANCE_MINUTES,
                                    SchedParams.DEFAULT_CHANGETIME_MINUTES,
                                    SchedParams.DEFAULT_PERFORMANCE_CHANGETIME_MINUTES);
      final List<String> subjectiveHeaders = new LinkedList<String>();
      for (final SubjectiveStation station : subjectiveStations) {
        subjectiveHeaders.add(station.getName());
      }

      final String name = Utilities.extractBasename(selectedFile);

      final TournamentSchedule schedule;
      if (csv) {
        schedule = new TournamentSchedule(name, selectedFile, subjectiveHeaders);
      } else {
        fis = new FileInputStream(selectedFile);
        schedule = new TournamentSchedule(name, fis, sheetName, subjectiveHeaders);
      }
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
    } catch (final ParseException e) {
      LOGGER.fatal(e.getMessage());
      System.exit(2);
    }

    try {
      if (!schedfile.canRead()) {
        LOGGER.fatal(schedfile.getAbsolutePath()
            + " is not readable");
        System.exit(4);
      }

      final TableOptimizer optimizer = new TableOptimizer(schedfile);
      final long start = System.currentTimeMillis();
      optimizer.optimize();
      final long stop = System.currentTimeMillis();
      LOGGER.info("Optimization took: "
          + (stop - start) / 1000.0 + " seconds");

    } catch (final IOException e) {
      LOGGER.fatal("Error reading file", e);
      System.exit(4);
    } catch (final RuntimeException e) {
      LOGGER.fatal(e, e);
      throw e;
    }

  }

  public TableOptimizer(final SchedParams params,
                        final TournamentSchedule schedule,
                        final File basedir) {
    this.schedule = schedule;
    this.basedir = basedir;
    this.params = params;
    if (!this.basedir.isDirectory()) {
      throw new IllegalArgumentException("Basedir must be a directory");
    }
  }

  /**
   * Check for a better schedule.
   */
  private void checkBetterSchedule(final TournamentSchedule newSchedule) {
    final ScheduleChecker checker = new ScheduleChecker(params, newSchedule);
    final List<ConstraintViolation> violations = checker.verifySchedule();
    for (final ConstraintViolation v : violations) {
      if (v.isHard()) {
        throw new FLLRuntimeException("Should not have any hard constraint violations: "
            + v.getMessage());
      }
    }
    if (violations.size() < minWarnings) {
      minWarnings = violations.size();
      if (numSolutions > 0) {
        final File outputFile = new File(basedir, String.format("%s-opt-%d.csv", schedule.getName(), numSolutions));
        writeSchedule(outputFile, newSchedule);

      }
      ++numSolutions;
    }
  }

  /**
   * @param outputFile
   * @throws IOException
   */
  private void writeSchedule(final File outputFile,
                             final TournamentSchedule newSchedule) throws IOException {
    BufferedWriter writer = null;
    try {
      writer = new BufferedWriter(new FileWriter(outputFile));
      writer.write(TournamentSchedule.TEAM_NUMBER_HEADER
          + ",");
      writer.write(TournamentSchedule.DIVISION_HEADER
          + ",");
      writer.write(TournamentSchedule.TEAM_NAME_HEADER
          + ",");
      writer.write(TournamentSchedule.ORGANIZATION_HEADER
          + ",");
      writer.write(TournamentSchedule.JUDGE_GROUP_HEADER);
      final List<String> categories = Collections.unmodifiableList(new LinkedList<String>(
                                                                                          newSchedule.getSubjectiveStations()));
      for (final String category : categories) {
        writer.write(","
            + category);
      }
      for (int round = 0; round < newSchedule.getNumberOfRounds(); ++round) {
        writer.write(","
            + String.format(TournamentSchedule.PERF_HEADER_FORMAT, round + 1));
        writer.write(","
            + String.format(TournamentSchedule.TABLE_HEADER_FORMAT, round + 1));
      }
      writer.newLine();

      for (final TeamScheduleInfo si : newSchedule.getSchedule()) {
        writer.write(si.getTeamNumber()
            + ",");
        writer.write(si.getDivision()
            + ",");
        writer.write(si.getTeamName()
            + ",");
        writer.write(si.getOrganization()
            + ",");
        writer.write(si.getJudgingStation()
            + ",");
        for (final String category : categories) {
          final Date d = si.getSubjectiveTimeByName(category).getTime();
          writer.write(",");
          writer.write(TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(d));
        }
        for (int round = 0; round < newSchedule.getNumberOfRounds(); ++round) {
          final PerformanceTime p = si.getPerf(round);
          writer.write(",");
          writer.write(TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(p.getTime()));
          writer.write(p.getTable()
              + " " + p.getSide());
        }
        writer.newLine();
      }

    } finally {
      if (null != writer) {
        writer.close();
      }
    }

  }

  public void optimize() {
    checkBetterSchedule(schedule);
    // FIXME setup search variables
  }

}
