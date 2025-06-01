/*
 * Copyright (c) 2000-2008 HighTechKids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.scheduler;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

import com.opencsv.CSVWriter;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Tournament;
import fll.TournamentTeam;
import fll.Utilities;
import fll.db.CategoryColumnMapping;
import fll.db.Queries;
import fll.db.RunMetadata;
import fll.documents.writers.SubjectivePdfWriter;
import fll.util.CellFileReader;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.TournamentData;
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
   * Table header for award group.
   */
  public static final String AWARD_GROUP_HEADER = "Award Group";

  /**
   * Table header for judging group.
   */
  public static final String JUDGE_GROUP_HEADER = "Judging Group";

  /**
   * Table header for wave.
   */
  public static final String WAVE_HEADER = "Wave";

  /**
   * Performance round header without the number.
   */
  public static final String BASE_PERF_HEADER = "Perf #";

  /**
   * Practice round header without the number.
   */
  private static final String BASE_PRACTICE_HEADER = "Practice #";

  /**
   * Simple string for a single practice round. No formatting needed.
   */
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

  /**
   * Used with {@link String#format(String, Object...)} to create a practice
   * table header.
   */
  public static final String PRACTICE_TABLE_HEADER_FORMAT = "Practice %d Table";

  /**
   * Simple string for a single practice round table. No formatting needed.
   */
  public static final String PRACTICE_TABLE_HEADER_FORMAT_SHORT = "Practice Table";

  /**
   * @return Number of performance rounds in this schedule
   */
  public int getTotalNumberOfRounds() {
    return numRegularMatchPlayRounds;
  }

  private final int numRegularMatchPlayRounds;

  /**
   * Always output without 24-hour time and without AM/PM.
   */
  private static final DateTimeFormatter OUTPUT_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm");

  /**
   * Use AM/PM for human readable dates.
   */
  private static final DateTimeFormatter HUMAN_OUTPUT_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");

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
    if (StringUtils.isBlank(str)) {
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
   * Convert the time to a string that will be parsed by
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

  /**
   * Format a time for humans to read using hours, minutes and AM/PM.
   * 
   * @param time the time to format
   * @return the formatted time, null converts to ""
   */
  public static String humanFormatTime(final @Nullable LocalTime time) {
    if (null == time) {
      return "";
    } else {
      return time.format(HUMAN_OUTPUT_TIME_FORMAT);
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
   * Read the data in {@code reader} from the header row until the team number
   * column is empty.
   * 
   * @param name {@link #getName()}
   * @param reader where to read the schedule from
   * @param columnInfo mapping of columns from the file to needed data
   * @throws IOException if there is an error reading the file
   * @throws ScheduleParseException if there is an error parsing the schedule
   * @throws ParseException if there is an error parsing the file
   */
  public TournamentSchedule(final String name,
                            final CellFileReader reader,
                            final ColumnInformation columnInfo)
      throws IOException, ParseException, ScheduleParseException {
    this.name = name;
    numRegularMatchPlayRounds = columnInfo.getNumPerfs();
    parseData(reader, columnInfo);
    reader.close();
    this.subjectiveStations.clear();
    this.subjectiveStations.addAll(columnInfo.getSubjectiveStationNames());
  }

  /**
   * Empty tournament schedule.
   */
  public TournamentSchedule() {
    numRegularMatchPlayRounds = 0;
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

    try (PreparedStatement getSched = connection.prepareStatement("SELECT team_number"
        + " FROM schedule" //
        + " WHERE schedule.tournament = ?");

        PreparedStatement getPerfRounds = connection.prepareStatement("SELECT perf_time, table_color, table_side" //
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
          final TournamentTeam team = TournamentTeam.getTournamentTeamFromDatabase(connection, currentTournament,
                                                                                   teamNumber);
          final TeamScheduleInfo ti = new TeamScheduleInfo(team);

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
              final PerformanceTime performance = new PerformanceTime(perfTime, tableColor, tableSide);
              ti.addPerformance(performance);
            } // foreach performance round
          } // allocate performance ResultSet

          cacheTeamScheduleInformation(ti);
        } // foreach sched result

      } // allocate sched ResultSet

    } // allocate prepared statements

    try (
        PreparedStatement waveCheckin = connection.prepareStatement("SELECT wave, checkin_time FROM schedule_wave_checkin WHERE tournament = ?")) {
      waveCheckin.setInt(1, tournamentID);
      final Collection<WaveCheckin> waveCheckins = new LinkedList<>();
      try (ResultSet rs = waveCheckin.executeQuery()) {
        while (rs.next()) {
          final String waveDb = castNonNull(rs.getString(1));
          final LocalTime checkin = castNonNull(rs.getTime(2)).toLocalTime();
          waveCheckins.add(new WaveCheckin(waveDb.equals(NULL_WAVE_DB_VALUE) ? null : waveDb, checkin));
        }
      }
      this.waveCheckinTimes.addAll(waveCheckins);
    }

    if (!schedule.isEmpty()) {
      this.numRegularMatchPlayRounds = schedule.get(0).getNumRegularMatchPlayRounds();
      validateRounds();
    } else {
      this.numRegularMatchPlayRounds = 0;
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
   * Parse the data of the schedule.
   *
   * @throws IOException on an error reading the file
   * @throws ScheduleParseException if there is an error with the schedule
   */
  @RequiresNonNull("matches")
  private void parseData(@UnderInitialization(TournamentSchedule.class) TournamentSchedule this,
                         final CellFileReader reader,
                         final ColumnInformation ci)
      throws IOException, ParseException, ScheduleParseException {
    reader.skipRows(ci.getHeaderRowIndex()
        + 1);
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
      throw new FLLRuntimeException("Attempting to add the same team to the schedule twice: "
          + ti.getTeamNumber());
    }
  }

  /**
   * Populate internal caches with the data from this newly created
   * {@link TeamScheduleInfo}.
   */
  @RequiresNonNull("matches")
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
   * @param dir where to write the files
   * @param baseFileName the base name of the files
   * @param description the challenge description
   * @param categoryToSchedule mapping of ScoreCategories to schedule columns
   * @param tournamentName the name of the tournament to display on the sheets
   * @param filenameSuffixes map score category to output filename suffixes, may
   *          be empty
   * @throws IOException if there is an error writing the files
   */
  public void outputSubjectiveSheets(final String tournamentName,
                                     final String dir,
                                     final String baseFileName,
                                     final ChallengeDescription description,
                                     final Map<ScoreCategory, Collection<String>> categoryToSchedule,
                                     final Map<ScoreCategory, @Nullable String> filenameSuffixes)
      throws IOException {

    // setup the sheets from the sucked in xml
    for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
      final Collection<String> scheduleColumns = categoryToSchedule.get(category);
      final String suffix = filenameSuffixes.get(category);
      if (null == scheduleColumns) {
        final String filename = dir
            + File.separator
            + baseFileName
            + "-"
            + category.getName()
            + (null != suffix ? "_"
                + suffix : "")
            + ".pdf";
        try (OutputStream stream = new FileOutputStream(filename)) {
          SubjectivePdfWriter.createDocumentForSchedule(stream, description, tournamentName, category, null, schedule);
        }
      } else {
        for (final String scheduleColumn : scheduleColumns) {
          final String filename = dir
              + File.separator
              + baseFileName
              + "-"
              + category.getName()
              + (scheduleColumns.size() > 1 ? ""
                  : "-"
                      + scheduleColumn)
              + (null != suffix ? "_"
                  + suffix : "")
              + ".pdf";
          // sort the schedule by the category we're working with
          if (null != scheduleColumn) {
            Collections.sort(schedule, new SubjectiveComparatorByAwardGroup(scheduleColumn));
          }

          try (OutputStream stream = new FileOutputStream(filename)) {
            SubjectivePdfWriter.createDocumentForSchedule(stream, description, tournamentName, category, scheduleColumn,
                                                          schedule);
          }
        } // foreach schedule column
      } // mappings found
    } // foreach category
  }

  /**
   * Sheets are sorted by table and then by time.
   *
   * @param tournamentData tournament information for name and performance names
   * @param output where to output
   * @param description where to get the goals from
   * @throws SQLException if there is an error talking to the database
   * @throws IOException if there is an error writing to the stream
   */
  public void outputPerformanceSheets(final TournamentData tournamentData,
                                      final OutputStream output,
                                      final ChallengeDescription description)
      throws SQLException, IOException {
    final ScoresheetGenerator scoresheets = new ScoresheetGenerator(getTotalNumberOfRounds()
        * schedule.size(), description, tournamentData.getCurrentTournament().getName());
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
      final int runNumber = si.computeRound(performance)
          + 1;
      final RunMetadata runMetadata = tournamentData.getRunMetadataFactory().getRunMetadata(runNumber);

      scoresheets.setTime(sheetIndex, performance.getTime());
      scoresheets.setTable(sheetIndex, String.format("%s %d", performance.getTable(), performance.getSide()));
      scoresheets.setRound(sheetIndex, runMetadata.getDisplayName());
      scoresheets.setNumber(sheetIndex, si.getTeamNumber());
      scoresheets.setDivision(sheetIndex, ScoresheetGenerator.AWARD_GROUP_LABEL, si.getAwardGroup());
      scoresheets.setName(sheetIndex, si.getTeamName());
      scoresheets.setPractice(sheetIndex, runMetadata.isPractice());

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
     * @param name the schedule column to sort by
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
   * Comparator for for sorting by wave and then team number.
   */
  public static final class ComparatorByWaveAndTeam implements Comparator<TeamScheduleInfo>, Serializable {

    /**
     * Singleton instance.
     */
    public static final ComparatorByWaveAndTeam INSTANCE = new ComparatorByWaveAndTeam();

    private ComparatorByWaveAndTeam() {
    }

    @Override
    public int compare(final TeamScheduleInfo one,
                       final TeamScheduleInfo two) {
      final @Nullable String oneWave = one.getWave();
      final @Nullable String twoWave = two.getWave();
      final int waveCompare;
      if (null == oneWave
          && null == twoWave) {
        waveCompare = 0;
      } else if (null == oneWave
          && null != twoWave) {
        waveCompare = 1;
      } else if (null != oneWave
          && null == twoWave) {
        waveCompare = -1;
      } else {
        // checker bug
        waveCompare = castNonNull(oneWave).compareTo(castNonNull(twoWave));
      }

      if (waveCompare == 0) {
        return Integer.compare(one.getTeamNumber(), two.getTeamNumber());
      } else {
        return waveCompare;
      }
    }
  }

  /**
   * Add the data from the specified round of the specified TeamScheduleInfo to
   * matches.
   *
   * @param ti the schedule info
   * @param performance the performance to add
   */
  @RequiresNonNull("matches")
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
   * @return the schedule info or null to stop reading (end of file or empty team
   *         number column)
   * @throws ScheduleParseException if there is an error with the schedule being
   *           read
   * @throws IOException if there is an error reading the file
   */
  private @Nullable TeamScheduleInfo parseLine(@UnderInitialization(TournamentSchedule.class) TournamentSchedule this,
                                               final CellFileReader reader,
                                               final ColumnInformation ci)
      throws IOException, ScheduleParseException {
    final @Nullable String @Nullable [] line = reader.readNext();
    if (null == line) {
      return null;
    }

    try {
      final String teamNumberStr = ci.getTeamNum(line);
      if (StringUtils.isBlank(teamNumberStr)) {
        return null;
      }

      final int teamNumber = Utilities.getIntegerNumberFormat().parse(teamNumberStr).intValue();
      final @Nullable String org = ci.getOrganization(line);
      final @Nullable String name = ci.getTeamName(line);
      final @Nullable String awardGroup = ci.getAwardGroup(line);
      final @Nullable String judgingGroup = ci.getJudgingGroup(line);
      final @Nullable String wave = ci.getWave(line);
      final TournamentTeam team = new TournamentTeam(teamNumber, null == org ? "" : org, null == name ? "" : name,
                                                     null == awardGroup ? "" : awardGroup,
                                                     null == judgingGroup ? "" : judgingGroup,
                                                     null == wave ? "" : wave);
      final TeamScheduleInfo ti = new TeamScheduleInfo(team);

      for (final CategoryColumnMapping mapping : ci.getSubjectiveColumnMappings()) {
        final String station = mapping.getScheduleColumn();
        final String str = ci.getSubjectiveTime(line, station);
        if (StringUtils.isBlank(str)) {
          throw new ScheduleParseException(String.format("Line %d is missing a time for subjective station '%s'",
                                                         reader.getLineNumber(), station));
        }

        // str is not null or empty, so parseTime cannot return null
        try {
          final LocalTime time = castNonNull(parseTime(str));
          ti.addSubjectiveTime(new SubjectiveTime(station, time));
        } catch (final DateTimeParseException e) {
          throw new ScheduleParseException(String.format("Line %d column '%s' has an unparsable time '%s'",
                                                         reader.getLineNumber(), station, str),
                                           e);
        }
      }

      // parse regular match play rounds
      for (int perfIndex = 0; perfIndex < ci.getNumPerfs(); ++perfIndex) {
        final String perfStr = ci.getPerf(line, perfIndex);
        if (StringUtils.isBlank(perfStr)) {
          throw new ScheduleParseException(String.format("Line %d is missing a time for performance %d",
                                                         reader.getLineNumber(), (perfIndex
                                                             + 1)));
        }

        final String table = ci.getPerfTable(line, perfIndex);
        if (StringUtils.isBlank(table)) {
          throw new ScheduleParseException(String.format("Line %d is missing a table for performance %d",
                                                         reader.getLineNumber(), (perfIndex
                                                             + 1)));
        }

        final String[] tablePieces = table.split(" ");
        if (tablePieces.length != 2) {
          throw new ScheduleParseException(String.format("Error parsing performance table information from: '%s', expecting 2 strings separated by a space",
                                                         table));
        }
        final String tableName = tablePieces[0];
        final int tableSide = Utilities.getIntegerNumberFormat().parse(tablePieces[1]).intValue();
        final int roundNumber = perfIndex
            + 1;
        try {
          // perfStr is not empty, so cannot be null
          final LocalTime perfTime = castNonNull(parseTime(perfStr));

          final PerformanceTime performance = new PerformanceTime(perfTime, tableName, tableSide);

          ti.addPerformance(performance);
          if (performance.getSide() > 2
              || performance.getSide() < 1) {
            throw new ScheduleParseException(String.format("There are only two sides to the table, number must be 1 or 2. team: %d performance round: %d line: %d",
                                                           ti.getTeamNumber(), roundNumber, reader.getLineNumber()));
          }
        } catch (final DateTimeParseException e) {
          throw new ScheduleParseException(String.format("Line %d performance %d has an unparsable time '%s'",
                                                         reader.getLineNumber(), roundNumber, perfStr),
                                           e);
        }
      }

      return ti;
    } catch (final ParseException | NumberFormatException pe) {
      throw new ScheduleParseException(String.format("Error parsing line '%s': %s", Arrays.toString(line),
                                                     pe.getMessage(), pe));
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

  private static final String NULL_WAVE_DB_VALUE = "__NULL_WAVE__";

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
        PreparedStatement delete = connection.prepareStatement("DELETE FROM schedule_wave_checkin WHERE tournament = ?")) {
      delete.setInt(1, tournamentID);
      delete.executeUpdate();
    }

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
        + " (tournament, team_number)"//
        + " VALUES(?, ?)");
        PreparedStatement insertPerfRounds = connection.prepareStatement("INSERT INTO sched_perf_rounds"//
            + " (tournament, team_number, perf_time, table_color, table_side)"//
            + " VALUES(?, ?, ?, ?, ?)");
        PreparedStatement insertSubjective = connection.prepareStatement("INSERT INTO sched_subjective" //
            + " (tournament, team_number, name, subj_time)" //
            + " VALUES(?, ?, ?, ?)");
        PreparedStatement insertWaveCheckin = connection.prepareStatement("INSERT INTO schedule_wave_checkin" //
            + " (tournament, wave, checkin_time)" //
            + " VALUES(?, ?, ?)")) {

      insertSchedule.setInt(1, tournamentID);

      insertPerfRounds.setInt(1, tournamentID);

      insertSubjective.setInt(1, tournamentID);

      insertWaveCheckin.setInt(1, tournamentID);

      for (final TeamScheduleInfo si : getSchedule()) {
        insertSchedule.setInt(2, si.getTeamNumber());
        insertSchedule.executeUpdate();

        insertPerfRounds.setInt(2, si.getTeamNumber());
        for (final PerformanceTime performance : si.getAllPerformances()) {
          insertPerfRounds.setTime(3, Time.valueOf(performance.getTime()));
          insertPerfRounds.setString(4, performance.getTable());
          insertPerfRounds.setInt(5, performance.getSide());
          insertPerfRounds.executeUpdate();
        }

        for (final SubjectiveTime subjectiveTime : si.getSubjectiveTimes()) {
          insertSubjective.setInt(2, si.getTeamNumber());
          insertSubjective.setString(3, subjectiveTime.getName());
          insertSubjective.setTime(4, Time.valueOf(subjectiveTime.getTime()));
          insertSubjective.executeUpdate();
        }
      } // foreach team

      for (final WaveCheckin waveCheckin : waveCheckinTimes) {
        final @Nullable String wave = waveCheckin.wave();
        insertWaveCheckin.setString(2, null == wave ? NULL_WAVE_DB_VALUE : wave);
        insertWaveCheckin.setTime(3, Time.valueOf(waveCheckin.checkin()));
      }
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
  public static final class ColumnInformation implements Serializable {

    private final int teamNumColumn;

    /**
     * Get the value of a column from the specified {@code line}.
     * 
     * @param line the line to read the data from
     * @param columnIndex the column index to get the value for
     * @return the value or {@code null} if the column index is out of range or the
     *         line has a {@code null} at the specified index
     */
    private static @Nullable String getValue(final @Nullable String[] line,
                                             final int columnIndex) {
      if (columnIndex < 0
          || columnIndex >= line.length) {
        return null;
      } else {
        return line[columnIndex];
      }
    }

    /**
     * @param line the line to parse
     * @return the team number column value, null if column cannot be found
     */
    public @Nullable String getTeamNum(final @Nullable String[] line) {
      return getValue(line, teamNumColumn);
    }

    private final int organizationColumn;

    /**
     * @param line the line to parse
     * @return the organization column value, null if column cannot be found
     */
    public @Nullable String getOrganization(final @Nullable String[] line) {
      return getValue(line, organizationColumn);
    }

    private final int teamNameColumn;

    /**
     * @param line the line to parse
     * @return the team name column value, null if column cannot be found
     */
    public @Nullable String getTeamName(final @Nullable String[] line) {
      return getValue(line, teamNameColumn);
    }

    private final int awardGroupColumn;

    /**
     * @param line the line to parse
     * @return the award group column value, null if column cannot be found
     */
    public @Nullable String getAwardGroup(final @Nullable String[] line) {
      return getValue(line, awardGroupColumn);
    }

    private final Collection<CategoryColumnMapping> subjectiveColumnMappings;

    private final Map<String, Integer> subjectiveColumnIndicies;

    /**
     * @return associations between subjective score categories and schedule columns
     */
    public Collection<CategoryColumnMapping> getSubjectiveColumnMappings() {
      return subjectiveColumnMappings;
    }

    /**
     * @return the names of the columns in the data file that specify times for
     *         subjective judging
     */
    public Collection<String> getSubjectiveStationNames() {
      return subjectiveColumnMappings.stream() //
                                     .map(CategoryColumnMapping::getScheduleColumn) //
                                     .collect(Collectors.toSet());
    }

    /**
     * @param line the line to parse
     * @param judgingStation the judging station
     * @return the time of the subjective judging station, null if not known
     */
    public @Nullable String getSubjectiveTime(final @Nullable String[] line,
                                              final String judgingStation) {
      if (subjectiveColumnIndicies.containsKey(judgingStation)) {
        final int index = subjectiveColumnIndicies.get(judgingStation);
        return getValue(line, index);
      } else {
        return null;
      }
    }

    private final int judgeGroupColumn;

    /**
     * @param line the line to parse
     * @return the judging group column value, null if column cannot be found
     */
    public @Nullable String getJudgingGroup(final @Nullable String[] line) {
      return getValue(line, judgeGroupColumn);
    }

    private final int waveColumn;

    /**
     * @param line the line to parse
     * @return the wave column value, null if column cannot be found
     */
    public @Nullable String getWave(final @Nullable String[] line) {
      return getValue(line, waveColumn);
    }

    private final int[] perfColumn;

    /**
     * @return number of performance rounds
     */
    public int getNumPerfs() {
      return perfColumn.length;
    }

    /**
     * @param line the line to parse
     * @param round the performance round to get the value for
     * @return the performance column value, null if column cannot be found
     */
    public @Nullable String getPerf(final @Nullable String[] line,
                                    final int round) {
      final int columnIndex = perfColumn[round];
      return getValue(line, columnIndex);
    }

    private final int[] perfTableColumn;

    /**
     * @param line the line to parse
     * @param round the performance round to get the value for
     * @return the performance table column value, null if column cannot be found
     */
    public @Nullable String getPerfTable(final @Nullable String[] line,
                                         final int round) {
      final int columnIndex = perfTableColumn[round];
      return getValue(line, columnIndex);
    }

    /**
     * @param headerLine the header line to check
     * @param columnName the name of the column to find
     * @return the column index for the specified name, -1 if no such column is
     *         found
     */
    private static int findColumnIndex(final @Nullable String[] headerLine,
                                       final @Nullable String columnName) {
      if (null == columnName) {
        return -1;
      }

      for (int i = 0; i < headerLine.length; ++i) {
        if (columnName.equals(headerLine[i])) {
          return i;
        }
      }
      return -1;
    }

    private final int headerRowIndex;

    /**
     * @return the index into the data file where the header row can be found
     */
    public int getHeaderRowIndex() {
      return headerRowIndex;
    }

    /**
     * Empty object used in place of {@code null}.
     */
    public static final ColumnInformation NULL = new ColumnInformation();

    private ColumnInformation() {
      this.headerRowIndex = 0;
      this.teamNumColumn = -1;
      this.organizationColumn = -1;
      this.teamNameColumn = -1;
      this.awardGroupColumn = -1;
      this.judgeGroupColumn = -1;
      this.waveColumn = -1;
      this.perfColumn = new int[0];
      this.perfTableColumn = new int[0];
      this.subjectiveColumnMappings = Collections.emptyList();
      this.subjectiveColumnIndicies = Collections.emptyMap();
    }

    /**
     * @param headerRowIndex {@link #getHeaderRowIndex()}
     * @param headerLine the header line, used to match column names to indicies
     * @param teamNumColumn {@link #getTeamNum(String[])}
     * @param organizationColumn {@link #getOrganization(String[])}
     * @param teamNameColumn {@link #getTeamName(String[])}
     * @param awardGroupColumn {@link #getAwardGroup(String[])}
     * @param subjectiveColumnMappings {@link #getSubjectiveColumnMappings()}
     * @param judgeGroupColumn {@link #getJudgingGroup(String[])}
     * @param waveColumn {@link #getWave(String[])}
     * @param perfColumn {@link #getPerf(String[], int)}
     * @param perfTableColumn {@link #getPerfTable(String[], int)}
     * @throws IllegalArgumentException if {@code perfColumn} and
     *           {@code perfTableColumn} are not the same length
     */
    @SuppressFBWarnings(value = "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", justification = "https://github.com/spotbugs/spotbugs/issues/927")
    public ColumnInformation(final int headerRowIndex,
                             final @Nullable String[] headerLine,
                             final String teamNumColumn,
                             final @Nullable String organizationColumn,
                             final @Nullable String teamNameColumn,
                             final @Nullable String awardGroupColumn,
                             final @Nullable String judgeGroupColumn,
                             final @Nullable String waveColumn,
                             final Collection<CategoryColumnMapping> subjectiveColumnMappings,
                             final String[] perfColumn,
                             final String[] perfTableColumn) {
      if (perfColumn.length != perfTableColumn.length) {
        throw new IllegalArgumentException(String.format("perfColumn (%d) must be the same length as perfTableColumn(%d)",
                                                         perfColumn.length, perfTableColumn.length));
      }

      this.headerRowIndex = headerRowIndex;
      this.teamNumColumn = findColumnIndex(headerLine, teamNumColumn);
      this.organizationColumn = findColumnIndex(headerLine, organizationColumn);
      this.teamNameColumn = findColumnIndex(headerLine, teamNameColumn);
      this.awardGroupColumn = findColumnIndex(headerLine, awardGroupColumn);
      this.judgeGroupColumn = findColumnIndex(headerLine, judgeGroupColumn);
      this.waveColumn = findColumnIndex(headerLine, waveColumn);

      this.perfColumn = new int[perfColumn.length];
      for (int i = 0; i < this.perfColumn.length; ++i) {
        this.perfColumn[i] = findColumnIndex(headerLine, perfColumn[i]);
      }
      this.perfTableColumn = new int[perfTableColumn.length];
      for (int i = 0; i < this.perfTableColumn.length; ++i) {
        this.perfTableColumn[i] = findColumnIndex(headerLine, perfTableColumn[i]);
      }

      this.subjectiveColumnMappings = Collections.unmodifiableCollection(new LinkedList<>(subjectiveColumnMappings));
      this.subjectiveColumnIndicies = new HashMap<>();
      this.subjectiveColumnMappings.stream().forEach(mapping -> {
        final String stationName = mapping.getScheduleColumn();
        final int index = findColumnIndex(headerLine, stationName);
        subjectiveColumnIndicies.put(stationName, index);
      });
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
      line.add(TournamentSchedule.WAVE_HEADER);
      final List<String> categories = Collections.unmodifiableList(new LinkedList<>(getSubjectiveStations()));
      for (final String category : categories) {
        line.add(category);
      }

      for (int round = 0; round < getTotalNumberOfRounds(); ++round) {
        final int runNumber = round
            + 1;
        line.add(String.format(TournamentSchedule.PERF_HEADER_FORMAT, runNumber));
        line.add(String.format(TournamentSchedule.TABLE_HEADER_FORMAT, runNumber));
      }

      csv.writeNext(line.toArray(new String[line.size()]));
      line.clear();

      for (final TeamScheduleInfo si : getSchedule()) {
        line.add(String.valueOf(si.getTeamNumber()));
        line.add(si.getAwardGroup());
        line.add(si.getTeamName());
        line.add(Utilities.stringValueOrEmpty(si.getOrganization()));
        line.add(si.getJudgingGroup());
        line.add(Utilities.stringValueOrEmpty(si.getWave()));
        for (final String category : categories) {
          final SubjectiveTime stime = si.getSubjectiveTimeByName(category);
          if (null != stime) {
            final LocalTime d = stime.getTime();
            line.add(TournamentSchedule.formatTime(d));
          }
        }

        si.enumeratePerformances().forEachOrdered(pair -> {
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

  /**
   * @return all waves in the schedule
   */
  public Set<String> getAllWaves() {
    return schedule.stream().map(TeamScheduleInfo::getWave).distinct().collect(Collectors.toSet());
  }

  private final LinkedList<WaveCheckin> waveCheckinTimes = new LinkedList<>();

  /**
   * @param times wave check-in times
   */
  public void setWaveCheckinTimes(final Collection<WaveCheckin> times) {
    waveCheckinTimes.clear();
    waveCheckinTimes.addAll(times);
  }

  /**
   * Check-in time for a wave.
   * 
   * @param wave the wave
   * @param checkin the check-in time
   */
  public record WaveCheckin(@Nullable String wave,
                            LocalTime checkin)
      implements Serializable {
  }

  /**
   * Schedule information for a wave.
   * 
   * @param wave the wave that the schedule is for
   * @param checkin the time that the wave is to checkin
   * @param performanceStart start of the first performance run
   * @param performanceEnd end of the last performance run
   * @param subjectiveStart start of the first subjective session
   * @param subjectiveEnd end of the last subjective session
   */
  public record GeneralSchedule(@Nullable String wave,
                                LocalTime checkin,
                                LocalTime performanceStart,
                                LocalTime performanceEnd,
                                LocalTime subjectiveStart,
                                LocalTime subjectiveEnd) {
  }

  private static final class GeneralScheduleComparator implements Comparator<GeneralSchedule>, Serializable {

    static final GeneralScheduleComparator INSTANCE = new GeneralScheduleComparator();

    @Override
    public int compare(final GeneralSchedule o1,
                       final GeneralSchedule o2) {
      final @Nullable String o1Wave = o1.wave();
      final @Nullable String o2Wave = o2.wave();
      if (null == o1Wave
          && null == o2Wave) {
        return 0;
      } else if (null == o1Wave) {
        return -1;
      } else if (null == o2Wave) {
        return 1;
      } else {
        return o1Wave.compareTo(o2Wave);
      }
    }
  }

  /**
   * @return general schedule sorted by wave, {@code null} is first
   */
  public List<GeneralSchedule> computeGeneralSchedule() {
    final List<GeneralSchedule> generalSchedule = new LinkedList<>();
    for (final @Nullable String wave : getAllWaves()) {
      final ImmutablePair<LocalTime, LocalTime> performance = computePerformanceGeneralSchedule(wave);
      final ImmutablePair<LocalTime, LocalTime> subjective = computeSubjectiveGeneralSchedule(wave);
      final LocalTime checkin = getCheckinTime(wave,
                                               performance.getLeft().isBefore(subjective.getLeft())
                                                   ? performance.getLeft()
                                                   : subjective.getLeft());

      final GeneralSchedule gs = new GeneralSchedule(wave, checkin, performance.getLeft(), performance.getRight(),
                                                     subjective.getLeft(), subjective.getRight());
      generalSchedule.add(gs);
    }

    Collections.sort(generalSchedule, GeneralScheduleComparator.INSTANCE);

    return generalSchedule;
  }

  private static final Duration DEFAULT_CHECKIN_OFFSET = Duration.ofMinutes(30);

  private LocalTime getCheckinTime(final @Nullable String wave,
                                   final LocalTime earliestEvent) {
    final Optional<WaveCheckin> checkin = waveCheckinTimes.stream() //
                                                          .filter(wc -> Objects.equals(wc.wave(), wave)) //
                                                          .findAny();
    if (checkin.isPresent()) {
      return checkin.get().checkin();
    } else {
      return earliestEvent.minus(DEFAULT_CHECKIN_OFFSET);
    }
  }

  /**
   * Compute the general performance schedule for the specified wave. Computes the
   * minimum duration between performance runs and uses that for the duration of
   * the last performance.
   * 
   * @param wave the wave to work with
   * @return start of first performance and end of last performance
   * @see TeamScheduleInfo#getWave()
   */
  private ImmutablePair<LocalTime, LocalTime> computePerformanceGeneralSchedule(final @Nullable String wave) {
    final Stream<PerformanceTime> perfTimes = schedule.stream() //
                                                      .filter(si -> Objects.equals(si.getWave(), wave)) //
                                                      .map(TeamScheduleInfo::allPerformances).flatMap(s -> s) //
                                                      .sorted();
    PerformanceTime first = null;
    PerformanceTime last = null;
    Duration minDifference = null;
    PerformanceTime prevTime = null;
    for (final PerformanceTime perfTime : perfTimes.collect(Collectors.toList())) {
      if (null == first) {
        first = perfTime;
      }
      last = perfTime;

      if (null != prevTime) {
        final Duration difference = Duration.between(prevTime.getTime(), perfTime.getTime());
        if (difference.isPositive()) {
          if (null == minDifference) {
            minDifference = difference;
          } else if (difference.compareTo(minDifference) < 0) {
            minDifference = difference;
          }
        }
      }
      prevTime = perfTime;
    }

    if (null == minDifference) {
      throw new FLLRuntimeException(String.format("No difference between performance times found for wave %s - null minDifference",
                                                  wave));
    }
    if (null == first) {
      throw new FLLRuntimeException(String.format("No performance times found for wave %s - null first", wave));
    }
    if (null == last) {
      throw new FLLRuntimeException(String.format("No performance times found for wave %s - null last", wave));
    }

    return ImmutablePair.of(first.getTime(), last.getTime().plus(minDifference));
  }

  /**
   * Compute the general subjective schedule for the specified wave. Computes the
   * minimum duration between subjective sessions and uses that for the duration
   * of
   * the last subjective session.
   * 
   * @param wave the wave to work with
   * @return start of first subjective session and end of last subjective session
   * @see TeamScheduleInfo#getWave()
   */
  private ImmutablePair<LocalTime, LocalTime> computeSubjectiveGeneralSchedule(final @Nullable String wave) {
    final Map<String, List<SubjectiveTime>> subjectiveTimesByCategory = schedule.stream() //
                                                                                .filter(si -> Objects.equals(si.getWave(),
                                                                                                             wave)) //
                                                                                .map(TeamScheduleInfo::getSubjectiveTimes)
                                                                                .flatMap(Collection::stream) //
                                                                                .collect(Collectors.groupingBy(SubjectiveTime::getName));
    // final Map<String, SortedSet<SubjectiveTime>> subjectiveTimesByCategory = new
    // HashMap<>();
    // for (final SubjectiveTime st : allSubjectiveTimes) {
    // subjectiveTimesByCategory.computeIfAbsent(st.getName(), k -> new
    // TreeSet<>()).add(st);
    // }

    LocalTime globalFirst = null;
    LocalTime globalLast = null;
    Duration globalMinDifference = null;
    for (final Map.Entry<String, List<SubjectiveTime>> entry : subjectiveTimesByCategory.entrySet()) {
      final List<SubjectiveTime> times = entry.getValue();
      times.sort(null);

      LocalTime localFirst = null;
      LocalTime localLast = null;
      Duration localMinDifference = null;
      LocalTime prevTime = null;
      for (final SubjectiveTime st : times) {
        if (null == localFirst) {
          localFirst = st.getTime();
        }
        localLast = st.getTime();

        if (null != prevTime) {
          final Duration difference = Duration.between(prevTime, st.getTime());
          if (difference.isPositive()) {
            if (null == localMinDifference) {
              localMinDifference = difference;
            } else if (difference.compareTo(localMinDifference) < 0) {
              localMinDifference = difference;
            }
          }
        }
        prevTime = st.getTime();
      }

      if (null == localMinDifference) {
        throw new FLLRuntimeException(String.format("No differences between subjective times found for %s in wave %s - null localMinDifference",
                                                    entry.getKey(), wave));
      }
      if (null == localFirst) {
        throw new FLLRuntimeException(String.format("No subjective times found for %s in wave %s - null localFirst",
                                                    entry.getKey(), wave));
      }
      if (null == localLast) {
        throw new FLLRuntimeException(String.format("No subjective times found for %s in wave %s - null localLast",
                                                    entry.getKey(), wave));
      }

      if (null == globalFirst
          || localFirst.isBefore(globalFirst)) {
        globalFirst = localFirst;
      }
      if (null == globalLast
          || localLast.isAfter(globalLast)) {
        globalLast = localLast;
      }
      if (null == globalMinDifference
          || localMinDifference.compareTo(globalMinDifference) < 0) {
        globalMinDifference = localMinDifference;
      }

    }

    if (null == globalMinDifference) {
      throw new FLLRuntimeException(String.format("No differences between subjective times found for wave %s - null globalMinDifference",
                                                  wave));
    }
    if (null == globalFirst) {
      throw new FLLRuntimeException(String.format("No subjective times found for wave %s - null globalFirst", wave));
    }
    if (null == globalLast) {
      throw new FLLRuntimeException(String.format("No subjective times found for wave %s - null globalLast", wave));
    }

    return ImmutablePair.of(globalFirst, globalLast.plus(globalMinDifference));
  }
}
