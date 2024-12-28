/*
 * Copyright (c) 2013 High Tech Kids.  All rights reserved
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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Tournament;

/**
 * Information about a tournament table.
 */
public final class TableInformation implements Serializable, Comparable<TableInformation> {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * @param id the id of the table information
   * @param sideA name of side A
   * @param sideB name of side B
   * @param sortOrder {@link #getSortOrder()}
   */
  public TableInformation(final int id,
                          final String sideA,
                          final String sideB,
                          final int sortOrder) {
    mId = id;
    mSideA = sideA;
    mSideB = sideB;
    this.sortOrder = sortOrder;
  }

  private final int sortOrder;

  /**
   * @return used to sort this table pair compared to other table pairs
   */
  public int getSortOrder() {
    return sortOrder;
  }

  private final String mSideA;

  /**
   * @return name of one side of the table
   */
  public String getSideA() {
    return mSideA;
  }

  private final String mSideB;

  /**
   * @return name of the other side of the table
   */
  public String getSideB() {
    return mSideB;
  }

  private final int mId;

  /**
   * @return the id of the table pair
   */
  public int getId() {
    return mId;
  }

  @Override
  public String toString() {
    return "TableInformation [A: "
        + getSideA()
        + " B: "
        + getSideB()
        + " id: "
        + getId()
        + " sortOrder: "
        + getSortOrder()
        + "]";
  }

  private static List<Integer> getTablesForDivision(final Connection connection,
                                                    final int tournament,
                                                    final String division)
      throws SQLException {
    final List<Integer> tableIds = new LinkedList<>();
    try (PreparedStatement prep = connection.prepareStatement("SELECT table_id FROM table_division" //
        + " WHERE playoff_division = ?"//
        + " AND tournament = ?" //
    )) {
      prep.setString(1, division);
      prep.setInt(2, tournament);
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final int id = rs.getInt(1);
          tableIds.add(id);
        }
      }
    }
    return tableIds;
  }

  /**
   * Filter {@code tables} to those that are specified for us in the playoff
   * bracket {@code bracket}.
   * 
   * @param tables the tables to filter, will not be modified
   * @param connection database
   * @param tournament the tournament
   * @param bracket the bracket
   * @return a new list of the tables to use
   * @throws SQLException
   */
  public static List<TableInformation> filterToTablesForBracket(final List<TableInformation> tables,
                                                                final Connection connection,
                                                                final Tournament tournament,
                                                                final String bracket)
      throws SQLException {
    final List<Integer> tableIdsForDivision = getTablesForDivision(connection, tournament.getTournamentID(), bracket);

    final List<TableInformation> useTables = tables.stream().filter(table -> tableIdsForDivision.isEmpty()
        || tableIdsForDivision.contains(table.getId())).collect(Collectors.toList());

    if (useTables.isEmpty()
        && !tables.isEmpty()) {
      LOGGER.warn("Tables are defined, but none are set to be used by bracket {}. This is unexpected, using all tables");
      return new LinkedList<>(tables);
    } else {
      return useTables;
    }
  }

  /**
   * Sort {@code tables} by usage from least used to most used.
   * 
   * @param tables the tables to sort, this list will be modified by sort
   * @param connection the database
   * @param tournament the tournament
   * @throws SQLException on a database error
   */
  public static void sortByTableUsage(final List<TableInformation> tables,
                                      final Connection connection,
                                      final Tournament tournament)
      throws SQLException {

    // sort by the usage
    final Map<Integer, Integer> tableUsage = new HashMap<>();
    try (PreparedStatement prep = connection.prepareStatement("select tablenames.PairID, COUNT(tablenames.PairID) as c"//
        + " FROM PlayoffTableData, tablenames" //
        + " WHERE PlayoffTableData.Tournament = ?" //
        + " AND PlayoffTableData.Tournament = tablenames.Tournament" //
        + " AND AssignedTable IS NOT NULL" //
        + " AND (PlayoffTableData.AssignedTable = tablenames.SideA OR PlayoffTableData.AssignedTable = tablenames.SideB)"//
        + " GROUP BY tablenames.PairID")) {
      prep.setInt(1, tournament.getTournamentID());

      // get table usage
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final int pairId = rs.getInt(1);
          final int count = rs.getInt(2);
          tableUsage.put(pairId, count);
        }
      } // result set
    } // prepared statement

    // sort by table usage
    Collections.sort(tables, (one,
                              two) -> {
      final Integer oneUse = tableUsage.get(one.getId());
      final Integer twoUse = tableUsage.get(two.getId());
      if (null == oneUse
          && null == twoUse) {
        return 0;
      } else if (null == oneUse) {
        return -1;
      } else if (null == twoUse) {
        return 1;
      } else {
        return oneUse.compareTo(twoUse);
      }
    });

  }

  /**
   * Replace the table information for the specified tournament.
   * 
   * @param connection the database
   * @param tournament the tournament to replace information for
   * @param tables the new table information
   * @throws SQLException on a database error
   */
  public static void saveTournamentTableInformation(final Connection connection,
                                                    final Tournament tournament,
                                                    final Collection<TableInformation> tables)
      throws SQLException {
    final boolean autoCommit = connection.getAutoCommit();
    try {
      connection.setAutoCommit(false);

      try (PreparedStatement delete = connection.prepareStatement("DELETE FROM tablenames WHERE tournament = ?")) {
        delete.setInt(1, tournament.getTournamentID());
        delete.executeUpdate();
      }

      try (
          PreparedStatement prep = connection.prepareStatement("INSERT INTO tablenames (Tournament, PairID, SideA, SideB, sort_order) VALUES(?, ?, ?, ?, ?)")) {
        prep.setInt(1, tournament.getTournamentID());

        for (final TableInformation table : tables) {
          prep.setInt(2, table.getId());
          prep.setString(3, table.getSideA());
          prep.setString(4, table.getSideB());
          prep.setInt(5, table.getSortOrder());
          prep.executeUpdate();
        }
      }

      connection.commit();
    } finally {
      connection.setAutoCommit(autoCommit);
    }
  }

  /**
   * Get table information for a tournament.
   *
   * @param connection where to get the table information from
   * @param tournament the tournament to get the information from
   * @return tables sorted by {@link #compareTo(TableInformation)}
   * @throws SQLException if there is a database error
   */
  public static List<TableInformation> getTournamentTableInformation(final Connection connection,
                                                                     final Tournament tournament)
      throws SQLException {
    final List<TableInformation> tableInfo = new LinkedList<>();
    try (
        PreparedStatement getAllTables = connection.prepareStatement("select tablenames.PairID, tablenames.SideA, tablenames.SideB, sort_order" //
            + " FROM tablenames" //
            + " WHERE tablenames.Tournament = ?")) {
      getAllTables.setInt(1, tournament.getTournamentID());
      try (ResultSet allTables = getAllTables.executeQuery()) {
        while (allTables.next()) {
          final int pairId = allTables.getInt(1);
          final String sideA = castNonNull(allTables.getString(2));
          final String sideB = castNonNull(allTables.getString(3));
          final int sortOrder = allTables.getInt(4);

          final TableInformation info = new TableInformation(pairId, sideA, sideB, sortOrder);
          tableInfo.add(info);
        } // foreach result
      } // allTables
    } // getAllTables

    Collections.sort(tableInfo);

    return tableInfo;
  }

  /**
   * Given a table side, find the table information that matches.
   *
   * @param tableInfo the list of all table information to look in
   * @param tableSide the table side to match
   * @return the table information or null
   */
  public static @Nullable TableInformation getTableInformationForTableSide(final List<TableInformation> tableInfo,
                                                                           final String tableSide) {
    for (final TableInformation info : tableInfo) {
      if (info.getSideA().equals(tableSide)
          || info.getSideB().equals(tableSide)) {
        return info;
      }
    }
    return null;
  }

  /**
   * @param connection database connection
   * @param tournament tournament to get tables for
   * @return all table names at the tournament
   * @throws SQLException on database error
   */
  public static Collection<String> getAllTableNames(final Connection connection,
                                                    final Tournament tournament)
      throws SQLException {
    final Collection<String> tables = new LinkedList<>();

    try (
        PreparedStatement prep = connection.prepareStatement("SELECT sidea, sideb FROM tablenames WHERE tournament = ? ORDER BY sort_order, SideA")) {
      prep.setInt(1, tournament.getTournamentID());
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String sideA = castNonNull(rs.getString(1));
          tables.add(sideA);

          final String sideB = castNonNull(rs.getString(2));
          tables.add(sideB);
        }
      } // result set
    } // prepared statement

    return tables;
  }

  /**
   * Order first on {@link #getSortOrder()} and then on {@link #getSideA()}.
   */
  @Override
  public int compareTo(final TableInformation o) {
    if (getSortOrder() < o.getSortOrder()) {
      return -1;
    } else if (getSortOrder() > o.getSortOrder()) {
      return 1;
    } else {
      return getSideA().compareTo(o.getSideA());
    }
  }

  @Override
  public int hashCode() {
    return getId();
  }

  @Override
  public boolean equals(final @Nullable Object o) {
    if (o instanceof TableInformation) {
      return this.equals((TableInformation) o);
    } else {
      return false;
    }
  }

  public boolean equals(final TableInformation other) {
    return getId() == other.getId() //
        && getSortOrder() == other.getSortOrder() //
        && getSideA().equals(other.getSideA()) //
        && getSideB().equals(other.getSideB());
  }

}