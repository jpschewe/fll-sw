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

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Tournament;

/**
 * Information about a tournament table.
 */
public final class TableInformation implements Serializable {
  /**
   * @param id the id of the table information
   * @param sideA name of side A
   * @param sideB name of side B
   * @param use true if this table should be used
   */
  public TableInformation(final int id,
                          final String sideA,
                          final String sideB,
                          final boolean use) {
    mId = id;
    mSideA = sideA;
    mSideB = sideB;
    mUse = use;
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

  private final boolean mUse;

  /**
   * @return if this table pair is to be used
   */
  public boolean getUse() {
    return mUse;
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
        + " use: "
        + getUse()
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
   * Get table information for a tournament.
   *
   * @param connection where to get the table information from
   * @param tournament the tournament to get the information from
   * @param division the award group to get the table information for
   * @return Tables listed by least used first
   * @throws SQLException if there is a database error
   */
  public static List<TableInformation> getTournamentTableInformation(final Connection connection,
                                                                     final int tournament,
                                                                     final String division)
      throws SQLException {
    final List<Integer> tableIdsForDivision = getTablesForDivision(connection, tournament, division);

    final List<TableInformation> tableInfo = new LinkedList<>();
    final Map<Integer, Integer> tableUsage = new HashMap<>();
    try (
        PreparedStatement getAllTables = connection.prepareStatement("select tablenames.PairID, tablenames.SideA, tablenames.SideB" //
            + " FROM tablenames" //
            + " WHERE tablenames.Tournament = ?")) {
      getAllTables.setInt(1, tournament);
      try (ResultSet allTables = getAllTables.executeQuery()) {
        while (allTables.next()) {
          final int pairId = allTables.getInt(1);
          final String sideA = castNonNull(allTables.getString(2));
          final String sideB = castNonNull(allTables.getString(3));

          final boolean use = tableIdsForDivision.isEmpty()
              || tableIdsForDivision.contains(pairId);

          final TableInformation info = new TableInformation(pairId, sideA, sideB, use);
          tableInfo.add(info);
        } // foreach result
      } // allTables
    } // getAllTables

    // sort by the usage
    try (PreparedStatement prep = connection.prepareStatement("select tablenames.PairID, COUNT(tablenames.PairID) as c"//
        + " FROM PlayoffData, tablenames" //
        + " WHERE PlayoffData.Tournament = ?" //
        + " AND PlayoffData.Tournament = tablenames.Tournament" //
        + " AND AssignedTable IS NOT NULL" //
        + " AND (PlayoffData.AssignedTable = tablenames.SideA OR PlayoffData.AssignedTable = tablenames.SideB)"//
        + " GROUP BY tablenames.PairID")) {
      prep.setInt(1, tournament);

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
    Collections.sort(tableInfo, (one,
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
   */
  public static Collection<String> getAllTableNames(final Connection connection,
                                                    final Tournament tournament)
      throws SQLException {
    final Collection<String> tables = new LinkedList<>();

    try (
        PreparedStatement prep = connection.prepareStatement("SELECT sidea, sideb FROM tablenames WHERE tournament = ?")) {
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

}