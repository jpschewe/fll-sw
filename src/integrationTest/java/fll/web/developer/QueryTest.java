/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.developer;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import org.xml.sax.SAXException;

import fll.TestUtils;
import fll.util.LogUtils;
import fll.web.IntegrationTestUtils;
import fll.web.WebTestUtils;

/**
 * Test that we can get database query results from {@link QueryHandler}.
 */
@ExtendWith(TestUtils.InitializeLogging.class)
@ExtendWith(IntegrationTestUtils.TomcatRequired.class)
public class QueryTest {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Test
  public void test0(final WebDriver selenium) throws IOException, SAXException, InterruptedException {
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
