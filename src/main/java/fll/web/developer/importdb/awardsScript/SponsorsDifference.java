/*
 * Copyright (c) 2023 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.developer.importdb.awardsScript;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Difference in the sponsors list between 2 databases.
 */
public final class SponsorsDifference extends AwardsScriptDifference {

  /**
   * @param sourceValue see {@link #getSourceSponsors()}
   * @param destValue see {@link #getDestSponsors()}
   */
  public SponsorsDifference(final List<String> sourceValue,
                            final List<String> destValue) {
    this.sourceValue = Collections.unmodifiableList(new ArrayList<>(sourceValue));
    this.destValue = Collections.unmodifiableList(new ArrayList<>(destValue));
  }

  @Override
  public String getDescription() {
    final StringBuilder description = new StringBuilder();
    description.append("<div>The list of sponsors is different between the source database and the destination database.</div>");
    description.append("<div>Source:<ul>");
    for (final String svalue : sourceValue) {
      description.append("<li>");
      description.append(svalue);
      description.append("</li>");
    }
    description.append("</ul></div>");
    description.append("<div>Destination:<ul>");
    for (final String dvalue : destValue) {
      description.append("<li>");
      description.append(dvalue);
      description.append("</li>");
    }
    description.append("</ul></div>");
    return description.toString();
  }

  private final List<String> sourceValue;

  /**
   * @return sponsors in the source database (unmodifiable)
   */
  public List<String> getSourceSponsors() {
    return sourceValue;
  }

  private final List<String> destValue;

  /**
   * @return sponsors in the destination database (unmodifiable)
   */
  public List<String> getDestSponsors() {
    return destValue;
  }
}
