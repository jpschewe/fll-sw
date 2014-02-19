/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import fll.scheduler.SubjectiveStation;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.WebUtils;

/**
 * Processes results of chooseSubjectiveHeaders.jsp and redirects to
 * {@link CheckViolations}.
 */
@WebServlet("/schedule/ProcessSubjectiveHeaders")
public class ProcessSubjectiveHeaders extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {

    // J2EE doesn't have things typed yet
    @SuppressWarnings("unchecked")
    final List<String> unusedHeaders = SessionAttributes.getNonNullAttribute(session, CheckViolations.UNUSED_HEADERS,
                                                                             List.class);

    // get params for subjectiveHeader
    final List<SubjectiveStation> subjectiveStations = new LinkedList<SubjectiveStation>();
    for (final String str : request.getParameterValues("subjectiveHeader")) {
      final int index = Integer.parseInt(str);
      final String header = unusedHeaders.get(index);
      final String durationStr = request.getParameter("duration_"
          + index);
      subjectiveStations.add(new SubjectiveStation(header, Integer.parseInt(durationStr)));
    }

    session.setAttribute(CheckViolations.SUBJECTIVE_STATIONS, subjectiveStations);

    WebUtils.sendRedirect(application, response, "/schedule/CheckViolations");
  }
}
