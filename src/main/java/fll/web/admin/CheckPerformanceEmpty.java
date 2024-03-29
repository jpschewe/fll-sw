/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

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

/**
 * Check that there isn't any performance data in the database. If there is,
 * make the user confirm before allowing the database to be imported.
 */
@WebServlet("/admin/CheckPerformanceEmpty")
public class CheckPerformanceEmpty extends BaseFLLServlet {

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
      final int tournamentId = Queries.getCurrentTournament(connection);

      if (hasScores(connection, tournamentId)) {
        response.sendRedirect(response.encodeRedirectURL("confirm-import-with-performance-scores.jsp"));
        return;
      } else {
        // insert into the import workflow after tournament verification
        final String redirect = String.format("%s/developer/importdb/FindMissingTeams", request.getContextPath());
        response.sendRedirect(response.encodeRedirectURL(redirect));
      }
    } catch (

    final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    }
  }

  private boolean hasScores(final Connection connection,
                            final int tournamentId)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT COUNT(*) FROM Performance "
        + " WHERE TOURNAMENT = ?")) {
      prep.setInt(1, tournamentId);
      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          return rs.getInt(1) > 0;
        } else {
          return false;
        }
      }
    }
  }

}
