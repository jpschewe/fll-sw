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
import fll.xml.SubjectiveScoreCategory;
import fll.xml.WinnerType;

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

      final Document pdfDoc = PdfUtils.createPortraitPdfDoc(response.getOutputStream(), new SimpleFooterHandler());

      generateReport(connection, pdfDoc, challengeDescription, tournament);

      pdfDoc.close();

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    } catch (final DocumentException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressFBWarnings(value = { "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "winner criteria determines sort")
  private void generateReport(final Connection connection,
                              final Document pdfDoc,
                              final ChallengeDescription challengeDescription,
                              final Tournament tournament)
      throws SQLException, DocumentException {

    final String challengeTitle = challengeDescription.getTitle();
    final WinnerType winnerCriteria = challengeDescription.getWinner();

    final List<SubjectiveScoreCategory> subjectiveCategories = challengeDescription.getSubjectiveCategories();
    final Collection<String> eventDivisions = Queries.getAwardGroups(connection);
    final Collection<String> judgingGroups = Queries.getJudgingStations(connection, tournament.getTournamentID());

    for (final SubjectiveScoreCategory catElement : subjectiveCategories) {
      final String catName = catElement.getName();
      final String catTitle = catElement.getTitle();

      // 1 - tournament
      // 2 - category
      // 3 - tournament
      // 4 - award group
      // 5 - judging group
      try (PreparedStatement prep = connection.prepareStatement("SELECT "//
          + " Teams.TeamNumber, Teams.TeamName, Teams.Organization, standardized_score" //
          + " FROM Teams, final_scores" //
          + " WHERE final_scores.tournament = ?" //
          + " AND final_scores.team_number = Teams.TeamNumber" //
          + " AND final_scores.category = ?" //
          + " AND final_scores.goal_group IS NULL"//
          + " AND final_scores.team_number IN (" //
          + "   SELECT TeamNumber FROM TournamentTeams"//
          + "   WHERE Tournament = ?" //
          + "   AND event_division = ?" //
          + "   AND judging_station = ?)" //
          + " ORDER BY standardized_score"
          + " "
          + winnerCriteria.getSortString() //
      )) {
        prep.setInt(1, tournament.getTournamentID());
        prep.setString(2, catName);
        prep.setInt(3, tournament.getTournamentID());

        for (final String division : eventDivisions) {
          for (final String judgingGroup : judgingGroups) {
            final PdfPTable table = PdfUtils.createTable(4);

            createHeader(table, challengeTitle, catTitle, division, judgingGroup, tournament);
            prep.setString(4, division);
            prep.setString(5, judgingGroup);

            boolean haveData = false;
            try (ResultSet rs = prep.executeQuery()) {
              while (rs.next()) {
                haveData = true;

                final int teamNumber = rs.getInt(1);
                final String teamName = rs.getString(2);
                final String organization = rs.getString(3);

                table.addCell(PdfUtils.createCell(String.valueOf(teamNumber)));
                table.addCell(PdfUtils.createCell(null == teamName ? "" : teamName));
                table.addCell(PdfUtils.createCell(null == organization ? "" : organization));
                double score = rs.getDouble(4);
                if (rs.wasNull()) {
                  score = Double.NaN;
                }
                if (Double.isNaN(score)) {
                  table.addCell(PdfUtils.createCell("No Score"));
                } else {
                  table.addCell(PdfUtils.createCell(Utilities.FLOATING_POINT_NUMBER_FORMAT_INSTANCE.format(score)));
                }
              } // foreach result
            } // allocate rs

            if (haveData) {
              table.keepRowsTogether(0);
              pdfDoc.add(table);

              pdfDoc.add(Chunk.NEXTPAGE);
            }

          } // foreach station
        } // foreach division

      } // allocate prep
    } // foreach category

  }

  private void createHeader(final PdfPTable table,
                            final String challengeTitle,
                            final String catTitle,
                            final String division,
                            final String judgingGroup,
                            final Tournament tournament)
      throws BadElementException {
    final PdfPCell tournamentCell = PdfUtils.createHeaderCell(String.format("%s - %s", challengeTitle,
                                                                            tournament.getDescription()));
    tournamentCell.setColspan(4);
    table.addCell(tournamentCell);

    final PdfPCell categoryHeader = PdfUtils.createHeaderCell(String.format("Category: %s - Award Group: %s - JudgingGroup: %s",
                                                                            catTitle, division, judgingGroup));
    categoryHeader.setColspan(4);
    table.addCell(categoryHeader);

    table.addCell(PdfUtils.createHeaderCell(TournamentSchedule.TEAM_NUMBER_HEADER));
    table.addCell(PdfUtils.createHeaderCell(TournamentSchedule.TEAM_NAME_HEADER));
    table.addCell(PdfUtils.createHeaderCell(TournamentSchedule.ORGANIZATION_HEADER));
    table.addCell(PdfUtils.createHeaderCell("Scaled Score"));

    table.setHeaderRows(3);
  }

}
