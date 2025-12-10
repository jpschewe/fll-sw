/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.text.ParseException;
import java.util.Collection;

import javax.swing.Box;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;

import fll.scores.PerformanceTeamScore;
import fll.util.FormatterUtils;
import fll.xml.PerformanceScoreCategory;

/**
 * Editor for {@link PerformanceScoreCategory} objects.
 */
public class PerformanceEditor extends ScoreCategoryEditor<PerformanceTeamScore> {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private final TiebreakerEditor tiebreaker;

  private final RestrictionListEditor restrictions;

  private final JFormattedTextField maximumScore;

  /**
   * @param category the category to edit
   */
  public PerformanceEditor(final PerformanceScoreCategory category) {
    super(category);

    final Box maximumScoreContainer = Box.createHorizontalBox();
    add(maximumScoreContainer);

    maximumScoreContainer.add(new JLabel("Maximum Score: "));

    maximumScore = FormatterUtils.createDoubleField();
    maximumScoreContainer.add(maximumScore);
    maximumScoreContainer.add(Box.createHorizontalGlue());

    restrictions = new RestrictionListEditor(category);
    add(restrictions);

    tiebreaker = new TiebreakerEditor(category);
    add(tiebreaker);

    // object is initialized
    maximumScore.addPropertyChangeListener("value", e -> {
      final Number value = (Number) maximumScore.getValue();
      if (null != value) {
        final double newMaximumScore = value.doubleValue();
        category.setMaximumScore(newMaximumScore);
      }
    });

    maximumScore.setValue(category.getMaximumScore());

  }

  @Override
  public void commitChanges() {
    super.commitChanges();

    try {
      maximumScore.commitEdit();
    } catch (final ParseException e) {
      LOGGER.debug("Got parse exception committing changes, assuming bad value and ignoring", e);
    }

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
