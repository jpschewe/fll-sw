/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.playoff;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Basic information for a head to head bracket
 */
public class BracketInfo {

  public BracketInfo(@JsonProperty("bracketName") final String bracketName,
                     @JsonProperty("firstRound") final int firstRound,
                     @JsonProperty("lastRound") final int lastRound) {
    this.bracketName = bracketName;
    this.firstRound = firstRound;
    this.lastRound = lastRound;
  }

  private int firstRound;

  public final int getFirstRound() {
    return firstRound;
  }

  private int lastRound;

  public final int getLastRound() {
    return lastRound;
  }

  private String bracketName;

  public final String getBracketName() {
    return bracketName;
  }

}