/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreboard;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import fll.Tournament;
import fll.TournamentTeam;
import fll.Utilities;
import fll.db.GlobalParameters;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.SessionAttributes;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;

/**
 * Support for allteams.jsp.
 */
public final class AllTeams {

  private AllTeams() {
  }

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static final int TEAMS_BETWEEN_LOGOS = 2;

  /**
   * @param application application variables
   * @param session session variables
   * @param pageContext page variables
   */
  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final PageContext pageContext) {
    final StringBuilder message = new StringBuilder();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final Tournament tournament = Tournament.getCurrentTournament(connection);
      final int tournamentId = tournament.getTournamentID();
      final Map<Integer, TournamentTeam> tournamentTeams = Queries.getTournamentTeams(connection, tournamentId);

      final List<String> allAwardGroups = Queries.getAwardGroups(connection, tournamentId);

      final List<TournamentTeam> allTeams = new LinkedList<>(tournamentTeams.values());
      final Map<Integer, String> teamHeaderColor = new HashMap<>();
      for (final Map.Entry<Integer, TournamentTeam> entry : tournamentTeams.entrySet()) {
        final TournamentTeam team = entry.getValue();

        final String headerColor = Dynamic.getColorForAwardGroup(team.getAwardGroup(),
                                                                 allAwardGroups.indexOf(team.getAwardGroup()));
        teamHeaderColor.put(entry.getKey(), headerColor);
        allTeams.add(entry.getValue());
      } // foreach tournament team

      final double scrollRate = GlobalParameters.getAllTeamScrollRate(connection);
      pageContext.setAttribute("scrollRate", scrollRate);

      final List<String> sponsorLogos = getSponsorLogos(application);
      pageContext.setAttribute("sponsorLogos", sponsorLogos);

      pageContext.setAttribute("teamsBetweenLogos", Integer.valueOf(TEAMS_BETWEEN_LOGOS));
      pageContext.setAttribute("allTeams", allTeams);
      pageContext.setAttribute("teamHeaderColor", teamHeaderColor);

    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage()
          + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    } finally {
      SessionAttributes.appendToMessage(session, message.toString());
    }

  }

  /**
   * Get the URsponsor logo filenames relative to "/sponsor_logos".
   *
   * @return sorted sponsor logos list
   */
  private static List<String> getSponsorLogos(final ServletContext application) {
    final String imagePath = application.getRealPath("/sponsor_logos");

    final List<String> logoFiles = Utilities.getGraphicFiles(new File(imagePath));

    return logoFiles;
  }

}
