/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import fll.TournamentTeam;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Commit the changes from edit_event_division.jsp.
 */
@WebServlet("/admin/CommitAwardGroups")
public class CommitAwardGroups extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Put the needed variables into the page context for the page to use.
   */
  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final PageContext pageContext) {
    final StringBuilder message = new StringBuilder();
    final String existingMessage = SessionAttributes.getMessage(session);
    if (null != existingMessage) {
      message.append(existingMessage);
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final int currentTournamentID = Queries.getCurrentTournament(connection);

      pageContext.setAttribute("divisions", Queries.getAwardGroups(connection, currentTournamentID));

      final Map<Integer, TournamentTeam> teams = Queries.getTournamentTeams(connection, currentTournamentID);
      pageContext.setAttribute("teams", teams.values());

    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage() + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving team data into the database", sqle);
    } finally {
      SQLFunctions.close(connection);
    }

    session.setAttribute(SessionAttributes.MESSAGE, message.toString());

  }

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
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final int currentTournamentID = Queries.getCurrentTournament(connection);

      if ("Commit".equals(request.getParameter("submit"))) {

        final Map<Integer, TournamentTeam> teams = Queries.getTournamentTeams(connection, currentTournamentID);
        for (final Map.Entry<Integer, TournamentTeam> entry : teams.entrySet()) {
          final int teamNumber = entry.getValue().getTeamNumber();
          final String teamNumberStr = String.valueOf(teamNumber);
          String newAwardGroup = request.getParameter(teamNumberStr);
          if (null != newAwardGroup
              && !newAwardGroup.isEmpty()) {
            if ("text".equals(newAwardGroup)) {
              // get from text box
              newAwardGroup = request.getParameter("text_"
                  + teamNumberStr);
            }
            if (null != newAwardGroup
                && !newAwardGroup.trim().isEmpty()) {
              newAwardGroup = newAwardGroup.trim();

              final String currentAwardGroup = Queries.getEventDivision(connection, teamNumber, currentTournamentID);
              if (!newAwardGroup.equals(currentAwardGroup)) {
                Queries.setEventDivision(connection, teamNumber, currentTournamentID, newAwardGroup);
              }
            }
          }
        }
      }

      message.append("<p class='success' id='success'>Award group changes saved</p>");

    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage() + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving award group data into the database", sqle);
    } finally {
      SQLFunctions.close(connection);
    }

    session.setAttribute(SessionAttributes.MESSAGE, message.toString());

    // send back to the admin index
    response.sendRedirect(response.encodeRedirectURL("index.jsp"));

  }

}
