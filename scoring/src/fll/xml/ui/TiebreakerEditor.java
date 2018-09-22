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

import javax.annotation.Nonnull;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import fll.xml.PerformanceScoreCategory;
import fll.xml.TiebreakerTest;

/**
 * Edit the list of {@link TiebreakerTest} objects on the
 * {@link PerformanceScoreCategory}.
 */
public class TiebreakerEditor extends JPanel {

  private final JComponent editorContainer;

  private PerformanceScoreCategory performance;

  private final List<TiebreakerTestEditor> editors = new LinkedList<>();

  public TiebreakerEditor() {
    super(new BorderLayout());

    editorContainer = Box.createVerticalBox();

    // add some space on the left side of the expanded panel
    final JPanel expansion = new JPanel(new BorderLayout());

    final Box buttonBox = Box.createHorizontalBox();
    expansion.add(buttonBox, BorderLayout.NORTH);

    final JButton add = new JButton("Add Tiebreaker");
    buttonBox.add(add);
    add.addActionListener(l -> addNewTest());

    buttonBox.add(Box.createHorizontalGlue());

    expansion.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
    expansion.add(editorContainer, BorderLayout.CENTER);

    final MovableExpandablePanel exPanel = new MovableExpandablePanel("Tie breakers", expansion, false, false);
    add(exPanel, BorderLayout.CENTER);
  }

  private void addNewTest() {
    final TiebreakerTest test = new TiebreakerTest(performance);
    addTest(test);
  }

  private void addTest(final TiebreakerTest test) {
    final TiebreakerTestEditor editor = new TiebreakerTestEditor(test);
    editors.add(editor);

    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(editor, BorderLayout.CENTER);

    final JButton delete = new JButton("Delete Tiebreaker");
    panel.add(delete, BorderLayout.EAST);

    delete.addActionListener(e -> {
      editorContainer.remove(panel);
      editors.remove(editor);
      editorContainer.validate();
    });

    panel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLACK));
    editorContainer.add(panel);
  }

  public void commitChanges() {
    final List<TiebreakerTest> newTiebreaker = new LinkedList<>();

    editors.forEach(editor -> {
      final TiebreakerTest test = editor.getTest();
      newTiebreaker.add(test);
    });

    performance.setTiebreaker(newTiebreaker);
  }

  /**
   * @param v specify the new performance element to edit
   */
  public void setPerformance(@Nonnull final PerformanceScoreCategory v) {
    editorContainer.removeAll();
    editors.clear();

    this.performance = v;

    performance.getTiebreaker().forEach(test -> {
      addTest(test);
    });
  }

}
