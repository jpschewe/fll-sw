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
   */
  public AwardWinner(@JsonProperty("name") final String name,
                     @JsonProperty("awardGroup") final String awardGroup,
                     @JsonProperty("teamNumber") final int teamNumber,
                     @JsonProperty("description") final @Nullable String description) {
    super(name, teamNumber, description);
    this.awardGroup = Objects.requireNonNull(awardGroup);
  }

  private final String awardGroup;

  /**
   * @return the award group
   */
  public final String getAwardGroup() {
    return awardGroup;
  }

}
