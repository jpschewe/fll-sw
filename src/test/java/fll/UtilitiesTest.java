/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.opencsv.CSVWriter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests for {@link Utilities}.
 * 
 * @author jpschewe
 */
@ExtendWith(TestUtils.InitializeLogging.class)
public class UtilitiesTest {

  /**
   * Test loading a csv file.
   * 
   * @throws SQLException test error
   * @throws IOException test error
   */
  @SuppressFBWarnings(value = "DMI_EMPTY_DB_PASSWORD", justification = "Empty password for in-memory database in test is ok")
  @Test
  public void testLoadCSVFile() throws SQLException, IOException {
    // create a csv file in memory
    final String[][] data = new String[][] { { "column1", "column2", "column3" },
                                             { "row1 - column1", "row1 - column2", "row1 - column3" },
                                             { "row2 - column1", "row2 - column2", "row2 - column3" }, };

    final StringWriter writer = new StringWriter();
    final CSVWriter csvWriter = new CSVWriter(writer);
    csvWriter.writeAll(Arrays.asList(data));
    csvWriter.close();

    Utilities.loadDBDriver();

    // create an in-memory database
    final String url = "jdbc:hsqldb:mem:loadCSVFileTest";
    try (Connection connection = DriverManager.getConnection(url, "sa", "")) {

      final Map<String, String> types = new HashMap<String, String>();
      types.put("column1", "varchar(255)");
      types.put("column2", "longvarchar");

      // load the csv file
      Utilities.loadCSVFile(connection, "testtable", types, new StringReader(writer.toString()));

      // check the databsae
      try (Statement stmt = connection.createStatement()) {
        try (ResultSet rs = stmt.executeQuery("SELECT * FROM testtable")) {
          final ResultSetMetaData meta = rs.getMetaData();
          assertEquals(data[0].length, meta.getColumnCount(), "Incorrect number of columns");
          final int[] columnIndicies = new int[data[0].length];
          for (int i = 0; i < columnIndicies.length; ++i) {
            try {
              columnIndicies[i] = rs.findColumn(data[0][i]);
            } catch (final SQLException sqle) {
              fail("Column not found: "
                  + data[0][i]);
            }
          }
          for (int row = 1; rs.next(); ++row) {
            for (int column = 0; column < data[row].length; ++column) {
              assertEquals(data[row][column], rs.getString(column
                  + 1));
            }
          }
        } // ResultSet
      } // Statement
    } // Connection

  }
}
