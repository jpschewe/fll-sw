/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.IOException;
import java.util.Objects;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.WebUtils;

/**
 * Process the results of scheduleConstraints.jsp.
 */
@WebServlet("/schedule/ProcessScheduleConstraints")
public class ProcessScheduleConstraints extends BaseFLLServlet {

  @Override
  protected void processRequest(HttpServletRequest request,
                                HttpServletResponse response,
                                ServletContext application,
                                HttpSession session)
      throws IOException, ServletException {

    final UploadScheduleData uploadScheduleData = SessionAttributes.getNonNullAttribute(session, UploadScheduleData.KEY,
                                                                                        UploadScheduleData.class);
    try {

      final String changeTimeDurationStr = request.getParameter("changeTimeDuration");
      Objects.requireNonNull(changeTimeDurationStr, "Missing parameter 'changeTimeDuration'");

      final int changeTimeDuration = Integer.parseInt(changeTimeDurationStr);
      uploadScheduleData.getSchedParams().setChangetimeMinutes(changeTimeDuration);

      final String performanceChangeTimeDurationStr = request.getParameter("performanceChangeTimeDuration");
      Objects.requireNonNull(performanceChangeTimeDurationStr, "Missing parameter 'performanceChangeTimeDuration'");

      final int performanceChangeTimeDuration = Integer.parseInt(performanceChangeTimeDurationStr);
      uploadScheduleData.getSchedParams().setPerformanceChangetimeMinutes(performanceChangeTimeDuration);

      final String performanceDurationStr = request.getParameter("performanceDuration");
      Objects.requireNonNull(performanceDurationStr, "Missing parameter 'performanceDuration'");

      final int performanceDuration = Integer.parseInt(performanceDurationStr);
      uploadScheduleData.getSchedParams().setPerformanceMinutes(performanceDuration);

      WebUtils.sendRedirect(application, response, "CheckViolations");
      
    } finally {
      session.setAttribute(UploadScheduleData.KEY, uploadScheduleData);
    }

  }

}
