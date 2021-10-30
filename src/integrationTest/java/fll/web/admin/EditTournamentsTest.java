/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import fll.TestUtils;
import fll.web.InitializeDatabaseTest;
import fll.web.IntegrationTestUtils;

/**
 * Test editing the tournaments list.
 */
@ExtendWith(TestUtils.InitializeLogging.class)
@ExtendWith(IntegrationTestUtils.TomcatRequired.class)
public class EditTournamentsTest {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * @param selenium web browser driver
   * @param seleniumWait browser wait object
   * @throws IOException test error
   * @throws InterruptedException test error
   */
  @Test
  public void testAddTournament(final WebDriver selenium,
                                final WebDriverWait seleniumWait)
      throws IOException, InterruptedException {
    try {
      final InputStream challengeStream = InitializeDatabaseTest.class.getResourceAsStream("data/challenge-ft.xml");
      LOGGER.trace("Initializating database");
      IntegrationTestUtils.initializeDatabase(selenium, seleniumWait, challengeStream);

      selenium.get(TestUtils.URL_ROOT
          + "admin/index.jsp");

      LOGGER.trace("Waiting for add-edit-tournaments to be clickable, then clicking");
      seleniumWait.until(ExpectedConditions.elementToBeClickable(By.id("add-edit-tournaments"))).click();

      LOGGER.trace("Waiting for addRow to be clickable, then clicking");
      seleniumWait.until(ExpectedConditions.elementToBeClickable(By.id("addRow"))).click();

      LOGGER.trace("Clicking on commit");
      selenium.findElement(By.name("commit")).click();

      LOGGER.trace("Waiting for success");
      seleniumWait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id("success")));
      LOGGER.trace("Finished");
    } catch (final RuntimeException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final AssertionError e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final IOException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    }
  }

}
