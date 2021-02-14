/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
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
import fll.scheduler.ConstraintViolation;
import fll.scheduler.SchedParams;
import fll.scheduler.ScheduleChecker;
import fll.scheduler.TournamentSchedule;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;

/**
 * Read the uploaded file, then check for constraint
 * violations. Stores the schedule in
 * {@link UploadScheduleData#setSchedule(TournamentSchedule)}.
 */
@WebServlet("/schedule/CheckViolations")
public class CheckViolations extends BaseFLLServlet {

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

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final int tournamentID = Queries.getCurrentTournament(connection);
      final Collection<ConstraintViolation> violations = schedule.compareWithDatabase(connection, tournamentID);
      final SchedParams schedParams = uploadScheduleData.getSchedParams();
      final ScheduleChecker checker = new ScheduleChecker(schedParams, schedule);
      violations.addAll(checker.verifySchedule());
      if (violations.isEmpty()) {
        WebUtils.sendRedirect(application, response, "/schedule/GatherTeamInformationChanges");
        return;
      } else {
        uploadScheduleData.setViolations(violations);
        for (final ConstraintViolation violation : violations) {
          if (ConstraintViolation.Type.HARD == violation.getType()) {
            WebUtils.sendRedirect(application, response, "/schedule/displayHardViolations.jsp");
            return;
          }
        }
        WebUtils.sendRedirect(application, response, "/schedule/displaySoftViolations.jsp");
        return;
      }

    } catch (final SQLException e) {
      final String message = "Error talking to the database";
      LOGGER.error(message, e);
      throw new FLLRuntimeException(message, e);
    } finally {
      session.setAttribute(UploadScheduleData.KEY, uploadScheduleData);
    }
  }

}
