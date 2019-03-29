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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;



import fll.Team;
import fll.db.ImportDB;

import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Servlet to find teams that are missing via
 * {@link ImportDB#findMissingTeams(Connection, Connection, String)}.
 * 
 * @author jpschewe
 */
@WebServlet("/developer/importdb/FindMissingTeams")
public class FindMissingTeams extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOG = org.apache.logging.log4j.LogManager.getLogger();

  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final StringBuilder message = new StringBuilder();

    final ImportDbSessionInfo sessionInfo = SessionAttributes.getNonNullAttribute(session,
                                                                                  ImportDBDump.IMPORT_DB_SESSION_KEY,
                                                                                  ImportDbSessionInfo.class);

    final String tournament = sessionInfo.getTournamentName();
    Objects.requireNonNull(tournament, "Missing tournament to import");

    final DataSource sourceDataSource = sessionInfo.getImportDataSource();
    Objects.requireNonNull(sourceDataSource, "Missing import data source");

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
    response.sendRedirect(response.encodeRedirectURL(SessionAttributes.getNonNullAttribute(session, "redirect_url",
                                                                                           String.class)));
  }

}
