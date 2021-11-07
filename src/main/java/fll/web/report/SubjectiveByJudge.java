/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;
import javax.xml.transform.TransformerException;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Tournament;
import fll.TournamentTeam;
import fll.Utilities;
import fll.db.Queries;
import fll.scheduler.ScheduleWriter;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.FOPUtils;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveScoreCategory;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Generate a report showing all subjective judge scores for each team by award
 * group.
 */
@WebServlet("/report/SubjectiveByJudge")
public class SubjectiveByJudge extends BaseFLLServlet {
  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(HttpServletRequest request,
                                HttpServletResponse response,
                                ServletContext application,
                                HttpSession session)
      throws IOException, ServletException {

    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.HEAD_JUDGE), false)) {
      return;
    }

    if (PromptSummarizeScores.checkIfSummaryUpdated(response, application, session, "/report/SubjectiveByJudge")) {
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    try (Connection connection = datasource.getConnection()) {

      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
      final int tournamentID = Queries.getCurrentTournament(connection);
      final Tournament tournament = Tournament.findTournamentByID(connection, tournamentID);

      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=subjectiveByJudge.pdf");

      try {
        final Document document = generateReport(connection, challengeDescription, tournament);
        final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

        FOPUtils.renderPdf(fopFactory, document, response.getOutputStream());
      } catch (FOPException | TransformerException e) {
        throw new FLLInternalException("Error creating the final scoresPDF", e);
      }

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private Document generateReport(final Connection connection,
                                  final ChallengeDescription challengeDescription,
                                  final Tournament tournament)
      throws SQLException, IOException {
    if (tournament.getTournamentID() != Queries.getCurrentTournament(connection)) {
      throw new FLLRuntimeException("Cannot generate final score report for a tournament other than the current tournament");
    }

    final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();

    final Element rootElement = FOPUtils.createRoot(document);
    document.appendChild(rootElement);

    final Element layoutMasterSet = FOPUtils.createXslFoElement(document, "layout-master-set");
    rootElement.appendChild(layoutMasterSet);

    final String pageMasterName = "simple";
    final Element pageMaster = FOPUtils.createSimplePageMaster(document, pageMasterName);
    layoutMasterSet.appendChild(pageMaster);
    pageMaster.setAttribute("reference-orientation", "90");

    final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
    rootElement.appendChild(pageSequence);
    pageSequence.setAttribute("id", FOPUtils.PAGE_SEQUENCE_NAME);

    final Element footer = FOPUtils.createSimpleFooter(document);
    pageSequence.appendChild(footer);

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    for (final String awardGroup : Queries.getAwardGroups(connection, tournament.getTournamentID())) {
      final Element ele = generateAwardGroupReport(connection, document, challengeDescription, tournament, awardGroup);
      documentBody.appendChild(ele);
      ele.setAttribute("page-break-after", "always");
    }

    return document;
  }

  private Element generateAwardGroupReport(final Connection connection,
                                           final Document document,
                                           final ChallengeDescription challengeDescription,
                                           final Tournament tournament,
                                           final String awardGroup)
      throws SQLException {
    final Element agReport = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    final Element agBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    agReport.appendChild(agBlock);
    agBlock.appendChild(document.createTextNode(awardGroup));
    agBlock.setAttribute("font-weight", "bold");

    // TODO: this method is not a terribly efficient way to get this data from a
    // database perspective. There are lots of round trips to the database. However
    // given that most of the database should be in memory this is probably OK.

    // collect list of judges per category
    final Map<SubjectiveScoreCategory, List<String>> judgesPerCategory = new LinkedHashMap<>();
    try (PreparedStatement getJudges = connection.prepareStatement("SELECT id as judge" //
        + " FROM judges" //
        + " WHERE tournament = ?" //
        + " AND category = ?" //
        + " AND station = ?" //
        + " ORDER BY judge ASC")) {
      getJudges.setInt(1, tournament.getTournamentID());
      getJudges.setString(3, awardGroup);

      for (final SubjectiveScoreCategory category : challengeDescription.getSubjectiveCategories()) {
        getJudges.setString(2, category.getName());
        try (ResultSet rs = getJudges.executeQuery()) {
          final List<String> judges = new LinkedList<>();
          while (rs.next()) {
            final String judge = castNonNull(rs.getString("judge"));
            judges.add(judge);
          }
          judgesPerCategory.put(category, judges);
        }
      } // foreach category
    }
    LOGGER.debug("judgesPerCategory: {}", judgesPerCategory);

    final Element table = FOPUtils.createBasicTable(document);
    agReport.appendChild(table);
    table.appendChild(FOPUtils.createTableColumn(document, 1)); // team number
    table.appendChild(FOPUtils.createTableColumn(document, 1)); // team name
    judgesPerCategory.forEach((category,
                               judges) -> {
      judges.forEach(judge -> {
        table.appendChild(FOPUtils.createTableColumn(document, 1));
      });
    });

    final Element tableHeader = FOPUtils.createTableHeader(document);
    tableHeader.setAttribute("font-weight", "bold");

    final Element headerRow = FOPUtils.createTableRow(document);
    tableHeader.appendChild(headerRow);

    headerRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                       String.format("Team #")),
                                              ScheduleWriter.STANDARD_BORDER_WIDTH));
    headerRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                       String.format("Team Name")),
                                              ScheduleWriter.STANDARD_BORDER_WIDTH));
    judgesPerCategory.forEach((category,
                               judges) -> {
      judges.forEach(judge -> {
        final String header = String.format("%s - %s", category.getTitle(), judge);
        headerRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                           header),
                                                  ScheduleWriter.STANDARD_BORDER_WIDTH));
      });
    });

    table.appendChild(tableHeader);

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    table.appendChild(tableBody);

    final Map<Integer, TournamentTeam> teams = Queries.getTournamentTeams(connection, tournament.getTournamentID());
    try (
        PreparedStatement getScores = connection.prepareStatement("SELECT category, team_number, judge, computed_total, no_show, standardized_score"
            + " FROM subjective_computed_scores"
            + " WHERE tournament = ? "
            + " AND category = ?"
            + " AND goal_group = ''"
            + " AND team_number = ?"
            + " AND judge = ?")) {
      for (final TournamentTeam team : teams.values()) {
        if (!awardGroup.equals(team.getAwardGroup())) {
          // only output teams for the current award group
          continue;
        }

        final Element teamRow = FOPUtils.createTableRow(document);
        tableBody.appendChild(teamRow);

        teamRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                         String.valueOf(team.getTeamNumber())),
                                                ScheduleWriter.STANDARD_BORDER_WIDTH));
        teamRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                         team.getTeamName()),
                                                ScheduleWriter.STANDARD_BORDER_WIDTH));

        getScores.setInt(1, tournament.getTournamentID());
        getScores.setInt(3, team.getTeamNumber());

        for (Map.Entry<SubjectiveScoreCategory, List<String>> e : judgesPerCategory.entrySet()) {
          final SubjectiveScoreCategory category = e.getKey();
          getScores.setString(2, category.getName());

          for (final String judge : e.getValue()) {

            getScores.setString(4, judge);

            try (ResultSet rs = getScores.executeQuery()) {
              final String text;
              if (rs.next()) {
                final boolean noShow = rs.getBoolean("no_show");
                if (noShow) {
                  text = "No Show";
                } else {
                  final double raw = rs.getDouble("computed_total");
                  final double scaled = rs.getDouble("standardized_score");
                  final String rawText = Utilities.getFormatForScoreType(category.getScoreType()).format(raw);
                  final String scaledText = Utilities.getFloatingPointNumberFormat().format(scaled);
                  text = String.format("%s / %s", rawText, scaledText);
                }
              } else {
                text = String.valueOf(Utilities.NON_BREAKING_SPACE);
              }
              teamRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                               text),
                                                      ScheduleWriter.STANDARD_BORDER_WIDTH));
            }
          } // foreach judge
        } // foreach category
      } // foreach team
    }

    return agReport;
  }

}
