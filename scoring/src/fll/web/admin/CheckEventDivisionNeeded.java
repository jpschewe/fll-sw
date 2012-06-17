/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

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
 * Check if the team being added/edited needs event divisions set. Assumes that
 * the team number and event division are set in the session.
 */
@WebServlet("/admin/CheckEventDivisionNeeded")
public class CheckEventDivisionNeeded extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Key for storing the list of event divisions. Value is a Collection of
   * String.
   */
  public static final String ALL_EVENT_DIVISIONS = "all_event_divisions";

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final DataSource datasource = SessionAttributes.getDataSource(session);

    try {
      final Connection connection = datasource.getConnection();

      final int teamNumber = SessionAttributes.getNonNullAttribute(session, GatherTeamData.TEAM_NUMBER, Integer.class);
      final int teamCurrentTournament = Queries.getTeamCurrentTournament(connection, teamNumber);

      final String eventDivision = SessionAttributes.getNonNullAttribute(session, CommitEventDivision.EVENT_DIVISION,
                                                                         String.class);

      final Collection<String> allEventDivisions = Queries.getEventDivisions(connection, teamCurrentTournament);
      if (!allEventDivisions.isEmpty()
          && !allEventDivisions.contains(eventDivision)) {
        session.setAttribute(CheckEventDivisionNeeded.ALL_EVENT_DIVISIONS, allEventDivisions);
        session.setAttribute(GatherTeamData.TEAM_NUMBER, teamNumber);
        response.sendRedirect(response.encodeRedirectURL("chooseEventDivision.jsp"));
      } else {
        session.setAttribute(CommitEventDivision.EVENT_DIVISION, eventDivision);
        response.sendRedirect(response.encodeRedirectURL("SaveTeamData"));
      }
    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new RuntimeException("There was an error talking to the database", e);
    }

  }

}
