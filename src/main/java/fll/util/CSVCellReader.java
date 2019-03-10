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

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

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
   * @throws IOException
   */
  public CSVCellReader(final Path file,
                       final char separator)
      throws IOException {
    final CSVParser parser = new CSVParserBuilder().withSeparator(separator).build();
    delegate = new CSVReaderBuilder(Files.newBufferedReader(file, Utilities.DEFAULT_CHARSET)).withCSVParser(parser)
                                                                                             .build();
  }

  /**
   * @throws IOException
   */
  public CSVCellReader(final Path file) throws IOException {
    delegate = new CSVReaderBuilder(Files.newBufferedReader(file, Utilities.DEFAULT_CHARSET)).build();
  }

  /**
   * @see fll.util.CellFileReader#getLineNumber()
   */
  public long getLineNumber() {
    return delegate.getLinesRead();
  }

  /**
   * @throws IOException
   * @see fll.util.CellFileReader#readNext()
   */
  public String[] readNext() throws IOException {
    return delegate.readNext();
  }

  public void close() throws IOException {
    delegate.close();
  }

}
