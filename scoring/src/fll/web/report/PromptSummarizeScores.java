/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;

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
 * Support for and handle the result from promptSummarizeScores.jsp.
 */
@WebServlet("/report/PromptSummarizeScores")
public class PromptSummarizeScores extends BaseFLLServlet {

  /**
   * Session variable key for the URL to redirect to after score summarization.
   */
  public static final String SUMMARY_REDIRECT_KEY = "summary_redirect";

  /**
   * Session variable key to note that we have checked if the user wants to
   * recompute summarized scores.
   */
  public static final String SUMMARY_CHECKED_KEY = "summary_checked";

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {

    session.setAttribute(SUMMARY_CHECKED_KEY, true);

    if (null != request.getParameter("recompute")) {
      WebUtils.sendRedirect(application, response, "SummarizePhase1");
    } else {
      final String url = SessionAttributes.getAttribute(session, SUMMARY_REDIRECT_KEY, String.class);
      WebUtils.sendRedirect(application, response, url);
    }
  }

}
