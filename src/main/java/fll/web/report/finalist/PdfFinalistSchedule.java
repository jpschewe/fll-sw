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
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Map;

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
import fll.db.Queries;
import fll.scheduler.ScheduleWriter;
import fll.scheduler.TournamentSchedule;
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

  /**
   * Create a page for the specified category.
   */
  private Element createCategoryPage(final Document document,
                                     final Connection connection,
                                     final String category,
                                     final String room,
                                     final FinalistSchedule schedule)
      throws SQLException {
    final Element page = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    final Element pageHeader = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    page.appendChild(pageHeader);
    pageHeader.setAttribute("font-family", TITLE_FONT_FAMILY);
    pageHeader.setAttribute("font-size", TITLE_FONT_SIZE);
    pageHeader.setAttribute("font-weight", TITLE_FONT_WEIGHT);

    final Element headerBlock1 = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    pageHeader.appendChild(headerBlock1);
    headerBlock1.appendChild(document.createTextNode(String.format("Finalist schedule for %s", category)));

    if (null != room
        && !"".equals(room)) {
      final Element headerBlock2 = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      pageHeader.appendChild(headerBlock2);
      headerBlock2.appendChild(document.createTextNode(String.format("Room: %s", room)));
    }

    final Element schedTable = FOPUtils.createBasicTable(document);
    page.appendChild(schedTable);

    schedTable.appendChild(FOPUtils.createTableColumn(document, 125));
    schedTable.appendChild(FOPUtils.createTableColumn(document, 125));
    schedTable.appendChild(FOPUtils.createTableColumn(document, 375));
    schedTable.appendChild(FOPUtils.createTableColumn(document, 375));

    final Element schedTableHeader = FOPUtils.createTableHeader(document);
    schedTable.appendChild(schedTableHeader);
    schedTableHeader.setAttribute("font-family", HEADER_FONT_FAMILY);
    schedTableHeader.setAttribute("font-size", HEADER_FONT_SIZE);
    schedTableHeader.setAttribute("font-weight", HEADER_FONT_WEIGHT);

    final Element schedTableHeaderRow = FOPUtils.createTableRow(document);
    schedTableHeader.appendChild(schedTableHeaderRow);

    Element cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, "Time");
    schedTableHeaderRow.appendChild(cell);
    FOPUtils.addBottomBorder(cell, ScheduleWriter.STANDARD_BORDER_WIDTH);
    FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                        FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

    cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, TournamentSchedule.TEAM_NUMBER_HEADER);
    schedTableHeaderRow.appendChild(cell);
    FOPUtils.addBottomBorder(cell, ScheduleWriter.STANDARD_BORDER_WIDTH);
    FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                        FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

    cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, TournamentSchedule.TEAM_NAME_HEADER);
    schedTableHeaderRow.appendChild(cell);
    FOPUtils.addBottomBorder(cell, ScheduleWriter.STANDARD_BORDER_WIDTH);
    FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                        FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

    cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, TournamentSchedule.ORGANIZATION_HEADER);
    schedTableHeaderRow.appendChild(cell);
    FOPUtils.addBottomBorder(cell, ScheduleWriter.STANDARD_BORDER_WIDTH);
    FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                        FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

    final Element schedTableBody = FOPUtils.createXslFoElement(document, "table-body");
    schedTable.appendChild(schedTableBody);
    schedTableBody.setAttribute("font-family", SCHEDULE_FONT_FAMILY);
    schedTableBody.setAttribute("font-size", SCHEDULE_FONT_SIZE);

    // foreach information output
    for (final FinalistDBRow row : schedule.getScheduleForCategory(category)) {
      final Element tableRow = FOPUtils.createTableRow(document);
      schedTableBody.appendChild(tableRow);

      // time, number, name, organization

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                      String.format("%d:%02d", row.getHour(), row.getMinute()));
      tableRow.appendChild(cell);
      FOPUtils.addBottomBorder(cell, ScheduleWriter.STANDARD_BORDER_WIDTH);
      FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                          FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

      final Team team = Team.getTeamFromDatabase(connection, row.getTeamNumber());
      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, String.valueOf(team.getTeamNumber()));
      tableRow.appendChild(cell);
      FOPUtils.addBottomBorder(cell, ScheduleWriter.STANDARD_BORDER_WIDTH);
      FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                          FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, team.getTeamName());
      tableRow.appendChild(cell);
      FOPUtils.addBottomBorder(cell, ScheduleWriter.STANDARD_BORDER_WIDTH);
      FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                          FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

      cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, team.getOrganization());
      tableRow.appendChild(cell);
      FOPUtils.addBottomBorder(cell, ScheduleWriter.STANDARD_BORDER_WIDTH);
      FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                          FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

    }

    return page;
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

    final Element tableBody = FOPUtils.createXslFoElement(document, "table-body");
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

    final Element date = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT,
                                                  tournament.getDate()
                                                            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)));
    row2.appendChild(date);
    FOPUtils.addBottomBorder(date, ScheduleWriter.THICK_BORDER_WIDTH);

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

    final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
    rootElement.appendChild(pageSequence);

    final Element header = createHeader(document, challengeDescription.getTitle(), tournament);
    pageSequence.appendChild(header);

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    final Map<String, String> rooms = schedule.getRooms();
    for (final String categoryTitle : schedule.getCategories()) {
      final Element ele = createCategoryPage(document, connection, categoryTitle, rooms.get(categoryTitle), schedule);
      documentBody.appendChild(ele);
      ele.setAttribute("page-break-after", "always");
    }

    return document;
  }

}
