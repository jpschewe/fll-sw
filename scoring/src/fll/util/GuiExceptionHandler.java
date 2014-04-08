/*
 * Copyright (c) 2014 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.lang.Thread.UncaughtExceptionHandler;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Display exception information in a dialog and then exit when the dialog is
 * closed.
 * Setup with
 * {@link Thread#setUncaughtExceptionHandler(UncaughtExceptionHandler)}.
 */
public class GuiExceptionHandler implements UncaughtExceptionHandler {

  private static final Logger LOGGER = LogUtils.getLogger();

  public GuiExceptionHandler() {
  }

  /**
   * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread,
   *      java.lang.Throwable)
   */
  @Override
  @SuppressFBWarnings(value = { "DM_EXIT" }, justification = "We want the application to exit after an error")
  public void uncaughtException(final Thread t,
                                final Throwable ex) {
    final String message = String.format("Unexpected error on thread %s: %s", t.getName(), ex.getMessage());
    LOGGER.fatal(message, ex);
    JOptionPane.showMessageDialog(null, message, "Unexpected Error", JOptionPane.ERROR_MESSAGE);
    System.exit(1);
  }

}
