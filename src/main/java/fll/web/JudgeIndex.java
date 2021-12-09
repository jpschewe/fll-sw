/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import javax.sql.DataSource;

import fll.Tournament;
import fll.db.Queries;
import fll.util.FLLRuntimeException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.PageContext;

/**
 * Helper functions for the judge index.
 */
public final class JudgeIndex {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private JudgeIndex() {
  }

  /**
   * @param application application variables
   * @param pageContext set page variables
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);
      final Collection<String> awardGroups = Queries.getAwardGroups(connection, tournament.getTournamentID());
      pageContext.setAttribute("awardGroups", awardGroups);
    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new FLLRuntimeException(e);
    }

  }

}
