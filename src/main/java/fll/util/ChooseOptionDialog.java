/*
 * Copyright (c) 2017 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;
import java.util.List;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.ListCellRenderer;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Choose a single option from a list of options.
 * This is a modal dialog.
 * 
 * @param <E> the type of objects in the dialog
 */
public class ChooseOptionDialog<E> extends JDialog {

  private @Nullable E selected = null;

  /**
   * @param parent parent frame needed for modal operation
   * @param options the options to choose from
   * @param renderer the renderer to use, may be null
   */
  public ChooseOptionDialog(final Frame parent,
                            final List<E> options,
                            final @Nullable ListCellRenderer<E> renderer) {
    super(parent, true);
    final Container cpane = getContentPane();
    cpane.setLayout(new BorderLayout());

    final JComboBox<E> combo = new JComboBox<>(new Vector<E>(options));
    cpane.add(combo, BorderLayout.CENTER);
    if (null != renderer) {
      combo.setRenderer(renderer);
    }

    final Box buttonBox = Box.createHorizontalBox();
    cpane.add(buttonBox, BorderLayout.SOUTH);

    final JButton ok = new JButton("Ok");
    ok.addActionListener(e -> {
      selected = combo.getItemAt(combo.getSelectedIndex());
      ChooseOptionDialog.this.setVisible(false);
    });
    buttonBox.add(ok);

    final JButton cancel = new JButton("Cancel");
    cancel.addActionListener(e -> {
      selected = null;
      ChooseOptionDialog.this.setVisible(false);
    });
    buttonBox.add(cancel);

    buttonBox.add(Box.createHorizontalGlue());

    pack();
  }

  /**
   * @return the selected value, null if canceled or still open
   */
  public @Nullable E getSelectedValue() {
    return selected;
  }

}
