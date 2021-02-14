/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

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

import org.apache.commons.lang3.StringUtils;

import fll.db.GlobalParameters;
import fll.flltools.MhubParameters;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;

/**
 * Submit action for edit_global_parameters.jsp.
 */
@WebServlet("/admin/StoreGlobalParameters")
public class StoreGlobalParameters extends BaseFLLServlet {

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

      GlobalParameters.setDoubleGlobalParameter(connection, GlobalParameters.STANDARDIZED_MEAN,
                                                Double.valueOf(request.getParameter("gStandardizedMean")));

      GlobalParameters.setDoubleGlobalParameter(connection, GlobalParameters.STANDARDIZED_SIGMA,
                                                Double.valueOf(request.getParameter("gStandardizedSigma")));

      GlobalParameters.setIntGlobalParameter(connection, GlobalParameters.DIVISION_FLIP_RATE,
                                             Integer.parseInt(request.getParameter("gDivisionFlipRate")));

      GlobalParameters.setAllTeamsMsPerRow(connection, Integer.parseInt(request.getParameter("gAllTeamsMsPerRow")));

      GlobalParameters.setHeadToHeadMsPerRow(connection, Integer.parseInt(request.getParameter("gHeadToHeadMsPerRow")));

      final String mhubHostname = request.getParameter("gMhubHostname");
      if (StringUtils.isBlank(mhubHostname)) {
        MhubParameters.setHostname(connection, null);
      } else {
        MhubParameters.setHostname(connection, mhubHostname.trim());
      }

      MhubParameters.setPort(connection, Integer.parseInt(request.getParameter("gMhubPort")));

      MhubParameters.setDisplayNode(connection, request.getParameter("gMhubDisplayNode"));

      if (message.length() == 0) {
        message.append("<p id='success'>Parameters saved</p>");
      }
    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage()
          + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving team data into the database", sqle);
    }

    SessionAttributes.appendToMessage(session, message.toString());
    response.sendRedirect(response.encodeRedirectURL("edit_global_parameters.jsp"));
  }
}
