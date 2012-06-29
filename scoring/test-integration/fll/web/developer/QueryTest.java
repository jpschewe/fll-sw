/*
 * Copyright (c) 2012 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web.developer;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.xml.sax.SAXException;

import fll.util.LogUtils;
import fll.web.WebTestUtils;

/**
 * Test that we can get database query results from {@link QueryHandler}.
 */
public class QueryTest {

  private static final Logger LOGGER = LogUtils.getLogger();

  @Test
  public void test0() throws IOException, SAXException {
    final String query = "SELECT * FROM Tournaments";
    final QueryHandler.ResultData result = WebTestUtils.executeServerQuery(query);
    for (final String colName : result.columnNames) {
      LOGGER.info(colName);
    }
  }

}
