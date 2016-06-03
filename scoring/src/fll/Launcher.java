/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

import java.awt.Container;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.log4j.Logger;

import fll.scheduler.SchedulerUI;
import fll.util.GuiExceptionHandler;
import fll.util.LogUtils;
import net.mtu.eggplant.util.gui.BasicWindowMonitor;
import net.mtu.eggplant.util.gui.GraphicsUtils;

/**
 * Launcher for fll-sw applications.
 */
public class Launcher extends JFrame {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static void main(final String[] args) {
    LogUtils.initializeLogging();

    Thread.setDefaultUncaughtExceptionHandler(new GuiExceptionHandler());

    // Use cross platform look and feel so that things look right all of the
    // time
    try {
      UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
    } catch (final ClassNotFoundException e) {
      LOGGER.warn("Could not find cross platform look and feel class", e);
    } catch (final InstantiationException e) {
      LOGGER.warn("Could not instantiate cross platform look and feel class", e);
    } catch (final IllegalAccessException e) {
      LOGGER.warn("Error loading cross platform look and feel", e);
    } catch (final UnsupportedLookAndFeelException e) {
      LOGGER.warn("Cross platform look and feel unsupported?", e);
    }

    try {
      final Launcher frame = new Launcher();

      frame.addWindowListener(new BasicWindowMonitor());
      GraphicsUtils.centerWindow(frame);

      frame.setVisible(true);
    } catch (final Exception e) {
      LOGGER.fatal("Unexpected error", e);
      JOptionPane.showMessageDialog(null, "Unexpected error: "
          + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }
  }

  public Launcher() {
    super();

    final Container cpane = getContentPane();
    cpane.setLayout(new GridLayout(0, 2));

    final JButton schedulerButton = new JButton("Scheduler");
    schedulerButton.addActionListener(ae -> {
      launchScheduler();
    });
    cpane.add(schedulerButton);

    pack();
  }

  private SchedulerUI scheduler = null;

  private void launchScheduler() {
    if (null != scheduler) {
      scheduler.setVisible(true);
    } else {
      try {
        scheduler = new SchedulerUI();
        GraphicsUtils.centerWindow(scheduler);
        scheduler.setVisible(true);
      } catch (final Exception e) {
        LOGGER.fatal("Unexpected error", e);
        JOptionPane.showMessageDialog(null, "Unexpected error: "
            + e.getMessage(), "Error Launching Scheduler", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

}
