/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import javax.sql.DataSource;

import fll.Tournament;
import fll.scheduler.ScheduleWriter;
import fll.scheduler.TournamentSchedule;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.TournamentData;
import fll.web.UserRole;
import fll.web.WebUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * @see ScheduleWriter#outputScheduleByWaveAndTeam(TournamentData,
 *      TournamentSchedule, java.io.OutputStream)
 */
@WebServlet("/admin/ScheduleByWaveAndTeam")
public class ScheduleByWaveAndTeam extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.PUBLIC), false)) {
      return;
    }

    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);
    final DataSource datasource = tournamentData.getDataSource();
    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = tournamentData.getCurrentTournament();

      if (!TournamentSchedule.scheduleExistsInDatabase(connection, tournament.getTournamentID())) {
        SessionAttributes.appendToMessage(session, "<p class='error'>There is no schedule for this tournament.</p>");
        WebUtils.sendRedirect(application, response, "/admin/index.jsp");
        return;
      }

      final TournamentSchedule schedule = new TournamentSchedule(connection, tournament.getTournamentID());

      response.reset();
      response.setContentType("application/pdf");
      response.setHeader("Content-Disposition",
                         String.format("attachment; filename=\"%s_schedule-by-wave-and-team.pdf\"",
                                       tournament.getName()));
      ScheduleWriter.outputScheduleByWaveAndTeam(tournamentData, schedule, response.getOutputStream());

    } catch (final SQLException sqle) {
      LOGGER.error(sqle.getMessage(), sqle);
      throw new RuntimeException(sqle);
    }
  }

}
