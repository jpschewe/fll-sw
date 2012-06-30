/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.developer;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.thoughtworks.selenium.SeleneseTestBase;

import fll.TestUtils;
import fll.util.LogUtils;
import fll.web.IntegrationTestUtils;
import fll.web.WebTestUtils;

/**
 * Test that we can get database query results from {@link QueryHandler}.
 */
public class QueryTest extends SeleneseTestBase {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Override
  @Before
  public void setUp() throws Exception {
    LogUtils.initializeLogging();
    super.setUp(TestUtils.URL_ROOT
        + "setup");
  }

  @Test
  public void test0() throws IOException, SAXException {
    IntegrationTestUtils.initializeDatabaseFromDump(selenium,
                                                    TestUtils.class.getResourceAsStream("/fll/data/testdb.flldb"));

    final String query = "SELECT * FROM Tournaments";
    final QueryHandler.ResultData result = WebTestUtils.executeServerQuery(query);
    for (final String colName : result.columnNames) {
      LOGGER.info(colName);
    }
  }

}
