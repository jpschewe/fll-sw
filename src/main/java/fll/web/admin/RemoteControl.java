/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.PageContext;
import javax.sql.DataSource;

import fll.Tournament;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.playoff.Playoff;
import fll.web.report.finalist.FinalistSchedule;

/**
 * Context information for remoteControl.jsp.
 */
public final class RemoteControl {

  private RemoteControl() {
  }

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * @param application read and write application variables
   * @param pageContext write page variables
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Tournament currentTournament = Tournament.getCurrentTournament(connection);

      final List<String> divisions = Playoff.getPlayoffBrackets(connection, currentTournament.getTournamentID());
      pageContext.setAttribute("divisions", divisions);

      if (null == application.getAttribute("playoffDivision")
          && !divisions.isEmpty()) {
        application.setAttribute("playoffDivision", divisions.get(0));
      }
      if (null == application.getAttribute("playoffRoundNumber")) {
        application.setAttribute("playoffRoundNumber", Integer.valueOf(1));
      }
      if (null == application.getAttribute("slideShowInterval")) {
        application.setAttribute("slideShowInterval", Integer.valueOf(10));
      }
      if (null == application.getAttribute(ApplicationAttributes.DISPLAY_PAGE)) {
        application.setAttribute(ApplicationAttributes.DISPLAY_PAGE, "welcome");
      }

      pageContext.setAttribute("numPlayoffRounds",
                               Queries.getNumPlayoffRounds(connection, currentTournament.getTournamentID()));

      final Collection<String> finalistDivisions = FinalistSchedule.getAllDivisions(connection,
                                                                                    currentTournament.getTournamentID());
      pageContext.setAttribute("finalistDivisions", finalistDivisions);

      pageContext.setAttribute("allJudgingGroups",
                               Queries.getJudgingStations(connection, currentTournament.getTournamentID()));

    } catch (final SQLException e) {
      LOGGER.error(e.getMessage(), e);
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
