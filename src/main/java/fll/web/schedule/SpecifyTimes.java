/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import fll.scheduler.TournamentSchedule;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;

/**
 * Support for specifyTimes.jsp.
 */
@WebServlet("/schedule/SpecifyTimes")
public final class SpecifyTimes extends BaseFLLServlet {

  /**
   * Setup page variables used by the JSP.
   * 
   * @param session session variables
   * @param pageContext set page variables
   */
  public static void populateContext(final HttpSession session,
                                     final PageContext pageContext) {

    final UploadScheduleData uploadScheduleData = SessionAttributes.getNonNullAttribute(session, UploadScheduleData.KEY,
                                                                                        UploadScheduleData.class);

    final TournamentSchedule schedule = uploadScheduleData.getSchedule();
    if (null == schedule) {
      throw new FLLInternalException("Schedule is null, something is wrong with the workflow");
    }
    pageContext.setAttribute("waves", schedule.getAllWaves());
  }

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
      throw new FLLInternalException("Schedule is null, something is wrong with the workflow");
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Collection<TournamentSchedule.WaveCheckin> checkins = //
          schedule.getAllWaves().stream() //
                  .map(wave -> {
                    final LocalTime checkin = WebUtils.getTimeRequestParameter(request,
                                                                               String.format("%s:checkin_time", wave));

                    return new TournamentSchedule.WaveCheckin(wave, checkin);
                  }) //
                  .collect(Collectors.toList());
      schedule.setWaveCheckinTimes(checkins);

      WebUtils.sendRedirect(application, response, "/schedule/CheckMissingTeams");
    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    } finally {
      session.setAttribute(UploadScheduleData.KEY, uploadScheduleData);
    }
  }

}
