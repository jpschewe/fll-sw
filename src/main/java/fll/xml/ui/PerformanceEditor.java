/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.util.Collection;

import fll.xml.PerformanceScoreCategory;

/**
 * Editor for {@link PerformanceScoreCategory} objects.
 */
public class PerformanceEditor extends ScoreCategoryEditor {

  private final TiebreakerEditor tiebreaker;

  private final RestrictionListEditor restrictions;

  /**
   * 
   * @param category the category to edit
   */
  public PerformanceEditor(final PerformanceScoreCategory category) {
    super(category);

    restrictions = new RestrictionListEditor(category);
    add(restrictions);

    tiebreaker = new TiebreakerEditor(category);
    add(tiebreaker);

  }

  @Override
  public void commitChanges() {
    super.commitChanges();
    tiebreaker.commitChanges();
    restrictions.commitChanges();
  }

  @Override
  protected void gatherValidityMessages(final Collection<String> messages) {
    super.gatherValidityMessages(messages);
  }

  @Override
  public boolean checkValidity(final Collection<String> messagesToDisplay) {
    boolean valid = super.checkValidity(messagesToDisplay);

    boolean localValid = restrictions.checkValidity(messagesToDisplay);
    valid &= localValid;

    localValid = tiebreaker.checkValidity(messagesToDisplay);
    valid &= localValid;

    return valid;
  }

}
