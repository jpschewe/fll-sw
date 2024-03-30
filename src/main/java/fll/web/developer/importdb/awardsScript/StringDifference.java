/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.developer.importdb.awardsScript;

import java.sql.Connection;
import java.sql.SQLException;

import fll.Tournament;

/**
 * Base class for differences where the value is a string.
 */
/* package */ abstract class StringDifference extends AwardsScriptDifference {

  /**
   * @param sourceValue see {@link #getSourceValue()}
   * @param destValue see {@link #getDestValue()}
   */
  /* package */ StringDifference(final String sourceValue,
                                 final String destValue) {
    this.sourceValue = sourceValue;
    this.destValue = destValue;
  }

  /**
   * @return descriptive text for the description. See source for
   *         {@link #getDescription()} for details.
   */
  protected abstract String getTextDescription();

  @Override
  public final String getDescription() {
    final StringBuilder description = new StringBuilder();
    description.append(String.format("<div>The %s is different between the source database and the destination database.</div>",
                                     getTextDescription()));
    description.append(String.format("<div>Source: %s</div>", getSourceValue()));
    description.append(String.format("<div>Destination:%s </div>", getDestValue()));
    return description.toString();
  }

  private final String sourceValue;

  /**
   * @return value in the source database
   */
  public final String getSourceValue() {
    return sourceValue;
  }

  private final String destValue;

  /**
   * @return value in the destination database
   */
  public final String getDestValue() {
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
