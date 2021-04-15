/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import fll.Utilities;

/**
 * Read csv files.
 */
public class CSVCellReader extends CellFileReader {

  private final CSVReader delegate;

  /**
   * @param file the file to read in
   * @throws IOException if the there is an error converting the file to a path,
   *           see {@link File#toPath()}
   */
  public CSVCellReader(final File file) throws IOException {
    this(file.toPath());
  }

  /**
   * @param file the file to read
   * @param separator the column separator
   * @throws IOException if there is an error opening the file
   */
  public CSVCellReader(final Path file,
                       final char separator)
      throws IOException {
    final CSVParser parser = new CSVParserBuilder().withSeparator(separator).build();
    delegate = new CSVReaderBuilder(Files.newBufferedReader(file, Utilities.DEFAULT_CHARSET)).withCSVParser(parser)
                                                                                             .build();
  }

  /**
   * @param file the file to read
   * @throws IOException if there is an error opening the file
   */
  public CSVCellReader(final Path file) throws IOException {
    delegate = new CSVReaderBuilder(Files.newBufferedReader(file, Utilities.DEFAULT_CHARSET)).build();
  }

  @Override
  public long getLineNumber() {
    return delegate.getLinesRead();
  }

  /**
   * @throws IOException if there is an error reading from the file
   */
  @Override
  public @Nullable String @Nullable [] readNext() throws IOException {
    try {
      return delegate.readNext();
    } catch (final CsvValidationException e) {
      throw new IOException("Invalid line in CSV file", e);
    }
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }

}
