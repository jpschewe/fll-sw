/*
 * Copyright (c) 2011 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.subjective;

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

import net.mtu.eggplant.util.sql.SQLFunctions;
import net.mtu.eggplant.xml.NodelistElementCollectionAdapter;

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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fll.TestUtils;
import fll.Utilities;
import fll.db.ImportDB;
import fll.db.ImportDBTest;
import fll.db.Queries;
import fll.util.LogUtils;
import fll.web.admin.DownloadSubjectiveData;

/**
 * Some basic tests for the subjective app.
 */
public class SubjectiveFrameTest {

  private static NoExitSecurityManagerInstaller noExitSecurityManagerInstaller;

  @AfterClass
  public static void tearDownOnce() {
    noExitSecurityManagerInstaller.uninstall();
  }

  @BeforeClass
  public static void setUpOnce() {
    FailOnThreadViolationRepaintManager.install();
    noExitSecurityManagerInstaller = NoExitSecurityManagerInstaller.installNoExitSecurityManager(new ExitCallHook() {
      public void exitCalled(final int status) {
        Assert.assertEquals("Bad exit status", 0, status);
      }
    });
  }

  private FrameFixture window;

  private String database;

  private File subjectiveScores;

  private Document document;

  @Before
  public void setUp() throws IOException, SQLException {
    Connection connection = null;
    try {
      LogUtils.initializeLogging();

      // create a database
      final InputStream dumpFileIS = ImportDBTest.class.getResourceAsStream("data/plymouth-2009-11-21.zip");
      Assert.assertNotNull("Cannot find test data", dumpFileIS);

      final File tempFile = File.createTempFile("flltest", null);
      database = tempFile.getAbsolutePath();

      connection = Utilities.createFileDataSource(database).getConnection();

      ImportDB.loadFromDumpIntoNewDB(new ZipInputStream(dumpFileIS), connection);

      document = Queries.getChallengeDocument(connection);

      // set the right tournament
      final String tournamentName = "11-21 Plymouth Middle";
      int tournamentID = -1;
      final Statement stmt = connection.createStatement();
      final ResultSet rs = stmt.executeQuery("Select tournament_id,Name from Tournaments ORDER BY Name");
      while (rs.next()) {
        final int id = rs.getInt(1);
        final String name = rs.getString(2);
        if (tournamentName.equals(name)) {
          tournamentID = id;
        }
      }
      Assert.assertTrue("Could find tournament "
          + tournamentName + " in database dump", tournamentID != -1);
      Queries.setCurrentTournament(connection, tournamentID);

      // create the subjective datafile
      subjectiveScores = File.createTempFile("testStartupState", ".fll");
      final FileOutputStream fileStream = new FileOutputStream(subjectiveScores);
      DownloadSubjectiveData.writeSubjectiveScores(connection, document, fileStream);
      fileStream.close();

      final SubjectiveFrame frame = GuiActionRunner.execute(new GuiQuery<SubjectiveFrame>() {
        protected SubjectiveFrame executeInEDT() throws IOException {
          return new SubjectiveFrame(subjectiveScores);
        }
      });
      window = new FrameFixture(frame);
    } catch (final AssertionError e) {
      TestUtils.saveScreenshot();
      throw e;
    } catch (final RuntimeException e) {
      TestUtils.saveScreenshot();
      throw e;
    } catch (final IOException e) {
      TestUtils.saveScreenshot();
      throw e;
    } catch (final SQLException e) {
      TestUtils.saveScreenshot();
      throw e;
    } finally {
      SQLFunctions.close(connection);
    }
  }

  @After
  public void tearDown() {
    window.cleanUp();
    if (!subjectiveScores.delete()) {
      subjectiveScores.deleteOnExit();
    }
    TestUtils.deleteDatabase(database);
  }

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
      for (final Element categoryDescription : new NodelistElementCollectionAdapter(
                                                                                    document.getDocumentElement()
                                                                                            .getElementsByTagName("subjectiveCategory"))) {
        final String title = categoryDescription.getAttribute("title");

        tabbedPane.selectTab(title);
        final JTableFixture table = window.table();
        if (expectedRowCounts.containsKey(title)) {
          final int expected = expectedRowCounts.get(title);
          Assert.assertEquals("Category "
              + title, expected, table.rowCount());
        } else {
          Assert.fail("Unknown category '"
              + title + "'");
        }
      }

      window.button(JButtonMatcher.withText("Quit")).click();
      final JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().using(window.robot);
      optionPane.button(JButtonMatcher.withText("Yes")).click();
    } catch (final AssertionError e) {
      TestUtils.saveScreenshot();
      throw e;
    } catch (final RuntimeException e) {
      TestUtils.saveScreenshot();
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
      Assert.assertEquals("10", table.valueAt(dataCell));

      window.button(JButtonMatcher.withText("Quit")).click();
      final JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().using(window.robot);
      optionPane.button(JButtonMatcher.withText("Yes")).click();
    } catch (final AssertionError e) {
      TestUtils.saveScreenshot();
      throw e;
    } catch (final RuntimeException e) {
      TestUtils.saveScreenshot();
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
      Assert.assertEquals("Value should have reverted", prevValue, table.valueAt(dataCell));

      window.button(JButtonMatcher.withText("Quit")).click();
      final JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().using(window.robot);
      optionPane.button(JButtonMatcher.withText("Yes")).click();
    } catch (final AssertionError e) {
      TestUtils.saveScreenshot();
      throw e;
    } catch (final RuntimeException e) {
      TestUtils.saveScreenshot();
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
      final JTableFixture table = window.table();
      // find 306
      final TableCell teamCell = table.cell("306");
      final int colIdx = table.columnIndexFor("Structural");
      final TableCell dataCell = TableCell.row(teamCell.row).column(colIdx);
      table.click(dataCell, MouseButton.LEFT_BUTTON);
      window.robot.enterText("1");
      table.pressAndReleaseKey(KeyPressInfo.keyCode(KeyEvent.VK_TAB));
      Assert.assertEquals("Value should have been overwritten", "1", table.valueAt(dataCell));

      window.button(JButtonMatcher.withText("Quit")).click();
      final JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().using(window.robot);
      optionPane.button(JButtonMatcher.withText("Yes")).click();
    } catch (final AssertionError e) {
      TestUtils.saveScreenshot();
      throw e;
    } catch (final RuntimeException e) {
      TestUtils.saveScreenshot();
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
        final TableCell dataCell = TableCell.row(teamCell.row).column(teamCell.column
            + SubjectiveTableModel.NUM_COLUMNS_LEFT_OF_SCORES + column);
        table.click(dataCell, MouseButton.LEFT_BUTTON);
        table.pressAndReleaseKey(KeyPressInfo.keyCode(KeyEvent.VK_BACK_SPACE));
        table.pressAndReleaseKey(KeyPressInfo.keyCode(KeyEvent.VK_TAB));
        Assert.assertEquals("Value should have been overwritten row: "
            + dataCell.row + " column: " + dataCell.column, "", table.valueAt(dataCell));
      }

      window.button(JButtonMatcher.withText("Quit")).click();
      final JOptionPaneFixture optionPane = JOptionPaneFinder.findOptionPane().using(window.robot);
      optionPane.button(JButtonMatcher.withText("Yes")).click();
    } catch (final AssertionError e) {
      TestUtils.saveScreenshot();
      throw e;
    } catch (final RuntimeException e) {
      TestUtils.saveScreenshot();
      throw e;
    }
  }

}
