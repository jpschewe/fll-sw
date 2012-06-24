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

import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Index page for admin.
 */
@WebServlet("/admin/index.jsp")
public class AdminIndex extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final StringBuilder message = new StringBuilder();
    final String existingMessage = SessionAttributes.getMessage(session);
    if (null != existingMessage) {
      message.append(existingMessage);
    }

    final DataSource datasource = SessionAttributes.getDataSource(session);
    try {
      final Connection connection = datasource.getConnection();
      final int currentTournamentID = Queries.getCurrentTournament(connection);

      session.setAttribute("playoffsInitialized",
                           Queries.isPlayoffDataInitialized(connection, Queries.getCurrentTournament(connection)));

      final int scoresheetsPerPage = Queries.getScoresheetLayoutNUp(connection);
      session.setAttribute("scoressheetsPerPage", scoresheetsPerPage);
      
      final int numSeedingRounds = Queries.getNumSeedingRounds(connection, currentTournamentID);
      session.setAttribute("numSeedingRounds", numSeedingRounds);

      session.setAttribute("servletLoaded", true);
      
    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage() + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving team data into the database", sqle);
    }
    session.setAttribute(SessionAttributes.MESSAGE, message.toString());
    response.sendRedirect(response.encodeRedirectURL("admin-index.jsp"));
  }

}
