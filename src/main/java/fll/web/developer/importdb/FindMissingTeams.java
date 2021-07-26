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
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.sql.DataSource;

import fll.Team;
import fll.db.ImportDB;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;

/**
 * Servlet to find teams that are missing via
 * {@link ImportDB#findMissingTeams(Connection, Connection, String)}.
 * 
 * @author jpschewe
 */
@WebServlet("/developer/importdb/FindMissingTeams")
public class FindMissingTeams extends BaseFLLServlet {

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

    final String tournament = sessionInfo.getTournamentName();
    if (null == tournament) {
      throw new FLLInternalException("Missing tournament to import");
    }

    final DataSource sourceDataSource = sessionInfo.getImportDataSource();

    final DataSource destDataSource = ApplicationAttributes.getDataSource(application);

    try (Connection sourceConnection = sourceDataSource.getConnection();
        Connection destConnection = destDataSource.getConnection()) {

      final List<Team> missingTeams = ImportDB.findMissingTeams(sourceConnection, destConnection, tournament);
      if (missingTeams.isEmpty()) {
        session.setAttribute(SessionAttributes.REDIRECT_URL, "CheckTeamInfo");
      } else {
        sessionInfo.setMissingTeams(missingTeams);
        session.setAttribute(ImportDBDump.IMPORT_DB_SESSION_KEY, sessionInfo);
        session.setAttribute(SessionAttributes.REDIRECT_URL, "promptCreateMissingTeams.jsp");
      }
    } catch (final SQLException sqle) {
      LOG.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    }

    session.setAttribute("message", message.toString());
    WebUtils.sendRedirect(response, session);
  }

}
