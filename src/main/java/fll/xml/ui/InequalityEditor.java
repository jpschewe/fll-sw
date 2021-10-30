/*
 * Copyright (c) 2021 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.xml.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import fll.xml.InequalityComparison;

/**
 * Editor to select {@link InequalityComparison} objects.
 */
class InequalityEditor extends JPanel {

  private final JComboBox<InequalityComparison> select;

  InequalityEditor(final InequalityComparison[] items) {
    super(new BorderLayout());

    final JLabel label = new JLabel("is ");
    add(label, BorderLayout.WEST);

    select = new JComboBox<>(items);
    add(select, BorderLayout.CENTER);
    select.setRenderer(Renderer.INSTANCE);
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

  /**
   * @param l the listener
   * @see JComboBox#addActionListener(java.awt.event.ActionListener)
   */
  public void addActionListener(final ActionListener l) {
    select.addActionListener(l);
  }

  /**
   * @param comparison the selected item
   *          see {@link JComboBox#setSelectedItem(Object)}
   */
  public void setSelectedItem(final InequalityComparison comparison) {
    select.setSelectedItem(comparison);
  }

  /**
   * @return the selected object
   */
  public InequalityComparison getSelectedItem() {
    return (InequalityComparison) select.getSelectedItem();
  }
}
