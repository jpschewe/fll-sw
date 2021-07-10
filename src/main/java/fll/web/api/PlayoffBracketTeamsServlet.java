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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Utilities;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.playoff.Playoff;

/**
 * Map of bracket name to collection of team numbers of teams in the bracket.
 */
@WebServlet("/api/PlayoffBracketTeams")
public class PlayoffBracketTeamsServlet extends HttpServlet {

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response)
      throws IOException, ServletException {
    final ServletContext application = getServletContext();
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.JUDGE, UserRole.REF), false)) {
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

      final Collection<String> playoffBrackets = Playoff.getPlayoffBrackets(connection, tournament);
      final Map<String, Collection<Integer>> playoffBracketTeams = new HashMap<>();
      for (final String playoffBracket : playoffBrackets) {
        final Collection<Integer> teams = Playoff.getTeamNumbersForPlayoffBracket(connection, tournament,
                                                                                  playoffBracket);
        playoffBracketTeams.put(playoffBracket, teams);
      }

      jsonMapper.writeValue(writer, playoffBracketTeams);
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }

  }

}
