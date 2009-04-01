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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.mtu.eggplant.util.Functions;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import fll.Utilities;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.Init;

/**
 * Commit the changes made by editTeam.jsp.
 * 
 * @author jpschewe
 * @version $Revision$
 */
public class CommitTeam extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(CommitTeam.class);

  /**
   * @param request
   * @param response
   */
  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Top of CommitTeam.doPost");
    }

    try {
      Init.initialize(request, response);
    } catch (final SQLException e) {
      throw new RuntimeException("Error in initialization", e);
    }

    final StringBuilder message = new StringBuilder();
    final ServletContext application = getServletContext();
    final HttpSession session = request.getSession();
    final Document challengeDocument = (Document) application.getAttribute("challengeDocument");
    final Connection connection = (Connection) application.getAttribute(ApplicationAttributes.CONNECTION);

    try {
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
        if (null != request.getParameter("addTeam")) {
          if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Adding"
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
          // this will be null if the tournament can't be changed
          final String newTournament = request.getParameter("currentTournament");
          if (null != newTournament) {
            final String teamCurrentTournament = Queries.getTeamCurrentTournament(connection, teamNumber);
            if (!Functions.safeEquals(teamCurrentTournament, newTournament)) {
              Queries.changeTeamCurrentTournament(connection, challengeDocument, teamNumber, request.getParameter("currentTournament"));
            }
          }
        }
      }

    } catch (final ParseException pe) {
      LOGGER.error("Error parsing team number, this is an internal error", pe);
      throw new RuntimeException("Error parsing team number, this is an internal error", pe);
    } catch (SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new RuntimeException("There was an error talking to the database", e);
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Bottom of CommitTeam.doPost");
    }

    if (message.length() > 0) {
      session.setAttribute("message", message.toString());
    }

    if ("1".equals(request.getParameter("addTeam"))) {
      response.sendRedirect(response.encodeRedirectURL("index.jsp"));
    } else {
      response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
    }
  }
}
