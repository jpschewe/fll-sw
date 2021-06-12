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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.text.ParseException;
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
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nonnull;

import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

import com.opencsv.CSVWriter;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Team;
import fll.Tournament;
import fll.TournamentTeam;
import fll.Utilities;
import fll.db.Queries;
import fll.documents.writers.SubjectivePdfWriter;
import fll.util.CellFileReader;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
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
  public static final float TIME_SEPARATOR_LINE_WIDTH = 2f;

  /**
   * Header on team number column.
   */
  public static final String TEAM_NUMBER_HEADER = "Team #";

  /**
   * Table header text for team name.
   */
  public static final String TEAM_NAME_HEADER = "Team Name";

  /**
   * Table header text for organization.
   */
  public static final String ORGANIZATION_HEADER = "Organization";

  /**
   * Should use AWARD_GROUP_HEADER now, only kept for old schedules
   */
  @Deprecated
  private static final String DIVISION_HEADER = "Div";

  /**
   * Table header for award group.
   */
  public static final String AWARD_GROUP_HEADER = "Award Group";

  /**
   * Table header for judging group.
   */
  public static final String JUDGE_GROUP_HEADER = "Judging Group";

  /**
   * Performance round header without the number.
   */
  public static final String BASE_PERF_HEADER = "Perf #";

  /**
   * Practice round header without the number.
   */
  private static final String BASE_PRACTICE_HEADER = "Practice #";

  private static final String BASE_PRACTICE_HEADER_SHORT = "Practice";

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

  /**
   * Used with {@link String#format(String, Object...)} to create a practice
   * table header.
   */
  public static final String PRACTICE_TABLE_HEADER_FORMAT = "Practice %d Table";

  private static final String PRACTICE_TABLE_HEADER_FORMAT_SHORT = "Practice Table";

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
  public static @Nullable LocalTime parseTime(final @Nullable String str) throws DateTimeParseException {
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
  public static String formatTime(final @Nullable LocalTime time) {
    if (null == time) {
      return "";
    } else {
      return time.format(OUTPUT_TIME_FORMAT);
    }
  }

  // time->table->team
  private final HashMap<LocalTime, Map<String, List<TeamScheduleInfo>>> matches = new HashMap<>();

  /* package */Map<LocalTime, Map<String, List<TeamScheduleInfo>>> getMatches() {
    // TODO should make read-only somehow
    return matches;
  }

  private final HashSet<String> tableColors = new HashSet<>();

  /**
   * @return colors of the tables (unmodifiable)
   */
  public Set<String> getTableColors() {
    return Collections.unmodifiableSet(tableColors);
  }

  private final HashSet<String> awardGroups = new HashSet<>();

  /**
   * @return award groups (unmodifiable)
   */
  public Set<String> getAwardGroups() {
    return Collections.unmodifiableSet(awardGroups);
  }

  private final HashSet<String> judgingGroups = new HashSet<>();

  /**
   * @return judging groups (unmodifiable)
   */
  public Set<String> getJudgingGroups() {
    return Collections.unmodifiableSet(judgingGroups);
  }

  private final LinkedList<TeamScheduleInfo> schedule = new LinkedList<>();

  private final HashSet<String> subjectiveStations = new HashSet<>();

  private final String name;

  /**
   * @return Name of this tournament
   */
  public String getName() {
    return name;
  }

  /**
   * @return The list of subjective stations for this schedule.
   */
  public Set<String> getSubjectiveStations() {
    return Collections.unmodifiableSet(subjectiveStations);
  }

  /**
   * @return An unmodifiable copy of the schedule.
   */
  public List<TeamScheduleInfo> getSchedule() {
    return Collections.unmodifiableList(schedule);
  }

  /**
   * Get the {@link TeamScheduleInfo} for the specified team number.
   *
   * @param teamNumber the team to get the schedule info for
   * @return null if cannot be found
   */
  public @Nullable TeamScheduleInfo getSchedInfoForTeam(final int teamNumber) {
    for (final TeamScheduleInfo si : schedule) {
      if (si.getTeamNumber() == teamNumber) {
        return si;
      }
    }
    return null;
  }

  /**
   * @param name {@link #getName()}
   * @param reader where to read the schedule from
   * @param subjectiveHeaders the headers for the subjective columns
   * @throws IOException if there is an error reading the file
   * @throws ScheduleParseException if there is an error parsing the schedule
   * @throws ParseException if there is an error parsing the file
   */
  public TournamentSchedule(final String name,
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
   * Empty tournament schedule.
   */
  public TournamentSchedule() {
    numRegularMatchPlayRounds = 0;
    numPracticeRounds = 0;
    name = "empty";
  }

  /**
   * Load a tournament from the database.
   *
   * @param connection database connection
   * @param tournamentID the tournament to load
   * @throws SQLException on a database error
   */
  public TournamentSchedule(final Connection connection,
                            final int tournamentID)
      throws SQLException {
    final Tournament currentTournament = Tournament.findTournamentByID(connection, tournamentID);
    if (null == currentTournament) {
      throw new IllegalArgumentException("Unable to find tournament with id: "
          + tournamentID);
    }

    name = currentTournament.getName();

    try (
        PreparedStatement getSubjectiveStations = connection.prepareStatement("SELECT DISTINCT name from sched_subjective WHERE tournament = ?")) {
      getSubjectiveStations.setInt(1, tournamentID);
      try (ResultSet stations = getSubjectiveStations.executeQuery()) {
        while (stations.next()) {
          final String name = castNonNull(stations.getString(1));
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
          final String judgingStation = castNonNull(sched.getString(2));

          final TeamScheduleInfo ti = new TeamScheduleInfo(teamNumber);
          ti.setJudgingGroup(judgingStation);

          getSubjective.setInt(2, teamNumber);
          try (ResultSet subjective = getSubjective.executeQuery()) {
            while (subjective.next()) {
              final String name = castNonNull(subjective.getString(1));
              final Time subjTime = castNonNull(subjective.getTime(2));
              ti.addSubjectiveTime(new SubjectiveTime(name, subjTime.toLocalTime()));
            }
          }

          getPerfRounds.setInt(2, teamNumber);
          try (ResultSet perfRounds = getPerfRounds.executeQuery()) {
            while (perfRounds.next()) {
              final LocalTime perfTime = castNonNull(perfRounds.getTime(1)).toLocalTime();
              final String tableColor = castNonNull(perfRounds.getString(2));
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
          if (null == eventDivision) {
            throw new FLLRuntimeException("Unable to find event division for "
                + teamNumber);
          }

          ti.setDivision(eventDivision);

          final Team team = Team.getTeamFromDatabase(connection, teamNumber);
          ti.setOrganization(team.getOrganization());
          ti.setTeamName(team.getTeamName());

          cacheTeamScheduleInformation(ti);
        } // foreach sched result

      } // allocate sched ResultSet

    } // allocate prepared statements

    if (!schedule.isEmpty()) {
      this.numRegularMatchPlayRounds = schedule.get(0).getNumRegularMatchPlayRounds();
      this.numPracticeRounds = schedule.get(0).getNumPracticeRounds();
      validateRounds();
    } else {
      this.numRegularMatchPlayRounds = 0;
      this.numPracticeRounds = 0;
    }

  }

  private void validateRounds(@UnderInitialization(fll.scheduler.TournamentSchedule.class) TournamentSchedule this) {
    for (final TeamScheduleInfo si : schedule) {
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
  @SuppressFBWarnings(value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", justification = "https://github.com/spotbugs/spotbugs/issues/927")
  private static boolean isHeaderLine(final @Nullable String[] line) {
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
   * @param reader where to read the columns from
   * @param subjectiveHeaders the known subjective headers
   * @return the column information
   * @throws IOException if there is an error reading the data
   * @throws RuntimeException if a header row cannot be found
   */
  public static ColumnInformation findColumns(final CellFileReader reader,
                                              final Collection<String> subjectiveHeaders)
      throws IOException {
    while (true) {
      final @Nullable String @Nullable [] line = reader.readNext();
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
  @SuppressFBWarnings(value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", justification = "https://github.com/spotbugs/spotbugs/issues/927")
  private static int countNumRegularMatchPlayRounds(final @Nullable String[] line) {
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

  @SuppressFBWarnings(value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", justification = "https://github.com/spotbugs/spotbugs/issues/927")
  private static int countNumPracticeRounds(final @Nullable String[] line) {
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

  private static boolean checkHeaderExists(final @Nullable String[] line,
                                           final String header) {
    return null != columnForHeader(line, header);
  }

  /**
   * Find the column that contains the specified header.
   *
   * @return the column, null if not found
   */
  @SuppressFBWarnings(value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", justification = "https://github.com/spotbugs/spotbugs/issues/927")
  private static @Nullable Integer columnForHeader(final @Nullable String[] line,
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
  private static int getColumnForHeader(final @Nullable String[] line,
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
                                               final @Nullable String[] line) {
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
  private void parseData(@UnderInitialization(TournamentSchedule.class) TournamentSchedule this,
                         final CellFileReader reader,
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
  private void addToSchedule(@UnderInitialization(TournamentSchedule.class) TournamentSchedule this,
                             final TeamScheduleInfo ti) {
    if (!schedule.contains(ti)) {
      schedule.add(ti);
    } else {
      LOGGER.warn("Attempting to add the same team to the schedule twice: "
          + ti.getTeamNumber());
    }
  }

  /**
   * Populate internal caches with the data from this newly created
   * {@link TeamScheduleInfo}.
   */
  private void cacheTeamScheduleInformation(@UnderInitialization(TournamentSchedule.class) TournamentSchedule this,
                                            final TeamScheduleInfo ti) {
    addToSchedule(ti);

    // keep track of some meta information
    ti.allPerformances().forEach(performance -> {
      tableColors.add(performance.getTable());
      addToMatches(ti, performance);
    });

    awardGroups.add(ti.getAwardGroup());
    judgingGroups.add(ti.getJudgingGroup());
  }

  /**
   * Output the detailed schedule.
   *
   * @param params schedule parameters
   * @param directory the directory to put the files in
   * @param baseFilename the base filename
   * @throws IOException if there is an error writing the schedules
   * @throws IllegalArgumentException if directory doesn't exist and can't be
   *           created or exists and isn't a directory
   */
  public void outputDetailedSchedules(final SchedParams params,
                                      final File directory,
                                      final String baseFilename)
      throws IOException, IllegalArgumentException {
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
      ScheduleWriter.outputSubjectiveSchedulesByJudgingStation(this, pdfFos);
    }

    final File byCategory = new File(directory, baseFilename
        + "-subjective-by-category.pdf");
    try (OutputStream pdfFos = new FileOutputStream(byCategory)) {
      ScheduleWriter.outputSubjectiveSchedulesByCategory(this, pdfFos);
    }

    final File byTime = new File(directory, baseFilename
        + "-subjective-by-time.pdf");
    try (OutputStream pdfFos = new FileOutputStream(byTime)) {
      ScheduleWriter.outputSubjectiveSchedulesByTimeOnly(this, pdfFos);
    }

    final File performance = new File(directory, baseFilename
        + "-performance.pdf");
    try (OutputStream pdfFos = new BufferedOutputStream(new FileOutputStream(performance))) {
      ScheduleWriter.outputPerformanceScheduleByTime(this, pdfFos);
    }

    final File teamSchedules = new File(directory, baseFilename
        + "-team-schedules.pdf");
    try (OutputStream pdfFos = new FileOutputStream(teamSchedules)) {
      ScheduleWriter.outputTeamSchedules(this, params, pdfFos);
    }
  }

  /**
   * @param dir where to write the files
   * @param baseFileName the base name of the files
   * @param description the challenge description
   * @param categoryToSchedule mapping of ScoreCategories to schedule columns
   * @param tournamentName the name of the tournament to display on the sheets
   * @param filenameSuffixes map score category to output filename suffixes, may
   *          be empty
   * @throws IOException if there is an error writing the files
   */
  public void outputSubjectiveSheets(@Nonnull final String tournamentName,
                                     final String dir,
                                     final String baseFileName,
                                     final ChallengeDescription description,
                                     @Nonnull final Map<ScoreCategory, String> categoryToSchedule,
                                     @Nonnull final Map<ScoreCategory, @Nullable String> filenameSuffixes)
      throws IOException {

    // setup the sheets from the sucked in xml
    for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
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
        Collections.sort(schedule, new SubjectiveComparatorByAwardGroup(subjectiveStation));
      }

      final String schedulerColumn = categoryToSchedule.get(category);

      try (OutputStream stream = new FileOutputStream(filename)) {
        SubjectivePdfWriter.createDocumentForSchedule(stream, description, tournamentName, category, schedulerColumn,
                                                      schedule);
      }
    }
  }

  /**
   * Sheets are sorted by table and then by time.
   *
   * @param tournamentName the name of the tournament to put in the sheets
   * @param output where to output
   * @param description where to get the goals from
   * @throws SQLException if there is an error talking to the database
   * @throws IOException if there is an error writing to the stream
   */
  public void outputPerformanceSheets(@Nonnull final String tournamentName,
                                      @Nonnull final OutputStream output,
                                      @Nonnull final ChallengeDescription description)
      throws SQLException, IOException {
    final ScoresheetGenerator scoresheets = new ScoresheetGenerator(getTotalNumberOfRounds()
        * schedule.size(), description, tournamentName);
    final SortedMap<PerformanceTime, TeamScheduleInfo> performanceTimes = new TreeMap<>(PerformanceTime.ByTableThenTime.INSTANCE);
    for (final TeamScheduleInfo si : schedule) {
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

    scoresheets.writeFile(output);
  }

  /**
   * Comparator for for sorting by the specified award group.
   */
  public static class SubjectiveComparatorByAwardGroup implements Comparator<TeamScheduleInfo>, Serializable {
    private final String name;

    /**
     * @param name the award group to sort by
     */
    public SubjectiveComparatorByAwardGroup(final String name) {
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
  public static class SubjectiveComparatorByTime implements Comparator<TeamScheduleInfo>, Serializable {
    private final String name;

    /**
     * @param name the subjective category to sort by time
     */
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
  public static final class ComparatorByTeam implements Comparator<TeamScheduleInfo>, Serializable {

    /**
     * Singleton instance.
     */
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
   * @return the general schedule as a string.
   */
  public String computeGeneralSchedule() {
    LocalTime minPerf = null;
    LocalTime maxPerf = null;
    // division -> date
    final Map<String, LocalTime> minSubjectiveTimes = new HashMap<>();
    final Map<String, LocalTime> maxSubjectiveTimes = new HashMap<>();

    for (final TeamScheduleInfo si : schedule) {
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
      final LocalTime latestEnd = null == latestStart ? null : latestStart.plus(subjectiveDuration);

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
  @RequiresNonNull("this.matches")
  private void addToMatches(@UnknownInitialization TournamentSchedule this,
                            final TeamScheduleInfo ti,
                            final PerformanceTime performance) {
    final Map<String, List<TeamScheduleInfo>> timeMatches;
    if (matches.containsKey(performance.getTime())) {
      timeMatches = castNonNull(matches.get(performance.getTime()));
    } else {
      timeMatches = new HashMap<>();
      matches.put(performance.getTime(), timeMatches);
    }

    final List<TeamScheduleInfo> tableMatches;
    if (timeMatches.containsKey(performance.getTable())) {
      tableMatches = castNonNull(timeMatches.get(performance.getTable()));
    } else {
      tableMatches = new LinkedList<>();
      timeMatches.put(performance.getTable(), tableMatches);
    }

    tableMatches.add(ti);
  }

  private void removeFromMatches(final TeamScheduleInfo ti,
                                 final PerformanceTime performance) {

    if (!matches.containsKey(performance.getTime())) {
      throw new IllegalArgumentException("Cannot find time info for "
          + performance.getTime()
          + " in matches");
    }
    final Map<String, List<TeamScheduleInfo>> timeMatches = castNonNull(matches.get(performance.getTime()));
    if (!timeMatches.containsKey(performance.getTable())) {
      throw new IllegalArgumentException("Cannot find table info for "
          + performance.getTime()
          + " in matches");
    }
    final List<TeamScheduleInfo> tableMatches = castNonNull(timeMatches.get(performance.getTable()));
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
  public @Nullable TeamScheduleInfo findOpponent(final TeamScheduleInfo ti,
                                                 final PerformanceTime performance) {
    final LocalTime performanceTime = performance.getTime();
    final String table = performance.getTable();
    final List<TeamScheduleInfo> tableMatches = matches.getOrDefault(performanceTime, Collections.emptyMap())
                                                       .getOrDefault(table, Collections.emptyList());
    if (tableMatches.size() > 1) {
      if (tableMatches.get(0).equals(tableMatches.get(1))) {
        throw new FLLRuntimeException("Internal error, matches is inconsistent. Has team competing against itself");
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
  private @Nullable TeamScheduleInfo parseLine(@UnderInitialization(TournamentSchedule.class) TournamentSchedule this,
                                               final CellFileReader reader,
                                               final ColumnInformation ci)
      throws IOException, ParseException, ScheduleParseException {
    final @Nullable String @Nullable [] line = reader.readNext();
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
        if (null == str
            || str.isEmpty()) {
          // If we got an empty string, then we must have hit the end
          return null;
        }
        // str is not null or empty, so parseTime cannot return null
        final LocalTime time = castNonNull(parseTime(str));
        ti.addSubjectiveTime(new SubjectiveTime(station, time));
      }

      ti.setJudgingGroup(line[ci.getJudgeGroupColumn()]);

      // parse regular match play rounds
      for (int perfIndex = 0; perfIndex < ci.getNumPerfs(); ++perfIndex) {
        final String perf1Str = line[ci.getPerfColumn(perfIndex)];
        if (null == perf1Str
            || perf1Str.isEmpty()) {
          // If we got an empty string, then we must have hit the end
          return null;
        }

        final String table = line[ci.getPerfTableColumn(perfIndex)];
        if (null == table
            || table.isEmpty()) {
          // If we got an empty string, then we must have hit the end
          return null;
        }

        final String[] tablePieces = table.split(" ");
        if (tablePieces.length != 2) {
          throw new RuntimeException("Error parsing table information from: "
              + table);
        }
        final LocalTime perf1Time = castNonNull(parseTime(perf1Str));

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
        if (null == perf1Str
            || perf1Str.isEmpty()) {
          // If we got an empty string, then we must have hit the end
          return null;
        }
        final String table = line[ci.getPracticePerfTableColumn(perfIndex)];
        if (null == table
            || table.isEmpty()) {
          // If we got an empty string, then we must have hit the end
          return null;
        }

        final String[] tablePieces = table.split(" ");
        if (tablePieces.length != 2) {
          throw new RuntimeException("Error parsing table information from: "
              + table);
        }
        final LocalTime perf1Time = parseTime(perf1Str);
        if (null == perf1Time) {
          return null;
        }

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
   * @throws SQLException on a database error
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
   * @param connection where to store the schedule
   * @param tournamentID the ID of the tournament
   * @throws SQLException on a database error
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

    for (final TeamScheduleInfo si : schedule) {
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
    for (final TeamScheduleInfo si : schedule) {
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
   * A -1 value for a column means that it was not found.
   */
  public static final class ColumnInformation {

    private final List<String> headerLine;

    /**
     * @return The columns that were parsed into the headers in the same order that
     *         they appear in the schedule.
     */
    public List<String> getHeaderLine() {
      return headerLine;
    }

    private final int teamNumColumn;

    /**
     * @return column that contains the team number
     */
    public int getTeamNumColumn() {
      return teamNumColumn;
    }

    private final int organizationColumn;

    /**
     * @return column that contains the organization
     */
    public int getOrganizationColumn() {
      return organizationColumn;
    }

    private final int teamNameColumn;

    /**
     * @return column that contains the team name
     */
    public int getTeamNameColumn() {
      return teamNameColumn;
    }

    private final int divisionColumn;

    /**
     * @return column for division (award group)
     */
    public int getDivisionColumn() {
      return divisionColumn;
    }

    private final Map<Integer, String> subjectiveColumns;

    /**
     * @return key is column index, value is the subjective judging station
     */
    public Map<Integer, String> getSubjectiveColumnInfo() {
      return subjectiveColumns;
    }

    private final int judgeGroupColumn;

    /**
     * @return column for judge
     */
    public int getJudgeGroupColumn() {
      return judgeGroupColumn;
    }

    private final int[] perfColumn;

    /**
     * @return number of performance rounds
     */
    public int getNumPerfs() {
      return perfColumn.length;
    }

    /**
     * @param round the performance round to get the column for
     * @return the column for the performance round time
     */
    public int getPerfColumn(final int round) {
      return perfColumn[round];
    }

    private final int[] perfTableColumn;

    /**
     * @param round the performance round to get the column for
     * @return the column for the performance round table
     */
    public int getPerfTableColumn(final int round) {
      return perfTableColumn[round];
    }

    private final int[] practiceColumn;

    /**
     * @return number of practice rounds
     */
    public int getNumPracticePerfs() {
      return practiceColumn.length;
    }

    /**
     * @param round the practice round to get the column for
     * @return the column for the practice round time
     */
    public int getPracticePerfColumn(final int round) {
      return practiceColumn[round];
    }

    private final int[] practiceTableColumn;

    /**
     * @param round the practice round to get the column for
     * @return the column for the practice round table
     */
    public int getPracticePerfTableColumn(final int round) {
      return practiceTableColumn[round];
    }

    /**
     * @param headerLine {@link #getHeaderLine()}
     * @param teamNumColumn {@link #getTeamNumColumn()}
     * @param organizationColumn {@link #getOrganizationColumn()}
     * @param teamNameColumn {@link #getTeamNameColumn()}
     * @param divisionColumn {@link #getDivisionColumn()}
     * @param subjectiveColumns {@link #getSubjectiveColumnInfo()}
     * @param judgeGroupColumn {@link #getJudgeGroupColumn()}
     * @param perfColumn {@link #getPerfColumn(int)}
     * @param perfTableColumn {@link #getPerfTableColumn(int)}
     * @param practiceColumn {@link #getPracticePerfColumn(int)}
     * @param practiceTableColumn {@link #getPracticePerfTableColumn(int)}
     */
    @SuppressFBWarnings(value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", justification = "https://github.com/spotbugs/spotbugs/issues/927")
    public ColumnInformation(final @Nullable String[] headerLine,
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
   * @param team the team to get the schedule information for
   * @return the schedule info or null if not found
   */
  public @Nullable TeamScheduleInfo findScheduleInfo(final int team) {
    for (final TeamScheduleInfo si : schedule) {
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
   * @param outputWriter where to write
   * @throws IOException on an error writing
   */
  public void writeToCSV(final Writer outputWriter) throws IOException {
    try (CSVWriter csv = new CSVWriter(outputWriter)) {

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
          final SubjectiveTime stime = si.getSubjectiveTimeByName(category);
          if (null != stime) {
            final LocalTime d = stime.getTime();
            line.add(TournamentSchedule.formatTime(d));
          }
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
   * @throws IOException if there is an error writing to the file
   */
  public void writeToCSV(final File outputFile) throws IOException {
    try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile), Utilities.DEFAULT_CHARSET)) {
      writeToCSV(writer);
    }
  }

  /**
   * Thrown for a missing column.
   */
  public static class MissingColumnException extends FLLRuntimeException {
    /**
     * @param message {@link #getMessage()}
     */
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
