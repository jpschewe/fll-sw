/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import net.mtu.eggplant.util.sql.SQLFunctions;

/**
 * Judge information.
 */
public final class JudgeInformation implements Serializable {
  private final String id;

  /**
   * Identifiers for judges must be unique within a
   * category, station combination.
   * 
   * @return The identifier of the judge
   */
  public String getId() {
    return id;
  }

  private final String category;

  /**
   * The name of the scoring category that this judge is scoring. This needs to
   * match a category name in the challenge description.
   * 
   * @return the name of the scoring category
   */
  public String getCategory() {
    return category;
  }

  private final String group;

  /**
   * @return The judging group the judge is at.
   */
  public String getGroup() {
    return group;
  }

  /**
   * @param id {@link #getId()}
   * @param category {@link #getCategory()}
   * @param group {@link #getGroup()}
   */
  @JsonCreator
  public JudgeInformation(@JsonProperty("id") final String id,
                          @JsonProperty("category") final String category,
                          @JsonProperty("group") final String group) {
    this.id = id;
    this.category = category;
    this.group = group;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  @EnsuresNonNullIf(expression = "#1", result = true)
  public boolean equals(final @Nullable Object o) {
    if (null == o) {
      return false;
    } else if (o == this) {
      return true;
    } else if (o.getClass().equals(JudgeInformation.class)) {
      final JudgeInformation other = (JudgeInformation) o;
      return getId().equals(other.getId())
          && getCategory().equals(other.getCategory())
          && getGroup().equals(other.getGroup());
    } else {
      return false;
    }
  }

  /**
   * Get all judges stored for this tournament.
   * 
   * @param connection the database
   * @param tournament tournament ID
   * @return the judges
   * @throws SQLException when there is an error talking to the database
   */
  public static Collection<JudgeInformation> getJudges(final Connection connection,
                                                       final int tournament)
      throws SQLException {
    Collection<JudgeInformation> judges = new LinkedList<JudgeInformation>();

    ResultSet rs = null;
    PreparedStatement stmt = null;
    try {
      stmt = connection.prepareStatement("SELECT id, category, station FROM Judges WHERE Tournament = ?");
      stmt.setInt(1, tournament);
      rs = stmt.executeQuery();
      while (rs.next()) {
        final String id = castNonNull(rs.getString(1));
        final String category = castNonNull(rs.getString(2));
        final String station = castNonNull(rs.getString(3));
        final JudgeInformation judge = new JudgeInformation(id, category, station);
        judges.add(judge);
      }
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
    }

    return judges;
  }
}