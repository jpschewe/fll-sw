/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.checkerframework.checker.nullness.qual.Nullable;

import fll.xml.ChallengeDescription;
import fll.xml.ScoreCategory;
import fll.xml.SubjectiveScoreCategory;

/**
 * Map subjective headers to subjective categories. Also map file suffixes.
 * This dialog is modal.
 */
/* package */ class MapSubjectiveHeaders extends JDialog {

  private final Map<ScoreCategory, JTextField> filenameSuffixes = new HashMap<>();

  private boolean canceled = true;

  /**
   * @return Was cancel pressed on the dialog?
   */
  public boolean isCanceled() {
    return canceled;
  }

  MapSubjectiveHeaders(final Frame owner,
                       final ChallengeDescription description) {
    super(owner, true);

    getContentPane().setLayout(new BorderLayout());

    final JTextArea instructions = new JTextArea("Specify the filename suffix for each category. Leave empty for no suffix.");
    instructions.setEditable(false);
    instructions.setWrapStyleWord(true);
    instructions.setLineWrap(true);
    getContentPane().add(instructions, BorderLayout.NORTH);

    final JPanel grid = new JPanel(new GridLayout(0, 2));
    getContentPane().add(grid, BorderLayout.CENTER);

    grid.add(new JLabel("Subjective Category"));
    grid.add(new JLabel("Filename suffix for category"));

    for (final SubjectiveScoreCategory category : description.getSubjectiveCategories()) {
      grid.add(new JLabel(category.getTitle()));

      final String defaultText = DEFAULT_CATEGORY_SUFFIXES.get(category.getName());
      final JTextField filenameSuffix = new JTextField(defaultText);
      grid.add(filenameSuffix);
      filenameSuffixes.put(category, filenameSuffix);
    }

    final Box buttonBox = Box.createHorizontalBox();
    getContentPane().add(buttonBox, BorderLayout.SOUTH);

    final JButton ok = new JButton("OK");
    buttonBox.add(ok);
    ok.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        setVisible(false);
        canceled = false;
      }
    });

    final JButton cancel = new JButton("Cancel");
    buttonBox.add(cancel);
    cancel.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent ae) {
        setVisible(false);
        canceled = true;
      }
    });

    // setMinimumSize(getPreferredSize());
    pack();
  }

  /**
   * Get filename suffix for category.
   * 
   * @param category what to find
   * @return null if not found or no suffix
   */
  public @Nullable String getFilenameSuffixForCategory(final ScoreCategory category) {
    final JTextField widget = filenameSuffixes.get(category);
    if (null != widget) {
      final String text = widget.getText();
      if (null == text
          || "".equals(text.trim())) {
        return null;
      } else {
        return text.trim();
      }
    } else {
      return null;
    }
  }

  /**
   * Map category names to file suffixes.
   */
  private static final Map<String, String> DEFAULT_CATEGORY_SUFFIXES;
  static {
    final Map<String, String> defaultCategorySuffixes = new HashMap<>();
    defaultCategorySuffixes.put("project", "LtBeigePaper");
    defaultCategorySuffixes.put("core_values", "LtPinkPaper");
    defaultCategorySuffixes.put("robot_design", "LtBluePaper");
    defaultCategorySuffixes.put("robot_programming", "LtPurplePaper");
    DEFAULT_CATEGORY_SUFFIXES = Collections.unmodifiableMap(defaultCategorySuffixes);
  }

}
