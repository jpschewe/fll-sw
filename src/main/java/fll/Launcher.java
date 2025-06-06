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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.Socket;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.sql.DataSource;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import org.apache.catalina.LifecycleException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.SemverException;

import static org.checkerframework.checker.nullness.util.NullnessUtil.castNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.db.Authentication;
import fll.db.DumpDB;
import fll.db.GlobalParameters;
import fll.db.ImportDB;
import fll.scheduler.SchedulerUI;
import fll.tomcat.TomcatLauncher;
import fll.util.ConsoleExceptionHandler;
import fll.util.FLLRuntimeException;
import fll.util.FormatterUtils;
import fll.util.GuiExceptionHandler;
import fll.web.UserRole;
import fll.web.WebUtils;
import fll.web.Welcome;
import fll.web.setup.CreateDB.UserAccount;
import fll.xml.ChallengeDescription;
import fll.xml.ui.ChallengeDescriptionEditor;
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

  private static final String OPT_MIGRATE = "migrate";

  private static Options buildOptions() {
    final Options options = new Options();
    options.addOption(null, OPT_START_WEB, false, "Immediately start the webserver");
    options.addOption(null, OPT_PORT, true, "The port to use for the web server. Default is "
        + DEFAULT_WEB_PORT);
    options.addOption(null, OPT_HEADLESS, false, "Run without the GUI and immediately start the webserver");
    options.addOption(null, OPT_CREATE_ADMIN, false, "Create an admin user");
    options.addOption(null, OPT_MIGRATE, true, "Replace database with the one from the specified installation");

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
      if (cmd.hasOption(OPT_MIGRATE)) {
        final String directory = cmd.getOptionValue(OPT_MIGRATE);
        try {
          migrate(directory);

          LOGGER.info("Migration of data from '{}' successful", directory);
        } catch (final FLLMigrationException e) {
          LOGGER.fatal("Error migrating data: {}", e.getMessage());
          System.exit(1);
        }
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

    setupDataDirectories();

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
   * Setup data directories that the user can put files in.
   */
  public static void setupDataDirectories() {
    // make sure the database backup directory exists
    try {
      Files.createDirectories(DumpDB.getDatabaseBackupPath());
    } catch (final FileAlreadyExistsException e) {
      LOGGER.error("Unable to create automatic database backup because the output directory %s exists and is not a directory");
    } catch (final IOException e) {
      LOGGER.error("Unable to create database backup directories, web server will likely fail to start", e);
    }

    // make sure the user images directory exists
    try {
      Files.createDirectories(UserImages.getImagesPath());
    } catch (final FileAlreadyExistsException e) {
      LOGGER.error("Unable to create user imgaes directory because the directory %s exists and is not a directory");
    } catch (final IOException e) {
      LOGGER.error("Unable to user images directory", e);
    }

    // make sure the default images are there
    final Path partnerLogo = UserImages.getImagesPath().resolve(Welcome.PARTNER_LOGO_FILENAME);
    if (!Files.exists(partnerLogo)) {
      UserImages.useDefaultPartnerLogo();
    }

    final Path fllLogo = UserImages.getImagesPath().resolve(Welcome.FLL_LOGO_FILENAME);
    if (!Files.exists(fllLogo)) {
      UserImages.useDefaultFllLogo();
    }

    final Path fllSubjectiveLogo = UserImages.getImagesPath().resolve(UserImages.FLL_SUBJECTIVE_LOGO_FILENAME);
    if (!Files.exists(fllSubjectiveLogo)) {
      UserImages.useDefaultFllSubjectiveLogo();
    }

    final Path challengeLogo = UserImages.getImagesPath().resolve(UserImages.CHALLENGE_LOGO_FILENAME);
    if (!Files.exists(challengeLogo)) {
      UserImages.useDefaultChallengeLogo();
    }

  }

  private static final Pattern USERNAME_PATTERN = Pattern.compile("^\\w+$");

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

      boolean done = false;
      while (!done) {
        final String user = console.readLine("Username: ");
        final char[] pass = console.readPassword("Password: ");
        final char[] passCheck = console.readPassword("Repeat password: ");

        done = true;
        if (null == user
            || !USERNAME_PATTERN.matcher(user).matches()) {
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
          if (existingUsers.contains(castNonNull(user))) {
            console.writer().format("The username '%s' already exists in the database%n", user);
            done = false;
          } else {
            Authentication.addUser(connection, castNonNull(user), String.valueOf(castNonNull(pass)));
            Authentication.setRoles(connection, castNonNull(user), Collections.singleton(UserRole.ADMIN));
          }
        } // create user

      } // while not done

    } catch (final SQLException e) {
      LOGGER.error("Error talking to the database", e);
      throw new FLLRuntimeException("Error talking to the database", e);
    }

  }

  private static final class AdminUserPrompt extends JDialog {
    AdminUserPrompt(final @UnknownInitialization(JFrame.class) JFrame parent) {
      super(parent, "Create Admin User", true);
      setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

      final Container cpane = getContentPane();
      cpane.setLayout(new GridBagLayout());

      GridBagConstraints gbc;

      gbc = new GridBagConstraints();
      gbc.weightx = 0;
      gbc.anchor = GridBagConstraints.FIRST_LINE_END;
      cpane.add(new JLabel("Username: "), gbc);

      final JFormattedTextField userEditor = FormatterUtils.createFieldForPattern(USERNAME_PATTERN);
      gbc = new GridBagConstraints();
      gbc.weightx = 1;
      gbc.anchor = GridBagConstraints.FIRST_LINE_START;
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      cpane.add(userEditor, gbc);
      userEditor.setColumns(ChallengeDescriptionEditor.SHORT_TEXT_WIDTH);

      gbc = new GridBagConstraints();
      gbc.weightx = 0;
      gbc.anchor = GridBagConstraints.FIRST_LINE_END;
      cpane.add(new JLabel("Password: "), gbc);

      final JPasswordField passwordEditor = new JPasswordField();
      gbc = new GridBagConstraints();
      gbc.weightx = 1;
      gbc.anchor = GridBagConstraints.FIRST_LINE_START;
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      cpane.add(passwordEditor, gbc);

      gbc = new GridBagConstraints();
      gbc.weightx = 0;
      gbc.anchor = GridBagConstraints.FIRST_LINE_END;
      cpane.add(new JLabel("Repeat Password: "), gbc);

      final JPasswordField password2Editor = new JPasswordField();
      gbc = new GridBagConstraints();
      gbc.weightx = 1;
      gbc.anchor = GridBagConstraints.FIRST_LINE_START;
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      cpane.add(password2Editor, gbc);

      final Box buttonBox = Box.createHorizontalBox();
      gbc = new GridBagConstraints();
      gbc.weightx = 1;
      gbc.anchor = GridBagConstraints.FIRST_LINE_START;
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      cpane.add(buttonBox, gbc);

      final JButton ok = new JButton("OK");
      buttonBox.add(ok);
      getRootPane().setDefaultButton(ok);
      ok.addActionListener(ae -> {
        final String username = userEditor.getText();
        if (username.isBlank()) {
          JOptionPane.showMessageDialog(this, "Username cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
          return;
        }

        final char[] pass = passwordEditor.getPassword();
        final char[] passCheck = password2Editor.getPassword();
        if (!Arrays.equals(pass, passCheck)) {
          JOptionPane.showMessageDialog(this, "Passwords do not match", "Error", JOptionPane.ERROR_MESSAGE);
          return;
        }

        this.user = username;
        this.password = String.valueOf(pass);
        this.canceled = false;
        setVisible(false);
      });

      final JButton cancel = new JButton("Cancel");
      buttonBox.add(cancel);
      cancel.addActionListener(ae -> {
        canceled = true;
        setVisible(false);
      });

      pack();
    }

    private boolean canceled = false;

    /**
     * @return was the dialog canceled?
     */
    public boolean isCanceled(@UnknownInitialization(AdminUserPrompt.class) AdminUserPrompt this) {
      return canceled;
    }

    private @Nullable String user = null;

    /**
     * @return the specified user
     */
    public @Nullable String getUser(@UnknownInitialization(AdminUserPrompt.class) AdminUserPrompt this) {
      return user;
    }

    private @Nullable String password = null;

    /**
     * @return the specified password
     */
    public @Nullable String getPassword(@UnknownInitialization(AdminUserPrompt.class) AdminUserPrompt this) {
      return password;
    }

  }

  /**
   * Create an admin user from the GUI.
   */
  private void createAdminUserGui(@UnknownInitialization(Launcher.class) Launcher this) {
    final DataSource datasource = createDatasource();
    try (Connection connection = datasource.getConnection()) {
      if (!Utilities.testDatabaseInitialized(connection)) {
        JOptionPane.showMessageDialog(this,
                                      "Database is not initialized, cannot create admin user now. A user will be created when the database is initialized",
                                      "Database Not Initialized", JOptionPane.WARNING_MESSAGE);
        return;
      }

      final AdminUserPrompt dialog = new AdminUserPrompt(this);
      dialog.setLocationRelativeTo(this);
      dialog.setVisible(true);
      if (dialog.isCanceled()) {
        return;
      }

      final String user = dialog.getUser();
      final String pass = dialog.getPassword();

      if (null == user) {
        JOptionPane.showMessageDialog(this, "Username cannot be null", "Error", JOptionPane.ERROR_MESSAGE);
      } else if (null == pass) {
        JOptionPane.showMessageDialog(this, "Password cannot be null", "Error", JOptionPane.ERROR_MESSAGE);
      } else {
        // create the user
        final Collection<String> existingUsers = Authentication.getUsers(connection);
        if (existingUsers.contains(user)) {
          final String message = String.format("The username '%s' already exists in the database", user);
          JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
        } else {
          Authentication.addUser(connection, user, String.valueOf(pass));
          Authentication.setRoles(connection, user, Collections.singleton(UserRole.ADMIN));
          JOptionPane.showMessageDialog(this, "User successfully created", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
      }

    } catch (final SQLException e) {
      final String message = String.format("Error talking to the database: %s", e.getMessage());
      JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
      LOGGER.error("Error talking to the database", e);
    }
  }

  /**
   * Create a {@link DataSource} that points to the database that will be used by
   * the web application.
   * 
   * @return the DataSource
   */
  public static DataSource createDatasource() {
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

  private final JButton migrateButton;

  /**
   * @param port the port to use for the web server
   */
  public Launcher(final int port) {
    super("FLL-SW");
    this.port = port;

    // allow us to prevent closing the window
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

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
    buttonBox.add(webserverStartButton);

    webserverStopButton = new JButton("Stop web server");
    buttonBox.add(webserverStopButton);

    mainPage = new JButton("Visit the main web page");
    buttonBox.add(mainPage);

    final JButton docsPage = new JButton("View the documentation");
    buttonBox.add(docsPage);

    final JButton sponsorLogos = new JButton("Sponsor Logos");
    sponsorLogos.setToolTipText("Opens the directory where the sponsor logos go");
    buttonBox.add(sponsorLogos);

    final JButton slideshow = new JButton("Slide show");
    slideshow.setToolTipText("Opens the directory where the slideshow images go");
    buttonBox.add(slideshow);

    final JButton custom = new JButton("Custom files");
    custom.setToolTipText("Opens the directory where to put extra files to display on the big screen display");
    buttonBox.add(custom);

    final JButton schedulerButton = new JButton("Scheduler");
    buttonBox.add(schedulerButton);

    final JButton challengeEditorButton = new JButton("Challenge Editor");
    buttonBox.add(challengeEditorButton);

    final JButton createAdminUser = new JButton("Create Admin User");
    buttonBox.add(createAdminUser);

    migrateButton = new JButton("Migrate database from other installation");
    buttonBox.add(migrateButton);

    final JButton exit = new JButton("Exit");
    cpane.add(exit, BorderLayout.SOUTH);

    // initialized
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        maybeExit();
      }
    });

    // ensure the webserver gets shutdown
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      stopWebserver();
    }));

    webserverStartButton.addActionListener(ae -> {
      startWebserver(true);
    });
    webserverStopButton.addActionListener(ae -> {
      stopWebserver();
    });

    mainPage.addActionListener(ae -> {
      loadFllHtml();
    });

    docsPage.addActionListener(ae -> {
      loadDocsHtml();
    });

    sponsorLogos.addActionListener(ae -> {
      final Path dir = getSponsorLogosDirectory();
      if (null != dir) {
        try {
          Desktop.getDesktop().open(dir.toFile());
        } catch (final IOException e) {
          final String message = "Error opening sponsor logos directory: "
              + e.getMessage();
          LOGGER.error(message, e);
          JOptionPane.showMessageDialog(this, message, "ERROR", JOptionPane.ERROR_MESSAGE);
        }
      } else {
        JOptionPane.showMessageDialog(this, "Cannot find sponsor logos directory.", "ERROR", JOptionPane.ERROR_MESSAGE);
      }
    });

    schedulerButton.addActionListener(ae -> {
      launchScheduler();
    });

    challengeEditorButton.addActionListener(ae -> {
      launchChallengeEditor();
    });

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
    createAdminUser.addActionListener(ae -> {
      createAdminUserGui();
    });

    migrateButton.addActionListener(ae -> {
      migrateGui();
    });

    exit.addActionListener(ae -> {
      maybeExit();
    });

    pack();

    startWebserverMonitor();
  }

  /**
   * Prompt the user if the webserver is running.
   */
  @SuppressFBWarnings(value = { "DM_EXIT" }, justification = "This method is ment to close the application")
  private void maybeExit(@UnknownInitialization(Launcher.class) Launcher this) {
    if (mServerOnline) {
      final int result = JOptionPane.showConfirmDialog(this,
                                                       "Closing the launcher will stop the web server. Are you sure you want to close?",
                                                       "Question", JOptionPane.YES_NO_OPTION);
      if (JOptionPane.NO_OPTION == result) {
        return;
      }
    }

    // make sure sessions are persisted
    stopWebserver();

    // close
    this.dispose();
    System.exit(0);
  }

  private void startWebserverMonitor(@UnknownInitialization(Launcher.class) Launcher this) {
    final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    executorService.scheduleWithFixedDelay(() -> {
      checkWebserver();
    }, 0, 10, TimeUnit.SECONDS);
  }

  /**
   * Check if the webserver is up and update the status label.
   */
  private void checkWebserver(@UnknownInitialization(Launcher.class) Launcher this) {
    boolean newServerOnline = false;
    LOGGER.trace("Checking if the server is online");
    try (Socket s = new Socket("localhost", port)) {
      newServerOnline = true;
    } catch (final IOException ex) {
      LOGGER.trace("Error checking web server, probably down", ex);
    }
    LOGGER.trace("Finished checking if the server is online");

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
    migrateButton.setEnabled(!mServerOnline);
    mainPage.setEnabled(mServerOnline);
  }

  private @Nullable SchedulerUI scheduler = null;

  private void launchScheduler(@UnknownInitialization(Launcher.class) Launcher this) {
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

        scheduler.setLocationRelativeTo(this);
        scheduler.setVisible(true);
      } catch (final Exception e) {
        LOGGER.fatal("Unexpected error", e);
        JOptionPane.showMessageDialog(null, "Unexpected error: "
            + e.getMessage(), "Error Launching Scheduler", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private @Nullable ChallengeDescriptionFrame challengeEditor = null;

  private void launchChallengeEditor(@UnknownInitialization(Launcher.class) Launcher this) {
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

        challengeEditor.setLocationRelativeTo(this);
        challengeEditor.setVisible(true);
      } catch (final Exception e) {
        LOGGER.fatal("Unexpected error", e);
        JOptionPane.showMessageDialog(null, "Unexpected error: "
            + e.getMessage(), "Error Launching Scheduler", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private @Nullable TomcatLauncher webserverLauncher = null;

  private boolean firewallNotificationDisplayed = false;

  private void startWebserver(@UnknownInitialization(Launcher.class) Launcher this,
                              final boolean loadFrontPage) {
    if (SystemUtils.IS_OS_WINDOWS
        && !firewallNotificationDisplayed) {
      JOptionPane.showMessageDialog(Launcher.this,
                                    "If Windows prompts you about the firewall, check both private and public networks and then click Allow access",
                                    "Firewall Information", JOptionPane.INFORMATION_MESSAGE);
      firewallNotificationDisplayed = true;
    }

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
      JOptionPane.showMessageDialog(Launcher.this, "Unexpected error starting webserver: "
          + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void stopWebserver(@UnknownInitialization(Launcher.class) Launcher this) {
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

  private static Path createFllFileWithPort(final Path sourceFllHtml,
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
  private void loadFllHtml(@UnknownInitialization(Launcher.class) Launcher this) {
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
  private @Nullable Path getFLLHtmlFile(@UnknownInitialization(Launcher.class) Launcher this) {
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
   * @return the directory for sponsor logos or null if not found
   */
  public static @Nullable Path getSponsorLogosDirectory() {
    final Path classesPath = TomcatLauncher.getClassesPath();
    final Path webroot = TomcatLauncher.findWebappRoot(classesPath);
    if (null == webroot) {
      return null;
    } else {
      final Path check = webroot.resolve(WebUtils.SPONSOR_LOGOS_PATH);
      if (Files.exists(check)
          && Files.isDirectory(check)) {
        return check.normalize();
      } else {
        return null;
      }
    }
  }

  /**
   * @return the directory for slideshow or null if not found
   */
  public static @Nullable Path getSlideshowDirectory() {
    final Path classesPath = TomcatLauncher.getClassesPath();
    final Path webroot = TomcatLauncher.findWebappRoot(classesPath);
    if (null == webroot) {
      return null;
    } else {
      final Path check = webroot.resolve(WebUtils.SLIDESHOW_PATH);
      if (Files.exists(check)
          && Files.isDirectory(check)) {
        return check.normalize();
      } else {
        return null;
      }
    }
  }

  /**
   * @return the directory for custom images or null if not found
   */
  public static @Nullable Path getCustomDirectory() {
    final Path classesPath = TomcatLauncher.getClassesPath();
    final Path webroot = TomcatLauncher.findWebappRoot(classesPath);
    if (null == webroot) {
      return null;
    } else {
      final Path check = webroot.resolve(WebUtils.CUSTOM_PATH);
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
  private void loadDocsHtml(@UnknownInitialization(Launcher.class) Launcher this) {
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
  private @Nullable Path getDocsHtmlFile(@UnknownInitialization(Launcher.class) Launcher this) {
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

  @SideEffectFree
  private static void setApplicationIcon(final @UnknownInitialization(JFrame.class) JFrame window) {
    try {
      // get images from small to large
      final URL imageURL = Launcher.class.getResource("/fll/resources/fll-sw.png");
      if (null == imageURL) {
        LOGGER.error("Cannot find application icon, not setting");
        return;
      }
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

  private static final class FLLMigrationException extends Exception {
    FLLMigrationException(final String message) {
      super(message);
    }
  }

  private void migrateGui(@UnknownInitialization(Launcher.class) Launcher this) {
    if (mServerOnline) {
      JOptionPane.showMessageDialog(this, "Cannot migrate data while the web server is running", "Migration Error",
                                    JOptionPane.ERROR_MESSAGE);
      return;
    }

    final JFileChooser chooser = new JFileChooser();
    chooser.setCurrentDirectory(new java.io.File("."));
    chooser.setDialogTitle("Choose the base directory of the installation to migrate from");
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    //
    // disable the "All files" option.
    //
    chooser.setAcceptAllFileFilterUsed(false);
    //
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      System.out.println("getCurrentDirectory(): "
          + chooser.getCurrentDirectory());
      System.out.println("getSelectedFile() : "
          + chooser.getSelectedFile());

      final int result = JOptionPane.showConfirmDialog(this,
                                                       "This action will clear any data in the current database. Do you want to continue?",
                                                       "Question", JOptionPane.YES_NO_OPTION);
      if (JOptionPane.YES_OPTION == result) {
        final File directory = chooser.getSelectedFile();
        if (null == directory) {
          return;
        }

        final String oldInstallationDir = directory.getAbsolutePath();
        try {
          migrate(oldInstallationDir);
          JOptionPane.showMessageDialog(this, "Migration complete", "Migration Information",
                                        JOptionPane.INFORMATION_MESSAGE);
        } catch (final FLLMigrationException e) {
          final String msg = String.format("Error during migration from '%s'", oldInstallationDir);

          LOGGER.error(msg, e);
          JOptionPane.showMessageDialog(this, msg, "Migration Error", JOptionPane.ERROR_MESSAGE);
        }
      }

    } else {
      return;
    }
  }

  /**
   * Migrate the database from the specified installation to the current
   * installation.
   * This cannot be executed with the web server running.
   * 
   * @param oldInstallationDirectory where to read the data from
   * @throws FLLMigrationException if there is an error, the message should be
   *           reported to the user
   */
  private static void migrate(final String oldInstallationDirectory) throws FLLMigrationException {
    try {
      final Path exportFile = Files.createTempFile("migrate", "flldb");

      final String oldDatabase = oldInstallationDirectory
          + "/web/WEB-INF/flldb";
      final DataSource oldDatasource = Utilities.createFileDataSource(oldDatabase);

      final Collection<UserAccount> oldAccounts = new LinkedList<>();

      try (Connection oldConnection = oldDatasource.getConnection()) {
        try {
          if (!Utilities.testDatabaseInitialized(oldConnection)) {
            throw new FLLMigrationException("The database at '"
                + oldDatabase
                + "' in the installation '"
                + oldInstallationDirectory
                + "' does not exist");
          }
        } catch (final SQLException e) {
          throw new FLLMigrationException("Error checking database at '"
              + oldDatabase
              + "': "
              + e.getMessage());
        }

        for (final String username : Authentication.getUsers(oldConnection)) {
          // password cannot be null for users that are known to be in the database
          final String hashedPassword = castNonNull(Authentication.getHashedPassword(oldConnection, username));
          final Set<UserRole> roles = Authentication.getRoles(oldConnection, username);

          final UserAccount account = new UserAccount(username, hashedPassword, roles);
          oldAccounts.add(account);
        }
        try (OutputStream os = Files.newOutputStream(exportFile); ZipOutputStream zipOut = new ZipOutputStream(os)) {
          final ChallengeDescription challengeDescription = GlobalParameters.getChallengeDescription(oldConnection);
          DumpDB.dumpDatabase(zipOut, oldConnection, challengeDescription, null);
        } catch (final SQLException | IOException e) {
          throw new FLLMigrationException("Error exporting from old database at '"
              + oldDatabase
              + "': "
              + e.getMessage());
        }
      } catch (final SQLException e) {
        throw new FLLMigrationException("Error reading accounts from old database at '"
            + oldDatabase
            + "': "
            + e.getMessage());
      }

      final DataSource newDatasource = createDatasource();
      try (Connection newConnection = newDatasource.getConnection()) {
        try (InputStream is = Files.newInputStream(exportFile); ZipInputStream zis = new ZipInputStream(is)) {
          DumpDB.automaticBackup(newConnection, "before-migrate");

          ImportDB.loadFromDumpIntoNewDB(zis, newConnection);

          final Collection<String> newDbUsers = Authentication.getUsers(newConnection);
          final Iterator<UserAccount> accountIter = oldAccounts.iterator();
          while (accountIter.hasNext()) {
            final UserAccount account = accountIter.next();
            if (!newDbUsers.contains(account.getUsername())) {
              // don't overwrite users
              Authentication.addAccount(newConnection, account);
            }
          }
        } catch (final IOException | SQLException e) {
          throw new FLLMigrationException("Error importing into the current database: "
              + e.getMessage());
        }
      } catch (final SQLException e) {
        throw new FLLMigrationException("Error importing into the curren database: "
            + e.getMessage());
      }
    } catch (final IOException e) {
      throw new FLLRuntimeException("Error creating temporary file", e);
    }

    final Path oldBaseDir = Paths.get(oldInstallationDirectory);
    final @Nullable Semver sourceVersion = getVersion(oldBaseDir);
    if (!sourceDumpsImages(sourceVersion)) {
      final Path oldWebDir = oldBaseDir.resolve("web");

      // copy sponsor logos
      final Path oldSponsorLogos = oldWebDir.resolve(WebUtils.SPONSOR_LOGOS_PATH);
      final Path newSponsorLogos = getSponsorLogosDirectory();
      if (null == newSponsorLogos) {
        throw new FLLRuntimeException("Unable to find current sponsor logos directory");
      }
      try {
        FileUtils.copyDirectory(oldSponsorLogos.toFile(), newSponsorLogos.toFile());
      } catch (final IOException e) {
        throw new FLLRuntimeException("Error copying sponsor logos", e);
      }

      // copy custom images
      final Path oldCustomImages = oldWebDir.resolve(WebUtils.CUSTOM_PATH);
      final @Nullable Path newCustomImages = getCustomDirectory();
      if (null == newCustomImages) {
        throw new FLLRuntimeException("Unable to find current custom images directory");
      }
      try {
        FileUtils.copyDirectory(oldCustomImages.toFile(), newCustomImages.toFile());
      } catch (final IOException e) {
        throw new FLLRuntimeException("Error copying custom images", e);
      }

      // copy slideshow
      final Path oldSlideshow = oldWebDir.resolve(WebUtils.SLIDESHOW_PATH);
      final Path newSlideshow = getSlideshowDirectory();
      if (null == newSlideshow) {
        throw new FLLRuntimeException("Unable to find current slideshow directory");
      }
      try {
        FileUtils.copyDirectory(oldSlideshow.toFile(), newSlideshow.toFile());
      } catch (final IOException e) {
        throw new FLLRuntimeException("Error copying slideshow", e);
      }

      // copy user images
      final Path oldUserImages = oldBaseDir.resolve(UserImages.getImagesPath());
      if (Files.exists(oldUserImages)) {
        final Path newUserImages = UserImages.getImagesPath();
        try {
          FileUtils.copyDirectory(oldUserImages.toFile(), newUserImages.toFile());
        } catch (final IOException e) {
          throw new FLLRuntimeException("Error copying user images", e);
        }
      }
    }

  }

  /**
   * @param oldBaseDir the base directory of the installation
   * @return null if the version cannot be determined
   */
  private static @Nullable Semver getVersion(final Path baseDir) {
    final Path sourceVersionFile = baseDir.resolve("classes/fll/resources/git.properties");
    if (!Files.exists(sourceVersionFile)) {
      return null;
    }

    try (Reader reader = Files.newBufferedReader(sourceVersionFile)) {
      final Properties versionProps = new Properties();
      versionProps.load(reader);
      final String version = versionProps.getProperty("git.build.version", "NO-PROPERTY");
      return new Semver(version);
    } catch (final IOException e) {
      LOGGER.warn("Error loading version information from {}", baseDir, e);
      return null;
    } catch (final SemverException e) {
      LOGGER.warn("Invalid semantic version found at {}", baseDir, e);
      return null;
    }
  }

  private static boolean sourceDumpsImages(final @Nullable Semver sourceVersion) {
    if (null == sourceVersion) {
      // assume that if the version didn't parse that the images need to be copied
      // explictly
      return false;
    }
    try {
      return sourceVersion.satisfies(">= 19.1.0");
    } catch (final SemverException e) {
      LOGGER.debug("source version {} doesn't meet requirement for image dumps", sourceVersion, e);
      return false;
    }
  }
}
