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

import fll.db.ImportDB;
import fll.db.TeamPropertyDifference;

import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;

/**
 * Servlet to check team information between the source and dest database.
 * 
 * @author jpschewe
 */
@WebServlet("/developer/importdb/CheckTeamInfo")
public class CheckTeamInfo extends BaseFLLServlet {

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

    final String tournament = sessionInfo.getTournamentName();
    Objects.requireNonNull(tournament, "Missing tournament name to import");

    final DataSource destDataSource = ApplicationAttributes.getDataSource(application);

    try (Connection sourceConnection = sourceDataSource.getConnection();
        Connection destConnection = destDataSource.getConnection()) {

      final List<TeamPropertyDifference> teamDifferences = ImportDB.checkTeamInfo(sourceConnection, destConnection,
                                                                                  tournament);
      if (teamDifferences.isEmpty()) {
        session.setAttribute(SessionAttributes.REDIRECT_URL, "ExecuteImport");
      } else {
        sessionInfo.setTeamDifferences(teamDifferences);
        session.setAttribute(ImportDBDump.IMPORT_DB_SESSION_KEY, sessionInfo);
        session.setAttribute(SessionAttributes.REDIRECT_URL, "resolveTeamInfoDifferences.jsp");
      }
    } catch (final SQLException sqle) {
      LOG.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    }

    session.setAttribute("message", message.toString());
    WebUtils.sendRedirect(response, session);
  }

}
