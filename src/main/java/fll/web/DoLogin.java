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
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.commons.codec.digest.DigestUtils;

import fll.db.Queries;

/**
 * Handle login credentials and if incorrect redirect back to login page.
 */
@WebServlet("/DoLogin")
public class DoLogin extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    doLogin(request, response, application, session);
  }

  /**
   * Does the work of login. Exists as a separate method so that it can be
   * called from {@link fll.web.admin.CreateUser}
   *
   * @param request the servlet request
   * @param response the servlet response
   * @param application the application context
   * @param session the session context
   * @throws IOException if there is an error writing to the response
   * @throws ServletException if there is an error getting data from the
   *           application context
   */
  public static void doLogin(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final ServletContext application,
                             final HttpSession session)
      throws IOException, ServletException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    try (final Connection connection = datasource.getConnection()) {

      // check for authentication table
      if (Queries.isAuthenticationEmpty(connection)) {
        LOGGER.warn("No authentication information in the database");
        SessionAttributes.appendToMessage(session,
                                          "<p class='error'>No authentication information in the database - see administrator</p>");
        response.sendRedirect(response.encodeRedirectURL("login.jsp"));
        return;
      }

      // compute hash
      final String user = request.getParameter("user");
      final String pass = request.getParameter("pass");
      if (null == user
          || user.isEmpty()
          || null == pass
          || pass.isEmpty()) {
        LOGGER.warn("Form fields missing");
        SessionAttributes.appendToMessage(session, "<p class='error'>You must fill out all fields in the form.</p>");
        response.sendRedirect(response.encodeRedirectURL("/login.jsp"));
        return;
      }
      final String hashedPass = DigestUtils.md5Hex(pass);

      // compare login information
      LOGGER.trace("Checking user: {} hashedPass: {}", user, hashedPass);
      final Map<String, String> authInfo = Queries.getAuthInfo(connection);
      for (final Map.Entry<String, String> entry : authInfo.entrySet()) {
        if (user.equals(entry.getKey())
            && hashedPass.equals(entry.getValue())) {
          // clear out old login cookies first
          CookieUtils.clearLoginCookies(application, request, response);

          final String magicKey = String.valueOf(System.currentTimeMillis());
          Queries.addValidLogin(connection, user, magicKey);
          CookieUtils.setLoginCookie(response, magicKey);

          String redirect = SessionAttributes.getRedirectURL(session);
          if (null == redirect) {
            redirect = "/index.jsp";
          }
          LOGGER.trace("Redirecting to {} with message '{}'", redirect, SessionAttributes.getMessage(session));
          response.sendRedirect(response.encodeRedirectURL(redirect));
          return;
        } else {
          LOGGER.trace("Didn't match user: {} pass: {}", entry.getKey(), entry.getValue());
        }
      }

      LOGGER.warn("Incorrect login credentials user: {}", user);
      SessionAttributes.appendToMessage(session, "<p class='error'>Incorrect login information provided</p>");
      response.sendRedirect(response.encodeRedirectURL("/login.jsp"));
      return;
    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }
  }

}
