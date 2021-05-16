/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.finalist;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Tournament;

/**
 * Parameters used to create a {@link FinalistSchedule} for an award group.
 * This class is mirrored in Javascript.
 */
public class FinalistScheduleParameters implements Serializable {

  /**
   * @param startTime {@link #getStartTime()}
   * @param intervalMinutes {@link #getIntervalMinutes()}
   */
  public FinalistScheduleParameters(@JsonProperty("startTime") final LocalTime startTime,
                                    @JsonProperty("intervalMinutes") final int intervalMinutes) {
    this.startTime = startTime;
    this.intervalMinutes = intervalMinutes;
  }

  private LocalTime startTime;

  /**
   * @return the start time for the schedule
   */
  public LocalTime getStartTime() {
    return startTime;
  }

  private int intervalMinutes;

  /**
   * @return the time between slots in the schedule in minutes
   */
  public int getIntervalMinutes() {
    return intervalMinutes;
  }

  /**
   * @param connection database connection
   * @param tournament the tournament to load the playoff schedules for
   * @return the finalist schedule parameters for the the tournament keyed by
   *         award group
   * @throws SQLException on a database error
   */
  public static Map<String, FinalistScheduleParameters> loadScheduleParameters(final Connection connection,
                                                                               final Tournament tournament)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT award_group, start_time, slot_duration" //
        + " FROM finalist_parameters" //
        + " WHERE tournament_id = ?")) {
      prep.setInt(1, tournament.getTournamentID());

      final Map<String, FinalistScheduleParameters> parameters = new HashMap<>();

      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String awardGroup = castNonNull(rs.getString("award_group"));
          final LocalTime startTime = castNonNull(rs.getTime("start_time")).toLocalTime();
          final int slotDuration = rs.getInt("slot_duration");
          final FinalistScheduleParameters params = new FinalistScheduleParameters(startTime, slotDuration);
          parameters.put(awardGroup, params);
        }
      }

      return parameters;
    }
  }

  /**
   * @param connection database connection
   * @param tournament tournament that the schedules belong to
   * @param parameters key=award group, value=parameters for the schedule
   * @throws SQLException on a database error
   */
  public static void storeScheduleParameters(final Connection connection,
                                             final Tournament tournament,
                                             final Map<String, FinalistScheduleParameters> parameters)
      throws SQLException {

    try (
        PreparedStatement delete = connection.prepareStatement("DELETE FROM finalist_parameters WHERE tournament_id = ?")) {
      delete.setInt(1, tournament.getTournamentID());
      delete.executeUpdate();
    }

    try (PreparedStatement prep = connection.prepareStatement("INSERT INTO finalist_parameters" //
        + " (tournament_id, award_group, start_time, slot_duration)"
        + " VALUES(?, ?, ?, ?)")) {
      prep.setInt(1, tournament.getTournamentID());
      for (final Map.Entry<String, FinalistScheduleParameters> entry : parameters.entrySet()) {
        final String awardGroup = entry.getKey();
        final FinalistScheduleParameters params = entry.getValue();

        prep.setString(2, awardGroup);
        prep.setTime(3, Time.valueOf(params.getStartTime()));
        prep.setInt(4, params.getIntervalMinutes());
        prep.executeUpdate();
      }
    }
  }

}
