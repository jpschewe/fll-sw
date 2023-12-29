/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import fll.db.Queries;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.PageContext;

/**
 * Java code for scoreEntry/select_team.jsp.
 */
public final class SelectTeam {

  private SelectTeam() {
  }

  /**
   * @param application get application variables
   * @param pageContext set page variables
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      try (
          PreparedStatement prep = connection.prepareStatement("SELECT MAX(RunNumber) FROM Performance WHERE Tournament = ?")) {
        prep.setInt(1, Queries.getCurrentTournament(connection));
        try (ResultSet rs = prep.executeQuery()) {
          final int maxRunNumber;
          if (rs.next()) {
            maxRunNumber = rs.getInt(1);
          } else {
            maxRunNumber = 1;
          }
          pageContext.setAttribute("maxRunNumber", maxRunNumber);
        } // result set
      } // prepared statement

    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    }
  }

}
