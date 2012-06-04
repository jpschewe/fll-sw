/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
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
 * Save team edits to teh database
 */
@WebServlet("/admin/SaveTeamData")
public class SaveTeamData extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final StringBuilder message = new StringBuilder();
    final DataSource datasource = SessionAttributes.getDataSource(session);

    try {
      final Connection connection = datasource.getConnection();

      final String origMessage = SessionAttributes.getMessage(session);
      if (null != origMessage) {
        message.append(origMessage);
      }

      final int teamNumber = SessionAttributes.getNonNullAttribute(session, GatherTeamData.TEAM_NUMBER, Integer.class);

      final String division = SessionAttributes.getNonNullAttribute(session, CommitTeam.DIVISION, String.class);

      final String teamName = SessionAttributes.getNonNullAttribute(session, CommitTeam.TEAM_NAME, String.class);

      final String organization = SessionAttributes.getAttribute(session, CommitTeam.ORGANIZATION, String.class);

      final String eventDivision = SessionAttributes.getNonNullAttribute(session, CommitEventDivision.EVENT_DIVISION,
                                                                         String.class);

      // store changes
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Updating "
            + teamNumber + " team info");
      }

      if (!SessionAttributes.getNonNullAttribute(session, GatherTeamData.ADD_TEAM, Boolean.class)) {
        Queries.updateTeam(connection, teamNumber, teamName, organization, division);
        message.append("<p id='success'>Successfully updated a team "
            + teamNumber + "'s info</p>");
      }

      final int teamCurrentTournament = Queries.getTeamCurrentTournament(connection, teamNumber);
      Queries.setEventDivision(connection, teamNumber, teamCurrentTournament, eventDivision);

      if (message.length() > 0) {
        session.setAttribute(SessionAttributes.MESSAGE, message.toString());
      }

      if (SessionAttributes.getNonNullAttribute(session, GatherTeamData.ADD_TEAM, Boolean.class)) {
        response.sendRedirect(response.encodeRedirectURL("index.jsp"));
      } else {
        response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
      }

    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new RuntimeException("There was an error talking to the database", e);
    }

  }
}
