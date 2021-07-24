/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Utilities;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.SessionAttributes;

/**
 * Map of team number to score.
 */
@WebServlet("/api/OverallScores")
public class OverallScoresServlet extends HttpServlet {

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response)
      throws IOException, ServletException {
    final ServletContext application = getServletContext();
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.isRef()
        && !auth.isJudge()) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final ObjectMapper jsonMapper = Utilities.createJsonMapper();

      response.reset();
      response.setContentType("application/json");
      final PrintWriter writer = response.getWriter();

      final int tournament = Queries.getCurrentTournament(connection);

      final Map<Integer, Double> scores = new HashMap<>();
      try (
          PreparedStatement prep = connection.prepareStatement("SELECT team_number, overall_score from overall_scores WHERE tournament = ?")) {
        prep.setInt(1, tournament);

        try (ResultSet rs = prep.executeQuery()) {
          while (rs.next()) {
            final int teamNumber = rs.getInt(1);
            final double overallScore = rs.getDouble(2);

            scores.put(teamNumber, overallScore);
          } // foreach team
        } // result set
      } // prepared statement

      jsonMapper.writeValue(writer, scores);
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }

  }

}
