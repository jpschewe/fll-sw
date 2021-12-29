/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import fll.scheduler.SchedParams;
import fll.scheduler.SubjectiveStation;
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
 * Support for specifySubjectiveStationDurations.jsp.
 */
@WebServlet("/schedule/SpecifySubjectiveStationDurations")
public final class SpecifySubjectiveStationDurations extends BaseFLLServlet {

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

    pageContext.setAttribute("default_duration", SchedParams.DEFAULT_SUBJECTIVE_MINUTES);

    pageContext.setAttribute("subjectiveStations",
                             uploadScheduleData.getColumnInformation().getSubjectiveStationNames());
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

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final List<SubjectiveStation> subjectiveStations = uploadScheduleData.getColumnInformation()
                                                                           .getSubjectiveStationNames().stream() //
                                                                           .map(name -> {
                                                                             final int duration = WebUtils.getIntRequestParameter(request,
                                                                                                                                  String.format("%s:duration",
                                                                                                                                                name));

                                                                             final SubjectiveStation station = new SubjectiveStation(name,
                                                                                                                                     duration);
                                                                             return station;
                                                                           }) //
                                                                           .collect(Collectors.toList());
      uploadScheduleData.setSubjectiveStations(subjectiveStations);

      WebUtils.sendRedirect(application, response, "/schedule/LoadSchedule");
    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    } finally {
      session.setAttribute(UploadScheduleData.KEY, uploadScheduleData);
    }
  }

}
