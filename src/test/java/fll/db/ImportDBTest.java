/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.db;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import fll.TestUtils;
import fll.Utilities;
import fll.xml.ChallengeDescription;

/**
 * @author jpschewe
 */
@ExtendWith(TestUtils.InitializeLogging.class)
public class ImportDBTest {

  /**
   * Test the 2012 plymouth database. Got an error about data truncation.
   * 
   * @throws SQLException test error
   * @throws IOException test error
   */
  @Test
  public void testTruncation() throws IOException, SQLException {
    try (InputStream dumpFileIS = ImportDBTest.class.getResourceAsStream("data/plymouth-2012-11-17.flldb")) {
      final File tempFile = File.createTempFile("flltest", null);
      final String database = tempFile.getAbsolutePath();
      try (Connection connection = Utilities.createFileDataSource(database).getConnection()) {

        final ImportDB.ImportResult importResult = ImportDB.loadFromDumpIntoNewDB(new ZipInputStream(dumpFileIS),
                                                                                  connection);
        TestUtils.deleteImportData(importResult);

      } finally {

        if (!tempFile.delete()) {
          tempFile.deleteOnExit();
        }
      }
      TestUtils.deleteDatabase(database);
    }
  }

  /**
   * Make sure that no show scores in the subjective data import properly.
   * 
   * @throws SQLException test error
   * @throws IOException test error
   */
  @Test
  public void testImportSubjectiveNoShow() throws IOException, SQLException {
    try (InputStream dumpFileIS = ImportDBTest.class.getResourceAsStream("data/mays-20110108-database.flldb")) {
      assertNotNull(dumpFileIS, "Cannot find test data");

      final File tempFile = File.createTempFile("flltest", null);
      final String database = tempFile.getAbsolutePath();
      try (Connection connection = Utilities.createFileDataSource(database).getConnection();
          Statement stmt = connection.createStatement()) {

        final ImportDB.ImportResult importResult = ImportDB.loadFromDumpIntoNewDB(new ZipInputStream(dumpFileIS),
                                                                                  connection);
        TestUtils.deleteImportData(importResult);

        // check that team 8777 has a no show in research
        try (ResultSet rs = stmt.executeQuery("SELECT NoShow FROM research WHERE TeamNumber = 8777")) {
          assertTrue(rs.next(), "Should have a row");
          assertTrue(rs.getBoolean(1), "Should have a no show");

        }
      } finally {
        if (!tempFile.delete()) {
          tempFile.deleteOnExit();
        }
      }
      TestUtils.deleteDatabase(database);
    } // allocate stream
  }

  /**
   * Test
   * {@link ImportDB#loadFromDumpIntoNewDB(java.util.zip.ZipInputStream, String)}
   * and make sure no exceptions are thrown. Also do a dump and load of the
   * database again to ensure there are no issues with the load of a newly
   * dumped database.
   * 
   * @throws SQLException test error
   * @throws IOException test error
   */
  @Test
  public void testLoadFromDumpIntoNewDB() throws IOException, SQLException {
    try (InputStream dumpFileIS = TestUtils.class.getResourceAsStream("data/testdb.flldb")) {
      assertNotNull(dumpFileIS, "Cannot find test data");

      final File tempFile = File.createTempFile("flltest", null);
      final String database = tempFile.getAbsolutePath();
      final File temp = File.createTempFile("fll", ".zip");
      try (Connection connection = Utilities.createFileDataSource(database).getConnection()) {

        final ImportDB.ImportResult importResult = ImportDB.loadFromDumpIntoNewDB(new ZipInputStream(dumpFileIS),
                                                                                  connection);
        TestUtils.deleteImportData(importResult);

        // dump to temp file
        final FileOutputStream fos = new FileOutputStream(temp);
        final ZipOutputStream zipOut = new ZipOutputStream(fos);

        final ChallengeDescription challengeDescription = GlobalParameters.getChallengeDescription(connection);
        assertNotNull(challengeDescription);
        DumpDB.dumpDatabase(zipOut, connection, challengeDescription, null);
        fos.close();

        // load from temp file
        final FileInputStream fis = new FileInputStream(temp);
        final ImportDB.ImportResult importResult2 = ImportDB.loadFromDumpIntoNewDB(new ZipInputStream(fis), connection);
        TestUtils.deleteImportData(importResult2);
        fis.close();
      } finally {
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
}
