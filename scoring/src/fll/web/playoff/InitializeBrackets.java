/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
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

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import fll.Team;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Initialize playoff brackets.
 */
@WebServlet("/playoff/InitializeBrackets")
public class InitializeBrackets extends BaseFLLServlet {

  /**
   * Collection of teams at this tournament in a Collection<Team>.
   */
  public static final String TEAMS = "teams";

  /**
   * Boolean if third place brackets should be enabled.
   */
  public static final String ENABLE_THIRD_PLACE = "enableThird";

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

    /*
     * Parameters: division - String for the division. enableThird - has value
     * 'yes' if we are to have 3rd/4th place brackets, null otherwise.
     */

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);
    Connection connection = null;
    String redirect = "index.jsp";
    try {
      connection = datasource.getConnection();

      final String divisionStr = request.getParameter("division");

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("division: '"
            + divisionStr + "'");
      }

      final String thirdFourthPlaceBrackets = request.getParameter("enableThird");
      boolean enableThird;
      if (null == thirdFourthPlaceBrackets) {
        enableThird = false;
      } else {
        enableThird = true;
      }
      session.setAttribute(ENABLE_THIRD_PLACE, enableThird);


      if (null == divisionStr) {
        message.append("<p class='error'>No division specified.</p>");
      } else if (PlayoffIndex.CREATE_NEW_PLAYOFF_DIVISION.equals(divisionStr)) {
        final Collection<Team> teams = Queries.getTournamentTeams(connection).values();
        session.setAttribute(TEAMS, teams);
        redirect = "create_playoff_division.jsp";
      } else {

        if (Queries.isPlayoffDataInitialized(connection, divisionStr)) {
          message.append("<p class='warning'>Playoffs have already been initialized for division "
              + divisionStr + ".</p>");
        } else {
          final Map<Integer, Team> tournamentTeams = Queries.getTournamentTeams(connection);
          final List<Team> teams = new ArrayList<Team>(tournamentTeams.values());
          Team.filterTeamsToEventDivision(connection, teams, divisionStr);

          Playoff.initializeBrackets(connection, challengeDocument, divisionStr, enableThird, teams);
          message.append("<p>Playoffs have been successfully initialized for division "
              + divisionStr + ".</p>");
        }
      }

    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage() + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving team data into the database", sqle);
    } finally {
      SQLFunctions.close(connection);
    }

    session.setAttribute(SessionAttributes.MESSAGE, message.toString());
    response.sendRedirect(response.encodeRedirectURL(redirect));
  }

}
