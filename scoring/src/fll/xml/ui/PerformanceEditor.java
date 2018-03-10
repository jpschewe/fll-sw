/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import fll.xml.PerformanceScoreCategory;
import fll.xml.ScoreCategory;

/**
 * Editor for {@link PerformanceScoreCategory} objects.
 */
public class PerformanceEditor extends ScoreCategoryEditor {

  private final TiebreakerEditor tiebreaker;

  private final RestrictionListEditor restrictions;

  public PerformanceEditor() {
    super();

    restrictions = new RestrictionListEditor();
    add(restrictions);

    tiebreaker = new TiebreakerEditor();
    add(tiebreaker);

  }

  /**
   * @param v must be a {@link PerformanceScoreCategory}
   * @throws IllegalArgumentException if not an instance of
   *           {@link PerformanceScoreCategory}.
   */
  @Override
  public void setCategory(final ScoreCategory v) {
    if (!(v instanceof PerformanceScoreCategory)) {
      throw new IllegalArgumentException("Can only edit PerformanceScoreCategory objects");
    }

    super.setCategory(v);

    final PerformanceScoreCategory performance = (PerformanceScoreCategory) v;

    tiebreaker.setPerformance(performance);
    restrictions.setPerformance(performance);
  }

  @Override
  public void commitChanges() {
    super.commitChanges();
    tiebreaker.commitChanges();
    restrictions.commitChanges();
  }
}
