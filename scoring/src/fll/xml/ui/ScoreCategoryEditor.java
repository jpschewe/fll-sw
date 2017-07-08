/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import fll.util.FormatterUtils;
import fll.xml.AbstractGoal;
import fll.xml.ScoreCategory;

/**
 * Editor for {@link ScoreCategory} objects.
 */
public class ScoreCategoryEditor extends JPanel {

  public ScoreCategoryEditor(final ScoreCategory category) {
    setLayout(new GridBagLayout());

    GridBagConstraints gbc;

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.LINE_END;
    add(new JLabel("weight: "), gbc);

    final JFormattedTextField weight = FormatterUtils.createDoubleField();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(weight, gbc);

    weight.setValue(category.getWeight());
    weight.addPropertyChangeListener("value", e -> {
      final double newWeight = ((Number) weight.getValue()).doubleValue();
      category.setWeight(newWeight);
    });

    for (final AbstractGoal goal : category.getGoals()) {

      final AbstractGoalEditor editor = new AbstractGoalEditor(goal);      
      final MovableExpandablePanel panel = new MovableExpandablePanel(goal.getTitle(), editor, true);
      editor.addPropertyChangeListener("title", e -> {
        final String newTitle = (String)e.getNewValue();
        panel.setTitle(newTitle);
      });

      gbc = new GridBagConstraints();
      gbc.weightx = 1;
      gbc.anchor = GridBagConstraints.LINE_START;
      gbc.gridwidth = GridBagConstraints.REMAINDER;

      add(panel, gbc);
    }
  }

}
