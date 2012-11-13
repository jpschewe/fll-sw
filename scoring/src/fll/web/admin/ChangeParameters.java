/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

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
import fll.db.TournamentParameters;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Submit action for edit_all_parameters.jsp.
 */
@WebServlet("/admin/ChangeParameters")
public class ChangeParameters extends BaseFLLServlet {

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

      storeSeedingRounds(connection, request, tournaments);

      storeMaxScoreboardRound(connection, request, tournaments);

      message.append("<p id='success'>Parameters saved</p>");
    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage() + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving team data into the database", sqle);
    } finally {
      SQLFunctions.close(connection);
    }

    session.setAttribute(SessionAttributes.MESSAGE, message.toString());
    response.sendRedirect(response.encodeRedirectURL("index.jsp"));
  }

  private void storeSeedingRounds(final Connection connection,
                                  final HttpServletRequest request,
                                  final List<Tournament> tournaments) throws SQLException {
    final int defaultNumRounds = Integer.valueOf(request.getParameter("seeding_rounds_default"));
    TournamentParameters.setIntDefaultParameter(connection, TournamentParameters.SEEDING_ROUNDS, defaultNumRounds);

    for (final Tournament tournament : tournaments) {
      final String str = request.getParameter("seeding_rounds_"
          + tournament.getTournamentID());
      if ("default".equals(str)) {
        TournamentParameters.unsetTournamentParameter(connection, tournament.getTournamentID(),
                                                      TournamentParameters.SEEDING_ROUNDS);
      } else {
        final int value = Integer.valueOf(str);
        TournamentParameters.setIntTournamentParameter(connection, tournament.getTournamentID(),
                                                       TournamentParameters.SEEDING_ROUNDS, value);
      }
    }
  }

  private void storeMaxScoreboardRound(final Connection connection,
                                       final HttpServletRequest request,
                                       final List<Tournament> tournaments) throws SQLException {
    final int defaultNumRounds = Integer.valueOf(request.getParameter("max_scoreboard_round_default"));
    TournamentParameters.setIntDefaultParameter(connection, TournamentParameters.MAX_SCOREBOARD_ROUND, defaultNumRounds);

    for (final Tournament tournament : tournaments) {
      final String str = request.getParameter("max_scoreboard_round_"
          + tournament.getTournamentID());
      if ("default".equals(str)) {
        TournamentParameters.unsetTournamentParameter(connection, tournament.getTournamentID(),
                                                      TournamentParameters.MAX_SCOREBOARD_ROUND);
      } else {
        final int value = Integer.valueOf(str);
        TournamentParameters.setIntTournamentParameter(connection, tournament.getTournamentID(),
                                                       TournamentParameters.MAX_SCOREBOARD_ROUND, value);
      }
    }
  }
}
