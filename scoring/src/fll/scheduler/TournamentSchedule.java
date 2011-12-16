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
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

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
import fll.util.CSVCellReader;
import fll.util.CellFileReader;
import fll.util.ExcelCellReader;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;

/**
 * Tournament schedule. Can parse the schedule from a spreadsheet or CSV file.
 * When using the schedule from inside the web application, note that the team
 * information isn't updated when the main database is modified, so you can end
 * up with teams in the schedule that are no longer in the tournament and you
 * can end up with teams that are in the tournament, but not in the schedule.
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
  public static final String PERF_HEADER_FORMAT = BASE_PERF_HEADER
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

  private final HashMap<Date, Map<String, List<TeamScheduleInfo>>> _matches = new HashMap<Date, Map<String, List<TeamScheduleInfo>>>();

  public Map<Date, Map<String, List<TeamScheduleInfo>>> getMatches() {
    // TODO should make read-only somehow
    return _matches;
  }

  private final HashSet<String> _tableColors = new HashSet<String>();

  public Set<String> getTableColors() {
    return Collections.unmodifiableSet(_tableColors);
  }

  private final HashSet<String> _divisions = new HashSet<String>();

  public Set<String> getDivisions() {
    return Collections.unmodifiableSet(_divisions);
  }

  private final HashSet<String> _judges = new HashSet<String>();

  public Set<String> getJudges() {
    return Collections.unmodifiableSet(_judges);
  }

  private final LinkedList<TeamScheduleInfo> _schedule = new LinkedList<TeamScheduleInfo>();

  private final HashSet<String> subjectiveStations = new HashSet<String>();

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
          ti.addSubjectiveTime(new SubjectiveTime(name, subjTime));
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
          final PerformanceTime performance = new PerformanceTime(round, Queries.timeToDate(perfTime), tableColor,
                                                                  tableSide);
          ti.setPerf(round, performance);

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
      if (null != line[i]
          && line[i].startsWith(BASE_PERF_HEADER) && line[i].length() > BASE_PERF_HEADER.length()) {
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
          && side == si.getPerfTableSide(round) && si.getPerfTime(round).after(time)) {
        if (retval == null) {
          retval = si;
        } else if (null != si.getPerfTime(round)
            && si.getPerfTime(round).before(retval.getPerfTime(round))) {
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
   * @param si the TeamScheduleInfo for the team
   * @param round the round the team is competing at (zero based index)
   * @return the team one needs to compete against in an extra round or null if
   *         the team does not need to stay
   */
  public TeamScheduleInfo checkIfTeamNeedsToStay(final TeamScheduleInfo si,
                                                 final int round) {
    final int otherSide = 2 == si.getPerfTableSide(round) ? 1 : 2;
    final TeamScheduleInfo next = findNextTeam(si.getPerfTime(round), si.getPerfTableColor(round), otherSide, round);

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
  public void outputDetailedSchedules(final SchedParams params,
                                      final OutputStream output) throws DocumentException, IOException {
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
      outputTeamSchedule(params, detailedSchedules, si);
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
  private void outputTeamSchedule(final SchedParams params,
                                  final Document detailedSchedules,
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
      cal.add(Calendar.MINUTE, params.getStationByName(subjectiveStation).getDurationMinutes());
      final Date end = cal.getTime();
      para.add(new Chunk(String.format("%s - %s", OUTPUT_DATE_FORMAT.get().format(start),
                                       OUTPUT_DATE_FORMAT.get().format(end)), TEAM_VALUE_FONT));
      para.add(Chunk.NEWLINE);
    }

    for (int round = 0; round < getNumberOfRounds(); ++round) {
      para.add(new Chunk(String.format(PERF_HEADER_FORMAT, round + 1)
          + ": ", TEAM_HEADER_FONT));
      final Date start = si.getPerfTime(round);
      cal.setTime(start);
      cal.add(Calendar.MINUTE, params.getPerformanceMinutes());
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
    final SortedMap<PerformanceTime, TeamScheduleInfo> performanceTimes = new TreeMap<PerformanceTime, TeamScheduleInfo>();
    for (int round = 0; round < getNumberOfRounds(); ++round) {
      for (final TeamScheduleInfo si : _schedule) {
        performanceTimes.put(si.getPerf(round), si);
      }
    }

    // list of teams staying around to even up the teams
    final List<TeamScheduleInfo> teamsStaying = new LinkedList<TeamScheduleInfo>();

    final PdfPTable table = createTable(7);
    table.setWidths(new float[] { 2, 1, 3, 3, 2, 2, 2 });

    final PdfPCell tournamentCell = createHeaderCell("Tournament: "
        + name + " Performance");
    tournamentCell.setColspan(7);
    table.addCell(tournamentCell);

    table.addCell(createHeaderCell(TEAM_NUMBER_HEADER));
    table.addCell(createHeaderCell(DIVISION_HEADER));
    table.addCell(createHeaderCell("School or Organization"));
    table.addCell(createHeaderCell("Team Name"));
    table.addCell(createHeaderCell("Time"));
    table.addCell(createHeaderCell("Table"));
    table.addCell(createHeaderCell("Round"));
    table.setHeaderRows(1);

    for (final Map.Entry<PerformanceTime, TeamScheduleInfo> entry : performanceTimes.entrySet()) {
      final PerformanceTime performance = entry.getKey();
      final TeamScheduleInfo si = entry.getValue();

      // check if team needs to stay and color the cell magenta if they do
      final BaseColor backgroundColor;
      if (null != checkIfTeamNeedsToStay(si, performance.getRound())) {
        teamsStaying.add(si);
        backgroundColor = BaseColor.MAGENTA;
      } else {
        backgroundColor = null;
      }

      table.addCell(createCell(String.valueOf(si.getTeamNumber())));
      table.addCell(createCell(si.getDivision()));
      table.addCell(createCell(si.getOrganization()));
      table.addCell(createCell(si.getTeamName()));
      table.addCell(createCell(OUTPUT_DATE_FORMAT.get().format(si.getPerfTime(performance.getRound())), backgroundColor));
      table.addCell(createCell(si.getPerfTableColor(performance.getRound())
          + " " + si.getPerfTableSide(performance.getRound()), backgroundColor));
      table.addCell(createCell(String.valueOf(performance.getRound() + 1)));
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
    return new SubjectiveComparator(name);
  }

  /**
   * Comparator for for sorting by the specified subjective station.
   */
  private static class SubjectiveComparator implements Comparator<TeamScheduleInfo>, Serializable {
    private final String name;

    public SubjectiveComparator(final String name) {
      this.name = name;
    }

    public int compare(final TeamScheduleInfo one,
                       final TeamScheduleInfo two) {

      if (!one.getDivision().equals(two.getDivision())) {
        return one.getDivision().compareTo(two.getDivision());
      } else if (!one.getJudgingStation().equals(two.getJudgingStation())) {
        return one.getJudgingStation().compareTo(two.getJudgingStation());
      } else {
        final SubjectiveTime oneTime = one.getSubjectiveTimeByName(name);
        final SubjectiveTime twoTime = two.getSubjectiveTimeByName(name);

        if (oneTime == null) {
          if (twoTime == null) {
            return 0;
          } else {
            return -1;
          }
        } else {
          if (null == twoTime) {
            return 1;
          } else {
            return oneTime.getTime().compareTo(twoTime.getTime());
          }
        }
      }
    }
  }

  /**
   * Compute the general schedule and return it as a string
   */
  public String computeGeneralSchedule() {
    Date minPerf = null;
    Date maxPerf = null;
    // category -> division -> date
    final Map<String, Map<String, Date>> minSubjectiveTimes = new HashMap<String, Map<String, Date>>();
    final Map<String, Map<String, Date>> maxSubjectiveTimes = new HashMap<String, Map<String, Date>>();

    for (final TeamScheduleInfo si : _schedule) {
      final String division = si.getDivision();
      for (final SubjectiveTime stime : si.getSubjectiveTimes()) {
        final Map<String, Date> minSubTimes;
        if (minSubjectiveTimes.containsKey(stime.getName())) {
          minSubTimes = minSubjectiveTimes.get(stime.getName());
        } else {
          minSubTimes = new HashMap<String, Date>();
          minSubjectiveTimes.put(stime.getName(), minSubTimes);
        }
        final Map<String, Date> maxSubTimes;
        if (maxSubjectiveTimes.containsKey(stime.getName())) {
          maxSubTimes = maxSubjectiveTimes.get(stime.getName());
        } else {
          maxSubTimes = new HashMap<String, Date>();
          maxSubjectiveTimes.put(stime.getName(), maxSubTimes);
        }

        final Date currentMin = minSubTimes.get(division);
        if (null == currentMin) {
          minSubTimes.put(division, stime.getTime());
        } else {
          if (stime.getTime().before(currentMin)) {
            minSubTimes.put(division, stime.getTime());
          }
        }
        final Date currentMax = maxSubTimes.get(division);
        if (null == currentMax) {
          maxSubTimes.put(division, stime.getTime());
        } else {
          if (stime.getTime().after(currentMax)) {
            maxSubTimes.put(division, stime.getTime());
          }
        }

      }

      for (int i = 0; i < getNumberOfRounds(); ++i) {
        if (null != si.getPerfTime(i)) {
          // ignore the teams that cross round boundaries
          final int opponentRound = findOpponentRound(si, i);
          if (opponentRound == i) {
            if (null == minPerf
                || si.getPerfTime(i).before(minPerf)) {
              minPerf = si.getPerfTime(i);
            }

            if (null == maxPerf
                || si.getPerfTime(i).after(maxPerf)) {
              maxPerf = si.getPerfTime(i);
            }
          }
        }
      }

    }

    // print out the general schedule
    final Formatter output = new Formatter();
    final Set<String> categories = new HashSet<String>();
    categories.addAll(minSubjectiveTimes.keySet());
    categories.addAll(maxSubjectiveTimes.keySet());
    for (final String category : categories) {
      final Map<String, Date> minTimes = minSubjectiveTimes.get(category);
      final Map<String, Date> maxTimes = maxSubjectiveTimes.get(category);
      output.format("%s%n", category);

      final Set<String> divisions = new HashSet<String>();
      divisions.addAll(minTimes.keySet());
      divisions.addAll(maxTimes.keySet());
      for (final String division : divisions) {
        output.format("    Earliest start for division %s: %s%n", division, OUTPUT_DATE_FORMAT.get().format(minTimes.get(division)));
        output.format("    Latest start for division %s: %s%n", division, OUTPUT_DATE_FORMAT.get().format(maxTimes.get(division)));
      }
    }
    output.format("Earliest performance start: %s%n", OUTPUT_DATE_FORMAT.get().format(minPerf));
    output.format("Latest performance start: %s%n", OUTPUT_DATE_FORMAT.get().format(maxPerf));
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
    if (_matches.containsKey(ti.getPerfTime(round))) {
      timeMatches = _matches.get(ti.getPerfTime(round));
    } else {
      timeMatches = new HashMap<String, List<TeamScheduleInfo>>();
      _matches.put(ti.getPerfTime(round), timeMatches);
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

  /**
   * Find the round of the opponent for a given team in a given round.
   * 
   * @param ti
   * @param round
   * @return the round number or -1 if no opponent
   */
  public int findOpponentRound(final TeamScheduleInfo ti,
                               final int round) {
    final List<TeamScheduleInfo> tableMatches = _matches.get(ti.getPerfTime(round)).get(ti.getPerfTableColor(round));
    if (tableMatches.size() > 1) {
      if (tableMatches.get(0).equals(ti)) {
        return tableMatches.get(1).findRoundFortime(ti.getPerfTime(round));
      } else {
        return tableMatches.get(0).findRoundFortime(ti.getPerfTime(round));
      }
    } else {
      return -1;
    }
  }

  /**
   * Find the opponent for a given team in a given round.
   * 
   * @param ti
   * @param round
   * @return the team number or null if no opponent
   */
  public TeamScheduleInfo findOpponent(final TeamScheduleInfo ti,
                                       final int round) {
    final List<TeamScheduleInfo> tableMatches = _matches.get(ti.getPerfTime(round)).get(ti.getPerfTableColor(round));
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
        String table = line[ci.getPerfTableColumn(perfNum)];
        String[] tablePieces = table.split(" ");
        if (tablePieces.length != 2) {
          throw new RuntimeException("Error parsing table information from: "
              + table);
        }
        final PerformanceTime performance = new PerformanceTime(perfNum, parseDate(perf1Str), tablePieces[0],
                                                                Utilities.NUMBER_FORMAT_INSTANCE.parse(tablePieces[1])
                                                                                                .intValue());
        ti.setPerf(perfNum, performance);
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
          insertPerfRounds.setTime(4, Queries.dateToTime(si.getPerfTime(round)));
          insertPerfRounds.setString(5, si.getPerfTableColor(round));
          insertPerfRounds.setInt(6, si.getPerfTableSide(round));
          insertPerfRounds.executeUpdate();
        }

        for (final SubjectiveTime subjectiveTime : si.getSubjectiveTimes()) {
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
      this.perfColumn = new int[perfColumn.length];
      System.arraycopy(perfColumn, 0, this.perfColumn, 0, perfColumn.length);
      this.perfTableColumn = new int[perfTableColumn.length];
      System.arraycopy(perfTableColumn, 0, this.perfTableColumn, 0, perfTableColumn.length);

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
