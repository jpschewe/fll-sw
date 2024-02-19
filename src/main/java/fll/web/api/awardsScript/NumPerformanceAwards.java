/*
 * Copyright (c) 2024 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.api.awardsScript;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Tournament;
import fll.Utilities;
import fll.db.AwardsScript;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.SessionAttributes;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Get the number of performance awards.
 */
@WebServlet("/api/AwardsScript/NumPerformanceAwards")
public class NumPerformanceAwards extends HttpServlet {

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response)
      throws IOException, ServletException {
    final ServletContext application = getServletContext();
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.isPublic()) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final ObjectMapper jsonMapper = Utilities.createJsonMapper();

      response.reset();
      response.setContentType("application/json");
      final PrintWriter writer = response.getWriter();

      final Tournament tournament = Tournament.getCurrentTournament(connection);
      final int numPerformanceAwards = AwardsScript.getNumPerformanceAwardsForTournament(connection, tournament);
      jsonMapper.writeValue(writer, numPerformanceAwards);
    } catch (final SQLException e) {
      throw new FLLRuntimeException(e);
    }

  }

}
