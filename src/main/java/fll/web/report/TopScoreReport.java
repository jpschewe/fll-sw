/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import fll.web.ApplicationAttributes;
import fll.web.scoreboard.Top10;
import fll.xml.ChallengeDescription;

/**
 * Gather data for topScoreReport.jsp.
 */
public final class TopScoreReport {

  private TopScoreReport() {
  }

  /**
   * @param application application context
   * @param pageContext page context
   * @throws SQLException if there is a database error
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext pageContext)
      throws SQLException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    try (Connection connection = datasource.getConnection()) {
      final Map<String, List<Top10.ScoreEntry>> scores = Top10.getTableAsMap(connection, description);
      pageContext.setAttribute("scoreMap", scores);
    }
  }

}
