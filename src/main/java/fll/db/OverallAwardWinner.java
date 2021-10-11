/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

import fll.xml.NonNumericCategory;
import fll.xml.SubjectiveScoreCategory;

/**
 * Represents the winner of an award. Needs to match the Javascript class in
 * js/fll-objects.js.
 */
public class OverallAwardWinner implements Serializable, Comparable<OverallAwardWinner> {

  /**
   * Used for JSON deserialization.
   */
  public static final class OverallAwardWinnerCollectionTypeInformation
      extends TypeReference<Collection<OverallAwardWinner>> {
    /** Single instance. */
    public static final OverallAwardWinnerCollectionTypeInformation INSTANCE = new OverallAwardWinnerCollectionTypeInformation();
  }

  /**
   * @param name see {@link #getName()}
   * @param teamNumber see {@link #getTeamNumber()}
   * @param description see {@link #getDescription()}
   * @param place {@link #getPlace()}
   */
  public OverallAwardWinner(@JsonProperty("name") final String name,
                            @JsonProperty("teamNumber") final int teamNumber,
                            @JsonProperty("description") final @Nullable String description,
                            @JsonProperty("place") final int place) {
    this.name = Objects.requireNonNull(name);
    this.teamNumber = teamNumber;
    this.description = description;
    this.place = place;
  }

  private final String name;

  /**
   * @return display name of the category
   * @see NonNumericCategory#getTitle()
   * @see SubjectiveScoreCategory#getTitle()
   */
  public final String getName() {
    return name;
  }

  private final int teamNumber;

  /**
   * @return the number of the team that is a winner
   */
  public final int getTeamNumber() {
    return teamNumber;
  }

  private final @Nullable String description;

  /**
   * If the award has a description, this is populated. This can be a description
   * of what the requirements of the award are or a description of what the team
   * did to receive the award.
   * 
   * @return the description of the award, may be null
   */
  public final @Nullable String getDescription() {
    return description;
  }

  private final int place;

  /**
   * @return the place for the team in the award
   */
  public final int getPlace() {
    return place;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName(), getTeamNumber());
  }

  @Override
  public boolean equals(final @Nullable Object o) {
    if (null == o) {
      return false;
    } else if (this == o) {
      return true;
    } else if (this.getClass().equals(o.getClass())) {
      final OverallAwardWinner other = (OverallAwardWinner) o;
      return this.getPlace() == other.getPlace() //
          && this.getTeamNumber() == other.getTeamNumber() //
          && this.getName().equals(other.getName());
    } else {
      return false;
    }
  }

  @Override
  public int compareTo(final OverallAwardWinner o) {
    final int nameCompare = getName().compareTo(o.getName());
    if (nameCompare == 0) {
      if (getPlace() < o.getPlace()) {
        return -1;
      } else if (getPlace() > o.getPlace()) {
        return 1;
      } else {
        return Integer.compare(getTeamNumber(), o.getTeamNumber());
      }
    } else {
      return nameCompare;
    }
  }

}
