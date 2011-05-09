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
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import fll.db.Queries;

/**
 * Log a user out of the application.
 * 
 * @web.servlet name="DoLogout"
 * @web.servlet-mapping url-pattern="/DoLogout"
 */
public class DoLogout extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final Cookie loginCookie = CookieUtils.findLoginCookie(request);
    if (null != loginCookie) {
      final DataSource datasource = SessionAttributes.getDataSource(session);
      try {
        final Connection connection = datasource.getConnection();
        Queries.removeValidLogin(connection, loginCookie.getValue());

        response.sendRedirect(response.encodeRedirectURL(request.getContextPath()));
      } catch (final SQLException e) {
        throw new RuntimeException(e);
      }
    }

  }

}
