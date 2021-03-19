/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import fll.db.Authentication;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;

/**
 * Create a user.
 */
@WebServlet("/admin/CreateUser")
public class CreateUser extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Set some variables for createUsername.jsp to use.
   * 
   * @param request used to get parameters
   * @param pageContext where to store page variables
   */
  public static void populateContext(final HttpServletRequest request,
                                     final PageContext pageContext) {
    pageContext.setAttribute("possibleRoles", UserRole.values());

    final Set<UserRole> selectedRoles = new HashSet<>();
    for (final UserRole role : UserRole.values()) {
      if (null != request.getParameter(role.name())) {
        selectedRoles.add(role);
      }
    }
    pageContext.setAttribute("selectedRoles", selectedRoles);
  }

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final AuthenticationContext auth = SessionAttributes.getAuthentication(session);

    if (!auth.requireRoles(request, response, session, Set.of(UserRole.ADMIN), true)) {
      return;
    }

    LOGGER.trace("Top of CreateUser");

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final String user = request.getParameter("user");
      final String pass = request.getParameter("pass");
      final String passCheck = request.getParameter("pass_check");
      if (null == pass
          || null == passCheck
          || null == user
          || user.isEmpty()
          || pass.isEmpty()
          || passCheck.isEmpty()) {
        LOGGER.debug("Missing information on form");
        SessionAttributes.appendToMessage(session, "<p class='error'>You must enter all information in the form.</p>");
        response.sendRedirect(response.encodeRedirectURL("createUsername.jsp"));
        return;
      }

      if (!pass.equals(passCheck)) {
        LOGGER.debug("Password check doesn't match");
        SessionAttributes.appendToMessage(session, "<p class='error'>Password and password check do not match.</p>");
        response.sendRedirect(response.encodeRedirectURL("createUsername.jsp"));
        return;
      }

      final Collection<String> existingUsers = Authentication.getUsers(connection);
      if (existingUsers.contains(user)) {
        LOGGER.debug("User already exists");
        SessionAttributes.appendToMessage(session, "<p class='error'>Username '"
            + user
            + "' already exists.</p>");
        response.sendRedirect(response.encodeRedirectURL("createUsername.jsp"));
        return;
      }

      Authentication.addUser(connection, user, pass);

      final Set<UserRole> selectedRoles = Arrays.stream(UserRole.values()) //
                                                .filter(r -> null != request.getParameter(String.format("role_%s",
                                                                                                        r.name()))) //
                                                .collect(Collectors.toSet());
      Authentication.setRoles(connection, user, selectedRoles);

      LOGGER.debug("Created user");
      SessionAttributes.appendToMessage(session,
                                        "<p class='success' id='success-create-user'>Successfully created user '"
                                            + user
                                            + "'</p>");

      // do a login if not already logged in
      if (!auth.getLoggedIn()) {
        LOGGER.info("Logged in new user {}", user);
        final AuthenticationContext newAuth = AuthenticationContext.loggedIn(user, selectedRoles);
        session.setAttribute(SessionAttributes.AUTHENTICATION, newAuth);
      }

      LOGGER.debug("Redirecting to index");
      response.sendRedirect(response.encodeRedirectURL(request.getContextPath()
          + "/index.jsp"));

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }

  }
}
