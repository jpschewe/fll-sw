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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Utilities;
import fll.db.Queries;
import fll.util.FLLInternalException;

import fll.web.ApplicationAttributes;
import fll.web.WebUtils;
import fll.web.playoff.BracketData.TopRightCornerStyle;

/**
 * Data for adminbrackets.jsp.
 */
public final class AdminBrackets {

  private AdminBrackets() {
  }

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * @param request request parameters
   * @param application application variables
   * @param pageContext page variables
   */
  public static void populateContext(final HttpServletRequest request,
                                     final ServletContext application,
                                     final PageContext pageContext) {

    final DataSource datasource = ApplicationAttributes.getDataSource(application);

    try (Connection connection = datasource.getConnection()) {

      final String divisionStr = request.getParameter("division");
      if (null == divisionStr) {
        throw new RuntimeException("No playoff bracket specified, please go back to the <a href='index.jsp'>playoff main page</a> and start again.");
      }
      pageContext.setAttribute("bracketName", divisionStr);

      final String specifiedFirstRound = WebUtils.getNonNullRequestParameter(request, "firstRound");
      final String specifiedLastRound = WebUtils.getNonNullRequestParameter(request, "lastRound");
      int firstRound;
      try {
        firstRound = Integer.parseInt(specifiedFirstRound);
      } catch (NumberFormatException nfe) {
        firstRound = 1;
      }

      final int tournament = Queries.getCurrentTournament(connection);
      final int lastColumn = 1
          + Queries.getNumPlayoffRounds(connection, tournament, divisionStr);

      int lastRound;
      try {
        lastRound = Integer.parseInt(specifiedLastRound);
      } catch (NumberFormatException nfe) {
        lastRound = lastColumn;
      }

      // Sanity check that the last round is valid
      if (lastRound < 2) {
        lastRound = 2;
      }
      if (lastRound > lastColumn) {
        lastRound = lastColumn;
      }
      // Sanity check that the first round is valid
      if (firstRound < 1) {
        firstRound = 1;
      }
      if (firstRound > 1
          && firstRound >= lastRound) {
        firstRound = lastRound
            - 1; // force the display of at least 2 rounds
      }

      final BracketData bracketInfo = new BracketData(connection, divisionStr, firstRound, lastRound, 4, true, false);

      for (int i = 1; i < lastColumn; i++) {
        bracketInfo.addBracketLabels(i);
      }
      bracketInfo.addStaticTableLabels(connection);

      bracketInfo.generateBracketOutput(connection, TopRightCornerStyle.MEET_BOTTOM_OF_CELL);

      pageContext.setAttribute("bracketInfo", bracketInfo);

      // expose all bracketInfo to the javascript
      final ObjectMapper jsonMapper = Utilities.createJsonMapper();
      final StringWriter writer = new StringWriter();
      try {
        jsonMapper.writeValue(writer, bracketInfo);
      } catch (final IOException e) {
        throw new FLLInternalException("Error writing JSON for bracketInfo", e);
      }
      final String bracketInfoJson = writer.toString();
      pageContext.setAttribute("bracketInfoJson", bracketInfoJson);

    } catch (final SQLException sqle) {
      LOGGER.error("Error talking to the database", sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    }

  }

}
