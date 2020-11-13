/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.commons.codec.digest.DigestUtils;

import fll.db.Authentication;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.CookieUtils;
import fll.web.SessionAttributes;

/**
 * Create a user if.
 */
@WebServlet("/admin/CreateUser")
public class CreateUser extends BaseFLLServlet {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

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

      try (
          PreparedStatement checkUser = connection.prepareStatement("SELECT fll_user FROM fll_authentication WHERE fll_user = ?")) {
        checkUser.setString(1, user);
        try (ResultSet rs = checkUser.executeQuery()) {
          if (rs.next()) {
            LOGGER.debug("User already exists");
            SessionAttributes.appendToMessage(session, "<p class='error'>Username '"
                + user
                + "' already exists.</p>");
            response.sendRedirect(response.encodeRedirectURL("createUsername.jsp"));
            return;
          }
        }
      }

      final String hashedPass = DigestUtils.md5Hex(pass);
      try (
          PreparedStatement addUser = connection.prepareStatement("INSERT INTO fll_authentication (fll_user, fll_pass) VALUES(?, ?)")) {
        addUser.setString(1, user);
        addUser.setString(2, hashedPass);
        addUser.executeUpdate();
      }

      LOGGER.debug("Created user");
      SessionAttributes.appendToMessage(session,
                                        "<p class='success' id='success-create-user'>Successfully created user '"
                                            + user
                                            + "'</p>");

      // do a login if not already logged in
      final Collection<String> loginKeys = CookieUtils.findLoginKey(request);
      final String authenticatedUser = Authentication.checkValidLogin(connection, loginKeys);
      if (null == authenticatedUser) {
        LOGGER.debug("Doing login");
        request.getRequestDispatcher("/DoLogin").forward(request, response);
      } else {
        LOGGER.debug("Redirecting to index");
        response.sendRedirect(response.encodeRedirectURL("index.jsp"));
      }

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    }

  }
}
