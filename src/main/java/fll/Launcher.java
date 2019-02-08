/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import org.apache.catalina.LifecycleException;
import org.apache.log4j.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.scheduler.SchedulerUI;
import fll.subjective.SubjectiveFrame;
import fll.util.GuiExceptionHandler;
import fll.util.LogUtils;
import fll.xml.ui.ChallengeDescriptionFrame;
import it.sauronsoftware.junique.AlreadyLockedException;
import it.sauronsoftware.junique.JUnique;
import net.mtu.eggplant.util.gui.GraphicsUtils;

/**
 * Launcher for fll-sw applications.
 */
public class Launcher extends JFrame {

  private static final Logger LOGGER = LogUtils.getLogger();

  private static final String OPEN_MSG = "open";

  /**
   * Port that the web serer runs on.
   */
  public static final int WEB_PORT = 9080;

  /**
   * Check that no other instance is running. If one is, send a message to bring
   * it to the front and then exit.
   * 
   * @param thisLauncher this launcher, used to bring to the front
   */
  @SuppressFBWarnings(value = { "DM_EXIT" }, justification = "Exiting when another instance is running is OK")
  private static void ensureSingleInstance(final Launcher thisLauncher) {
    final String id = Launcher.class.getName();
    boolean start;
    try {
      JUnique.acquireLock(id, message -> {
        if (null != thisLauncher) {
          if (OPEN_MSG.equals(message)) {
            thisLauncher.toFront();
            thisLauncher.setVisible(true);
          } else {
            LOGGER.error("Unknow message received from other launcher: '"
                + message
                + "'");
          }
        }
        return null;
      });
      start = true;
    } catch (final AlreadyLockedException e) {
      // Application already running.
      LOGGER.info("Launcher already running, bringing to the front and then exiting");
      start = false;
    }
    if (!start) {
      // send open message to the already active instance.
      JUnique.sendMessage(id, OPEN_MSG);
      System.exit(0);
    }
  }

  public static void main(final String[] args) {
    LogUtils.initializeLogging();

    GuiExceptionHandler.registerExceptionHandler();

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

      ensureSingleInstance(frame);

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

  private final JLabel mServerStatus;

  private boolean mServerOnline = false;

  private static final String ONLINE = "ONLINE";

  private static final Color ONLINE_COLOR = Color.GREEN;

  private static final String OFFLINE = "OFFLINE";

  private static final Color OFFLINE_COLOR = Color.RED;

  private final JButton webserverStartButton;

  private final JButton webserverStopButton;

  private final JButton mainPage;

  public Launcher() {
    super();
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setApplicationIcon(this);

    final Container cpane = getContentPane();
    cpane.setLayout(new BorderLayout());

    final JPanel topPanel = new JPanel(new FlowLayout());
    cpane.add(topPanel, BorderLayout.NORTH);
    topPanel.add(new JLabel("Web server is"));
    mServerStatus = new JLabel(OFFLINE);
    mServerStatus.setOpaque(true);
    mServerStatus.setBackground(OFFLINE_COLOR);
    topPanel.add(mServerStatus);

    final JPanel buttonBox = new JPanel(new GridLayout(0, 2));
    cpane.add(buttonBox, BorderLayout.CENTER);

    webserverStartButton = new JButton("Start web server");

    webserverStartButton.addActionListener(ae -> {
      startWebserver();
    });
    buttonBox.add(webserverStartButton);

    webserverStopButton = new JButton("Stop web server");
    webserverStopButton.addActionListener(ae -> {
      stopWebserver();
    });
    buttonBox.add(webserverStopButton);

    mainPage = new JButton("Visit the main web page");
    mainPage.addActionListener(ae -> {
      loadFllHtml();
    });
    buttonBox.add(mainPage);

    final JButton docsPage = new JButton("View the documentation");
    docsPage.addActionListener(ae -> {
      loadDocsHtml();
    });
    buttonBox.add(docsPage);

    final JButton sponsorLogos = new JButton("Sponsor Logos");
    sponsorLogos.setToolTipText("Opens the directory where the sponsor logos go");
    sponsorLogos.addActionListener(ae -> {
      final File dir = getSponsorLogosDirectory();
      if (null != dir) {
        try {
          Desktop.getDesktop().open(dir);
        } catch (final IOException e) {
          final String message = "Error opening sponsor_logos directory: "
              + e.getMessage();
          LOGGER.error(message, e);
          JOptionPane.showMessageDialog(this, message, "ERROR", JOptionPane.ERROR_MESSAGE);
        }
      } else {
        JOptionPane.showMessageDialog(this, "Cannot find sponsor_logos directory.", "ERROR", JOptionPane.ERROR_MESSAGE);
      }
    });
    buttonBox.add(sponsorLogos);

    final JButton slideshow = new JButton("Slide show");
    slideshow.setToolTipText("Opens the directory where the slideshow images go");
    slideshow.addActionListener(ae -> {
      final File dir = getSlideshowDirectory();
      if (null != dir) {
        try {
          Desktop.getDesktop().open(dir);
        } catch (final IOException e) {
          final String message = "Error opening slideshow directory: "
              + e.getMessage();
          LOGGER.error(message, e);
          JOptionPane.showMessageDialog(this, message, "ERROR", JOptionPane.ERROR_MESSAGE);
        }
      } else {
        JOptionPane.showMessageDialog(this, "Cannot find slideshow directory.", "ERROR", JOptionPane.ERROR_MESSAGE);
      }
    });
    buttonBox.add(slideshow);

    final JButton custom = new JButton("Custom files");
    custom.setToolTipText("Opens the directory where to put extra files to display on the big screen display");
    custom.addActionListener(ae -> {
      final File dir = getCustomDirectory();
      if (null != dir) {
        try {
          Desktop.getDesktop().open(dir);
        } catch (final IOException e) {
          final String message = "Error opening custom directory: "
              + e.getMessage();
          LOGGER.error(message, e);
          JOptionPane.showMessageDialog(this, message, "ERROR", JOptionPane.ERROR_MESSAGE);
        }
      } else {
        JOptionPane.showMessageDialog(this, "Cannot find custom directory.", "ERROR", JOptionPane.ERROR_MESSAGE);
      }
    });
    buttonBox.add(custom);

    final JButton subjectiveButton = new JButton("Subjective Application");
    subjectiveButton.addActionListener(ae -> {
      launchSubjective();
    });
    buttonBox.add(subjectiveButton);

    final JButton schedulerButton = new JButton("Scheduler");
    schedulerButton.addActionListener(ae -> {
      launchScheduler();
    });
    buttonBox.add(schedulerButton);

    final JButton challengeEditorButton = new JButton("Challenge Editor");
    challengeEditorButton.addActionListener(ae -> {
      launchChallengeEditor();
    });
    buttonBox.add(challengeEditorButton);

    final JButton exit = new JButton("Exit");
    exit.addActionListener(ae -> {
      setVisible(false);
    });
    cpane.add(exit, BorderLayout.SOUTH);

    pack();

    startWebserverMonitor();
  }

  private void startWebserverMonitor() {
    final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    executorService.scheduleWithFixedDelay(() -> {
      checkWebserver();
    }, 0, 10, TimeUnit.SECONDS);
  }

  /**
   * Check if the webserver is up and update the status label.
   */
  private void checkWebserver() {
    boolean newServerOnline = false;
    try (final Socket s = new Socket("localhost", WEB_PORT)) {
      newServerOnline = true;
    } catch (final IOException ex) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Error checking web server, probably down", ex);
      }
    }
    if (newServerOnline != mServerOnline) {
      // change detected
      mServerOnline = newServerOnline;
      if (mServerOnline) {
        mServerStatus.setText(ONLINE);
        mServerStatus.setBackground(ONLINE_COLOR);
      } else {
        mServerStatus.setText("Offline");
        mServerStatus.setBackground(OFFLINE_COLOR);
      }
    }

    webserverStartButton.setEnabled(!mServerOnline);
    webserverStopButton.setEnabled(mServerOnline);
    mainPage.setEnabled(mServerOnline);
  }

  private SchedulerUI scheduler = null;

  private void launchScheduler() {
    if (null != scheduler) {
      scheduler.setVisible(true);
    } else {
      try {
        scheduler = new SchedulerUI();
        setApplicationIcon(scheduler);

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
        setApplicationIcon(subjective);

        subjective.addWindowListener(new WindowAdapter() {
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

  private ChallengeDescriptionFrame challengeEditor = null;

  private void launchChallengeEditor() {
    if (null != challengeEditor) {
      challengeEditor.setVisible(true);
    } else {
      try {
        challengeEditor = new ChallengeDescriptionFrame();

        challengeEditor.addWindowListener(new WindowAdapter() {
          @Override
          public void windowClosed(final WindowEvent e) {
            challengeEditor = null;
          }
        });
        // should be able to watch for window closing, but hidden works
        challengeEditor.addComponentListener(new ComponentAdapter() {
          @Override
          public void componentHidden(final ComponentEvent e) {
            challengeEditor = null;
          }
        });

        GraphicsUtils.centerWindow(challengeEditor);
        challengeEditor.setVisible(true);
      } catch (final Exception e) {
        LOGGER.fatal("Unexpected error", e);
        JOptionPane.showMessageDialog(null, "Unexpected error: "
            + e.getMessage(), "Error Launching Scheduler", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private TomcatLauncher webserverLauncher = null;

  private void startWebserver() {
    if (null == webserverLauncher) {
      webserverLauncher = new TomcatLauncher();
    }
    try {
      webserverLauncher.start();

      loadFllHtml();
    } catch (LifecycleException e) {
      LOGGER.fatal("Unexpected error starting webserver", e);
      JOptionPane.showMessageDialog(null, "Unexpected error starting webserver: "
          + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void stopWebserver() {
    if (null != webserverLauncher) {
      try {
        webserverLauncher.stop();
        webserverLauncher = null;
      } catch (final LifecycleException e) {
        LOGGER.fatal("Unexpected error stopping webserver", e);
        JOptionPane.showMessageDialog(null, "Unexpected error stopping webserver: "
            + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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

  /**
   * @return the directory or null if not found
   */
  private File getSponsorLogosDirectory() {
    final String[] possibleLocations = { "tomcat/webapps/fll-sw/sponsor_logos" };
    for (final String location : possibleLocations) {
      final File f = new File(location);
      if (f.exists()
          && f.isDirectory()) {
        return f;
      }
    }
    return null;
  }

  /**
   * @return the directory or null if not found
   */
  private File getSlideshowDirectory() {
    final String[] possibleLocations = { "tomcat/webapps/fll-sw/slideshow" };
    for (final String location : possibleLocations) {
      final File f = new File(location);
      if (f.exists()
          && f.isDirectory()) {
        return f;
      }
    }
    return null;
  }

  /**
   * @return the directory or null if not found
   */
  private File getCustomDirectory() {
    final String[] possibleLocations = { "tomcat/webapps/fll-sw/custom" };
    for (final String location : possibleLocations) {
      final File f = new File(location);
      if (f.exists()
          && f.isDirectory()) {
        return f;
      }
    }
    return null;
  }

  /**
   * Open up the main page.
   */
  private void loadDocsHtml() {
    final String docsHtml = getDocsHtmlFile();
    if (null == docsHtml) {
      JOptionPane.showMessageDialog(this, "Cannot find docs index.html, you will need to open this is your browser.");
      return;
    }
    final File htmlFile = new File(docsHtml);
    try {
      Desktop.getDesktop().browse(htmlFile.toURI());
    } catch (final IOException e) {
      LOGGER.error("Unable to open web browser", e);
      JOptionPane.showMessageDialog(this, "Cannot open docs index.html, you will need to open this is your browser.");
    }
  }

  /**
   * Find the path to index.html in the docs directory.
   * 
   * @return null if the file cannot be found
   */
  private String getDocsHtmlFile() {
    final String[] possibleLocations = { "docs", "web/documentation", "build/docs", "scoring/build/docs", "." };
    for (final String location : possibleLocations) {
      final File f = new File(location
          + "/index.html");
      if (f.exists()
          && f.isFile()) {
        return f.getAbsolutePath();
      }
    }
    return null;
  }

  private static void setApplicationIcon(final Window window) {
    try {
      // get images from small to large
      final URL imageURL = Launcher.class.getResource("/fll/resources/fll-sw.png");
      final BufferedImage original = ImageIO.read(imageURL);

      final List<BufferedImage> images = new LinkedList<>();
      for (final int size : new int[] { 8, 16, 32, 64, 128, 256, 512, 1024 }) {
        final double scaleWidth = (double) size
            / original.getWidth();
        final double scaleHeight = (double) size
            / original.getHeight();

        final BufferedImage scaled = new BufferedImage(size, size, original.getType());
        final Graphics2D scaledGraphics = scaled.createGraphics();
        final AffineTransform transform = AffineTransform.getScaleInstance(scaleWidth, scaleHeight);
        scaledGraphics.drawRenderedImage(original, transform);
        images.add(scaled);
      }
      window.setIconImages(images);
    } catch (final IOException e) {
      LOGGER.error("Unable to get application icon, not setting icon", e);
    }
  }

}
