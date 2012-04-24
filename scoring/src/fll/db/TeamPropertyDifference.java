/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

/**
 * Represents a different in a property for a team.
 */
public class TeamPropertyDifference {
  /**
   * Team string properties that can differ.
   */
  public enum TeamProperty {
    NAME, ORGANIZATION, DIVISION
  };

  public TeamPropertyDifference(final int teamNumber, final TeamProperty property, final String sourceValue, final String destValue) {
    this.teamNumber = teamNumber;
    this.property = property;
    this.sourceValue = sourceValue;
    this.destValue = destValue;
  }

  private final int teamNumber;

  public int getTeamNumber() {
    return teamNumber;
  }

  private final String sourceValue;

  public String getSourceValue() {
    return sourceValue;
  }

  private final String destValue;

  public String getDestValue() {
    return destValue;
  }

  private final TeamProperty property;

  public TeamProperty getProperty() {
    return property;
  }

}
