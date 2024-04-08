/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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

import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.diffplug.common.base.Errors;

import fll.Utilities;
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
    final Element staticContent = FOPUtils.createXslFoElement(document, FOPUtils.STATIC_CONTENT_TAG);
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
    table.appendChild(FOPUtils.createTableColumn(document, 2)); // wave
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
    FOPUtils.addBorders(teamNumberHeaderCell, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                        FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);

    final Element teamNameHeaderCell = FOPUtils.createTableCell(document, null, TournamentSchedule.TEAM_NAME_HEADER);
    headerRow.appendChild(teamNameHeaderCell);
    FOPUtils.addBorders(teamNameHeaderCell, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                        FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);

    final Element organizationHeaderCell = FOPUtils.createTableCell(document, null,
                                                                    TournamentSchedule.ORGANIZATION_HEADER);
    headerRow.appendChild(organizationHeaderCell);
    FOPUtils.addBorders(organizationHeaderCell, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                        FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);

    final Element judgeGroupHeaderCell = FOPUtils.createTableCell(document, null,
                                                                  TournamentSchedule.JUDGE_GROUP_HEADER);
    headerRow.appendChild(judgeGroupHeaderCell);
    FOPUtils.addBorders(judgeGroupHeaderCell, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                        FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);

    final Element awardGroupHeaderCell = FOPUtils.createTableCell(document, null,
                                                                  TournamentSchedule.AWARD_GROUP_HEADER);
    headerRow.appendChild(awardGroupHeaderCell);
    FOPUtils.addBorders(awardGroupHeaderCell, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                        FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);

    final Element waveHeaderCell = FOPUtils.createTableCell(document, null, TournamentSchedule.WAVE_HEADER);
    headerRow.appendChild(waveHeaderCell);
    FOPUtils.addBorders(waveHeaderCell, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                        FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);

    for (final String subjectiveStation : subjectiveStations) {
      final Element subjHeaderCell = FOPUtils.createTableCell(document, null, subjectiveStation);
      headerRow.appendChild(subjHeaderCell);
      FOPUtils.addBorders(subjHeaderCell, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                          FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);
    }
    for (int round = 0; round < schedule.getNumberOfPracticeRounds(); ++round) {
      final Element headerCell1 = FOPUtils.createTableCell(document, null,
                                                           String.format(TournamentSchedule.PRACTICE_HEADER_FORMAT,
                                                                         round
                                                                             + 1));
      headerRow.appendChild(headerCell1);
      FOPUtils.addBorders(headerCell1, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                          FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);

      final Element headerCell2 = FOPUtils.createTableCell(document, null,
                                                           String.format(TournamentSchedule.PRACTICE_TABLE_HEADER_FORMAT,
                                                                         round
                                                                             + 1));
      headerRow.appendChild(headerCell2);
      FOPUtils.addBorders(headerCell2, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                          FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);
    }
    for (int round = 0; round < schedule.getNumberOfRegularMatchPlayRounds(); ++round) {
      final Element headerCell1 = FOPUtils.createTableCell(document, null,
                                                           String.format(TournamentSchedule.PERF_HEADER_FORMAT, round
                                                               + 1));
      headerRow.appendChild(headerCell1);
      FOPUtils.addBorders(headerCell1, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                          FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);

      final Element headerCell2 = FOPUtils.createTableCell(document, null,
                                                           String.format(TournamentSchedule.TABLE_HEADER_FORMAT, round
                                                               + 1));
      headerRow.appendChild(headerCell2);
      FOPUtils.addBorders(headerCell2, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                          FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);
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
      FOPUtils.addBorders(teamNumberCell, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                          FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);

      final Element teamNameCell = FOPUtils.createTableCell(document, null, si.getTeamName());
      row.appendChild(teamNameCell);
      FOPUtils.addBorders(teamNameCell, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                          FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);

      final Element organizationCell = FOPUtils.createTableCell(document, null,
                                                                Utilities.stringValueOrEmpty(si.getOrganization()));
      row.appendChild(organizationCell);
      FOPUtils.addBorders(organizationCell, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                          FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);

      final Element judgeGroupCell = FOPUtils.createTableCell(document, null, si.getJudgingGroup());
      row.appendChild(judgeGroupCell);
      FOPUtils.addBorders(judgeGroupCell, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                          FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);

      final Element awardGroupCell = FOPUtils.createTableCell(document, null, si.getAwardGroup());
      row.appendChild(awardGroupCell);
      FOPUtils.addBorders(awardGroupCell, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                          FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);

      final Element waveCell = FOPUtils.createTableCell(document, null, Utilities.stringValueOrEmpty(si.getWave()));
      row.appendChild(waveCell);
      FOPUtils.addBorders(waveCell, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                          FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);

      for (final String subjectiveStation : subjectiveStations) {
        final SubjectiveTime stime = si.getSubjectiveTimeByName(subjectiveStation);
        if (null == stime) {
          throw new RuntimeException("Cannot find time for "
              + subjectiveStation);
        }
        final Element cell = FOPUtils.createTableCell(document, null, TournamentSchedule.formatTime(stime.getTime()));
        row.appendChild(cell);
        FOPUtils.addBorders(cell, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                            FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);
      }

      si.enumeratePracticePerformances().forEachOrdered(Errors.rethrow().wrap(pair -> {
        final PerformanceTime perf = pair.getLeft();

        final Element cell1 = FOPUtils.createTableCell(document, null, TournamentSchedule.formatTime(perf.getTime()));
        row.appendChild(cell1);
        FOPUtils.addBorders(cell1, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                            FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);

        final Element cell2 = FOPUtils.createTableCell(document, null,
                                                       String.format("%s %s", perf.getTable(), perf.getSide()));
        row.appendChild(cell2);
        FOPUtils.addBorders(cell2, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                            FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);
      }));

      si.enumerateRegularMatchPlayPerformances().forEachOrdered(Errors.rethrow().wrap(pair -> {
        final PerformanceTime perf = pair.getLeft();

        final Element cell1 = FOPUtils.createTableCell(document, null, TournamentSchedule.formatTime(perf.getTime()));
        row.appendChild(cell1);
        FOPUtils.addBorders(cell1, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                            FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);

        final Element cell2 = FOPUtils.createTableCell(document, null,
                                                       String.format("%s %s", perf.getTable(), perf.getSide()));
        row.appendChild(cell2);
        FOPUtils.addBorders(cell2, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                            FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);
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
   * Output the performance schedule per table, sorted by time.
   *
   * @param schedule the schedule to write
   * @param pdfFos where to write the schedule
   * @throws IOException error writing to the stream
   */
  public static void outputPerformanceSchedulePerTableByTime(final TournamentSchedule schedule,
                                                             final OutputStream pdfFos)
      throws IOException {
    try {
      final Document performanceDoc = createPerformanceSchedulePerTable(schedule, true, false);
      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

      FOPUtils.renderPdf(fopFactory, performanceDoc, pdfFos);
    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the performance schedule PDF", e);
    }
  }

  /**
   * Like {@link #outputPerformanceSchedulePerTableByTime(TournamentSchedule,
   * OutputStream)}, only drops the award group column and adds a notes column.
   *
   * @param schedule the schedule to write
   * @param pdfFos where to write the schedule
   * @throws IOException error writing to the stream
   */
  public static void outputPerformanceSchedulePerTableByTimeForNotes(final TournamentSchedule schedule,
                                                                     final OutputStream pdfFos)
      throws IOException {
    try {
      final Document performanceDoc = createPerformanceSchedulePerTable(schedule, false, true);
      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

      FOPUtils.renderPdf(fopFactory, performanceDoc, pdfFos);
    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the performance schedule PDF", e);
    }
  }

  private static Document createPerformanceSchedulePerTable(final TournamentSchedule schedule,
                                                            final boolean displayAwardGroup,
                                                            final boolean displayNotes) {
    final SortedSet<ImmutablePair<String, Integer>> tables = new TreeSet<>();

    final Map<PerformanceTime, TeamScheduleInfo> performanceTimes = new HashMap<>();
    for (final TeamScheduleInfo si : schedule.getSchedule()) {
      for (final PerformanceTime pt : si.getAllPerformances()) {
        performanceTimes.put(pt, si);
        final ImmutablePair<String, Integer> table = ImmutablePair.of(pt.getTable(), pt.getSide());
        tables.add(table);
      }
    }

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

    final String footerText = String.format("Tournament: %s", schedule.getName());
    final Element footer = FOPUtils.createSimpleFooter(document, footerText, FOPUtils.PAGE_SEQUENCE_NAME);
    pageSequence.appendChild(footer);

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    tables.forEach(pair -> {
      final String table = pair.getLeft();
      final int side = pair.getRight();

      final String headerText = String.format("Tournament: %s Performance - %s %d", schedule.getName(), table, side);

      final Map<PerformanceTime, TeamScheduleInfo> tablePerformanceTimes = performanceTimes.entrySet().stream() //
                                                                                           .filter(e -> e.getKey()
                                                                                                         .getTable()
                                                                                                         .equals(table)
                                                                                               && e.getKey()
                                                                                                   .getSide() == side) //
                                                                                           .collect(Collectors.toMap(Map.Entry::getKey,
                                                                                                                     Map.Entry::getValue));

      final Element ele = createPerformanceScheduleTable(schedule, headerText, document, tablePerformanceTimes,
                                                         displayAwardGroup, displayNotes);
      documentBody.appendChild(ele);
      ele.setAttribute("page-break-after", "always");
    });

    return document;
  }

  private static Document createPerformanceSchedule(final TournamentSchedule schedule) {
    final Map<PerformanceTime, TeamScheduleInfo> performanceTimes = new HashMap<>();
    for (final TeamScheduleInfo si : schedule.getSchedule()) {
      for (final PerformanceTime pt : si.getAllPerformances()) {
        performanceTimes.put(pt, si);
      }
    }

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

    final String headerText = String.format("Tournament: %s", schedule.getName());

    final Element footer = FOPUtils.createSimpleFooter(document, headerText, FOPUtils.PAGE_SEQUENCE_NAME);
    pageSequence.appendChild(footer);

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    final Element ele = createPerformanceScheduleTable(schedule, headerText, document, performanceTimes, true, false);
    documentBody.appendChild(ele);

    return document;
  }

  private static final class PerformanceEntryComparator
      implements Comparator<Map.Entry<PerformanceTime, TeamScheduleInfo>>, Serializable {

    static final PerformanceEntryComparator INSTANCE = new PerformanceEntryComparator();

    @Override
    public int compare(final Map.Entry<PerformanceTime, TeamScheduleInfo> o1,
                       final Map.Entry<PerformanceTime, TeamScheduleInfo> o2) {
      return o1.getKey().compareTo(o2.getKey());
    }
  }

  private static Element createPerformanceScheduleTable(final TournamentSchedule schedule,
                                                        final String headerText,
                                                        final Document document,
                                                        final Map<PerformanceTime, TeamScheduleInfo> performanceTimes,
                                                        final boolean displayAwardGroup,
                                                        final boolean displayNotes) {

    final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    final Element table = FOPUtils.createBasicTable(document);
    container.appendChild(table);

    table.appendChild(FOPUtils.createTableColumn(document, 2)); // team number
    if (displayAwardGroup) {
      table.appendChild(FOPUtils.createTableColumn(document, 2)); // award group
    }
    table.appendChild(FOPUtils.createTableColumn(document, 2)); // organization
    table.appendChild(FOPUtils.createTableColumn(document, 3)); // team name
    table.appendChild(FOPUtils.createTableColumn(document, 2)); // time
    table.appendChild(FOPUtils.createTableColumn(document, 2)); // table
    table.appendChild(FOPUtils.createTableColumn(document, 2)); // round
    if (displayNotes) {
      table.appendChild(FOPUtils.createTableColumn(document, 3)); // notes
    }

    final Element header = FOPUtils.createTableHeader(document);
    table.appendChild(header);

    final List<String> headerNames = new LinkedList<>();

    headerNames.add(TournamentSchedule.TEAM_NUMBER_HEADER);
    if (displayAwardGroup) {
      headerNames.add(TournamentSchedule.AWARD_GROUP_HEADER);
    }
    headerNames.add(TournamentSchedule.ORGANIZATION_HEADER);
    headerNames.add(TournamentSchedule.TEAM_NAME_HEADER);
    headerNames.add("Time");
    headerNames.add("Table");
    headerNames.add("Round");
    if (displayNotes) {
      headerNames.add("Notes");
    }

    final Element headerRow1 = FOPUtils.createTableRow(document);
    header.appendChild(headerRow1);

    final Element tournamentHeader = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, headerText);
    headerRow1.appendChild(tournamentHeader);
    tournamentHeader.setAttribute("number-columns-spanned", String.valueOf(headerNames.size()));
    FOPUtils.addTopBorder(tournamentHeader, FOPUtils.STANDARD_BORDER_WIDTH);
    FOPUtils.addBottomBorder(tournamentHeader, FOPUtils.STANDARD_BORDER_WIDTH);
    FOPUtils.addLeftBorder(tournamentHeader, FOPUtils.STANDARD_BORDER_WIDTH);
    FOPUtils.addRightBorder(tournamentHeader, FOPUtils.STANDARD_BORDER_WIDTH);

    final Element headerRow2 = FOPUtils.createTableRow(document);
    header.appendChild(headerRow2);

    for (final String headerName : headerNames) {
      final Element headerCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, headerName);
      headerRow2.appendChild(headerCell);
      FOPUtils.addBorders(headerCell, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                          FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);
    }

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    table.appendChild(tableBody);

    // list of teams staying around to even up the teams
    final List<TeamScheduleInfo> teamsMissingOpponents = new LinkedList<>();

    LocalTime prevTime = null;
    Element prevRow = null;
    final List<Map.Entry<PerformanceTime, TeamScheduleInfo>> entries = new LinkedList<>(performanceTimes.entrySet());
    Collections.sort(entries, PerformanceEntryComparator.INSTANCE);
    for (final Map.Entry<PerformanceTime, TeamScheduleInfo> entry : entries) {
      final Element row = FOPUtils.createTableRow(document);
      tableBody.appendChild(row);

      final PerformanceTime performance = entry.getKey();
      final TeamScheduleInfo si = entry.getValue();

      // check if team is missing an opponent
      if (null == schedule.findOpponent(si, performance)) {
        teamsMissingOpponents.add(si);
        row.setAttribute("background-color", "lightgrey");
      }

      final LocalTime performanceTime = performance.getTime();
      final double topBorderWidth;
      if (Objects.equals(performanceTime, prevTime)) {
        topBorderWidth = FOPUtils.STANDARD_BORDER_WIDTH;

        // keep the rows with the same times together
        FOPUtils.keepWithPrevious(row);
      } else {
        topBorderWidth = FOPUtils.THICK_BORDER_WIDTH;
      }

      final Element teamNumberCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                              String.valueOf(si.getTeamNumber()));
      row.appendChild(teamNumberCell);
      FOPUtils.addBorders(teamNumberCell, topBorderWidth, FOPUtils.STANDARD_BORDER_WIDTH,
                          FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);

      if (displayAwardGroup) {
        final Element awardGroupCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                String.valueOf(si.getAwardGroup()));
        row.appendChild(awardGroupCell);
        FOPUtils.addBorders(awardGroupCell, topBorderWidth, FOPUtils.STANDARD_BORDER_WIDTH,
                            FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);
      }

      final Element organizationCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                Utilities.stringValueOrEmpty(si.getOrganization()));
      row.appendChild(organizationCell);
      FOPUtils.addBorders(organizationCell, topBorderWidth, FOPUtils.STANDARD_BORDER_WIDTH,
                          FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);

      final Element teamNameCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                            String.valueOf(si.getTeamName()));
      row.appendChild(teamNameCell);
      FOPUtils.addBorders(teamNameCell, topBorderWidth, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                          FOPUtils.STANDARD_BORDER_WIDTH);

      final Element timeCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                        TournamentSchedule.formatTime(performanceTime));
      row.appendChild(timeCell);
      FOPUtils.addBorders(timeCell, topBorderWidth, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                          FOPUtils.STANDARD_BORDER_WIDTH);

      final Element perfTableCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                             performance.getTable()
                                                                 + " "
                                                                 + performance.getSide());
      row.appendChild(perfTableCell);
      FOPUtils.addBorders(perfTableCell, topBorderWidth, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                          FOPUtils.STANDARD_BORDER_WIDTH);

      final Element perfRoundCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                             si.getRoundName(performance));
      row.appendChild(perfRoundCell);
      FOPUtils.addBorders(perfRoundCell, topBorderWidth, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                          FOPUtils.STANDARD_BORDER_WIDTH);

      if (displayNotes) {
        final Element notesCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                           String.valueOf(Utilities.NON_BREAKING_SPACE));
        row.appendChild(notesCell);
        FOPUtils.addBorders(notesCell, topBorderWidth, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                            FOPUtils.STANDARD_BORDER_WIDTH);
      }

      prevTime = performanceTime;
      prevRow = row;
    }

    // output teams staying
    if (!teamsMissingOpponents.isEmpty()) {
      final String formatString = "Team %d does not have an opponent.";
      final Element stayingTable = FOPUtils.createBasicTable(document);
      container.appendChild(stayingTable);

      stayingTable.appendChild(FOPUtils.createTableColumn(document, 1));

      final Element stayingTableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
      stayingTable.appendChild(stayingTableBody);

      for (final TeamScheduleInfo si : teamsMissingOpponents) {
        final Element stayingTableRow = FOPUtils.createTableRow(document);
        stayingTableBody.appendChild(stayingTableRow);

        final Element stayingTableCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                  String.format(formatString, si.getTeamNumber()));
        stayingTableRow.appendChild(stayingTableCell);
      }

    }

    // make sure the last row isn't by itself
    if (null != prevRow) {
      FOPUtils.keepWithPreviousAlways(prevRow);
    }

    return container;
  }

  /**
   * Output the schedule for each team.
   *
   * @param schedule the tournament schedule
   * @param stream where to write the schedule
   * @throws IOException if there is an error writing to the stream
   */
  public static void outputTeamSchedules(final TournamentSchedule schedule,
                                         final OutputStream stream)
      throws IOException {

    try {
      final Document document = createTeamSchedules(schedule);

      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

      FOPUtils.renderPdf(fopFactory, document, stream);
    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the teamschedules PDF", e);
    }
  }

  private static Document createTeamSchedules(final TournamentSchedule schedule) {
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
      final Element teamSchedule = outputTeamSchedule(document, schedule, si);
      documentBody.appendChild(teamSchedule);
    }

    return document;
  }

  /**
   * Output the schedule for the specified team.
   *
   * @param schedule the tournament schedule
   * @param stream where to write the schedule
   * @param teamNumber the team to output the schedule for
   * @throws IOException if there is an error writing to the stream
   */
  public static void outputTeamSchedule(final TournamentSchedule schedule,
                                        final OutputStream stream,
                                        final int teamNumber)
      throws IOException {

    try {
      final Document document = createTeamSchedule(schedule, teamNumber);

      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

      FOPUtils.renderPdf(fopFactory, document, stream);
    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the team schedule PDF", e);
    }
  }

  private static Document createTeamSchedule(final TournamentSchedule schedule,
                                             final int teamNumber) {
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

    final @Nullable TeamScheduleInfo si = schedule.getSchedInfoForTeam(teamNumber);
    if (null != si) {
      final Element teamSchedule = outputTeamSchedule(document, schedule, si);
      documentBody.appendChild(teamSchedule);
    } else {
      final Element emptyBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      documentBody.appendChild(emptyBlock);
      emptyBlock.appendChild(document.createTextNode(String.format("Cannot find schedule for team with number %d",
                                                                   teamNumber)));
    }

    return document;
  }

  /**
   * Output the detailed schedule for a team for the day.
   */
  private static Element outputTeamSchedule(final Document document,
                                            final TournamentSchedule schedule,
                                            final TeamScheduleInfo si) {
    final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    container.setAttribute("keep-together.within-page", "always");
    container.setAttribute("font-size", "10pt");
    container.setAttribute("font-weight", "bold");

    final Element header1 = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(header1);
    header1.setAttribute("font-size", "12pt");
    header1.appendChild(document.createTextNode(String.format("Detailed schedule for Team #%d - %s", si.getTeamNumber(),
                                                              si.getTeamName())));

    final Element header2 = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(header2);
    header2.setAttribute("font-size", "12pt");
    header2.appendChild(document.createTextNode(String.format("Organization: %s",
                                                              Utilities.stringValueOrEmpty(si.getOrganization()))));

    final Element division = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(division);

    final Element divisionHeader = FOPUtils.createXslFoElement(document, "inline");
    division.appendChild(divisionHeader);
    divisionHeader.appendChild(document.createTextNode("Award Group: "));

    final Element divisionValue = FOPUtils.createXslFoElement(document, "inline");
    division.appendChild(divisionValue);
    divisionValue.appendChild(document.createTextNode(si.getAwardGroup()));

    container.appendChild(FOPUtils.createBlankLine(document));

    appendTeamSchedule(document, schedule, si, container);

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
   * Append the elements for the team schedule in time order to the specified
   * container. All styling is set at the container level.
   * 
   * @param document used to create elements
   * @param schedule tournament schedule
   * @param si schedule information for the team
   * @param container the container for all of the elements
   */
  public static void appendTeamSchedule(final Document document,
                                        final TournamentSchedule schedule,
                                        final TeamScheduleInfo si,
                                        final Element container) {
    // build all of the elements to display and then add them to the document in
    // time order
    final SortedMap<LocalTime, Element> scheduleElements = new TreeMap<>();

    for (final String subjectiveStation : schedule.getSubjectiveStations()) {
      final Element block = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);

      final SubjectiveTime stime = si.getSubjectiveTimeByName(subjectiveStation);
      if (null == stime) {
        throw new RuntimeException("Cannot find time for "
            + subjectiveStation);
      }
      final LocalTime start = stime.getTime();

      final String text = String.format("%s%s%s", TournamentSchedule.formatTime(start),
                                        String.valueOf(Utilities.NON_BREAKING_SPACE).repeat(2), subjectiveStation);
      block.appendChild(document.createTextNode(text));

      scheduleElements.put(start, block);
    }

    for (final PerformanceTime performance : si.getAllPerformances()) {
      final Element block = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);

      final String roundName = si.getRoundName(performance);

      final LocalTime start = performance.getTime();

      final String text = String.format("%s%sPerformance %s %s %d", TournamentSchedule.formatTime(start),
                                        String.valueOf(Utilities.NON_BREAKING_SPACE).repeat(2), roundName,
                                        performance.getTable(), performance.getSide());
      block.appendChild(document.createTextNode(text));

      scheduleElements.put(start, block);
    }

    // add the elements in time order
    scheduleElements.values().forEach(container::appendChild);
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

    final String footerText = String.format("Tournament: %s", schedule.getName());
    final Element footer = FOPUtils.createSimpleFooter(document, footerText, FOPUtils.PAGE_SEQUENCE_NAME);
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
    FOPUtils.addBorders(tournamentHeader, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                        FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);

    final Element headerRow2 = FOPUtils.createTableRow(document);
    header.appendChild(headerRow2);

    for (final String headerName : headerNames) {
      final Element headerCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, headerName);
      headerRow2.appendChild(headerCell);
      FOPUtils.addBorders(headerCell, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                          FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);
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
        topBorderWidth = FOPUtils.STANDARD_BORDER_WIDTH;

        // keep the rows with the same times together
        FOPUtils.keepWithPrevious(row);
      } else {
        topBorderWidth = TournamentSchedule.TIME_SEPARATOR_LINE_WIDTH;
      }

      Element cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, String.valueOf(si.getTeamNumber()));
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, si.getAwardGroup());
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                      Utilities.stringValueOrEmpty(si.getOrganization()));
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, si.getTeamName());
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, TournamentSchedule.formatTime(time));
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, si.getJudgingGroup());
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      prevTime = time;
      lastRow = row;
    }

    if (null != lastRow) {
      // make sure the last row isn't by itself
      FOPUtils.keepWithPreviousAlways(lastRow);
    }

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

    final String headerFooterText = String.format("Tournament: %s", schedule.getName());
    final Element footer = FOPUtils.createSimpleFooter(document, headerFooterText, FOPUtils.PAGE_SEQUENCE_NAME);
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

    final Element tournamentHeader = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, headerFooterText);
    headerRow1.appendChild(tournamentHeader);
    tournamentHeader.setAttribute("number-columns-spanned", String.valueOf(headerNames.length));
    FOPUtils.addBorders(tournamentHeader, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                        FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);

    final Element headerRow2 = FOPUtils.createTableRow(document);
    header.appendChild(headerRow2);

    for (final String headerName : headerNames) {
      final Element headerCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, headerName);
      headerRow2.appendChild(headerCell);
      FOPUtils.addBorders(headerCell, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                          FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);
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
        topBorderWidth = FOPUtils.STANDARD_BORDER_WIDTH;

        // keep the rows with the same times together
        FOPUtils.keepWithPrevious(row);
      } else {
        topBorderWidth = FOPUtils.THICK_BORDER_WIDTH;
      }

      Element cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                              String.valueOf(teamAtTime.getTeamInfo().getTeamNumber()));
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, teamAtTime.getTeamInfo().getAwardGroup());
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                      Utilities.stringValueOrEmpty(teamAtTime.getTeamInfo().getOrganization()));
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, teamAtTime.getTeamInfo().getTeamName());
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, teamAtTime.getSubjTime().getName());
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, teamAtTime.getTeamInfo().getJudgingGroup());
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, TournamentSchedule.formatTime(time));
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      prevTime = time;
      lastRow = row;
    }

    if (null != lastRow) {
      // make sure the last row isn't by itself
      FOPUtils.keepWithPreviousAlways(lastRow);
    }

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

    final String footerText = String.format("Tournament: %s", schedule.getName());
    final Element footer = FOPUtils.createSimpleFooter(document, footerText, FOPUtils.PAGE_SEQUENCE_NAME);
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
    FOPUtils.addBorders(tournamentHeader, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                        FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);

    final Element headerRow2 = FOPUtils.createTableRow(document);
    header.appendChild(headerRow2);

    for (final String headerName : headerNames) {
      final Element headerCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, headerName);
      headerRow2.appendChild(headerCell);
      FOPUtils.addBorders(headerCell, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                          FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);
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
        topBorderWidth = FOPUtils.STANDARD_BORDER_WIDTH;
        // keep the rows with the same award group together if possible
        FOPUtils.keepWithPrevious(row);
      } else {
        topBorderWidth = FOPUtils.THICK_BORDER_WIDTH;
      }

      Element cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, String.valueOf(si.getTeamNumber()));
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, awardGroup);
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                      Utilities.stringValueOrEmpty(si.getOrganization()));
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, si.getTeamName());
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                      null == stime ? "" : TournamentSchedule.formatTime(stime.getTime()));
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, si.getJudgingGroup());
      row.appendChild(cell);
      FOPUtils.addBottomBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addLeftBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addRightBorder(cell, FOPUtils.STANDARD_BORDER_WIDTH);
      FOPUtils.addTopBorder(cell, topBorderWidth);

      prevAwardGroup = awardGroup;
      lastRow = row;
    }

    if (null != lastRow) {
      // make sure the last row isn't by itself
      FOPUtils.keepWithPreviousAlways(lastRow);
    }

    return table;

  }

}
