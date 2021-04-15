/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Text editor that scrolls.
 */
public class TextAreaEditor extends JPanel {

  private final JTextArea editor;

  /**
   * @param numRows the number of rows to display
   * @param numColumns the number of columns to display
   * @see JTextArea#JTextArea(int, int)
   */
  public TextAreaEditor(final int numRows,
                        final int numColumns) {
    super(new BorderLayout());

    editor = new JTextArea(numRows, numColumns);

    final JScrollPane scroll = new JScrollPane(editor);
    add(scroll, BorderLayout.CENTER);
  }

  /**
   * @return the current text
   * @see JTextArea#getText()
   */
  public String getText() {
    return editor.getText();
  }

  /**
   * @param text the text to display
   * @see JTextArea#setText(String)
   */
  public void setText(final @Nullable String text) {
    editor.setText(text);
  }
}
