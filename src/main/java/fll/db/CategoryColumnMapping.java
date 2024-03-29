/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;

import fll.util.FLLInternalException;

/**
 * Mapping between subjective category and schedule columns.
 */
public class CategoryColumnMapping implements Serializable {

  /**
   * @param categoryName see {@link #getCategoryName()}
   * @param scheduleColumn see {@link #getScheduleColumn()}
   */
  public CategoryColumnMapping(final String categoryName,
                               final String scheduleColumn) {
    mCategoryName = categoryName;
    mScheduleColumn = scheduleColumn;
  }

  private final String mCategoryName;

  /**
   * @return the name of the category
   */
  public String getCategoryName() {
    return mCategoryName;
  }

  private final String mScheduleColumn;

  /**
   * @return the schedule column that matches to the category name
   */
  public String getScheduleColumn() {
    return mScheduleColumn;
  }

  /**
   * Store a set of mappings for a tournament to the database.
   *
   * @param connection the database to store the data in
   * @param tournamentId the id of the tournament
   * @param categoryColumnMappings the data to store
   * @throws SQLException if there is a problem talking to the database
   */
  public static void store(final Connection connection,
                           final int tournamentId,
                           final Collection<CategoryColumnMapping> categoryColumnMappings)
      throws SQLException {
    try (
        PreparedStatement delete = connection.prepareStatement("DELETE FROM category_schedule_column WHERE tournament = ?")) {
      delete.setInt(1, tournamentId);
      delete.executeUpdate();
    }

    try (PreparedStatement insert = connection.prepareStatement("INSERT INTO category_schedule_column" //
        + " (tournament, category, schedule_column)" //
        + " VALUES(?, ?, ?)")) {
      insert.setInt(1, tournamentId);
      for (final CategoryColumnMapping mapping : categoryColumnMappings) {
        insert.setString(2, mapping.getCategoryName());
        insert.setString(3, mapping.getScheduleColumn());
        insert.executeUpdate();
      }
    }
  }

  /**
   * Load a set of mappings for a tournament from the database.
   *
   * @param connection where to load the data from
   * @param tournamentId specify the tournament to load
   * @throws SQLException if there is a database error
   * @return the loaded data
   */
  public static Collection<CategoryColumnMapping> load(final Connection connection,
                                                       final int tournamentId)
      throws SQLException {
    final Collection<CategoryColumnMapping> mappings = new LinkedList<>();

    try (
        PreparedStatement get = connection.prepareStatement("SELECT category, schedule_column FROM category_schedule_column WHERE tournament = ?")) {
      get.setInt(1, tournamentId);
      try (ResultSet found = get.executeQuery()) {
        while (found.next()) {
          final String category = found.getString(1);
          final String column = found.getString(2);
          if (null == category
              || null == column) {
            throw new FLLInternalException("Inconsistent database, category or schedule_column is null in category_schedule_column");
          }
          final CategoryColumnMapping map = new CategoryColumnMapping(category, column);
          mappings.add(map);
        }
      }
    }

    return mappings;
  }

}
