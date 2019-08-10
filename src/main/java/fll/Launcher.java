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
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.scheduler.SchedulerUI;
import fll.subjective.SubjectiveFrame;
import fll.util.GuiExceptionHandler;
import fll.xml.ui.ChallengeDescriptionFrame;
import it.sauronsoftware.junique.AlreadyLockedException;
import it.sauronsoftware.junique.JUnique;
import net.mtu.eggplant.util.gui.GraphicsUtils;

/**
 * Launcher for fll-sw applications.
 */
@SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "This class is not meant to be serialized")
public class Launcher extends JFrame {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static final String OPEN_MSG = "open";

  /**
   * Port that the web server runs on.
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

  private static final String OPT_START_WEB = "start-web";

  private static final String OPT_HELP = "help";

  private static Options buildOptions() {
    final Options options = new Options();
    options.addOption(null, OPT_START_WEB, false, "immediately start the webserver");
    options.addOption("h", OPT_HELP, false, "immediately start the webserver");

    return options;
  }

  private static void usage(final Options options) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Launcher", options);
  }

  public static void main(final String[] args) {
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

    final Options options = buildOptions();
    // parse options
    boolean startWeb = false;
    try {
      final CommandLineParser parser = new DefaultParser();
      final CommandLine cmd = parser.parse(options, args);

      if (cmd.hasOption(OPT_START_WEB)) {
        startWeb = true;
      }
      if (cmd.hasOption(OPT_HELP)) {
        usage(options);
        System.exit(0);
      }

    } catch (final org.apache.commons.cli.ParseException pe) {
      LOGGER.error(pe.getMessage());
      usage(options);
      System.exit(1);
    }

    try {
      final Launcher frame = new Launcher();

      ensureSingleInstance(frame);

      GraphicsUtils.centerWindow(frame);

      frame.setVisible(true);

      if (startWeb) {
        frame.startWebserver(false);
      }
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
    // allow us to prevent closing the window
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        maybeExit();
      }
    });

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
      startWebserver(true);
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
      final Path dir = getSponsorLogosDirectory();
      if (null != dir) {
        try {
          Desktop.getDesktop().open(dir.toFile());
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
      final Path dir = getSlideshowDirectory();
      if (null != dir) {
        try {
          Desktop.getDesktop().open(dir.toFile());
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
      final Path dir = getCustomDirectory();
      if (null != dir) {
        try {
          Desktop.getDesktop().open(dir.toFile());
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
      maybeExit();
    });
    cpane.add(exit, BorderLayout.SOUTH);

    pack();

    startWebserverMonitor();
  }

  /**
   * Prompt the user if the webserver is running.
   */
  @SuppressFBWarnings(value = { "DM_EXIT" }, justification = "This method is ment to close the application")
  private void maybeExit() {
    if (mServerOnline) {
      final int result = JOptionPane.showConfirmDialog(this,
                                                       "Closing the launcher will stop the web server. Are you sure you want to close?",
                                                       "Question", JOptionPane.YES_NO_OPTION);
      if (JOptionPane.NO_OPTION == result) {
        return;
      }
    }
    // close
    this.dispose();
    System.exit(0);
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

  private void startWebserver(final boolean loadFrontPage) {
    if (null == webserverLauncher) {
      webserverLauncher = new TomcatLauncher();
    }
    try {
      webserverLauncher.start();

      if (loadFrontPage) {
        loadFllHtml();
      }
    } catch (final LifecycleException e) {
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
    final Path fllHtml = getFLLHtmlFile();
    if (null == fllHtml) {
      JOptionPane.showMessageDialog(this, "Cannot find fll-sw.html, you will need to open this is your browser.");
      return;
    }
    try {
      Desktop.getDesktop().browse(fllHtml.toUri());
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
  private Path getFLLHtmlFile() {
    final Path classesPath = TomcatLauncher.getClassesPath();

    final String[] possibleLocations = { //
                                         "../../src/main/dist", // eclipse
                                         "..", // distribution
                                         "../../../../src/main/dist" // gradle
    };
    for (final String location : possibleLocations) {
      final Path check = classesPath.resolve(location).resolve("fll-sw.html");
      if (Files.exists(check)
          && Files.isRegularFile(check)) {
        return check.normalize();
      }
    }
    return null;
  }

  /**
   * @return the directory or null if not found
   */
  private Path getSponsorLogosDirectory() {
    final Path classesPath = TomcatLauncher.getClassesPath();
    final Path webroot = TomcatLauncher.findWebappRoot(classesPath);
    if (null == webroot) {
      return null;
    } else {
      final Path check = webroot.resolve("sponsor_logos");
      if (Files.exists(check)
          && Files.isDirectory(check)) {
        return check.normalize();
      } else {
        return null;
      }
    }
  }

  /**
   * @return the directory or null if not found
   */
  private Path getSlideshowDirectory() {
    final Path classesPath = TomcatLauncher.getClassesPath();
    final Path webroot = TomcatLauncher.findWebappRoot(classesPath);
    if (null == webroot) {
      return null;
    } else {
      final Path check = webroot.resolve("slideshow");
      if (Files.exists(check)
          && Files.isDirectory(check)) {
        return check.normalize();
      } else {
        return null;
      }
    }
  }

  /**
   * @return the directory or null if not found
   */
  private Path getCustomDirectory() {
    final Path classesPath = TomcatLauncher.getClassesPath();
    final Path webroot = TomcatLauncher.findWebappRoot(classesPath);
    if (null == webroot) {
      return null;
    } else {
      final Path check = webroot.resolve("custom");
      if (Files.exists(check)
          && Files.isDirectory(check)) {
        return check.normalize();
      } else {
        return null;
      }
    }
  }

  /**
   * Open up the main page.
   */
  private void loadDocsHtml() {
    final Path docsHtml = getDocsHtmlFile();
    if (null == docsHtml) {
      JOptionPane.showMessageDialog(this, "Cannot find docs index.html, you will need to open this is your browser.");
      return;
    }
    try {
      Desktop.getDesktop().browse(docsHtml.toUri());
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
  private Path getDocsHtmlFile() {
    final Path classesPath = TomcatLauncher.getClassesPath();
    final String[] possibleLocations = { //
                                         "../web/documentation", // distribution
                                         "../../src/main/documentation", // eclipse
                                         "../../../../src/main/documentation" // gradle
    };

    for (final String location : possibleLocations) {
      final Path check = classesPath.resolve(location).resolve("index.html");
      if (Files.exists(check)
          && Files.isRegularFile(check)) {
        return check.normalize();
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
