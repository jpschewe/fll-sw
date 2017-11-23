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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import fll.Tournament;
import fll.db.GlobalParameters;
import fll.db.Queries;
import fll.db.TournamentParameters;
import fll.flltools.MhubParameters;
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
                                final HttpSession session)
      throws IOException, ServletException {
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

      storeSeedingRounds(connection, request, tournaments, message);

      storeMaxScoreboardRound(connection, request, tournaments);

      storePerformanceAdvancementPercentage(connection, request, tournaments);

      GlobalParameters.setDoubleGlobalParameter(connection, GlobalParameters.STANDARDIZED_MEAN,
                                                Double.valueOf(request.getParameter("gStandardizedMean")));

      GlobalParameters.setDoubleGlobalParameter(connection, GlobalParameters.STANDARDIZED_SIGMA,
                                                Double.valueOf(request.getParameter("gStandardizedSigma")));

      GlobalParameters.setDoubleGlobalParameter(connection, GlobalParameters.DIVISION_FLIP_RATE,
                                                Double.valueOf(request.getParameter("gDivisionFlipRate")));

      GlobalParameters.setUseQuartilesInRankingReport(connection,
                                                      Boolean.valueOf(request.getParameter("gUseQuartiles")));

      GlobalParameters.setAllTeamsMsPerRow(connection, Integer.parseInt(request.getParameter("gAllTeamsMsPerRow")));

      GlobalParameters.setHeadToHeadMsPerRow(connection, Integer.parseInt(request.getParameter("gHeadToHeadMsPerRow")));

      final String mhubHostname = request.getParameter("gMhubHostname");
      if (StringUtils.isBlank(mhubHostname)) {
        MhubParameters.setHostname(connection, null);
      } else {
        MhubParameters.setHostname(connection, mhubHostname.trim());
      }

      MhubParameters.setPort(connection, Integer.parseInt(request.getParameter("gMhubPort")));

      MhubParameters.setDisplayNode(connection, request.getParameter("gMhubDisplayNode"));

      if (message.length() == 0) {
        message.append("<p id='success'>Parameters saved</p>");
      }
    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage()
          + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving team data into the database", sqle);
    } finally {
      SQLFunctions.close(connection);
    }

    session.setAttribute(SessionAttributes.MESSAGE, message.toString());
    response.sendRedirect(response.encodeRedirectURL("edit_all_parameters.jsp"));
  }

  private void storeSeedingRounds(final Connection connection,
                                  final HttpServletRequest request,
                                  final List<Tournament> tournaments,
                                  final StringBuilder message)
      throws SQLException {
    final int defaultNumRounds = Integer.parseInt(request.getParameter("seeding_rounds_default"));
    TournamentParameters.setDefaultNumSeedingRounds(connection, defaultNumRounds);

    for (final Tournament tournament : tournaments) {
      // determine if the seeding rounds needs to change
      final boolean valueSet = TournamentParameters.isNumSeedingRoundsSet(connection, tournament.getTournamentID());
      final int currentValue = TournamentParameters.getNumSeedingRounds(connection, tournament.getTournamentID());

      final String str = request.getParameter("seeding_rounds_"
          + tournament.getTournamentID());
      if ("default".equals(str)) {

        if (valueSet) {
          if (Queries.isPlayoffDataInitialized(connection, tournament.getTournamentID())) {
            message.append("<p class='error'>You cannot change the number of seeding rounds once the head to head brackets are initialized. Tournament: "
                + tournament.getName()
                + "</p>");
          } else {
            TournamentParameters.unsetNumSeedingRounds(connection, tournament.getTournamentID());
          }
        }
      } else {
        final int value = Integer.parseInt(str);
        if (!valueSet
            || value != currentValue) {
          if (Queries.isPlayoffDataInitialized(connection, tournament.getTournamentID())) {
            message.append("<p class='error'>You cannot change the number of seeding rounds once the head to head brackets are initialized. Tournament: "
                + tournament.getName()
                + "</p>");
          } else {
            TournamentParameters.setNumSeedingRounds(connection, tournament.getTournamentID(), value);
          }
        }
      }
    }
  }

  private void storeMaxScoreboardRound(final Connection connection,
                                       final HttpServletRequest request,
                                       final List<Tournament> tournaments)
      throws SQLException {
    final int defaultNumRounds = Integer.parseInt(request.getParameter("max_scoreboard_round_default"));
    TournamentParameters.setDefaultMaxScoreboardPerformanceRound(connection, defaultNumRounds);

    for (final Tournament tournament : tournaments) {
      final String str = request.getParameter("max_scoreboard_round_"
          + tournament.getTournamentID());
      if ("default".equals(str)) {
        TournamentParameters.unsetMaxScoreboardPerformanceRound(connection, tournament.getTournamentID());
      } else {
        final int value = Integer.parseInt(str);
        TournamentParameters.setMaxScoreboardPerformanceRound(connection, tournament.getTournamentID(), value);
      }
    }
  }

  private void storePerformanceAdvancementPercentage(final Connection connection,
                                                     final HttpServletRequest request,
                                                     final List<Tournament> tournaments)
      throws SQLException {
    final int defaultValue = Integer.parseInt(request.getParameter("performance_advancement_percentage_default"));
    TournamentParameters.setDefaultPerformanceAdvancementPercentage(connection, defaultValue);

    for (final Tournament tournament : tournaments) {
      final String str = request.getParameter("performance_advancement_percentage_"
          + tournament.getTournamentID());
      if (null == str
          || "".equals(str.trim())
          || "default".equals(str)) {
        TournamentParameters.unsetPerformanceAdvancementPercentage(connection, tournament.getTournamentID());
      } else {
        final int value = Integer.parseInt(str);
        TournamentParameters.setPerformanceAdvancementPercentage(connection, tournament.getTournamentID(), value);
      }
    }
  }

}
