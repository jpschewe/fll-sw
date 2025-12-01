/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import fll.xml.NonNumericCategory;

/**
 * Keep track of non-numeric nominees for a category used in finalist
 * scheduling.
 * Each instance represents a category and the teams in that category.
 * Mirrors javascript class in fll-objects.js. Property names need to match the
 * javascript/JSON.
 */
public class FinalistNonNumericNominees {

  /**
   * @param categoryName see {@link #getCategoryName()}
   * @param nominees see {@link #getNominees()}
   */
  public FinalistNonNumericNominees(@JsonProperty("categoryName") final String categoryName,
                                    @JsonProperty("nominees") final Collection<Integer> nominees) {
    this.mCategoryName = categoryName;
    this.nominees = new HashSet<>(nominees);
  }

  private final String mCategoryName;

  /**
   * @return title of the category for these nominees.
   * @see NonNumericCategory#getTitle()
   */
  public String getCategoryName() {
    return mCategoryName;
  }

  /**
   * Store this instance in the database. This replaces any nominees
   * for the category.
   * 
   * @param connection database connection
   * @param tournamentId the tournament to store the nominees with
   * @throws SQLException on a database error
   */
  public void store(final Connection connection,
                    final int tournamentId)
      throws SQLException {
    storeNominees(connection, tournamentId, mCategoryName, nominees);
  }

  private final Set<Integer> nominees;

  /**
   * @return read-only set of the team numbers that are nominated in the category
   */
  public Set<Integer> getNominees() {
    return Collections.unmodifiableSet(nominees);
  }

  /**
   * Replace the nominees in the database for the specified category.
   * 
   * @param connection database to add the nominees to
   * @param tournamentId the tournament to add the nominees to
   * @param category the category the nominees are for
   * @param nominees see {@link #getNominees()}
   * @throws SQLException on a database error
   */
  private static void storeNominees(final Connection connection,
                                    final int tournamentId,
                                    final String category,
                                    final Set<Integer> nominees)
      throws SQLException {
    try (PreparedStatement delete = connection.prepareStatement("DELETE FROM finalist_non_numeric_nominees"//
        + " WHERE tournament = ?"//
        + " AND category = ?")) {
      delete.setInt(1, tournamentId);
      delete.setString(2, category);
      delete.executeUpdate();
    }

    try (PreparedStatement insert = connection.prepareStatement("INSERT INTO finalist_non_numeric_nominees" //
        + " (tournament, category, team_number) VALUES(?, ?, ?)")) {

      insert.setInt(1, tournamentId);
      insert.setString(2, category);

      for (final Integer nominee : nominees) {
        insert.setInt(3, nominee);
        insert.executeUpdate();
      } // foreach nominee

    } // allocation of PreparedStatement
  }

  /**
   * Get all non-numeric categories known for the specified tournament.
   * 
   * @param connection the database to get the categories from
   * @param tournamentId the tournament to get the categories for
   * @return the non-numeric categories
   * @throws SQLException on a database error
   */
  public static Set<String> getCategories(final Connection connection,
                                          final int tournamentId)
      throws SQLException {
    final Set<String> result = new HashSet<>();
    try (
        PreparedStatement get = connection.prepareStatement("SELECT DISTINCT category FROM finalist_non_numeric_nominees WHERE tournament = ?")) {
      get.setInt(1, tournamentId);
      try (ResultSet rs = get.executeQuery()) {
        while (rs.next()) {
          final String category = rs.getString(1);
          if (null != category) {
            result.add(category);
          }
        }
      }
    }
    return result;
  }

  /**
   * Get all nominees in the specified category.
   * 
   * @param connection database connection
   * @param tournamentId the tournament
   * @param category the category
   * @return teams that are nominees in the category
   * @throws SQLException on a database error
   */
  public static Set<Integer> getNominees(final Connection connection,
                                         final int tournamentId,
                                         final String category)
      throws SQLException {
    final Set<Integer> result = new HashSet<>();
    try (PreparedStatement get = connection.prepareStatement("SELECT team_number FROM finalist_non_numeric_nominees"
        + " WHERE tournament = ?" //
        + " AND category = ?")) {
      get.setInt(1, tournamentId);
      get.setString(2, category);
      try (ResultSet rs = get.executeQuery()) {
        while (rs.next()) {
          final int team = rs.getInt("team_number");

          result.add(team);
        } // foreach result
      } // allocate query
    } // allocate prepared statement

    return result;
  }

}
