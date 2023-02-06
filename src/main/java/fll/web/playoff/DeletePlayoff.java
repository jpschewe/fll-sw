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

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
 * Delete a playoff bracket.
 */
@WebServlet("/playoff/DeletePlayoff")
public class DeletePlayoff extends BaseFLLServlet {

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
      try {

        final int tournamentID = Queries.getCurrentTournament(connection);

        final String bracketName = request.getParameter("division");
        if (null == bracketName
            || "".equals(bracketName)) {
          SessionAttributes.appendToMessage(session,
                                            "<p class='error'>No playoff bracket specified to uninitialize</p>");
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
          getMinMaxRunPrep.setString(2, bracketName);
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

        final boolean initialized = Queries.isPlayoffDataInitialized(connection, tournamentID, bracketName);
        final int playoffMaxPerformanceRound = Playoff.getMaxPerformanceRound(connection, tournamentID);
        final int maxPerformanceRound = Queries.getMaxRunNumber(connection, tournamentID);
        if (!initialized
            || (maxRun == playoffMaxPerformanceRound
                && maxPerformanceRound <= maxRun)) {

          UninitializePlayoff.uninitializePlayoffBracket(connection, tournamentID, bracketName, minRun, maxRun);

          try (
              PreparedStatement delete = connection.prepareStatement("DELETE FROM playoff_bracket_teams WHERE tournament_id = ? AND bracket_name = ?")) {
            delete.setInt(1, tournamentID);
            delete.setString(2, bracketName);
            delete.executeUpdate();
          }

          connection.commit();

          LOGGER.info("Deleted playoff bracket "
              + bracketName);

          SessionAttributes.appendToMessage(session, "<p id='success'>Deleted playoff bracket "
              + bracketName
              + ".</p>");
        } else {
          SessionAttributes.appendToMessage(session, "<p class='error'>Unable to delete playoff bracket "
              + bracketName
              + ".</p>");
        }

        WebUtils.sendRedirect(application, response, "/playoff/index.jsp");

      } finally {
        connection.setAutoCommit(oldAutocommit);
      }
    } catch (final SQLException e) {
      LOGGER.error(e.getMessage(), e);
      throw new FLLRuntimeException("Database error uninitializing playoffs", e);
    }
  }
}
