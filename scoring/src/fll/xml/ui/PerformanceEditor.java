/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.GridBagConstraints;

import fll.xml.PerformanceScoreCategory;
import fll.xml.ScoreCategory;

/**
 * Editor for {@link PerformanceScoreCategory} objects.
 */
public class PerformanceEditor extends ScoreCategoryEditor {

  private final TiebreakerEditor tiebreaker;
  
  public PerformanceEditor() {
    super();
    
    GridBagConstraints gbc;

    // FIXME restrictions

    tiebreaker = new TiebreakerEditor();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.weighty = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.BOTH;
    add(tiebreaker, gbc);
    
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
    
    this.tiebreaker.setPerformance((PerformanceScoreCategory)v);
  }
  
}
