/*
 * This code is released under GPL; see LICENSE for details.
 */

package fll.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import fll.TestUtils;
import fll.Utilities;

/**
 * Tests for the opencsv package.
 */
@ExtendWith(TestUtils.InitializeLogging.class)
public class OpenCSVTest {

  /**
   * Test that the default constructor for {@link CSVWriter} and {@link CSVReader}
   * are consistent in handling backslash.
   * See https://sourceforge.net/p/opencsv/bugs/268/ for more information.
   * 
   * @throws IOException test error
   * @throws CsvValidationException test error
   */
  @Test
  public void testWriterReaderBackslashConsistent() throws IOException, CsvValidationException {
    final String[] l1 = new String[] { "TEAMNUMBER", "TOURNAMENT", "JUDGE", "NOSHOW", "IDENTIFY_ONE",
                                       "IDENTIFY_ONE_COMMENT", "IDENTIFY_TWO", "IDENTIFY_TWO_COMMENT", "DESIGN_1",
                                       "DESIGN_1_COMMENT", "DESIGN_2", "DESIGN_2_COMMENT", "CREATE_1",
                                       "CREATE_1_COMMENT", "CREATE_2", "CREATE_2_COMMENT", "ITERATE_1",
                                       "ITERATE_1_COMMENT", "ITERATE_2", "ITERATE_2_COMMENT", "COMMUNICATE_1",
                                       "COMMUNICATE_1_COMMENT", "COMMUNICATE_2", "COMMUNICATE_2_COMMENT", "NOTE",
                                       "COMMENT_GREAT_JOB", "COMMENT_THINK_ABOUT" };
    final String[] l2 = new String[] { "XXXX", "12", "Judge1", "false", "4.0", "", "8.0", "", "3.0", "", "4.0", "",
                                       "7.0", "", "3.0", "", "4.0", "", "4.0", "", "4.0", "", "2.0", "\\", "", "", "" };
    final String[] l3 = new String[] { "XXXX", "12", "judge2", "false", "4.0", "", "7.0", "", "4.0", "", "4.0", "",
                                       "7.0", "", "4.0", "", "4.0", "", "4.0", "", "5.0", "", "5.0", "", "",
                                       "XXXXXXX.  X XXXXX XXXX XXX XXXX XXXXXX XXXXXX XX XXXXXXX XXXXXXXX XXX XXX X&X XXXXXXXXXX XXX XXXX XXXX XXXXXX.",
                                       "XXXXXX XXXX XXX XXXXX XX XXXXXXXX XX XXXX XXX XXXXXXXXXXX.  XXXX, XXXXXXX XXX XXXXX XXX XX XXXX XX XXX XXXXXXXX XXXXXXXX XXXX XXXX." };
    final String[] l4 = new String[] { "XXXX", "12", "judge3", "false", "4.0",
                                       "! XXX XXXXXXX, XXXXXX X XXXXX XX XXXXXXX XXX XXXXXXXX XXX XXXXXXXX XXX XXX XXXX XX XXXXXX XXXX XXXXXXXX XXX’XX XXXXXXX.",
                                       "4.0", "", "5.0", "", "5.0", "", "4.0", "", "2.0", "!", "5.0", "", "5.0", ".",
                                       "5.0", "", "8.0", "",
                                       "XXXX XXXXX XXXX, XXX XXXXXX,  XXXXXX YYYYY. \n\nXXXX\nXXXX", "XXXX YYY\n\nXXXX",
                                       "XXXX.\n\nXXXXX" };

    final StringWriter writer = new StringWriter();
    try (ICSVWriter csvWriter = Utilities.createCSVWriter((writer))) {
      csvWriter.writeNext(l1);
      csvWriter.writeNext(l2);
      csvWriter.writeNext(l3);
      csvWriter.writeNext(l4);
    }

    final String data = writer.toString();

    StringReader reader = new StringReader(data);
    try (CSVReader csvReader = Utilities.createCSVReader(reader)) {
      String[] headerLine = csvReader.readNext();
      assertNotNull(headerLine);

      String[] line;
      while (null != (line = csvReader.readNext())) {
        assertEquals(headerLine.length, line.length,
                     "The number of columns in each row should match the number of columns in the header row");
      }
    }
  }

  /**
   * Test that writing and reading with double quotes in a column works properly.
   * 
   * @throws IOException test error
   * @throws CsvValidationException test error
   */
  @Test
  public void testQuoteInString() throws IOException, CsvValidationException {
    final String[] header = new String[] { "column1", "column2", "column3", "column4" };
    final String[] l1 = new String[] { "good", "something", "\"", "else" };
    final String[] l2 = new String[] { "one", "two", "\"three\"", "four" };
    final String[] l3 = new String[] { "five", "six", "\"seven", "eight" };
    final String[] l4 = new String[] { "nine", "ten", "eleven\"", "twelve" };

    final StringWriter writer = new StringWriter();
    try (ICSVWriter csvWriter = Utilities.createCSVWriter((writer))) {
      csvWriter.writeNext(header);
      csvWriter.writeNext(l1);
      csvWriter.writeNext(l2);
      csvWriter.writeNext(l3);
      csvWriter.writeNext(l4);
    }

    final String data = writer.toString();

    StringReader reader = new StringReader(data);
    try (CSVReader csvReader = Utilities.createCSVReader(reader)) {
      String[] headerLine = csvReader.readNext();
      assertNotNull(headerLine);

      final String[] actual1 = csvReader.readNext();
      assertNotNull(actual1);
      assertEquals(headerLine.length, actual1.length);
      assertArrayEquals(l1, actual1);

      final String[] actual2 = csvReader.readNext();
      assertNotNull(actual2);
      assertEquals(headerLine.length, actual2.length);
      assertArrayEquals(l2, actual2);

      final String[] actual3 = csvReader.readNext();
      assertNotNull(actual3);
      assertEquals(headerLine.length, actual3.length);
      assertArrayEquals(l3, actual3);

      final String[] actual4 = csvReader.readNext();
      assertNotNull(actual4);
      assertEquals(headerLine.length, actual4.length);
      assertArrayEquals(l4, actual4);
    }
  }

}
