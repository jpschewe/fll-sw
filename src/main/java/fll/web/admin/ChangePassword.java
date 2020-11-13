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
import java.util.Objects;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import org.apache.commons.codec.digest.DigestUtils;

import fll.db.Authentication;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.CookieUtils;
import fll.web.SessionAttributes;

/**
 * Java code to handle changing of passwords.
 */
@WebServlet("/admin/ChangePassword")
public class ChangePassword extends BaseFLLServlet {

  /**
   * @param request used to read parameters
   * @param application get application variables
   * @param pageContext set page variables
   */
  public static void populateContext(final HttpServletRequest request,
                                     final ServletContext application,
                                     final PageContext pageContext) {

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final Collection<String> loginKeys = CookieUtils.findLoginKey(request);
      final String user = Authentication.checkValidLogin(connection, loginKeys);
      pageContext.setAttribute("fll_user", user);

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
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final Collection<String> loginKeys = CookieUtils.findLoginKey(request);
      final String user = Authentication.checkValidLogin(connection, loginKeys);

      final String passwordHash = Authentication.getHashedPassword(connection, user);
      final String oldPassword = request.getParameter("old_password");
      final String hashedOldPass = DigestUtils.md5Hex(oldPassword);
      if (!Objects.equals(passwordHash, hashedOldPass)) {
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

      final String newPasswordHash = DigestUtils.md5Hex(newPassword);

      // invalidate all login keys now that the password has changed
      Authentication.changePassword(connection, user, newPasswordHash);
      Authentication.removeValidLoginByUser(connection, user);

      SessionAttributes.appendToMessage(session, "<p id='success'>Password changed for '"
          + user
          + "', you will now need to login again.</p>");
      response.sendRedirect(response.encodeRedirectURL("changePassword.jsp"));

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }

  }

}
