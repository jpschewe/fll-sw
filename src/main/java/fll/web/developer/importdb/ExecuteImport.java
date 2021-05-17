/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.developer.importdb;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import fll.Tournament;
import fll.db.GlobalParameters;
import fll.db.ImportDB;
import fll.db.Queries;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;
import fll.xml.ChallengeDescription;

/**
 * Servlet to do the actual import.
 * 
 * @author jpschewe
 */
@WebServlet("/developer/importdb/ExecuteImport")
public class ExecuteImport extends BaseFLLServlet {

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

    final DataSource destDataSource = ApplicationAttributes.getDataSource(application);

    final String tournament = sessionInfo.getTournamentName();
    if (null == tournament) {
      throw new FLLInternalException("Missing tournament to import");
    }

    try (Connection sourceConnection = sourceDataSource.getConnection();
        Connection destConnection = destDataSource.getConnection()) {

      final boolean differences = ImportDB.checkForDifferences(sourceConnection, destConnection, tournament);
      if (differences) {
        message.append("<p class='error'>Error, there are still differences that need to be resolved before the import can be completed.</p>");
        session.setAttribute(SessionAttributes.REDIRECT_URL, "CheckTeamInfo");
      } else {
        ImportDB.importDatabase(sourceConnection, destConnection, tournament, sessionInfo.isImportPerformance(),
                                sessionInfo.isImportSubjective(), sessionInfo.isImportFinalist());

        // update score totals
        final Tournament destTournament = Tournament.findTournamentByName(destConnection, tournament);
        final int destTournamentID = destTournament.getTournamentID();
        final ChallengeDescription description = GlobalParameters.getChallengeDescription(destConnection);
        Queries.updateScoreTotals(description, destConnection, destTournamentID);

        message.append(String.format("<p>Import of tournament %s successful.</p>", tournament));
        session.setAttribute(SessionAttributes.REDIRECT_URL, sessionInfo.getRedirectURL());

        session.removeAttribute(ImportDBDump.IMPORT_DB_SESSION_KEY);
      }

    } catch (final SQLException sqle) {
      LOG.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    }

    session.setAttribute("message", message.toString());
    WebUtils.sendRedirect(response, session);
  }

}
