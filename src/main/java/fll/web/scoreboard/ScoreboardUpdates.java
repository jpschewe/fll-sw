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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import fll.web.display.DisplayHandler;
import fll.web.playoff.DatabaseTeamScore;
import fll.web.playoff.TeamScore;
import fll.xml.ChallengeDescription;
import fll.xml.PerformanceScoreCategory;
import fll.xml.ScoreType;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.Session;

/**
 * Handle sending scoreboard updates to the web clients.
 */
public final class ScoreboardUpdates {

  private ScoreboardUpdates() {
  }

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  // uuid -> session
  private static final Map<String, Session> ALL_CLIENTS = new ConcurrentHashMap<>();

  /**
   * Message type used for score updates.
   */
  public static final String SCORE_UPDATE_TYPE = "score_update";

  /**
   * Message type used for score deletes.
   */
  public static final String SCORE_DELETE_TYPE = "score_delete";

  /**
   * Message type used to reload the page.
   */
  public static final String RELOAD_TYPE = "reload";

  /**
   * Add the client to the list of clients to receive score updates.
   * 
   * @param client the session to add
   * @param displayUuid uuid for the display, if null, generate a UUID
   * @param httpSession used to lookup the application context
   * @return the UUID for the scoreboard
   */
  public static String addClient(final @Nullable String displayUuid,
                                 final Session client,
                                 final HttpSession httpSession) {
    final String uuid;
    if (StringUtils.isBlank(displayUuid)) {
      uuid = UUID.randomUUID().toString();
    } else {
      uuid = displayUuid;
    }

    final ServletContext application = httpSession.getServletContext();
    ALL_CLIENTS.put(uuid, client);
    sendAllScores(application, uuid, client);
    return uuid;
  }

  private static void sendAllScores(final ServletContext application,
                                    final String displayUuid,
                                    final Session client) {
    final ObjectMapper jsonMapper = Utilities.createJsonMapper();

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

        final DisplayInfo displayInfo = DisplayHandler.resolveDisplay(displayUuid);
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
              final int runNumber = rs.getInt("runNumber");

              final @Nullable TournamentTeam team = teams.get(teamNumber);
              if (null == team) {
                LOGGER.error("Unable to find team {} in the list of teams while getting all scores to display, skipping",
                             teamNumber);
                continue;
              }

              final TeamScore teamScore = new DatabaseTeamScore(teamNumber, runNumber, rs);
              final double score = performanceElement.evaluate(teamScore);
              final String formattedScore = Utilities.getFormatForScoreType(performanceScoreType).format(score);
              final ScoreUpdateMessage update = new ScoreUpdateMessage(team, score, formattedScore, teamScore);

              try {
                final String updateStr = jsonMapper.writeValueAsString(update);

                newScore(client, awardGroupsToDisplay, numSeedingRounds, runningHeadToHead, maxRunNumberToDisplay, team,
                         teamScore.getRunNumber(), updateStr);
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
      LOGGER.error("Got error sending initial scores to client, dropping: {}", displayUuid, e);
      removeClient(displayUuid);
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
   * the page. In the future this may be smarter.
   */
  public static void deleteScore() {
    final DeleteMessage message = new DeleteMessage();
    try {
      final String msg = Utilities.createJsonMapper().writeValueAsString(message);
      final Set<String> toRemove = new HashSet<>();
      for (final Map.Entry<String, Session> entry : ALL_CLIENTS.entrySet()) {
        try {
          final Session client = entry.getValue();
          client.getBasicRemote().sendText(msg);
        } catch (final IOException e) {
          LOGGER.warn("Got error writing to client, dropping: {}", entry.getKey(), e);
          toRemove.add(entry.getKey());
        } catch (final IllegalStateException e) {
          LOGGER.warn("Illegal state exception writing to client, dropping: {}", entry.getKey(), e);
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
  }

  /**
   * Notify the display to reload because the award groups being displayed have
   * changed.
   */
  public static void awardGroupChange() {
    final ReloadMessage message = new ReloadMessage();
    try {
      final String msg = Utilities.createJsonMapper().writeValueAsString(message);

      // TODO: make this smarter and only require the displays that changed to reload
      final Set<String> toRemove = new HashSet<>();
      for (final Map.Entry<String, Session> entry : ALL_CLIENTS.entrySet()) {
        try {
          final Session client = entry.getValue();
          client.getBasicRemote().sendText(msg);
        } catch (final IOException e) {
          LOGGER.warn("Got error writing to client, dropping: {}", entry.getKey(), e);
          toRemove.add(entry.getKey());
        } catch (final IllegalStateException e) {
          LOGGER.warn("Illegal state exception writing to client, dropping: {}", entry.getKey(), e);
          toRemove.add(entry.getKey());
        }
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
   * Notify all clients of a new verified score.
   * 
   * @param connection database connection
   * @param team {@link ScoreUpdateMessage#getTeam()}
   * @param score {@link ScoreUpdateMessage#getScore()}
   * @param formattedScore {@link ScoreUpdateMessage#getFormattedScore()}
   * @param teamScore used to get some score information
   */
  public static void newScore(final Connection connection,
                              final TournamentTeam team,
                              final double score,
                              final String formattedScore,
                              final TeamScore teamScore)
      throws SQLException {
    final ScoreUpdateMessage update = new ScoreUpdateMessage(team, score, formattedScore, teamScore);
    newScore(connection, update);
  }

  private static void newScore(final Connection connection,
                               final ScoreUpdateMessage update)
      throws SQLException {
    final ObjectMapper jsonMapper = Utilities.createJsonMapper();
    try {
      final String updateStr = jsonMapper.writeValueAsString(update);

      final Set<String> toRemove = new HashSet<>();
      for (final Map.Entry<String, Session> entry : ALL_CLIENTS.entrySet()) {
        try {
          newScore(connection, entry.getKey(), entry.getValue(), update.getTeam(), update.getRunNumber(), updateStr);
        } catch (final IOException e) {
          LOGGER.warn("Got error writing to client, dropping: {}", entry.getKey(), e);
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

  }

  private static void newScore(final Connection connection,
                               final String displayUuid,
                               final Session client,
                               final TournamentTeam team,
                               final int runNumber,
                               final String data)
      throws IOException, SQLException {

    final Tournament currentTournament = Tournament.getCurrentTournament(connection);
    final int numSeedingRounds = TournamentParameters.getNumSeedingRounds(connection,
                                                                          currentTournament.getTournamentID());
    final boolean runningHeadToHead = TournamentParameters.getRunningHeadToHead(connection,
                                                                                currentTournament.getTournamentID());
    final int maxRunNumberToDisplay = DelayedPerformance.getMaxRunNumberToDisplay(connection, currentTournament);

    final DisplayInfo displayInfo = DisplayHandler.resolveDisplay(displayUuid);
    final List<String> allAwardGroups = Queries.getAwardGroups(connection, currentTournament.getTournamentID());
    final List<String> awardGroupsToDisplay = displayInfo.determineScoreboardAwardGroups(allAwardGroups);

    newScore(client, awardGroupsToDisplay, numSeedingRounds, runningHeadToHead, maxRunNumberToDisplay, team, runNumber,
             data);
  }

  private static void newScore(final Session client,
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
      if (!client.isOpen()) {
        throw new IOException("Client has closed the connection");
      }
      client.getBasicRemote().sendText(data);
    }
  }

}
