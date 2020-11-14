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
import java.io.Console;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.sql.DataSource;
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
import org.checkerframework.checker.nullness.qual.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.db.Authentication;
import fll.scheduler.SchedulerUI;
import fll.tomcat.TomcatLauncher;
import fll.util.ConsoleExceptionHandler;
import fll.util.FLLRuntimeException;
import fll.util.GuiExceptionHandler;
import fll.web.UserRole;
import fll.xml.ui.ChallengeDescriptionFrame;
import it.sauronsoftware.junique.AlreadyLockedException;
import it.sauronsoftware.junique.JUnique;

/**
 * Launcher for fll-sw applications.
 */
@SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "This class is not meant to be serialized")
public class Launcher extends JFrame {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  private static final String OPEN_MSG = "open";

  /**
   * The default port that the web server runs on.
   */
  public static final int DEFAULT_WEB_PORT = 9080;

  private final int port;

  /**
   * Check that no other instance is running. If one is, send a message to bring
   * it to the front and then exit.
   *
   * @param thisLauncher this launcher, used to bring to the front
   */
  @SuppressFBWarnings(value = { "DM_EXIT" }, justification = "Exiting when another instance is running is OK")
  private static void ensureSingleInstance(final Launcher thisLauncher) {
    final String id = String.format("%s_%d", Launcher.class.getName(), thisLauncher.port);
    boolean start;
    try {
      JUnique.acquireLock(id, message -> {
        if (null != thisLauncher) {
          if (OPEN_MSG.equals(message)) {
            thisLauncher.toFront();
            thisLauncher.setVisible(true);
          } else {
            LOGGER.error("Unknown message received from other launcher: '"
                + message
                + "'");
          }
        }
        return null;
      });
      start = true;
    } catch (final AlreadyLockedException e) {
      // Application already running.
      LOGGER.info("Launcher already running on web port {}, bringing to the front and then exiting", thisLauncher.port);
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

  private static final String OPT_PORT = "port";

  private static final String OPT_HEADLESS = "headless";

  private static final String OPT_CREATE_ADMIN = "create-admin";

  private static Options buildOptions() {
    final Options options = new Options();
    options.addOption(null, OPT_START_WEB, false, "Immediately start the webserver");
    options.addOption(null, OPT_PORT, true, "The port to use for the web server. Deafult is "
        + DEFAULT_WEB_PORT);
    options.addOption(null, OPT_HEADLESS, false, "Run without the GUI and immediately start the webserver");
    options.addOption(null, OPT_CREATE_ADMIN, false, "Create an admin user");

    options.addOption("h", OPT_HELP, false, "help");

    return options;
  }

  private static void usage(final Options options) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Launcher", options);
  }

  private static void setupGui() {
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
  }

  /**
   * @param args see "--help" for arguemnts
   */
  public static void main(final String[] args) {
    final Options options = buildOptions();
    // parse options
    int port = DEFAULT_WEB_PORT;
    boolean startWeb = false;
    boolean headless = false;
    try {
      final CommandLineParser parser = new DefaultParser();
      final CommandLine cmd = parser.parse(options, args);

      if (cmd.hasOption(OPT_START_WEB)) {
        startWeb = true;
      }
      if (cmd.hasOption(OPT_PORT)) {
        try {
          port = Integer.parseInt(cmd.getOptionValue(OPT_PORT));
        } catch (final NumberFormatException e) {
          LOGGER.fatal("Unable to parse port number {}", cmd.getOptionValue(OPT_PORT), e);
          System.exit(1);
        }
      }
      if (cmd.hasOption(OPT_HEADLESS)) {
        startWeb = true;
        headless = true;
      }
      if (cmd.hasOption(OPT_CREATE_ADMIN)) {
        createAdminUserCli();
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

    if (headless) {
      runHeadless(port);
    } else {
      setupGui();

      try {
        final Launcher frame = new Launcher(port);

        ensureSingleInstance(frame);

        frame.setLocationRelativeTo(null);

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
  }

  /**
   * Create an admin user from the command line.
   */
  private static void createAdminUserCli() {
    final DataSource datasource = createDatasource();
    try (Connection connection = datasource.getConnection()) {
      if (!Utilities.testDatabaseInitialized(connection)) {
        LOGGER.warn("Database is not initialized, cannot create admin user now. A user will be created when the database is initialized");
        return;
      }

      final Console console = System.console();
      if (null == console) {
        throw new IllegalStateException("No console is connected");
      }

      final Pattern usernamePattern = Pattern.compile("^\\w+$");
      boolean done = false;
      while (!done) {
        final String user = console.readLine("Username: ");
        final char[] pass = console.readPassword("Password: ");
        final char[] passCheck = console.readPassword("Repeat password: ");

        done = true;
        if (!usernamePattern.matcher(user).matches()) {
          console.writer().format("Username can only contain letters, numbers and underscore%n");
          done = false;
        }
        if (null == pass) {
          console.writer().format("Password not read, try again%n");
          done = false;
        }
        if (!Arrays.equals(pass, passCheck)) {
          console.writer().format("Passwords do not match%n");
          done = false;
        }

        if (done) {
          // create the user
          final Collection<String> existingUsers = Authentication.getUsers(connection);
          if (existingUsers.contains(user)) {
            console.writer().format("The username '%s' already exists in the database%n", user);
            done = false;
          } else {
            Authentication.addUser(connection, user, String.valueOf(pass));
            Authentication.setRoles(connection, user, Collections.singleton(UserRole.ADMIN));
          }
        } // create user

      } // while not done

    } catch (final SQLException e) {
      LOGGER.error("Error talking to the database", e);
      throw new FLLRuntimeException("Error talking to the database", e);
    }

  }

  private static DataSource createDatasource() {
    final Path classesPath = TomcatLauncher.getClassesPath();
    LOGGER.debug("Classes path {}", classesPath);

    final Path webappRoot = TomcatLauncher.findWebappRoot(classesPath);
    if (null == webappRoot) {
      throw new FLLRuntimeException("Cannot find location of the database");
    }
    LOGGER.debug("Web app root: {}", webappRoot);

    final String database = webappRoot.resolve("WEB-INF/flldb").toString();
    final DataSource datasource = Utilities.createFileDataSource(database);
    return datasource;
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

  /**
   * @param port the port to use for the web server
   */
  public Launcher(final int port) {
    super();
    this.port = port;

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
    topPanel.add(new JLabel("port "
        + this.port));
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
    try (Socket s = new Socket("localhost", port)) {
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

        scheduler.setLocationRelativeTo(null);
        scheduler.setVisible(true);
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

        challengeEditor.setLocationRelativeTo(null);
        challengeEditor.setVisible(true);
      } catch (final Exception e) {
        LOGGER.fatal("Unexpected error", e);
        JOptionPane.showMessageDialog(null, "Unexpected error: "
            + e.getMessage(), "Error Launching Scheduler", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private @Nullable TomcatLauncher webserverLauncher = null;

  private void startWebserver(final boolean loadFrontPage) {
    if (null == webserverLauncher) {
      webserverLauncher = new TomcatLauncher(port);
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

  private Path createFllFileWithPort(final Path sourceFllHtml,
                                     final int port)
      throws IOException {
    final String content = new String(Files.readAllBytes(sourceFllHtml), Utilities.DEFAULT_CHARSET);
    final String newContent = content.replaceAll(":9080", String.format(":%d", port));
    final Path temp = Files.createTempFile("fll-sw", ".html");
    Files.write(temp, newContent.getBytes(Utilities.DEFAULT_CHARSET));

    // make sure the file is cleaned up
    temp.toFile().deleteOnExit();

    return temp;
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
      final Path modifiedForPort = createFllFileWithPort(fllHtml, port);
      Desktop.getDesktop().browse(modifiedForPort.toUri());
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
  private @Nullable Path getFLLHtmlFile() {
    final Path classesPath = TomcatLauncher.getClassesPath();

    final String[] possibleLocations = { //
                                         "../../src/main/root-docs", // eclipse
                                         "..", // distribution
                                         "../../../../src/main/root-docs" // gradle
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
  private @Nullable Path getSponsorLogosDirectory() {
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
  private @Nullable Path getSlideshowDirectory() {
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
  private @Nullable Path getCustomDirectory() {
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
  private @Nullable Path getDocsHtmlFile() {
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

  private static final int[] ICON_SIZES = new int[] { 8, 16, 32, 64, 128, 256, 512, 1024 };

  private static void setApplicationIcon(final Window window) {
    try {
      // get images from small to large
      final URL imageURL = Launcher.class.getResource("/fll/resources/fll-sw.png");
      final BufferedImage original = ImageIO.read(imageURL);

      final List<BufferedImage> images = new LinkedList<>();
      for (final int size : ICON_SIZES) {
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

  @SuppressFBWarnings(value = { "DM_EXIT" }, justification = "This method is ment to close the application")
  private static void runHeadless(final int port) {
    ConsoleExceptionHandler.registerExceptionHandler();

    final TomcatLauncher webserverLauncher = new TomcatLauncher(port);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      if (null != webserverLauncher) {
        try {
          webserverLauncher.stop();
        } catch (final LifecycleException e) {
          LOGGER.error("Exception stopping webserver", e);
        }
      }
    }));

    try {
      webserverLauncher.start();
    } catch (final LifecycleException e) {
      LOGGER.fatal("Unable to start webserver", e);
      System.exit(1);
    }

  }

}
