/*
 * Copyright (c) 2000-2008 HighTechKids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.scheduler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
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
import java.util.SortedSet;
import java.util.TreeSet;

import net.mtu.eggplant.util.Functions;
import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import fll.Team;
import fll.Tournament;
import fll.Utilities;
import fll.db.Queries;
import fll.scheduler.TeamScheduleInfo.SubjectiveTime;
import fll.util.CSVCellReader;
import fll.util.CellFileReader;
import fll.util.ExcelCellReader;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;

/**
 * Tournament schedule. Can parse the schedule from a spreadsheet.
 * <p>
 * Note to developers: The comments prefixed with "constraint:" refer back to
 * the scheduling document.
 * </p>
 */
public class TournamentSchedule implements Serializable {
  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Header on team number column.
   */
  public static final String TEAM_NUMBER_HEADER = "Team #";

  public static final String TEAM_NAME_HEADER = "Team Name";

  public static final String ORGANIZATION_HEADER = "Organization";

  public static final String DIVISION_HEADER = "Div";

  public static final String RESEARCH_HEADER = "Research";

  public static final String TECHNICAL_HEADER = "Technical";

  public static final String JUDGE_GROUP_HEADER = "Judging Group";

  public static final String BASE_PERF_HEADER = "Perf #";

  /**
   * Used with {@link String#format(String, Object...)} to create a performance
   * round header.
   */
  private static final String PERF_HEADER_FORMAT = BASE_PERF_HEADER
      + "%d";

  /**
   * Used with {@link String#format(String, Object...)} to create a performance
   * table header.
   */
  public static final String TABLE_HEADER_FORMAT = "Perf %d Table";

  private final int numRounds;

  /**
   * Number of rounds in this schedule.
   */
  public int getNumberOfRounds() {
    return numRounds;
  }

  private static final ThreadLocal<DateFormat> DATE_FORMAT_AM_PM = new ThreadLocal<DateFormat>() {
    @Override
    protected DateFormat initialValue() {
      return new SimpleDateFormat("hh:mm a");
    }
  };

  public static final ThreadLocal<DateFormat> DATE_FORMAT_AM_PM_SS = new ThreadLocal<DateFormat>() {
    @Override
    protected DateFormat initialValue() {
      return new SimpleDateFormat("hh:mm:ss a");
    }
  };

  public static final ThreadLocal<DateFormat> OUTPUT_DATE_FORMAT = new ThreadLocal<DateFormat>() {
    @Override
    protected DateFormat initialValue() {
      return new SimpleDateFormat("H:mm");
    }
  };

  private static final ThreadLocal<DateFormat> DATE_FORMAT_SS = new ThreadLocal<DateFormat>() {
    @Override
    protected DateFormat initialValue() {
      return new SimpleDateFormat("HH:mm:ss");
    }
  };

  private long changetime = Utilities.convertMinutesToMilliseconds(SchedParams.DEFAULT_CHANGETIME_MINUTES);

  private long performanceChangetime = Utilities.convertMinutesToMilliseconds(SchedParams.DEFAULT_PERFORMANCE_CHANGETIME_MINUTES);

  private int performanceDurationMinutes = SchedParams.DEFAULT_PERFORMANCE_MINUTES;

  private long performanceDuration = Utilities.convertMinutesToMilliseconds(performanceDurationMinutes);

  private int subjectiveDurationMinutes = SchedParams.DEFAULT_SUBJECTIVE_MINUTES;

  private long subjectiveDuration = Utilities.convertMinutesToMilliseconds(subjectiveDurationMinutes);

  private long specialPerformanceChangetime = Utilities.convertMinutesToMilliseconds(30);

  private final HashMap<Date, Map<String, List<TeamScheduleInfo>>> _matches = new HashMap<Date, Map<String, List<TeamScheduleInfo>>>();

  private final HashSet<String> _tableColors = new HashSet<String>();

  private final HashSet<String> _divisions = new HashSet<String>();

  private final HashSet<String> _judges = new HashSet<String>();

  private final LinkedList<TeamScheduleInfo> _schedule = new LinkedList<TeamScheduleInfo>();

  private final Set<String> subjectiveStations = new HashSet<String>();

  /**
   * Name of this tournament.
   */
  private final String name;

  /**
   * The list of subjective stations for this schedule.
   */
  public Set<String> getSubjectiveStations() {
    return Collections.unmodifiableSet(subjectiveStations);
  }

  /**
   * @return An unmodifiable copy of the schedule.
   */
  public List<TeamScheduleInfo> getSchedule() {
    return Collections.unmodifiableList(_schedule);
  }

  /**
   * Get the {@link TeamScheduleInfo} for the specified team number.
   * 
   * @return null if cannot be found
   */
  public TeamScheduleInfo getSchedInfoForTeam(final int teamNumber) {
    for (final TeamScheduleInfo si : _schedule) {
      if (si.getTeamNumber() == teamNumber) {
        return si;
      }
    }
    return null;
  }

  /**
   * @param name the name of the tournament
   * @param stream how to access the spreadsheet
   * @param sheetName the name of the worksheet the data is on
   * @param subjectiveHeaders the headers for the subjective columns
   * @throws ScheduleParseException if there is an error parsing the schedule
   */
  public TournamentSchedule(final String name,
                            final InputStream stream,
                            final String sheetName,
                            final Collection<String> subjectiveHeaders) throws IOException, ParseException,
      InvalidFormatException, ScheduleParseException {
    this(name, new ExcelCellReader(stream, sheetName), subjectiveHeaders);
  }

  /**
   * Read the tournament schedule from a CSV file.
   * 
   * @param csvFile where to read the schedule from
   * @param subjectiveHeaders the headers for the subjective columns
   * @throws ScheduleParseException
   * @throws ParseException
   */
  public TournamentSchedule(final String name,
                            final File csvFile,
                            final Collection<String> subjectiveHeaders) throws IOException, ParseException,
      ScheduleParseException {
    this(name, new CSVCellReader(csvFile), subjectiveHeaders);
  }

  /**
   * Common construction.
   * 
   * @throws IOException
   * @throws ScheduleParseException
   * @throws ParseException
   */
  private TournamentSchedule(final String name,
                             final CellFileReader reader,
                             final Collection<String> subjectiveHeaders) throws IOException, ParseException,
      ScheduleParseException {
    this.name = name;
    final ColumnInformation columnInfo = findColumns(reader, subjectiveHeaders);
    numRounds = columnInfo.getNumPerfs();
    parseData(reader, columnInfo);
    reader.close();
    this.subjectiveStations.clear();
    this.subjectiveStations.addAll(subjectiveHeaders);
  }

  /**
   * Load a tournament from the database.
   * 
   * @param connection
   * @param tournamentID
   * @throws SQLException
   */
  public TournamentSchedule(final Connection connection,
                            final int tournamentID) throws SQLException {
    final Tournament currentTournament = Tournament.findTournamentByID(connection, tournamentID);
    name = currentTournament.getName();

    PreparedStatement getSched = null;
    ResultSet sched = null;
    PreparedStatement getPerfRounds = null;
    ResultSet perfRounds = null;
    PreparedStatement getNumRounds = null;
    ResultSet numRounds = null;
    PreparedStatement getSubjective = null;
    ResultSet subjective = null;
    PreparedStatement getSubjectiveStations = null;
    ResultSet stations = null;
    try {
      getSubjectiveStations = connection.prepareStatement("SELECT DISTINCT name from sched_subjective WHERE tournament = ?");
      getSubjectiveStations.setInt(1, tournamentID);
      stations = getSubjectiveStations.executeQuery();
      while (stations.next()) {
        final String name = stations.getString(1);
        subjectiveStations.add(name);
      }
      SQLFunctions.close(stations);
      stations = null;
      SQLFunctions.close(getSubjectiveStations);
      getSubjectiveStations = null;

      getNumRounds = connection.prepareStatement("SELECT MAX(round) FROM sched_perf_rounds WHERE tournament = ?");
      getNumRounds.setInt(1, tournamentID);
      numRounds = getNumRounds.executeQuery();
      if (numRounds.next()) {
        this.numRounds = numRounds.getInt(1) + 1;
      } else {
        throw new RuntimeException("No rounds found for tournament: "
            + tournamentID);
      }

      getSched = connection.prepareStatement("SELECT team_number, judging_station"
          + " FROM schedule"//
          + " WHERE tournament = ?");
      getSched.setInt(1, tournamentID);

      getPerfRounds = connection.prepareStatement("SELECT round, perf_time, table_color, table_side" //
          + " FROM sched_perf_rounds" //
          + " WHERE tournament = ? AND team_number = ?" //
          + " ORDER BY round ASC");
      getPerfRounds.setInt(1, tournamentID);

      getSubjective = connection.prepareStatement("SELECT name, subj_time" //
          + " FROM sched_subjective" //
          + " WHERE tournament = ? AND team_number = ?");
      getSubjective.setInt(1, tournamentID);

      sched = getSched.executeQuery();
      while (sched.next()) {
        final int teamNumber = sched.getInt(1);
        final String judgingStation = sched.getString(2);

        final TeamScheduleInfo ti = new TeamScheduleInfo(this.numRounds, teamNumber);
        ti.setJudgingStation(judgingStation);

        getSubjective.setInt(2, teamNumber);
        subjective = getSubjective.executeQuery();
        while (subjective.next()) {
          final String name = subjective.getString(1);
          final Time subjTime = subjective.getTime(2);
          ti.addSubjectiveTime(new TeamScheduleInfo.SubjectiveTime(name, subjTime));
        }

        getPerfRounds.setInt(2, teamNumber);
        perfRounds = getPerfRounds.executeQuery();
        int prevRound = -1;
        while (perfRounds.next()) {
          final int round = perfRounds.getInt(1);
          if (round != prevRound + 1) {
            throw new RuntimeException("Rounds must be consecutive and start at 1. Tournament: "
                + tournamentID + " team: " + teamNumber + " round: " + (round + 1) + " prevRound: " + (prevRound + 1));
          }
          final Time perfTime = perfRounds.getTime(2);
          final String tableColor = perfRounds.getString(3);
          final int tableSide = perfRounds.getInt(4);
          if (tableSide != 1
              && tableSide != 2) {
            throw new RuntimeException("Tables sides must be 1 or 2. Tournament: "
                + tournamentID + " team: " + teamNumber);
          }
          ti.setPerf(round, Queries.timeToDate(perfTime));
          ti.setPerfTableColor(round, tableColor);
          ti.setPerfTableSide(round, prevRound);

          prevRound = round;
        }
        final String eventDivision = Queries.getEventDivision(connection, teamNumber, tournamentID);
        ti.setDivision(eventDivision);

        final Team team = Team.getTeamFromDatabase(connection, teamNumber);
        ti.setOrganization(team.getOrganization());
        ti.setTeamName(team.getTeamName());

        _schedule.add(ti);
      }

    } finally {
      SQLFunctions.close(stations);
      stations = null;
      SQLFunctions.close(getSubjectiveStations);
      getSubjectiveStations = null;
      SQLFunctions.close(sched);
      sched = null;
      SQLFunctions.close(getSched);
      getSched = null;
      SQLFunctions.close(perfRounds);
      perfRounds = null;
      SQLFunctions.close(getPerfRounds);
      getPerfRounds = null;
      SQLFunctions.close(numRounds);
      numRounds = null;
      SQLFunctions.close(getNumRounds);
      getNumRounds = null;
      SQLFunctions.close(subjective);
      subjective = null;
      SQLFunctions.close(getSubjective);
      getSubjective = null;
    }
  }

  /**
   * Check if this line is a header line. This checks for key headers and
   * returns true if they are found.
   */
  private static boolean isHeaderLine(final String[] line) {
    boolean retval = false;
    for (int i = 0; i < line.length; ++i) {
      if (TEAM_NUMBER_HEADER.equals(line[i])) {
        retval = true;
      }
    }

    return retval;
  }

  /**
   * Find the index of the columns. Reads lines until the headers are found or
   * EOF is reached.
   * 
   * @return the column information
   * @throws IOException
   * @throws RuntimeException if a header row cannot be found
   */
  public static ColumnInformation findColumns(final CellFileReader reader,
                                              final Collection<String> subjectiveHeaders) throws IOException {
    while (true) {
      final String[] line = reader.readNext();
      if (null == line) {
        throw new RuntimeException("Cannot find header line and reached EOF");
      }

      if (isHeaderLine(line)) {
        return parseHeader(subjectiveHeaders, line);
      }
    }
  }

  /**
   * Figure out how many performance rounds exist in this header line. This
   * method also checks that the corresponding table header exists for each
   * round and that the round numbers are contiguous starting at 1.
   * 
   * @throws FLLRuntimeException if there are problems with the performance
   *           round headers found
   */
  private static int countNumRounds(final String[] line) {
    final SortedSet<Integer> perfRounds = new TreeSet<Integer>();
    for (int i = 0; i < line.length; ++i) {
      if (line[i].startsWith(BASE_PERF_HEADER)
          && line[i].length() > BASE_PERF_HEADER.length()) {
        final String perfNumberStr = line[i].substring(BASE_PERF_HEADER.length());
        final Integer perfNumber = Integer.valueOf(perfNumberStr);
        if (!perfRounds.add(perfNumber)) {
          throw new FLLRuntimeException("Found performance rounds num "
              + perfNumber + " twice in the header: " + Arrays.asList(line));
        }
      }
    }

    if (perfRounds.isEmpty()) {
      throw new FLLRuntimeException("No performance rounds found in the header: "
          + Arrays.asList(line));
    }

    /*
     * check that the values start at 1, are contiguous, and that the
     * corresponding table header exists
     */
    int expectedValue = 1;
    for (Integer value : perfRounds) {
      if (null == value) {
        throw new FLLInternalException("Found null performance round in header!");
      }
      if (expectedValue != value.intValue()) {
        throw new FLLRuntimeException("Performance rounds not contiguous after "
            + (expectedValue - 1) + " found " + value);
      }

      final String tableHeader = String.format(TABLE_HEADER_FORMAT, expectedValue);
      if (!checkHeaderExists(line, tableHeader)) {
        throw new FLLRuntimeException("Couldn't find header for round "
            + expectedValue + ". Looking for header '" + tableHeader + "'");
      }

      ++expectedValue;
    }

    return perfRounds.size();
  }

  private static boolean checkHeaderExists(final String[] line,
                                           final String header) {
    return null != columnForHeader(line, header);
  }

  /**
   * Find the column that contains the specified header.
   * 
   * @return the column, null if not found
   */
  private static Integer columnForHeader(final String[] line,
                                         final String header) {
    for (int i = 0; i < line.length; ++i) {
      if (header.equals(line[i])) {
        return i;
      }
    }
    return null;
  }

  /**
   * Get the column number or throw {@link FLLRuntimeException} if the column it
   * not found.
   */
  private static int getColumnForHeader(final String[] line,
                                        final String header) {
    final Integer column = columnForHeader(line, header);
    if (null == column) {
      throw new FLLRuntimeException("Unable to find header '"
          + header + "' in " + Arrays.asList(line));
    } else {
      return column;
    }
  }

  private static ColumnInformation parseHeader(final Collection<String> subjectiveHeaders,
                                               final String[] line) {
    final Collection<String> remainingHeaders = new LinkedList<String>(Arrays.asList(line));
    final int numPerfRounds = countNumRounds(line);

    final int[] perfColumn = new int[numPerfRounds];
    final int[] perfTableColumn = new int[numPerfRounds];

    final int teamNumColumn = getColumnForHeader(line, TEAM_NUMBER_HEADER);
    remainingHeaders.remove(TEAM_NUMBER_HEADER);
    final int organizationColumn = getColumnForHeader(line, ORGANIZATION_HEADER);
    remainingHeaders.remove(ORGANIZATION_HEADER);
    final int teamNameColumn = getColumnForHeader(line, TEAM_NAME_HEADER);
    remainingHeaders.remove(TEAM_NAME_HEADER);
    final int divisionColumn = getColumnForHeader(line, DIVISION_HEADER);
    remainingHeaders.remove(DIVISION_HEADER);

    final int judgeGroupColumn = getColumnForHeader(line, JUDGE_GROUP_HEADER);
    remainingHeaders.remove(JUDGE_GROUP_HEADER);
    for (int round = 0; round < numPerfRounds; ++round) {
      final String perfHeader = String.format(PERF_HEADER_FORMAT, (round + 1));
      final String perfTableHeader = String.format(TABLE_HEADER_FORMAT, (round + 1));
      perfColumn[round] = getColumnForHeader(line, perfHeader);
      remainingHeaders.remove(perfHeader);
      perfTableColumn[round] = getColumnForHeader(line, perfTableHeader);
      remainingHeaders.remove(perfTableHeader);
    }

    final Map<Integer, String> subjectiveColumns = new HashMap<Integer, String>();
    for (final String header : subjectiveHeaders) {
      final int column = getColumnForHeader(line, header);
      subjectiveColumns.put(column, header);
    }

    return new ColumnInformation(line, teamNumColumn, organizationColumn, teamNameColumn, divisionColumn,
                                 subjectiveColumns, judgeGroupColumn, perfColumn, perfTableColumn);
  }

  /**
   * Parse the data of the schedule.
   * 
   * @throws IOException
   * @throws ScheduleParseException if there is an error with the schedule
   */
  private void parseData(final CellFileReader reader,
                         final ColumnInformation ci) throws IOException, ParseException, ScheduleParseException {
    TeamScheduleInfo ti;
    while (null != (ti = parseLine(reader, ci))) {
      cacheTeamScheduleInformation(ti);
    }
  }

  /**
   * Populate internal caches with the data from this newly created
   * {@link TeamScheduleInfo}.
   */
  private void cacheTeamScheduleInformation(final TeamScheduleInfo ti) {
    _schedule.add(ti);

    // keep track of some meta information
    for (int round = 0; round < getNumberOfRounds(); ++round) {
      _tableColors.add(ti.getPerfTableColor(round));
      addToMatches(ti, round);
    }
    _divisions.add(ti.getDivision());
    _judges.add(ti.getJudgingStation());
  }

  /**
   * Verify the schedule.
   * 
   * @return the constraint violations found, empty if no violations
   * @throws IOException
   */
  public List<ConstraintViolation> verifySchedule() {
    if (getNumberOfRounds() != 3) {
      throw new FLLRuntimeException("Schedules with other than 3 performance rounds cannot be properly checked");
    }

    final List<ConstraintViolation> constraintViolations = new LinkedList<ConstraintViolation>();

    for (final TeamScheduleInfo verify : _schedule) {
      verifyTeam(constraintViolations, verify);
    }

    verifyPerformanceAtTime(constraintViolations);
    verifyNumTeamsAtTable(constraintViolations);
    verifySubjectiveAtTime(constraintViolations);
    verifyNoOverlap(constraintViolations);

    return constraintViolations;
  }

  /**
   * Make sure that there are no overlaps in times at each scheduling location.
   * 
   * @param constraintViolations
   */
  private void verifyNoOverlap(final List<ConstraintViolation> violations) {
    final Map<String, SortedSet<Date>> tableToTime = new HashMap<String, SortedSet<Date>>();
    // category -> judge -> times
    final Map<String, Map<String, SortedSet<Date>>> subjectiveToTime = new HashMap<String, Map<String, SortedSet<Date>>>();
    for (final TeamScheduleInfo si : _schedule) {
      final String judge = si.getJudgingStation();
      for (final SubjectiveTime subj : si.getSubjectiveTimes()) {
        Map<String, SortedSet<Date>> map = subjectiveToTime.get(subj.getName());
        if (null == map) {
          map = new HashMap<String, SortedSet<Date>>();
          subjectiveToTime.put(subj.getName(), map);
        }
        SortedSet<Date> times = map.get(judge);
        if (null == times) {
          times = new TreeSet<Date>();
          map.put(judge, times);
        }
        times.add(subj.getTime());
      }

      for (int round = 0; round < getNumberOfRounds(); ++round) {
        final String table = si.getPerfTableColor(round);
        final int side = si.getPerfTableSide(round);
        final String tableKey = table
            + " " + side;
        SortedSet<Date> performance = tableToTime.get(tableKey);
        if (null == performance) {
          performance = new TreeSet<Date>();
          tableToTime.put(tableKey, performance);
        }
        performance.add(si.getPerf(round));
      }
    }

    // find violations
    for (final Map.Entry<String, Map<String, SortedSet<Date>>> topEntry : subjectiveToTime.entrySet()) {
      final String category = topEntry.getKey();
      for (final Map.Entry<String, SortedSet<Date>> entry : topEntry.getValue().entrySet()) {
        Date prev = null;
        for (final Date current : entry.getValue()) {
          if (null != prev) {
            if (prev.getTime()
                + getSubjectiveDuration() > current.getTime()) {
              final String message = String.format("Overlap in %s for judge %s between %s and %s", category,
                                                   entry.getKey(), OUTPUT_DATE_FORMAT.get().format(prev),
                                                   OUTPUT_DATE_FORMAT.get().format(current));
              violations.add(new ConstraintViolation(true, ConstraintViolation.NO_TEAM, new SubjectiveTime(category,
                                                                                                           prev), null,
                                                     null, message));
            }
          }

          prev = current;
        }
      }
    }

    for (final Map.Entry<String, SortedSet<Date>> entry : tableToTime.entrySet()) {
      Date prev = null;
      for (final Date current : entry.getValue()) {
        if (null != prev) {
          if (prev.getTime()
              + getPerformanceDuration() > current.getTime()) {
            final String message = String.format("Overlap in performance for table %s between %s and %s",
                                                 entry.getKey(), OUTPUT_DATE_FORMAT.get().format(prev),
                                                 OUTPUT_DATE_FORMAT.get().format(current));
            violations.add(new ConstraintViolation(true, ConstraintViolation.NO_TEAM, null, null, prev, message));
          }
        }

        prev = current;
      }
    }

  }

  /**
   * Find the team that is competing earliest after the specified time on the
   * specified table and side in the specified round.
   * 
   * @param time the time after which to find a competition
   * @param table the table color
   * @param side the side
   * @param round the round - zero based index
   */
  private TeamScheduleInfo findNextTeam(final Date time,
                                        final String table,
                                        final int side,
                                        final int round) {
    TeamScheduleInfo retval = null;
    for (final TeamScheduleInfo si : _schedule) {
      if (table.equals(si.getPerfTableColor(round))
          && side == si.getPerfTableSide(round) && si.getPerf(round).after(time)) {
        if (retval == null) {
          retval = si;
        } else if (null != si.getPerf(round)
            && si.getPerf(round).before(retval.getPerf(round))) {
          retval = si;
        }
      }
    }
    return retval;
  }

  /**
   * Check if the specified team needs to stay around after their performance to
   * even up the table.
   * 
   * @param schedule the schedule
   * @param si the TeamScheduleInfo for the team
   * @param round the round the team is competing at (zero based index)
   * @return the team one needs to compete against in an extra round or null if
   *         the team does not need to stay
   */
  private TeamScheduleInfo checkIfTeamNeedsToStay(final TeamScheduleInfo si,
                                                  final int round) {
    final int otherSide = 2 == si.getPerfTableSide(round) ? 1 : 2;
    final TeamScheduleInfo next = findNextTeam(si.getPerf(round), si.getPerfTableColor(round), otherSide, round);

    if (null != next) {
      final TeamScheduleInfo nextOpponent = findOpponent(next, round);
      if (null == nextOpponent) {
        return next;
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  /**
   * Output the detailed schedule.
   * 
   * @param output where to write the detailed schedule
   * @throws DocumentException
   * @throws IOException
   */
  public void outputDetailedSchedules(final OutputStream output) throws DocumentException, IOException {
    // print out detailed schedules
    final Document detailedSchedules = new Document(PageSize.LETTER); // portrait

    // Measurements are always in points (72 per inch)
    // This sets up 1/4 inch margins
    detailedSchedules.setMargins(0.25f * 72, 0.25f * 72, 0.35f * 72, 0.35f * 72);

    // output to a PDF
    PdfWriter.getInstance(detailedSchedules, output);

    detailedSchedules.open();

    for (final String subjectiveStation : subjectiveStations) {
      outputSubjectiveSchedule(detailedSchedules, subjectiveStation);
      detailedSchedules.add(Chunk.NEXTPAGE);
    }

    outputPerformanceSchedule(detailedSchedules);

    for (final TeamScheduleInfo si : _schedule) {
      outputTeamSchedule(detailedSchedules, si);
      detailedSchedules.add(Chunk.NEXTPAGE);
    }

    detailedSchedules.close();
  }

  private static final Font TEAM_TITLE_FONT = FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD);

  private static final Font TEAM_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD);

  private static final Font TEAM_VALUE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);

  /**
   * Output the detailed schedule for a team for the day.
   * 
   * @throws DocumentException
   */
  private void outputTeamSchedule(final Document detailedSchedules,
                                  final TeamScheduleInfo si) throws DocumentException {
    // used for various time computations
    final Calendar cal = Calendar.getInstance();

    final Paragraph para = new Paragraph();
    para.add(new Chunk(String.format("Detailed schedule for Team #%d - %s", si.getTeamNumber(), si.getTeamName()),
                       TEAM_TITLE_FONT));
    para.add(Chunk.NEWLINE);

    para.add(new Chunk("Division: ", TEAM_HEADER_FONT));
    para.add(new Chunk(si.getDivision(), TEAM_VALUE_FONT));
    para.add(Chunk.NEWLINE);

    for (final String subjectiveStation : subjectiveStations) {
      para.add(new Chunk(subjectiveStation
          + ": ", TEAM_HEADER_FONT));
      final Date start = si.getSubjectiveTimeByName(subjectiveStation).getTime();
      cal.setTime(start);
      cal.add(Calendar.MINUTE, subjectiveDurationMinutes);
      final Date end = cal.getTime();
      para.add(new Chunk(String.format("%s - %s", OUTPUT_DATE_FORMAT.get().format(start),
                                       OUTPUT_DATE_FORMAT.get().format(end)), TEAM_VALUE_FONT));
      para.add(Chunk.NEWLINE);
    }

    for (int round = 0; round < getNumberOfRounds(); ++round) {
      para.add(new Chunk(String.format(PERF_HEADER_FORMAT, round + 1)
          + ": ", TEAM_HEADER_FONT));
      final Date start = si.getPerf(round);
      cal.setTime(start);
      cal.add(Calendar.MINUTE, performanceDurationMinutes);
      final Date end = cal.getTime();
      para.add(new Chunk(String.format("%s - %s %s %d", OUTPUT_DATE_FORMAT.get().format(start),
                                       OUTPUT_DATE_FORMAT.get().format(end), si.getPerfTableColor(round),
                                       si.getPerfTableSide(round)), TEAM_VALUE_FONT));
      para.add(Chunk.NEWLINE);
    }

    para.add(Chunk.NEWLINE);
    para.add(new Chunk(
                       "Note that there may be more judging and a head to head round after this judging, please see the main tournament schedule for these details.",
                       TEAM_HEADER_FONT));
    detailedSchedules.add(para);
  }

  private void outputPerformanceSchedule(final Document detailedSchedules) throws DocumentException {

    for (int round = 0; round < getNumberOfRounds(); ++round) {
      Collections.sort(_schedule, getPerformanceComparator(round));

      // list of teams staying around to even up the teams
      final List<TeamScheduleInfo> teamsStaying = new LinkedList<TeamScheduleInfo>();

      final PdfPTable table = createTable(6);
      table.setWidths(new float[] { 2, 1, 3, 3, 2, 2 });

      final PdfPCell tournamentCell = createHeaderCell("Tournament: "
          + name + " Performance Round: " + String.valueOf(round + 1));
      tournamentCell.setColspan(6);
      table.addCell(tournamentCell);

      table.addCell(createHeaderCell(TEAM_NUMBER_HEADER));
      table.addCell(createHeaderCell(DIVISION_HEADER));
      table.addCell(createHeaderCell("School or Organization"));
      table.addCell(createHeaderCell("Team Name"));
      table.addCell(createHeaderCell(new Formatter().format(PERF_HEADER_FORMAT, (round + 1)).toString()));
      table.addCell(createHeaderCell(new Formatter().format(TABLE_HEADER_FORMAT, (round + 1)).toString()));
      table.setHeaderRows(1);

      for (final TeamScheduleInfo si : _schedule) {
        // check if team needs to stay and color the cell magenta if they do
        final BaseColor backgroundColor;
        if (null != checkIfTeamNeedsToStay(si, round)) {
          teamsStaying.add(si);
          backgroundColor = BaseColor.MAGENTA;
        } else {
          backgroundColor = null;
        }

        table.addCell(createCell(String.valueOf(si.getTeamNumber())));
        table.addCell(createCell(si.getDivision()));
        table.addCell(createCell(si.getOrganization()));
        table.addCell(createCell(si.getTeamName()));
        table.addCell(createCell(OUTPUT_DATE_FORMAT.get().format(si.getPerf(round)), backgroundColor));
        table.addCell(createCell(si.getPerfTableColor(round)
            + " " + si.getPerfTableSide(round), backgroundColor));
      }

      detailedSchedules.add(table);

      // output teams staying
      if (!teamsStaying.isEmpty()) {
        final String formatString = "Team %d will please stay at the table and compete again - score will not count.";
        final PdfPTable stayingTable = createTable(1);
        for (final TeamScheduleInfo si : teamsStaying) {
          stayingTable.addCell(createCell(new Formatter().format(formatString, si.getTeamNumber()).toString(),
                                          BaseColor.MAGENTA));
        }
        detailedSchedules.add(stayingTable);

      }

      detailedSchedules.add(Chunk.NEXTPAGE);
    }
  }

  private static PdfPCell createCell(final String text) throws BadElementException {
    final PdfPCell cell = createBasicCell(new Chunk(text));
    return cell;
  }

  private static PdfPCell createCell(final String text,
                                     final BaseColor backgroundColor) throws BadElementException {
    final PdfPCell cell = createCell(text);
    if (null != backgroundColor) {
      cell.setBackgroundColor(backgroundColor);
    }
    return cell;
  }

  private static PdfPCell createBasicCell(final Chunk chunk) throws BadElementException {
    final PdfPCell cell = new PdfPCell(new Phrase(chunk));
    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
    cell.setUseDescender(true);
    return cell;
  }

  private static PdfPCell createHeaderCell(final String text) throws BadElementException {
    final Chunk chunk = new Chunk(text);
    chunk.getFont().setStyle(Font.BOLD);
    final PdfPCell cell = createBasicCell(chunk);

    return cell;
  }

  private static PdfPTable createTable(final int columns) throws BadElementException {
    final PdfPTable table = new PdfPTable(columns);
    // table.setCellsFitPage(true);
    table.setWidthPercentage(100);
    return table;
  }

  private void outputSubjectiveSchedule(final Document detailedSchedules,
                                        final String subjectiveStation) throws DocumentException {
    final PdfPTable table = createTable(6);
    table.setWidths(new float[] { 2, 1, 3, 3, 2, 2 });

    final PdfPCell tournamentCell = createHeaderCell("Tournament: "
        + name + " - " + subjectiveStation);
    tournamentCell.setColspan(6);
    table.addCell(tournamentCell);

    table.addCell(createHeaderCell(TEAM_NUMBER_HEADER));
    table.addCell(createHeaderCell(DIVISION_HEADER));
    table.addCell(createHeaderCell("School or Organization"));
    table.addCell(createHeaderCell("Team Name"));
    table.addCell(createHeaderCell(subjectiveStation));
    table.addCell(createHeaderCell(JUDGE_GROUP_HEADER));
    table.setHeaderRows(2);

    Collections.sort(_schedule, getComparatorForSubjective(subjectiveStation));
    for (final TeamScheduleInfo si : _schedule) {
      table.addCell(createCell(String.valueOf(si.getTeamNumber())));
      table.addCell(createCell(si.getDivision()));
      table.addCell(createCell(si.getOrganization()));
      table.addCell(createCell(si.getTeamName()));
      table.addCell(createCell(OUTPUT_DATE_FORMAT.get().format(si.getSubjectiveTimeByName(subjectiveStation).getTime())));
      table.addCell(createCell(si.getJudgingStation()));
    }

    detailedSchedules.add(table);

  }

  /**
   * Get the comparator for outputting the schedule for the specified subjective
   * station. Sort by division, then judge, then by time.
   */
  private Comparator<TeamScheduleInfo> getComparatorForSubjective(final String name) {
    return new Comparator<TeamScheduleInfo>() {
      public int compare(final TeamScheduleInfo one,
                         final TeamScheduleInfo two) {

        if (!one.getDivision().equals(two.getDivision())) {
          return one.getDivision().compareTo(two.getDivision());
        } else if (!one.getJudgingStation().equals(two.getJudgingStation())) {
          return one.getJudgingStation().compareTo(two.getJudgingStation());
        } else {
          final TeamScheduleInfo.SubjectiveTime oneTime = one.getSubjectiveTimeByName(name);
          final TeamScheduleInfo.SubjectiveTime twoTime = two.getSubjectiveTimeByName(name);

          if (oneTime == null
              && twoTime == null) {
            return 0;
          } else if (oneTime == null
              && twoTime != null) {
            return -1;
          } else if (oneTime != null
              && twoTime == null) {
            return 1;
          } else {
            return oneTime.getTime().compareTo(twoTime.getTime());
          }
        }
      }
    };
  }

  /**
   * Sort by by time, then by table color, then table side
   */
  private Comparator<TeamScheduleInfo> getPerformanceComparator(final int round) {
    // Should think about caching this
    return new PerformanceComparator(round);
  }

  /**
   * Comparator for TeamScheduleInfo based up the performance times for a
   * specified round.
   */
  private static final class PerformanceComparator implements Comparator<TeamScheduleInfo>, Serializable {
    private final int round;

    public PerformanceComparator(final int round) {
      this.round = round;
    }

    public int compare(final TeamScheduleInfo one,
                       final TeamScheduleInfo two) {
      if (!one.getPerf(round).equals(two.getPerf(round))) {
        return one.getPerf(round).compareTo(two.getPerf(round));
      } else if (!one.getPerfTableColor(round).equals(two.getPerfTableColor(round))) {
        return one.getPerfTableColor(round).compareTo(two.getPerfTableColor(round));
      } else {
        final int oneSide = one.getPerfTableSide(round);
        final int twoSide = two.getPerfTableSide(round);
        if (oneSide == twoSide) {
          return 0;
        } else if (oneSide < twoSide) {
          return -1;
        } else {
          return 1;
        }
      }
    }
  }

  /**
   * Compute the general schedule and return it as a string
   */
  public String computeGeneralSchedule() {
    final Date[] minPerf = new Date[getNumberOfRounds()];
    final Date[] maxPerf = new Date[getNumberOfRounds()];
    final Map<String, Date> minSubjectiveTimes = new HashMap<String, Date>();
    final Map<String, Date> maxSubjectiveTimes = new HashMap<String, Date>();

    for (final TeamScheduleInfo si : _schedule) {
      for (final TeamScheduleInfo.SubjectiveTime stime : si.getSubjectiveTimes()) {
        final Date currentMin = minSubjectiveTimes.get(stime.getName());
        if (null == currentMin) {
          minSubjectiveTimes.put(stime.getName(), stime.getTime());
        } else {
          if (stime.getTime().before(currentMin)) {
            minSubjectiveTimes.put(stime.getName(), stime.getTime());
          }
        }
        final Date currentMax = maxSubjectiveTimes.get(stime.getName());
        if (null == currentMax) {
          minSubjectiveTimes.put(stime.getName(), stime.getTime());
        } else {
          if (stime.getTime().after(currentMax)) {
            maxSubjectiveTimes.put(stime.getName(), stime.getTime());
          }
        }

      }

      for (int i = 0; i < getNumberOfRounds(); ++i) {
        if (null != si.getPerf(i)) {
          // ignore the teams that cross round boundaries
          final int opponentRound = findOpponentRound(si, i);
          if (opponentRound == i) {
            if (null == minPerf[i]
                || si.getPerf(i).before(minPerf[i])) {
              minPerf[i] = si.getPerf(i);
            }

            if (null == maxPerf[i]
                || si.getPerf(i).after(maxPerf[i])) {
              maxPerf[i] = si.getPerf(i);
            }
          }
        }
      }

    }

    // print out the general schedule
    final Formatter output = new Formatter();
    final Set<String> subjectiveKeys = new HashSet<String>();
    subjectiveKeys.addAll(minSubjectiveTimes.keySet());
    subjectiveKeys.addAll(maxSubjectiveTimes.keySet());
    for (final String key : subjectiveKeys) {
      output.format("min %s: %s%n", key, OUTPUT_DATE_FORMAT.get().format(minSubjectiveTimes.get(key)));
      output.format("max %s: %s%n", key, OUTPUT_DATE_FORMAT.get().format(maxSubjectiveTimes.get(key)));
    }
    for (int i = 0; i < getNumberOfRounds(); ++i) {
      output.format("min performance round %d: %s%n", (i + 1), OUTPUT_DATE_FORMAT.get().format(minPerf[i]));
      output.format("max performance round %d: %s%n", (i + 1), OUTPUT_DATE_FORMAT.get().format(maxPerf[i]));
    }
    return output.toString();
  }

  /**
   * Add the data from the specified round of the specified TeamScheduleInfo to
   * matches.
   * 
   * @param ti the schedule info
   * @param round the round we care about
   */
  private void addToMatches(final TeamScheduleInfo ti,
                            final int round) {
    final Map<String, List<TeamScheduleInfo>> timeMatches;
    if (_matches.containsKey(ti.getPerf(round))) {
      timeMatches = _matches.get(ti.getPerf(round));
    } else {
      timeMatches = new HashMap<String, List<TeamScheduleInfo>>();
      _matches.put(ti.getPerf(round), timeMatches);
    }

    final List<TeamScheduleInfo> tableMatches;
    if (timeMatches.containsKey(ti.getPerfTableColor(round))) {
      tableMatches = timeMatches.get(ti.getPerfTableColor(round));
    } else {
      tableMatches = new LinkedList<TeamScheduleInfo>();
      timeMatches.put(ti.getPerfTableColor(round), tableMatches);
    }

    tableMatches.add(ti);

  }

  private void verifyNumTeamsAtTable(final Collection<ConstraintViolation> violations) {
    for (final Map.Entry<Date, Map<String, List<TeamScheduleInfo>>> dateEntry : _matches.entrySet()) {
      for (final Map.Entry<String, List<TeamScheduleInfo>> timeEntry : dateEntry.getValue().entrySet()) {
        final List<TeamScheduleInfo> tableMatches = timeEntry.getValue();
        if (tableMatches.size() > 2) {
          final List<Integer> teams = new LinkedList<Integer>();
          for (final TeamScheduleInfo team : tableMatches) {
            teams.add(team.getTeamNumber());
          }
          final String message = String.format("Too many teams competing on table: %s at time: %s. Teams: %s",
                                               timeEntry.getKey(), OUTPUT_DATE_FORMAT.get().format(dateEntry.getKey()),
                                               teams);
          violations.add(new ConstraintViolation(true, ConstraintViolation.NO_TEAM, null, null, null, message));
        }
      }
    }
  }

  /**
   * Verify that there are no more than <code>numberOfTables</code> teams
   * performing at the same time.
   */
  private void verifyPerformanceAtTime(final Collection<ConstraintViolation> violations) {
    // constraint: tournament:1
    final Map<Date, Set<TeamScheduleInfo>> teamsAtTime = new HashMap<Date, Set<TeamScheduleInfo>>();
    for (final TeamScheduleInfo si : _schedule) {
      for (int round = 0; round < getNumberOfRounds(); ++round) {
        final Set<TeamScheduleInfo> teams;
        if (teamsAtTime.containsKey(si.getPerf(round))) {
          teams = teamsAtTime.get(si.getPerf(round));
        } else {
          teams = new HashSet<TeamScheduleInfo>();
          teamsAtTime.put(si.getPerf(round), teams);
        }
        teams.add(si);
      }
    }

    for (final Map.Entry<Date, Set<TeamScheduleInfo>> entry : teamsAtTime.entrySet()) {
      if (entry.getValue().size() > _tableColors.size() * 2) {
        final String message = String.format("There are too many teams in performance at %s",
                                             OUTPUT_DATE_FORMAT.get().format(entry.getKey()));
        violations.add(new ConstraintViolation(true, ConstraintViolation.NO_TEAM, null, null, null, message));
      }
    }
  }

  /**
   * Ensure that no more than 1 team is in a subjective judging station at once.
   */
  private void verifySubjectiveAtTime(final Collection<ConstraintViolation> violations) {
    // constraint: tournament:1
    // category -> time -> teams
    final Map<String, Map<Date, Set<TeamScheduleInfo>>> allSubjective = new HashMap<String, Map<Date, Set<TeamScheduleInfo>>>();
    for (final TeamScheduleInfo si : _schedule) {
      for (final SubjectiveTime subj : si.getSubjectiveTimes()) {
        final Map<Date, Set<TeamScheduleInfo>> teamsAtTime;
        if (allSubjective.containsKey(subj.getName())) {
          teamsAtTime = allSubjective.get(subj.getName());
        } else {
          teamsAtTime = new HashMap<Date, Set<TeamScheduleInfo>>();
          allSubjective.put(subj.getName(), teamsAtTime);
        }
        final Set<TeamScheduleInfo> teams;
        if (teamsAtTime.containsKey(subj.getTime())) {
          teams = teamsAtTime.get(subj.getTime());
        } else {
          teams = new HashSet<TeamScheduleInfo>();
          teamsAtTime.put(subj.getTime(), teams);
        }
        teams.add(si);
      }
    }

    for (final Map.Entry<String, Map<Date, Set<TeamScheduleInfo>>> topEntry : allSubjective.entrySet()) {

      for (final Map.Entry<Date, Set<TeamScheduleInfo>> entry : topEntry.getValue().entrySet()) {
        if (entry.getValue().size() > _judges.size()) {
          final String message = String.format("There are too many teams in %s at %s", topEntry.getKey(),
                                               OUTPUT_DATE_FORMAT.get().format(entry.getKey()));
          violations.add(new ConstraintViolation(true, ConstraintViolation.NO_TEAM, null,
                                                 new SubjectiveTime(topEntry.getKey(), entry.getKey()), null, message));
        }

        final Set<String> judges = new HashSet<String>();
        for (final TeamScheduleInfo ti : entry.getValue()) {
          if (!judges.add(ti.getJudgingStation())) {
            final String message = String.format("%s judge %s cannot see more than one team at %s", topEntry.getKey(),
                                                 ti.getJudgingStation(), OUTPUT_DATE_FORMAT.get()
                                                                                           .format(entry.getKey()));
            violations.add(new ConstraintViolation(true, ConstraintViolation.NO_TEAM, null,
                                                   new SubjectiveTime(topEntry.getKey(), entry.getKey()), null, message));
          }
        }
      }
    }
  }

  /**
   * Find the round of the opponent for a given team in a given round.
   * 
   * @param matches
   * @param ti
   * @param round
   * @return the round number or -1 if no opponent
   */
  private int findOpponentRound(final TeamScheduleInfo ti,
                                final int round) {
    final List<TeamScheduleInfo> tableMatches = _matches.get(ti.getPerf(round)).get(ti.getPerfTableColor(round));
    if (tableMatches.size() > 1) {
      if (tableMatches.get(0).equals(ti)) {
        return tableMatches.get(1).findRoundFortime(ti.getPerf(round));
      } else {
        return tableMatches.get(0).findRoundFortime(ti.getPerf(round));
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
   * @return the team number or null if no opponent
   */
  private TeamScheduleInfo findOpponent(final TeamScheduleInfo ti,
                                        final int round) {
    final List<TeamScheduleInfo> tableMatches = _matches.get(ti.getPerf(round)).get(ti.getPerfTableColor(round));
    if (tableMatches.size() > 1) {
      if (tableMatches.get(0).equals(ti)) {
        return tableMatches.get(1);
      } else {
        return tableMatches.get(0);
      }
    } else {
      return null;
    }
  }

  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "IM_BAD_CHECK_FOR_ODD", justification = "The size of a container cannot be negative")
  private void verifyTeam(final Collection<ConstraintViolation> violations,
                          final TeamScheduleInfo ti) {
    // Relationship between each subjective category
    for (final SubjectiveTime category1 : ti.getSubjectiveTimes()) {
      for (final SubjectiveTime category2 : ti.getSubjectiveTimes()) {
        if (!category1.getName().equals(category2.getName())) {
          final Date cat1Time = category1.getTime();
          final Date cat2Time = category2.getTime();

          if (cat1Time.before(cat2Time)) {
            if (cat1Time.getTime()
                + getSubjectiveDuration() > cat2Time.getTime()) {
              final String message = String.format("Team %d is still in %s when they need to start %s",
                                                   ti.getTeamNumber(), category1.getName(), category2.getName());
              violations.add(new ConstraintViolation(true, ti.getTeamNumber(), category1, category2, null, message));
              return;
            } else if (cat1Time.getTime()
                + getSubjectiveDuration() + getChangetime() > cat2Time.getTime()) {
              final String message = String.format("Team %d has doesn't have enough time between %s and %s (need %d minutes)",
                                                   ti.getTeamNumber(), category1.getName(), category2.getName(),
                                                   getChangetimeAsMinutes());
              violations.add(new ConstraintViolation(true, ti.getTeamNumber(), category1, category2, null, message));
              return;
            }
          } else {
            if (cat2Time.getTime()
                + getSubjectiveDuration() > cat1Time.getTime()) {
              final String message = String.format("Team %d is still in %s when they need start %s",
                                                   ti.getTeamNumber(), category2.getName(), category1.getName());
              violations.add(new ConstraintViolation(true, ti.getTeamNumber(), category1, category2, null, message));
              return;
            } else if (cat2Time.getTime()
                + getSubjectiveDuration() + getChangetime() > cat1Time.getTime()) {
              final String message = String.format("Team %d has doesn't have enough time between %s and %s (need %d minutes)",
                                                   ti.getTeamNumber(), category1.getName(), category2.getName(),
                                                   getChangetimeAsMinutes());
              violations.add(new ConstraintViolation(true, ti.getTeamNumber(), category1, category2, null, message));
              return;
            }
          }
        }
      }
    }

    // constraint: team:3
    final long changetime;
    final int round1OpponentRound = findOpponentRound(ti, 0);
    final int round2OpponentRound = findOpponentRound(ti, 1);
    if (round1OpponentRound != 0
        || round2OpponentRound != 1) {
      changetime = getSpecialPerformanceChangetime();
    } else {
      changetime = getPerformanceChangetime();
    }
    if (ti.getPerf(0).getTime()
        + getPerformanceDuration() > ti.getPerf(1).getTime()) {
      final String message = String.format("Team %d is still in performance %d when they are to start performance %d: %s - %s",
                                           ti.getTeamNumber(), 1, 2, OUTPUT_DATE_FORMAT.get().format(ti.getPerf(0)),
                                           OUTPUT_DATE_FORMAT.get().format(ti.getPerf(1)));
      violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, null, ti.getPerf(1), message));
    } else if (ti.getPerf(0).getTime()
        + getPerformanceDuration() + changetime > ti.getPerf(1).getTime()) {
      final String message = String.format("Team %d doesn't have enough time (%d minutes) between performance 1 and performance 2: %s - %s",
                                           ti.getTeamNumber(), changetime
                                               / 1000 / Utilities.SECONDS_PER_MINUTE,
                                           OUTPUT_DATE_FORMAT.get().format(ti.getPerf(0)),
                                           OUTPUT_DATE_FORMAT.get().format(ti.getPerf(1)));
      violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, null, ti.getPerf(1), message));
    }

    if (ti.getPerf(1).getTime()
        + getPerformanceDuration() > ti.getPerf(2).getTime()) {
      final String message = String.format("Team %d is still in performance %d when they are to start performance %d: %s - %s",
                                           ti.getTeamNumber(), 2, 3, OUTPUT_DATE_FORMAT.get().format(ti.getPerf(0)),
                                           OUTPUT_DATE_FORMAT.get().format(ti.getPerf(1)));
      violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, null, ti.getPerf(2), message));
    } else if (ti.getPerf(1).getTime()
        + getPerformanceDuration() + getPerformanceChangetime() > ti.getPerf(2).getTime()) {
      final String message = String.format("Team %d doesn't have enough time (%d minutes) between performance 2 and performance 3: %s - %s",
                                           ti.getTeamNumber(), changetime
                                               / 1000 / Utilities.SECONDS_PER_MINUTE,
                                           OUTPUT_DATE_FORMAT.get().format(ti.getPerf(1)),
                                           OUTPUT_DATE_FORMAT.get().format(ti.getPerf(2)));
      violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, null, ti.getPerf(2), message));
    }

    // constraint: team:4
    for (int round = 0; round < getNumberOfRounds(); ++round) {
      final String performanceName = String.valueOf(round + 1);
      verifyPerformanceVsSubjective(violations, ti, performanceName, ti.getPerf(round));
    }

    // constraint: team:5
    for (int round = 0; round < getNumberOfRounds(); ++round) {
      final TeamScheduleInfo opponent = findOpponent(ti, round);
      if (null != opponent) {
        if (!Functions.safeEquals(ti.getDivision(), opponent.getDivision())) {
          final String divMessage = String.format("Team %d in division %s is competing against team %d from division %s round %d",
                                                  ti.getTeamNumber(), ti.getDivision(), opponent.getTeamNumber(),
                                                  opponent.getDivision(), (round + 1));
          violations.add(new ConstraintViolation(false, ti.getTeamNumber(), null, null, ti.getPerf(round), divMessage));
        }

        int opponentSide = -1;
        // figure out which round matches up
        for (int oround = 0; oround < getNumberOfRounds(); ++oround) {
          if (opponent.getPerf(oround).equals(ti.getPerf(round))) {
            opponentSide = opponent.getPerfTableSide(oround);
            break;
          }
        }
        if (-1 == opponentSide) {
          final String message = String.format("Unable to find time match for rounds between team %d and team %d at time %s",
                                               ti.getTeamNumber(), opponent.getTeamNumber(),
                                               OUTPUT_DATE_FORMAT.get().format(ti.getPerf(round)));
          violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, null, ti.getPerf(round), message));
        } else {
          if (opponentSide == ti.getPerfTableSide(round)) {
            final String message = String.format("Team %d and team %d are both on table %s side %d at the same time for round %d",
                                                 ti.getTeamNumber(), opponent.getTeamNumber(),
                                                 ti.getPerfTableColor(round), ti.getPerfTableSide(round), (round + 1));
            violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, null, ti.getPerf(round), message));
          }
        }

        for (int r = round + 1; r < getNumberOfRounds(); ++r) {
          final TeamScheduleInfo otherOpponent = findOpponent(ti, r);
          if (otherOpponent != null
              && opponent.equals(otherOpponent)) {
            final String message = String.format("Team %d competes against %d more than once rounds: %d, %d",
                                                 ti.getTeamNumber(), opponent.getTeamNumber(), (round + 1), (r + 1));
            violations.add(new ConstraintViolation(false, ti.getTeamNumber(), null, null, null, message));
          }
        }
      } else {
        // only a problem if this is not the last round and we don't have an odd
        // number of teams
        if (!(round == getNumberOfRounds() - 1 && (_schedule.size() % 2) == 1)) {
          final String message = String.format("Team %d has no opponent for round %d", ti.getTeamNumber(), (round + 1));
          violations.add(new ConstraintViolation(false, ti.getTeamNumber(), null, null, ti.getPerf(round), message));
        }
      }
    }

    // check if the team needs to stay for any extra founds
    final String performanceName = "extra";
    for (int round = 0; round < getNumberOfRounds(); ++round) {
      final TeamScheduleInfo next = checkIfTeamNeedsToStay(ti, round);
      if (null != next) {

        // check for competing against a team twice with this extra run
        for (int r = 0; r < getNumberOfRounds(); ++r) {
          final TeamScheduleInfo otherOpponent = findOpponent(ti, r);
          if (otherOpponent != null
              && next.equals(otherOpponent)) {
            final String message = String.format("Team %d competes against %d more than once rounds: %d, extra",
                                                 ti.getTeamNumber(), next.getTeamNumber(), (r + 1));
            violations.add(new ConstraintViolation(false, ti.getTeamNumber(), null, null, null, message));
            violations.add(new ConstraintViolation(false, next.getTeamNumber(), null, null, null, message));
          }
        }

        // everything else checked out, only only need to check the end time
        // against subjective and the next round
        final Date performanceTime = next.getPerf(round);
        verifyPerformanceVsSubjective(violations, ti, performanceName, performanceTime);

        if (round + 1 < getNumberOfRounds()) {
          if (next.getPerf(round).getTime()
              + getPerformanceDuration() > ti.getPerf(round + 1).getTime()) {
            final String message = String.format("Team %d will be in performance round %d when it is starting the extra performance round: %s - %s",
                                                 ti.getTeamNumber(), round,
                                                 OUTPUT_DATE_FORMAT.get().format(next.getPerf(round)),
                                                 OUTPUT_DATE_FORMAT.get().format(ti.getPerf(round + 1)));
            violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, null, ti.getPerf(round + 1), message));
          } else if (next.getPerf(round).getTime()
              + getPerformanceDuration() + getPerformanceChangetime() > ti.getPerf(round + 1).getTime()) {
            final String message = String.format("Team %d doesn't have enough time (%d minutes) between performance %d and performance extra: %s - %s",
                                                 ti.getTeamNumber(), changetime
                                                     / 1000 / Utilities.SECONDS_PER_MINUTE, round,
                                                 OUTPUT_DATE_FORMAT.get().format(next.getPerf(round)),
                                                 OUTPUT_DATE_FORMAT.get().format(ti.getPerf(round + 1)));
            violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, null, ti.getPerf(round + 1), message));
          }
        }

      }
    }
  }

  private void verifyPerformanceVsSubjective(final Collection<ConstraintViolation> violations,
                                             final TeamScheduleInfo ti,
                                             final String performanceName,
                                             final Date performanceTime) {
    for (final SubjectiveTime subj : ti.getSubjectiveTimes()) {
      final Date time = subj.getTime();
      if (time.before(performanceTime)) {
        if (time.getTime()
            + getSubjectiveDuration() > performanceTime.getTime()) {
          final String message = String.format("Team %d will be in %s when performance round %s starts",
                                               ti.getTeamNumber(), subj.getName(), performanceName);
          violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, subj, performanceTime, message));
        } else if (time.getTime()
            + getSubjectiveDuration() + getChangetime() > performanceTime.getTime()) {
          final String message = String.format("Team %d has doesn't have enough time between %s and performance round %s (need %d minutes)",
                                               ti.getTeamNumber(), OUTPUT_DATE_FORMAT.get().format(subj.getTime()),
                                               performanceName, getChangetimeAsMinutes());
          violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, subj, performanceTime, message));
        }
      } else {
        if (performanceTime.getTime()
            + getPerformanceDuration() > time.getTime()) {
          final String message = String.format("Team %d wil be in %s when performance round %s starts",
                                               ti.getTeamNumber(), subj.getName(), performanceName);
          violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, subj, performanceTime, message));
        } else if (performanceTime.getTime()
            + getPerformanceDuration() + getChangetime() > time.getTime()) {
          final String message = String.format("Team %d has doesn't have enough time between %s and performance round %s (need %d minutes)",
                                               ti.getTeamNumber(), subj.getName(), performanceName,
                                               getChangetimeAsMinutes());
          violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, subj, performanceTime, message));
        }
      }
    }
  }

  /**
   * @return the schedule info or null if the last line is read
   * @throws ScheduleParseException if there is an error with the schedule being
   *           read
   */
  private TeamScheduleInfo parseLine(final CellFileReader reader,
                                     final ColumnInformation ci) throws IOException, ParseException,
      ScheduleParseException {
    final String[] line = reader.readNext();
    if (null == line) {
      return null;
    }

    try {

      if (ci.getTeamNumColumn() >= line.length) {
        return null;
      }
      final String teamNumberStr = line[ci.getTeamNumColumn()];
      if (teamNumberStr.length() < 1) {
        // hit empty row
        return null;
      }

      final int teamNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(teamNumberStr).intValue();
      final TeamScheduleInfo ti = new TeamScheduleInfo(getNumberOfRounds(), teamNumber);
      ti.setTeamName(line[ci.getTeamNameColumn()]);
      ti.setOrganization(line[ci.getOrganizationColumn()]);
      ti.setDivision(line[ci.getDivisionColumn()]);

      for (final Map.Entry<Integer, String> entry : ci.getSubjectiveColumnInfo().entrySet()) {
        final String station = entry.getValue();
        final int column = entry.getKey();
        final String str = line[column];
        if ("".equals(str)) {
          // If we got an empty string, then we must have hit the end
          return null;
        }
        final Date date = parseDate(str);
        ti.addSubjectiveTime(new SubjectiveTime(station, date));
      }

      ti.setJudgingStation(line[ci.getJudgeGroupColumn()]);

      for (int perfNum = 0; perfNum < getNumberOfRounds(); ++perfNum) {
        final String perf1Str = line[ci.getPerfColumn(perfNum)];
        if ("".equals(perf1Str)) {
          // If we got an empty string, then we must have hit the end
          return null;
        }
        ti.setPerf(perfNum, parseDate(perf1Str));
        String table = line[ci.getPerfTableColumn(perfNum)];
        String[] tablePieces = table.split(" ");
        if (tablePieces.length != 2) {
          throw new RuntimeException("Error parsing table information from: "
              + table);
        }
        ti.setPerfTableColor(perfNum, tablePieces[0]);
        ti.setPerfTableSide(perfNum, Utilities.NUMBER_FORMAT_INSTANCE.parse(tablePieces[1]).intValue());
        if (ti.getPerfTableSide(perfNum) > 2
            || ti.getPerfTableSide(perfNum) < 1) {
          final String message = "There are only two sides to the table, number must be 1 or 2 team: "
              + ti.getTeamNumber() + " round " + (perfNum + 1);
          LOGGER.error(message);
          throw new ScheduleParseException(message);
        }
      }

      return ti;
    } catch (final ParseException pe) {
      LOGGER.error("Error parsing line: "
          + Arrays.toString(line), pe);
      throw pe;
    }
  }

  /**
   * Check for AM/PM flag and then pick the right parser.
   * 
   * @throws ParseException if the date cannot be parsed
   */
  private static Date parseDate(final String s) throws ParseException {
    if (s.indexOf("AM") >= 0
        || s.indexOf("PM") >= 0) {
      if (s.split(":").length > 2) {
        return DATE_FORMAT_AM_PM_SS.get().parse(s);
      } else {
        return DATE_FORMAT_AM_PM.get().parse(s);
      }
    } else {
      if (s.split(":").length > 2) {
        return DATE_FORMAT_SS.get().parse(s);
      } else {
        return OUTPUT_DATE_FORMAT.get().parse(s);
      }
    }
  }

  /**
   * Time to allocate for a performance run.
   * 
   * @return the performanceDuration (milliseconds)
   */
  public long getPerformanceDuration() {
    return performanceDuration;
  }

  /**
   * Time to allocate for either subjective judging.
   * 
   * @return the subjectiveDuration (milliseconds)
   */
  public long getSubjectiveDuration() {
    return subjectiveDuration;
  }

  /**
   * This is the time required between events.
   * 
   * @return the changetime (milliseconds)
   */
  public long getChangetime() {
    return changetime;
  }

  public long getChangetimeAsMinutes() {
    return getChangetime()
        / Utilities.SECONDS_PER_MINUTE / Utilities.MILLISECONDS_PER_SECOND;
  }

  /**
   * This is the time required between performance runs for each team.
   * 
   * @return the performanceChangetime (milliseconds)
   */
  public long getPerformanceChangetime() {
    return performanceChangetime;
  }

  /**
   * This is the time required between performance runs for the two teams in
   * involved in the performance run that crosses round 1 and round 2 when there
   * is an odd number of teams.
   * 
   * @return the specialPerformanceChangetime (milliseconds)
   */
  public long getSpecialPerformanceChangetime() {
    return specialPerformanceChangetime;
  }

  /**
   * @param specialPerformanceChangetime the specialPerformanceChangetime to set
   */
  public void setSpecialPerformanceChangetime(final long specialPerformanceChangetime) {
    this.specialPerformanceChangetime = specialPerformanceChangetime;
  }

  /**
   * @param performanceChangetime the performanceChangetime to set
   */
  public void setPerformanceChangetime(final long performanceChangetime) {
    this.performanceChangetime = performanceChangetime;
  }

  /**
   * @param changetime the changetime to set
   */
  public void setChangetime(final long changetime) {
    this.changetime = changetime;
  }

  /**
   * @param subjectiveDuration the subjectiveDuration to set
   */
  public void setSubjectiveDuration(final long subjectiveDuration) {
    this.subjectiveDuration = subjectiveDuration;
  }

  /**
   * @param performanceDuration the performanceDuration to set
   */
  public void setPerformanceDuration(final long performanceDuration) {
    this.performanceDuration = performanceDuration;
  }

  /**
   * Check if the schedule exists in the database.
   * 
   * @param connection database connection
   * @param tournamentID ID of the tournament to look for
   * @return if a schedule exists in the database for the specified tournament
   * @throws SQLException
   */
  public static boolean scheduleExistsInDatabase(final Connection connection,
                                                 final int tournamentID) throws SQLException {
    ResultSet rs = null;
    PreparedStatement prep = null;
    try {
      prep = connection.prepareStatement("SELECT COUNT(team_number) FROM schedule where tournament = ?");
      prep.setInt(1, tournamentID);
      rs = prep.executeQuery();
      if (rs.next()) {
        return rs.getInt(1) > 0;
      } else {
        return false;
      }
    } finally {
      SQLFunctions.close(rs);
      rs = null;
      SQLFunctions.close(prep);
      prep = null;
    }
  }

  /**
   * Store a tournament schedule in the database. This will delete any previous
   * schedule for the same tournament.
   * 
   * @param tournamentID the ID of the tournament
   */
  public void storeSchedule(final Connection connection,
                            final int tournamentID) throws SQLException {
    PreparedStatement deletePerfRounds = null;
    PreparedStatement deleteSchedule = null;
    PreparedStatement deleteSubjective = null;
    PreparedStatement insertSchedule = null;
    PreparedStatement insertPerfRounds = null;
    PreparedStatement insertSubjective = null;
    try {
      // delete previous tournament schedule
      deletePerfRounds = connection.prepareStatement("DELETE FROM sched_perf_rounds WHERE tournament = ?");
      deletePerfRounds.setInt(1, tournamentID);
      deletePerfRounds.executeUpdate();

      deleteSubjective = connection.prepareStatement("DELETE FROM sched_subjective WHERE tournament = ?");
      deleteSubjective.setInt(1, tournamentID);
      deleteSubjective.executeUpdate();

      deleteSchedule = connection.prepareStatement("DELETE FROM schedule WHERE tournament = ?");
      deleteSchedule.setInt(1, tournamentID);
      deleteSchedule.executeUpdate();

      // insert new tournament schedule
      insertSchedule = connection.prepareStatement("INSERT INTO schedule"//
          + " (tournament, team_number, judging_station)"//
          + " VALUES(?, ?, ?)");
      insertSchedule.setInt(1, tournamentID);

      insertPerfRounds = connection.prepareStatement("INSERT INTO sched_perf_rounds"//
          + " (tournament, team_number, round, perf_time, table_color, table_side)"//
          + " VALUES(?, ?, ?, ?, ?, ?)");
      insertPerfRounds.setInt(1, tournamentID);

      insertSubjective = connection.prepareStatement("INSERT INTO sched_subjective" //
          + " (tournament, team_number, name, subj_time)" //
          + " VALUES(?, ?, ?, ?)");
      insertSubjective.setInt(1, tournamentID);

      for (final TeamScheduleInfo si : getSchedule()) {
        insertSchedule.setInt(2, si.getTeamNumber());
        insertSchedule.setString(3, si.getJudgingStation());
        insertSchedule.executeUpdate();

        insertPerfRounds.setInt(2, si.getTeamNumber());
        for (int round = 0; round < si.getNumberOfRounds(); ++round) {
          insertPerfRounds.setInt(3, round);
          insertPerfRounds.setTime(4, Queries.dateToTime(si.getPerf(round)));
          insertPerfRounds.setString(5, si.getPerfTableColor(round));
          insertPerfRounds.setInt(6, si.getPerfTableSide(round));
          insertPerfRounds.executeUpdate();
        }

        for (final TeamScheduleInfo.SubjectiveTime subjectiveTime : si.getSubjectiveTimes()) {
          insertSubjective.setInt(2, si.getTeamNumber());
          insertSubjective.setString(3, subjectiveTime.getName());
          insertSubjective.setTime(4, Queries.dateToTime(subjectiveTime.getTime()));
          insertSubjective.executeUpdate();
        }
      }

    } finally {
      SQLFunctions.close(deletePerfRounds);
      deletePerfRounds = null;
      SQLFunctions.close(deleteSchedule);
      deleteSchedule = null;
      SQLFunctions.close(deleteSchedule);
      deleteSchedule = null;
      SQLFunctions.close(insertSchedule);
      insertSchedule = null;
      SQLFunctions.close(insertPerfRounds);
      insertPerfRounds = null;
      SQLFunctions.close(insertSubjective);
      insertSubjective = null;
    }
  }

  /**
   * Check if the current schedule is consistent with the specified tournament
   * in the database.
   * 
   * @param connection the database connection
   * @param tournamentID the tournament to check
   * @return the constraint violations, empty if no violations
   * @throws SQLException
   */
  public Collection<ConstraintViolation> compareWithDatabase(final Connection connection,
                                                             final int tournamentID) throws SQLException {
    final Collection<ConstraintViolation> violations = new LinkedList<ConstraintViolation>();
    final Map<Integer, Team> dbTeams = Queries.getTournamentTeams(connection, tournamentID);
    final Set<Integer> scheduleTeamNumbers = new HashSet<Integer>();
    for (final TeamScheduleInfo si : _schedule) {
      scheduleTeamNumbers.add(si.getTeamNumber());
      if (!dbTeams.containsKey(si.getTeamNumber())) {
        violations.add(new ConstraintViolation(true, si.getTeamNumber(), null, null, null, "Team "
            + si.getTeamNumber() + " is in schedule, but not in database"));
      }
    }
    for (final Integer dbNum : dbTeams.keySet()) {
      if (!scheduleTeamNumbers.contains(dbNum)) {
        violations.add(new ConstraintViolation(true, dbNum, null, null, null, "Team "
            + dbNum + " is in database, but not in schedule"));
      }
    }

    return violations;
  }

  /**
   * Keep track of column information from a spreadsheet.
   */
  public static final class ColumnInformation {

    private final List<String> headerLine;

    /**
     * The columns that were parsed into the headers in the same order that they
     * appear in the schedule.
     */
    public List<String> getHeaderLine() {
      return headerLine;
    }

    private final int teamNumColumn;

    public int getTeamNumColumn() {
      return teamNumColumn;
    }

    private final int organizationColumn;

    public int getOrganizationColumn() {
      return organizationColumn;
    }

    private final int teamNameColumn;

    public int getTeamNameColumn() {
      return teamNameColumn;
    }

    private final int divisionColumn;

    public int getDivisionColumn() {
      return divisionColumn;
    }

    private final Map<Integer, String> subjectiveColumns;

    public Map<Integer, String> getSubjectiveColumnInfo() {
      return subjectiveColumns;
    }

    private final int judgeGroupColumn;

    public int getJudgeGroupColumn() {
      return judgeGroupColumn;
    }

    private final int[] perfColumn;

    public int getNumPerfs() {
      return perfColumn.length;
    }

    public int getPerfColumn(final int round) {
      return perfColumn[round];
    }

    private final int[] perfTableColumn;

    public int getPerfTableColumn(final int round) {
      return perfTableColumn[round];
    }

    public ColumnInformation(final String[] headerLine,
                             final int teamNumColumn,
                             final int organizationColumn,
                             final int teamNameColumn,
                             final int divisionColumn,
                             final Map<Integer, String> subjectiveColumns,
                             final int judgeGroupColumn,
                             final int[] perfColumn,
                             final int[] perfTableColumn) {
      this.headerLine = Collections.unmodifiableList(Arrays.asList(headerLine));
      this.teamNumColumn = teamNumColumn;
      this.organizationColumn = organizationColumn;
      this.teamNameColumn = teamNameColumn;
      this.divisionColumn = divisionColumn;
      this.subjectiveColumns = subjectiveColumns;
      this.judgeGroupColumn = judgeGroupColumn;
      this.perfColumn = perfColumn;
      this.perfTableColumn = perfTableColumn;

      // determine which columns aren't used
      final List<String> unused = new LinkedList<String>();
      for (int column = 0; column < this.headerLine.size(); ++column) {
        boolean match = false;
        if (column == this.teamNumColumn //
            || column == this.organizationColumn //
            || column == this.teamNameColumn //
            || column == this.divisionColumn //
            || column == this.judgeGroupColumn //
        ) {
          match = true;
        }
        for (final int pc : this.perfColumn) {
          if (pc == column) {
            match = true;
          }
        }
        for (final int ptc : this.perfTableColumn) {
          if (ptc == column) {
            match = true;
          }
        }
        for (final int sc : this.subjectiveColumns.keySet()) {
          if (sc == column) {
            match = true;
          }
        }
        if (!match) {
          unused.add(this.headerLine.get(column));
        }
      }
      this.unusedColumns = Collections.unmodifiableList(unused);
    }

    private final List<String> unusedColumns;

    /**
     * @return the column names that were in the header line, but not used.
     */
    public List<String> getUnusedColumns() {
      return unusedColumns;
    }
  }
}
