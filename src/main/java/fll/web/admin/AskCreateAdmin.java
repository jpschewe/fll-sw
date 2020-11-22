/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import fll.db.Authentication;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;

/**
 * Helper for ask-create-admin.jsp.
 */
public final class AskCreateAdmin {

  private AskCreateAdmin() {
  }

  /**
   * @param application application variables
   * @param pageContext expose variables to the page
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final Collection<String> adminUsers = Authentication.getAdminUsers(connection);
      pageContext.setAttribute("adminUsers", adminUsers);
    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error getting admin users from the database", e);
    }

  }
}
