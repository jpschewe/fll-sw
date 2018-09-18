/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.jsp.PageContext;

import fll.web.scoreboard.Top10;

/**
 * Gather data for topScoreReport.jsp.
 */
public class TopScoreReport {

  /**
   * @param application application context
   * @param pageContext page context
   * @throws SQLException if there is a database error
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext)
      throws SQLException {
    final Map<String, List<Top10.ScoreEntry>> scores = Top10.getTableAsMap(application);
    pageContext.setAttribute("scoreMap", scores);
  }

}
