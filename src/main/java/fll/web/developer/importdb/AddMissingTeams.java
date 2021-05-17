/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.developer.importdb;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import fll.Team;
import fll.db.Queries;

import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;

/**
 * Add teams after promptCreateMissingTeams.jsp.
 * 
 * @author jpschewe
 */
@WebServlet("/developer/importdb/AddMissingTeams")
public class AddMissingTeams extends BaseFLLServlet {

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

    final Collection<Team> missingTeams = sessionInfo.getMissingTeams();

    final DataSource destDataSource = ApplicationAttributes.getDataSource(application);

    try (Connection sourceConnection = sourceDataSource.getConnection();
        Connection destConnection = destDataSource.getConnection()) {

      for (final Team team : missingTeams) {
        // ignore the duplicate return value, if the team is already in the database it
        // will all work out in ExecuteImport
        Queries.addTeam(destConnection, team.getTeamNumber(), team.getTeamName(), team.getOrganization());
      }

      session.setAttribute(SessionAttributes.REDIRECT_URL, "CheckTeamInfo");

    } catch (final SQLException sqle) {
      LOG.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    }

    session.setAttribute("message", message.toString());
    WebUtils.sendRedirect(response, session);
  }
}
