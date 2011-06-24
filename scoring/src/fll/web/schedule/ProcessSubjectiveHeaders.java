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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.WebUtils;

/**
 * Processes results of chooseSubjectiveHeaders.jsp and redirects to
 * {@link CheckViolations}.
 * 
 * @web.servlet name="ProcessSubjectiveHeaders"
 * @web.servlet-mapping url-pattern="/schedule/ProcessSubjectiveHeaders"
 */
public class ProcessSubjectiveHeaders extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {

    // J2EE doesn't have things typed yet
    @SuppressWarnings("unchecked")
    final List<String> unusedHeaders = (List<String>) SessionAttributes.getNonNullAttribute(session,
                                                                                            CheckViolations.UNUSED_HEADERS,
                                                                                            List.class);

    // get params for subjectiveHeader
    final List<String> subjectiveHeaders = new LinkedList<String>();
    for (final String str : request.getParameterValues("subjectiveHeader")) {
      final int index = Integer.valueOf(str);
      final String header = unusedHeaders.get(index);
      subjectiveHeaders.add(header);
    }

    session.setAttribute(CheckViolations.SUBJECTIVE_HEADERS, subjectiveHeaders);
    
    WebUtils.sendRedirect(application, response, "/schedule/CheckViolations");
  }
}
