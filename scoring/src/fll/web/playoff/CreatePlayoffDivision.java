/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.Team;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Create a new playoff division.
 */
@WebServlet("/playoff/CreatePlayoffDivision")
public class CreatePlayoffDivision extends BaseFLLServlet {

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

    String redirect = "index.jsp";
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final PlayoffSessionData data = SessionAttributes.getNonNullAttribute(session, PlayoffIndex.SESSION_DATA,
                                                                            PlayoffSessionData.class);

      final int currentTournamentID = Queries.getCurrentTournament(connection);

      final List<String> playoffDivisions = Playoff.getPlayoffDivisions(connection, currentTournamentID);

      final String divisionStr = request.getParameter("division_name");
      if (null == divisionStr
          || "".equals(divisionStr)) {
        message.append("<p class='error'>You need to specify a name for the palyoff division</p>");
        redirect = "create_playoff_division.jsp";
      } else if (playoffDivisions.contains(divisionStr)) {
        message.append("<p class='error'>The division '"
            + divisionStr + "' already exists, please pick a different name");
        redirect = "create_playoff_division.jsp";
      } else if (Queries.getEventDivisions(connection, currentTournamentID).contains(divisionStr)) {
        message.append("<p class='error'>The division '"
            + divisionStr + "' matches an event division, please pick a different name");
        redirect = "create_playoff_division.jsp";
      } else {
        final String[] selectedTeams = request.getParameterValues("selected_team");
        final List<Integer> teamNumbers = new LinkedList<Integer>();
        for (final String teamStr : selectedTeams) {
          final int num = Integer.valueOf(teamStr);
          teamNumbers.add(num);
        }

        data.setDivision(divisionStr);

        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Selected team numbers: "
              + teamNumbers);
        }

        final List<Team> teams = new LinkedList<Team>();
        for (final int number : teamNumbers) {
          final Team team = Team.getTeamFromDatabase(connection, number);
          teams.add(team);
        }

        session.setAttribute(InitializeBrackets.DIVISION_TEAMS, teams);

        redirect = "InitializeBrackets";
      }
    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage() + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    } finally {
      SQLFunctions.close(connection);
    }

    session.setAttribute(SessionAttributes.MESSAGE, message.toString());
    response.sendRedirect(response.encodeRedirectURL(redirect));

  }

}
