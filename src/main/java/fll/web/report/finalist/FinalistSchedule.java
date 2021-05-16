/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Tournament;

/**
 * The schedule for finalist judging.
 */
public class FinalistSchedule implements Serializable {

  /**
   * Create a schedule.
   * 
   * @param categories key is the category name, value is if the category is
   *          public
   * @param schedule the schedule entries
   */
  public FinalistSchedule(@JsonProperty("categories") final Collection<FinalistCategory> categories,
                          @JsonProperty("schedule") final Collection<FinalistDBRow> schedule) {
    final List<FinalistDBRow> rows = new LinkedList<>(schedule);
    rows.sort(FinalistDBRow.TIME_SORT_INSTANCE);
    this.mSchedule = Collections.unmodifiableCollection(rows);
    this.categories = Collections.unmodifiableCollection(new LinkedList<>(categories));

    for (final FinalistCategory row : categories) {
      mCategoryNames.add(row.getCategoryName());
      mRooms.put(row.getCategoryName(), row.getRoom());
    }
  }

  /**
   * Load a schedule from the database.
   *
   * @throws SQLException on a database error
   * @param awardGroup the award group that the schedule is for
   * @param connection where to load from
   * @param tournament the tournament to load a schedule for
   */
  public FinalistSchedule(final Connection connection,
                          final int tournament,
                          final String awardGroup)
      throws SQLException {
    final Collection<FinalistCategory> newCategories = new LinkedList<>();

    try (
        PreparedStatement getCategories = connection.prepareStatement("SELECT category, room FROM finalist_categories WHERE tournament = ? AND division = ?")) {
      getCategories.setInt(1, tournament);
      getCategories.setString(2, awardGroup);
      try (ResultSet categories = getCategories.executeQuery()) {
        while (categories.next()) {
          final String name = castNonNull(categories.getString(1));
          final String room = categories.getString(2);
          mCategoryNames.add(name);
          mRooms.put(name, room);
          newCategories.add(new FinalistCategory(name, room));
        }
      }

      try (
          PreparedStatement getSchedule = connection.prepareStatement("SELECT category, judge_time, judge_end_time, team_number FROM finalist_schedule WHERE tournament = ? AND division = ?")) {
        getSchedule.setInt(1, tournament);
        getSchedule.setString(2, awardGroup);

        final Map<LocalTime, LocalTime> endTimes = new HashMap<>();
        final Map<@KeyFor("endTimes") LocalTime, Map<String, Integer>> categoryInfo = new HashMap<>();
        try (ResultSet schedule = getSchedule.executeQuery()) {
          while (schedule.next()) {
            final String categoryName = castNonNull(schedule.getString(1));
            final LocalTime judgeTime = castNonNull(schedule.getTime(2)).toLocalTime();
            final LocalTime judgeEndTime = castNonNull(schedule.getTime(3)).toLocalTime();
            final Integer teamNumber = Integer.valueOf(schedule.getInt(4));

            endTimes.put(judgeTime, judgeEndTime);
            categoryInfo.computeIfAbsent(judgeTime, k -> new HashMap<>()).put(categoryName, teamNumber);
          } // foreach result
        } // allocate ResultSet

        final List<FinalistDBRow> rows = categoryInfo.entrySet().stream() //
                                                     .map(entry -> new FinalistDBRow(entry.getKey(),
                                                                                     // checker bug
                                                                                     // https://github.com/typetools/checker-framework/issues/4668
                                                                                     castNonNull(endTimes.get(entry.getKey())),
                                                                                     entry.getValue())) //
                                                     .sorted(FinalistDBRow.TIME_SORT_INSTANCE) //
                                                     .collect(Collectors.toList());
        this.mSchedule = Collections.unmodifiableCollection(rows);
      }

      this.categories = Collections.unmodifiableCollection(newCategories);
    }
  }

  private final Collection<FinalistCategory> categories;

  /**
   * @return the finalist category information for the schedule
   */
  public Collection<FinalistCategory> getCategories() {
    return categories;
  }

  private final Set<String> mCategoryNames = new HashSet<>();

  /**
   * @return set of category names (unmodifiable)
   */
  @JsonIgnore
  public Set<String> getCategoryNames() {
    return Collections.unmodifiableSet(mCategoryNames);
  }

  private final Map<String, @Nullable String> mRooms = new HashMap<>();

  /**
   * Unmodifiable version of the rooms for each category.
   *
   * @return key=category name, value=room
   */
  @JsonIgnore
  public Map<String, @Nullable String> getRooms() {
    return Collections.unmodifiableMap(mRooms);
  }

  /**
   * Unmodifiable.
   */
  private final Collection<FinalistDBRow> mSchedule;

  /**
   * The full schedule.
   *
   * @return unmodifiable collection sorted by time
   */
  public Collection<FinalistDBRow> getSchedule() {
    return mSchedule; // already unmodifiable from constructor
  }

  /**
   * Store the schedule to the database. Remove any finalist schedule existing
   * for the tournament.
   *
   * @param connection database connection
   * @param tournament the tournament that the schedule is for
   * @param awardGroup award group that the schedule is for
   * @throws SQLException on a database error
   */
  public void store(final Connection connection,
                    final int tournament,
                    final String awardGroup)
      throws SQLException {

    try (
        PreparedStatement deleteSchedPrep = connection.prepareStatement("DELETE FROM finalist_schedule WHERE tournament = ? AND division = ?")) {
      deleteSchedPrep.setInt(1, tournament);
      deleteSchedPrep.setString(2, awardGroup);
      deleteSchedPrep.executeUpdate();
    }

    try (
        PreparedStatement deleteCategoriesPrep = connection.prepareStatement("DELETE FROM finalist_categories WHERE tournament = ? AND division = ?")) {
      deleteCategoriesPrep.setInt(1, tournament);
      deleteCategoriesPrep.setString(2, awardGroup);
      deleteCategoriesPrep.executeUpdate();
    }

    try (
        PreparedStatement insertCategoriesPrep = connection.prepareStatement("INSERT INTO finalist_categories (tournament, division, category, room) VALUES(?, ?, ?, ?)")) {
      insertCategoriesPrep.setInt(1, tournament);
      insertCategoriesPrep.setString(2, awardGroup);

      for (final String categoryName : mCategoryNames) {
        insertCategoriesPrep.setString(3, categoryName);
        insertCategoriesPrep.setString(4, mRooms.get(categoryName));
        insertCategoriesPrep.executeUpdate();
      }
    }

    try (
        PreparedStatement insertSchedPrep = connection.prepareStatement("INSERT INTO finalist_schedule (tournament, division, category, judge_time, judge_end_time, team_number) VALUES(?, ?, ?, ?, ?, ?)")) {
      insertSchedPrep.setInt(1, tournament);
      insertSchedPrep.setString(2, awardGroup);
      for (final FinalistDBRow row : mSchedule) {

        insertSchedPrep.setTime(4, Time.valueOf(row.getTime()));
        insertSchedPrep.setTime(5, Time.valueOf(row.getEndTime()));

        for (final Map.Entry<String, Integer> rowEntry : row.getCategories().entrySet()) {
          final String categoryName = rowEntry.getKey();
          final int teamNumber = rowEntry.getValue();

          insertSchedPrep.setString(3, categoryName);
          insertSchedPrep.setInt(6, teamNumber);
          insertSchedPrep.executeUpdate();
        } // foreach category and team mapping

      } // foreach row
    }

  }

  /**
   * Get the names of all divisions that have finalist schedules
   * stored for the specified tournament.
   *
   * @param connection the database connection
   * @param tournament the tournament id
   * @return the division names
   * @throws SQLException on a database error
   */
  public static Collection<String> getAllDivisions(final Connection connection,
                                                   final int tournament)
      throws SQLException {
    final Collection<String> result = new LinkedList<>();
    try (
        PreparedStatement getDivisions = connection.prepareStatement("SELECT DISTINCT division FROM finalist_categories WHERE tournament = ?")) {
      getDivisions.setInt(1, tournament);
      try (ResultSet divisions = getDivisions.executeQuery()) {
        while (divisions.next()) {
          result.add(castNonNull(divisions.getString(1)));
        }
      }
    }

    return result;
  }

  /**
   * @param connection database connection
   * @param tournament the tournament the schedules belong to
   * @param schedules map of award group to schedule
   * @throws SQLException on a database error
   */
  public static void storeSchedules(final Connection connection,
                                    final Tournament tournament,
                                    final Map<String, FinalistSchedule> schedules)
      throws SQLException {
    for (Map.Entry<String, FinalistSchedule> entry : schedules.entrySet()) {
      final String awardGroup = entry.getKey();
      final FinalistSchedule schedule = entry.getValue();
      schedule.store(connection, tournament.getTournamentID(), awardGroup);
    }
  }

  /**
   * Load all finalist schedules for a tournament.
   * 
   * @param connection database connection
   * @param tournament the tournament to load finalist schedules for
   * @return the schedules, may be empty
   * @throws SQLException on a database error
   */
  public static Map<String, FinalistSchedule> loadSchedules(final Connection connection,
                                                            final Tournament tournament)
      throws SQLException {
    final Map<String, FinalistSchedule> schedules = new HashMap<>();
    for (final String awardGroup : FinalistSchedule.getAllDivisions(connection, tournament.getTournamentID())) {
      final FinalistSchedule schedule = new FinalistSchedule(connection, tournament.getTournamentID(), awardGroup);
      schedules.put(awardGroup, schedule);
    }

    return schedules;
  }

}
