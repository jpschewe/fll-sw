/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.BorderLayout;
import java.util.Collection;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import fll.xml.EnumConditionStatement;
import fll.xml.GoalScope;
import fll.xml.InequalityComparison;

/**
 * Edit {@link EnumConditionStatement}.
 */
/* package */ class EnumConditionStatementEditor extends JPanel implements Validatable {

  private final StringValueEditor leftEditor;

  private final StringValueEditor rightEditor;

  private final EnumConditionStatement stmt;

  EnumConditionStatementEditor(final EnumConditionStatement stmt,
                               final GoalScope goalScope) {
    super(new BorderLayout());
    this.stmt = stmt;

    this.add(Box.createHorizontalStrut(10), BorderLayout.WEST);

    final Box container = Box.createVerticalBox();
    add(container, BorderLayout.CENTER);

    leftEditor = new StringValueEditor(stmt.getLeft(), goalScope);
    container.add(leftEditor);

    final JComboBox<InequalityComparison> comparisonEditor = new JComboBox<>(new InequalityComparison[] { InequalityComparison.EQUAL_TO,
                                                                                                          InequalityComparison.NOT_EQUAL_TO });
    container.add(comparisonEditor);
    comparisonEditor.addActionListener(e -> {
      stmt.setComparison(comparisonEditor.getItemAt(comparisonEditor.getSelectedIndex()));
    });

    rightEditor = new StringValueEditor(stmt.getRight(), goalScope);
    container.add(rightEditor);

    container.add(Box.createVerticalGlue());
  }

  /**
   * Force any pending edits to complete.
   */
  public void commitChanges() {
    leftEditor.commitChanges();
    rightEditor.commitChanges();
    stmt.setLeft(leftEditor.getStringValue());
    stmt.setRight(rightEditor.getStringValue());
  }

  @Override
  public boolean checkValidity(Collection<String> messagesToDisplay) {
    boolean valid = true;

    final boolean leftValid = leftEditor.checkValidity(messagesToDisplay);
    valid &= leftValid;

    final boolean rightValid = rightEditor.checkValidity(messagesToDisplay);
    valid &= rightValid;

    return valid;
  }

}
