/*
 * Copyright (c) 2000-2008 HighTechKids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.scheduler;

import java.io.BufferedOutputStream;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import com.diffplug.common.base.Errors;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.opencsv.CSVWriter;

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
import fll.util.PdfUtils;
import fll.util.SimpleFooterHandler;
import fll.web.playoff.ScoresheetGenerator;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreCategory;
import fll.xml.SubjectiveScoreCategory;

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
  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

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

  public static final String AWARD_GROUP_HEADER = "Award Group";

  public static final String JUDGE_GROUP_HEADER = "Judging Group";

  public static final String BASE_PERF_HEADER = "Perf #";

  public static final String BASE_PRACTICE_HEADER = "Practice #";

  public static final String BASE_PRACTICE_HEADER_SHORT = "Practice";

  /**
   * Used with {@link String#format(String, Object...)} to create a performance
   * round header.
   */
  public static final String PERF_HEADER_FORMAT = BASE_PERF_HEADER
      + "%d";

  /**
   * Used with {@link String#format(String, Object...)} to create a practice
   * round header.
   */
  public static final String PRACTICE_HEADER_FORMAT = BASE_PRACTICE_HEADER
      + "%d";

  /**
   * Used with {@link String#format(String, Object...)} to create a performance
   * table header.
   */
  public static final String TABLE_HEADER_FORMAT = "Perf %d Table";

  public static final String PRACTICE_TABLE_HEADER_FORMAT = "Practice %d Table";

  public static final String PRACTICE_TABLE_HEADER_FORMAT_SHORT = "Practice Table";

  /**
   * @return Number of rounds in this schedule (practice and regular match play).
   * @see #getNumberOfPracticeRounds()
   * @see #getNumberOfRegularMatchPlayRounds()
   */
  public int getTotalNumberOfRounds() {
    return getNumberOfRegularMatchPlayRounds()
        + getNumberOfPracticeRounds();
  }

  private final int numRegularMatchPlayRounds;

  /**
   * @return number of regular match play rounds in this schedule
   */
  public int getNumberOfRegularMatchPlayRounds() {
    return numRegularMatchPlayRounds;
  }

  private final int numPracticeRounds;

  /**
   * @return number of practice rounds in this schedule
   */
  public int getNumberOfPracticeRounds() {
    return numPracticeRounds;
  }

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

  private final HashSet<String> _tableColors = new HashSet<>();

  public Set<String> getTableColors() {
    return Collections.unmodifiableSet(_tableColors);
  }

  private final HashSet<String> _awardGroups = new HashSet<>();

  public Set<String> getAwardGroups() {
    return Collections.unmodifiableSet(_awardGroups);
  }

  private final HashSet<String> _judgingGroups = new HashSet<>();

  public Set<String> getJudgingGroups() {
    return Collections.unmodifiableSet(_judgingGroups);
  }

  private final LinkedList<TeamScheduleInfo> _schedule = new LinkedList<>();

  private final HashSet<String> subjectiveStations = new HashSet<>();

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
    numRegularMatchPlayRounds = columnInfo.getNumPerfs();
    numPracticeRounds = columnInfo.getNumPracticePerfs();
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

    try (
        PreparedStatement getSubjectiveStations = connection.prepareStatement("SELECT DISTINCT name from sched_subjective WHERE tournament = ?")) {
      getSubjectiveStations.setInt(1, tournamentID);
      try (ResultSet stations = getSubjectiveStations.executeQuery()) {
        while (stations.next()) {
          final String name = stations.getString(1);
          subjectiveStations.add(name);
        }
      }
    }

    try (PreparedStatement getSched = connection.prepareStatement("SELECT team_number, judging_station"
        + " FROM schedule"//
        + " WHERE tournament = ?");

        PreparedStatement getPerfRounds = connection.prepareStatement("SELECT perf_time, table_color, table_side, practice" //
            + " FROM sched_perf_rounds" //
            + " WHERE tournament = ? AND team_number = ?");

        PreparedStatement getSubjective = connection.prepareStatement("SELECT name, subj_time" //
            + " FROM sched_subjective" //
            + " WHERE tournament = ? AND team_number = ?")) {

      getSched.setInt(1, tournamentID);
      getPerfRounds.setInt(1, tournamentID);

      getSubjective.setInt(1, tournamentID);

      try (ResultSet sched = getSched.executeQuery()) {
        while (sched.next()) {
          final int teamNumber = sched.getInt(1);
          final String judgingStation = sched.getString(2);

          final TeamScheduleInfo ti = new TeamScheduleInfo(teamNumber);
          ti.setJudgingGroup(judgingStation);

          getSubjective.setInt(2, teamNumber);
          try (ResultSet subjective = getSubjective.executeQuery()) {
            while (subjective.next()) {
              final String name = subjective.getString(1);
              final Time subjTime = subjective.getTime(2);
              ti.addSubjectiveTime(new SubjectiveTime(name, subjTime.toLocalTime()));
            }
          }

          getPerfRounds.setInt(2, teamNumber);
          try (ResultSet perfRounds = getPerfRounds.executeQuery()) {
            while (perfRounds.next()) {
              final LocalTime perfTime = perfRounds.getTime(1).toLocalTime();
              final String tableColor = perfRounds.getString(2);
              final int tableSide = perfRounds.getInt(3);
              if (tableSide != 1
                  && tableSide != 2) {
                throw new RuntimeException("Tables sides must be 1 or 2. Tournament: "
                    + tournamentID
                    + " team: "
                    + teamNumber);
              }
              final boolean practice = perfRounds.getBoolean(4);
              final PerformanceTime performance = new PerformanceTime(perfTime, tableColor, tableSide, practice);
              ti.addPerformance(performance);
            } // foreach performance round
          } // allocate performance ResultSet

          final String eventDivision = Queries.getEventDivision(connection, teamNumber, tournamentID);
          ti.setDivision(eventDivision);

          final Team team = Team.getTeamFromDatabase(connection, teamNumber);
          ti.setOrganization(team.getOrganization());
          ti.setTeamName(team.getTeamName());

          cacheTeamScheduleInformation(ti);
        } // foreach sched result

      } // allocate sched ResultSet

    } // allocate prepared statements

    if (!_schedule.isEmpty()) {
      this.numRegularMatchPlayRounds = _schedule.get(0).getNumRegularMatchPlayRounds();
      this.numPracticeRounds = _schedule.get(0).getNumPracticeRounds();
      validateRounds();
    } else {
      this.numRegularMatchPlayRounds = 0;
      this.numPracticeRounds = 0;
    }

  }

  private void validateRounds() {
    for (final TeamScheduleInfo si : _schedule) {
      if (this.numRegularMatchPlayRounds != si.getNumRegularMatchPlayRounds()) {
        throw new RuntimeException("Should have "
            + this.numRegularMatchPlayRounds
            + " performance rounds for all teams, but found "
            + si.getNumRegularMatchPlayRounds()
            + " for team "
            + si.getTeamNumber());
      }

      if (this.numPracticeRounds != si.getNumPracticeRounds()) {
        throw new RuntimeException("Should have "
            + this.numPracticeRounds
            + " practice for all teams, but found "
            + si.getNumPracticeRounds()
            + " for team "
            + si.getTeamNumber());
      }

    }
  }

  /**
   * Check if this line is a header line. This checks for key headers and
   * returns true if they are found.
   */
  private static boolean isHeaderLine(final String[] line) {
    boolean retval = false;
    for (final String element : line) {
      if (TEAM_NUMBER_HEADER.equals(element)) {
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
   * Figure out how many regular match play rounds exist in this header line. This
   * method also checks that the corresponding table header exists for each
   * round and that the round numbers are contiguous starting at 1.
   *
   * @throws FLLRuntimeException if there are problems with the performance
   *           round headers found
   */
  private static int countNumRegularMatchPlayRounds(final String[] line) {
    final SortedSet<Integer> perfRounds = new TreeSet<>();
    for (final String element : line) {
      if (null != element
          && element.startsWith(BASE_PERF_HEADER)
          && element.length() > BASE_PERF_HEADER.length()) {
        final String perfNumberStr = element.substring(BASE_PERF_HEADER.length());
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
    for (final Integer value : perfRounds) {
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

  private static int countNumPracticeRounds(final String[] line) {
    final SortedSet<Integer> perfRounds = new TreeSet<>();
    for (final String element : line) {
      if (null != element
          && element.startsWith(BASE_PRACTICE_HEADER)
          && element.length() > BASE_PRACTICE_HEADER.length()) {
        final String perfNumberStr = element.substring(BASE_PRACTICE_HEADER.length());
        final Integer perfNumber = Integer.valueOf(perfNumberStr);
        if (!perfRounds.add(perfNumber)) {
          throw new FLLRuntimeException("Found practice rounds num "
              + perfNumber
              + " twice in the header: "
              + Arrays.asList(line));
        }
      } else if (null != element
          && element.equals(BASE_PRACTICE_HEADER_SHORT)) {
        if (!perfRounds.add(1)) {
          throw new FLLRuntimeException("Found practice rounds num "
              + 1
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
    for (final Integer value : perfRounds) {
      if (null == value) {
        throw new FLLInternalException("Found null practice round in header!");
      }
      if (expectedValue != value.intValue()) {
        throw new FLLRuntimeException("Practice rounds not contiguous after "
            + (expectedValue
                - 1)
            + " found "
            + value);
      }

      if (1 == expectedValue
          && 1 == perfRounds.size()
          && checkHeaderExists(line, PRACTICE_TABLE_HEADER_FORMAT_SHORT)) {
        // pass
        LOGGER.trace("Found short practice table header");
      } else {
        final String tableHeader = String.format(PRACTICE_TABLE_HEADER_FORMAT, expectedValue);
        if (!checkHeaderExists(line, tableHeader)) {
          throw new FLLRuntimeException("Couldn't find header for round "
              + expectedValue
              + ". Looking for header '"
              + tableHeader
              + "'");
        }
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
    final int numPerfRounds = countNumRegularMatchPlayRounds(line);
    final int numPracticeRounds = countNumPracticeRounds(line);

    final int[] perfColumn = new int[numPerfRounds];
    final int[] perfTableColumn = new int[numPerfRounds];

    final int[] practiceColumn = new int[numPracticeRounds];
    final int[] practiceTableColumn = new int[numPracticeRounds];

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

    if (1 == numPracticeRounds) {
      // check for short version of headers first, then try long version
      final Integer index = columnForHeader(line, BASE_PRACTICE_HEADER_SHORT);
      if (null == index) {
        practiceColumn[0] = getColumnForHeader(line, String.format(PRACTICE_HEADER_FORMAT, 1));
      } else {
        practiceColumn[0] = index.intValue();
      }

      final Integer indexTable = columnForHeader(line, PRACTICE_TABLE_HEADER_FORMAT_SHORT);
      if (null == indexTable) {
        practiceTableColumn[0] = getColumnForHeader(line, String.format(PRACTICE_TABLE_HEADER_FORMAT, 1));
      } else {
        practiceTableColumn[0] = indexTable.intValue();
      }
    } else {
      for (int round = 0; round < numPracticeRounds; ++round) {
        final String perfHeader = String.format(PRACTICE_HEADER_FORMAT, (round
            + 1));
        final String perfTableHeader = String.format(PRACTICE_TABLE_HEADER_FORMAT, (round
            + 1));
        practiceColumn[round] = getColumnForHeader(line, perfHeader);
        practiceTableColumn[round] = getColumnForHeader(line, perfTableHeader);
      }
    }

    final Map<Integer, String> subjectiveColumns = new HashMap<>();
    for (final String header : subjectiveHeaders) {
      final int column = getColumnForHeader(line, header);
      subjectiveColumns.put(column, header);
    }

    return new ColumnInformation(line, teamNumColumn, organizationColumn, teamNameColumn, divisionColumn,
                                 subjectiveColumns, judgeGroupColumn, perfColumn, perfTableColumn, practiceColumn,
                                 practiceTableColumn);
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
    ti.allPerformances().forEach(performance -> {
      _tableColors.add(performance.getTable());
      addToMatches(ti, performance);
    });

    _awardGroups.add(ti.getAwardGroup());
    _judgingGroups.add(ti.getJudgingGroup());
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

    final File byDivision = new File(directory, baseFilename
        + "-subjective-by-division.pdf");
    try (OutputStream pdfFos = new FileOutputStream(byDivision)) {
      outputSubjectiveSchedulesByJudgingStation(pdfFos);
    }

    final File byCategory = new File(directory, baseFilename
        + "-subjective-by-category.pdf");
    try (OutputStream pdfFos = new FileOutputStream(byCategory)) {
      outputSubjectiveSchedulesByCategory(pdfFos);
    }

    final File byTime = new File(directory, baseFilename
        + "-subjective-by-time.pdf");
    try (OutputStream pdfFos = new FileOutputStream(byTime)) {
      outputSubjectiveSchedulesByTimeOnly(pdfFos);
    }

    final File performance = new File(directory, baseFilename
        + "-performance.pdf");
    try (OutputStream pdfFos = new BufferedOutputStream(new FileOutputStream(performance))) {
      ScheduleWriter.outputPerformanceScheduleByTime(this, pdfFos);
    }

    final File teamSchedules = new File(directory, baseFilename
        + "-team-schedules.pdf");
    try (OutputStream pdfFos = new FileOutputStream(teamSchedules)) {
      outputTeamSchedules(params, pdfFos);
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
   * Output the subjective schedules with a table for each category and sorted
   * by time.
   *
   * @param pdfFos where to write the schedule
   * @throws DocumentException
   */
  public void outputSubjectiveSchedulesByCategory(final OutputStream pdfFos) throws DocumentException {
    final Document detailedSchedulesByTime = PdfUtils.createPortraitPdfDoc(pdfFos, new SimpleFooterHandler());
    for (final String subjectiveStation : subjectiveStations) {
      outputSubjectiveScheduleByCategory(detailedSchedulesByTime, subjectiveStation);
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
        + (getNumberOfRegularMatchPlayRounds()
            * 2)
        + (getNumberOfPracticeRounds()
            * 2);
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
    for (int i = 0; i < getNumberOfPracticeRounds(); ++i) {
      columnWidths[idx] = 2; // time
      ++idx;
      columnWidths[idx] = 2; // table
      ++idx;
    }
    for (int i = 0; i < getNumberOfRegularMatchPlayRounds(); ++i) {
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
    for (int round = 0; round < getNumberOfPracticeRounds(); ++round) {
      table.addCell(PdfUtils.createHeaderCell(String.format(PRACTICE_HEADER_FORMAT, round
          + 1)));
      table.addCell(PdfUtils.createHeaderCell(String.format(PRACTICE_TABLE_HEADER_FORMAT, round
          + 1)));
    }
    for (int round = 0; round < getNumberOfRegularMatchPlayRounds(); ++round) {
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

      si.enumeratePracticePerformances().forEachOrdered(Errors.rethrow().wrap(pair -> {
        final PerformanceTime perf = pair.getLeft();
        table.addCell(PdfUtils.createCell(formatTime(perf.getTime())));

        table.addCell(PdfUtils.createCell(String.format("%s %s", perf.getTable(), perf.getSide())));
      }));

      si.enumerateRegularMatchPlayPerformances().forEachOrdered(Errors.rethrow().wrap(pair -> {
        final PerformanceTime perf = pair.getLeft();

        table.addCell(PdfUtils.createCell(formatTime(perf.getTime())));

        table.addCell(PdfUtils.createCell(String.format("%s %s", perf.getTable(), perf.getSide())));
      }));

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

    for (final PerformanceTime performance : si.getAllPerformances()) {
      final String roundName = si.getRoundName(performance);
      para.add(new Chunk(roundName
          + ": ", TEAM_HEADER_FONT));
      final LocalTime start = performance.getTime();
      final LocalTime end = start.plus(Duration.ofMinutes(params.getPerformanceMinutes()));
      para.add(new Chunk(String.format("%s - %s %s %d", formatTime(start), formatTime(end), performance.getTable(),
                                       performance.getSide()),
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
   * @param filenameSuffixes map score category to output filename suffixes, may
   *          be empty
   * @throws DocumentException
   * @throws MalformedURLException
   * @throws IOException
   */
  public void outputSubjectiveSheets(@Nonnull final String tournamentName,
                                     final String dir,
                                     final String baseFileName,
                                     final ChallengeDescription description,
                                     @Nonnull final Map<ScoreCategory, String> categoryToSchedule,
                                     @Nonnull final Map<ScoreCategory, String> filenameSuffixes)
      throws DocumentException, MalformedURLException, IOException {

    // setup the sheets from the sucked in xml
    for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
      final SheetElement sheetElement = createSubjectiveSheetElement(category);
      final String suffix = filenameSuffixes.get(category);

      final String filename = dir
          + File.separator
          + baseFileName
          + "-"
          + category.getName()
          + (null != suffix ? "_"
              + suffix : "")
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

  public static SheetElement createSubjectiveSheetElement(final SubjectiveScoreCategory sc) {
    // Get the info from the .xml sheet for the specific subjective category
    // An sc == a subjective category
    final SheetElement sheet = new SheetElement(sc);
    return sheet;
  }

  /**
   * Sheets are sorted by table and then by time.
   *
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
    final ScoresheetGenerator scoresheets = new ScoresheetGenerator(getTotalNumberOfRounds()
        * _schedule.size(), description, tournamentName);
    final SortedMap<PerformanceTime, TeamScheduleInfo> performanceTimes = new TreeMap<>(PerformanceTime.ByTableThenTime.INSTANCE);
    for (final TeamScheduleInfo si : _schedule) {
      for (final PerformanceTime pt : si.getAllPerformances()) {
        performanceTimes.put(pt, si);
      }
    }

    int sheetIndex = 0;
    for (final Map.Entry<PerformanceTime, TeamScheduleInfo> entry : performanceTimes.entrySet()) {
      final PerformanceTime performance = entry.getKey();
      final TeamScheduleInfo si = entry.getValue();

      scoresheets.setTime(sheetIndex, performance.getTime());
      scoresheets.setTable(sheetIndex, String.format("%s %d", performance.getTable(), performance.getSide()));
      scoresheets.setRound(sheetIndex, si.getRoundName(performance));
      scoresheets.setNumber(sheetIndex, si.getTeamNumber());
      scoresheets.setDivision(sheetIndex, ScoresheetGenerator.AWARD_GROUP_LABEL, si.getAwardGroup());
      scoresheets.setName(sheetIndex, si.getTeamName());
      scoresheets.setPractice(sheetIndex, performance.isPractice());

      ++sheetIndex;
    }

    final Pair<Boolean, Float> orientationResult = ScoresheetGenerator.guessOrientation(description);
    final boolean orientationIsPortrait = orientationResult.getLeft();
    final float pagesPerScoreSheet = orientationResult.getRight();
    scoresheets.writeFile(output, orientationIsPortrait, pagesPerScoreSheet);
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
    String prevAwardGroup = null;
    for (final TeamScheduleInfo si : _schedule) {
      final LocalTime time = si.getSubjectiveTimeByName(subjectiveStation).getTime();
      final String awardGroup = si.getAwardGroup();
      final float topBorderWidth;
      if (Objects.equals(awardGroup, prevAwardGroup)) {
        topBorderWidth = Rectangle.UNDEFINED;
        // keep the rows with the same award group together
        table.getRow(table.getLastCompletedRowIndex()).setMayNotBreak(true);
      } else {
        topBorderWidth = TIME_SEPARATOR_LINE_WIDTH;
      }

      PdfPCell cell = PdfUtils.createCell(String.valueOf(si.getTeamNumber()));
      cell.setBorderWidthTop(topBorderWidth);
      table.addCell(cell);

      cell = PdfUtils.createCell(awardGroup);
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

      prevAwardGroup = awardGroup;
    }

    // make sure the last row isn't by itself
    table.getRow(table.getLastCompletedRowIndex()).setMayNotBreak(true);

    detailedSchedules.add(table);

  }

  private void outputSubjectiveScheduleByCategory(final Document detailedSchedules,
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
   * Output the subjective schedule sorted by time, then station, then award
   * group, then team
   * number.
   * 
   * @param detailedSchedules
   * @throws DocumentException
   */
  public void outputSubjectiveSchedulesByTimeOnly(final OutputStream stream) throws DocumentException {
    final Document detailedSchedulesByTime = PdfUtils.createPortraitPdfDoc(stream, new SimpleFooterHandler());
    outputSubjectiveSchedulesByTimeOnly(detailedSchedulesByTime);
    detailedSchedulesByTime.close();
  }

  private void outputSubjectiveSchedulesByTimeOnly(final Document detailedSchedules) throws DocumentException {
    final String[] headers = new String[] { TEAM_NUMBER_HEADER, AWARD_GROUP_HEADER, ORGANIZATION_HEADER,
                                            TEAM_NAME_HEADER, "Station", JUDGE_GROUP_HEADER, "Time" };

    final PdfPTable table = PdfUtils.createTable(headers.length);
    int currentRow = 0;
    table.setWidths(new float[] { 2, 2, 3, 3, 2, 2, 2 });

    final PdfPCell tournamentCell = PdfUtils.createHeaderCell("Tournament: "
        + getName());
    tournamentCell.setColspan(headers.length);
    table.addCell(tournamentCell);
    table.completeRow();
    currentRow++;

    for (final String header : headers) {
      table.addCell(PdfUtils.createHeaderCell(header));
    }
    table.completeRow();
    currentRow++;
    table.setHeaderRows(2);

    final Stream<TeamScheduleInfo> s1 = _schedule.stream();
    final Stream<List<TeamAtSubjectiveTime>> s2 = s1.map(ti -> ti.getSubjectiveTimes().stream()
                                                                 .map(st -> new TeamAtSubjectiveTime(ti, st))
                                                                 .collect(Collectors.toList()));
    final Stream<TeamAtSubjectiveTime> s3 = s2.flatMap(Collection::stream);
    final List<TeamAtSubjectiveTime> times = s3.collect(Collectors.toList());
    Collections.sort(times);

    LocalTime prevTime = null;
    for (final TeamAtSubjectiveTime teamAtTime : times) {
      final LocalTime time = teamAtTime.getSubjTime().getTime();

      final float topBorderWidth;
      if (Objects.equals(time, prevTime)) {
        topBorderWidth = Rectangle.UNDEFINED;

        // keep the rows with the same times together
        table.getRow(currentRow
            - 1).setMayNotBreak(true);
      } else {
        topBorderWidth = TIME_SEPARATOR_LINE_WIDTH;
      }

      PdfPCell cell = PdfUtils.createCell(String.valueOf(teamAtTime.getTeamInfo().getTeamNumber()));
      cell.setBorderWidthTop(topBorderWidth);
      table.addCell(cell);

      cell = PdfUtils.createCell(teamAtTime.getTeamInfo().getAwardGroup());
      cell.setBorderWidthTop(topBorderWidth);
      table.addCell(cell);

      cell = PdfUtils.createCell(teamAtTime.getTeamInfo().getOrganization());
      cell.setBorderWidthTop(topBorderWidth);
      table.addCell(cell);

      cell = PdfUtils.createCell(teamAtTime.getTeamInfo().getTeamName());
      cell.setBorderWidthTop(topBorderWidth);
      table.addCell(cell);

      cell = PdfUtils.createCell(teamAtTime.getSubjTime().getName());
      cell.setBorderWidthTop(topBorderWidth);
      table.addCell(cell);

      cell = PdfUtils.createCell(teamAtTime.getTeamInfo().getJudgingGroup());
      cell.setBorderWidthTop(topBorderWidth);
      table.addCell(cell);

      cell = PdfUtils.createCell(formatTime(time));
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

    @Override
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

    @Override
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

    @Override
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

      } // foreach subjective time

      for (final PerformanceTime performance : si.getAllPerformances()) {
        if (null != performance.getTime()) {
          if (null == minPerf
              || performance.getTime().isBefore(minPerf)) {
            minPerf = performance.getTime();
          }

          if (null == maxPerf
              || performance.getTime().isAfter(maxPerf)) {
            maxPerf = performance.getTime();
          }
        }
      }

    } // foreach team

    // print out the general schedule
    final Formatter output = new Formatter();
    final Set<String> stations = new HashSet<>();
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
   * @param performance the performance to add
   */
  private void addToMatches(final TeamScheduleInfo ti,
                            final PerformanceTime performance) {
    final Map<String, List<TeamScheduleInfo>> timeMatches;
    if (_matches.containsKey(performance.getTime())) {
      timeMatches = _matches.get(performance.getTime());
    } else {
      timeMatches = new HashMap<>();
      _matches.put(performance.getTime(), timeMatches);
    }

    final List<TeamScheduleInfo> tableMatches;
    if (timeMatches.containsKey(performance.getTable())) {
      tableMatches = timeMatches.get(performance.getTable());
    } else {
      tableMatches = new LinkedList<>();
      timeMatches.put(performance.getTable(), tableMatches);
    }

    tableMatches.add(ti);
  }

  private void removeFromMatches(final TeamScheduleInfo ti,
                                 final PerformanceTime performance) {

    if (!_matches.containsKey(performance.getTime())) {
      throw new IllegalArgumentException("Cannot find time info for "
          + performance.getTime()
          + " in matches");
    }
    final Map<String, List<TeamScheduleInfo>> timeMatches = _matches.get(performance.getTime());
    if (!timeMatches.containsKey(performance.getTable())) {
      throw new IllegalArgumentException("Cannot find table info for "
          + performance.getTime()
          + " in matches");
    }
    final List<TeamScheduleInfo> tableMatches = timeMatches.get(performance.getTable());
    if (!tableMatches.remove(ti)) {
      throw new IllegalArgumentException("Cannot find team "
          + ti.getTeamNumber()
          + " in the matches for round "
          + performance.getTime());
    }
  }

  /**
   * Find the opponent for a given team in a given round.
   *
   * @param ti the team schedule information
   * @param performance the {@link PerformanceTime} object to find the opponent
   *          for
   * @return the team number or null if no opponent
   */
  public TeamScheduleInfo findOpponent(final TeamScheduleInfo ti,
                                       final PerformanceTime performance) {
    final LocalTime performanceTime = performance.getTime();
    final String table = performance.getTable();
    final List<TeamScheduleInfo> tableMatches = _matches.get(performanceTime).get(table);
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

      final int teamNumber = Utilities.getIntegerNumberFormat().parse(teamNumberStr).intValue();
      final TeamScheduleInfo ti = new TeamScheduleInfo(teamNumber);
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

      // parse regular match play rounds
      for (int perfIndex = 0; perfIndex < ci.getNumPerfs(); ++perfIndex) {
        final String perf1Str = line[ci.getPerfColumn(perfIndex)];
        if (perf1Str.isEmpty()) {
          // If we got an empty string, then we must have hit the end
          return null;
        }
        final String table = line[ci.getPerfTableColumn(perfIndex)];
        final String[] tablePieces = table.split(" ");
        if (tablePieces.length != 2) {
          throw new RuntimeException("Error parsing table information from: "
              + table);
        }
        final LocalTime perf1Time = parseTime(perf1Str);

        final String tableName = tablePieces[0];
        final int tableSide = Utilities.getIntegerNumberFormat().parse(tablePieces[1]).intValue();
        final int roundNumber = perfIndex
            + 1;
        final PerformanceTime performance = new PerformanceTime(perf1Time, tableName, tableSide, false);

        ti.addPerformance(performance);
        if (performance.getSide() > 2
            || performance.getSide() < 1) {
          final String message = "There are only two sides to the table, number must be 1 or 2. team: "
              + ti.getTeamNumber()
              + " round "
              + roundNumber;
          LOGGER.error(message);
          throw new ScheduleParseException(message);
        }
      }

      // parse practice rounds
      for (int perfIndex = 0; perfIndex < ci.getNumPracticePerfs(); ++perfIndex) {
        final String perf1Str = line[ci.getPracticePerfColumn(perfIndex)];
        if (perf1Str.isEmpty()) {
          // If we got an empty string, then we must have hit the end
          return null;
        }
        final String table = line[ci.getPracticePerfTableColumn(perfIndex)];
        final String[] tablePieces = table.split(" ");
        if (tablePieces.length != 2) {
          throw new RuntimeException("Error parsing table information from: "
              + table);
        }
        final LocalTime perf1Time = parseTime(perf1Str);

        final String tableName = tablePieces[0];
        final int tableSide = Utilities.getIntegerNumberFormat().parse(tablePieces[1]).intValue();
        final int roundNumber = perfIndex
            + 1;
        final PerformanceTime performance = new PerformanceTime(perf1Time, tableName, tableSide, true);

        ti.addPerformance(performance);
        if (performance.getSide() > 2
            || performance.getSide() < 1) {
          final String message = "There are only two sides to the table, number must be 1 or 2. team: "
              + ti.getTeamNumber()
              + " practice round "
              + roundNumber;
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
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT COUNT(team_number) FROM schedule where tournament = ?")) {
      prep.setInt(1, tournamentID);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1) > 0;
        } else {
          return false;
        }
      } // ResultSet
    } // PreparedStatement
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
    // delete previous tournament schedule
    try (
        PreparedStatement deletePerfRounds = connection.prepareStatement("DELETE FROM sched_perf_rounds WHERE tournament = ?")) {
      deletePerfRounds.setInt(1, tournamentID);
      deletePerfRounds.executeUpdate();
    }

    try (
        PreparedStatement deleteSubjective = connection.prepareStatement("DELETE FROM sched_subjective WHERE tournament = ?")) {
      deleteSubjective.setInt(1, tournamentID);
      deleteSubjective.executeUpdate();
    }

    try (PreparedStatement deleteSchedule = connection.prepareStatement("DELETE FROM schedule WHERE tournament = ?")) {
      deleteSchedule.setInt(1, tournamentID);
      deleteSchedule.executeUpdate();
    }

    // insert new tournament schedule
    try (PreparedStatement insertSchedule = connection.prepareStatement("INSERT INTO schedule"//
        + " (tournament, team_number, judging_station)"//
        + " VALUES(?, ?, ?)");
        PreparedStatement insertPerfRounds = connection.prepareStatement("INSERT INTO sched_perf_rounds"//
            + " (tournament, team_number, practice, perf_time, table_color, table_side)"//
            + " VALUES(?, ?, ?, ?, ?, ?)");
        PreparedStatement insertSubjective = connection.prepareStatement("INSERT INTO sched_subjective" //
            + " (tournament, team_number, name, subj_time)" //
            + " VALUES(?, ?, ?, ?)")) {

      insertSchedule.setInt(1, tournamentID);

      insertPerfRounds.setInt(1, tournamentID);

      insertSubjective.setInt(1, tournamentID);

      for (final TeamScheduleInfo si : getSchedule()) {
        insertSchedule.setInt(2, si.getTeamNumber());
        insertSchedule.setString(3, si.getJudgingGroup());
        insertSchedule.executeUpdate();

        insertPerfRounds.setInt(2, si.getTeamNumber());
        for (final PerformanceTime performance : si.getAllPerformances()) {
          insertPerfRounds.setBoolean(3, performance.isPractice());
          insertPerfRounds.setTime(4, Time.valueOf(performance.getTime()));
          insertPerfRounds.setString(5, performance.getTable());
          insertPerfRounds.setInt(6, performance.getSide());
          insertPerfRounds.executeUpdate();
        }

        for (final SubjectiveTime subjectiveTime : si.getSubjectiveTimes()) {
          insertSubjective.setInt(2, si.getTeamNumber());
          insertSubjective.setString(3, subjectiveTime.getName());
          insertSubjective.setTime(4, Time.valueOf(subjectiveTime.getTime()));
          insertSubjective.executeUpdate();
        }
      } // foreach team
    }

  }

  /**
   * Find teams that are in this schedule and not in the database.
   *
   * @param connection the database connection
   * @param tournamentID the tournament to check
   * @return teams not in the database, empty if all teams are in the database
   * @throws SQLException on a database error
   */
  public Collection<TeamScheduleInfo> findTeamsNotInDatabase(final Connection connection,
                                                             final int tournamentID)
      throws SQLException {
    final Collection<TeamScheduleInfo> missingTeams = new LinkedList<>();
    final Map<Integer, TournamentTeam> dbTeams = Queries.getTournamentTeams(connection, tournamentID);

    for (final TeamScheduleInfo si : _schedule) {
      if (!dbTeams.containsKey(si.getTeamNumber())) {
        missingTeams.add(si);
      }
    }
    return missingTeams;
  }

  /**
   * Check if the current schedule is consistent with the specified tournament
   * in the database.
   *
   * @param connection the database connection
   * @param tournamentID the tournament to check
   * @return the constraint violations, empty if no violations
   * @throws SQLException on a database error
   */
  public Collection<ConstraintViolation> compareWithDatabase(final Connection connection,
                                                             final int tournamentID)
      throws SQLException {
    final Collection<ConstraintViolation> violations = new LinkedList<>();
    final Map<Integer, TournamentTeam> dbTeams = Queries.getTournamentTeams(connection, tournamentID);
    final Set<Integer> scheduleTeamNumbers = new HashSet<>();
    for (final TeamScheduleInfo si : _schedule) {
      scheduleTeamNumbers.add(si.getTeamNumber());
      if (!dbTeams.containsKey(si.getTeamNumber())) {
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

    private final int[] practiceColumn;

    public int getNumPracticePerfs() {
      return practiceColumn.length;
    }

    public int getPracticePerfColumn(final int round) {
      return practiceColumn[round];
    }

    private final int[] practiceTableColumn;

    public int getPracticePerfTableColumn(final int round) {
      return practiceTableColumn[round];
    }

    public ColumnInformation(final String[] headerLine,
                             final int teamNumColumn,
                             final int organizationColumn,
                             final int teamNameColumn,
                             final int divisionColumn,
                             final Map<Integer, String> subjectiveColumns,
                             final int judgeGroupColumn,
                             final int[] perfColumn,
                             final int[] perfTableColumn,
                             final int[] practiceColumn,
                             final int[] practiceTableColumn) {
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

      this.practiceColumn = new int[practiceColumn.length];
      System.arraycopy(practiceColumn, 0, this.practiceColumn, 0, practiceColumn.length);
      this.practiceTableColumn = new int[practiceTableColumn.length];
      System.arraycopy(practiceTableColumn, 0, this.practiceTableColumn, 0, practiceTableColumn.length);

      // determine which columns aren't used
      final List<String> unused = new LinkedList<>();
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

        for (final int pc : this.practiceColumn) {
          if (pc == column) {
            match = true;
          }
        }
        for (final int ptc : this.practiceTableColumn) {
          if (ptc == column) {
            match = true;
          }
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
   * @param si the team to rescheduled
   * @param oldTime the old time that the team was scheduled at
   * @param perfTime the new performance information
   */
  public void reassignTable(final TeamScheduleInfo si,
                            final LocalTime oldTime,
                            final PerformanceTime perfTime) {
    final PerformanceTime oldPerformanceTime = si.getPerformanceAtTime(oldTime);
    if (null == oldPerformanceTime) {
      throw new IllegalArgumentException("Team "
          + si.getTeamNumber()
          + " does not have a performance at "
          + oldTime);
    }

    removeFromMatches(si, oldPerformanceTime);
    si.removePerformance(oldPerformanceTime);

    si.addPerformance(perfTime);
    addToMatches(si, perfTime);
  }

  /**
   * Write out to the specified writer.
   *
   * @param outputWriter
   * @throws IOException
   */
  public void writeToCSV(final Writer outputWriter) throws IOException {
    try (final CSVWriter csv = new CSVWriter(outputWriter)) {

      final List<String> line = new ArrayList<>();
      line.add(TournamentSchedule.TEAM_NUMBER_HEADER);
      line.add(TournamentSchedule.AWARD_GROUP_HEADER);
      line.add(TournamentSchedule.TEAM_NAME_HEADER);
      line.add(TournamentSchedule.ORGANIZATION_HEADER);
      line.add(TournamentSchedule.JUDGE_GROUP_HEADER);
      final List<String> categories = Collections.unmodifiableList(new LinkedList<>(getSubjectiveStations()));
      for (final String category : categories) {
        line.add(category);
      }
      for (int round = 0; round < getNumberOfPracticeRounds(); ++round) {
        line.add(String.format(TournamentSchedule.PRACTICE_HEADER_FORMAT, round
            + 1));
        line.add(String.format(TournamentSchedule.PRACTICE_TABLE_HEADER_FORMAT, round
            + 1));
      }

      for (int round = 0; round < getNumberOfRegularMatchPlayRounds(); ++round) {
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

        si.enumeratePracticePerformances().forEachOrdered(pair -> {
          final PerformanceTime p = pair.getLeft();
          line.add(TournamentSchedule.formatTime(p.getTime()));
          line.add(p.getTable()
              + " "
              + p.getSide());
        });

        si.enumerateRegularMatchPlayPerformances().forEachOrdered(pair -> {
          final PerformanceTime p = pair.getLeft();
          line.add(TournamentSchedule.formatTime(p.getTime()));
          line.add(p.getTable()
              + " "
              + p.getSide());
        });

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

  /**
   * Write the schedule as a CSV file to the specified stream.
   *
   * @param stream where to write the schedule
   * @throws IOException if there is a problem writing to the stream
   */
  public void outputScheduleAsCSV(final OutputStream stream) throws IOException {
    try (OutputStreamWriter outputWriter = new OutputStreamWriter(stream, Utilities.DEFAULT_CHARSET)) {
      writeToCSV(outputWriter);
    }
  }

}
