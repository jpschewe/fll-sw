/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

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
import fll.Utilities;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.util.FOPUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.playoff.Playoff;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreType;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Report displaying which teams won each playoff bracket.
 */
@WebServlet("/report/PlayoffReport")
public class PlayoffReport extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

      final Tournament tournament = Tournament.findTournamentByID(connection, Queries.getCurrentTournament(connection));

      final Document doc = createReport(connection, tournament, challengeDescription);
      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

      if (LOGGER.isTraceEnabled()) {
        try (StringWriter writer = new StringWriter()) {
          XMLUtils.writeXML(doc, writer);
          LOGGER.trace(writer.toString());
        }
      }

      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=playoffReport.pdf");

      FOPUtils.renderPdf(fopFactory, doc, response.getOutputStream());
    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new RuntimeException(e);
    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the playoff report", e);
    }
  }

  private Document createReport(final Connection connection,
                                final Tournament tournament,
                                final ChallengeDescription challengeDescription)
      throws SQLException {

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
    FOPUtils.createSimplePageMaster(document, layoutMasterSet, pageMasterName, FOPUtils.PAGE_LETTER_SIZE,
                                    new FOPUtils.Margins(topMargin, bottomMargin, leftMargin, rightMargin),
                                    headerHeight, footerHeight);

    final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
    rootElement.appendChild(pageSequence);

    final Element header = createHeader(document, challengeDescription, tournament);
    pageSequence.appendChild(header);

    final Element footer = FOPUtils.createSimpleFooter(document);
    pageSequence.appendChild(footer);

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    // TODO: limit to brackets that have really been completed
    final List<String> playoffDivisions = Playoff.getPlayoffBrackets(connection, tournament.getTournamentID());
    for (final String division : playoffDivisions) {

      final Element paragraph = processDivision(connection, tournament, document, division,
                                                challengeDescription.getPerformance().getScoreType());
      documentBody.appendChild(paragraph);
    }

    return document;
  }

  /**
   * Create the paragraph for the specified division.
   */
  private Element processDivision(final Connection connection,
                                  final Tournament tournament,
                                  final Document document,
                                  final String division,
                                  final ScoreType performanceScoreType)
      throws SQLException {

    final Element paragraph = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    final Element title = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    paragraph.appendChild(title);
    title.setAttribute("font-weight", "bold");
    title.appendChild(document.createTextNode("Results for head to head bracket "
        + division));

    final int maxRun = Playoff.getMaxPerformanceRound(connection, tournament.getTournamentID(), division);

    if (maxRun < 1) {
      final Element e1 = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      paragraph.appendChild(e1);
      e1.appendChild(document.createTextNode("Cannot determine max run number for this playoff bracket. This is an internal error"));
    } else {
      try (
          PreparedStatement teamPrep = connection.prepareStatement("SELECT Teams.TeamNumber, Teams.TeamName, Teams.Organization" //
              + " FROM PlayoffData, Teams" //
              + " WHERE PlayoffData.Tournament = ?" //
              + " AND PlayoffData.event_division = ?" //
              + " AND PlayoffData.run_number = ?" //
              + " AND Teams.TeamNumber = PlayoffData.team"//
              + " ORDER BY PlayoffData.linenumber" //
          );
          PreparedStatement scorePrep = connection.prepareStatement("SELECT ComputedTotal" //
              + " FROM Performance" //
              + " WHERE Tournament = ?" //
              + " AND TeamNumber = ?" //
              + " AND RunNumber = ?"//
          )) {
        teamPrep.setInt(1, tournament.getTournamentID());
        teamPrep.setString(2, division);

        // figure out the last teams
        final List<String> lastTeams = new LinkedList<>();

        teamPrep.setInt(3, maxRun
            - 1);
        scorePrep.setInt(1, tournament.getTournamentID());
        scorePrep.setInt(3, maxRun
            - 1);
        try (ResultSet team1Result = teamPrep.executeQuery()) {
          while (team1Result.next()) {
            final int teamNumber = team1Result.getInt(1);
            final String teamName = team1Result.getString(2);
            final String organization = team1Result.getString(3);

            scorePrep.setInt(2, teamNumber);
            try (ResultSet scoreResult = scorePrep.executeQuery()) {
              final String scoreStr;
              if (scoreResult.next()) {
                scoreStr = Utilities.getFormatForScoreType(performanceScoreType).format(scoreResult.getDouble(1));
              } else {
                scoreStr = "unknown";
              }

              lastTeams.add(String.format("Team %d from %s - %s with a score of %s", teamNumber, organization, teamName,
                                          scoreStr));
            } // scoreResult
          }
        } // teamResult

        // determine the winners
        int lastTeamsIndex = 0;
        teamPrep.setInt(3, maxRun);
        try (ResultSet team2Result = teamPrep.executeQuery()) {
          while (team2Result.next()) {
            final int teamNumber = team2Result.getInt(1);
            final String teamName = team2Result.getString(2);

            final Element e1 = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
            paragraph.appendChild(e1);
            e1.appendChild(document.createTextNode(String.format("Competing for places %d and %d", lastTeamsIndex
                + 1, lastTeamsIndex
                    + 2)));

            if (lastTeamsIndex < lastTeams.size()) {
              final Element teamEle = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
              paragraph.appendChild(teamEle);
              teamEle.appendChild(document.createTextNode(lastTeams.get(lastTeamsIndex)));
            } else {
              final Element teamEle = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
              paragraph.appendChild(teamEle);
              teamEle.appendChild(document.createTextNode("Internal error, unknown team competing"));
            }
            ++lastTeamsIndex;

            if (lastTeamsIndex < lastTeams.size()) {
              final Element teamEle = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
              paragraph.appendChild(teamEle);
              teamEle.appendChild(document.createTextNode(lastTeams.get(lastTeamsIndex)));
            } else {
              final Element teamEle = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
              paragraph.appendChild(teamEle);
              teamEle.appendChild(document.createTextNode("Internal error, unknown team competing"));
            }
            ++lastTeamsIndex;

            final Element winnerEle = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
            paragraph.appendChild(winnerEle);
            winnerEle.appendChild(document.createTextNode(String.format("The winner is team %d - %s", teamNumber,
                                                                        teamName)));

            paragraph.appendChild(FOPUtils.createBlankLine(document));

          } // foreach result
        } // teamResult
      } // prepared statements
    } // finished playoff

    return paragraph;
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
    if (null != tournament.getLevel()) {
      tournamentTitle = String.format("%s: %s", tournament.getLevel(), tournamentName);
    } else {
      tournamentTitle = tournamentName;
    }
    subtitleBlock.appendChild(document.createTextNode(tournamentTitle));

    final Element subtitleCenter = FOPUtils.createXslFoElement(document, "leader");
    subtitleBlock.appendChild(subtitleCenter);
    subtitleCenter.setAttribute("leader-pattern", "space");

    if (null != tournament.getDate()) {
      final String dateString = String.format("Date: %s", AwardsReport.DATE_FORMATTER.format(tournament.getDate()));

      subtitleBlock.appendChild(document.createTextNode(dateString));
    }

    staticContent.appendChild(FOPUtils.createHorizontalLine(document, 1));

    return staticContent;
  }

  private static String createTitle(final ChallengeDescription description,
                                    final Tournament tournament) {
    final StringBuilder titleBuilder = new StringBuilder();
    titleBuilder.append(description.getTitle());

    if (null != tournament.getLevel()) {
      titleBuilder.append(" ");
      titleBuilder.append(tournament.getLevel());
    }

    titleBuilder.append(" Head to Head Winners");
    return titleBuilder.toString();
  }

}
