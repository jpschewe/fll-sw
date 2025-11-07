/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.IOException;
import java.util.Set;

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

/**
 * Process the results of scheduleConstraints.jsp.
 */
@WebServlet("/schedule/ProcessScheduleConstraints")
public class ProcessScheduleConstraints extends BaseFLLServlet {

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
    try {
      final int changeTimeDuration = WebUtils.getIntRequestParameter(request, "changeTimeDuration");
      uploadScheduleData.getSchedParams().setChangetimeMinutes(changeTimeDuration);

      final int performanceChangeTimeDuration = WebUtils.getIntRequestParameter(request,
                                                                                "performanceChangeTimeDuration");
      uploadScheduleData.getSchedParams().setPerformanceChangetimeMinutes(performanceChangeTimeDuration);

      final int subjectiveChangeTimeDuration = WebUtils.getIntRequestParameter(request, "subjectiveChangeTimeDuration");
      uploadScheduleData.getSchedParams().setSubjectiveChangetimeMinutes(subjectiveChangeTimeDuration);

      final int performanceDuration = WebUtils.getIntRequestParameter(request, "performanceDuration");
      uploadScheduleData.getSchedParams().setPerformanceMinutes(performanceDuration);

      final int numPerformanceRuns = WebUtils.getIntRequestParameter(request, "numPerformanceRuns");
      uploadScheduleData.setNumPerformanceRuns(numPerformanceRuns);

      WebUtils.sendRedirect(application, response, "chooseScheduleHeaders.jsp");
    } finally {
      session.setAttribute(UploadScheduleData.KEY, uploadScheduleData);
    }

  }

}
