/*
 * Copyright (c) 2019 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Represents the winner of an award. Needs to match the Javascript class in
 * js/fll-objects.js.
 */
public class OverallAwardWinner implements Serializable {

  /**
   * Used for JSON deserialization.
   */
  public static final class OverallAwardWinnerCollectionTypeInformation
      extends TypeReference<Collection<OverallAwardWinner>> {
    public static final OverallAwardWinnerCollectionTypeInformation INSTANCE = new OverallAwardWinnerCollectionTypeInformation();
  }

  /**
   * @param name see {@link #getName()}
   * @param teamNumber see {@link #getTeamNumber()}
   * @param description see {@link #getDescription()}
   */
  public OverallAwardWinner(@JsonProperty("name") final String name,
                            @JsonProperty("teamNumber") final int teamNumber,
                            @JsonProperty("description") final String description) {
    this.name = Objects.requireNonNull(name);
    this.teamNumber = teamNumber;
    this.description = Objects.requireNonNull(description);
  }

  private final String name;

  /**
   * For scored categories this is
   * {@link fll.xml.SubjectiveScoreCategory#getName()} and
   * needs to be converted to a displayed title through the challenge description.
   * For the extra awards, this is the display name.
   * 
   * @return name of the category
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

  private final String description;

  /**
   * If the award has a description, this is populated. This can be a description
   * of what the requirements of the award are or a description of what the team
   * did to receive the award.
   * 
   * @return the description of the award, may be null
   */
  public final String getDescription() {
    return description;
  }

}
