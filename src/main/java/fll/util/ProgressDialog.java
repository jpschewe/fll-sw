/*
 * Copyright (c) 2015 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

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

  private JLabel mNote;

  /**
   * @param owner {@link JDialog#getOwner()}
   * @param title {@link JDialog#getTitle()}
   */
  public ProgressDialog(final Frame owner,
                        final String title) {
    super(owner, title, true);

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

    addComponentListener(new ComponentAdapter() {
      public void componentShown(ComponentEvent e) {
        // reset the canceled flag
        setCanceled(false);
      }
    });

    pack();
  }

  /**
   * @param note The text displayed above the progress bar.
   */
  public void setNote(final String note) {
    mNote.setText(note);
    pack();
  }

  private boolean mCanceled;

  /**
   * @return Check if the cancel button has been pressed.
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

}
