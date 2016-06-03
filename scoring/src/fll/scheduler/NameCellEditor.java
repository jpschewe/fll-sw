/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import javax.swing.DefaultCellEditor;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 * Edit strings that cannot be empty.
 */
/* package */ final class NameCellEditor extends DefaultCellEditor {

  public NameCellEditor() {
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
    final String value = getComponent().getText();
    if (value.isEmpty()
        || value.contains(",")) {
      JOptionPane.showMessageDialog(getComponent(), "Names cannot be empty and cannot contain commas", "Error",
                                    JOptionPane.WARNING_MESSAGE);
      return false;
    } else {
      return super.stopCellEditing();
    }
  }
}