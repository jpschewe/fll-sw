/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.log4j.Logger;

import fll.scheduler.SchedulerUI;
import fll.subjective.SubjectiveFrame;
import fll.util.GuiExceptionHandler;
import fll.util.LogUtils;
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
      frame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(final WindowEvent e) {
          System.exit(0);
        }
        @Override
        public void windowClosed(final WindowEvent e) {
          System.exit(0);
        }
      });
      // should be able to watch for window closing, but hidden works
      frame.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentHidden(final ComponentEvent e) {
          System.exit(0);
        }
      });
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

    final JButton subjectiveButton = new JButton("Subjective Application");
    subjectiveButton.addActionListener(ae -> {
      launchSubjective();
    });
    cpane.add(subjectiveButton);

    final JButton exit = new JButton("Exit");
    exit.addActionListener(ae -> {
      System.exit(0);
    });
    cpane.add(exit);

    pack();
  }

  private SchedulerUI scheduler = null;

  private void launchScheduler() {
    if (null != scheduler) {
      scheduler.setVisible(true);
    } else {
      try {
        scheduler = new SchedulerUI();
        scheduler.addWindowListener(new WindowAdapter() {
          @Override
          public void windowClosing(final WindowEvent e) {
            scheduler = null;
          }
          @Override
          public void windowClosed(final WindowEvent e) {
            scheduler = null;
          }
        });
        // should be able to watch for window closing, but hidden works
        scheduler.addComponentListener(new ComponentAdapter() {
          @Override
          public void componentHidden(final ComponentEvent e) {
            scheduler = null;
          }
        });

        GraphicsUtils.centerWindow(scheduler);
        scheduler.setVisible(true);
      } catch (final Exception e) {
        LOGGER.fatal("Unexpected error", e);
        JOptionPane.showMessageDialog(null, "Unexpected error: "
            + e.getMessage(), "Error Launching Scheduler", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private SubjectiveFrame subjective = null;

  private void launchSubjective() {
    if (null != subjective) {
      subjective.setVisible(true);
    } else {
      try {
        subjective = new SubjectiveFrame();
        
        subjective.addWindowListener(new WindowAdapter() {
          @Override
          public void windowClosing(final WindowEvent e) {
            subjective = null;
          }
          @Override
          public void windowClosed(final WindowEvent e) {
            subjective = null;
          }
        });
        // should be able to watch for window closing, but hidden works
        subjective.addComponentListener(new ComponentAdapter() {
          @Override
          public void componentHidden(final ComponentEvent e) {
            subjective = null;
          }
        });
        
        GraphicsUtils.centerWindow(subjective);
        subjective.setVisible(true);
        subjective.promptForFile();
      } catch (final Exception e) {
        LOGGER.fatal("Unexpected error", e);
        JOptionPane.showMessageDialog(null, "Unexpected error: "
            + e.getMessage(), "Error Launching Scheduler", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

}
