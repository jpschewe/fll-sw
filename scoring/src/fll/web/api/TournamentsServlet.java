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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import com.google.gson.Gson;

import fll.Tournament;
import fll.TournamentTeam;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;

@WebServlet("/api/Tournaments/*")
public class TournamentsServlet extends HttpServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response) throws IOException, ServletException {
    final ServletContext application = getServletContext();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final Gson gson = new Gson();

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
            id = Integer.valueOf(tournamentStr);
          } catch (final NumberFormatException e) {
            throw new RuntimeException("Error parsing tournament id "
                + tournamentStr, e);
          }
        }

        final Tournament tournament = Tournament.findTournamentByID(connection, id);
        if (null != tournament) {
          final String json = gson.toJson(tournament);
          writer.print(json);
          return;
        } else {
          throw new RuntimeException("No tournament found with id "
              + id);
        }

      }

      final Collection<Tournament> tournaments = Tournament.getTournaments(connection);

      final String json = gson.toJson(tournaments);

      writer.print(json);

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(connection);
    }

  }

}
