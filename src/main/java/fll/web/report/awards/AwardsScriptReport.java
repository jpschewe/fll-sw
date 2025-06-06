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
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.sql.DataSource;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.diffplug.common.base.Errors;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.ScoreStandardization;
import fll.Team;
import fll.Tournament;
import fll.TournamentLevel;
import fll.Utilities;
import fll.db.AdvancingTeam;
import fll.db.AwardWinner;
import fll.db.AwardWinners;
import fll.db.AwardsScript;
import fll.db.CategoriesIgnored;
import fll.db.OverallAwardWinner;
import fll.db.Queries;
import fll.db.RunMetadataFactory;
import fll.db.TournamentParameters;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.FOPUtils;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.TournamentData;
import fll.web.UserRole;
import fll.web.api.AwardsReportSortedGroupsServlet;
import fll.web.report.FinalComputedScores;
import fll.web.report.PlayoffReport;
import fll.web.report.finalist.FinalistDBRow;
import fll.web.report.finalist.FinalistSchedule;
import fll.web.scoreboard.Top10;
import fll.xml.ChallengeDescription;
import fll.xml.NonNumericCategory;
import fll.xml.PerformanceScoreCategory;
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
 * Generate the awards script as a PDF.
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

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.HEAD_JUDGE, UserRole.REPORT_GENERATOR), false)) {
      return;
    }

    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);
    final DataSource datasource = tournamentData.getDataSource();
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    try (Connection connection = datasource.getConnection()) {
      ScoreStandardization.computeSummarizedScoresIfNeeded(connection, description,
                                                          tournamentData.getCurrentTournament());

      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=awardsScript.pdf");

      try {
        final Document document = generateDocument(tournamentData, description, connection);

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
                                          final RunMetadataFactory runMetadataFactory,
                                          final VelocityContext templateContext)
      throws SQLException {
    for (final AwardsScript.Macro macro : AwardsScript.Macro.values()) {
      final String value = AwardsScript.getMacroValue(connection, runMetadataFactory, macro);
      templateContext.put(macro.getText(), value);
    }
  }

  private Document generateDocument(final TournamentData tournamentData,
                                    final ChallengeDescription description,
                                    final Connection connection)
      throws SQLException {
    final Tournament tournament = tournamentData.getCurrentTournament();

    final VelocityContext templateContext = new VelocityContext();
    addMacrosToTemplateContext(connection, tournamentData.getRunMetadataFactory(), templateContext);

    final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();

    final Element rootElement = FOPUtils.createRoot(document);
    document.appendChild(rootElement);
    rootElement.setAttribute("id", FOPUtils.PAGE_SEQUENCE_NAME);

    final Element layoutMasterSet = FOPUtils.createXslFoElement(document, "layout-master-set");
    rootElement.appendChild(layoutMasterSet);

    final double leftMargin = 0.5;
    final double rightMargin = leftMargin;
    final double topMargin = 0.5;
    final double bottomMargin = 0.5;
    final double headerHeight = 1.2;
    final double footerHeight = 0.3;

    final String pageMasterName = "simple";
    final Element pageMaster = FOPUtils.createSimplePageMaster(document, pageMasterName, FOPUtils.PAGE_LETTER_SIZE,
                                                               new FOPUtils.Margins(topMargin, bottomMargin, leftMargin,
                                                                                    rightMargin),
                                                               headerHeight, footerHeight);
    layoutMasterSet.appendChild(pageMaster);

    final Element pageSequenceStart = FOPUtils.createPageSequence(document, pageMasterName);
    rootElement.appendChild(pageSequenceStart);

    final Element headerStart = createHeader(document, description, tournament, null, null);
    pageSequenceStart.appendChild(headerStart);

    final Element footerStart = createFooter(document, null);
    pageSequenceStart.appendChild(footerStart);

    final Element documentBodyStart = FOPUtils.createBody(document);
    pageSequenceStart.appendChild(documentBodyStart);

    final Element intro = createSectionBlock(connection, document, tournament, templateContext,
                                             AwardsScript.Section.FRONT_MATTER);
    documentBodyStart.appendChild(intro);
    intro.setAttribute("space-after", BLOCK_SPACING);

    final Element sponsors = createSponsors(connection, document, tournament, templateContext);
    documentBodyStart.appendChild(sponsors);
    sponsors.setAttribute("space-after", BLOCK_SPACING);

    final Element volunteers = createVolunteers(connection, tournament, document, templateContext);
    documentBodyStart.appendChild(volunteers);
    volunteers.setAttribute("page-break-after", "always");

    addAwards(description, connection, document, tournament, templateContext, pageMasterName, rootElement);

    final Element pageSequenceEnd = FOPUtils.createPageSequence(document, pageMasterName);
    rootElement.appendChild(pageSequenceEnd);

    final Element headerEnd = createHeader(document, description, tournament, null, null);
    pageSequenceEnd.appendChild(headerEnd);

    final Element footerEnd = createFooter(document, null);
    pageSequenceEnd.appendChild(footerEnd);

    final Element documentBodyEnd = FOPUtils.createBody(document);
    pageSequenceEnd.appendChild(documentBodyEnd);

    final Element endAwards = createSectionBlock(connection, document, tournament, templateContext,
                                                 AwardsScript.Section.END_AWARDS);
    documentBodyEnd.appendChild(endAwards);

    final Element advancing = createAdvancingTeams(connection, document, tournament);
    documentBodyEnd.appendChild(advancing);

    final Element footerSection = createSectionBlock(connection, document, tournament, templateContext,
                                                     AwardsScript.Section.FOOTER);
    documentBodyEnd.appendChild(footerSection);

    return document;
  }

  /**
   * @param beforeText used to specify the award that this is before
   */
  private Element createFooter(final Document document,
                               final @Nullable String beforeText) {
    final Element staticContent = FOPUtils.createXslFoElement(document, FOPUtils.STATIC_CONTENT_TAG);
    staticContent.setAttribute("flow-name", "xsl-region-after");
    staticContent.setAttribute("font-size", "10pt");

    final Element table = FOPUtils.createBasicTable(document);
    staticContent.appendChild(table);

    table.appendChild(FOPUtils.createTableColumn(document, 1));
    table.appendChild(FOPUtils.createTableColumn(document, 1));
    table.appendChild(FOPUtils.createTableColumn(document, 1));

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    table.appendChild(tableBody);

    final Element row = FOPUtils.createTableRow(document);
    tableBody.appendChild(row);

    final Element leftCell = FOPUtils.createTableCell(document, null, "");
    row.appendChild(leftCell);

    final Element centerCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, "");
    row.appendChild(centerCell);
    final Element block = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    block.setAttribute(FOPUtils.TEXT_ALIGN_ATTRIBUTE, FOPUtils.TEXT_ALIGN_CENTER);
    block.appendChild(document.createTextNode("Page "));
    block.appendChild(FOPUtils.createXslFoElement(document, "page-number"));
    block.appendChild(document.createTextNode(" of "));
    final Element lastPage = FOPUtils.createXslFoElement(document, "page-number-citation-last");
    lastPage.setAttribute("ref-id", FOPUtils.PAGE_SEQUENCE_NAME);
    block.appendChild(lastPage);
    centerCell.appendChild(block);

    final Element rightCell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, null == beforeText ? ""
        : String.format("Before %s", beforeText));
    row.appendChild(rightCell);

    return staticContent;
  }

  private Element createAdvancingTeams(final Connection connection,
                                       final Document document,
                                       final Tournament tournament)
      throws SQLException {
    final List<String> sortedAwardGroups = AwardsReportSortedGroupsServlet.getAwardGroupsSorted(connection,
                                                                                                tournament.getTournamentID());

    final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    final List<AdvancingTeam> advancing = AdvancingTeam.loadAdvancingTeams(connection, tournament.getTournamentID());
    if (!advancing.isEmpty()) {
      final Element advancingElement = AwardsReport.addAdvancingTeams(advancing, connection, document, tournament,
                                                                      sortedAwardGroups);
      container.appendChild(advancingElement);
    } else {
      final Element emptyBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      container.appendChild(emptyBlock);
      emptyBlock.appendChild(document.createTextNode("No advancing teams"));
    }

    return container;
  }

  /**
   * Get the award groups in order, this does not contain groups created for
   * advancing teams.
   *
   * @param connection database connection
   * @param tournament the tournament to get the order or
   * @return unmodifiable list of the award groups in sorted order
   */
  public static List<String> getAwardGroupOrder(final Connection connection,
                                                final Tournament tournament)
      throws SQLException {
    final Collection<String> allAwardGroups = Queries.getAwardGroups(connection, tournament.getTournamentID());
    final List<String> sortedAwardGroups = AwardsReportSortedGroupsServlet.getAwardGroupsSorted(connection,
                                                                                                tournament.getTournamentID());

    // ensure all award groups are in the list
    final List<String> localSortedGroups = new LinkedList<>(sortedAwardGroups);
    allAwardGroups.stream() //
                  .filter(e -> !localSortedGroups.contains(e))//
                  .forEach(localSortedGroups::add);

    // remove award groups not in the standard award groups
    return localSortedGroups.stream() //
                            .filter(allAwardGroups::contains) //
                            .collect(Collectors.toList());
  }

  private List<AwardCategory> filterAwardOrder(final Connection connection,
                                               final Tournament tournament,
                                               final List<AwardCategory> fullAwardOrder)
      throws SQLException {

    final List<AwardCategory> filteredAwardOrder = new LinkedList<>();
    for (final AwardCategory category : fullAwardOrder) {
      LOGGER.trace("filterAwardOrder: Processing category {}", category.getTitle());

      if (category instanceof NonNumericCategory
          && CategoriesIgnored.isNonNumericCategoryIgnored(connection, tournament.getLevel(),
                                                           (NonNumericCategory) category)) {
        LOGGER.debug("Ignoring category {}", category.getTitle());
        continue;
      } else if (category instanceof HeadToHeadCategory) {
        if (TournamentParameters.getRunningHeadToHead(connection, tournament.getTournamentID())) {
          filteredAwardOrder.add(category);
        }
      } else {
        filteredAwardOrder.add(category);
      }
    }

    return filteredAwardOrder;
  }

  @SuppressFBWarnings(value = { "DLS_DEAD_LOCAL_STORE" }, justification = "Switch statement requires storing of variable")
  private void addAwards(final ChallengeDescription description,
                         final Connection connection,
                         final Document document,
                         final Tournament tournament,
                         final VelocityContext templateContext,
                         final String pageMasterName,
                         final Element rootElement)
      throws SQLException {
    final List<String> awardGroupOrder = getAwardGroupOrder(connection, tournament);
    final List<AwardCategory> fullAwardOrder = AwardsScript.getAwardOrder(description, connection, tournament);
    final List<AwardCategory> awardOrder = filterAwardOrder(connection, tournament, fullAwardOrder);

    final Map<String, FinalistSchedule> finalistSchedulesPerAwardGroup = FinalistSchedule.loadSchedules(connection,
                                                                                                        tournament);

    final List<AwardWinner> nonNumericPerAwardGroupWinners = AwardWinners.getNonNumericAwardWinners(connection,
                                                                                                    tournament.getTournamentID())
                                                                         .stream() //
                                                                         .filter(Errors.rethrow()
                                                                                       .wrapPredicate(w -> AwardsReport.isNonNumericAwarded(connection,
                                                                                                                                            description,
                                                                                                                                            tournament.getLevel(),
                                                                                                                                            w))) //
                                                                         .collect(Collectors.toList());
    final Map<String, Map<String, List<AwardWinner>>> organizedNonNumericPerAwardGroupWinners = AwardsReport.organizeAwardWinners(nonNumericPerAwardGroupWinners);

    final Map<String, List<OverallAwardWinner>> nonNumericOverallWinners = AwardsReport.getNonNumericOverallWinners(description,
                                                                                                                    connection,
                                                                                                                    tournament);

    final List<AwardWinner> subjectiveWinners = AwardWinners.getSubjectiveAwardWinners(connection,
                                                                                       tournament.getTournamentID());
    final Map<String, Map<String, List<AwardWinner>>> organizedSubjectiveWinners = AwardsReport.organizeAwardWinners(subjectiveWinners);

    final List<AwardWinner> virtualSubjectiveWinners = AwardWinners.getVirtualSubjectiveAwardWinners(connection,
                                                                                                     tournament.getTournamentID());
    final Map<String, Map<String, List<AwardWinner>>> organizedVirtualSubjectiveWinners = AwardsReport.organizeAwardWinners(virtualSubjectiveWinners);

    @Nullable
    AwardCategory prevCategory = null;
    final ListIterator<AwardCategory> iter = awardOrder.listIterator();
    while (iter.hasNext()) {
      final AwardCategory category = iter.next();

      LOGGER.trace("addAwards: Processing category {}", category.getTitle());

      if (category instanceof NonNumericCategory
          && CategoriesIgnored.isNonNumericCategoryIgnored(connection, tournament.getLevel(),
                                                           (NonNumericCategory) category)) {
        throw new FLLInternalException("Should have filtered this ignored non-numberic category out: "
            + category.getTitle());
      }

      final Element categoryPage;
      switch (category) {
      case PerformanceScoreCategory awardCategory -> {
        categoryPage = createPerformanceCategory(description, connection, tournament, document, templateContext,
                                                 awardGroupOrder, (PerformanceScoreCategory) category);
      }
      case NonNumericCategory awardCategory -> {
        if (category.getPerAwardGroup()) {
          categoryPage = createNonNumericOrSubjectiveCategory(connection, tournament, description, document,
                                                              templateContext, awardGroupOrder, awardCategory,
                                                              organizedNonNumericPerAwardGroupWinners,
                                                              finalistSchedulesPerAwardGroup);
        } else {
          categoryPage = createNonNumericOverallCategory(connection, description, tournament, document, templateContext,
                                                         awardCategory, nonNumericOverallWinners,
                                                         finalistSchedulesPerAwardGroup);
        }
      }
      case SubjectiveScoreCategory awardCategory -> {
        categoryPage = createNonNumericOrSubjectiveCategory(connection, tournament, description, document,
                                                            templateContext, awardGroupOrder,
                                                            (SubjectiveScoreCategory) category,
                                                            organizedSubjectiveWinners, finalistSchedulesPerAwardGroup);
      }
      case VirtualSubjectiveScoreCategory awardCategory -> {
        categoryPage = createNonNumericOrSubjectiveCategory(connection, tournament, description, document,
                                                            templateContext, awardGroupOrder,
                                                            (VirtualSubjectiveScoreCategory) category,
                                                            organizedVirtualSubjectiveWinners,
                                                            finalistSchedulesPerAwardGroup);
      }
      case ChampionshipCategory awardCategory -> {
        categoryPage = createNonNumericOrSubjectiveCategory(connection, tournament, description, document,
                                                            templateContext, awardGroupOrder, category,
                                                            organizedNonNumericPerAwardGroupWinners,
                                                            finalistSchedulesPerAwardGroup);
      }
      case HeadToHeadCategory awardCategory -> {
        if (TournamentParameters.getRunningHeadToHead(connection, tournament.getTournamentID())) {
          categoryPage = createHeadToHead(connection, tournament, description, document, templateContext,
                                          awardGroupOrder, category);
        } else {
          throw new FLLInternalException("Should have filtered out head to head category when not enabled in this tournament");
        }
      }
      default -> {
        categoryPage = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
        categoryPage.appendChild(document.createTextNode(String.format("Category %s is of an unknown type: %s",
                                                                       category.getTitle(),
                                                                       category.getClass().getName())));
      }
      }

      if (null != categoryPage) {
        final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
        rootElement.appendChild(pageSequence);

        final String presenter = getCategoryPresenter(connection, tournament, category);

        final Element header = createHeader(document, description, tournament,
                                            null == prevCategory ? null : prevCategory.getTitle(), presenter);
        pageSequence.appendChild(header);

        final @Nullable String beforeText;
        if (iter.hasNext()) {
          final AwardCategory nextCategory = iter.next();
          beforeText = nextCategory.getTitle();

          // move back
          iter.previous();
        } else {
          beforeText = null;
        }

        final Element footer = createFooter(document, beforeText);
        pageSequence.appendChild(footer);

        final Element documentBody = FOPUtils.createBody(document);
        pageSequence.appendChild(documentBody);

        documentBody.appendChild(categoryPage);
        categoryPage.setAttribute("page-break-after", "always");

        prevCategory = category;
      }
    }
  }

  private String getCategoryDescription(final Connection connection,
                                        final Tournament tournament,
                                        final VelocityContext templateContext,
                                        final AwardCategory category)
      throws SQLException {
    final String rawText;
    if (category instanceof SubjectiveScoreCategory) {
      rawText = AwardsScript.getCategoryText(connection, tournament, (SubjectiveScoreCategory) category);
    } else if (category instanceof VirtualSubjectiveScoreCategory) {
      rawText = AwardsScript.getCategoryText(connection, tournament, (VirtualSubjectiveScoreCategory) category);
    } else if (category instanceof NonNumericCategory) {
      rawText = AwardsScript.getCategoryText(connection, tournament, (NonNumericCategory) category);
    } else if (category instanceof ChampionshipCategory) {
      rawText = AwardsScript.getSectionText(connection, tournament, AwardsScript.Section.CATEGORY_CHAMPIONSHIP);
    } else if (category instanceof HeadToHeadCategory) {
      rawText = AwardsScript.getSectionText(connection, tournament, AwardsScript.Section.CATEGORY_HEAD2HEAD);
    } else if (category instanceof PerformanceScoreCategory) {
      rawText = AwardsScript.getSectionText(connection, tournament, AwardsScript.Section.CATEGORY_PERFORMANCE);
    } else {
      rawText = "Unknown Category";
    }

    try (StringWriter writer = new StringWriter()) {
      if (!Velocity.evaluate(templateContext, writer, category.getTitle(), rawText)) {
        throw new FLLRuntimeException(String.format("Error evaluating template for category %s", category.getTitle()));
      }
      return writer.toString();
    } catch (final IOException e) {
      throw new FLLInternalException("Should not get IO exception writing to a string", e);
    }
  }

  private String getCategoryPresenter(final Connection connection,
                                      final Tournament tournament,
                                      final AwardCategory category)
      throws SQLException {
    if (category instanceof SubjectiveScoreCategory) {
      return AwardsScript.getPresenter(connection, tournament, (SubjectiveScoreCategory) category);
    } else if (category instanceof VirtualSubjectiveScoreCategory) {
      return AwardsScript.getPresenter(connection, tournament, (VirtualSubjectiveScoreCategory) category);
    } else if (category instanceof NonNumericCategory) {
      return AwardsScript.getPresenter(connection, tournament, (NonNumericCategory) category);
    } else if (category instanceof ChampionshipCategory) {
      return AwardsScript.getSectionText(connection, tournament, AwardsScript.Section.CATEGORY_CHAMPIONSHIP_PRESENTER);
    } else if (category instanceof HeadToHeadCategory) {
      return AwardsScript.getSectionText(connection, tournament, AwardsScript.Section.CATEGORY_HEAD2HEAD_PRESENTER);
    } else if (category instanceof PerformanceScoreCategory) {
      return AwardsScript.getSectionText(connection, tournament, AwardsScript.Section.CATEGORY_PERFORMANCE_PRESENTER);
    } else {
      return "Unknown Category";
    }
  }

  private static final int AWARD_PLACE_WIDTH = 1;

  private static final int AWARD_WINNER_WIDTH = 4;

  private static final String CATEGORY_TITLE_FONT_SIZE = "16pt";

  private static final String AWARD_GROUP_FONT_SIZE = "14pt";

  private static final String BLOCK_SPACING = "0.25in";

  private Element createPresenter(final Document document,
                                  final Connection connection,
                                  final Tournament tournament,
                                  final AwardCategory category)
      throws SQLException {
    final Element categoryPresenter = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    final String presenterText = String.format("Presented By: %s",
                                               getCategoryPresenter(connection, tournament, category));
    categoryPresenter.appendChild(document.createTextNode(presenterText));
    return categoryPresenter;
  }

  private Element createCategoryTitle(final Document document,
                                      final AwardCategory category) {
    final Element categoryTitle = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    categoryTitle.appendChild(document.createTextNode(String.format("%s Award", category.getTitle())));
    categoryTitle.setAttribute("text-align", FOPUtils.TEXT_ALIGN_CENTER);
    categoryTitle.setAttribute("font-weight", "bold");
    categoryTitle.setAttribute("font-size", CATEGORY_TITLE_FONT_SIZE);

    return categoryTitle;
  }

  private Element createPerformanceCategory(final ChallengeDescription description,
                                            final Connection connection,
                                            final Tournament tournament,
                                            final Document document,
                                            final VelocityContext templateContext,
                                            final List<String> awardGroupOrder,
                                            final PerformanceScoreCategory category)
      throws SQLException {

    final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    final Element categoryTitle = createCategoryTitle(document, category);
    container.appendChild(categoryTitle);

    final Element categoryDescription = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(categoryDescription);
    final String rawDescription = getCategoryDescription(connection, tournament, templateContext, category);
    FOPUtils.appendTextAsParagraphs(document, rawDescription, categoryDescription, true);

    final Element categoryPresenter = createPresenter(document, connection, tournament, category);
    container.appendChild(categoryPresenter);

    final Map<String, List<Top10.ScoreEntry>> scores = Top10.getTableAsMapByAwardGroup(connection, description, true,
                                                                                       false);

    final int numAwards = AwardsScript.getNumPerformanceAwards(connection, tournament);

    for (final String awardGroup : awardGroupOrder) {
      if (scores.containsKey(awardGroup)) {
        final List<Top10.ScoreEntry> rawScoresforAwardGroup = scores.get(awardGroup);

        final Element awardGroupBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
        container.appendChild(awardGroupBlock);
        awardGroupBlock.appendChild(document.createTextNode(String.format("Award Group: %s", awardGroup)));
        awardGroupBlock.setAttribute("font-weight", "bold");
        awardGroupBlock.setAttribute("text-align", FOPUtils.TEXT_ALIGN_CENTER);

        final Map<Integer, ImmutablePair<Integer, Double>> teamPerformanceRanks = FinalComputedScores.gatherRankedPerformanceTeams(connection,
                                                                                                                                   description.getWinner(),
                                                                                                                                   tournament,
                                                                                                                                   awardGroup);

        // place -> {teams}
        final NavigableMap<Integer, List<Integer>> placesToTeams = new TreeMap<>();
        teamPerformanceRanks.entrySet().stream() //
                            .forEach(e -> {
                              placesToTeams.computeIfAbsent(e.getValue().getLeft(), k -> new LinkedList<>())
                                           .add(e.getKey());
                            });

        final LinkedList<Map.Entry<@KeyFor("placesToTeams") Integer, List<Integer>>> winners = placesToTeams.entrySet()
                                                                                                            .stream()
                                                                                                            .limit(numAwards)
                                                                                                            .collect(Collectors.toCollection(LinkedList::new));

        winners.descendingIterator().forEachRemaining(e -> {
          final @KeyFor("placesToTeams") Integer place = e.getKey();
          final List<Integer> teamsInPlace = e.getValue();
          if (teamsInPlace.isEmpty()) {
            throw new FLLInternalException("No teams in place "
                + place);
          }

          final Integer firstTeamNumber = teamsInPlace.get(0);
          final Optional<Top10.ScoreEntry> firstTeamEntry = rawScoresforAwardGroup.stream() //
                                                                                  .filter(x -> x.getTeamNumber() == firstTeamNumber.intValue()) //
                                                                                  .findAny();

          if (!firstTeamEntry.isPresent()) {
            throw new FLLRuntimeException("Unable to find the performance score for team "
                + firstTeamNumber);
          }

          final String formattedScore = firstTeamEntry.get().getFormattedScore();

          final Element table = FOPUtils.createBasicTable(document);
          container.appendChild(table);

          table.appendChild(FOPUtils.createTableColumn(document, AWARD_PLACE_WIDTH));
          table.appendChild(FOPUtils.createTableColumn(document, AWARD_WINNER_WIDTH));

          final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
          table.appendChild(tableBody);

          final Element row = FOPUtils.createTableRow(document);
          tableBody.appendChild(row);

          final boolean tie = teamsInPlace.size() > 1;
          final String placeTitle = String.format("%d%s Place Winner%s", place, suffixForPlace(place), tie ? "s" : "");

          final Element placeCell = FOPUtils.createTableCell(document, null, placeTitle);
          row.appendChild(placeCell);
          placeCell.setAttribute("background-color", "yellow");

          final Element teamCell = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);
          row.appendChild(teamCell);

          final Element teamPlaceBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
          teamCell.appendChild(teamPlaceBlock);
          teamPlaceBlock.appendChild(document.createTextNode(String.format("With a score of %s:", formattedScore)));

          teamsInPlace.stream().forEach(Errors.rethrow().wrap(teamNumber -> {
            final Team team = Team.getTeamFromDatabase(connection, teamNumber);
            addTeamToCell(document, teamCell, team);
          }));

        });

      }
    }

    return container;
  }

  private static List<Integer> getFinalistsForCategory(final Map<String, FinalistSchedule> finalistSchedulesPerAwardGroup,
                                                       final String categoryTitle) {
    return finalistSchedulesPerAwardGroup.entrySet().stream() //
                                         .map(Map.Entry::getValue) //
                                         .map(FinalistSchedule::getSchedule) //
                                         .flatMap(Collection::stream) //
                                         .map(FinalistDBRow::getCategories) //
                                         .<@Nullable Integer> map(map -> map.get(categoryTitle)) //
                                         .filter(Objects::nonNull) //
                                         .sorted() //
                                         .collect(Collectors.toList());
  }

  private static List<Integer> getFinalistsForCategory(final Map<String, FinalistSchedule> finalistSchedulesPerAwardGroup,
                                                       final String awardGroup,
                                                       final String categoryTitle) {
    final FinalistSchedule schedule = finalistSchedulesPerAwardGroup.get(awardGroup);
    if (null != schedule) {
      return schedule.getSchedule().stream() //
                     .map(FinalistDBRow::getCategories) //
                     .<@Nullable Integer> map(map -> map.get(categoryTitle)) //
                     .filter(Objects::nonNull) //
                     .sorted() //
                     .collect(Collectors.toList());
    } else {
      return Collections.emptyList();
    }
  }

  private Element createHeadToHead(final Connection connection,
                                   final Tournament tournament,
                                   final ChallengeDescription challengeDescription,
                                   final Document document,
                                   final VelocityContext templateContext,
                                   final List<String> awardGroupOrder,
                                   final AwardCategory category)
      throws DOMException, SQLException {

    final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    final Element categoryTitle = createCategoryTitle(document, category);
    container.appendChild(categoryTitle);

    final Element categoryDescription = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(categoryDescription);
    final String rawDescription = getCategoryDescription(connection, tournament, templateContext, category);
    FOPUtils.appendTextAsParagraphs(document, rawDescription, categoryDescription, true);

    final Element categoryPresenter = createPresenter(document, connection, tournament, category);
    container.appendChild(categoryPresenter);

    PlayoffReport.populateBody(connection, tournament, challengeDescription, document, container, awardGroupOrder);

    return container;
  }

  private Element createNonNumericOrSubjectiveCategory(final Connection connection,
                                                       final Tournament tournament,
                                                       final ChallengeDescription description,
                                                       final Document document,
                                                       final VelocityContext templateContext,
                                                       final List<String> awardGroupOrder,
                                                       final AwardCategory category,
                                                       final Map<String, Map<String, List<AwardWinner>>> winners,
                                                       final Map<String, FinalistSchedule> finalistSchedulesPerAwardGroup)
      throws SQLException {
    final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    final Element categoryTitle = createCategoryTitle(document, category);
    container.appendChild(categoryTitle);

    final Element categoryDescription = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(categoryDescription);
    final String rawDescription = getCategoryDescription(connection, tournament, templateContext, category);
    FOPUtils.appendTextAsParagraphs(document, rawDescription, categoryDescription, true);

    final Element categoryPresenter = createPresenter(document, connection, tournament, category);
    container.appendChild(categoryPresenter);

    // award group -> [winner]
    final Map<String, List<AwardWinner>> categoryWinners = winners.getOrDefault(category.getTitle(),
                                                                                Collections.emptyMap());
    if (!categoryWinners.isEmpty()) {
      for (final String awardGroup : awardGroupOrder) {
        final List<AwardWinner> agWinners = categoryWinners.getOrDefault(awardGroup, Collections.emptyList());
        if (!agWinners.isEmpty()) {
          Collections.sort(agWinners);

          final Element awardGroupTitle = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
          container.appendChild(awardGroupTitle);
          awardGroupTitle.setAttribute("text-align", FOPUtils.TEXT_ALIGN_CENTER);
          awardGroupTitle.setAttribute("font-weight", "bold");
          awardGroupTitle.setAttribute("font-size", AWARD_GROUP_FONT_SIZE);

          awardGroupTitle.appendChild(document.createTextNode(String.format("Award Group: %s", awardGroup)));

          final List<Integer> finalists = getFinalistsForCategory(finalistSchedulesPerAwardGroup, awardGroup,
                                                                  category.getTitle());
          if (!finalists.isEmpty()) {
            final Element finalistContainer = createFinalistsContainer(connection, document, finalists);
            container.appendChild(finalistContainer);
            finalistContainer.setAttribute("space-after", BLOCK_SPACING);
          }

          final boolean displayPlace = AwardsReport.displayPlace(description, category.getTitle());

          outputWinners(connection, displayPlace, document, container, agWinners);

        } else {
          final Element emptyBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
          container.appendChild(emptyBlock);
          emptyBlock.appendChild(document.createTextNode(String.format("No winners for %s in award group %s",
                                                                       category.getTitle(), awardGroup)));
        }
      } // foreach award group

    } else {
      final Element emptyBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      container.appendChild(emptyBlock);
      emptyBlock.appendChild(document.createTextNode(String.format("No winners for %s in any award group",
                                                                   category.getTitle())));
    }

    return container;

  }

  private static void outputWinners(final Connection connection,
                                    final boolean displayPlace,
                                    final Document document,
                                    final Element container,
                                    final List<? extends OverallAwardWinner> winners) {
    // place -> {teams}
    final NavigableMap<Integer, List<OverallAwardWinner>> placesToWinners = new TreeMap<>();
    winners.stream() //
           .forEach(w -> {
             placesToWinners.computeIfAbsent(w.getPlace(), k -> new LinkedList<>()).add(w);
           });

    placesToWinners.descendingMap().entrySet().forEach(e -> {
      final @KeyFor("placesToWinners.descendingMap()") Integer place = e.getKey();
      final List<OverallAwardWinner> winnersInPlace = e.getValue();
      if (winnersInPlace.isEmpty()) {
        throw new FLLInternalException("No teams in place "
            + place);
      }

      final Element table = FOPUtils.createBasicTable(document);
      container.appendChild(table);

      table.appendChild(FOPUtils.createTableColumn(document, AWARD_PLACE_WIDTH));
      table.appendChild(FOPUtils.createTableColumn(document, AWARD_WINNER_WIDTH));

      final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
      table.appendChild(tableBody);

      final Element row = FOPUtils.createTableRow(document);
      tableBody.appendChild(row);

      final boolean tie = winnersInPlace.size() > 1;
      final String placeTitle;
      if (displayPlace) {
        placeTitle = String.format("%d%s Place Winner%s", place, suffixForPlace(place), tie ? "s" : "");
      } else {
        placeTitle = String.format("Winner%s", tie ? "s" : "");
      }

      final Element placeCell = FOPUtils.createTableCell(document, null, placeTitle);
      row.appendChild(placeCell);
      placeCell.setAttribute("background-color", "yellow");

      final Element teamCell = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);
      row.appendChild(teamCell);

      winnersInPlace.stream().forEach(Errors.rethrow().wrap(winner -> {
        final Team team = Team.getTeamFromDatabase(connection, winner.getTeamNumber());

        final @Nullable String description = winner.getDescription();
        if (null != description) {
          final Element descriptionBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
          teamCell.appendChild(descriptionBlock);
          descriptionBlock.appendChild(document.createTextNode(String.format("Description: %s", description)));
        }

        addTeamToCell(document, teamCell, team);
      }));

    });
  }

  private Element createNonNumericOverallCategory(final Connection connection,
                                                  ChallengeDescription description,
                                                  final Tournament tournament,
                                                  final Document document,
                                                  final VelocityContext templateContext,
                                                  final NonNumericCategory category,
                                                  final Map<String, List<OverallAwardWinner>> winners,
                                                  final Map<String, FinalistSchedule> finalistSchedulesPerAwardGroup)
      throws SQLException {
    final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    final List<OverallAwardWinner> categoryWinners = winners.getOrDefault(category.getTitle(), Collections.emptyList());
    if (!categoryWinners.isEmpty()) {
      Collections.sort(categoryWinners);

      final Element categoryTitle = createCategoryTitle(document, category);
      container.appendChild(categoryTitle);

      final Element categoryDescription = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      container.appendChild(categoryDescription);
      final String rawDescription = getCategoryDescription(connection, tournament, templateContext, category);
      FOPUtils.appendTextAsParagraphs(document, rawDescription, categoryDescription, true);

      final Element categoryPresenter = createPresenter(document, connection, tournament, category);
      container.appendChild(categoryPresenter);

      final List<Integer> finalists = getFinalistsForCategory(finalistSchedulesPerAwardGroup, category.getTitle());
      if (!finalists.isEmpty()) {
        final Element finalistContainer = createFinalistsContainer(connection, document, finalists);
        container.appendChild(finalistContainer);
        finalistContainer.setAttribute("space-after", BLOCK_SPACING);
      }

      final boolean displayPlace = AwardsReport.displayPlace(description, category.getTitle());

      outputWinners(connection, displayPlace, document, container, categoryWinners);

    } else {
      final Element emptyBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      container.appendChild(emptyBlock);
      emptyBlock.appendChild(document.createTextNode(String.format("No winners for %s", category.getTitle())));
    }

    return container;
  }

  private Element createFinalistsContainer(final Connection connection,
                                           Document document,
                                           final List<Integer> finalists) {
    final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    final Element table = FOPUtils.createBasicTable(document);
    container.appendChild(table);

    table.appendChild(FOPUtils.createTableColumn(document, AWARD_PLACE_WIDTH));
    table.appendChild(FOPUtils.createTableColumn(document, AWARD_WINNER_WIDTH));

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    table.appendChild(tableBody);

    final Element row = FOPUtils.createTableRow(document);
    tableBody.appendChild(row);

    final Element placeCell = FOPUtils.createTableCell(document, null, "The finalists are:");
    row.appendChild(placeCell);
    placeCell.setAttribute("background-color", "yellow");

    final Element teamCell = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);
    row.appendChild(teamCell);

    finalists.stream().forEach(Errors.rethrow().wrap(teamNumber -> {
      final Team team = Team.getTeamFromDatabase(connection, teamNumber);
      addTeamToCell(document, teamCell, team);
    }));

    return container;
  }

  private static void addTeamToCell(final Document document,
                                    final Element teamCell,
                                    final Team team) {
    final Element teamContainer = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    teamCell.appendChild(teamContainer);

    final Element teamNumberBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    teamContainer.appendChild(teamNumberBlock);
    teamNumberBlock.appendChild(document.createTextNode(String.format("Number: %d", team.getTeamNumber())));

    final Element teamNameBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    teamContainer.appendChild(teamNameBlock);
    teamNameBlock.appendChild(document.createTextNode(String.format("Name: %s", team.getTeamName())));

    final Element teamOrganizationBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    teamContainer.appendChild(teamOrganizationBlock);
    final String organization = team.getOrganization();
    teamOrganizationBlock.appendChild(document.createTextNode(String.format("Organization: %s",
                                                                            null == organization ? "" : organization)));

    final Element blank = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    teamContainer.appendChild(blank);
    blank.appendChild(document.createTextNode(Utilities.NON_BREAKING_SPACE_STRING));
  }

  private static String suffixForPlace(final int place) {
    final int last2Digits = place
        % 100;
    final int lastDigit = place
        % 10;
    if (place >= 10
        && last2Digits >= 10
        && last2Digits <= 20) {
      return "th";
    } else {
      switch (lastDigit) {
      case 1:
        return "st";
      case 2:
        return "nd";
      case 3:
        return "rd";
      default:
        return "th";
      }
    }
  }

  private Element createSponsors(final Connection connection,
                                 final Document document,
                                 final Tournament tournament,
                                 final VelocityContext templateContext)
      throws SQLException {
    final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    try (StringWriter writer = new StringWriter()) {
      final Element startBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      container.appendChild(startBlock);

      final String rawText = AwardsScript.getSectionText(connection, tournament, AwardsScript.Section.SPONSORS_INTRO);

      if (!Velocity.evaluate(templateContext, writer, AwardsScript.Section.FRONT_MATTER.name(), rawText)) {
        throw new FLLRuntimeException(String.format("Error evaluating template for section %s",
                                                    AwardsScript.Section.SPONSORS_INTRO));
      }
      startBlock.appendChild(document.createTextNode(writer.toString()));
    } catch (final IOException e) {
      throw new FLLInternalException("Should not get IO exception writing to a string", e);
    }

    final Element listBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(listBlock);

    final List<String> sponsors = AwardsScript.getSponsors(connection, tournament);
    if (!sponsors.isEmpty()) {
      final Element list = FOPUtils.createXslFoElement(document, "list-block");
      listBlock.appendChild(list);

      sponsors.forEach(s -> {
        final Element listItem = FOPUtils.createXslFoElement(document, "list-item");
        list.appendChild(listItem);
        listItem.setAttribute("keep-together.within-page", "always");

        final Element itemLabel = FOPUtils.createSimpleListItemLabel(document);
        listItem.appendChild(itemLabel);
        itemLabel.setAttribute("end-indent", "label-end()");
        itemLabel.setAttribute("start-indent", "6pt");

        final Element itemBody = FOPUtils.createXslFoElement(document, "list-item-body");
        listItem.appendChild(itemBody);
        itemBody.setAttribute("start-indent", "body-start()");

        final Element bodyBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
        itemBody.appendChild(bodyBlock);
        bodyBlock.appendChild(document.createTextNode(s));
      });
    } else {
      listBlock.appendChild(document.createTextNode("No sponsors specified"));
      listBlock.setAttribute("font-style", "italic");
    }

    try (StringWriter writer = new StringWriter()) {
      final Element endBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      container.appendChild(endBlock);

      final String rawText = AwardsScript.getSectionText(connection, tournament,
                                                         AwardsScript.Section.SPONSORS_RECOGNITION);

      if (!Velocity.evaluate(templateContext, writer, AwardsScript.Section.FRONT_MATTER.name(), rawText)) {
        throw new FLLRuntimeException(String.format("Error evaluating template for section %s",
                                                    AwardsScript.Section.SPONSORS_RECOGNITION));
      }
      endBlock.appendChild(document.createTextNode(writer.toString()));
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

    titleBuilder.format(" Awards Ceremony");
    return titleBuilder.toString();
  }

  /**
   * @param afterText used to specify the award that this is after, may be null
   * @param presenter who is presenting the award, may be null
   */
  private Element createHeader(final Document document,
                               final ChallengeDescription description,
                               final Tournament tournament,
                               final @Nullable String afterText,
                               final @Nullable String presenter) {
    final Element staticContent = FOPUtils.createXslFoElement(document, FOPUtils.STATIC_CONTENT_TAG);
    staticContent.setAttribute("flow-name", "xsl-region-before");
    staticContent.setAttribute("font-size", "10pt");

    if (null != afterText
        || !StringUtils.isBlank(presenter)) {
      final Element afterPresenterBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      staticContent.appendChild(afterPresenterBlock);
      afterPresenterBlock.setAttribute(FOPUtils.TEXT_ALIGN_ATTRIBUTE, FOPUtils.TEXT_ALIGN_LEFT);

      if (null != afterText) {
        afterPresenterBlock.appendChild(document.createTextNode(String.format("After %s", afterText)));
      }

      if (!StringUtils.isBlank(presenter)) {
        afterPresenterBlock.appendChild(document.createTextNode(String.format(" Presented By: %s", presenter)));
      }
    }

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

  private Element createVolunteers(final Connection connection,
                                   final Tournament tournament,
                                   final Document document,
                                   final VelocityContext templateContext)
      throws SQLException {
    return createSectionBlock(connection, document, tournament, templateContext, AwardsScript.Section.VOLUNTEERS);
  }

  private Element createSectionBlock(final Connection connection,
                                     final Document document,
                                     final Tournament tournament,
                                     final VelocityContext templateContext,
                                     final AwardsScript.Section section)
      throws SQLException {
    final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    final String rawText = AwardsScript.getSectionText(connection, tournament, section);

    try (StringWriter writer = new StringWriter()) {
      if (!Velocity.evaluate(templateContext, writer, section.name(), rawText)) {
        throw new FLLRuntimeException(String.format("Error evaluating template for section %s", section));
      }
      final String text = writer.toString();
      FOPUtils.appendTextAsParagraphs(document, text, container, true);
    } catch (final IOException e) {
      throw new FLLInternalException("Should not get IO exception writing to a string", e);
    }

    return container;
  }

}
