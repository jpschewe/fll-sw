/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import fll.db.Queries;

/**
 * Handle login credentials and if incorrect redirect back to login page.
 * 
 * @web.servlet name="DoLogin"
 * @web.servlet-mapping url-pattern="/DoLogin"
 */
public class DoLogin extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {

    final DataSource datasource = (DataSource) session.getAttribute(SessionAttributes.DATASOURCE);
    try {
      final Connection connection = datasource.getConnection();

      // check for authentication table
      if (Queries.isAuthenticationEmpty(connection)) {
        session.setAttribute(SessionAttributes.MESSAGE,
                             "<p class='error'>No authentication information in the database - see administrator</p>");
        response.sendRedirect(response.encodeRedirectURL(request.getContextPath()
            + "/login.jsp"));
        return;
      }

      // compute hash

      // compare login information

      session.setAttribute(SessionAttributes.MESSAGE, "<p class='error'>Incorrect login information provided</p>");
      response.sendRedirect(response.encodeRedirectURL(request.getContextPath()
          + "/login.jsp"));
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }
  }

}
