/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.finalist;

import java.io.Serializable;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Class that represents a category in the finalist schedule.
 * Mirrors javascript class in fll-objects.js. Property names need to match the
 * javascript/JSON.
 */
/* package */ final class FinalistCategory implements Serializable {

  /**
   * @param categoryName name of the category
   * @param room the room that the category is being judged in
   */
  FinalistCategory(@JsonProperty("categoryName") final String categoryName,
                   @JsonProperty("room") final @Nullable String room) {
    this.categoryName = categoryName;
    this.room = room;
  }

  private final String categoryName;

  public String getCategoryName() {
    return categoryName;
  }

  private final @Nullable String room;

  public @Nullable String getRoom() {
    return room;
  }

}
