/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

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
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import fll.db.Authentication;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.UserRole;

/**
 * Java code to handle removing of users.
 */
@WebServlet("/admin/RemoveUser")
public class RemoveUser extends BaseFLLServlet {

  /**
   * @param application get application variables
   * @param pageContext write page variables
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final Collection<String> users = Authentication.getUsers(connection);
      pageContext.setAttribute("all_users", users);

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }

  }

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

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final String userToRemove = request.getParameter("remove_user");
      if (null != userToRemove
          && !userToRemove.isEmpty()) {
        Authentication.removeUser(connection, userToRemove);
        Authentication.markLoggedOut(application, userToRemove);

        SessionAttributes.appendToMessage(session, "<p id='success'>Removed user '"
            + userToRemove);
      }

      response.sendRedirect(response.encodeRedirectURL("removeUser.jsp"));

    } catch (final SQLException e) {
      throw new FLLRuntimeException(e);
    }

  }

}
