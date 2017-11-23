/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;
import fll.Tournament;
import fll.db.GlobalParameters;
import fll.db.TournamentParameters;
import fll.flltools.MhubParameters;
import fll.web.ApplicationAttributes;

/**
 * Gather parameter information for edit_all_parameters.jsp.
 */
public class GatherParameterInformation {

  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext)
      throws SQLException {

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final List<Tournament> tournaments = Tournament.getTournaments(connection);
      pageContext.setAttribute("tournaments", tournaments);

      pageContext.setAttribute("numSeedingRounds_default", TournamentParameters.getDefaultNumSeedRounds(connection));
      final Map<Integer, Integer> numSeedingRounds = new HashMap<Integer, Integer>();
      for (final Tournament tournament : tournaments) {
        if (TournamentParameters.tournamentParameterValueExists(connection, tournament.getTournamentID(),
                                                                TournamentParameters.SEEDING_ROUNDS)) {
          numSeedingRounds.put(tournament.getTournamentID(),
                               TournamentParameters.getNumSeedingRounds(connection, tournament.getTournamentID()));
        }
      }
      pageContext.setAttribute("numSeedingRounds", numSeedingRounds);

      pageContext.setAttribute("maxScoreboardRound_default",
                               TournamentParameters.getDefaultMaxScoreboardPerformanceRound(connection));
      final Map<Integer, Integer> maxScoreboardRound = new HashMap<Integer, Integer>();
      for (final Tournament tournament : tournaments) {
        if (TournamentParameters.tournamentParameterValueExists(connection, tournament.getTournamentID(),
                                                                TournamentParameters.MAX_SCOREBOARD_ROUND)) {
          maxScoreboardRound.put(tournament.getTournamentID(),
                                 TournamentParameters.getMaxScoreboardPerformanceRound(connection,
                                                                                       tournament.getTournamentID()));
        }
      }
      pageContext.setAttribute("maxScoreboardRound", maxScoreboardRound);

      pageContext.setAttribute("performanceAdvancementPercentage_default",
                               TournamentParameters.getDefaultPerformanceAdvancementPercentage(connection));
      final Map<Integer, Integer> performanceAdvancementPercentage = new HashMap<Integer, Integer>();
      for (final Tournament tournament : tournaments) {
        if (TournamentParameters.tournamentParameterValueExists(connection, tournament.getTournamentID(),
                                                                TournamentParameters.PERFORMANCE_ADVANCEMENT_PERCENTAGE)) {
          performanceAdvancementPercentage.put(tournament.getTournamentID(),
                                               TournamentParameters.getPerformanceAdvancementPercentage(connection,
                                                                                                        tournament.getTournamentID()));
        }
      }
      pageContext.setAttribute("performanceAdvancementPercentage", performanceAdvancementPercentage);

      pageContext.setAttribute("gStandardizedMean",
                               GlobalParameters.getDoubleGlobalParameter(connection,
                                                                         GlobalParameters.STANDARDIZED_MEAN));

      pageContext.setAttribute("gStandardizedSigma",
                               GlobalParameters.getDoubleGlobalParameter(connection,
                                                                         GlobalParameters.STANDARDIZED_SIGMA));

      pageContext.setAttribute("gDivisionFlipRate",
                               GlobalParameters.getIntGlobalParameter(connection, GlobalParameters.DIVISION_FLIP_RATE));

      pageContext.setAttribute("gUseQuartiles", GlobalParameters.getUseQuartilesInRankingReport(connection));

      pageContext.setAttribute("gAllTeamsMsPerRow", GlobalParameters.getAllTeamsMsPerRow(connection));
      pageContext.setAttribute("gHeadToHeadMsPerRow", GlobalParameters.getHeadToHeadMsPerRow(connection));

      pageContext.setAttribute("gMhubHostname", MhubParameters.getHostname(connection));
      pageContext.setAttribute("gMhubPort", MhubParameters.getPort(connection));
      pageContext.setAttribute("gMhubDisplayNode", MhubParameters.getDisplayNode(connection));

    } finally {
      SQLFunctions.close(connection);
    }
  }
}
