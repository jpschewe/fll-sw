/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.ScoreStandardization;
import fll.Team;
import fll.Tournament;
import fll.TournamentTeam;
import fll.db.AwardDeterminationOrder;
import fll.db.CategoriesIgnored;
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
import fll.web.report.awards.AwardCategory;
import fll.web.report.awards.ChampionshipCategory;
import fll.web.report.awards.HeadToHeadCategory;
import fll.web.scoreboard.Top10;
import fll.xml.ChallengeDescription;
import fll.xml.NonNumericCategory;
import fll.xml.PerformanceScoreCategory;
import fll.xml.SubjectiveScoreCategory;
import fll.xml.VirtualSubjectiveScoreCategory;
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

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static final int LINE_THICKNESS = 1;

  private static final int SEPARATOR_THICKNESS = 2;

  private static final int NUM_FINALISTS = 4;

  private static final int NUM_PERFORMANCE_FINALISTS = 1;

  private static final int NUM_NON_NUMERIC_FINALISTS = 1;

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

    final String groupName = WebUtils.getNonNullRequestParameter(request, "groupName");

    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);
    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
    final DataSource datasource = tournamentData.getDataSource();
    try (Connection connection = datasource.getConnection()) {
      ScoreStandardization.computeSummarizedScoresIfNeeded(connection, challengeDescription,
                                                          tournamentData.getCurrentTournament());

      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=awardSummarySheet.pdf");

      try {
        final Document document = generateReport(connection, challengeDescription, tournamentData, groupName);
        final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

        FOPUtils.renderPdf(fopFactory, document, response.getOutputStream());
      } catch (FOPException | TransformerException e) {
        throw new FLLInternalException("Error creating the award summary sheet", e);
      }

    } catch (final SQLException e) {
      throw new FLLRuntimeException(e);
    }
  }

  @SuppressFBWarnings(value = { "DLS_DEAD_LOCAL_STORE" }, justification = "Switch statement requires storing of variable")
  private Document generateReport(final Connection connection,
                                  final ChallengeDescription challengeDescription,
                                  final TournamentData tournamentData,
                                  final String groupName)
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

    final Element groupSection = FOPUtils.createXslFoElement(document, FOPUtils.INLINE_TAG);
    headerBlock.appendChild(groupSection);
    groupSection.appendChild(document.createTextNode("Judging Group: "));
    groupSection.appendChild(document.createTextNode(groupName));

    boolean first = true;
    for (AwardCategory category : AwardDeterminationOrder.get(connection, challengeDescription)) {
      switch (category) {
      case PerformanceScoreCategory awardCategory -> {
        final @Nullable Element performance = createPerformanceBlock(document, connection, challengeDescription,
                                                                     tournamentData, groupName, category);
        if (null != performance) {
          if (!first) {
            report.appendChild(FOPUtils.createHorizontalLineBlock(document, SEPARATOR_THICKNESS));
          } else {
            first = false;
          }
          report.appendChild(performance);
        }
      }
      case NonNumericCategory awardCategory -> {
        if (!CategoriesIgnored.isNonNumericCategoryIgnored(connection, tournamentData.getCurrentTournament().getLevel(),
                                                           awardCategory)) {
          if (!first) {
            report.appendChild(FOPUtils.createHorizontalLineBlock(document, SEPARATOR_THICKNESS));
          } else {
            first = false;
          }
          final Element element = createNonNumericBlock(document, awardCategory);
          report.appendChild(element);
        }
      }
      case SubjectiveScoreCategory awardCategory -> {
        final @Nullable Element subjectiveElement = createSubjectiveBlock(document, connection,
                                                                          challengeDescription.getWinner(),
                                                                          tournamentData.getCurrentTournament(),
                                                                          groupName, awardCategory.getName(),
                                                                          awardCategory.getTitle());
        if (null != subjectiveElement) {
          if (!first) {
            report.appendChild(FOPUtils.createHorizontalLineBlock(document, SEPARATOR_THICKNESS));
          } else {
            first = false;
          }
          report.appendChild(subjectiveElement);

        }
      }
      case VirtualSubjectiveScoreCategory awardCategory -> {
        final @Nullable Element subjectiveElement = createSubjectiveBlock(document, connection,
                                                                          challengeDescription.getWinner(),
                                                                          tournamentData.getCurrentTournament(),
                                                                          groupName, awardCategory.getName(),
                                                                          awardCategory.getTitle());
        if (null != subjectiveElement) {
          if (!first) {
            report.appendChild(FOPUtils.createHorizontalLineBlock(document, SEPARATOR_THICKNESS));
          } else {
            first = false;
          }
          report.appendChild(subjectiveElement);

        }
      }
      case ChampionshipCategory awardCategory -> {
        if (!first) {
          report.appendChild(FOPUtils.createHorizontalLineBlock(document, SEPARATOR_THICKNESS));
        } else {
          first = false;
        }
        final Element champions = createChampionsBlock(document);
        report.appendChild(champions);
      }
      case HeadToHeadCategory awardCategory -> {
        LOGGER.debug("Skipping head to head category");
      }
      default -> throw new RuntimeException("Unknown category type: "
          + category.getClass());
      }
    }

    return document;
  }

  private Element createChampionsBlock(final Document document) {
    final Element section = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    final Element title = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    section.appendChild(title);
    title.appendChild(document.createTextNode(ChampionshipCategory.INSTANCE.getTitle()));
    title.setAttribute("font-weight", "bold");

    final Element row1Block = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    section.appendChild(row1Block);

    final Element list = FOPUtils.createXslFoElement(document, "list-block");
    section.appendChild(list);

    final Element listItem = FOPUtils.createXslFoElement(document, "list-item");
    list.appendChild(listItem);
    listItem.setAttribute("keep-together.within-page", "always");

    final Element itemLabel = FOPUtils.createXslFoElement(document, "list-item-label");
    listItem.appendChild(itemLabel);

    final Element itemLabelBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    itemLabel.appendChild(itemLabelBlock);
    itemLabelBlock.appendChild(document.createTextNode("1."));
    itemLabel.setAttribute("end-indent", "label-end()");

    final Element itemBody = FOPUtils.createXslFoElement(document, "list-item-body");
    listItem.appendChild(itemBody);
    itemBody.setAttribute("start-indent", "body-start()");

    final Element bodyBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    itemBody.appendChild(bodyBlock);
    bodyBlock.appendChild(FOPUtils.createHorizontalLine(document, LINE_THICKNESS));

    return section;
  }

  private @Nullable Element createPerformanceBlock(final Document document,
                                                   final Connection connection,
                                                   final ChallengeDescription description,
                                                   final TournamentData tournamentData,
                                                   final String groupName,
                                                   final AwardCategory awardCategory)
      throws SQLException {
    final Map<Integer, TournamentTeam> tournamentTeams = Queries.getTournamentTeams(connection,
                                                                                    tournamentData.getCurrentTournament()
                                                                                                  .getTournamentID());
    final Set<Team> teamsNeedingSeeding = Queries.getTeamsNeedingRegularMatchPlayRuns(connection,
                                                                                      tournamentData.getRunMetadataFactory(),
                                                                                      tournamentTeams, true);

    final Element section = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    final Element title = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    section.appendChild(title);

    final Element categoryTitle = FOPUtils.createXslFoElement(document, FOPUtils.INLINE_TAG);
    title.appendChild(categoryTitle);
    categoryTitle.appendChild(document.createTextNode(awardCategory.getTitle()));
    categoryTitle.setAttribute("font-weight", "bold");

    if (!teamsNeedingSeeding.isEmpty()) {
      final Element allScoresEntered = FOPUtils.createXslFoElement(document, FOPUtils.INLINE_TAG);
      title.appendChild(allScoresEntered);
      allScoresEntered.appendChild(document.createTextNode(String.format(" %d teams have not finished regular match play",
                                                                         teamsNeedingSeeding.size())));
      allScoresEntered.setAttribute("font-style", "italic");
      allScoresEntered.setAttribute("font-size", "10pt");
    }

    final Element row1Block = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    section.appendChild(row1Block);

    final Element list = FOPUtils.createXslFoElement(document, "list-block");
    section.appendChild(list);

    final Map<String, List<Top10.ScoreEntry>> performanceData = Top10.getTableAsMapByJudgingStation(connection,
                                                                                                    description,
                                                                                                    tournamentData.getCurrentTournament(),
                                                                                                    true, false);

    final List<Top10.ScoreEntry> scores = performanceData.get(groupName);
    if (null == scores) {
      throw new FLLRuntimeException(String.format("Unable to find performance scores for Judging Station '%s'",
                                                  groupName));
    }

    if (scores.isEmpty()) {
      return null;
    }

    for (final Top10.ScoreEntry entry : scores) {
      if (entry.getRank() > NUM_PERFORMANCE_FINALISTS) {
        break;
      }

      final Element listItem = FOPUtils.createXslFoElement(document, "list-item");
      list.appendChild(listItem);
      listItem.setAttribute("keep-together.within-page", "always");

      final Element itemLabel = FOPUtils.createXslFoElement(document, "list-item-label");
      listItem.appendChild(itemLabel);

      final Element itemLabelBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      itemLabel.appendChild(itemLabelBlock);
      itemLabelBlock.appendChild(document.createTextNode(String.format("%d.", entry.getRank())));
      itemLabel.setAttribute("end-indent", "label-end()");

      final Element itemBody = FOPUtils.createXslFoElement(document, "list-item-body");
      listItem.appendChild(itemBody);
      itemBody.setAttribute("start-indent", "body-start()");

      final Element bodyBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      itemBody.appendChild(bodyBlock);
      bodyBlock.appendChild(document.createTextNode(String.valueOf(entry.getTeamNumber())));
      bodyBlock.appendChild(document.createTextNode(" "));
      bodyBlock.appendChild(document.createTextNode(entry.getTeamName()));
      bodyBlock.appendChild(document.createTextNode(" "));
      bodyBlock.appendChild(document.createTextNode(entry.getOrganization()));
      bodyBlock.appendChild(document.createTextNode(" - "));
      bodyBlock.appendChild(document.createTextNode(entry.getFormattedScore()));
    }

    return section;
  }

  private @Nullable Element createSubjectiveBlock(final Document document,
                                                  final Connection connection,
                                                  final WinnerType winnerCriteria,
                                                  final Tournament tournament,
                                                  final String judgingGroup,
                                                  final String awardCategoryName,
                                                  final String awardCategoryTitle)
      throws SQLException {
    final Element section = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    final Element title = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    section.appendChild(title);
    title.appendChild(document.createTextNode(awardCategoryTitle));
    title.setAttribute("font-weight", "bold");

    final Element row1Block = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    section.appendChild(row1Block);

    final Element list = FOPUtils.createXslFoElement(document, "list-block");
    section.appendChild(list);

    FinalComputedScores.iterateOverSubjectiveScores(connection, awardCategoryName, winnerCriteria, tournament,
                                                    judgingGroup, (teamNumber,
                                                                   score,
                                                                   rank) -> {
                                                      if (rank <= NUM_FINALISTS) {
                                                        try {
                                                          final Team team = Team.getTeamFromDatabase(connection,
                                                                                                     teamNumber);

                                                          final Element listItem = FOPUtils.createXslFoElement(document,
                                                                                                               "list-item");
                                                          list.appendChild(listItem);
                                                          listItem.setAttribute("keep-together.within-page", "always");

                                                          final Element itemLabel = FOPUtils.createXslFoElement(document,
                                                                                                                "list-item-label");
                                                          listItem.appendChild(itemLabel);

                                                          final Element itemLabelBlock = FOPUtils.createXslFoElement(document,
                                                                                                                     FOPUtils.BLOCK_TAG);
                                                          itemLabel.appendChild(itemLabelBlock);
                                                          itemLabelBlock.appendChild(document.createTextNode(String.format("%d.",
                                                                                                                           rank)));
                                                          itemLabel.setAttribute("end-indent", "label-end()");

                                                          final Element itemBody = FOPUtils.createXslFoElement(document,
                                                                                                               "list-item-body");
                                                          listItem.appendChild(itemBody);
                                                          itemBody.setAttribute("start-indent", "body-start()");

                                                          final Element bodyBlock = FOPUtils.createXslFoElement(document,
                                                                                                                FOPUtils.BLOCK_TAG);
                                                          itemBody.appendChild(bodyBlock);
                                                          bodyBlock.appendChild(document.createTextNode(String.valueOf(team.getTeamNumber())));
                                                          bodyBlock.appendChild(document.createTextNode(" "));
                                                          bodyBlock.appendChild(document.createTextNode(team.getTeamName()));
                                                          bodyBlock.appendChild(document.createTextNode(" "));
                                                          final String org = team.getOrganization();
                                                          bodyBlock.appendChild(document.createTextNode(null == org ? ""
                                                              : org));
                                                        } catch (final SQLException e) {
                                                          throw new FLLRuntimeException("Unable to find team with number "
                                                              + teamNumber, e);
                                                        }
                                                      }
                                                    });

    if (list.getChildNodes().getLength() < 1) {
      return null;
    } else {
      return section;
    }
  }

  private Element createNonNumericBlock(final Document document,
                                        final AwardCategory awardCategory) {
    final Element section = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    final Element title = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    section.appendChild(title);
    title.appendChild(document.createTextNode(awardCategory.getTitle()));
    title.setAttribute("font-weight", "bold");

    final Element row1Block = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    section.appendChild(row1Block);

    final Element list = FOPUtils.createXslFoElement(document, "list-block");
    section.appendChild(list);

    for (int i = 0; i < NUM_NON_NUMERIC_FINALISTS; ++i) {
      final Element listItem = FOPUtils.createXslFoElement(document, "list-item");
      list.appendChild(listItem);
      listItem.setAttribute("keep-together.within-page", "always");

      final Element itemLabel = FOPUtils.createSimpleListItemLabel(document);
      listItem.appendChild(itemLabel);
      itemLabel.setAttribute("end-indent", "label-end()");

      final Element itemBody = FOPUtils.createXslFoElement(document, "list-item-body");
      listItem.appendChild(itemBody);
      itemBody.setAttribute("start-indent", "body-start()");

      final Element bodyBlock = FOPUtils.createHorizontalLineBlock(document, LINE_THICKNESS);
      itemBody.appendChild(bodyBlock);
    }
    return section;
  }

}
