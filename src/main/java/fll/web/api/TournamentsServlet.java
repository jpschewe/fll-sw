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
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Tournament;
import fll.Utilities;
import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.SessionAttributes;
import fll.web.UserRole;

/**
 * Get list of {@link Tournament} objects.
 */
@WebServlet("/api/Tournaments/*")
public class TournamentsServlet extends HttpServlet {

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response)
      throws IOException, ServletException {
    final HttpSession session = request.getSession();
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.PUBLIC), false)) {
      return;
    }

    final ServletContext application = getServletContext();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final ObjectMapper jsonMapper = Utilities.createJsonMapper();

      response.reset();
      response.setContentType("application/json");
      final PrintWriter writer = response.getWriter();

      final String pathInfo = request.getPathInfo();
      if (null != pathInfo
          && pathInfo.length() > 1) {
        final String tournamentStr = pathInfo.substring(1);

        int id;
        if ("current".equals(tournamentStr)) {
          id = Queries.getCurrentTournament(connection);
        } else {
          try {
            id = Integer.parseInt(tournamentStr);
          } catch (final NumberFormatException e) {
            throw new RuntimeException("Error parsing tournament id "
                + tournamentStr, e);
          }
        }

        final Tournament tournament = Tournament.findTournamentByID(connection, id);
        jsonMapper.writeValue(writer, tournament);
        return;

      }

      final Collection<Tournament> tournaments = Tournament.getTournaments(connection);

      jsonMapper.writeValue(writer, tournaments);
    } catch (final SQLException e) {
      throw new FLLRuntimeException(e);
    }

  }

  /**
   * Type information for reading a collection of tournament objects.
   */
  public static final class TournamentsTypeInformation extends TypeReference<Collection<Tournament>> {
    /**
     * Singleton instance.
     */
    public static final TournamentsTypeInformation INSTANCE = new TournamentsTypeInformation();
  }

}
