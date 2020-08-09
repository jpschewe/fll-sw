/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.GridBagConstraints;
import java.text.ParseException;
import java.util.Collection;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;

import org.apache.commons.lang3.StringUtils;

import fll.util.FormatterUtils;
import fll.xml.AbstractGoal;

/**
 * Editor for {@link AbstractGoal} objects.
 */
/* package */ abstract class AbstractGoalEditor extends GoalElementEditor {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private final JFormattedTextField mNameEditor;

  /* package */ AbstractGoalEditor(final AbstractGoal goal) {
    super(goal);

    mGoal = goal;

    GridBagConstraints gbc;

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

    mNameEditor.setColumns(ChallengeDescriptionEditor.MEDIUM_TEXT_WIDTH);
    mNameEditor.setMaximumSize(mNameEditor.getPreferredSize());

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    add(new JLabel("Category: "), gbc);
  }

  /**
   * Force any pending edits to complete.
   */
  public void commitChanges() {
    super.commitChanges();

    try {
      mNameEditor.commitEdit();
    } catch (final ParseException e) {
      LOGGER.debug("Got parse exception committing changes to name, assuming bad value and ignoring", e);
    }
  }

  private final AbstractGoal mGoal;

  /**
   * @return the goal that is being edited
   */
  public AbstractGoal getGoal() {
    return mGoal;
  }

  @Override
  protected void gatherValidityMessages(final Collection<String> messages) {
    super.gatherValidityMessages(messages);

    if (StringUtils.isBlank(mNameEditor.getText())) {
      messages.add("The goal must have a name");
    }
  }

}
