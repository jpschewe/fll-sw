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

import javax.sql.DataSource;

import fll.Tournament;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.WebUtils;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Servlet to check if the tournament exists in the dest database.
 * 
 * @author jpschewe
 */
@WebServlet("/developer/importdb/CheckTournamentExists")
public class CheckTournamentExists extends BaseFLLServlet {

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

    // support finding the selected tournament in the session as well
    final String selectedTournamentParam = request.getParameter("tournament");
    final String selectedTournament;
    if (null == selectedTournamentParam) {
      selectedTournament = sessionInfo.getTournamentName();
    } else {
      selectedTournament = selectedTournamentParam;
    }

    if (null != request.getParameter("submit_tournament")) {
      // just came from selectTournament.jsp
      sessionInfo.setImportSubjective(null != request.getParameter("importSubjective"));
      sessionInfo.setImportPerformance(null != request.getParameter("importPerformance"));
      sessionInfo.setImportFinalist(null != request.getParameter("importFinalist"));
      sessionInfo.setImportAwardsScript(null != request.getParameter("importAwardsScript"));

      if (LOG.isDebugEnabled()) {
        LOG.debug("subjective: {}", request.getParameter("importSubjective"));
        LOG.debug("performance: {}", request.getParameter("importPerformance"));
        LOG.debug("finalist: {}", request.getParameter("importFinalist"));
        LOG.debug("finalist: {}", request.getParameter("importAwardsScript"));
        LOG.debug("import subjective: {} performance: {} finalist: {} awards script: {}",
                  sessionInfo.isImportSubjective(), sessionInfo.isImportPerformance(), sessionInfo.isImportFinalist());
      }
    }

    final String redirectUrl;
    if (null != selectedTournament) {
      sessionInfo.setTournamentName(selectedTournament);

      // Check if the tournament exists
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      try (Connection connection = datasource.getConnection()) {
        if (!Tournament.doesTournamentExist(connection, selectedTournament)) {
          redirectUrl = "promptCreateTournament.jsp";
        } else {
          redirectUrl = "FindMissingTeams";
        }
      } catch (final SQLException sqle) {
        LOG.error(sqle, sqle);
        throw new FLLRuntimeException("Error talking to the database", sqle);
      }

      session.setAttribute(ImportDBDump.IMPORT_DB_SESSION_KEY, sessionInfo);
    } else {
      throw new FLLRuntimeException("Can't find the 'tournament' parameter");
    }

    session.setAttribute("message", message.toString());
    WebUtils.sendRedirect(response, redirectUrl);
  }

}
