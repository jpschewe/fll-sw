/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreboard;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fll.Tournament;
import fll.TournamentTeam;
import fll.Utilities;
import fll.db.Queries;
import fll.db.RunMetadata;
import fll.db.RunMetadataFactory;
import fll.scores.DatabasePerformanceTeamScore;
import fll.scores.PerformanceTeamScore;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.web.ApplicationAttributes;
import fll.web.TournamentData;
import fll.web.WebUtils;
import fll.web.display.DisplayHandler;
import fll.web.display.DisplayInfo;
import fll.web.display.UnknownDisplayException;
import fll.xml.ChallengeDescription;
import fll.xml.PerformanceScoreCategory;
import fll.xml.ScoreType;
import jakarta.servlet.ServletContext;
import jakarta.websocket.Session;

/**
 * Handle sending scoreboard updates to the web clients.
 */
public final class ScoreboardUpdates {

  private ScoreboardUpdates() {
  }

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

  // uuid -> session
  private static final Map<String, Session> ALL_CLIENTS = new ConcurrentHashMap<>();

  /**
   * Add the client to the list of clients to receive score updates.
   * 
   * @param client the session to add
   * @param displayUuid uuid for the display, if null, generate a UUID
   * @param application used to get application data
   * @return the UUID for the scoreboard
   */
  public static String addClient(final @Nullable String displayUuid,
                                 final Session client,
                                 final ServletContext application) {
    final String uuid;
    if (StringUtils.isBlank(displayUuid)) {
      uuid = DisplayHandler.registerStandaloneScoreboard(client);
    } else {
      uuid = displayUuid;
    }

    ALL_CLIENTS.put(uuid, client);
    final DataSource datasource = ApplicationAttributes.getDataSource(application);
    final TournamentData tournamentData = ApplicationAttributes.getTournamentData(application);
    final ChallengeDescription challengeDescription = ApplicationAttributes.getChallengeDescription(application);
    try {
      sendAllScores(tournamentData.getRunMetadataFactory(), datasource, challengeDescription, uuid, client);
    } catch (final UnknownDisplayException e) {
      LOGGER.error("Cannot find display {} that was just added as a new client", uuid);
    }

    return uuid;
  }

  private static void sendAllScores(final RunMetadataFactory runMetadataFactory,
                                    final DataSource datasource,
                                    final ChallengeDescription challengeDescription,
                                    final String displayUuid,
                                    final Session client)
      throws UnknownDisplayException {
    final ObjectMapper jsonMapper = Utilities.createJsonMapper();

    final ScoreType performanceScoreType = challengeDescription.getPerformance().getScoreType();
    final PerformanceScoreCategory performanceElement = challengeDescription.getPerformance();

    try (Connection connection = datasource.getConnection()) {
      final Tournament currentTournament = Tournament.getCurrentTournament(connection);
      final DisplayInfo displayInfo = DisplayHandler.resolveDisplay(displayUuid);
      final List<String> allAwardGroups = Queries.getAwardGroups(connection, currentTournament.getTournamentID());
      final List<String> awardGroupsToDisplay = displayInfo.determineScoreboardAwardGroups(allAwardGroups);

      final Map<Integer, TournamentTeam> teams = Queries.getTournamentTeams(connection,
                                                                            currentTournament.getTournamentID());

      // get all scores in ascending order by time. This ensures that the most recent
      // scores display sees the newest scores last
      try (PreparedStatement prep = connection.prepareStatement("SELECT TeamNumber, RunNumber from Performance" //
          + " WHERE tournament = ?" //
          + " ORDER BY TimeStamp ASC, TeamNumber ASC" //
      )) {
        prep.setInt(1, currentTournament.getTournamentID());
        try (ResultSet rs = prep.executeQuery()) {
          while (rs.next()) {
            final int teamNumber = rs.getInt(1);
            final int runNumber = rs.getInt(2);
            final RunMetadata runMetadata = runMetadataFactory.getRunMetadata(runNumber);
            if (!runMetadata.isScoreboardDisplay()) {
              continue;
            }

            final @Nullable TournamentTeam team = teams.get(teamNumber);
            if (null == team) {
              LOGGER.error("Unable to find team {} in the list of teams while getting all scores to display, skipping",
                           teamNumber);
              continue;
            }

            final PerformanceTeamScore teamScore = DatabasePerformanceTeamScore.fetchTeamScore(currentTournament.getTournamentID(),
                                                                                               teamNumber, runNumber,
                                                                                               connection);
            if (null == teamScore) {
              throw new FLLInternalException(String.format("Unable to find score for tournament %d team %d run %d",
                                                           currentTournament.getTournamentID(), teamNumber, runNumber));
            }

            final double score = performanceElement.evaluate(teamScore);
            final String formattedScore = Utilities.getFormatForScoreType(performanceScoreType).format(score);
            final ScoreUpdateMessage update = new ScoreUpdateMessage(team, score, formattedScore, teamScore,
                                                                     runMetadata);

            try {
              final String updateStr = jsonMapper.writeValueAsString(update);

              if (!newScore(client, awardGroupsToDisplay, team, updateStr)) {
                removeClient(displayUuid);
              }
            } catch (final JsonProcessingException e) {
              throw new FLLInternalException("Unable to format score update as JSON", e);
            }
          }
        }
      }

    } catch (final SQLException e) {
      throw new FLLRuntimeException("Error getting initial scores for display", e);
    }

  }

  /**
   * @param displayUuid uuid of the client
   */
  public static void removeClient(final String displayUuid) {
    final @Nullable Session client = ALL_CLIENTS.remove(displayUuid);
    if (null != client) {
      try {
        client.close();
      } catch (final IOException e) {
        LOGGER.debug("Got error closing websocket, ignoring", e);
      }
    }
  }

  /**
   * Tell the clients that a score was deleted. For now the clients just reload
   * the page. In the future this may be smarter. Messages are sent
   * asynchronously.
   */
  public static void deleteScore() {
    THREAD_POOL.execute(() -> {
      final DeleteMessage message = new DeleteMessage();
      try {
        final String msg = Utilities.createJsonMapper().writeValueAsString(message);
        final Set<String> toRemove = new HashSet<>();
        for (final Map.Entry<String, Session> entry : ALL_CLIENTS.entrySet()) {
          final Session client = entry.getValue();
          if (!WebUtils.sendWebsocketTextMessage(client, msg)) {
            toRemove.add(entry.getKey());
          }
        }

        // remove clients that had issues
        for (final String uuid : toRemove) {
          removeClient(uuid);
        }
      } catch (final JsonProcessingException e) {
        throw new FLLInternalException("Error converting DeleteMessage to JSON", e);
      }
    });
  }

  /**
   * Notify the display to reload due to some change in the scores that can't be
   * handled by incremental score updates.
   */
  public static void reload() {
    final ReloadMessage message = new ReloadMessage();
    try {
      final String msg = Utilities.createJsonMapper().writeValueAsString(message);

      // TODO: make this smarter and only require the displays that changed to reload
      final Set<String> toRemove = new HashSet<>();
      for (final Map.Entry<String, Session> entry : ALL_CLIENTS.entrySet()) {
        THREAD_POOL.execute(() -> {
          final Session client = entry.getValue();
          if (!WebUtils.sendWebsocketTextMessage(client, msg)) {
            toRemove.add(entry.getKey());
          }
        });
      }

      // remove clients that had issues
      for (final String uuid : toRemove) {
        removeClient(uuid);
      }
    } catch (final JsonProcessingException e) {
      throw new FLLInternalException("Error converting ReloadMessage to JSON", e);
    }
  }

  /**
   * Notify the display to reload because the award groups being displayed have
   * changed. Executed asynchronously.
   */
  public static void awardGroupChange() {
    reload();
  }

  /**
   * Notify all clients of a new verified score. Messages are sent asynchronously.
   * 
   * @param runMetadataFactory run metadata cache
   * @param datasource database connection
   * @param team {@link ScoreUpdateMessage#getTeam()}
   * @param score {@link ScoreUpdateMessage#getScore()}
   * @param formattedScore {@link ScoreUpdateMessage#getFormattedScore()}
   * @param teamScore used to get some score information
   */
  public static void newScore(final RunMetadataFactory runMetadataFactory,
                              final DataSource datasource,
                              final TournamentTeam team,
                              final double score,
                              final String formattedScore,
                              final PerformanceTeamScore teamScore) {
    final RunMetadata runMetadata = runMetadataFactory.getRunMetadata(teamScore.getRunNumber());
    if (runMetadata.isScoreboardDisplay()) {
      final ScoreUpdateMessage update = new ScoreUpdateMessage(team, score, formattedScore, teamScore, runMetadata);
      newScore(datasource, update);
    }
  }

  private static void newScore(final DataSource datasource,
                               final ScoreUpdateMessage update) {
    THREAD_POOL.execute(() -> {
      final ObjectMapper jsonMapper = Utilities.createJsonMapper();
      try (Connection connection = datasource.getConnection()) {

        final Tournament currentTournament = Tournament.getCurrentTournament(connection);
        final List<String> allAwardGroups = Queries.getAwardGroups(connection, currentTournament.getTournamentID());

        try {
          final String updateStr = jsonMapper.writeValueAsString(update);

          final Set<String> toRemove = new HashSet<>();
          for (final Map.Entry<String, Session> entry : ALL_CLIENTS.entrySet()) {
            try {
              if (!newScore(allAwardGroups, entry.getKey(), entry.getValue(), update.getTeam(), updateStr)) {
                toRemove.add(entry.getKey());
              }
            } catch (final UnknownDisplayException e) {
              LOGGER.warn("Display {} is unknown", entry.getKey(), e);
              toRemove.add(entry.getKey());
            }
          }

          // remove clients that had issues
          for (final String uuid : toRemove) {
            removeClient(uuid);
          }

        } catch (final JsonProcessingException e) {
          throw new FLLInternalException("Unable to format scoreboard message as JSON", e);
        }
      } catch (final SQLException e) {
        throw new FLLInternalException("Error talking to the database while sending scores to the scoreboards", e);
      }
    });
  }

  private static boolean newScore(final List<String> allAwardGroups,
                                  final String displayUuid,
                                  final Session client,
                                  final TournamentTeam team,
                                  final String data)
      throws UnknownDisplayException {
    final DisplayInfo displayInfo = DisplayHandler.resolveDisplay(displayUuid);
    final List<String> awardGroupsToDisplay = displayInfo.determineScoreboardAwardGroups(allAwardGroups);

    return newScore(client, awardGroupsToDisplay, team, data);
  }

  /**
   * Send a score to the specified display if the display should receive it.
   * 
   * @return false on an error sending data
   */
  private static boolean newScore(final Session client,
                                  final Collection<String> awardGroupsToDisplay,
                                  final TournamentTeam team,
                                  final String data) {
    if (awardGroupsToDisplay.contains(team.getAwardGroup())) {
      return WebUtils.sendWebsocketTextMessage(client, data);
    } else {
      return true;
    }
  }

}
