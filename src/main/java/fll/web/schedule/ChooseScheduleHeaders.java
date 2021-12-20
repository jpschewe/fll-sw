/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.schedule;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import fll.Tournament;
import fll.db.TournamentParameters;
import fll.scheduler.SchedParams;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.PageContext;

/**
 * Support for chooseScheduleHeaders.jsp.
 */
public final class ChooseScheduleHeaders {

  private ChooseScheduleHeaders() {
  }

  /**
   * Setup page variables used by the JSP.
   * 
   * @param application application attributes
   * @param pageContext set page variables
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {
    pageContext.setAttribute("default_duration", SchedParams.DEFAULT_SUBJECTIVE_MINUTES);

    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);

      final int numSeedingRounds = TournamentParameters.getNumSeedingRounds(connection, tournament.getTournamentID());
      pageContext.setAttribute("numSeedingRounds", numSeedingRounds);

      final int numPracticeRounds = TournamentParameters.getNumPracticeRounds(connection, tournament.getTournamentID());
      pageContext.setAttribute("numPracticeRounds", numPracticeRounds);

    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    }
  }
}
