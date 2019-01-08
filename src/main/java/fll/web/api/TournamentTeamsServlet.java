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

import com.fasterxml.jackson.databind.ObjectMapper;

import fll.TournamentTeam;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;

@WebServlet("/api/TournamentTeams/*")
public class TournamentTeamsServlet extends HttpServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response) throws IOException, ServletException {
    final ServletContext application = getServletContext();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final Map<Integer, TournamentTeam> teamMap = Queries.getTournamentTeams(connection);
      final ObjectMapper jsonMapper = new ObjectMapper();

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
    } finally {
      SQLFunctions.close(connection);
    }

  }

}
