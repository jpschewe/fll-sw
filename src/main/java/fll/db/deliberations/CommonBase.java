/*
 * Copyright (c) 2024 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db.deliberations;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data common to all classes for deliberations.
 */
public class CommonBase {

  /**
   * @param awardGroup {@link #getAwardGroup()}
   * @param categoryName {@link #getCategoryname()}
   */
  public CommonBase(@JsonProperty("awardGroup") final String awardGroup,
                    @JsonProperty("categoryName") final String categoryName) {
    this.awardGroup = awardGroup;
    this.categoryName = categoryName;
  }

  private final String awardGroup;

  /**
   * @return award group that the writer belongs to
   */
  public String getAwardGroup() {
    return awardGroup;
  }

  private final String categoryName;

  /**
   * @return the category name for the writer
   * @see OverallAwardWinner#getName()
   */
  public String getCategoryName() {
    return categoryName;
  }

}