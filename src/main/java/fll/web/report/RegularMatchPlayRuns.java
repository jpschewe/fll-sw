/*
 * Copyright (c) 2022 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.sql.DataSource;

import fll.Tournament;
import fll.TournamentTeam;
import fll.Utilities;
import fll.db.TournamentParameters;
import fll.util.FLLInternalException;
import fll.web.ApplicationAttributes;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreType;
import jakarta.servlet.ServletContext;
import jakarta.servlet.jsp.PageContext;

/**
 * Support for regular-match-play-runs.jsp.
 */
public final class RegularMatchPlayRuns {

  private RegularMatchPlayRuns() {
  }

  /**
   * @param application get application variables
   * @param page used to set variable "maxScoresPerTeam" and
   *          "data" a sorted map of {@link TournamentTeam} to a list of formatted
   *          scores
   */
  public static void populateContext(final ServletContext application,
                                     final PageContext page) {
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final ChallengeDescription description = ApplicationAttributes.getChallengeDescription(application);
    final ScoreType performanceScoreType = description.getPerformance().getScoreType();

    try (Connection connection = datasource.getConnection()) {
      final Tournament currentTournament = Tournament.getCurrentTournament(connection);

      final int numRegularMatchPlayRounds = TournamentParameters.getNumSeedingRounds(connection,
                                                                                     currentTournament.getTournamentID());

      final SortedMap<TournamentTeam, List<String>> data = new TreeMap<>(TournamentTeam.TEAM_NUMBER_COMPARATOR);

      try (
          PreparedStatement prep = connection.prepareStatement("SELECT teamnumber, computedtotal, noshow, bye FROM performance" //
              + " WHERE tournament = ?" //
              + "   AND runnumber <= ?" //
              + " ORDER BY teamnumber ASC, runnumber ASC")) {
        prep.setInt(1, currentTournament.getTournamentID());
        prep.setInt(2, numRegularMatchPlayRounds);

        try (ResultSet rs = prep.executeQuery()) {
          while (rs.next()) {
            final int teamNumber = rs.getInt(1);
            final double score = rs.getDouble(2);
            final boolean scoreIsNull = rs.wasNull();
            final boolean noShow = rs.getBoolean(3);
            final boolean bye = rs.getBoolean(4);

            final TournamentTeam team = TournamentTeam.getTournamentTeamFromDatabase(connection, currentTournament,
                                                                                     teamNumber);
            final String value;
            if (noShow) {
              value = "No Show";
            } else if (bye) {
              value = "Bye";
            } else if (scoreIsNull) {
              value = "No Score";
            } else {
              value = Utilities.getFormatForScoreType(performanceScoreType).format(score);
            }

            data.computeIfAbsent(team, k -> new LinkedList<>()).add(value);
          }
        }
      }

      final int maxScoresPerTeam = data.entrySet().stream() //
                                       .map(Map.Entry::getValue) //
                                       .mapToInt(d -> d.size()) //
                                       .max().orElse(0);

      // fill out scores to max seen
      data.forEach((team,
                    performanceData) -> {
        while (performanceData.size() < maxScoresPerTeam) {
          performanceData.add("&nbsp;");
        }
      });

      page.setAttribute("data", data);
      page.setAttribute("maxScoresPerTeam", maxScoresPerTeam);

    } catch (final SQLException e) {
      throw new FLLInternalException("Error getting data for performance runs", e);
    }
  }

}
