/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;

/**
 * Edit strings that cannot be empty.
 */
/*package*/ final class NameCellEditor extends DefaultCellEditor {

  public NameCellEditor() {
    super(new JTextField());
  }

  @Override
  public boolean stopCellEditing() {
    final String value = getCellEditorValue().toString();
    if (value.isEmpty()) {
      return false;
    } else {
      return true;
    }
  }
}