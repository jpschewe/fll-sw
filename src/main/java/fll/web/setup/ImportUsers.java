/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.setup;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;

import fll.db.Authentication;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.web.BaseFLLServlet;
import fll.web.SessionAttributes;

/**
 * Process form data from import-users.jsp.
 */
@WebServlet("/setup/ImportUsers")
public class ImportUsers extends BaseFLLServlet {

  @Override
  protected void processRequest(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final ServletContext application,
                                final HttpSession session)
      throws IOException, ServletException {

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      @SuppressWarnings("unchecked")
      final Collection<CreateDB.UserAccount> previousAccounts = (Collection<CreateDB.UserAccount>) SessionAttributes.getNonNullAttribute(session,
                                                                                                                                         CreateDB.PREVIOUS_ACCOUNTS,
                                                                                                                                         Collection.class);
      session.removeAttribute(CreateDB.PREVIOUS_ACCOUNTS);

      for (final CreateDB.UserAccount account : previousAccounts) {
        if (!StringUtils.isBlank(request.getParameter(account.getUsername()))) {
          Authentication.addAccount(connection, account);
        }
      }

      final String redirect;
      if (Authentication.getAdminUsers(connection).isEmpty()) {
        redirect = "/admin/createUsername.jsp?ADMIN";
      } else {
        redirect = "/admin/ask-create-admin.jsp";
      }
      response.sendRedirect(response.encodeRedirectURL(request.getContextPath()
          + redirect));
    } catch (final SQLException e) {
      throw new FLLInternalException("Error importing users into the database", e);
    }

  }

}
