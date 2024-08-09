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
import fll.util.FLLInternalException;
import fll.util.FOPUtils;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.report.awards.AwardCategory;
import fll.web.report.awards.AwardsScriptReport;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreType;
import fll.xml.SubjectiveScoreCategory;
import fll.xml.VirtualSubjectiveScoreCategory;
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

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.JUDGE, UserRole.REPORT_GENERATOR), false)) {
      return;
    }

    if (PromptSummarizeScores.checkIfSummaryUpdated(request, response, application, session,
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

    for (final String awardGroup : AwardsScriptReport.getAwardGroupOrder(connection, tournament)) {
      final Map<AwardCategory, Map<String, Map<Integer, ImmutablePair<Integer, Double>>>> teamSubjectiveRanks = FinalComputedScores.gatherRankedSubjectiveTeams(connection,
                                                                                                                                                                challengeDescription,
                                                                                                                                                                tournament);

      for (final SubjectiveScoreCategory category : challengeDescription.getSubjectiveCategories()) {
        final @Nullable Map<String, Map<Integer, ImmutablePair<Integer, Double>>> categoryTeamRanks = teamSubjectiveRanks.get(category);
        if (null == categoryTeamRanks) {
          throw new FLLInternalException("Cannot find subjective ranks for "
              + category.getName());
        }

        final Element ele = generateCategoryReport(connection, document, tournament, teams, awardGroup,
                                                   category.getName(), category.getTitle(), category.getScoreType(),
                                                   categoryTeamRanks);
        documentBody.appendChild(ele);
        ele.setAttribute("page-break-after", "always");
      }
      for (final VirtualSubjectiveScoreCategory category : challengeDescription.getVirtualSubjectiveCategories()) {
        final @Nullable Map<String, Map<Integer, ImmutablePair<Integer, Double>>> categoryTeamRanks = teamSubjectiveRanks.get(category);
        if (null == categoryTeamRanks) {
          throw new FLLInternalException("Cannot find subjective ranks for "
              + category.getName());
        }

        final Element ele = generateCategoryReport(connection, document, tournament, teams, awardGroup,
                                                   category.getName(), category.getTitle(), category.getScoreType(),
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
                                         final String categoryName,
                                         final String categoryTitle,
                                         final ScoreType categoryScoreType,
                                         final Map<String, Map<Integer, ImmutablePair<Integer, Double>>> categoryTeamRanks)
      throws SQLException {

    try (PreparedStatement prep = connection.prepareStatement("SELECT computed_total"
        + " FROM subjective_computed_scores"
        + " WHERE tournament = ?"
        + " AND category = ?"
        + " AND team_number = ?"
        + " ORDER BY judge ASC")) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setString(2, categoryName);

      final Element agReport = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

      final Element agBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      agReport.appendChild(agBlock);
      agBlock.appendChild(document.createTextNode(String.format("Category: %s - Award Group: %s", categoryTitle,
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
                                                FOPUtils.STANDARD_BORDER_WIDTH));
      headerRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                         String.format("Team Name")),
                                                FOPUtils.STANDARD_BORDER_WIDTH));
      headerRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                         String.format("Organization")),
                                                FOPUtils.STANDARD_BORDER_WIDTH));
      headerRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                         String.format("Judging Group")),
                                                FOPUtils.STANDARD_BORDER_WIDTH));
      headerRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                         String.format("Rank")),
                                                FOPUtils.STANDARD_BORDER_WIDTH));
      headerRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                         String.format("Scaled Score")),
                                                FOPUtils.STANDARD_BORDER_WIDTH));
      headerRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                         String.format("Raw Score(s)")),
                                                FOPUtils.STANDARD_BORDER_WIDTH));

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
        prep.setInt(3, t.getTeamNumber());
        try (ResultSet rs = prep.executeQuery()) {
          while (rs.next()) {
            final double raw = rs.getDouble(1);
            final String rawStr = Utilities.getFormatForScoreType(categoryScoreType).format(raw);
            rawScores.add(rawStr);
          }
        }
        d.rawScore = rawScores.stream().collect(Collectors.joining(", "));

        d.rank = rankAndScore.getLeft();
        d.scaledScore = rankAndScore.getRight();
        reportData.add(d);
      }

      if (reportData.isEmpty()) {
        final Element row = FOPUtils.createTableRow(document);
        tableBody.appendChild(row);
        final Element cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, "No data");
        row.appendChild(cell);
        cell.setAttribute("number-columns-spanned", "7");
      } else {
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
                                                  FOPUtils.STANDARD_BORDER_WIDTH));
          teamRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                           data.team.getTeamName()),
                                                  FOPUtils.STANDARD_BORDER_WIDTH));

          final @Nullable String organization = data.team.getOrganization();
          teamRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                           null == organization ? "" : organization),
                                                  FOPUtils.STANDARD_BORDER_WIDTH));
          teamRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                           data.team.getJudgingGroup()),
                                                  FOPUtils.STANDARD_BORDER_WIDTH));

          teamRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                           String.valueOf(data.rank)),
                                                  FOPUtils.STANDARD_BORDER_WIDTH));

          teamRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                           Utilities.getFloatingPointNumberFormat()
                                                                                    .format(data.scaledScore)),
                                                  FOPUtils.STANDARD_BORDER_WIDTH));

          teamRow.appendChild(FOPUtils.addBorders(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                                           data.rawScore),
                                                  FOPUtils.STANDARD_BORDER_WIDTH));
        } // foreach team
      }

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
