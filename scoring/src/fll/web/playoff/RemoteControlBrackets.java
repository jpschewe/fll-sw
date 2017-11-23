/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Team;
import fll.db.GlobalParameters;
import fll.util.FLLInternalException;
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

    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    // can't close the database connection here as it's used inside
    // bracketData to create the output, which is called after this scope exits
    try {
      final Connection connection = datasource.getConnection();

      pageContext.setAttribute("maxNameLength", Team.MAX_TEAM_NAME_LEN);

      final List<BracketData> allBracketData = new LinkedList<>();

      int numRows = 0;
      for (final DisplayInfo.H2HBracketDisplay h2hBracket : displayInfo.getBrackets()) {
        final BracketData bracketData = new BracketData(connection, h2hBracket.getBracket(), h2hBracket.getFirstRound(),
                                                        h2hBracket.getFirstRound()
                                                            + 2,
                                                        4, false, true, h2hBracket.getIndex());

        bracketData.addBracketLabels(h2hBracket.getFirstRound());
        bracketData.addStaticTableLabels();

        numRows += bracketData.getNumRows();

        allBracketData.add(bracketData);
      }

      pageContext.setAttribute("allBracketData", allBracketData);

      // expose allBracketData to the javascript
      final ObjectMapper jsonMapper = new ObjectMapper();
      final StringWriter writer = new StringWriter();
      try {
        jsonMapper.writeValue(writer, allBracketData);
      } catch (final IOException e) {
        throw new FLLInternalException("Error writing JSON for allBracketData", e);
      }
      final String allBracketDataJson = writer.toString();
      pageContext.setAttribute("allBracketDataJson", allBracketDataJson);

      // used for scroll control
      final int msPerRow = GlobalParameters.getHeadToHeadMsPerRow(connection);
      final int scrollDuration = numRows
          * msPerRow;

      pageContext.setAttribute("scrollDuration", scrollDuration);
    } catch (final SQLException sqle) {
      LOGGER.error("Error talking to the database", sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    }

  }

}
