/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;

import fll.xml.PerformanceScoreCategory;
import fll.xml.Restriction;

/**
 * Edit the list of {@link Restriction} objects on the
 * {@link PerformanceScoreCategory}.
 */
public class RestrictionListEditor extends JPanel implements Validatable {

  private final JComponent editorContainer;

  private final PerformanceScoreCategory performance;

  private final List<RestrictionEditor> editors = new LinkedList<>();

  /**
   * @param performance where to get the restrictions to edit
   */
  public RestrictionListEditor(final PerformanceScoreCategory performance) {
    super(new BorderLayout());
    this.performance = performance;

    editorContainer = Box.createVerticalBox();

    // add some space on the left side of the expanded panel
    final JPanel expansion = new JPanel(new BorderLayout());

    final Box buttonBox = Box.createHorizontalBox();
    expansion.add(buttonBox, BorderLayout.NORTH);

    final JButton add = new JButton("Add Restriction");
    buttonBox.add(add);

    buttonBox.add(Box.createHorizontalGlue());

    expansion.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
    expansion.add(editorContainer, BorderLayout.CENTER);

    final MovableExpandablePanel exPanel = new MovableExpandablePanel("Restrictions", expansion, false, false);
    add(exPanel, BorderLayout.CENTER);

    // object initialized
    add.addActionListener(l -> addNewRestriction());

    performance.getRestrictions().forEach(this::addRestriction);

  }

  private void addNewRestriction(@UnknownInitialization(RestrictionListEditor.class) RestrictionListEditor this) {
    final Restriction test = new Restriction();
    addRestriction(test);
  }

  private void addRestriction(@UnknownInitialization(RestrictionListEditor.class) RestrictionListEditor this,
                              final Restriction restriction) {
    final RestrictionEditor editor = new RestrictionEditor(restriction, performance);
    editors.add(editor);

    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(editor, BorderLayout.CENTER);

    final JButton delete = new JButton("Delete Restriction");
    panel.add(delete, BorderLayout.EAST);

    delete.addActionListener(e -> {
      editorContainer.remove(panel);
      GuiUtils.removeFromContainer(editorContainer, editor);
      editors.remove(editor);
    });

    panel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.BLACK));
    GuiUtils.addToContainer(editorContainer, panel);
  }

  /**
   * Commit any changes to the restrictions.
   */
  public void commitChanges() {
    final List<Restriction> newRestrictions = new LinkedList<>();

    editors.forEach(editor -> {
      final Restriction r = editor.getRestriction();
      newRestrictions.add(r);
    });

    performance.setRestrictions(newRestrictions);
  }

  @Override
  public boolean checkValidity(final Collection<String> messagesToDisplay) {
    boolean valid = true;

    for (final RestrictionEditor editor : editors) {
      final boolean editorValid = editor.checkValidity(messagesToDisplay);
      valid &= editorValid;
    }

    return valid;
  }

}
