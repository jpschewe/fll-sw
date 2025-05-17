/*
 * Copyright (c) 2025 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;
import javax.xml.transform.TransformerException;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.SubjectiveScore;
import fll.Tournament;
import fll.TournamentTeam;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.FOPUtils;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.TournamentData;
import fll.web.UserRole;
import fll.web.WebUtils;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveGoalRef;
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
 * PDF report displaying the raw score information for a virtual subjective
 * category.
 */
@WebServlet("/report/VirtualSubjectiveCategoryReport")
public class VirtualSubjectiveCategoryReport extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.HEAD_JUDGE, UserRole.REPORT_GENERATOR), false)) {
      return;
    }

    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);
    final Tournament tournament = tournamentData.getCurrentTournament();

    final DataSource datasource = tournamentData.getDataSource();

    final String categoryName = WebUtils.getNonNullRequestParameter(request, "categoryName");
    final @Nullable VirtualSubjectiveScoreCategory category = challengeDescription.getVirtualSubjectiveCategoryByName(categoryName);
    if (null == category) {
      SessionAttributes.appendToMessage(session,
                                        String.format("<p class='error'>There is no virtual subjective category with the name '%' defined in the challenge description</p>",
                                                      categoryName));
      WebUtils.sendRedirect(response, "index.jsp");
      return;
    }

    // TODO: decide if I need to compute summarized scores here

    try (Connection connection = datasource.getConnection()) {

      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition",
                         String.format("filename=virt-category-%s-details.pdf", category.getName()));

      try {
        final Document document = generateReport(connection, tournament, category);
        final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();
        FOPUtils.renderPdf(fopFactory, document, response.getOutputStream());
      } catch (FOPException | TransformerException e) {
        throw new FLLInternalException(e);
      }

    } catch (final SQLException e) {
      throw new FLLRuntimeException(e);
    }
  }

  private static Document generateReport(final Connection connection,
                                         final Tournament tournament,
                                         final VirtualSubjectiveScoreCategory category)
      throws SQLException {

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
    documentBody.setAttribute("font-size", "8pt");

    final Map<Integer, TournamentTeam> teams = Queries.getTournamentTeams(connection, tournament.getTournamentID());

    for (final Map.Entry<Integer, TournamentTeam> entry : teams.entrySet()) {
      final Element ele = generateTeamTable(connection, tournament, document, entry.getValue(), category);

      documentBody.appendChild(ele);
      // ele.setAttribute("page-break-after", "always");
      ele.setAttribute("space-after", "10");
    }

    return document;
  }

  private static final double BORDER_WIDTH = 1.0;

  private static Element generateTeamTable(Connection connection,
                                           Tournament tournament,
                                           final Document document,
                                           final TournamentTeam team,
                                           final VirtualSubjectiveScoreCategory category)
      throws SQLException {
    final List<String> judges = collectJudges(connection, tournament, category, team);

    final Element teamTable = FOPUtils.createBasicTable(document);

    // --- table columns
    teamTable.appendChild(FOPUtils.createTableColumn(document, 15)); // Team info, category goals

    for (int i = 0; i < judges.size(); ++i) {
      teamTable.appendChild(FOPUtils.createTableColumn(document, 10)); // judge
    }

    teamTable.appendChild(FOPUtils.createTableColumn(document, 10)); // Average & total
    // --- end table columns

    // --- header
    final Element headerRow = FOPUtils.createTableHeader(document);
    teamTable.appendChild(headerRow);

    final Element teamInfoCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                          String.format("Team %d %s", team.getTeamNumber(),
                                                                        team.getTeamName()));
    headerRow.appendChild(teamInfoCell);
    FOPUtils.addBorders(teamInfoCell, BORDER_WIDTH);

    for (final String judge : judges) {
      final Element judgeCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, judge);
      headerRow.appendChild(judgeCell);
      FOPUtils.addBorders(judgeCell, BORDER_WIDTH);
    }

    final Element averageHeader = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, "Average");
    headerRow.appendChild(averageHeader);
    FOPUtils.addBorders(averageHeader, BORDER_WIDTH);
    // --- end header

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    teamTable.appendChild(tableBody);

    // --- goal rows
    for (final SubjectiveGoalRef goalRef : category.getGoalReferences()) {
      final Element row = FOPUtils.createTableRow(document);
      tableBody.appendChild(row);

      final Element goalCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                        goalRef.getGoal().getTitle());
      row.appendChild(goalCell);
      FOPUtils.addBorders(goalCell, BORDER_WIDTH);

      for (final String judge : judges) {
        // FIXME need scores

        final Element scoreCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT,
                                                           String.format("Score for judge %s", judge));
        row.appendChild(scoreCell);
        FOPUtils.addBorders(scoreCell, BORDER_WIDTH);
      }

      final Element averageCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, "AVG");
      row.appendChild(averageCell);
      FOPUtils.addBorders(averageCell, BORDER_WIDTH);
    }
    // --- end goal rows

    // --- totals row
    // --- end totals row

    return teamTable;
  }

  /**
   * Collect the list of judges for a category and team.
   * 
   * @return list of judges sorted by name
   */
  private static List<String> collectJudges(final Connection connection,
                                            final Tournament tournament,
                                            final VirtualSubjectiveScoreCategory category,
                                            final TournamentTeam team)
      throws SQLException {
    final Set<String> judges = new HashSet<>();
    for (final SubjectiveGoalRef goalRef : category.getGoalReferences()) {
      final Collection<String> categoryJudges = collectJudges(connection, tournament, goalRef.getCategory(), team);
      judges.addAll(categoryJudges);
    }

    return judges.stream().sorted().toList();
  }

  private static Collection<String> collectJudges(final Connection connection,
                                                  final Tournament tournament,
                                                  final SubjectiveScoreCategory category,
                                                  final TournamentTeam team)
      throws SQLException {
    final Collection<SubjectiveScore> scores = SubjectiveScore.getScoresForTeam(connection, category, tournament, team);
    return scores.stream().map(SubjectiveScore::getJudge).toList();
  }

}
