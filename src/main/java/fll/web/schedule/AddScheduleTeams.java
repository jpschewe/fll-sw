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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import fll.db.Queries;
import fll.scheduler.TeamScheduleInfo;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;

/**
 * Add teams to the database that are in the schedule.
 */
@WebServlet("/schedule/AddScheduleTeams")
public class AddScheduleTeams extends BaseFLLServlet {

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

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final int tournamentID = Queries.getCurrentTournament(connection);

      final Collection<TeamScheduleInfo> missingTeams = uploadScheduleData.getMissingTeams();
      missingTeams.stream().forEach(ti -> {
        try {
          Queries.addTeam(connection, ti.getTeamNumber(), ti.getTeamName(), ti.getOrganization());

          Queries.addTeamToTournament(connection, ti.getTeamNumber(), tournamentID, ti.getAwardGroup(),
                                      ti.getJudgingGroup());
        } catch (final SQLException e) {
          final String message = "Error talking to the database";
          LOGGER.error(message, e);
          throw new FLLInternalException(message, e);
        }
      });

      WebUtils.sendRedirect(application, response, "/schedule/CheckMissingTeams");

    } catch (final SQLException e) {
      final String message = "Error talking to the database";
      LOGGER.error(message, e);
      throw new FLLInternalException(message, e);
    }
  }

}
