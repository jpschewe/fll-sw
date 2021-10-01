/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.awards;

/**
 * Store information about the head to head category.
 */
public final class HeadToHeadCategory implements AwardCategory {

  /**
   * Single instance.
   */
  public static final HeadToHeadCategory INSTANCE = new HeadToHeadCategory();

  /**
   * Display name for the pre-defined category.
   */
  public static final String AWARD_TITLE = "Head to Head";

  private HeadToHeadCategory() {
  }

  @Override
  public String getTitle() {
    return AWARD_TITLE;
  }

  @Override
  public boolean getPerAwardGroup() {
    return true;
  }

}
