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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.Tournament;
import fll.TournamentTeam;
import fll.Utilities;
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
import fll.xml.ScoreCategory;
import fll.xml.SubjectiveScoreCategory;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Report showing all teams in an award group sorted by rank in their judging
 * group and then by scaled score.
 */
@WebServlet("/report/ScaledSubjectiveByAwardGroup")
public class ScaledSubjectiveByAwardGroup extends BaseFLLServlet {

  @Override
  protected void processRequest(HttpServletRequest request,
                                HttpServletResponse response,
                                ServletContext application,
                                HttpSession session)
      throws IOException, ServletException {

    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.JUDGE), false)) {
      return;
    }

    if (PromptSummarizeScores.checkIfSummaryUpdated(response, application, session,
                                                    "/report/ScaledSubjectiveByAwardGroup")) {
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    try (Connection connection = datasource.getConnection()) {

      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
      final Tournament tournament = Tournament.getCurrentTournament(connection);

      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=ScaledSubjectiveByAwardGroup.pdf");

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

    final Map<Integer, TournamentTeam> teams = Queries.getTournamentTeams(connection, tournament.getTournamentID());

    for (final String awardGroup : Queries.getAwardGroups(connection, tournament.getTournamentID())) {
      final Map<ScoreCategory, Map<String, Map<Integer, ImmutablePair<Integer, Double>>>> teamSubjectiveRanks = FinalComputedScores.gatherRankedSubjectiveTeams(connection,
                                                                                                                                                                challengeDescription.getSubjectiveCategories(),
                                                                                                                                                                challengeDescription.getWinner(),
                                                                                                                                                                tournament,
                                                                                                                                                                awardGroup);

      for (final SubjectiveScoreCategory category : challengeDescription.getSubjectiveCategories()) {
        final @Nullable Map<String, Map<Integer, ImmutablePair<Integer, Double>>> categoryTeamRanks = teamSubjectiveRanks.get(category);
        if (null == categoryTeamRanks) {
          throw new FLLInternalException("Cannot find subjective ranks for "
              + category.getName());
        }

        final Element ele = generateCategoryReport(connection, document, tournament, teams, awardGroup, category,
                                                   categoryTeamRanks);
        documentBody.appendChild(ele);
        ele.setAttribute("page-break-after", "always");
      }
    }

    return document;
  }

  private Element generateCategoryReport(final Connection connection,
                                         final Document document,
                                         final Tournament tournament,
                                         final Map<Integer, TournamentTeam> teams,
                                         final String awardGroup,
                                         final SubjectiveScoreCategory category,
                                         final Map<String, Map<Integer, ImmutablePair<Integer, Double>>> categoryTeamRanks)
      throws SQLException {

    try (PreparedStatement prep = connection.prepareStatement("SELECT computed_total"
        + " FROM subjective_computed_scores"
        + " WHERE tournament = ?"
        + " AND category = ?"
        + " AND goal_group = ?"
        + " AND team_number = ?"
        + " ORDER BY judge ASC")) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setString(2, category.getName());
      prep.setString(3, "");

      final Element agReport = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

      final Element agBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      agReport.appendChild(agBlock);
      agBlock.appendChild(document.createTextNode(String.format("Category: %s - Award Group: %s", category.getTitle(),
                                                                awardGroup)));
      agBlock.setAttribute("font-weight", "bold");
      agBlock.setAttribute(FOPUtils.TEXT_ALIGN_ATTRIBUTE, FOPUtils.TEXT_ALIGN_CENTER);

      final Element table = FOPUtils.createBasicTable(document);
      agReport.appendChild(table);
      table.appendChild(FOPUtils.createTableColumn(document, 1)); // team number
      table.appendChild(FOPUtils.createTableColumn(document, 1)); // team name
      table.appendChild(FOPUtils.createTableColumn(document, 1)); // organization
      table.appendChild(FOPUtils.createTableColumn(document, 1)); // judging group
      table.appendChild(FOPUtils.createTableColumn(document, 1)); // rank in judging group
      table.appendChild(FOPUtils.createTableColumn(document, 1)); // scaled score
      table.appendChild(FOPUtils.createTableColumn(document, 1)); // raw score(s)

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
      headerRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                         String.format("Organization")),
                                                ScheduleWriter.STANDARD_BORDER_WIDTH));
      headerRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                         String.format("Judging Group")),
                                                ScheduleWriter.STANDARD_BORDER_WIDTH));
      headerRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                         String.format("Rank")),
                                                ScheduleWriter.STANDARD_BORDER_WIDTH));
      headerRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                         String.format("Scaled Score")),
                                                ScheduleWriter.STANDARD_BORDER_WIDTH));
      headerRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                         String.format("Raw Score(s)")),
                                                ScheduleWriter.STANDARD_BORDER_WIDTH));

      table.appendChild(tableHeader);

      final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
      table.appendChild(tableBody);

      final List<Data> reportData = new LinkedList<>();
      for (final TournamentTeam t : teams.values()) {
        if (!awardGroup.equals(t.getAwardGroup())) {
          continue;
        }

        final Map<Integer, ImmutablePair<Integer, Double>> judgingRanks = categoryTeamRanks.get(t.getJudgingGroup());
        if (null == judgingRanks) {
          continue;
        }
        final ImmutablePair<Integer, Double> rankAndScore = judgingRanks.get(t.getTeamNumber());
        if (null == rankAndScore) {
          continue;
        }

        final Data d = new Data(t);

        final List<String> rawScores = new LinkedList<>();
        prep.setInt(4, t.getTeamNumber());
        try (ResultSet rs = prep.executeQuery()) {
          while (rs.next()) {
            final double raw = rs.getDouble(1);
            final String rawStr = Utilities.getFormatForScoreType(category.getScoreType()).format(raw);
            rawScores.add(rawStr);
          }
        }
        d.rawScore = rawScores.stream().collect(Collectors.joining(", "));

        d.rank = rankAndScore.getLeft();
        d.scaledScore = rankAndScore.getRight();
        reportData.add(d);
      }

      Collections.sort(reportData);

      for (final Data data : reportData) {
        if (!awardGroup.equals(data.team.getAwardGroup())) {
          // only output teams for the current award group
          continue;
        }

        final Element teamRow = FOPUtils.createTableRow(document);
        tableBody.appendChild(teamRow);

        teamRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                         String.valueOf(data.team.getTeamNumber())),
                                                ScheduleWriter.STANDARD_BORDER_WIDTH));
        teamRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                         data.team.getTeamName()),
                                                ScheduleWriter.STANDARD_BORDER_WIDTH));

        final @Nullable String organization = data.team.getOrganization();
        teamRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                         null == organization ? "" : organization),
                                                ScheduleWriter.STANDARD_BORDER_WIDTH));
        teamRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                         data.team.getJudgingGroup()),
                                                ScheduleWriter.STANDARD_BORDER_WIDTH));

        teamRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                         String.valueOf(data.rank)),
                                                ScheduleWriter.STANDARD_BORDER_WIDTH));

        teamRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                         Utilities.getFloatingPointNumberFormat()
                                                                                  .format(data.scaledScore)),
                                                ScheduleWriter.STANDARD_BORDER_WIDTH));

        teamRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                         data.rawScore),
                                                ScheduleWriter.STANDARD_BORDER_WIDTH));
      } // foreach team

      return agReport;
    }
  }

  // CHECKSTYLE:OFF data class
  private static final class Data implements Comparable<Data> {
    Data(final TournamentTeam team) {
      this.team = team;
    }

    final TournamentTeam team;

    int rank = -1;

    String rawScore = "";

    double scaledScore = Double.NaN;

    @Override
    public int compareTo(final Data o) {
      final int rankOrder = Integer.compare(this.rank, o.rank);
      if (0 == rankOrder) {
        final int scaledOrder = Double.compare(this.scaledScore, o.scaledScore);
        if (0 == scaledOrder) {
          return Integer.compare(this.team.getTeamNumber(), o.team.getTeamNumber());
        } else {
          // descending
          return -1
              * scaledOrder;
        }
      } else {
        return rankOrder;
      }
    }

    @Override
    public int hashCode() {
      return team.hashCode();
    }

    @Override
    public boolean equals(final @Nullable Object o) {
      if (this == o) {
        return true;
      } else if (null == o) {
        return false;
      } else if (this.getClass().equals(o.getClass())) {
        final Data other = (Data) o;
        return this.team.equals(other.team);
      } else {
        return false;
      }
    }
  }
  // CHECKSTYLE:ON

}
