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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Utilities;
import fll.db.Queries;
import fll.scheduler.TournamentSchedule;
import fll.web.ApplicationAttributes;

/**
 * API access to the tournament schedule.
 */
@WebServlet("/api/Schedule/*")
public class ScheduleServlet extends HttpServlet {

  @Override
  protected final void doGet(final HttpServletRequest request,
                             final HttpServletResponse response)
      throws IOException, ServletException {
    final ServletContext application = getServletContext();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final ObjectMapper jsonMapper = Utilities.createJsonMapper();

      response.reset();
      response.setContentType("application/json");
      final PrintWriter writer = response.getWriter();

      final int currentTournament = Queries.getCurrentTournament(connection);
      if (TournamentSchedule.scheduleExistsInDatabase(connection, currentTournament)) {
        final TournamentSchedule schedule = new TournamentSchedule(connection, currentTournament);

        jsonMapper.writeValue(writer, schedule);
      } else {
        jsonMapper.writeValue(writer, null);
      }

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }

  }

}
