/*
 * Copyright (c) 2000-2008 HighTechKids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.report.finalist;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
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

import fll.Tournament;
import fll.TournamentTeam;
import fll.db.Queries;
import fll.scheduler.ScheduleWriter;
import fll.util.FLLInternalException;
import fll.util.FOPUtils;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.xml.ChallengeDescription;
import net.mtu.eggplant.util.sql.SQLFunctions;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Generate a PDF with one page per team any finalist schedule.
 * 
 * @author jpschewe
 */
@WebServlet("/report/finalist/TeamFinalistSchedule")
public class TeamFinalistSchedule extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager.getLogger();

  private static final String TITLE_FONT_WEIGHT = "bold";

  private static final String TITLE_FONT_SIZE = "12pt";

  private static final String TITLE_FONT_FAMILY = "Times";

  private static final String VALUE_FONT_SIZE = TITLE_FONT_SIZE;

  private static final String VALUE_FONT_FAMILY = "Helvetica";

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.ADMIN), false)) {
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

    Connection connection = null;
    try {
      connection = datasource.getConnection();

      // setting some response headers
      response.setHeader("Expires", "0");
      response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
      response.setHeader("Pragma", "public");
      // setting the content type
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=teamFinalistSchedule.pdf");

      // write to a ByteArrayOutputStream so that we can compute size
      final ServletOutputStream stream = response.getOutputStream();

      // add content
      final int currentTournament = Queries.getCurrentTournament(connection);

      final Collection<String> divisions = FinalistSchedule.getAllDivisions(connection, currentTournament);
      final Collection<FinalistSchedule> schedules = new LinkedList<FinalistSchedule>();
      for (final String division : divisions) {
        final FinalistSchedule schedule = new FinalistSchedule(connection, currentTournament, division);
        schedules.add(schedule);
      }

      outputSchedule(stream, connection, challengeDescription, schedules);

      stream.flush();

    } catch (final SQLException e) {
      LOG.error(e, e);
      throw new RuntimeException(e);
    } catch (final IOException e) {
      LOG.error(e, e);
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(connection);
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

    final Element title = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT, "Finalist Callback Schedule");
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
                              final Collection<FinalistSchedule> schedules)
      throws IOException, SQLException {
    try {

      final Document document = createDocument(connection, challengeDescription, schedules);

      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

      FOPUtils.renderPdf(fopFactory, document, stream);
    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the performance schedule PDF", e);
    }
  }

  private Document createDocument(final Connection connection,
                                  final ChallengeDescription challengeDescription,
                                  final Collection<FinalistSchedule> schedules)
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

    final Map<Integer, TournamentTeam> tournamentTeams = Queries.getTournamentTeams(connection,
                                                                                    tournament.getTournamentID());
    final List<Integer> teamNumbers = new LinkedList<Integer>(tournamentTeams.keySet());
    Collections.sort(teamNumbers);

    for (final int teamNum : teamNumbers) {
      final TournamentTeam team = tournamentTeams.get(teamNum);

      for (final FinalistSchedule schedule : schedules) {
        final List<FinalistDBRow> finalistTimes = schedule.getScheduleForTeam(teamNum);
        final Map<String, String> rooms = schedule.getRooms();

        if (!finalistTimes.isEmpty()) {

          final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
          documentBody.appendChild(container);
          container.setAttribute("font-weight", TITLE_FONT_WEIGHT);
          container.setAttribute("font-size", TITLE_FONT_SIZE);
          container.setAttribute("font-family", TITLE_FONT_FAMILY);
          container.setAttribute("padding-after", "5px");

          final Element block1 = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
          container.appendChild(block1);
          block1.appendChild(document.createTextNode(String.format("Finalist times for Team %d",
                                                                   team.getTeamNumber())));

          final Element block2 = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
          container.appendChild(block2);
          block2.appendChild(document.createTextNode(String.format("%s / %s", team.getTeamName(),
                                                                   team.getOrganization())));

          final Element block3 = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
          container.appendChild(block3);
          block3.appendChild(document.createTextNode(String.format("Award Group: %s", team.getAwardGroup())));

          final Element table = FOPUtils.createBasicTable(document);
          documentBody.appendChild(table);
          table.setAttribute("font-family", VALUE_FONT_FAMILY);
          table.setAttribute("font-size", VALUE_FONT_SIZE);

          table.appendChild(FOPUtils.createTableColumn(document, 1));
          table.appendChild(FOPUtils.createTableColumn(document, 1));
          table.appendChild(FOPUtils.createTableColumn(document, 1));

          final Element tableHeader = FOPUtils.createTableHeader(document);
          table.appendChild(tableHeader);
          tableHeader.setAttribute("font-weight", TITLE_FONT_WEIGHT);
          tableHeader.setAttribute("font-size", TITLE_FONT_SIZE);
          tableHeader.setAttribute("font-family", TITLE_FONT_FAMILY);
          final Element tableHeaderRow = FOPUtils.createTableRow(document);
          tableHeader.appendChild(tableHeaderRow);

          Element cell = FOPUtils.createTableCell(document, null, "Time");
          tableHeaderRow.appendChild(cell);
          FOPUtils.addBorders(cell, ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH,
                              ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH);
          FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                              FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

          cell = FOPUtils.createTableCell(document, null, "Room");
          tableHeaderRow.appendChild(cell);
          FOPUtils.addBorders(cell, ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH,
                              ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH);
          FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                              FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

          cell = FOPUtils.createTableCell(document, null, "Category");
          tableHeaderRow.appendChild(cell);
          FOPUtils.addBorders(cell, ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH,
                              ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH);
          FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                              FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

          final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
          table.appendChild(tableBody);

          for (final FinalistDBRow row : finalistTimes) {
            final Element tableRow = FOPUtils.createTableRow(document);
            tableBody.appendChild(tableRow);

            final String categoryName = row.getCategoryName();
            String room = rooms.get(categoryName);
            if (null == room) {
              room = "";
            }

            cell = FOPUtils.createTableCell(document, null, String.format("%d:%02d", row.getHour(), row.getMinute()));
            tableRow.appendChild(cell);
            FOPUtils.addBorders(cell, ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH,
                                ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH);
            FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                                FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

            cell = FOPUtils.createTableCell(document, null, room);
            tableRow.appendChild(cell);
            FOPUtils.addBorders(cell, ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH,
                                ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH);
            FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                                FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

            cell = FOPUtils.createTableCell(document, null, categoryName);
            tableRow.appendChild(cell);
            FOPUtils.addBorders(cell, ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH,
                                ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH);
            FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                                FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

          } // foreach row
          table.setAttribute("page-break-after", "always");

        } // non-empty list of teams

      } // foreach schedule

    } // foreach team

    return document;
  }

}
