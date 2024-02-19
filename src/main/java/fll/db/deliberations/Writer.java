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
 * Writer for a deliberation category.
 */
public final class Writer extends CommonBase {
  /**
   * @param awardGroup {@link CommonBase#getAwardGroup()}
   * @param categoryName {@link CommmonBase#getCategoryName()}
   * @param number {@link #getNumber()}
   * @param name {@link #getName()}
   */
  public Writer(@JsonProperty("awardGroup") final String awardGroup,
                @JsonProperty("categoryName") final String categoryName,
                @JsonProperty("number") final int number,
                @JsonProperty("name") final String name) {
    super(awardGroup, categoryName);
    this.number = number;
    this.name = name;
  }

  private final int number;

  /**
   * @return writer number
   */
  public int getNumber() {
    return number;
  }

  private final String name;

  /**
   * @return name of the writer
   */
  public String getName() {
    return name;
  }

  /**
   * @param connection the database connection
   * @param tournament the tournament
   * @return the deliberation writers for the tournament
   * @throws SQLException on a database error
   */
  public static Collection<Writer> getWriters(final Connection connection,
                                              final Tournament tournament)
      throws SQLException {
    final Collection<Writer> result = new LinkedList<>();
    try (
        PreparedStatement prep = connection.prepareStatement("SELECT award_group, category_name, writer_number, writer_name FROM deliberation_writers WHERE tournament_id = ?")) {
      prep.setInt(1, tournament.getTournamentID());
      try (ResultSet rs = prep.executeQuery()) {
        while (rs.next()) {
          final String awardGroup = castNonNull(rs.getString(1));
          final String categoryName = castNonNull(rs.getString(2));
          final int number = rs.getInt(3);
          final String name = castNonNull(rs.getString(4));

          final Writer writer = new Writer(awardGroup, categoryName, number, name);
          result.add(writer);
        }
      }
    }
    return result;
  }

  /**
   * @param connection database connection
   * @param tournament tournament to update
   * @param writers new data
   * @throws SQLException on a database error
   */
  public static void setWriters(final Connection connection,
                                final Tournament tournament,
                                final Collection<Writer> writers)
      throws SQLException {
    try (
        PreparedStatement delete = connection.prepareStatement("DELETE FROM deliberation_writers WHERE tournament_id = ?");
        PreparedStatement insert = connection.prepareStatement("INSERT INTO deliberation_writers (tournament_id, award_group, category_name, writer_number, writer_name) VALUES(?, ?, ?, ?, ?)")) {
      delete.setInt(1, tournament.getTournamentID());
      delete.executeUpdate();

      insert.setInt(1, tournament.getTournamentID());
      for (final Writer writer : writers) {
        insert.setString(2, writer.getAwardGroup());
        insert.setString(3, writer.getCategoryName());
        insert.setInt(4, writer.getNumber());
        insert.setString(5, writer.getName());
        insert.executeUpdate();
      }
    }
  }
}