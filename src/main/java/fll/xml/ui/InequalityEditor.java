/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import fll.xml.InequalityComparison;

/**
 * Editor to select {@link InequalityComparison} objects.
 */
class InequalityEditor extends JComboBox<InequalityComparison> {

  InequalityEditor(final InequalityComparison[] items) {
    super(items);
    setRenderer(Renderer.INSTANCE);
  }

  private static final class Renderer implements ListCellRenderer<InequalityComparison> {

    public static final Renderer INSTANCE = new Renderer();

    private final DefaultListCellRenderer delegate = new DefaultListCellRenderer();

    @Override
    public Component getListCellRendererComponent(final JList<? extends InequalityComparison> list,
                                                  final InequalityComparison value,
                                                  final int index,
                                                  final boolean isSelected,
                                                  final boolean cellHasFocus) {
      return delegate.getListCellRendererComponent(list, value.getDisplay(), index, isSelected, cellHasFocus);
    }

  }
}
