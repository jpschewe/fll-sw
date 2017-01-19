/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Helpers for scoregenbrackets.jsp.
 */
public class ScoregenBrackets {
  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * @param application application context
   * @param pageContext page context, information is put in here
   */
  public static void populateContext(final ServletContext application,
                                     final HttpServletRequest request,
                                     final PageContext pageContext) {

    Connection connection = null;
    Statement stmt = null;
    ResultSet rs = null;
    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      connection = datasource.getConnection();

      String division = request.getParameter("division");
      if (null == division) {
        division = (String) request.getAttribute("division");

        if (null == division) {
          throw new RuntimeException("No playoff bracket specified, please go back to the <a href='index.jsp'>playoff main page</a> and start again.");
        }
      }
      pageContext.setAttribute("division", division);

      int firstRound;
      String firstRoundStr = request.getParameter("firstRound");
      if (null == firstRoundStr) {
        firstRound = 1;
      } else {
        try {
          firstRound = Integer.parseInt(firstRoundStr);
        } catch (NumberFormatException nfe) {
          firstRound = 1;
        }
      }

      final int lastColumn = 1
          + Queries.getNumPlayoffRounds(connection, division);

      int lastRound;
      String lastRoundStr = request.getParameter("lastRound");
      if (null == lastRoundStr) {
        lastRound = 1;
      } else {
        try {
          lastRound = Integer.parseInt(lastRoundStr);
        } catch (NumberFormatException nfe) {
          lastRound = lastColumn;
        }
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

      request.setAttribute("firstRound", Integer.valueOf(firstRound));
      request.setAttribute("lastRound", Integer.valueOf(lastRound));

    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new RuntimeException(e);
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
      SQLFunctions.close(connection);
    }

  }
}