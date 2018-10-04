/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import org.apache.log4j.Logger;

import fll.Utilities;
import fll.util.FormatterUtils;
import fll.util.LogUtils;
import fll.xml.ChallengeDescription;
import fll.xml.SubjectiveScoreCategory;
import fll.xml.WinnerType;
import fll.xml.ui.MovableExpandablePanel.DeleteEvent;
import fll.xml.ui.MovableExpandablePanel.DeleteEventListener;
import fll.xml.ui.MovableExpandablePanel.MoveEvent;
import fll.xml.ui.MovableExpandablePanel.MoveEvent.MoveDirection;
import fll.xml.ui.MovableExpandablePanel.MoveEventListener;

/**
 * Editor for {@link ChallengeDescription} objects.
 */
public class ChallengeDescriptionEditor extends JPanel {

  private static final Logger LOGGER = LogUtils.getLogger();

  private final ChallengeDescription mDescription;

  /**
   * @return the description being edited
   */
  public ChallengeDescription getDescription() {
    return mDescription;
  }

  private final JFormattedTextField mTitleEditor;

  private final JFormattedTextField mRevisionEditor;

  private final JFormattedTextField mCopyrightEditor;

  private final JComboBox<WinnerType> mWinnerEditor;

  private final PerformanceEditor mPerformanceEditor;

  private final List<SubjectiveCategoryEditor> mSubjectiveEditors = new LinkedList<>();

  private final JComponent mSubjectiveContainer;

  private final MoveEventListener mSubjectiveMoveListener;

  private final DeleteEventListener mSubjectiveDeleteListener;

  public ChallengeDescriptionEditor(@Nonnull final ChallengeDescription description) {
    super(new BorderLayout());
    this.mDescription = description;

    final JComponent topPanel = Box.createVerticalBox();
    add(topPanel, BorderLayout.CENTER);
    topPanel.setAlignmentX(LEFT_ALIGNMENT);

    // properties specific to the challenge description
    final JPanel challengePanel = new JPanel(new GridBagLayout());
    topPanel.add(challengePanel);

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    challengePanel.add(new JLabel("Title: "), gbc);

    mTitleEditor = FormatterUtils.createStringField();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    challengePanel.add(mTitleEditor, gbc);
    
    mTitleEditor.addPropertyChangeListener("value", e -> {
      if (null != mDescription) {
        mDescription.setTitle(mTitleEditor.getText());
      }
    });

    mTitleEditor.setColumns(80);
    mTitleEditor.setMaximumSize(mTitleEditor.getPreferredSize());
    mTitleEditor.setValue(mDescription.getTitle());                                                                                   

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    challengePanel.add(new JLabel("Revision: "), gbc);

    mRevisionEditor = FormatterUtils.createStringField();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    challengePanel.add(mRevisionEditor, gbc);

    mRevisionEditor.addPropertyChangeListener("value", e -> {
      if (null != mDescription) {
        mDescription.setRevision(mRevisionEditor.getText());
      }
    });

    mRevisionEditor.setColumns(20);
    mRevisionEditor.setMaximumSize(mRevisionEditor.getPreferredSize());
    mRevisionEditor.setValue(mDescription.getRevision());                                                                             

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    challengePanel.add(new JLabel("Copyright: "), gbc);

    mCopyrightEditor = FormatterUtils.createStringField();
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    challengePanel.add(mCopyrightEditor, gbc);

    mCopyrightEditor.addPropertyChangeListener("value", e -> {
      if (null != mDescription) {
        mDescription.setCopyright(mCopyrightEditor.getText());
      }
    });

    mCopyrightEditor.setColumns(80);
    mCopyrightEditor.setMaximumSize(mCopyrightEditor.getPreferredSize());
    mCopyrightEditor.setValue(mDescription.getCopyright());                                                                           

    gbc = new GridBagConstraints();
    gbc.weightx = 0;
    gbc.anchor = GridBagConstraints.FIRST_LINE_END;
    challengePanel.add(new JLabel("Best score: "), gbc);

    mWinnerEditor = new JComboBox<>(WinnerType.values());
    gbc = new GridBagConstraints();
    gbc.weightx = 1;
    gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    challengePanel.add(mWinnerEditor, gbc);

    mWinnerEditor.addActionListener(e -> {
      if (null != mDescription) {
        final WinnerType winner = mWinnerEditor.getItemAt(mWinnerEditor.getSelectedIndex());
        mDescription.setWinner(winner);
      }
    });
    mWinnerEditor.setSelectedItem(mDescription.getWinner());      

    // child elements of the challenge description
    mPerformanceEditor = new PerformanceEditor(mDescription.getPerformance());
    mPerformanceEditor.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
    final MovableExpandablePanel performance = new MovableExpandablePanel("Performance", mPerformanceEditor, false,
                                                                          false);
    topPanel.add(performance);

    final Box subjectiveTopContainer = Box.createVerticalBox();
    topPanel.add(subjectiveTopContainer);

    subjectiveTopContainer.setBorder(BorderFactory.createTitledBorder("Subjective"));

    final Box subjectiveButtonBox = Box.createHorizontalBox();
    subjectiveTopContainer.add(subjectiveButtonBox);

    final JButton addSubjectiveCategory = new JButton("Add Subjective Category");
    subjectiveButtonBox.add(addSubjectiveCategory);
    addSubjectiveCategory.addActionListener(l -> addNewSubjectiveCategory());

    subjectiveButtonBox.add(Box.createHorizontalGlue());

    mSubjectiveContainer = Box.createVerticalBox();
    subjectiveTopContainer.add(mSubjectiveContainer);

    mSubjectiveMoveListener = new MoveEventListener() {

      @Override
      public void requestedMove(final MoveEvent e) {
        final int oldIndex = Utilities.getIndexOfComponent(mSubjectiveContainer, e.getComponent());
        if (oldIndex < 0) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Unable to find source of move event in subjective container");
          }
          return;
        }

        final int newIndex;
        if (e.getDirection() == MoveDirection.DOWN) {
          newIndex = oldIndex
              + 1;
        } else {
          newIndex = oldIndex
              - 1;
        }

        if (newIndex < 0
            || newIndex >= mSubjectiveContainer.getComponentCount()) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Can't move component outside the container oldIndex: "
                + oldIndex
                + " newIndex: "
                + newIndex);
          }
          return;
        }

        // update editor list
        final SubjectiveCategoryEditor editor = mSubjectiveEditors.remove(oldIndex);
        mSubjectiveEditors.add(newIndex, editor);

        // update the UI
        mSubjectiveContainer.add(editor, newIndex);
        mSubjectiveContainer.validate();

        // update the order in the challenge description
        final SubjectiveScoreCategory category = mDescription.removeSubjectiveCategory(oldIndex);
        mDescription.addSubjectiveCategory(newIndex, category);
      }
    };

    mSubjectiveDeleteListener = new DeleteEventListener() {

      @Override
      public void requestDelete(final DeleteEvent e) {
        final int confirm = JOptionPane.showConfirmDialog(ChallengeDescriptionEditor.this,
                                                          "Are you sure that you want to delete the subjective category?",
                                                          "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
          return;
        }

        final int index = Utilities.getIndexOfComponent(mSubjectiveContainer, e.getComponent());
        if (index < 0) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Unable to find source of delete event in subjective category container");
          }
          return;
        }

        // update editor list
        mSubjectiveEditors.remove(index);

        // update the challenge description
        mDescription.removeSubjectiveCategory(index);

        // update the UI
        mSubjectiveContainer.remove(index);
        mSubjectiveContainer.validate();
      }
    };

    mDescription.getSubjectiveCategories().forEach(this::addSubjectiveCategory);

    // fill in the bottom of the panel
    topPanel.add(Box.createVerticalGlue());

  }

  private void addNewSubjectiveCategory() {
    final String name = String.format("category_%d", mSubjectiveEditors.size());
    final String title = String.format("Category %d", mSubjectiveEditors.size());

    final SubjectiveScoreCategory cat = new SubjectiveScoreCategory(name, title);
    mDescription.addSubjectiveCategory(cat);

    addSubjectiveCategory(cat);
  }

  private void addSubjectiveCategory(final SubjectiveScoreCategory cat) {
    final SubjectiveCategoryEditor editor = new SubjectiveCategoryEditor(cat);
    editor.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

    final MovableExpandablePanel container = new MovableExpandablePanel(cat.getTitle(), editor, true, true);
    container.addMoveEventListener(mSubjectiveMoveListener);
    container.addDeleteEventListener(mSubjectiveDeleteListener);

    editor.addPropertyChangeListener("title", e -> {
      final String newTitle = (String) e.getNewValue();
      container.setTitle(newTitle);
    });

    mSubjectiveContainer.add(container);
    mSubjectiveContainer.validate();

    mSubjectiveEditors.add(editor);
  }

  /**
   * Force any pending edits to complete.
   */
  public void commitChanges() {
    try {
      mTitleEditor.commitEdit();
    } catch (final ParseException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Got parse exception committing title changes, assuming bad value and ignoring", e);
      }
    }

    try {
      mRevisionEditor.commitEdit();
    } catch (final ParseException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Got parse exception committing revision changes, assuming bad value and ignoring", e);
      }
    }

    try {
      mCopyrightEditor.commitEdit();
    } catch (final ParseException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Got parse exception committing copyright changes, assuming bad value and ignoring", e);
      }
    }

    mPerformanceEditor.commitChanges();
    mSubjectiveEditors.forEach(e -> e.commitChanges());
  }

}
