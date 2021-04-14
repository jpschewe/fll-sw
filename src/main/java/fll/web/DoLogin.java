/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import fll.db.Authentication;
import fll.util.FLLRuntimeException;

/**
 * Handle login credentials and if incorrect redirect back to login page.
 */
@WebServlet("/DoLogin")
public class DoLogin extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Store the form parameters to be used by a redirect. Also track the URL to
   * redirect back to.
   * 
   * @param request the request
   * @param session the session to store the parameters in
   */
  public static void storeParameters(final HttpServletRequest request,
                                     final HttpSession session) {
    final Object origUriObj = request.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
    final String origUri;
    if (null != origUriObj) {
      origUri = origUriObj.toString();
    } else {
      origUri = null;
    }

    FormParameterStorage.storeParameters(request, session);

    session.setAttribute(SessionAttributes.REDIRECT_URL, origUri);

    LOGGER.debug("Request is for {}", request.getRequestURI());
    LOGGER.debug("forward.request_uri: {}", request.getAttribute("javax.servlet.forward.request_uri"));
    LOGGER.debug("forward.context_path: {}", request.getAttribute("javax.servlet.forward.context_path"));
    LOGGER.debug("forward.servlet_path: {}", request.getAttribute("javax.servlet.forward.servlet_path"));
    LOGGER.debug("forward.path_info: {}", request.getAttribute("javax.servlet.forward.path_info"));
    LOGGER.debug("forward.query_string: {}", request.getAttribute("javax.servlet.forward.query_string"));
  }

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

    try (Connection connection = datasource.getConnection()) {

      // check for authentication table
      if (Authentication.isAuthenticationEmpty(connection)) {
        LOGGER.warn("No authentication information in the database");
        SessionAttributes.appendToMessage(session,
                                          "<p class='error'>No authentication information in the database - see administrator</p>");
        response.sendRedirect(response.encodeRedirectURL("login.jsp"));
        return;
      }

      LOGGER.trace("Form parameters: {}", request.getParameterMap());
      final String user = request.getParameter("user");

      // compute hash
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

      // check for locked account
      if (Authentication.isAccountLocked(connection, user)) {
        LOGGER.warn("Account {} is locked", user);
        SessionAttributes.appendToMessage(session,
                                          "<p class='error'>Account is locked. Wait for it to unlock or contact an admin.</p>");
        response.sendRedirect(response.encodeRedirectURL("/login.jsp"));
        return;
      }

      if (Authentication.checkValidPassword(connection, user, pass)) {
        Authentication.recordSuccessfulLogin(connection, user);

        // store authentication information
        LOGGER.info("User {} logged in", user);
        final Set<UserRole> roles = Authentication.getRoles(connection, user);
        final AuthenticationContext newAuth = AuthenticationContext.loggedIn(user, roles);
        session.setAttribute(SessionAttributes.AUTHENTICATION, newAuth);

        String redirect = SessionAttributes.getRedirectURL(session);
        if (null == redirect) {
          redirect = "/index.jsp";
        }
        // clear the attribute since it's been used
        session.removeAttribute(SessionAttributes.REDIRECT_URL);

        LOGGER.trace("Redirecting to {} with message '{}'", redirect, SessionAttributes.getMessage(session));
        response.sendRedirect(response.encodeRedirectURL(redirect));
      } else {
        Authentication.recordFailedLogin(connection, user);

        LOGGER.warn("Incorrect login credentials user: {}", user);
        SessionAttributes.appendToMessage(session, "<p class='error'>Incorrect login information provided</p>");
        response.sendRedirect(response.encodeRedirectURL("/login.jsp"));
      }

    } catch (final SQLException e) {
      throw new FLLRuntimeException(e);
    }
  }

}
