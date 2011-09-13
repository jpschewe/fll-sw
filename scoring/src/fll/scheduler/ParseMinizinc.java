/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.log4j.Logger;

import fll.scheduler.TeamScheduleInfo.PerformanceTime;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;

/**
 * Parse the output from minizinc and output a schedule.
 */
public class ParseMinizinc {

  private static final Logger LOGGER = LogUtils.getLogger();

  private final int nsubjective;

  private final int nrounds;

  private final int ntables;

  private final int ngroups;

  private final int tmax;

  private final int maxTeamsPerGroup;

  private static final String arrayPrefix = "1..";

  private int startIdx;

  private int commaIdx;

  private String line;

  private final LineNumberReader reader;

  private final File resultFile;

  private final Date startTime;

  private final int tinc;

  private int[][][][] sy;

  private int[][][][] sz;

  private int[][][][][] py;

  private int[][][][][] pz;

  public ParseMinizinc(final Date startTime,
                       final File datafile,
                       final File resultFile) throws IOException {
    this.startTime = startTime;

    final Properties properties = parseMinizincData(datafile);
    LOGGER.debug(properties.toString());

    tinc = readIntProperty(properties, "TInc");
    ngroups = readIntProperty(properties, "NGroups");
    nrounds = readIntProperty(properties, "NRounds");
    ntables = readIntProperty(properties, "NTables");
    final int tmaxHours = readIntProperty(properties, "TMax_hours");
    tmax = tmaxHours
        * 60 / tinc;
    nsubjective = readIntProperty(properties, "NSubjective");
    final String groupCountsStr = properties.getProperty("group_counts");
    final int lbracket = groupCountsStr.indexOf('[');
    if (-1 == lbracket) {
      throw new FLLRuntimeException("No '[' found in group_counts: '"
          + groupCountsStr + "'");
    }
    final int rbracket = groupCountsStr.indexOf(']', lbracket);
    if (-1 == rbracket) {
      throw new FLLRuntimeException("No ']' found in group_counts: '"
          + groupCountsStr + "'");
    }
    final String groups = groupCountsStr.substring(lbracket + 1, rbracket);
    int lmaxTeamsPerGroup = 0;
    for (final String groupCount : groups.split(",")) {
      final int count = Integer.valueOf(groupCount);
      lmaxTeamsPerGroup = Math.max(lmaxTeamsPerGroup, count);
    }
    maxTeamsPerGroup = lmaxTeamsPerGroup;
    LOGGER.debug("Max teams in group: "
        + maxTeamsPerGroup);

    this.reader = new LineNumberReader(new FileReader(resultFile));
    this.resultFile = resultFile;
  }

  /**
   * Depends on startIdx and commaIdx.
   * 
   * @return
   * @throws ParseException
   */
  private int parseArrayBound(final String num) throws ParseException {
    startIdx = line.indexOf(arrayPrefix, commaIdx);
    if (-1 == startIdx) {
      throw new ParseException("Can't find "
          + num + " '1..' in py line '" + line + "'", reader.getLineNumber());
    }
    commaIdx = line.indexOf(',', startIdx);
    if (-1 == commaIdx) {
      throw new ParseException("Can't find "
          + num + " comma in py line '" + line + "'", reader.getLineNumber());
    }
    final String end = line.substring(startIdx
        + arrayPrefix.length(), commaIdx);
    return Integer.valueOf(end);
  }

  private int[][][][] parse4dArray(final int one,
                                   final int two,
                                   final int three,
                                   final int four) throws ParseException {
    final int[][][][] retval = new int[one][two][three][four];
    final int startIdx = line.indexOf('[');
    if (-1 == startIdx) {
      throw new ParseException("Can't find start bracket of array in line '"
          + line + "'", reader.getLineNumber());
    }
    final int endIdx = line.indexOf(']');
    if (-1 == endIdx) {
      throw new ParseException("Can't find end bracket of array in line '"
          + line + "'", reader.getLineNumber());
    }

    final String array = line.substring(startIdx + 1, endIdx);
    final String[] tokens = array.split(",");
    if (tokens.length != one
        * two * three * four) {
      throw new ParseException("Expected "
          + (one
              * two * three * four) + " elements in array, but found " + tokens.length, reader.getLineNumber());
    }

    int idx1 = 0;
    int idx2 = 0;
    int idx3 = 0;
    int idx4 = 0;
    for (final String str : tokens) {
      try {
        final int ele = Integer.valueOf(str.trim());
        retval[idx1][idx2][idx3][idx4] = ele;
        ++idx4;
        if (idx4 >= four) {
          idx4 = 0;
          ++idx3;
        }
        if (idx3 >= three) {
          idx3 = 0;
          ++idx2;
        }
        if (idx2 >= two) {
          idx2 = 0;
          ++idx1;
        }
        if (idx1 > one) {
          throw new ParseException("idx1 overflow, too many values", reader.getLineNumber());
        }
      } catch (final NumberFormatException e) {
        throw new ParseException("Unable to convert '"
            + str + "' into an integer", reader.getLineNumber());
      }
    }

    return retval;
  }

  private int[][][][][] parse5dArray(final int one,
                                     final int two,
                                     final int three,
                                     final int four,
                                     final int five) throws ParseException {
    final int[][][][][] retval = new int[one][two][three][four][five];
    final int startIdx = line.indexOf('[');
    if (-1 == startIdx) {
      throw new ParseException("Can't find start bracket of array in line '"
          + line + "'", reader.getLineNumber());
    }
    final int endIdx = line.indexOf(']');
    if (-1 == endIdx) {
      throw new ParseException("Can't find end bracket of array in line '"
          + line + "'", reader.getLineNumber());
    }

    final String array = line.substring(startIdx + 1, endIdx);
    final String[] tokens = array.split(",");
    if (tokens.length != one
        * two * three * four * five) {
      throw new ParseException("Expected "
          + (one
              * two * three * four * five) + " elements in array, but found " + tokens.length, reader.getLineNumber());
    }

    int idx1 = 0;
    int idx2 = 0;
    int idx3 = 0;
    int idx4 = 0;
    int idx5 = 0;
    for (final String str : tokens) {
      try {
        final int ele = Integer.valueOf(str.trim());
        retval[idx1][idx2][idx3][idx4][idx5] = ele;
        ++idx5;
        if (idx5 >= five) {
          idx5 = 0;
          ++idx4;
        }
        if (idx4 >= four) {
          idx4 = 0;
          ++idx3;
        }
        if (idx3 >= three) {
          idx3 = 0;
          ++idx2;
        }
        if (idx2 >= two) {
          idx2 = 0;
          ++idx1;
        }
        if (idx1 > one) {
          throw new ParseException("idx1 overflow, too many values", reader.getLineNumber());
        }
      } catch (final NumberFormatException e) {
        throw new ParseException("Unable to convert '"
            + str + "' into an integer", reader.getLineNumber());
      }
    }

    return retval;
  }

  private int[][][][][] parseP(final String varname) throws IOException, ParseException {
    final int parenIndex = line.indexOf('(');

    if (-1 == parenIndex) {
      throw new ParseException("Can't find paren in "
          + varname + " line '" + line + "'", reader.getLineNumber());
    }
    commaIdx = parenIndex;
    final int pyNGroups = parseArrayBound("first");
    LOGGER.debug(varname
        + " ngroups: " + pyNGroups);
    if (ngroups != pyNGroups) {
      throw new ParseException("Inconsistent number of groups "
          + varname + " says: " + pyNGroups + " previous line says: " + ngroups, reader.getLineNumber());
    }

    final int pyMaxTeamsPerGroup = parseArrayBound("second");
    LOGGER.debug(varname
        + " maxTeamsPerGroup: " + pyMaxTeamsPerGroup);
    if (maxTeamsPerGroup != pyMaxTeamsPerGroup) {
      throw new ParseException("Inconsistent max number of teams per group "
          + varname + " says: " + pyMaxTeamsPerGroup + " previous line says: " + maxTeamsPerGroup,
                               reader.getLineNumber());
    }

    final int pyNtables = parseArrayBound("third");
    LOGGER.debug(varname
        + " NTables: " + pyNtables);
    if (ntables != pyNtables) {
      throw new ParseException("Inconsistent ntables "
          + varname + " says: " + pyNtables + " previous line says: " + ntables, reader.getLineNumber());
    }

    final int pySides = parseArrayBound("fourth");
    LOGGER.debug(varname
        + " NSides: " + pySides);
    if (2 != pySides) {
      throw new ParseException("Should always have 2 sides to a table. "
          + varname + " says: " + pySides, reader.getLineNumber());
    }

    final int pyTMax = parseArrayBound("fifth");
    LOGGER.debug(varname
        + " TMax: " + pyTMax);
    if (tmax != pyTMax) {
      throw new ParseException("Inconsistent tmax "
          + varname + " says: " + pyTMax + " previous line says: " + tmax, reader.getLineNumber());
    }
    int[][][][][] px = parse5dArray(pyNGroups, pyMaxTeamsPerGroup, pyNtables, pySides, pyTMax);
    return px;
  }

  private int[][][][] parseS(final String varname) throws IOException, ParseException {
    final int parenIndex = line.indexOf('(');

    if (-1 == parenIndex) {
      throw new ParseException("Can't find paren in "
          + varname + " line '" + line + "'", reader.getLineNumber());
    }
    commaIdx = parenIndex;
    final int pyNGroups = parseArrayBound("first");
    LOGGER.debug(varname
        + " ngroups: " + pyNGroups);
    if (ngroups != pyNGroups) {
      throw new ParseException("Inconsistent number of groups "
          + varname + " says: " + pyNGroups + " previous line says: " + ngroups, reader.getLineNumber());
    }

    final int pyMaxTeamsPerGroup = parseArrayBound("second");
    LOGGER.debug(varname
        + " maxTeamsPerGroup: " + pyMaxTeamsPerGroup);
    if (maxTeamsPerGroup != pyMaxTeamsPerGroup) {
      throw new ParseException("Inconsistent max number of teams per group "
          + varname + " says: " + pyMaxTeamsPerGroup + " previous line says: " + maxTeamsPerGroup,
                               reader.getLineNumber());
    }

    final int pyNSubj = parseArrayBound("third");
    LOGGER.debug(varname
        + " NSubj: " + pyNSubj);
    if (nsubjective != pyNSubj) {
      throw new ParseException("Inconsistent NSujb "
          + varname + " says: " + pyNSubj + " previous line says: " + nsubjective, reader.getLineNumber());
    }

    final int pyTMax = parseArrayBound("fourth");
    LOGGER.debug(varname
        + " TMax: " + pyTMax);
    if (tmax != pyTMax) {
      throw new ParseException("Inconsistent tmax "
          + varname + " says: " + pyTMax + " previous line says: " + tmax, reader.getLineNumber());
    }
    final int[][][][] sx = parse4dArray(pyNGroups, pyMaxTeamsPerGroup, pyNSubj, pyTMax);
    return sx;
  }

  /**
   * Entry point.
   * 
   * @throws IOException if there is an error reading the file
   * @throws ParseException if there is an error parsing
   */
  private void parse() throws IOException, ParseException {
    while (null != (line = reader.readLine())) {
      if (line.length() == 0) {
        // empty
        return;
      } else if (line.equals("----------")) {
        // end of results
        break;
      } else if (line.startsWith("py = array5d")) {
        py = parseP("py");
      } else if (line.startsWith("pz = array5d")) {
        pz = parseP("pz");
      } else if (line.startsWith("sy = array4d")) {
        sy = parseS("sy");
      } else if (line.startsWith("sz = array4d")) {
        sz = parseS("sz");
      } else {
        throw new ParseException("Unrecognized line: '"
            + line + "'", 0);
      }
    }

    LOGGER.info("Finished parsing");
    LOGGER.info("nsubjective: "
        + nsubjective);
    LOGGER.info("ntables: "
        + ntables);
    LOGGER.info("ngroups: "
        + ngroups);
    for (int group = 0; group < sz.length; ++group) {
      for (int team = 0; team < sz[group].length; ++team) {
        for (int subj = 0; subj < sz[group][team].length; ++subj) {
          for (int t = 0; t < sz[group][team][subj].length; ++t) {
            if (sz[group][team][subj][t] > 0) {
              LOGGER.trace(String.format("sz[%d][%d][%d][%d] = %d", group, team, subj, t, sz[group][team][subj][t]));
            }
          }
        }
      }
    }
    for (int group = 0; group < sy.length; ++group) {
      for (int team = 0; team < sy[group].length; ++team) {
        for (int subj = 0; subj < sy[group][team].length; ++subj) {
          for (int t = 0; t < sy[group][team][subj].length; ++t) {
            if (sy[group][team][subj][t] > 0) {
              LOGGER.trace(String.format("sy[%d][%d][%d][%d] = %d", group, team, subj, t, sy[group][team][subj][t]));
            }
          }
        }
      }
    }

    outputSchedule();
  }

  private void outputSchedule() throws IOException {
    // FIXME do sanity check on n*
    final File schedule = new File(resultFile.getAbsolutePath()
        + ".csv");
    LOGGER.info("Schedule output to "
        + schedule.getAbsolutePath());

    final BufferedWriter writer = new BufferedWriter(new FileWriter(schedule));
    writer.write(TournamentSchedule.TEAM_NUMBER_HEADER
        + ",");
    writer.write(TournamentSchedule.TEAM_NAME_HEADER
        + ",");
    writer.write(TournamentSchedule.ORGANIZATION_HEADER
        + ",");
    writer.write(TournamentSchedule.DIVISION_HEADER
        + ",");
    writer.write(TournamentSchedule.JUDGE_GROUP_HEADER);
    for (int subj = 0; subj < nsubjective; ++subj) {
      writer.write(",Subj"
          + (subj + 1));
    }
    for (int round = 0; round < nrounds; ++round) {
      writer.write(","
          + String.format(TournamentSchedule.PERF_HEADER_FORMAT, round + 1));
      writer.write(","
          + String.format(TournamentSchedule.TABLE_HEADER_FORMAT, round + 1));
    }
    writer.newLine();

    for (int group = 0; group < ngroups; ++group) {
      for (int team = 0; team < maxTeamsPerGroup; ++team) {
        if (!teamExists(group, team)) {
          LOGGER.debug("doesn't exist group "
              + (group + 1) + " team " + (team + 1));
          continue;
        }
        LOGGER.debug("group "
            + (group + 1) + " team " + (team + 1));
        final int teamNum = (group + 1)
            * 100 + team;
        final int judgingGroup = group + 1;
        writer.write(String.format("%d,Team %d, Org %d, D%d, G%d", teamNum, teamNum, teamNum, judgingGroup,
                                   judgingGroup));
        for (int subj = 0; subj < nsubjective; ++subj) {
          final Date time = getTime(sz[group][team][subj], 1);
          if (null == time) {
            throw new RuntimeException("Could not find a subjective start for group: "
                + (group + 1) + " team: " + (team + 1) + " subj: " + (subj + 1));
          }
          writer.write(",");
          writer.write(TournamentSchedule.OUTPUT_DATE_FORMAT.get().format(time));
        }

        // find all performances for a team and then sort by time
        final SortedSet<PerformanceTime> perfTimes = new TreeSet<PerformanceTime>();
        for (int round = 0; round < nrounds; ++round) {
          for (int table = 0; table < ntables; ++table) {
            for (int side = 0; side < 2; ++side) {
              final Date time = getTime(pz[group][team][table][side], round + 1);
              if (null != time) {
                perfTimes.add(new PerformanceTime(time, "Table"
                    + (table + 1), (side + 1)));
              }
            }
          }
        }
        if (perfTimes.size() != nrounds) {
          throw new FLLRuntimeException("Expecting "
              + nrounds + " performance times, but only found " + perfTimes.size() + " group: " + (group + 1)
              + " team: " + (team + 1));
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
    }

    writer.close();
  }

  /**
   * Get the nth time from slot that is 1.
   * 
   * @param slots the slots to look in
   * @param count which time to find, 1 based count
   * @return
   */
  private Date getTime(final int[] slots,
                       final int count) {
    int n = 0;
    for (int i = 0; i < slots.length; ++i) {
      if (slots[i] == 1) {
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
   * Check if a team exists by checking if at least 1 of the sz variables is 1.
   */
  private boolean teamExists(final int group,
                             final int team) {
    for (int t = 0; t < tmax; ++t) {
      if (sz[group][team][0][t] > 0) {
        return true;
      }
    }
    return false;
  }

  public static Properties parseMinizincData(final File datafile) throws IOException {
    final BufferedReader reader = new BufferedReader(new FileReader(datafile));
    final StringWriter strWriter = new StringWriter();
    final BufferedWriter writer = new BufferedWriter(strWriter);

    String line;
    while (null != (line = reader.readLine())) {
      writer.write(line.replace('%', '#').replace(';', ' '));
      writer.newLine();
    }
    reader.close();
    writer.close();

    // load into properties
    final StringReader strReader = new StringReader(strWriter.toString());
    final Properties properties = new Properties();
    properties.load(new ReaderInputStream(strReader));

    return properties;
  }

  public static int readIntProperty(final Properties properties,
                                    final String property) {
    final String value = properties.getProperty(property);
    if (null == property) {
      throw new NullPointerException("Property '"
          + property + "' doesn't have a value");
    }
    return Integer.valueOf(value.trim());
  }

  public static void main(final String[] args) {
    LogUtils.initializeLogging();

    if (args.length != 3) {
      LOGGER.fatal("You must specify the start time (HH:MM), data file, and schedule result file");
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

      final File resultFile = new File(args[2]);
      if (!resultFile.canRead()) {
        LOGGER.fatal(resultFile.getAbsolutePath()
            + " is not readable");
        System.exit(3);
      }

      LOGGER.info("Reading result file: "
          + resultFile.getAbsolutePath());
      final ParseMinizinc parser = new ParseMinizinc(startTime, datafile, resultFile);
      parser.parse();
    } catch (final NumberFormatException e) {
      LOGGER.fatal("tinc cannot be parsed as a number: "
          + e.getMessage());
      System.exit(5);
    } catch (final IOException e) {
      LOGGER.fatal("Error reading file", e);
      System.exit(4);
    } catch (final ParseException e) {
      LOGGER.fatal(e.getMessage());
      System.exit(2);
    }

  }
}
