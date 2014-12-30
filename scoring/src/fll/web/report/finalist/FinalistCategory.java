/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.finalist;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Class that represents a category in the finalist schedule.
 * Mirrors javascript class in fll-objects.js. Property names need to match the
 * javascript/JSON.
 */
final class FinalistCategory {

  public FinalistCategory(@JsonProperty("categoryName") final String categoryName,
                          @JsonProperty("isPublic") final boolean isPublic,
                          @JsonProperty("room") final String room) {
    this.categoryName = categoryName;
    this.isPublic = isPublic;
    this.room = room;
  }

  private final String categoryName;

  public String getCategoryName() {
    return categoryName;
  }

  private final boolean isPublic;

  public boolean getIsPublic() {
    return isPublic;
  }

  private final String room;

  public String getRoom() {
    return room;
  }

}
