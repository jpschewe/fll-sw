/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import fll.db.NonNumericNominees;
import fll.db.Queries;
import fll.web.ApplicationAttributes;

/**
 * Helper functions for edit-award-winners.jsp.
 */
public final class EditAwardWinners {

  private EditAwardWinners() {
  }

  /**
   * Populate the page context with information for the jsp.
   * pageContext:
   * <ul>
   * <li>nonNumericCategories - javascript list of categories</li>
   * </ul>
   * 
   * @param application used for application variables
   * @param pageContext populated with the specified variables
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext)
      throws SQLException {

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final int tournamentId = Queries.getCurrentTournament(connection);
      final Set<String> nonNumericCategories = NonNumericNominees.getCategories(connection, tournamentId);
      final String nonNumericCategoriesStr = nonNumericCategories.stream().collect(Collectors.joining("\", \"", "\"", "\""));
      final StringBuilder str = new StringBuilder();
      str.append("[");
      str.append(nonNumericCategoriesStr);
      str.append("]");
      pageContext.setAttribute("nonNumericCategories", str.toString());
    }
  }

}
