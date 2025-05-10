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

import javax.sql.DataSource;

import fll.ScoreStandardization;
import fll.Tournament;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.TournamentData;
import fll.web.scoreboard.Top10;
import fll.xml.ChallengeDescription;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * PDF of top regular match play performance scores organized by award group.
 */
@WebServlet("/report/TopScoreReportPerAwardGroupPdf")
public class TopScoreReportPerAwardGroupPdf extends TopScoreReportPdf {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    response.reset();
    response.setContentType("application/pdf");
    response.setHeader("Content-Disposition", "filename=TopScoreReportPerAwardGroup.pdf");

    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);
    final DataSource datasource = tournamentData.getDataSource();
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    try (Connection connection = datasource.getConnection()) {
      ScoreStandardization.computeSummarizedScoresIfNeeded(connection, description,
                                                          tournamentData.getCurrentTournament());

      final Tournament tournament = tournamentData.getCurrentTournament();

      final Map<String, List<Top10.ScoreEntry>> scores = Top10.getTableAsMapByAwardGroup(connection, description, true,
                                                                                         false);
      outputReport(response.getOutputStream(), description, tournament, "Award Group", scores);
    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    }

  }

}
