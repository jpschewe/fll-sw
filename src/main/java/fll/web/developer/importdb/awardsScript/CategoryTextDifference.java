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
 * Difference in the text for a category between 2 databases.
 */
public abstract class CategoryTextDifference extends AwardsScriptDifference {

  /**
   * @param category see {@link #getCategory()}
   * @param sourceValue see {@link #getSourceValue()}
   * @param destValue see {@link #getDestValue()}
   */
  public CategoryTextDifference(final AwardCategory category,
                                final String sourceValue,
                                final String destValue) {
    this.category = category;
    this.sourceValue = sourceValue;
    this.destValue = destValue;
  }

  @Override
  public String getDescription() {
    final StringBuilder description = new StringBuilder();
    description.append(String.format("<div>The text for category %s is different between the source database and the destination database.</div>",
                                     category.getTitle()));
    description.append(String.format("<div>Source: %s</div>", sourceValue));
    description.append(String.format("<div>Destination:%s </div>", destValue));
    return description.toString();
  }

  private final AwardCategory category;

  /**
   * @return the category that has a different value
   */
  public AwardCategory getCategory() {
    return category;
  }

  private final String sourceValue;

  /**
   * @return macro value in the source database
   */
  public String getSourceValue() {
    return sourceValue;
  }

  private final String destValue;

  /**
   * @return macro value in the destination database
   */
  public String getDestValue() {
    return destValue;
  }

  @Override
  public abstract void resolveDifference(Connection sourceConnection,
                                         Tournament sourceTournament,
                                         Connection destConnection,
                                         Tournament destTournament,
                                         AwardsScriptDifferenceAction action)
      throws SQLException;
}
