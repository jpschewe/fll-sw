/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import fll.xml.AbstractGoal;

/**
 * Editor for {@link AbstractGoal} objects.
 */
/* package */ class AbstractGoalEditor extends JPanel {

  public AbstractGoalEditor(final AbstractGoal goal) {
    super(new FlowLayout());
    mGoal = goal;

    add(new DragHandle(this));

    final JLabel title = new JLabel(goal.getTitle());
    add(title);

  }

  private final AbstractGoal mGoal;

  public AbstractGoal getGoal() {
    return mGoal;
  }
  
}
