/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.xml;

import java.io.File;

import java.sql.SQLException;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import junit.framework.Assert;

/**
 * Add class comment here!
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
    final Document document = ChallengeParser.parse(stream);
    Assert.assertNotNull(document);

    final String database = "testdb";
    GenerateDB.generateDB(document, database, false);

    GenerateDB.generateDB(document, database, true);
    
    cleanupDB(database);
  }

  /**
   * Remove the files associated with the database.  This may mark them to be
   * deleted on exit of the JVM if they cannot be deleted immediately.
   */
  private static void cleanupDB(final String database) {
    final String[] extensions = new String[] {
      "properties",
      "script",
      "data",
      "backup",
      "log",
    };
    for(int i=0; i<extensions.length; i++) {
      final File file = new File(database + "." + extensions[i]);
      if(file.exists()) {
        if(!file.delete()) {
          file.deleteOnExit();
        }
      }
    }
  }
}
