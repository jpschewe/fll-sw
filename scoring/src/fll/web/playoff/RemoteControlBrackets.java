/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import fll.Team;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.DisplayInfo;

/**
 * Data for remoteControlBrackets.jsp.
 */
public class RemoteControlBrackets {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final PageContext pageContext) {
    final DisplayInfo displayInfo = DisplayInfo.getInfoForDisplay(application, session);
    // store the brackets to know when a refresh is required
    session.setAttribute("brackets", displayInfo.getBrackets());

    final DisplayInfo.H2HBracketDisplay h2hBracket = displayInfo.getBrackets().get(0); // HACK
                                                                                       // just
                                                                                       // for
                                                                                       // now

    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    // can't close the database connection here as it's used inside
    // bracketInfo to create the output, which is called after this scope exits
    try {
      final Connection connection = datasource.getConnection();

      pageContext.setAttribute("maxNameLength", Team.MAX_TEAM_NAME_LEN);

      // FIXME will need a list of these eventually
      final BracketData bracketInfo = new BracketData(connection, h2hBracket.getBracket(), h2hBracket.getFirstRound(),
                                                      h2hBracket.getFirstRound()
                                                          + 2,
                                                      4, false, true);

      bracketInfo.addBracketLabels(h2hBracket.getFirstRound());
      bracketInfo.addStaticTableLabels();

      pageContext.setAttribute("bracketInfo", bracketInfo);

      // FIXME needs to be the sum of numRows for all brackets
      pageContext.setAttribute("numRows", bracketInfo.getNumRows());

    } catch (final SQLException sqle) {
      LOGGER.error("Error talking to the database", sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    }

  }

}
