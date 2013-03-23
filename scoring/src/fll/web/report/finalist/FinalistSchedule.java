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
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.mtu.eggplant.util.sql.SQLFunctions;
import fll.db.Queries;

/**
 * The schedule for finalist judging.
 */
public class FinalistSchedule implements Serializable {

  /**
   * Create a schedule.
   * 
   * @param tournament the tournament that the schedule is associated with
   * @param categories key is the category name, value is if the category is
   *          public
   * @param schedule the schedule entries
   */
  public FinalistSchedule(final int tournament,
                          final String division,
                          final Map<String, Boolean> categories,
                          final Collection<FinalistDBRow> schedule) {
    this.mTournament = tournament;
    this.mDivision = division;
    this.mCategories = Collections.unmodifiableMap(new HashMap<String, Boolean>(categories));
    this.mSchedule = Collections.unmodifiableCollection(new LinkedList<FinalistDBRow>(schedule));
  }

  /**
   * Load a schedule from the database.
   * 
   * @param connection where to load from
   * @param tournament the tournament to load a schedule for
   */
  public FinalistSchedule(final Connection connection,
                          final int tournament,
                          final String division) throws SQLException {
    final Map<String, Boolean> newCategories = new HashMap<String, Boolean>();
    final Collection<FinalistDBRow> newSchedule = new LinkedList<FinalistDBRow>();
    mTournament = tournament;
    mDivision = division;

    PreparedStatement getCategories = null;
    ResultSet categories = null;
    PreparedStatement getSchedule = null;
    ResultSet schedule = null;
    try {
      getCategories = connection.prepareStatement("SELECT category, is_public FROM finalist_categories WHERE tournament = ? AND division = ?");
      getCategories.setInt(1, mTournament);
      getCategories.setString(2, mDivision);
      categories = getCategories.executeQuery();
      while (categories.next()) {
        final String name = categories.getString(1);
        final boolean isPublic = categories.getBoolean(2);
        newCategories.put(name, isPublic);
      }

      getSchedule = connection.prepareStatement("SELECT category, judge_time, team_number FROM finalist_schedule WHERE tournament = ? AND division = ?");
      getSchedule.setInt(1, mTournament);
      getSchedule.setString(2, mDivision);
      schedule = getSchedule.executeQuery();
      while (schedule.next()) {
        final String name = schedule.getString(1);
        final Time judgeTime = schedule.getTime(2);
        final int teamNumber = schedule.getInt(3);

        final Date judgeDate = Queries.timeToDate(judgeTime);
        final Calendar cal = Calendar.getInstance();
        cal.setTime(judgeDate);
        final int hour = cal.get(Calendar.HOUR);
        final int minute = cal.get(Calendar.MINUTE);

        final FinalistDBRow row = new FinalistDBRow(name, hour, minute, teamNumber);
        newSchedule.add(row);
      }

      mCategories = Collections.unmodifiableMap(newCategories);
      mSchedule = Collections.unmodifiableCollection(newSchedule);

    } finally {
      SQLFunctions.close(categories);
      SQLFunctions.close(getCategories);
      SQLFunctions.close(schedule);
      SQLFunctions.close(getSchedule);
    }
  }

  private final Map<String, Boolean> mCategories;

  /**
   * Unmodifiable version of the categories.
   * 
   * @return key=category name, value=is public
   */
  public Map<String, Boolean> getCategories() {
    return Collections.unmodifiableMap(mCategories);
  }

  private final Collection<FinalistDBRow> mSchedule;

  /**
   * The schedule time slots for the specified category.
   * 
   * @return list sorted by time
   */
  public List<FinalistDBRow> getScheduleForCategory(final String category) {
    final List<FinalistDBRow> result = new LinkedList<FinalistDBRow>();
    for (final FinalistDBRow row : mSchedule) {
      if (row.getCategoryName().equals(category)) {
        result.add(row);
      }
    }
    Collections.sort(result, FinalistDBRow.TIME_SORT_INSTANCE);
    return result;
  }

  private final int mTournament;

  public int getTournament() {
    return mTournament;
  }

  private final String mDivision;

  public String getDivision() {
    return mDivision;
  }

  /**
   * Store the schedule to the database. Remove any finalist schedule existing
   * for the tournament.
   * 
   * @param connection
   * @throws SQLException
   */
  public void store(final Connection connection) throws SQLException {
    PreparedStatement deleteCategoriesPrep = null;
    PreparedStatement insertCategoriesPrep = null;
    PreparedStatement deleteSchedPrep = null;
    PreparedStatement insertSchedPrep = null;
    try {
      deleteSchedPrep = connection.prepareStatement("DELETE FROM finalist_schedule WHERE tournament = ? AND division = ?");
      deleteSchedPrep.setInt(1, getTournament());
      deleteSchedPrep.setString(2, getDivision());
      deleteSchedPrep.executeUpdate();

      deleteCategoriesPrep = connection.prepareStatement("DELETE FROM finalist_categories WHERE tournament = ? AND division = ?");
      deleteCategoriesPrep.setInt(1, getTournament());
      deleteCategoriesPrep.setString(2, getDivision());
      deleteCategoriesPrep.executeUpdate();

      insertCategoriesPrep = connection.prepareStatement("INSERT INTO finalist_categories (tournament, division, category, is_public) VALUES(?, ?, ?, ?)");
      insertCategoriesPrep.setInt(1, getTournament());
      insertCategoriesPrep.setString(2, getDivision());

      for (final Map.Entry<String, Boolean> entry : mCategories.entrySet()) {
        insertCategoriesPrep.setString(3, entry.getKey());
        insertCategoriesPrep.setBoolean(4, entry.getValue());
        insertCategoriesPrep.executeUpdate();
      }

      insertSchedPrep = connection.prepareStatement("INSERT INTO finalist_schedule (tournament, division, category, judge_time, team_number) VALUES(?, ?, ?, ?, ?)");
      insertSchedPrep.setInt(1, getTournament());
      insertSchedPrep.setString(2, getDivision());
      for (final FinalistDBRow row : mSchedule) {
        insertSchedPrep.setString(3, row.getCategoryName());
        insertSchedPrep.setTime(4, Queries.dateToTime(row.getTime()));
        insertSchedPrep.setInt(5, row.getTeamNumber());
        insertSchedPrep.executeUpdate();
      }

    } finally {
      SQLFunctions.close(deleteSchedPrep);
      SQLFunctions.close(insertSchedPrep);
      SQLFunctions.close(deleteCategoriesPrep);
      SQLFunctions.close(insertCategoriesPrep);
    }
  }

  /**
   * Get the names of all divisions that have finalist schedules
   * stored for the specified tournament.
   * 
   * @param connection the database connection
   * @param tournament the tournament id
   * @return the division names
   * @throws SQLException
   */
  public static Collection<String> getAllDivisions(final Connection connection,
                                                   final int tournament) throws SQLException {
    PreparedStatement getDivisions = null;
    ResultSet divisions = null;
    final Collection<String> result = new LinkedList<String>();
    try {
      getDivisions = connection.prepareStatement("SELECT DISTINCT division FROM finalist_categories WHERE tournament = ?");
      getDivisions.setInt(1, tournament);
      divisions = getDivisions.executeQuery();
      while (divisions.next()) {
        result.add(divisions.getString(1));
      }
    } finally {
      SQLFunctions.close(divisions);
      SQLFunctions.close(getDivisions);
    }

    return result;
  }

}
