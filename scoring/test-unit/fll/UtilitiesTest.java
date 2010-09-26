/*
 * Copyright (c) 2000-2002 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */
package fll;

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

import net.mtu.eggplant.util.sql.SQLFunctions;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import au.com.bytecode.opencsv.CSVWriter;
import fll.util.LogUtils;

/**
 * Tests for {@link Utilities}.
 * 
 * @author jpschewe
 * @version $Revision$
 */
public class UtilitiesTest {

  @Before
  public void setUp() {
    LogUtils.initializeLogging();
  }

  /**
   * Test loading a csv file.
   */
  @Test
  public void testLoadCSVFile() throws SQLException, IOException {
    Connection connection = null;
    Statement stmt = null;
    ResultSet rs = null;
    try {
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
      connection = DriverManager.getConnection(url, "sa", "");

      // load the csv file
      Utilities.loadCSVFile(connection, "testtable", new StringReader(writer.toString()));

      // check the databsae
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT * FROM testtable");
      final ResultSetMetaData meta = rs.getMetaData();
      Assert.assertEquals("Incorrect number of columns", data[0].length, meta.getColumnCount());
      final int[] columnIndicies = new int[data[0].length];
      for (int i = 0; i < columnIndicies.length; ++i) {
        try {
          columnIndicies[i] = rs.findColumn(data[0][i]);
        } catch (final SQLException sqle) {
          Assert.fail("Column not found: "
              + data[0][i]);
        }
      }
      for (int row = 1; rs.next(); ++row) {
        for (int column = 0; column < data[row].length; ++column) {
          Assert.assertEquals(data[row][column], rs.getString(column + 1));
        }
      }

    } finally {
      SQLFunctions.close(rs);
      SQLFunctions.close(stmt);
      SQLFunctions.close(connection);
    }

  }
}
