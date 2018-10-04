/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.text.ParseException;

import javax.swing.Box;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;

import org.apache.log4j.Logger;

import fll.util.FormatterUtils;
import fll.util.LogUtils;
import fll.xml.SubjectiveScoreCategory;

/**
 * 
 */
public class SubjectiveCategoryEditor extends ScoreCategoryEditor {

  private static final Logger LOGGER = LogUtils.getLogger();

  private final JFormattedTextField mTitleEditor;

  private final JFormattedTextField mNameEditor;

  private final SubjectiveScoreCategory mSubjectiveCategory;

  public SubjectiveCategoryEditor(final SubjectiveScoreCategory category) {
    super(category);
    mSubjectiveCategory = category;

    final Box titleContainer = Box.createHorizontalBox();
    add(titleContainer, 0);
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
    add(nameContainer, 1);
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
  }

  protected void fireTitleChange(final String oldTitle,
                                 final String newTitle) {
    firePropertyChange("title", oldTitle, newTitle);
  }

}
