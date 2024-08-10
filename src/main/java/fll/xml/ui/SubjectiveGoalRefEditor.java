/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.util.Collection;
import java.util.LinkedList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.util.ChooseOptionDialog;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveGoalRef;
import fll.xml.SubjectiveScoreCategory;

/**
 * Editor for {@link SubjectiveGoalRef} objects.
 */
@SuppressFBWarnings(value = { "SE_BAD_FIELD",
                              "SE_BAD_FIELD_STORE" }, justification = "This class isn't going to be serialized")
/* package */ class SubjectiveGoalRefEditor extends Box implements Validatable {

  private final ValidityPanel validPanel;

  private final JButton categoryEditor;

  private final JButton goalEditor;

  private final ChallengeDescription description;

  private SubjectiveScoreCategory category;

  private AbstractGoal goal;

  /**
   * @param ref the object to edit
   */
  /* package */ SubjectiveGoalRefEditor(final ChallengeDescription description,
                                        final SubjectiveGoalRef ref) {
    this(description, ref.getCategory(), ref.getGoal());
  }

  /**
   * @param description used to find subjective categories
   * @param category subjective category for the reference
   * @param goal the goal for the reference
   */
  /* package */ SubjectiveGoalRefEditor(final ChallengeDescription description,
                                        final SubjectiveScoreCategory category,
                                        final AbstractGoal goal) {
    super(BoxLayout.LINE_AXIS);
    this.description = description;
    this.category = category;
    this.goal = goal;

    validPanel = new ValidityPanel();
    add(validPanel);

    categoryEditor = new JButton(category.getTitle());
    add(categoryEditor);
    categoryEditor.setToolTipText("Click to change the category");
    categoryEditor.addActionListener(l -> {
      final Collection<SubjectiveScoreCategory> categories = description.getSubjectiveCategories();
      if (categories.isEmpty()) {
        JOptionPane.showMessageDialog(SubjectiveGoalRefEditor.this, "No subjective categories found, define one first",
                                      "Error", JOptionPane.ERROR_MESSAGE);
        return;
      }

      final ChooseOptionDialog<SubjectiveScoreCategory> dialog = new ChooseOptionDialog<>(JOptionPane.getRootFrame(),
                                                                                          new LinkedList<>(categories),
                                                                                          new SubjectiveScoreCategoryCellRenderer());
      dialog.setVisible(true);
      final SubjectiveScoreCategory selected = dialog.getSelectedValue();
      if (null != selected) {
        this.category = selected;
        categoryEditor.setText(selected.getTitle());
      }
    });

    goalEditor = new JButton(goal.getTitle());
    add(goalEditor);
    goalEditor.setToolTipText("Click to change the goal");
    goalEditor.addActionListener(l -> {
      final Collection<AbstractGoal> goals = this.category.getAllGoals();
      if (goals.isEmpty()) {
        JOptionPane.showMessageDialog(SubjectiveGoalRefEditor.this, "No goals found in category, define one first",
                                      "Error", JOptionPane.ERROR_MESSAGE);
        return;
      }

      final ChooseOptionDialog<AbstractGoal> dialog = new ChooseOptionDialog<>(JOptionPane.getRootFrame(),
                                                                               new LinkedList<>(goals),
                                                                               new AbstractGoalCellRenderer());
      dialog.setVisible(true);
      final AbstractGoal selected = dialog.getSelectedValue();
      if (null != selected) {
        this.goal = selected;
        goalEditor.setText(selected.getTitle());
      }
    });
  }

  @Override
  public boolean checkValidity(final Collection<String> messagesToDisplay) {
    final Collection<String> messages = new LinkedList<>();

    boolean valid = true;
    if (!description.getSubjectiveCategories().contains(category)) {
      messages.add(String.format("Category %s is not in the description, please select the category again",
                                 category.getTitle()));
      valid = false;
    }

    if (!category.getAllGoals().contains(goal)) {
      messages.add(String.format("Goal %s not found in category %s, please select the goal again", category.getTitle(),
                                 goal.getTitle()));
      valid = false;
    }

    if (!messages.isEmpty()) {
      validPanel.setInvalid(String.join("<br/>", messages));
    } else {
      validPanel.setValid();
    }

    return valid;
  }

  /**
   * @return a ref that matches the values in the editor
   */
  public SubjectiveGoalRef getSubjectiveGoalRef() {
    return new SubjectiveGoalRef(goal.getName(), category);
  }

}
