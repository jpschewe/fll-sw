/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import fll.Utilities;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Commit the changes made by editTeam.jsp.
 */
@WebServlet("/admin/CommitTeam")
public class CommitTeam extends BaseFLLServlet {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Value is String.
   */
  public static final String TEAM_NAME = "teamName";

  /**
   * Value is String
   */
  public static final String DIVISION = "division";

  /**
   * Value is String
   */
  public static final String ORGANIZATION = "organization";

  /**
   * Key for storing the list of judging stations. Value is a
   * {@link java.util.Collection} of {@link String}.
   */
  public static final String ALL_JUDGING_STATIONS = "all_judging_stations";

  /**
   * Key for session. Value is a string.
   */
  public static final String EVENT_DIVISION = "event_division";

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Top of CommitTeam.doPost");
    }

    final StringBuilder message = new StringBuilder();
    final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);
    final DataSource datasource = SessionAttributes.getDataSource(session);

    try {
      final Connection connection = datasource.getConnection();
      // parse the numbers first so that we don't get a partial commit
      final int teamNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(request.getParameter("teamNumber")).intValue();
      session.setAttribute(GatherTeamData.TEAM_NUMBER, teamNumber);

      final String division = resolveDivision(request);
      session.setAttribute(DIVISION, division);

      final String teamName = request.getParameter("teamName");
      session.setAttribute(TEAM_NAME, teamName);

      final String organization = request.getParameter("organization");
      session.setAttribute(ORGANIZATION, organization);

      String redirect = null;
      if (null != request.getParameter("delete")) {
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info("Deleting "
              + teamNumber);
        }
        Queries.deleteTeam(teamNumber, challengeDocument, connection);
        message.append("<p id='success'>Successfully deleted team "
            + teamNumber + "</p>");

        redirect = "select_team.jsp";
      } else {
        // this will be null if the tournament can't be changed
        final String newTournamentStr = request.getParameter("currentTournament");
        final int newTournament = newTournamentStr == null ? -1
            : Utilities.NUMBER_FORMAT_INSTANCE.parse(newTournamentStr).intValue();

        if (null != newTournamentStr) {
          // need to get these before the team is put in the tournament.
          final Collection<String> allEventDivisions = Queries.getEventDivisions(connection, newTournament);
          session.setAttribute(CheckEventDivisionNeeded.ALL_EVENT_DIVISIONS, allEventDivisions);
          
          final Collection<String> allJudgingStations = Queries.getJudgingStations(connection, newTournament);
          session.setAttribute(CommitTeam.ALL_JUDGING_STATIONS, allJudgingStations);
        }

        if (null != request.getParameter("advance")) {
          if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Advancing "
                + teamNumber);
          }

          final boolean result = Queries.advanceTeam(connection, teamNumber);
          if (!result) {
            message.append("<p class='error'>Error advancing team</p>");
            LOGGER.error("Error advancing team: "
                + teamNumber);
            redirect = "select_team.jsp";
          } else {
            message.append("<p id='success'>Successfully advanced team "
                + teamNumber + "</p>");
          }
        } else if (null != request.getParameter("demote")) {
          if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Demoting team: "
                + teamNumber);
          }

          Queries.demoteTeam(connection, challengeDocument, teamNumber);
          message.append("<p id='success'>Successfully demoted team "
              + teamNumber + "</p>");
        }

        if (SessionAttributes.getNonNullAttribute(session, GatherTeamData.ADD_TEAM, Boolean.class)) {
          if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Adding "
                + teamNumber);
          }

          final String otherTeam = Queries.addTeam(connection, teamNumber, teamName, organization, division,
                                                   newTournament);
          if (null != otherTeam) {
            message.append("<p class='error'>Error, team number "
                + teamNumber + " is already assigned.</p>");
            LOGGER.error("TeamNumber "
                + teamNumber + " is already assigned");
            redirect = "index.jsp";
          } else {
            message.append("<p id='success'>Successfully added team "
                + teamNumber + "</p>");
          }
        } else {
          if (null != newTournamentStr) {
            final int teamCurrentTournament = Queries.getTeamCurrentTournament(connection, teamNumber);
            if (teamCurrentTournament != newTournament) {
              Queries.changeTeamCurrentTournament(connection, teamNumber, newTournament);
            }
          }
        }

      } // not deleting team

      if (message.length() > 0) {
        session.setAttribute(SessionAttributes.MESSAGE, message.toString());
      }

      if (null == redirect) {
        response.sendRedirect(response.encodeRedirectURL("CheckEventDivisionNeeded"));
      } else {
        response.sendRedirect(response.encodeRedirectURL(redirect));
      }

    } catch (final ParseException pe) {
      LOGGER.error("Error parsing team number, this is an internal error", pe);
      throw new RuntimeException("Error parsing team number, this is an internal error", pe);
    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new RuntimeException("There was an error talking to the database", e);
    }

  }

  /**
   * Figure out what the division is based on the value of the "division"
   * parameter and possibly the "division_text" parameter.
   */
  private String resolveDivision(final HttpServletRequest request) {
    final String div = request.getParameter("division");
    if ("text".equals(div)) {
      return request.getParameter("division_text");
    } else {
      return div;
    }
  }
}
