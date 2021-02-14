/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;

/**
 * Uninitialize a playoff division.
 */
@WebServlet("/playoff/UninitializePlayoff")
public class UninitializePlayoff extends BaseFLLServlet {

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

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final boolean oldAutocommit = connection.getAutoCommit();
      connection.setAutoCommit(false);

      final int tournamentID = Queries.getCurrentTournament(connection);

      final String division = request.getParameter("division");
      if (null == division
          || "".equals(division)) {
        SessionAttributes.appendToMessage(session, "<p class='error'>No playoff bracket specified to uninitialize</p>");
        WebUtils.sendRedirect(application, response, "/playoff/index.jsp");
        return;
      }

      final int minRun;
      final int maxRun;
      try (
          PreparedStatement getMinMaxRunPrep = connection.prepareStatement("SELECT MIN(run_number), MAX(run_number) FROM PlayoffData" //
              + " WHERE tournament = ?" //
              + " AND event_division = ?")) {
        getMinMaxRunPrep.setInt(1, tournamentID);
        getMinMaxRunPrep.setString(2, division);
        try (ResultSet getMinMaxRunResult = getMinMaxRunPrep.executeQuery()) {
          if (getMinMaxRunResult.next()) {
            minRun = getMinMaxRunResult.getInt(1);
            maxRun = getMinMaxRunResult.getInt(2);
          } else {
            minRun = -1;
            maxRun = -1;
          }
        }
      }

      if (minRun != -1
          && maxRun != -1) {
        try (PreparedStatement deletePerformance = connection.prepareStatement("DELETE FROM Performance" //
            + " WHERE runNumber >= ?"//
            + " AND runNumber <= ?" //
            + " AND tournament = ?" //
            + " AND teamnumber IN (" //
            + "  SELECT DISTINCT team" //
            + "    FROM PlayoffData" //
            + "    WHERE event_division = ? )")) {
          deletePerformance.setInt(1, minRun);
          deletePerformance.setInt(2, maxRun);
          deletePerformance.setInt(3, tournamentID);
          deletePerformance.setString(4, division);
          deletePerformance.executeUpdate();
        }
      }

      try (
          PreparedStatement deletePlayoff = connection.prepareStatement("DELETE FROM PlayoffData WHERE event_division = ? AND tournament = ?")) {
        deletePlayoff.setString(1, division);
        deletePlayoff.setInt(2, tournamentID);
        deletePlayoff.executeUpdate();
      }

      try (
          PreparedStatement deleteAutoFinish = connection.prepareStatement("DELETE FROM automatic_finished_playoff WHERE tournament_id = ? and bracket_name = ?")) {
        deleteAutoFinish.setInt(1, tournamentID);
        deleteAutoFinish.setString(2, division);
        deleteAutoFinish.executeUpdate();
      }

      connection.commit();

      connection.setAutoCommit(oldAutocommit);

      LOGGER.info("Uninitialized playoff division "
          + division);

      SessionAttributes.appendToMessage(session, "<p id='success'>Uninitialized playoff bracket "
          + division
          + ".</p>");
      WebUtils.sendRedirect(application, response, "/playoff/index.jsp");
    } catch (final SQLException e) {
      LOGGER.error(e.getMessage(), e);
      throw new FLLRuntimeException("Database error uninitializing playoffs", e);
    }
  }
}
