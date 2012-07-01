/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.WebUtils;

/**
 * Commit the schedule in uploadSchedule_schedule to the database for the
 * current tournament.
 */
@WebServlet("/schedule/CommitEventDivisions")
public class CommitEventDivisions extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final DataSource datasource = ApplicationAttributes.getDataSource();

    Connection connection = null;
    try {
      connection = datasource.getConnection();
      final int tournamentID = Queries.getCurrentTournament(connection);

      // can't put types inside a session
      @SuppressWarnings("unchecked")
      final List<EventDivisionInfo> eventDivisionInfo = SessionAttributes.getNonNullAttribute(session,
                                                                                              GatherEventDivisionChanges.EVENT_DIVISION_INFO_KEY,
                                                                                              List.class);
      for (final EventDivisionInfo info : eventDivisionInfo) {
        Queries.updateTeamEventDivision(connection, info.getTeamNumber(), tournamentID, info.getEventDivision());
      }
      session.removeAttribute(GatherEventDivisionChanges.EVENT_DIVISION_INFO_KEY);

      WebUtils.sendRedirect(application, response, "/schedule/CommitSchedule");
      return;
    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new FLLRuntimeException("There was an error talking to the database", e);
    } finally {
      SQLFunctions.close(connection);
    }

  }
}
