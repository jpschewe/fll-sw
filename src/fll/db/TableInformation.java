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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.mtu.eggplant.util.sql.SQLFunctions;

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

  public String getSideA() {
    return mSideA;
  }

  private final String mSideB;

  public String getSideB() {
    return mSideB;
  }

  private final boolean mUse;

  public boolean getUse() {
    return mUse;
  }

  private final int mId;

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
    final List<Integer> tableIds = new LinkedList<Integer>();
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT table_id FROM table_division" //
          + " WHERE playoff_division = ?"//
          + " AND tournament = ?" //
      );
      prep.setString(1, division);
      prep.setInt(2, tournament);
      rs = prep.executeQuery();
      while (rs.next()) {
        final int id = rs.getInt(1);
        tableIds.add(id);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }
    return tableIds;
  }

  /**
   * Get table information for a tournament.
   * 
   * @param connection
   * @param tournament
   * @param division
   * @return Tables listed by least used first
   */
  public static List<TableInformation> getTournamentTableInformation(final Connection connection,
                                                                     final int tournament,
                                                                     final String division)
      throws SQLException {
    final List<Integer> tableIdsForDivision = getTablesForDivision(connection, tournament, division);

    final List<TableInformation> tableInfo = new LinkedList<TableInformation>();
    PreparedStatement getAllTables = null;
    ResultSet allTables = null;
    PreparedStatement prep = null;
    ResultSet rs = null;
    final Map<Integer, Integer> tableUsage = new HashMap<Integer, Integer>();
    try {
      // get all tables
      getAllTables = connection.prepareStatement("select tablenames.PairID, tablenames.SideA, tablenames.SideB" //
          + " FROM tablenames" //
          + " WHERE tablenames.Tournament = ?");
      getAllTables.setInt(1, tournament);
      allTables = getAllTables.executeQuery();
      while (allTables.next()) {
        final int pairId = allTables.getInt(1);
        final String sideA = allTables.getString(2);
        final String sideB = allTables.getString(3);

        final boolean use = tableIdsForDivision.isEmpty()
            || tableIdsForDivision.contains(pairId);

        final TableInformation info = new TableInformation(pairId, sideA, sideB, use);
        tableInfo.add(info);
      }

      // sort by the usage
      prep = connection.prepareStatement("select tablenames.PairID, COUNT(tablenames.PairID) as c"//
          + " FROM PlayoffData, tablenames" //
          + " WHERE PlayoffData.Tournament = ?" //
          + " AND PlayoffData.Tournament = tablenames.Tournament" //
          + " AND AssignedTable IS NOT NULL" //
          + " AND (PlayoffData.AssignedTable = tablenames.SideA OR PlayoffData.AssignedTable = tablenames.SideB)"//
          + " GROUP BY tablenames.PairID");
      prep.setInt(1, tournament);

      // get table usage
      rs = prep.executeQuery();
      while (rs.next()) {
        final int pairId = rs.getInt(1);
        final int count = rs.getInt(2);
        tableUsage.put(pairId, count);
      }

      // sort by table usage
      Collections.sort(tableInfo, new Comparator<TableInformation>() {
        @Override
        public int compare(final TableInformation one,
                           final TableInformation two) {
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
        }

      });

    } finally {
      SQLFunctions.close(allTables);
      SQLFunctions.close(getAllTables);
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
    }

    return tableInfo;
  }

  /**
   * Given a table side, find the table information that matches.
   * 
   * @param tableInfo the list of all table information to look in
   * @param tableSide the table side to match
   * @return the table information or null
   */
  public static TableInformation getTableInformationForTableSide(final List<TableInformation> tableInfo,
                                                                 final String tableSide) {
    for (final TableInformation info : tableInfo) {
      if (info.getSideA().equals(tableSide)
          || info.getSideB().equals(tableSide)) {
        return info;
      }
    }
    return null;
  }

}