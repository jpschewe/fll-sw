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

import net.mtu.eggplant.util.sql.SQLFunctions;

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

    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final int teamNumber = SessionAttributes.getNonNullAttribute(session, GatherTeamData.TEAM_NUMBER, Integer.class);

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Top CheckEventDivisionNeeded team: "
            + teamNumber);
      }

      final int teamCurrentTournament = Queries.getTeamCurrentTournament(connection, teamNumber);

      final String eventDivision = Queries.getEventDivision(connection, teamNumber, teamCurrentTournament);
      session.setAttribute(CommitTeam.EVENT_DIVISION, eventDivision);

      @SuppressWarnings("unchecked")
      final Collection<String> allEventDivisions = (Collection<String>) SessionAttributes.getNonNullAttribute(session,
                                                                                                              CheckEventDivisionNeeded.ALL_EVENT_DIVISIONS,
                                                                                                              Collection.class);

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Current event division: "
            + eventDivision + " all: " + allEventDivisions + " tournament: " + teamCurrentTournament);
      }

      if (!allEventDivisions.isEmpty()
          && !allEventDivisions.contains(eventDivision)) {
        response.sendRedirect(response.encodeRedirectURL("chooseEventDivision.jsp"));
      } else {
        response.sendRedirect(response.encodeRedirectURL("CheckJudgingStationNeeded"));
      }
    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new RuntimeException("There was an error talking to the database", e);
    } finally {
      SQLFunctions.close(connection);
    }

  }

}
