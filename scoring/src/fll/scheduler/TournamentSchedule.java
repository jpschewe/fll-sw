/*
 * Copyright (c) 2000-2008 HighTechKids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.scheduler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
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

import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;

import fll.Utilities;
import fll.util.CellFileReader;
import fll.util.ExcelCellReader;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;

/**
 * Tournament schedule. Can parse the schedule from a spreadsheet.
 */
public class TournamentSchedule {

  private static final Logger LOGGER = Logger.getLogger(TournamentSchedule.class);

  /**
   * Header on team number column.
   */
  public static final String TEAM_NUMBER_HEADER = "Team #";

  public static final String TEAM_NAME_HEADER = "Team Name";

  public static final String ORGANIZATION_HEADER = "Organization";

  public static final String DIVISION_HEADER = "Div";

  public static final String PRESENTATION_HEADER = "Presentation";

  public static final String TECHNICAL_HEADER = "Technical";

  public static final String JUDGE_GROUP_HEADER = "Judging Group";

  public static final String BASE_PERF_HEADER = "Perf #";

  /**
   * Used with {@link String#format(String, Object...)} to create a performance
   * round header.
   */
  private static final String PERF_HEADER_FORMAT = "Perf #%d";

  /**
   * Used with {@link String#format(String, Object...)} to create a performance
   * table header.
   */
  private static final String TABLE_HEADER_FORMAT = "Perf %d Table";

  private int _teamNumColumn = -1;

  private int _teamNameColumn = -1;

  private int _organizationColumn = -1;

  private int _divisionColumn = -1;

  private int _presentationColumn = -1;

  private int _technicalColumn = -1;

  private int _judgeGroupColumn = -1;

  private int[] _perfColumn;

  private int[] _perfTableColumn;

  public int getNumberOfRounds() {
    if (null == _perfColumn) {
      throw new FLLInternalException("_perfColumn isn't set yet, it must be set to get the number of rounds");
    }
    return _perfColumn.length;
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

  public static final long SECONDS_PER_MINUTE = 60;

  public static final long MILLISECONDS_PER_SECOND = 1000;

  private long performanceDuration = 5
      * SECONDS_PER_MINUTE * MILLISECONDS_PER_SECOND;

  private long subjectiveDuration = 20
      * SECONDS_PER_MINUTE * MILLISECONDS_PER_SECOND;

  private long changetime = 15
      * SECONDS_PER_MINUTE * MILLISECONDS_PER_SECOND;

  private long performanceChangetime = 45
      * SECONDS_PER_MINUTE * MILLISECONDS_PER_SECOND;

  private long specialPerformanceChangetime = 30
      * SECONDS_PER_MINUTE * MILLISECONDS_PER_SECOND;

  private final Map<Date, Map<String, List<TeamScheduleInfo>>> _matches = new HashMap<Date, Map<String, List<TeamScheduleInfo>>>();

  private final String _sheetName;

  public String getSheetName() {
    return _sheetName;
  }

  private final File _file;

  public File getFile() {
    return _file;
  }

  private final Set<String> _tableColors = new HashSet<String>();

  private final Set<String> _divisions = new HashSet<String>();

  private final Set<String> _judges = new HashSet<String>();

  private final List<TeamScheduleInfo> _schedule = new LinkedList<TeamScheduleInfo>();

  /**
   * @return An unmodifiable copy of the schedule.
   */
  public List<TeamScheduleInfo> getSchedule() {
    return Collections.unmodifiableList(_schedule);
  }

  /**
   * @param f the spreadsheet file to read
   * @param sheetName the name of the sheet to look at
   * @throws ScheduleParseException if there is an error parsing the schedule
   */
  public TournamentSchedule(final File f,
                            final String sheetName) throws IOException, ParseException, InvalidFormatException,
      ScheduleParseException {
    _file = f;
    _sheetName = sheetName;

    LOGGER.info(new Formatter().format("Reading file %s", _file.getAbsoluteFile()));

    if (!_file.canRead()
        || !_file.isFile()) {
      throw new RuntimeException("File is not readable or not a file: "
          + _file.getAbsolutePath());
    }

    final CellFileReader reader = new ExcelCellReader(_file, _sheetName);

    findColumns(reader);
    parseData(reader);
    reader.close();
  }

  /**
   * Check if this line is a header line. This checks for key headers and
   * returns true if they are found.
   */
  private static boolean isHeaderLine(final String[] line) {
    boolean retval = false;
    for (int i = 0; i < line.length; ++i) {
      if (line[i].equals(TEAM_NUMBER_HEADER)) {
        retval = true;
      }
    }

    return retval;
  }

  /**
   * Find the index of the columns. Reads lines until the headers are found or
   * EOF is reached.
   * 
   * @throws IOException
   * @throws RuntimeException if a column cannot be found
   */
  private void findColumns(final CellFileReader reader) throws IOException {
    _teamNumColumn = -1;

    while (_teamNumColumn == -1) {
      final String[] line = reader.readNext();
      if (null == line) {
        throw new RuntimeException("Cannot find header line and reached EOF");
      }

      if (isHeaderLine(line)) {
        parseHeader(line);
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

  private void parseHeader(final String[] line) {
    final int numPerfRounds = countNumRounds(line);
    _perfColumn = new int[numPerfRounds];
    _perfTableColumn = new int[numPerfRounds];

    _teamNumColumn = getColumnForHeader(line, TEAM_NUMBER_HEADER);
    _organizationColumn = getColumnForHeader(line, ORGANIZATION_HEADER);
    _teamNameColumn = getColumnForHeader(line, TEAM_NAME_HEADER);
    _divisionColumn = getColumnForHeader(line, DIVISION_HEADER);
    _presentationColumn = getColumnForHeader(line, PRESENTATION_HEADER);
    _technicalColumn = getColumnForHeader(line, TECHNICAL_HEADER);
    _judgeGroupColumn = getColumnForHeader(line, JUDGE_GROUP_HEADER);
    for (int round = 0; round < numPerfRounds; ++round) {
      _perfColumn[round] = getColumnForHeader(line, String.format(PERF_HEADER_FORMAT, (round + 1)));
      _perfTableColumn[round] = getColumnForHeader(line, String.format(TABLE_HEADER_FORMAT, (round + 1)));
    }
  }

  /**
   * Parse the data of the schedule.
   * 
   * @throws IOException
   * @throws ScheduleParseException if there is an error with the schedule
   */
  private void parseData(final CellFileReader reader) throws IOException, ParseException, ScheduleParseException {
    TeamScheduleInfo ti;
    while (null != (ti = parseLine(reader))) {
      _schedule.add(ti);

      // keep track of some meta information
      for (int round = 0; round < getNumberOfRounds(); ++round) {
        _tableColors.add(ti.getPerfTableColor(round));
        addToMatches(ti, round);
      }
      _divisions.add(ti.getDivision());
      _judges.add(ti.getJudge());
    }
  }

  /**
   * Verify the schedule.
   * 
   * @return the constraint violations found, empty if no violations
   * @throws IOException
   */
  public List<ConstraintViolation> verifySchedule() {
    final List<ConstraintViolation> constraintViolations = new LinkedList<ConstraintViolation>();

    // create separate local variables for each return so that the function is
    // guaranteed to be called. If this isn't done the short-circuit boolean
    // logic evaluation will prevent the function from being called.

    for (final TeamScheduleInfo verify : _schedule) {
      verifyTeam(constraintViolations, verify);
    }

    verifyPerformanceAtTime(constraintViolations);
    verifyNumTeamsAtTable(constraintViolations);
    verifyPresentationAtTime(constraintViolations);
    verifyTechnicalAtTime(constraintViolations);

    return constraintViolations;
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

  public void outputDetailedSchedules() throws DocumentException, IOException {
    final String filename = _file.getPath();
    final int dotIdx = filename.lastIndexOf('.');
    final String baseFilename;
    if (-1 == dotIdx) {
      baseFilename = filename;
    } else {
      baseFilename = filename.substring(0, dotIdx);
    }
    final File pdfFile = new File(baseFilename
        + "-detailed.pdf");
    LOGGER.info("Writing detailed schedules to "
        + pdfFile.getAbsolutePath());

    // print out detailed schedules
    final Document detailedSchedules = new Document(PageSize.LETTER); // portrait

    // Measurements are always in points (72 per inch)
    // This sets up 1/4 inch margins
    detailedSchedules.setMargins(0.25f * 72, 0.25f * 72, 0.35f * 72, 0.35f * 72);

    // output to a PDF
    final FileOutputStream output = new FileOutputStream(pdfFile);
    final PdfWriter writer = PdfWriter.getInstance(detailedSchedules, output);
    writer.setPageEvent(new PageFooter());

    detailedSchedules.open();

    outputPresentationSchedule(detailedSchedules);
    detailedSchedules.add(Chunk.NEXTPAGE);

    outputTechnicalSchedule(detailedSchedules);
    detailedSchedules.add(Chunk.NEXTPAGE);

    outputPerformanceSchedule(detailedSchedules);

    detailedSchedules.close();
    output.close();

  }

  private void outputPerformanceSchedule(final Document detailedSchedules) throws DocumentException {

    for (int round = 0; round < getNumberOfRounds(); ++round) {
      Collections.sort(_schedule, getPerformanceComparator(round));

      // list of teams staying around to even up the teams
      final List<TeamScheduleInfo> teamsStaying = new LinkedList<TeamScheduleInfo>();

      final PdfPTable table = createTable(6);
      table.setWidths(new float[] { 2, 1, 3, 3, 2, 2 });

      table.addCell(createHeaderCell(TEAM_NUMBER_HEADER));
      table.addCell(createHeaderCell(DIVISION_HEADER));
      table.addCell(createHeaderCell("School or Organization"));
      table.addCell(createHeaderCell("Team Name"));
      table.addCell(createHeaderCell(new Formatter().format("Perf #%d", (round + 1)).toString()));
      table.addCell(createHeaderCell(new Formatter().format("Perf %d Table", (round + 1)).toString()));
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

  private void outputPresentationSchedule(final Document detailedSchedules) throws DocumentException {
    final PdfPTable table = createTable(6);
    table.setWidths(new float[] { 2, 1, 3, 3, 2, 2 });

    Collections.sort(_schedule, PRESENTATION_COMPARATOR);
    table.addCell(createHeaderCell(TEAM_NUMBER_HEADER));
    table.addCell(createHeaderCell(DIVISION_HEADER));
    table.addCell(createHeaderCell("School or Organization"));
    table.addCell(createHeaderCell("Team Name"));
    table.addCell(createHeaderCell(PRESENTATION_HEADER));
    table.addCell(createHeaderCell(JUDGE_GROUP_HEADER));
    table.setHeaderRows(1);

    for (final TeamScheduleInfo si : _schedule) {
      table.addCell(createCell(String.valueOf(si.getTeamNumber())));
      table.addCell(createCell(si.getDivision()));
      table.addCell(createCell(si.getOrganization()));
      table.addCell(createCell(si.getTeamName()));
      table.addCell(createCell(OUTPUT_DATE_FORMAT.get().format(si.getPresentation())));
      table.addCell(createCell(si.getJudge()));
    }

    detailedSchedules.add(table);

  }

  private void outputTechnicalSchedule(final Document detailedSchedules) throws DocumentException {
    Collections.sort(_schedule, TECHNICAL_COMPARATOR);

    final PdfPTable table = createTable(6);
    table.setWidths(new float[] { 2, 1, 3, 3, 2, 2 });

    // header
    table.addCell(createHeaderCell("Team #"));
    table.addCell(createHeaderCell("Div"));
    table.addCell(createHeaderCell("School or Organization"));
    table.addCell(createHeaderCell("Team Name"));
    table.addCell(createHeaderCell("Technical"));
    table.addCell(createHeaderCell("Judging Group"));
    table.setHeaderRows(1);

    for (final TeamScheduleInfo si : _schedule) {
      table.addCell(createCell(String.valueOf(si.getTeamNumber())));
      table.addCell(createCell(si.getDivision()));
      table.addCell(createCell(si.getOrganization()));
      table.addCell(createCell(si.getTeamName()));
      table.addCell(createCell(OUTPUT_DATE_FORMAT.get().format(si.getTechnical())));
      table.addCell(createCell(si.getJudge()));
    }
    detailedSchedules.add(table);

  }

  /**
   * Sort by division, then judge, then by time.
   */
  private static final Comparator<TeamScheduleInfo> PRESENTATION_COMPARATOR = new Comparator<TeamScheduleInfo>() {
    public int compare(final TeamScheduleInfo one,
                       final TeamScheduleInfo two) {
      if (!one.getDivision().equals(two.getDivision())) {
        return one.getDivision().compareTo(two.getDivision());
      } else if (!one.getJudge().equals(two.getJudge())) {
        return one.getJudge().compareTo(two.getJudge());
      } else {
        return one.getPresentation().compareTo(two.getPresentation());
      }
    }
  };

  /**
   * Sort by division, then by time.
   */
  private static final Comparator<TeamScheduleInfo> TECHNICAL_COMPARATOR = new Comparator<TeamScheduleInfo>() {
    public int compare(final TeamScheduleInfo one,
                       final TeamScheduleInfo two) {
      if (!one.getDivision().equals(two.getDivision())) {
        return one.getDivision().compareTo(two.getDivision());
      } else if (!one.getJudge().equals(two.getJudge())) {
        return one.getJudge().compareTo(two.getJudge());
      } else {
        return one.getTechnical().compareTo(two.getTechnical());
      }
    }
  };

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
    Date minTechnical = null;
    Date maxTechnical = null;
    Date minPresentation = null;
    Date maxPresentation = null;
    final Date[] minPerf = new Date[getNumberOfRounds()];
    final Date[] maxPerf = new Date[getNumberOfRounds()];

    for (final TeamScheduleInfo si : _schedule) {
      if (null != si.getTechnical()) {
        if (null == minTechnical
            || si.getTechnical().before(minTechnical)) {
          minTechnical = si.getTechnical();
        }
        if (null == maxTechnical
            || si.getTechnical().after(maxTechnical)) {
          maxTechnical = si.getTechnical();
        }
      }
      if (null != si.getPresentation()) {
        if (null == minPresentation
            || si.getPresentation().before(minPresentation)) {
          minPresentation = si.getPresentation();
        }
        if (null == maxPresentation
            || si.getPresentation().after(maxPresentation)) {
          maxPresentation = si.getPresentation();
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
    output.format("min technical: %s%n", OUTPUT_DATE_FORMAT.get().format(minTechnical));
    output.format("max technical: %s%n", OUTPUT_DATE_FORMAT.get().format(maxTechnical));
    output.format("min presentation: %s%n", OUTPUT_DATE_FORMAT.get().format(minPresentation));
    output.format("max presentation: %s%n", OUTPUT_DATE_FORMAT.get().format(maxPresentation));
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
    // constraint set 6
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
   * Ensure that no more than 1 team is in presentation judging at once.
   */
  private void verifyPresentationAtTime(final Collection<ConstraintViolation> violations) {
    // constraint set 7
    final Map<Date, Set<TeamScheduleInfo>> teamsAtTime = new HashMap<Date, Set<TeamScheduleInfo>>();
    for (final TeamScheduleInfo si : _schedule) {
      final Set<TeamScheduleInfo> teams;
      if (teamsAtTime.containsKey(si.getPresentation())) {
        teams = teamsAtTime.get(si.getPresentation());
      } else {
        teams = new HashSet<TeamScheduleInfo>();
        teamsAtTime.put(si.getPresentation(), teams);
      }
      teams.add(si);
    }

    for (final Map.Entry<Date, Set<TeamScheduleInfo>> entry : teamsAtTime.entrySet()) {
      if (entry.getValue().size() > _judges.size()) {
        final String message = String.format("There are too many teams in presentation at %s in presentation",
                                             OUTPUT_DATE_FORMAT.get().format(entry.getKey()));
        violations.add(new ConstraintViolation(true, ConstraintViolation.NO_TEAM, null, null, null, message));
      }

      final Set<String> judges = new HashSet<String>();
      for (final TeamScheduleInfo ti : entry.getValue()) {
        if (!judges.add(ti.getJudge())) {
          final String message = String
                                       .format(
                                               "Presentation judge %s cannot see more than one team at %s in presentation",
                                               ti.getJudge(), OUTPUT_DATE_FORMAT.get().format(ti.getPresentation()));
          violations.add(new ConstraintViolation(true, ConstraintViolation.NO_TEAM, null, null, null, message));
        }
      }

    }
  }

  /**
   * Ensure that no more than 1 team is in technical judging at once.
   */
  private void verifyTechnicalAtTime(final Collection<ConstraintViolation> violations) {
    // constraint set 7
    final Map<Date, Set<TeamScheduleInfo>> teamsAtTime = new HashMap<Date, Set<TeamScheduleInfo>>();
    for (final TeamScheduleInfo si : _schedule) {
      final Set<TeamScheduleInfo> teams;
      if (teamsAtTime.containsKey(si.getTechnical())) {
        teams = teamsAtTime.get(si.getTechnical());
      } else {
        teams = new HashSet<TeamScheduleInfo>();
        teamsAtTime.put(si.getTechnical(), teams);
      }
      teams.add(si);
    }

    for (final Map.Entry<Date, Set<TeamScheduleInfo>> entry : teamsAtTime.entrySet()) {
      if (entry.getValue().size() > _judges.size()) {
        final String message = String.format("There are too many teams in technical at %s in technical",
                                             OUTPUT_DATE_FORMAT.get().format(entry.getKey()));
        violations.add(new ConstraintViolation(true, ConstraintViolation.NO_TEAM, null, null, null, message));
      }

      final Set<String> judges = new HashSet<String>();
      for (final TeamScheduleInfo ti : entry.getValue()) {
        if (!judges.add(ti.getJudge())) {
          final String message = String
                                       .format(
                                               "Technical judge %s cannot see more than one team at %s in presentation",
                                               ti.getJudge(), OUTPUT_DATE_FORMAT.get().format(ti.getPresentation()));
          violations.add(new ConstraintViolation(true, ConstraintViolation.NO_TEAM, null, null, null, message));
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
    // constraint set 1
    if (ti.getPresentation().before(ti.getTechnical())) {
      if (ti.getPresentation().getTime()
          + getSubjectiveDuration() > ti.getTechnical().getTime()) {
        final String message = String.format("Team %d is still in presentation when they need to start technical",
                                             ti.getTeamNumber());
        violations.add(new ConstraintViolation(true, ti.getTeamNumber(), ti.getPresentation(), ti.getTechnical(), null,
                                               message));
        return;
      } else if (ti.getPresentation().getTime()
          + getSubjectiveDuration() + getChangetime() > ti.getTechnical().getTime()) {
        final String message = String
                                     .format(
                                             "Team %d has doesn't have enough time between presentation and technical (need %d minutes)",
                                             ti.getTeamNumber(), getChangetimeAsMinutes());
        violations.add(new ConstraintViolation(false, ti.getTeamNumber(), ti.getPresentation(), ti.getTechnical(),
                                               null, message));
        return;
      }
    } else {
      if (ti.getTechnical().getTime()
          + getSubjectiveDuration() > ti.getPresentation().getTime()) {
        final String message = String.format("Team %d is still in technical when they need start presentation",
                                             ti.getTeamNumber());
        violations.add(new ConstraintViolation(true, ti.getTeamNumber(), ti.getPresentation(), ti.getTechnical(), null,
                                               message));
        return;
      } else if (ti.getTechnical().getTime()
          + getSubjectiveDuration() + getChangetime() > ti.getPresentation().getTime()) {
        final String message = String
                                     .format(
                                             "Team %d has doesn't have enough time between presentation and technical (need %d minutes)",
                                             ti.getTeamNumber(), getChangetimeAsMinutes());
        violations.add(new ConstraintViolation(false, ti.getTeamNumber(), ti.getPresentation(), ti.getTechnical(),
                                               null, message));
        return;
      }
    }

    // constraint set 3
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
      final String message = String
                                   .format(
                                           "Team %d is still in performance %d when they are to start performance %d: %s - %s",
                                           ti.getTeamNumber(), 1, 2, OUTPUT_DATE_FORMAT.get().format(ti.getPerf(0)),
                                           OUTPUT_DATE_FORMAT.get().format(ti.getPerf(1)));
      violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, null, ti.getPerf(1), message));
    } else if (ti.getPerf(0).getTime()
        + getPerformanceDuration() + changetime > ti.getPerf(1).getTime()) {
      final String message = String
                                   .format(
                                           "Team %d doesn't have enough time (%d minutes) between performance 1 and performance 2: %s - %s",
                                           ti.getTeamNumber(), changetime
                                               / 1000 / SECONDS_PER_MINUTE, OUTPUT_DATE_FORMAT.get()
                                                                                              .format(ti.getPerf(0)),
                                           OUTPUT_DATE_FORMAT.get().format(ti.getPerf(1)));
      violations.add(new ConstraintViolation(false, ti.getTeamNumber(), null, null, ti.getPerf(1), message));
    }

    if (ti.getPerf(1).getTime()
        + getPerformanceDuration() + getPerformanceChangetime() > ti.getPerf(2).getTime()) {
      final String message = String
      .format(
              "Team %d is still in performance %d when they are to start performance %d: %s - %s",
              ti.getTeamNumber(), 2, 3, OUTPUT_DATE_FORMAT.get().format(ti.getPerf(0)),
              OUTPUT_DATE_FORMAT.get().format(ti.getPerf(1)));
      violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, null, ti.getPerf(2), message));
    }
    else if (ti.getPerf(1).getTime()
        + getPerformanceDuration() + getPerformanceChangetime() > ti.getPerf(2).getTime()) {
      final String message = String
                                   .format(
                                           "Team %d doesn't have enough time (%d minutes) between performance 2 and performance 3: %s - %s",
                                           ti.getTeamNumber(), changetime
                                               / 1000 / SECONDS_PER_MINUTE, OUTPUT_DATE_FORMAT.get()
                                                                                              .format(ti.getPerf(1)),
                                           OUTPUT_DATE_FORMAT.get().format(ti.getPerf(2)));
      violations.add(new ConstraintViolation(false, ti.getTeamNumber(), null, null, ti.getPerf(2), message));
    }

    // constraint set 4
    for (int round = 0; round < getNumberOfRounds(); ++round) {
      final String performanceName = String.valueOf(round + 1);
      verifyPerformanceVsPresentation(violations, ti, ti.getPerf(round), performanceName);
    }

    // constraint set 5
    for (int round = 0; round < getNumberOfRounds(); ++round) {
      final String performanceName = String.valueOf(round + 1);
      verifyPerformanceVsTechnical(violations, ti, performanceName, ti.getPerf(round));
    }

    // make sure that all opponents are different & sides are different
    for (int round = 0; round < getNumberOfRounds(); ++round) {
      final TeamScheduleInfo opponent = findOpponent(ti, round);
      if (null != opponent) {
        int opponentSide = -1;
        // figure out which round matches up
        for (int oround = 0; oround < getNumberOfRounds(); ++oround) {
          if (opponent.getPerf(oround).equals(ti.getPerf(round))) {
            opponentSide = opponent.getPerfTableSide(oround);
            break;
          }
        }
        if (-1 == opponentSide) {
          final String message = String
                                       .format(
                                               "Unable to find time match for rounds between team %d and team %d at time %s",
                                               ti.getTeamNumber(), opponent.getTeamNumber(),
                                               OUTPUT_DATE_FORMAT.get().format(ti.getPerf(round)));
          violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, null, ti.getPerf(round), message));
        } else {
          if (opponentSide == ti.getPerfTableSide(round)) {
            final String message = String
                                         .format(
                                                 "Team %d and team %d are both on table %s side %d at the same time for round %d",
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
            violations.add(new ConstraintViolation(false, opponent.getTeamNumber(), null, null, null, message));
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
        // everything else checked out, only only need to check the end time
        // against subjective and the next round
        final Date performanceTime = next.getPerf(round);
        verifyPerformanceVsPresentation(violations, ti, performanceTime, performanceName);

        verifyPerformanceVsTechnical(violations, ti, performanceName, performanceTime);

        if (round + 1 < getNumberOfRounds()) {
          if (next.getPerf(round).getTime()
              + getPerformanceDuration() > ti.getPerf(round + 1).getTime()) {
            final String message = String
                            .format(
                                    "Team %d will be in performance round %d when it is starting the extra performance round: %s - %s",
                                    ti.getTeamNumber(), round,
                                    OUTPUT_DATE_FORMAT.get().format(next.getPerf(round)),
                                    OUTPUT_DATE_FORMAT.get().format(ti.getPerf(round + 1)));
            violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, null, ti.getPerf(round + 1), message));
          }
          else if (next.getPerf(round).getTime()
              + getPerformanceDuration() + getPerformanceChangetime() > ti.getPerf(round + 1).getTime()) {
            final String message = String
                            .format(
                                    "Team %d doesn't have enough time (%d minutes) between performance %d and performance extra: %s - %s",
                                    ti.getTeamNumber(), changetime
                                        / 1000 / SECONDS_PER_MINUTE, round,
                                    OUTPUT_DATE_FORMAT.get().format(next.getPerf(round)),
                                    OUTPUT_DATE_FORMAT.get().format(ti.getPerf(round + 1)));
            violations.add(new ConstraintViolation(false, ti.getTeamNumber(), null, null, ti.getPerf(round + 1), message));
          }
        }

      }
    }
  }

  private void verifyPerformanceVsTechnical(final Collection<ConstraintViolation> violations,
                                            final TeamScheduleInfo ti,
                                            final String performanceName,
                                            final Date performanceTime) {
    if (ti.getTechnical().before(performanceTime)) {
      if (ti.getTechnical().getTime()
          + getSubjectiveDuration() > performanceTime.getTime()) {
        final String message = String
                     .format(
                             "Team %d will be in %s when performance round %s starts",
                             ti.getTeamNumber(), "technical", performanceName);
        violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, ti.getTechnical(), performanceTime,
                                               message));
      }
      else if (ti.getTechnical().getTime()
          + getSubjectiveDuration() + getChangetime() > performanceTime.getTime()) {
        final String message = String
                     .format(
                             "Team %d has doesn't have enough time between %s and performance round %s (need %d minutes)",
                             ti.getTeamNumber(), "technical", performanceName, getChangetimeAsMinutes());
        violations.add(new ConstraintViolation(false, ti.getTeamNumber(), null, ti.getTechnical(), performanceTime,
                                               message));
      }
    } else {
      if (performanceTime.getTime()
          + getPerformanceDuration() > ti.getTechnical().getTime()) {
        final String message = String
                     .format(
                             "Team %d wil be in %s when performance round %s starts",
                             ti.getTeamNumber(), "technical", performanceName);
        violations.add(new ConstraintViolation(true, ti.getTeamNumber(), null, ti.getTechnical(), performanceTime,
                                               message));
      }
      else if (performanceTime.getTime()
          + getPerformanceDuration() + getChangetime() > ti.getTechnical().getTime()) {
        final String message = String
                     .format(
                             "Team %d has doesn't have enough time between %s and performance round %s (need %d minutes)",
                             ti.getTeamNumber(), "technical", performanceName, getChangetimeAsMinutes());
        violations.add(new ConstraintViolation(false, ti.getTeamNumber(), null, ti.getTechnical(), performanceTime,
                                               message));
      }
    }
  }

  private void verifyPerformanceVsPresentation(final Collection<ConstraintViolation> violations,
                                               final TeamScheduleInfo ti,
                                               final Date performanceTime,
                                               final String performanceName) {
    if (ti.getPresentation().before(performanceTime)) {
      if (ti.getPresentation().getTime()
          + getSubjectiveDuration() > performanceTime.getTime()) {
        final String message = String
                     .format(
                             "Team %d will be in %s when performance round %s starts",
                             ti.getTeamNumber(), "presentation", performanceName);
        violations.add(new ConstraintViolation(true, ti.getTeamNumber(), ti.getPresentation(), null, performanceTime,
                                               message));
      }
      else if (ti.getPresentation().getTime()
          + getSubjectiveDuration() + getChangetime() > performanceTime.getTime()) {
        final String message = String
                     .format(
                             "Team %d has doesn't have enough time between %s and performance round %s (need %d minutes)",
                             ti.getTeamNumber(), "presentation", performanceName, getChangetimeAsMinutes());
        violations.add(new ConstraintViolation(false, ti.getTeamNumber(), ti.getPresentation(), null, performanceTime,
                                               message));
      }
    } else {
      if (performanceTime.getTime()
          + getPerformanceDuration() > ti.getPresentation().getTime()) {
        final String message = String
                     .format(
                             "Team %d wil be in %s when performance round %s starts",
                             ti.getTeamNumber(), "presentation", performanceName);
        violations.add(new ConstraintViolation(true, ti.getTeamNumber(), ti.getPresentation(), null, performanceTime,
                                               message));
      }
      else if (performanceTime.getTime()
          + getPerformanceDuration() + getChangetime() > ti.getPresentation().getTime()) {
        final String message = String
                     .format(
                             "Team %d has doesn't have enough time between %s and performance round %s (need %d minutes)",
                             ti.getTeamNumber(), "presentation", performanceName, getChangetimeAsMinutes());
        violations.add(new ConstraintViolation(false, ti.getTeamNumber(), ti.getPresentation(), null, performanceTime,
                                               message));
      }
    }
  }

  /**
   * @return the schedule info or null if the last line is read
   * @throws ScheduleParseException if there is an error with the schedule being
   *           read
   */
  private TeamScheduleInfo parseLine(final CellFileReader reader) throws IOException, ParseException,
      ScheduleParseException {
    final String[] line = reader.readNext();

    try {

      final String teamNumberStr = line[_teamNumColumn];
      if (teamNumberStr.length() < 1) {
        // hit empty row
        return null;
      }

      final int teamNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(teamNumberStr).intValue();
      final TeamScheduleInfo ti = new TeamScheduleInfo(reader.getLineNumber(), getNumberOfRounds(), teamNumber);
      ti.setTeamName(line[_teamNameColumn]);
      ti.setOrganization(line[_organizationColumn]);
      ti.setDivision(line[_divisionColumn]);
      final String presentationStr = line[_presentationColumn];
      if ("".equals(presentationStr)) {
        // If we got an empty string, then we must have hit the end
        return null;
      }
      ti.setPresentation(parseDate(presentationStr));

      final String technicalStr = line[_technicalColumn];
      if ("".equals(technicalStr)) {
        // If we got an empty string, then we must have hit the end
        return null;
      }
      ti.setTechnical(parseDate(technicalStr));

      ti.setJudge(line[_judgeGroupColumn]);

      for (int perfNum = 0; perfNum < getNumberOfRounds(); ++perfNum) {
        final String perf1Str = line[_perfColumn[perfNum]];
        if ("".equals(perf1Str)) {
          // If we got an empty string, then we must have hit the end
          return null;
        }
        ti.setPerf(perfNum, parseDate(perf1Str));
        String table = line[_perfTableColumn[perfNum]];
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
   * Time to allocate for either presentation or technical judging.
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
        / SECONDS_PER_MINUTE / MILLISECONDS_PER_SECOND;
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
   * Page footer for schedule.
   */
  private static final class PageFooter extends PdfPageEventHelper {
    @Override
    public void onEndPage(final PdfWriter writer,
                          final Document document) {
      final Rectangle page = document.getPageSize();
      final PdfPTable foot = new PdfPTable(1);
      final PdfPCell cell = new PdfPCell(new Phrase("- "
          + writer.getPageNumber() + " -"));
      cell.setHorizontalAlignment(Element.ALIGN_CENTER);
      cell.setUseDescender(true);
      foot.addCell(cell);
      foot.setTotalWidth(page.getWidth()
          - document.leftMargin() - document.rightMargin());
      foot.writeSelectedRows(0, -1, document.leftMargin(), document.bottomMargin(), writer.getDirectContent());
    }
  }

}
