/*
 * Copyright (c) 2009 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.util;

import java.io.Closeable;
import java.io.IOException;

/**
 * Abstraction for reading files with cell data (csv, xls, ...).
 */
public interface CellFileReader extends Closeable {

  /**
   * 
   * @return The most recent line read (0-based)
   */
  public int getLineNumber();
  /**
   * Read the next line
   * @return the line as an array of Strings
   * @throws IOException
   */
  public String[] readNext() throws IOException;
  
}
