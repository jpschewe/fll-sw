/*
 * Copyright (c) 2000-2008 HighTechKids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.scheduler;

import java.awt.Color;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
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

import org.apache.log4j.Logger;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Cell;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Table;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import fll.Utilities;
import fll.util.CellFileReader;
import fll.util.ExcelCellReader;

/**
 * Parse a CSV file representing the detailed schedule for a tournament.
 * 
 * @author jpschewe
 * @version $Revision$
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

  static final int NUMBER_OF_ROUNDS = 3;

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

  public static final long SECONDS_PER_MILLISECOND = 1000;

  private long performanceDuration = 5
      * SECONDS_PER_MINUTE * SECONDS_PER_MILLISECOND;

  private long subjectiveDuration = 20
      * SECONDS_PER_MINUTE * SECONDS_PER_MILLISECOND;

  private long changetime = 15
      * SECONDS_PER_MINUTE * SECONDS_PER_MILLISECOND;

  private long performanceChangetime = 45
      * SECONDS_PER_MINUTE * SECONDS_PER_MILLISECOND;

  private long specialPerformanceChangetime = 30
      * SECONDS_PER_MINUTE * SECONDS_PER_MILLISECOND;

  private final Map<Date, Map<String, List<TeamScheduleInfo>>> _matches = new HashMap<Date, Map<String, List<TeamScheduleInfo>>>();

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
   * @param args files to parse
   */
  public static void main(final String[] args) {
    if (args.length < 1) {
      LOGGER.error("No files specified, nothing to do");
      System.exit(1);
    }

    for (final String arg : args) {
      final File f = new File(arg);
      if (f.isDirectory()) {
        final File[] files = f.listFiles(EXCEL_FILE_FILTER);
        for (final File f2 : files) {
          checkFile(f2);
        }
      } else {
        checkFile(f);
      }
    }

    LOGGER.info("Finished, if no errors found, you're good");
    System.exit(0);
  }

  public static final FileFilter EXCEL_FILE_FILTER = new FileFilter() {
    public boolean accept(final File f) {
      return f.getName().endsWith(".xls")
          || f.getName().endsWith(".xslx");
    }
  };

  /**
   * Parse, verify, compute the general schedule and output the detailed
   * schedules.
   * 
   * @see #parseFile()
   * @see #verifySchedule()
   * @see #computeGeneralSchedule()
   * @see #outputDetailedSchedules()
   */
  private static void checkFile(final File f) {
    try {
      final ParseSchedule ps = new ParseSchedule(f);

      final Collection<ConstraintViolation> violations = ps.verifySchedule();

      if (violations.isEmpty()) {
        ps.computeGeneralSchedule();

        try {
          ps.outputDetailedSchedules();
        } catch (final DocumentException e) {
          throw new RuntimeException("Error creating PDF document", e);
        }
      } else {
        for (final ConstraintViolation violation : violations) {
          LOGGER.error(violation.getMessage());
        }
      }
    } catch (final ParseException ioe) {
      LOGGER.fatal(ioe, ioe);
      System.exit(1);
    } catch (final IOException ioe) {
      LOGGER.fatal(ioe, ioe);
      System.exit(1);
    }
  }

  public ParseSchedule(final File f) throws IOException, ParseException {
    _file = f;
    
    LOGGER.info(new Formatter().format("Reading file %s", _file.getAbsoluteFile()));

    if (!_file.canRead()
        || !_file.isFile()) {
      throw new RuntimeException("File is not readable or not a file: "
          + _file.getAbsolutePath());
    }

    final CellFileReader reader = new ExcelCellReader(_file);

    findColumns(reader);
    parseData(reader);
    reader.close();
  }

  /**
   * Find the index of the columns.
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

      for (int i = 0; i < line.length; ++i) {
        if (line[i].equals(TEAM_NUMBER_HEADER)) {
          _teamNumColumn = i;
        } else if (line[i].contains("Organization")) {
          _organizationColumn = i;
        } else if (line[i].equals("Team Name")) {
          _teamNameColumn = i;
        } else if (line[i].equals(DIVISION_HEADER)) {
          _divisionColumn = i;
        } else if (line[i].equals(PRESENTATION_HEADER)) {
          _presentationColumn = i;
        } else if (line[i].equals(TECHNICAL_HEADER)) {
          _technicalColumn = i;
        } else if (line[i].equals(JUDGE_GROUP_HEADER)) {
          _judgeGroupColumn = i;
        } else if (line[i].equals(PERF_1_HEADER)) {
          _perf1Column = i;
        } else if (line[i].equals(PERF_1_TABLE_HEADER)) {
          _perf1TableColumn = i;
        } else if (line[i].equals(PERF_2_HEADER)) {
          _perf2Column = i;
        } else if (line[i].equals(PERF_2_TABLE_HEADER)) {
          _perf2TableColumn = i;
        } else if (line[i].equals(PERF_3_HEADER)) {
          _perf3Column = i;
        } else if (line[i].equals(PERF_3_TABLE_HEADER)) {
          _perf3TableColumn = i;
        }
      }
    }

    if (-1 == _teamNumColumn) {
      throw new RuntimeException("Could not find teamNumColumn");
    }

    if (-1 == _teamNameColumn) {
      throw new RuntimeException("Could not find teamNamColumn");
    }

    if (-1 == _organizationColumn) {
      throw new RuntimeException("Could not find organizationColumn");
    }

    if (-1 == _divisionColumn) {
      throw new RuntimeException("Could not find divisionColumn");
    }
    if (-1 == _presentationColumn) {
      throw new RuntimeException("Could not find presentationColumn");
    }
    if (-1 == _technicalColumn) {
      throw new RuntimeException("Could not find technicalColumn");
    }
    if (-1 == _judgeGroupColumn) {
      throw new RuntimeException("Could not find judgeGroupColumn");
    }
    if (-1 == _perf1Column) {
      throw new RuntimeException("Could not find perf1Column");
    }
    if (-1 == _perf1TableColumn) {
      throw new RuntimeException("Could not find perf1TableColumn");
    }
    if (-1 == _perf2Column) {
      throw new RuntimeException("Could not find perf2Column");
    }
    if (-1 == _perf2TableColumn) {
      throw new RuntimeException("Could not find perf2TableColumn");
    }
    if (-1 == _perf3Column) {
      throw new RuntimeException("Could not find perf3Column");
    }
    if (-1 == _perf3TableColumn) {
      throw new RuntimeException("Could not find perf3TableColumn");
    }
  }

  /**
   * Parse the data of the schedule.
   * 
   * @throws IOException
   */
  private void parseData(final CellFileReader reader) throws IOException, ParseException {
    TeamScheduleInfo ti;
    while (null != (ti = parseLine(reader))) {
      _schedule.add(ti);

      // keep track of some meta information
      for (int round = 0; round < ParseSchedule.NUMBER_OF_ROUNDS; ++round) {
        _tableColors.add(ti.getPerfTableColor(round));
        addToMatches(_matches, ti, round);
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

    final int numberOfTableColors = _tableColors.size();
    final int numJudges = _judges.size();
    verifyPerformanceAtTime(constraintViolations, numberOfTableColors);
    verifyPresentationAtTime(constraintViolations, numJudges);
    verifyTechnicalAtTime(constraintViolations, numJudges);

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
  private TeamScheduleInfo findNextTeam(final Date time, final String table, final int side, final int round) {
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
  private TeamScheduleInfo checkIfTeamNeedsToStay(final TeamScheduleInfo si, final int round) {
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

    for (int round = 0; round < NUMBER_OF_ROUNDS; ++round) {
      Collections.sort(_schedule, getPerformanceComparator(round));

      // list of teams staying around to even up the teams
      final List<TeamScheduleInfo> teamsStaying = new LinkedList<TeamScheduleInfo>();

      final Table table = createTable(6);
      table.setWidths(new float[] { 2, 1, 3, 3, 2, 1 });

      int row = 0;
      table.addCell(createHeaderCell("Team #"), row, 0);
      table.addCell(createHeaderCell("Div"), row, 1);
      table.addCell(createHeaderCell("School or Organization"), row, 2);
      table.addCell(createHeaderCell("Team Name"), row, 3);
      table.addCell(createHeaderCell(new Formatter().format("Perf #%d", (round + 1)).toString()), row, 4);
      table.addCell(createHeaderCell(new Formatter().format("Perf %d Table", (round + 1)).toString()), row, 5);
      table.endHeaders();
      ++row;

      for (final TeamScheduleInfo si : _schedule) {
        // check if team needs to stay and color the cell magenta if they do
        final Color backgroundColor;
        if (null != checkIfTeamNeedsToStay(si, round)) {
          teamsStaying.add(si);
          backgroundColor = Color.MAGENTA;
        } else {
          backgroundColor = null;
        }

        table.addCell(createCell(String.valueOf(si.getTeamNumber())), row, 0);
        table.addCell(createCell(si.getDivision()), row, 1);
        table.addCell(createCell(si.getOrganization()), row, 2);
        table.addCell(createCell(si.getTeamName()), row, 3);
        table.addCell(createCell(OUTPUT_DATE_FORMAT.get().format(si.getPerf(round)), backgroundColor), row, 4);
        table.addCell(createCell(si.getPerfTableColor(round)
            + " " + si.getPerfTableSide(round), backgroundColor), row, 5);

        ++row;
      }

      detailedSchedules.add(table);

      // output teams staying
      if (!teamsStaying.isEmpty()) {
        final String formatString = "Team %d will please stay at the table and compete again - score will not count.";
        final Table stayingTable = createTable(1);
        for (final TeamScheduleInfo si : teamsStaying) {
          stayingTable.addCell(createCell(new Formatter().format(formatString, si.getTeamNumber()).toString(), Color.MAGENTA));
        }
        detailedSchedules.add(stayingTable);

      }

      detailedSchedules.add(Chunk.NEXTPAGE);
    }
  }

  private static Cell createCell(final String text) throws BadElementException {
    final Cell cell = createBasicCell(new Chunk(text));
    return cell;
  }

  private static Cell createCell(final String text, final Color backgroundColor) throws BadElementException {
    final Cell cell = createCell(text);
    if (null != backgroundColor) {
      cell.setBackgroundColor(backgroundColor);
    }
    return cell;
  }

  private static Cell createBasicCell(final Chunk chunk) throws BadElementException {
    final Cell cell = new Cell(chunk);
    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
    cell.setUseDescender(true);
    return cell;
  }

  private static Cell createHeaderCell(final String text) throws BadElementException {
    final Chunk chunk = new Chunk(text);
    chunk.getFont().setStyle(Font.BOLD);
    final Cell cell = createBasicCell(chunk);
    cell.setHeader(true);

    return cell;
  }

  private static Table createTable(final int columns) throws BadElementException {
    final Table table = new Table(columns);
    table.setCellsFitPage(true);
    table.setWidth(100);
    return table;
  }

  private void outputPresentationSchedule(final Document detailedSchedules) throws DocumentException {
    final Table table = createTable(6);
    table.setWidths(new float[] { 2, 1, 3, 3, 2, 1 });

    Collections.sort(_schedule, PRESENTATION_COMPARATOR);
    int row = 0;
    table.addCell(createHeaderCell("Team #"), row, 0);
    table.addCell(createHeaderCell("Div"), row, 1);
    table.addCell(createHeaderCell("School or Organization"), row, 2);
    table.addCell(createHeaderCell("Team Name"), row, 3);
    table.addCell(createHeaderCell("Presentation"), row, 4);
    table.addCell(createHeaderCell("Judging Station"), row, 5);
    table.endHeaders();
    ++row;

    for (final TeamScheduleInfo si : _schedule) {
      table.addCell(createCell(String.valueOf(si.getTeamNumber())), row, 0);
      table.addCell(createCell(si.getDivision()), row, 1);
      table.addCell(createCell(si.getOrganization()), row, 2);
      table.addCell(createCell(si.getTeamName()), row, 3);
      table.addCell(createCell(OUTPUT_DATE_FORMAT.get().format(si.getPresentation())), row, 4);
      table.addCell(createCell(si.getJudge()), row, 5);

      ++row;
    }

    detailedSchedules.add(table);

  }

  private void outputTechnicalSchedule(final Document detailedSchedules) throws DocumentException {
    Collections.sort(_schedule, TECHNICAL_COMPARATOR);

    final Table table = createTable(6);
    table.setWidths(new float[] { 2, 1, 3, 3, 2, 1 });

    int row = 0;

    // header
    table.addCell(createHeaderCell("Team #"), row, 0);
    table.addCell(createHeaderCell("Div"), row, 1);
    table.addCell(createHeaderCell("School or Organization"), row, 2);
    table.addCell(createHeaderCell("Team Name"), row, 3);
    table.addCell(createHeaderCell("Technical"), row, 4);
    table.addCell(createHeaderCell("Judging Station"), row, 5);
    table.endHeaders();
    ++row;

    for (final TeamScheduleInfo si : _schedule) {
      table.addCell(createCell(String.valueOf(si.getTeamNumber())), row, 0);
      table.addCell(createCell(si.getDivision()), row, 1);
      table.addCell(createCell(si.getOrganization()), row, 2);
      table.addCell(createCell(si.getTeamName()), row, 3);
      table.addCell(createCell(OUTPUT_DATE_FORMAT.get().format(si.getTechnical())), row, 4);
      table.addCell(createCell(si.getJudge()), row, 5);

      ++row;
    }
    detailedSchedules.add(table);

  }

  /**
   * Sort by division, then judge, then by time.
   */
  private static final Comparator<TeamScheduleInfo> PRESENTATION_COMPARATOR = new Comparator<TeamScheduleInfo>() {
    public int compare(final TeamScheduleInfo one, final TeamScheduleInfo two) {
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
    public int compare(final TeamScheduleInfo one, final TeamScheduleInfo two) {
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
    return new Comparator<TeamScheduleInfo>() {
      public int compare(final TeamScheduleInfo one, final TeamScheduleInfo two) {
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
    };
  }

  /**
   * Compute the general schedule and output it into the log.
   */
  public void computeGeneralSchedule() {
    Date minTechnical = null;
    Date maxTechnical = null;
    Date minPresentation = null;
    Date maxPresentation = null;
    final Date[] minPerf = new Date[NUMBER_OF_ROUNDS];
    final Date[] maxPerf = new Date[NUMBER_OF_ROUNDS];

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

      for (int i = 0; i < NUMBER_OF_ROUNDS; ++i) {
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
    LOGGER.info("min technical: "
        + OUTPUT_DATE_FORMAT.get().format(minTechnical));
    LOGGER.info("max technical: "
        + OUTPUT_DATE_FORMAT.get().format(maxTechnical));
    LOGGER.info("min presentation: "
        + OUTPUT_DATE_FORMAT.get().format(minPresentation));
    LOGGER.info("max presentation: "
        + OUTPUT_DATE_FORMAT.get().format(maxPresentation));
    for (int i = 0; i < NUMBER_OF_ROUNDS; ++i) {
      LOGGER.info("min performance round "
          + (i + 1) + ": " + OUTPUT_DATE_FORMAT.get().format(minPerf[i]));
      LOGGER.info("max performance round "
          + (i + 1) + ": " + OUTPUT_DATE_FORMAT.get().format(maxPerf[i]));
    }

  }

  /**
   * Add the data from the specified round of the specified TeamScheduleInfo to
   * matches.
   * 
   * @param matches the list of matches
   * @param ti the schedule info
   * @param round the round we care about
   * @return true if this succeeds, false if this shows too many teams on the
   *         table
   */
  private static boolean addToMatches(final Map<Date, Map<String, List<TeamScheduleInfo>>> matches, final TeamScheduleInfo ti, final int round) {
    final Map<String, List<TeamScheduleInfo>> timeMatches;
    if (matches.containsKey(ti.getPerf(round))) {
      timeMatches = matches.get(ti.getPerf(round));
    } else {
      timeMatches = new HashMap<String, List<TeamScheduleInfo>>();
      matches.put(ti.getPerf(round), timeMatches);
    }

    final List<TeamScheduleInfo> tableMatches;
    if (timeMatches.containsKey(ti.getPerfTableColor(round))) {
      tableMatches = timeMatches.get(ti.getPerfTableColor(round));
    } else {
      tableMatches = new LinkedList<TeamScheduleInfo>();
      timeMatches.put(ti.getPerfTableColor(round), tableMatches);
    }

    tableMatches.add(ti);

    if (tableMatches.size() > 2) {
      LOGGER.error(new Formatter().format("Too many teams competing on table: %s at time: %s. Teams: %s", ti.getPerfTableColor(round),
                                          OUTPUT_DATE_FORMAT.get().format(ti.getPerf(round)), tableMatches));
      return false;
    } else {
      return true;
    }
  }

  /**
   * Verify that there are no more than <code>numberOfTables</code> teams
   * performing at the same time.
   */
  private void verifyPerformanceAtTime(final Collection<ConstraintViolation> violations, final int numberOfTableColors) {
    // constraint set 6
    final Map<Date, Set<TeamScheduleInfo>> teamsAtTime = new HashMap<Date, Set<TeamScheduleInfo>>();
    for (final TeamScheduleInfo si : _schedule) {
      for (int round = 0; round < NUMBER_OF_ROUNDS; ++round) {
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
      if (entry.getValue().size() > numberOfTableColors * 2) {
        final String message = String.format("There are too many teams in performance at %s", OUTPUT_DATE_FORMAT.get().format(entry.getKey()));
        violations.add(new ConstraintViolation(ConstraintViolation.NO_TEAM, null, null, null, message));
      }
    }
  }

  /**
   * Ensure that no more than 1 team is in presentation judging at once.
   */
  private void verifyPresentationAtTime(final Collection<ConstraintViolation> violations, final int numJudges) {
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
      if (entry.getValue().size() > numJudges) {
        final String message = String.format("There are too many teams in presentation at %s in presentation", OUTPUT_DATE_FORMAT.get().format(entry.getKey()));
        violations.add(new ConstraintViolation(ConstraintViolation.NO_TEAM, null, null, null, message));
      }

      final Set<String> judges = new HashSet<String>();
      for (final TeamScheduleInfo ti : entry.getValue()) {
        if (!judges.add(ti.getJudge())) {
          final String message = String.format("Presentation judge %s cannot see more than one team at %s in presentation", ti.getJudge(),
                                               OUTPUT_DATE_FORMAT.get().format(ti.getPresentation()));
          violations.add(new ConstraintViolation(ConstraintViolation.NO_TEAM, null, null, null, message));
        }
      }

    }
  }

  /**
   * Ensure that no more than 1 team is in technical judging at once.
   */
  private void verifyTechnicalAtTime(final Collection<ConstraintViolation> violations, final int numJudges) {
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
      if (entry.getValue().size() > numJudges) {
        final String message = String.format("There are too many teams in technical at %s in technical", OUTPUT_DATE_FORMAT.get().format(entry.getKey()));
        violations.add(new ConstraintViolation(ConstraintViolation.NO_TEAM, null, null, null, message));
      }

      final Set<String> judges = new HashSet<String>();
      for (final TeamScheduleInfo ti : entry.getValue()) {
        if (!judges.add(ti.getJudge())) {
          final String message = String.format("Technical judge %s cannot see more than one team at %s in presentation", ti.getJudge(),
                                               OUTPUT_DATE_FORMAT.get().format(ti.getPresentation()));
          violations.add(new ConstraintViolation(ConstraintViolation.NO_TEAM, null, null, null, message));
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
  private int findOpponentRound(final TeamScheduleInfo ti, final int round) {
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
  private TeamScheduleInfo findOpponent(final TeamScheduleInfo ti, final int round) {
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

  /**
   * @return null if ok, message if not ok
   */
  private String verifyPerformanceVsSubjective(final int teamNumber,
                                               final Date subjectiveTime,
                                               final String subjectiveName,
                                               final Date performanceTime,
                                               final String performanceName) {
    if (subjectiveTime.before(performanceTime)) {
      if (subjectiveTime.getTime()
          + getSubjectiveDuration() + getChangetime() > performanceTime.getTime()) {
        return String.format("Team %d has doesn't have enough time between %s and performance round %s", teamNumber, subjectiveName, performanceName);
      }
    } else {
      if (performanceTime.getTime()
          + getPerformanceDuration() + getChangetime() > subjectiveTime.getTime()) {
        return String.format("Team %d has doesn't have enough time between %s and performance round %s", teamNumber, subjectiveName, performanceName);
      }
    }
    return null;
  }

  private void verifyTeam(final Collection<ConstraintViolation> violations, final TeamScheduleInfo ti) {
    // constraint set 1
    if (ti.getPresentation().before(ti.getTechnical())) {
      if (ti.getPresentation().getTime()
          + getSubjectiveDuration() + getChangetime() > ti.getTechnical().getTime()) {
        final String message = String.format("Team %d has doesn't have enough time between presentation and technical", ti.getTeamNumber());
        violations.add(new ConstraintViolation(ti.getTeamNumber(), ti.getPresentation(), ti.getTechnical(), null, message));
        return;
      }
    } else {
      if (ti.getTechnical().getTime()
          + getSubjectiveDuration() + getChangetime() > ti.getPresentation().getTime()) {
        final String message = String.format("Team %d has doesn't have enough time between presentation and technical", ti.getTeamNumber());
        violations.add(new ConstraintViolation(ti.getTeamNumber(), ti.getPresentation(), ti.getTechnical(), null, message));
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
        + getPerformanceDuration() + changetime > ti.getPerf(1).getTime()) {
      final String message = String.format("Team %d doesn't have enough time (%d minutes) between performance 1 and performance 2: %s - %s",
                                           ti.getTeamNumber(), changetime
                                               / 1000 / SECONDS_PER_MINUTE, OUTPUT_DATE_FORMAT.get().format(ti.getPerf(0)),
                                           OUTPUT_DATE_FORMAT.get().format(ti.getPerf(1)));
      violations.add(new ConstraintViolation(ti.getTeamNumber(), null, null, ti.getPerf(1), message));
    }

    if (ti.getPerf(1).getTime()
        + getPerformanceDuration() + getPerformanceChangetime() > ti.getPerf(2).getTime()) {
      final String message = String.format("Team %d doesn't have enough time (%d minutes) between performance 2 and performance 3: %s - %s",
                                           ti.getTeamNumber(), changetime
                                               / 1000 / SECONDS_PER_MINUTE, OUTPUT_DATE_FORMAT.get().format(ti.getPerf(1)),
                                           OUTPUT_DATE_FORMAT.get().format(ti.getPerf(2)));
      violations.add(new ConstraintViolation(ti.getTeamNumber(), null, null, ti.getPerf(2), message));
    }

    // constraint set 4
    for (int round = 0; round < NUMBER_OF_ROUNDS; ++round) {
      final String message = verifyPerformanceVsSubjective(ti.getTeamNumber(), ti.getPresentation(), "presentation", ti.getPerf(round),
                                                           String.valueOf(round + 1));
      if (null != message) {
        violations.add(new ConstraintViolation(ti.getTeamNumber(), ti.getPresentation(), null, ti.getPerf(round), message));
      }
    }

    // constraint set 5
    for (int round = 0; round < NUMBER_OF_ROUNDS; ++round) {
      final String message = verifyPerformanceVsSubjective(ti.getTeamNumber(), ti.getTechnical(), "technical", ti.getPerf(round), String.valueOf(round + 1));
      if (null != message) {
        violations.add(new ConstraintViolation(ti.getTeamNumber(), null, ti.getTechnical(), ti.getPerf(round), message));
      }
    }

    // make sure that all opponents are different & sides are different
    for (int round = 0; round < NUMBER_OF_ROUNDS; ++round) {
      final TeamScheduleInfo opponent = findOpponent(ti, round);
      if (null != opponent) {
        int opponentSide = -1;
        // figure out which round matches up
        for (int oround = 0; oround < NUMBER_OF_ROUNDS; ++oround) {
          if (opponent.getPerf(oround).equals(ti.getPerf(round))) {
            opponentSide = opponent.getPerfTableSide(oround);
            break;
          }
        }
        if (-1 == opponentSide) {
          final String message = String.format("Unable to find time match for rounds between team %d and team %d at time %s", ti.getTeamNumber(),
                                               opponent.getTeamNumber(), OUTPUT_DATE_FORMAT.get().format(ti.getPerf(round)));
          violations.add(new ConstraintViolation(ti.getTeamNumber(), null, null, ti.getPerf(round), message));
        } else {
          if (opponentSide == ti.getPerfTableSide(round)) {
            final String message = String.format("Team %d and team %d are both on table %s side %d at the same time for round %d", ti.getTeamNumber(),
                                                 opponent.getTeamNumber(), ti.getPerfTableColor(round), ti.getPerfTableSide(round), (round + 1));
            violations.add(new ConstraintViolation(ti.getTeamNumber(), null, null, ti.getPerf(round), message));
          }
        }

        for (int r = round + 1; r < NUMBER_OF_ROUNDS; ++r) {
          final TeamScheduleInfo otherOpponent = findOpponent(ti, r);
          if (otherOpponent != null
              && opponent.equals(otherOpponent)) {
            final String message = String.format("Team %d competes against %d more than once", ti.getTeamNumber(), opponent.getTeamNumber());
            violations.add(new ConstraintViolation(ti.getTeamNumber(), null, null, null, message));
            violations.add(new ConstraintViolation(opponent.getTeamNumber(), null, null, null, message));
          }
        }
      } else {
        // only a problem if this is not the last round and we don't have an odd number of teams
        if (!(round == NUMBER_OF_ROUNDS - 1 && (_schedule.size() % 2) == 1)) {
          final String message = String.format("Team %d has no opponent for round %d", ti.getTeamNumber(), (round + 1));
          violations.add(new ConstraintViolation(ti.getTeamNumber(), null, null, ti.getPerf(round), message));
        }
      }
    }

    // check if the team needs to stay for any extra founds
    for (int round = 0; round < NUMBER_OF_ROUNDS; ++round) {
      final TeamScheduleInfo next = checkIfTeamNeedsToStay(ti, round);
      if (null != next) {
        // everything else checked out, only only need to check the end time
        // against subjective and the next round
        String message = verifyPerformanceVsSubjective(ti.getTeamNumber(), ti.getPresentation(), "presentation", next.getPerf(round), "extra");
        if (null != message) {
          violations.add(new ConstraintViolation(ti.getTeamNumber(), ti.getPresentation(), null, ti.getPerf(round), message));
        }

        message = verifyPerformanceVsSubjective(ti.getTeamNumber(), ti.getTechnical(), "technical", next.getPerf(round), "extra");
        if (null != message) {
          violations.add(new ConstraintViolation(ti.getTeamNumber(), null, ti.getTechnical(), ti.getPerf(round), message));
        }

        if (round + 1 < NUMBER_OF_ROUNDS) {
          if (next.getPerf(round).getTime()
              + getPerformanceDuration() + getPerformanceChangetime() > ti.getPerf(round + 1).getTime()) {
            message = String.format("Team %d doesn't have enough time (%d minutes) between performance %d and performance extra: %s - %s", ti.getTeamNumber(),
                                    changetime
                                        / 1000 / SECONDS_PER_MINUTE, round, OUTPUT_DATE_FORMAT.get().format(next.getPerf(round)),
                                    OUTPUT_DATE_FORMAT.get().format(ti.getPerf(round + 1)));
            violations.add(new ConstraintViolation(ti.getTeamNumber(), null, null, ti.getPerf(round + 1), message));
          }
        }

      }
    }
  }

  /**
   * @return the schedule info or null if there was an error, or the last line
   *         is hit
   */
  private TeamScheduleInfo parseLine(final CellFileReader reader) throws IOException, ParseException {
    final String[] line = reader.readNext();

    try {

      final String teamNumberStr = line[_teamNumColumn];
      if (teamNumberStr.length() < 1) {
        // hit empty row
        return null;
      }
      final TeamScheduleInfo ti = new TeamScheduleInfo(reader.getLineNumber());
      ti.setTeamNumber(Utilities.NUMBER_FORMAT_INSTANCE.parse(teamNumberStr).intValue());
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

      final String perf1Str = line[_perf1Column];
      if ("".equals(perf1Str)) {
        // If we got an empty string, then we must have hit the end
        return null;
      }
      ti.setPerf(0, parseDate(perf1Str));
      String table = line[_perf1TableColumn];
      String[] tablePieces = table.split(" ");
      if (tablePieces.length != 2) {
        throw new RuntimeException("Error parsing table information from: "
            + table);
      }
      ti.setPerfTableColor(0, tablePieces[0]);
      ti.setPerfTableSide(0, Utilities.NUMBER_FORMAT_INSTANCE.parse(tablePieces[1]).intValue());
      if (ti.getPerfTableSide(0) > 2
          || ti.getPerfTableSide(0) < 1) {
        // FIXME need to do something more serious here so the GUI catches it
        LOGGER.error("There are only two sides to the table, number must be 1 or 2 team: "
            + ti.getTeamNumber() + " round 1");
      }

      table = line[_perf2TableColumn];
      tablePieces = table.split(" ");
      if (tablePieces.length != 2) {
        throw new RuntimeException("Error parsing table information from: "
            + table);
      }
      ti.setPerfTableColor(1, tablePieces[0]);
      ti.setPerfTableSide(1, Utilities.NUMBER_FORMAT_INSTANCE.parse(tablePieces[1]).intValue());
      if (ti.getPerfTableSide(1) > 2
          || ti.getPerfTableSide(1) < 1) {
        // FIXME need to do something more serious here so the GUI catches it
        LOGGER.error("There are only two sides to the table, number must be 1 or 2 team: "
            + ti.getTeamNumber() + " round 2");
      }
      final String perf2Str = line[_perf2Column];
      if ("".equals(perf2Str)) {
        // If we got an empty string, then we must have hit the end
        return null;
      }
      ti.setPerf(1, parseDate(perf2Str));

      table = line[_perf3TableColumn];
      tablePieces = table.split(" ");
      if (tablePieces.length != 2) {
        throw new RuntimeException("Error parsing table information from: "
            + table);
      }
      ti.setPerfTableColor(2, tablePieces[0]);
      ti.setPerfTableSide(2, Utilities.NUMBER_FORMAT_INSTANCE.parse(tablePieces[1]).intValue());
      if (ti.getPerfTableSide(2) > 2
          || ti.getPerfTableSide(2) < 1) {
        // FIXME need to do something more serious here so the GUI catches it
        LOGGER.error("There are only two sides to the table, number must be 1 or 2 team: "
            + ti.getTeamNumber() + " round 2");
      }
      final String perf3Str = line[_perf3Column];
      if ("".equals(perf3Str)) {
        // If we got an empty string, then we must have hit the end
        return null;
      }
      ti.setPerf(2, parseDate(perf3Str));

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
    if (s.indexOf("AM") > 0
        || s.indexOf("PM") > 0) {
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
  public void setSpecialPerformanceChangetime(long specialPerformanceChangetime) {
    this.specialPerformanceChangetime = specialPerformanceChangetime;
  }

  /**
   * @param performanceChangetime the performanceChangetime to set
   */
  public void setPerformanceChangetime(long performanceChangetime) {
    this.performanceChangetime = performanceChangetime;
  }

  /**
   * @param changetime the changetime to set
   */
  public void setChangetime(long changetime) {
    this.changetime = changetime;
  }

  /**
   * @param subjectiveDuration the subjectiveDuration to set
   */
  public void setSubjectiveDuration(long subjectiveDuration) {
    this.subjectiveDuration = subjectiveDuration;
  }

  /**
   * @param performanceDuration the performanceDuration to set
   */
  public void setPerformanceDuration(long performanceDuration) {
    this.performanceDuration = performanceDuration;
  }

  /**
   * Page footer for schedule.
   */
  private static final class PageFooter extends PdfPageEventHelper {
    @Override
    public void onEndPage(final PdfWriter writer, final Document document) {
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
