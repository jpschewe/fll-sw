/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Utilities;
import fll.db.NonNumericNominees;
import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;
import fll.web.api.NonNumericNomineesServlet;

/**
 * Store the data from the non-numeric nominees page.
 */
@WebServlet("/report/StoreNonNumericNominees")
public class StoreNonNumericNominees extends BaseFLLServlet {

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

    final StringBuilder message = new StringBuilder();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final int tournament = Queries.getCurrentTournament(connection);

      // get parameters
      final String nomineesStr = request.getParameter("non-numeric-nominees_data");
      if (null == nomineesStr
          || "".equals(nomineesStr)) {
        throw new FLLRuntimeException("Parameter 'non-numeric-nominees_data' cannot be null");
      }

      // decode JSON
      final ObjectMapper jsonMapper = Utilities.createJsonMapper();

      LOGGER.debug("Storing nominees: {}", nomineesStr);

      final Collection<NonNumericNominees> nominees = jsonMapper.readValue(nomineesStr,
                                                                           NonNumericNomineesServlet.NonNumericNomineesTypeInformation.INSTANCE);
      for (final NonNumericNominees nominee : nominees) {
        nominee.store(connection, tournament);
      }

      message.append("<p id='success'>Non-numeric nominees saved to the database</p>");

    } catch (final SQLException e) {
      message.append("<p class='error'>Error saving non-numeric nominees into the database: "
          + e.getMessage()
          + "</p>");
      LOGGER.error(e, e);
      throw new RuntimeException("Error saving subjective data into the database", e);
    }

    session.setAttribute("message", message.toString());
    response.sendRedirect(response.encodeRedirectURL("non-numeric-nominees.jsp"));

  }

}
