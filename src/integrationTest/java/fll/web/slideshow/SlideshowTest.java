/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.slideshow;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import fll.TestUtils;
import fll.db.GenerateDB;
import fll.web.DisplayInfo;
import fll.web.InitializeDatabaseTest;
import fll.web.IntegrationTestUtils;

/**
 * Test for the slideshow code.
 */
@ExtendWith(TestUtils.InitializeLogging.class)
@ExtendWith(IntegrationTestUtils.TomcatRequired.class)
public class SlideshowTest {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * Test setting slideshow interval and make sure it doesn't error.
   *
   * @throws IOException
   * @throws InterruptedException
   */
  @Test
  public void testSlideshowInterval(final WebDriver selenium,
                                    final WebDriverWait seleniumWait)
      throws IOException, InterruptedException {
    LOGGER.info("Top testSlideshowInterval");
    try {
      final InputStream challengeStream = InitializeDatabaseTest.class.getResourceAsStream("data/challenge-ft.xml");
      IntegrationTestUtils.initializeDatabase(selenium, seleniumWait, challengeStream);

      IntegrationTestUtils.setTournament(selenium, seleniumWait, GenerateDB.DUMMY_TOURNAMENT_NAME);

      // add a dummy team so that we have something in the database
      IntegrationTestUtils.addTeam(selenium, seleniumWait, 1, "team", "org", "1", GenerateDB.DUMMY_TOURNAMENT_NAME);

      IntegrationTestUtils.loadPage(selenium, seleniumWait, TestUtils.URL_ROOT
          + "/admin/");

      selenium.findElement(By.id("remote-control")).click();

      seleniumWait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@name='remotePage' and @value='"
          + DisplayInfo.SLIDESHOW_REMOTE_PAGE
          + "']"))).click();
      selenium.findElement(By.name("slideInterval")).sendKeys("5");
      selenium.findElement(By.name("submit_data")).click();

      seleniumWait.until(ExpectedConditions.presenceOfElementLocated(By.id("success")));

      IntegrationTestUtils.loadPage(selenium, seleniumWait, TestUtils.URL_ROOT
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
