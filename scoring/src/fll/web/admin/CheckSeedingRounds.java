/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fll.Team;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.playoff.PlayoffIndex;

/**
 * Check seeding round information for teams. Redirects to
 * checkSeedingRoundsResult.jsp.
 */
@WebServlet("/playoff/CheckSeedingRounds")
public class CheckSeedingRounds extends BaseFLLServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(CheckSeedingRounds.class);

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    Connection connection = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();
      final StringBuilder message = new StringBuilder();
      if (null != SessionAttributes.getMessage(session)) {
        message.append(SessionAttributes.getMessage(session));
      }

      final String division = request.getParameter("division");

      final Map<Integer, Team> tournamentTeams = Queries.getTournamentTeams(connection);

      session.setAttribute("division", division);
      
      if(PlayoffIndex.CREATE_NEW_PLAYOFF_DIVISION.equals(division)) {
        // FIXME create new division and come back here when done
        
      }

      if (Queries.isPlayoffDataInitialized(connection, division)) {
        message.append("<p class='warning'>Playoffs have already been initialized for this division.</p>");
        session.setAttribute("less", Collections.emptyList());
        session.setAttribute("more", Collections.emptyList());
      } else {
        final List<Team> less = Queries.getTeamsNeedingSeedingRuns(connection, tournamentTeams, division, true);
        session.setAttribute("less", less);

        final List<Team> more = Queries.getTeamsWithExtraRuns(connection, tournamentTeams, division, true);
        session.setAttribute("more", more);
      }

      session.setAttribute(SessionAttributes.MESSAGE, message.toString());
      response.sendRedirect(response.encodeRedirectURL("checkSeedingRoundsResult.jsp"));

    } catch (final SQLException e) {
      LOGGER.error("Error talking to the database", e);
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(connection);
    }

  }

}
