/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.report;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Tournament;
import fll.Utilities;
import fll.db.Queries;
import fll.scheduler.ScheduleWriter;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLInternalException;
import fll.util.FOPUtils;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.xml.ChallengeDescription;
import fll.xml.GoalElement;
import fll.xml.SubjectiveScoreCategory;
import fll.xml.WinnerType;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Display the report for scores by score group.
 *
 * @author jpschewe
 */
@WebServlet("/report/CategoryScoresByScoreGroup")
public class CategoryScoresByScoreGroup extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.ADMIN), false)) {
      return;
    }

    if (PromptSummarizeScores.checkIfSummaryUpdated(response, application, session,
                                                    "/report/CategoryScoresByScoreGroup")) {
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
      final int tournamentID = Queries.getCurrentTournament(connection);
      final Tournament tournament = Tournament.findTournamentByID(connection, tournamentID);
      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=categoryScoresByJudgingStation.pdf");

      outputReport(response.getOutputStream(), connection, challengeDescription, tournament);
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "winner criteria determines sort")
  private Document createDocument(final Connection connection,
                                  final ChallengeDescription challengeDescription,
                                  final Tournament tournament)
      throws SQLException {
    final Document document = XMLUtils.DOCUMENT_BUILDER.newDocument();

    final Element rootElement = FOPUtils.createRoot(document);
    document.appendChild(rootElement);

    final Element layoutMasterSet = FOPUtils.createXslFoElement(document, "layout-master-set");
    rootElement.appendChild(layoutMasterSet);

    final String pageMasterName = "simple";
    final Element pageMaster = FOPUtils.createSimplePageMaster(document, pageMasterName, FOPUtils.PAGE_LETTER_SIZE,
                                                               FOPUtils.STANDARD_MARGINS, 0.2,
                                                               FOPUtils.STANDARD_FOOTER_HEIGHT);
    layoutMasterSet.appendChild(pageMaster);

    final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
    rootElement.appendChild(pageSequence);
    pageSequence.setAttribute("id", FOPUtils.PAGE_SEQUENCE_NAME);

    final Element header = createLegend(document);
    pageSequence.appendChild(header);

    final Element footer = FOPUtils.createSimpleFooter(document);
    pageSequence.appendChild(footer);

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    final String challengeTitle = challengeDescription.getTitle();
    final WinnerType winnerCriteria = challengeDescription.getWinner();

    final List<SubjectiveScoreCategory> subjectiveCategories = challengeDescription.getSubjectiveCategories();
    final Collection<String> eventDivisions = Queries.getAwardGroups(connection);
    final Collection<String> judgingGroups = Queries.getJudgingStations(connection, tournament.getTournamentID());

    final Iterator<SubjectiveScoreCategory> iter = subjectiveCategories.iterator();
    while (iter.hasNext()) {
      final SubjectiveScoreCategory catElement = iter.next();
      final String catName = catElement.getName();
      final String catTitle = catElement.getTitle();

      // 1 - tournament
      // 2 - category
      // 3 - goal group
      // 4 - tournament
      // 5 - award group
      // 6 - judging group
      try (PreparedStatement prep = connection.prepareStatement("SELECT "//
          + " Teams.TeamNumber, Teams.TeamName, Teams.Organization, final_scores.final_score" //
          + " FROM Teams, final_scores" //
          + " WHERE final_scores.tournament = ?" //
          + " AND final_scores.team_number = Teams.TeamNumber" //
          + " AND final_scores.category = ?" //
          + " AND final_scores.goal_group = ?"//
          + " AND final_scores.team_number IN (" //
          + "   SELECT TeamNumber FROM TournamentTeams"//
          + "   WHERE Tournament = ?" //
          + "   AND event_division = ?" //
          + "   AND judging_station = ?)" //
          + " ORDER BY final_scores.final_score"
          + " "
          + winnerCriteria.getSortString() //
      )) {
        prep.setInt(1, tournament.getTournamentID());
        prep.setString(2, catName);
        prep.setInt(4, tournament.getTournamentID());

        // the raw category is added in the stream pipeline because there is no
        // guarantee that the resulting set is mutable
        final Set<String> goalGroups = Stream.concat(Stream.of(""), // raw category
                                                     catElement.getGoalElements().stream()
                                                               .filter(GoalElement::isGoalGroup)
                                                               .map(GoalElement::getTitle))
                                             .distinct().collect(Collectors.toSet());
        for (final String goalGroup : goalGroups) {
          prep.setString(3, goalGroup);

          for (final String division : eventDivisions) {
            for (final String judgingGroup : judgingGroups) {
              final Element table = FOPUtils.createBasicTable(document);
              table.setAttribute("page-break-after", "always");

              table.appendChild(FOPUtils.createTableColumn(document, 1));
              table.appendChild(FOPUtils.createTableColumn(document, 1));
              table.appendChild(FOPUtils.createTableColumn(document, 1));
              table.appendChild(FOPUtils.createTableColumn(document, 1));

              final Element tableHeader = createTableHeader(document, challengeTitle, catTitle, goalGroup, division,
                                                            judgingGroup, tournament);
              table.appendChild(tableHeader);

              final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
              table.appendChild(tableBody);

              prep.setString(5, division);
              prep.setString(6, judgingGroup);

              boolean haveData = false;
              try (ResultSet rs = prep.executeQuery()) {
                while (rs.next()) {
                  haveData = true;

                  final Element tableRow = FOPUtils.createTableRow(document);
                  tableBody.appendChild(tableRow);
                  FOPUtils.keepWithPrevious(tableRow);

                  final int teamNumber = rs.getInt(1);
                  final String teamName = rs.getString(2);
                  final String organization = rs.getString(3);

                  Element cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                          String.valueOf(teamNumber));
                  tableRow.appendChild(cell);
                  FOPUtils.addBorders(cell, ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH,
                                      ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH);
                  FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                                      FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

                  cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                  null == teamName ? "" : teamName);
                  tableRow.appendChild(cell);
                  FOPUtils.addBorders(cell, ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH,
                                      ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH);
                  FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                                      FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

                  cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                                  null == organization ? "" : organization);
                  tableRow.appendChild(cell);
                  FOPUtils.addBorders(cell, ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH,
                                      ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH);
                  FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                                      FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

                  double score = rs.getDouble(4);
                  if (rs.wasNull()) {
                    score = Double.NaN;
                  }
                  final StringBuilder scoreText = new StringBuilder();
                  if (Double.isNaN(score)) {
                    scoreText.append("No Score");

                  } else {
                    final boolean zeroInRequiredGoal = FinalComputedScores.checkZeroInRequiredGoal(connection,
                                                                                                   tournament,
                                                                                                   catElement,
                                                                                                   teamNumber);

                    scoreText.append(Utilities.getFloatingPointNumberFormat().format(score));
                    if (zeroInRequiredGoal) {
                      scoreText.append(" @");
                    }
                  }

                  cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, scoreText.toString());
                  tableRow.appendChild(cell);
                  FOPUtils.addBorders(cell, ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH,
                                      ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH);
                  FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                                      FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

                } // foreach result
              } // allocate rs

              if (haveData) {
                // only add the table if there is data
                documentBody.appendChild(table);
              }

            } // foreach station
          } // foreach division
        } // foreach goal group
      } // allocate prep
    } // foreach category

    return document;
  }

  private void outputReport(final OutputStream stream,
                            final Connection connection,
                            final ChallengeDescription challengeDescription,
                            final Tournament tournament)
      throws IOException, SQLException {
    try {

      final Document document = createDocument(connection, challengeDescription, tournament);

      final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

      FOPUtils.renderPdf(fopFactory, document, stream);
    } catch (FOPException | TransformerException e) {
      throw new FLLInternalException("Error creating the performance schedule PDF", e);
    }
  }

  private Element createLegend(final Document document) {

    final Element staticContent = FOPUtils.createXslFoElement(document, "static-content");
    staticContent.setAttribute("flow-name", "xsl-region-before");

    final Element block = FOPUtils.createXslFoElement(document, FOPUtils.BLOCK_TAG);
    staticContent.appendChild(block);
    block.appendChild(document.createTextNode("@ - zero score on required goal"));

    return staticContent;

  }

  private Element createTableHeader(final Document document,
                                    final String challengeTitle,
                                    final String catTitle,
                                    final String goalGroup,
                                    final String division,
                                    final String judgingGroup,
                                    final Tournament tournament) {
    final Element tableHeader = FOPUtils.createTableHeader(document);
    tableHeader.setAttribute("font-weight", "bold");

    final Element row1 = FOPUtils.createTableRow(document);
    tableHeader.appendChild(row1);

    Element cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER,
                                            String.format("%s - %s", challengeTitle, tournament.getDescription()));
    row1.appendChild(cell);
    cell.setAttribute("number-columns-spanned", "4");
    FOPUtils.addBorders(cell, ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH,
                        ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH);
    FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                        FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

    final Element row2 = FOPUtils.createTableRow(document);
    tableHeader.appendChild(row2);

    final String categoryText;
    if (null == goalGroup
        || goalGroup.trim().isEmpty()) {
      categoryText = String.format("Category: %s - Award Group: %s - JudgingGroup: %s", catTitle, division,
                                   judgingGroup);
    } else {
      categoryText = String.format("Category: %s - Goal Group - %s - Award Group: %s - JudgingGroup: %s", catTitle,
                                   goalGroup, division, judgingGroup);
    }
    cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, categoryText);
    row2.appendChild(cell);
    cell.setAttribute("number-columns-spanned", "4");
    FOPUtils.addBorders(cell, ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH,
                        ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH);
    FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                        FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

    final Element row3 = FOPUtils.createTableRow(document);
    tableHeader.appendChild(row3);

    cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, TournamentSchedule.TEAM_NUMBER_HEADER);
    row3.appendChild(cell);
    FOPUtils.addBorders(cell, ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH,
                        ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH);
    FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                        FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

    cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, TournamentSchedule.TEAM_NAME_HEADER);
    row3.appendChild(cell);
    FOPUtils.addBorders(cell, ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH,
                        ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH);
    FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                        FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

    cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, TournamentSchedule.ORGANIZATION_HEADER);
    row3.appendChild(cell);
    FOPUtils.addBorders(cell, ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH,
                        ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH);
    FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                        FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

    cell = FOPUtils.createTableCell(document, FOPUtils.TEXT_ALIGN_CENTER, "Scaled Score");
    row3.appendChild(cell);
    FOPUtils.addBorders(cell, ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH,
                        ScheduleWriter.STANDARD_BORDER_WIDTH, ScheduleWriter.STANDARD_BORDER_WIDTH);
    FOPUtils.addPadding(cell, FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING,
                        FOPUtils.TABLE_CELL_STANDARD_PADDING, FOPUtils.TABLE_CELL_STANDARD_PADDING);

    return tableHeader;
  }

}
