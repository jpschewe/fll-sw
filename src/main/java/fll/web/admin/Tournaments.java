/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.web.admin;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.PageContext;
import javax.sql.DataSource;



import fll.Tournament;
import fll.db.GenerateDB;

import fll.web.ApplicationAttributes;

/**
 * Java code used in /admin/tournaments.jsp.
 */
public final class Tournaments {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * The key used for new tournaments so that {@link StoreTournamentData} knows
   * when to add a tournament rather than modify one.
   */
  public static final int NEW_TOURNAMENT_KEY = -1;

  private Tournaments() {
    // no instances
  }

  /**
   * Populate page context with variables for /admin/tournaments.jsp.
   *
   * @param application the application context
   * @param pageContext populated with variables
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection(); Statement stmt = connection.createStatement()) {

      final List<Tournament> tournaments = Tournament.getTournaments(connection).stream()
                                                     .filter(t -> !GenerateDB.DUMMY_TOURNAMENT_NAME.equals(t.getName())
                                                         && !GenerateDB.DROP_TOURNAMENT_NAME.equals(t.getName()))
                                                     .collect(Collectors.toList());
      pageContext.setAttribute("tournaments", tournaments);

      pageContext.setAttribute("newTournamentId", NEW_TOURNAMENT_KEY);
    } catch (final SQLException sqle) {
      LOGGER.error(sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    }
  }

}
