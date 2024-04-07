/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;

import fll.Tournament;
import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;
import fll.xml.ChallengeDescription;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Commit the changes made by editTeam.jsp.
 */
@WebServlet("/admin/CommitTeam")
public class CommitTeam extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Value is String.
   */
  public static final String TEAM_NAME = "teamName";

  /**
   * Value is String.
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

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.ADMIN), false)) {
      return;
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Top of CommitTeam.doPost");
    }

    final StringBuilder message = new StringBuilder();
    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);

    try (Connection connection = datasource.getConnection()) {
      final int teamNumber = WebUtils.getIntRequestParameter(request, "teamNumber");
      LOGGER.debug("Committing changes to {}", teamNumber);

      if (!StringUtils.isEmpty(request.getParameter("delete"))) {
        LOGGER.info("Deleting {}", teamNumber);

        Queries.deleteTeam(teamNumber, challengeDescription, connection);
        message.append("<p id='success'>Successfully deleted team "
            + teamNumber
            + "</p>");
      } else if (!StringUtils.isEmpty(request.getParameter("commit"))) {
        LOGGER.debug("Adding ot editing team {}", teamNumber);

        final String teamName = WebUtils.getNonNullRequestParameter(request, "teamName");
        final String organization = request.getParameter("organization");

        if (Boolean.valueOf(request.getParameter("addTeam"))) {
          LOGGER.info("Adding {}");

          final String otherTeam = Queries.addTeam(connection, teamNumber, teamName, organization);
          if (null != otherTeam) {
            final String msg = String.format("Team number %s is already assigned to %s", teamNumber, teamName);
            message.append("<p class='error'>"
                + msg
                + "</p>");
            LOGGER.error(msg);
          } else {
            message.append("<p id='success'>Successfully added team "
                + teamNumber
                + "</p>");
          }
        } else {
          LOGGER.debug("Updating information for team: {}", teamNumber);

          Queries.updateTeam(connection, teamNumber, teamName, organization);
          message.append("<p id='success'>Successfully updated team "
              + teamNumber
              + "'s info</p>");
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
                  + teamNumber
                  + " should be assigned to tournament "
                  + tournament.getName());
            }

            if (Boolean.valueOf(request.getParameter("tournament_"
                + tournament.getTournamentID()))) {

              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Team "
                    + teamNumber
                    + " has checked tournament "
                    + tournament.getName());
              }

              final String eventDivision = WebUtils.getNonNullRequestParameter(request, "event_division_"
                  + tournament.getTournamentID());
              final String judgingGroup = WebUtils.getNonNullRequestParameter(request, "judging_station_"
                  + tournament.getTournamentID());
              final String wave = WebUtils.getNonNullRequestParameter(request, "wave_"
                  + tournament.getTournamentID());

              if (!previouslyAssignedTournaments.contains(tournament.getTournamentID())) {
                if (LOGGER.isDebugEnabled()) {
                  LOGGER.debug("Adding team "
                      + teamNumber
                      + " to tournament "
                      + tournament.getName());
                }

                // add to tournament
                Queries.addTeamToTournament(connection, teamNumber, tournament.getTournamentID(), eventDivision,
                                            judgingGroup, StringUtils.isBlank(wave) ? null : wave);
              } else {
                // just update the division and judging station information

                final String prevEventDivision = Queries.getEventDivision(connection, teamNumber,
                                                                          tournament.getTournamentID());
                if (null == prevEventDivision) {
                  throw new FLLRuntimeException("Unable to find award group for team "
                      + teamNumber);
                }
                if (!eventDivision.equals(prevEventDivision)) {
                  Queries.updateTeamEventDivision(connection, teamNumber, tournament.getTournamentID(), eventDivision);
                }

                final String prevJudgingGroup = Queries.getJudgingGroup(connection, teamNumber,
                                                                        tournament.getTournamentID());
                if (null == prevJudgingGroup) {
                  throw new FLLRuntimeException("Unable to find judging group for team "
                      + teamNumber);
                }
                if (!judgingGroup.equals(prevJudgingGroup)) {
                  Queries.updateTeamJudgingGroups(connection, teamNumber, tournament.getTournamentID(), judgingGroup);
                }
              }

            } else if (previouslyAssignedTournaments.contains(tournament.getTournamentID())) {
              // team was removed from tournament
              Queries.deleteTeamFromTournament(connection, description, teamNumber, tournament.getTournamentID());
            }

          } // playoffs not initialized
        } // foreach tournament

      } // if committing data

      if (message.length() > 0) {
        SessionAttributes.appendToMessage(session, message.toString());
      }

      final String referrer = SessionAttributes.getAttribute(session, "edit_team_referrer", String.class);
      session.removeAttribute("edit_team_referrer");
      response.sendRedirect(response.encodeRedirectURL(StringUtils.isEmpty(referrer) ? "index.jsp" : referrer));
    } catch (

    final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new FLLRuntimeException("There was an error talking to the database", e);
    }

  }

}
