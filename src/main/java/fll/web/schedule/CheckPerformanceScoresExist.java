/*
 * Copyright (c) 2025 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.TournamentData;
import fll.web.WebUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Check if performance scores exist for the current tournament.
 */
@WebServlet("/schedule/CheckPerformanceScoresExist")
public class CheckPerformanceScoresExist extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);

    try (
        PreparedStatement checkPerf = tournamentData.getDataSource().getConnection()
                                                    .prepareStatement("SELECT COUNT(*) FROM performance where tournament = ?")) {
      checkPerf.setInt(1, tournamentData.getCurrentTournament().getTournamentID());
      try (ResultSet rs = checkPerf.executeQuery()) {
        final boolean havePerformanceScores;
        if (rs.next()) {
          final int count = rs.getInt(1);
          havePerformanceScores = count > 0;
        } else {
          havePerformanceScores = false;
        }

        if (havePerformanceScores) {
          WebUtils.sendRedirect(application, response, "/schedule/promptPerformanceScoresExist.jsp");
        } else {
          WebUtils.sendRedirect(application, response, "/schedule/CheckScheduleExists");
        }
      }
    } catch (final SQLException e) {
      throw new FLLInternalException("Error checking for performance scores", e);
    }

  }

}
