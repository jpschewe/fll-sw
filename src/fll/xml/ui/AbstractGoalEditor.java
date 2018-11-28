/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import fll.util.FormatterUtils;
import fll.util.LogUtils;
import fll.util.TextAreaEditor;
import fll.xml.AbstractGoal;

/**
 * Editor for {@link AbstractGoal} objects.
 * Fires property change "title" when the title changes. This allows the
 * container to update it's title. The type of this property is {@link String}.
 */
/* package */ abstract class AbstractGoalEditor extends JPanel implements Validatable {

  private static final Logger LOGGER = LogUtils.getLogger();

  private final JFormattedTextField mTitleEditor;

  private final JFormattedTextField mNameEditor;

  private final TextAreaEditor mDescriptionEditor;

  private final JFormattedTextField mCategoryEditor;

  private final ValidityPanel goalValid;

  public AbstractGoalEditor(final AbstractGoal goal) {
    super(new GridBagLayout());
    mGoal = goal;

    GridBagConstraints gbc;

    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    goalValid = new ValidityPanel();
    add(goalValid, gbc);

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    add(new JLabel("Title: "), gbc);

    mTitleEditor = FormatterUtils.createStringField();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(mTitleEditor, gbc);
    mTitleEditor.setValue(goal.getTitle());

    mTitleEditor.addPropertyChangeListener("value", e -> {
      final String oldTitle = goal.getTitle();
      final String newTitle = mTitleEditor.getText();
      goal.setTitle(newTitle);
      fireTitleChange(oldTitle, newTitle);
    });

    mTitleEditor.setColumns(80);
    mTitleEditor.setMaximumSize(mTitleEditor.getPreferredSize());

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    add(new JLabel("Name: "), gbc);

    mNameEditor = FormatterUtils.createDatabaseNameField();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(mNameEditor, gbc);
    mNameEditor.setValue(goal.getName());

    mNameEditor.addPropertyChangeListener("value", e -> {
      final String newValue = mNameEditor.getText();
      goal.setName(newValue);
    });

    mNameEditor.setColumns(40);
    mNameEditor.setMaximumSize(mNameEditor.getPreferredSize());

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    add(new JLabel("Description: "), gbc);

    mDescriptionEditor = new TextAreaEditor(2, 40);
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(mDescriptionEditor, gbc);
    mDescriptionEditor.setText(goal.getDescription());

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    add(new JLabel("Category: "), gbc);

    mCategoryEditor = FormatterUtils.createStringField();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(mCategoryEditor, gbc);
    mCategoryEditor.setValue(goal.getCategory());

    mCategoryEditor.addPropertyChangeListener("value", e -> {
      final String newValue = mCategoryEditor.getText();
      goal.setCategory(newValue);
    });

  }

  /**
   * Force any pending edits to complete.
   */
  public void commitChanges() {
    try {
      mTitleEditor.commitEdit();
    } catch (final ParseException e) {
      LOGGER.debug("Got parse exception committing changes to title, assuming bad value and ignoring", e);
    }

    try {
      mNameEditor.commitEdit();
    } catch (final ParseException e) {
      LOGGER.debug("Got parse exception committing changes to name, assuming bad value and ignoring", e);
    }

    mGoal.setDescription(mDescriptionEditor.getText());

    try {
      mCategoryEditor.commitEdit();
    } catch (final ParseException e) {
      LOGGER.debug("Got parse exception committing changes to category, assuming bad value and ignoring", e);
    }

  }

  protected void fireTitleChange(final String oldTitle,
                                 final String newTitle) {
    firePropertyChange("title", oldTitle, newTitle);
  }

  private final AbstractGoal mGoal;

  /**
   * @return the goal that is being edited
   */
  public AbstractGoal getGoal() {
    return mGoal;
  }

  /**
   * Called by {@link #checkValidity()}. If the list is empty after the call, then
   * the goal is valid, otherwise the goal is invalid and the messages will be
   * displayed to the user.
   * Subclasses should override this to add extra checks. Make sure to call the
   * parent class method.
   * 
   * @param messages put invalid messages in the list.
   */
  protected void gatherValidityMessages(final List<String> messages) {

    if (StringUtils.isBlank(mTitleEditor.getText())) {
      messages.add("The goal must have a title");
    }

    if (StringUtils.isBlank(mNameEditor.getText())) {
      messages.add("The goal must have a name");
    }

  }

  @Override
  public boolean checkValidity() {
    final List<String> messages = new LinkedList<>();
    gatherValidityMessages(messages);

    if (!messages.isEmpty()) {
      goalValid.setInvalid(String.join("<br/>", messages));
      return false;
    } else {
      goalValid.setValid();
      return true;
    }
  }

}
