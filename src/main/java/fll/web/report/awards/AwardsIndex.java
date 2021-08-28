/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.awards;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import javax.sql.DataSource;

import fll.Tournament;
import fll.TournamentLevel;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.PageContext;

/**
 * Support code for the awards index page.
 */
public final class AwardsIndex {

  private AwardsIndex() {
  }

  /**
   * @param application read application variables
   * @param page set page variables
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext page) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Collection<Tournament> tournaments = Tournament.getTournaments(connection);
      page.setAttribute("tournaments", tournaments);

      final Collection<TournamentLevel> tournamentLevels = TournamentLevel.getAllLevels(connection);
      page.setAttribute("tournamentLevels", tournamentLevels);
    } catch (final SQLException e) {
      throw new FLLInternalException(e);
    }
  }
}
