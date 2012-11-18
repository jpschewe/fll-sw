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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import fll.TestUtils;
import fll.Utilities;
import fll.util.LogUtils;

/**
 * @author jpschewe
 */
public class ImportDBTest {

  @Before
  public void setUp() {
    LogUtils.initializeLogging();
  }

  /**
   * Test the 2012 plymouth database. Got an error about data truncation.
   */
  @Test
  public void testTruncation() throws IOException, SQLException  {
    final InputStream dumpFileIS = ImportDBTest.class.getResourceAsStream("data/plymouth-2012-11-17.flldb");
    final File tempFile = File.createTempFile("flltest", null);
    final String database = tempFile.getAbsolutePath();
    Statement stmt = null;
    ResultSet rs = null;
    Connection connection = null;
    try {
      connection = Utilities.createFileDataSource(database).getConnection();

      ImportDB.loadFromDumpIntoNewDB(new ZipInputStream(dumpFileIS), connection);

      connection.close();
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
      SQLFunctions.close(connection);

      if (!tempFile.delete()) {
        tempFile.deleteOnExit();
      }
    }
    TestUtils.deleteDatabase(database);
  }
  
  /**
   * Make sure that no show scores in the subjective data import properly.
   * 
   * @throws IOException
   * @throws SQLException
   */
  @Test
  public void testImportSubjectiveNoShow() throws IOException, SQLException {
    final InputStream dumpFileIS = ImportDBTest.class.getResourceAsStream("data/mays-20110108-database.flldb");
    Assert.assertNotNull("Cannot find test data", dumpFileIS);

    final File tempFile = File.createTempFile("flltest", null);
    final String database = tempFile.getAbsolutePath();
    Statement stmt = null;
    ResultSet rs = null;
    Connection connection = null;
    try {
      connection = Utilities.createFileDataSource(database).getConnection();

      ImportDB.loadFromDumpIntoNewDB(new ZipInputStream(dumpFileIS), connection);

      // check that team 8777 has a no show in research
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT NoShow FROM research WHERE TeamNumber = 8777");
      Assert.assertTrue("Should have a row", rs.next());
      Assert.assertTrue("Should have a no show", rs.getBoolean(1));

      connection.close();
    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
      SQLFunctions.close(connection);

      if (!tempFile.delete()) {
        tempFile.deleteOnExit();
      }
    }
    TestUtils.deleteDatabase(database);
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
    final InputStream dumpFileIS = TestUtils.class.getResourceAsStream("data/testdb.flldb");
    Assert.assertNotNull("Cannot find test data", dumpFileIS);

    final File tempFile = File.createTempFile("flltest", null);
    final String database = tempFile.getAbsolutePath();
    final File temp = File.createTempFile("fll", ".zip");
    Connection connection = null;
    try {
      connection = Utilities.createFileDataSource(database).getConnection();

      ImportDB.loadFromDumpIntoNewDB(new ZipInputStream(dumpFileIS), connection);

      // dump to temp file
      final FileOutputStream fos = new FileOutputStream(temp);
      final ZipOutputStream zipOut = new ZipOutputStream(fos);

      final Document challengeDocument = Queries.getChallengeDocument(connection);
      Assert.assertNotNull(challengeDocument);
      DumpDB.dumpDatabase(zipOut, connection, challengeDocument);
      fos.close();

      // load from temp file
      final FileInputStream fis = new FileInputStream(temp);
      ImportDB.loadFromDumpIntoNewDB(new ZipInputStream(fis), connection);
      fis.close();
    } finally {
      SQLFunctions.close(connection);
      if (!tempFile.delete()) {
        tempFile.deleteOnExit();
      }
      if (!temp.delete()) {
        temp.deleteOnExit();
      }
    }
    TestUtils.deleteDatabase(database);
  }
}
