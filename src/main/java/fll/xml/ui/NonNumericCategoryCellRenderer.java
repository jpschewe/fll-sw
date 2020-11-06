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

import fll.xml.NonNumericCategory;

/**
 * Render {@link NonNumericCategory} objects as their title.
 */
/* package */ final class NonNumericCategoryCellRenderer implements ListCellRenderer<NonNumericCategory> {

  private final DefaultListCellRenderer delegate = new DefaultListCellRenderer();

  @Override
  public Component getListCellRendererComponent(final JList<? extends NonNumericCategory> list,
                                                final NonNumericCategory value,
                                                final int index,
                                                final boolean isSelected,
                                                final boolean cellHasFocus) {
    return delegate.getListCellRendererComponent(list, null == value ? "" : value.getTitle(), index, isSelected,
                                                 cellHasFocus);
  }

}