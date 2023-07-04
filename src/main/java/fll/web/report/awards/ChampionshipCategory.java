/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.awards;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Store information about the championship.
 */
public final class ChampionshipCategory implements AwardCategory {

  /**
   * Single instance.
   */
  public static final ChampionshipCategory INSTANCE = new ChampionshipCategory();

  /**
   * Display name for the pre-defined Championship award.
   * This must match the value of CHAMPIONSHIP_NAME in finalist.js.
   */
  public static final String CHAMPIONSHIP_AWARD_TITLE = "Champion's";

  private ChampionshipCategory() {
  }

  /**
   * @return {@link #CHAMPIONSHIP_AWARD_TITLE}
   */
  @Override
  public String getTitle() {
    return CHAMPIONSHIP_AWARD_TITLE;
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
      final ChampionshipCategory other = (ChampionshipCategory) o;
      return getTitle().equals(other.getTitle())
          && getPerAwardGroup() == other.getPerAwardGroup();
    }
  }

}
