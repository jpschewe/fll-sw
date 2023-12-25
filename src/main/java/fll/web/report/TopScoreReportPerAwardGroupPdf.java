/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import javax.xml.transform.TransformerException;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Tournament;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.FOPUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.scoreboard.Top10;
import fll.xml.ChallengeDescription;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * PDF of top performance scores organized by award group.
 */
@WebServlet("/report/TopScoreReportPerAwardGroupPdf")
public class TopScoreReportPerAwardGroupPdf extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    if (PromptSummarizeScores.checkIfSummaryUpdated(request, response, application, session,
                                                    "/report/TopScoreReportPerAwardGroupPdf")) {
      return;
    }

    response.reset();
    response.setContentType("application/pdf");
    response.setHeader("Content-Disposition", "filename=TopScoreReportPerAwardGroup.pdf");

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);
      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

      final Map<String, List<Top10.ScoreEntry>> scores = Top10.getTableAsMapByAwardGroup(connection, description);
      outputReport(response.getOutputStream(), challengeDescription, tournament, "Award Group", scores);
    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    }

  }

  private void outputReport(final OutputStream stream,
                            final ChallengeDescription challengeDescription,
                            final Tournament tournament,
                            final String groupIdentifier,
                            final Map<String, List<Top10.ScoreEntry>> scores)
      throws IOException, SQLException {
    try {

      final Document document = createDocument(challengeDescription, tournament, groupIdentifier, scores);

      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

      FOPUtils.renderPdf(fopFactory, document, stream);
    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the PDF", e);
    }
  }

  /**
   * Number of columns in the report, needs to be consnistent with the number of
   * columns created in the table header
   */
  private static final int NUM_COLMNS = 5;

  private Document createDocument(final ChallengeDescription challengeDescription,
                                  final Tournament tournament,
                                  final String groupIdentifier,
                                  final Map<String, List<Top10.ScoreEntry>> scores) {
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

    final String challengeTitle = challengeDescription.getTitle();

    for (final Map.Entry<String, List<Top10.ScoreEntry>> groupEntry : scores.entrySet()) {
      final String group = groupEntry.getKey();
      final List<Top10.ScoreEntry> groupScores = groupEntry.getValue();

      final Element table = FOPUtils.createBasicTable(document);
      documentBody.appendChild(table);
      table.setAttribute("page-break-after", "always");

      table.appendChild(FOPUtils.createTableColumn(document, 1)); // rank
      table.appendChild(FOPUtils.createTableColumn(document, 1)); // team number
      table.appendChild(FOPUtils.createTableColumn(document, 2)); // team name
      table.appendChild(FOPUtils.createTableColumn(document, 2)); // organization
      table.appendChild(FOPUtils.createTableColumn(document, 1)); // score

      final Element header = createHeader(document, challengeTitle, groupIdentifier, group, tournament);
      table.appendChild(header);

      final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
      table.appendChild(tableBody);

      for (final Top10.ScoreEntry scoreEntry : groupScores) {
        final Element row = FOPUtils.createTableRow(document);
        tableBody.appendChild(row);
        FOPUtils.keepWithPrevious(row);

        row.appendChild(FOPUtils.createStandardTableCell(document, String.valueOf(scoreEntry.getRank())));

        row.appendChild(FOPUtils.createStandardTableCell(document, String.valueOf(scoreEntry.getTeamNumber())));

        final String teamName = scoreEntry.getTeamName();
        row.appendChild(FOPUtils.createStandardTableCell(document, null == teamName ? "" : teamName));

        final String organization = scoreEntry.getOrganization();
        row.appendChild(FOPUtils.createStandardTableCell(document, null == organization ? "" : organization));

        row.appendChild(FOPUtils.createStandardTableCell(document, scoreEntry.getFormattedScore()));
      }

    }

    return document;
  }

  private Element createHeader(final Document document,
                               final String challengeTitle,
                               final String groupIdentifier,
                               final String group,
                               final Tournament tournament) {
    final Element tableHeader = FOPUtils.createTableHeader(document);
    tableHeader.setAttribute("font-weight", "bold");

    final Element row1 = FOPUtils.createTableRow(document);
    tableHeader.appendChild(row1);

    final Element row1Cell = FOPUtils.createStandardTableCell(document, String.format("%s - %s", challengeTitle,
                                                                                      tournament.getDescription()));
    row1.appendChild(row1Cell);
    row1Cell.setAttribute("number-columns-spanned", String.valueOf(NUM_COLMNS));

    final Element row2 = FOPUtils.createTableRow(document);
    tableHeader.appendChild(row2);
    final Element row2Cell = FOPUtils.createStandardTableCell(document,
                                                              String.format("Top regular match play round scores for %s: %s",
                                                                            groupIdentifier, group));
    row2.appendChild(row2Cell);
    row2Cell.setAttribute("number-columns-spanned", String.valueOf(NUM_COLMNS));

    final Element row3 = FOPUtils.createTableRow(document);
    tableHeader.appendChild(row3);

    row3.appendChild(FOPUtils.createStandardTableCell(document, "Rank"));
    row3.appendChild(FOPUtils.createStandardTableCell(document, TournamentSchedule.TEAM_NUMBER_HEADER));
    row3.appendChild(FOPUtils.createStandardTableCell(document, TournamentSchedule.TEAM_NAME_HEADER));
    row3.appendChild(FOPUtils.createStandardTableCell(document, TournamentSchedule.ORGANIZATION_HEADER));
    row3.appendChild(FOPUtils.createStandardTableCell(document, "Score"));

    return tableHeader;
  }

}
