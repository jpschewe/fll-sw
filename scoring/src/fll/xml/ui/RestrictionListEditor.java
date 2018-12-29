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
import fll.xml.Restriction;

/**
 * Edit the list of {@link Restriction} objects on the
 * {@link PerformanceScoreCategory}.
 */
public class RestrictionListEditor extends JPanel {

  private final JComponent editorContainer;

  private final PerformanceScoreCategory performance;

  private final List<RestrictionEditor> editors = new LinkedList<>();

  public RestrictionListEditor(@Nonnull final PerformanceScoreCategory performance) {
    super(new BorderLayout());
    this.performance = performance;

    editorContainer = Box.createVerticalBox();

    // add some space on the left side of the expanded panel
    final JPanel expansion = new JPanel(new BorderLayout());

    final Box buttonBox = Box.createHorizontalBox();
    expansion.add(buttonBox, BorderLayout.NORTH);

    final JButton add = new JButton("Add Restriction");
    buttonBox.add(add);
    add.addActionListener(l -> addNewRestriction());

    buttonBox.add(Box.createHorizontalGlue());

    expansion.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
    expansion.add(editorContainer, BorderLayout.CENTER);

    final MovableExpandablePanel exPanel = new MovableExpandablePanel("Restrictions", expansion, false, false);
    add(exPanel, BorderLayout.CENTER);

    performance.getRestrictions().forEach(this::addRestriction);

  }

  private void addNewRestriction() {
    final Restriction test = new Restriction(performance);
    addRestriction(test);
  }

  private void addRestriction(final Restriction restriction) {
    final RestrictionEditor editor = new RestrictionEditor(restriction, performance);
    editors.add(editor);

    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(editor, BorderLayout.CENTER);

    final JButton delete = new JButton("Delete Restriction");
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
    final List<Restriction> newRestrictions = new LinkedList<>();

    editors.forEach(editor -> {
      final Restriction r = editor.getRestriction();
      newRestrictions.add(r);
    });

    performance.setRestrictions(newRestrictions);
  }

}
