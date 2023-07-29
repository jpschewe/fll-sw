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

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;
import javax.sql.DataSource;

import fll.Tournament;
import fll.TournamentTeam;
import fll.Utilities;
import fll.db.DelayedPerformance;
import fll.db.GlobalParameters;
import fll.db.Queries;
import fll.db.TournamentParameters;
import fll.web.ApplicationAttributes;
import fll.web.DisplayInfo;
import fll.web.SessionAttributes;
import fll.xml.ChallengeDescription;
import fll.xml.ScoreType;

/**
 * Support for allteams.jsp.
 */
public final class AllTeams {

  private AllTeams() {
  }

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static final int TEAMS_BETWEEN_LOGOS = 2;

  /**
   * @param application application variables
   * @param session session variables
   * @param pageContext page variables
   */
  public static void populateContext(final ServletContext application,
                                     final HttpSession session,
                                     final PageContext pageContext) {
    final StringBuilder message = new StringBuilder();

    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
    final boolean floatingPointScores = challengeDescription.getPerformance().getScoreType() == ScoreType.FLOAT;

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection();
        PreparedStatement prep = connection.prepareStatement("SELECT " //
            + " verified_performance.RunNumber, verified_performance.NoShow, verified_performance.ComputedTotal"
            + " FROM verified_performance" //
            + " WHERE verified_performance.Tournament = ?" //
            + "   AND verified_performance.TeamNumber = ?" //
            + "   AND verified_performance.Bye = False" //
            + "   AND (? OR verified_performance.RunNumber <= ?)" //
            + "   AND verified_performance.RunNumber <= ?" //
            + " ORDER BY verified_performance.RunNumber")) {

      final Tournament tournament = Tournament.getCurrentTournament(connection);
      final int tournamentId = tournament.getTournamentID();
      final int numSeedingRuns = TournamentParameters.getNumSeedingRounds(connection, tournamentId);
      final boolean runningHeadToHead = TournamentParameters.getRunningHeadToHead(connection, tournamentId);
      final Map<Integer, TournamentTeam> tournamentTeams = Queries.getTournamentTeams(connection, tournamentId);
      final int maxRunNumberToDisplay = DelayedPerformance.getMaxRunNumberToDisplay(connection, tournament);

      final DisplayInfo displayInfo = DisplayInfo.getInfoForDisplay(application, session);
      final List<String> allAwardGroups = Queries.getAwardGroups(connection, tournamentId);
      final List<String> awardGroupsToDisplay = displayInfo.determineScoreboardAwardGroups(allAwardGroups);

      prep.setInt(1, tournamentId);
      prep.setBoolean(3, !runningHeadToHead);
      prep.setInt(4, numSeedingRuns);
      prep.setInt(5, maxRunNumberToDisplay);

      final List<TournamentTeam> teamsWithScores = new LinkedList<>();
      final Map<Integer, String> teamHeaderColor = new HashMap<>();
      final Map<Integer, List<ComputedPerformanceScore>> scores = new HashMap<>();
      for (final Map.Entry<Integer, TournamentTeam> entry : tournamentTeams.entrySet()) {
        final TournamentTeam team = entry.getValue();

        if (awardGroupsToDisplay.contains(team.getAwardGroup())) {
          final String headerColor = Queries.getColorForIndex(allAwardGroups.indexOf(team.getAwardGroup()));
          teamHeaderColor.put(entry.getKey(), headerColor);

          prep.setInt(2, entry.getKey());

          int nonNoShowScores = 0;
          final List<ComputedPerformanceScore> teamScores = new LinkedList<>();
          try (ResultSet rs = prep.executeQuery()) {
            while (rs.next()) {
              final int runNumber = rs.getInt(1);
              final boolean noShow = rs.getBoolean(2);
              final double computedTotal = rs.getDouble(3);
              final ComputedPerformanceScore score = new ComputedPerformanceScore(floatingPointScores, runNumber,
                                                                                  noShow, computedTotal);
              teamScores.add(score);
              if (!noShow) {
                ++nonNoShowScores;
              }
            }
          }

          if (!teamScores.isEmpty()
              && nonNoShowScores > 0) {
            teamsWithScores.add(entry.getValue());
            scores.put(entry.getKey(), teamScores);
          }
        } // if in displayed award groups

      } // foreach tournament team

      final List<String> sponsorLogos = getSponsorLogos(application);

      final double scrollRate = GlobalParameters.getAllTeamScrollRate(connection);

      pageContext.setAttribute("sponsorLogos", sponsorLogos);
      pageContext.setAttribute("teamsBetweenLogos", Integer.valueOf(TEAMS_BETWEEN_LOGOS));
      pageContext.setAttribute("teamsWithScores", teamsWithScores);
      pageContext.setAttribute("scores", scores);
      pageContext.setAttribute("scrollRate", Double.valueOf(scrollRate));
      pageContext.setAttribute("teamHeaderColor", teamHeaderColor);

    } catch (final SQLException sqle) {
      message.append("<p class='error'>Error talking to the database: "
          + sqle.getMessage()
          + "</p>");
      LOGGER.error(sqle, sqle);
      throw new RuntimeException("Error talking to the database", sqle);
    } finally {
      SessionAttributes.appendToMessage(session, message.toString());
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

  /**
   * A performance score that can be displayed.
   */
  public static final class ComputedPerformanceScore {
    /**
     * @param floatingPointScores if true then display scores as floating point
     *          values
     * @param runNumber {@link #getRunNumber()}
     * @param isNoShow if this is a no show
     * @param score the score to display, not used if {@code isNoShow} is true
     */
    public ComputedPerformanceScore(final boolean floatingPointScores,
                                    final int runNumber,
                                    final boolean isNoShow,
                                    final double score) {
      mRunNumber = runNumber;
      if (isNoShow) {
        mScoreString = "No Show";
      } else {
        if (floatingPointScores) {
          mScoreString = Utilities.getFloatingPointNumberFormat().format(score);
        } else {
          mScoreString = Utilities.getIntegerNumberFormat().format(score);
        }
      }
    }

    private final int mRunNumber;

    /**
     * @return the run number for the score
     */
    public int getRunNumber() {
      return mRunNumber;
    }

    private final String mScoreString;

    /**
     * @return the score to display
     */
    public String getScoreString() {
      return mScoreString;
    }

  }
}
