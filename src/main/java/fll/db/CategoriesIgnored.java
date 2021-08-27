/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import fll.TournamentLevel;
import fll.xml.NonNumericCategory;

/**
 * Information about categories that exist in the challenge description, but
 * aren't awarded.
 */
public final class CategoriesIgnored {

  private CategoriesIgnored() {
  }

  private enum CategoryType {
    SUBJECTIVE, PERFORMANCE, NON_NUMERIC;
  }

  /**
   * @param connection database connection
   * @param level tournament level
   * @param category the category to check
   * @return true if the specified non-numeric category is not awarded at the
   *         specified tournament level
   * @throws SQLException on a database error
   */
  public static boolean isNonNumericCategoryIgnored(final Connection connection,
                                                    final TournamentLevel level,
                                                    final NonNumericCategory category)
      throws SQLException {
    try (PreparedStatement prep = connection.prepareStatement("SELECT category_identifier" //
        + " FROM categories_ignored" //
        + " WHERE level_id = ?" //
        + "  AND category_type = ?" //
        + "  AND category_identifier = ?")) {
      prep.setInt(1, level.getId());
      prep.setString(2, CategoryType.NON_NUMERIC.name());
      prep.setString(3, category.getTitle());
      try (ResultSet rs = prep.executeQuery()) {
        return rs.next();
      }
    }
  }

  /**
   * @param connection database connection
   * @param level tournament level
   * @param categories categories to NOT award at the specified tournament level,
   *          may be empty
   * @throws SQLException on a database error
   */
  public static void storeIgnoredNonNumericCategories(final Connection connection,
                                                      final TournamentLevel level,
                                                      final Collection<NonNumericCategory> categories)
      throws SQLException {
    try (
        PreparedStatement delete = connection.prepareStatement("DELETE FROM categories_ignored WHERE level_id = ? AND category_type = ?");
        PreparedStatement insert = connection.prepareStatement("INSERT INTO categories_ignored (level_id, category_type, category_identifier) VALUES(?, ?, ?)")) {

      delete.setInt(1, level.getId());
      delete.setString(2, CategoryType.NON_NUMERIC.name());
      delete.executeUpdate();

      if (!categories.isEmpty()) {
        insert.setInt(1, level.getId());
        insert.setString(2, CategoryType.NON_NUMERIC.name());

        for (final NonNumericCategory category : categories) {
          insert.setString(3, category.getTitle());
          insert.addBatch();
        }
        insert.executeBatch();
      }
    }
  }

}
