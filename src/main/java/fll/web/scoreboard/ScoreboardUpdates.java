/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.scoreboard;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.TournamentTeam;
import fll.Utilities;
import fll.util.FLLInternalException;
import jakarta.servlet.AsyncContext;

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
    synchronized (CLIENTS_LOCK) {
      ALL_CLIENTS.add(client);
    }
  }

  /**
   * Notify all clients of a new verified score.
   * 
   * @param team {@link ScoreUpdate#team}
   * @param score {@link ScoreUpdate#score}
   * @param formattedScore {@link ScoreUpdate#formattedScore}
   */
  public static void notifyClients(final TournamentTeam team,
                                   final double score,
                                   final String formattedScore) {
    final ScoreUpdate update = new ScoreUpdate(team, score, formattedScore);
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
          final PrintWriter writer = client.getResponse().getWriter();
          writer.write(String.format("event: %s\n", SCORE_UPDATE_TYPE));
          writer.write(String.format("data: %s\n\n", updateStr));
          writer.flush();
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

  /**
   * Message sent on the client.
   */
  public static final class ScoreUpdate {

    /**
     * @param team {@link #team}
     * @param score {@link #score}
     * @param formattedScore {@link #formattedScore}
     */
    public ScoreUpdate(final TournamentTeam team,
                       final double score,
                       final String formattedScore) {
      this.team = team;
      this.score = score;
      this.formattedScore = formattedScore;
    }

    /**
     * The team that the score is for.
     */
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "Used by JSON")
    public final TournamentTeam team;

    /**
     * The score.
     */
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "Used by JSON")
    public final double score;

    /**
     * The score string to display.
     */
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "Used by JSON")
    public final String formattedScore;

  }

}
