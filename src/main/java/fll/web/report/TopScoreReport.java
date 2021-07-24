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

import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.PageContext;
import javax.sql.DataSource;

import fll.Tournament;
import fll.web.ApplicationAttributes;
import fll.web.scoreboard.Top10;
import fll.xml.ChallengeDescription;

/**
 * Gather data for the top performance score reports.
 */
public final class TopScoreReport {

  private TopScoreReport() {
  }

  /**
   * @param application application context
   * @param pageContext page context
   * @throws SQLException if there is a database error
   */
  public static void populateContextPerAwardGroup(final ServletContext application,
                                                  final PageContext pageContext)
      throws SQLException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    try (Connection connection = datasource.getConnection()) {
      final Map<String, List<Top10.ScoreEntry>> scores = Top10.getTableAsMapByAwardGroup(connection, description);
      pageContext.setAttribute("scoreMap", scores);
    }
  }

  /**
   * @param application application context
   * @param pageContext page context
   * @throws SQLException if there is a database error
   */
  public static void populateContextPerJudgingStation(final ServletContext application,
                                                      final PageContext pageContext)
      throws SQLException {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    try (Connection connection = datasource.getConnection()) {
      final Tournament tournament = Tournament.getCurrentTournament(connection);

      final Map<String, List<Top10.ScoreEntry>> scores = Top10.getTableAsMapByJudgingStation(connection, description,
                                                                                             tournament);
      pageContext.setAttribute("scoreMap", scores);
    }
  }

}
