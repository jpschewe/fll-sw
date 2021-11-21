/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.sql.DataSource;
import javax.xml.transform.TransformerException;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Tournament;
import fll.TournamentTeam;
import fll.db.NonNumericNominees;
import fll.db.NonNumericNominees.Nominee;
import fll.util.FLLInternalException;
import fll.util.FOPUtils;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.report.awards.AwardsReport;
import fll.xml.ChallengeDescription;
import fll.xml.NonNumericCategory;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Report of teams nominated for non-numeric awards.
 */
@WebServlet("/report/NonNumericNomineesReport")
public class NonNumericNomineesReport extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.HEAD_JUDGE), false)) {
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);

    try (Connection connection = datasource.getConnection()) {
      final Document doc = createReport(connection, description);
      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

      if (LOGGER.isTraceEnabled()) {
        try (StringWriter writer = new StringWriter()) {
          XMLUtils.writeXML(doc, writer);
          LOGGER.trace(writer.toString());
        }
      }

      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=nonNumericNomineesReport.pdf");

      FOPUtils.renderPdf(fopFactory, doc, response.getOutputStream());
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the awards report", e);
    }

  }

  private Document createReport(final Connection connection,
                                final ChallengeDescription description)
      throws SQLException {
    final Tournament tournament = Tournament.getCurrentTournament(connection);

    final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();

    final Element rootElement = FOPUtils.createRoot(document);
    document.appendChild(rootElement);

    final Element layoutMasterSet = FOPUtils.createXslFoElement(document, "layout-master-set");
    rootElement.appendChild(layoutMasterSet);

    final double leftMargin = 0.5;
    final double rightMargin = leftMargin;
    final double topMargin = 1;
    final double bottomMargin = 0.5;
    final double headerHeight = topMargin;
    final double footerHeight = 0.3;

    final String pageMasterName = "simple";
    final Element pageMaster = FOPUtils.createSimplePageMaster(document, pageMasterName, FOPUtils.PAGE_LETTER_SIZE,
                                                               new FOPUtils.Margins(topMargin, bottomMargin, leftMargin,
                                                                                    rightMargin),
                                                               headerHeight, footerHeight);
    layoutMasterSet.appendChild(pageMaster);

    final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
    rootElement.appendChild(pageSequence);

    final Element header = createHeader(document, description, tournament);
    pageSequence.appendChild(header);

    final Element footer = FOPUtils.createSimpleFooter(document);
    pageSequence.appendChild(footer);
    pageSequence.setAttribute("id", FOPUtils.PAGE_SEQUENCE_NAME);

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    for (final NonNumericCategory category : description.getNonNumericCategories()) {
      final Element element = addCategory(connection, tournament, document, category);
      documentBody.appendChild(element);
    }

    return document;
  }

  private Element addCategory(final Connection connection,
                              final Tournament tournament,
                              final Document document,
                              final NonNumericCategory category)
      throws SQLException {
    final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    container.setAttribute("keep-together.within-page", "always");

    container.appendChild(FOPUtils.createHorizontalLine(document, 2));

    final Element categoryTitleBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(categoryTitleBlock);
    categoryTitleBlock.setAttribute("font-weight", "bold");
    categoryTitleBlock.appendChild(document.createTextNode(category.getTitle()));

    final String categoryDescription = category.getDescription();
    if (null != categoryDescription
        && !categoryDescription.isBlank()) {
      final Element categoryDescriptionBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      container.appendChild(categoryDescriptionBlock);
      categoryDescriptionBlock.appendChild(document.createTextNode(categoryDescription));
    }

    final Element table = FOPUtils.createBasicTable(document);
    container.appendChild(table);

    table.appendChild(FOPUtils.createTableColumn(document, 1)); // team number
    table.appendChild(FOPUtils.createTableColumn(document, 1)); // team name
    table.appendChild(FOPUtils.createTableColumn(document, 1)); // organization
    table.appendChild(FOPUtils.createTableColumn(document, 1)); // award group

    final Map<String, Collection<TournamentTeam>> groupedTeams = new HashMap<>();
    for (final Nominee nominee : NonNumericNominees.getNominees(connection, tournament.getTournamentID(),
                                                                category.getTitle())) {
      final TournamentTeam team = TournamentTeam.getTournamentTeamFromDatabase(connection, tournament,
                                                                               nominee.getTeamNumber());
      groupedTeams.computeIfAbsent(team.getAwardGroup(), k -> new LinkedList<>()).add(team);
    }

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    table.appendChild(tableBody);

    if (!groupedTeams.isEmpty()) {
      for (final Map.Entry<String, Collection<TournamentTeam>> entry : groupedTeams.entrySet()) {
        for (final TournamentTeam team : entry.getValue()) {

          final Element row = FOPUtils.createTableRow(document);
          tableBody.appendChild(row);

          row.appendChild(FOPUtils.createTableCell(document, null, String.valueOf(team.getTeamNumber())));

          row.appendChild(FOPUtils.createTableCell(document, null, team.getTeamName()));

          final String organization = team.getOrganization();
          row.appendChild(FOPUtils.createTableCell(document, null, null == organization ? "" : organization));

          row.appendChild(FOPUtils.createTableCell(document, null, team.getAwardGroup()));
        } // foreach team in award group
      } // foreach award group
    } else {
      final int columnsInTable = FOPUtils.columnsInTable(table);
      final Element row = FOPUtils.createTableRow(document);
      tableBody.appendChild(row);

      final Element cell = FOPUtils.createTableCell(document, null, "No teams have been nominated for this category");
      row.appendChild(cell);
      cell.setAttribute("font-style", "italic");
      cell.setAttribute("number-columns-spanned", String.valueOf(columnsInTable));
    }

    return container;
  }

  private Element createHeader(final Document document,
                               final ChallengeDescription description,
                               final Tournament tournament) {
    final Element staticContent = FOPUtils.createXslFoElement(document, "static-content");
    staticContent.setAttribute("flow-name", "xsl-region-before");
    staticContent.setAttribute("font-size", "10pt");

    final Element titleBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    staticContent.appendChild(titleBlock);
    titleBlock.setAttribute("text-align", "center");
    titleBlock.setAttribute("font-size", "16pt");
    titleBlock.setAttribute("font-weight", "bold");

    final String reportTitle = createTitle(description, tournament);
    titleBlock.appendChild(document.createTextNode(reportTitle));

    staticContent.appendChild(FOPUtils.createBlankLine(document));

    final Element subtitleBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    staticContent.appendChild(subtitleBlock);
    subtitleBlock.setAttribute("text-align-last", "justify");
    subtitleBlock.setAttribute("font-weight", "bold");

    final String tournamentDescription = tournament.getDescription();
    final String tournamentName = null == tournamentDescription ? tournament.getName() : tournamentDescription;
    final String tournamentTitle = String.format("%s: %s", tournament.getLevel().getName(), tournamentName);
    subtitleBlock.appendChild(document.createTextNode(tournamentTitle));

    final Element subtitleCenter = FOPUtils.createXslFoElement(document, FOPUtils.LEADER_TAG);
    subtitleBlock.appendChild(subtitleCenter);
    subtitleCenter.setAttribute("leader-pattern", "space");

    final LocalDate tournamentDate = tournament.getDate();
    if (null != tournamentDate) {
      final String dateString = String.format("Date: %s", AwardsReport.DATE_FORMATTER.format(tournamentDate));

      subtitleBlock.appendChild(document.createTextNode(dateString));
    }

    staticContent.appendChild(FOPUtils.createBlankLine(document));

    return staticContent;
  }

  private static String createTitle(final ChallengeDescription description,
                                    final Tournament tournament) {
    final StringBuilder titleBuilder = new StringBuilder();
    titleBuilder.append(description.getTitle());

    if (null != tournament.getLevel()) {
      titleBuilder.append(" ");
      titleBuilder.append(tournament.getLevel().getName());
    }

    titleBuilder.append(" Non-numeric Nominees");
    return titleBuilder.toString();
  }

}
