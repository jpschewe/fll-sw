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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import fll.Utilities;
import fll.db.Queries;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Commit the changes made by editTeam.jsp.
 * 
 * @author jpschewe
 * @version $Revision$
 */
public class CommitTeam extends BaseFLLServlet {

  private static final Logger LOGGER = Logger.getLogger(CommitTeam.class);

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Top of CommitTeam.doPost");
    }

    final StringBuilder message = new StringBuilder();
    final Document challengeDocument = (Document) application.getAttribute("challengeDocument");
    final DataSource datasource = (DataSource) session.getAttribute(SessionAttributes.DATASOURCE);

    try {
      final Connection connection = datasource.getConnection();
      // parse the numbers first so that we don't get a partial commit
      final int teamNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(request.getParameter("teamNumber")).intValue();
      final String division = request.getParameter("division");

      if (null != request.getParameter("delete")) {
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info("Deleting "
              + teamNumber);
        }
        Queries.deleteTeam(teamNumber, challengeDocument, connection);
      } else if (null != request.getParameter("advance")) {
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info("Advancing "
              + teamNumber);
        }
        final boolean result = Queries.advanceTeam(connection, teamNumber);
        if (!result) {
          message.append("<p class='error'>Error advancing team</p>");
          LOGGER.error("Error advancing team: "
              + teamNumber);
        }
      } else if (null != request.getParameter("demote")) {
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info("Demoting team: "
              + teamNumber);
        }
        Queries.demoteTeam(connection, challengeDocument, teamNumber);
      } else if (null != request.getParameter("commit")) {
        if(SessionAttributes.getNonNullAttribute(session, "addTeam", Boolean.class)) {
          if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Adding "
                + teamNumber);
          }
          final String otherTeam = Queries.addTeam(connection, teamNumber, request.getParameter("teamName"), request.getParameter("organization"),
                                                   request.getParameter("region"), division);
          if (null != otherTeam) {
            message.append("<p class='error'>Error, team number "
                + teamNumber + " is already assigned.</p>");
            LOGGER.error("TeamNumber "
                + teamNumber + " is already assigned");
          }
        } else {
          if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Updating "
                + teamNumber + " team info");
          }
          Queries.updateTeam(connection, teamNumber, request.getParameter("teamName"), request.getParameter("organization"), request.getParameter("region"),
                             division);
        }
        
        // this will be null if the tournament can't be changed
        final String newTournamentStr = request.getParameter("currentTournament");
        if (null != newTournamentStr) {
          final int newTournament = Utilities.NUMBER_FORMAT_INSTANCE.parse(newTournamentStr).intValue();
          final int teamCurrentTournament = Queries.getTeamCurrentTournament(connection, teamNumber);
          if (teamCurrentTournament != newTournament) {
            Queries.changeTeamCurrentTournament(connection, teamNumber, newTournament);
          }
        }
      }

    } catch (final ParseException pe) {
      LOGGER.error("Error parsing team number, this is an internal error", pe);
      throw new RuntimeException("Error parsing team number, this is an internal error", pe);
    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new RuntimeException("There was an error talking to the database", e);
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Bottom of CommitTeam.doPost");
    }

    if (message.length() > 0) {
      session.setAttribute("message", message.toString());
    }

    if(SessionAttributes.getNonNullAttribute(session, "addTeam", Boolean.class)) {
      response.sendRedirect(response.encodeRedirectURL("index.jsp"));
    } else {
      response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
    }
  }
}
