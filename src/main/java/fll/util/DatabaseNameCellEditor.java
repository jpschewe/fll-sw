/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.util.regex.Pattern;

import javax.swing.DefaultCellEditor;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 * Edit cells in a table enforcing the rules of a database name. Must start with
 * a character and only allow letters, numbers and underscore after that.
 */
public final class DatabaseNameCellEditor extends DefaultCellEditor {

  private static final String DATABASE_NAME_REGEXP = "[a-zA-Z]\\w*";

  /**
   * Regular expression for valid database names.
   * The XML pattern is "\p{L}[\p{L}\p{Nd}_]*".
   */
  public static final Pattern DATABASE_NAME_PATTERN = Pattern.compile(DATABASE_NAME_REGEXP);

  /**
   * Create a cell editor for database name.
   */
  public DatabaseNameCellEditor() {
    super(new JTextField());
  }

  @Override
  public JTextField getComponent() {
    return (JTextField) super.getComponent();
  }

  @Override
  public Object getCellEditorValue() {
    final String str = super.getCellEditorValue().toString();
    return str;
  }

  @Override
  public boolean stopCellEditing() {
    final String str = getComponent().getText();
    boolean stop;
    if (DATABASE_NAME_PATTERN.matcher(str).matches()) {
      stop = true;
    } else {
      stop = false;
    }

    if (stop) {
      return super.stopCellEditing();
    } else {
      JOptionPane.showMessageDialog(getComponent(),
                                    "The string must start with a character and then only have letters, numbers and underscores after that",
                                    "Error", JOptionPane.WARNING_MESSAGE);
      return false;
    }
  }

}