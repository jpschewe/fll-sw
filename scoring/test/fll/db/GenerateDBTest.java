/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.db;


import java.sql.SQLException;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

import org.w3c.dom.Document;

import fll.TestUtils;
import fll.xml.ChallengeParser;

import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import junit.framework.Assert;

/**
 * Test generating various databases.
 *
 * @version $Revision$
 */
public class GenerateDBTest extends TestCase {
  
  private static final Logger LOG = Logger.getLogger(GenerateDBTest.class);

  /**
   * Test creating a new database from scratch and creating over an existing
   * database.
   */
  public void testCreateDB()
    throws SQLException, UnsupportedEncodingException {
    final InputStream stream = GenerateDBTest.class.getResourceAsStream("data/challenge-test.xml");
    Assert.assertNotNull(stream);
    final Document document = ChallengeParser.parse(new InputStreamReader(stream));
    Assert.assertNotNull(document);

    final String database = "testdb";
    GenerateDB.generateDB(document, database, false);

    GenerateDB.generateDB(document, database, true);
    
    TestUtils.cleanupDB(database);
  }
}
