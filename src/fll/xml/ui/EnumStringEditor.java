/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import fll.util.ChooseOptionDialog;
import fll.util.FormatterUtils;
import fll.xml.AbstractGoal;
import fll.xml.GoalRef;
import fll.xml.GoalScope;
import fll.xml.GoalScoreType;
import fll.xml.ScopeException;

/**
 * Editor to allow one to pick a string or an enumerated goal.
 */
class EnumStringEditor extends JPanel implements Validatable {

  private GoalRef goalRef;

  private String string;

  private final JButton goalEditor;

  private final JFormattedTextField stringEditor;

  private static final String NO_GOAL = "<NONE>";

  private static final String GOAL_PANEL = "goal";

  private static final String STRING_PANEL = "string";

  private final JCheckBox decision;

  private final GoalScope goalScope;

  public EnumStringEditor(final GoalRef goalRef,
                          final String str,
                          @Nonnull final GoalScope goalScope) {
    super(new BorderLayout());
    this.goalRef = goalRef;
    this.string = str;
    this.goalScope = goalScope;

    decision = new JCheckBox("String");
    add(decision, BorderLayout.NORTH);

    final CardLayout layout = new CardLayout();
    final JPanel panel = new JPanel(layout);
    add(panel, BorderLayout.CENTER);

    goalEditor = new JButton(null == this.goalRef ? NO_GOAL : this.goalRef.getGoalName());

    if (null != this.goalRef) {
      this.goalRef.getGoal().addPropertyChangeListener(nameListener);
    }

    goalEditor.setToolTipText("Click to change the referenced goal");
    goalEditor.addActionListener(l -> {
      final Collection<AbstractGoal> goals = goalScope.getAllGoals().stream().filter(AbstractGoal::isEnumerated)
                                                      .collect(Collectors.toList());

      final ChooseOptionDialog<AbstractGoal> dialog = new ChooseOptionDialog<>(JOptionPane.getRootFrame(),
                                                                               new LinkedList<>(goals),
                                                                               new AbstractGoalCellRenderer());
      dialog.setVisible(true);
      final AbstractGoal selected = dialog.getSelectedValue();
      if (null != selected) {
        if (null != this.goalRef) {
          this.goalRef.getGoal().removePropertyChangeListener(nameListener);
        }

        this.goalRef = new GoalRef(selected.getName(), goalScope, GoalScoreType.RAW);
        goalEditor.setText(selected.getTitle());

        this.goalRef.getGoal().addPropertyChangeListener(nameListener);
      }
    });

    final Box goalPanel = Box.createHorizontalBox();
    panel.add(goalPanel, GOAL_PANEL);
    goalPanel.add(goalEditor);
    goalPanel.add(Box.createHorizontalGlue());

    stringEditor = FormatterUtils.createStringField();
    panel.add(stringEditor, STRING_PANEL);

    decision.addActionListener(e -> {
      if (decision.isSelected()) {
        layout.show(panel, STRING_PANEL);
        this.goalRef = null;
        goalEditor.setText(NO_GOAL);
      } else {
        layout.show(panel, GOAL_PANEL);
        this.string = null;
        stringEditor.setText(null);
      }
    });

    // initial setup
    if (null != str) {
      decision.doClick();
    }
  }

  /**
   * @return the referenced goal or null. If null, then {@link #getString()}
   *         should not return null.
   */
  public GoalRef getGoalRef() {
    return goalRef;
  }

  /**
   * @return the string. If null, then {@link #getGoalRef()} should not return
   *         null.
   */
  public String getString() {
    return string;
  }

  private final PropertyChangeListener nameListener = new PropertyChangeListener() {
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
      if ("name".equals(evt.getPropertyName())) {
        final String newName = (String) evt.getNewValue();
        goalRef.setGoalName(newName);
      } else if ("title".equals(evt.getPropertyName())) {
        final String newTitle = (String) evt.getNewValue();
        goalEditor.setText(newTitle);
      }
    }
  };

  @Override
  public boolean checkValidity(Collection<String> messagesToDisplay) {
    boolean valid = true;

    if (!decision.isSelected()) {
      try {
        goalScope.getGoal(goalRef.getGoalName());
      } catch (final ScopeException e) {
        messagesToDisplay.add(String.format("Goal %s is not known. It may have been deleted.", goalRef.getGoalName()));
        valid = false;
      }
    }

    return valid;
  }

}
