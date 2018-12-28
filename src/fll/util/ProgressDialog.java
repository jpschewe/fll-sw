/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.ProgressMonitor;

/**
 * A modal indeterminate progress dialog.
 * If a determinate progress dialog is desired, use {@link ProgressMonitor}
 */
public class ProgressDialog extends JDialog implements CheckCanceled {

  private final Object mLock = new Object();

  public ProgressDialog(final Dialog owner) {
    super(owner, true);

    initComponents();
  }

  public ProgressDialog(final Dialog owner,
                        final String title) {
    super(owner, title, true);

    initComponents();
  }

  public ProgressDialog(final Frame owner) {
    super(owner, true);

    initComponents();
  }

  public ProgressDialog(final Frame owner,
                        final String title) {
    super(owner, title, true);

    initComponents();
  }

  public ProgressDialog(final Window owner) {
    super(owner, Dialog.ModalityType.APPLICATION_MODAL);

    initComponents();
  }

  public ProgressDialog(final Window owner,
                        final String title) {
    super(owner, title, Dialog.ModalityType.APPLICATION_MODAL);

    initComponents();
  }

  private JLabel mNote;

  private void initComponents() {
    final Container cpane = getContentPane();
    cpane.setLayout(new BorderLayout());

    mNote = new JLabel();
    cpane.add(mNote, BorderLayout.NORTH);

    final JProgressBar progress = new JProgressBar();
    progress.setIndeterminate(true);
    cpane.add(progress, BorderLayout.CENTER);

    final JButton cancel = new JButton("Cancel");
    cpane.add(cancel, BorderLayout.SOUTH);
    cancel.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent ae) {
        setCanceled(true);
      }
    });

    pack();
  }

  /**
   * The text displayed above the progress bar.
   */
  public void setNote(final String note) {
    mNote.setText(note);
    pack();
  }

  private boolean mCanceled;

  /**
   * Check if the cancel button has been pressed.
   */
  public boolean isCanceled() {
    synchronized (mLock) {
      return mCanceled;
    }
  }

  private void setCanceled(final boolean v) {
    synchronized (mLock) {
      mCanceled = v;
    }
  }

  /**
   * Overridden to reset the canceled flag.
   * 
   * @see java.awt.Dialog#setVisible(boolean)
   */
  @Override
  public void setVisible(final boolean value) {
    if (value) {
      setCanceled(false);
    }

    super.setVisible(value);
  }

}
