/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import au.com.bytecode.opencsv.CSVReader;
import fll.Utilities;

/**
 * Read csv files.
 */
public class CSVCellReader extends CellFileReader {

  private final CSVReader delegate;

  /**
   * @see CSVReader#CSVReader(java.io.Reader, char)
   */
  public CSVCellReader(final File file,
                       final char separator) throws FileNotFoundException {
    delegate = new CSVReader(new InputStreamReader(new FileInputStream(file), Utilities.DEFAULT_CHARSET), separator);
  }

  /**
   * @see CSVReader#CSVReader(java.io.Reader)
   */
  public CSVCellReader(final File file) throws FileNotFoundException {
    delegate = new CSVReader(new InputStreamReader(new FileInputStream(file), Utilities.DEFAULT_CHARSET));
  }

  /**
   * @see fll.util.CellFileReader#getLineNumber()
   */
  public int getLineNumber() {
    return delegate.getLineNumber();
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
