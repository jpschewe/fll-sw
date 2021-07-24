/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.sql.DataSource;

import fll.Team;
import fll.Tournament;
import fll.TournamentTeam;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.xml.ChallengeDescription;

/**
 * Initialize playoff brackets.
 */
@WebServlet("/playoff/InitializeBrackets")
public class InitializeBrackets extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

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

    final StringBuilder message = new StringBuilder();

    /*
     * Parameters: division - String for the division. enableThird - has value
     * 'yes' if we are to have 3rd/4th place brackets, null otherwise.
     */

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

    final String redirect = "index.jsp";
    try (Connection connection = datasource.getConnection()) {

      final PlayoffSessionData data = SessionAttributes.getNonNullAttribute(session, PlayoffIndex.SESSION_DATA,
                                                                            PlayoffSessionData.class);

      final Tournament currentTournament = data.getCurrentTournament();
      final int currentTournamentID = currentTournament.getTournamentID();

      final String bracket = data.getBracket();
      if (null == bracket) {
        message.append("<p class='error'>No playoff bracket specified.</p>");
      } else if (Queries.isPlayoffDataInitialized(connection, bracket)) {
        message.append("<p class='warning'>Playoffs have already been initialized for playoff bracket "
            + bracket
            + ".</p>");
      } else {
        final List<Integer> teamNumbersInBracket = Playoff.getTeamNumbersForPlayoffBracket(connection,
                                                                                           currentTournamentID,
                                                                                           bracket);
        final Map<Integer, TournamentTeam> tournamentTeams = data.getTournamentTeams();

        final String errors = Playoff.involvedInUnfinishedPlayoff(connection, currentTournamentID,
                                                                  teamNumbersInBracket);
        if (null != errors) {
          message.append(errors);
        } else {
          final List<Team> teams = teamNumbersInBracket.stream().map(teamNum -> {
            if (tournamentTeams.containsKey(teamNum)) {
              return tournamentTeams.get(teamNum);
            } else {
              throw new FLLInternalException("Inconsistency between database and stored playoff information. Cannot find "
                  + teamNum
                  + " in the database");
            }
          }).collect(Collectors.toList());

          Playoff.initializeBrackets(connection, challengeDescription, bracket, data.getEnableThird(), teams,
                                     data.getSort());
        }

        message.append("<p id='success'>Playoffs have been successfully initialized for division "
            + data.getBracket()
            + ".</p>");
      }

    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage()
          + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving team data into the database", sqle);
    }

    SessionAttributes.appendToMessage(session, message.toString());
    response.sendRedirect(response.encodeRedirectURL(redirect));
  }

}
