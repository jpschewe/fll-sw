/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import junit.framework.JUnit4TestAdapter;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

import fll.TestUtils;
import fll.Utilities;

/**
 * @author jpschewe
 * @version $Revision$
 */
public class ImportDBTest {

  /**
   * To allow ant to find the unit tests
   */
  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(ImportDBTest.class);
  }

  /**
   * Test
   * {@link ImportDB#loadFromDumpIntoNewDB(java.util.zip.ZipInputStream, String)}
   * and make sure no exceptions are thrown. Also do a dump and load of the
   * database again to ensure there are no issues with the load of a newly
   * dumped database.
   */
  @Test
  public void testLoadFromDumpIntoNewDB() throws IOException, SQLException {
    final InputStream dumpFileIS = ImportDBTest.class.getResourceAsStream("data/test-database.zip");
    Assert.assertNotNull("Cannot find test data", dumpFileIS);

    final File tempFile = File.createTempFile("flltest", null);
    final String database = tempFile.getAbsolutePath();
    final File temp = File.createTempFile("fll", ".zip");

    try {
      ImportDB.loadFromDumpIntoNewDB(new ZipInputStream(dumpFileIS), database);

      // dump to temp file
      final FileOutputStream fos = new FileOutputStream(temp);
      final ZipOutputStream zipOut = new ZipOutputStream(fos);

      final Connection connection = Utilities.createDataSource(database).getConnection();
      final Document challengeDocument = Queries.getChallengeDocument(connection);
      Assert.assertNotNull(challengeDocument);
      DumpDB.dumpDatabase(zipOut, connection, challengeDocument);
      fos.close();
      connection.close();

      // load from temp file
      final FileInputStream fis = new FileInputStream(temp);
      ImportDB.loadFromDumpIntoNewDB(new ZipInputStream(fis), database);
      fis.close();
    } finally {
      if (!temp.delete()) {
        temp.deleteOnExit();
      }
      TestUtils.deleteDatabase(database);
    }
  }
}
