/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.db;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.zip.ZipInputStream;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author jpschewe
 * @version $Revision$
 * 
 */
public class ImportDBTest extends TestCase {

  private static final Logger LOG = Logger.getLogger(ImportDBTest.class);

  /**
   * Test
   * {@link ImportDB#loadFromDumpIntoNewDB(java.util.zip.ZipInputStream, String)}
   * and make sure no exceptions are thrown.
   * 
   */
  @Test
  public void testLoadFromDumpIntoNewDB() throws IOException, SQLException {
    final InputStream dumpFileIS = ImportDBTest.class.getResourceAsStream("data/test-database.zip");
    Assert.assertNotNull("Cannot find test data", dumpFileIS);

    final File tempFile = File.createTempFile("flltest", null);
    final String database = tempFile.getAbsolutePath();
    
    ImportDB.loadFromDumpIntoNewDB(new ZipInputStream(dumpFileIS), database);

  }
}
