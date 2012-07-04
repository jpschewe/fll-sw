/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.slideshow;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;

import fll.TestUtils;
import fll.db.GenerateDB;
import fll.util.LogUtils;
import fll.web.InitializeDatabaseTest;
import fll.web.IntegrationTestUtils;

/**
 * 
 */
public class SlideshowTest {

  private Selenium selenium;

  @Before
  public void setUp() throws Exception {
    LogUtils.initializeLogging();
    selenium = new DefaultSelenium("localhost", 4444, "*firefox", TestUtils.URL_ROOT
        + "setup");
    selenium.start();
  }

  @After
  public void tearDown() {
    selenium.close();
  }

  /**
   * Test setting slideshow interval and make sure it doesn't error.
   * 
   * @throws IOException
   */
  @Test
  public void testSlideshowInterval() throws IOException {
    LogUtils.getLogger().info("Top testSLideshowInterval");
    final InputStream challengeStream = InitializeDatabaseTest.class.getResourceAsStream("data/challenge-ft.xml");
    IntegrationTestUtils.initializeDatabase(selenium, challengeStream, true);

    IntegrationTestUtils.setTournament(selenium, GenerateDB.DUMMY_TOURNAMENT_NAME);

    // add a dummy team so that we have something in the database
    IntegrationTestUtils.addTeam(selenium, 1, "team", "org", "1", GenerateDB.DUMMY_TOURNAMENT_NAME);

    try {
      selenium.click("link=Admin Index");
      selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);

      selenium.click("link=Remote control of display");
      selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);

      selenium.click("slideshow");
      selenium.type("slideInterval", "5");
      selenium.click("submit");
      selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);

      Assert.assertTrue("Didn't get success from commit",
                        selenium.isTextPresent("Successfully set remote control parameters"));

      selenium.open(TestUtils.URL_ROOT
          + "/slideshow/index.jsp");
      selenium.waitForPageToLoad(IntegrationTestUtils.WAIT_FOR_PAGE_TIMEOUT);
      Assert.assertFalse("Got error", selenium.isTextPresent("An error has occurred"));

    } catch (final RuntimeException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final AssertionError e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    }
    LogUtils.getLogger().info("Bottom testSLideshowInterval");
  }

}
