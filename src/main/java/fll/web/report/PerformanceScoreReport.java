/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
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

import fll.Tournament;
import fll.TournamentTeam;
import fll.Utilities;
import fll.db.Queries;
import fll.db.RunMetadata;
import fll.util.FLLInternalException;
import fll.util.FOPUtils;
import fll.util.FP;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.TournamentData;
import fll.web.UserRole;
import fll.web.playoff.DatabaseTeamScore;
import fll.web.playoff.TeamScore;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.EnumeratedValue;
import fll.xml.GoalElement;
import fll.xml.GoalGroup;
import fll.xml.PerformanceScoreCategory;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Report displaying the details of performance scores for each team.
 */
@WebServlet("/report/PerformanceScoreReport")
public class PerformanceScoreReport extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager.getLogger();

  private static final String TITLE_FONT_FAMILY = "Times";

  private static final String TITLE_FONT_SIZE = "12pt";

  private static final String TITLE_FONT_WEIGHT = "bold";

  private static final String SCORE_FONT_FAMILY = TITLE_FONT_FAMILY;

  private static final String SCORE_FONT_SIZE = "10pt";

  private static final String REPORT_TITLE = "Performance Score Report";

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
    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      // setting some response headers
      response.setHeader("Expires", "0");
      response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
      response.setHeader("Pragma", "public");
      // setting the content type
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=performanceScoreReport.pdf");

      final OutputStream stream = response.getOutputStream();

      try {
        final Map<Integer, TournamentTeam> teams = Queries.getTournamentTeams(connection);

        final Document document = createDocument(tournamentData, connection, challengeDescription, teams.values());

        final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

        FOPUtils.renderPdf(fopFactory, document, stream);
      } catch (FOPException | TransformerException e) {
        throw new FLLInternalException("Error creating the performance schedule PDF", e);
      }

      stream.flush();

    } catch (final SQLException e) {
      LOG.error(e, e);
      throw new RuntimeException(e);
    }
  }

  /**
   * @param tournamentData data for the tournament
   * @param connection database connection
   * @param challengeDescription challenge description
   * @param teams the teams to output the details for
   * @return the document to be rendered
   * @throws SQLException if there is a database error
   */
  public static Document createDocument(final TournamentData tournamentData,
                                        final Connection connection,
                                        final ChallengeDescription challengeDescription,
                                        final Collection<TournamentTeam> teams)
      throws SQLException {
    final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();

    final Element rootElement = FOPUtils.createRoot(document);
    document.appendChild(rootElement);

    final Element layoutMasterSet = FOPUtils.createXslFoElement(document, "layout-master-set");
    rootElement.appendChild(layoutMasterSet);

    final String pageMasterName = "simple";
    final Element pageMaster = FOPUtils.createSimplePageMaster(document, pageMasterName, FOPUtils.PAGE_LETTER_SIZE,
                                                               FOPUtils.STANDARD_MARGINS, 1.25,
                                                               FOPUtils.STANDARD_FOOTER_HEIGHT);
    layoutMasterSet.appendChild(pageMaster);

    if (teams.isEmpty()) {
      final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
      rootElement.appendChild(pageSequence);
      pageSequence.setAttribute("id", FOPUtils.PAGE_SEQUENCE_NAME);

      final Element header = createHeader(document, challengeDescription.getTitle(),
                                          tournamentData.getCurrentTournament(), null);
      pageSequence.appendChild(header);

      final Element footer = FOPUtils.createSimpleFooter(document);
      pageSequence.appendChild(footer);

      final Element documentBody = FOPUtils.createBody(document);
      pageSequence.appendChild(documentBody);
      final Element block = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      documentBody.appendChild(block);

      block.appendChild(document.createTextNode("No teams in the tournament."));
    } else {
      for (final TournamentTeam team : teams) {
        final Element teamPageSequence = createTeamPageSequence(tournamentData, connection, document, pageMasterName,
                                                                challengeDescription, team);
        rootElement.appendChild(teamPageSequence);
      }
    }

    return document;
  }

  private static Element createTeamPageSequence(final TournamentData tournamentData,
                                                final Connection connection,
                                                final Document document,
                                                final String pageMasterName,
                                                final ChallengeDescription challenge,
                                                final TournamentTeam team)
      throws SQLException {
    final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);

    final String pageSequenceId = String.format("ps-%d", team.getTeamNumber());
    pageSequence.setAttribute("id", pageSequenceId);

    final Element header = createHeader(document, challenge.getTitle(), tournamentData.getCurrentTournament(), team);
    pageSequence.appendChild(header);

    final Element footer = FOPUtils.createSimpleFooter(document, pageSequenceId);
    pageSequence.appendChild(footer);

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    final Element teamData = outputTeam(tournamentData, connection, document, challenge, team);
    documentBody.appendChild(teamData);

    return pageSequence;
  }

  private static Element outputTeam(final TournamentData tournamentData,
                                    final Connection connection,
                                    final Document document,
                                    final ChallengeDescription challenge,
                                    final TournamentTeam team)
      throws SQLException {
    final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
    container.setAttribute("font-family", SCORE_FONT_FAMILY);
    container.setAttribute("font-size", SCORE_FONT_SIZE);

    final Element table = FOPUtils.createBasicTable(document);
    container.appendChild(table);

    final Element tableHeader = FOPUtils.createTableHeader(document);
    tableHeader.setAttribute("font-family", TITLE_FONT_FAMILY);
    tableHeader.setAttribute("font-size", TITLE_FONT_SIZE);
    tableHeader.setAttribute("font-weight", TITLE_FONT_WEIGHT);

    final Element tableHeaderRow = FOPUtils.createTableRow(document);
    tableHeader.appendChild(tableHeaderRow);

    table.appendChild(FOPUtils.createTableColumn(document, 1));
    tableHeaderRow.appendChild(createCell(document, ""));

    final List<RunMetadata> regularMatchPlayRuns = tournamentData.getRunMetadataFactory()
                                                                 .getRegularMatchPlayRunMetadata();
    for (final RunMetadata metadata : regularMatchPlayRuns) {
      table.appendChild(FOPUtils.createTableColumn(document, 1));

      tableHeaderRow.appendChild(createCell(document, metadata.getDisplayName()));
    }
    // add the table header to the table after all of the columns are created
    table.appendChild(tableHeader);

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    table.appendChild(tableBody);

    final PerformanceScoreCategory performance = challenge.getPerformance();

    final List<TeamScore> scores = getScores(connection, tournamentData.getCurrentTournament(), team,
                                             regularMatchPlayRuns);
    for (final GoalElement goalEle : performance.getGoalElements()) {
      if (goalEle.isGoalGroup()) {
        outputGoalGroup(document, tableBody, performance, scores, (GoalGroup) goalEle);
      } else if (goalEle.isGoal()) {
        outputGoal(document, tableBody, performance, scores, (AbstractGoal) goalEle);
      } else {
        throw new FLLInternalException("Unexpected goal element type: "
            + goalEle.getClass());
      }
    }

    // totals
    final Element totalRow = FOPUtils.createTableRow(document);
    tableBody.appendChild(totalRow);

    final Element totalCell = createCell(document, "Total");
    totalRow.appendChild(totalCell);
    totalCell.setAttribute("font-family", TITLE_FONT_FAMILY);
    totalCell.setAttribute("font-size", TITLE_FONT_SIZE);
    totalCell.setAttribute("font-weight", TITLE_FONT_WEIGHT);

    final double bestTotalScore = bestTotalScore(performance, scores);
    for (final TeamScore score : scores) {
      final Element scoreCell;
      if (!score.scoreExists()) {
        scoreCell = createCell(document, "");
      } else if (score.isBye()) {
        scoreCell = createCell(document, "Bye");
      } else if (score.isNoShow()) {
        scoreCell = createCell(document, "No Show");
      } else {
        final double totalScore = performance.evaluate(score);

        scoreCell = createCell(document,
                               Utilities.getFormatForScoreType(performance.getScoreType()).format(totalScore));
        if (FP.equals(bestTotalScore, totalScore, ChallengeParser.INITIAL_VALUE_TOLERANCE)) {
          scoreCell.setAttribute("font-weight", "bold");
        }
      }
      totalRow.appendChild(scoreCell);

    }

    final Element legendBlock = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    container.appendChild(legendBlock);
    legendBlock.appendChild(document.createTextNode("The team's top score for each goal and overall are in bold."));
    FOPUtils.keepWithPreviousAlways(legendBlock);
    legendBlock.setAttribute("space-before", "2pt");

    return container;
  }

  private static void outputGoalGroup(final Document document,
                                      final Element tableBody,
                                      final PerformanceScoreCategory performance,
                                      final List<TeamScore> scores,
                                      final GoalGroup group) {
    final int numCols = scores.size()
        + 1;

    // description
    final Element row = FOPUtils.createTableRow(document);
    tableBody.appendChild(row);
    FOPUtils.keepWithPrevious(row);

    final Element titleCell = createCell(document, group.getTitleAndDescription());
    row.appendChild(titleCell);
    titleCell.setAttribute("font-family", TITLE_FONT_FAMILY);
    titleCell.setAttribute("font-size", TITLE_FONT_SIZE);
    titleCell.setAttribute("font-weight", TITLE_FONT_WEIGHT);
    titleCell.setAttribute("number-columns-spanned", String.valueOf(numCols));

    FOPUtils.addTopBorder(titleCell, 2);

    for (final AbstractGoal goal : group.getGoals()) {
      outputGoal(document, tableBody, performance, scores, goal);
    }
  }

  private static void outputGoal(final Document document,
                                 final Element tableBody,
                                 final PerformanceScoreCategory performance,
                                 final List<TeamScore> scores,
                                 final AbstractGoal goal) {
    final double bestScore = bestScoreForGoal(scores, goal);

    final Element row = FOPUtils.createTableRow(document);
    tableBody.appendChild(row);
    FOPUtils.keepWithPrevious(row);

    final StringBuilder goalTitle = new StringBuilder();
    goalTitle.append(goal.getTitle());
    if (goal.isComputed()) {
      goalTitle.append(" (computed)");
    }
    final Element titleCell = createCell(document, goalTitle.toString());
    row.appendChild(titleCell);
    titleCell.setAttribute("font-family", TITLE_FONT_FAMILY);
    titleCell.setAttribute("font-size", TITLE_FONT_SIZE);
    titleCell.setAttribute("font-weight", TITLE_FONT_WEIGHT);

    for (final TeamScore score : scores) {
      if (!score.scoreExists()
          || score.isBye()
          || score.isNoShow()) {
        row.appendChild(createCell(document, ""));
      } else {
        final double computedValue = goal.evaluate(score);

        final StringBuilder cellStr = new StringBuilder();
        if (!goal.isComputed()) {
          if (goal.isEnumerated()) {
            final String enumValue = score.getEnumRawScore(goal.getName());
            boolean found = false;
            for (final EnumeratedValue ev : goal.getValues()) {
              if (ev.getValue().equals(enumValue)) {
                cellStr.append(ev.getTitle()
                    + " -> ");
                found = true;
                break;
              }
            }
            if (!found) {
              LOG.warn("Could not find enumerated title for "
                  + enumValue);
              cellStr.append(enumValue
                  + " -> ");
            }
          } else {
            if (goal.isYesNo()) {
              if (FP.greaterThan(score.getRawScore(goal.getName()), 0, ChallengeParser.INITIAL_VALUE_TOLERANCE)) {
                cellStr.append("Yes -> ");
              } else {
                cellStr.append("No -> ");
              }
            } else {
              final double rawValue = goal.getRawScore(score);
              cellStr.append(Utilities.getFormatForScoreType(goal.getScoreType()).format(rawValue)
                  + " -> ");
            }
          } // not enumerated
        } // not computed

        cellStr.append(Utilities.getFormatForScoreType(performance.getScoreType()).format(computedValue));
        final Element scoreCell = createCell(document, cellStr.toString());
        row.appendChild(scoreCell);
        if (FP.equals(bestScore, computedValue, ChallengeParser.INITIAL_VALUE_TOLERANCE)) {
          scoreCell.setAttribute("font-weight", "bold");
        }
      } // exists, non-bye, non-no show
    } // foreach score
  }

  /**
   * @return best total score
   */
  private static double bestTotalScore(final PerformanceScoreCategory performance,
                                       final List<TeamScore> scores) {
    double bestScore = Double.MAX_VALUE
        * -1;
    for (final TeamScore score : scores) {
      if (score.scoreExists()
          && !score.isBye()
          && !score.isNoShow()) {
        final double computedValue = performance.evaluate(score);
        bestScore = Math.max(bestScore, computedValue);
      } else {
        bestScore = Math.max(bestScore, 0);
      }
    }
    return bestScore;
  }

  /**
   * @return the best score for the specified goal
   */
  private static double bestScoreForGoal(final List<TeamScore> scores,
                                         final AbstractGoal goal) {
    double bestScore = Double.MAX_VALUE
        * -1;
    for (final TeamScore score : scores) {
      if (score.scoreExists()
          && !score.isBye()
          && !score.isNoShow()) {
        final double computedValue = goal.evaluate(score);
        bestScore = Math.max(bestScore, computedValue);
      } else {
        bestScore = Math.max(bestScore, 0);
      }
    }
    return bestScore;
  }

  private static List<TeamScore> getScores(final Connection connection,
                                           final Tournament tournament,
                                           final TournamentTeam team,
                                           final List<RunMetadata> regularMatchPlayRuns)
      throws SQLException {
    final List<TeamScore> scores = new LinkedList<>();
    for (final RunMetadata metadata : regularMatchPlayRuns) {
      scores.add(new DatabaseTeamScore(tournament.getTournamentID(), team.getTeamNumber(), metadata.getRunNumber(),
                                       connection));
    }
    return scores;
  }

  private static Element createCell(final Document document,
                                    final String text) {
    final Element cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, text);
    FOPUtils.addBorders(cell, FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH,
                        FOPUtils.STANDARD_BORDER_WIDTH, FOPUtils.STANDARD_BORDER_WIDTH);
    FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                        FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

    return cell;
  }

  private static Element createHeader(final Document document,
                                      final String challengeName,
                                      final Tournament tournament,
                                      final @Nullable TournamentTeam team) {
    final Element staticContent = FOPUtils.createXslFoElement(document, FOPUtils.STATIC_CONTENT_TAG);
    staticContent.setAttribute("flow-name", "xsl-region-before");
    staticContent.setAttribute("font-weight", TITLE_FONT_WEIGHT);
    staticContent.setAttribute("font-size", TITLE_FONT_SIZE);
    staticContent.setAttribute("font-family", TITLE_FONT_FAMILY);

    final Element table = FOPUtils.createBasicTable(document);
    staticContent.appendChild(table);

    table.appendChild(FOPUtils.createTableColumn(document, 1));
    table.appendChild(FOPUtils.createTableColumn(document, 1));

    final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
    table.appendChild(tableBody);

    final Element row1 = FOPUtils.createTableRow(document);
    tableBody.appendChild(row1);

    row1.appendChild(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT, challengeName));
    row1.appendChild(FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT,
                                              String.format("Tournament: %s", tournament.getDescription())));

    final Element row2 = FOPUtils.createTableRow(document);
    tableBody.appendChild(row2);

    final Element title = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_LEFT, REPORT_TITLE);
    row2.appendChild(title);
    FOPUtils.addBottomBorder(title, FOPUtils.THICK_BORDER_WIDTH);

    final Element date = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_RIGHT, tournament.getDateString());
    row2.appendChild(date);
    FOPUtils.addBottomBorder(date, FOPUtils.THICK_BORDER_WIDTH);

    if (null != team) {
      // team information
      final Element container = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_CONTAINER_TAG);
      staticContent.appendChild(container);
      container.setAttribute("space-before", "1pt");

      final Element block1 = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      container.appendChild(block1);

      block1.appendChild(document.createTextNode("Team #"
          + team.getTeamNumber()
          + " "
          + team.getTeamName()
          + " / "
          + team.getOrganization()));

      final Element block2 = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
      container.appendChild(block2);

      block2.appendChild(document.createTextNode("Award Group: "
          + team.getAwardGroup()));
    }

    return staticContent;

  }
}
