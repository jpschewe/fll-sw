/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.developer.importdb;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;

import fll.Tournament;
import fll.web.SessionAttributes;

/**
 * Populate page context for selectTournament.jsp.
 */
public final class SelectTournament {
  private SelectTournament() {
  }

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * @param session session variables
   * @param page page variables
   */
  public static void populateContext(final HttpSession session,
                                     final PageContext page) {

    final ImportDbSessionInfo sessionInfo = SessionAttributes.getNonNullAttribute(session,
                                                                                  ImportDBDump.IMPORT_DB_SESSION_KEY,
                                                                                  ImportDbSessionInfo.class);

    try (Connection connection = sessionInfo.getImportDataSource().getConnection()) {

      final List<Tournament> tournaments = Tournament.getTournaments(connection);
      page.setAttribute("tournaments", tournaments);

      final String selectedTournamentName = Tournament.getCurrentTournament(connection).getName();
      page.setAttribute("selectedTournament", selectedTournamentName);

      if (sessionInfo.isImportSubjective()) {
        page.setAttribute("importSubjectiveChecked", "checked");
      } else {
        page.setAttribute("importSubjectiveChecked", "");
      }

      if (sessionInfo.isImportPerformance()) {
        page.setAttribute("importPerformanceChecked", "checked");
      } else {
        page.setAttribute("importPerformanceChecked", "");
      }

      if (sessionInfo.isImportFinalist()) {
        page.setAttribute("importFinalistChecked", "checked");
      } else {
        page.setAttribute("importFinalistChecked", "");
      }

    } catch (final SQLException e) {
      LOGGER.error("There was an error talking to the database", e);
      throw new RuntimeException("There was an error talking to the database", e);
    }
  }

}
