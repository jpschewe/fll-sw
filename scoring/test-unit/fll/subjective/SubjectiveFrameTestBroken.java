/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.subjective;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.junit.Assert;
import org.uispec4j.TabGroup;
import org.uispec4j.Table;
import org.uispec4j.Trigger;
import org.uispec4j.UISpec4J;
import org.uispec4j.UISpecTestCase;
import org.uispec4j.Window;
import org.uispec4j.interception.FileChooserHandler;
import org.uispec4j.interception.WindowInterceptor;
import org.w3c.dom.Element;

import fll.util.LogUtils;

/**
 * @author jpschewe
 * @version $Revision$
 */
public class SubjectiveFrameTestBroken extends UISpecTestCase {

  // static {
  // ensurre UISpec4J can intercept windows
  // TODO need to figure out how to fix this
  // UISpec4J.init();
  // }

  private Window _mainWindow;

  private TabGroup _tabbedPane;

  @Override
  protected void setUp() throws Exception {
    LogUtils.initializeLogging();

    super.setUp();
    UISpec4J.setWindowInterceptionTimeLimit(100);
  }

  /**
   * Start up the app with the specified file. Sets _mainWindow and _tabbedPane.
   * 
   * @param scoresFile the file to read the scores from.
   */
  protected final void startApp(final File scoresFile) {
    Assert.assertNotNull(scoresFile);
    Assert.assertTrue(scoresFile.canRead());

    // catch the main window
    _mainWindow = WindowInterceptor.run(new Trigger() {
      public void run() throws Exception {

        // catch the file dialog and selec the file
        WindowInterceptor.init(new Trigger() {
          public void run() throws Exception {
            SubjectiveFrame.main(null);
          }
        }).process(FileChooserHandler.init().select(scoresFile)).run();

      }
    });
    _tabbedPane = _mainWindow.getTabGroup();
  }

  /**
   * Get a table for a subjective element
   * 
   * @param subjectiveElement the element describing the subjective category
   * @return the table, Null if not found
   */
  protected final Table getTable(final Element subjectiveElement) {
    final String title = subjectiveElement.getAttribute("title");
    _tabbedPane.selectTab(title);
    return _tabbedPane.getSelectedTab().getTable();
  }

  // @XTest
  public void testStartupState() throws SQLException, IOException {
    // create a database
    // final InputStream stream =
    // GenerateDBTest.class.getResourceAsStream("data/challenge-test.xml");
    // Assert.assertNotNull(stream);
    // final Document document = ChallengeParser.parse(new
    // InputStreamReader(stream));
    // Assert.assertNotNull(document);
    //
    // final String database = "testdb";
    // GenerateDB.generateDB(document, database, false);
    // final Connection connection = Utilities.createDBConnection(database);
    //
    // // TODO create teams
    //    
    // // TODO assign judges
    //    
    //
    // // create the zip file
    // final File subjectiveScores = File.createTempFile("testStartupState",
    // ".zip");
    // subjectiveScores.deleteOnExit();
    // final FileOutputStream fileStream = new
    // FileOutputStream(subjectiveScores);
    // GetFile.writeSubjectiveScores(connection, document, fileStream);
    // fileStream.close();
    //    
    // // start the app with the specified scores
    // startApp(subjectiveScores);
    //    
    // // TODO check that the right teams exist in the table
    //    
    // // TODO try and enter some data
    //    
    // subjectiveScores.delete();
    // TestUtils.cleanupDB(database);

    // assertFalse(getTable().isEmpty());
    // assertEquals(4, getTable().getColumnCount());
    // assertEquals(10, getTable().getRowCount());
    //
    // // test for cell values
    // for(int row = 0; row < 10; ++row) {
    // String expected;
    // expected = String.valueOf(row) + " a";
    // assertEquals(expected, getTable().getContentAt(row, 0));
    // expected = String.valueOf(row) + " b";
    // assertEquals(expected, getTable().getContentAt(row, 1));
    // expected = String.valueOf(row) + " c";
    // assertEquals(expected, getTable().getContentAt(row, 2));
    // expected = String.valueOf(row) + " d";
    // assertEquals(expected, getTable().getContentAt(row, 3));
    // }
    //
  }

  // /**
  // * Test editing the table.
  // */
  // @XTest
  // public void testEdit() {
  // final String testValue = "testing";
  // getTable().editCell(0, 1, testValue, true);
  // assertEquals(testValue, getTable().getContentAt(0, 1));
  // }

}
