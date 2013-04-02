/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.finalist;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import fll.util.FLLRuntimeException;

/**
 * Outputs the PDF showing times of all finalist categories.
 */
@WebServlet("/report/finalist/PublicFinalistSchedule")
public class PublicFinalistSchedule extends AbstractFinalistSchedule {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final String division = request.getParameter("division");
    if (null == division
        || "".equals(division)) {
      throw new FLLRuntimeException("Parameter 'division' cannot be null");
    }

    processRequest(division, false, response, application);
  }

}
