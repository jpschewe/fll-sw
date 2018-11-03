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
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import org.apache.log4j.Logger;
import org.bouncycastle.operator.OperatorCreationException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.scheduler.SchedulerUI;
import fll.subjective.SubjectiveFrame;
import fll.util.FLLInternalException;
import fll.util.FLLRuntimeException;
import fll.util.GuiExceptionHandler;
import fll.util.LogUtils;
import fll.web.CertificateUtils;
import net.mtu.eggplant.util.gui.GraphicsUtils;

/**
 * Launcher for fll-sw applications.
 */
public class Launcher extends JFrame {

  private static final Logger LOGGER = LogUtils.getLogger();

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

    final JButton schedulerButton = new JButton("Scheduler");
    schedulerButton.addActionListener(ae -> {
      launchScheduler();
    });
    buttonBox.add(schedulerButton);

    final JButton subjectiveButton = new JButton("Subjective Application");
    subjectiveButton.addActionListener(ae -> {
      launchSubjective();
    });
    buttonBox.add(subjectiveButton);

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
    try (final Socket s = new Socket("localhost", 9080)) {
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

  private void startWebserver() {
    final int result = JOptionPane.showConfirmDialog(this, "Have you connected to the network yet?", "Network Question",
                                                     JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
    if (JOptionPane.YES_OPTION == result) {
      if (!checkCertificateStore()) {
        LOGGER.trace("Creating certificate store");
        createCertificateStore();
      }

      controlWebserver(true);
    } else {
      JOptionPane.showMessageDialog(this,
                                    "You should connect to the network first to ensure there aren't issues with SSL later");
    }
  }

  private void stopWebserver() {
    controlWebserver(false);
  }

  private transient Thread webserverThread = null;

  private void controlWebserver(final boolean start) {
    if (start
        && null != webserverThread) {
      JOptionPane.showMessageDialog(this, "Webserver has already been told to start");
      return;
    }

    final String[] env = { "JRE_HOME="
        + System.getProperty("java.home") };
    if (start) {
      webserverThread = new Thread(() -> {
        try {
          if (isWindows()) {
            Runtime.getRuntime().exec("cmd /c start /b bin\\start-tomcat.bat", env);
          } else {
            Runtime.getRuntime().exec("./bin/start-tomcat.sh", env);
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
        if (isWindows()) {
          Runtime.getRuntime().exec("cmd /c start /b bin\\stop-tomcat.bat", env);
        } else {
          Runtime.getRuntime().exec("./bin/stop-tomcat.sh", env);
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

  private static boolean isWindows() {
    final boolean windows = System.getProperty("os.name").startsWith("Windows");
    return windows;
  }

  /**
   * This filename must match the filename in the server.xml for tomcat.
   * This file lives in the tomcat conf directory.
   */
  public static final String KEYSTORE_FILENAME = "tomcat.keystore";

  /**
   * Create the certificate store to be used by tomcat.
   */
  private void createCertificateStore() {
    final Path tomcatConfDir = getTomcatConfDir();
    if (null == tomcatConfDir) {
      throw new FLLRuntimeException("Cannot find tomcat conf directory, cannot write keystore.");
    }
    final Path keystoreFilename = tomcatConfDir.resolve(KEYSTORE_FILENAME);

    try {
      CertificateUtils.createAndStoreCertificate(keystoreFilename);
    } catch (final IOException e) {
      LOGGER.error("I/O error writing keystore "
          + e.getMessage(), e);

      JOptionPane.showMessageDialog(this, "I/O error writing keystore "
          + e.getMessage());
    } catch (NoSuchAlgorithmException | OperatorCreationException | CertificateException | KeyStoreException e) {
      LOGGER.error("Internal error creating SSL certificate: "
          + e.getMessage(), e);

      JOptionPane.showMessageDialog(this, "Internal error creating SSL certificate: "
          + e.getMessage());
    }
  }

  /**
   * @return if the certificate store contains all of the IP addresses of this
   *         system and is not expired.
   */
  private boolean checkCertificateStore() {
    final Path tomcatConfDir = getTomcatConfDir();
    if (null == tomcatConfDir) {
      throw new FLLRuntimeException("Cannot find tomcat conf directory, cannot write keystore.");
    }
    final Path keystoreFile = tomcatConfDir.resolve(KEYSTORE_FILENAME);

    if (!Files.exists(keystoreFile)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Keystore doesn't exist: "
            + keystoreFile);
      }
      return false;
    }

    try {
      return CertificateUtils.checkCertificateStore(keystoreFile);
    } catch (CertificateParsingException | KeyStoreException e) {
      throw new FLLInternalException("Error checking certificate store: "
          + e.getMessage(), e);
    }
  }

  /**
   * Find the path to the tomcat conf directory.
   * 
   * @return null if the directory cannot be found
   */
  private static Path getTomcatConfDir() {
    final Path[] possibleLocations = { //
                                       Paths.get("tomcat", "conf"), //
                                       Paths.get("."), //
                                       Paths.get("build", "tomcat", "conf"), //
                                       Paths.get("scoring", "build", "tomcat", "conf") //
    };
    for (final Path location : possibleLocations) {
      final Path check = location.resolve("server.xml");
      if (Files.exists(check)) {
        return location;
      }
    }
    return null;
  }

}
