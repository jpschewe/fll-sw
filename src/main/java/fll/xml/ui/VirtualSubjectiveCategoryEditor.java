/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;

import fll.Utilities;
import fll.util.FormatterUtils;
import fll.util.GuiUtils;
import fll.xml.AbstractGoal;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveGoalRef;
import fll.xml.SubjectiveScoreCategory;
import fll.xml.VirtualSubjectiveScoreCategory;

/**
 * Editor for {@link VirtualSubjectiveScoreCategory} objects.
 */
public class VirtualSubjectiveCategoryEditor extends Box implements Validatable {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private final JFormattedTextField weight;

  private final List<SubjectiveGoalRefEditor> goalReferenceEditors = new LinkedList<>();

  private final Box goalReferenceContainer;

  private final VirtualSubjectiveScoreCategory category;

  private final ChallengeDescription description;

  private final ValidityPanel categoryValid;

  /**
   * @param description used to find subjective categories
   * @param scoreCategory the object to edit
   */
  public VirtualSubjectiveCategoryEditor(final ChallengeDescription description,
                                         final VirtualSubjectiveScoreCategory scoreCategory) {
    super(BoxLayout.PAGE_AXIS);
    this.description = description;
    category = scoreCategory;

    categoryValid = new ValidityPanel();
    add(categoryValid);

    final Box weightContainer = Box.createHorizontalBox();
    add(weightContainer);

    weightContainer.add(new JLabel("Weight: "));

    weight = FormatterUtils.createDoubleField();
    weightContainer.add(weight);
    weightContainer.add(Box.createHorizontalGlue());

    final Box buttonBox = Box.createHorizontalBox();
    add(buttonBox);

    final JButton addGoalReference = new JButton("Add Goal Reference");
    buttonBox.add(addGoalReference);
    buttonBox.add(Box.createHorizontalGlue());

    goalReferenceContainer = Box.createVerticalBox();
    add(goalReferenceContainer);

    // object is initialized
    weight.addPropertyChangeListener("value", e -> {
      if (null != category) {
        final Number value = (Number) weight.getValue();
        if (null != value) {
          final double newWeight = value.doubleValue();
          category.setWeight(newWeight);
        }
      }
    });

    addGoalReference.addActionListener(l -> addNewGoalReference());

    weight.setValue(category.getWeight());

    category.getGoalReferences().forEach(this::addGoalReference);
  }

  /**
   * @return the category being edited
   */
  public VirtualSubjectiveScoreCategory getCategory() {
    return category;
  }

  private void addNewGoalReference(@UnknownInitialization(VirtualSubjectiveCategoryEditor.class) VirtualSubjectiveCategoryEditor this) {
    final Optional<SubjectiveScoreCategory> category = description.getSubjectiveCategories().stream().findAny();
    if (category.isEmpty()) {
      JOptionPane.showMessageDialog(this, "No subjective categories found, define one first", "Error",
                                    JOptionPane.ERROR_MESSAGE);
      return;
    }

    final Optional<AbstractGoal> goal = category.get().getAllGoals().stream().findAny();
    if (goal.isEmpty()) {
      JOptionPane.showMessageDialog(this, String.format("No goals found in category %s, define one first",
                                                        category.get().getTitle()),
                                    "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    addGoalReference(category.get(), goal.get());
  }

  private void addGoalReference(@UnknownInitialization(VirtualSubjectiveCategoryEditor.class) VirtualSubjectiveCategoryEditor this,
                                final SubjectiveGoalRef ref) {
    addGoalReference(ref.getCategory(), ref.getGoal());
  }

  private void addGoalReference(@UnknownInitialization(VirtualSubjectiveCategoryEditor.class) VirtualSubjectiveCategoryEditor this,
                                final SubjectiveScoreCategory category,
                                final AbstractGoal goal) {
    final SubjectiveGoalRefEditor editor = new SubjectiveGoalRefEditor(description, category, goal);
    goalReferenceEditors.add(editor);

    final Box panel = Box.createHorizontalBox();
    panel.add(editor);
    final JButton delete = new JButton("Delete");
    panel.add(delete);
    panel.add(Box.createHorizontalGlue());
    
    delete.addActionListener(l -> {
      final int confirm = JOptionPane.showConfirmDialog(VirtualSubjectiveCategoryEditor.this,
                                                        "Are you sure that you want to delete the goal reference?",
                                                        "Confirm Delete", JOptionPane.YES_NO_OPTION);
      if (confirm != JOptionPane.YES_OPTION) {
        return;
      }

      final int index = Utilities.getIndexOfComponent(goalReferenceContainer, panel);
      if (index < 0) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Unable to find source of delete event in goal container");
        }
        return;
      }

      // update editor list
      goalReferenceEditors.remove(index);

      // update the UI
      GuiUtils.removeFromContainer(goalReferenceContainer, index);
    });
    GuiUtils.addToContainer(goalReferenceContainer, panel);
  }

  /**
   * @return the object being edited
   */
  public VirtualSubjectiveScoreCategory getVirtualSubjectiveScoreCategory() {
    return category;
  }

  /**
   * Force any pending edits to complete.
   */
  public void commitChanges() {
    try {
      weight.commitEdit();
    } catch (final ParseException e) {
      LOGGER.debug("Got parse exception committing changes, assuming bad value and ignoring", e);
    }

    category.setGoalReferences(goalReferenceEditors.stream().map(SubjectiveGoalRefEditor::getSubjectiveGoalRef)
                                                   .toList());
  }

  @Override
  public boolean checkValidity(final Collection<String> messagesToDisplay) {
    boolean valid = true;
    final List<String> messages = new LinkedList<>();

    for (final SubjectiveGoalRefEditor editor : goalReferenceEditors) {
      if (!editor.checkValidity(messages)) {
        valid = false;
      }
    }

    if (!valid) {
      if (!messages.isEmpty()) {
        categoryValid.setInvalid(String.join("<br/>", messages));
      } else {
        categoryValid.setInvalid("");
      }
    } else {
      categoryValid.setValid();
    }
    return valid;
  }
}
