/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import javax.swing.JComboBox;

import fll.xml.GoalScope;
import fll.xml.TiebreakerTest;
import fll.xml.WinnerType;

/**
 * Editor for {@link TiebreakerTest} objects.
 * All changes are immediately written to the object.
 */
public class TiebreakerTestEditor extends PolynomialEditor {

  private final TiebreakerTest test;

  /**
   * @return the object being edited
   */
  public TiebreakerTest getTest() {
    return test;
  }

  /**
   * @param test the object to edit
   * @param goalScope where to find goals
   */
  public TiebreakerTestEditor(final TiebreakerTest test,
                              final GoalScope goalScope) {
    super(test, goalScope, null);
    this.test = test;

    final JComboBox<WinnerType> winnerEditor = new JComboBox<>(WinnerType.values());
    this.add(winnerEditor);

    winnerEditor.addActionListener(e -> {
      final WinnerType winner = winnerEditor.getItemAt(winnerEditor.getSelectedIndex());
      test.setWinner(winner);
    });

    winnerEditor.setToolTipText("Determine if the high score or low score determines the winner of this tiebreaker");
  }

}
