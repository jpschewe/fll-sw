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

/**
 * Keep track of non-numeric subjective nominees.
 * The subjective categories defined here are those that are not listed in the
 * challenge descriptor.
 * Each instance represents a category and the teams in that category.
 * Mirrors javascript class in fll-objects.js. Property names need to match the
 * javascript/JSON.
 */
public class NonNumericNominees {

  public NonNumericNominees(@JsonProperty("categoryName") final String categoryName,
                            @JsonProperty("teamNumbers") final Collection<Integer> teamNumbers) {
    mCategoryName = categoryName;
    mTeamNumbers = new HashSet<>(teamNumbers);
  }

  private final String mCategoryName;

  /**
   * @return Name of the category for these nominees.
   */
  public String getCategoryName() {
    return mCategoryName;
  }

  private final Set<Integer> mTeamNumbers;

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
    clearNominees(connection, tournamentId, mCategoryName);
    addNominees(connection, tournamentId, mCategoryName, mTeamNumbers);
  }

  /**
   * Numbers of the teams that are the nominees.
   * 
   * @return read-only set
   */
  public Set<Integer> getTeamNumbers() {
    return Collections.unmodifiableSet(mTeamNumbers);
  }

  /**
   * Clear the nominees for the specified category at the tournament.
   * 
   * @param connection database connection
   * @param tournamentId the tournament
   * @param category the category
   * @throws SQLException
   */
  public static void clearNominees(final Connection connection,
                                   final int tournamentId,
                                   final String category)
      throws SQLException {
    try (PreparedStatement delete = connection.prepareStatement("DELETE FROM non_numeric_nominees"//
        + " WHERE tournament = ?"//
        + " AND category = ?")) {
      delete.setInt(1, tournamentId);
      delete.setString(2, category);
      delete.executeUpdate();
    }
  }

  /**
   * Add a nominee to the database. If they already are a nominee, this function
   * does nothing.
   * 
   * @param connection database connection
   * @param tournamentId passed through
   * @param category passed through
   * @param teamNumber passed as a singleton set
   * @throws SQLException on a database error
   * @see #addNominees(Connection, int, String, Set)
   */
  public static void addNominee(final Connection connection,
                                final int tournamentId,
                                final String category,
                                final int teamNumber)
      throws SQLException {
    addNominees(connection, tournamentId, category, Collections.singleton(teamNumber));
  }

  /**
   * Add a set of nominees to the database. If the nominee already exsts, there
   * is no error.
   * 
   * @param connection database to add the nominees to
   * @param tournamentId the tournament to add the nominees to
   * @param category the category the nominees are for
   * @param teamNumbers the teams that are nominees
   * @throws SQLException on a database error
   */
  public static void addNominees(final Connection connection,
                                 final int tournamentId,
                                 final String category,
                                 final Set<Integer> teamNumbers)
      throws SQLException {
    final boolean autoCommit = connection.getAutoCommit();
    try {
      connection.setAutoCommit(false);

      try (PreparedStatement check = connection.prepareStatement("SELECT team_number FROM non_numeric_nominees" //
          + " WHERE tournament = ?" //
          + "   AND category = ?" //
          + "   AND team_number = ?");
          PreparedStatement insert = connection.prepareStatement("INSERT INTO non_numeric_nominees" //
              + " (tournament, category, team_number) VALUES(?, ?, ?)")) {

        check.setInt(1, tournamentId);
        check.setString(2, category);

        insert.setInt(1, tournamentId);
        insert.setString(2, category);

        for (final int teamNumber : teamNumbers) {
          check.setInt(3, teamNumber);
          insert.setInt(3, teamNumber);

          try (ResultSet checkResult = check.executeQuery()) {
            if (!checkResult.next()) {
              insert.executeUpdate();
            }
          }
        }

        connection.commit();
      } // allocation of PreparedStatements
    } finally {
      connection.setAutoCommit(autoCommit);
    }
  }

  /**
   * Get all subjective categories know for the specified tournament.
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
        PreparedStatement get = connection.prepareStatement("SELECT DISTINCT category FROM non_numeric_nominees WHERE tournament = ?")) {
      get.setInt(1, tournamentId);
      try (ResultSet rs = get.executeQuery()) {
        while (rs.next()) {
          final String category = rs.getString(1);
          result.add(category);
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
    try (PreparedStatement get = connection.prepareStatement("SELECT DISTINCT team_number FROM non_numeric_nominees"
        + " WHERE tournament = ?" //
        + " AND category = ?")) {
      get.setInt(1, tournamentId);
      get.setString(2, category);
      try (ResultSet rs = get.executeQuery()) {
        while (rs.next()) {
          final int team = rs.getInt(1);
          result.add(team);
        }
      }
    }

    return result;
  }

}
