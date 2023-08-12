/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreboard;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.Tournament;
import fll.TournamentTeam;
import fll.Utilities;
import fll.db.DelayedPerformance;
import fll.db.Queries;
import fll.db.TournamentParameters;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.DisplayInfo;
import fll.web.playoff.DatabaseTeamScore;
import fll.web.playoff.TeamScore;
import fll.xml.ChallengeDescription;
import fll.xml.PerformanceScoreCategory;
import fll.xml.ScoreType;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Handle sending scoreboard updates to the web clients.
 */
public final class ScoreboardUpdates {

  private ScoreboardUpdates() {
  }

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static final Set<AsyncContext> ALL_CLIENTS = new HashSet<>();

  private static final Object CLIENTS_LOCK = new Object();

  /**
   * Message type used for score updates.
   */
  public static final String SCORE_UPDATE_TYPE = "score_update";

  /**
   * Add the client to the list of clients to receive score updates.
   * 
   * @param client the session to add
   */
  public static void addClient(final AsyncContext client) {
    if (!(client.getRequest() instanceof HttpServletRequest)) {
      LOGGER.warn("Found client that doesn't have a servlet request, ignoring: "
          + client.getRequest().getRemoteAddr());
      return;
    }

    synchronized (CLIENTS_LOCK) {
      ALL_CLIENTS.add(client);
    }
    sendAllScores(client);
  }

  private static void sendAllScores(final AsyncContext client) {
    final ObjectMapper jsonMapper = Utilities.createJsonMapper();

    final ServletContext application = client.getRequest().getServletContext();
    // cast is checked in addClient()
    final HttpSession session = ((HttpServletRequest) client.getRequest()).getSession();

    final ChallengeDescription challengeDscription = ApplicationAttributes.getChallengeDescription(application);
    final ScoreType performanceScoreType = challengeDscription.getPerformance().getScoreType();
    final PerformanceScoreCategory performanceElement = challengeDscription.getPerformance();

    try {
      final DataSource datasource = ApplicationAttributes.getDataSource(application);
      try (Connection connection = datasource.getConnection()) {
        final Tournament currentTournament = Tournament.getCurrentTournament(connection);
        final int numSeedingRounds = TournamentParameters.getNumSeedingRounds(connection,
                                                                              currentTournament.getTournamentID());
        final boolean runningHeadToHead = TournamentParameters.getRunningHeadToHead(connection,
                                                                                    currentTournament.getTournamentID());
        final int maxRunNumberToDisplay = DelayedPerformance.getMaxRunNumberToDisplay(connection, currentTournament);

        final DisplayInfo displayInfo = DisplayInfo.getInfoForDisplay(application, session);
        final List<String> allAwardGroups = Queries.getAwardGroups(connection, currentTournament.getTournamentID());
        final List<String> awardGroupsToDisplay = displayInfo.determineScoreboardAwardGroups(allAwardGroups);

        final Map<Integer, TournamentTeam> teams = Queries.getTournamentTeams(connection,
                                                                              currentTournament.getTournamentID());

        // get all scores in ascending order by time. This ensures that the most recent
        // scores display sees the newest scores last
        try (PreparedStatement prep = connection.prepareStatement("SELECT * from Performance" //
            + " WHERE tournament = ?" //
            + " ORDER BY TimeStamp ASC, TeamNumber ASC" //
        )) {
          prep.setInt(1, currentTournament.getTournamentID());
          try (ResultSet rs = prep.executeQuery()) {
            while (rs.next()) {
              final int teamNumber = rs.getInt("teamNumber");

              final @Nullable TournamentTeam team = teams.get(teamNumber);
              if (null == team) {
                LOGGER.error("Unable to find team {} in the list of teams while getting all scores to display, skipping",
                             teamNumber);
                continue;
              }

              final TeamScore teamScore = new DatabaseTeamScore(teamNumber, rs);
              final double score = performanceElement.evaluate(teamScore);
              final String formattedScore = Utilities.getFormatForScoreType(performanceScoreType).format(score);
              final ScoreUpdate update = new ScoreUpdate(team, score, formattedScore, teamScore);

              try {
                final String updateStr = jsonMapper.writeValueAsString(update);

                notifyClient(client, awardGroupsToDisplay, numSeedingRounds, runningHeadToHead, maxRunNumberToDisplay,
                             team, teamScore.getRunNumber(), updateStr);
              } catch (final JsonProcessingException e) {
                throw new FLLInternalException("Unable to format score update as JSON", e);
              }
            }
          }
        }

      } catch (final SQLException e) {
        throw new FLLRuntimeException("Error getting initial scores for display", e);
      }
    } catch (final IOException e) {
      LOGGER.error("Got error sending initial scores to client, dropping: {}", client.getRequest().getRemoteAddr(), e);
      synchronized (CLIENTS_LOCK) {
        ALL_CLIENTS.remove(client);
      }
    }

  }

  /**
   * Notify all clients of a new verified score.
   * 
   * @param team {@link ScoreUpdate#getTeam()}
   * @param score {@link ScoreUpdate#getScore()}
   * @param formattedScore {@link ScoreUpdate#getFormattedScore()}
   * @param teamScore used to get some score information
   */
  public static void notifyClients(final TournamentTeam team,
                                   final double score,
                                   final String formattedScore,
                                   final TeamScore teamScore) {
    final ScoreUpdate update = new ScoreUpdate(team, score, formattedScore, teamScore);
    notifyClients(update);
  }

  private static void notifyClients(final ScoreUpdate update) {
    final ObjectMapper jsonMapper = Utilities.createJsonMapper();
    try {
      final String updateStr = jsonMapper.writeValueAsString(update);

      final Set<AsyncContext> toRemove = new HashSet<>();
      final Set<AsyncContext> clients;
      synchronized (CLIENTS_LOCK) {
        clients = new HashSet<>(ALL_CLIENTS);
      }

      for (final AsyncContext client : clients) {
        try {
          notifyClient(client, update.getTeam(), update.getRunNumber(), updateStr);
        } catch (final IOException e) {
          LOGGER.warn("Got error writing to client, dropping: {}", client.getRequest().getRemoteAddr(), e);
          toRemove.add(client);
        }
      }

      // remove clients that had issues
      synchronized (CLIENTS_LOCK) {
        ALL_CLIENTS.removeAll(toRemove);
      }

    } catch (final JsonProcessingException e) {
      throw new FLLInternalException("Unable to format scoreboard message as JSON", e);
    }

  }

  private static void notifyClient(final AsyncContext client,
                                   final TournamentTeam team,
                                   final int runNumber,
                                   final String data)
      throws IOException {

    final ServletContext application = client.getRequest().getServletContext();
    // cast is checked in addClient()
    final HttpSession session = ((HttpServletRequest) client.getRequest()).getSession();

    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    try (Connection connection = datasource.getConnection()) {
      final Tournament currentTournament = Tournament.getCurrentTournament(connection);
      final int numSeedingRounds = TournamentParameters.getNumSeedingRounds(connection,
                                                                            currentTournament.getTournamentID());
      final boolean runningHeadToHead = TournamentParameters.getRunningHeadToHead(connection,
                                                                                  currentTournament.getTournamentID());
      final int maxRunNumberToDisplay = DelayedPerformance.getMaxRunNumberToDisplay(connection, currentTournament);

      final DisplayInfo displayInfo = DisplayInfo.getInfoForDisplay(application, session);
      final List<String> allAwardGroups = Queries.getAwardGroups(connection, currentTournament.getTournamentID());
      final List<String> awardGroupsToDisplay = displayInfo.determineScoreboardAwardGroups(allAwardGroups);

      notifyClient(client, awardGroupsToDisplay, numSeedingRounds, runningHeadToHead, maxRunNumberToDisplay, team,
                   runNumber, data);
    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error talking to the database", e);
    }
  }

  private static void notifyClient(final AsyncContext client,
                                   final Collection<String> awardGroupsToDisplay,
                                   final int numSeedingRounds,
                                   final boolean runningHeadToHead,
                                   final int maxRunNumberToDisplay,
                                   final TournamentTeam team,
                                   final int runNumber,
                                   final String data)
      throws IOException {
    if (runNumber <= maxRunNumberToDisplay
        && awardGroupsToDisplay.contains(team.getAwardGroup())
        && (!runningHeadToHead
            || runNumber <= numSeedingRounds)) {

      final PrintWriter writer = client.getResponse().getWriter();
      writer.write(String.format("event: %s%n", SCORE_UPDATE_TYPE));
      writer.write(String.format("data: %s%n%n", data));
      writer.flush();
    }

  }

  /**
   * Message sent on the client.
   */
  public static final class ScoreUpdate {

    /**
     * @param team {@link #team}
     * @param score {@link #score}
     * @param formattedScore {@link #formattedScore}
     * @param teamScore used to gather {@link #isBye()} {@link #isNoShow()}
     *          {@link #getRunNumber()}
     */
    public ScoreUpdate(final TournamentTeam team,
                       final double score,
                       final String formattedScore,
                       final TeamScore teamScore) {
      this.team = team;
      this.score = score;
      this.formattedScore = formattedScore;
      this.runNumber = teamScore.getRunNumber();
      this.bye = teamScore.isBye();
      this.noShow = teamScore.isNoShow();
    }

    /**
     * @return The team that the score is for.
     */
    public TournamentTeam getTeam() {
      return team;
    }

    private final TournamentTeam team;

    /**
     * @return The score.
     */
    public double getScore() {
      return score;
    }

    private final double score;

    /**
     * @return The score string to display.
     */
    public String getFormattedScore() {
      return formattedScore;
    }

    private final String formattedScore;

    /**
     * @return the run number for the score
     */
    public int getRunNumber() {
      return runNumber;
    }

    private final int runNumber;

    /**
     * @return if this is a bye
     */
    public boolean isBye() {
      return bye;
    }

    private final boolean bye;

    /**
     * @return if this is a no show
     */
    public boolean isNoShow() {
      return noShow;
    }

    private final boolean noShow;

  }

}
