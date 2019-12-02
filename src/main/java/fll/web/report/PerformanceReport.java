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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Tournament;
import fll.Utilities;
import fll.db.Queries;
import fll.scheduler.TournamentSchedule;
import fll.util.PdfUtils;
import fll.util.SimpleFooterHandler;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreType;
import fll.xml.WinnerType;

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
    if (PromptSummarizeScores.checkIfSummaryUpdated(response, application, session, "/report/PerformanceReport")) {
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
      final int tournamentID = Queries.getCurrentTournament(connection);
      final Tournament tournament = Tournament.findTournamentByID(connection, tournamentID);
      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=performanceScoreReport.pdf");

      final Document pdfDoc = PdfUtils.createPortraitPdfDoc(response.getOutputStream(), new SimpleFooterHandler());

      generateReport(connection, pdfDoc, challengeDescription, tournament);

      pdfDoc.close();

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    } catch (final DocumentException e) {
      throw new RuntimeException(e);
    }
  }

  private static final int NUM_COLMNS = 8;

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "winner criteria determines sort")
  private void generateReport(final Connection connection,
                              final Document pdfDoc,
                              final ChallengeDescription challengeDescription,
                              final Tournament tournament)
      throws SQLException, DocumentException {

    final String challengeTitle = challengeDescription.getTitle();
    final WinnerType winnerCriteria = challengeDescription.getWinner();

    final ScoreType performanceScoreType = challengeDescription.getPerformance().getScoreType();
    final NumberFormat rawScoreFormat = Utilities.getFormatForScoreType(performanceScoreType);

    final Collection<String> awardGroups = Queries.getAwardGroups(connection);

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
        final PdfPTable table = PdfUtils.createTable(NUM_COLMNS);
        table.setWidths(new float[] { 1, 2, 2, 2, 1, 1, 1, 1});

        createHeader(table, challengeTitle, awardGroup, tournament);

        prep.setString(3, awardGroup);

        boolean haveData = false;
        try (ResultSet rs = prep.executeQuery()) {
          while (rs.next()) {
            haveData = true;

            final int teamNumber = rs.getInt(1);
            final String teamName = rs.getString(2);
            final String organization = rs.getString(3);
            final Array scores = rs.getArray(4);
            final double minScore = rs.getDouble(5);
            final double maxScore = rs.getDouble(6);
            final double average = rs.getDouble(7);
            final double stdev = rs.getDouble(8);

            table.addCell(PdfUtils.createCell(String.valueOf(teamNumber)));
            table.addCell(PdfUtils.createCell(null == teamName ? "" : teamName));
            table.addCell(PdfUtils.createCell(null == organization ? "" : organization));

            final String scoresText = scoresToText(rawScoreFormat, scores);
            table.addCell(PdfUtils.createCell(scoresText));

            table.addCell(PdfUtils.createCell(Utilities.getFloatingPointNumberFormat().format(minScore)));
            table.addCell(PdfUtils.createCell(Utilities.getFloatingPointNumberFormat().format(maxScore)));
            table.addCell(PdfUtils.createCell(Utilities.getFloatingPointNumberFormat().format(average)));
            table.addCell(PdfUtils.createCell(Utilities.getFloatingPointNumberFormat().format(stdev)));
          } // foreach result
        } // allocate rs

        if (haveData) {
          table.keepRowsTogether(0);
          pdfDoc.add(table);

          pdfDoc.add(Chunk.NEXTPAGE);
        }

      } // foreach division
    } // allocate prep

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

  private void createHeader(final PdfPTable table,
                            final String challengeTitle,
                            final String awardGroup,
                            final Tournament tournament)
      throws BadElementException {
    final PdfPCell tournamentCell = PdfUtils.createHeaderCell(String.format("%s - %s", challengeTitle,
                                                                            tournament.getDescription()));
    tournamentCell.setColspan(NUM_COLMNS);
    table.addCell(tournamentCell);

    final PdfPCell categoryHeader = PdfUtils.createHeaderCell(String.format("Award Group: %s", awardGroup));

    categoryHeader.setColspan(NUM_COLMNS);
    table.addCell(categoryHeader);

    table.addCell(PdfUtils.createHeaderCell(TournamentSchedule.TEAM_NUMBER_HEADER));
    table.addCell(PdfUtils.createHeaderCell(TournamentSchedule.TEAM_NAME_HEADER));
    table.addCell(PdfUtils.createHeaderCell(TournamentSchedule.ORGANIZATION_HEADER));
    table.addCell(PdfUtils.createHeaderCell("Scores"));
    table.addCell(PdfUtils.createHeaderCell("Min"));
    table.addCell(PdfUtils.createHeaderCell("Max"));
    table.addCell(PdfUtils.createHeaderCell("Avg"));
    table.addCell(PdfUtils.createHeaderCell("Std Dev"));

    table.setHeaderRows(3);
  }

}
