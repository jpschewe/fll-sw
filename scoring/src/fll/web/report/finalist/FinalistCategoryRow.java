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
  private String categoryName;

  public String getCategoryName() {
    return categoryName;
  }

  private boolean isPublic;

  public boolean isPublic() {
    return isPublic;
  }
  
}
