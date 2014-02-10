/*
 * Copyright (c) 2013 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.report.finalist;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Mirrors javascript class in schedule.js. Variable names need to match the
 * javascript/JSON.
 */
final class FinalistCategoryRow {
  @SuppressFBWarnings(value = { "UWF_UNWRITTEN_FIELD" }, justification = "Populated by JSON deserialization")
  private String categoryName;

  public String getCategoryName() {
    return categoryName;
  }

  @SuppressFBWarnings(value = { "UWF_UNWRITTEN_FIELD" }, justification = "Populated by JSON deserialization")
  private boolean isPublic;

  public boolean isPublic() {
    return isPublic;
  }

}
