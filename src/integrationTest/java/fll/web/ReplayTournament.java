/*
 * Copyright (c) 2016 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Formatter;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.zip.ZipInputStream;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import fll.TestUtils;
import fll.Tournament;
import fll.db.ImportDB;
import fll.util.GuiExceptionHandler;
import net.mtu.eggplant.util.BasicFileFilter;

/**
 *
 */
public class ReplayTournament {

  private static final Preferences PREFS = Preferences.userNodeForPackage(ReplayTournament.class);

  private static final String DATABASE_DIRECTORY_PREF = "databaseDirectory";

  private static final String OUTPUT_DIRECTORY_PREF = "outputDirectory";

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Replay a tournament and save it's output.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    GuiExceptionHandler.registerExceptionHandler();

    try {
      // ask for the database to load
      final Path database = chooseDatabase();
      if (null == database) {
        return;
      }

      // ask which tournament from the database

      Class.forName("org.hsqldb.jdbcDriver").newInstance();

      try (final Connection testDataConn = DriverManager.getConnection("jdbc:hsqldb:mem:replay")) {
        loadDatabase(testDataConn, database);

        // choose output directory
        final Path outputDirectory = chooseOutputDirectory();
        if (null == outputDirectory) {
          // canceled selection
          return;
        }

        // choose tournament
        final Tournament testTournament = selectTournament(testDataConn);
        if (null == testTournament) {
          // canceled selection
          return;
        }

        final IntegrationTestUtils.WebDriverType driver = selectWebDriver();
        if (null == driver) {
          // canceled selection
          return;
        }

        // run
        final FullTournamentTest replay = new FullTournamentTest();
        final WebDriver selenium = IntegrationTestUtils.createWebDriver(driver);
        final WebDriverWait seleniumWait = new WebDriverWait(selenium,
                                                             IntegrationTestUtils.WAIT_FOR_ELEMENT.getSeconds());
        try {
          replay.replayTournament(selenium, seleniumWait, testDataConn, testTournament.getName(), outputDirectory);
        } finally {
          selenium.quit();
        }
      }

      System.exit(0);
    } catch (final Throwable e) {
      LOGGER.fatal("Unexpected error", e);
      JOptionPane.showMessageDialog(null, "Unexpected error: "
          + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }

  }

  private static IntegrationTestUtils.WebDriverType selectWebDriver() {
    final IntegrationTestUtils.WebDriverType[] drivers = IntegrationTestUtils.WebDriverType.values();
    final IntegrationTestUtils.WebDriverType selected = (IntegrationTestUtils.WebDriverType) JOptionPane.showInputDialog(null,
                                                                                                                         "Select the web driver to use",
                                                                                                                         "Select Web Driver",
                                                                                                                         JOptionPane.QUESTION_MESSAGE,
                                                                                                                         null,
                                                                                                                         drivers,
                                                                                                                         null);
    return selected;
  }

  private static Tournament selectTournament(final Connection testDataConn) throws SQLException {
    final List<Tournament> tournaments = Tournament.getTournaments(testDataConn);
    if (tournaments.size() == 1) {
      return tournaments.get(0);
    } else {
      final Tournament selected = (Tournament) JOptionPane.showInputDialog(null, "Select the tournament to replay",
                                                                           "Select Tournament",
                                                                           JOptionPane.QUESTION_MESSAGE, null,
                                                                           tournaments.toArray(), null);
      return selected;
    }
  }

  private static void loadDatabase(final Connection testDataConn,
                                   final Path database)
      throws IOException, SQLException {
    try (final InputStream dbResourceStream = new FileInputStream(database.toFile())) {
      final ZipInputStream zipStream = new ZipInputStream(dbResourceStream);
      final ImportDB.ImportResult result = ImportDB.loadFromDumpIntoNewDB(zipStream, testDataConn);
      TestUtils.deleteImportData(result);
    }

  }

  /**
   * Ask the user for the database to load.
   *
   * @return the database path or null if nothing is there to load
   */
  private static Path chooseDatabase() {
    final String startingDirectory = PREFS.get(DATABASE_DIRECTORY_PREF, null);

    final JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Choose the database");
    final FileFilter filter = new BasicFileFilter("FLL Saved Database (flldb)", new String[] { "flldb" });
    fileChooser.setFileFilter(filter);
    if (null != startingDirectory) {
      fileChooser.setCurrentDirectory(new File(startingDirectory));
    }

    final int returnVal = fileChooser.showOpenDialog(null);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      final File currentDirectory = fileChooser.getCurrentDirectory();
      PREFS.put(DATABASE_DIRECTORY_PREF, currentDirectory.getAbsolutePath());

      final File selectedFile = fileChooser.getSelectedFile();
      if (null != selectedFile
          && selectedFile.isFile()
          && selectedFile.canRead()) {
        return selectedFile.toPath();
      } else if (null != selectedFile) {
        JOptionPane.showMessageDialog(null,
                                      new Formatter().format("%s is not a file or is not readable",
                                                             selectedFile.getAbsolutePath()),
                                      "Error reading file", JOptionPane.ERROR_MESSAGE);
        return null;
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  /**
   * Ask the user for the output directory to use
   *
   * @return the database path or null if no directory was chosen
   */
  private static Path chooseOutputDirectory() {
    final String startingDirectory = PREFS.get(OUTPUT_DIRECTORY_PREF, null);

    final JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Choose the directory to store the files in");
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fileChooser.setAcceptAllFileFilterUsed(false);

    if (null != startingDirectory) {
      fileChooser.setCurrentDirectory(new File(startingDirectory));
    }

    final int returnVal = fileChooser.showOpenDialog(null);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      final File selectedFile = fileChooser.getSelectedFile();
      if (null != selectedFile
          && selectedFile.isDirectory()
          && selectedFile.canWrite()) {

        PREFS.put(OUTPUT_DIRECTORY_PREF, selectedFile.getAbsolutePath());

        return selectedFile.toPath();
      } else if (null != selectedFile) {
        JOptionPane.showMessageDialog(null,
                                      new Formatter().format("%s is not a directory or is not writable",
                                                             selectedFile.getAbsolutePath()),
                                      "Error choosing directory", JOptionPane.ERROR_MESSAGE);
        return null;
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

}
