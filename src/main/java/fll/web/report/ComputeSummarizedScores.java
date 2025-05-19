/*
 * Copyright (c) 2025 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;

import fll.ScoreStandardization;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.WebUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Compute the summarized scores.
 * 
 * @see ScoreStandardization#computeSummarizedScoresIfNeeded(ServletContext)
 */
@WebServlet("/report/ComputeSummarizedScores")
public class ComputeSummarizedScores extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    ScoreStandardization.computeSummarizedScoresIfNeeded(application);
    SessionAttributes.appendToMessage(session, "<div class='success'>Summarized Scores</div>");
    WebUtils.sendRedirect(response, "/report/index.jsp");
  }

}
