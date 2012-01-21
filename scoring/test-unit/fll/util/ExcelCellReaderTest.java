/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import junit.framework.Assert;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.Test;

import fll.Utilities;

/**
 * Test for {@link ExcelCellReader}.
 */
public class ExcelCellReaderTest {

  @Test
  public void testEmptyCells() throws InvalidFormatException, IOException, ParseException {
    final InputStream stream = ExcelCellReaderTest.class.getResourceAsStream("data/excel-test.xls");
    final ExcelCellReader reader = new ExcelCellReader(stream, "roster");
    String[] values;
    boolean found = false;
    while (null != (values = reader.readNext())) {
      if (values.length > 0) { // skip empty lines
        if ("line number".equals(values[0])) {
          // skip header
          continue;
        }
        // find a line 19 and check that column 2 is null
        final Number lineNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(values[0]);
        if (19 == lineNumber.intValue()) {
          found = true;
          Assert.assertNull("line 19 should have null for column 2", values[2]);
        }
      }
    }
    Assert.assertTrue("Can't find line 19", found);
  }
  
  @Test
  public void testEmptyFirstCell() throws InvalidFormatException, IOException, ParseException {
    final InputStream stream = ExcelCellReaderTest.class.getResourceAsStream("data/excel-test.xls");
    final ExcelCellReader reader = new ExcelCellReader(stream, "Northome");
    String[] values;
    boolean found = false;
    while (null != (values = reader.readNext())) {
      if (values.length > 0) { // skip empty lines
        if ("line number".equals(values[1])) {
          // skip header
          continue;
        }
        // find a line 3 and check that column 1 is null
        final Number lineNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(values[1]);
        if (3 == lineNumber.intValue()) {
          found = true;
          Assert.assertNull("line 3 should have null for column 1", values[0]);
        }
      }
    }
    Assert.assertTrue("Can't find line 3, null first cell messed things up", found);    
  }

  /**
   * Make sure that integers read in are kept as integers.
   * 
   * @throws IOException
   * @throws InvalidFormatException
   */
  @Test
  public void testIntegers() throws InvalidFormatException, IOException {
    final InputStream stream = ExcelCellReaderTest.class.getResourceAsStream("data/excel-test.xls");
    final ExcelCellReader reader = new ExcelCellReader(stream, "roster");
    // check that everything in column E (4) is an integer
    String[] values;
    while (null != (values = reader.readNext())) {
      if (values.length > 0) { // skip empty lines
        if ("line number".equals(values[0])) {
          // skip header
          continue;
        }
        final String divStr = values[4];
        Assert.assertTrue("Found decimal point line: "
            + values[0], -1 == divStr.indexOf('.'));
      }
    }

  }

}
