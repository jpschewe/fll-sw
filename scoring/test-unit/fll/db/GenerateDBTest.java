/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.db;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import junit.framework.Assert;
import net.mtu.eggplant.util.sql.SQLFunctions;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import fll.TestUtils;
import fll.Utilities;
import fll.util.LogUtils;
import fll.xml.ChallengeParser;

/**
 * Test generating various databases.
 * 
 * @version $Revision$
 */
public class GenerateDBTest {

  @Before
  public void setUp() {
    LogUtils.initializeLogging();
  }

  /**
   * Test creating a new database from scratch and creating over an existing
   * database.
   */
  @Test
  public void testCreateDB() throws SQLException, UnsupportedEncodingException {
    final InputStream stream = GenerateDBTest.class.getResourceAsStream("data/challenge-test.xml");
    Assert.assertNotNull(stream);
    final Document document = ChallengeParser.parse(new InputStreamReader(stream));
    Assert.assertNotNull(document);

    final String database = "testdb";
    final DataSource datasource = Utilities.createDataSource(database);

    Connection connection = null;
    try {
      connection = datasource.getConnection();
      GenerateDB.generateDB(document, connection, false);

      GenerateDB.generateDB(document, connection, true);
    } finally {
      SQLFunctions.close(connection);
    }

    TestUtils.deleteDatabase(database);
  }
}
