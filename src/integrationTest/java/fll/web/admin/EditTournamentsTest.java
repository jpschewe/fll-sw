/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
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
      IntegrationTestUtils.initializeDatabase(selenium, seleniumWait, challengeStream);

      seleniumWait.until(ExpectedConditions.elementToBeClickable(By.linkText("Admin Index"))).click();

      seleniumWait.until(ExpectedConditions.elementToBeClickable(By.id("add-edit-tournaments"))).click();

      seleniumWait.until(ExpectedConditions.elementToBeClickable(By.id("addRow"))).click();

      // get num rows
      final WebElement numRowsEle = seleniumWait.until(ExpectedConditions.presenceOfElementLocated(By.name("numRows")));
      final String numRowsStr = numRowsEle.getAttribute("value");
      assertNotNull(numRowsStr);
      final int numRows = Integer.parseInt(numRowsStr);

      // type in tournament name
      final int lastRowIdx = numRows
          - 1;
      final String lastRowName = "name"
          + lastRowIdx;
      final WebElement lastRow = selenium.findElement(By.name(lastRowName));

      lastRow.sendKeys("test tournament");
      selenium.findElement(By.name("commit")).click();

      seleniumWait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.id("success")));
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
