/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.finalist;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import javax.xml.transform.TransformerException;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Team;
import fll.Tournament;
import fll.Utilities;
import fll.db.Queries;
import fll.scheduler.ScheduleWriter;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.FOPUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.xml.ChallengeDescription;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Outputs the PDF showing times of finalist categories.
 */
@WebServlet("/report/finalist/PdfFinalistSchedule")
public class PdfFinalistSchedule extends BaseFLLServlet {

  private static final String TITLE_FONT_FAMILY = "Times";

  private static final String TITLE_FONT_SIZE = "12pt";

  private static final String TITLE_FONT_WEIGHT = "bold";

  private static final String HEADER_FONT_FAMILY = "Helvetica";

  private static final String HEADER_FONT_SIZE = TITLE_FONT_SIZE;

  private static final String HEADER_FONT_WEIGHT = TITLE_FONT_WEIGHT;

  private static final String SCHEDULE_FONT_FAMILY = HEADER_FONT_FAMILY;

  private static final String SCHEDULE_FONT_SIZE = "10pt";

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    final String division = request.getParameter("division");
    if (null == division
        || "".equals(division)) {
      throw new FLLRuntimeException("Parameter 'division' cannot be null");
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

      // setting some response headers
      response.setHeader("Expires", "0");
      response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
      response.setHeader("Pragma", "public");
      // setting the content type
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=finalist-schedule.pdf");

      final OutputStream stream = response.getOutputStream();

      final FinalistSchedule schedule = new FinalistSchedule(connection, Queries.getCurrentTournament(connection),
                                                             division);

      outputSchedule(stream, connection, challengeDescription, schedule);

      stream.flush();

    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new RuntimeException(e);
    }

  }

  private Element createHeader(final Document document,
                               final String challengeName,
                               final Tournament tournament) {
    final Element staticContent = FOPUtils.createXslFoElement(document, "static-content");
    staticContent.setAttribute("flow-name", "xsl-region-before");
    staticContent.setAttribute("font-weight", TITLE_FONT_WEIGHT);
    staticContent.setAttribute("font-size", TITLE_FONT_SIZE);
    staticContent.setAttribute("font-family", TITLE_FONT_FAMILY);

    final Element table = FOPUtils.createBasicTable(document);
    staticContent.appendChild(table);

    table.appendChild(FOPUtils.createTableColumn(document, 2));
    table.appendChild(FOPUtils.createTableColumn(document, 1));

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    table.appendChild(tableBody);

    final Element row1 = FOPUtils.createTableRow(document);
    tableBody.appendChild(row1);

    row1.appendChild(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT, challengeName));
    row1.appendChild(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT,
                                              String.format("Tournament: %s", tournament.getName())));

    final Element row2 = FOPUtils.createTableRow(document);
    tableBody.appendChild(row2);

    final Element title = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT, "Finalist Schedule");
    row2.appendChild(title);
    FOPUtils.addBottomBorder(title, ScheduleWriter.THICK_BORDER_WIDTH);

    final LocalDate tournamentDate = tournament.getDate();
    if (null != tournamentDate) {
      final Element date = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT,
                                                    tournamentDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)));
      row2.appendChild(date);
      FOPUtils.addBottomBorder(date, ScheduleWriter.THICK_BORDER_WIDTH);
    }

    return staticContent;

  }

  private void outputSchedule(final OutputStream stream,
                              final Connection connection,
                              final ChallengeDescription challengeDescription,
                              final FinalistSchedule schedule)
      throws IOException, SQLException {
    try {

      final Document document = createDocument(connection, challengeDescription, schedule);

      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

      FOPUtils.renderPdf(fopFactory, document, stream);
    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the performance schedule PDF", e);
    }
  }

  private Document createDocument(final Connection connection,
                                  final ChallengeDescription challengeDescription,
                                  final FinalistSchedule schedule)
      throws SQLException {
    final Tournament tournament = Tournament.getCurrentTournament(connection);

    final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();

    final Element rootElement = FOPUtils.createRoot(document);
    document.appendChild(rootElement);

    final Element layoutMasterSet = FOPUtils.createXslFoElement(document, "layout-master-set");
    rootElement.appendChild(layoutMasterSet);

    final String pageMasterName = "simple";
    final Element pageMaster = FOPUtils.createSimplePageMaster(document, pageMasterName, FOPUtils.PAGE_LETTER_SIZE,
                                                               FOPUtils.STANDARD_MARGINS, 0.75, 0);
    layoutMasterSet.appendChild(pageMaster);
    pageMaster.setAttribute("reference-orientation", "90");

    final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
    rootElement.appendChild(pageSequence);

    final Element header = createHeader(document, challengeDescription.getTitle(), tournament);
    pageSequence.appendChild(header);

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    final Element mainTable = createMainTable(connection, document, schedule);
    documentBody.appendChild(mainTable);

    return document;
  }

  /**
   * Simple hour and minute time format.
   */
  public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm");

  private Element createMainTable(final Connection connection,
                                  final Document document,
                                  final FinalistSchedule schedule) {

    // convert to list to ensure we have a stable iteration order
    final List<String> categories = new LinkedList<>(schedule.getCategoryNames());
    final Map<String, String> categoryToRoom = schedule.getRooms();

    final Element mainTable = FOPUtils.createBasicTable(document);

    mainTable.appendChild(FOPUtils.createTableColumn(document, 1));
    categories.stream().forEach((ignored) -> {
      mainTable.appendChild(FOPUtils.createTableColumn(document, 2));
    });

    final Element mainTableHeader = FOPUtils.createTableHeader(document);
    mainTable.appendChild(mainTableHeader);
    mainTableHeader.setAttribute("font-family", HEADER_FONT_FAMILY);
    mainTableHeader.setAttribute("font-size", HEADER_FONT_SIZE);
    mainTableHeader.setAttribute("font-weight", HEADER_FONT_WEIGHT);

    final Element mainTableHeaderRow = FOPUtils.createTableRow(document);
    mainTableHeader.appendChild(mainTableHeaderRow);

    final Element timeCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, "Time");
    mainTableHeaderRow.appendChild(timeCell);
    FOPUtils.addBorders(timeCell, ScheduleWriter.STANDARD_BORDER_WIDTH);
    FOPUtils.addPadding(timeCell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                        FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

    categories.stream().forEach((categoryTitle) -> {
      final Element cell = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);
      mainTableHeaderRow.appendChild(cell);
      FOPUtils.addBorders(cell, ScheduleWriter.STANDARD_BORDER_WIDTH);
      FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                          FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

      final Element catEle = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      cell.appendChild(catEle);
      catEle.appendChild(document.createTextNode(categoryTitle));

      if (categoryToRoom.containsKey(categoryTitle)) {
        final Element roomEle = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
        cell.appendChild(roomEle);

        final String room = categoryToRoom.get(categoryTitle);
        roomEle.appendChild(document.createTextNode(String.format("Room: %s", room)));
      }

    });

    final Element mainTableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    mainTable.appendChild(mainTableBody);
    mainTableBody.setAttribute("font-family", SCHEDULE_FONT_FAMILY);
    mainTableBody.setAttribute("font-size", SCHEDULE_FONT_SIZE);

    final SortedMap<LocalTime, Map<String, FinalistDBRow>> organized = organizeSchedule(schedule);
    organized.forEach((timeSlot,
                       timeData) -> {
      final Element tableRow = FOPUtils.createTableRow(document);
      mainTableBody.appendChild(tableRow);

      final Element tCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                     TIME_FORMAT.format(timeSlot));
      tableRow.appendChild(tCell);
      FOPUtils.addBorders(tCell, ScheduleWriter.STANDARD_BORDER_WIDTH);
      FOPUtils.addPadding(tCell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                          FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

      categories.stream().forEach((categoryTitle) -> {
        final Element e = createTeamCell(connection, document, categoryTitle, timeData);
        tableRow.appendChild(e);
      });

    });

    return mainTable;
  }

  private Element createTeamCell(Connection connection,
                                 Document document,
                                 String categoryTitle,
                                 Map<String, FinalistDBRow> timeData) {
    final Element cell = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);
    cell.setAttribute(FOPUtils.TEXT_ALIGN_ATTRIBUTE, FOPUtils.TEXT_ALIGN_CENTER);

    if (timeData.containsKey(categoryTitle)) {
      final FinalistDBRow scheduleRow = timeData.get(categoryTitle);
      try {
        final Team team = Team.getTeamFromDatabase(connection, scheduleRow.getTeamNumber());

        final Element numEle = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
        cell.appendChild(numEle);
        numEle.appendChild(document.createTextNode(String.valueOf(team.getTeamNumber())));

        final Element nameEle = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
        cell.appendChild(nameEle);
        nameEle.appendChild(document.createTextNode(team.getTeamName()));

        final Element orgEle = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
        cell.appendChild(orgEle);
        orgEle.appendChild(document.createTextNode(team.getOrganization()));

      } catch (final SQLException e) {
        throw new FLLRuntimeException("Error getting information for team "
            + scheduleRow.getTeamNumber(), e);
      }
    } else {
      final Element e = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      cell.appendChild(e);
      e.appendChild(document.createTextNode(String.valueOf(Utilities.NON_BREAKING_SPACE)));
    }

    FOPUtils.addBorders(cell, ScheduleWriter.STANDARD_BORDER_WIDTH);
    FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                        FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

    return cell;
  }

  /**
   * Organize the data so that all time slots are together with a map of
   * categories to data for the category at the time. The map is sorted by time.
   */
  private SortedMap<LocalTime, Map<String, FinalistDBRow>> organizeSchedule(final FinalistSchedule schedule) {
    final SortedMap<LocalTime, Map<String, FinalistDBRow>> organized = new TreeMap<>();

    schedule.getSchedule().forEach(row -> {
      final LocalTime rowTimeSlow = row.getTime();
      final Map<String, FinalistDBRow> timeData = organized.computeIfAbsent(rowTimeSlow, k -> new HashMap<>());
      timeData.put(row.getCategoryName(), row);
    });

    return organized;
  }

}
