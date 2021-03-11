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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

import fll.Team;
import fll.Tournament;
import fll.Utilities;
import fll.db.AdvancingTeam;
import fll.db.AwardWinner;
import fll.db.AwardWinners;
import fll.db.OverallAwardWinner;
import fll.util.FLLInternalException;
import fll.util.FOPUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.api.AwardsReportSortedGroupsServlet;
import fll.web.scoreboard.Top10;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveScoreCategory;
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
    if (PromptSummarizeScores.checkIfSummaryUpdated(response, application, session, "/report/AwardsReport")) {
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

    final List<String> sortedAwardGroups = AwardsReportSortedGroupsServlet.getAwardGroupsSorted(connection,
                                                                                                tournament.getTournamentID());

    addPerformance(connection, document, documentBody, description, sortedAwardGroups);

    addHeadToHead(connection, tournament, description, document, documentBody, sortedAwardGroups);

    addSubjectiveChallengeWinners(connection, description, document, documentBody, tournament, sortedAwardGroups);
    addSubjectiveExtraWinners(connection, document, documentBody, tournament, sortedAwardGroups);
    addSubjectiveOverallWinners(connection, document, documentBody, tournament);

    final List<AdvancingTeam> advancing = AdvancingTeam.loadAdvancingTeams(connection, tournament.getTournamentID());
    if (!advancing.isEmpty()) {
      final Element advancingElement = addAdvancingTeams(advancing, connection, document, tournament,
                                                         sortedAwardGroups);
      documentBody.appendChild(advancingElement);
    }

    return document;
  }

  private void addHeadToHead(final Connection connection,
                             final Tournament tournament,
                             final ChallengeDescription challengeDescription,
                             final Document document,
                             final Element documentBody,
                             final List<String> sortedGroups)
      throws SQLException {
    final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    documentBody.appendChild(container);

    container.setAttribute("keep-together.within-page", "always");

    container.appendChild(FOPUtils.createHorizontalLine(document, 2));

    PlayoffReport.populateBody(connection, tournament, challengeDescription, document, container, sortedGroups);
  }

  private void addSubjectiveChallengeWinners(final Connection connection,
                                             final ChallengeDescription description,
                                             final Document document,
                                             final Element documentBody,
                                             final Tournament tournament,
                                             final List<String> sortedAwardGroups)
      throws SQLException {
    final List<AwardWinner> winners = AwardWinners.getChallengeAwardWinners(connection, tournament.getTournamentID());

    final List<String> categoryOrder = description.getSubjectiveCategories().stream() //
                                                  .map(SubjectiveScoreCategory::getTitle) //
                                                  .collect(Collectors.toList());
    addSubjectiveWinners(connection, document, documentBody, winners, sortedAwardGroups, categoryOrder);
  }

  private void addSubjectiveExtraWinners(final Connection connection,
                                         final Document document,
                                         final Element documentBody,
                                         final Tournament tournament,
                                         final List<String> sortedAwardGroups)
      throws SQLException {
    final List<AwardWinner> winners = AwardWinners.getExtraAwardWinners(connection, tournament.getTournamentID());

    addSubjectiveWinners(connection, document, documentBody, winners, sortedAwardGroups, Collections.emptyList());
  }

  private void addSubjectiveWinners(final Connection connection,
                                    final Document document,
                                    final Element documentBody,
                                    final List<AwardWinner> winners,
                                    final List<String> sortedAwardGroups,
                                    final List<String> categoryOrder)
      throws SQLException {
    final Map<String, Map<String, List<AwardWinner>>> organizedWinners = new HashMap<>();
    for (final AwardWinner winner : winners) {
      final Map<String, List<AwardWinner>> agWinners = organizedWinners.computeIfAbsent(winner.getName(),
                                                                                        k -> new HashMap<>());
      final List<AwardWinner> categoryWinners = agWinners.computeIfAbsent(winner.getAwardGroup(),
                                                                          k -> new LinkedList<>());
      categoryWinners.add(winner);
    }

    final List<String> fullOrder = new LinkedList<String>(categoryOrder);
    organizedWinners.keySet().forEach(c -> {
      if (!fullOrder.contains(c)) {
        fullOrder.add(c);
      }
    });
    for (final String categoryName : fullOrder) {
      if (organizedWinners.containsKey(categoryName)) {
        final Map<String, List<AwardWinner>> categoryWinners = organizedWinners.get(categoryName);
        final Element container = addSubjectiveAwardGroupWinners(connection, document, categoryName, categoryWinners,
                                                                 sortedAwardGroups);
        documentBody.appendChild(container);
      }
    }
  }

  private void addSubjectiveOverallWinners(final Connection connection,
                                           final Document document,
                                           final Element documentBody,
                                           final Tournament tournament)
      throws SQLException {
    final List<OverallAwardWinner> winners = AwardWinners.getOverallAwardWinners(connection,
                                                                                 tournament.getTournamentID());

    final Map<String, List<OverallAwardWinner>> organizedWinners = new HashMap<>();
    for (final OverallAwardWinner winner : winners) {
      final List<OverallAwardWinner> categoryWinners = organizedWinners.computeIfAbsent(winner.getName(),
                                                                                        k -> new LinkedList<>());
      categoryWinners.add(winner);
    }

    for (final Map.Entry<String, List<OverallAwardWinner>> entry : organizedWinners.entrySet()) {
      final String categoryName = entry.getKey();
      final List<OverallAwardWinner> categoryWinners = entry.getValue();
      if (!categoryWinners.isEmpty()) {
        final Element container = addSubjectiveOverallWinners(connection, document, categoryName, categoryWinners);
        documentBody.appendChild(container);
      }
    }
  }

  /**
   * @param categoryWinners awardGroup to list of winners
   */
  private Element addSubjectiveOverallWinners(final Connection connection,
                                              final Document document,
                                              final String categoryName,
                                              final List<OverallAwardWinner> categoryWinners)
      throws SQLException {
    final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    container.setAttribute("keep-together.within-page", "always");

    container.appendChild(FOPUtils.createHorizontalLine(document, 2));

    final Element categoryTitleBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(categoryTitleBlock);
    categoryTitleBlock.setAttribute("font-weight", "bold");
    categoryTitleBlock.appendChild(document.createTextNode(String.format("%s Award", categoryName)));

    final Element table = FOPUtils.createBasicTable(document);
    container.appendChild(table);

    table.appendChild(FOPUtils.createTableColumn(document, AWARD_DESCRIPTION_WIDTH));
    table.appendChild(FOPUtils.createTableColumn(document, TEAM_NUMBER_WIDTH));
    table.appendChild(FOPUtils.createTableColumn(document, SUBJECTIVE_TEAM_NAME_WIDTH));
    final int columnsInTable = FOPUtils.columnsInTable(table);

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    table.appendChild(tableBody);

    for (final OverallAwardWinner winner : categoryWinners) {
      final Element row = FOPUtils.createTableRow(document);
      tableBody.appendChild(row);

      row.appendChild(FOPUtils.createTableCell(document, null, "Winner:"));

      row.appendChild(FOPUtils.createTableCell(document, null, String.valueOf(winner.getTeamNumber())));

      final int teamNumber = winner.getTeamNumber();
      final Team team = Team.getTeamFromDatabase(connection, teamNumber);
      row.appendChild(FOPUtils.createTableCell(document, null, String.valueOf(team.getTeamName())));

      if (null != winner.getDescription()) {
        final Element descriptionRow = FOPUtils.createTableRow(document);
        tableBody.appendChild(descriptionRow);

        final Element descriptionCell = FOPUtils.createTableCell(document, null,
                                                                 String.format("%c%c%s", Utilities.NON_BREAKING_SPACE,
                                                                               Utilities.NON_BREAKING_SPACE,
                                                                               winner.getDescription()));
        descriptionRow.appendChild(descriptionCell);

        descriptionCell.setAttribute("number-columns-spanned", String.valueOf(columnsInTable));
      }
    } // foreach winner

    return container;
  }

  /**
   * @param categoryWinners awardGroup to list of winners
   */
  private Element addSubjectiveAwardGroupWinners(final Connection connection,
                                                 final Document document,
                                                 final String categoryName,
                                                 final Map<String, List<AwardWinner>> categoryWinners,
                                                 final List<String> sortedGroups)
      throws SQLException {
    final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    container.setAttribute("keep-together.within-page", "always");

    container.appendChild(FOPUtils.createHorizontalLine(document, 2));

    final Element categoryTitleBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(categoryTitleBlock);
    categoryTitleBlock.setAttribute("font-weight", "bold");
    categoryTitleBlock.appendChild(document.createTextNode(String.format("%s Award", categoryName)));

    final Element table = FOPUtils.createBasicTable(document);
    container.appendChild(table);

    table.appendChild(FOPUtils.createTableColumn(document, AWARD_DESCRIPTION_WIDTH));
    table.appendChild(FOPUtils.createTableColumn(document, TEAM_NUMBER_WIDTH));
    table.appendChild(FOPUtils.createTableColumn(document, SUBJECTIVE_TEAM_NAME_WIDTH));
    final int columnsInTable = FOPUtils.columnsInTable(table);

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    table.appendChild(tableBody);

    final List<String> localSortedGroups = new LinkedList<>(sortedGroups);
    categoryWinners.entrySet().stream().map(Map.Entry::getKey).filter(e -> !localSortedGroups.contains(e))
                   .forEach(localSortedGroups::add);

    for (final String group : localSortedGroups) {
      if (categoryWinners.containsKey(group)) {
        final List<AwardWinner> agWinners = categoryWinners.get(group);

        if (!agWinners.isEmpty()) {
          final Element row = FOPUtils.createTableRow(document);
          tableBody.appendChild(row);

          boolean first = true;
          for (final AwardWinner winner : agWinners) {
            if (first) {
              row.appendChild(FOPUtils.createTableCell(document, null, String.format("Winner %s:", group)));

              first = false;
            } else {
              row.appendChild(FOPUtils.createTableCell(document, null, ""));
            }

            row.appendChild(FOPUtils.createTableCell(document, null, String.valueOf(winner.getTeamNumber())));

            final int teamNumber = winner.getTeamNumber();
            final Team team = Team.getTeamFromDatabase(connection, teamNumber);
            row.appendChild(FOPUtils.createTableCell(document, null, String.valueOf(team.getTeamName())));

            if (null != winner.getDescription()) {
              final Element descriptionRow = FOPUtils.createTableRow(document);
              tableBody.appendChild(descriptionRow);

              final Element descriptionCell = FOPUtils.createTableCell(document, null,
                                                                       String.format("%c%c%s",
                                                                                     Utilities.NON_BREAKING_SPACE,
                                                                                     Utilities.NON_BREAKING_SPACE,
                                                                                     winner.getDescription()));
              descriptionRow.appendChild(descriptionCell);

              descriptionCell.setAttribute("number-columns-spanned", String.valueOf(columnsInTable));
            }
          } // foreach winner
        } // have winners in award group
      } // group exists
    } // foreach award group

    return container;
  }

  private static final int AWARD_DESCRIPTION_WIDTH = 2;

  private static final int TEAM_NUMBER_WIDTH = 1;

  private static final int TEAM_NAME_WIDTH = 3;

  private static final int SCORE_DESCRIPTION_WIDTH = 2;

  private static final int SCORE_WIDTH = 1;

  private static final int SUBJECTIVE_TEAM_NAME_WIDTH = TEAM_NAME_WIDTH
      + SCORE_DESCRIPTION_WIDTH
      + SCORE_WIDTH;

  private void addPerformance(final Connection connection,
                              final Document document,
                              final Element documentBody,
                              final ChallengeDescription description,
                              final List<String> sortedAwardGroups)
      throws SQLException {
    documentBody.appendChild(FOPUtils.createHorizontalLine(document, 2));

    final Element categoryTitleBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    documentBody.appendChild(categoryTitleBlock);
    categoryTitleBlock.setAttribute("font-weight", "bold");

    categoryTitleBlock.appendChild(document.createTextNode("Robot Performance Award - top score from regular match play"));

    final Map<String, List<Top10.ScoreEntry>> scores = Top10.getTableAsMapByAwardGroup(connection, description);

    final Element table = FOPUtils.createBasicTable(document);
    documentBody.appendChild(table);

    table.appendChild(FOPUtils.createTableColumn(document, AWARD_DESCRIPTION_WIDTH));
    table.appendChild(FOPUtils.createTableColumn(document, TEAM_NUMBER_WIDTH));
    table.appendChild(FOPUtils.createTableColumn(document, TEAM_NAME_WIDTH));
    table.appendChild(FOPUtils.createTableColumn(document, SCORE_DESCRIPTION_WIDTH));
    table.appendChild(FOPUtils.createTableColumn(document, SCORE_WIDTH));

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    table.appendChild(tableBody);

    // make sure all groups are in the sort
    final List<String> localSortedAwardGroups = new LinkedList<>(sortedAwardGroups);
    scores.entrySet().stream().map(Map.Entry::getKey).filter(e -> !localSortedAwardGroups.contains(e))
          .forEach(localSortedAwardGroups::add);

    for (final String group : localSortedAwardGroups) {
      if (scores.containsKey(group)) {
        final List<Top10.ScoreEntry> scoreList = scores.get(group);

        final Optional<Top10.ScoreEntry> winner = scoreList.stream().findFirst();
        if (winner.isPresent()) {
          final Element row = FOPUtils.createTableRow(document);
          tableBody.appendChild(row);

          row.appendChild(FOPUtils.createTableCell(document, null, String.format("Winner %s:", group)));

          row.appendChild(FOPUtils.createTableCell(document, null, String.valueOf(winner.get().getTeamNumber())));

          row.appendChild(FOPUtils.createTableCell(document, null, String.valueOf(winner.get().getTeamName())));

          row.appendChild(FOPUtils.createTableCell(document, null, "With a score of:"));

          row.appendChild(FOPUtils.createTableCell(document, null, String.valueOf(winner.get().getFormattedScore())));
        } // have a winner
      } // group has scores
    } // foreach group

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

    final LocalDate tournamentDate = tournament.getDate();
    if (null != tournamentDate) {
      final String dateString = String.format("Date: %s", DATE_FORMATTER.format(tournamentDate));

      subtitleBlock.appendChild(document.createTextNode(dateString));
    }

    staticContent.appendChild(FOPUtils.createBlankLine(document));

    return staticContent;
  }

  /**
   * Date format for reports.
   */
  public static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder().appendValue(ChronoField.MONTH_OF_YEAR,
                                                                                                    2)
                                                                                       .appendLiteral('/')
                                                                                       .appendValue(ChronoField.DAY_OF_MONTH,
                                                                                                    2)
                                                                                       .appendLiteral('/')
                                                                                       .appendValue(ChronoField.YEAR, 4)
                                                                                       .toFormatter();

  private static String createTitle(final ChallengeDescription description,
                                    final Tournament tournament) {
    final StringBuilder titleBuilder = new StringBuilder();
    titleBuilder.append(description.getTitle());

    if (null != tournament.getLevel()) {
      titleBuilder.append(" ");
      titleBuilder.append(tournament.getLevel());
    }

    titleBuilder.append(" Award Winners");
    return titleBuilder.toString();
  }

  private Element addAdvancingTeams(final List<AdvancingTeam> advancing,
                                    final Connection connection,
                                    final Document document,
                                    final Tournament tournament,
                                    final List<String> sortedGroups)
      throws SQLException {

    final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    container.setAttribute("keep-together.within-page", "always");

    container.appendChild(FOPUtils.createHorizontalLine(document, 2));

    final Element categoryTitleBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(categoryTitleBlock);
    categoryTitleBlock.setAttribute("font-weight", "bold");
    if (null != tournament.getNextLevel()) {
      categoryTitleBlock.appendChild(document.createTextNode(String.format("Teams advancing to %s",
                                                                           tournament.getNextLevel())));
    } else {
      categoryTitleBlock.appendChild(document.createTextNode(String.format("Teams advancing to the next tournament",
                                                                           tournament.getNextLevel())));
    }

    final Element table = FOPUtils.createBasicTable(document);
    container.appendChild(table);

    table.appendChild(FOPUtils.createTableColumn(document, AWARD_DESCRIPTION_WIDTH));
    table.appendChild(FOPUtils.createTableColumn(document, TEAM_NUMBER_WIDTH));
    table.appendChild(FOPUtils.createTableColumn(document, SUBJECTIVE_TEAM_NAME_WIDTH));
    final int columnsInTable = FOPUtils.columnsInTable(table);

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    table.appendChild(tableBody);

    final Map<String, List<AdvancingTeam>> organizedAdvancing = new HashMap<>();
    for (final AdvancingTeam advance : advancing) {
      final List<AdvancingTeam> agAdvancing = organizedAdvancing.computeIfAbsent(advance.getGroup(),
                                                                                 k -> new LinkedList<>());
      agAdvancing.add(advance);
    }

    final List<String> localSortedGroups = new LinkedList<>(sortedGroups);
    organizedAdvancing.entrySet().stream().map(Map.Entry::getKey).filter(e -> !localSortedGroups.contains(e))
                      .forEach(localSortedGroups::add);
    for (final String group : localSortedGroups) {
      if (organizedAdvancing.containsKey(group)) {
        final List<AdvancingTeam> groupAdvancing = organizedAdvancing.get(group);
        outputAdvancingTeams(connection, document, tableBody, group, groupAdvancing, columnsInTable);
      } // group exists
    } // foreach group

    return container;
  }

  private void outputAdvancingTeams(final Connection connection,
                                    final Document document,
                                    final Element tableBody,
                                    final String group,
                                    final List<AdvancingTeam> groupAdvancing,
                                    final int columnsInTable)
      throws SQLException {

    boolean first = true;
    for (final AdvancingTeam winner : groupAdvancing) {
      final Element row = FOPUtils.createTableRow(document);
      tableBody.appendChild(row);

      if (first) {
        row.appendChild(FOPUtils.createTableCell(document, null, String.format("%s:", group)));
        first = false;
      } else {
        row.appendChild(FOPUtils.createTableCell(document, null, ""));
      }

      row.appendChild(FOPUtils.createTableCell(document, null, String.valueOf(winner.getTeamNumber())));

      final int teamNumber = winner.getTeamNumber();
      final Team team = Team.getTeamFromDatabase(connection, teamNumber);
      row.appendChild(FOPUtils.createTableCell(document, null, String.valueOf(team.getTeamName())));

    } // foreach advancing

    // add some spacing
    final Element emptyRow = FOPUtils.createTableRow(document);
    tableBody.appendChild(emptyRow);
    final Element emptyCell = FOPUtils.createTableCell(document, null, String.valueOf(Utilities.NON_BREAKING_SPACE));
    emptyRow.appendChild(emptyCell);
    emptyCell.setAttribute("number-columns-spanned", String.valueOf(columnsInTable));

  }

}
