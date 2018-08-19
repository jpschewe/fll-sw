/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import fll.Tournament;
import fll.db.Queries;
import fll.db.TournamentParameters;
import fll.scheduler.TournamentSchedule;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.SessionAttributes;
import fll.xml.ChallengeDescription;

/**
 * Populate context for admin index.
 */
public class AdminIndex {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Populate page context with variables for the admin index page.
   * 
   * @param application the application context
   * @param session the session context
   * @param pageContext populated with variables
   */
  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final PageContext pageContext) {
    final StringBuilder message = new StringBuilder();
    final String existingMessage = SessionAttributes.getMessage(session);
    if (null != existingMessage) {
      message.append(existingMessage);
    }

    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection(); Statement stmt = connection.createStatement()) {

      final int currentTournamentID = Queries.getCurrentTournament(connection);
      pageContext.setAttribute("currentTournament", Tournament.findTournamentByID(connection, currentTournamentID));

      final int numSeedingRounds = TournamentParameters.getNumSeedingRounds(connection, currentTournamentID);
      pageContext.setAttribute("numSeedingRounds", numSeedingRounds);

      final List<Tournament> tournaments = Tournament.getTournaments(connection);
      pageContext.setAttribute("tournaments", tournaments);

      boolean teamsUploaded = false;
      try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Teams WHERE TeamNumber >= 0")) {
        while (rs.next()) {
          final int count = rs.getInt(1);
          teamsUploaded = count > 0;
        }
      }
      pageContext.setAttribute("teamsUploaded", teamsUploaded);

      pageContext.setAttribute("scheduleUploaded",
                               TournamentSchedule.scheduleExistsInDatabase(connection, currentTournamentID));

      pageContext.setAttribute("judgesAssigned", Queries.isJudgesProperlyAssigned(connection, challengeDescription));

      final boolean tablesAssigned = Tables.tablesAssigned(connection, currentTournamentID);
      pageContext.setAttribute("tablesAssigned", tablesAssigned);

    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage()
          + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    }

    session.setAttribute(SessionAttributes.MESSAGE, message.toString());
  }

}
