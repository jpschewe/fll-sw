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
  public abstract String getFieldDescription();

  @Override
  public final String getDescription() {
    final StringBuilder description = new StringBuilder();
    description.append(String.format("<div>The %s for category %s is different between the source database and the destination database.</div>",
                                     getFieldDescription(), getCategory().getTitle()));
    description.append(String.format("<div>Source: %s</div>", getSourceValue()));
    description.append(String.format("<div>Destination:%s </div>", getDestValue()));
    return description.toString();
  }

  @Override
  public abstract void resolveDifference(Connection sourceConnection,
                                         Tournament sourceTournament,
                                         Connection destConnection,
                                         Tournament destTournament,
                                         AwardsScriptDifferenceAction action)
      throws SQLException;
}
