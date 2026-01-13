/*
 * This code is released under GPL; see LICENSE for details.
 */

package fll.web.admin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import javax.sql.DataSource;

import fll.db.Authentication;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.PageContext;

/**
 * Helper for {@code user-info.jsp}.
 */
public final class UserInfo {

  private UserInfo() {
  }

  public static void populateContext(final ServletContext application,
                                     final PageContext page) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Collection<Authentication.UserInformation> users = Authentication.getUserInfo(connection);
      page.setAttribute("users", users);
    } catch (final SQLException e) {
      throw new FLLRuntimeException(e);
    }
  }
}
