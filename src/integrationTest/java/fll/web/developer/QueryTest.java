/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.developer;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.xml.sax.SAXException;

import fll.TestUtils;
import fll.web.IntegrationTestUtils;
import fll.web.WebTestUtils;

/**
 * Test that we can get database query results from {@link QueryHandler}.
 */
@ExtendWith(TestUtils.InitializeLogging.class)
@ExtendWith(IntegrationTestUtils.TomcatRequired.class)
public class QueryTest {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   *
   * @param selenium web driver
   * @param seleniumWait wait for elements
   * @throws IOException test error
   * @throws SAXException test error
   * @throws InterruptedException test error
   */
  @Test
  public void test0(final WebDriver selenium, final WebDriverWait seleniumWait) throws IOException, SAXException, InterruptedException {
    try {
      IntegrationTestUtils.initializeDatabaseFromDump(selenium,
                                                      seleniumWait, TestUtils.class.getResourceAsStream("/fll/data/testdb.flldb"));

      final String query = "SELECT * FROM Tournaments";
      final QueryHandler.ResultData result = WebTestUtils.executeServerQuery(query);
      for (final String colName : result.getColumnNames()) {
        LOGGER.info(colName);
      }
    } catch (final IOException | RuntimeException | AssertionError e) {
      IntegrationTestUtils.storeScreenshot(selenium);
      throw e;
    }
  }

}
