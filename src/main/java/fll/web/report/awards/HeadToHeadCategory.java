/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.awards;

import org.checkerframework.checker.nullness.qual.Nullable;

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
  public static final String AWARD_TITLE = "Head to Head Brackets";

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

  @Override
  public int hashCode() {
    return getTitle().hashCode();
  }

  @Override
  public boolean equals(final @Nullable Object o) {
    if (null == o) {
      return false;
    } else if (this == o) {
      return true;
    } else if (!o.getClass().equals(this.getClass())) {
      return false;
    } else {
      final HeadToHeadCategory other = (HeadToHeadCategory) o;
      return getTitle().equals(other.getTitle())
          && getPerAwardGroup() == other.getPerAwardGroup();
    }
  }

  @Override
  public boolean isRanked() {
    return true;
  }

}
