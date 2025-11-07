/*
 * Copyright (c) 2025 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Tournament;
import fll.Utilities;
import fll.db.TournamentParameters;
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
 * Access to tournament parameters.
 */
@WebServlet("/api/TournamentParameters")
public class TournamentParametersServlet extends HttpServlet {

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
      final Tournament tournament = Tournament.getCurrentTournament(connection);

      final ObjectMapper jsonMapper = Utilities.createJsonMapper();

      response.reset();
      response.setContentType("application/json");
      final PrintWriter writer = response.getWriter();

      final TournamentParametersData data = new TournamentParametersData(TournamentParameters.getRunningHeadToHead(connection,
                                                                                                                   tournament.getTournamentID()));
      jsonMapper.writeValue(writer, data);
    } catch (final SQLException e) {
      throw new FLLRuntimeException(e);
    }

  }

  /**
   * @param runningHead2Head see
   *          {@link TournamentParameters#getRunningHeadToHead(Connection, int)}
   */
  public record TournamentParametersData(boolean runningHead2Head) {
  }

}
