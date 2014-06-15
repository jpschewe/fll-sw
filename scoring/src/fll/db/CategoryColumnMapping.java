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

import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Mapping between subjective category and schedule columns.
 */
public class CategoryColumnMapping implements Serializable {

  public CategoryColumnMapping(final String categoryName,
                               final String scheduleColumn) {
    mCategoryName = categoryName;
    mScheduleColumn = scheduleColumn;
  }

  private final String mCategoryName;

  public String getCategoryName() {
    return mCategoryName;
  }

  private final String mScheduleColumn;

  public String getScheduleColumn() {
    return mScheduleColumn;
  }

  /**
   * Store a set of mappings for a tournament to the database.
   */
  public static void store(final Connection connection,
                           final int tournamentId,
                           final Collection<CategoryColumnMapping> categoryColumnMappings) throws SQLException {
    PreparedStatement insert = null;
    PreparedStatement delete = null;
    try {
      delete = connection.prepareStatement("DELETE FROM category_schedule_column WHERE tournament = ?");
      delete.setInt(1, tournamentId);
      delete.executeUpdate();

      insert = connection.prepareStatement("INSERT INTO category_schedule_column" //
          + " (tournament, category, schedule_column)" //
          + " VALUES(?, ?, ?)");
      insert.setInt(1, tournamentId);
      for (final CategoryColumnMapping mapping : categoryColumnMappings) {
        insert.setString(2, mapping.getCategoryName());
        insert.setString(3, mapping.getScheduleColumn());
        insert.executeUpdate();
      }

    } finally {
      SQLFunctions.close(delete);
      SQLFunctions.close(insert);
    }
  }

  /**
   * Load a set of mappings for a tournament from the database.
   */
  public static Collection<CategoryColumnMapping> load(final Connection connection,
                                                       final int tournamentId) throws SQLException {
    final Collection<CategoryColumnMapping> mappings = new LinkedList<CategoryColumnMapping>();

    PreparedStatement get = null;
    ResultSet found = null;
    try {
      get = connection.prepareStatement("SELECT category, schedule_column FROM category_schedule_column WHERE tournament = ?");
      get.setInt(1, tournamentId);
      found = get.executeQuery();
      while (found.next()) {
        final String category = found.getString(1);
        final String column = found.getString(2);
        final CategoryColumnMapping map = new CategoryColumnMapping(category, column);
        mappings.add(map);
      }
    } finally {
      SQLFunctions.close(found);
      SQLFunctions.close(get);
    }

    return mappings;
  }

}
