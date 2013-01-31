/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.report;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;

import fll.Tournament;
import fll.Utilities;
import fll.db.Queries;
import fll.scheduler.TournamentSchedule;
import fll.util.PdfUtils;
import fll.util.SimpleFooterHandler;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreCategory;
import fll.xml.WinnerType;

/**
 * Display the report for scores by score group.
 * 
 * @author jpschewe
 */
@WebServlet("/report/CategoryScoresByScoreGroup")
public class CategoryScoresByScoreGroup extends BaseFLLServlet {

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    Connection connection = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();
      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
      final int tournamentID = Queries.getCurrentTournament(connection);
      final Tournament tournament = Tournament.findTournamentByID(connection, tournamentID);
      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition", "filename=categoryScoresByJudgingStation.pdf");

      final Document pdfDoc = PdfUtils.createPdfDoc(response.getOutputStream(), new SimpleFooterHandler());

      generateReport(connection, pdfDoc, challengeDescription, tournament);

      pdfDoc.close();

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    } catch (final DocumentException e) {
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(connection);
    }
  }

  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "Category name determines table column, winner criteria determines sort")
  private void generateReport(final Connection connection,
                              final Document pdfDoc,
                              final ChallengeDescription challengeDescription,
                              final Tournament tournament) throws SQLException, DocumentException {

    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      final String challengeTitle = challengeDescription.getTitle();
      final WinnerType winnerCriteria = challengeDescription.getWinner();

      final List<ScoreCategory> subjectiveCategories = challengeDescription.getSubjectiveCategories();
      final Collection<String> eventDivisions = Queries.getEventDivisions(connection);
      final Collection<String> judgingStations = Queries.getJudgingStations(connection, tournament.getTournamentID());

      for (final ScoreCategory catElement : subjectiveCategories) {
        final String catName = catElement.getName();
        final String catTitle = catElement.getTitle();

        for (final String division : eventDivisions) {
          for (final String judgingStation : judgingStations) {
            final PdfPTable table = PdfUtils.createTable(4);

            createHeader(table, challengeTitle, catTitle, division, judgingStation, tournament);

            prep = connection.prepareStatement("SELECT "//
                + " Teams.TeamNumber, Teams.TeamName, Teams.Organization, FinalScores." + catName //
                + " FROM Teams, FinalScores" //
                + " WHERE FinalScores.Tournament = ?" //
                + " AND FinalScores.TeamNumber = Teams.TeamNumber" + " AND FinalScores.TeamNumber IN (" //
                + "   SELECT TeamNumber FROM TournamentTeams"//
                + "   WHERE Tournament = ?" //
                + "   AND event_division = ?" //
                + "   AND judging_station = ?)" //
                + " ORDER BY " + catName + " " + winnerCriteria.getSortString() //
            );
            prep.setInt(1, tournament.getTournamentID());
            prep.setInt(2, tournament.getTournamentID());
            prep.setString(3, division);
            prep.setString(4, judgingStation);

            rs = prep.executeQuery();
            while (rs.next()) {
              table.addCell(PdfUtils.createCell(String.valueOf(rs.getInt(1))));
              table.addCell(PdfUtils.createCell(rs.getString(2)));
              table.addCell(PdfUtils.createCell(rs.getString(3)));
              double score = rs.getDouble(4);
              if (rs.wasNull()) {
                score = Double.NaN;
              }
              if (Double.isNaN(score)) {
                table.addCell(PdfUtils.createCell("No Score"));
              } else {
                table.addCell(PdfUtils.createCell(Utilities.NUMBER_FORMAT_INSTANCE.format(score)));
              }
            }

            pdfDoc.add(table);

            pdfDoc.add(new Paragraph(Chunk.NEWLINE));

          } // foreach station
        } // foreach division
      } // foreach category

    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }

  }

  private void createHeader(final PdfPTable table,
                            final String challengeTitle,
                            final String catTitle,
                            final String division,
                            final String judgingStation,
                            final Tournament tournament) throws BadElementException {
    final PdfPCell tournamentCell = PdfUtils.createHeaderCell(String.format("%s - %s", challengeTitle,
                                                                            tournament.getName()));
    tournamentCell.setColspan(4);
    table.addCell(tournamentCell);

    final PdfPCell categoryHeader = PdfUtils.createHeaderCell(String.format("Category: %s - Division: %s - JudgingStation: %s",
                                                                            catTitle, division, judgingStation));
    categoryHeader.setColspan(4);
    table.addCell(categoryHeader);

    table.addCell(PdfUtils.createHeaderCell(TournamentSchedule.TEAM_NUMBER_HEADER));
    table.addCell(PdfUtils.createHeaderCell(TournamentSchedule.TEAM_NAME_HEADER));
    table.addCell(PdfUtils.createHeaderCell(TournamentSchedule.ORGANIZATION_HEADER));
    table.addCell(PdfUtils.createHeaderCell("Scaled Score"));

    table.setHeaderRows(3);
  }

}
