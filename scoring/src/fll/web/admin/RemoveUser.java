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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.CookieUtils;
import fll.web.SessionAttributes;

/**
 * Java code to handle removing of users.
 */
@WebServlet("/admin/RemoveUser")
public class RemoveUser extends BaseFLLServlet {

  public static void populateContext(final HttpServletRequest request,
                                     final ServletContext application,
                                     final PageContext pageContext) {

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final Collection<String> loginKeys = CookieUtils.findLoginKey(request);
      final String user = Queries.checkValidLogin(connection, loginKeys);
      pageContext.setAttribute("fll_user", user);

      final Collection<String> users = Queries.getUsers(connection);
      pageContext.setAttribute("all_users", users);

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(connection);
    }

  }

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session) throws IOException, ServletException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final String userToRemove = request.getParameter("remove_user");
      if (null != userToRemove
          && !userToRemove.isEmpty()) {
        Queries.removeUser(connection, userToRemove);

        session.setAttribute(SessionAttributes.MESSAGE, "<p id='success'>Removed user '"
            + userToRemove);

      }

      response.sendRedirect(response.encodeRedirectURL("removeUser.jsp"));

    } catch (final SQLException e) {
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(connection);
    }

  }

}
