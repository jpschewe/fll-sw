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

/**
 * Represents the winner of an award in an award group. Needs to match the
 * Javascript class in
 * js/fll-objects.js.
 */
public class AwardWinner extends OverallAwardWinner implements Serializable {

  /**
   * Used for JSON deserialization.
   */
  public static final class AwardWinnerCollectionTypeInformation extends TypeReference<Collection<AwardWinner>> {
    /** single instance. */
    public static final AwardWinnerCollectionTypeInformation INSTANCE = new AwardWinnerCollectionTypeInformation();
  }

  /**
   * @param name see {@link #getName()}
   * @param awardGroup see {@link #getAwardGroup()}
   * @param teamNumber see {@link #getTeamNumber()}
   * @param description see {@link #getDescription()}
   * @param place {@link #getPlace}
   */
  public AwardWinner(@JsonProperty("name") final String name,
                     @JsonProperty("awardGroup") final String awardGroup,
                     @JsonProperty("teamNumber") final int teamNumber,
                     @JsonProperty("description") final @Nullable String description,
                     @JsonProperty("place") final int place) {
    super(name, teamNumber, description, place);
    this.awardGroup = Objects.requireNonNull(awardGroup);
  }

  private final String awardGroup;

  /**
   * @return the award group
   */
  public final String getAwardGroup() {
    return awardGroup;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName(), getTeamNumber(), getAwardGroup());
  }

  @Override
  public boolean equals(final @Nullable Object o) {
    if (null == o) {
      return false;
    } else if (this == o) {
      return true;
    } else if (this.getClass().equals(o.getClass())) {
      final AwardWinner other = (AwardWinner) o;
      return this.getPlace() == other.getPlace() //
          && this.getTeamNumber() == other.getTeamNumber() //
          && this.getName().equals(other.getName()) //
          && this.getAwardGroup().equals(other.getAwardGroup());
    } else {
      return false;
    }
  }

}
