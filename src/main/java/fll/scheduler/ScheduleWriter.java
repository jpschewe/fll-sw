/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.transform.TransformerException;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.diffplug.common.base.Errors;

import fll.scheduler.TournamentSchedule.SubjectiveComparatorByAwardGroup;
import fll.scheduler.TournamentSchedule.SubjectiveComparatorByTime;
import fll.util.FLLInternalException;
import fll.util.FOPUtils;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Writes various versions of a schedule.
 */
public final class ScheduleWriter {
  private ScheduleWriter() {
  }

  /**
   * Output the schedule sorted by team number. This schedule looks much like
   * the input spreadsheet.
   *
   * @param schedule where to write the schedule
   * @param stream where to write the schedule
   * @throws IOException error writing to the stream
   */
  public static void outputScheduleByTeam(final TournamentSchedule schedule,
                                          final OutputStream stream)
      throws IOException {
    try {
      final Document performanceDoc = createScheduleByTeam(schedule);
      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

      FOPUtils.renderPdf(fopFactory, performanceDoc, stream);
    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the performance schedule PDF", e);
    }
  }

  private static Element createHeader(final Document document,
                                      final String text) {
    final Element staticContent = FOPUtils.createXslFoElement(document, "static-content");
    staticContent.setAttribute("flow-name", "xsl-region-before");

    final Element block = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    staticContent.appendChild(block);

    block.setAttribute("text-align", FOPUtils.TEXT_ALIGN_CENTER);
    block.setAttribute("font-weight", "bold");
    block.appendChild(document.createTextNode(text));

    return staticContent;
  }

  private static Document createScheduleByTeam(final TournamentSchedule schedule) {
    final Set<String> subjectiveStations = schedule.getSubjectiveStations();

    final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();

    final Element rootElement = FOPUtils.createRoot(document);
    document.appendChild(rootElement);

    final Element layoutMasterSet = FOPUtils.createXslFoElement(document, "layout-master-set");
    rootElement.appendChild(layoutMasterSet);

    final String pageMasterName = "simple";
    final Element pageMaster = FOPUtils.createSimplePageMaster(document, pageMasterName, FOPUtils.PAGE_LETTER_SIZE,
                                                               FOPUtils.STANDARD_MARGINS, 0.2,
                                                               FOPUtils.STANDARD_FOOTER_HEIGHT);
    layoutMasterSet.appendChild(pageMaster);
    pageMaster.setAttribute("reference-orientation", "90");

    final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
    rootElement.appendChild(pageSequence);
    pageSequence.setAttribute("id", FOPUtils.PAGE_SEQUENCE_NAME);

    final Element header = createHeader(document, "Tournament: "
        + schedule.getName());
    pageSequence.appendChild(header);

    final Element footer = FOPUtils.createSimpleFooter(document);
    pageSequence.appendChild(footer);

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    final Element table = FOPUtils.createBasicTable(document);
    documentBody.appendChild(table);

    table.appendChild(FOPUtils.createTableColumn(document, 2)); // team number
    table.appendChild(FOPUtils.createTableColumn(document, 3)); // team name
    table.appendChild(FOPUtils.createTableColumn(document, 3)); // organization
    table.appendChild(FOPUtils.createTableColumn(document, 2)); // judging group
    table.appendChild(FOPUtils.createTableColumn(document, 2)); // award group
    for (int i = 0; i < subjectiveStations.size(); ++i) {
      table.appendChild(FOPUtils.createTableColumn(document, 2)); // time
    }
    for (int i = 0; i < schedule.getNumberOfPracticeRounds(); ++i) {
      table.appendChild(FOPUtils.createTableColumn(document, 2)); // time
      table.appendChild(FOPUtils.createTableColumn(document, 2)); // table
    }
    for (int i = 0; i < schedule.getNumberOfRegularMatchPlayRounds(); ++i) {
      table.appendChild(FOPUtils.createTableColumn(document, 2)); // time
      table.appendChild(FOPUtils.createTableColumn(document, 2)); // table
    }

    final Element tableHeader = FOPUtils.createTableHeader(document);
    table.appendChild(tableHeader);

    final Element headerRow = FOPUtils.createTableRow(document);
    tableHeader.appendChild(headerRow);

    final Element teamNumberHeaderCell = FOPUtils.createTableCell(document, null,
                                                                  TournamentSchedule.TEAM_NUMBER_HEADER);
    headerRow.appendChild(teamNumberHeaderCell);
    FOPUtils.addBorders(teamNumberHeaderCell, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                        STANDARD_BORDER_WIDTH);

    final Element teamNameHeaderCell = FOPUtils.createTableCell(document, null, TournamentSchedule.TEAM_NAME_HEADER);
    headerRow.appendChild(teamNameHeaderCell);
    FOPUtils.addBorders(teamNameHeaderCell, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                        STANDARD_BORDER_WIDTH);

    final Element organizationHeaderCell = FOPUtils.createTableCell(document, null,
                                                                    TournamentSchedule.ORGANIZATION_HEADER);
    headerRow.appendChild(organizationHeaderCell);
    FOPUtils.addBorders(organizationHeaderCell, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                        STANDARD_BORDER_WIDTH);

    final Element judgeGroupHeaderCell = FOPUtils.createTableCell(document, null,
                                                                  TournamentSchedule.JUDGE_GROUP_HEADER);
    headerRow.appendChild(judgeGroupHeaderCell);
    FOPUtils.addBorders(judgeGroupHeaderCell, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                        STANDARD_BORDER_WIDTH);

    final Element awardGroupHeaderCell = FOPUtils.createTableCell(document, null,
                                                                  TournamentSchedule.AWARD_GROUP_HEADER);
    headerRow.appendChild(awardGroupHeaderCell);
    FOPUtils.addBorders(awardGroupHeaderCell, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                        STANDARD_BORDER_WIDTH);

    for (final String subjectiveStation : subjectiveStations) {
      final Element subjHeaderCell = FOPUtils.createTableCell(document, null, subjectiveStation);
      headerRow.appendChild(subjHeaderCell);
      FOPUtils.addBorders(subjHeaderCell, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);
    }
    for (int round = 0; round < schedule.getNumberOfPracticeRounds(); ++round) {
      final Element headerCell1 = FOPUtils.createTableCell(document, null,
                                                           String.format(TournamentSchedule.PRACTICE_HEADER_FORMAT,
                                                                         round
                                                                             + 1));
      headerRow.appendChild(headerCell1);
      FOPUtils.addBorders(headerCell1, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);

      final Element headerCell2 = FOPUtils.createTableCell(document, null,
                                                           String.format(TournamentSchedule.PRACTICE_TABLE_HEADER_FORMAT,
                                                                         round
                                                                             + 1));
      headerRow.appendChild(headerCell2);
      FOPUtils.addBorders(headerCell2, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);
    }
    for (int round = 0; round < schedule.getNumberOfRegularMatchPlayRounds(); ++round) {
      final Element headerCell1 = FOPUtils.createTableCell(document, null,
                                                           String.format(TournamentSchedule.PERF_HEADER_FORMAT, round
                                                               + 1));
      headerRow.appendChild(headerCell1);
      FOPUtils.addBorders(headerCell1, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);

      final Element headerCell2 = FOPUtils.createTableCell(document, null,
                                                           String.format(TournamentSchedule.TABLE_HEADER_FORMAT, round
                                                               + 1));
      headerRow.appendChild(headerCell2);
      FOPUtils.addBorders(headerCell2, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);
    }

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    table.appendChild(tableBody);

    final List<TeamScheduleInfo> scheduleEntries = new ArrayList<>(schedule.getSchedule());
    Collections.sort(scheduleEntries, TournamentSchedule.ComparatorByTeam.INSTANCE);
    for (final TeamScheduleInfo si : scheduleEntries) {
      final Element row = FOPUtils.createTableRow(document);
      tableBody.appendChild(row);

      final Element teamNumberCell = FOPUtils.createTableCell(document, null, String.valueOf(si.getTeamNumber()));
      row.appendChild(teamNumberCell);
      FOPUtils.addBorders(teamNumberCell, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);

      final Element teamNameCell = FOPUtils.createTableCell(document, null, si.getTeamName());
      row.appendChild(teamNameCell);
      FOPUtils.addBorders(teamNameCell, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);

      final Element organizationCell = FOPUtils.createTableCell(document, null, si.getOrganization());
      row.appendChild(organizationCell);
      FOPUtils.addBorders(organizationCell, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);

      final Element judgeGroupCell = FOPUtils.createTableCell(document, null, si.getJudgingGroup());
      row.appendChild(judgeGroupCell);
      FOPUtils.addBorders(judgeGroupCell, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);

      final Element awardGroupCell = FOPUtils.createTableCell(document, null, si.getAwardGroup());
      row.appendChild(awardGroupCell);
      FOPUtils.addBorders(awardGroupCell, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);

      for (final String subjectiveStation : subjectiveStations) {
        final SubjectiveTime stime = si.getSubjectiveTimeByName(subjectiveStation);
        if (null == stime) {
          throw new RuntimeException("Cannot find time for "
              + subjectiveStation);
        }
        final Element cell = FOPUtils.createTableCell(document, null, TournamentSchedule.formatTime(stime.getTime()));
        row.appendChild(cell);
        FOPUtils.addBorders(cell, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                            STANDARD_BORDER_WIDTH);
      }

      si.enumeratePracticePerformances().forEachOrdered(Errors.rethrow().wrap(pair -> {
        final PerformanceTime perf = pair.getLeft();

        final Element cell1 = FOPUtils.createTableCell(document, null, TournamentSchedule.formatTime(perf.getTime()));
        row.appendChild(cell1);
        FOPUtils.addBorders(cell1, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                            STANDARD_BORDER_WIDTH);

        final Element cell2 = FOPUtils.createTableCell(document, null,
                                                       String.format("%s %s", perf.getTable(), perf.getSide()));
        row.appendChild(cell2);
        FOPUtils.addBorders(cell2, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                            STANDARD_BORDER_WIDTH);
      }));

      si.enumerateRegularMatchPlayPerformances().forEachOrdered(Errors.rethrow().wrap(pair -> {
        final PerformanceTime perf = pair.getLeft();

        final Element cell1 = FOPUtils.createTableCell(document, null, TournamentSchedule.formatTime(perf.getTime()));
        row.appendChild(cell1);
        FOPUtils.addBorders(cell1, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                            STANDARD_BORDER_WIDTH);

        final Element cell2 = FOPUtils.createTableCell(document, null,
                                                       String.format("%s %s", perf.getTable(), perf.getSide()));
        row.appendChild(cell2);
        FOPUtils.addBorders(cell2, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                            STANDARD_BORDER_WIDTH);
      }));

    }

    return document;
  }

  /**
   * Output the performance schedule, sorted by time.
   *
   * @param schedule the schedule to write
   * @param pdfFos where to write the schedule
   * @throws IOException error writing to the stream
   */
  public static void outputPerformanceScheduleByTime(final TournamentSchedule schedule,
                                                     final OutputStream pdfFos)
      throws IOException {
    try {
      final Document performanceDoc = createPerformanceSchedule(schedule);
      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

      FOPUtils.renderPdf(fopFactory, performanceDoc, pdfFos);
    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the performance schedule PDF", e);
    }
  }

  /**
   * Standard border for table cells in points.
   */
  public static final double STANDARD_BORDER_WIDTH = 0.5;

  /**
   * Think border for table cells in points.
   */
  public static final double THICK_BORDER_WIDTH = 2;

  private static Document createPerformanceSchedule(final TournamentSchedule schedule) {
    final SortedMap<PerformanceTime, TeamScheduleInfo> performanceTimes = new TreeMap<>();
    for (final TeamScheduleInfo si : schedule.getSchedule()) {
      for (final PerformanceTime pt : si.getAllPerformances()) {
        performanceTimes.put(pt, si);
      }
    }

    // list of teams staying around to even up the teams
    final List<TeamScheduleInfo> teamsMissingOpponents = new LinkedList<>();

    final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();

    final Element rootElement = FOPUtils.createRoot(document);
    document.appendChild(rootElement);

    final Element layoutMasterSet = FOPUtils.createXslFoElement(document, "layout-master-set");
    rootElement.appendChild(layoutMasterSet);

    final String pageMasterName = "simple";
    final Element pageMaster = FOPUtils.createSimplePageMaster(document, pageMasterName);
    layoutMasterSet.appendChild(pageMaster);

    final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
    rootElement.appendChild(pageSequence);
    pageSequence.setAttribute("id", FOPUtils.PAGE_SEQUENCE_NAME);

    final Element footer = FOPUtils.createSimpleFooter(document);
    pageSequence.appendChild(footer);

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    final Element table = FOPUtils.createBasicTable(document);
    documentBody.appendChild(table);

    table.appendChild(FOPUtils.createTableColumn(document, 2));
    table.appendChild(FOPUtils.createTableColumn(document, 2));
    table.appendChild(FOPUtils.createTableColumn(document, 3));
    table.appendChild(FOPUtils.createTableColumn(document, 3));
    table.appendChild(FOPUtils.createTableColumn(document, 2));
    table.appendChild(FOPUtils.createTableColumn(document, 2));
    table.appendChild(FOPUtils.createTableColumn(document, 2));

    final Element header = FOPUtils.createTableHeader(document);
    table.appendChild(header);

    final String[] headerNames = new String[] { TournamentSchedule.TEAM_NUMBER_HEADER,
                                                TournamentSchedule.AWARD_GROUP_HEADER,
                                                TournamentSchedule.ORGANIZATION_HEADER,
                                                TournamentSchedule.TEAM_NAME_HEADER, "Time", "Table", "Round" };

    final Element headerRow1 = FOPUtils.createTableRow(document);
    header.appendChild(headerRow1);

    final Element tournamentHeader = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                              String.format("Tournament: %s Performance",
                                                                            schedule.getName()));
    headerRow1.appendChild(tournamentHeader);
    tournamentHeader.setAttribute("number-columns-spanned", String.valueOf(headerNames.length));
    FOPUtils.addTopBorder(tournamentHeader, STANDARD_BORDER_WIDTH);
    FOPUtils.addBottomBorder(tournamentHeader, STANDARD_BORDER_WIDTH);
    FOPUtils.addLeftBorder(tournamentHeader, STANDARD_BORDER_WIDTH);
    FOPUtils.addRightBorder(tournamentHeader, STANDARD_BORDER_WIDTH);

    final Element headerRow2 = FOPUtils.createTableRow(document);
    header.appendChild(headerRow2);

    for (final String headerName : headerNames) {
      final Element headerCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, headerName);
      headerRow2.appendChild(headerCell);
      FOPUtils.addBorders(headerCell, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);
    }

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    table.appendChild(tableBody);

    LocalTime prevTime = null;
    Element prevRow = null;
    for (final Map.Entry<PerformanceTime, TeamScheduleInfo> entry : performanceTimes.entrySet()) {
      final Element row = FOPUtils.createTableRow(document);
      tableBody.appendChild(row);

      final PerformanceTime performance = entry.getKey();
      final TeamScheduleInfo si = entry.getValue();

      // check if team is missing an opponent
      if (null == schedule.findOpponent(si, performance)) {
        teamsMissingOpponents.add(si);
        row.setAttribute("background-color", "magenta");
      }

      final LocalTime performanceTime = performance.getTime();
      final double topBorderWidth;
      if (Objects.equals(performanceTime, prevTime)) {
        topBorderWidth = STANDARD_BORDER_WIDTH;

        // keep the rows with the same times together
        FOPUtils.keepWithPrevious(row);
      } else {
        topBorderWidth = THICK_BORDER_WIDTH;
      }

      final Element teamNumberCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                              String.valueOf(si.getTeamNumber()));
      row.appendChild(teamNumberCell);
      FOPUtils.addBorders(teamNumberCell, topBorderWidth, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);

      final Element awardGroupCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                              String.valueOf(si.getAwardGroup()));
      row.appendChild(awardGroupCell);
      FOPUtils.addBorders(awardGroupCell, topBorderWidth, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);

      final Element organizationCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                String.valueOf(si.getOrganization()));
      row.appendChild(organizationCell);
      FOPUtils.addBorders(organizationCell, topBorderWidth, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);

      final Element teamNameCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                            String.valueOf(si.getTeamName()));
      row.appendChild(teamNameCell);
      FOPUtils.addBorders(teamNameCell, topBorderWidth, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);

      final Element timeCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                        TournamentSchedule.formatTime(performanceTime));
      row.appendChild(timeCell);
      FOPUtils.addBorders(timeCell, topBorderWidth, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);

      final Element perfTableCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                             performance.getTable()
                                                                 + " "
                                                                 + performance.getSide());
      row.appendChild(perfTableCell);
      FOPUtils.addBorders(perfTableCell, topBorderWidth, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);

      final Element perfRoundCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                             si.getRoundName(performance));
      row.appendChild(perfRoundCell);
      FOPUtils.addBorders(perfRoundCell, topBorderWidth, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);

      prevTime = performanceTime;
      prevRow = row;
    }

    // output teams staying
    if (!teamsMissingOpponents.isEmpty()) {
      final String formatString = "Team %d does not have an opponent.";
      final Element stayingTable = FOPUtils.createBasicTable(document);
      documentBody.appendChild(stayingTable);

      stayingTable.appendChild(FOPUtils.createTableColumn(document, 1));

      final Element stayingTableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
      stayingTable.appendChild(stayingTableBody);

      for (final TeamScheduleInfo si : teamsMissingOpponents) {
        final Element stayingTableRow = FOPUtils.createTableRow(document);
        stayingTableBody.appendChild(stayingTableRow);

        stayingTableRow.setAttribute("background-color", "magenta");

        final Element stayingTableCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                  String.format(formatString, si.getTeamNumber()));
        stayingTableRow.appendChild(stayingTableCell);
      }

    }

    // make sure the last row isn't by itself
    if (null != prevRow) {
      FOPUtils.keepWithPreviousAlways(prevRow);
    }

    return document;
  }

  /**
   * Output the schedule for each team.
   *
   * @param schedule the tournament schedule
   * @param params schedule parameters
   * @param stream where to write the schedule
   * @throws IOException if there is an error writing to the stream
   */
  public static void outputTeamSchedules(final TournamentSchedule schedule,
                                         final SchedParams params,
                                         final OutputStream stream)
      throws IOException {

    try {
      final Document document = createTeamSchedules(schedule, params);

      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

      FOPUtils.renderPdf(fopFactory, document, stream);
    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the performance schedule PDF", e);
    }
  }

  private static Document createTeamSchedules(final TournamentSchedule schedule,
                                              final SchedParams params) {
    final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();

    final Element rootElement = FOPUtils.createRoot(document);
    document.appendChild(rootElement);

    final Element layoutMasterSet = FOPUtils.createXslFoElement(document, "layout-master-set");
    rootElement.appendChild(layoutMasterSet);

    final String pageMasterName = "simple";
    final Element pageMaster = FOPUtils.createSimplePageMaster(document, pageMasterName);
    layoutMasterSet.appendChild(pageMaster);

    final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
    rootElement.appendChild(pageSequence);
    pageSequence.setAttribute("id", FOPUtils.PAGE_SEQUENCE_NAME);

    final Element footer = FOPUtils.createSimpleFooter(document);
    pageSequence.appendChild(footer);

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    final List<TeamScheduleInfo> scheduleEntries = new ArrayList<>(schedule.getSchedule());
    Collections.sort(scheduleEntries, TournamentSchedule.ComparatorByTeam.INSTANCE);
    for (final TeamScheduleInfo si : scheduleEntries) {
      final Element teamSchedule = outputTeamSchedule(document, schedule, params, si);
      documentBody.appendChild(teamSchedule);
    }

    return document;
  }

  /**
   * Output the detailed schedule for a team for the day.
   */
  private static Element outputTeamSchedule(final Document document,
                                            final TournamentSchedule schedule,
                                            final SchedParams params,
                                            final TeamScheduleInfo si) {
    final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    container.setAttribute("keep-together.within-page", "always");
    container.setAttribute("font-size", "10pt");

    final Element header1 = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(header1);
    header1.setAttribute("font-size", "12pt");
    header1.setAttribute("font-weight", "bold");
    header1.appendChild(document.createTextNode(String.format("Detailed schedule for Team #%d - %s", si.getTeamNumber(),
                                                              si.getTeamName())));

    final Element header2 = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(header2);
    header2.setAttribute("font-size", "12pt");
    header2.setAttribute("font-weight", "bold");
    header2.appendChild(document.createTextNode(String.format("Organization: %s", si.getOrganization())));

    final Element division = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(division);

    final Element divisionHeader = FOPUtils.createXslFoElement(document, "inline");
    division.appendChild(divisionHeader);
    divisionHeader.setAttribute("font-weight", "bold");
    divisionHeader.appendChild(document.createTextNode("Award Group: "));

    final Element divisionValue = FOPUtils.createXslFoElement(document, "inline");
    division.appendChild(divisionValue);
    divisionValue.appendChild(document.createTextNode(si.getAwardGroup()));

    container.appendChild(FOPUtils.createBlankLine(document));

    // build all of the elements to display and then add them to the document in
    // time order
    final SortedMap<LocalTime, Element> scheduleElements = new TreeMap<>();

    for (final String subjectiveStation : schedule.getSubjectiveStations()) {
      final Element block = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);

      final Element header = FOPUtils.createXslFoElement(document, "inline");
      block.appendChild(header);
      header.setAttribute("font-weight", "bold");
      header.appendChild(document.createTextNode(subjectiveStation
          + ": "));

      final Element value = FOPUtils.createXslFoElement(document, "inline");
      block.appendChild(value);
      final SubjectiveTime stime = si.getSubjectiveTimeByName(subjectiveStation);
      if (null == stime) {
        throw new RuntimeException("Cannot find time for "
            + subjectiveStation);
      }
      final LocalTime start = stime.getTime();
      final LocalTime end = start.plus(params.getStationByName(subjectiveStation).getDuration());
      value.appendChild(document.createTextNode(String.format("%s - %s", TournamentSchedule.formatTime(start),
                                                              TournamentSchedule.formatTime(end))));

      scheduleElements.put(start, block);
    }

    for (final PerformanceTime performance : si.getAllPerformances()) {
      final Element block = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);

      final String roundName = si.getRoundName(performance);
      final Element header = FOPUtils.createXslFoElement(document, "inline");
      block.appendChild(header);
      header.setAttribute("font-weight", "bold");
      header.appendChild(document.createTextNode(String.format("Performance %s: ", roundName)));

      final Element value = FOPUtils.createXslFoElement(document, "inline");
      block.appendChild(value);
      final LocalTime start = performance.getTime();
      final LocalTime end = start.plus(Duration.ofMinutes(params.getPerformanceMinutes()));
      value.appendChild(document.createTextNode(String.format("%s - %s %s %d", TournamentSchedule.formatTime(start),
                                                              TournamentSchedule.formatTime(end),
                                                              performance.getTable(), performance.getSide())));

      scheduleElements.put(start, block);
    }

    // add the elements in time order
    scheduleElements.values().forEach(container::appendChild);

    container.appendChild(FOPUtils.createBlankLine(document));

    final Element instructions1 = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(instructions1);
    instructions1.setAttribute("font-weight", "bold");
    instructions1.appendChild(document.createTextNode("Performance rounds must start on time, and will start without you. Please ensure your team arrives at least 5 minutes ahead of scheduled time, and checks in."));

    final Element instructions2 = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(instructions2);
    instructions2.setAttribute("font-weight", "bold");
    instructions2.appendChild(document.createTextNode("Note that there may be more judging and a head to head round after this judging, please see the main tournament schedule for these details."));

    container.appendChild(FOPUtils.createBlankLine(document));
    container.appendChild(FOPUtils.createBlankLine(document));

    return container;
  }

  /**
   * Output the subjective schedules with a table for each category and sorted
   * by time.
   *
   * @param schedule the schedule to write
   * @param pdfFos where to write the schedule
   * @throws IOException if there is an error writing to the stream
   */
  public static void outputSubjectiveSchedulesByCategory(final TournamentSchedule schedule,
                                                         final OutputStream pdfFos)
      throws IOException {
    try {
      final Document performanceDoc = createSubjectiveSchedulesByCategory(schedule);
      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

      FOPUtils.renderPdf(fopFactory, performanceDoc, pdfFos);
    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the performance schedule PDF", e);
    }
  }

  private static Document createSubjectiveSchedulesByCategory(final TournamentSchedule schedule) {
    final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();

    final Element rootElement = FOPUtils.createRoot(document);
    document.appendChild(rootElement);

    final Element layoutMasterSet = FOPUtils.createXslFoElement(document, "layout-master-set");
    rootElement.appendChild(layoutMasterSet);

    final String pageMasterName = "simple";
    final Element pageMaster = FOPUtils.createSimplePageMaster(document, pageMasterName);
    layoutMasterSet.appendChild(pageMaster);
    // pageMaster.setAttribute("reference-orientation", "90");

    final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
    rootElement.appendChild(pageSequence);
    pageSequence.setAttribute("id", FOPUtils.PAGE_SEQUENCE_NAME);

    final Element footer = FOPUtils.createSimpleFooter(document);
    pageSequence.appendChild(footer);

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    for (final String subjectiveStation : schedule.getSubjectiveStations()) {
      final Element table = outputSubjectiveScheduleByCategory(document, schedule, subjectiveStation);
      documentBody.appendChild(table);
      table.setAttribute("page-break-after", "always");
    }

    return document;
  }

  private static Element outputSubjectiveScheduleByCategory(final Document document,
                                                            final TournamentSchedule schedule,
                                                            final String subjectiveStation) {
    final Element table = FOPUtils.createBasicTable(document);

    table.appendChild(FOPUtils.createTableColumn(document, 2));
    table.appendChild(FOPUtils.createTableColumn(document, 1));
    table.appendChild(FOPUtils.createTableColumn(document, 3));
    table.appendChild(FOPUtils.createTableColumn(document, 3));
    table.appendChild(FOPUtils.createTableColumn(document, 2));
    table.appendChild(FOPUtils.createTableColumn(document, 2));

    final Element header = FOPUtils.createTableHeader(document);
    table.appendChild(header);

    final String[] headerNames = new String[] { TournamentSchedule.TEAM_NUMBER_HEADER,
                                                TournamentSchedule.AWARD_GROUP_HEADER,
                                                TournamentSchedule.ORGANIZATION_HEADER,
                                                TournamentSchedule.TEAM_NAME_HEADER, subjectiveStation,
                                                TournamentSchedule.JUDGE_GROUP_HEADER };

    final Element headerRow1 = FOPUtils.createTableRow(document);
    header.appendChild(headerRow1);

    final Element tournamentHeader = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                              String.format("Tournament: %s - %s", schedule.getName(),
                                                                            subjectiveStation));
    headerRow1.appendChild(tournamentHeader);
    tournamentHeader.setAttribute("number-columns-spanned", String.valueOf(headerNames.length));
    FOPUtils.addBorders(tournamentHeader, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                        STANDARD_BORDER_WIDTH);

    final Element headerRow2 = FOPUtils.createTableRow(document);
    header.appendChild(headerRow2);

    for (final String headerName : headerNames) {
      final Element headerCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, headerName);
      headerRow2.appendChild(headerCell);
      FOPUtils.addBorders(headerCell, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);
    }

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    table.appendChild(tableBody);

    final List<TeamScheduleInfo> scheduleEntries = new ArrayList<>(schedule.getSchedule());
    Collections.sort(scheduleEntries, new SubjectiveComparatorByTime(subjectiveStation));
    LocalTime prevTime = null;
    Element lastRow = null;
    for (final TeamScheduleInfo si : scheduleEntries) {
      final Element row = FOPUtils.createTableRow(document);
      tableBody.appendChild(row);

      final SubjectiveTime stime = si.getSubjectiveTimeByName(subjectiveStation);
      if (null == stime) {
        throw new RuntimeException("Cannot find time for "
            + subjectiveStation);
      }
      final LocalTime time = stime.getTime();

      final double topBorderWidth;
      if (Objects.equals(time, prevTime)) {
        topBorderWidth = STANDARD_BORDER_WIDTH;

        // keep the rows with the same times together
        FOPUtils.keepWithPrevious(row);
      } else {
        topBorderWidth = TournamentSchedule.TIME_SEPARATOR_LINE_WIDTH;
      }

      Element cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, String.valueOf(si.getTeamNumber()));
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, si.getAwardGroup());
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, si.getOrganization());
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, si.getTeamName());
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, TournamentSchedule.formatTime(time));
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, si.getJudgingGroup());
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      prevTime = time;
      lastRow = row;
    }

    // make sure the last row isn't by itself
    FOPUtils.keepWithPreviousAlways(lastRow);

    return table;
  }

  /**
   * Output the subjective schedule sorted by time, then station, then award
   * group, then judging group, then team
   * number.
   * 
   * @param schedule the schedule to write
   * @param stream where to write the document
   * @throws IOException if there is an error writing the document
   */
  public static void outputSubjectiveSchedulesByTimeOnly(final TournamentSchedule schedule,
                                                         final OutputStream stream)
      throws IOException {
    try {
      final Document performanceDoc = createSubjectiveSchedulesByTimeOnly(schedule);
      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

      FOPUtils.renderPdf(fopFactory, performanceDoc, stream);
    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the subjective schedule by time PDF", e);
    }
  }

  private static Document createSubjectiveSchedulesByTimeOnly(final TournamentSchedule schedule) {
    final String[] headerNames = new String[] { TournamentSchedule.TEAM_NUMBER_HEADER,
                                                TournamentSchedule.AWARD_GROUP_HEADER,
                                                TournamentSchedule.ORGANIZATION_HEADER,
                                                TournamentSchedule.TEAM_NAME_HEADER, "Station",
                                                TournamentSchedule.JUDGE_GROUP_HEADER, "Time" };

    final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();

    final Element rootElement = FOPUtils.createRoot(document);
    document.appendChild(rootElement);

    final Element layoutMasterSet = FOPUtils.createXslFoElement(document, "layout-master-set");
    rootElement.appendChild(layoutMasterSet);

    final String pageMasterName = "simple";
    final Element pageMaster = FOPUtils.createSimplePageMaster(document, pageMasterName);
    layoutMasterSet.appendChild(pageMaster);

    final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
    rootElement.appendChild(pageSequence);
    pageSequence.setAttribute("id", FOPUtils.PAGE_SEQUENCE_NAME);

    final Element footer = FOPUtils.createSimpleFooter(document);
    pageSequence.appendChild(footer);

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    final Element table = FOPUtils.createBasicTable(document);
    documentBody.appendChild(table);

    table.appendChild(FOPUtils.createTableColumn(document, 2));
    table.appendChild(FOPUtils.createTableColumn(document, 2));
    table.appendChild(FOPUtils.createTableColumn(document, 3));
    table.appendChild(FOPUtils.createTableColumn(document, 3));
    table.appendChild(FOPUtils.createTableColumn(document, 2));
    table.appendChild(FOPUtils.createTableColumn(document, 2));
    table.appendChild(FOPUtils.createTableColumn(document, 2));

    final Element header = FOPUtils.createTableHeader(document);
    table.appendChild(header);

    final Element headerRow1 = FOPUtils.createTableRow(document);
    header.appendChild(headerRow1);

    final Element tournamentHeader = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                              String.format("Tournament: %s", schedule.getName()));
    headerRow1.appendChild(tournamentHeader);
    tournamentHeader.setAttribute("number-columns-spanned", String.valueOf(headerNames.length));
    FOPUtils.addBorders(tournamentHeader, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                        STANDARD_BORDER_WIDTH);

    final Element headerRow2 = FOPUtils.createTableRow(document);
    header.appendChild(headerRow2);

    for (final String headerName : headerNames) {
      final Element headerCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, headerName);
      headerRow2.appendChild(headerCell);
      FOPUtils.addBorders(headerCell, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);
    }

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    table.appendChild(tableBody);

    final Stream<TeamScheduleInfo> s1 = schedule.getSchedule().stream();
    final Stream<List<TeamAtSubjectiveTime>> s2 = s1.map(ti -> ti.getSubjectiveTimes().stream()
                                                                 .map(st -> new TeamAtSubjectiveTime(ti, st))
                                                                 .collect(Collectors.toList()));
    final Stream<TeamAtSubjectiveTime> s3 = s2.flatMap(Collection::stream);
    final List<TeamAtSubjectiveTime> times = s3.collect(Collectors.toList());
    Collections.sort(times);

    LocalTime prevTime = null;
    Element lastRow = null;
    for (final TeamAtSubjectiveTime teamAtTime : times) {
      final LocalTime time = teamAtTime.getSubjTime().getTime();

      final Element row = FOPUtils.createTableRow(document);
      tableBody.appendChild(row);

      final double topBorderWidth;
      if (Objects.equals(time, prevTime)) {
        topBorderWidth = STANDARD_BORDER_WIDTH;

        // keep the rows with the same times together
        FOPUtils.keepWithPrevious(row);
      } else {
        topBorderWidth = THICK_BORDER_WIDTH;
      }

      Element cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                              String.valueOf(teamAtTime.getTeamInfo().getTeamNumber()));
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, teamAtTime.getTeamInfo().getAwardGroup());
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, teamAtTime.getTeamInfo().getOrganization());
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, teamAtTime.getTeamInfo().getTeamName());
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, teamAtTime.getSubjTime().getName());
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, teamAtTime.getTeamInfo().getJudgingGroup());
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, TournamentSchedule.formatTime(time));
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      prevTime = time;
      lastRow = row;
    }

    // make sure the last row isn't by itself
    FOPUtils.keepWithPreviousAlways(lastRow);

    return document;
  }

  /**
   * Output the subjective schedules with a table for each category and sorted
   * by judging station, then by time.
   *
   * @param schedule the schedule to output
   * @param pdfFos where to output the schedule
   * @throws IOException if there is a problem writing to the stream
   */
  public static void outputSubjectiveSchedulesByJudgingStation(final TournamentSchedule schedule,
                                                               final OutputStream pdfFos)
      throws IOException {
    try {
      final Document document = createSubjectiveSchedulesByJudgingStation(schedule);
      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

      FOPUtils.renderPdf(fopFactory, document, pdfFos);
    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the subjective schedule by judging station PDF", e);
    }
  }

  private static Document createSubjectiveSchedulesByJudgingStation(final TournamentSchedule schedule) {
    final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();

    final Element rootElement = FOPUtils.createRoot(document);
    document.appendChild(rootElement);

    final Element layoutMasterSet = FOPUtils.createXslFoElement(document, "layout-master-set");
    rootElement.appendChild(layoutMasterSet);

    final String pageMasterName = "simple";
    final Element pageMaster = FOPUtils.createSimplePageMaster(document, pageMasterName);
    layoutMasterSet.appendChild(pageMaster);

    final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
    rootElement.appendChild(pageSequence);
    pageSequence.setAttribute("id", FOPUtils.PAGE_SEQUENCE_NAME);

    final Element footer = FOPUtils.createSimpleFooter(document);
    pageSequence.appendChild(footer);

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    for (final String subjectiveStation : schedule.getSubjectiveStations()) {
      final Element ele = createSubjectiveScheduleByJudgingStation(schedule, document, subjectiveStation);
      documentBody.appendChild(ele);
      ele.setAttribute("page-break-after", "always");
    }

    return document;
  }

  private static Element createSubjectiveScheduleByJudgingStation(final TournamentSchedule schedule,
                                                                  final Document document,
                                                                  final String subjectiveStation) {
    final String[] headerNames = new String[] { TournamentSchedule.TEAM_NUMBER_HEADER,
                                                TournamentSchedule.AWARD_GROUP_HEADER,
                                                TournamentSchedule.ORGANIZATION_HEADER,
                                                TournamentSchedule.TEAM_NAME_HEADER, subjectiveStation,
                                                TournamentSchedule.JUDGE_GROUP_HEADER };

    final Element table = FOPUtils.createBasicTable(document);

    table.appendChild(FOPUtils.createTableColumn(document, 2));
    table.appendChild(FOPUtils.createTableColumn(document, 1));
    table.appendChild(FOPUtils.createTableColumn(document, 3));
    table.appendChild(FOPUtils.createTableColumn(document, 3));
    table.appendChild(FOPUtils.createTableColumn(document, 2));
    table.appendChild(FOPUtils.createTableColumn(document, 2));

    final Element header = FOPUtils.createTableHeader(document);
    table.appendChild(header);

    final Element headerRow1 = FOPUtils.createTableRow(document);
    header.appendChild(headerRow1);

    final Element tournamentHeader = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                              String.format("Tournament: %s - %s", schedule.getName(),
                                                                            subjectiveStation));
    headerRow1.appendChild(tournamentHeader);
    tournamentHeader.setAttribute("number-columns-spanned", String.valueOf(headerNames.length));
    FOPUtils.addBorders(tournamentHeader, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                        STANDARD_BORDER_WIDTH);

    final Element headerRow2 = FOPUtils.createTableRow(document);
    header.appendChild(headerRow2);

    for (final String headerName : headerNames) {
      final Element headerCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, headerName);
      headerRow2.appendChild(headerCell);
      FOPUtils.addBorders(headerCell, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);
    }

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    table.appendChild(tableBody);

    final List<TeamScheduleInfo> scheduleEntries = new ArrayList<>(schedule.getSchedule());
    Collections.sort(scheduleEntries, new SubjectiveComparatorByAwardGroup(subjectiveStation));
    String prevAwardGroup = null;
    Element lastRow = null;
    for (final TeamScheduleInfo si : scheduleEntries) {
      final Element row = FOPUtils.createTableRow(document);
      tableBody.appendChild(row);

      final SubjectiveTime stime = si.getSubjectiveTimeByName(subjectiveStation);
      final String awardGroup = si.getAwardGroup();
      final double topBorderWidth;
      if (Objects.equals(awardGroup, prevAwardGroup)) {
        topBorderWidth = STANDARD_BORDER_WIDTH;
        // keep the rows with the same award group together if possible
        FOPUtils.keepWithPrevious(row);
      } else {
        topBorderWidth = THICK_BORDER_WIDTH;
      }

      Element cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, String.valueOf(si.getTeamNumber()));
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, awardGroup);
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, si.getOrganization());
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, si.getTeamName());
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                      null == stime ? "" : TournamentSchedule.formatTime(stime.getTime()));
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, si.getJudgingGroup());
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      prevAwardGroup = awardGroup;
      lastRow = row;
    }

    // make sure the last row isn't by itself
    FOPUtils.keepWithPreviousAlways(lastRow);

    return table;

  }

}
