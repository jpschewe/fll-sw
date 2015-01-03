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
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.Tournament;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.WebUtils;

/**
 * Support for report/getPerformancePercentage.jsp.
 */
public class GetPerformancePercentage {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Check if summary scores need to be updated. If they do, redirect and set
   * the session variable SUMMARY_REDIRECT to point to
   * getPerformancePercentage.jsp.
   * 
   * @return if the summary scores need to be updated, the calling method should
   *         return immediately if this is true as a redirect has been executed.
   */
  public static boolean checkIfSummaryUpdated(final HttpServletResponse response,
                                              final ServletContext application,
                                              final HttpSession session) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("top check if summary updated");
    }

    if (null != session.getAttribute(PromptSummarizeScores.SUMMARY_CHECKED_KEY)) {
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

        session.setAttribute(PromptSummarizeScores.SUMMARY_REDIRECT_KEY, "/report/getPerformancePercentage.jsp");
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
