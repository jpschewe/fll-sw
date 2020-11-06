/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringUtils;

import fll.util.ChooseOptionDialog;
import fll.util.FormatterUtils;
import fll.util.TextAreaEditor;
import fll.xml.ChallengeDescription;
import fll.xml.ChallengeParser;
import fll.xml.ChallengeValidationException;
import fll.xml.NonNumericCategory;
import fll.xml.SubjectiveScoreCategory;

/**
 * Editor for {@link SubjectiveScoreCategory} objects.
 */
public class SubjectiveCategoryEditor extends ScoreCategoryEditor {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private final JFormattedTextField mTitleEditor;

  private final JFormattedTextField mNameEditor;

  private final SubjectiveScoreCategory mSubjectiveCategory;

  private final TextAreaEditor mScoreSheetInstructions;

  private final JComponent nominatesContainer;

  private final ChallengeDescription description;

  /**
   * @param category the object to edit
   * @param description used to lookup non-numeric categories
   */
  public SubjectiveCategoryEditor(final SubjectiveScoreCategory category,
                                  final ChallengeDescription description) {
    super(category);
    this.mSubjectiveCategory = category;
    this.description = description;

    final Box titleContainer = Box.createHorizontalBox();
    add(titleContainer, 1); // add at particular index to get above weight

    titleContainer.add(new JLabel("Title: "));

    mTitleEditor = FormatterUtils.createStringField();
    titleContainer.add(mTitleEditor);
    mTitleEditor.setColumns(80);
    mTitleEditor.setMaximumSize(mTitleEditor.getPreferredSize());

    mTitleEditor.addPropertyChangeListener("value", e -> {
      if (null != mSubjectiveCategory) {
        final String oldTitle = mSubjectiveCategory.getTitle();
        final String newTitle = mTitleEditor.getText();
        mSubjectiveCategory.setTitle(newTitle);
        fireTitleChange(oldTitle, newTitle);
      }
    });

    titleContainer.add(Box.createHorizontalGlue());

    final Box nameContainer = Box.createHorizontalBox();
    add(nameContainer, 2); // add at particular index to get above weight

    nameContainer.add(new JLabel("Name: "));

    mNameEditor = FormatterUtils.createDatabaseNameField();
    nameContainer.add(mNameEditor);

    mNameEditor.setColumns(40);
    mNameEditor.setMaximumSize(mNameEditor.getPreferredSize());

    mNameEditor.addPropertyChangeListener("value", e -> {
      if (null != mSubjectiveCategory) {
        final String newName = mNameEditor.getText();
        mSubjectiveCategory.setName(newName);
      }
    });

    nameContainer.add(Box.createHorizontalGlue());

    final Box scoreSheetInstructionsContainer = Box.createHorizontalBox();
    add(scoreSheetInstructionsContainer, 4); // just below weight
    scoreSheetInstructionsContainer.add(new JLabel("Instructions: "));

    mScoreSheetInstructions = new TextAreaEditor(4, 40);
    scoreSheetInstructionsContainer.add(mScoreSheetInstructions);
    mScoreSheetInstructions.setText(mSubjectiveCategory.getScoreSheetInstructions());

    final Box buttonBar = Box.createHorizontalBox();
    this.add(buttonBar);

    final JButton addGoal = new JButton("Add Nominates");
    buttonBar.add(addGoal);
    addGoal.addActionListener(l -> addNewNominates());
    addGoal.setToolTipText("Add a non-numeric category to nominate teams for during judging of this subjective category");

    buttonBar.add(Box.createHorizontalGlue());

    nominatesContainer = Box.createVerticalBox();
    this.add(nominatesContainer);

    mSubjectiveCategory.getNominates().forEach(this::addNominates);

    mTitleEditor.setValue(mSubjectiveCategory.getTitle());
    mNameEditor.setValue(mSubjectiveCategory.getName());
  }

  /**
   * @return the subjective score category, may be null
   */
  public SubjectiveScoreCategory getSubjectiveScoreCategory() {
    return mSubjectiveCategory;
  }

  @Override
  public void commitChanges() {
    super.commitChanges();

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

    mSubjectiveCategory.setScoreSheetInstructions(mScoreSheetInstructions.getText());
  }

  /**
   * @param oldTitle the old title
   * @param newTitle the new title
   */
  protected void fireTitleChange(final String oldTitle,
                                 final String newTitle) {
    firePropertyChange("title", oldTitle, newTitle);
  }

  @Override
  protected void gatherValidityMessages(final Collection<String> messages) {
    super.gatherValidityMessages(messages);

    if (StringUtils.isBlank(mTitleEditor.getText())) {
      messages.add("The category must have a title");
    }

    if (StringUtils.isBlank(mNameEditor.getText())) {
      messages.add("The category must have a name");
    }

    if (StringUtils.isBlank(mScoreSheetInstructions.getText())) {
      messages.add("The instructions must not be empty");
    }

    try {
      ChallengeParser.validateSubjectiveCategory(mSubjectiveCategory);
    } catch (final ChallengeValidationException e) {
      messages.add(e.getMessage());
    }
  }

  private void addNewNominates() {
    final Collection<NonNumericCategory> categories = description.getNonNumericCategories();
    if (categories.isEmpty()) {
      JOptionPane.showMessageDialog(this, "You need to define some non-numeric categories first", "Error",
                                    JOptionPane.ERROR_MESSAGE);
      return;
    }
    final ChooseOptionDialog<NonNumericCategory> dialog = new ChooseOptionDialog<>(JOptionPane.getRootFrame(),
                                                                                   new LinkedList<>(categories),
                                                                                   new NonNumericCategoryCellRenderer());
    dialog.setVisible(true);
    final NonNumericCategory selected = dialog.getSelectedValue();
    if (null != selected) {
      final SubjectiveScoreCategory.Nominates ref = new SubjectiveScoreCategory.Nominates(selected.getTitle());
      this.mSubjectiveCategory.addNominates(ref);
      addNominates(ref);
    }
  }

  private void addNominates(final SubjectiveScoreCategory.Nominates nominates) {
    final Box row = Box.createHorizontalBox();

    row.add(new JLabel("  "));

    final NominatesEditor editor = new NominatesEditor(nominates, description);
    row.add(editor);

    final JButton delete = new JButton("Delete Nominates");
    delete.addActionListener(l -> {
      mSubjectiveCategory.removeNominates(nominates);
      GuiUtils.removeFromContainer(nominatesContainer, row);
    });
    row.add(delete);

    row.add(Box.createHorizontalGlue());

    GuiUtils.addToContainer(nominatesContainer, row);
  }

}
