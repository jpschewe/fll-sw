/*
 * Copyright (c) 2024 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.web.report.awards.AwardCategory;
import fll.web.report.awards.ChampionshipCategory;
import fll.web.report.awards.HeadToHeadCategory;
import fll.xml.ChallengeDescription;
import fll.xml.NonNumericCategory;
import fll.xml.PerformanceScoreCategory;
import fll.xml.SubjectiveScoreCategory;
import fll.xml.VirtualSubjectiveScoreCategory;

/**
 * Load and save the award determination order in the database.
 */
public final class AwardDeterminationOrder {

  private AwardDeterminationOrder() {
  }

  /**
   * @param connection the database
   * @param description used to ensure that all awards are returned, even if not
   *          stored in the database
   * @return the order that awards are determined
   * @throws SQLException on a database error
   */
  public static List<AwardCategory> get(final Connection connection,
                                        final ChallengeDescription description)
      throws SQLException {
    final List<AwardCategory> order = new LinkedList<>();

    try (Statement stmt = connection.createStatement()) {
      try (
          ResultSet rs = stmt.executeQuery("SELECT award FROM award_determination_order ORDER BY determined_order ASC")) {
        while (rs.next()) {
          final String award = castNonNull(rs.getString(1));
          order.add(description.getCategoryByTitle(award));
        }
      }
    }

    // ensure that all categories are added.
    if (!order.contains(ChampionshipCategory.INSTANCE)) {
      order.add(ChampionshipCategory.INSTANCE);
    }
    final PerformanceScoreCategory performance = description.getPerformance();
    if (!order.contains(performance)) {
      order.add(performance);
    }
    for (final VirtualSubjectiveScoreCategory cat : description.getVirtualSubjectiveCategories()) {
      if (!order.contains(cat)) {
        order.add(cat);
      }
    }
    for (final SubjectiveScoreCategory cat : description.getSubjectiveCategories()) {
      if (!order.contains(cat)) {
        order.add(cat);
      }
    }
    for (final NonNumericCategory cat : description.getNonNumericCategories()) {
      // add all categories, handle ignored categories on output
      if (!order.contains(cat)) {
        order.add(cat);
      }
    }
    if (!order.contains(HeadToHeadCategory.INSTANCE)) {
      order.add(HeadToHeadCategory.INSTANCE);
    }
    return order;
  }

  /**
   * @param connection where to store the data
   * @param awardDeterminationOrder the data to store
   * @throws SQLException on a database error
   */
  public static void save(final Connection connection,
                          final List<AwardCategory> awardDeterminationOrder)
      throws SQLException {
    final boolean autoCommit = connection.getAutoCommit();
    try {
      connection.setAutoCommit(false);
      try (Statement stmt = connection.createStatement()) {
        stmt.executeUpdate("DELETE FROM award_determination_order");
      }

      try (
          PreparedStatement prep = connection.prepareStatement("INSERT INTO award_determination_order (award, determined_order) VALUES(?, ?)")) {
        int index = 0;
        for (final AwardCategory award : awardDeterminationOrder) {
          ++index;
          prep.setString(1, award.getTitle());
          prep.setInt(2, index);
          prep.executeUpdate();
        }
      }

      connection.commit();
    } finally {
      connection.setAutoCommit(autoCommit);
    }
  }

  /**
   * @param connection the database to check
   * @return if there is data stored in the database
   * @throws SQLException on a database error
   */
  public static boolean dataExists(final Connection connection) throws SQLException {
    try (Statement stmt = connection.createStatement()) {
      try (
          ResultSet rs = stmt.executeQuery("SELECT award FROM award_determination_order ORDER BY determined_order ASC")) {
        return rs.next();
      }
    }
  }
}
