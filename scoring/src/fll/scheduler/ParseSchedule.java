/*
 * Copyright (c) 2000-2008 HighTechKids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.scheduler;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
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
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import net.mtu.eggplant.util.BasicFileFilter;

import org.apache.log4j.Logger;

import au.com.bytecode.opencsv.CSVReader;

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

  private static final int NUMBER_OF_ROUNDS = 3;

  private static final ThreadLocal<DateFormat> DATE_FORMAT_AM_PM = new ThreadLocal<DateFormat>() {
    @Override
    protected DateFormat initialValue() {
      return new SimpleDateFormat("hh:mm a");
    }
  };

  private static final ThreadLocal<DateFormat> DATE_FORMAT_AM_PM_SS = new ThreadLocal<DateFormat>() {
    @Override
    protected DateFormat initialValue() {
      return new SimpleDateFormat("hh:mm:ss a");
    }
  };

  private static final ThreadLocal<DateFormat> OUTPUT_DATE_FORMAT = new ThreadLocal<DateFormat>() {
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

  private final Map<Date, Map<String, List<TeamScheduleInfo>>> _matches = new HashMap<Date, Map<String, List<TeamScheduleInfo>>>();

  private final File _file;

  private final Set<String> _tableColors = new HashSet<String>();

  private final Set<String> _divisions = new HashSet<String>();

  private final Set<String> _judges = new HashSet<String>();

  private final List<TeamScheduleInfo> _schedule = new LinkedList<TeamScheduleInfo>();

  private static final Preferences PREFS = Preferences.userNodeForPackage(ParseSchedule.class);

  private static final String STARTING_DIRECTORY_PREF = "startingDirectory";

  /**
   * @param args
   */
  public static void main(final String[] args) {
    final String startingDirectory = PREFS.get(STARTING_DIRECTORY_PREF, null);

    final JFileChooser fileChooser = new JFileChooser();
    final FileFilter filter = new BasicFileFilter("csv or directory", "csv");
    fileChooser.setFileFilter(filter);
    fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    fileChooser.setMultiSelectionEnabled(true);
    if (null != startingDirectory) {
      fileChooser.setCurrentDirectory(new File(startingDirectory));
    }

    final int returnVal = fileChooser.showOpenDialog(null);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      final File currentDirectory = fileChooser.getCurrentDirectory();
      PREFS.put(STARTING_DIRECTORY_PREF, currentDirectory.getAbsolutePath());

      final File[] selectedFiles = fileChooser.getSelectedFiles();

      for (final File file : selectedFiles) {
        if (file.isDirectory()) {
          final File[] files = file.listFiles(new java.io.FileFilter() {
            public boolean accept(final File pathname) {
              return pathname.getName().endsWith(".csv");
            }
          });
          for (final File f : files) {
            final ParseSchedule ps = new ParseSchedule(f);

            try {
              ps.parseFile();
            } catch (final IOException ioe) {
              LOGGER.fatal(ioe, ioe);
              System.exit(1);
            }
          }
        } else if (file.isFile()) {
          final ParseSchedule ps = new ParseSchedule(file);

          try {
            ps.parseFile();
          } catch (final IOException ioe) {
            LOGGER.fatal(ioe, ioe);
            System.exit(1);
          }
        } else {
          LOGGER.error("No such file or directory: "
              + file.getAbsolutePath());
        }
      }
    }

    LOGGER.info("Finished, if no errors found, you're good");
  }

  public ParseSchedule(final File f) {
    _file = f;
  }

  /**
   * Find the index of the columns. If a column can't be found, output an error
   * and exit.
   * 
   * @throws IOException
   */
  private void findColumns(final CSVReader csvreader) throws IOException {

    while (_teamNumColumn == -1) {
      final String[] line = csvreader.readNext();
      if (null == line) {
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
      LOGGER.fatal("Could not find teamNumColumn");
      System.exit(1);
    }

    if (-1 == _teamNameColumn) {
      LOGGER.fatal("Could not find teamNamColumn");
      System.exit(1);
    }

    if (-1 == _organizationColumn) {
      LOGGER.fatal("Could not find organizationColumn");
      System.exit(1);
    }

    if (-1 == _divisionColumn) {
      LOGGER.fatal("Could not find divisionColumn");
      System.exit(1);
    }
    if (-1 == _presentationColumn) {
      LOGGER.fatal("Could not find presentationColumn");
      System.exit(1);
    }
    if (-1 == _technicalColumn) {
      LOGGER.fatal("Could not find technicalColumn");
      System.exit(1);
    }
    if (-1 == _judgeGroupColumn) {
      LOGGER.fatal("Could not find judgeGroupColumn");
      System.exit(1);
    }
    if (-1 == _perf1Column) {
      LOGGER.fatal("Could not find perf1Column");
      System.exit(1);
    }
    if (-1 == _perf1TableColumn) {
      LOGGER.fatal("Could not find perf1TableColumn");
      System.exit(1);
    }
    if (-1 == _perf2Column) {
      LOGGER.fatal("Could not find perf2Column");
      System.exit(1);
    }
    if (-1 == _perf2TableColumn) {
      LOGGER.fatal("Could not find perf2TableColumn");
      System.exit(1);
    }
    if (-1 == _perf3Column) {
      LOGGER.fatal("Could not find perf3Column");
      System.exit(1);
    }
    if (-1 == _perf3TableColumn) {
      LOGGER.fatal("Could not find perf3TableColumn");
      System.exit(1);
    }
  }

  /**
   * Parse the data of the schedule.
   * 
   * @throws IOException
   */
  private void parseData(final CSVReader csvreader) throws IOException {
    TeamScheduleInfo ti;
    while (null != (ti = parseLine(csvreader))) {
      _schedule.add(ti);

      // keep track of some meta information
      for (int round = 0; round < ti.perfTableColor.length; ++round) {
        _tableColors.add(ti.perfTableColor[round]);
        addToMatches(_matches, ti, round);
      }
      _divisions.add(ti.division);
      _judges.add(ti.judge);
    }
  }

  /**
   * Verify the schedule.
   * 
   * @return if the schedule is valid.
   * @throws IOException
   */
  private boolean verifySchedule() {
    // create separate local variables for each return so that the function is
    // guaranteed to be called. If this isn't done the short-circuit boolean
    // logic evaluation will prevent the function from being called.

    boolean retval = true;

    for (final TeamScheduleInfo verify : _schedule) {
      final boolean ret = verifyTeam(verify);
      retval &= ret;
    }

    final int numberOfTableColors = _tableColors.size();
    final int numJudges = _judges.size();
    boolean ret = verifyPerformanceAtTime(numberOfTableColors, _schedule);
    retval &= ret;
    ret = verifyPresentationAtTime(_schedule, numJudges);
    retval &= ret;
    ret = verifyTechnicalAtTime(_schedule, numJudges);
    retval &= ret;

    return retval;
  }

  public void parseFile() throws IOException {
    LOGGER.info(new Formatter().format("Reading file %s", _file.getAbsoluteFile()));

    if (!_file.canRead()
        || !_file.isFile()) {
      LOGGER.fatal("File is not readable or not a file: "
          + _file.getAbsolutePath());
      return;
    }

    final CSVReader csvreader = new CSVReader(new FileReader(_file));
    findColumns(csvreader);

    parseData(csvreader);

    verifySchedule();

    computeGeneralSchedule();

    try {
      outputDetailedSchedules(_schedule);
    } catch (final DocumentException e) {
      throw new RuntimeException("Error creating PDF document", e);
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
  private TeamScheduleInfo findNextTeam(final Date time, final String table, final int side, final int round) {
    TeamScheduleInfo retval = null;
    for (final TeamScheduleInfo si : _schedule) {
      if (table.equals(si.perfTableColor[round])
          && side == si.perfTableSide[round] && si.perf[round].after(time)) {
        if (retval == null) {
          retval = si;
        } else if (null != si.perf[round]
            && si.perf[round].before(retval.perf[round])) {
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
    final int otherSide = 2 == si.perfTableSide[round] ? 1 : 2;
    final TeamScheduleInfo next = findNextTeam(si.perf[round], si.perfTableColor[round], otherSide, round);

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

  private void outputDetailedSchedules(final List<TeamScheduleInfo> schedule) throws DocumentException, IOException {
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

    outputPresentationSchedule(schedule, detailedSchedules);
    detailedSchedules.add(Chunk.NEXTPAGE);

    outputTechnicalSchedule(schedule, detailedSchedules);
    detailedSchedules.add(Chunk.NEXTPAGE);

    outputPerformanceSchedule(schedule, detailedSchedules);

    detailedSchedules.close();
    output.close();

  }

  private void outputPerformanceSchedule(final List<TeamScheduleInfo> schedule, final Document detailedSchedules) throws DocumentException {

    for (int round = 0; round < NUMBER_OF_ROUNDS; ++round) {
      Collections.sort(schedule, getPerformanceComparator(round));

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

      for (final TeamScheduleInfo si : schedule) {
        // check if team needs to stay and color the cell magenta if they do
        final Color backgroundColor;
        if (null != checkIfTeamNeedsToStay(si, round)) {
          teamsStaying.add(si);
          backgroundColor = Color.MAGENTA;
        } else {
          backgroundColor = null;
        }

        table.addCell(createCell(String.valueOf(si.teamNumber)), row, 0);
        table.addCell(createCell(si.division), row, 1);
        table.addCell(createCell(si.organization), row, 2);
        table.addCell(createCell(si.teamName), row, 3);
        table.addCell(createCell(OUTPUT_DATE_FORMAT.get().format(si.perf[round]), backgroundColor), row, 4);
        table.addCell(createCell(si.perfTableColor[round]
            + " " + si.perfTableSide[round], backgroundColor), row, 5);

        ++row;
      }

      detailedSchedules.add(table);

      // output teams staying
      if (!teamsStaying.isEmpty()) {
        final String formatString = "Team %d will please stay at the table and compete again - score will not count.";
        final Table stayingTable = createTable(1);
        for (final TeamScheduleInfo si : teamsStaying) {
          stayingTable.addCell(createCell(new Formatter().format(formatString, si.teamNumber).toString(), Color.MAGENTA));
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

  private void outputPresentationSchedule(final List<TeamScheduleInfo> schedule, final Document detailedSchedules) throws DocumentException {
    final Table table = createTable(6);
    table.setWidths(new float[] { 2, 1, 3, 3, 2, 1 });

    Collections.sort(schedule, PRESENTATION_COMPARATOR);
    int row = 0;
    table.addCell(createHeaderCell("Team #"), row, 0);
    table.addCell(createHeaderCell("Div"), row, 1);
    table.addCell(createHeaderCell("School or Organization"), row, 2);
    table.addCell(createHeaderCell("Team Name"), row, 3);
    table.addCell(createHeaderCell("Presentation"), row, 4);
    table.addCell(createHeaderCell("Judging Station"), row, 5);
    table.endHeaders();
    ++row;

    for (final TeamScheduleInfo si : schedule) {
      table.addCell(createCell(String.valueOf(si.teamNumber)), row, 0);
      table.addCell(createCell(si.division), row, 1);
      table.addCell(createCell(si.organization), row, 2);
      table.addCell(createCell(si.teamName), row, 3);
      table.addCell(createCell(OUTPUT_DATE_FORMAT.get().format(si.presentation)), row, 4);
      table.addCell(createCell(si.judge), row, 5);

      ++row;
    }

    detailedSchedules.add(table);

  }

  private void outputTechnicalSchedule(final List<TeamScheduleInfo> schedule, final Document detailedSchedules) throws DocumentException {
    Collections.sort(schedule, TECHNICAL_COMPARATOR);

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

    for (final TeamScheduleInfo si : schedule) {
      table.addCell(createCell(String.valueOf(si.teamNumber)), row, 0);
      table.addCell(createCell(si.division), row, 1);
      table.addCell(createCell(si.organization), row, 2);
      table.addCell(createCell(si.teamName), row, 3);
      table.addCell(createCell(OUTPUT_DATE_FORMAT.get().format(si.technical)), row, 4);
      table.addCell(createCell(si.judge), row, 5);

      ++row;
    }
    detailedSchedules.add(table);

  }

  /**
   * Sort by division, then judge, then by time.
   */
  private static final Comparator<TeamScheduleInfo> PRESENTATION_COMPARATOR = new Comparator<TeamScheduleInfo>() {
    public int compare(final TeamScheduleInfo one, final TeamScheduleInfo two) {
      if (!one.division.equals(two.division)) {
        return one.division.compareTo(two.division);
      } else if (!one.judge.equals(two.judge)) {
        return one.judge.compareTo(two.judge);
      } else {
        return one.presentation.compareTo(two.presentation);
      }
    }
  };

  /**
   * Sort by division, then by time.
   */
  private static final Comparator<TeamScheduleInfo> TECHNICAL_COMPARATOR = new Comparator<TeamScheduleInfo>() {
    public int compare(final TeamScheduleInfo one, final TeamScheduleInfo two) {
      if (!one.division.equals(two.division)) {
        return one.division.compareTo(two.division);
      } else if (!one.judge.equals(two.judge)) {
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
        if (!one.perf[round].equals(two.perf[round])) {
          return one.perf[round].compareTo(two.perf[round]);
        } else if (!one.perfTableColor[round].equals(two.perfTableColor[round])) {
          return one.perfTableColor[round].compareTo(two.perfTableColor[round]);
        } else {
          final int oneSide = one.perfTableSide[round];
          final int twoSide = two.perfTableSide[round];
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

  private void computeGeneralSchedule() {
    Date minTechnical = null;
    Date maxTechnical = null;
    Date minPresentation = null;
    Date maxPresentation = null;
    final Date[] minPerf = new Date[NUMBER_OF_ROUNDS];
    final Date[] maxPerf = new Date[NUMBER_OF_ROUNDS];

    for (final TeamScheduleInfo si : _schedule) {
      if (null != si.technical) {
        if (null == minTechnical
            || si.technical.before(minTechnical)) {
          minTechnical = si.technical;
        }
        if (null == maxTechnical
            || si.technical.after(maxTechnical)) {
          maxTechnical = si.technical;
        }
      }
      if (null != si.presentation) {
        if (null == minPresentation
            || si.presentation.before(minPresentation)) {
          minPresentation = si.presentation;
        }
        if (null == maxPresentation
            || si.presentation.after(maxPresentation)) {
          maxPresentation = si.presentation;
        }
      }

      for (int i = 0; i < NUMBER_OF_ROUNDS; ++i) {
        if (null != si.perf[i]) {
          // ignore the teams that cross round boundaries
          final int opponentRound = findOpponentRound(si, i);
          if (opponentRound == i) {
            if (null == minPerf[i]
                || si.perf[i].before(minPerf[i])) {
              minPerf[i] = si.perf[i];
            }

            if (null == maxPerf[i]
                || si.perf[i].after(maxPerf[i])) {
              maxPerf[i] = si.perf[i];
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
    if (matches.containsKey(ti.perf[round])) {
      timeMatches = matches.get(ti.perf[round]);
    } else {
      timeMatches = new HashMap<String, List<TeamScheduleInfo>>();
      matches.put(ti.perf[round], timeMatches);
    }

    final List<TeamScheduleInfo> tableMatches;
    if (timeMatches.containsKey(ti.perfTableColor[round])) {
      tableMatches = timeMatches.get(ti.perfTableColor[round]);
    } else {
      tableMatches = new LinkedList<TeamScheduleInfo>();
      timeMatches.put(ti.perfTableColor[round], tableMatches);
    }

    tableMatches.add(ti);

    if (tableMatches.size() > 2) {
      LOGGER.error(new Formatter().format("Too many teams competing on table: %s at time: %s. Teams: %s", ti.perfTableColor[round],
                                          OUTPUT_DATE_FORMAT.get().format(ti.perf[round]), tableMatches));
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
    final Map<Date, Set<TeamScheduleInfo>> teamsAtTime = new HashMap<Date, Set<TeamScheduleInfo>>();
    for (final TeamScheduleInfo si : schedule) {
      for (int round = 0; round < NUMBER_OF_ROUNDS; ++round) {
        final Set<TeamScheduleInfo> teams;
        if (teamsAtTime.containsKey(si.perf[round])) {
          teams = teamsAtTime.get(si.perf[round]);
        } else {
          teams = new HashSet<TeamScheduleInfo>();
          teamsAtTime.put(si.perf[round], teams);
        }
        teams.add(si);
      }
    }

    boolean retval = true;
    for (final Map.Entry<Date, Set<TeamScheduleInfo>> entry : teamsAtTime.entrySet()) {
      if (entry.getValue().size() > numberOfTableColors * 2) {
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
    for (final TeamScheduleInfo si : schedule) {
      final Set<TeamScheduleInfo> teams;
      if (teamsAtTime.containsKey(si.presentation)) {
        teams = teamsAtTime.get(si.presentation);
      } else {
        teams = new HashSet<TeamScheduleInfo>();
        teamsAtTime.put(si.presentation, teams);
      }
      teams.add(si);
    }

    boolean retval = true;
    for (final Map.Entry<Date, Set<TeamScheduleInfo>> entry : teamsAtTime.entrySet()) {
      if (entry.getValue().size() > numJudges) {
        LOGGER.error(new Formatter().format("There are too many teams at %s in presentation", entry.getKey()));

        retval = false;
      }

      final Set<String> judges = new HashSet<String>();
      for (final TeamScheduleInfo ti : entry.getValue()) {
        if (!judges.add(ti.judge)) {
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
    for (final TeamScheduleInfo si : schedule) {
      final Set<TeamScheduleInfo> teams;
      if (teamsAtTime.containsKey(si.technical)) {
        teams = teamsAtTime.get(si.technical);
      } else {
        teams = new HashSet<TeamScheduleInfo>();
        teamsAtTime.put(si.technical, teams);
      }
      teams.add(si);
    }

    boolean retval = true;
    for (final Map.Entry<Date, Set<TeamScheduleInfo>> entry : teamsAtTime.entrySet()) {
      if (entry.getValue().size() > numJudges) {
        LOGGER.error(new Formatter().format("There are too many teams at %s in technical", entry.getKey()));

        retval = false;
      }

      final Set<String> judges = new HashSet<String>();
      for (final TeamScheduleInfo ti : entry.getValue()) {
        if (!judges.add(ti.judge)) {
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
  private int findOpponentRound(final TeamScheduleInfo ti, final int round) {
    final List<TeamScheduleInfo> tableMatches = _matches.get(ti.perf[round]).get(ti.perfTableColor[round]);
    if (tableMatches.size() > 1) {
      if (tableMatches.get(0).equals(ti)) {
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
   * @return the team number or null if no opponent
   */
  private TeamScheduleInfo findOpponent(final TeamScheduleInfo ti, final int round) {
    final List<TeamScheduleInfo> tableMatches = _matches.get(ti.perf[round]).get(ti.perfTableColor[round]);
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

  private boolean verifyPerformanceVsSubjective(final int teamNumber,
                                                final Date subjectiveTime,
                                                final String subjectiveName,
                                                final Date performanceTime,
                                                final String performanceName) {
    if (subjectiveTime.before(performanceTime)) {
      if (subjectiveTime.getTime()
          + SUBJECTIVE_DURATION + CHANGETIME > performanceTime.getTime()) {
        LOGGER.error(new Formatter().format("Team %d has doesn't have enough time between %s and performance round %s", teamNumber, subjectiveName,
                                            performanceName));
        return false;
      }
    } else {
      if (performanceTime.getTime()
          + PERFORMANCE_DURATION + CHANGETIME > subjectiveTime.getTime()) {
        LOGGER.error(new Formatter().format("Team %d has doesn't have enough time between %s and performance round %s", teamNumber, subjectiveName,
                                            performanceName));
        return false;
      }
    }
    return true;
  }

  private boolean verifyTeam(final TeamScheduleInfo ti) {
    // constraint set 1
    if (ti.presentation.before(ti.technical)) {
      if (ti.presentation.getTime()
          + SUBJECTIVE_DURATION + CHANGETIME > ti.technical.getTime()) {
        LOGGER.error(new Formatter().format("Team %d has doesn't have enough time between presentation and technical", ti.teamNumber));
        return false;
      }
    } else {
      if (ti.technical.getTime()
          + SUBJECTIVE_DURATION + CHANGETIME > ti.presentation.getTime()) {
        LOGGER.error(new Formatter().format("Team %d has doesn't have enough time between presentation and technical", ti.teamNumber));
        return false;
      }
    }

    // constraint set 3
    final long changetime;
    final int round1OpponentRound = findOpponentRound(ti, 0);
    final int round2OpponentRound = findOpponentRound(ti, 1);
    if (round1OpponentRound != 0
        || round2OpponentRound != 1) {
      changetime = SPECIAL_PERFORMANCE_CHANGETIME;
    } else {
      changetime = PERFORMANCE_CHANGETIME;
    }
    if (ti.perf[0].getTime()
        + PERFORMANCE_DURATION + changetime > ti.perf[1].getTime()) {
      LOGGER.error(new Formatter().format("Team %d doesn't have enough time (%d minutes) between performance 1 and performance 2: %s - %s", ti.teamNumber,
                                          changetime
                                              / 1000 / SECONDS_PER_MINUTE, OUTPUT_DATE_FORMAT.get().format(ti.perf[0]), OUTPUT_DATE_FORMAT.get()
                                                                                                                                          .format(ti.perf[1])));
    }

    if (ti.perf[1].getTime()
        + PERFORMANCE_DURATION + PERFORMANCE_CHANGETIME > ti.perf[2].getTime()) {
      LOGGER.error(new Formatter().format("Team %d doesn't have enough time (%d minutes) between performance 2 and performance 3: %s - %s", ti.teamNumber,
                                          changetime
                                              / 1000 / SECONDS_PER_MINUTE, OUTPUT_DATE_FORMAT.get().format(ti.perf[1]), OUTPUT_DATE_FORMAT.get()
                                                                                                                                          .format(ti.perf[2])));
    }

    // constraint set 4
    for (int round = 0; round < NUMBER_OF_ROUNDS; ++round) {
      final boolean result = verifyPerformanceVsSubjective(ti.teamNumber, ti.presentation, "presentation", ti.perf[round], String.valueOf(round + 1));
      if (!result) {
        return false;
      }
    }

    // constraint set 5
    for (int round = 0; round < NUMBER_OF_ROUNDS; ++round) {
      final boolean result = verifyPerformanceVsSubjective(ti.teamNumber, ti.technical, "technical", ti.perf[round], String.valueOf(round + 1));
      if (!result) {
        return false;
      }
    }

    // make sure that all opponents are different & sides are different
    for (int round = 0; round < NUMBER_OF_ROUNDS; ++round) {
      final TeamScheduleInfo opponent = findOpponent(ti, round);
      if (null != opponent) {
        int opponentSide = -1;
        // figure out which round matches up
        for (int oround = 0; oround < NUMBER_OF_ROUNDS; ++oround) {
          if (opponent.perf[oround].equals(ti.perf[round])) {
            opponentSide = opponent.perfTableSide[oround];
            break;
          }
        }
        if (-1 == opponentSide) {
          LOGGER.error(new Formatter().format("Unable to find time match for rounds between team %d and team %d at time %s", ti.teamNumber,
                                              opponent.teamNumber, OUTPUT_DATE_FORMAT.get().format(ti.perf[round])));
        } else {
          if (opponentSide == ti.perfTableSide[round]) {
            LOGGER.error(new Formatter().format("Team %d and team %d are both on table %s side %d at the same time for round %d", ti.teamNumber,
                                                opponent.teamNumber, ti.perfTableColor[round], ti.perfTableSide[round], (round + 1)));
          }
        }

        for (int r = round + 1; r < NUMBER_OF_ROUNDS; ++r) {
          final TeamScheduleInfo otherOpponent = findOpponent(ti, r);
          if (otherOpponent != null
              && opponent.equals(otherOpponent)) {
            LOGGER.error(new Formatter().format("Team %d competes against %d more than once", ti.teamNumber, opponent.teamNumber));
            return false;
          }
        }
      }
    }

    // check if the team needs to stay for any extra founds
    for (int round = 0; round < NUMBER_OF_ROUNDS; ++round) {
      final TeamScheduleInfo next = checkIfTeamNeedsToStay(ti, round);
      if (null != next) {
        // everything else checked out, only only need to check the end time
        // against subjective and the next round
        boolean result = verifyPerformanceVsSubjective(ti.teamNumber, ti.technical, "presentation", next.perf[round], "extra");
        if (!result) {
          return false;
        }

        result = verifyPerformanceVsSubjective(ti.teamNumber, ti.technical, "technical", next.perf[round], "extra");
        if (!result) {
          return false;
        }

        if (round + 1 < NUMBER_OF_ROUNDS) {
          if (next.perf[round].getTime()
              + PERFORMANCE_DURATION + PERFORMANCE_CHANGETIME > ti.perf[round + 1].getTime()) {
            LOGGER.error(String.format("Team %d doesn't have enough time (%d minutes) between performance %d and performance extra: %s - %s", ti.teamNumber,
                                       changetime
                                           / 1000 / SECONDS_PER_MINUTE, round, OUTPUT_DATE_FORMAT.get().format(next.perf[round]),
                                       OUTPUT_DATE_FORMAT.get().format(ti.perf[round + 1])));
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
      if (teamNumberStr.length() < 1) {
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
      if (tablePieces.length != 2) {
        throw new RuntimeException("Error parsing table information from: "
            + table);
      }
      ti.perfTableColor[0] = tablePieces[0];
      ti.perfTableSide[0] = Utilities.NUMBER_FORMAT_INSTANCE.parse(tablePieces[1]).intValue();
      if (ti.perfTableSide[0] > 2
          || ti.perfTableSide[0] < 1) {
        LOGGER.error("There are only two sides to the table, number must be 1 or 2 team: "
            + ti.teamNumber + " round 1");
      }
      ti.perf[0] = parseDate(line[_perf1Column]);

      table = line[_perf2TableColumn];
      tablePieces = table.split(" ");
      if (tablePieces.length != 2) {
        throw new RuntimeException("Error parsing table information from: "
            + table);
      }
      ti.perfTableColor[1] = tablePieces[0];
      ti.perfTableSide[1] = Utilities.NUMBER_FORMAT_INSTANCE.parse(tablePieces[1]).intValue();
      if (ti.perfTableSide[1] > 2
          || ti.perfTableSide[1] < 1) {
        LOGGER.error("There are only two sides to the table, number must be 1 or 2 team: "
            + ti.teamNumber + " round 2");
      }
      ti.perf[1] = parseDate(line[_perf2Column]);

      table = line[_perf3TableColumn];
      tablePieces = table.split(" ");
      if (tablePieces.length != 2) {
        throw new RuntimeException("Error parsing table information from: "
            + table);
      }
      ti.perfTableColor[2] = tablePieces[0];
      ti.perfTableSide[2] = Utilities.NUMBER_FORMAT_INSTANCE.parse(tablePieces[1]).intValue();
      if (ti.perfTableSide[2] > 2
          || ti.perfTableSide[2] < 1) {
        LOGGER.error("There are only two sides to the table, number must be 1 or 2 team: "
            + ti.teamNumber + " round 2");
      }
      ti.perf[2] = parseDate(line[_perf3Column]);

      return ti;
    } catch (final ParseException pe) {
      LOGGER.error("Error parsing line: "
          + Arrays.toString(line), pe);
      return null;
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
   * Holds data about the schedule for a team.
   * 
   * @author jpschewe
   * @version $Revision$
   */
  private static final class TeamScheduleInfo {
    private int teamNumber;

    private String teamName;

    private String organization;

    private String division;

    private Date presentation;

    private Date technical;

    private String judge;

    private Date[] perf = new Date[NUMBER_OF_ROUNDS];

    private String[] perfTableColor = new String[NUMBER_OF_ROUNDS];

    private int[] perfTableSide = new int[NUMBER_OF_ROUNDS];

    /**
     * Find the performance round for the matching time.
     * 
     * @param time
     * @return the round, -1 if cannot be found
     */
    public int findRoundFortime(final Date time) {
      for (int round = 0; round < NUMBER_OF_ROUNDS; ++round) {
        if (perf[round].equals(time)) {
          return round;
        }
      }
      return -1;
    }

    @Override
    public String toString() {
      return "[ScheduleInfo for "
          + teamNumber + "]";
    }

    @Override
    public int hashCode() {
      return teamNumber;
    }

    @Override
    public boolean equals(final Object o) {
      if (o == this) {
        return true;
      } else if (o instanceof TeamScheduleInfo) {
        return ((TeamScheduleInfo) o).teamNumber == this.teamNumber;
      } else {
        return false;
      }
    }
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
