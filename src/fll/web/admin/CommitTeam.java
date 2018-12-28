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
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import fll.Tournament;
import fll.Utilities;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.xml.ChallengeDescription;
import net.mtu.eggplant.util.sql.SQLFunctions;

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
    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);

    Connection connection = null;
    try {
      connection = datasource.getConnection();
      // parse the numbers first so that we don't get a partial commit
      final int teamNumber = Utilities.INTEGER_NUMBER_FORMAT_INSTANCE.parse(request.getParameter("teamNumber")).intValue();

      String redirect = null;
      if (null != request.getParameter("delete")) {
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info("Deleting "
              + teamNumber);
        }
        Queries.deleteTeam(teamNumber, challengeDescription, connection);
        message.append("<p id='success'>Successfully deleted team "
            + teamNumber + "</p>");

        redirect = "index.jsp";
      } else {
        final String teamName = request.getParameter("teamName");
        final String organization = request.getParameter("organization");

        if (Boolean.valueOf(request.getParameter("addTeam"))) {
          if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Adding "
                + teamNumber);
          }

          // came from the index, send back to the index
          redirect = "index.jsp";

          final String otherTeam = Queries.addTeam(connection, teamNumber, teamName, organization);
          if (null != otherTeam) {
            message.append("<p class='error'>Error, team number "
                + teamNumber + " is already assigned.</p>");
            LOGGER.error("TeamNumber "
                + teamNumber + " is already assigned");
          } else {
            message.append("<p id='success'>Successfully added team "
                + teamNumber + "</p>");
          }
        } else {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Updating information for team: "
                + teamNumber);
          }

          redirect = "index.jsp";

          Queries.updateTeam(connection, teamNumber, teamName, organization);
          message.append("<p id='success'>Successfully updated a team "
              + teamNumber + "'s info</p>");
        }

        // assign tournaments
        final List<Tournament> allTournaments = Tournament.getTournaments(connection);
        final Collection<Integer> previouslyAssignedTournaments = Queries.getAllTournamentsForTeam(connection,
                                                                                                   teamNumber);

        for (final Tournament tournament : allTournaments) {

          // can't change tournaments where the playoffs have been initialized
          if (!Queries.isPlayoffDataInitialized(connection, tournament.getTournamentID())) {

            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Checking if "
                  + teamNumber + " should be assigned to tournament " + tournament.getName());
            }

            if (Boolean.valueOf(request.getParameter("tournament_"
                + tournament.getTournamentID()))) {

              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Team "
                    + teamNumber + " has checked tournament " + tournament.getName());
              }

              final String eventDivision = request.getParameter("event_division_"
                  + tournament.getTournamentID());
              final String judgingGroup = request.getParameter("judging_station_"
                  + tournament.getTournamentID());

              if (!previouslyAssignedTournaments.contains(tournament.getTournamentID())) {
                if (LOGGER.isDebugEnabled()) {
                  LOGGER.debug("Adding team "
                      + teamNumber + " to tournament " + tournament.getName());
                }

                // add to tournament
                Queries.addTeamToTournament(connection, teamNumber, tournament.getTournamentID(), eventDivision,
                                            judgingGroup);
              } else {
                // just update the division and judging station information

                final String prevEventDivision = Queries.getEventDivision(connection, teamNumber,
                                                                          tournament.getTournamentID());
                if (!eventDivision.equals(prevEventDivision)) {
                  Queries.setEventDivision(connection, teamNumber, tournament.getTournamentID(), eventDivision);
                }

                final String prevJudgingGroup = Queries.getJudgingGroup(connection, teamNumber,
                                                                            tournament.getTournamentID());
                if (!judgingGroup.equals(prevJudgingGroup)) {
                  Queries.setJudgingGroup(connection, teamNumber, tournament.getTournamentID(), judgingGroup);
                }
              }

            } else if (previouslyAssignedTournaments.contains(tournament.getTournamentID())) {
              // team was removed from tournament
              Queries.deleteTeamFromTournament(connection, description, teamNumber, tournament.getTournamentID());
            }
            
          } // playoffs not initialized
        } // foreach tournament

      } // not deleting team

      if (message.length() > 0) {
        session.setAttribute(SessionAttributes.MESSAGE, message.toString());
      }

      response.sendRedirect(response.encodeRedirectURL(redirect));

    } catch (final ParseException pe) {
      LOGGER.error("Error parsing team number, this is an internal error", pe);
      throw new FLLInternalException("Error parsing team number, this is an internal error", pe);
    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new FLLRuntimeException("There was an error talking to the database", e);
    } finally {
      SQLFunctions.close(connection);
    }

  }

}
