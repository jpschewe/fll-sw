/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import fll.Team;
import fll.db.Queries;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Advance teams to the next tournament.
 * @web.servlet name="AdvanceTeams"
 * @web.servlet-mapping url-pattern="/admin/AdvanceTeams"
 */
public class AdvanceTeams extends BaseFLLServlet {

  private static final Logger LOGGER = Logger.getLogger(AdvanceTeams.class);

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Top of AdvanceTeams.doPost");
    }

    final StringBuilder message = new StringBuilder();
    final DataSource datasource = (DataSource) session.getAttribute(SessionAttributes.DATASOURCE);

    // can't put types inside a session
    @SuppressWarnings("unchecked")
    final List<Team> teamsToAdvance = SessionAttributes.getNonNullAttribute(session, "advancingTeams", List.class);

    try {
      final Connection connection = datasource.getConnection();

      for(final Team team : teamsToAdvance) {
        Queries.advanceTeam(connection, team.getTeamNumber());
      }
      message.append("<p><i>Successfully advanced teams</i></p>");
    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new RuntimeException("There was an error talking to the database", e);
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Bottom of AdvanceTeams.doPost");
    }

    if (message.length() > 0) {
      session.setAttribute("message", message.toString());
    }

    response.sendRedirect(response.encodeRedirectURL("index.jsp"));
  }

}
