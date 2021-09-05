/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml;

/**
 * Champtionship category. There isn't much to this class other than properties
 * to make it behave like standard categories.
 */
public final class ChampionshipCategory implements Category {

  /**
   * Single instance.
   */
  public static final ChampionshipCategory INSTANCE = new ChampionshipCategory();

  /**
   * Display name for the pre-defined Championship award.
   * This must match the value of CHAMPIONSHIP_NAME in finalist.js.
   */
  public static final String CHAMPIONSHIP_AWARD_TITLE = "Championship";

  private ChampionshipCategory() {
  }

  /**
   * @return {@link #CHAMPIONSHIP_AWARD_TITLE}
   */
  @Override
  public String getTitle() {
    return CHAMPIONSHIP_AWARD_TITLE;
  }
  
}
