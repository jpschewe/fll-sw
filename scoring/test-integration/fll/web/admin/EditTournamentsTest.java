/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.Assert;

import org.junit.Test;

import com.thoughtworks.selenium.SeleneseTestCase;

import fll.web.InitializeDatabaseTest;
import fll.web.IntegrationTestUtils;

/**
 * Test editing the tournaments list
 */
public class EditTournamentsTest extends SeleneseTestCase {

  @Override
  public void setUp() throws Exception {
    super.setUp("http://localhost:9080/setup");
  }

  @Test
  public void testAddTournament() throws IOException {
    final InputStream challengeStream = InitializeDatabaseTest.class.getResourceAsStream("data/challenge-ft.xml");
    IntegrationTestUtils.initializeDatabase(selenium, challengeStream, true);
    try {
      selenium.click("link=Admin Index");
      selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);

      selenium.click("link=Edit Tournaments");
      selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);

      selenium.click("addRow");
      selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);

      // get num rows
      final String numRowsStr = selenium.getValue("numRows");
      Assert.assertNotNull(numRowsStr);
      final int numRows = Integer.valueOf(numRowsStr);

      // type in tournament name
      final int lastRowIdx = numRows - 1;
      final String lastRowName = "name"
          + lastRowIdx;
      final String lastRowValue = selenium.getValue(lastRowName);
      Assert.assertTrue("There should not be a value in the last row", null == lastRowValue || "".equals(lastRowValue));

      selenium.type(lastRowName, "test tournament");
      selenium.click("commit");
      selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);

      
      Assert.assertTrue("Didn't get success from commit", selenium.isTextPresent("Successfully committed tournament changes"));
    } catch (final RuntimeException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final AssertionError e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    }
  }

}
