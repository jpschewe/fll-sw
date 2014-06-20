/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.Tournament;
import fll.db.Queries;
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

  public static void populateContext(final ServletContext application,
                                     final HttpSession session) {
    final StringBuilder message = new StringBuilder();
    final String existingMessage = SessionAttributes.getMessage(session);
    if (null != existingMessage) {
      message.append(existingMessage);
    }

    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    ResultSet rs = null;
    ResultSet rs2 = null;
    Statement stmt = null;
    PreparedStatement prep = null;
    Connection connection = null;
    try {
      connection = datasource.getConnection();
      stmt = connection.createStatement();

      final int currentTournamentID = Queries.getCurrentTournament(connection);
      session.setAttribute("currentTournamentID", currentTournamentID);

      session.setAttribute("playoffsInitialized",
                           Queries.isPlayoffDataInitialized(connection, Queries.getCurrentTournament(connection)));

      final int numSeedingRounds = Queries.getNumSeedingRounds(connection, currentTournamentID);
      session.setAttribute("numSeedingRounds", numSeedingRounds);

      final List<Tournament> tournaments = Tournament.getTournaments(connection);
      session.setAttribute("tournaments", tournaments);

      boolean teamsUploaded = false;
      rs = stmt.executeQuery("SELECT COUNT(*) FROM Teams WHERE TeamNumber >= 0");
      while (rs.next()) {
        final int count = rs.getInt(1);
        teamsUploaded = count > 0;
      }
      session.setAttribute("teamsUploaded", teamsUploaded);

      session.setAttribute("scheduleUploaded",
                           TournamentSchedule.scheduleExistsInDatabase(connection, currentTournamentID));

      session.setAttribute("judgesAssigned", Queries.isJudgesProperlyAssigned(connection, challengeDescription));

      final boolean tablesAssigned = Tables.tablesAssigned(connection, currentTournamentID);
      session.setAttribute("tablesAssigned", tablesAssigned);

    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage() + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving team data into the database", sqle);
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(rs2);
      SQLFunctions.close(stmt);
      SQLFunctions.close(prep);
      SQLFunctions.close(connection);
    }

    session.setAttribute(SessionAttributes.MESSAGE, message.toString());
  }

}
