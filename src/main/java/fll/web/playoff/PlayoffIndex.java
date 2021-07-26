/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.sql.Connection;
import java.sql.SQLException;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;
import javax.sql.DataSource;

import fll.db.Queries;
import fll.db.TournamentParameters;
import fll.web.ApplicationAttributes;
import fll.web.SessionAttributes;
import fll.web.admin.Tables;

/**
 * Populate context for playoff index page.
 */
public final class PlayoffIndex {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private PlayoffIndex() {
  }

  /**
   * Instance of {@link PlayoffSessionData} is stored here.
   */
  public static final String SESSION_DATA = "playoff_data";

  /**
   * @param application get application variables
   * @param session get and set session variables
   * @param pageContext set page variables
   */
  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final PageContext pageContext) {

    final StringBuilder message = new StringBuilder();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      final int currentTournamentID = Queries.getCurrentTournament(connection);

      pageContext.setAttribute("runningHeadToHead",
                               TournamentParameters.getRunningHeadToHead(connection, currentTournamentID));

      final PlayoffSessionData data = new PlayoffSessionData(connection);
      session.setAttribute(SESSION_DATA, data);

      final boolean tablesAssigned = Tables.tablesAssigned(connection, currentTournamentID);
      pageContext.setAttribute("tablesAssigned", tablesAssigned);

    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage()
          + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving team data into the database", sqle);
    }

    SessionAttributes.appendToMessage(session, message.toString());
  }

}
