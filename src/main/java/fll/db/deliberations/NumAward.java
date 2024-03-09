/*
 * Copyright (c) 2024 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db.deliberations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;

import com.fasterxml.jackson.annotation.JsonProperty;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import fll.Tournament;

/**
 * Number of awards for a category.
 */
public final class NumAward extends CommonBase {
  /**
   * @param awardGroup {@link CommonBase#getAwardGroup()}
   * @param categoryName {@link CommonBase#getCategoryName()}
   * @param numAwards {@link #getNumAwards()}
   */
  public NumAward(@JsonProperty("awardGroup") final String awardGroup,
                  @JsonProperty("categoryName") final String categoryName,
                  @JsonProperty("numAwards") final int numAwards) {
    super(awardGroup, categoryName);
    this.numAwards = numAwards;
  }

  private final int numAwards;

  /**
   * @return number of awards for the category
   */
  public int getNumAwards() {
    return numAwards;
  }

  /**
   * @param connection the database connection
   * @param tournament the tournament
   * @return the deliberation num awards for the tournament
   * @throws SQLException on a database error
   */
  public static Collection<NumAward> getNumAwards(final Connection connection,
                                                  final Tournament tournament)
      throws SQLException {
    final Collection<NumAward> result = new LinkedList<>();
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT award_group, category_name, num_awards FROM deliberation_num_awards WHERE tournament_id = ?")) {
      prep.setInt(1, tournament.getTournamentID());
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String awardGroup = castNonNull(rs.getString(1));
          final String categoryName = castNonNull(rs.getString(2));
          final int numAwards = rs.getInt(3);

          final NumAward v = new NumAward(awardGroup, categoryName, numAwards);
          result.add(v);
        }
      }
    }
    return result;
  }

  /**
   * @param connection database connection
   * @param tournament tournament to update
   * @param numAwards new data
   * @throws SQLException on a database error
   */
  public static void setNumAwards(final Connection connection,
                                  final Tournament tournament,
                                  final Collection<NumAward> numAwards)
      throws SQLException {
    try (
        PreparedStatement delete = connection.prepareStatement("DELETE FROM deliberation_num_awards WHERE tournament_id = ?");
        PreparedStatement insert = connection.prepareStatement("INSERT INTO deliberation_num_awards (tournament_id, award_group, category_name, num_awards) VALUES(?, ?, ?, ?)")) {
      delete.setInt(1, tournament.getTournamentID());
      delete.executeUpdate();

      insert.setInt(1, tournament.getTournamentID());
      for (final NumAward v : numAwards) {
        insert.setString(2, v.getAwardGroup());
        insert.setString(3, v.getCategoryName());
        insert.setInt(4, v.getNumAwards());
        insert.executeUpdate();
      }
    }
  }

}