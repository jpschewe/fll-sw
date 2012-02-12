/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import fll.Utilities;

/**
 * Abstraction for reading files with cell data (csv, xls, ...).
 */
public abstract class CellFileReader implements Closeable {

  private static final Logger LOGGER = LogUtils.getLogger();

  /**
   * @return The most recent line read (0-based)
   */
  public abstract int getLineNumber();

  /**
   * Read the next line
   * 
   * @return the line as an array of Strings
   * @throws IOException
   */
  public abstract String[] readNext() throws IOException;

  public abstract void close() throws IOException;

  /**
   * Open file and create the appropriate cell reader for it. If this file is a
   * spreadsheet, then the sheet name must be specified.
   * 
   * @param file the file to open
   * @param sheetName the sheet to load in the file, ignored if a CSV/TSV file
   * @return the appropriate cell reader
   * @throws IOException
   * @throws InvalidFormatException
   */
  public static CellFileReader createCellReader(final File file,
                                                final String sheetName) throws InvalidFormatException, IOException {
    if (ExcelCellReader.isExcelFile(file)) {
      FileInputStream fis = null;
      try {
        fis = new FileInputStream(file);
        return new ExcelCellReader(fis, sheetName);
      } finally {
        if (null != fis) {
          fis.close();
        }
      }
    } else {
      // determine if the file is tab separated or comma separated, check the
      // first line for tabs and if they aren't found, assume it's comma
      // separated
      final BufferedReader breader = new BufferedReader(new InputStreamReader(new FileInputStream(file),
                                                                              Utilities.DEFAULT_CHARSET));
      try {
        final String firstLine = breader.readLine();
        if (null == firstLine) {
          LOGGER.warn("Empty file");
          return new CSVCellReader(file);
        }
        if (firstLine.indexOf("\t") != -1) {
          return new CSVCellReader(file, '\t');
        } else {
          return new CSVCellReader(file);
        }
      } finally {
        breader.close();
      }
    }

  }
}
