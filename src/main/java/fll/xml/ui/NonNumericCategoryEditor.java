/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
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

import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

import fll.util.FormatterUtils;
import fll.util.TextAreaEditor;
import fll.xml.ChallengeParser;
import fll.xml.ChallengeValidationException;
import fll.xml.NonNumericCategory;

/**
 * Editor for {@link NonNumericCategory} objects.
 */
/* package */ class NonNumericCategoryEditor extends JPanel implements Validatable {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private final JFormattedTextField titleEditor;

  private final JCheckBox perAwardGroupEditor;

  private final TextAreaEditor description;

  private final ValidityPanel validPanel;

  /* package */ NonNumericCategoryEditor(final NonNumericCategory category) {
    super(new GridBagLayout());

    this.category = category;

    GridBagConstraints gbc;
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    validPanel = new ValidityPanel();
    add(validPanel, gbc);

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    add(new JLabel("Name: "), gbc);

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    add(new JLabel("Title: "), gbc);

    titleEditor = FormatterUtils.createStringField();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(titleEditor, gbc);
    titleEditor.setValue(this.category.getTitle());

    titleEditor.addPropertyChangeListener("value", e -> {
      final String oldTitle = this.category.getTitle();
      final String newTitle = titleEditor.getText();
      this.category.setTitle(newTitle);
      fireTitleChange(oldTitle, newTitle);
    });

    titleEditor.setColumns(ChallengeDescriptionEditor.LONG_TEXT_WIDTH);
    titleEditor.setMaximumSize(titleEditor.getPreferredSize());

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    add(new JLabel("Description: "), gbc);

    this.description = new TextAreaEditor(2, 40);
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(description, gbc);
    description.setText(this.category.getDescription());

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    add(new JLabel("Per award group: "), gbc);

    perAwardGroupEditor = new JCheckBox();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(perAwardGroupEditor, gbc);
    perAwardGroupEditor.setSelected(this.category.getPerAwardGroup());
    perAwardGroupEditor.addActionListener(e -> {
      this.category.setPerAwardGroup(perAwardGroupEditor.isSelected());
    });
    perAwardGroupEditor.setToolTipText("If checked, winners of this non-numeric category are per award group, otherwise they are for the whole tournament");

  }

  protected void fireTitleChange(@UnknownInitialization(Component.class) NonNumericCategoryEditor this,
                                 final String oldTitle,
                                 final String newTitle) {
    firePropertyChange("title", oldTitle, newTitle);
  }

  private final NonNumericCategory category;

  /**
   * @return the non-numeric category that is being edited
   */
  public NonNumericCategory getNonNumericCategory() {
    return category;
  }

  /**
   * Force any pending edits to complete.
   */
  public void commitChanges() {
    try {
      titleEditor.commitEdit();
    } catch (final ParseException e) {
      LOGGER.debug("Got parse exception committing changes to title, assuming bad value and ignoring", e);
    }

    category.setDescription(description.getText());

  }

  @Override
  public boolean checkValidity(final Collection<String> messagesToDisplay) {
    final List<String> messages = new LinkedList<>();

    if (StringUtils.isBlank(titleEditor.getText())) {
      messages.add("The non-numeric category must have a title");
    }

    try {
      ChallengeParser.validateNonNumericCategory(category);
    } catch (final ChallengeValidationException e) {
      final String message = e.getMessage();
      if (null != message) {
        messages.add(message);
      } else {
        messages.add(e.toString());
      }
    }

    if (!messages.isEmpty()) {
      validPanel.setInvalid(String.join("<br/>", messages));
      return false;
    } else {
      validPanel.setValid();
      return true;
    }
  }

}
