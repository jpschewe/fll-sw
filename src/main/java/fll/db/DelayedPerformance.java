/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Tournament;
import fll.web.admin.Tournaments;

/**
 * Utilities for working with delaying the display of performance scores.
 */
public final class DelayedPerformance {

  /** This matches the format used by the jquery timepicker. */
  public static final DateTimeFormatter TIME_FORMATTER = new DateTimeFormatterBuilder().appendValue(ChronoField.CLOCK_HOUR_OF_AMPM)
                                                                                       .appendLiteral(':')
                                                                                       .appendValue(ChronoField.MINUTE_OF_HOUR,
                                                                                                    2)
                                                                                       .appendText(ChronoField.AMPM_OF_DAY,
                                                                                                   Map.of(0L, "am", 1L,
                                                                                                          "pm"))
                                                                                       .toFormatter();

  /**
   * @param runNumber {@link #getRunNumber()}
   * @param delayUntil {@link #getDelayUntil()}
   */
  public DelayedPerformance(final int runNumber,
                            final LocalDateTime delayUntil) {
    this.runNumber = runNumber;
    this.delayUntil = delayUntil;
  }

  private final int runNumber;

  /**
   * @return the run number to delay the display of
   */
  public int getRunNumber() {
    return runNumber;
  }

  private final LocalDateTime delayUntil;

  /**
   * @return when to display the scores for {@link #getRunNumber()}
   */
  public LocalDateTime getDelayUntil() {
    return delayUntil;
  }

  /**
   * @return delay until formatted for use with jquery-ui date picker
   * @see Tournaments#DATE_FORMATTER
   * @see #getDelayUntil()
   */
  @JsonIgnore
  public String getDelayUntilDateString() {
    return delayUntil.toLocalDate().format(Tournaments.DATE_FORMATTER);
  }

  /**
   * @return delay until formatted for use with jquery time picker
   * @see #TIME_FORMATTER
   * @see #getDelayUntil()
   */
  @JsonIgnore
  public String getDelayUntilTimeString() {
    return delayUntil.toLocalTime().format(TIME_FORMATTER);
  }

  /**
   * @param connection database connection
   * @param tournament the tournament to deal with
   * @return the maximum run number to display
   * @throws SQLException on a database error
   */
  public static int getMaxRunNumberToDisplay(final Connection connection,
                                             final Tournament tournament)
      throws SQLException {

    try (PreparedStatement prep = connection.prepareStatement("SELECT run_number" //
        + " FROM delayed_performance" //
        + " WHERE tournament_id = ?" //
        + " AND delayed_until > CURRENT_TIMESTAMP" //
        + " ORDER BY run_number ASC" //
        + " LIMIT 1" //
    )) {
      prep.setInt(1, tournament.getTournamentID());

      try (ResultSet rs = prep.executeQuery()) {
        if (rs.next()) {
          final int runNumber = rs.getInt("run_number");
          // display all run numbers before this one
          return runNumber
              - 1;
        } else {
          // display all runs
          return Integer.MAX_VALUE;
        }
      } // result set

    } // prepared statement

  }

  /**
   * @param connection database connection
   * @param tournament the tournament to get the performance delays for
   * @return the performance delays sorted by {@link #getRunNumber()}
   * @throws SQLException on a database error
   */
  public static List<DelayedPerformance> loadDelayedPerformances(final Connection connection,
                                                                 final Tournament tournament)
      throws SQLException {

    final List<DelayedPerformance> delays = new LinkedList<>();

    try (PreparedStatement prep = connection.prepareStatement("SELECT run_number, delayed_until" //
        + " FROM delayed_performance" //
        + " WHERE tournament_id = ?" //
    )) {
      prep.setInt(1, tournament.getTournamentID());

      try (ResultSet rs = prep.executeQuery()) {

        while (rs.next()) {
          final int runNumber = rs.getInt("run_number");
          final Timestamp delayUntil = castNonNull(rs.getTimestamp("delayed_until"));
          final DelayedPerformance delay = new DelayedPerformance(runNumber, delayUntil.toLocalDateTime());
          delays.add(delay);
        }

      } // result set
    } // prepared statement

    delays.sort(SortByRunNumber.INSTANCE);
    return delays;
  }

  /**
   * @param connection database connection
   * @param tournament tournament the delays are for
   * @param delays the performance delays
   * @throws SQLException on a database error
   */
  public static void storeDelayedPerformances(final Connection connection,
                                              final Tournament tournament,
                                              final List<DelayedPerformance> delays)
      throws SQLException {
    try (
        PreparedStatement delete = connection.prepareStatement("DELETE FROM delayed_performance WHERE tournament_id = ?")) {
      delete.setInt(1, tournament.getTournamentID());
      delete.executeUpdate();
    }

    try (PreparedStatement insert = connection.prepareStatement("INSERT INTO delayed_performance " //

        + " (tournament_id, run_number, delayed_until)" //
        + " VALUES(?, ?, ?)" //
    )) {
      insert.setInt(1, tournament.getTournamentID());
      for (final DelayedPerformance delay : delays) {
        insert.setInt(2, delay.getRunNumber());
        insert.setTimestamp(3, Timestamp.valueOf(delay.getDelayUntil()));
        insert.executeUpdate();
      }

    }

  }

  private static final class SortByRunNumber implements Comparator<DelayedPerformance>, Serializable {

    public static final SortByRunNumber INSTANCE = new SortByRunNumber();

    @Override
    public int compare(final DelayedPerformance o1,
                       final DelayedPerformance o2) {
      return Integer.compare(o1.runNumber, o2.runNumber);
    }

  }

}
