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

import fll.Tournament;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.scoreboard.Top10;
import fll.xml.ChallengeDescription;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * PDF of top performance scores organized by judging station.
 */
@WebServlet("/report/TopScoreReportPerJudgingStationPdf")
public class TopScoreReportPerJudgingStationPdf extends TopScoreReportPdf {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    if (PromptSummarizeScores.checkIfSummaryUpdated(request, response, application, session,
                                                    "/report/TopScoreReportPerJudgingStationPdf")) {
      return;
    }

    response.reset();
    response.setContentType("application/pdf");
    response.setHeader("Content-Disposition", "filename=TopScoreReportPerJudgingStation.pdf");

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);
      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

      final Map<String, List<Top10.ScoreEntry>> scores = Top10.getTableAsMapByJudgingStation(connection, description,
                                                                                             tournament);
      outputReport(response.getOutputStream(), challengeDescription, tournament, "Judging Station", scores);
    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    }

  }

}
