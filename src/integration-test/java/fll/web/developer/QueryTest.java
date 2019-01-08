/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.developer;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebDriver;
import org.xml.sax.SAXException;

import fll.TestUtils;
import fll.util.LogUtils;
import fll.web.IntegrationTestUtils;
import fll.web.WebTestUtils;

/**
 * Test that we can get database query results from {@link QueryHandler}.
 */
public class QueryTest {

  private static final Logger LOGGER = LogUtils.getLogger();

  private WebDriver selenium;

  @Before
  public void setUp() {
    LogUtils.initializeLogging();
    selenium = IntegrationTestUtils.createWebDriver();
  }

  @After
  public void tearDown() {
    selenium.quit();
  }

  @Test
  public void test0() throws IOException, SAXException, InterruptedException {
    try {
      IntegrationTestUtils.initializeDatabaseFromDump(selenium,
                                                      TestUtils.class.getResourceAsStream("/fll/data/testdb.flldb"));

      final String query = "SELECT * FROM Tournaments";
      final QueryHandler.ResultData result = WebTestUtils.executeServerQuery(query);
      for (final String colName : result.getColumnNames()) {
        LOGGER.info(colName);
      }
    } catch (final IOException | RuntimeException | AssertionError e) {
      LOGGER.fatal(e, e);
      IntegrationTestUtils.storeScreenshot(selenium);
    }
  }

}
