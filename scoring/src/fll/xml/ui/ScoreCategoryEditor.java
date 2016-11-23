/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import fll.util.LogUtils;
import fll.xml.AbstractGoal;
import fll.xml.ScoreCategory;

/**
 * Editor for {@link ScoreCategory} objects.
 */
public class ScoreCategoryEditor extends JPanel {

  private static final Logger LOGGER = LogUtils.getLogger();
  
  public ScoreCategoryEditor(final ScoreCategory category) {
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
         
    for (final AbstractGoal goal : category.getGoals()) {
      final AbstractGoalEditor editor = new AbstractGoalEditor(goal);
      add(editor);
    }
  }

}
