/*
 * Copyright (c) 2020 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

import fll.util.FormatterUtils;
import fll.util.TextAreaEditor;
import fll.xml.GoalElement;

/**
 * Editor for {@link GoalElement}.
 * Fires property change "title" when the title changes. This allows the
 * container to update it's title. The type of this property is {@link String}.
 */
/* package */ abstract class GoalElementEditor extends JPanel implements Validatable {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private final GoalElement goalElement;

  /**
   * @return the object being edited
   */
  public GoalElement getGoalElement() {
    return goalElement;
  }

  private final JFormattedTextField mTitleEditor;

  private final TextAreaEditor mDescriptionEditor;

  private final ValidityPanel goalElementValid;

  /**
   * @param goalElement the object to edit
   */
  /* package */ GoalElementEditor(final GoalElement goalElement) {
    super(new GridBagLayout());
    this.goalElement = goalElement;

    GridBagConstraints gbc;

    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    goalElementValid = new ValidityPanel();
    add(goalElementValid, gbc);

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
    mTitleEditor.setValue(goalElement.getTitle());

    mTitleEditor.addPropertyChangeListener("value", e -> {
      final String oldTitle = goalElement.getTitle();
      final String newTitle = mTitleEditor.getText();
      goalElement.setTitle(newTitle);
      fireTitleChange(oldTitle, newTitle);
    });

    mTitleEditor.setColumns(ChallengeDescriptionEditor.LONG_TEXT_WIDTH);
    mTitleEditor.setMaximumSize(mTitleEditor.getPreferredSize());

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    add(new JLabel("Description: "), gbc);

    mDescriptionEditor = new TextAreaEditor(2, ChallengeDescriptionEditor.MEDIUM_TEXT_WIDTH);
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(mDescriptionEditor, gbc);
    mDescriptionEditor.setText(goalElement.getDescription());

  }

  /**
   * Called by {@link #checkValidity(Collection)}. If the list is empty after the
   * call, then
   * the goal is valid, otherwise the goal is invalid and the messages will be
   * displayed to the user.
   * Subclasses should override this to add extra checks. Make sure to call the
   * parent class method.
   *
   * @param messages put invalid messages in the list.
   */
  protected void gatherValidityMessages(final Collection<String> messages) {

    if (StringUtils.isBlank(mTitleEditor.getText())) {
      messages.add("The goal must have a title");
    }

  }

  @Override
  public boolean checkValidity(final Collection<String> messagesToDisplay) {
    final List<String> messages = new LinkedList<>();
    gatherValidityMessages(messages);

    if (!messages.isEmpty()) {
      goalElementValid.setInvalid(String.join("<br/>", messages));
      return false;
    } else {
      goalElementValid.setValid();
      return true;
    }
  }

  protected void fireTitleChange(@UnknownInitialization(Component.class) GoalElementEditor this,
                                 final String oldTitle,
                                 final String newTitle) {
    firePropertyChange("title", oldTitle, newTitle);
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

    goalElement.setDescription(mDescriptionEditor.getText());

  }

}
