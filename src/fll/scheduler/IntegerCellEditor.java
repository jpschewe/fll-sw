/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.scheduler;

import javax.swing.DefaultCellEditor;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.apache.log4j.Logger;

import fll.util.LogUtils;

/*package*/ final class IntegerCellEditor extends DefaultCellEditor {

  private static final Logger LOGGER = LogUtils.getLogger();

  private final int minValue;

  private final int maxValue;

  /**
   * Create a cell editor for integers.
   * 
   * @param min the minimum allowed value (inclusive)
   * @param max the maximum allowed value (inclusive)
   */
  public IntegerCellEditor(final int min,
                           final int max) {
    super(new JTextField());
    minValue = min;
    maxValue = max;
  }

  @Override
  public JTextField getComponent() {
    return (JTextField) super.getComponent();
  }

  @Override
  public Object getCellEditorValue() {
    final String str = super.getCellEditorValue().toString();
    final Integer value = Integer.parseInt(str);
    return value;
  }

  @Override
  public boolean stopCellEditing() {
    final String str = getComponent().getText();
    boolean stop;
    try {
      final Integer value = Integer.parseInt(str);
      if (minValue <= value
          && value <= maxValue) {
        stop = true;
      } else {
        stop = false;
      }
    } catch (final NumberFormatException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Could not parse as integer", e);
      }
      stop = false;
    }

    if (stop) {
      return super.stopCellEditing();
    } else {
      JOptionPane.showMessageDialog(getComponent(),
                                    String.format("You must enter an integer between %d and %d", minValue, maxValue),
                                    "Error", JOptionPane.WARNING_MESSAGE);
      return false;
    }
  }

}