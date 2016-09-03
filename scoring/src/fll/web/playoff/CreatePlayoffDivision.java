/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
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

import fll.Tournament;
import fll.TournamentTeam;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Create a new playoff division.
 */
@WebServlet("/playoff/CreatePlayoffDivision")
public class CreatePlayoffDivision extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Populate the context for create_playoff_division.jsp.
   * 
   * @param application
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final int currentTournamentID = Queries.getCurrentTournament(connection);

      final List<String> judgingStations = Queries.getJudgingStations(connection, currentTournamentID);
      pageContext.setAttribute("judgingStations", judgingStations);

      final List<String> awardGroups = Queries.getAwardGroups(connection, currentTournamentID);
      pageContext.setAttribute("awardGroups", awardGroups);

    } catch (final SQLException sqle) {
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    } finally {
      SQLFunctions.close(connection);
    }
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

    String redirect = "index.jsp";
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final PlayoffSessionData data = SessionAttributes.getNonNullAttribute(session, PlayoffIndex.SESSION_DATA,
                                                                            PlayoffSessionData.class);

      final Tournament currentTournament = data.getCurrentTournament();
      final int currentTournamentID = currentTournament.getTournamentID();

      final List<String> playoffDivisions = Playoff.getPlayoffBrackets(connection, currentTournamentID);

      if (null != request.getParameter("selected_teams")) {
        final String bracketName = request.getParameter("bracket_name");
        if (null == bracketName
            || "".equals(bracketName)) {
          message.append("<p class='error'>You need to specify a name for the playoff bracket</p>");
          redirect = "create_playoff_division.jsp";
        } else if (playoffDivisions.contains(bracketName)) {
          message.append("<p class='error'>The playoff bracket '"
              + bracketName + "' already exists, please pick a different name");
          redirect = "create_playoff_division.jsp";
        } else {
          final String[] selectedTeams = request.getParameterValues("selected_team");
          final List<Integer> teamNumbers = new LinkedList<Integer>();
          for (final String teamStr : selectedTeams) {
            final int num = Integer.parseInt(teamStr);
            teamNumbers.add(num);
          }

          if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Selected team numbers: "
                + teamNumbers);
          }

          Playoff.createPlayoffBracket(connection, currentTournamentID, bracketName, teamNumbers);

          message.append("<p id='success'>Created playoff bracket"
              + bracketName + "</p>");

          redirect = "index.jsp";
        }
      } else {
        // create bracket based on award group or judging group

        boolean done = false;

        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
          final String paramName = paramNames.nextElement();
          if (paramName.startsWith("create_award_group_")) {
            final String idxStr = paramName.substring("create_award_group_".length());
            final int idx = Integer.parseInt(idxStr);
            final String awardGroup = request.getParameter("award_group_"
                + idx);

            // get list of teams in this award group
            final List<Integer> teamNumbers = new LinkedList<>();
            for (final Map.Entry<Integer, TournamentTeam> entry : data.getTournamentTeams().entrySet()) {
              if (awardGroup.equals(entry.getValue().getAwardGroup())) {
                teamNumbers.add(entry.getKey());
              }
            }

            Playoff.createPlayoffBracket(connection, currentTournamentID, awardGroup, teamNumbers);

            message.append("<p id='success'>Created playoff bracket '"
                + awardGroup + "'</p>");
            redirect = "index.jsp";
            done = true;
          } else if (paramName.startsWith("create_judging_group_")) {
            final String idxStr = paramName.substring("create_judging_group_".length());
            final int idx = Integer.parseInt(idxStr);
            final String judgingGroup = request.getParameter("judging_group_"
                + idx);

            // get list of teams in this judging group
            final List<Integer> teamNumbers = new LinkedList<>();
            for (final Map.Entry<Integer, TournamentTeam> entry : data.getTournamentTeams().entrySet()) {
              if (judgingGroup.equals(entry.getValue().getJudgingGroup())) {
                teamNumbers.add(entry.getKey());
              }
            }

            Playoff.createPlayoffBracket(connection, currentTournamentID, judgingGroup, teamNumbers);

            message.append("<p id='success'>Created playoff bracket '"
                + judgingGroup + "'</p>");
            redirect = "index.jsp";
            done = true;
          }
        }

        if (!done) {
          message.append("<p class='error'>No action specified</p>");
          redirect = "create_playoff_division.jsp";
        }
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
