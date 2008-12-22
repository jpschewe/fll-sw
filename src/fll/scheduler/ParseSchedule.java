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
import java.util.Collections;
import java.util.Comparator;
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

import com.lowagie.text.pdf.PdfPTable;

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

  private static final Logger LOGGER = Logger.getLogger(ParseSchedule.class);

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

  private int _teamNameColumn = -1;

  private int _organizationColumn = -1;

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

  private static int NUMBER_OF_ROUNDS = 3;

  private static final DateFormat DATE_FORMAT_AM_PM = new SimpleDateFormat("hh:mm a");

  private static final DateFormat DATE_FORMAT_AM_PM_SS = new SimpleDateFormat("hh:mm:ss a");

  private static final DateFormat OUTPUT_DATE_FORMAT = new SimpleDateFormat("HH:mm");

  private static final DateFormat DATE_FORMAT_SS = new SimpleDateFormat("HH:mm:ss");

  public static final long SECONDS_PER_MINUTE = 60;

  public static final long PERFORMANCE_DURATION = 5 * SECONDS_PER_MINUTE * 1000;

  public static final long SUBJECTIVE_DURATION = 20 * SECONDS_PER_MINUTE * 1000;

  public static final long CHANGETIME = 15 * SECONDS_PER_MINUTE * 1000;

  /**
   * This is the time required between performance runs for each team.
   */
  public static final long PERFORMANCE_CHANGETIME = 45 * SECONDS_PER_MINUTE * 1000;

  /**
   * This is the time required between performance runs for the two teams in
   * involved in the performance run that crosses round 1 and round 2 when there
   * is an odd number of teams.
   */
  public static final long SPECIAL_PERFORMANCE_CHANGETIME = 30 * SECONDS_PER_MINUTE * 1000;

  /**
   * @param args
   */
  public static void main(final String[] args) {
    if(args.length < 1) {
      LOGGER.fatal("Must specify the file to read");
      System.exit(1);
    }

    for(final String arg : args) {
      final File file = new File(arg);
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
            LOGGER.fatal(ioe, ioe);
            System.exit(1);
          }
        }
      } else if(file.isFile()) {
        final ParseSchedule ps = new ParseSchedule();

        try {
          ps.parseFile(file);
        } catch(final IOException ioe) {
          LOGGER.fatal(ioe, ioe);
          System.exit(1);
        }
      }
    }

    LOGGER.info("Finished, if no errors found, you're good");
  }

  public void parseFile(final File file) throws IOException {
    LOGGER.info(new Formatter().format("Reading file %s", file.getAbsoluteFile()));

    if(!file.canRead() || !file.isFile()) {
      LOGGER.fatal("File is not readable or not a file: " + file.getAbsolutePath());
      return;
    }

    final CSVReader csvreader = new CSVReader(new FileReader(file));

    while(_teamNumColumn == -1) {
      final String[] line = csvreader.readNext();
      if(null == line) {
        LOGGER.fatal("Cannot find header line and reached EOF");
        System.exit(1);
      }

      _teamNumColumn = -1;
      _teamNameColumn = -1;
      _organizationColumn = -1;
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
        } else if(line[i].contains("Organization")) {
          _organizationColumn = i;
        } else if(line[i].equals("Team Name")) {
          _teamNameColumn = i;
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
      LOGGER.fatal("Could not find teamNumColumn");
      System.exit(1);
    }

    if(-1 == _teamNameColumn) {
      LOGGER.fatal("Could not find teamNamColumn");
      System.exit(1);
    }

    if(-1 == _organizationColumn) {
      LOGGER.fatal("Could not find organizationColumn");
      System.exit(1);
    }

    if(-1 == _divisionColumn) {
      LOGGER.fatal("Could not find divisionColumn");
      System.exit(1);
    }
    if(-1 == _presentationColumn) {
      LOGGER.fatal("Could not find presentationColumn");
      System.exit(1);
    }
    if(-1 == _technicalColumn) {
      LOGGER.fatal("Could not find technicalColumn");
      System.exit(1);
    }
    if(-1 == _judgeGroupColumn) {
      LOGGER.fatal("Could not find judgeGroupColumn");
      System.exit(1);
    }
    if(-1 == _perf1Column) {
      LOGGER.fatal("Could not find perf1Column");
      System.exit(1);
    }
    if(-1 == _perf1TableColumn) {
      LOGGER.fatal("Could not find perf1TableColumn");
      System.exit(1);
    }
    if(-1 == _perf2Column) {
      LOGGER.fatal("Could not find perf2Column");
      System.exit(1);
    }
    if(-1 == _perf2TableColumn) {
      LOGGER.fatal("Could not find perf2TableColumn");
      System.exit(1);
    }
    if(-1 == _perf3Column) {
      LOGGER.fatal("Could not find perf3Column");
      System.exit(1);
    }
    if(-1 == _perf3TableColumn) {
      LOGGER.fatal("Could not find perf3TableColumn");
      System.exit(1);
    }

    final Map<Date, Map<String, List<TeamScheduleInfo>>> matches = new HashMap<Date, Map<String, List<TeamScheduleInfo>>>();
    final Set<String> tableColors = new HashSet<String>();
    final Set<String> divisions = new HashSet<String>();
    final Set<String> judges = new HashSet<String>();
    final List<TeamScheduleInfo> schedule = new LinkedList<TeamScheduleInfo>();
    TeamScheduleInfo ti;
    while(null != (ti = parseLine(csvreader))) {
      schedule.add(ti);

      // keep track of some meta information
      for(int round = 0; round < ti.perfTableColor.length; ++round) {
        tableColors.add(ti.perfTableColor[round]);
        addToMatches(matches, ti, round);
      }
      divisions.add(ti.division);
      judges.add(ti.judge);
    }

    for(final TeamScheduleInfo verify : schedule) {
      verifyTeam(matches, verify);
    }

    final int numberOfTableColors = tableColors.size();
    // final int numDivisions = divisions.size();
    final int numJudges = judges.size();
    verifyPerformanceAtTime(numberOfTableColors, schedule);
    verifyPresentationAtTime(schedule, numJudges);
    verifyTechnicalAtTime(schedule, numJudges);

    computeGeneralSchedule(schedule, matches);

    // print out detailed schedules
    outputPresentationSchedule(schedule);
    outputTechnicalSchedule(schedule);
    outputPerformanceSchedule(schedule);
  }

  private void outputPerformanceSchedule(final List<TeamScheduleInfo> schedule) {
    for(int round = 0; round < NUMBER_OF_ROUNDS; ++round) {
      Collections.sort(schedule, getPerformanceComparator(round));
      LOGGER.info(new Formatter().format("Team #\tDiv\tSchool or Organization\tTeam Name\tPerf #%d\tPerf %d Table", (round+1), (round+1)));
      final String formatString = "%d\t%s\t%s\t%s\t%s\t%s %d";
      for(final TeamScheduleInfo si : schedule) {
        LOGGER.info(new Formatter().format(formatString, si.teamNumber, si.division, si.organization, si.teamName, OUTPUT_DATE_FORMAT
            .format(si.perf[round]), si.perfTableColor[round], si.perfTableSide[round]));
      }
    }
  }

  private void outputPresentationSchedule(final List<TeamScheduleInfo> schedule) {
    Collections.sort(schedule, _presentationComparator);
    LOGGER.info("Team #\tDiv\tSchool or Organization\tTeam Name\tPresentation\tJudging Station");
    final String formatString = "%d\t%s\t%s\t%s\t%s\t%s";
    for(final TeamScheduleInfo si : schedule) {
      LOGGER.info(new Formatter().format(formatString, si.teamNumber, si.division, si.organization, si.teamName, OUTPUT_DATE_FORMAT
          .format(si.presentation), si.judge));
    }
    
  }

  private void outputTechnicalSchedule(final List<TeamScheduleInfo> schedule) {
    Collections.sort(schedule, _technicalComparator);
    LOGGER.info("Team #\tDiv\tSchool or Organization\tTeam Name\tTechnical\tJudging Station");
    final String formatString = "%d\t%s\t%s\t%s\t%s\t%s";
    for(final TeamScheduleInfo si : schedule) {
      LOGGER.info(new Formatter().format(formatString, si.teamNumber, si.division, si.organization, si.teamName, OUTPUT_DATE_FORMAT
          .format(si.technical), si.judge));
    }
  }

  /**
   * Sort by division, then judge, then by time.
   */
  private static final Comparator<TeamScheduleInfo> _presentationComparator = new Comparator<TeamScheduleInfo>() {
    public int compare(final TeamScheduleInfo one, final TeamScheduleInfo two) {
      if(!one.division.equals(two.division)) {
        return one.division.compareTo(two.division);
      } else if(!one.judge.equals(two.judge)) {
        return one.judge.compareTo(two.judge);
      } else {
        return one.presentation.compareTo(two.presentation);
      }
    }
  };

  /**
   * Sort by division, then by time.
   */
  private static final Comparator<TeamScheduleInfo> _technicalComparator = new Comparator<TeamScheduleInfo>() {
    public int compare(final TeamScheduleInfo one, final TeamScheduleInfo two) {
      if(!one.division.equals(two.division)) {
        return one.division.compareTo(two.division);
      } else if(!one.judge.equals(two.judge)) {
        return one.judge.compareTo(two.judge);
      } else {
        return one.technical.compareTo(two.technical);
      }
    }
  };

  /**
   * Sort by by time, then by table color, then table side
   */
  private Comparator<TeamScheduleInfo> getPerformanceComparator(final int round) {
    return new Comparator<TeamScheduleInfo>() {
      public int compare(final TeamScheduleInfo one, final TeamScheduleInfo two) {
        if(!one.perf[round].equals(two.perf[round])) {
          return one.perf[round].compareTo(two.perf[round]);
        } else if(!one.perfTableColor[round].equals(two.perfTableColor[round])) {
          return one.perfTableColor[round].compareTo(two.perfTableColor[round]);
        } else {
          final int oneSide = one.perfTableSide[round];
          final int twoSide = two.perfTableSide[round];
          if(oneSide == twoSide) {
            return 0;
          } else if(oneSide < twoSide) {
            return -1;
          } else {
            return 1;
          }
        }
      }
    };
  }

  private void computeGeneralSchedule(final List<TeamScheduleInfo> schedule, final Map<Date, Map<String, List<TeamScheduleInfo>>> matches) {
    Date minTechnical = null;
    Date maxTechnical = null;
    Date minPresentation = null;
    Date maxPresentation = null;
    final Date[] minPerf = new Date[NUMBER_OF_ROUNDS];
    final Date[] maxPerf = new Date[NUMBER_OF_ROUNDS];

    for(final TeamScheduleInfo si : schedule) {
      if(null != si.technical) {
        if(null == minTechnical || si.technical.before(minTechnical)) {
          minTechnical = si.technical;
        }
        if(null == maxTechnical || si.technical.after(maxTechnical)) {
          maxTechnical = si.technical;
        }
      }
      if(null != si.presentation) {
        if(null == minPresentation || si.presentation.before(minPresentation)) {
          minPresentation = si.presentation;
        }
        if(null == maxPresentation || si.presentation.after(maxPresentation)) {
          maxPresentation = si.presentation;
        }
      }

      for(int i = 0; i < NUMBER_OF_ROUNDS; ++i) {
        if(null != si.perf[i]) {
          // ignore the teams that cross round boundaries
          final int opponentRound = findOpponentRound(matches, si, i);
          if(opponentRound == i) {
            if(null == minPerf[i] || si.perf[i].before(minPerf[i])) {
              minPerf[i] = si.perf[i];
            }

            if(null == maxPerf[i] || si.perf[i].after(maxPerf[i])) {
              maxPerf[i] = si.perf[i];
            }
          }
        }
      }

    }

    // print out the general schedule
    LOGGER.info("min technical: " + OUTPUT_DATE_FORMAT.format(minTechnical));
    LOGGER.info("max technical: " + OUTPUT_DATE_FORMAT.format(maxTechnical));
    LOGGER.info("min presentation: " + OUTPUT_DATE_FORMAT.format(minPresentation));
    LOGGER.info("max presentation: " + OUTPUT_DATE_FORMAT.format(maxPresentation));
    for(int i = 0; i < NUMBER_OF_ROUNDS; ++i) {
      LOGGER.info("min performance round " + (i + 1) + ": " + OUTPUT_DATE_FORMAT.format(minPerf[i]));
      LOGGER.info("max performance round " + (i + 1) + ": " + OUTPUT_DATE_FORMAT.format(maxPerf[i]));
    }

  }

  /**
   * Add the data from the specified round of the specified TeamScheduleInfo to
   * matches.
   * 
   * @param matches
   *          the list of matches
   * @param ti
   *          the schedule info
   * @param round
   *          the round we care about
   * @return true if this succeeds, false if this shows too many teams on the
   *         table
   */
  private static boolean addToMatches(final Map<Date, Map<String, List<TeamScheduleInfo>>> matches, final TeamScheduleInfo ti, final int round) {
    final Map<String, List<TeamScheduleInfo>> timeMatches;
    if(matches.containsKey(ti.perf[round])) {
      timeMatches = matches.get(ti.perf[round]);
    } else {
      timeMatches = new HashMap<String, List<TeamScheduleInfo>>();
      matches.put(ti.perf[round], timeMatches);
    }

    final List<TeamScheduleInfo> tableMatches;
    if(timeMatches.containsKey(ti.perfTableColor[round])) {
      tableMatches = timeMatches.get(ti.perfTableColor[round]);
    } else {
      tableMatches = new LinkedList<TeamScheduleInfo>();
      timeMatches.put(ti.perfTableColor[round], tableMatches);
    }

    tableMatches.add(ti);

    if(tableMatches.size() > 2) {
      LOGGER.error(new Formatter().format("Too many teams competing on table: %s at time: %s. Teams: %s", ti.perfTableColor[round],
          OUTPUT_DATE_FORMAT.format(ti.perf[round]), tableMatches));
      return false;
    } else {
      return true;
    }
  }

  /**
   * Verify that there are no more than <code>numberOfTables</code> teams
   * performing at the same time.
   * 
   * @param numberOfTableColors
   * @param round
   * @return
   */
  private boolean verifyPerformanceAtTime(final int numberOfTableColors, final List<TeamScheduleInfo> schedule) {
    // constraint set 6
    final Map<Date, Set<Integer>> teamsAtTime = new HashMap<Date, Set<Integer>>();
    for(final TeamScheduleInfo si : schedule) {
      for(int round = 0; round < NUMBER_OF_ROUNDS; ++round) {
        Set<Integer> teams;
        if(teamsAtTime.containsKey(si.perf[round])) {
          teams = teamsAtTime.get(si.perf[round]);
        } else {
          teams = new HashSet<Integer>();
          teamsAtTime.put(si.perf[round], teams);
        }
        teams.add(si.teamNumber);
      }
    }

    boolean retval = true;
    for(final Map.Entry<Date, Set<Integer>> entry : teamsAtTime.entrySet()) {
      if(entry.getValue().size() > numberOfTableColors * 2) {
        LOGGER.error(new Formatter().format("There are too many teams at %s", entry.getKey()));

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
  private static boolean verifyPresentationAtTime(final List<TeamScheduleInfo> schedule, final int numJudges) {
    // constraint set 7
    final Map<Date, Set<TeamScheduleInfo>> teamsAtTime = new HashMap<Date, Set<TeamScheduleInfo>>();
    for(final TeamScheduleInfo si : schedule) {
      final Set<TeamScheduleInfo> teams;
      if(teamsAtTime.containsKey(si.presentation)) {
        teams = teamsAtTime.get(si.presentation);
      } else {
        teams = new HashSet<TeamScheduleInfo>();
        teamsAtTime.put(si.presentation, teams);
      }
      teams.add(si);
    }

    boolean retval = true;
    for(final Map.Entry<Date, Set<TeamScheduleInfo>> entry : teamsAtTime.entrySet()) {
      if(entry.getValue().size() > numJudges) {
        LOGGER.error(new Formatter().format("There are too many teams at %s in presentation", entry.getKey()));

        retval = false;
      }

      final Set<String> judges = new HashSet<String>();
      for(final TeamScheduleInfo ti : entry.getValue()) {
        if(!judges.add(ti.judge)) {
          LOGGER.error(new Formatter().format("Judges %s cannot see more than one team at %s in presentation", ti.judge, ti.presentation));
          retval = false;
        }
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
  private static boolean verifyTechnicalAtTime(final List<TeamScheduleInfo> schedule, final int numJudges) {
    // constraint set 7
    final Map<Date, Set<TeamScheduleInfo>> teamsAtTime = new HashMap<Date, Set<TeamScheduleInfo>>();
    for(final TeamScheduleInfo si : schedule) {
      final Set<TeamScheduleInfo> teams;
      if(teamsAtTime.containsKey(si.technical)) {
        teams = teamsAtTime.get(si.technical);
      } else {
        teams = new HashSet<TeamScheduleInfo>();
        teamsAtTime.put(si.technical, teams);
      }
      teams.add(si);
    }

    boolean retval = true;
    for(final Map.Entry<Date, Set<TeamScheduleInfo>> entry : teamsAtTime.entrySet()) {
      if(entry.getValue().size() > numJudges) {
        LOGGER.error(new Formatter().format("There are too many teams at %s in technical", entry.getKey()));

        retval = false;
      }

      final Set<String> judges = new HashSet<String>();
      for(final TeamScheduleInfo ti : entry.getValue()) {
        if(!judges.add(ti.judge)) {
          LOGGER.error(new Formatter().format("Judges %s cannot see more than one team at %s in presentation", ti.judge, ti.presentation));
          retval = false;
        }
      }
    }

    return retval;
  }

  /**
   * Find the round of the opponent for a given team in a given round.
   * 
   * @param matches
   * @param ti
   * @param round
   * @return the round number or -1 if no opponent
   */
  private int findOpponentRound(final Map<Date, Map<String, List<TeamScheduleInfo>>> matches, final TeamScheduleInfo ti, final int round) {
    final List<TeamScheduleInfo> tableMatches = matches.get(ti.perf[round]).get(ti.perfTableColor[round]);
    if(tableMatches.size() > 1) {
      if(tableMatches.get(0).equals(ti)) {
        return tableMatches.get(1).findRoundFortime(ti.perf[round]);
      } else {
        return tableMatches.get(0).findRoundFortime(ti.perf[round]);
      }
    } else {
      return -1;
    }
  }

  /**
   * Find the opponent for a given team in a given round.
   * 
   * @param matches
   * @param ti
   * @param round
   * @return the team number or -1 if no opponent
   */
  private int findOpponent(final Map<Date, Map<String, List<TeamScheduleInfo>>> matches, final TeamScheduleInfo ti, final int round) {
    final List<TeamScheduleInfo> tableMatches = matches.get(ti.perf[round]).get(ti.perfTableColor[round]);
    if(tableMatches.size() > 1) {
      if(tableMatches.get(0).equals(ti)) {
        return tableMatches.get(1).teamNumber;
      } else {
        return tableMatches.get(0).teamNumber;
      }
    } else {
      return -1;
    }
  }

  private boolean verifyTeam(final Map<Date, Map<String, List<TeamScheduleInfo>>> matches, final TeamScheduleInfo ti) {
    // constraint set 1
    if(ti.presentation.before(ti.technical)) {
      if(ti.presentation.getTime() + SUBJECTIVE_DURATION + CHANGETIME > ti.technical.getTime()) {
        LOGGER.error(new Formatter().format("Team %d has doesn't have enough time between presentation and technical", ti.teamNumber));
        return false;
      }
    } else {
      if(ti.technical.getTime() + SUBJECTIVE_DURATION + CHANGETIME > ti.presentation.getTime()) {
        LOGGER.error(new Formatter().format("Team %d has doesn't have enough time between presentation and technical", ti.teamNumber));
        return false;
      }
    }

    // constraint set 3
    final long changetime;
    final int round1OpponentRound = findOpponentRound(matches, ti, 0);
    final int round2OpponentRound = findOpponentRound(matches, ti, 1);
    if(round1OpponentRound != 0 || round2OpponentRound != 1) {
      changetime = SPECIAL_PERFORMANCE_CHANGETIME;
    } else {
      changetime = PERFORMANCE_CHANGETIME;
    }
    if(ti.perf[0].getTime() + PERFORMANCE_DURATION + changetime > ti.perf[1].getTime()) {
      LOGGER.error(new Formatter().format("Team %d doesn't have enough time (%d minutes) between performance 1 and performance 2: %s - %s",
          ti.teamNumber, changetime / 1000 / SECONDS_PER_MINUTE, OUTPUT_DATE_FORMAT.format(ti.perf[0]), OUTPUT_DATE_FORMAT.format(ti.perf[1])));
    }

    if(ti.perf[1].getTime() + PERFORMANCE_DURATION + PERFORMANCE_CHANGETIME > ti.perf[2].getTime()) {
      LOGGER.error(new Formatter().format("Team %d doesn't have enough time (%d minutes) between performance 2 and performance 3: %s - %s",
          ti.teamNumber, changetime / 1000 / SECONDS_PER_MINUTE, OUTPUT_DATE_FORMAT.format(ti.perf[1]), OUTPUT_DATE_FORMAT.format(ti.perf[2])));
    }

    // constraint set 4
    for(int round = 0; round < NUMBER_OF_ROUNDS; ++round) {
      if(ti.presentation.before(ti.perf[round])) {
        if(ti.presentation.getTime() + SUBJECTIVE_DURATION + CHANGETIME > ti.perf[round].getTime()) {
          LOGGER.error(new Formatter().format("Team %d has doesn't have enough time between presentation and performance round %d", ti.teamNumber,
              round + 1));
          return false;
        }
      } else {
        if(ti.perf[round].getTime() + PERFORMANCE_DURATION + CHANGETIME > ti.presentation.getTime()) {
          LOGGER.error(new Formatter().format("Team %d has doesn't have enough time between presentation and performance round %d", ti.teamNumber,
              round + 1));
          return false;
        }
      }
    }

    // constraint set 5
    for(int round = 0; round < NUMBER_OF_ROUNDS; ++round) {
      if(ti.technical.before(ti.perf[round])) {
        if(ti.technical.getTime() + SUBJECTIVE_DURATION + CHANGETIME > ti.perf[round].getTime()) {
          LOGGER.error(new Formatter().format("Team %d has doesn't have enough time between technical and performance round %d", ti.teamNumber,
              round + 1));
          return false;
        }
      } else {
        if(ti.perf[round].getTime() + PERFORMANCE_DURATION + CHANGETIME > ti.technical.getTime()) {
          LOGGER.error(new Formatter().format("Team %d has doesn't have enough time between technical and performance round %d", ti.teamNumber,
              round + 1));
          return false;
        }
      }
    }

    // make sure that all oponents are different
    for(int round = 0; round < NUMBER_OF_ROUNDS; ++round) {
      final int opponent = findOpponent(matches, ti, round);
      if(-1 != opponent) {
        for(int r = round + 1; r < NUMBER_OF_ROUNDS; ++r) {
          final int otherOpponent = findOpponent(matches, ti, r);
          if(otherOpponent != -1 && opponent == otherOpponent) {
            LOGGER.error(new Formatter().format("Team %d competes against %d more than once", ti.teamNumber, opponent));
            return false;
          }
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
      ti.teamName = line[_teamNameColumn];
      ti.organization = line[_organizationColumn];
      ti.division = line[_divisionColumn];
      ti.presentation = parseDate(line[_presentationColumn]);
      ti.technical = parseDate(line[_technicalColumn]);
      ti.judge = line[_judgeGroupColumn];
      ti.perf[0] = parseDate(line[_perf1Column]);
      String table = line[_perf1TableColumn];
      String[] tablePieces = table.split(" ");
      if(tablePieces.length != 2) {
        throw new RuntimeException("Error parsing table information from: " + table);
      }
      ti.perfTableColor[0] = tablePieces[0];
      ti.perfTableSide[0] = Utilities.NUMBER_FORMAT_INSTANCE.parse(tablePieces[1]).intValue();
      ti.perf[0] = parseDate(line[_perf1Column]);

      table = line[_perf2TableColumn];
      tablePieces = table.split(" ");
      if(tablePieces.length != 2) {
        throw new RuntimeException("Error parsing table information from: " + table);
      }
      ti.perfTableColor[1] = tablePieces[0];
      ti.perfTableSide[1] = Utilities.NUMBER_FORMAT_INSTANCE.parse(tablePieces[1]).intValue();
      ti.perf[1] = parseDate(line[_perf2Column]);

      table = line[_perf3TableColumn];
      tablePieces = table.split(" ");
      if(tablePieces.length != 2) {
        throw new RuntimeException("Error parsing table information from: " + table);
      }
      ti.perfTableColor[2] = tablePieces[0];
      ti.perfTableSide[2] = Utilities.NUMBER_FORMAT_INSTANCE.parse(tablePieces[1]).intValue();
      ti.perf[2] = parseDate(line[_perf3Column]);

      return ti;
    } catch(final ParseException pe) {
      LOGGER.error("Error parsing line: " + Arrays.toString(line), pe);
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
        return OUTPUT_DATE_FORMAT.parse(s);
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

    public String teamName;

    public String organization;

    public String division;

    public Date presentation;

    public Date technical;

    public String judge;

    public Date[] perf = new Date[NUMBER_OF_ROUNDS];

    public String[] perfTableColor = new String[NUMBER_OF_ROUNDS];

    public int[] perfTableSide = new int[NUMBER_OF_ROUNDS];

    /**
     * Find the performance round for the matching time.
     * 
     * @param time
     * @return the round, -1 if cannot be found
     */
    public int findRoundFortime(final Date time) {
      for(int round = 0; round < NUMBER_OF_ROUNDS; ++round) {
        if(perf[round].equals(time)) {
          return round;
        }
      }
      return -1;
    }

    @Override
    public String toString() {
      return "[ScheduleInfo for " + teamNumber + "]";
    }
  }
}
