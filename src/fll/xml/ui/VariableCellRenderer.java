/*
 * Copyright (c) 2018 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import fll.xml.Variable;

/**
 * Render {@link Variable} objects as their name.
 */
/* package */ final class VariableCellRenderer implements ListCellRenderer<Variable> {

  private final DefaultListCellRenderer delegate = new DefaultListCellRenderer();

  @Override
  public Component getListCellRendererComponent(final JList<? extends Variable> list,
                                                final Variable value,
                                                final int index,
                                                final boolean isSelected,
                                                final boolean cellHasFocus) {
    return delegate.getListCellRendererComponent(list, value.getName(), index, isSelected, cellHasFocus);
  }

}