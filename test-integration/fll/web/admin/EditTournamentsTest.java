/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.admin;

import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import fll.util.LogUtils;
import fll.web.InitializeDatabaseTest;
import fll.web.IntegrationTestUtils;

/**
 * Test editing the tournaments list
 */
public class EditTournamentsTest {

  private WebDriver selenium;

  @Before
  public void setUp() throws Exception {
    LogUtils.initializeLogging();
    selenium = IntegrationTestUtils.createWebDriver();
  }

  @After
  public void tearDown() {
    selenium.quit();
  }

  @Test
  public void testAddTournament() throws IOException, InterruptedException {
    try {
      final InputStream challengeStream = InitializeDatabaseTest.class.getResourceAsStream("data/challenge-ft.xml");
      IntegrationTestUtils.initializeDatabase(selenium, challengeStream);
      Thread.sleep(IntegrationTestUtils.WAIT_FOR_PAGE_LOAD_MS);
      
      selenium.findElement(By.linkText("Admin Index")).click();
      Thread.sleep(IntegrationTestUtils.WAIT_FOR_PAGE_LOAD_MS);

      selenium.findElement(By.id("add-edit-tournaments")).click();
      Thread.sleep(IntegrationTestUtils.WAIT_FOR_PAGE_LOAD_MS);
      
      selenium.findElement(By.name("addRow")).click();
      Thread.sleep(IntegrationTestUtils.WAIT_FOR_PAGE_LOAD_MS);
      
      // get num rows
      final WebElement numRowsEle = selenium.findElement(By.name("numRows"));
      final String numRowsStr = numRowsEle.getAttribute("value");
      Assert.assertNotNull(numRowsStr);
      final int numRows = Integer.parseInt(numRowsStr);

      // type in tournament name
      final int lastRowIdx = numRows
          - 1;
      final String lastRowName = "name"
          + lastRowIdx;
      final WebElement lastRow = selenium.findElement(By.name(lastRowName));

      lastRow.sendKeys("test tournament");
      selenium.findElement(By.name("commit")).click();

      selenium.findElement(By.id("success"));
    } catch (final RuntimeException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch (final AssertionError e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    } catch(final IOException e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;      
    }
  }

}
