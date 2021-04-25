/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.util.ChooseOptionDialog;
import fll.util.FLLInternalException;
import fll.xml.ChallengeDescription;
import fll.xml.NonNumericCategory;
import fll.xml.SubjectiveScoreCategory;

/**
 * Editor for {@link SubjectiveScoreCategory.Nominates} objects.
 */
@SuppressFBWarnings(value = { "SE_BAD_FIELD",
                              "SE_BAD_FIELD_STORE" }, justification = "This class isn't going to be serialized")
/* package */ class NominatesEditor extends Box {

  private final SubjectiveScoreCategory.Nominates nominates;

  private final JButton editor;

  /**
   * @param nominates the object to edit
   * @param description used to get the list of non-numeric categories
   */
  /* package */ NominatesEditor(final SubjectiveScoreCategory.Nominates nominates,
                                final ChallengeDescription description) {
    super(BoxLayout.LINE_AXIS);

    this.nominates = nominates;

    editor = new JButton(nominates.getNonNumericCategoryTitle());
    add(editor);
    final NonNumericCategory category = description.getNonNumericCategoryByTitle(nominates.getNonNumericCategoryTitle());
    if (null == category) {
      throw new FLLInternalException("Cannot find non-numeric category with title "
          + nominates.getNonNumericCategoryTitle());
    }
    category.addPropertyChangeListener(titleListener);

    editor.setToolTipText("Click to change the referenced non-numeric category");
    editor.addActionListener(l -> {
      final Collection<NonNumericCategory> categories = description.getNonNumericCategories();

      final ChooseOptionDialog<NonNumericCategory> dialog = new ChooseOptionDialog<>(JOptionPane.getRootFrame(),
                                                                                     new LinkedList<>(categories),
                                                                                     new NonNumericCategoryCellRenderer());
      dialog.setVisible(true);
      final NonNumericCategory selected = dialog.getSelectedValue();
      if (null != selected) {
        final NonNumericCategory oldCategory = description.getNonNumericCategoryByTitle(nominates.getNonNumericCategoryTitle());
        if (null == oldCategory) {
          throw new FLLInternalException("Cannot find non-numeric category with title "
              + nominates.getNonNumericCategoryTitle());
        }
        oldCategory.removePropertyChangeListener(titleListener);

        nominates.setNonNumericCategoryName(selected.getTitle());
        editor.setText(selected.getTitle());

        selected.addPropertyChangeListener(titleListener);
      }
    });

  }

  private final TitleChangeListener titleListener = new TitleChangeListener();

  private class TitleChangeListener implements PropertyChangeListener {

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
      if ("title".equals(evt.getPropertyName())) {
        final String newTitle = (String) evt.getNewValue();
        nominates.setNonNumericCategoryName(newTitle);
        editor.setText(newTitle);
      }
    }
  }

}
