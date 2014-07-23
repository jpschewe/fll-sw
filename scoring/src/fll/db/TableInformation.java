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
import java.util.LinkedList;
import java.util.List;

import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Information about a tournament table.
 */
public final class TableInformation implements Serializable {
  /**
   * @param id the id of the table information
   * @param sideA name of side A
   * @param sideB name of side B
   * @param use can this table be used
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

  private static List<Integer> getTablesForDivision(final Connection connection,
                                                    final int tournament,
                                                    final String division) throws SQLException {
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
   * @return
   */
  public static List<TableInformation> getTournamentTableInformation(final Connection connection,
                                                                     final int tournament,
                                                                     final String division) throws SQLException {
    final List<Integer> tableIdsForDivision = getTablesForDivision(connection, tournament, division);

    final List<TableInformation> tableInfo = new LinkedList<TableInformation>();
    PreparedStatement prep = null;
    ResultSet rs = null;
    try {
      prep = connection.prepareStatement("SELECT PairID, SideA, SideB FROM tablenames" //
          + " WHERE Tournament = ?" //
      );
      prep.setInt(1, tournament);

      rs = prep.executeQuery();
      while (rs.next()) {
        final int pairId = rs.getInt(1);
        final String sideA = rs.getString(2);
        final String sideB = rs.getString(3);

        final boolean use = tableIdsForDivision.isEmpty()
            || tableIdsForDivision.contains(pairId);

        final TableInformation info = new TableInformation(pairId, sideA, sideB, use);
        tableInfo.add(info);
      }

    } finally {
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