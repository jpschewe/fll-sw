/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.developer.importdb.awardsScript;

import java.sql.Connection;
import java.sql.SQLException;

import fll.Tournament;
import fll.web.report.awards.AwardCategory;

/**
 * Base class for string differences for a category
 */
abstract class CategoryStringDifference extends StringDifference {

  /**
   * @param sourceValue
   * @param destValue
   */
  /* package */ CategoryStringDifference(final AwardCategory category,
                                         final String sourceValue,
                                         final String destValue) {
    super(sourceValue, destValue);
    this.category = category;
  }

  private final AwardCategory category;

  /**
   * @return the category that has a different value
   */
  public AwardCategory getCategory() {
    return category;
  }

  /**
   * @return description of the field that has a difference
   */
  protected abstract String getFieldDescription();

  protected final String getTextDescription() {
    return String.format("%s for category %s", getFieldDescription(), getCategory().getTitle());
  }

  @Override
  public abstract void resolveDifference(Connection sourceConnection,
                                         Tournament sourceTournament,
                                         Connection destConnection,
                                         Tournament destTournament,
                                         AwardsScriptDifferenceAction action)
      throws SQLException;
}
