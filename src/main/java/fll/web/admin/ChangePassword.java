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

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.db.Authentication;
import fll.web.ApplicationAttributes;
import fll.web.AuthenticationContext;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;
import fll.web.WebUtils;

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
    if (!currentAuth.getLoggedIn()) {
      SessionAttributes.appendToMessage(session, "<p id='error'>You must be logged in to change a password</p>");
      response.sendRedirect(response.encodeRedirectURL("changePassword.jsp"));
      return;
    }

    // if logged in, this cannot be null
    final String username = castNonNull(currentAuth.getUsername());

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final String oldPassword = WebUtils.getNonNullRequestParameter(request, "old_password");
      if (!Authentication.checkValidPassword(connection, username, oldPassword)) {
        SessionAttributes.appendToMessage(session, "<p class='error'>Old password is incorrect</p>");
        response.sendRedirect(response.encodeRedirectURL("changePassword.jsp"));
        return;
      }

      final String newPassword = WebUtils.getNonNullRequestParameter(request, "pass");
      final String newPasswordCheck = WebUtils.getNonNullRequestParameter(request, "pass_check");
      if (!Objects.equals(newPassword, newPasswordCheck)) {
        SessionAttributes.appendToMessage(session, "<p class='error'>New passwords don't match</p>");
        response.sendRedirect(response.encodeRedirectURL("changePassword.jsp"));
        return;
      }

      // invalidate all login keys now that the password has changed
      Authentication.changePassword(connection, username, newPassword);
      Authentication.markLoggedOut(application, username);

      // need to create new auth after marking logged out so that this session isn't
      // logged out
      final AuthenticationContext newAuth = AuthenticationContext.loggedIn(username, currentAuth.getRoles());

      SessionAttributes.appendToMessage(session, "<p id='success'>Password changed for '"
          + newAuth.getUsername()
          + "'. Other sessions will be logged out.</p>");
      response.sendRedirect(response.encodeRedirectURL("changePassword.jsp"));

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }

  }

}
