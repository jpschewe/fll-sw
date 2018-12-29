/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import fll.xml.Goal;
import fll.xml.RubricRange;

/**
 * Edit the rubric of a goal.
 */
public class RubricEditor extends JPanel {

  private final JComponent rangeList;

  private final Goal goal;

  private final List<RubricRangeEditor> rangeEditors = new LinkedList<>();

  public RubricEditor(final Goal goal) {
    super(new BorderLayout());
    this.goal = goal;

    final JPanel rangePanel = new JPanel(new BorderLayout());
    final Box buttonBox = Box.createHorizontalBox();
    rangePanel.add(buttonBox, BorderLayout.NORTH);

    final JButton addRange = new JButton("Add Rubric Range");
    buttonBox.add(addRange);
    addRange.addActionListener(l -> addNewRange());

    buttonBox.add(Box.createHorizontalGlue());

    rangeList = Box.createVerticalBox();
    rangePanel.add(rangeList, BorderLayout.CENTER);

    final MovableExpandablePanel rubricPanel = new MovableExpandablePanel("Rubric", rangePanel, false, false);
    add(rubricPanel, BorderLayout.CENTER);

    goal.getRubric().forEach(range -> {
      addRange(range);
    });

  }

  private void addNewRange() {
    final String title = String.format("Range %d", rangeEditors.size());
    final RubricRange newRange = new RubricRange(title);
    addRange(newRange);
  }

  private void addRange(final RubricRange range) {
    final RubricRangeEditor editor = new RubricRangeEditor(range);
    rangeEditors.add(editor);

    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(editor, BorderLayout.CENTER);

    final JButton delete = new JButton("Delete");
    panel.add(delete, BorderLayout.EAST);

    delete.addActionListener(e -> {
      GuiUtils.removeFromContainer(rangeList, panel);
      rangeEditors.remove(editor);
    });

    panel.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.BLACK));
    GuiUtils.addToContainer(rangeList, panel);
  }

  public void commitChanges() {
    final List<RubricRange> newRubric = new LinkedList<>();

    rangeEditors.forEach(editor -> {
      editor.commitChanges();
      final RubricRange range = editor.getRange();

      newRubric.add(range);
    });

    goal.setRubric(newRubric);
  }
}
