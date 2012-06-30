/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
import org.w3c.dom.Document;

import fll.Tournament;
import fll.db.Queries;
import fll.scheduler.TournamentSchedule;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Index page for admin.
 */
@WebServlet("/admin/index.jsp")
public class AdminIndex extends BaseFLLServlet {

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

    final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);

    final DataSource datasource = SessionAttributes.getDataSource(session);
    ResultSet rs = null;
    ResultSet rs2 = null;
    Statement stmt = null;
    PreparedStatement prep = null;
    try {
      final Connection connection = datasource.getConnection();
      stmt = connection.createStatement();

      final int currentTournamentID = Queries.getCurrentTournament(connection);
      session.setAttribute("currentTournamentID", currentTournamentID);

      session.setAttribute("playoffsInitialized",
                           Queries.isPlayoffDataInitialized(connection, Queries.getCurrentTournament(connection)));

      final int scoresheetsPerPage = Queries.getScoresheetLayoutNUp(connection);
      session.setAttribute("scoressheetsPerPage", scoresheetsPerPage);

      final int numSeedingRounds = Queries.getNumSeedingRounds(connection, currentTournamentID);
      session.setAttribute("numSeedingRounds", numSeedingRounds);

      session.setAttribute("servletLoaded", true);

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

      session.setAttribute("judgesAssigned", Queries.isJudgesProperlyAssigned(connection, challengeDocument));

      boolean tablesAssigned = false;
      prep = connection.prepareStatement("SELECT COUNT(*) FROM tablenames WHERE Tournament = ?");
      prep.setInt(1, currentTournamentID);
      rs2 = prep.executeQuery();
      while (rs2.next()) {
        final int count = rs2.getInt(1);
        tablesAssigned = count > 0;
      }
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
    }

    session.setAttribute(SessionAttributes.MESSAGE, message.toString());
    response.sendRedirect(response.encodeRedirectURL("admin-index.jsp"));
  }

}
