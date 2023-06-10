/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.developer.importdb.awardsScript;

/**
 * Possible actions to take on an awards script difference.
 */
public enum AwardsScriptDifferenceAction {

  KEEP_DESTINATION("Keep the value in the destination database"), // (write to source tournament),
  KEEP_SOURCE_AS_TOURNAMENT("Write the source value to the tournament layer in the destination database"), //
  KEEP_SOURCE_AS_TOURNAMENT_LEVEL(
      "Write the source value to the tournament level layer in the destination database - WILL EFFECT MULTIPLE TOURNAMENTS"), //
  KEEP_SOURCE_AS_SEASON(
      "Write the source value to the season layer in the destination database - WILL EFFECT MULTIPLE TOURNAMENTS"); //

  private final String description;

  /**
   * @return user friendly description of the action
   */
  public String getDescription() {
    return description;
  }

  /**
   * Calls {@link #name()}. Added so that JSPs can reference the name as a
   * property.
   * 
   * @return see {@link #name()}
   */
  public String getName() {
    return name();
  }

  AwardsScriptDifferenceAction(final String description) {
    this.description = description;
  }
}