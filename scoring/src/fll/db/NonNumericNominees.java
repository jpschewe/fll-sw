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
import java.util.HashSet;
import java.util.Set;

import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Keep track of non-numeric subjective nominees.
 * The subjective categories defined here are those that are not listed in the
 * challenge descriptor.
 */
public class NonNumericNominees {

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
                                   final String category) throws SQLException {
    PreparedStatement delete = null;
    try {
      delete = connection.prepareStatement("DELETE FROM non_numeric_nominees"//
          + " WHERE tournament = ?"//
          + " AND category = ?");
      delete.setInt(1, tournamentId);
      delete.setString(2, category);
      delete.executeUpdate();
    } finally {
      SQLFunctions.close(delete);
    }
  }

  /**
   * Add a nominee to the database. If they already are a nominee, this function
   * does nothing.
   * 
   * @throws SQLException
   */
  public static void addNominee(final Connection connection,
                                final int tournamentId,
                                final String category,
                                final int teamNumber) throws SQLException {
    PreparedStatement check = null;
    ResultSet checkResult = null;
    PreparedStatement insert = null;
    final boolean autoCommit = connection.getAutoCommit();
    try {
      connection.setAutoCommit(false);

      check = connection.prepareStatement("SELECT team_number FROM non_numeric_nominees" //
          + " WHERE tournament = ?" + "   AND category = ?" + "   AND team_number = ?");
      check.setInt(1, tournamentId);
      check.setString(2, category);
      check.setInt(3, teamNumber);

      insert = connection.prepareStatement("INSERT INTO non_numeric_nominees" //
          + " (tournament, category, team_number) VALUES(?, ?, ?)");
      insert.setInt(1, tournamentId);
      insert.setString(2, category);
      insert.setInt(3, teamNumber);

      checkResult = check.executeQuery();
      if (!checkResult.next()) {
        insert.executeUpdate();
      }

      connection.commit();
    } finally {
      connection.setAutoCommit(autoCommit);

      SQLFunctions.close(checkResult);
      SQLFunctions.close(check);
      SQLFunctions.close(insert);
    }
  }

  /**
   * Get all subjective categories know for the specified tournament.
   * 
   * @throws SQLException
   */
  public static Set<String> getCategories(final Connection connection,
                                          final int tournamentId) throws SQLException {
    final Set<String> result = new HashSet<>();
    PreparedStatement get = null;
    ResultSet rs = null;
    try {
      get = connection.prepareStatement("SELECT DISTINCT category FROM non_numeric_nominees WHERE tournament = ?");
      get.setInt(1, tournamentId);
      rs = get.executeQuery();
      while (rs.next()) {
        final String category = rs.getString(1);
        result.add(category);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(get);
    }
    return result;
  }

  /**
   * Get all nominees in the specified category.
   * 
   * @throws SQLException
   */
  public static Set<Integer> getNominees(final Connection connection,
                                         final int tournamentId,
                                         final String category) throws SQLException {
    final Set<Integer> result = new HashSet<>();
    PreparedStatement get = null;
    ResultSet rs = null;
    try {
      get = connection.prepareStatement("SELECT DISTINCT team_number FROM non_numeric_nominees"
          + " WHERE tournament = ?" //
          + " AND category = ?");
      get.setInt(1, tournamentId);
      get.setString(2, category);
      rs = get.executeQuery();
      while (rs.next()) {
        final int team = rs.getInt(1);
        result.add(team);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(get);
    }

    return result;
  }

}
