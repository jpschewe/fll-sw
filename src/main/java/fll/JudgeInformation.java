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

  private final boolean finalScores;

  /**
   * @return true if the scores for this judge are final
   */
  public boolean isFinalScores() {
    return finalScores;
  }

  /**
   * @param id {@link #getId()}
   * @param category {@link #getCategory()}
   * @param group {@link #getGroup()}
   * @param finalScores {@link #isFinalScores()}
   */
  @JsonCreator
  public JudgeInformation(@JsonProperty("id") final String id,
                          @JsonProperty("category") final String category,
                          @JsonProperty("group") final String group,
                          @JsonProperty("finalScores") final boolean finalScores) {
    this.id = id;
    this.category = category;
    this.group = group;
    this.finalScores = finalScores;
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
    final Collection<JudgeInformation> judges = new LinkedList<JudgeInformation>();

    try (
        PreparedStatement stmt = connection.prepareStatement("SELECT id, category, station, final_scores FROM Judges WHERE Tournament = ?")) {
      stmt.setInt(1, tournament);
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          final String id = castNonNull(rs.getString(1));
          final String category = castNonNull(rs.getString(2));
          final String station = castNonNull(rs.getString(3));
          final boolean finalScores = rs.getBoolean(4);
          final JudgeInformation judge = new JudgeInformation(id, category, station, finalScores);
          judges.add(judge);
        }
      }
    }

    return judges;
  }
}