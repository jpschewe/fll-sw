/*
 * Copyright (c) 2000-2008 HighTechKids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.scheduler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nonnull;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;

import au.com.bytecode.opencsv.CSVWriter;
import fll.Team;
import fll.Tournament;
import fll.TournamentTeam;
import fll.Utilities;
import fll.db.Queries;
import fll.documents.elements.SheetElement;
import fll.documents.writers.SubjectivePdfWriter;
import fll.util.CSVCellReader;
import fll.util.CellFileReader;
import fll.util.ExcelCellReader;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.util.PdfUtils;
import fll.util.SimpleFooterHandler;
import fll.web.playoff.ScoresheetGenerator;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreCategory;
import fll.xml.XMLUtils;
import net.mtu.eggplant.util.sql.SQLFunctions;

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
   * How wide to make the line between time separations in the "by time" schedule
   * outputs.
   */
  private static final float TIME_SEPARATOR_LINE_WIDTH = 2f;

  /**
   * Header on team number column.
   */
  public static final String TEAM_NUMBER_HEADER = "Team #";

  public static final String TEAM_NAME_HEADER = "Team Name";

  public static final String ORGANIZATION_HEADER = "Organization";

  /**
   * Should use AWARD_GROUP_HEADER now, only kept for old schedules
   */
  @Deprecated
  private static final String DIVISION_HEADER = "Div";

  public static final String AWARD_GROUP_HEADER = "Awawrd Group";

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

  /**
   * XML format for time type.
   */
  private static final DateTimeFormatter XML_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

  /**
   * Always output without 24-hour time and without AM/PM.
   */
  private static final DateTimeFormatter OUTPUT_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm");

  /**
   * Parse times as 24-hour and then use
   * {@link TournamentSchedule#EARLIEST_HOUR} to decide if it's really
   * AM or PM.
   */
  private static final DateTimeFormatter INPUT_TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");

  /**
   * Any date with an hour before this needs to have 12 added to it as it must
   * be in the afternoon.
   */
  private static final int EARLIEST_HOUR = 7;

  /**
   * The format used inside Excel spreadsheets.
   */
  private static final DateTimeFormatter TIME_FORMAT_AM_PM_SS = DateTimeFormatter.ofPattern("hh:mm:ss a");

  /**
   * Parse a time from a schedule. This method allows only hours and minutes to
   * be
   * specified and still have times in the afternoon. If there is any time that
   * has an hour before {@link #EARLIEST_HOUR} the time is assumed to be in the
   * afternoon.
   * 
   * @param str a string representing a schedule time, if null the return value
   *          is null, if empty the return value is null
   * @return the {@link LocalTime} object for the string
   * @throws DateTimeParseException if the string could not be parsed as a time
   *           for a schedule
   */
  public static LocalTime parseTime(final String str) throws DateTimeParseException {
    if (null == str
        || str.trim().isEmpty()) {
      return null;
    }

    try {
      // first try with the generic parser
      final LocalTime time = LocalTime.parse(str);
      return time;
    } catch (final DateTimeParseException e) {
      // try with seconds and AM/PM
      try {
        final LocalTime time = LocalTime.parse(str, TIME_FORMAT_AM_PM_SS);
        return time;
      } catch (final DateTimeParseException ampme) {
        // then try with 24-hour clock
        final LocalTime time = LocalTime.parse(str, INPUT_TIME_FORMAT);
        if (time.getHour() < EARLIEST_HOUR) {
          // no time should be this early, it must be the afternoon.
          return time.plusHours(12);
        } else {
          return time;
        }
      }
    }
  }

  /**
   * Conver the time to a string that will be parsed by
   * {@link #parseTime(String)}.
   * 
   * @param time the time to format, may be null
   * @return the formatted time, null converts to ""
   */
  public static String formatTime(final LocalTime time) {
    if (null == time) {
      return "";
    } else {
      return time.format(OUTPUT_TIME_FORMAT);
    }
  }

  public static final ThreadLocal<DateFormat> DATE_FORMAT_AM_PM_SS = new ThreadLocal<DateFormat>() {
    @Override
    protected DateFormat initialValue() {
      return new SimpleDateFormat("hh:mm:ss a");
    }
  };

  // time->table->team
  private final HashMap<LocalTime, Map<String, List<TeamScheduleInfo>>> _matches = new HashMap<>();

  /* package */Map<LocalTime, Map<String, List<TeamScheduleInfo>>> getMatches() {
    // TODO should make read-only somehow
    return _matches;
  }

  private final HashSet<String> _tableColors = new HashSet<String>();

  public Set<String> getTableColors() {
    return Collections.unmodifiableSet(_tableColors);
  }

  private final HashSet<String> _awardGroups = new HashSet<String>();

  public Set<String> getAwardGroups() {
    return Collections.unmodifiableSet(_awardGroups);
  }

  private final HashSet<String> _judgingGroups = new HashSet<String>();

  public Set<String> getJudgingGroups() {
    return Collections.unmodifiableSet(_judgingGroups);
  }

  private final LinkedList<TeamScheduleInfo> _schedule = new LinkedList<TeamScheduleInfo>();

  private final HashSet<String> subjectiveStations = new HashSet<String>();

  /**
   * Name of this tournament.
   */
  private final String name;

  public String getName() {
    return name;
  }

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
   * Read the tournament schedule from a spreadsheet.
   * 
   * @param name the name of the tournament
   * @param stream how to access the spreadsheet
   * @param sheetName the name of the worksheet the data is on
   * @param subjectiveHeaders the headers for the subjective columns
   * @throws ScheduleParseException if there is an error parsing the schedule
   */
  public TournamentSchedule(final String name,
                            final InputStream stream,
                            final String sheetName,
                            final Collection<String> subjectiveHeaders)
      throws IOException, ParseException, InvalidFormatException, ScheduleParseException {
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
                            final Collection<String> subjectiveHeaders)
      throws IOException, ParseException, ScheduleParseException {
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
                             final Collection<String> subjectiveHeaders)
      throws IOException, ParseException, ScheduleParseException {
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
                            final int tournamentID)
      throws SQLException {
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
        this.numRounds = numRounds.getInt(1)
            + 1;
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
        ti.setJudgingGroup(judgingStation);

        getSubjective.setInt(2, teamNumber);
        subjective = getSubjective.executeQuery();
        while (subjective.next()) {
          final String name = subjective.getString(1);
          final Time subjTime = subjective.getTime(2);
          ti.addSubjectiveTime(new SubjectiveTime(name, subjTime.toLocalTime()));
        }

        getPerfRounds.setInt(2, teamNumber);
        perfRounds = getPerfRounds.executeQuery();
        int prevRound = -1;
        while (perfRounds.next()) {
          final int round = perfRounds.getInt(1);
          if (round != prevRound
              + 1) {
            throw new RuntimeException("Rounds must be consecutive and start at 1. Tournament: "
                + tournamentID
                + " team: "
                + teamNumber
                + " round: "
                + (round
                    + 1)
                + " prevRound: "
                + (prevRound
                    + 1));
          }
          final LocalTime perfTime = perfRounds.getTime(2).toLocalTime();
          final String tableColor = perfRounds.getString(3);
          final int tableSide = perfRounds.getInt(4);
          if (tableSide != 1
              && tableSide != 2) {
            throw new RuntimeException("Tables sides must be 1 or 2. Tournament: "
                + tournamentID
                + " team: "
                + teamNumber);
          }
          final PerformanceTime performance = new PerformanceTime(perfTime, tableColor, tableSide);
          ti.setPerf(round, performance);

          prevRound = round;
        }
        final String eventDivision = Queries.getEventDivision(connection, teamNumber, tournamentID);
        ti.setDivision(eventDivision);

        final Team team = Team.getTeamFromDatabase(connection, teamNumber);
        ti.setOrganization(team.getOrganization());
        ti.setTeamName(team.getTeamName());

        cacheTeamScheduleInformation(ti);
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
                                              final Collection<String> subjectiveHeaders)
      throws IOException {
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
          && line[i].startsWith(BASE_PERF_HEADER)
          && line[i].length() > BASE_PERF_HEADER.length()) {
        final String perfNumberStr = line[i].substring(BASE_PERF_HEADER.length());
        final Integer perfNumber = Integer.valueOf(perfNumberStr);
        if (!perfRounds.add(perfNumber)) {
          throw new FLLRuntimeException("Found performance rounds num "
              + perfNumber
              + " twice in the header: "
              + Arrays.asList(line));
        }
      }
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
            + (expectedValue
                - 1)
            + " found "
            + value);
      }

      final String tableHeader = String.format(TABLE_HEADER_FORMAT, expectedValue);
      if (!checkHeaderExists(line, tableHeader)) {
        throw new FLLRuntimeException("Couldn't find header for round "
            + expectedValue
            + ". Looking for header '"
            + tableHeader
            + "'");
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
   * Get the column number or throw {@link MissingColumnException} if the column
   * it
   * not found.
   */
  private static int getColumnForHeader(final String[] line,
                                        final String header)
      throws MissingColumnException {
    final Integer column = columnForHeader(line, header);
    if (null == column) {
      throw new MissingColumnException("Unable to find header '"
          + header
          + "' in "
          + Arrays.asList(line));
    } else {
      return column;
    }
  }

  private static ColumnInformation parseHeader(final Collection<String> subjectiveHeaders,
                                               final String[] line) {
    final int numPerfRounds = countNumRounds(line);

    final int[] perfColumn = new int[numPerfRounds];
    final int[] perfTableColumn = new int[numPerfRounds];

    final int teamNumColumn = getColumnForHeader(line, TEAM_NUMBER_HEADER);
    final int organizationColumn = getColumnForHeader(line, ORGANIZATION_HEADER);
    final int teamNameColumn = getColumnForHeader(line, TEAM_NAME_HEADER);

    int judgeGroupColumn = -1;
    try {
      judgeGroupColumn = getColumnForHeader(line, JUDGE_GROUP_HEADER);
    } catch (final MissingColumnException e) {
      judgeGroupColumn = -1;
    }

    int divisionColumn = -1;
    try {
      divisionColumn = getColumnForHeader(line, DIVISION_HEADER);
    } catch (final MissingColumnException e) {
      divisionColumn = -1;
    }
    if (-1 == divisionColumn) {
      try {
        divisionColumn = getColumnForHeader(line, AWARD_GROUP_HEADER);
      } catch (final MissingColumnException e) {
        divisionColumn = -1;
      }
    }

    // Need one of judge group column or division column
    if (-1 == judgeGroupColumn
        && -1 == divisionColumn) {
      throw new MissingColumnException("Must have judging station column or award group column");
    } else if (-1 == judgeGroupColumn) {
      judgeGroupColumn = divisionColumn;
    } else if (-1 == divisionColumn) {
      divisionColumn = judgeGroupColumn;
    }

    for (int round = 0; round < numPerfRounds; ++round) {
      final String perfHeader = String.format(PERF_HEADER_FORMAT, (round
          + 1));
      final String perfTableHeader = String.format(TABLE_HEADER_FORMAT, (round
          + 1));
      perfColumn[round] = getColumnForHeader(line, perfHeader);
      perfTableColumn[round] = getColumnForHeader(line, perfTableHeader);
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
                         final ColumnInformation ci)
      throws IOException, ParseException, ScheduleParseException {
    TeamScheduleInfo ti;
    while (null != (ti = parseLine(reader, ci))) {
      cacheTeamScheduleInformation(ti);
    }
  }

  /**
   * Add a team to the schedule. Check that the team isn't already in the
   * schedule.
   */
  private void addToSchedule(final TeamScheduleInfo ti) {
    if (!_schedule.contains(ti)) {
      _schedule.add(ti);
    } else {
      LOGGER.warn("Attempting to add the same team to the schedule twice: "
          + ti.getTeamNumber());
    }
  }

  /**
   * Populate internal caches with the data from this newly created
   * {@link TeamScheduleInfo}.
   */
  private void cacheTeamScheduleInformation(final TeamScheduleInfo ti) {
    addToSchedule(ti);

    // keep track of some meta information
    for (int round = 0; round < getNumberOfRounds(); ++round) {
      _tableColors.add(ti.getPerfTableColor(round));
      addToMatches(ti, round);
    }
    _awardGroups.add(ti.getAwardGroup());
    _judgingGroups.add(ti.getJudgingGroup());
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
  private TeamScheduleInfo findNextTeam(final LocalTime time,
                                        final String table,
                                        final int side,
                                        final int round) {
    TeamScheduleInfo retval = null;
    for (final TeamScheduleInfo si : _schedule) {
      if (table.equals(si.getPerfTableColor(round))
          && side == si.getPerfTableSide(round)
          && si.getPerfTime(round).isAfter(time)) {
        if (retval == null) {
          retval = si;
        } else if (null != si.getPerfTime(round)
            && si.getPerfTime(round).isBefore(retval.getPerfTime(round))) {
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
   * @param directory the directory to put the files in
   * @param baseFilename the base filename
   * @throws DocumentException
   * @throws IOException
   * @throws IllegalArgumentExcption if directory doesn't exist and can't be
   *           created or exists and isn't a directory
   */
  public void outputDetailedSchedules(final SchedParams params,
                                      final File directory,
                                      final String baseFilename)
      throws DocumentException, IOException {
    if (!directory.exists()) {
      if (!directory.mkdirs()) {
        throw new IllegalArgumentException("Unable to create "
            + directory.getAbsolutePath());
      }
    } else if (!directory.isDirectory()) {
      throw new IllegalArgumentException(directory.getAbsolutePath()
          + " exists, but isn't a directory");
    }

    OutputStream pdfFos = null;
    try {
      final File byDivision = new File(directory, baseFilename
          + "-subjective-by-division.pdf");

      pdfFos = new FileOutputStream(byDivision);
      outputSubjectiveSchedulesByJudgingStation(pdfFos);
      IOUtils.closeQuietly(pdfFos);
      pdfFos = null;

      final File byTime = new File(directory, baseFilename
          + "-subjective-by-time.pdf");
      pdfFos = new FileOutputStream(byTime);
      outputSubjectiveSchedulesByTime(pdfFos);
      IOUtils.closeQuietly(pdfFos);
      pdfFos = null;

      final File performance = new File(directory, baseFilename
          + "-performance.pdf");
      pdfFos = new FileOutputStream(performance);
      outputPerformanceScheduleByTime(pdfFos);
      IOUtils.closeQuietly(pdfFos);
      pdfFos = null;

      final File teamSchedules = new File(directory, baseFilename
          + "-team-schedules.pdf");
      pdfFos = new FileOutputStream(teamSchedules);
      outputTeamSchedules(params, pdfFos);
      IOUtils.closeQuietly(pdfFos);
      pdfFos = null;

    } finally {
      IOUtils.closeQuietly(pdfFos);
    }
  }

  /**
   * Output the schedule for each team.
   * 
   * @param params schedule parameters
   * @param pdfFos where to write the schedule
   * @throws DocumentException
   */
  public void outputTeamSchedules(final SchedParams params,
                                  final OutputStream pdfFos)
      throws DocumentException {
    Collections.sort(_schedule, ComparatorByTeam.INSTANCE);
    final Document teamDoc = PdfUtils.createPortraitPdfDoc(pdfFos, new SimpleFooterHandler());
    for (final TeamScheduleInfo si : _schedule) {
      outputTeamSchedule(params, teamDoc, si);
    }
    teamDoc.close();
  }

  /**
   * Output the performance schedule, sorted by time.
   * 
   * @param pdfFos where to write the schedule
   * @throws DocumentException
   */
  public void outputPerformanceScheduleByTime(final OutputStream pdfFos) throws DocumentException {
    final Document performanceDoc = PdfUtils.createPortraitPdfDoc(pdfFos, new SimpleFooterHandler());
    outputPerformanceSchedule(performanceDoc);
    performanceDoc.close();
  }

  /**
   * Output the subjective schedules with a table for each category and sorted
   * by time.
   * 
   * @param pdfFos where to write the schedule
   * @throws DocumentException
   */
  public void outputSubjectiveSchedulesByTime(final OutputStream pdfFos) throws DocumentException {
    final Document detailedSchedulesByTime = PdfUtils.createPortraitPdfDoc(pdfFos, new SimpleFooterHandler());
    for (final String subjectiveStation : subjectiveStations) {
      outputSubjectiveScheduleByTime(detailedSchedulesByTime, subjectiveStation);
      detailedSchedulesByTime.add(Chunk.NEXTPAGE);
    }
    detailedSchedulesByTime.close();
  }

  /**
   * Output the subjective schedules with a table for each category and sorted
   * by judging station, then by time.
   * 
   * @param pdfFos where to output the schedule
   * @throws DocumentException
   */
  public void outputSubjectiveSchedulesByJudgingStation(final OutputStream pdfFos) throws DocumentException {
    final Document detailedSchedulesByDivision = PdfUtils.createPortraitPdfDoc(pdfFos, new SimpleFooterHandler());
    for (final String subjectiveStation : subjectiveStations) {
      outputSubjectiveScheduleByDivision(detailedSchedulesByDivision, subjectiveStation);
      detailedSchedulesByDivision.add(Chunk.NEXTPAGE);
    }
    detailedSchedulesByDivision.close();
  }

  /**
   * Output the schedule sorted by team number. This schedule looks much like
   * the input spreadsheet.
   * 
   * @param stream where to write the schedule
   * @throws DocumentException
   */
  public void outputScheduleByTeam(final OutputStream stream) throws DocumentException {
    final Document pdf = PdfUtils.createLandscapePdfDoc(stream, new SimpleFooterHandler());

    final int numColumns = 5
        + subjectiveStations.size()
        + getNumberOfRounds()
            * 2;
    final PdfPTable table = PdfUtils.createTable(numColumns);
    final float[] columnWidths = new float[numColumns];
    int idx = 0;
    columnWidths[idx] = 2; // team number
    ++idx;
    columnWidths[idx] = 3; // team name
    ++idx;
    columnWidths[idx] = 3; // organization
    ++idx;
    columnWidths[idx] = 2; // judging group
    ++idx;
    columnWidths[idx] = 2; // division
    ++idx;
    for (int i = 0; i < subjectiveStations.size(); ++i) {
      columnWidths[idx] = 2; // time
      ++idx;
    }
    for (int i = 0; i < getNumberOfRounds(); ++i) {
      columnWidths[idx] = 2; // time
      ++idx;
      columnWidths[idx] = 2; // table
      ++idx;
    }
    table.setWidths(columnWidths);

    final PdfPCell tournamentCell = PdfUtils.createHeaderCell("Tournament: "
        + getName());
    tournamentCell.setColspan(numColumns);

    table.addCell(tournamentCell);

    table.addCell(PdfUtils.createHeaderCell(TEAM_NUMBER_HEADER));
    table.addCell(PdfUtils.createHeaderCell(TEAM_NAME_HEADER));
    table.addCell(PdfUtils.createHeaderCell(ORGANIZATION_HEADER));
    table.addCell(PdfUtils.createHeaderCell(JUDGE_GROUP_HEADER));
    table.addCell(PdfUtils.createHeaderCell(AWARD_GROUP_HEADER));
    for (final String subjectiveStation : subjectiveStations) {
      table.addCell(PdfUtils.createHeaderCell(subjectiveStation));
    }
    for (int round = 0; round < getNumberOfRounds(); ++round) {
      table.addCell(PdfUtils.createHeaderCell(String.format(PERF_HEADER_FORMAT, round
          + 1)));
      table.addCell(PdfUtils.createHeaderCell(String.format(TABLE_HEADER_FORMAT, round
          + 1)));
    }
    table.setHeaderRows(2);

    Collections.sort(_schedule, ComparatorByTeam.INSTANCE);
    for (final TeamScheduleInfo si : _schedule) {
      table.addCell(PdfUtils.createCell(String.valueOf(si.getTeamNumber())));
      table.addCell(PdfUtils.createCell(si.getTeamName()));
      table.addCell(PdfUtils.createCell(si.getOrganization()));
      table.addCell(PdfUtils.createCell(si.getJudgingGroup()));
      table.addCell(PdfUtils.createCell(si.getAwardGroup()));

      for (final String subjectiveStation : subjectiveStations) {
        table.addCell(PdfUtils.createCell(formatTime(si.getSubjectiveTimeByName(subjectiveStation).getTime())));
      }

      for (int round = 0; round < getNumberOfRounds(); ++round) {
        final PerformanceTime perf = si.getPerf(round);

        table.addCell(PdfUtils.createCell(formatTime(perf.getTime())));

        table.addCell(PdfUtils.createCell(String.format("%s %s", perf.getTable(), perf.getSide())));
      }

    }

    pdf.add(table);

    pdf.close();
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
                                  final TeamScheduleInfo si)
      throws DocumentException {
    final Paragraph para = new Paragraph();
    para.add(new Chunk(String.format("Detailed schedule for Team #%d - %s", si.getTeamNumber(), si.getTeamName()),
                       TEAM_TITLE_FONT));
    para.add(Chunk.NEWLINE);

    para.add(new Chunk(String.format("Organization: %s", si.getOrganization()), TEAM_TITLE_FONT));
    para.add(Chunk.NEWLINE);

    para.add(new Chunk("Division: ", TEAM_HEADER_FONT));
    para.add(new Chunk(si.getAwardGroup(), TEAM_VALUE_FONT));
    para.add(Chunk.NEWLINE);

    for (final String subjectiveStation : subjectiveStations) {
      para.add(new Chunk(subjectiveStation
          + ": ", TEAM_HEADER_FONT));
      final LocalTime start = si.getSubjectiveTimeByName(subjectiveStation).getTime();
      final LocalTime end = start.plus(params.getStationByName(subjectiveStation).getDuration());
      para.add(new Chunk(String.format("%s - %s", formatTime(start), formatTime(end)), TEAM_VALUE_FONT));
      para.add(Chunk.NEWLINE);
    }

    for (int round = 0; round < getNumberOfRounds(); ++round) {
      para.add(new Chunk(String.format(PERF_HEADER_FORMAT, round
          + 1)
          + ": ", TEAM_HEADER_FONT));
      final LocalTime start = si.getPerfTime(round);
      final LocalTime end = start.plus(Duration.ofMinutes(params.getPerformanceMinutes()));
      para.add(new Chunk(String.format("%s - %s %s %d", formatTime(start), formatTime(end), si.getPerfTableColor(round),
                                       si.getPerfTableSide(round)),
                         TEAM_VALUE_FONT));
      para.add(Chunk.NEWLINE);
    }

    para.add(Chunk.NEWLINE);
    para.add(new Chunk("Performance rounds must start on time, and will start without you. Please ensure your team arrives at least 5 minutes ahead of scheduled time, and checks in.",
                       TEAM_HEADER_FONT));

    para.add(Chunk.NEWLINE);
    para.add(new Chunk("Note that there may be more judging and a head to head round after this judging, please see the main tournament schedule for these details.",
                       TEAM_HEADER_FONT));
    para.add(Chunk.NEWLINE);
    para.add(Chunk.NEWLINE);
    para.add(Chunk.NEWLINE);

    para.setKeepTogether(true);
    detailedSchedules.add(para);
  }

  /**
   * @param dir where to write the files
   * @param baseFileName the base name of the files
   * @param description the challenge description
   * @param categoryToSchedule mapping of ScoreCategories to schedule columns
   * @param tournamentName the name of the tournament to display on the sheets
   * @throws DocumentException
   * @throws MalformedURLException
   * @throws IOException
   */
  public void outputSubjectiveSheets(@Nonnull final String tournamentName,
                                     final String dir,
                                     final String baseFileName,
                                     final ChallengeDescription description,
                                     final Map<ScoreCategory, String> categoryToSchedule)
      throws DocumentException, MalformedURLException, IOException {

    // setup the sheets from the sucked in xml
    for (final ScoreCategory category : description.getSubjectiveCategories()) {
      final SheetElement sheetElement = createSubjectiveSheetElement(category);

      final String filename = dir
          + File.separator
          + baseFileName
          + "_SubjectiveSheets-"
          + category.getName()
          + ".pdf";

      // sort the schedule by the category we're working with
      final String subjectiveStation = categoryToSchedule.get(category);
      if (null != subjectiveStation) {
        Collections.sort(_schedule, getComparatorForSubjectiveByDivision(subjectiveStation));
      }

      final ScoreCategory scoreCategory = sheetElement.getSheetData();
      final String schedulerColumn = categoryToSchedule.get(scoreCategory);

      try (OutputStream stream = new FileOutputStream(filename)) {
        SubjectivePdfWriter.createDocument(stream, description, tournamentName, sheetElement, schedulerColumn,
                                           _schedule);
      }
    }
  }

  public static SheetElement createSubjectiveSheetElement(final ScoreCategory sc) {
    // Get the info from the .xml sheet for the specific subjective category
    // An sc == a subjective category
    final SheetElement sheet = new SheetElement(sc);
    return sheet;
  }

  /**
   * @param tournamentName the name of the tournament to put in the sheets
   * @param output where to output
   * @param description where to get the goals from
   * @throws DocumentException
   * @throws SQLException
   * @throws IOException
   */
  public void outputPerformanceSheets(@Nonnull final String tournamentName,
                                      @Nonnull final OutputStream output,
                                      @Nonnull final ChallengeDescription description)
      throws DocumentException, SQLException, IOException {
    final ScoresheetGenerator scoresheets = new ScoresheetGenerator(getNumberOfRounds()
        * _schedule.size(), description, tournamentName);
    final SortedMap<PerformanceTime, TeamScheduleInfo> performanceTimes = new TreeMap<PerformanceTime, TeamScheduleInfo>();
    for (int round = 0; round < getNumberOfRounds(); ++round) {
      for (final TeamScheduleInfo si : _schedule) {
        performanceTimes.put(si.getPerf(round), si);
      }
    }

    int sheetIndex = 0;
    for (final Map.Entry<PerformanceTime, TeamScheduleInfo> entry : performanceTimes.entrySet()) {
      final PerformanceTime performance = entry.getKey();
      final TeamScheduleInfo si = entry.getValue();
      final int round = si.computeRound(performance);

      scoresheets.setTime(sheetIndex, performance.getTime());
      scoresheets.setTable(sheetIndex, String.format("%s %d", performance.getTable(), performance.getSide()));
      scoresheets.setRound(sheetIndex, String.valueOf(round
          + 1));
      scoresheets.setNumber(sheetIndex, si.getTeamNumber());
      scoresheets.setDivision(sheetIndex, ScoresheetGenerator.AWARD_GROUP_LABEL, si.getAwardGroup());
      scoresheets.setName(sheetIndex, si.getTeamName());

      ++sheetIndex;
    }

    final boolean orientationIsPortrait = ScoresheetGenerator.guessOrientation(description);
    scoresheets.writeFile(output, orientationIsPortrait);
  }

  private void outputPerformanceSchedule(final Document detailedSchedules) throws DocumentException {
    final SortedMap<PerformanceTime, TeamScheduleInfo> performanceTimes = new TreeMap<PerformanceTime, TeamScheduleInfo>();
    for (int round = 0; round < getNumberOfRounds(); ++round) {
      for (final TeamScheduleInfo si : _schedule) {
        performanceTimes.put(si.getPerf(round), si);
      }
    }

    // list of teams staying around to even up the teams
    final List<TeamScheduleInfo> teamsMissingOpponents = new LinkedList<TeamScheduleInfo>();

    final PdfPTable table = PdfUtils.createTable(7);
    int currentRow = 0;
    table.setWidths(new float[] { 2, 1, 3, 3, 2, 2, 2 });

    final PdfPCell tournamentCell = PdfUtils.createHeaderCell("Tournament: "
        + getName()
        + " Performance");
    tournamentCell.setColspan(7);
    table.addCell(tournamentCell);
    table.completeRow();
    ++currentRow;

    table.addCell(PdfUtils.createHeaderCell(TEAM_NUMBER_HEADER));
    table.addCell(PdfUtils.createHeaderCell(AWARD_GROUP_HEADER));
    table.addCell(PdfUtils.createHeaderCell(ORGANIZATION_HEADER));
    table.addCell(PdfUtils.createHeaderCell(TEAM_NAME_HEADER));
    table.addCell(PdfUtils.createHeaderCell("Time"));
    table.addCell(PdfUtils.createHeaderCell("Table"));
    table.addCell(PdfUtils.createHeaderCell("Round"));
    table.completeRow();
    ++currentRow;

    table.setHeaderRows(1);

    LocalTime prevTime = null;
    for (final Map.Entry<PerformanceTime, TeamScheduleInfo> entry : performanceTimes.entrySet()) {
      final PerformanceTime performance = entry.getKey();
      final TeamScheduleInfo si = entry.getValue();
      final int round = si.computeRound(performance);

      // check if team is missing an opponent
      final BaseColor backgroundColor;
      if (null == findOpponent(si, round)) {
        teamsMissingOpponents.add(si);
        backgroundColor = BaseColor.MAGENTA;
      } else {
        backgroundColor = null;
      }

      final LocalTime performanceTime = si.getPerfTime(round);
      final float topBorderWidth;
      if (Objects.equals(performanceTime, prevTime)) {
        topBorderWidth = Rectangle.UNDEFINED;

        // keep the rows with the same times together
        table.getRow(currentRow
            - 1).setMayNotBreak(true);
      } else {
        topBorderWidth = TIME_SEPARATOR_LINE_WIDTH;
      }

      PdfPCell cell = PdfUtils.createCell(String.valueOf(si.getTeamNumber()));
      cell.setBorderWidthTop(topBorderWidth);
      table.addCell(cell);

      cell = PdfUtils.createCell(si.getAwardGroup());
      cell.setBorderWidthTop(topBorderWidth);
      table.addCell(cell);

      cell = PdfUtils.createCell(si.getOrganization());
      cell.setBorderWidthTop(topBorderWidth);
      table.addCell(cell);

      cell = PdfUtils.createCell(si.getTeamName());
      cell.setBorderWidthTop(topBorderWidth);
      table.addCell(cell);

      cell = PdfUtils.createCell(formatTime(performanceTime), backgroundColor);
      cell.setBorderWidthTop(topBorderWidth);
      table.addCell(cell);

      cell = PdfUtils.createCell(si.getPerfTableColor(round)
          + " "
          + si.getPerfTableSide(round), backgroundColor);
      cell.setBorderWidthTop(topBorderWidth);
      table.addCell(cell);

      cell = PdfUtils.createCell(String.valueOf(round
          + 1));
      cell.setBorderWidthTop(topBorderWidth);
      table.addCell(cell);
      table.completeRow();

      ++currentRow;
      prevTime = performanceTime;
    }

    detailedSchedules.add(table);

    // output teams staying
    if (!teamsMissingOpponents.isEmpty()) {
      final String formatString = "Team %d does not have an opponent.";
      final PdfPTable stayingTable = PdfUtils.createTable(1);
      for (final TeamScheduleInfo si : teamsMissingOpponents) {
        stayingTable.addCell(PdfUtils.createCell(new Formatter().format(formatString, si.getTeamNumber()).toString(),
                                                 BaseColor.MAGENTA));
      }
      detailedSchedules.add(stayingTable);

    }

    // make sure the last row isn't by itself
    table.getRow(currentRow
        - 1).setMayNotBreak(true);

    detailedSchedules.add(Chunk.NEXTPAGE);
  }

  private void outputSubjectiveScheduleByDivision(final Document detailedSchedules,
                                                  final String subjectiveStation)
      throws DocumentException {
    final PdfPTable table = PdfUtils.createTable(6);
    table.setWidths(new float[] { 2, 1, 3, 3, 2, 2 });

    final PdfPCell tournamentCell = PdfUtils.createHeaderCell("Tournament: "
        + getName()
        + " - "
        + subjectiveStation);
    tournamentCell.setColspan(6);
    table.addCell(tournamentCell);

    table.addCell(PdfUtils.createHeaderCell(TEAM_NUMBER_HEADER));
    table.addCell(PdfUtils.createHeaderCell(AWARD_GROUP_HEADER));
    table.addCell(PdfUtils.createHeaderCell(ORGANIZATION_HEADER));
    table.addCell(PdfUtils.createHeaderCell(TEAM_NAME_HEADER));
    table.addCell(PdfUtils.createHeaderCell(subjectiveStation));
    table.addCell(PdfUtils.createHeaderCell(JUDGE_GROUP_HEADER));
    table.setHeaderRows(2);

    Collections.sort(_schedule, getComparatorForSubjectiveByDivision(subjectiveStation));
    for (final TeamScheduleInfo si : _schedule) {
      table.addCell(PdfUtils.createCell(String.valueOf(si.getTeamNumber())));
      table.addCell(PdfUtils.createCell(si.getAwardGroup()));
      table.addCell(PdfUtils.createCell(si.getOrganization()));
      table.addCell(PdfUtils.createCell(si.getTeamName()));
      table.addCell(PdfUtils.createCell(formatTime(si.getSubjectiveTimeByName(subjectiveStation).getTime())));
      table.addCell(PdfUtils.createCell(si.getJudgingGroup()));
    }

    detailedSchedules.add(table);

  }

  private void outputSubjectiveScheduleByTime(final Document detailedSchedules,
                                              final String subjectiveStation)
      throws DocumentException {
    final PdfPTable table = PdfUtils.createTable(6);
    int currentRow = 0;
    table.setWidths(new float[] { 2, 1, 3, 3, 2, 2 });

    final PdfPCell tournamentCell = PdfUtils.createHeaderCell("Tournament: "
        + getName()
        + " - "
        + subjectiveStation);
    tournamentCell.setColspan(6);
    table.addCell(tournamentCell);
    table.completeRow();
    currentRow++;

    table.addCell(PdfUtils.createHeaderCell(TEAM_NUMBER_HEADER));
    table.addCell(PdfUtils.createHeaderCell(AWARD_GROUP_HEADER));
    table.addCell(PdfUtils.createHeaderCell(ORGANIZATION_HEADER));
    table.addCell(PdfUtils.createHeaderCell(TEAM_NAME_HEADER));
    table.addCell(PdfUtils.createHeaderCell(subjectiveStation));
    table.addCell(PdfUtils.createHeaderCell(JUDGE_GROUP_HEADER));
    table.completeRow();
    currentRow++;
    table.setHeaderRows(2);

    Collections.sort(_schedule, getComparatorForSubjectiveByTime(subjectiveStation));
    LocalTime prevTime = null;
    for (final TeamScheduleInfo si : _schedule) {
      final LocalTime time = si.getSubjectiveTimeByName(subjectiveStation).getTime();

      final float topBorderWidth;
      if (Objects.equals(time, prevTime)) {
        topBorderWidth = Rectangle.UNDEFINED;

        // keep the rows with the same times together
        table.getRow(currentRow
            - 1).setMayNotBreak(true);
      } else {
        topBorderWidth = TIME_SEPARATOR_LINE_WIDTH;
      }

      PdfPCell cell = PdfUtils.createCell(String.valueOf(si.getTeamNumber()));
      cell.setBorderWidthTop(topBorderWidth);
      table.addCell(cell);

      cell = PdfUtils.createCell(si.getAwardGroup());
      cell.setBorderWidthTop(topBorderWidth);
      table.addCell(cell);

      cell = PdfUtils.createCell(si.getOrganization());
      cell.setBorderWidthTop(topBorderWidth);
      table.addCell(cell);

      cell = PdfUtils.createCell(si.getTeamName());
      cell.setBorderWidthTop(topBorderWidth);
      table.addCell(cell);

      cell = PdfUtils.createCell(formatTime(time));
      cell.setBorderWidthTop(topBorderWidth);
      table.addCell(cell);

      cell = PdfUtils.createCell(si.getJudgingGroup());
      cell.setBorderWidthTop(topBorderWidth);
      table.addCell(cell);
      table.completeRow();

      currentRow++;
      prevTime = time;
    }

    // make sure the last row isn't by itself
    table.getRow(currentRow
        - 1).setMayNotBreak(true);

    detailedSchedules.add(table);

  }

  /**
   * Get the comparator for outputting the schedule for the specified subjective
   * station. Sort by division, then judge, then by time.
   */
  private Comparator<TeamScheduleInfo> getComparatorForSubjectiveByDivision(final String name) {
    return new SubjectiveComparatorByDivision(name);
  }

  /**
   * Get the comparator for outputting the schedule for the specified subjective
   * station. Sort by time, division, then judge.
   */
  private Comparator<TeamScheduleInfo> getComparatorForSubjectiveByTime(final String name) {
    return new SubjectiveComparatorByTime(name);
  }

  /**
   * Comparator for for sorting by the specified subjective station.
   */
  private static class SubjectiveComparatorByDivision implements Comparator<TeamScheduleInfo>, Serializable {
    private final String name;

    public SubjectiveComparatorByDivision(final String name) {
      this.name = name;
    }

    public int compare(final TeamScheduleInfo one,
                       final TeamScheduleInfo two) {

      if (!one.getAwardGroup().equals(two.getAwardGroup())) {
        return one.getAwardGroup().compareTo(two.getAwardGroup());
      } else {
        final SubjectiveTime oneTime = one.getSubjectiveTimeByName(name);
        final SubjectiveTime twoTime = two.getSubjectiveTimeByName(name);

        final int timeCompare;
        if (oneTime == null) {
          if (twoTime == null) {
            timeCompare = 0;
          } else {
            timeCompare = -1;
          }
        } else {
          if (null == twoTime) {
            timeCompare = 1;
          } else {
            timeCompare = oneTime.getTime().compareTo(twoTime.getTime());
          }
        }
        if (timeCompare == 0) {
          return one.getJudgingGroup().compareTo(two.getJudgingGroup());
        } else {
          return timeCompare;
        }
      }
    }
  }

  /**
   * Comparator for for sorting by the specified subjective station.
   */
  private static class SubjectiveComparatorByTime implements Comparator<TeamScheduleInfo>, Serializable {
    private final String name;

    public SubjectiveComparatorByTime(final String name) {
      this.name = name;
    }

    public int compare(final TeamScheduleInfo one,
                       final TeamScheduleInfo two) {

      final SubjectiveTime oneTime = one.getSubjectiveTimeByName(name);
      final SubjectiveTime twoTime = two.getSubjectiveTimeByName(name);

      final int timeCompare;
      if (oneTime == null) {
        if (twoTime == null) {
          timeCompare = 0;
        } else {
          timeCompare = -1;
        }
      } else {
        if (null == twoTime) {
          timeCompare = 1;
        } else {
          timeCompare = oneTime.getTime().compareTo(twoTime.getTime());
        }
      }
      if (timeCompare == 0) {
        if (!one.getAwardGroup().equals(two.getAwardGroup())) {
          return one.getAwardGroup().compareTo(two.getAwardGroup());
        } else {
          return one.getJudgingGroup().compareTo(two.getJudgingGroup());
        }
      } else {
        return timeCompare;
      }
    }
  }

  /**
   * Comparator for for sorting by team number.
   */
  private static class ComparatorByTeam implements Comparator<TeamScheduleInfo>, Serializable {

    public static final ComparatorByTeam INSTANCE = new ComparatorByTeam();

    private ComparatorByTeam() {
    }

    public int compare(final TeamScheduleInfo one,
                       final TeamScheduleInfo two) {

      return Integer.compare(one.getTeamNumber(), two.getTeamNumber());
    }
  }

  /**
   * Compute the general schedule and return it as a string
   */
  public String computeGeneralSchedule() {
    LocalTime minPerf = null;
    LocalTime maxPerf = null;
    // division -> date
    final Map<String, LocalTime> minSubjectiveTimes = new HashMap<>();
    final Map<String, LocalTime> maxSubjectiveTimes = new HashMap<>();

    for (final TeamScheduleInfo si : _schedule) {
      final String judgingStation = si.getJudgingGroup();
      for (final SubjectiveTime stime : si.getSubjectiveTimes()) {
        final LocalTime currentMin = minSubjectiveTimes.get(judgingStation);
        if (null == currentMin) {
          minSubjectiveTimes.put(judgingStation, stime.getTime());
        } else {
          if (stime.getTime().isBefore(currentMin)) {
            minSubjectiveTimes.put(judgingStation, stime.getTime());
          }
        }
        final LocalTime currentMax = maxSubjectiveTimes.get(judgingStation);
        if (null == currentMax) {
          maxSubjectiveTimes.put(judgingStation, stime.getTime());
        } else {
          if (stime.getTime().isAfter(currentMax)) {
            maxSubjectiveTimes.put(judgingStation, stime.getTime());
          }
        }

      }

      for (int i = 0; i < getNumberOfRounds(); ++i) {
        if (null != si.getPerfTime(i)) {
          if (null == minPerf
              || si.getPerfTime(i).isBefore(minPerf)) {
            minPerf = si.getPerfTime(i);
          }

          if (null == maxPerf
              || si.getPerfTime(i).isAfter(maxPerf)) {
            maxPerf = si.getPerfTime(i);
          }
        }
      }

    }

    // print out the general schedule
    final Formatter output = new Formatter();
    final Set<String> stations = new HashSet<String>();
    stations.addAll(minSubjectiveTimes.keySet());
    stations.addAll(maxSubjectiveTimes.keySet());
    for (final String station : stations) {
      final LocalTime earliestStart = minSubjectiveTimes.get(station);
      final LocalTime latestStart = maxSubjectiveTimes.get(station);
      final Duration subjectiveDuration = Duration.ofMinutes(SolverParams.DEFAULT_SUBJECTIVE_MINUTES);
      final LocalTime latestEnd = latestStart.plus(subjectiveDuration);

      output.format("Subjective times for judging station %s: %s - %s (assumes default subjective time of %d minutes)%n",
                    station, formatTime(earliestStart), formatTime(latestEnd), SolverParams.DEFAULT_SUBJECTIVE_MINUTES);
    }
    if (null != minPerf
        && null != maxPerf) {
      final Duration performanceDuration = Duration.ofMinutes(SolverParams.DEFAULT_PERFORMANCE_MINUTES);
      final LocalTime performanceEnd = maxPerf.plus(performanceDuration);

      output.format("Performance times: %s - %s (assumes default performance time of %d minutes)%n",
                    formatTime(minPerf), formatTime(performanceEnd), SolverParams.DEFAULT_PERFORMANCE_MINUTES);
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

  private void removeFromMatches(final TeamScheduleInfo ti,
                                 final int round) {
    if (!_matches.containsKey(ti.getPerfTime(round))) {
      throw new IllegalArgumentException("Cannot find time info for "
          + round
          + " in matches");
    }
    final Map<String, List<TeamScheduleInfo>> timeMatches = _matches.get(ti.getPerfTime(round));
    if (!timeMatches.containsKey(ti.getPerfTableColor(round))) {
      throw new IllegalArgumentException("Cannot find table info for "
          + round
          + " in matches");
    }
    final List<TeamScheduleInfo> tableMatches = timeMatches.get(ti.getPerfTableColor(round));
    if (!tableMatches.remove(ti)) {
      throw new IllegalArgumentException("Cannot find team "
          + ti.getTeamNumber()
          + " in the matches for round "
          + round);
    }
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
      if (tableMatches.get(0).equals(tableMatches.get(1))) {
        throw new FLLRuntimeException("Internal error, _matches is inconsistent. Has team competing against itself");
      }
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
                                     final ColumnInformation ci)
      throws IOException, ParseException, ScheduleParseException {
    final String[] line = reader.readNext();
    if (null == line) {
      return null;
    }

    try {

      if (ci.getTeamNumColumn() >= line.length) {
        return null;
      }
      final String teamNumberStr = line[ci.getTeamNumColumn()];
      if (null == teamNumberStr
          || teamNumberStr.length() < 1) {
        // hit empty row
        return null;
      }

      final int teamNumber = Utilities.INTEGER_NUMBER_FORMAT_INSTANCE.parse(teamNumberStr).intValue();
      final TeamScheduleInfo ti = new TeamScheduleInfo(getNumberOfRounds(), teamNumber);
      ti.setTeamName(line[ci.getTeamNameColumn()]);
      ti.setOrganization(line[ci.getOrganizationColumn()]);
      ti.setDivision(line[ci.getDivisionColumn()]);

      for (final Map.Entry<Integer, String> entry : ci.getSubjectiveColumnInfo().entrySet()) {
        final String station = entry.getValue();
        final int column = entry.getKey();
        final String str = line[column];
        if (str.isEmpty()) {
          // If we got an empty string, then we must have hit the end
          return null;
        }
        final LocalTime time = parseTime(str);
        ti.addSubjectiveTime(new SubjectiveTime(station, time));
      }

      ti.setJudgingGroup(line[ci.getJudgeGroupColumn()]);

      for (int perfNum = 0; perfNum < getNumberOfRounds(); ++perfNum) {
        final String perf1Str = line[ci.getPerfColumn(perfNum)];
        if (perf1Str.isEmpty()) {
          // If we got an empty string, then we must have hit the end
          return null;
        }
        String table = line[ci.getPerfTableColumn(perfNum)];
        String[] tablePieces = table.split(" ");
        if (tablePieces.length != 2) {
          throw new RuntimeException("Error parsing table information from: "
              + table);
        }
        final LocalTime perf1Time = parseTime(perf1Str);
        final PerformanceTime performance = new PerformanceTime(perf1Time, tablePieces[0],
                                                                Utilities.INTEGER_NUMBER_FORMAT_INSTANCE.parse(tablePieces[1])
                                                                                                        .intValue());
        ti.setPerf(perfNum, performance);
        if (ti.getPerfTableSide(perfNum) > 2
            || ti.getPerfTableSide(perfNum) < 1) {
          final String message = "There are only two sides to the table, number must be 1 or 2 team: "
              + ti.getTeamNumber()
              + " round "
              + (perfNum
                  + 1);
          LOGGER.error(message);
          throw new ScheduleParseException(message);
        }
      }

      return ti;
    } catch (final ParseException | NumberFormatException pe) {
      LOGGER.error("Error parsing line: "
          + Arrays.toString(line), pe);
      throw pe;
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
                                                 final int tournamentID)
      throws SQLException {
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
                            final int tournamentID)
      throws SQLException {
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
        insertSchedule.setString(3, si.getJudgingGroup());
        insertSchedule.executeUpdate();

        insertPerfRounds.setInt(2, si.getTeamNumber());
        for (int round = 0; round < si.getNumberOfRounds(); ++round) {
          insertPerfRounds.setInt(3, round);
          insertPerfRounds.setTime(4, Time.valueOf(si.getPerfTime(round)));
          insertPerfRounds.setString(5, si.getPerfTableColor(round));
          insertPerfRounds.setInt(6, si.getPerfTableSide(round));
          insertPerfRounds.executeUpdate();
        }

        for (final SubjectiveTime subjectiveTime : si.getSubjectiveTimes()) {
          insertSubjective.setInt(2, si.getTeamNumber());
          insertSubjective.setString(3, subjectiveTime.getName());
          insertSubjective.setTime(4, Time.valueOf(subjectiveTime.getTime()));
          insertSubjective.executeUpdate();
        }
      }

    } finally {
      SQLFunctions.close(deletePerfRounds);
      deletePerfRounds = null;
      SQLFunctions.close(deleteSchedule);
      deleteSchedule = null;
      SQLFunctions.close(deleteSubjective);
      deleteSubjective = null;
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
                                                             final int tournamentID)
      throws SQLException {
    final Collection<ConstraintViolation> violations = new LinkedList<ConstraintViolation>();
    final Map<Integer, TournamentTeam> dbTeams = Queries.getTournamentTeams(connection, tournamentID);
    final Set<Integer> scheduleTeamNumbers = new HashSet<Integer>();
    for (final TeamScheduleInfo si : _schedule) {
      scheduleTeamNumbers.add(si.getTeamNumber());
      if (!dbTeams.containsKey(si.getTeamNumber())) {
        // TODO: could support adding teams to the database based upon the
        // schedule
        violations.add(new ConstraintViolation(ConstraintViolation.Type.HARD, si.getTeamNumber(), null, null, null,
                                               "Team "
                                                   + si.getTeamNumber()
                                                   + " is in schedule, but not in database"));
      }

      if (si.getJudgingGroup().isEmpty()) {
        violations.add(new ConstraintViolation(ConstraintViolation.Type.HARD, si.getTeamNumber(), null, null, null,
                                               "Team "
                                                   + si.getTeamNumber()
                                                   + " has no judging station specified"));
      }
    }
    for (final Integer dbNum : dbTeams.keySet()) {
      if (!scheduleTeamNumbers.contains(dbNum)) {
        violations.add(new ConstraintViolation(ConstraintViolation.Type.HARD, dbNum, null, null, null, "Team "
            + dbNum
            + " is in database, but not in schedule"));
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

  /**
   * Find the {@link TeamScheduleInfo} object for the specified team number.
   * 
   * @return the schedule info or null if not found
   */
  public TeamScheduleInfo findScheduleInfo(final int team) {
    for (final TeamScheduleInfo si : _schedule) {
      if (si.getTeamNumber() == team) {
        return si;
      }
    }
    return null;
  }

  /**
   * @param team the team to rescheduled
   * @param oldTime the old time that the team was scheduled at
   * @param perfTime the new performance information
   */
  public void reassignTable(final int team,
                            final LocalTime oldTime,
                            final PerformanceTime perfTime) {
    final TeamScheduleInfo si = findScheduleInfo(team);
    if (null == si) {
      throw new IllegalArgumentException("Cannot find team "
          + team
          + " in the schedule");
    }
    final int round = si.findRoundFortime(oldTime);
    if (-1 == round) {
      throw new IllegalArgumentException("Team "
          + team
          + " does not have a performance at "
          + oldTime);
    }

    removeFromMatches(si, round);
    si.setPerf(round, perfTime);
    addToMatches(si, round);
  }

  /**
   * Convert the schedule to an XML document that conforms to
   * fll/resources/schedule.xsd. The document that is returned has already been
   * run through {@link #validateXML(org.w3c.dom.Document)}.
   */
  public org.w3c.dom.Document createXML() {
    final org.w3c.dom.Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();

    final Element top = document.createElementNS(null, "schedule");
    document.appendChild(top);

    for (final TeamScheduleInfo si : getSchedule()) {
      final Element team = document.createElementNS(null, "team");
      top.appendChild(team);
      team.setAttributeNS(null, "number", String.valueOf(si.getTeamNumber()));
      team.setAttributeNS(null, "judging_station", si.getJudgingGroup());

      for (final String subjName : si.getKnownSubjectiveStations()) {
        final LocalTime time = si.getSubjectiveTimeByName(subjName).getTime();
        final Element subjective = document.createElementNS(null, "subjective");
        team.appendChild(subjective);
        subjective.setAttributeNS(null, "name", subjName);
        subjective.setAttributeNS(null, "time", XML_TIME_FORMAT.format(time));
      }

      for (int round = 0; round < si.getNumberOfRounds(); ++round) {
        final PerformanceTime perfTime = si.getPerf(round);
        final Element perf = document.createElementNS(null, "performance");
        team.appendChild(perf);
        perf.setAttributeNS(null, "round", String.valueOf(round
            + 1));
        perf.setAttributeNS(null, "table_color", perfTime.getTable());
        perf.setAttributeNS(null, "table_side", String.valueOf(perfTime.getSide()));
        perf.setAttributeNS(null, "time", XML_TIME_FORMAT.format(perfTime.getTime()));
      }
    }

    try {
      validateXML(document);
    } catch (final SAXException e) {
      throw new FLLInternalException("Schedule XML document is invalid", e);
    }

    return document;
  }

  /**
   * Validate the schedule XML document.
   * 
   * @throws SAXException on an error
   */
  public static void validateXML(final org.w3c.dom.Document document) throws SAXException {
    try {
      final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

      final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      final Source schemaFile = new StreamSource(classLoader.getResourceAsStream("fll/resources/schedule.xsd"));
      final Schema schema = factory.newSchema(schemaFile);

      final Validator validator = schema.newValidator();
      validator.validate(new DOMSource(document));
    } catch (final IOException e) {
      throw new RuntimeException("Internal error, should never get IOException here", e);
    }

  }

  /**
   * Write out to the specified writer.
   * 
   * @param outputWriter
   * @throws IOException
   */
  public void writeToCSV(final Writer outputWriter) throws IOException {
    try (final CSVWriter csv = new CSVWriter(outputWriter)) {

      final List<String> line = new ArrayList<String>();
      line.add(TournamentSchedule.TEAM_NUMBER_HEADER);
      line.add(TournamentSchedule.AWARD_GROUP_HEADER);
      line.add(TournamentSchedule.TEAM_NAME_HEADER);
      line.add(TournamentSchedule.ORGANIZATION_HEADER);
      line.add(TournamentSchedule.JUDGE_GROUP_HEADER);
      final List<String> categories = Collections.unmodifiableList(new LinkedList<String>(getSubjectiveStations()));
      for (final String category : categories) {
        line.add(category);
      }
      for (int round = 0; round < getNumberOfRounds(); ++round) {
        line.add(String.format(TournamentSchedule.PERF_HEADER_FORMAT, round
            + 1));
        line.add(String.format(TournamentSchedule.TABLE_HEADER_FORMAT, round
            + 1));
      }
      csv.writeNext(line.toArray(new String[line.size()]));
      line.clear();

      for (final TeamScheduleInfo si : getSchedule()) {
        line.add(String.valueOf(si.getTeamNumber()));
        line.add(si.getAwardGroup());
        line.add(si.getTeamName());
        line.add(si.getOrganization());
        line.add(si.getJudgingGroup());
        for (final String category : categories) {
          final LocalTime d = si.getSubjectiveTimeByName(category).getTime();
          line.add(TournamentSchedule.formatTime(d));
        }
        for (int round = 0; round < getNumberOfRounds(); ++round) {
          final PerformanceTime p = si.getPerf(round);
          line.add(TournamentSchedule.formatTime(p.getTime()));
          line.add(p.getTable()
              + " "
              + p.getSide());
        }
        csv.writeNext(line.toArray(new String[line.size()]));
        line.clear();
      }
    }
  }

  /**
   * Write out the current schedule to a CSV file.
   * 
   * @param outputFile where to write
   * @throws IOException
   */
  public void writeToCSV(final File outputFile) throws IOException {
    try (final Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile), Utilities.DEFAULT_CHARSET)) {
      writeToCSV(writer);
    }
  }

  public static class MissingColumnException extends FLLRuntimeException {
    public MissingColumnException(final String message) {
      super(message);
    }
  }

}
