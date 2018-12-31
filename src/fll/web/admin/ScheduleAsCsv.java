/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import fll.db.Queries;
import fll.scheduler.TournamentSchedule;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.WebUtils;

/**
 * @see TournamentSchedule#outputScheduleAsCSV(java.io.OutputStream)
 */
@WebServlet("/admin/ScheduleAsCsv")
public class ScheduleAsCsv extends BaseFLLServlet {

  private static Logger LOGGER = LogUtils.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final int currentTournamentID = Queries.getCurrentTournament(connection);

      if (!TournamentSchedule.scheduleExistsInDatabase(connection, currentTournamentID)) {
        session.setAttribute(SessionAttributes.MESSAGE,
                             "<p class='error'>There is no schedule for this tournament.</p>");
        WebUtils.sendRedirect(application, response, "/admin/index.jsp");
        return;
      }

      final TournamentSchedule schedule = new TournamentSchedule(connection, currentTournamentID);

      response.reset();
      response.setContentType("text/csv");
      response.setHeader("Content-Disposition", "filename=schedule.csv");
      schedule.outputScheduleAsCSV(response.getOutputStream());

    } catch (final SQLException sqle) {
      LOGGER.error(sqle.getMessage(), sqle);
      throw new RuntimeException(sqle);
    }
  }

}
