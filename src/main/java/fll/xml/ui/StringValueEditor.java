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
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.util.ChooseOptionDialog;
import fll.util.FLLInternalException;
import fll.util.FormatterUtils;
import fll.xml.AbstractGoal;
import fll.xml.GoalRef;
import fll.xml.GoalScope;
import fll.xml.GoalScoreType;
import fll.xml.StringConstant;
import fll.xml.StringValue;

/**
 * Editor for {@link StringValue}.
 */
@SuppressFBWarnings(value = { "SE_BAD_FIELD",
                              "SE_BAD_FIELD_STORE" }, justification = "This class isn't going to be serialized")
class StringValueEditor extends JPanel implements Validatable {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private final JButton goalEditor;

  private final JFormattedTextField stringEditor;

  private static final String NO_GOAL = "<NONE>";

  private static final String GOAL_PANEL = "goal";

  private static final String STRING_PANEL = "string";

  private final JCheckBox decision;

  private final GoalScope goalScope;

  private @Nullable AbstractGoal selectedGoal;

  StringValueEditor(final StringValue value,
                    final GoalScope goalScope) {
    super(new BorderLayout());
    this.goalScope = goalScope;

    decision = new JCheckBox("String");
    add(decision, BorderLayout.NORTH);

    final CardLayout layout = new CardLayout();
    final JPanel panel = new JPanel(layout);
    add(panel, BorderLayout.CENTER);

    goalEditor = new JButton(value.isGoalRef() ? NO_GOAL : value.getRawStringValue());

    if (value.isGoalRef()) {
      final GoalRef goalRef = (GoalRef) value;
      goalRef.getGoal().addPropertyChangeListener(nameListener);
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
        if (null != this.selectedGoal) {
          this.selectedGoal.removePropertyChangeListener(nameListener);
        }

        this.selectedGoal = selected;
        this.selectedGoal.addPropertyChangeListener(nameListener);
        goalEditor.setText(this.selectedGoal.getTitle());
      }
    });

    final Box goalPanel = Box.createHorizontalBox();
    panel.add(goalPanel, GOAL_PANEL);
    goalPanel.add(goalEditor);
    goalPanel.add(Box.createHorizontalGlue());

    stringEditor = FormatterUtils.createStringField();
    panel.add(stringEditor, STRING_PANEL);
    stringEditor.setText(value.getRawStringValue());

    decision.addActionListener(e -> {
      if (decision.isSelected()) {
        layout.show(panel, STRING_PANEL);
      } else {
        layout.show(panel, GOAL_PANEL);
      }
    });

    // initial setup
    if (value.isStringConstant()) {
      decision.doClick();
    }
  }

  /**
   * @return the value being edited
   */
  public StringValue getStringValue() {
    if (decision.isSelected()) {
      return new StringConstant(stringEditor.getText());
    } else {
      if (null != selectedGoal) {
        return new GoalRef(selectedGoal.getName(), goalScope, GoalScoreType.RAW);
      } else {
        throw new FLLInternalException("Invalid state. Editor has goal selected, but there is no goal");
      }
    }
  }

  private final PropertyChangeListener nameListener = new PropertyChangeListener() {
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
      if ("title".equals(evt.getPropertyName())) {
        final String newTitle = (String) evt.getNewValue();
        goalEditor.setText(newTitle);
      }
    }
  };

  @Override
  public boolean checkValidity(Collection<String> messagesToDisplay) {
    boolean valid = true;

    if (!decision.isSelected()) {
      final AbstractGoal localSelectedGoal = selectedGoal;
      if (null == localSelectedGoal) {
        messagesToDisplay.add("You must select a goal to reference");
        valid = false;
      } else {
        if (!goalScope.getAllGoals().contains(localSelectedGoal)) {
          messagesToDisplay.add(String.format("Goal %s is not known. It may have been deleted.",
                                              localSelectedGoal.getName()));
          valid = false;
        }
      }
    }

    return valid;
  }

  /**
   * Force any pending edits to complete.
   */
  public void commitChanges() {
    try {
      stringEditor.commitEdit();
    } catch (final ParseException e) {
      LOGGER.debug("Got parse exception committing changes to string constant, assuming bad value and ignoring", e);
    }
  }
}
