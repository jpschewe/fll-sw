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
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import javax.sql.DataSource;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.diffplug.common.base.Errors;

import fll.Team;
import fll.Tournament;
import fll.TournamentLevel;
import fll.Utilities;
import fll.db.AwardsScript;
import fll.db.CategoriesIgnored;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.FOPUtils;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.api.AwardsReportSortedGroupsServlet;
import fll.web.report.AwardsReport;
import fll.web.report.FinalComputedScores;
import fll.web.report.PromptSummarizeScores;
import fll.web.report.finalist.FinalistSchedule;
import fll.web.scoreboard.Top10;
import fll.xml.Category;
import fll.xml.ChallengeDescription;
import fll.xml.ChampionshipCategory;
import fll.xml.NonNumericCategory;
import fll.xml.PerformanceScoreCategory;
import fll.xml.ScoreType;
import fll.xml.SubjectiveScoreCategory;
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

    if (PromptSummarizeScores.checkIfSummaryUpdated(response, application, session,
                                                    "/report/awards/AwardsScriptReport")) {
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

  private Document generateDocument(final ChallengeDescription description,
                                    final Connection connection)
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

    final Element intro = createSectionBlock(connection, tournament, document, templateContext,
                                             AwardsScript.Section.FRONT_MATTER);
    documentBody.appendChild(intro);
    intro.setAttribute("space-after", "0.25in");

    final Element sponsors = createSponsors(connection, tournament, document, templateContext);
    documentBody.appendChild(sponsors);
    sponsors.setAttribute("space-after", "0.25in");

    final Element volunteers = createVolunteers(connection, tournament, document, templateContext);
    documentBody.appendChild(volunteers);
    volunteers.setAttribute("page-break-after", "always");

    addAwards(description, connection, tournament, document, templateContext, documentBody);

    final Element endAwards = createSectionBlock(connection, tournament, document, templateContext,
                                                 AwardsScript.Section.END_AWARDS);
    documentBody.appendChild(endAwards);

    // FIXME advancing teams

    final Element footerSection = createSectionBlock(connection, tournament, document, templateContext,
                                                     AwardsScript.Section.FOOTER);
    documentBody.appendChild(footerSection);

    return document;
  }

  private List<String> getAwardGroupOrder(final Connection connection,
                                          final Tournament tournament)
      throws SQLException {
    final Collection<String> allAwardGroups = Queries.getAwardGroups(connection, tournament.getTournamentID());
    final List<String> sortedAwardGroups = AwardsReportSortedGroupsServlet.getAwardGroupsSorted(connection,
                                                                                                tournament.getTournamentID());

    // in case any of the award groups missing
    final List<String> localSortedGroups = new LinkedList<>(sortedAwardGroups);
    allAwardGroups.stream().filter(e -> !localSortedGroups.contains(e)).forEach(localSortedGroups::add);

    return localSortedGroups;
  }

  private void addAwards(final ChallengeDescription description,
                         final Connection connection,
                         final Tournament tournament,
                         final Document document,
                         final VelocityContext templateContext,
                         final Element documentBody)
      throws SQLException {
    final List<String> awardGroupOrder = getAwardGroupOrder(connection, tournament);
    final List<Category> awardOrder = AwardsScript.getAwardOrder(description, connection, tournament);

    final Map<String, FinalistSchedule> finalistSchedulesPerAwardGroup = FinalistSchedule.loadSchedules(connection,
                                                                                                        tournament);

    for (final Category category : awardOrder) {
      if (category instanceof NonNumericCategory
          && CategoriesIgnored.isNonNumericCategoryIgnored(connection, tournament.getLevel(),
                                                           (NonNumericCategory) category)) {
        continue;
      }
      final Element categoryPage;
      if (category instanceof PerformanceScoreCategory) {
        categoryPage = createPerformanceCategory(description, connection, tournament, document, templateContext,
                                                 awardGroupOrder, (PerformanceScoreCategory) category);
      } else {
        categoryPage = createCategory(connection, tournament, document, templateContext, awardGroupOrder, category);
      }
      documentBody.appendChild(categoryPage);
      categoryPage.setAttribute("page-break-after", "always");
    }
  }

  private String getCategoryDescription(final Connection connection,
                                        final Tournament tournament,
                                        final VelocityContext templateContext,
                                        final Category category)
      throws SQLException {
    final String rawText;
    if (category instanceof SubjectiveScoreCategory) {
      rawText = AwardsScript.getCategoryText(connection, tournament, (SubjectiveScoreCategory) category);
    } else if (category instanceof NonNumericCategory) {
      rawText = AwardsScript.getCategoryText(connection, tournament, (NonNumericCategory) category);
    } else if (category instanceof ChampionshipCategory) {
      rawText = AwardsScript.getSectionText(connection, tournament, AwardsScript.Section.CATEGORY_CHAMPIONSHIP);
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
                                      final Category category)
      throws SQLException {
    if (category instanceof SubjectiveScoreCategory) {
      return AwardsScript.getPresenter(connection, tournament, (SubjectiveScoreCategory) category);
    } else if (category instanceof NonNumericCategory) {
      return AwardsScript.getPresenter(connection, tournament, (NonNumericCategory) category);
    } else if (category instanceof ChampionshipCategory) {
      return AwardsScript.getSectionText(connection, tournament, AwardsScript.Section.CATEGORY_CHAMPIONSHIP_PRESENTER);
    } else if (category instanceof PerformanceScoreCategory) {
      return AwardsScript.getSectionText(connection, tournament, AwardsScript.Section.CATEGORY_PERFORMANCE_PRESENTER);
    } else {
      return "Unknown Category";
    }
  }

  private Element createCategory(final Connection connection,
                                 final Tournament tournament,
                                 final Document document,
                                 final VelocityContext templateContext,
                                 final List<String> awardGroupOrder,
                                 final Category category)
      throws SQLException {
    final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    final Element categoryTitle = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(categoryTitle);
    categoryTitle.appendChild(document.createTextNode(category.getTitle()));
    categoryTitle.setAttribute("text-align", FOPUtils.TEXT_ALIGN_CENTER);

    final Element categoryDescription = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(categoryDescription);
    categoryDescription.appendChild(document.createTextNode(getCategoryDescription(connection, tournament,
                                                                                   templateContext, category)));

    final Element categoryPresenter = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(categoryPresenter);
    final String presenterText = String.format("Presenter: %s", getCategoryPresenter(connection, tournament, category));
    categoryPresenter.appendChild(document.createTextNode(presenterText));

    if (category.getPerAwardGroup()) {
      for (final String awardGroup : awardGroupOrder) {
        // FIXME output award group

        // FIXME finalists
        // Finalists (yellow background) |
        // The finalists are:
        // team 1234 - name
        // ...

        // FIXME nth place ... 1st place

        /*
         * 1st place winner (span 4 rows, yellow background) | The winner is: \n team
         * number \n team name \n description
         * 
         */
      }
    } else {
      // FIXME
    }

    return container;
  }

  private static final int AWARD_PLACE_WIDTH = 1;

  private static final int AWARD_WINNER_WIDTH = 4;

  private static final String CATEGORY_TITLE_FONT_SIZE = "16pt";

  private Element createPerformanceCategory(final ChallengeDescription description,
                                            final Connection connection,
                                            final Tournament tournament,
                                            final Document document,
                                            final VelocityContext templateContext,
                                            final List<String> awardGroupOrder,
                                            final PerformanceScoreCategory category)
      throws SQLException {
    final ScoreType performanceScoreType = category.getScoreType();

    final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    final Element categoryTitle = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(categoryTitle);
    categoryTitle.appendChild(document.createTextNode(String.format("%s Award", category.getTitle())));
    categoryTitle.setAttribute("text-align", FOPUtils.TEXT_ALIGN_CENTER);
    categoryTitle.setAttribute("font-weight", "bold");
    categoryTitle.setAttribute("font-size", CATEGORY_TITLE_FONT_SIZE);

    final Element categoryDescription = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(categoryDescription);
    categoryDescription.appendChild(document.createTextNode(getCategoryDescription(connection, tournament,
                                                                                   templateContext, category)));

    final Element categoryPresenter = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(categoryPresenter);
    final String presenterText = String.format("Presenter: %s", getCategoryPresenter(connection, tournament, category));
    categoryPresenter.appendChild(document.createTextNode(presenterText));

    final Map<String, List<Top10.ScoreEntry>> scores = Top10.getTableAsMapByAwardGroup(connection, description);

    for (final String awardGroup : awardGroupOrder) {
      if (scores.containsKey(awardGroup)) {
        final Element awardGroupBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
        container.appendChild(awardGroupBlock);
        awardGroupBlock.appendChild(document.createTextNode(String.format("Award Group: %s", awardGroup)));
        awardGroupBlock.setAttribute("font-weight", "bold");

        final Map<Integer, ImmutablePair<Integer, Double>> teamPerformanceRanks = FinalComputedScores.gatherRankedPerformanceTeams(connection,
                                                                                                                                   description.getWinner(),
                                                                                                                                   tournament,
                                                                                                                                   awardGroup);

        // place -> {teams}
        final SortedMap<Integer, List<Integer>> placesToTeams = new TreeMap<>();
        teamPerformanceRanks.entrySet().stream() //
                            .forEach(e -> {
                              placesToTeams.computeIfAbsent(e.getValue().getLeft(), k -> new LinkedList<>())
                                           .add(e.getKey());
                            });

        // FIXME: make this a parameter that can be specified
        final int numAwards = 2;
        final Stream<Map.Entry<@KeyFor("placesToTeams") Integer, List<Integer>>> winners = placesToTeams.entrySet()
                                                                                                        .stream()
                                                                                                        .limit(numAwards);

        winners.forEach(e -> {
          final @KeyFor("placesToTeams") Integer place = e.getKey();
          final List<Integer> teamsInPlace = e.getValue();
          if (teamsInPlace.isEmpty()) {
            throw new FLLInternalException("No teams in place "
                + place);
          }

          final Integer firstTeamNumber = teamsInPlace.get(0);
          final ImmutablePair<Integer, Double> pair = teamPerformanceRanks.get(firstTeamNumber);
          if (null == pair) {
            throw new FLLInternalException("No score information for team "
                + firstTeamNumber);
          }
          final double score = pair.getRight();

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

          // TODO: make this cell yellow background?
          final Element placeCell = FOPUtils.createTableCell(document, null, placeTitle);
          row.appendChild(placeCell);

          final Element teamCell = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_CELL_TAG);
          row.appendChild(teamCell);

          final Element teamPlaceBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
          teamCell.appendChild(teamPlaceBlock);
          final String teamPlaceFormat;
          if (tie) {
            teamPlaceFormat = "The teams tied for %d%s place with a score of %s is:";
          } else {
            teamPlaceFormat = "The %d%s place team with a score of %s is:";
          }
          final String formattedScore = Utilities.getFormatForScoreType(performanceScoreType).format(score);

          teamPlaceBlock.appendChild(document.createTextNode(String.format(teamPlaceFormat, place,
                                                                           suffixForPlace(place), formattedScore)));

          teamsInPlace.stream().forEach(Errors.rethrow().wrap(teamNumber -> {
            final Team team = Team.getTeamFromDatabase(connection, teamNumber);

            final Element teamNumberBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
            teamCell.appendChild(teamNumberBlock);
            teamNumberBlock.appendChild(document.createTextNode(String.format("%d", teamNumber)));

            final Element teamNameBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
            teamCell.appendChild(teamNameBlock);
            teamNameBlock.appendChild(document.createTextNode(team.getTeamName()));

            final Element teamOrganizationBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
            teamCell.appendChild(teamOrganizationBlock);
            final String organization = team.getOrganization();
            teamOrganizationBlock.appendChild(document.createTextNode(null == organization ? "" : organization));
          }));

        });

      }
    }

    return container;
  }

  private static String suffixForPlace(final int place) {
    final int lastDigit = place
        % 10;
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

  private Element createSponsors(final Connection connection,
                                 final Tournament tournament,
                                 final Document document,
                                 final VelocityContext templateContext)
      throws SQLException {
    final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    final Element mainBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(mainBlock);

    try (StringWriter writer = new StringWriter()) {
      final String rawText = AwardsScript.getSectionText(connection, tournament, AwardsScript.Section.SPONSORS_INTRO);

      if (!Velocity.evaluate(templateContext, writer, AwardsScript.Section.FRONT_MATTER.name(), rawText)) {
        throw new FLLRuntimeException(String.format("Error evaluating template for section %s",
                                                    AwardsScript.Section.SPONSORS_INTRO));
      }
      mainBlock.appendChild(document.createTextNode(writer.toString()));
    } catch (final IOException e) {
      throw new FLLInternalException("Should not get IO exception writing to a string", e);
    }

    final List<String> sponsors = AwardsScript.getSponsors(connection, tournament);
    if (!sponsors.isEmpty()) {
      final Element list = FOPUtils.createXslFoElement(document, "list-block");
      mainBlock.appendChild(list);
      list.setAttribute("provisional-distance-between-starts", "10pt");

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
      mainBlock.appendChild(document.createTextNode("No sponsors specified"));
    }

    try (StringWriter writer = new StringWriter()) {
      final String rawText = AwardsScript.getSectionText(connection, tournament,
                                                         AwardsScript.Section.SPONSORS_RECOGNITION);

      if (!Velocity.evaluate(templateContext, writer, AwardsScript.Section.FRONT_MATTER.name(), rawText)) {
        throw new FLLRuntimeException(String.format("Error evaluating template for section %s",
                                                    AwardsScript.Section.SPONSORS_RECOGNITION));
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

  private Element createVolunteers(final Connection connection,
                                   final Tournament tournament,
                                   final Document document,
                                   final VelocityContext templateContext)
      throws SQLException {
    return createSectionBlock(connection, tournament, document, templateContext, AwardsScript.Section.VOLUNTEERS);
  }

  private Element createSectionBlock(final Connection connection,
                                     final Tournament tournament,
                                     final Document document,
                                     final VelocityContext templateContext,
                                     final AwardsScript.Section section)
      throws SQLException {
    final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);

    final Element mainBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(mainBlock);

    final String rawText = AwardsScript.getSectionText(connection, tournament, section);

    try (StringWriter writer = new StringWriter()) {
      if (!Velocity.evaluate(templateContext, writer, section.name(), rawText)) {
        throw new FLLRuntimeException(String.format("Error evaluating template for section %s", section));
      }
      mainBlock.appendChild(document.createTextNode(writer.toString()));
    } catch (final IOException e) {
      throw new FLLInternalException("Should not get IO exception writing to a string", e);
    }

    return container;
  }

}
