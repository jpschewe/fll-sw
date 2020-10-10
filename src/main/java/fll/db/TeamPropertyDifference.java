/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

import java.io.Serializable;

/**
 * Represents a different in a property for a team.
 */
public class TeamPropertyDifference implements Serializable {
  /**
   * Team string properties that can differ.
   */
  public enum TeamProperty {
    /**
     * The name is different.
     */
    NAME,
    /** The organization is different. */
    ORGANIZATION
  };

  /**
   * @param teamNumber {@link #getTeamNumber()}
   * @param property {@link #getProperty()}
   * @param sourceValue {@link #getSourceValue()}
   * @param destValue {@link #getDestValue()}
   */
  public TeamPropertyDifference(final int teamNumber,
                                final TeamProperty property,
                                final String sourceValue,
                                final String destValue) {
    this.teamNumber = teamNumber;
    this.property = property;
    this.sourceValue = sourceValue;
    this.destValue = destValue;
  }

  private final int teamNumber;

  /**
   * @return team that the difference applies to
   */
  public int getTeamNumber() {
    return teamNumber;
  }

  private final String sourceValue;

  /**
   * @return value in the source database
   */
  public String getSourceValue() {
    return sourceValue;
  }

  private final String destValue;

  /**
   * @return value in the destination database
   */
  public String getDestValue() {
    return destValue;
  }

  private final TeamProperty property;

  /**
   * @return what property is different
   */
  public TeamProperty getProperty() {
    return property;
  }

}
