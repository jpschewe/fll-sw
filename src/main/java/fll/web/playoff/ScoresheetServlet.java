/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.xml.ChallengeDescription;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Displays a score sheet.
 */
@WebServlet("/playoff/ScoresheetServlet")
public class ScoresheetServlet extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.REF), false)) {
      return;
    }

    final @Nullable String editTablesParam = request.getParameter("editTables");
    final boolean editTablesOnly = Boolean.valueOf(editTablesParam);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
      final int tournament = Queries.getCurrentTournament(connection);
      if (!editTablesOnly) {
        response.reset();
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "filename=scoreSheet.pdf");
      }

      // Create the scoresheet generator - must provide correct number of
      // scoresheets
      final ScoresheetGenerator gen = new ScoresheetGenerator(request, connection, tournament, challengeDescription);

      if (!editTablesOnly) {
        gen.writeFile(response.getOutputStream());
      } else {
        // send back to the same page
        SessionAttributes.appendToMessage(session, "<div class='success'>Table assignments updated</div>");
        final String referrer = request.getHeader("Referer");
        response.sendRedirect(response.encodeRedirectURL(StringUtils.isEmpty(referrer) ? "index.jsp" : referrer));
      }

    } catch (final SQLException e) {
      final String errorMessage = "There was an error talking to the database";
      LOGGER.error(errorMessage, e);
      throw new FLLRuntimeException(errorMessage, e);
    }
  }

}
