/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.sql.DataSource;

import fll.Tournament;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;

/**
 * Support for and handle the result from promptSummarizeScores.jsp.
 */
@WebServlet("/report/PromptSummarizeScores")
public class PromptSummarizeScores extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Session variable key for the URL to redirect to after score summarization.
   */
  public static final String SUMMARY_REDIRECT_KEY = "summary_redirect";

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.HEAD_JUDGE), false)) {
      return;
    }

    if (null != request.getParameter("recompute")) {
      WebUtils.sendRedirect(application, response, "summarizePhase1.jsp");
    } else {
      final String url = SessionAttributes.getAttribute(session, SUMMARY_REDIRECT_KEY, String.class);
      LOGGER.debug("redirect is {}", url);

      if (null == url) {
        WebUtils.sendRedirect(application, response, "index.jsp");
      } else {
        WebUtils.sendRedirect(application, response, url);
      }
    }
  }

  /**
   * Check if summary scores need to be updated. If they do, redirect and set
   * the session variable SUMMARY_REDIRECT to point to
   * redirect.
   *
   * @param response used to send a redirect
   * @param application the application context
   * @param session the session context
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

    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);
    if (!auth.isHeadJudge()) {
      // only the head judge can summarize scores, so don't prompt others to summarize
      // the scores
      return false;
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final int tournamentId = Queries.getCurrentTournament(connection);
      final Tournament tournament = Tournament.findTournamentByID(connection, tournamentId);

      if (tournament.checkTournamentNeedsSummaryUpdate(connection)) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Needs summary update");
        }

        if (null != session.getAttribute(SUMMARY_REDIRECT_KEY)) {
          LOGGER.debug("redirect has already been set, it must be the case that the user is skipping summarization, allow it");
          return false;
        } else {
          session.setAttribute(SUMMARY_REDIRECT_KEY, redirect);
          WebUtils.sendRedirect(application, response, "/report/promptSummarizeScores.jsp");
          return true;
        }
      } else {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("No updated needed");
        }

        return false;
      }

    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new RuntimeException(e);
    } catch (final IOException e) {
      LOGGER.error(e, e);
      throw new RuntimeException(e);
    }

  }

}
