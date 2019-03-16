/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.subjective;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.fest.swing.core.KeyPressInfo;
import org.fest.swing.core.MouseButton;
import org.fest.swing.core.matcher.JButtonMatcher;
import org.fest.swing.data.TableCell;
import org.fest.swing.edt.FailOnThreadViolationRepaintManager;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.finder.JOptionPaneFinder;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JOptionPaneFixture;
import org.fest.swing.fixture.JTabbedPaneFixture;
import org.fest.swing.fixture.JTableFixture;
import org.fest.swing.security.ExitCallHook;
import org.fest.swing.security.NoExitSecurityManagerInstaller;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fll.TestUtils;
import fll.Utilities;
import fll.db.GlobalParameters;
import fll.db.ImportDB;
import fll.db.ImportDBTest;
import fll.db.Queries;
import fll.web.IntegrationTestUtils;
import fll.web.admin.DownloadSubjectiveData;
import fll.xml.ChallengeDescription;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

/**
 * Some basic tests for the subjective app.
 */
@ExtendWith(TestUtils.InitializeLogging.class)
public class SubjectiveFrameTest {

  private static NoExitSecurityManagerInstaller noExitSecurityManagerInstaller;

  @AfterAll
  public static void tearDownOnce() {
    noExitSecurityManagerInstaller.uninstall();
  }

  @BeforeAll
  public static void setUpOnce() {
    FailOnThreadViolationRepaintManager.install();
    noExitSecurityManagerInstaller = NoExitSecurityManagerInstaller.installNoExitSecurityManager(new ExitCallHook() {
      public void exitCalled(final int status) {
        assertEquals(0, status, "Bad exit status");
      }
    });
  }

  private FrameFixture window;

  private String database;

  private File subjectiveScores;

  private Document document;

  @BeforeEach
  public void setUp() throws IOException, SQLException {
    final File tempFile = File.createTempFile("flltest", null);
    database = tempFile.getAbsolutePath();

    try (Connection connection = Utilities.createFileDataSource(database).getConnection();
        InputStream dumpFileIS = ImportDBTest.class.getResourceAsStream("data/plymouth-2009-11-21.zip")) {
      assertNotNull(dumpFileIS, "Cannot find test data");

      final ImportDB.ImportResult importResult = ImportDB.loadFromDumpIntoNewDB(new ZipInputStream(dumpFileIS),
                                                                                connection);
      TestUtils.deleteImportData(importResult);

      document = GlobalParameters.getChallengeDocument(connection);

      // set the right tournament
      final String tournamentName = "11-21 Plymouth Middle";
      int tournamentID = -1;
      try (Statement stmt = connection.createStatement()) {
        try (ResultSet rs = stmt.executeQuery("Select tournament_id,Name from Tournaments ORDER BY Name")) {
          while (rs.next()) {
            final int id = rs.getInt(1);
            final String name = rs.getString(2);
            if (tournamentName.equals(name)) {
              tournamentID = id;
            }
          }
        }
      }
      assertTrue(tournamentID != -1, "Could find tournament "
          + tournamentName
          + " in database dump");
      Queries.setCurrentTournament(connection, tournamentID);

      // create the subjective datafile
      subjectiveScores = File.createTempFile("testStartupState", ".fll");
      try (FileOutputStream fileStream = new FileOutputStream(subjectiveScores)) {
        final ChallengeDescription description = new ChallengeDescription(document.getDocumentElement());
        // If the schedule gets added, then make sure that the column offset in
        // testBackspaceClears gets modified
        DownloadSubjectiveData.writeSubjectiveData(connection, document, description, null, null, fileStream);
      }

      final SubjectiveFrame frame = GuiActionRunner.execute(new GuiQuery<SubjectiveFrame>() {
        protected SubjectiveFrame executeInEDT() throws IOException {
          final SubjectiveFrame f = new SubjectiveFrame();
          f.load(subjectiveScores);
          return f;
        }
      });
      window = new FrameFixture(frame);
    } catch (final AssertionError e) {
      IntegrationTestUtils.saveScreenshot();
      throw e;
    } catch (final RuntimeException e) {
      IntegrationTestUtils.saveScreenshot();
      throw e;
    } catch (final IOException e) {
      IntegrationTestUtils.saveScreenshot();
      throw e;
    } catch (final SQLException e) {
      IntegrationTestUtils.saveScreenshot();
      throw e;
    }
  }

  @AfterEach
  public void tearDown() {
    if (null != window) {
      window.cleanUp();
    }

    if (null != subjectiveScores
        && !subjectiveScores.delete()) {
      subjectiveScores.deleteOnExit();
    }
    if (null != database) {
      TestUtils.deleteDatabase(database);
    }
  }

  @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "fields are initialized in test setup")
  @Test
  public void testStartupState() throws IOException {
    try {
      window.show(); // shows the frame to test

      final JTabbedPaneFixture tabbedPane = window.tabbedPane();

      final Map<String, Integer> expectedRowCounts = new HashMap<String, Integer>();
      expectedRowCounts.put("Teamwork", 29);
      expectedRowCounts.put("Design", 41);
      expectedRowCounts.put("Programming", 29);
      expectedRowCounts.put("Research Project Assessment", 58);
      for (final Element categoryDescription : new NodelistElementCollectionAdapter(document.getDocumentElement()
                                                                                            .getElementsByTagName("subjectiveCategory"))) {
        final String title = categoryDescription.getAttribute("title");

        tabbedPane.selectTab(title);
        final JTableFixture table = window.table();
        if (expectedRowCounts.containsKey(title)) {
          final int expected = expectedRowCounts.get(title);
          assertEquals(expected, table.rowCount(), "Category "
              + title);
        } else {
          fail("Unknown category '"
              + title
              + "'");
        }
      }

      window.button(JButtonMatcher.withText("Quit")).click();
      final JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().using(window.robot);
      optionPane.button(JButtonMatcher.withText("Yes")).click();
    } catch (final AssertionError e) {
      IntegrationTestUtils.saveScreenshot();
      throw e;
    } catch (final RuntimeException e) {
      IntegrationTestUtils.saveScreenshot();
      throw e;
    }
  }

  @Test
  public void testValidValue() throws IOException {
    try {
      window.show(); // shows the frame to test

      final JTabbedPaneFixture tabbedPane = window.tabbedPane();

      tabbedPane.selectTab("Design");
      final JTableFixture table = window.table();
      // find 306
      final TableCell teamCell = table.cell("306");
      final int colIdx = table.columnIndexFor("Structural");
      final TableCell dataCell = TableCell.row(teamCell.row).column(colIdx);
      table.enterValue(dataCell, "10");
      assertEquals("10", table.valueAt(dataCell));

      window.button(JButtonMatcher.withText("Quit")).click();
      final JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().using(window.robot);
      optionPane.button(JButtonMatcher.withText("Yes")).click();
    } catch (final AssertionError e) {
      IntegrationTestUtils.saveScreenshot();
      throw e;
    } catch (final RuntimeException e) {
      IntegrationTestUtils.saveScreenshot();
      throw e;
    }
  }

  @Test
  public void testInvalidValue() throws IOException {
    try {
      window.show(); // shows the frame to test

      final JTabbedPaneFixture tabbedPane = window.tabbedPane();

      tabbedPane.selectTab("Design");
      final JTableFixture table = window.table();
      // find 306
      final TableCell teamCell = table.cell("306");
      final int colIdx = table.columnIndexFor("Structural");
      final TableCell dataCell = TableCell.row(teamCell.row).column(colIdx);
      final String prevValue = table.valueAt(dataCell);
      table.enterValue(dataCell, "50");
      assertEquals(prevValue, table.valueAt(dataCell), "Value should have reverted");

      window.button(JButtonMatcher.withText("Quit")).click();
      final JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().using(window.robot);
      optionPane.button(JButtonMatcher.withText("Yes")).click();
    } catch (final AssertionError e) {
      IntegrationTestUtils.saveScreenshot();
      throw e;
    } catch (final RuntimeException e) {
      IntegrationTestUtils.saveScreenshot();
      throw e;
    }
  }

  /**
   * When one clicks on a cell, then the data should be overwritten.
   */
  @Test
  public void testOverwriteOnSelect() throws IOException {
    try {
      window.show(); // shows the frame to test

      final JTabbedPaneFixture tabbedPane = window.tabbedPane();

      tabbedPane.selectTab("Design");
      Thread.yield();
      final JTableFixture table = window.table();
      // find 306
      final TableCell teamCell = table.cell("306");
      final int colIdx = table.columnIndexFor("Structural");
      final TableCell dataCell = TableCell.row(teamCell.row).column(colIdx);
      table.click(dataCell, MouseButton.LEFT_BUTTON);
      window.robot.enterText("1");
      table.pressAndReleaseKey(KeyPressInfo.keyCode(KeyEvent.VK_TAB));
      assertEquals("1", table.valueAt(dataCell), "Value should have been overwritten");

      window.button(JButtonMatcher.withText("Quit")).click();
      final JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().using(window.robot);
      optionPane.button(JButtonMatcher.withText("Yes")).click();
    } catch (final AssertionError e) {
      IntegrationTestUtils.saveScreenshot();
      throw e;
    } catch (final RuntimeException e) {
      IntegrationTestUtils.saveScreenshot();
      throw e;
    }
  }

  /**
   * When one clicks on a cell, then the data should be overwritten.
   */
  @Test
  public void testBackspaceClears() throws IOException {
    try {
      window.show(); // shows the frame to test

      final JTabbedPaneFixture tabbedPane = window.tabbedPane();

      tabbedPane.selectTab("Design");
      final JTableFixture table = window.table();
      // find 306
      final TableCell teamCell = table.cell("306");
      final int numSubCategories = 5;
      for (int column = 0; column < numSubCategories; ++column) {
        // no schedule, so base num columns is good
        final TableCell dataCell = TableCell.row(teamCell.row).column(teamCell.column
            + SubjectiveTableModel.BASE_NUM_COLUMNS_LEFT_OF_SCORES
            + column);
        table.click(dataCell, MouseButton.LEFT_BUTTON);
        table.pressAndReleaseKey(KeyPressInfo.keyCode(KeyEvent.VK_BACK_SPACE));
        table.pressAndReleaseKey(KeyPressInfo.keyCode(KeyEvent.VK_TAB));
        assertEquals("", table.valueAt(dataCell), "Value should have been overwritten row: "
            + dataCell.row
            + " column: "
            + dataCell.column);
      }

      window.button(JButtonMatcher.withText("Quit")).click();
      final JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().using(window.robot);
      optionPane.button(JButtonMatcher.withText("Yes")).click();
    } catch (final AssertionError e) {
      IntegrationTestUtils.saveScreenshot();
      throw e;
    } catch (final RuntimeException e) {
      IntegrationTestUtils.saveScreenshot();
      throw e;
    }
  }

}
