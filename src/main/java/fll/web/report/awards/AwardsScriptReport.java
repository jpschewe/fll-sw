/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.awards;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Formatter;
import java.util.Set;

import javax.sql.DataSource;
import javax.xml.transform.TransformerException;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Tournament;
import fll.TournamentLevel;
import fll.db.AwardsScript;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.FOPUtils;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.report.AwardsReport;
import fll.web.report.PromptSummarizeScores;
import fll.xml.ChallengeDescription;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * 
 */
@WebServlet("/report/awards/AwardsScriptReport")
public class AwardsScriptReport extends BaseFLLServlet {

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

    if (PromptSummarizeScores.checkIfSummaryUpdated(response, application, session, "/report/awards/AwardsScriptReport")) {
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    try (Connection connection = datasource.getConnection()) {

      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=awardsScript.pdf");

      try {
        final Document document = generateDocument(description, connection);

        if (LOGGER.isTraceEnabled()) {
          try (StringWriter writer = new StringWriter()) {
            XMLUtils.writeXML(document, writer);
            LOGGER.trace(writer.toString());
          }
        }

        final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

        FOPUtils.renderPdf(fopFactory, document, response.getOutputStream());
      } catch (FOPException | TransformerException e) {
        throw new FLLInternalException("Error creating the final scoresPDF", e);
      }

    } catch (final SQLException e) {
      throw new FLLRuntimeException(e);
    }

  }

  private void addMacrosToTemplateContext(final Connection connection,
                                          final Tournament tournament,
                                          final VelocityContext templateContext)
      throws SQLException {
    for (final AwardsScript.Macro macro : AwardsScript.Macro.values()) {
      final String value = AwardsScript.getMacroValue(connection, tournament, macro);
      templateContext.put(macro.getText(), value);
    }
  }

  private Document generateDocument(ChallengeDescription description,
                                    Connection connection)
      throws SQLException {
    final Tournament tournament = Tournament.getCurrentTournament(connection);

    final VelocityContext templateContext = new VelocityContext();
    addMacrosToTemplateContext(connection, tournament, templateContext);

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

    final Element intro = createFrontMatter(connection, tournament, document, templateContext);
    documentBody.appendChild(intro);

    return document;
  }

  private Element createFrontMatter(final Connection connection,
                                    final Tournament tournament,
                                    final Document document,
                                    final VelocityContext templateContext)
      throws SQLException {
    final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    final Element mainBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(mainBlock);

    final String frontMatterText = AwardsScript.getSectionText(connection, tournament,
                                                               AwardsScript.Section.FRONT_MATTER);

    try (StringWriter writer = new StringWriter()) {
      if (!Velocity.evaluate(templateContext, writer, AwardsScript.Section.FRONT_MATTER.name(), frontMatterText)) {
        throw new FLLRuntimeException(String.format("Error evaluating template for section %s",
                                                    AwardsScript.Section.FRONT_MATTER));
      }
      mainBlock.appendChild(document.createTextNode(writer.toString()));
    } catch (final IOException e) {
      throw new FLLInternalException("Should not get IO exception writing to a string", e);
    }

    return container;
  }

  private static String createTitle(final ChallengeDescription description,
                                    final Tournament tournament) {
    final Formatter titleBuilder = new Formatter();
    titleBuilder.format(description.getTitle());

    if (null != tournament.getLevel()) {
      titleBuilder.format(" %s", tournament.getLevel().getName());
    }

    titleBuilder.format(" Awards Script");
    return titleBuilder.toString();
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

    final TournamentLevel tournamentLevel = tournament.getLevel();
    final String tournamentName = tournament.getTitle();
    final String tournamentTitle = String.format("%s: %s", tournamentLevel.getName(), tournamentName);
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

}
