/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreboard;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import fll.TournamentTeam;
import fll.Utilities;
import fll.db.GlobalParameters;
import fll.db.Queries;
import fll.db.TournamentParameters;
import fll.util.LogUtils;
import fll.web.ApplicationAttributes;
import fll.web.SessionAttributes;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreType;

/**
 * Support for allteams.jsp.
 */
public class AllTeams {

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final int TEAMS_BETWEEN_LOGOS = 2;

  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final PageContext pageContext) {
    final StringBuilder message = new StringBuilder();
    final String existingMessage = SessionAttributes.getMessage(session);
    if (null != existingMessage) {
      message.append(existingMessage);
    }

    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
    final boolean floatingPointScores = challengeDescription.getPerformance().getScoreType() == ScoreType.FLOAT;

    int numScores = 0;
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (final Connection connection = datasource.getConnection();
        final PreparedStatement prep = connection.prepareStatement("SELECT " //
            + " verified_performance.RunNumber, verified_performance.NoShow, verified_performance.ComputedTotal"
            + " FROM verified_performance" //
            + " WHERE verified_performance.Tournament = ?" //
            + "   AND verified_performance.TeamNumber = ?" //
            + "   AND verified_performance.Bye = False" //
            + "   AND verified_performance.RunNumber <= ?" //
            + " ORDER BY verified_performance.RunNumber")) {

      final int tournamentId = Queries.getCurrentTournament(connection);
      final int maxScoreboardRound = TournamentParameters.getMaxScoreboardPerformanceRound(connection, tournamentId);
      final Map<Integer, TournamentTeam> tournamentTeams = Queries.getTournamentTeams(connection, tournamentId);

      prep.setInt(1, tournamentId);
      prep.setInt(3, maxScoreboardRound);

      final List<String> awardGroups = Queries.getAwardGroups(connection, tournamentId);
      final List<TournamentTeam> teamsWithScores = new LinkedList<>();
      final Map<Integer, String> teamHeaderColor = new HashMap<>();
      final Map<Integer, List<ComputedPerformanceScore>> scores = new HashMap<>();
      for (final Map.Entry<Integer, TournamentTeam> entry : tournamentTeams.entrySet()) {

        final String headerColor = Queries.getColorForIndex(awardGroups.indexOf(entry.getValue().getAwardGroup()));
        teamHeaderColor.put(entry.getKey(), headerColor);

        prep.setInt(2, entry.getKey());

        final List<ComputedPerformanceScore> teamScores = new LinkedList<>();
        try (final ResultSet rs = prep.executeQuery()) {
          while (rs.next()) {
            final ComputedPerformanceScore score = new ComputedPerformanceScore(floatingPointScores, rs.getInt(1),
                                                                                rs.getBoolean(2), rs.getDouble(3));
            teamScores.add(score);

            ++numScores;
          }
        }

        if (!teamScores.isEmpty()) {
          teamsWithScores.add(entry.getValue());
          scores.put(entry.getKey(), teamScores);
        }

      }

      final List<String> sponsorLogos = getSponsorLogos(application);

      // estimate how many rows there are
      final int msPerRow = GlobalParameters.getAllTeamsMsPerRow(connection);
      final int scrollDuration = msPerRow // ms per row
          * (1
              * teamsWithScores.size() // award group, organization, team
                                       // name, hr, scores header
              + numScores // one row for each score
              + (teamsWithScores.size()
                  / TEAMS_BETWEEN_LOGOS)// one row for each sponsor logo
          );

      pageContext.setAttribute("sponsorLogos", sponsorLogos);
      pageContext.setAttribute("teamsBetweenLogos", Integer.valueOf(TEAMS_BETWEEN_LOGOS));
      pageContext.setAttribute("teamsWithScores", teamsWithScores);
      pageContext.setAttribute("scores", scores);
      pageContext.setAttribute("scrollDuration", Integer.valueOf(scrollDuration));
      pageContext.setAttribute("teamHeaderColor", teamHeaderColor);

    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage()
          + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    } finally {
      session.setAttribute(SessionAttributes.MESSAGE, message.toString());
    }

  }

  /**
   * Get the URsponsor logo filenames relative to "/sponsor_logos".
   * 
   * @return sorted sponsor logos list
   */
  private static List<String> getSponsorLogos(final ServletContext application) {
    final String imagePath = application.getRealPath("/sponsor_logos");

    final List<String> logoFiles = Utilities.getGraphicFiles(new File(imagePath));

    return logoFiles;
  }

  public static final class ComputedPerformanceScore {
    public ComputedPerformanceScore(final boolean floatingPointScores,
                                    final int runNumber,
                                    final boolean isNoShow,
                                    final double score) {
      mRunNumber = runNumber;
      if (isNoShow) {
        mScoreString = "No Show";
      } else {
        if (floatingPointScores) {
          mScoreString = Utilities.FLOATING_POINT_NUMBER_FORMAT_INSTANCE.format(score);
        } else {
          mScoreString = Utilities.INTEGER_NUMBER_FORMAT_INSTANCE.format(score);
        }
      }
    }

    private final int mRunNumber;

    public int getRunNumber() {
      return mRunNumber;
    }

    private final String mScoreString;

    public String getScoreString() {
      return mScoreString;
    }

  }
}
