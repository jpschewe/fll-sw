/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.report;

import java.io.IOException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;
import javax.xml.transform.TransformerException;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FopFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.ScoreStandardization;
import fll.Tournament;
import fll.Utilities;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLInternalException;
import fll.util.FOPUtils;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.TournamentData;
import fll.web.UserRole;
import fll.web.report.awards.AwardsScriptReport;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreType;
import fll.xml.WinnerType;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.mtu.eggplant.xml.XMLUtils;

/**
 * Display the report of performance scores per team and per award group.
 *
 * @author jpschewe
 */
@WebServlet("/report/PerformanceReport")
public class PerformanceReport extends BaseFLLServlet {

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
    final DataSource datasource = tournamentData.getDataSource();
    try (Connection connection = datasource.getConnection()) {
      ScoreStandardization.computeSummarizedScoresIfNeeded(connection, challengeDescription,
                                                          tournamentData.getCurrentTournament());

      final Tournament tournament = tournamentData.getCurrentTournament();
      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=performanceScoreReport.pdf");

      try {

        final Document document = createReport(connection, challengeDescription, tournament);

        final FopFactory fopFactory = FOPUtils.createSimpleFopFactory();

        FOPUtils.renderPdf(fopFactory, document, response.getOutputStream());
      } catch (FOPException | TransformerException e) {
        throw new FLLInternalException("Error creating the performance schedule PDF", e);
      }

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private static final int NUM_COLMNS = 8;

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "winner criteria determines sort")
  private Document createReport(final Connection connection,
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
                                                               FOPUtils.STANDARD_MARGINS, 0,
                                                               FOPUtils.STANDARD_FOOTER_HEIGHT);
    layoutMasterSet.appendChild(pageMaster);

    final Element pageSequence = FOPUtils.createPageSequence(document, pageMasterName);
    rootElement.appendChild(pageSequence);
    pageSequence.setAttribute("id", FOPUtils.PAGE_SEQUENCE_NAME);

    final Element footer = FOPUtils.createSimpleFooter(document);
    pageSequence.appendChild(footer);

    final Element documentBody = FOPUtils.createBody(document);
    pageSequence.appendChild(documentBody);

    final String challengeTitle = challengeDescription.getTitle();
    final WinnerType winnerCriteria = challengeDescription.getWinner();

    final ScoreType performanceScoreType = challengeDescription.getPerformance().getScoreType();
    final NumberFormat rawScoreFormat = Utilities.getFormatForScoreType(performanceScoreType);

    final List<String> awardGroups = AwardsScriptReport.getAwardGroupOrder(connection, tournament);

    // 1 - tournament
    // 2 - tournament
    // 3 - award group
    try (PreparedStatement prep = connection.prepareStatement("SELECT "//
        + " performance.TeamNumber, MIN(Teams.TeamName), MIN(Teams.Organization)" //
        + "  ,array_agg(ComputedTotal) as scores" //
        + "  ,min(ComputedTotal) as min_score, max(ComputedTotal) as max_score"
        + "  ,avg(ComputedTotal) as average, stddev_pop(ComputedTotal)" //
        + " FROM Teams, performance" //
        + " WHERE performance.tournament = ?" //
        + " AND performance.bye = FALSE" //
        + " AND performance.NoShow = FALSE" //
        + " AND performance.ComputedTotal IS NOT NULL" //
        + " AND performance.TeamNumber = Teams.TeamNumber" //
        + " AND performance.TeamNumber IN (" //
        + "   SELECT TeamNumber FROM TournamentTeams"//
        + "   WHERE Tournament = ?" //
        + "   AND event_division = ?" //
        + "   )" //
        + " GROUP BY performance.TeamNumber" //
        + " ORDER BY max_score"
        + " "
        + winnerCriteria.getSortString() //
    )) {
      prep.setInt(1, tournament.getTournamentID());
      prep.setInt(2, tournament.getTournamentID());

      for (final String awardGroup : awardGroups) {
        final Element table = FOPUtils.createBasicTable(document);
        table.setAttribute("page-break-after", "always");

        table.appendChild(FOPUtils.createTableColumn(document, 1));
        table.appendChild(FOPUtils.createTableColumn(document, 2));
        table.appendChild(FOPUtils.createTableColumn(document, 2));
        table.appendChild(FOPUtils.createTableColumn(document, 2));
        table.appendChild(FOPUtils.createTableColumn(document, 1));
        table.appendChild(FOPUtils.createTableColumn(document, 1));
        table.appendChild(FOPUtils.createTableColumn(document, 1));
        table.appendChild(FOPUtils.createTableColumn(document, 1));

        final Element header = createHeader(document, challengeTitle, awardGroup, tournament);
        table.appendChild(header);

        final Element tableBody = FOPUtils.createXslFoElement(document, FOPUtils.TABLE_BODY_TAG);
        table.appendChild(tableBody);

        prep.setString(3, awardGroup);

        boolean haveData = false;
        try (ResultSet rs = prep.executeQuery()) {
          while (rs.next()) {
            haveData = true;

            final Element row = FOPUtils.createTableRow(document);
            tableBody.appendChild(row);
            FOPUtils.keepWithPrevious(row);

            final int teamNumber = rs.getInt(1);
            final String teamName = rs.getString(2);
            final String organization = rs.getString(3);
            final Array scores = castNonNull(rs.getArray(4)); // aggregation can't return null
            final double minScore = rs.getDouble(5);
            final double maxScore = rs.getDouble(6);
            final double average = rs.getDouble(7);
            final double stdev = rs.getDouble(8);

            row.appendChild(FOPUtils.createStandardTableCell(document, String.valueOf(teamNumber)));
            row.appendChild(FOPUtils.createStandardTableCell(document, null == teamName ? "" : teamName));
            row.appendChild(FOPUtils.createStandardTableCell(document, null == organization ? "" : organization));

            final String scoresText = scoresToText(rawScoreFormat, scores);
            row.appendChild(FOPUtils.createStandardTableCell(document, scoresText));

            row.appendChild(FOPUtils.createStandardTableCell(document, Utilities.getFloatingPointNumberFormat()
                                                                                .format(minScore)));
            row.appendChild(FOPUtils.createStandardTableCell(document, Utilities.getFloatingPointNumberFormat()
                                                                                .format(maxScore)));
            row.appendChild(FOPUtils.createStandardTableCell(document,
                                                             Utilities.getFloatingPointNumberFormat().format(average)));
            row.appendChild(FOPUtils.createStandardTableCell(document,
                                                             Utilities.getFloatingPointNumberFormat().format(stdev)));
          } // foreach result
        } // allocate rs

        if (haveData) {
          // only add the table if there is data
          documentBody.appendChild(table);
        }

      } // foreach division
    } // allocate prep

    return document;
  }

  private String scoresToText(final NumberFormat format,
                              final Array scores)
      throws SQLException {
    final Collection<String> values = new LinkedList<>();

    try (ResultSet rs = scores.getResultSet()) {
      while (rs.next()) {
        values.add(format.format(rs.getDouble(2)));
      }
    }

    return String.join(", ", values);
  }

  private Element createHeader(final Document document,
                               final String challengeTitle,
                               final String awardGroup,
                               final Tournament tournament) {
    final Element tableHeader = FOPUtils.createTableHeader(document);
    tableHeader.setAttribute("font-weight", "bold");

    final Element row1 = FOPUtils.createTableRow(document);
    tableHeader.appendChild(row1);

    Element cell = FOPUtils.createStandardTableCell(document, String.format("%s - %s", challengeTitle,
                                                                            tournament.getDescription()));
    row1.appendChild(cell);
    cell.setAttribute("number-columns-spanned", String.valueOf(NUM_COLMNS));

    final Element row2 = FOPUtils.createTableRow(document);
    tableHeader.appendChild(row2);
    cell = FOPUtils.createStandardTableCell(document, String.format("Award Group: %s", awardGroup));
    row2.appendChild(cell);
    cell.setAttribute("number-columns-spanned", String.valueOf(NUM_COLMNS));

    final Element row3 = FOPUtils.createTableRow(document);
    tableHeader.appendChild(row3);

    row3.appendChild(FOPUtils.createStandardTableCell(document, TournamentSchedule.TEAM_NUMBER_HEADER));
    row3.appendChild(FOPUtils.createStandardTableCell(document, TournamentSchedule.TEAM_NAME_HEADER));
    row3.appendChild(FOPUtils.createStandardTableCell(document, TournamentSchedule.ORGANIZATION_HEADER));
    row3.appendChild(FOPUtils.createStandardTableCell(document, "Scores"));
    row3.appendChild(FOPUtils.createStandardTableCell(document, "Min"));
    row3.appendChild(FOPUtils.createStandardTableCell(document, "Max"));
    row3.appendChild(FOPUtils.createStandardTableCell(document, "Avg"));
    row3.appendChild(FOPUtils.createStandardTableCell(document, "Std Dev"));

    return tableHeader;
  }
}
