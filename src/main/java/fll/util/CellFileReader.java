/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.checkerframework.checker.nullness.qual.Nullable;

import fll.Utilities;

/**
 * Abstraction for reading files with cell data (csv, xls, ...).
 */
public abstract class CellFileReader implements Closeable {

  private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();

  /**
   * @return The most recent line read (0-based)
   */
  public abstract long getLineNumber();

  /**
   * Read the next line.
   *
   * @return the line as an array of Strings, null at the end of file
   * @throws IOException if there is an error reading the file
   */
  public abstract @Nullable String @Nullable [] readNext() throws IOException;

  @Override
  public abstract void close() throws IOException;

  /**
   * Open file and create the appropriate cell reader for it. If this file is a
   * spreadsheet, then the sheet name must be specified.
   *
   * @param file the file to open
   * @param sheetName the sheet to load in the file, ignored if a CSV/TSV file
   * @return the appropriate cell reader
   * @throws IOException if there is a problem reading the file
   * @throws InvalidFormatException if there is a problem reading the Excel file
   */
  public static CellFileReader createCellReader(final File file,
                                                final @Nullable String sheetName)
      throws InvalidFormatException, IOException {
    return createCellReader(file.toPath(), sheetName);
  }

  /**
   * Read either an Excel file or a CSV file.
   *
   * @param file the file to read
   * @param sheetName the sheet in an Excel file to read, null if reading a CSV
   *          file
   * @return the appropriate cell reader
   * @throws InvalidFormatException if there is a problem reading the Excel file
   * @throws IOException if there is a problem reading the file
   */
  public static CellFileReader createCellReader(final Path file,
                                                final @Nullable String sheetName)
      throws InvalidFormatException, IOException {
    if (ExcelCellReader.isExcelFile(file)) {
      try (InputStream fis = Files.newInputStream(file)) {
        if (null == sheetName) {
          throw new IllegalArgumentException("Sheet name cannot be null when reading an Excel file");
        } else {
          return new ExcelCellReader(fis, sheetName);
        }
      }
    } else {
      // determine if the file is tab separated or comma separated, check the
      // first line for tabs and if they aren't found, assume it's comma
      // separated
      try (BufferedReader breader = Files.newBufferedReader(file, Utilities.DEFAULT_CHARSET)) {
        final String firstLine = breader.readLine();
        if (null == firstLine) {
          LOGGER.warn("Empty file");
          return new CSVCellReader(file);
        }
        if (firstLine.indexOf('\t') != -1) {
          return new CSVCellReader(file, '\t');
        } else {
          return new CSVCellReader(file);
        }
      }
    }

  }

  /**
   * @param skip number of rows to skip
   * @throws IOException if there is an error reading the file
   */
  public void skipRows(final int skip) throws IOException {
    int leftToSkip = skip;
    while (leftToSkip > 0) {
      readNext();
      --leftToSkip;
    }
  }
}
