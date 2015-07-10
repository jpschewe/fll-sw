/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import net.mtu.eggplant.util.sql.SQLFunctions;
import fll.Tournament;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.WebUtils;

/**
 * Support for and handle the result from promptSummarizeScores.jsp.
 */
@WebServlet("/report/PromptSummarizeScores")
public class PromptSummarizeScores extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

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
      WebUtils.sendRedirect(application, response, "summarizePhase1.jsp");
    } else {
      final String url = SessionAttributes.getAttribute(session, SUMMARY_REDIRECT_KEY, String.class);
      WebUtils.sendRedirect(application, response, url);
    }
  }

  /**
   * Check if summary scores need to be updated. If they do, redirect and set
   * the session variable SUMMARY_REDIRECT to point to
   * redirect.
   * 
   * @param redirect the page to visit once the scores have been summarized
   * @return if the summary scores need to be updated, the calling method should
   *         return immediately if this is true as a redirect has been executed.
   */
  public static boolean checkIfSummaryUpdated(final HttpServletResponse response,
                                              final ServletContext application,
                                              final HttpSession session,
                                              final String redirect) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("top check if summary updated");
    }
  
    if (null != session.getAttribute(SUMMARY_CHECKED_KEY)) {
      LOGGER.info("summary checked");
  
      // alredy checked, can just continue
      return false;
    }
  
    Connection connection = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();
  
      final int tournamentId = Queries.getCurrentTournament(connection);
      final Tournament tournament = Tournament.findTournamentByID(connection, tournamentId);
  
      if (tournament.checkTournamentNeedsSummaryUpdate(connection)) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Needs summary update");
        }
  
        session.setAttribute(SUMMARY_REDIRECT_KEY, redirect);
        WebUtils.sendRedirect(application, response, "promptSummarizeScores.jsp");
        return true;
      } else {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("No updated needed");
        }
  
        return false;
      }
  
    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new RuntimeException(e);
    } catch (IOException e) {
      LOGGER.error(e, e);
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(connection);
    }
  
  }

}
