/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.sql.DataSource;

import fll.db.Queries;
import fll.scheduler.TeamScheduleInfo;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;

/**
 * Check for teams that are in the schedule and not in the database.
 */
@WebServlet("/schedule/CheckMissingTeams")
public class CheckMissingTeams extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.ADMIN), false)) {
      return;
    }

    final UploadScheduleData uploadScheduleData = SessionAttributes.getNonNullAttribute(session, UploadScheduleData.KEY,
                                                                                        UploadScheduleData.class);

    final TournamentSchedule schedule = uploadScheduleData.getSchedule();
    if (null == schedule) {
      throw new FLLInternalException("Schedule is not set");
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final int tournamentID = Queries.getCurrentTournament(connection);

      final Collection<TeamScheduleInfo> missingTeams = schedule.findTeamsNotInDatabase(connection, tournamentID);
      if (!missingTeams.isEmpty()) {
        uploadScheduleData.setMissingTeams(missingTeams);
        WebUtils.sendRedirect(application, response, "/schedule/promptAddScheduleTeams.jsp");
      } else {
        WebUtils.sendRedirect(application, response, "/schedule/CheckViolations");
      }
    } catch (final SQLException e) {
      final String message = "Error talking to the database";
      LOGGER.error(message, e);
      throw new FLLInternalException(message, e);
    } finally {
      session.setAttribute(UploadScheduleData.KEY, uploadScheduleData);
    }

  }

}
