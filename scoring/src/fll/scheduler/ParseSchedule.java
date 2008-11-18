/*
 * Copyright (c) 2000-2008 HighTechKids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.scheduler;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import au.com.bytecode.opencsv.CSVReader;
import fll.Utilities;

/**
 * 
 * Parse a CSV file representing the detailed schedule for a tournament.
 * 
 * @author jpschewe
 * @version $Revision$
 * 
 */
public class ParseSchedule {

  private static final Logger LOG = Logger.getLogger(ParseSchedule.class);

  /**
   * Header on team number column.
   */
  public static final String TEAM_NUMBER_HEADER = "Team #";

  public static final String DIVISION_HEADER = "Div";

  public static final String PRESENTATION_HEADER = "Presentation";

  public static final String TECHNICAL_HEADER = "Technical";

  public static final String JUDGE_GROUP_HEADER = "Judging Station";

  public static final String PERF_1_HEADER = "Perf #1";

  public static final String PERF_1_TABLE_HEADER = "Perf 1 Table";

  public static final String PERF_2_HEADER = "Perf #2";

  public static final String PERF_2_TABLE_HEADER = "Perf 2 Table";

  public static final String PERF_3_HEADER = "Perf #3";

  public static final String PERF_3_TABLE_HEADER = "Perf 3 Table";

  private int _teamNumColumn = -1;

  private int _divisionColumn = -1;

  private int _presentationColumn = -1;

  private int _technicalColumn = -1;

  private int _judgeGroupColumn = -1;

  private int _perf1Column = -1;

  private int _perf1TableColumn = -1;

  private int _perf2Column = -1;

  private int _perf2TableColumn = -1;

  private int _perf3Column = -1;

  private int _perf3TableColumn = -1;

  private int _numberOfRounds = 3;

  private static final DateFormat DATE_FORMAT_AM_PM = new SimpleDateFormat("hh:mm a");

  private static final DateFormat DATE_FORMAT_AM_PM_SS = new SimpleDateFormat("hh:mm:ss a");

  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm");

  private static final DateFormat DATE_FORMAT_SS = new SimpleDateFormat("HH:mm:ss");

  /**
   * @param args
   */
  public static void main(final String[] args) {
    if(args.length < 1) {
      LOG.fatal("Must specify the file to read");
      System.exit(1);
    }

    final File file = new File(args[0]);
    if(file.isDirectory()) {
      final File[] files = file.listFiles(new FileFilter() {
        public boolean accept(final File pathname) {
          return pathname.getName().endsWith(".csv");
        }
      });
      for(final File f : files) {
        final ParseSchedule ps = new ParseSchedule();

        try {
          ps.parseFile(f);
        } catch(final IOException ioe) {
          LOG.fatal(ioe, ioe);
          System.exit(1);
        }
      }
    } else if(file.isFile()) {
      final ParseSchedule ps = new ParseSchedule();

      try {
        ps.parseFile(file);
      } catch(final IOException ioe) {
        LOG.fatal(ioe, ioe);
        System.exit(1);
      }
    }

    LOG.info("Finished, if no errors found, you're good");
  }

  public void parseFile(final File file) throws IOException {
    LOG.info(new Formatter().format("Reading file %s", file.getAbsoluteFile()));

    if(!file.canRead() || !file.isFile()) {
      LOG.fatal("File is not readable or not a file: " + file.getAbsolutePath());
      return;
    }

    final CSVReader csvreader = new CSVReader(new FileReader(file));

    String[] line;
    while(null != (line = csvreader.readNext()) && _teamNumColumn == -1) {
      _teamNumColumn = -1;
      _divisionColumn = -1;
      _presentationColumn = -1;
      _technicalColumn = -1;
      _judgeGroupColumn = -1;
      _perf1Column = -1;
      _perf1TableColumn = -1;
      _perf2Column = -1;
      _perf2TableColumn = -1;
      _perf3Column = -1;
      _perf3TableColumn = -1;

      for(int i = 0; i < line.length; ++i) {
        if(line[i].equals(TEAM_NUMBER_HEADER)) {
          _teamNumColumn = i;
        } else if(line[i].equals(DIVISION_HEADER)) {
          _divisionColumn = i;
        } else if(line[i].equals(PRESENTATION_HEADER)) {
          _presentationColumn = i;
        } else if(line[i].equals(TECHNICAL_HEADER)) {
          _technicalColumn = i;
        } else if(line[i].equals(JUDGE_GROUP_HEADER)) {
          _judgeGroupColumn = i;
        } else if(line[i].equals(PERF_1_HEADER)) {
          _perf1Column = i;
        } else if(line[i].equals(PERF_1_TABLE_HEADER)) {
          _perf1TableColumn = i;
        } else if(line[i].equals(PERF_2_HEADER)) {
          _perf2Column = i;
        } else if(line[i].equals(PERF_2_TABLE_HEADER)) {
          _perf2TableColumn = i;
        } else if(line[i].equals(PERF_3_HEADER)) {
          _perf3Column = i;
        } else if(line[i].equals(PERF_3_TABLE_HEADER)) {
          _perf3TableColumn = i;
        }
      }
    }

    if(-1 == _teamNumColumn) {
      LOG.fatal("Could not find teamNumColumn");
      System.exit(1);
    }

    if(-1 == _divisionColumn) {
      LOG.fatal("Could not find divisionColumn");
      System.exit(1);
    }
    if(-1 == _presentationColumn) {
      LOG.fatal("Could not find presentationColumn");
      System.exit(1);
    }
    if(-1 == _technicalColumn) {
      LOG.fatal("Could not find technicalColumn");
      System.exit(1);
    }
    if(-1 == _judgeGroupColumn) {
      LOG.fatal("Could not find judgeGroupColumn");
      System.exit(1);
    }
    if(-1 == _perf1Column) {
      LOG.fatal("Could not find perf1Column");
      System.exit(1);
    }
    if(-1 == _perf1TableColumn) {
      LOG.fatal("Could not find perf1TableColumn");
      System.exit(1);
    }
    if(-1 == _perf2Column) {
      LOG.fatal("Could not find perf2Column");
      System.exit(1);
    }
    if(-1 == _perf2TableColumn) {
      LOG.fatal("Could not find perf2TableColumn");
      System.exit(1);
    }
    if(-1 == _perf3Column) {
      LOG.fatal("Could not find perf3Column");
      System.exit(1);
    }
    if(-1 == _perf3TableColumn) {
      LOG.fatal("Could not find perf3TableColumn");
      System.exit(1);
    }

    final Set<String> tableNames = new HashSet<String>();
    final List<TeamScheduleInfo> schedule = new LinkedList<TeamScheduleInfo>();
    TeamScheduleInfo ti;
    while(null != (ti = parseLine(csvreader))) {
      schedule.add(ti);

      // keep track of table names
      for(int i = 0; i < ti.perfTable.length; ++i) {
        tableNames.add(ti.perfTable[i]);
      }

      verifyTeam(ti);
    }

    final int numberOfTables = tableNames.size();
    for(int round = 0; round < _numberOfRounds; ++round) {
      verifyPerformanceAtTime(numberOfTables, round, schedule);
    }
    verifyPresentationAtTime(schedule);
    verifyTechnicalAtTime(schedule);
    
  }

  /**
   * Verify that there are no more than <code>numberOfTables</code> teams
   * performing at the same time in round <code>round</code>.
   * 
   * @param numberOfTables
   * @param round
   * @return
   */
  private static boolean verifyPerformanceAtTime(final int numberOfTables, final int round, final List<TeamScheduleInfo> schedule) {
    // constraint set 6
    final Map<Date, Set<Integer>> teamsAtTime = new HashMap<Date, Set<Integer>>();
    for(final TeamScheduleInfo si : schedule) {
      Set<Integer> teams;
      if(teamsAtTime.containsKey(si.perf[round])) {
        teams = teamsAtTime.get(si.perf[round]);
      } else {
        teams = new HashSet<Integer>();
      }
      teams.add(si.teamNumber);
    }

    boolean retval = true;
    for(final Map.Entry<Date, Set<Integer>> entry : teamsAtTime.entrySet()) {
      if(entry.getValue().size() > numberOfTables) {
        LOG.error(new Formatter().format("There are too many teams at %s in round %d", entry.getKey(), round));

        retval = false;
      }
    }

    return retval;
  }
  
  /**
   * Ensure that no more than 1 team is in presentation judging at once.
   * 
   * @param schedule
   * @return
   */
  private static boolean verifyPresentationAtTime(final List<TeamScheduleInfo> schedule) {
    // constraint set 7
    final Map<Date, Set<Integer>> teamsAtTime = new HashMap<Date, Set<Integer>>();
    for(final TeamScheduleInfo si : schedule) {
      Set<Integer> teams;
      if(teamsAtTime.containsKey(si.presentation)) {
        teams = teamsAtTime.get(si.presentation);
      } else {
        teams = new HashSet<Integer>();
      }
      teams.add(si.teamNumber);
    }

    boolean retval = true;
    for(final Map.Entry<Date, Set<Integer>> entry : teamsAtTime.entrySet()) {
      if(entry.getValue().size() > 1) {
        LOG.error(new Formatter().format("There are too many teams at %s in presentation", entry.getKey()));

        retval = false;
      }
    }

    return retval;
  }

  /**
   * Ensure that no more than 1 team is in technical judging at once.
   * 
   * @param schedule
   * @return
   */
  private static boolean verifyTechnicalAtTime(final List<TeamScheduleInfo> schedule) {
    // constraint set 7
    final Map<Date, Set<Integer>> teamsAtTime = new HashMap<Date, Set<Integer>>();
    for(final TeamScheduleInfo si : schedule) {
      Set<Integer> teams;
      if(teamsAtTime.containsKey(si.technical)) {
        teams = teamsAtTime.get(si.technical);
      } else {
        teams = new HashSet<Integer>();
      }
      teams.add(si.teamNumber);
    }

    boolean retval = true;
    for(final Map.Entry<Date, Set<Integer>> entry : teamsAtTime.entrySet()) {
      if(entry.getValue().size() > 1) {
        LOG.error(new Formatter().format("There are too many teams at %s in technical", entry.getKey()));

        retval = false;
      }
    }

    return retval;
  }
  
  public static final long SECONDS_PER_MINUTE = 60;

  public static final long PERFORMANCE_DURATION = 5 * SECONDS_PER_MINUTE * 1000;

  public static final long SUBJECTIVE_DURATION = 20 * SECONDS_PER_MINUTE * 1000;

  public static final long CHANGETIME = 15 * SECONDS_PER_MINUTE * 1000;

  public static final long PERFORMANCE_CHANGETIME = 35 * SECONDS_PER_MINUTE * 1000;

  private boolean verifyTeam(final TeamScheduleInfo ti) {
    // constraint set 1
    if(ti.presentation.before(ti.technical)) {
      if(ti.presentation.getTime() + SUBJECTIVE_DURATION + CHANGETIME > ti.technical.getTime()) {
        LOG.error(new Formatter().format("Team %d has doesn't have enough time between presentation and technical", ti.teamNumber));
        return false;
      }
    } else {
      if(ti.technical.getTime() + SUBJECTIVE_DURATION + CHANGETIME > ti.presentation.getTime()) {
        LOG.error(new Formatter().format("Team %d has doesn't have enough time between presentation and technical", ti.teamNumber));
        return false;
      }
    }

    // constraint set 3
    if(ti.perf[0].getTime() + PERFORMANCE_DURATION + PERFORMANCE_CHANGETIME > ti.perf[1].getTime()) {
      LOG.error(new Formatter().format("Team %d doesn't have enough time between performance 1 and performance 2: %s - %s", ti.teamNumber,
          DATE_FORMAT.format(ti.perf[0]), DATE_FORMAT.format(ti.perf[1])));
    }
    if(ti.perf[1].getTime() + PERFORMANCE_DURATION + PERFORMANCE_CHANGETIME > ti.perf[2].getTime()) {
      LOG.error(new Formatter().format("Team %d doesn't have enough time between performance 2 and performance 3: %s - %s", ti.teamNumber,
          DATE_FORMAT.format(ti.perf[1]), DATE_FORMAT.format(ti.perf[2])));
    }

    // constraint set 4
    for(int round = 0; round < _numberOfRounds; ++round) {
      if(ti.presentation.before(ti.perf[round])) {
        if(ti.presentation.getTime() + SUBJECTIVE_DURATION + CHANGETIME > ti.perf[round].getTime()) {
          LOG.error(new Formatter().format("Team %d has doesn't have enough time between presentation and performance round %d", ti.teamNumber,
              round + 1));
          return false;
        }
      } else {
        if(ti.perf[round].getTime() + PERFORMANCE_DURATION + CHANGETIME > ti.presentation.getTime()) {
          LOG.error(new Formatter().format("Team %d has doesn't have enough time between presentation and performance round %d", ti.teamNumber,
              round + 1));
          return false;
        }
      }
    }

    // constraint set 5
    for(int round = 0; round < _numberOfRounds; ++round) {
      if(ti.technical.before(ti.perf[round])) {
        if(ti.technical.getTime() + SUBJECTIVE_DURATION + CHANGETIME > ti.perf[round].getTime()) {
          LOG.error(new Formatter().format("Team %d has doesn't have enough time between technical and performance round %d", ti.teamNumber,
              round + 1));
          return false;
        }
      } else {
        if(ti.perf[round].getTime() + PERFORMANCE_DURATION + CHANGETIME > ti.technical.getTime()) {
          LOG.error(new Formatter().format("Team %d has doesn't have enough time between technical and performance round %d", ti.teamNumber,
              round + 1));
          return false;
        }
      }
    }

    return true;
  }

  /**
   * @return the schedule info or null if there was an error, or the last line
   *         is hit
   */
  private TeamScheduleInfo parseLine(final CSVReader csvReader) throws IOException {
    final String[] line = csvReader.readNext();
    try {

      final String teamNumberStr = line[_teamNumColumn];
      if(teamNumberStr.length() < 1) {
        // hit empty row
        return null;
      }
      final TeamScheduleInfo ti = new TeamScheduleInfo();
      ti.teamNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(teamNumberStr).intValue();
      ti.division = line[_divisionColumn];
      ti.presentation = parseDate(line[_presentationColumn]);
      ti.technical = parseDate(line[_technicalColumn]);
      ti.judge = line[_judgeGroupColumn];
      ti.perf[0] = parseDate(line[_perf1Column]);
      ti.perfTable[0] = line[_perf1TableColumn];
      ti.perf[1] = parseDate(line[_perf2Column]);
      ti.perfTable[1] = line[_perf2TableColumn];
      ti.perf[2] = parseDate(line[_perf3Column]);
      ti.perfTable[2] = line[_perf3TableColumn];

      return ti;
    } catch(final ParseException pe) {
      LOG.error("Error parsing line: " + Arrays.toString(line), pe);
      return null;
    }
  }

  /**
   * Check for AM/PM flag and then pick the right parser.
   * 
   * @throws ParseException
   *           if the date cannot be parsed
   */
  private static Date parseDate(final String s) throws ParseException {
    if(s.indexOf("AM") > 0 || s.indexOf("PM") > 0) {
      if(s.split(":").length > 2) {
        return DATE_FORMAT_AM_PM_SS.parse(s);
      } else {
        return DATE_FORMAT_AM_PM.parse(s);
      }
    } else {
      if(s.split(":").length > 2) {
        return DATE_FORMAT_SS.parse(s);
      } else {
        return DATE_FORMAT.parse(s);
      }
    }
  }

  /**
   * Holds data about the schedule for a team.
   * 
   * @author jpschewe
   * @version $Revision$
   * 
   */
  private final class TeamScheduleInfo {
    public int teamNumber;

    public String division;

    public Date presentation;

    public Date technical;

    public String judge;

    public Date[] perf = new Date[_numberOfRounds];

    public String[] perfTable = new String[_numberOfRounds];

  }
}
