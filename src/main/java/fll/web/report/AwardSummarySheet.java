/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import javax.sql.DataSource;
import javax.xml.transform.TransformerException;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Tournament;
import fll.db.GlobalParameters;
import fll.db.Queries;
import fll.db.TournamentParameters;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.FOPUtils;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;
import fll.xml.ChallengeDescription;
import fll.xml.WinnerType;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Generate PDF for award summary sheet for the specified award group.
 */
@WebServlet("/report/AwardSummarySheet")
public class AwardSummarySheet extends BaseFLLServlet {

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

    if (PromptSummarizeScores.checkIfSummaryUpdated(response, application, session, "/report/AwardSummarySheet")) {
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    try (Connection connection = datasource.getConnection()) {

      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
      final int tournamentID = Queries.getCurrentTournament(connection);
      final Tournament tournament = Tournament.findTournamentByID(connection, tournamentID);

      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=awardSummarySheet.pdf");

      final String awardGroup = WebUtils.getNonNullRequestParameter(request, "awardGroup");

      try {
        final Document document = generateReport(connection, challengeDescription, tournament, awardGroup);
        final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

        FOPUtils.renderPdf(fopFactory, document, response.getOutputStream());
      } catch (FOPException | TransformerException e) {
        throw new FLLInternalException("Error creating the award summary sheet", e);
      }

    } catch (final SQLException e) {
      throw new FLLRuntimeException(e);
    }
  }

  private Document generateReport(final Connection connection,
                                  final ChallengeDescription challengeDescription,
                                  final Tournament tournament,
                                  final String awardGroup)
      throws SQLException, IOException {
    if (tournament.getTournamentID() != Queries.getCurrentTournament(connection)) {
      throw new FLLRuntimeException("Cannot generate report for a tournament other than the current tournament");
    }

    final WinnerType winnerCriteria = challengeDescription.getWinner();

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

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    final Element report = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    documentBody.appendChild(report);

    final Element headerBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    report.appendChild(headerBlock);
    headerBlock.setAttribute("text-align-last", "justify");

    final Element titleInline = FOPUtils.createXslFoElement(document, FOPUtils.INLINE_TAG);
    headerBlock.appendChild(titleInline);
    titleInline.appendChild(document.createTextNode("AWARD SUMMARY SHEET"));
    titleInline.setAttribute("font-weight", "bold");
    titleInline.setAttribute("font-size", "18pt");

    final Element headerSpacer = FOPUtils.createXslFoElement(document, FOPUtils.LEADER_TAG);
    titleInline.appendChild(headerSpacer);
    headerSpacer.setAttribute("leader-pattern", "space");

    final Element awardGroupSection = FOPUtils.createXslFoElement(document, FOPUtils.INLINE_TAG);
    headerBlock.appendChild(awardGroupSection);
    awardGroupSection.appendChild(document.createTextNode("Judging Group: "));
    awardGroupSection.appendChild(document.createTextNode(awardGroup));

    return document;
  }

}
