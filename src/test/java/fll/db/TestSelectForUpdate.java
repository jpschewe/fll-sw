/*
 * Copyright (c) 2024 High Tech Kids.  All rights reserved
 * HighTechKids is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import fll.TestUtils;
import fll.Utilities;

@ExtendWith(TestUtils.InitializeLogging.class)
public class TestSelectForUpdate {

  /**
   * @throws SQLException on a database error
   * @throws IOException on a database error
   */
  @Test
  public void testUpdate1() throws IOException, SQLException {
    final File tempFile = File.createTempFile("flltest", null);
    final String database = tempFile.getAbsolutePath();

    try (Connection connection = Utilities.createFileDataSource(database).getConnection();
        Statement stmt = connection.createStatement()) {

      stmt.executeUpdate("CREATE TABLE test_update (pk integer, data varchar(64), CONSTRAINT test_update_pk PRIMARY KEY (pk))");

      try (PreparedStatement insert = connection.prepareStatement("INSERT INTO test_update(pk, data) VALUES(?, ?)")) {
        for (int i = 1; i < 10; ++i) {
          insert.setInt(1, i);
          insert.setString(2, String.format("test %d", i));
          insert.executeUpdate();
        }
      }

      final String modifiedData = "modified";
      final int testKey = 2;
      try (
          PreparedStatement selectUpdate = connection.prepareStatement("SELECT data FROM test_update WHERE pk = ? FOR UPDATE")) {
        selectUpdate.setInt(1, testKey);
        try (ResultSet rs = selectUpdate.executeQuery()) {
          if (rs.next()) {
            rs.updateString(1, modifiedData);
            rs.updateRow();
          } else {
            fail("Unable to find data to update");
          }
        }
      }

      try (PreparedStatement check = connection.prepareStatement("SELECT data FROM test_update WHERE pk = ?")) {
        check.setInt(1, testKey);
        try (ResultSet rs = check.executeQuery()) {
          if (rs.next()) {
            final String checkData = rs.getString(1);
            assertEquals(modifiedData, checkData);
          } else {
            fail("Unable to find data after update");
          }
        }
      }

    } finally {
      if (!tempFile.delete()) {
        tempFile.deleteOnExit();
      }
      TestUtils.deleteDatabase(database);
    }

  }

}
