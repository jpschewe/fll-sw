/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

import java.awt.Container;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import org.apache.log4j.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.scheduler.SchedulerUI;
import fll.subjective.SubjectiveFrame;
import fll.util.FLLInternalException;
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
        @SuppressFBWarnings(value = { "DM_EXIT" }, justification = "Exiting from main is OK")
        public void windowClosing(final WindowEvent e) {
          System.exit(0);
        }

        @Override
        @SuppressFBWarnings(value = { "DM_EXIT" }, justification = "Exiting from main is OK")
        public void windowClosed(final WindowEvent e) {
          System.exit(0);
        }
      });
      // should be able to watch for window closing, but hidden works
      frame.addComponentListener(new ComponentAdapter() {
        @Override
        @SuppressFBWarnings(value = { "DM_EXIT" }, justification = "Exiting from main is OK")
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
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

    final Container cpane = getContentPane();
    cpane.setLayout(new GridLayout(0, 2));

    final JButton webserverStartButton = new JButton("Start Webserver");
    webserverStartButton.addActionListener(ae -> {
      controlWebserver(true);
    });
    cpane.add(webserverStartButton);

    final JButton webserverStopButton = new JButton("Stop Webserver");
    webserverStopButton.addActionListener(ae -> {
      controlWebserver(false);
    });
    cpane.add(webserverStopButton);

    final JButton mainPage = new JButton("Visit the main web page");
    mainPage.addActionListener(ae -> {
      loadFllHtml();
    });
    cpane.add(mainPage);

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
      setVisible(false);
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

  /**
   * Find tomcat directory.
   * 
   * @return the location or null if it cannot be determined
   */
  private String findTomcatDirectory() {
    final String[] possibleLocations = { "tomcat", "../tomcat", "build/tomcat", "scoring/build/tomcat" };
    for (final String location : possibleLocations) {
      final File f = new File(location);
      if (f.exists()
          && f.isDirectory()) {
        return f.getAbsolutePath();
      }
    }
    return null;
  }

  private transient Thread webserverThread = null;

  private void controlWebserver(final boolean start) {
    if (start
        && null != webserverThread) {
      JOptionPane.showMessageDialog(this, "Webserver has already been told to start");
      return;
    }

    final String tomcatDir = findTomcatDirectory();
    if (null == tomcatDir) {
      LOGGER.error("Cannot find tomcat directory");
      JOptionPane.showMessageDialog(this, "Cannot find tomcat directory", "Error launching webserver",
                                    JOptionPane.ERROR_MESSAGE);
    }
    final boolean windows = System.getProperty("os.name").startsWith("Windows");

    if (start) {
      webserverThread = new Thread(() -> {
        try {
          if (windows) {
            Runtime.getRuntime().exec(String.format("cmd /c start %s/bin/startup.bat", tomcatDir));
          } else {
            Runtime.getRuntime().exec(String.format("%s/bin/startup.sh", tomcatDir));
          }
        } catch (final IOException e) {
          throw new FLLInternalException("Could not start tomcat", e);
        }

        // TODO: invoke via java rather than system call

        webserverThread = null;
      });

      webserverThread.setDaemon(true);

      webserverThread.start();

      loadFllHtml();

    } else {
      try {
        if (windows) {
          Runtime.getRuntime().exec(String.format("cmd /c start %s/bin/shutdown.bat", tomcatDir));
        } else {
          Runtime.getRuntime().exec(String.format("%s/bin/shutdown.sh", tomcatDir));
        }
      } catch (final IOException e) {
        throw new FLLInternalException("Could not stop tomcat", e);
      }

    }
  }

  /**
   * Open up the main page.
   */
  private void loadFllHtml() {
    final String fllHtml = getFLLHtmlFile();
    if (null == fllHtml) {
      JOptionPane.showMessageDialog(this, "Cannot find fll-sw.html, you will need to open this is your browser.");
      return;
    }
    final File htmlFile = new File(fllHtml);
    try {
      Desktop.getDesktop().browse(htmlFile.toURI());
    } catch (final IOException e) {
      LOGGER.error("Unable to open web browser", e);
      JOptionPane.showMessageDialog(this, "Cannot open fll-sw.html, you will need to open this is your browser.");
    }
  }

  /**
   * Find the path to fll-sw.html.
   * 
   * @return null if the file cannot be found
   */
  private String getFLLHtmlFile() {
    final String[] possibleLocations = { "bin", ".", "build/bin", "scoring/build/bin" };
    for (final String location : possibleLocations) {
      final File f = new File(location
          + "/fll-sw.html");
      if (f.exists()
          && f.isFile()) {
        return f.getAbsolutePath();
      }
    }
    return null;
  }

}
