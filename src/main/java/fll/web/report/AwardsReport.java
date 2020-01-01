/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

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

import fll.Tournament;
import fll.util.FLLInternalException;
import fll.util.FOPUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.xml.ChallengeDescription;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Awards report.
 */
@WebServlet("/report/AwardsReport")
public class AwardsReport extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    if (PromptSummarizeScores.checkIfSummaryUpdated(response, application, session, "/report/FinalComputedScores")) {
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
      response.setHeader("Content-Disposition", "filename=awardsReport.pdf");

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

    final String pageMasterName = "simple";
    FOPUtils.createSimplePageMaster(document, layoutMasterSet, pageMasterName, 8.5, 11, 0.5, 0.5, 1, 0.5, 1, 0.3);

    final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
    rootElement.appendChild(pageSequence);

    final Element header = createHeader(document, description, tournament);
    pageSequence.appendChild(header);

    final Element footer = FOPUtils.createSimpleFooter(document);
    pageSequence.appendChild(footer);

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    final Element test = FOPUtils.createXslFoElement(document, "block");
    documentBody.appendChild(test);
    test.appendChild(document.createTextNode("hello world"));

    return document;
  }

  private Element createHeader(final Document document,
                               final ChallengeDescription description,
                               final Tournament tournament) {
    final Element staticContent = FOPUtils.createXslFoElement(document, "static-content");
    staticContent.setAttribute("flow-name", "xsl-region-before");
    staticContent.setAttribute("font-size", "10pt");

    final Element titleBlock = FOPUtils.createXslFoElement(document, "block");
    staticContent.appendChild(titleBlock);
    titleBlock.setAttribute("text-align", "center");
    titleBlock.setAttribute("font-size", "16pt");
    titleBlock.setAttribute("font-weight", "bold");

    final String reportTitle = createTitle(description, tournament);
    titleBlock.appendChild(document.createTextNode(reportTitle));

    staticContent.appendChild(FOPUtils.createBlankLine(document));

    final Element subtitleBlock = FOPUtils.createXslFoElement(document, "block");
    staticContent.appendChild(subtitleBlock);
    subtitleBlock.setAttribute("text-align-last", "justify");
    subtitleBlock.setAttribute("font-weight", "bold");
    
    final String tournamentName = null == tournament.getDescription() ? tournament.getName()
        : tournament.getDescription();
    final String tournamentTitle;
    if (tournament.getLevel().isPresent()) {
      tournamentTitle = String.format("%s: %s", tournament.getLevel().get(), tournamentName);
    } else {
      tournamentTitle = tournamentName;
    }
    subtitleBlock.appendChild(document.createTextNode(tournamentTitle));

    final Element subtitleCenter = FOPUtils.createXslFoElement(document, "leader");
    subtitleBlock.appendChild(subtitleCenter);
    subtitleCenter.setAttribute("leader-pattern", "space");

    if (null != tournament.getDate()) {
      final String dateString = String.format("Date: %s", DATE_FORMATTER.format(tournament.getDate()));

      subtitleBlock.appendChild(document.createTextNode(dateString));
    }

    staticContent.appendChild(FOPUtils.createBlankLine(document));

    final Element lineBlock = FOPUtils.createXslFoElement(document, "block");
    staticContent.appendChild(lineBlock);
    final Element line = FOPUtils.createXslFoElement(document, "leader");
    line.setAttribute("leader-pattern", "rule");
    line.setAttribute("leader-length", "100%");
    line.setAttribute("rule-style", "solid");
    line.setAttribute("rule-thickness", "2pt");
    lineBlock.appendChild(line);

    return staticContent;
  }

  private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder().appendValue(ChronoField.MONTH_OF_YEAR,
                                                                                                     2)
                                                                                        .appendLiteral('/')
                                                                                        .appendValue(ChronoField.DAY_OF_MONTH,
                                                                                                     2)
                                                                                        .appendLiteral('/')
                                                                                        .appendValue(ChronoField.YEAR,
                                                                                                     4)
                                                                                        .toFormatter();

  private static String createTitle(final ChallengeDescription description,
                                    final Tournament tournament) {
    final StringBuilder titleBuilder = new StringBuilder();
    titleBuilder.append(description.getTitle());

    if (tournament.getLevel().isPresent()) {
      titleBuilder.append(" ");
      titleBuilder.append(tournament.getLevel().get());
    }

    titleBuilder.append(" Award Winners");
    return titleBuilder.toString();
  }
}
