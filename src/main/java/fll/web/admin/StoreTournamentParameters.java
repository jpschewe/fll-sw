/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

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

import fll.Tournament;
import fll.db.TournamentParameters;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Submit action for edit_tournament_parameters.jsp.
 */
@WebServlet("/admin/StoreTournamentParameters")
public class StoreTournamentParameters extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final StringBuilder message = new StringBuilder();
    final String existingMessage = SessionAttributes.getMessage(session);
    if (null != existingMessage) {
      message.append(existingMessage);
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);

      final int numSeedingRounds = Integer.parseInt(request.getParameter("seeding_rounds"));
      TournamentParameters.setNumSeedingRounds(connection, tournament.getTournamentID(), numSeedingRounds);

      final boolean runningHeadToHead = "on".equals(request.getParameter("running_head_to_head"));
      TournamentParameters.setRunningHeadToHead(connection, tournament.getTournamentID(), runningHeadToHead);

      final int performanceAdvancementPercentage = Integer.parseInt(request.getParameter("performance_advancement_percentage"));
      TournamentParameters.setPerformanceAdvancementPercentage(connection, tournament.getTournamentID(),
                                                               performanceAdvancementPercentage);

      if (message.length() == 0) {
        message.append("<p id='success'>Tournament parameters saved</p>");
      }
    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage()
          + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving tournament parameters to the database", sqle);
    }

    session.setAttribute(SessionAttributes.MESSAGE, message.toString());
    response.sendRedirect(response.encodeRedirectURL("edit_tournament_parameters.jsp"));
  }

}
