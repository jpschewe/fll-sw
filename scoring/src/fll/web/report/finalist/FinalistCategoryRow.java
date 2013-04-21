/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.finalist;


/**
 * Mirrors javascript class in schedule.js. Variable names need to match the
 * javascript/JSON.
 */
final class FinalistCategoryRow {
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "UWF_UNWRITTEN_FIELD" }, justification = "Populated by JSON deserialization")
  private String categoryName;

  public String getCategoryName() {
    return categoryName;
  }

  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = { "UWF_UNWRITTEN_FIELD" }, justification = "Populated by JSON deserialization")
  private boolean isPublic;

  public boolean isPublic() {
    return isPublic;
  }
  
}
