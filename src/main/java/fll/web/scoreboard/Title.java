/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreboard;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;
import javax.sql.DataSource;

import fll.Tournament;
import fll.db.Queries;
import fll.web.ApplicationAttributes;
import fll.web.DisplayInfo;

/**
 * Helper for title.jsp.
 */
public final class Title {

  private Title() {
  }

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Populate the context for the JSP.
   *
   * @param application application context
   * @param session session context
   * @param pageContext page context
   */
  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final PageContext pageContext) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);

      final DisplayInfo displayInfo = DisplayInfo.getInfoForDisplay(application, session);

      final List<String> allJudgingGroups = Queries.getJudgingStations(connection, tournament.getTournamentID());
      final List<String> judgingGroupsToDisplay = displayInfo.determineScoreboardJudgingGroups(allJudgingGroups);

      final String judgingGroupTitle = String.join(", ", judgingGroupsToDisplay);

      pageContext.setAttribute("judgeGroupTitle", judgingGroupTitle);
    } catch (final SQLException sqle) {
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    }
  }

}
