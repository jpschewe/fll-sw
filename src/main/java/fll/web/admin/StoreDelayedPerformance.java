/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import fll.Tournament;
import fll.db.DelayedPerformance;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
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
import jakarta.servlet.jsp.PageContext;

/**
 * Store data from /admin/delayed_performance.jsp.
 */
@WebServlet("/admin/StoreDelayedPerformance")
public class StoreDelayedPerformance extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * @param application used to get variables
   * @param pageContext where to store the variables
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);

      final List<DelayedPerformance> delays = DelayedPerformance.loadDelayedPerformances(connection, tournament);
      pageContext.setAttribute("delays", delays);
    } catch (final SQLException sqle) {
      LOGGER.error(sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    }
  }

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

    final StringBuilder message = new StringBuilder();
    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    if (null == request.getParameter("numRows")) {
      message.append("<p class='error'>numRows parameter missing.</p>");
    } else {
      try (Connection connection = datasource.getConnection()) {

        createAndInsertPerformanceDelays(request, connection);

        message.append("<p id='success'>Committed performance delay changes.</p>");
      } catch (final SQLException e) {
        LOGGER.error("Error talking to the database", e);
        throw new FLLInternalException(e);
      }
    }

    SessionAttributes.appendToMessage(session, message.toString());

    // finally redirect to index.jsp
    response.sendRedirect(response.encodeRedirectURL("index.jsp"));
  }

  private static void createAndInsertPerformanceDelays(final HttpServletRequest request,
                                                       final Connection connection)
      throws SQLException {
    final String numRowsStr = WebUtils.getNonNullRequestParameter(request, "numRows");

    final int numRows = Integer.parseInt(numRowsStr);

    final Tournament currentTournament = Tournament.getCurrentTournament(connection);

    final List<DelayedPerformance> delays = new LinkedList<>();

    for (int row = 0; row < numRows; ++row) {
      final String runNumberStr = request.getParameter("runNumber"
          + row);
      final String dateTimeStr = request.getParameter("datetime"
          + row);

      if (null != runNumberStr
          && null != dateTimeStr) {
        final int runNumber = Integer.parseInt(runNumberStr);

        final LocalDateTime delayUntil = LocalDateTime.parse(dateTimeStr, Tournaments.DATE_TIME_FORMATTER);

        final DelayedPerformance delay = new DelayedPerformance(runNumber, delayUntil);
        delays.add(delay);
      }
    }

    DelayedPerformance.storeDelayedPerformances(connection, currentTournament, delays);
  }

}
