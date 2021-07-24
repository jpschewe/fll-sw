/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
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

import fll.TournamentTeam;
import fll.Utilities;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.SessionAttributes;

/**
 * Access the current tournament teams.
 */
@WebServlet("/api/TournamentTeams/*")
public class TournamentTeamsServlet extends HttpServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response)
      throws IOException, ServletException {
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.isPublic()) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    final ServletContext application = getServletContext();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final Map<Integer, TournamentTeam> teamMap = Queries.getTournamentTeams(connection);
      final ObjectMapper jsonMapper = Utilities.createJsonMapper();

      response.reset();
      response.setContentType("application/json");
      final PrintWriter writer = response.getWriter();

      final String pathInfo = request.getPathInfo();
      if (null != pathInfo
          && pathInfo.length() > 1) {
        final String teamNumberStr = pathInfo.substring(1);
        try {
          final int teamNumber = Integer.parseInt(teamNumberStr);
          LOGGER.info("Found team number: "
              + teamNumber);
          final TournamentTeam team = teamMap.get(teamNumber);
          if (null != team) {
            jsonMapper.writeValue(writer, team);
            return;
          } else {
            throw new RuntimeException("No team found with number "
                + teamNumber);
          }
        } catch (final NumberFormatException e) {
          throw new RuntimeException("Error parsing team number "
              + teamNumberStr, e);
        }
      }

      final Collection<TournamentTeam> teams = teamMap.values();

      jsonMapper.writeValue(writer, teams);
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }

  }

}
