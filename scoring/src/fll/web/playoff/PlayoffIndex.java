/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.apache.log4j.Logger;

import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.SessionAttributes;
import fll.web.admin.Tables;

/**
 * Populate context for playoff index page.
 */
public class PlayoffIndex {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * Instance of {@link PlayoffSessionData} is stored here.
   */
  public static final String SESSION_DATA = "playoff_data";

  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final PageContext pageContext) {

    final StringBuilder message = new StringBuilder();
    final String existingMessage = SessionAttributes.getMessage(session);
    if (null != existingMessage) {
      message.append(existingMessage);
    }

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      final int currentTournamentID = Queries.getCurrentTournament(connection);

      final PlayoffSessionData data = new PlayoffSessionData(connection);
      session.setAttribute(SESSION_DATA, data);

      final boolean tablesAssigned = Tables.tablesAssigned(connection, currentTournamentID);
      pageContext.setAttribute("tablesAssigned", tablesAssigned);

    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage() + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error saving team data into the database", sqle);
    } finally {
      SQLFunctions.close(connection);
    }

    session.setAttribute(SessionAttributes.MESSAGE, message.toString());
  }

}
