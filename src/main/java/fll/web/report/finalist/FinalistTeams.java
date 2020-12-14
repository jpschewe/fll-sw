/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.finalist;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import fll.Team;
import fll.TournamentTeam;
import fll.db.GlobalParameters;
import fll.db.Queries;
import fll.web.ApplicationAttributes;

/**
 * Support for FinalistTeams.jsp.
 */
public final class FinalistTeams {

  private FinalistTeams() {
  }

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * @param application application variables
   * @param pageContext page variables
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final int tournament = Queries.getCurrentTournament(connection);

      final SortedSet<TournamentTeam> teams = new TreeSet<>(Team.TEAM_NUMBER_COMPARATOR);

      final Map<Integer, TournamentTeam> allTeams = Queries.getTournamentTeams(connection, tournament);

      int numRows = 0;
      for (final String division : FinalistSchedule.getAllDivisions(connection, tournament)) {

        final FinalistSchedule schedule = new FinalistSchedule(connection, tournament, division);
        for (final FinalistDBRow row : schedule.getSchedule()) {
          final TournamentTeam team = allTeams.get(row.getTeamNumber());
          if (null != team) {
            ++numRows;
            teams.add(team);
          } else {
            LOGGER.warn("Cannot find team for number: "
                + row.getTeamNumber()
                + " in finalist teams, skipping");
          }
        } // foreach schedule row

      } // foreach division

      pageContext.setAttribute("teams", teams);

      // used for scroll control
      final int msPerRow = GlobalParameters.getHeadToHeadMsPerRow(connection);
      final int scrollDuration = numRows
          * msPerRow;

      pageContext.setAttribute("scrollDuration", scrollDuration);

    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new RuntimeException(e);
    }
  }

}
