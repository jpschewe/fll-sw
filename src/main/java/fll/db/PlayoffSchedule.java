/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

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

import fll.Tournament;

/**
 * Schedule for a playoff bracket.
 * Mirrors javascript class in fll-objects.js. Property names need to match the
 * javascript/JSON.
 */
public class PlayoffSchedule implements Serializable {

  /**
   * @param startTime {@link #getStartTime()}
   * @param endTime {@link #getEndTime()}
   */
  public PlayoffSchedule(@JsonProperty("startTime") final LocalTime startTime,
                         @JsonProperty("endTim") final LocalTime endTime) {
    this.startTime = startTime;
    this.endTime = endTime;
  }

  private final LocalTime startTime;

  /**
   * @return start time of the playoff bracket
   */
  public LocalTime getStartTime() {
    return startTime;
  }

  private final LocalTime endTime;

  /**
   * @return end time of the playoff bracket
   */
  public LocalTime getEndTime() {
    return endTime;
  }

  /**
   * @param connection database connection
   * @param tournament the tournament to load the playoff schedules for
   * @return the playoff schedules for the the tournament
   * @throws SQLException on a database error
   */
  public static Map<String, PlayoffSchedule> loadPlayoffSchedules(final Connection connection,
                                                                  final Tournament tournament)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT bracket_name, start_time, end_time" //
        + " FROM playoff_schedules" //
        + " WHERE tournament_id = ?")) {
      prep.setInt(1, tournament.getTournamentID());

      final Map<String, PlayoffSchedule> playoffSchedules = new HashMap<>();

      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String bracketName = rs.getString("bracket_name");
          final LocalTime startTime = rs.getTime("start_time").toLocalTime();
          final LocalTime endTime = rs.getTime("end_time").toLocalTime();
          final PlayoffSchedule schedule = new PlayoffSchedule(startTime, endTime);
          playoffSchedules.put(bracketName, schedule);
        }
      }

      return playoffSchedules;
    }
  }

  /**
   * @param connection database connection
   * @param tournament tournament that the schedules belong to
   * @param schedules key=bracket name, value=schedule for the bracket
   * @throws SQLException on a database error
   */
  public static void storePlayoffSchedules(final Connection connection,
                                           final Tournament tournament,
                                           final Map<String, PlayoffSchedule> schedules)
      throws SQLException {

    try (
        PreparedStatement delete = connection.prepareStatement("DELETE FROM playoff_schedules WHERE tournament_id = ?")) {
      delete.setInt(1, tournament.getTournamentID());
      delete.executeUpdate();
    }

    try (PreparedStatement prep = connection.prepareStatement("INSERT INTO playoff_schedules" //
        + " (tournament_id, bracket_name, start_time, end_time)"
        + " VALUES(?, ?, ?, ?)")) {
      prep.setInt(1, tournament.getTournamentID());
      for (final Map.Entry<String, PlayoffSchedule> entry : schedules.entrySet()) {
        final String bracketName = entry.getKey();
        final PlayoffSchedule schedule = entry.getValue();

        prep.setString(2, bracketName);
        prep.setTime(3, Time.valueOf(schedule.getStartTime()));
        prep.setTime(4, Time.valueOf(schedule.getEndTime()));
        prep.executeUpdate();
      }
    }

  }

}
