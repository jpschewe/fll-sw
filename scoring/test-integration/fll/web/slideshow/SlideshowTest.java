/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.slideshow;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import fll.TestUtils;
import fll.db.GenerateDB;
import fll.util.LogUtils;
import fll.web.DisplayInfo;
import fll.web.InitializeDatabaseTest;
import fll.web.IntegrationTestUtils;

/**
 * 
 */
public class SlideshowTest {

  private static final Logger LOGGER = LogUtils.getLogger();

  private WebDriver selenium;

  @Before
  public void setUp() throws Exception {
    LOGGER.info("Top of setup");
    LogUtils.initializeLogging();
    selenium = IntegrationTestUtils.createWebDriver();
    selenium.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
    LOGGER.info("Bottom of setup");
  }

  @After
  public void tearDown() {
    selenium.quit();
  }

  /**
   * Test setting slideshow interval and make sure it doesn't error.
   * 
   * @throws IOException
   * @throws InterruptedException 
   */
  @Test
  public void testSlideshowInterval() throws IOException, InterruptedException {
    LOGGER.info("Top testSlideshowInterval");
    try {
      final InputStream challengeStream = InitializeDatabaseTest.class.getResourceAsStream("data/challenge-ft.xml");
      IntegrationTestUtils.initializeDatabase(selenium, challengeStream);

      IntegrationTestUtils.setTournament(selenium, GenerateDB.DUMMY_TOURNAMENT_NAME);

      // add a dummy team so that we have something in the database
      IntegrationTestUtils.addTeam(selenium, 1, "team", "org", "1", GenerateDB.DUMMY_TOURNAMENT_NAME);

      IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
                                   + "/admin/");

      selenium.findElement(By.id("remote-control")).click();
      Thread.sleep(IntegrationTestUtils.WAIT_FOR_PAGE_LOAD_MS);

      selenium.findElement(By.xpath("//input[@name='remotePage' and @value='" + DisplayInfo.SLIDESHOW_REMOTE_PAGE + "']")).click();
      selenium.findElement(By.name("slideInterval")).sendKeys("5");
      selenium.findElement(By.name("submit")).click();
      Thread.sleep(IntegrationTestUtils.WAIT_FOR_PAGE_LOAD_MS);

      selenium.findElement(By.id("success"));

      IntegrationTestUtils.loadPage(selenium, TestUtils.URL_ROOT
          + "/slideshow.jsp");

    } catch (final RuntimeException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final AssertionError e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    }
    LOGGER.info("Bottom testSlideshowInterval");
  }
}
