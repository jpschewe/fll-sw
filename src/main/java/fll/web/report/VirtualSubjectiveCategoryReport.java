/*
 * Copyright (c) 2025 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import javax.xml.transform.TransformerException;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.SubjectiveScore;
import fll.Team;
import fll.Tournament;
import fll.TournamentTeam;
import fll.Utilities;
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
    final Set<SubjectiveScoreCategory> referencedCategories = getReferencedCategories(category);

    teams.values().stream().sorted(Comparator.comparing(TournamentTeam::getJudgingGroup)).forEach(team -> {
      try {
        final Map<SubjectiveScoreCategory, Collection<SubjectiveScore>> referencedScores = getReferencedScores(connection,
                                                                                                               tournament,
                                                                                                               referencedCategories,
                                                                                                               team);
        final Element ele = generateTeamTable(document, team, category, referencedScores);

        documentBody.appendChild(ele);
        ele.setAttribute("space-after", "10");
      } catch (final SQLException e) {
        throw new FLLRuntimeException(e);
      }
    });

    return document;
  }

  private static final double BORDER_WIDTH = 1.0;

  private static final String SIDE_PADDING = "0.02in";

  private static Element generateTeamTable(final Document document,
                                           final TournamentTeam team,
                                           final VirtualSubjectiveScoreCategory category,
                                           final Map<SubjectiveScoreCategory, Collection<SubjectiveScore>> referencedScores)
      throws SQLException {

    final List<String> judges = referencedScores.values().stream().flatMap(Collection::stream)
                                                .map(SubjectiveScore::getJudge).distinct().sorted().toList();

    final Element teamTable = FOPUtils.createBasicTable(document);

    // --- table columns
    teamTable.appendChild(FOPUtils.createTableColumn(document, 25)); // Team info, category goals

    for (int i = 0; i < judges.size(); ++i) {
      teamTable.appendChild(FOPUtils.createTableColumn(document, 10)); // judge
    }

    teamTable.appendChild(FOPUtils.createTableColumn(document, 10)); // Average & total
    // --- end table columns

    // --- header
    final Element headerRow = FOPUtils.createTableHeader(document);
    teamTable.appendChild(headerRow);

    final Element teamInfoCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                          String.format("Team %d %s - %s", team.getTeamNumber(),
                                                                        team.getTeamName(), team.getJudgingGroup()));
    headerRow.appendChild(teamInfoCell);
    FOPUtils.addBorders(teamInfoCell, BORDER_WIDTH);
    teamInfoCell.setAttribute("padding-left", SIDE_PADDING);
    teamInfoCell.setAttribute("padding-right", SIDE_PADDING);

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
    final NumberFormat scoreFormatter = Utilities.getFormatForScoreType(category.getScoreType());
    double scoreTotal = 0;
    for (final SubjectiveGoalRef goalRef : category.getGoalReferences()) {
      final Element row = FOPUtils.createTableRow(document);
      tableBody.appendChild(row);

      final Element goalCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT,
                                                        String.format("%s: %s", goalRef.getCategory().getTitle(),
                                                                      goalRef.getGoal().getTitle()));
      row.appendChild(goalCell);
      FOPUtils.addBorders(goalCell, BORDER_WIDTH);
      goalCell.setAttribute("padding-left", SIDE_PADDING);
      goalCell.setAttribute("padding-right", SIDE_PADDING);

      final SubjectiveScoreCategory referencedCategory = goalRef.getCategory();
      final Collection<SubjectiveScore> scores = referencedScores.getOrDefault(referencedCategory,
                                                                               Collections.emptyList());
      final NumberFormat referencedCategoryScoreFormatter = Utilities.getFormatForScoreType(referencedCategory.getScoreType());
      int scoresCount = 0;
      double scoresSum = 0;
      for (final String judge : judges) {
        final Optional<SubjectiveScore> score = scores.stream().filter(s -> s.getJudge().equals(judge)).findAny();

        final String formattedScore;
        if (score.isPresent()) {
          if (!score.get().getNoShow()) {
            final double rawScore = score.get().getStandardSubScores().getOrDefault(goalRef.getGoal().getName(), 0D);
            ++scoresCount;
            scoresSum += rawScore;

            formattedScore = referencedCategoryScoreFormatter.format(rawScore);
          } else {
            formattedScore = "No Show";
          }
        } else {
          formattedScore = Utilities.NON_BREAKING_SPACE_STRING;
        }

        final Element scoreCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, formattedScore);
        row.appendChild(scoreCell);
        FOPUtils.addBorders(scoreCell, BORDER_WIDTH);
      }

      final String formattedAverage;
      if (scoresCount > 0) {
        final double average = scoresSum
            / scoresCount;
        formattedAverage = scoreFormatter.format(average);

        scoreTotal += average;
      } else {
        formattedAverage = Utilities.NON_BREAKING_SPACE_STRING;
      }

      final Element averageCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, formattedAverage);
      row.appendChild(averageCell);
      FOPUtils.addBorders(averageCell, BORDER_WIDTH);
    }
    // --- end goal rows

    // --- totals row
    final Element totalRow = FOPUtils.createTableRow(document);
    tableBody.appendChild(totalRow);

    final Element totalCellHeader = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, "Total Score");
    totalRow.appendChild(totalCellHeader);

    totalCellHeader.setAttribute("number-columns-spanned", String.valueOf(judges.size()
        + 1));
    FOPUtils.addBorders(totalCellHeader, BORDER_WIDTH);
    totalCellHeader.setAttribute("padding-left", SIDE_PADDING);
    totalCellHeader.setAttribute("padding-right", SIDE_PADDING);
    totalCellHeader.setAttribute("font-weight", "bold");

    final String formattedTotal = scoreFormatter.format(scoreTotal);
    final Element totalCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, formattedTotal);
    totalRow.appendChild(totalCell);
    FOPUtils.addBorders(totalCell, BORDER_WIDTH);

    // --- end totals row

    return teamTable;
  }

  private static Set<SubjectiveScoreCategory> getReferencedCategories(final VirtualSubjectiveScoreCategory category) {
    return category.getGoalReferences().stream().map(SubjectiveGoalRef::getCategory).collect(Collectors.toSet());
  }

  private static Map<SubjectiveScoreCategory, Collection<SubjectiveScore>> getReferencedScores(final Connection connection,
                                                                                               final Tournament tournament,
                                                                                               final Set<SubjectiveScoreCategory> referencedCategories,
                                                                                               final Team team)
      throws SQLException {
    final Map<SubjectiveScoreCategory, Collection<SubjectiveScore>> referencedScores = new HashMap<>();
    for (final SubjectiveScoreCategory referencedCategory : referencedCategories) {
      final Collection<SubjectiveScore> scores = SubjectiveScore.getScoresForTeam(connection, referencedCategory,
                                                                                  tournament, team);
      referencedScores.put(referencedCategory, scores);
    }
    return referencedScores;
  }

}
