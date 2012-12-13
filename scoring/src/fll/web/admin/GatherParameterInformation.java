/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
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

import fll.Tournament;
import fll.db.GlobalParameters;
import fll.db.TournamentParameters;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Gather parameter information for edit_all_parameters.jsp.
 */
@WebServlet("/admin/GatherParameterInformation")
public class GatherParameterInformation extends BaseFLLServlet {

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

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final List<Tournament> tournaments = Tournament.getTournaments(connection);
      session.setAttribute("tournaments", tournaments);

      session.setAttribute("numSeedingRounds_default",
                           TournamentParameters.getIntTournamentParameterDefault(connection,
                                                                                 TournamentParameters.SEEDING_ROUNDS));
      final Map<Integer, Integer> numSeedingRounds = new HashMap<Integer, Integer>();
      for (final Tournament tournament : tournaments) {
        if (TournamentParameters.tournamentParameterValueExists(connection, tournament.getTournamentID(),
                                                                TournamentParameters.SEEDING_ROUNDS)) {
          numSeedingRounds.put(tournament.getTournamentID(),
                               TournamentParameters.getIntTournamentParameter(connection, tournament.getTournamentID(),
                                                                              TournamentParameters.SEEDING_ROUNDS));
        }
      }
      session.setAttribute("numSeedingRounds", numSeedingRounds);

      session.setAttribute("maxScoreboardRound_default",
                           TournamentParameters.getIntTournamentParameterDefault(connection,
                                                                                 TournamentParameters.MAX_SCOREBOARD_ROUND));
      final Map<Integer, Integer> maxScoreboardRound = new HashMap<Integer, Integer>();
      for (final Tournament tournament : tournaments) {
        if (TournamentParameters.tournamentParameterValueExists(connection, tournament.getTournamentID(),
                                                                TournamentParameters.MAX_SCOREBOARD_ROUND)) {
          maxScoreboardRound.put(tournament.getTournamentID(),
                                 TournamentParameters.getIntTournamentParameter(connection,
                                                                                tournament.getTournamentID(),
                                                                                TournamentParameters.MAX_SCOREBOARD_ROUND));
        }
      }
      session.setAttribute("maxScoreboardRound", maxScoreboardRound);

      session.setAttribute("gStandardizedMean",
                           GlobalParameters.getDoubleGlobalParameter(connection, GlobalParameters.STANDARDIZED_MEAN));

      session.setAttribute("gStandardizedSigma",
                           GlobalParameters.getDoubleGlobalParameter(connection, GlobalParameters.STANDARDIZED_SIGMA));

    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage() + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving team data into the database", sqle);
    } finally {
      SQLFunctions.close(connection);
    }

    session.setAttribute("servletLoaded", true);

    session.setAttribute(SessionAttributes.MESSAGE, message.toString());
    response.sendRedirect(response.encodeRedirectURL("edit_all_parameters.jsp"));
  }
}
