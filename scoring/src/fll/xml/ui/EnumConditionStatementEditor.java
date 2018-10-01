/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.BorderLayout;

import javax.annotation.Nonnull;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import fll.xml.EnumConditionStatement;
import fll.xml.GoalScope;
import fll.xml.InequalityComparison;

/**
 * Edit {@link EnumConditionStatement}.
 */
/* package */ class EnumConditionStatementEditor extends JPanel {

  private final EnumStringEditor leftEditor;

  private final EnumStringEditor rightEditor;

  private final EnumConditionStatement stmt;

  public EnumConditionStatementEditor(@Nonnull final EnumConditionStatement stmt,
                                      @Nonnull final GoalScope goalScope) {
    super(new BorderLayout());
    this.stmt = stmt;

    this.add(Box.createHorizontalStrut(10), BorderLayout.WEST);

    final Box container = Box.createVerticalBox();
    add(container, BorderLayout.CENTER);

    leftEditor = new EnumStringEditor(stmt.getLeftGoalRef(), stmt.getLeftString(), goalScope);
    container.add(leftEditor);

    final JComboBox<InequalityComparison> comparisonEditor = new JComboBox<>(new InequalityComparison[] { InequalityComparison.EQUAL_TO,
                                                                                                          InequalityComparison.NOT_EQUAL_TO });
    container.add(comparisonEditor);
    comparisonEditor.addActionListener(e -> {
      stmt.setComparison(comparisonEditor.getItemAt(comparisonEditor.getSelectedIndex()));
    });

    rightEditor = new EnumStringEditor(stmt.getRightGoalRef(), stmt.getRightString(), goalScope);
    container.add(rightEditor);

    container.add(Box.createVerticalGlue());
  }

  /**
   * Force any pending edits to complete.
   */
  public void commitChanges() {
    stmt.setLeftGoalRef(leftEditor.getGoalRef());
    stmt.setLeftString(leftEditor.getString());
    stmt.setRightGoalRef(rightEditor.getGoalRef());
    stmt.setRightString(rightEditor.getString());
  }

}
