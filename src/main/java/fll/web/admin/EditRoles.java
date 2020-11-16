/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;

/**
 * Help with editing of roles.
 */
@WebServlet("/admin/EditRoles")
public class EditRoles extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * @param application application variables
   * @param pageContext setup variables for the page
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {
    pageContext.setAttribute("possibleRoles", UserRole.values());

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final Collection<String> users = Authentication.getUsers(connection);
      pageContext.setAttribute("users", users);

      final Map<String, Set<UserRole>> userRoles = new HashMap<>();
      for (final String user : users) {
        final Set<UserRole> roles = Authentication.getRoles(connection, user);
        userRoles.put(user, roles);
      }
      pageContext.setAttribute("userRoles", userRoles);
    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error getting role information from the database", e);
    }

  }

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final Collection<String> users = Authentication.getUsers(connection);
      for (final String user : users) {
        final Set<UserRole> roles = Arrays.stream(UserRole.values()) //
                                          .filter(role -> null != request.getParameter(String.format("%s_%s", user,
                                                                                                     role.name()))) //
                                          .collect(Collectors.toSet());

        final Set<UserRole> currentRoles = Authentication.getRoles(connection, user);
        if (!roles.equals(currentRoles)) {
          LOGGER.debug("Setting roles for '{}' to {}", user, roles);
          Authentication.setRoles(connection, user, roles);

          Authentication.markRefreshNeeded(application, user);
        }
      }

      SessionAttributes.appendToMessage(session,
                                        "<p class='success' id='success-edit-roles'>Successfully edited roles</p>");
      response.sendRedirect(response.encodeRedirectURL("index.jsp"));

    } catch (final SQLException e) {
      throw new ServletException("Error talking to the database", e);
    }
  }

}
