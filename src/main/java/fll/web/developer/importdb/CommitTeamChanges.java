/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.developer.importdb;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import fll.db.Queries;
import fll.db.TeamPropertyDifference;
import fll.db.TeamPropertyDifference.TeamProperty;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;

/**
 * Commit changes made on resolveMissingTeams.jsp.
 * 
 * @author jpschewe
 */
@WebServlet("/developer/importdb/CommitTeamChanges")
public class CommitTeamChanges extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager.getLogger();

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

    final StringBuilder message = new StringBuilder();
    final ImportDbSessionInfo sessionInfo = SessionAttributes.getNonNullAttribute(session,
                                                                                  ImportDBDump.IMPORT_DB_SESSION_KEY,
                                                                                  ImportDbSessionInfo.class);

    final DataSource sourceDataSource = sessionInfo.getImportDataSource();
    Objects.requireNonNull(sourceDataSource, "Missing import data source");

    final DataSource destDataSource = ApplicationAttributes.getDataSource(application);

    final List<TeamPropertyDifference> teamDifferences = sessionInfo.getTeamDifferences();

    try (Connection sourceConnection = sourceDataSource.getConnection();
        Connection destConnection = destDataSource.getConnection()) {

      for (int idx = 0; idx < teamDifferences.size(); ++idx) {
        final TeamPropertyDifference difference = teamDifferences.get(idx);
        final String userChoice = request.getParameter(String.valueOf(idx));
        if (null == userChoice) {
          throw new RuntimeException("Missing paramter '"
              + idx
              + "' when committing team change");
        } else if ("source".equals(userChoice)) {
          applyDifference(destConnection, difference.getTeamNumber(), difference.getProperty(),
                          difference.getSourceValue());
        } else if ("dest".equals(userChoice)) {
          applyDifference(sourceConnection, difference.getTeamNumber(), difference.getProperty(),
                          difference.getDestValue());
        } else {
          throw new RuntimeException(String.format("Unknown value '%s' for choice of parameter '%d'", userChoice, idx));
        }

      }

      session.setAttribute(SessionAttributes.REDIRECT_URL, "CheckTeamInfo");

    } catch (final SQLException sqle) {
      LOG.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    }

    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL(SessionAttributes.getRedirectURL(session)));
  }

  /**
   * Apply a change to a database.
   * 
   * @param connection the database to change
   * @param teamNumber the team to change
   * @param property the property on the team to change
   * @param value the new value
   */
  private void applyDifference(final Connection connection,
                               final int teamNumber,
                               final TeamProperty property,
                               final String value)
      throws SQLException {
    switch (property) {
    case NAME:
      Queries.updateTeamName(connection, teamNumber, value);
      break;
    case ORGANIZATION:
      Queries.updateTeamOrganization(connection, teamNumber, value);
      break;
    default:
      throw new IllegalArgumentException("Unknown property "
          + property);
    }

  }
}
