/*
 * Copyright (c) 2010 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

package fll.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * Wrapper class for a response that stores everything in a local buffer to be
 * processed later.
 *
 * @see HttpServletResponse
 */
public class ByteResponseWrapper extends HttpServletResponseWrapper {
  private final StringWriter string;

  private final PrintWriter writer;

  private boolean binaryUsed = false;

  /**
   * Was {@link #getOutputStream()} used for output.
   *
   * @return if {@link #getOutputStream()} has been called
   */
  public boolean isBinaryUsed() {
    return binaryUsed;
  }

  private boolean stringUsed = false;

  /**
   * @return if {@link #getWriter()} used for output.
   */
  public boolean isStringUsed() {
    return stringUsed;
  }

  /**
   * @return the string that was written
   */
  public String getString() {
    return string.getBuffer().toString();
  }

  /**
   * @param response passed to the parent class
   */
  public ByteResponseWrapper(final HttpServletResponse response) {
    super(response);
    string = new StringWriter();
    writer = new PrintWriter(string);
  }

  @Override
  public PrintWriter getWriter() {
    if (binaryUsed) {
      throw new IllegalStateException("Cannot call getWriter after calling getOutputStream");
    }
    stringUsed = true;
    return writer;
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    if (stringUsed) {
      throw new IllegalStateException("Cannot call getOutputStream after calling getWriter");
    }
    binaryUsed = true;
    // let everything be written directly
    return super.getOutputStream();
  }

}
