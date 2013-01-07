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
import java.util.LinkedList;
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
import fll.Tournament;
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
   * Used as both a request parameter and a session key to specify the division
   * to initialize.
   */
  public static final String DIVISION = "division";

  /**
   * Collection of teams at this tournament in a Collection<Team>. Stored in the
   * session.
   */
  public static final String TOURNAMENT_TEAMS = "tournament_teams";

  /**
   * If the division is not an event division, the teams to put
   * in the playoff bracket. Stored in the session. Type is List<Team>.
   */
  public static final String DIVISION_TEAMS = "division_teams";

  /**
   * Page to redirect to once this servlet is finished successfully. Type is
   * String and is stored in the session.
   */
  public static final String NEXTHOP_SUCCESS = "nexthop_success";

  /**
   * Page to redirect to once this servlet is finished with an error. Type is
   * String and is stored in the session.
   */
  public static final String NEXTHOP_ERROR = "nexthop_error";

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

      final PlayoffSessionData data = SessionAttributes.getNonNullAttribute(session, PlayoffIndex.SESSION_DATA,
                                                                            PlayoffSessionData.class);

      final Tournament currentTournament = data.getCurrentTournament();
      final int currentTournamentID = currentTournament.getTournamentID();

      final String divisionStr;
      final String divParam = request.getParameter(DIVISION);
      if (null == divParam
          || "".equals(divParam)) {
        divisionStr = data.getDivision();
      } else {
        divisionStr = divParam;
      }
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("division: '"
            + divisionStr + "'");
      }
      data.setDivision(divisionStr);

      final boolean enableThird;
      final String thirdFourthPlaceBrackets = request.getParameter("enableThird");
      if (null == thirdFourthPlaceBrackets) {
        enableThird = false;
      } else {
        enableThird = true;
      }

      if (null == divisionStr) {
        message.append("<p class='error'>No division specified.</p>");
      } else if (PlayoffIndex.CREATE_NEW_PLAYOFF_DIVISION.equals(divisionStr)) {
        redirect = "create_playoff_division.jsp";
      } else {

        if (Queries.isPlayoffDataInitialized(connection, divisionStr)) {
          message.append("<p class='warning'>Playoffs have already been initialized for division "
              + divisionStr + ".</p>");
        } else {
          final List<String> eventDivisions = Queries.getEventDivisions(connection, currentTournamentID);
          if (eventDivisions.contains(divisionStr)) {
            final Map<Integer, Team> tournamentTeams = data.getTournamentTeams();
            final List<Team> teams = new ArrayList<Team>(tournamentTeams.values());
            Team.filterTeamsToEventDivision(connection, teams, divisionStr);

            Playoff.initializeBrackets(connection, challengeDocument, divisionStr, enableThird, teams);
          } else {
            // assume new playoff division

            // can't do generics inside the session
            @SuppressWarnings("unchecked")
            final List<Team> teams = (List<Team>) SessionAttributes.getNonNullAttribute(session,
                                                                                        InitializeBrackets.DIVISION_TEAMS,
                                                                                        List.class);
            final List<Integer> teamNumbers = new LinkedList<Integer>();
            for (final Team t : teams) {
              teamNumbers.add(t.getTeamNumber());
            }

            final String errors = Playoff.involvedInUnfinishedPlayoff(connection, currentTournamentID, teamNumbers);
            if (null != errors) {
              message.append(errors);
            } else {

              Playoff.initializeBrackets(connection, challengeDocument, divisionStr, enableThird, teams);
            }
          }
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
