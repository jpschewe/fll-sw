/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.commons.codec.digest.DigestUtils;

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

    final DataSource datasource = SessionAttributes.getDataSource(session);
    try {
      final Connection connection = datasource.getConnection();

      // check for authentication table
      if (Queries.isAuthenticationEmpty(connection)) {
        session.setAttribute(SessionAttributes.MESSAGE,
                             "<p class='error'>No authentication information in the database - see administrator</p>");
        response.sendRedirect(response.encodeRedirectURL("login.jsp"));
        return;
      }

      // compute hash
      final String user = request.getParameter("user");
      final String pass = request.getParameter("pass");
      if (null == user
          || user.isEmpty() || null == pass || pass.isEmpty()) {
        session.setAttribute(SessionAttributes.MESSAGE,
                             "<p class='error'>You must fill out all fields in the form.</p>");
        response.sendRedirect(response.encodeRedirectURL("login.jsp"));
        return;
      }
      final String hashedPass = DigestUtils.md5Hex(pass);

      // compare login information
      final Map<String, String> authInfo = Queries.getAuthInfo(connection);
      for (final Map.Entry<String, String> entry : authInfo.entrySet()) {
        if (user.equals(entry.getKey())
            && hashedPass.equals(entry.getValue())) {
          final String magicKey = String.valueOf(System.currentTimeMillis());
          Queries.addValidLogin(connection, magicKey);
          CookieUtils.setLoginCookie(response, magicKey);

          response.sendRedirect(response.encodeRedirectURL(SessionAttributes.getRedirectURL(session)));
          return;
        }
      }

      session.setAttribute(SessionAttributes.MESSAGE, "<p class='error'>Incorrect login information provided</p>");
      response.sendRedirect(response.encodeRedirectURL("login.jsp"));
      return;
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }
  }

}
