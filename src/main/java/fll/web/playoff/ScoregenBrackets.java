/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.PageContext;
import javax.sql.DataSource;

import fll.db.Queries;
import fll.db.TableInformation;
import fll.web.ApplicationAttributes;
import fll.web.playoff.BracketData.TopRightCornerStyle;

/**
 * Helpers for scoregenbrackets.jsp.
 */
public final class ScoregenBrackets {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private ScoregenBrackets() {
  }

  /**
   * @param request used to get query parameters
   * @param application application context
   * @param pageContext page context, information is put in here
   */
  public static void populateContext(final ServletContext application,
                                     final HttpServletRequest request,
                                     final PageContext pageContext) {

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {

      String division = request.getParameter("division");
      if (null == division) {
        division = (String) request.getAttribute("division");

        if (null == division) {
          throw new RuntimeException("No playoff bracket specified, please go back to the <a href='index.jsp'>playoff main page</a> and start again.");
        }
      }
      pageContext.setAttribute("division", division);

      int firstRound;
      final String firstRoundStr = request.getParameter("firstRound");
      if (null == firstRoundStr) {
        firstRound = 1;
      } else {
        try {
          firstRound = Integer.parseInt(firstRoundStr);
        } catch (final NumberFormatException nfe) {
          firstRound = 1;
        }
      }

      final int tournament = Queries.getCurrentTournament(connection);
      final int lastColumn = 1
          + Queries.getNumPlayoffRounds(connection, tournament, division);

      int lastRound;
      final String lastRoundStr = request.getParameter("lastRound");
      if (null == lastRoundStr) {
        lastRound = 1;
      } else {
        try {
          lastRound = Integer.parseInt(lastRoundStr);
        } catch (final NumberFormatException nfe) {
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

      final int currentTournament = Queries.getCurrentTournament(connection);

      final BracketData bracketInfo = new BracketData(connection, division, firstRound, lastRound, 4, true, false);

      final int numMatches = bracketInfo.addBracketLabelsAndScoreGenFormElements(connection, currentTournament,
                                                                                 division);
      pageContext.setAttribute("numMatches", numMatches);

      bracketInfo.generateBracketOutput(connection, TopRightCornerStyle.MEET_BOTTOM_OF_CELL);

      pageContext.setAttribute("bracketInfo", bracketInfo);

      final List<TableInformation> tableInfo = TableInformation.getTournamentTableInformation(connection,
                                                                                              currentTournament,
                                                                                              bracketInfo.getBracketName());
      pageContext.setAttribute("tableInfo", tableInfo);

    } catch (final SQLException e) {
      LOGGER.error(e, e);
      throw new RuntimeException(e);
    }

  }
}