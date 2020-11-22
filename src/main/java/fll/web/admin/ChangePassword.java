/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import fll.db.Authentication;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Java code to handle changing of passwords.
 */
@WebServlet("/admin/ChangePassword")
public class ChangePassword extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    final AuthenticationContext currentAuth = SessionAttributes.getAuthentication(session);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final String oldPassword = request.getParameter("old_password");
      if (!Authentication.checkValidPassword(connection, currentAuth.getUsername(), oldPassword)) {
        SessionAttributes.appendToMessage(session, "<p class='error'>Old password is incorrect</p>");
        response.sendRedirect(response.encodeRedirectURL("changePassword.jsp"));
        return;
      }

      final String newPassword = request.getParameter("pass");
      final String newPasswordCheck = request.getParameter("pass_check");
      if (!Objects.equals(newPassword, newPasswordCheck)) {
        SessionAttributes.appendToMessage(session, "<p class='error'>New passwords don't match</p>");
        response.sendRedirect(response.encodeRedirectURL("changePassword.jsp"));
        return;
      }

      // invalidate all login keys now that the password has changed
      Authentication.changePassword(connection, currentAuth.getUsername(), newPassword);
      Authentication.markLoggedOut(application, currentAuth.getUsername());

      // need to create new auth after marking logged out so that this session isn't
      // logged out
      final AuthenticationContext newAuth = AuthenticationContext.loggedIn(currentAuth.getUsername(),
                                                                           currentAuth.getRoles());

      SessionAttributes.appendToMessage(session, "<p id='success'>Password changed for '"
          + newAuth.getUsername()
          + "'. Other sessions will be logged out.</p>");
      response.sendRedirect(response.encodeRedirectURL("changePassword.jsp"));

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }

  }

}
