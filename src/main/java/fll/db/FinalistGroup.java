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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Tournament;

/**
 * Time that a finalist group is expected to be judged.
 * Mirrors javascript class in fll-objects.js. Property names need to match the
 * javascript/JSON.
 */
public class FinalistGroup implements Serializable {

  /**
   * @param startTime {@link #getStartTime()}
   * @param endTime {@link #getEndTime()}
   * @param judgingGroups {@link #getJudgingGroups()}
   */
  public FinalistGroup(@JsonProperty("startTime") final LocalTime startTime,
                       @JsonProperty("endTime") final LocalTime endTime,
                       @JsonProperty("judgingGroups") final Set<String> judgingGroups) {
    this.startTime = startTime;
    this.endTime = endTime;
    this.judgingGroups.clear();
    this.judgingGroups.addAll(judgingGroups);
  }

  private final LocalTime startTime;

  /**
   * @return start time of the finalist judging
   */
  public LocalTime getStartTime() {
    return startTime;
  }

  private final LocalTime endTime;

  /**
   * @return end time of the finalist judging
   */
  public LocalTime getEndTime() {
    return endTime;
  }

  private final Set<String> judgingGroups = new HashSet<>();

  /**
   * @return judging groups in this finalist group
   */
  public Set<String> getJudgingGroups() {
    return Collections.unmodifiableSet(judgingGroups);
  }

  /**
   * @param connection database connection
   * @param tournament the tournament to load the finalist groups for
   * @return the finalist groups for the the tournament keyed by finalist group
   * @throws SQLException on a database error
   */
  public static Map<String, FinalistGroup> loadFinalistGroups(final Connection connection,
                                                              final Tournament tournament)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT name, start_time, end_time" //
        + " FROM finalist_groups" //
        + " WHERE tournament_id = ?");
        PreparedStatement judgingGroupPrep = connection.prepareStatement("SELECT judging_group" //
            + " FROM finalist_groups_judging_groups" //
            + " WHERE tournament_id = ?" //
            + "   AND name = ?")) {
      prep.setInt(1, tournament.getTournamentID());
      judgingGroupPrep.setInt(1, tournament.getTournamentID());

      final Map<String, FinalistGroup> finalistGroups = new HashMap<>();

      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String name = castNonNull(rs.getString("name"));
          final LocalTime startTime = castNonNull(rs.getTime("start_time")).toLocalTime();
          final LocalTime endTime = castNonNull(rs.getTime("end_time")).toLocalTime();

          final Set<String> judgingGroups = new HashSet<>();
          judgingGroupPrep.setString(2, name);
          try (ResultSet judgingGroupsRs = judgingGroupPrep.executeQuery()) {
            while (judgingGroupsRs.next()) {
              final String ag = castNonNull(judgingGroupsRs.getString("judging_group"));
              judgingGroups.add(ag);
            }
          }
          final FinalistGroup schedule = new FinalistGroup(startTime, endTime, judgingGroups);
          finalistGroups.put(name, schedule);
        }
      }

      return finalistGroups;
    }
  }

  /**
   * @param connection database connection
   * @param tournament tournament that the schedules belong to
   * @param groups key=name, value=finalist group
   * @throws SQLException on a database error
   */
  public static void storeFinalistGroups(final Connection connection,
                                         final Tournament tournament,
                                         final Map<String, FinalistGroup> groups)
      throws SQLException {

    try (
        PreparedStatement delete = connection.prepareStatement("DELETE FROM finalist_groups WHERE tournament_id = ?")) {
      delete.setInt(1, tournament.getTournamentID());
      delete.executeUpdate();
    }

    try (PreparedStatement prep = connection.prepareStatement("INSERT INTO finalist_groups" //
        + " (tournament_id, name, start_time, end_time)"
        + " VALUES(?, ?, ?, ?)");
        PreparedStatement agPrep = connection.prepareStatement("INSERT INTO finalist_groups_judging_groups" //
            + " (tournament_id, name, judging_group)" //
            + " VALUES(?, ?, ?)")) {

      prep.setInt(1, tournament.getTournamentID());
      agPrep.setInt(1, tournament.getTournamentID());

      for (final Map.Entry<String, FinalistGroup> entry : groups.entrySet()) {
        final String name = entry.getKey();
        final FinalistGroup group = entry.getValue();

        prep.setString(2, name);
        prep.setTime(3, Time.valueOf(group.getStartTime()));
        prep.setTime(4, Time.valueOf(group.getEndTime()));
        prep.executeUpdate();

        agPrep.setString(2, name);
        for (final String jg : group.getJudgingGroups()) {
          agPrep.setString(3, jg);
          agPrep.executeUpdate();
        }
      }
    }

  }

}
