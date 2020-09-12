/*
 * Copyright (c) 2000-2003 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll.db;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.w3c.dom.Document;

import fll.TestUtils;
import fll.Utilities;
import fll.xml.ChallengeParser;

/**
 * Test generating various databases.
 */
@ExtendWith(TestUtils.InitializeLogging.class)
public class GenerateDBTest {

  /**
   * Test creating a new database from scratch and creating over an existing
   * database.
   * 
   * @throws IOException test error
   * @throws SQLException test error
   */
  @Test
  public void testCreateDB() throws SQLException, IOException {
    try (InputStream stream = GenerateDBTest.class.getResourceAsStream("data/challenge-test.xml")) {
      assertNotNull(stream);
      final Document document = ChallengeParser.parse(new InputStreamReader(stream, Utilities.DEFAULT_CHARSET));
      assertNotNull(document);

      final File tempFile = File.createTempFile("flltest", null);
      final String database = tempFile.getAbsolutePath();

      final DataSource datasource = Utilities.createFileDataSource(database);

      try (Connection connection = datasource.getConnection()) {
        GenerateDB.generateDB(document, connection);

        GenerateDB.generateDB(document, connection);
      } finally {
        if (!tempFile.delete()) {
          tempFile.deleteOnExit();
        }
      }

      TestUtils.deleteDatabase(database);
    }
  }
}
