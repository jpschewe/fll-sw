/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.transform.TransformerException;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
   * Output the performance schedule, sorted by time.
   *
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
  private static final double STANDARD_BORDER_WIDTH = 0.5;

  /**
   * Think border for table cells in points.
   */
  private static final double THICK_BORDER_WIDTH = 2;

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
    FOPUtils.createSimplePageMaster(document, layoutMasterSet, pageMasterName);

    final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
    rootElement.appendChild(pageSequence);

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

    final Element headerRow1 = FOPUtils.createXslFoElement(document, "table-row");
    header.appendChild(headerRow1);

    final Element tournamentHeader = FOPUtils.createTableCell(document, Optional.of(FOPUtils.TEXT_ALIGN_CENTER),
                                                              String.format("Tournament: %s Performance",
                                                                            schedule.getName()));
    headerRow1.appendChild(tournamentHeader);
    tournamentHeader.setAttribute("number-columns-spanned", String.valueOf(headerNames.length));
    FOPUtils.addTopBorder(tournamentHeader, STANDARD_BORDER_WIDTH);
    FOPUtils.addBottomBorder(tournamentHeader, STANDARD_BORDER_WIDTH);
    FOPUtils.addLeftBorder(tournamentHeader, STANDARD_BORDER_WIDTH);
    FOPUtils.addRightBorder(tournamentHeader, STANDARD_BORDER_WIDTH);

    final Element headerRow2 = FOPUtils.createXslFoElement(document, "table-row");
    header.appendChild(headerRow2);

    for (final String headerName : headerNames) {
      final Element headerCell = FOPUtils.createTableCell(document, Optional.of(FOPUtils.TEXT_ALIGN_CENTER),
                                                          headerName);
      headerRow2.appendChild(headerCell);
      FOPUtils.addBorders(headerCell, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);
    }

    final Element tableBody = FOPUtils.createXslFoElement(document, "table-body");
    table.appendChild(tableBody);

    LocalTime prevTime = null;
    Element prevRow = null;
    for (final Map.Entry<PerformanceTime, TeamScheduleInfo> entry : performanceTimes.entrySet()) {
      final Element row = FOPUtils.createXslFoElement(document, "table-row");
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
        row.setAttribute("keep-with-previous", "always");
      } else {
        topBorderWidth = THICK_BORDER_WIDTH;
      }

      final Element teamNumberCell = FOPUtils.createTableCell(document, Optional.of(FOPUtils.TEXT_ALIGN_CENTER),
                                                              String.valueOf(si.getTeamNumber()));
      row.appendChild(teamNumberCell);
      FOPUtils.addBorders(teamNumberCell, topBorderWidth, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);

      final Element awardGroupCell = FOPUtils.createTableCell(document, Optional.of(FOPUtils.TEXT_ALIGN_CENTER),
                                                              String.valueOf(si.getAwardGroup()));
      row.appendChild(awardGroupCell);
      FOPUtils.addBorders(awardGroupCell, topBorderWidth, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);

      final Element organizationCell = FOPUtils.createTableCell(document, Optional.of(FOPUtils.TEXT_ALIGN_CENTER),
                                                                String.valueOf(si.getOrganization()));
      row.appendChild(organizationCell);
      FOPUtils.addBorders(organizationCell, topBorderWidth, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);

      final Element teamNameCell = FOPUtils.createTableCell(document, Optional.of(FOPUtils.TEXT_ALIGN_CENTER),
                                                            String.valueOf(si.getTeamName()));
      row.appendChild(teamNameCell);
      FOPUtils.addBorders(teamNameCell, topBorderWidth, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);

      final Element timeCell = FOPUtils.createTableCell(document, Optional.of(FOPUtils.TEXT_ALIGN_CENTER),
                                                        TournamentSchedule.formatTime(performanceTime));
      row.appendChild(timeCell);
      FOPUtils.addBorders(timeCell, topBorderWidth, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);

      final Element perfTableCell = FOPUtils.createTableCell(document, Optional.of(FOPUtils.TEXT_ALIGN_CENTER),
                                                             performance.getTable()
                                                                 + " "
                                                                 + performance.getSide());
      row.appendChild(perfTableCell);
      FOPUtils.addBorders(perfTableCell, topBorderWidth, STANDARD_BORDER_WIDTH, STANDARD_BORDER_WIDTH,
                          STANDARD_BORDER_WIDTH);

      final Element perfRoundCell = FOPUtils.createTableCell(document, Optional.of(FOPUtils.TEXT_ALIGN_CENTER),
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

      final Element stayingTableBody = FOPUtils.createXslFoElement(document, "table-body");
      stayingTable.appendChild(stayingTableBody);

      for (final TeamScheduleInfo si : teamsMissingOpponents) {
        final Element stayingTableRow = FOPUtils.createXslFoElement(document, "table-row");
        stayingTableBody.appendChild(stayingTableRow);

        stayingTableRow.setAttribute("background-color", "magenta");

        final Element stayingTableCell = FOPUtils.createTableCell(document, Optional.of(FOPUtils.TEXT_ALIGN_CENTER),
                                                                  String.format(formatString, si.getTeamNumber()));
        stayingTableRow.appendChild(stayingTableCell);
      }

    }

    // make sure the last row isn't by itself
    if (null != prevRow) {
      prevRow.setAttribute("keep-with-previous", "always");
    }

    return document;
  }

}
